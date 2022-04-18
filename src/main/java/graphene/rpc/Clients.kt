package graphene.rpc

import graphene.app.API
import graphene.app.APIType
import graphene.extension.info
import graphene.serializers.GRAPHENE_JSON_PLATFORM_SERIALIZER
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
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

fun GrapheneClient(action: GrapheneClientConfigBuilder.() -> Unit) = GrapheneClient(
    GrapheneClientConfigBuilder(GrapheneClientConfig()).apply(action).build()
)

class GrapheneClientConfigBuilder(conf: GrapheneClientConfig) {
    var id: Long = conf.id
    var name: String = conf.name
    var url: String = conf.url
    var debug: Boolean = conf.debug
    var enableFallback: Boolean = conf.enableFallback
    fun build() = GrapheneClientConfig(id, name, url, debug, enableFallback)
}

data class GrapheneClientConfig(
    val id: Long = 0L,
    val name: String = "localhost",
    val url: String = "ws://127.0.0.1/",
    val debug: Boolean = true,
    val enableFallback: Boolean = false
)

class GrapheneClient(val configuration: GrapheneClientConfig) : AllBroadcaster {

    constructor(node: K_Node) : this(GrapheneClientConfig(0L, node.name, node.url))
    private fun <T> T.console(title: Any = System.currentTimeMillis()) = apply { if (configuration.debug) listOf("GrapheneClient", title, this.toString()).info() }

    private val session = SupervisorJob()
    private val clientScope = CoroutineScope(Dispatchers.IO + session)

    override val broadcastScope = CoroutineScope(Dispatchers.IO + session)

    private var lastSocketSession: Job = Job()

    private val sequence: AtomicInteger = AtomicInteger(0)
    private val connected: AtomicBoolean = AtomicBoolean(false)
    private val identifiers: MutableMap<APIType, Int?> = mutableMapOf(APIType.LOGIN to 1)

    private val sendingChannel: Channel<BroadcastStruct> = Channel()
    val fallbackChannel: Channel<BroadcastStruct> = Channel(UNLIMITED)

    private val waiting: MutableList<Continuation<Unit>> = mutableListOf()

    private val client = HttpClient(CIO.create()) {
        install(WebSockets) {
            // TODO: 2022/4/11  
            contentConverter = KotlinxWebsocketSerializationConverter(Json {
                ignoreUnknownKeys = false
                encodeDefaults = true
            })
            pingInterval = 10_000
        }
        install(ContentNegotiation)
    }

    private val callbackMap: MutableMap<Int, BroadcastStruct> = mutableMapOf()

    private val subscribeMap: MutableMap<Int, BroadcastStruct> = mutableMapOf()

    private fun callback(id: Int, result: BroadcastStruct) = callbackMap.set(id, result)

    private fun callback(id: Int, result: SocketResult) = callbackMap.remove(id)?.cont?.resume(result)
    private fun callback(id: Int, result: SocketException) = callbackMap.remove(id)?.cont?.resumeWithException(result)
    private fun callback(id: Int, result: Result<SocketResult>) = callbackMap.remove(id)?.cont?.resumeWith(result)

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
        return suspendCancellableCoroutine {
            waiting.add(it)
            it.invokeOnCancellation { _ ->
                waiting.remove(it)
            }
        }
    }

    // broadcast
    override suspend fun broadcast(method: API, params: JsonArray) : SocketResult {
        if (method.type != APIType.LOGIN) waitForOpen()
        return suspendCancellableCoroutine {
            val struct = BroadcastStruct(method, false, params, it)
            broadcastScope.launch {
                try {
                    sendingChannel.send(struct)
                } catch (e: Throwable) {
                    struct.cont.resumeWithException(e)
                    e.printStackTrace()
                    throw e
                }
            }
        }
    }

    private fun open() {
        connected.set(true)
        waiting.forEach { kotlin.runCatching { it.resume(Unit) } } // TODO: 2022/4/14  
        waiting.clear()
        if (configuration.debug) "======== Websocket Open ======== ${configuration.url}".info()
    }

    private suspend fun DefaultClientWebSocketSession.sendRPC() {
        if (configuration.debug) "======== Start Sending ======== ${configuration.url}".info()
        while (isActive) {
            val struct = sendingChannel.receive()
            val socketCall = buildSocketCall(struct)
            callback(socketCall.id, struct)
            try {
                GRAPHENE_JSON_PLATFORM_SERIALIZER.encodeToString(socketCall).console("Call >>>")
                sendSerialized(socketCall)
            } catch (e: Exception) {
                callbackMap.remove(socketCall.id)
                sendingChannel.send(struct)
                e.printStackTrace()
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.receiveRPC() {
        if (configuration.debug) "======== Start Recving ======== ${configuration.url}".info()
        while (isActive) {
            val result = receiveDeserialized<SocketResult>().console("Recv <<<")
            callback(result.id, result)
        }
    }

    suspend fun launchSocket() {
        client.wss(configuration.url) {
            val sendJob = launch { sendRPC() }
            val receiveJob = launch { receiveRPC() }
            login().let { if (!it) throw SocketErrorException("Incorrect node params!") }
            getIdentifiers().let { identifiers.putAll(it) }
            open()
            listOf(sendJob, receiveJob).joinAll()
        }
    }

    // public
    fun start() {
        if (lastSocketSession.isActive) lastSocketSession.cancel()
        lastSocketSession = clientScope.launch {
            try {
                launchSocket()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
             // TODO: 2022/4/1 java.net.SocketException: Bad file descriptor
        }
    }

    fun stop(reason: Exception = SocketManualStopException()) {
        connected.set(false)
        sequence.set(0)
        identifiers.clear()
        identifiers[APIType.LOGIN] = 1
        if (lastSocketSession.isActive) lastSocketSession.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun cancel(reason: Exception = SocketManualStopException()) {
        "STOP() CALLED FROM".console()
//        if (!sendingChannel.isClosedForReceive) {
            clientScope.launch {
                if (configuration.enableFallback && !fallbackChannel.isClosedForSend) {
                    callbackMap.forEach {
                        fallbackChannel.send(it.value)
                    }
                    callbackMap.clear()
                    sendingChannel.consumeEach {
                        fallbackChannel.send(it)
                    }
                } else {
                    callbackMap.clear()
                    sendingChannel.consumeEach {
                        runCatching { it.cont.resumeWithException(reason) }
                    }
                }
            }
//        }
//        waiting.forEach {
//            runCatching { it.resumeWithException(reason) }
//        }
//        waiting.clear()
        "STOP() CALLED FROM".info()
//        session.cancel()
    }
}