package net.bokumin45.archivedownloader

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

interface ArchiveService {
    @GET("services/collection-rss.php")
    @Headers("Accept: application/xml")
    suspend fun getLatestUploads(): String

    @GET("advancedsearch.php")
    suspend fun searchItems(
        @Query("q") query: String,
        @Query("output") output: String = "json",
        @Query("fl[]") fields: List<String> = listOf("identifier", "title", "mediatype"),
        @Query("rows") rows: Int = 50,
        @Query("page") page: Int = 1
    ): SearchResponse

    suspend fun getCategoryItems(
        @Path("category") category: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): List<ArchiveItem>

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