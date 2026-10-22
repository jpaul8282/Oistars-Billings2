package com.example.data.local

import androidx.room.TypeConverter
import org.json.JSONArray

class StringListConverter {
    @TypeConverter
    fun fromString(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val jsonArray = JSONArray(value)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (_: Exception) {
            value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }

    @TypeConverter
    fun fromList(list: List<String>?): String {
        if (list.isNullOrEmpty()) return "[]"
        val jsonArray = JSONArray()
        for (item in list) {
            jsonArray.put(item)
        }
        return jsonArray.toString()
    }
}
