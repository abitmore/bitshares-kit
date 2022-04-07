package graphene.extension

import org.slf4j.LoggerFactory

fun <T: Any?> T.info() = apply {
    kotlin.runCatching {
        LoggerFactory.getLogger("BitSharesKit Log").info(toString())
    }
}