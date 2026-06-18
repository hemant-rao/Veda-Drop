package com.example.data

import android.os.Build
import android.util.Log
import com.example.data.UserEntity
import com.example.data.ChatMessageEntity
import com.example.data.PartnerServiceEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object NikhatGlowFirestoreManager {
    private const val TAG = "NikhatGlowFirestoreManager"

    // Gracefully check if Firestore is configured and initialized
    val isEnabled: Boolean by lazy {
        try {
            val db = FirebaseFirestore.getInstance()
            db != null
        } catch (e: Exception) {
            Log.w(TAG, "Firestore is not initialized. App is running in Local Room Offline-First Fallback. To enable production Firestore sync, please add a valid google-services.json file to your project.")
            false
        }
    }

    // Get FireStore Instance if available, otherwise null
    private fun getDb(): FirebaseFirestore? {
        return if (isEnabled) {
            try {
                FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    // Save User Profile to Firestore
    suspend fun saveUserProfile(user: UserEntity) {
        val db = getDb() ?: return
        try {
            db.collection("users")
                .document(user.id)
                .set(user)
                .await()
            Log.d(TAG, "Successfully persisted User profile to Firestore: ${user.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed preserving user profile to firestore: ${e.message}")
        }
    }

    // Retrieve User Profile from Firestore
    suspend fun getUserProfile(userId: String): UserEntity? {
        val db = getDb() ?: return null
        return try {
            val doc = db.collection("users").document(userId).get().await()
            if (doc.exists()) {
                doc.toObject(UserEntity::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile from Firestore: ${e.message}")
            null
        }
    }

    // Save Partner Profile or Service listings
    suspend fun savePartnerService(partnerService: PartnerServiceEntity) {
        val db = getDb() ?: return
        try {
            db.collection("partner_services")
                .document(partnerService.id)
                .set(partnerService)
                .await()
            Log.d(TAG, "Persisted Partner service to firestore: ${partnerService.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving Partner service to Firestore: ${e.message}")
        }
    }

    // Save Chat Message to Firestore
    suspend fun saveChatMessage(msg: ChatMessageEntity) {
        val db = getDb() ?: return
        try {
            val docRef = db.collection("chats")
                .document() // AutoID
            docRef.set(msg).await()
            Log.d(TAG, "Persisted ChatMessage to Firestore: ${msg.text}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving ChatMessage to Firestore: ${e.message}")
        }
    }
}
