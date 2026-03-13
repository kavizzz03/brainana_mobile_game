package com.example.brainana.data.repository

import com.example.brainana.data.models.GameTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class PuzzleData(
    val question: String,
    val solution: Int
)

class GameRepository {
    private val client = OkHttpClient()

    suspend fun fetchPuzzle(theme: GameTheme): PuzzleData = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = "https://marcconrad.com/uob/${theme.apiPath}/api.php?out=json"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "{}")

            PuzzleData(
                question = json.optString("question"),
                solution = json.optInt("solution")
            )
        } catch (e: Exception) {
            PuzzleData("", -1)
        }
    }
}