package com.raj.fix

import android.content.Context
import android.content.SharedPreferences

/**
 * Local-only settings. The Gemini key is stored ONLY in this phone's
 * private SharedPreferences — it is never sent anywhere except directly to
 * Google's Gemini API from this phone, and never hardcoded in the app.
 * There is deliberately NO cloud sync and NO key in any file in the repo.
 */
object Prefs {
    private const val FILE = "fix_prefs"
    private const val KEY_GEMINI = "gemini_api_key"
    private const val KEY_FAVOURITES = "favourite_guide_ids"
    private const val KEY_FAV_TITLES = "favourite_guide_titles" // entries "guideId|title"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getGeminiKey(context: Context): String =
        prefs(context).getString(KEY_GEMINI, "") ?: ""

    fun hasGeminiKey(context: Context): Boolean = getGeminiKey(context).isNotBlank()

    fun setGeminiKey(context: Context, key: String) {
        prefs(context).edit().putString(KEY_GEMINI, key.trim()).apply()
    }

    fun clearGeminiKey(context: Context) {
        prefs(context).edit().remove(KEY_GEMINI).apply()
    }

    fun getFavourites(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_FAVOURITES, emptySet()) ?: emptySet()

    fun isFavourite(context: Context, guideId: Int): Boolean =
        getFavourites(context).contains(guideId.toString())

    fun toggleFavourite(context: Context, guideId: Int): Boolean {
        val set = getFavourites(context).toMutableSet()
        val nowFav = if (set.contains(guideId.toString())) {
            set.remove(guideId.toString()); false
        } else {
            set.add(guideId.toString()); true
        }
        prefs(context).edit().putStringSet(KEY_FAVOURITES, set).apply()
        return nowFav
    }

    /** Remember a favourite's title so Saved can show it without internet. */
    fun setFavouriteTitle(context: Context, guideId: Int, title: String) {
        val set = prefs(context).getStringSet(KEY_FAV_TITLES, emptySet())?.toMutableSet()
            ?: mutableSetOf()
        set.removeAll { it.startsWith("$guideId|") }
        set.add("$guideId|$title")
        prefs(context).edit().putStringSet(KEY_FAV_TITLES, set).apply()
    }

    fun getFavouriteTitles(context: Context): Map<Int, String> {
        val out = mutableMapOf<Int, String>()
        val set = prefs(context).getStringSet(KEY_FAV_TITLES, emptySet()) ?: emptySet()
        for (entry in set) {
            val parts = entry.split("|", limit = 2)
            if (parts.size == 2) parts[0].toIntOrNull()?.let { out[it] = parts[1] }
        }
        return out
    }
}
