package graphene.serializers

import graphene.extension.toUnicodeString
import graphene.protocol.SerializeIndex
import kotlinx.io.core.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule
import kotlin.experimental.and

@OptIn(ExperimentalSerializationApi::class)
class IODecoder(
    private val input: Input,
    val io: IO,
) : AbstractDecoder() {
    override val serializersModule: SerializersModule = io.serializersModule

    private val byteOrder = io.configuration.byteOrder
    private val isIndexed = io.configuration.isIndexed
    private val currentInput: Input = input

    private var presenceCount = if (isIndexed) decodeVarInt() else -1
    private var indexNotNull = if (presenceCount-- > 0) decodeVarInt() else -1
    private var index = 0

    fun decodePresence(): Boolean {
        return if (isIndexed) {
            if (presenceCount < 0) {
                false
            } else {
                if (index++ == indexNotNull) {
                    indexNotNull = if (presenceCount-- > 0) decodeVarInt() else -1
                    true
                } else {
                    false
                }
            }
        } else {
            decodeBoolean()
        }
    }

    fun decodeByteArray(size: Int): ByteArray = ByteArray(size).also { currentInput.readFully(it, size) }
    fun decodeVarInt(): Int = currentInput.readVarInt()
    fun decodeVarLong(): Long = currentInput.readVarLong()

    override fun decodeNotNullMark(): Boolean = decodePresence()
    override fun decodeNull(): Nothing? = null

    override fun decodeBoolean(): Boolean = currentInput.readByte() != 0x00.toByte()
    override fun decodeByte(): Byte = currentInput.readByte()
    override fun decodeShort(): Short = currentInput.readShort(byteOrder)
    override fun decodeInt(): Int = currentInput.readInt(byteOrder)
    override fun decodeLong(): Long = currentInput.readLong(byteOrder)
    override fun decodeFloat(): Float = currentInput.readFloat(byteOrder)
    override fun decodeDouble(): Double = currentInput.readDouble(byteOrder)
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return decodeVarInt()
    }

    override fun decodeChar(): Char = currentInput.readShort(byteOrder).toChar()
    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = TODO()
    override fun decodeString(): String {
        val size = decodeVarInt()
        return decodeByteArray(size).toUnicodeString()
    }
    override fun decodeValue(): Any = TODO()


    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        return decodeVarInt()
    }
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        val conf = IO(io) { isIndexed = descriptor.annotations.any { it is SerializeIndex } }
        return IODecoder(currentInput, conf)
    }
    override fun endStructure(descriptor: SerialDescriptor) {
        super.endStructure(descriptor)
    }
    private fun Input.readVarInt(): Int {
        var value = 0
        var i = 0
        var b: Int
        while (readByte().also { b = it.toInt() } and 0x80.toByte() != 0.toByte()) {
            value = value or (b and 0x7F shl i)
            i += 7
            require(i <= 35) { "Variable too long" }
        }
        return value or (b shl i)
    }
    private fun Input.readVarLong(): Long {
        var value = 0L
        var i = 0
        var b: Long
        while (readByte().also { b = it.toLong() } and 0x80L.toByte() != 0.toByte()) {
            value = value or (b and 0x7F shl i)
            i += 7
            require(i <= 63) { "Variable too long" }
        }
        return value or (b shl i)
    }

}