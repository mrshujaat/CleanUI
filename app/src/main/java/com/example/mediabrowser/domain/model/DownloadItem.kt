package com.example.mediabrowser.domain.model

data class DownloadItem(
    val id: Long = 0,
    val postId: Long,
    val fileUrl: String,
    val fileName: String,
    val localUri: String?,
    val mediaType: MediaType,
    val status: DownloadStatus,
    val progress: Int = 0,
    val createdAt: Long,
    val errorMessage: String? = null,
    val fileSizeBytes: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val score: Int = 0,
    val tags: List<String> = emptyList()
)

enum class DownloadStatus {
    QUEUED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}