package graphene.serializers

import graphene.extension.info
import graphene.extension.toUnicodeByteArray
import graphene.protocol.SerializeIndex
import kotlinx.io.core.*
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

class IOConfiguration(
    val byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN,
    val encodeIndex: Boolean = false,
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
    private val useLittleEndian = io.configuration.byteOrder == ByteOrder.LITTLE_ENDIAN
    private val encodeIndex = io.configuration.encodeIndex
    private val outputIndexed by lazy { BytePacketBuilder() }
    private val currentOutput = if (encodeIndex) outputIndexed else output

    private var index = 0
    private var presenceCount = 0
    fun encodePresence(value: Boolean) {
        if (encodeIndex) {
            if (value) {
                encodeVarInt(index++)
                presenceCount++
            } else {
                index++
            }
        } else {
            currentOutput.writeByte(if (value) 0x01 else 0x00)
        }
    }

    fun encodeByteArray(value: ByteArray): Unit = currentOutput.writeFully(value)
    fun encodeVarInt(value: Int) = currentOutput.writeVarInt(value)
    fun encodeVarInt(value: Long) = currentOutput.writeVarInt(value)

    override fun encodeBoolean(value: Boolean) = currentOutput.writeByte(if (value) 0x01 else 0x00)
    override fun encodeByte(value: Byte) = currentOutput.writeByte(value)
    override fun encodeShort(value: Short) = if (useLittleEndian) currentOutput.writeShortLittleEndian(value) else currentOutput.writeShort(value)
    override fun encodeInt(value: Int) = if (useLittleEndian) currentOutput.writeIntLittleEndian(value) else currentOutput.writeInt(value)
    override fun encodeLong(value: Long) = if (useLittleEndian) currentOutput.writeLongLittleEndian(value) else currentOutput.writeLong(value)
    override fun encodeFloat(value: Float) = if (useLittleEndian) currentOutput.writeFloatLittleEndian(value) else currentOutput.writeFloat(value)
    override fun encodeDouble(value: Double) = if (useLittleEndian) currentOutput.writeDoubleLittleEndian(value) else currentOutput.writeDouble(value)
    override fun encodeChar(value: Char) = if (useLittleEndian) currentOutput.writeShortLittleEndian(value.toShort()) else currentOutput.writeShort(value.toShort())
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = TODO()
    override fun encodeString(value: String) {
        val bytes = value.toUnicodeByteArray()
        encodeVarInt(bytes.size)
        encodeByteArray(bytes)
    }
    override fun encodeValue(value: Any) = TODO()
    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        encodeVarInt(collectionSize)
        return super.beginCollection(descriptor, collectionSize)
    }
    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val conf = IO(io) { encodeIndex = descriptor.annotations.any { it is SerializeIndex } }
        return IOEncoder(currentOutput, conf)
    }
    override fun endStructure(descriptor: SerialDescriptor) {
        if (encodeIndex) {
            output.writeVarInt(presenceCount)
            output.writeFully(outputIndexed.build().readBytes())
            outputIndexed.close()
        } else {
            super.endStructure(descriptor)
        }
    }

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
}

class IOBuilder(io: IO) {
    var byteOrder: ByteOrder = io.configuration.byteOrder
    var encodeIndex: Boolean = io.configuration.encodeIndex
    var serializersModule: SerializersModule = io.serializersModule
    fun build(): IOConfiguration {
        return IOConfiguration(
            byteOrder,
            encodeIndex
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
