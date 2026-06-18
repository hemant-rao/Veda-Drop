package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Wire DTOs for the GlamGo REST contract (`/api/glamgo/v1/`). Field names use
 * @Json to map the backend's snake_case to Kotlin camelCase. Money is paise
 * (Long). Server ids are Int; the app models stringify them in [Mappers].
 *
 * Every field that the backend may omit is nullable / defaulted so a partial
 * response never crashes Moshi.
 */

// ── Auth ─────────────────────────────────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class OtpRequestResp(
    @Json(name = "otp_token") val otpToken: String,
    @Json(name = "expires_in") val expiresIn: Int = 0,
    @Json(name = "resend_after_s") val resendAfterS: Int = 0,
    @Json(name = "dev_otp") val devOtp: String? = null,
)

@JsonClass(generateAdapter = true)
data class OtpVerifyResp(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "is_new_user") val isNewUser: Boolean = false,
    val profile: ProfileDto? = null,
)

@JsonClass(generateAdapter = true)
data class RefreshResp(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
)

@JsonClass(generateAdapter = true)
data class ProfileDto(
    val id: Int = 0,
    val phone: String? = null,
    val name: String? = null,
    val email: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val locale: String? = null,
    val role: String? = null,
    // partner profile extras
    val bio: String? = null,
    @Json(name = "experience_years") val experienceYears: Int? = null,
    @Json(name = "rating_avg") val ratingAvg: Float? = null,
    @Json(name = "completed_jobs") val completedJobs: Int? = null,
    @Json(name = "kyc_status") val kycStatus: String? = null,
)

@JsonClass(generateAdapter = true)
data class MeResp(val profile: ProfileDto? = null)

// ── Catalog ──────────────────────────────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class CategoryDto(
    val id: Int,
    val name: String,
    val slug: String? = null,
    @Json(name = "icon_url") val iconUrl: String? = null,
    val sort: Int = 0,
)

@JsonClass(generateAdapter = true)
data class ServiceDto(
    val id: Int,
    @Json(name = "category_id") val categoryId: Int = 0,
    val name: String,
    val slug: String? = null,
    val description: String? = null,
    @Json(name = "duration_min") val durationMin: Int = 60,
    @Json(name = "base_price_paise") val basePricePaise: Long = 0,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "rating_avg") val ratingAvg: Float = 0f,
    @Json(name = "rating_count") val ratingCount: Int = 0,
    val inclusions: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class CategoriesResp(val items: List<CategoryDto> = emptyList())

@JsonClass(generateAdapter = true)
data class ServicesResp(val items: List<ServiceDto> = emptyList())

// ── Partners ─────────────────────────────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class PartnerDto(
    val id: Int,
    val name: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "rating_avg") val ratingAvg: Float = 0f,
    @Json(name = "rating_count") val ratingCount: Int = 0,
    @Json(name = "distance_km") val distanceKm: Double? = null,
    @Json(name = "eta_min") val etaMin: Int? = null,
    @Json(name = "from_price_paise") val fromPricePaise: Long? = null,
    val bio: String? = null,
    @Json(name = "experience_years") val experienceYears: Int? = null,
    @Json(name = "completed_jobs") val completedJobs: Int? = null,
    @Json(name = "kyc_status") val kycStatus: String? = null,
    val certifications: List<String>? = null,
    val languages: List<String>? = null,
    val portfolio: List<String>? = null,
    val categories: List<String>? = null,
    @Json(name = "services_offered") val servicesOffered: List<Int>? = null,
)

@JsonClass(generateAdapter = true)
data class PartnersResp(val items: List<PartnerDto> = emptyList())

@JsonClass(generateAdapter = true)
data class SlotDto(
    @Json(name = "slot_id") val slotId: String,
    val start: String? = null,
    val end: String? = null,
    val available: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class SlotsResp(val slots: List<SlotDto> = emptyList())

// ── Addresses ────────────────────────────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class AddressDto(
    val id: Int,
    val label: String? = null,
    val line1: String,
    val line2: String? = null,
    val city: String? = null,
    val pincode: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    @Json(name = "is_default") val isDefault: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class AddressesResp(val items: List<AddressDto> = emptyList())

@JsonClass(generateAdapter = true)
data class AddressCreateReq(
    val label: String? = null,
    val line1: String,
    val line2: String? = null,
    val city: String? = null,
    val pincode: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    @Json(name = "is_default") val isDefault: Boolean = false,
)

// ── Quote + Booking ──────────────────────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class QuoteReq(
    @Json(name = "partner_id") val partnerId: Int,
    @Json(name = "service_id") val serviceId: Int,
    @Json(name = "slot_id") val slotId: String? = null,
    @Json(name = "address_id") val addressId: Int? = null,
    @Json(name = "coupon_code") val couponCode: String? = null,
    @Json(name = "use_wallet") val useWallet: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class QuoteBreakdownDto(
    @Json(name = "base_paise") val basePaise: Long = 0,
    @Json(name = "distance_paise") val distancePaise: Long = 0,
    @Json(name = "surge_paise") val surgePaise: Long = 0,
    @Json(name = "discount_paise") val discountPaise: Long = 0,
    @Json(name = "coupon_paise") val couponPaise: Long = 0,
    @Json(name = "tax_paise") val taxPaise: Long = 0,
    @Json(name = "wallet_applied_paise") val walletAppliedPaise: Long = 0,
)

@JsonClass(generateAdapter = true)
data class CouponDto(val code: String? = null, val valid: Boolean = false, val message: String? = null)

@JsonClass(generateAdapter = true)
data class QuoteResp(
    @Json(name = "quote_id") val quoteId: String,
    @Json(name = "expires_at") val expiresAt: String? = null,
    val breakdown: QuoteBreakdownDto = QuoteBreakdownDto(),
    @Json(name = "total_paise") val totalPaise: Long = 0,
    val currency: String = "INR",
    val coupon: CouponDto? = null,
)

@JsonClass(generateAdapter = true)
data class BookingCreateReq(@Json(name = "quote_id") val quoteId: String)

@JsonClass(generateAdapter = true)
data class BookingDto(
    val id: Int,
    val status: String,
    @Json(name = "service_id") val serviceId: Int? = null,
    @Json(name = "partner_id") val partnerId: Int? = null,
    @Json(name = "customer_id") val customerId: Int? = null,
    @Json(name = "service_name") val serviceName: String? = null,
    @Json(name = "service_image_url") val serviceImageUrl: String? = null,
    @Json(name = "category_name") val categoryName: String? = null,
    @Json(name = "partner_name") val partnerName: String? = null,
    @Json(name = "partner_avatar") val partnerAvatar: String? = null,
    @Json(name = "start_otp") val startOtp: String? = null,
    @Json(name = "slot_start") val slotStart: String? = null,
    @Json(name = "slot_end") val slotEnd: String? = null,
    val address: AddressDto? = null,
    @Json(name = "total_paise") val totalPaise: Long = 0,
    @Json(name = "payment_status") val paymentStatus: String = "unpaid",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "quote_breakdown") val quoteBreakdown: QuoteBreakdownDto? = null,
    @Json(name = "completion_proof") val completionProof: List<String>? = null,
    val timeline: List<TimelineEventDto>? = null,
)

@JsonClass(generateAdapter = true)
data class TimelineEventDto(val status: String, val at: String? = null, val note: String? = null)

@JsonClass(generateAdapter = true)
data class BookingsResp(val items: List<BookingDto> = emptyList())

@JsonClass(generateAdapter = true)
data class CancelReq(val reason: String)

@JsonClass(generateAdapter = true)
data class StartOtpResp(val otp: String? = null)

// ── Wallet ───────────────────────────────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class WalletResp(@Json(name = "balance_paise") val balancePaise: Long = 0, val currency: String = "INR")

@JsonClass(generateAdapter = true)
data class WalletTxnDto(
    val id: Int,
    val type: String,
    @Json(name = "amount_paise") val amountPaise: Long,
    val reason: String? = null,
    val ref: String? = null,
    val at: String? = null,
)

@JsonClass(generateAdapter = true)
data class WalletTxnsResp(val items: List<WalletTxnDto> = emptyList())

@JsonClass(generateAdapter = true)
data class AddMoneyReq(@Json(name = "amount_paise") val amountPaise: Long)

// ── Reviews / Complaints / Wishlist / Chat / AI ──────────────────────────────
@JsonClass(generateAdapter = true)
data class ReviewReq(
    val rating: Int,
    val comment: String? = null,
    @Json(name = "image_upload_ids") val imageUploadIds: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class ReviewDto(
    val id: Int,
    val rating: Int = 0,
    val comment: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class ReviewsResp(val items: List<ReviewDto> = emptyList())

@JsonClass(generateAdapter = true)
data class ComplaintReq(
    @Json(name = "booking_id") val bookingId: Int? = null,
    val subject: String,
    val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class ComplaintDto(
    val id: Int,
    @Json(name = "booking_id") val bookingId: Int? = null,
    val subject: String,
    val message: String? = null,
    val status: String = "open",
    val resolution: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    val messages: List<ComplaintMessageDto>? = null,
)

@JsonClass(generateAdapter = true)
data class ComplaintMessageDto(
    val id: Int,
    @Json(name = "sender_type") val senderType: String,
    val message: String,
    @Json(name = "created_at") val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class ComplaintsResp(val items: List<ComplaintDto> = emptyList())

@JsonClass(generateAdapter = true)
data class MessageReq(val message: String)

@JsonClass(generateAdapter = true)
data class WishlistReq(
    @Json(name = "service_id") val serviceId: Int? = null,
    @Json(name = "partner_id") val partnerId: Int? = null,
)

@JsonClass(generateAdapter = true)
data class WishlistResp(
    @Json(name = "service_ids") val serviceIds: List<Int> = emptyList(),
    @Json(name = "partner_ids") val partnerIds: List<Int> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ChatSendReq(
    val text: String? = null,
    val kind: String = "text",
    @Json(name = "upload_id") val uploadId: String? = null,
    @Json(name = "duration_ms") val durationMs: Int = 0,
)

@JsonClass(generateAdapter = true)
data class ChatMessageDto(
    val id: Int,
    @Json(name = "booking_id") val bookingId: Int? = null,
    @Json(name = "partner_id") val partnerId: Int? = null,
    @Json(name = "sender_type") val senderType: String,
    val text: String? = null,
    val kind: String = "text",
    @Json(name = "duration_ms") val durationMs: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class ChatMessagesResp(val items: List<ChatMessageDto> = emptyList())

@JsonClass(generateAdapter = true)
data class AiChatReq(
    val message: String,
    @Json(name = "session_id") val sessionId: String? = null,
    val history: List<Map<String, String>>? = null,
)

@JsonClass(generateAdapter = true)
data class AiChatResp(
    @Json(name = "session_id") val sessionId: String? = null,
    val reply: String = "",
    val fallback: Boolean = false,
)

// ── Partner-side ─────────────────────────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class KycReq(
    @Json(name = "aadhaar_no") val aadhaarNo: String,
    @Json(name = "pan_no") val panNo: String,
    @Json(name = "selfie_upload_id") val selfieUploadId: String,
    @Json(name = "document_upload_ids") val documentUploadIds: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class KycStatusResp(val status: String = "not_started", val reason: String? = null)

@JsonClass(generateAdapter = true)
data class PartnerServiceDto(
    val id: Int,
    @Json(name = "service_id") val serviceId: Int,
    @Json(name = "price_paise") val pricePaise: Long = 0,
    val active: Boolean = true,
    val name: String? = null,
)

@JsonClass(generateAdapter = true)
data class PartnerServicesResp(val items: List<PartnerServiceDto> = emptyList())

@JsonClass(generateAdapter = true)
data class PartnerServiceReq(
    @Json(name = "service_id") val serviceId: Int,
    @Json(name = "price_paise") val pricePaise: Long,
    val active: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class StatusReq(
    val to: String,
    @Json(name = "start_otp") val startOtp: String? = null,
    @Json(name = "proof_upload_ids") val proofUploadIds: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class EarningsResp(
    @Json(name = "balance_paise") val balancePaise: Long = 0,
    @Json(name = "lifetime_paise") val lifetimePaise: Long = 0,
)

@JsonClass(generateAdapter = true)
data class OkResp(val ok: Boolean = true)

@JsonClass(generateAdapter = true)
data class ProfileResp(val profile: ProfileDto? = null)

@JsonClass(generateAdapter = true)
data class ServicesWrap(val services: List<ServiceDto> = emptyList(), val reason: String? = null)
