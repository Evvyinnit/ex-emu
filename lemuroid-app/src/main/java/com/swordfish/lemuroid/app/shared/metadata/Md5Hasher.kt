package com.swordfish.lemuroid.app.shared.metadata

import android.content.Context
import android.net.Uri
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Md5Hasher {
    suspend fun md5(
        context: Context,
        uri: Uri,
    ): String? = withContext(Dispatchers.IO) {
        try {
            val digest = MessageDigest.getInstance("MD5")
            context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            } ?: return@withContext null
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }
}