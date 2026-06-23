package com.example.mediabrowser.download

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.mediabrowser.domain.model.MediaType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    fun enqueueDownload(
        downloadId: Long,
        fileUrl: String,
        fileName: String,
        mediaType: MediaType,
        wifiOnly: Boolean
    ) {
        val mimeType = when (mediaType) {
            MediaType.IMAGE -> "image/*"
            MediaType.GIF -> "image/gif"
            MediaType.VIDEO -> "video/*"
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putLong(DownloadWorker.KEY_DOWNLOAD_ID, downloadId)
            .putString(DownloadWorker.KEY_FILE_URL, fileUrl)
            .putString(DownloadWorker.KEY_FILE_NAME, fileName)
            .putString(DownloadWorker.KEY_MIME_TYPE, mimeType)
            .build()

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag("download_$downloadId")
            .build()

        workManager.enqueue(request)
    }

    fun cancelDownload(downloadId: Long) {
        workManager.cancelAllWorkByTag("download_$downloadId")
    }
}
