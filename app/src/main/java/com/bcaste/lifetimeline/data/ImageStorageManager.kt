package com.bcaste.lifetimeline.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val imagesDir = File(context.filesDir, "event_images").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Saves an image from a URI and returns only the filename.
     * Storing relative paths (filenames) makes the database portable across reinstalls.
     */
    suspend fun saveImage(uriString: String): String = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)
        
        // If it's already an internal file name (not a full path), don't re-process
        if (!uriString.startsWith("/") && !uriString.contains(":")) {
            return@withContext uriString
        }

        // Check if it's an absolute path from our own app (legacy or currently being added)
        if (uriString.startsWith(context.filesDir.absolutePath)) {
            return@withContext File(uriString).name
        }

        val inputStream = try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            null
        }
        val originalBitmap = inputStream?.let { BitmapFactory.decodeStream(it) }
        inputStream?.close()

        if (originalBitmap == null) return@withContext uriString

        // Resize if too large (Max width/height 1600px)
        val resizedBitmap = resizeBitmap(originalBitmap, 1600)
        
        // Compress until size < 400KB starting at 80% quality
        var quality = 80
        val byteArrayOutputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        
        while (byteArrayOutputStream.size() > 400 * 1024 && quality > 10) {
            byteArrayOutputStream.reset()
            quality -= 5
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        }

        val fileName = "IMG_${UUID.randomUUID()}.jpg"
        val file = File(imagesDir, fileName)
        
        FileOutputStream(file).use { out ->
            out.write(byteArrayOutputStream.toByteArray())
        }

        fileName // Return only the filename
    }

    /**
     * Resolves a stored URI to a valid current absolute path.
     * Handles legacy absolute paths by attempting to find the filename in the new app directory.
     */
    fun resolveUri(storedUri: String): String {
        // If it's an external URI or already points to a valid file, return it
        if (storedUri.contains("://")) return storedUri
        
        val file = if (storedUri.startsWith("/")) {
            // Legacy absolute path
            val legacyFile = File(storedUri)
            if (legacyFile.exists()) return storedUri
            
            // If it doesn't exist, it might be from a different install/user.
            // Try to see if the filename exists in our current dir.
            File(imagesDir, legacyFile.name)
        } else {
            // New relative path (filename only)
            File(imagesDir, storedUri)
        }
        
        return if (file.exists()) file.absolutePath else storedUri
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) return bitmap

        val aspectRatio: Float = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxSize
            newHeight = (maxSize / aspectRatio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * aspectRatio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    fun deleteImage(pathOrName: String) {
        val file = if (pathOrName.startsWith("/")) File(pathOrName) else File(imagesDir, pathOrName)
        if (file.exists()) {
            file.delete()
        }
    }
}
