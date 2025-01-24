package net.bokumin45.archivedownloader

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

class ArchiveRepository(private val archiveService: ArchiveService) {
    suspend fun getLatestUploads(): List<ArchiveItem> {
        val xmlString = archiveService.getLatestUploads()
        Log.d("ArchiveRepository", "Raw XML length: ${xmlString.length}")
        return parseRssFeed(xmlString)
    }

    private fun parseRssFeed(xmlString: String): List<ArchiveItem> {
        val items = mutableListOf<ArchiveItem>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false

        val parser = factory.newPullParser()
        parser.setInput(xmlString.reader())

        var currentTitle = ""
        var currentLink = ""
        var currentCategory = ""
        var insideItem = false
        var itemCount = 0

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "item" -> {
                            insideItem = true
                            itemCount++
                            Log.d("ArchiveRepository", "Processing item #$itemCount")
                        }

                        "title" -> if (insideItem) {
                            currentTitle = parser.nextText().trim()
                            Log.d("ArchiveRepository", "Title: $currentTitle")
                        }

                        "link" -> if (insideItem) {
                            currentLink = parser.nextText().trim()
                            Log.d("ArchiveRepository", "Link: $currentLink")
                        }

                        "category" -> if (insideItem) {
                            currentCategory = parser.nextText().trim()
                            Log.d("ArchiveRepository", "Category: $currentCategory")
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "item") {
                        if (currentTitle.isNotEmpty() && currentLink.isNotEmpty() && currentCategory.isNotEmpty()) {
                            val identifier = currentLink.substringAfterLast("/")
                            items.add(
                                ArchiveItem(
                                    title = currentTitle,
                                    link = currentLink,
                                    category = currentCategory,
                                    identifier = identifier
                                )
                            )
                            Log.d("ArchiveRepository", "Added item: $identifier")
                        } else {
                            Log.w(
                                "ArchiveRepository",
                                "Skipped incomplete item - Title: $currentTitle, Link: $currentLink, Category: $currentCategory"
                            )
                        }
                        insideItem = false
                        currentTitle = ""
                        currentLink = ""
                        currentCategory = ""
                    }
                }
            }
            eventType = parser.next()
        }

        Log.d("ArchiveRepository", "Total items found: ${items.size}")
        return items
    }

    suspend fun getCategoryItems(category: String, page: Int): List<ArchiveItem> {
        return try {
            val queryString = when {
                category == "latest" -> "*"
                category.contains("/") -> {
                    val (main, sub) = category.split("/")
                    "collection:$main AND mediatype:$sub"
                }
                else -> "collection:$category"
            }

            val response = archiveService.searchItems(
                query = queryString,
                page = page,
                rows = 50,
                fields = listOf("identifier", "title", "mediatype", "collection", "subject")
            )
            response.response.docs.map { it.toArchiveItem() }
        } catch (e: Exception) {
            throw Exception("Failed to fetch category items: ${e.message}")
        }
    }

    suspend fun searchItems(query: String, page: Int): SearchResponse {
        return archiveService.searchItems(
            query = query,
            fields = listOf("identifier", "title", "mediatype"),
            rows = 50,
            page = page
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getHotItems(period: HotPeriod): List<ArchiveItem> {
        val now = java.time.LocalDate.now()
        val dateQuery = when (period) {
            HotPeriod.DAY -> {
                val dayAgo = now.minusDays(1)
                "addeddate:[${dayAgo} TO ${now}]"
            }
            HotPeriod.WEEK -> {
                val weekAgo = now.minusWeeks(1)
                "addeddate:[${weekAgo} TO ${now}]"
            }
            HotPeriod.MONTH -> {
                val monthAgo = now.minusMonths(1)
                "addeddate:[${monthAgo} TO ${now}]"
            }
            HotPeriod.YEAR -> {
                val yearAgo = now.minusYears(1)
                "addeddate:[${yearAgo} TO ${now}]"
            }
        }

        val sorts = when (period) {
            HotPeriod.DAY -> listOf("-downloads", "-addeddate")
            HotPeriod.WEEK -> listOf("-downloads", "-addeddate")
            HotPeriod.MONTH -> listOf("-addeddate", "-downloads")
            HotPeriod.YEAR -> listOf("-downloads", "-addeddate")
        }

        return try {
            Log.d("ArchiveRepository", "Date Query: $dateQuery")
            Log.d("ArchiveRepository", "Sorts: $sorts")

            val response = archiveService.getHotItems(
                query = dateQuery,
                fields = listOf("identifier", "title", "mediatype", "downloads", "addeddate"),
                sorts = sorts,
                rows = 50
            )
            response.response.docs.map { it.toArchiveItem() }
        } catch (e: Exception) {
            Log.e("ArchiveRepository", "Error fetching hot items", e)
            throw Exception("Failed to fetch hot items: ${e.message}")
        }
    }
}