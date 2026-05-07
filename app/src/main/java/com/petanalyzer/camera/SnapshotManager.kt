package com.petanalyzer.camera

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class SnapshotManager(private val context: Context) {

    private var lastSnapshotTime = 0L
    private val cooldownMs = 3000L

    private val snapshotsDir: File
        get() = File(context.filesDir, "snapshots").also { it.mkdirs() }

    fun shouldCapture(): Boolean {
        val now = System.currentTimeMillis()
        return now - lastSnapshotTime >= cooldownMs
    }

    fun save(bitmap: Bitmap, behavior: String): String? {
        if (!shouldCapture()) return null

        return try {
            val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"))
            val date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            val file = File(snapshotsDir, "pet_${date}_$now.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            lastSnapshotTime = System.currentTimeMillis()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun cleanup(daysToKeep: Int = 30) {
        val cutoff = System.currentTimeMillis() - daysToKeep.toLong() * 24 * 3600_000
        snapshotsDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) file.delete()
        }
    }
}
