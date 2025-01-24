package net.bokumin45.archivedownloader

import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

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

    @GET("advancedsearch.php")
    suspend fun getHotItems(
        @Query("q") query: String,
        @Query("fl[]") fields: List<String>,
        @Query("sort[]") sorts: List<String>,
        @Query("output") output: String = "json",
        @Query("rows") rows: Int = 50,
        @Query("page") page: Int = 1
    ): SearchResponse

    @GET("metadata/{identifier}")
    suspend fun getMetadata(@Path("identifier") identifier: String): MetadataResponse

    companion object {
        const val BASE_URL = "https://archive.org/"
        fun getThumbnailUrl(identifier: String): String {
            return "${BASE_URL}services/img/$identifier"
        }
    }
}