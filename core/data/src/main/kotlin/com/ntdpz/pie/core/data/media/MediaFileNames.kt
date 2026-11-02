package com.ntdpz.pie.core.data.media

import android.webkit.MimeTypeMap
import java.util.UUID

internal object MediaFileNames {
    fun forId(id: String, mime: String): String {
        require(UUID.fromString(id).toString() == id)
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            ?.takeIf { it.matches(Regex("[a-zA-Z0-9]+")) }
            ?: if (mime.startsWith("video/")) "mp4" else "bin"
        return "$id.$extension"
    }
}
