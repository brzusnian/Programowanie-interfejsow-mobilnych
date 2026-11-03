
package com.example.skaneralergenow

import android.content.Context
import org.json.JSONObject

class DataManager(context: Context) {

    private val data: JSONObject

    init {
        val jsonString = context.assets.open("data.json")
            .bufferedReader()
            .use { it.readText() }

        data = JSONObject(jsonString)
    }

    // Pobieranie wybranej kategorii z pliku JSON
    private fun getCategory(category: String): Map<String, List<String>> {

        val result = mutableMapOf<String, List<String>>()

        val jsonObject = data.optJSONObject(category)
            ?: return result

        val keys = jsonObject.keys()

        while (keys.hasNext()) {
            val key = keys.next()
            val array = jsonObject.getJSONArray(key)

            val variants = mutableListOf<String>()

            for (i in 0 until array.length()) {
                variants.add(
                    array.getString(i).lowercase()
                )
            }

            result[key] = variants
        }

        return result
    }

    // Alergeny krytyczne
    fun getLethalAllergens(): Map<String, List<String>> {
        return getCategory("LETHAL_ALLERGENS")
    }

    // Pozostałe alergeny
    fun getOtherAllergens(): Map<String, List<String>> {
        return getCategory("OTHER_ALLERGENS")
    }

    // Szkodliwe substancje
    fun getHarmfulSubstances(): Map<String, List<String>> {
        return getCategory("HARMFUL_SUBSTANCES")
    }

    // Numery E i ich opisy
    fun getENumbers(): Map<String, String> {

        val result = mutableMapOf<String, String>()

        val jsonObject = data.optJSONObject("E_NUMBER_DB")
            ?: return result

        val keys = jsonObject.keys()

        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = jsonObject.getString(key)
        }

        return result
    }
}