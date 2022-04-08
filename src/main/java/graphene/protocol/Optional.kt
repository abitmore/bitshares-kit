package graphene.protocol

import graphene.serializers.OptionalSerializer
import kotlinx.serialization.Serializable

@Serializable(with = OptionalSerializer::class)
data class Optional<T>(
    val valueSafe: T? = null
) {
    val value get() = valueSafe!!
    val isPresent get() = valueSafe != null
//            && !(value is Collection<*> && (value as Collection<*>).isEmpty())

    override fun toString(): String {
        return valueSafe.toString()
    }
}

fun <T> Optional<T>.getOrNull() = valueSafe
fun <T> Optional<T>.getOrThrow() = value
fun <T> Optional<T>.getOrElse(fallback: () -> T) = valueSafe ?: fallback()

fun <T> optional(value: T? = null) = Optional(value)