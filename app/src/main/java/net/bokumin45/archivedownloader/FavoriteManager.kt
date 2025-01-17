package net.bokumin45.archivedownloader

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FavoriteManager(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun addFavorite(item: ArchiveItem) {
        val favorites = getFavorites().toMutableSet()
        favorites.add(item)
        saveFavorites(favorites)
    }

    fun removeFavorite(item: ArchiveItem) {
        val favorites = getFavorites().toMutableSet()
        favorites.removeIf { it.identifier == item.identifier }
        saveFavorites(favorites)
    }

    fun isFavorite(identifier: String): Boolean {
        return getFavorites().any { it.identifier == identifier }
    }

    fun getFavorites(): Set<ArchiveItem> {
        val json = preferences.getString("favorite_items", null) ?: return emptySet()
        val type = object : TypeToken<Set<ArchiveItem>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptySet()
        }
    }

    private fun saveFavorites(favorites: Set<ArchiveItem>) {
        val json = gson.toJson(favorites)
        preferences.edit().putString("favorite_items", json).apply()
    }
}