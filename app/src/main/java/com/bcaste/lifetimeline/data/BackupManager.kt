package com.bcaste.lifetimeline.data

import android.content.Context
import com.bcaste.lifetimeline.data.local.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {
    private val dbName = "lifetimeline.db"
    private val imagesDirName = "event_images"

    suspend fun createBackup(outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Ensure any pending writes are flushed
            database.openHelper.writableDatabase

            ZipOutputStream(outputStream).use { zipOut ->
                // 1. Backup Database files
                val dbFile = context.getDatabasePath(dbName)
                if (dbFile.exists()) {
                    addToZip(dbFile, dbName, zipOut)
                }
                
                // Backup journal/wal files if they exist
                val shmFile = File(dbFile.absolutePath + "-shm")
                if (shmFile.exists()) addToZip(shmFile, dbName + "-shm", zipOut)
                
                val walFile = File(dbFile.absolutePath + "-wal")
                if (walFile.exists()) addToZip(walFile, dbName + "-wal", zipOut)

                // 2. Backup Images
                val imagesDir = File(context.filesDir, imagesDirName)
                if (imagesDir.exists() && imagesDir.isDirectory) {
                    imagesDir.listFiles()?.forEach { file ->
                        addToZip(file, "$imagesDirName/${file.name}", zipOut)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(inputStream: InputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Close database to allow file replacement
            database.close()

            val tempDir = File(context.cacheDir, "restore_temp")
            if (tempDir.exists()) tempDir.deleteRecursively()
            tempDir.mkdirs()

            // 2. Extract ZIP to temp
            ZipInputStream(inputStream).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    val file = File(tempDir, entry.name)
                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()
                        FileOutputStream(file).use { zipIn.copyTo(it) }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            // 3. Move files to permanent locations
            val dbFile = context.getDatabasePath(dbName)
            val restoredDb = File(tempDir, dbName)
            if (restoredDb.exists()) {
                // Delete current DB files
                dbFile.delete()
                File(dbFile.absolutePath + "-shm").delete()
                File(dbFile.absolutePath + "-wal").delete()
                
                restoredDb.copyTo(dbFile, overwrite = true)
                
                // Copy shm/wal if present in backup
                File(tempDir, dbName + "-shm").let { if (it.exists()) it.copyTo(File(dbFile.absolutePath + "-shm"), true) }
                File(tempDir, dbName + "-wal").let { if (it.exists()) it.copyTo(File(dbFile.absolutePath + "-wal"), true) }
            }

            val restoredImagesDir = File(tempDir, imagesDirName)
            if (restoredImagesDir.exists()) {
                val appImagesDir = File(context.filesDir, imagesDirName)
                appImagesDir.deleteRecursively()
                restoredImagesDir.copyRecursively(appImagesDir, overwrite = true)
            }

            tempDir.deleteRecursively()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addToZip(file: File, zipPath: String, zipOut: ZipOutputStream) {
        FileInputStream(file).use { fis ->
            val entry = ZipEntry(zipPath)
            zipOut.putNextEntry(entry)
            fis.copyTo(zipOut)
            zipOut.closeEntry()
        }
    }
}
