package graphene.extension

import org.slf4j.LoggerFactory

fun <T: Any?> T.info() = apply {
    if (this is ByteArray) {
        LoggerFactory.getLogger("BitSharesKit Log").info("================================================== "+toHexString())
    } else {
        kotlin.runCatching {
            LoggerFactory.getLogger("BitSharesKit Log").info("================================================== "+toString())
        }
    }

}