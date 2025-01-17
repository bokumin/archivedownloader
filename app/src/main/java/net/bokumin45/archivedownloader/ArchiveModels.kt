package net.bokumin45.archivedownloader

data class ArchiveCategory(
    val name: String,
    val items: List<ArchiveItem>,
    val subCategories: List<ArchiveCategory> = emptyList(),
    val parent: String? = null
) {
    val displayName: String get() = name.substringAfterLast('/')
    val totalItemCount: Int get() = items.size + subCategories.sumOf { it.totalItemCount }
}

data class ArchiveItem(
    val title: String,
    val link: String,
    val category: String,
    val identifier: String
) {
    val thumbnailUrl: String
        get() = "${ArchiveService.BASE_URL}services/img/$identifier"

    val mainCategory: String
        get() = category.substringBefore('/')

    val subCategory: String
        get() = category.substringAfter('/', "")
}

data class ArchiveFile(
    val name: String,
    val size: Long,
    val format: String
)
data class MetadataResponse(
    val files: List<ArchiveFile> = emptyList(),
    val metadata: ArchiveMetadata = ArchiveMetadata(),
    val server: String = "",
    val dir: String = ""
)

data class ArchiveMetadata(
    val identifier: String = "",
    val title: String = "",
    val mediatype: String = "",
    val description: String = "",
    val creator: String = "",
    val date: String = "",
    val year: String = "",
    val publicdate: String = "",
    val addeddate: String = "",
    val uploader: String = ""
)