package graphene.serializers

import kotlinx.io.core.ByteOrder
import kotlinx.serialization.json.Json

var GRAPHENE_JSON_PLATFORM_SERIALIZER = Json {
    ignoreUnknownKeys = false
    encodeDefaults = false
}

var GRAPHENE_IO_PLATFORM_SERIALIZER = IO {
    byteOrder = ByteOrder.LITTLE_ENDIAN
}

