package com.ferji.inspecciones.utils


import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

object GsonUtils {
    val gson = Gson()

    const val TAG = "GsonUtils"

    fun jsonToStringList(jsonString: String?): List<String> { // <-- ESTE ES EL MÉTODO
        if (jsonString.isNullOrBlank()) {
            return emptyList()
        }
        return try {
            // TypeToken es la forma estándar en Gson para manejar tipos genéricos como List<String>
            val listType: Type = object : TypeToken<ArrayList<String>>() {}.type
            gson.fromJson(jsonString, listType) ?: emptyList()
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error de sintaxis al deserializar JSON a List<String>: $jsonString", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error general al deserializar JSON a List<String>: $jsonString", e)
            emptyList()
        }
    }

    // ✅ Para convertir un String simple a JSON
    fun stringToJson(value: String): String {
        return gson.toJson(value)
    }

    // ✅ Para convertir JSON de vuelta a String simple
    fun jsonToString(json: String): String {
        return try {
            gson.fromJson(json, String::class.java) ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    // ✅ Para listas (si aún las necesitas para otras cosas)
    fun listToJson(list: List<String>): String {
        return gson.toJson(list)
    }

    fun jsonToList(json: String): List<String> {
        return try {
            gson.fromJson(json, object : TypeToken<List<String>>() {}.type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ✅ Para sets (si aún los necesitas)
    fun setToJson(set: Set<String>): String {
        return gson.toJson(set)
    }

    fun jsonToSet(json: String): Set<String> {
        return try {
            gson.fromJson(json, object : TypeToken<Set<String>>() {}.type) ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    // ✅ Para cualquier objeto (genérico)
    inline fun <reified T> toJson(obj: T): String {
        return gson.toJson(obj)
    }

    inline fun <reified T> fromJson(json: String): T? {
        return try {
            gson.fromJson(json, T::class.java)
        } catch (e: Exception) {
            null
        }
    }


    inline fun <reified T> fromJsonToList(json: String?): List<T> {
        if (json.isNullOrBlank() || json == "[]") {
            return emptyList()
        }
        return try {
            val listType: Type = object : TypeToken<List<T>>() {}.type
            // Modificación aquí:
            val result: List<T>? = gson.fromJson<List<T>>(json, listType) // Especifica explícitamente List<T>
            result ?: emptyList()
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error de sintaxis al deserializar JSON a List<${T::class.java.simpleName}>: $json", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error general al deserializar JSON a List<${T::class.java.simpleName}>: $json", e)
            emptyList()
        }
    }

    inline fun <reified T> fromJsonToObject(json: String?): T? {
        if (json.isNullOrBlank()) {
            return null
        }
        return try {
            gson.fromJson(json, T::class.java)
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Error de sintaxis al deserializar JSON a ${T::class.java.simpleName}: $json", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error general al deserializar JSON a ${T::class.java.simpleName}: $json", e)
            null
        }
    }
}