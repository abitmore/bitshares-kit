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
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.seconds

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
    val name: String = "null",
    val url: String = "wss://",
    val debug: Boolean = true,
    val enableFallback: Boolean = false
)

// TODO: 2022/4/19  move to plugin
class GrapheneClient(val configuration: GrapheneClientConfig) : CoroutineScope, AllBroadcaster {

    enum class State {
        CONNECTING,
        CONNECTED,
        LOGGING_IN,
        CLOSED
    }

    override val coroutineContext: CoroutineContext = SupervisorJob()
    override val broadcastScope = CoroutineScope(Dispatchers.IO + coroutineContext)

    val state: StateFlow<State> get() = stateLocal
    private val stateLocal = MutableStateFlow(State.CLOSED)

    private val sequence: AtomicInt = atomic(0)
    private val connected: AtomicBoolean = atomic(false)
    private val identifiers: MutableMap<APIType, Int?> = mutableMapOf(APIType.LOGIN to 1)

    private val sendingChannel: Channel<BroadcastStruct> = Channel()
    val fallbackChannel: Channel<BroadcastStruct> = Channel(UNLIMITED)

    private val waiting: MutableList<Continuation<Unit>> = mutableListOf()

    private val httpClient = HttpClient(CIO.create()) {
        install(WebSockets) {
            // TODO: 2022/4/11  
            contentConverter = KotlinxWebsocketSerializationConverter(Json {
                ignoreUnknownKeys = false
                encodeDefaults = true
            })
            pingInterval = 10.seconds.inWholeMilliseconds // keeping alive requires 60 seconds
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

    suspend fun awaitConnection() {
        if (connected.value) return
        return suspendCancellableCoroutine {
            waiting.add(it)
            it.invokeOnCancellation { _ ->
                waiting.remove(it)
            }
        }
    }

    private suspend fun open() {
        connected.value = true
        waiting.forEach { kotlin.runCatching { it.resume(Unit) } } // TODO: 2022/4/14  
        waiting.clear()
        if (configuration.debug) "======== Websocket Open ======== ${configuration.url}".info()
    }

    private suspend fun DefaultClientWebSocketSession.sendJsonRpc() {
        if (configuration.debug) "======== Start Sending ======== ${configuration.url}".info()
        while (isActive) {
            val struct = sendingChannel.receive()
            val socketCall = buildSocketCall(struct)
            callback(socketCall.id, struct)
            try {
                GRAPHENE_JSON_PLATFORM_SERIALIZER.encodeToString(socketCall).also { "Call >>> $it".info() }
                sendSerialized(socketCall)
            } catch (e: Exception) {
                callbackMap.remove(socketCall.id)
                sendingChannel.send(struct)
                e.printStackTrace()
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.receiveJsonRpc() {
        if (configuration.debug) "======== Start Recving ======== ${configuration.url}".info()
        while (isActive) {
            val result = receiveDeserialized<SocketResult>().also { "Recv <<< $it".info() }
            callback(result.id, result)
        }
    }

    // broadcast
    override suspend fun broadcast(method: API, params: JsonArray) : SocketResult {
        if (method.type != APIType.LOGIN) awaitConnection()
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

    suspend fun start() {
        stateLocal.emit(State.CONNECTING)
        try {
            httpClient.wss(configuration.url) {
                val sendJob = launch { sendJsonRpc() }
                val receiveJob = launch { receiveJsonRpc() }
                stateLocal.emit(State.LOGGING_IN)
                login().let { if (!it) throw SocketErrorException("Incorrect username or password!") }
                getIdentifiers().let { identifiers.putAll(it) }
                open()
                stateLocal.emit(State.CONNECTED)
                listOf(sendJob, receiveJob).joinAll()
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            stateLocal.emit(State.CLOSED)
        }

    }

    fun stop(reason: Exception = SocketManualStopException()) {
        connected.value = false
        sequence.value = 0
        identifiers.clear()
        identifiers[APIType.LOGIN] = 1
//        if (lastSocketSession.isActive) lastSocketSession.cancel()
    }

    suspend fun cancel1(reason: Exception = SocketManualStopException()) {
//        if (!sendingChannel.isClosedForReceive) {
        @OptIn(ExperimentalCoroutinesApi::class)
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
//        }
//        waiting.forEach {
//            runCatching { it.resumeWithException(reason) }
//        }
//        waiting.clear()
//        session.cancel()
        "Session Canceled for $reason".info()
    }
}