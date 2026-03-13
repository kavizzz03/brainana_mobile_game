package com.example.brainana.data.repository

import com.example.brainana.data.models.Player
import com.example.brainana.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class LeaderboardRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getLeaderboard(field: String): List<Player> {
        return try {
            val snapshot = db.collection(Constants.FIRESTORE_PLAYERS_COLLECTION)
                .orderBy(field, Query.Direction.DESCENDING)
                .limit(Constants.LEADERBOARD_LIMIT)
                .get().await()

            snapshot.toObjects(Player::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}