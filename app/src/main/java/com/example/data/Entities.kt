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
    val partnerPublicCode: String = "", // §691 — unique UPPERCASE transfer code
    // §694 — display + partner business prefs.
    val phone: String = "",
    val gender: String = "", // "", "male", "female", "any"
    val minimumOrderPaise: Long = 0,
    val travelRadiusKm: Double = 0.0,
    // §707 — the backend profile ID (customer or partner). For a partner this is
    // also their public bookable ID a customer can search to find + book them.
    val profileId: Int = 0
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
    // Coarse location used for the partner's PRE-ACCEPT view: the backend redacts
    // the precise address to city/pincode for viewer=partner until the job is
    // accepted, so the partner queue shows only this until then.
    val city: String = "",
    val pincode: String = "",
    // §698 — customer's saved booking/home location = the partner's destination on
    // the live map. Nullable: older bookings / pure-manual addresses may lack coords.
    val addressLat: Double? = null,
    val addressLon: Double? = null,
    val totalPaise: Long,
    val paymentStatus: String, // "pending", "paid", "refunded"
    val createdAt: Long = System.currentTimeMillis(),
    val startOtp: String = "",
    val completionProofUrls: String = "", // Comma-separated URLs
    val reviewRating: Int = 0, // 0 means unreviewed
    val reviewComment: String = "",
    // §710 #5/#6 — the customer's note + a display summary of ALL booked services
    // (so a multi-service booking shows every line, not just the primary service).
    val customerNotes: String = "",
    val itemsSummary: String = "",
    // §703 — pre-visit safety gate (detail view): drives the customer "Confirm
    // visit" step and the partner's disabled "On my way" button.
    val preVisitRequired: Boolean = false,
    val preVisitContactOk: Boolean = false,
    // §704 — the counterparty's contact, revealed by the server only while the
    // booking is live (and the customer's number only if she opted in). Drives the
    // call button; empty once the booking is over.
    val counterpartyName: String = "",
    val counterpartyPhone: String = "",
    val callAllowed: Boolean = false,
    // §722 req-2 — distance (km) from the partner's base location to the customer's
    // service location; populated only for the partner/admin viewer. Null = unknown.
    val distanceKm: Double? = null,
    // §723 dual rating — has the partner rated this customer yet, and the rating given.
    val customerRated: Boolean = false,
    val customerRating: Int = 0,
    // §728 (parity C1) — TRANSPARENCY. The server-built lifecycle timeline encoded as
    // one "status|iso8601" line per entry ("" = that state not yet reached); decoded by
    // the booking-detail vertical stepper. Stored as a plain String so Room needs no
    // TypeConverter. Empty when the server omitted it (older backend).
    val timelineEncoded: String = "",
    // §728 (parity C1) — the partner's live start-selfie proof (base64 data: URL /
    // resolved URL); shown to whoever can see the booking as arrival proof. Blank
    // until the partner starts the job with a selfie.
    val startSelfieUrl: String = "",
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
    val voiceDurationMs: Long = 0,
    // §714 cust-chat-4 — the server returns blocked=true to the SENDER for a
    // moderation-soft-blocked message so her own UI can show it wasn't delivered.
    val blocked: Boolean = false
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
