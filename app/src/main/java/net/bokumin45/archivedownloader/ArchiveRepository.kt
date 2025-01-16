package net.bokumin45.archivedownloader.repository

import android.util.Log
import net.bokumin45.archivedownloader.ArchiveService
import net.bokumin45.archivedownloader.ArchiveItem
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

class ArchiveRepository(private val service: ArchiveService) {
    suspend fun getLatestUploads(): List<ArchiveItem> {
        val response = service.getLatestUploads()
        Log.d("ArchiveRepository", "Received XML response: ${response.take(100)}...")
        return parseRssFeed(response)
    }

    private fun parseRssFeed(xmlString: String): List<ArchiveItem> {
        val items = mutableListOf<ArchiveItem>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xmlString))

        var currentTitle = ""
        var currentLink = ""
        var currentCategory = ""
        var insideItem = false

        try {
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "item" -> insideItem = true
                            "title" -> {
                                if (insideItem) {
                                    parser.next()
                                    currentTitle = parser.text?.trim() ?: ""
                                }
                            }
                            "link" -> {
                                if (insideItem) {
                                    parser.next()
                                    currentLink = parser.text?.trim() ?: ""
                                }
                            }
                            "category" -> {
                                if (insideItem) {
                                    parser.next()
                                    currentCategory = parser.text?.trim() ?: ""
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "item") {
                            if (currentLink.isNotEmpty()) {
                                val identifier = currentLink.substringAfterLast("/")
                                items.add(
                                    ArchiveItem(
                                        title = currentTitle,
                                        link = currentLink,
                                        category = currentCategory,
                                        identifier = identifier,
                                        thumbnailUrl = ArchiveService.getThumbnailUrl(identifier)
                                    )
                                )
                                Log.d("ArchiveRepository", "Added item: $currentTitle ($currentCategory)")
                            }
                            // Reset values for next item
                            currentTitle = ""
                            currentLink = ""
                            currentCategory = ""
                            insideItem = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e("ArchiveRepository", "Error parsing RSS feed", e)
            throw e
        }

        Log.d("ArchiveRepository", "Successfully parsed ${items.size} items")
        return items
    }
}