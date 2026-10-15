package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.LruCache
import com.example.viewmodel.toBitmapSafe

/**
 * High-performance in-memory cache for app icon Bitmaps to prevent main thread jank / lag
 * during SpringBoard rendering, App Library scrolling, and launcher interactions.
 */
object AppIconCache {
    // Cache up to 100 app icon Bitmaps in memory
    private val memoryCache = object : LruCache<String, Bitmap>(100) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount / 1024 // KB
        }
    }

    fun getIcon(context: Context, packageName: String): Bitmap? {
        val cached = memoryCache.get(packageName)
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        return try {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(packageName)
            val bmp = drawable.toBitmapSafe(144)
            if (bmp != null) {
                memoryCache.put(packageName, bmp)
            }
            bmp
        } catch (e: Exception) {
            null
        }
    }

    fun putIcon(packageName: String, bitmap: Bitmap) {
        if (!bitmap.isRecycled) {
            memoryCache.put(packageName, bitmap)
        }
    }

    fun clear() {
        memoryCache.evictAll()
    }
}
