package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = "me",
    val name: String,
    val email: String,
    val role: String, // "customer" or "partner"
    val kycStatus: String, // "not_started", "submitted", "under_review", "approved", "rejected"
    val kycReason: String? = null,
    val aadhaarNo: String = "",
    val panNo: String = "",
    val selfieUrl: String = "",
    val walletBalancePaise: Long = 0,
    val partnerBio: String = "",
    val partnerExperience: Int = 0,
    val partnerServicesOffered: String = "", // Comma-separated list of service IDs
    val averageRating: Float = 4.8f,
    val completedJobs: Int = 0,
    val partnerPublicCode: String = "" // §691 — unique UPPERCASE transfer code
)

@Entity(tableName = "addresses")
data class AddressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val labelText: String, // "Home", "Office", "Other"
    val line1: String,
    val line2: String = "",
    val city: String,
    val pincode: String,
    val lat: Double,
    val lon: Double,
    val isDefault: Boolean = false
)

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey val id: String, // e.g. "B-2301"
    val status: String, // "pending" -> "accepted"|"rejected" -> "assigned" -> "partner_on_the_way" -> "arrived" -> "started" -> "completed" -> "cancelled" -> "refunded"
    val serviceId: String,
    val serviceName: String,
    val serviceImageUrl: String,
    val categoryName: String,
    val partnerId: String,
    val partnerName: String,
    val partnerAvatar: String,
    val dateTimeSlot: String,
    val slotStartIso: String = "", // §691 — raw ISO-8601 slot start, for window math
    val addressText: String,
    val totalPaise: Long,
    val paymentStatus: String, // "pending", "paid", "refunded"
    val createdAt: Long = System.currentTimeMillis(),
    val startOtp: String = "",
    val completionProofUrls: String = "", // Comma-separated URLs
    val reviewRating: Int = 0, // 0 means unreviewed
    val reviewComment: String = ""
)

@Entity(tableName = "partner_services")
data class PartnerServiceEntity(
    @PrimaryKey val id: String, // partnerId_serviceId
    val serviceId: String,
    val name: String,
    val categoryName: String,
    val pricePaise: Long,
    val durationMin: Int,
    val active: Boolean = true,
    val productsUsed: String = "Premium sealed pack products, mutually verified upon arrival."
)

@Entity(tableName = "wallet_transactions")
data class WalletTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "credit", "debit"
    val role: String, // "customer", "partner"
    val amountPaise: Long,
    val reason: String,
    val at: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookingId: String,
    val senderRole: String, // "customer", "partner"
    val text: String,
    val kind: String = "text", // "text", "image", "voice"
    val timestamp: Long = System.currentTimeMillis(),
    val voiceDurationMs: Long = 0
)

@Entity(tableName = "complaints")
data class ComplaintEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookingId: String,
    val subject: String,
    val message: String,
    val status: String, // "Pending", "In Review", "Resolved"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorite_partners")
data class FavoritePartnerEntity(
    @PrimaryKey val partnerId: String
)
