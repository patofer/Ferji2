package com.ferji.inspecciones.utils



import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


object GsonUtils {
    private val gson = Gson()

    fun toJson(obj: Any): String {
        return gson.toJson(obj)
    }

    fun <T> fromJson(json: String, classOfT: Class<T>): T {
        return gson.fromJson(json, classOfT)
    }

    fun listToJson(list: List<String>): String {
        return gson.toJson(list)
    }

    fun jsonToList(json: String): List<String> {
        if (json.isBlank() || json == "null") return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setToJson(set: Set<String>): String {
        return gson.toJson(set.toList())
    }

    fun jsonToSet(json: String): Set<String> {
        return jsonToList(json).toSet()
    }
}