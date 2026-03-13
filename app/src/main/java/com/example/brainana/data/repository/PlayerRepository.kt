package com.example.brainana.data.repository

import com.example.brainana.data.models.Player
import com.example.brainana.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class PlayerRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun fetchPlayerProfile(uid: String): Player? {
        return try {
            val doc = db.collection(Constants.FIRESTORE_PLAYERS_COLLECTION)
                .document(uid).get().await()
            doc.toObject(Player::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createPlayer(player: Player) {
        try {
            db.collection(Constants.FIRESTORE_PLAYERS_COLLECTION)
                .document(player.uid).set(player).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updatePlayer(player: Player) {
        try {
            db.collection(Constants.FIRESTORE_PLAYERS_COLLECTION)
                .document(player.uid).set(player).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updatePlayerAvatar(uid: String, avatarStyle: String) {
        try {
            db.collection(Constants.FIRESTORE_PLAYERS_COLLECTION)
                .document(uid).update("avatarStyle", avatarStyle).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updatePlayerStats(uid: String, highScore: Int, totalXp: Int, level: Int) {
        try {
            db.collection(Constants.FIRESTORE_PLAYERS_COLLECTION)
                .document(uid).update(
                    mapOf(
                        "highScore" to highScore,
                        "totalEarnings" to totalXp,
                        "level" to level
                    )
                ).await()
        } catch (e: Exception) {
            throw e
        }
    }
}