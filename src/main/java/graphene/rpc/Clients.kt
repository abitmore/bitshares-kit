package graphene.rpc

import graphene.app.API
import graphene.app.APIType
import graphene.extension.info
import graphene.serializers.GRAPHENE_JSON_PLATFORM_SERIALIZER
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import io.ktor.util.collections.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class GrapheneClient(val node: Node, var debug: Boolean = true) : Broadcaster, LoginBroadcaster {

    private val session = Job()
    private val clientScope = CoroutineScope(Dispatchers.IO + session)

    private val sendScope = CoroutineScope(Dispatchers.IO.limitedParallelism(10) + session)
    private val receiveScope = CoroutineScope(Dispatchers.IO.limitedParallelism(10) + session)

    override val broadcastScope = CoroutineScope(Dispatchers.IO + session)


    private fun <T> T.console(title: Any = System.currentTimeMillis()) = apply { if (debug) listOf("GrapheneClient", title, this.toString()).info() }

    private val sequence: AtomicInteger = AtomicInteger(0)
    private val connected: AtomicBoolean = AtomicBoolean(false)
    private val identifiers: MutableMap<APIType, Int?> = mutableMapOf(APIType.LOGIN to 1)

    private val sendingChannel: Channel<BroadcastStruct> = Channel()
    val fallbackChannel: Channel<BroadcastStruct> = Channel(UNLIMITED)

    private val waiting: MutableList<Continuation<Unit>> = ConcurrentList()

    private val client = HttpClient(CIO.create()) {
        install(WebSockets) {
            // TODO: 2022/4/11  
            contentConverter = KotlinxWebsocketSerializationConverter(Json {
                ignoreUnknownKeys = false
                encodeDefaults = true
            })
            pingInterval = 10000
        }
        install(ContentNegotiation)
    }

    private val callbackMap: MutableMap<Int, Continuation<SocketResult>> = mutableMapOf()
    private val subscribeMap: MutableMap<Int, Continuation<SocketResult>> = mutableMapOf()

    private fun callback(id: Int, result: Continuation<SocketResult>) = callbackMap.set(id, result)
    private fun callback(id: Int, result: SocketResult) = callbackMap.remove(id)?.resume(result)
    private fun callback(id: Int, result: SocketException) = callbackMap.remove(id)?.resumeWithException(result)
    private fun callback(id: Int, result: Result<SocketResult>) = callbackMap.remove(id)?.resumeWith(result)

    private fun buildSocketCall(struct: BroadcastStruct): SocketCall {
        val id = sequence.getAndIncrement()
        val version = SocketCall.JSON_RPC_VERSION
        val method = SocketCall.METHOD_CALL
        val params = buildJsonArray {
            add(identifiers[struct.method.type]) // TODO: 2022/4/12
            add(struct.method.nameString)
            add(struct.params)
        }
        return SocketCall(id, version, method, params)
    }

    suspend fun waitForOpen() {
        if (connected.get()) return
        return suspendCoroutine { waiting.add(it) }
    }

    // broadcast
    override suspend fun broadcast(method: API, params: JsonArray) : SocketResult {
        return suspendCoroutine {
            val struct = BroadcastStruct(method, false, params, it)
            broadcastScope.launch { broadcast(struct) }
        }
    }
    // TODO: 2022/4/12
    override suspend fun broadcast(struct: BroadcastStruct) {
        if (struct.method.type != APIType.LOGIN) waitForOpen()
        sendingChannel.send(struct)
    }

    private fun open() {
        connected.set(true)
        waiting.forEach { it.resume(Unit) }
        console("================ WEBSOCKET OPEN ================")
    }

    private suspend fun DefaultClientWebSocketSession.sendRPC() {
        console("Start Calling >>>")
        while (true) {
            try {
                val struct = sendingChannel.receive()
                val socketCall = buildSocketCall(struct)
                try {
                    callback(socketCall.id, struct.cont)
                    GRAPHENE_JSON_PLATFORM_SERIALIZER.encodeToString(socketCall).console("Call >>>")
                    sendSerialized(socketCall)
                } catch (e: Exception) {
                    callbackMap.remove(socketCall.id)
                    fallbackChannel.send(struct)
                    sendingChannel.consumeEach { fallbackChannel.send(it) }

                    e.printStackTrace()
                    break
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                break
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.receiveRPC() {
        console("Start Receiving <<<")
        while (true) {
            try {
                val result = receiveDeserialized<SocketResult>().console("Recv <<<")
                callback(result.id, result)
            } catch (e: Exception) {
                e.printStackTrace()
                e.console("================ WEBSOCKET STOP ================")
                break
            }
        }
    }

    private suspend fun launchSocket() {
        client.wss(node.url) {
            try {
                val sendJob = sendScope.launch { sendRPC() }
                val receiveJob = receiveScope.launch { receiveRPC() }
                login(node).let { if (!it) throw SocketErrorException("Incorrect node params!") }
                getIdentifiers().let { identifiers.putAll(it) }
                open()
                listOf(sendJob, receiveJob).joinAll()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    // public
    fun start() {
        clientScope.launch {
            launchSocket() // TODO: 2022/4/1 java.net.SocketException: Bad file descriptor
        }
    }

    fun stop(reason: Exception = SocketManualStopException()) {
        "STOP() CALLED FROM".console()
        try {
            sendingChannel.close(reason)
            fallbackChannel.close(reason)
            session.cancel()
            waiting.forEach {
                it.resumeWithException(reason)
            }
            waiting.clear()
        } catch (e: Throwable) {
//            e.printStackTrace()
        }
    }
}