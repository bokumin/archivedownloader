package net.bokumin45.archivedownloader

data class ArchiveItem(
    val title: String,
    val link: String,
    val category: String,
    val identifier: String,
    val thumbnailUrl: String? = null
)

data class ArchiveFile(
    val name: String,
    val size: Long,
    val format: String
)