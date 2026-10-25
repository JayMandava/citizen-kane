package dev.jeyanth.citizenkane

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.HttpURLConnection
import java.net.URL
import java.io.ByteArrayOutputStream

private object Images {
    val permits = Semaphore(4)
    val cache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    suspend fun load(url: String): Bitmap? = withContext(Dispatchers.IO) {
        cache.get(url)?.let { return@withContext it }
        permits.withPermit {
            runCatching {
                val connection = URL(url).openConnection() as HttpURLConnection
                val bytes = try {
                    connection.connectTimeout = 10_000; connection.readTimeout = 10_000
                    connection.inputStream.use { input ->
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            require(output.size() + count <= 8 * 1024 * 1024)
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                } finally { connection.disconnect() }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 1
                    while (bounds.outWidth / inSampleSize > 1024 || bounds.outHeight / inSampleSize > 1024) inSampleSize *= 2
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.also { cache.put(url, it) }
            }.getOrNull()
        }
    }
}

@Composable fun ArticleImage(url: String?, modifier: Modifier = Modifier) {
    val bitmap by produceState<Bitmap?>(null, url) { value = null; if (url != null) value = Images.load(url) }
    Box(modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = .15f))) {
        bitmap?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop) }
    }
}
