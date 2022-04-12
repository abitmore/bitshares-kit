package graphene.serializers

import kotlinx.io.core.ByteOrder
import kotlinx.io.core.ByteReadPacket
import kotlinx.io.core.buildPacket
import kotlinx.io.core.readBytes
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

class IOConfiguration(
    val byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN,
    val isIndexed: Boolean = false,
)

sealed class IO(
    val configuration: IOConfiguration,
    override val serializersModule: SerializersModule,
) : BinaryFormat {
    companion object Default : IO(IOConfiguration(), EmptySerializersModule)
    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
        return buildPacket {
            val encoder = IOEncoder(this, this@IO)
            serializer.serialize(encoder, value)
        }.readBytes()
    }
    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        return ByteReadPacket(bytes).run {
            val decoder = IODecoder(this, this@IO)
            deserializer.deserialize(decoder)
        }
    }

}

internal class IOImpl(
    configuration: IOConfiguration,
    serializersModule: SerializersModule,
) : IO(configuration, serializersModule)


class IOBuilder(io: IO) {
    var byteOrder: ByteOrder = io.configuration.byteOrder
    var isIndexed: Boolean = io.configuration.isIndexed
    var serializersModule: SerializersModule = io.serializersModule
    fun build(): IOConfiguration {
        return IOConfiguration(
            byteOrder,
            isIndexed
        )
    }
}

fun IO(from: IO = IO.Default, action: IOBuilder.() -> Unit): IO {
    val builder = IOBuilder(from)
    builder.action()
    return IOImpl(
        builder.build(),
        builder.serializersModule
    )
}
