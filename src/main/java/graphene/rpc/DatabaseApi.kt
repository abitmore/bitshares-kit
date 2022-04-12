package graphene.rpc

import graphene.app.APIType
import graphene.app.API_TYPE_MAP
import graphene.app.DatabaseAPI
import graphene.app.LoginAPI
import graphene.chain.AbstractObject
import graphene.protocol.ObjectId
import graphene.protocol.SignedTransaction
import kotlinx.coroutines.*

interface DatabaseBroadcaster : Broadcaster {

    // get_objects
    suspend fun getObjects(ids: List<ObjectId>): List<AbstractObject?> {
        return sendForResultOrNull(DatabaseAPI.GET_OBJECTS, ids) ?: emptyList()
    }
    suspend fun getObjects(vararg ids: ObjectId): List<AbstractObject?> {
        return sendForResultOrNull(DatabaseAPI.GET_OBJECTS, ids.toList()) ?: emptyList()
    }
    suspend fun getObject(id: ObjectId): AbstractObject? {
        return getObjects(id).firstOrNull()
    }

    // get_transaction_hex
    suspend fun getTransactionHex(tx: SignedTransaction): String? {
        return sendForResultOrNull(DatabaseAPI.GET_TRANSACTION_HEX, tx)
    }
    suspend fun getTransactionHexOrThrow(tx: SignedTransaction): String {
        return sendForResultOrThrow(DatabaseAPI.GET_TRANSACTION_HEX, tx)
    }

}

interface LoginBroadcaster : Broadcaster {

    // login
    suspend fun login(node: Node): Boolean {
        return sendForResultOrNull(LoginAPI.LOGIN, node.username, node.password) ?: false
    }
    suspend fun getIdentifiers(): Map<APIType, Int?> {
        return API_TYPE_MAP.map { (type, api) ->
            broadcastScope.async { type to sendForResultOrNull<Int>(api) }
        }.awaitAll().toMap()
    }


}