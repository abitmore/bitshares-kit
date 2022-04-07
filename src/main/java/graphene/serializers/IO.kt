package graphene.serializers

import graphene.extension.info
import graphene.extension.toHexByteArray
import kotlinx.io.core.*
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

class IOConfiguration(
    val byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN,
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
    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T = TODO()
}

internal class IOImpl(
    configuration: IOConfiguration,
    serializersModule: SerializersModule,
) : IO(configuration, serializersModule)

class IOEncoder(
    private val output: Output,
    val io: IO,
) : AbstractEncoder() {
    override val serializersModule: SerializersModule = io.serializersModule
    private fun Output.writeVarInt(value: Int) {
        var curr = value
        while (curr and -0x80 != 0) {
            writeByte((curr and 0x7F or 0x80).toByte())
            curr = curr ushr 7
        }
        writeByte((curr and 0x7F).toByte())
    }
    private fun Output.writeVarInt(value: Long) {
        var curr = value
        while (curr and -0x80L != 0L) {
            writeByte((curr and 0x7F or 0x80).toByte())
            curr = curr ushr 7
        }
        writeByte((curr and 0x7F).toByte())
    }
    private val useLittleEndian = io.configuration.byteOrder == ByteOrder.LITTLE_ENDIAN

    fun encodeByteArray(value: ByteArray): Unit = output.writeFully(value)
    fun encodeVarInt(value: Int): Unit = output.writeVarInt(value)
    fun encodeVarInt(value: Long): Unit = output.writeVarInt(value)
    override fun encodeBoolean(value: Boolean) = output.writeByte(if (value) 0x01 else 0x00)
    override fun encodeByte(value: Byte): Unit = output.writeByte(value)
    override fun encodeShort(value: Short): Unit = if (useLittleEndian) output.writeShortLittleEndian(value) else output.writeShort(value)
    override fun encodeInt(value: Int): Unit = if (useLittleEndian) output.writeIntLittleEndian(value) else output.writeInt(value)
    override fun encodeLong(value: Long): Unit = if (useLittleEndian) output.writeLongLittleEndian(value) else output.writeLong(value)
    override fun encodeFloat(value: Float): Unit = if (useLittleEndian) output.writeFloatLittleEndian(value) else output.writeFloat(value)
    override fun encodeDouble(value: Double): Unit = if (useLittleEndian) output.writeDoubleLittleEndian(value) else output.writeDouble(value)
    override fun encodeChar(value: Char): Unit = if (useLittleEndian) output.writeShortLittleEndian(value.toShort()) else output.writeShort(value.toShort())
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int): Unit = TODO()
    override fun encodeString(value: String): Unit {
        val bytes = value.toHexByteArray()
        encodeVarInt(bytes.size)
        encodeByteArray(bytes)
    }
    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        encodeVarInt(collectionSize)
        return super.beginCollection(descriptor, collectionSize)
    }
    override fun encodeValue(value: Any) {
        "IOEncoder encodeValue: $value".info()
    }
    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        "IOEncoder encodeElement: $descriptor $index".info()
        return super.encodeElement(descriptor, index)
    }
}

class IOBuilder(io: IO) {
    var byteOrder: ByteOrder = io.configuration.byteOrder
    var serializersModule: SerializersModule = io.serializersModule
    fun build(): IOConfiguration {
        return IOConfiguration(
            byteOrder
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
