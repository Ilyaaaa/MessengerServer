package Utils

import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object Md5Creator {
    fun createMd5(st: String): String {
        var digest = ByteArray(0)

        try {
            val messageDigest = MessageDigest.getInstance("MD5")
            messageDigest!!.reset()
            messageDigest.update(st.toByteArray())
            digest = messageDigest.digest()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }

        val bigInt = BigInteger(1, digest)
        var md5Hex = bigInt.toString(16)

        while (md5Hex.length < 32) {
            md5Hex = "0" + md5Hex
        }

        return md5Hex
    }
}