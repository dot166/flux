package io.github.dot166.flux

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.github.dot166.jlib.app.DefaultSharedPrefsManager

object FeedUtils {
    private var cachedMap: MutableMap<String, MutableList<Int>> = mutableMapOf()

    fun getNotified(context: Context, url: String): MutableList<Int> {
        val map = getCachedFeedInfo(context)
        return map[url] ?: mutableListOf()
    }

    private fun getCachedFeedInfo(context: Context): MutableMap<String, MutableList<Int>> {
        if (cachedMap.isEmpty()) {
            val gson = GsonBuilder()
                .serializeNulls()
                .create()
            cachedMap = gson.fromJson(DefaultSharedPrefsManager.getSharedPreferencesStorage(context).getString("notified")?:"", object : TypeToken<MutableMap<String, MutableList<Int>>>() {}) ?: cachedMap
        }
        return cachedMap
    }

    fun setItems(notified: MutableList<Int>, url: String, context: Context) {
        val map = getCachedFeedInfo(context)
        map[url] = notified
        val gson = GsonBuilder()
            .serializeNulls()
            .create()
        val json = gson.toJson(map)
        DefaultSharedPrefsManager.getSharedPreferencesStorage(context).setString("notified", json)
    }
}