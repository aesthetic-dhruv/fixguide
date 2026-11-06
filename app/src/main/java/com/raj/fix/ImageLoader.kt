package com.raj.fix

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tiny image loader — no extra library, so the build stays small and reliable.
 * Loads iFixit guide-images CDN URLs (standard/medium/thumbnail) on a
 * background thread, with an in-memory cache. A failed image simply leaves
 * the placeholder; it never crashes the list.
 */
object ImageLoader {
    private val cache: LruCache<String, Bitmap> by lazy {
        val maxKb = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
        object : LruCache<String, Bitmap>(maxKb) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
        }
    }

    fun load(url: String, view: ImageView) {
        if (url.isEmpty()) {
            view.setImageResource(android.R.drawable.ic_menu_gallery)
            return
        }
        view.tag = url
        val cached = cache.get(url)
        if (cached != null) {
            view.setImageBitmap(cached)
            return
        }
        view.setImageResource(android.R.drawable.ic_menu_gallery)
        Thread {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("User-Agent", "Fix/1.0 (Android)")
                val bitmap = conn.inputStream.use { BitmapFactory.decodeStream(it) }
                conn.disconnect()
                if (bitmap != null) {
                    cache.put(url, bitmap)
                    view.post {
                        if (view.tag == url) view.setImageBitmap(bitmap)
                    }
                }
            } catch (_: Exception) {
                // Leave placeholder; image failure is not fatal.
            }
        }.start()
    }
}
