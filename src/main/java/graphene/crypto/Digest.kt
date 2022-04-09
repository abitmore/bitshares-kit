package graphene.crypto

import org.bouncycastle.jcajce.provider.digest.*

fun md5(data: ByteArray): ByteArray = MD5.Digest().digest(data)
fun sha1(data: ByteArray): ByteArray = SHA1.Digest().digest(data)
fun sha256(data: ByteArray): ByteArray = SHA256.Digest().digest(data)
fun sha512(data: ByteArray): ByteArray = SHA512.Digest().digest(data)

fun ripemd160(data: ByteArray): ByteArray = RIPEMD160.Digest().digest(data)
