package com.ntdpz.pie.feature.history

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.ntdpz.pie.core.domain.media.DiaryMedia
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads headers only; never decodes a full photo to discover its layout size. */
internal suspend fun readMemoryAspectRatio(context: Context, item: DiaryMedia): Float? = withContext(Dispatchers.IO) {
    try {
        val uri = Uri.parse(item.uri)
        if (item.isVideo) {
            val reader = MediaMetadataRetriever()
            try {
                reader.setDataSource(context, uri)
                memoryAspectRatio(
                    reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0,
                    reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0,
                    reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0,
                )
            } finally { reader.release() }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val orientation = try {
                context.contentResolver.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                }
            } catch (_: java.io.IOException) { null }
            val rotated = orientation in setOf(ExifInterface.ORIENTATION_TRANSPOSE, ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSVERSE, ExifInterface.ORIENTATION_ROTATE_270)
            memoryAspectRatio(bounds.outWidth, bounds.outHeight, if (rotated) 90 else 0)
        }
    } catch (cancelled: CancellationException) { throw cancelled }
    catch (_: Exception) { null }
}

internal fun memoryAspectRatio(width: Int, height: Int, rotation: Int): Float? {
    if (width <= 0 || height <= 0) return null
    return if (rotation % 180 != 0) height.toFloat() / width else width.toFloat() / height
}
