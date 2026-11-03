
package com.example.skaneralergenow

class AllergenDetector(
    private val dataManager: DataManager
) {

    data class DetectionResult(
        val lethalAllergens: List<String>,
        val otherAllergens: List<String>,
        val harmfulSubstances: List<String>,
        val eNumbers: Map<String, String>
    )

    fun analyzeText(text: String): DetectionResult {

        val normalizedText = text.lowercase()

        val lethal = findMatches(
            normalizedText,
            dataManager.getLethalAllergens()
        )

        val other = findMatches(
            normalizedText,
            dataManager.getOtherAllergens()
        )

        val harmful = findMatches(
            normalizedText,
            dataManager.getHarmfulSubstances()
        )

        val eNumbersDatabase = dataManager.getENumbers()
        val foundENumbers = mutableMapOf<String, String>()

        val eNumberRegex = Regex(
            """\be\d{3,4}[a-z]?\b""",
            RegexOption.IGNORE_CASE
        )

        val detectedENumbers = eNumberRegex
            .findAll(normalizedText)
            .map { it.value.uppercase() }
            .toSet()

        for (number in detectedENumbers) {
            val description = eNumbersDatabase[number]

            if (description != null) {
                foundENumbers[number] = description
            }
        }

        return DetectionResult(
            lethalAllergens = lethal,
            otherAllergens = other,
            harmfulSubstances = harmful,
            eNumbers = foundENumbers
        )
    }

    private fun findMatches(
        text: String,
        category: Map<String, List<String>>
    ): List<String> {

        val found = mutableSetOf<String>()

        for ((name, variants) in category) {

            for (variant in variants) {

                if (variant.isBlank()) continue

                if (text.contains(variant)) {
                    found.add(name)
                    break
                }

                // Dopasowanie przybliżone dla literówek OCR
                val words = text.split(
                    Regex("""[^\p{L}\p{N}]+""")
                )

                val similarWordFound = words.any { word ->
                    word.length >= 4 &&
                            similarity(word, variant) >= 0.8
                }

                if (similarWordFound) {
                    found.add(name)
                    break
                }
            }
        }

        return found.toList()
    }

    private fun similarity(first: String, second: String): Double {

        val distance = levenshteinDistance(first, second)
        val maxLength = maxOf(first.length, second.length)

        if (maxLength == 0) return 1.0

        return 1.0 - distance.toDouble() / maxLength
    }

    private fun levenshteinDistance(
        first: String,
        second: String
    ): Int {

        val matrix = Array(first.length + 1) {
            IntArray(second.length + 1)
        }

        for (i in 0..first.length) {
            matrix[i][0] = i
        }

        for (j in 0..second.length) {
            matrix[0][j] = j
        }

        for (i in 1..first.length) {
            for (j in 1..second.length) {

                val cost = if (first[i - 1] == second[j - 1]) {
                    0
                } else {
                    1
                }

                matrix[i][j] = minOf(
                    matrix[i - 1][j] + 1,
                    matrix[i][j - 1] + 1,
                    matrix[i - 1][j - 1] + cost
                )
            }
        }

        return matrix[first.length][second.length]
    }
}