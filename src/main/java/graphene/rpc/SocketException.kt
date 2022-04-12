package graphene.rpc

import io.ktor.utils.io.errors.*

sealed class SocketException : IOException()
class SocketManualStopException : SocketException()
class SocketClosedException : SocketException()
class SocketErrorException(override val message: String) : SocketException() {
    constructor(error: SocketError): this(error.error.toString())
}