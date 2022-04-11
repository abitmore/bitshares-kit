package graphene.serializers

import graphene.chain.AbstractObject
import graphene.protocol.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

internal val ID_TYPE_DESCRIPTOR = PrimitiveSerialDescriptor("ObjectIdType", PrimitiveKind.STRING)

class ObjectSerializer<T: AbstractObject> : KSerializer<T> {
    override val descriptor: SerialDescriptor = ID_TYPE_DESCRIPTOR
    override fun deserialize(decoder: Decoder): T {
        decoder as JsonDecoder // TODO: 2022/4/5
        val element = decoder.decodeJsonElement().jsonObject
        @OptIn(InternalSerializationApi::class)
        val serializer = element["id"]!!.jsonPrimitive.content.toGrapheneType().toObjectClass().serializer()
        return decoder.json.decodeFromJsonElement(serializer, element) as T
    }
    @OptIn(InternalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: T) {
        (value::class.serializer() as KSerializer<T>).serialize(encoder, value)
    }
}

class ObjectSerializer1 : JsonContentPolymorphicSerializer<AbstractObject>(AbstractObject::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out AbstractObject> {
        @OptIn(InternalSerializationApi::class)
        return element.jsonObject["id"]!!.jsonPrimitive.content.toGrapheneType().toObjectClass().serializer()
    }
}

class ObjectIdSerializer<T: ObjectId>() : KSerializer<T> {
    override val descriptor: SerialDescriptor = ID_TYPE_DESCRIPTOR
    override fun deserialize(decoder: Decoder): T =
        when (decoder) {
            is JsonDecoder -> decoder.decodeString().toGrapheneObjectId()
            is IODecoder -> TODO()
            else -> TODO()
        }
    override fun serialize(encoder: Encoder, value: T) {
        when (encoder) {
            is JsonEncoder -> encoder.encodeString(value.standardId)
            is IOEncoder -> encoder.encodeVarLong(value.instance.toLong())
            else -> TODO()
        }
    }
}

object ObjectIdDefaultSerializer : KSerializer<ObjectId> {
    override val descriptor: SerialDescriptor = ID_TYPE_DESCRIPTOR
    override fun deserialize(decoder: Decoder): ObjectId =
        decoder.decodeString().toGrapheneObjectId()
    override fun serialize(encoder: Encoder, value: ObjectId) {
        when (encoder) {
            is JsonEncoder -> encoder.encodeString(value.standardId)
            is IOEncoder -> encoder.encodeLong(value.number.toLong())
            else -> TODO()
        }
    }
}

