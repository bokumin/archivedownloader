package net.bokumin45.archivedownloader.repository

import android.util.Log
import net.bokumin45.archivedownloader.ArchiveItem
import net.bokumin45.archivedownloader.ArchiveService
import net.bokumin45.archivedownloader.SearchResponse
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
                            items.add(ArchiveItem(
                                title = currentTitle,
                                link = currentLink,
                                category = currentCategory,
                                identifier = identifier
                            ))
                            Log.d("ArchiveRepository", "Added item: $identifier")
                        } else {
                            Log.w("ArchiveRepository", "Skipped incomplete item - Title: $currentTitle, Link: $currentLink, Category: $currentCategory")
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

    suspend fun searchItems(query: String, page: Int): SearchResponse {
        return archiveService.searchItems(
            query = query,
            fields = listOf("identifier", "title", "mediatype"),
            rows = 50,
            page = page
        )
    }
}