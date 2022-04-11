package graphene.serializers

import graphene.extension.toUnicodeByteArray
import graphene.protocol.SerializeIndex
import kotlinx.io.core.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
class IOEncoder(
    private val output: Output,
    val io: IO,
) : AbstractEncoder() {
    override val serializersModule: SerializersModule = io.serializersModule
    private val encodeIndex = io.configuration.isIndexed
    private val byteOrder = io.configuration.byteOrder
    private val outputTemp by lazy { BytePacketBuilder() }

    private val outputCurrent = if (encodeIndex) outputTemp else output

    private var presenceCount = 0
    private var index = 0

    fun encodePresence(value: Boolean) {
        if (encodeIndex) {
            if (value) {
                encodeVarInt(index++)
                presenceCount++
            } else {
                index++
            }
        } else {
            encodeBoolean(value)
        }
    }

    fun encodeByteArray(value: ByteArray): Unit = outputCurrent.writeFully(value)
    fun encodeVarInt(value: Int) = outputCurrent.writeVarInt(value)
    fun encodeVarLong(value: Long) = outputCurrent.writeVarLong(value)

    override fun encodeNotNullMark() = encodePresence(true)
    override fun encodeNull() = encodePresence(false)

    override fun encodeBoolean(value: Boolean) = outputCurrent.writeByte(if (value) 0x01 else 0x00)
    override fun encodeByte(value: Byte) = outputCurrent.writeByte(value)
    override fun encodeShort(value: Short) = outputCurrent.writeShort(value, byteOrder)
    override fun encodeInt(value: Int) = outputCurrent.writeInt(value, byteOrder)
    override fun encodeLong(value: Long) = outputCurrent.writeLong(value, byteOrder)
    override fun encodeFloat(value: Float) = outputCurrent.writeFloat(value, byteOrder)
    override fun encodeDouble(value: Double) = outputCurrent.writeDouble(value, byteOrder)
    override fun encodeChar(value: Char) = outputCurrent.writeShort(value.toShort(), byteOrder)
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
        val conf = IO(io) { isIndexed = descriptor.annotations.any { it is SerializeIndex } }
        return IOEncoder(outputCurrent, conf)
    }
    override fun endStructure(descriptor: SerialDescriptor) {
        if (encodeIndex) {
            output.writeVarInt(presenceCount)
            output.writeFully(outputTemp.build().readBytes())
            outputTemp.close()
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
    private fun Output.writeVarLong(value: Long) {
        var curr = value
        while (curr and -0x80L != 0L) {
            writeByte((curr and 0x7F or 0x80).toByte())
            curr = curr ushr 7
        }
        writeByte((curr and 0x7F).toByte())
    }
}