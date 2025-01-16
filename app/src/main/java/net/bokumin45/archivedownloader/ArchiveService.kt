package net.bokumin45.archivedownloader

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Url

interface ArchiveService {
    @GET("services/collection-rss.php")
    @Headers("Accept: application/xml")
    suspend fun getLatestUploads(): String

    @GET("metadata/{identifier}")
    suspend fun getMetadata(@Path("identifier") identifier: String): MetadataResponse

    @GET
    suspend fun downloadFile(@Url fileUrl: String): retrofit2.Response<okhttp3.ResponseBody>

    companion object {
        const val BASE_URL = "https://archive.org/"
        fun getThumbnailUrl(identifier: String): String {
            return "${BASE_URL}services/img/$identifier"
        }
    }
}