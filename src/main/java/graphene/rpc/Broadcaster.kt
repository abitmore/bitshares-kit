package graphene.rpc

import graphene.app.API
import graphene.serializers.GRAPHENE_JSON_PLATFORM_SERIALIZER
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.jvm.Throws

interface AllBroadcaster : LoginBroadcaster, DatabaseBroadcaster

@OptIn(ExperimentalCoroutinesApi::class)
interface Broadcaster {
    val broadcastScope: CoroutineScope
    suspend fun broadcast(method: API, params: JsonArray) : SocketResult
//    fun broadcast(struct: BroadcastStruct)
}

/* ================================ begin ================================ */

inline fun <reified R> decodeParamsFromJsonElementOrThrow(result: SocketResult) : R {
    return when (result) {
        is SocketCallback -> GRAPHENE_JSON_PLATFORM_SERIALIZER.decodeFromJsonElement(result.result)
        is SocketError ->  throw SocketErrorException(result)
        is SocketNotice -> TODO()
    }
}

/* ================================ 0 params ================================ */
@Throws(SocketException::class, )
suspend inline fun Broadcaster.sendOrThrow(method: API) : SocketResult {
    val array = buildJsonArray { }
    return broadcast(method, array)
}

@Throws(SocketException::class, )
suspend inline fun <reified R> Broadcaster.sendForResultOrThrow(method: API) : R {
    return decodeParamsFromJsonElementOrThrow(sendOrThrow(method))
}
suspend inline fun <reified R> Broadcaster.sendForResultOrNull(method: API) : R? {
    return runCatching { decodeParamsFromJsonElementOrThrow<R>(sendOrThrow(method)) }.getOrNull()
}

/* ================================ 1 params ================================ */
@Throws(SocketException::class, )
suspend inline fun <reified T1> Broadcaster.sendOrThrow(method: API, param1: T1) : SocketResult {
    val array = buildJsonArray {
        add(GRAPHENE_JSON_PLATFORM_SERIALIZER.encodeToJsonElement(param1))
    }
    return broadcast(method, array)
}
@Throws(SocketException::class, )
suspend inline fun <reified T1, reified R> Broadcaster.sendForResultOrThrow(method: API, param1: T1) : R {
    return decodeParamsFromJsonElementOrThrow(sendOrThrow(method, param1))
}
suspend inline fun <reified T1, reified R> Broadcaster.sendForResultOrNull(method: API, param1: T1) : R? {
    return runCatching { decodeParamsFromJsonElementOrThrow<R>(sendOrThrow(method, param1)) }.getOrNull()
}

/* ================================ 2 params ================================ */
@Throws(SocketException::class, )
suspend inline fun <reified T1, reified T2> Broadcaster.sendOrThrow(method: API, param1: T1, param2: T2) : SocketResult {
    val array = buildJsonArray {
        add(GRAPHENE_JSON_PLATFORM_SERIALIZER.encodeToJsonElement(param1))
        add(GRAPHENE_JSON_PLATFORM_SERIALIZER.encodeToJsonElement(param2))
    }
    return broadcast(method, array)
}
@Throws(SocketException::class, )
suspend inline fun <reified T1, reified T2, reified R> Broadcaster.sendForResultOrThrow(method: API, param1: T1, param2: T2) : R {
    return decodeParamsFromJsonElementOrThrow(sendOrThrow(method, param1, param2))
}
suspend inline fun <reified T1, reified T2, reified R> Broadcaster.sendForResultOrNull(method: API, param1: T1, param2: T2) : R? {
    return runCatching { decodeParamsFromJsonElementOrThrow<R>(sendOrThrow(method, param1, param2)) }.getOrNull()
}

/* ================================ end ================================ */