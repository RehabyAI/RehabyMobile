package com.rehaby.app.util

/**
 * YouTube provides static thumbnails without an API key, e.g.
 * `https://img.youtube.com/vi/VIDEO_ID/hqdefault.jpg`
 */
object YoutubeThumbnails {

    fun hqDefaultFromWatchUrl(watchUrl: String): String? {
        val id = extractVideoId(watchUrl) ?: return null
        return "https://img.youtube.com/vi/$id/hqdefault.jpg"
    }

    fun extractVideoId(url: String): String? {
        val trimmed = url.trim()
        Regex("[?&]v=([^&]+)").find(trimmed)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }?.let { return it }
        Regex("youtu\\.be/([^?&/]+)").find(trimmed)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }?.let { return it }
        return null
    }
}
