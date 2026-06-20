package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Wire DTOs for the NikhatGlow REST contract (`/api/nikhatglow/v1/`). Field names use
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
    // §691 — partner's own unique transfer code (UPPERCASE).
    @Json(name = "public_code") val publicCode: String? = null,
    // §694 — partner business prefs (returned by ser_partner_profile / auth/me).
    val gender: String? = null,
    @Json(name = "minimum_order_paise") val minimumOrderPaise: Long? = null,
    @Json(name = "travel_radius_km") val travelRadiusKm: Double? = null,
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
    // §690 — partner-driven PRICE RANGE. We never set a service price; partners do.
    // The catalog returns the min/max of what offering partners charge + how many
    // offer it. Nullable: a service with no offering won't be returned at all, but
    // older backends omit these → null (the app falls back to base price).
    @Json(name = "price_min_paise") val priceMinPaise: Long? = null,
    @Json(name = "price_max_paise") val priceMaxPaise: Long? = null,
    @Json(name = "from_price_paise") val fromPricePaise: Long? = null,
    @Json(name = "partner_count") val partnerCount: Int = 0,
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
    // §691 — the partner's own unique code (UPPERCASE), shown on their profile and
    // shared with a colleague to receive a targeted booking transfer.
    @Json(name = "public_code") val publicCode: String? = null,
    // §694 — partner business prefs.
    val gender: String? = null,
    @Json(name = "minimum_order_paise") val minimumOrderPaise: Long? = null,
    @Json(name = "travel_radius_km") val travelRadiusKm: Double? = null,
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

// ── Geo (§687/§692 — free OpenStreetMap proxy responses) ─────────────────────
@JsonClass(generateAdapter = true)
data class GeoSuggestionDto(
    @Json(name = "place_id") val placeId: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
)

@JsonClass(generateAdapter = true)
data class GeoSuggestionsResp(
    val suggestions: List<GeoSuggestionDto> = emptyList(),
    @Json(name = "_disabled") val disabled: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class GeoReverseResp(
    val address: String? = null,
    val city: String? = null,
    val pincode: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    @Json(name = "_disabled") val disabled: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class GeoGeocodeResultDto(val address: String? = null, val lat: Double? = null, val lon: Double? = null)

@JsonClass(generateAdapter = true)
data class GeoGeocodeResp(
    val results: List<GeoGeocodeResultDto> = emptyList(),
    @Json(name = "_disabled") val disabled: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class GeoDirectionsResp(
    val polyline: String = "",
    @Json(name = "distance_m") val distanceM: Int = 0,
    @Json(name = "duration_s") val durationS: Int = 0,
    @Json(name = "_disabled") val disabled: Boolean = false,
)

// ── Geo gateway app-config (§690 — GET /api/geo/app-config?app=nikhatglow) ────
@JsonClass(generateAdapter = true)
data class GeoFeaturesDto(
    val autocomplete: Boolean = true,
    @Json(name = "reverse_geocode") val reverseGeocode: Boolean = true,
    val geocode: Boolean = true,
    val directions: Boolean = true,
    @Json(name = "live_tracking") val liveTracking: Boolean = true,
    @Json(name = "map_tiles") val mapTiles: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class GeoAppConfigDto(
    val app: String = "nikhatglow",
    val enabled: Boolean = true,
    @Json(name = "maps_enabled") val mapsEnabled: Boolean = false,
    @Json(name = "weather_enabled") val weatherEnabled: Boolean = false,
    // §692 — free OpenStreetMap: no key. The map renders from this MapLibre style.
    @Json(name = "tile_style_url") val tileStyleUrl: String = "https://tiles.openfreemap.org/styles/liberty",
    @Json(name = "map_provider") val mapProvider: String = "osm",
    // Legacy Ola fields kept so older app builds / payloads still parse; unused.
    @Json(name = "tile_key") val tileKey: String = "",
    @Json(name = "base_url") val baseUrl: String = "https://api.olamaps.io",
    val features: GeoFeaturesDto = GeoFeaturesDto(),
)

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

// ── Cart (single-partner, multi-service) ─────────────────────────────────────
@JsonClass(generateAdapter = true)
data class CartItemDto(
    val id: Int,
    @Json(name = "service_id") val serviceId: Int,
    val name: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    val qty: Int = 1,
    @Json(name = "unit_price_paise") val unitPricePaise: Long = 0,
    @Json(name = "line_total_paise") val lineTotalPaise: Long = 0,
)

@JsonClass(generateAdapter = true)
data class CartResp(
    @Json(name = "partner_id") val partnerId: Int? = null,
    @Json(name = "partner_name") val partnerName: String? = null,
    val items: List<CartItemDto> = emptyList(),
    @Json(name = "subtotal_paise") val subtotalPaise: Long = 0,
    val count: Int = 0,
)

@JsonClass(generateAdapter = true)
data class CartAddReq(
    @Json(name = "partner_id") val partnerId: Int,
    @Json(name = "service_id") val serviceId: Int,
    val qty: Int = 1,
)

@JsonClass(generateAdapter = true)
data class CartItemPatchReq(val qty: Int)

@JsonClass(generateAdapter = true)
data class CartQuoteReq(
    @Json(name = "slot_id") val slotId: String? = null,
    @Json(name = "address_id") val addressId: Int? = null,
    @Json(name = "coupon_code") val couponCode: String? = null,
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
data class BookingCreateReq(
    @Json(name = "quote_id") val quoteId: String,
    // §694 — optional booking-time data capture (backend defaults if omitted).
    @Json(name = "customer_notes") val customerNotes: String? = null,
    @Json(name = "gender_preference") val genderPreference: String? = null,
    @Json(name = "booking_source") val bookingSource: String? = null,
    // Backend expects an OBJECT (Optional[dict]) — sending a JSON *string* here
    // makes pydantic 422 ("Some details look invalid"). Keep this a real Map so
    // Moshi emits {"platform":...} not "\"{...}\"".
    @Json(name = "device_info") val deviceInfo: Map<String, String?>? = null,
    // §704 — share her number with the partner after accept (default OFF).
    @Json(name = "customer_share_number") val customerShareNumber: Boolean = false,
)

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
    // §703 — pre-visit safety gate state (detail view): drives the customer's
    // "Confirm visit" step + the partner's disabled "On my way" button.
    @Json(name = "pre_visit_required") val preVisitRequired: Boolean = false,
    @Json(name = "pre_visit_contact_ok") val preVisitContactOk: Boolean = false,
    // §704 — counterparty contact reveal (only while live; gated by the share choice).
    val contact: ContactDto? = null,
    @Json(name = "customer_share_number") val customerShareNumber: Boolean = false,
    // §704 — present on a felt-unsafe cancel response.
    @Json(name = "women_helpline") val womenHelpline: String? = null,
    @Json(name = "safe_exit_message") val safeExitMessage: String? = null,
)

@JsonClass(generateAdapter = true)
data class ContactPartyDto(
    val name: String? = null,
    val phone: String? = null,
    @Json(name = "call_allowed") val callAllowed: Boolean = false,
    @Json(name = "chat_only") val chatOnly: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class ContactDto(
    val partner: ContactPartyDto? = null,
    val customer: ContactPartyDto? = null,
)

@JsonClass(generateAdapter = true)
data class TimelineEventDto(val status: String, val at: String? = null, val note: String? = null)

@JsonClass(generateAdapter = true)
data class BookingsResp(val items: List<BookingDto> = emptyList())

@JsonClass(generateAdapter = true)
data class CancelReq(
    val reason: String,
    // §704 — e.g. "felt_unsafe" → no penalty/cooldown + women-helpline surfaced.
    @Json(name = "reason_code") val reasonCode: String? = null,
)

@JsonClass(generateAdapter = true)
data class BlockedPartnersResp(@Json(name = "partner_ids") val partnerIds: List<Int> = emptyList())

// §704 — customer reschedule: move a pending/accepted booking to a new slot
// ("<partnerId>:<YYYY-MM-DD>:<hour>"); allowed up to 3h before the slot.
@JsonClass(generateAdapter = true)
data class RescheduleReq(@Json(name = "slot_id") val slotId: String)

// ── §691 reassignment ─────────────────────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class ReassignmentOfferBookingDto(
    val id: Int,
    val status: String? = null,
    @Json(name = "service_name") val serviceName: String? = null,
    @Json(name = "slot_start") val slotStart: String? = null,
    val city: String? = null,
    val pincode: String? = null,
)

@JsonClass(generateAdapter = true)
data class ReassignmentOfferDto(
    @Json(name = "offer_id") val offerId: Int,
    @Json(name = "booking_id") val bookingId: Int,
    val mode: String = "broadcast",
    val status: String = "open",
    @Json(name = "agreed_total_paise") val agreedTotalPaise: Long = 0,
    @Json(name = "is_targeted_to_me_window") val isTargetedToMeWindow: Boolean = false,
    @Json(name = "exclusive_until") val exclusiveUntil: String? = null,
    @Json(name = "expires_at") val expiresAt: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    val booking: ReassignmentOfferBookingDto? = null,
)

@JsonClass(generateAdapter = true)
data class OffersResp(val items: List<ReassignmentOfferDto> = emptyList())

@JsonClass(generateAdapter = true)
data class ChangePartnerResp(val booking: BookingDto? = null, val offer: ReassignmentOfferDto? = null)

@JsonClass(generateAdapter = true)
data class ReassignmentStatusResp(
    @Json(name = "booking_status") val bookingStatus: String? = null,
    val offer: ReassignmentOfferDto? = null,
)

@JsonClass(generateAdapter = true)
data class TransferReq(
    val mode: String = "broadcast",                       // broadcast | targeted
    @Json(name = "target_public_code") val targetPublicCode: String? = null,
)

@JsonClass(generateAdapter = true)
data class TransferResp(val booking: BookingDto? = null, val offer: ReassignmentOfferDto? = null)

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
    // §704 — app's own quick-reply templates skip moderation (safe + instant).
    val predefined: Boolean = false,
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
    val blocked: Boolean = false,   // §704 — the sender's own blocked message
    @Json(name = "created_at") val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class ChatMessagesResp(val items: List<ChatMessageDto> = emptyList())

// ── §704 talk-request + partner inbox ────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class TalkRequestDto(
    val id: Int,
    @Json(name = "booking_id") val bookingId: Int? = null,
    @Json(name = "requester_type") val requesterType: String? = null,
    val status: String = "pending",
    val reason: String? = null,
    @Json(name = "reject_count") val rejectCount: Int = 0,
    @Json(name = "chat_open_until") val chatOpenUntil: String? = null,
    @Json(name = "chat_open_now") val chatOpenNow: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class TalkRequestStateResp(
    @Json(name = "chat_open") val chatOpen: Boolean = false,
    @Json(name = "talk_request") val talkRequest: TalkRequestDto? = null,
)

@JsonClass(generateAdapter = true)
data class PartnerInboxThreadDto(
    @Json(name = "customer_id") val customerId: Int,
    val count: Int = 0,
    val last: ChatMessageDto? = null,
)

@JsonClass(generateAdapter = true)
data class PartnerInboxResp(val items: List<PartnerInboxThreadDto> = emptyList())

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
    @Json(name = "selfie_upload_id") val selfieUploadId: String? = null,
    @Json(name = "document_upload_ids") val documentUploadIds: List<String> = emptyList(),
    // §704 — the legal name printed on her ID; the admin locks the display name to it.
    @Json(name = "legal_name") val legalName: String? = null,
)

@JsonClass(generateAdapter = true)
data class KycStatusResp(
    val status: String = "not_started",
    val reason: String? = null,
    // §704 — once approved, the verified (locked) name.
    @Json(name = "name_locked") val nameLocked: Boolean = false,
    @Json(name = "verified_name") val verifiedName: String? = null,
)

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

// ── Partner subscription (₹99/month) ─────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class SubscriptionDto(
    val plan: String = "monthly_99",
    val status: String = "none",                         // none|trial|active|expired|cancelled
    @Json(name = "price_paise") val pricePaise: Long = 9900,
    @Json(name = "is_active") val isActive: Boolean = false,
    @Json(name = "current_period_start") val currentPeriodStart: String? = null,
    @Json(name = "current_period_end") val currentPeriodEnd: String? = null,
    @Json(name = "trial_end") val trialEnd: String? = null,
    @Json(name = "days_left") val daysLeft: Int = 0,
    @Json(name = "auto_renew") val autoRenew: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class SubscriptionPaymentDto(
    val id: Int = 0,
    @Json(name = "amount_paise") val amountPaise: Long = 0,
    val method: String? = null,
    val status: String = "paid",
    val ref: String? = null,
    @Json(name = "period_start") val periodStart: String? = null,
    @Json(name = "period_end") val periodEnd: String? = null,
    val at: String? = null,
)

@JsonClass(generateAdapter = true)
data class SubscriptionPaymentsResp(val items: List<SubscriptionPaymentDto> = emptyList())

// ── Partner availability (working hours) ─────────────────────────────────────
@JsonClass(generateAdapter = true)
data class PartnerAvailabilityResp(
    @Json(name = "working_hours") val workingHours: WorkingHoursDto? = null,
    val days: List<Int> = emptyList(),
    val leaves: List<String> = emptyList(),
    @Json(name = "hour_overrides") val hourOverrides: Map<String, List<Int>> = emptyMap(),
)

@JsonClass(generateAdapter = true)
data class WorkingHoursDto(val start: String? = null, val end: String? = null)

// ── Partner earnings (connector model — informational, paid offline) ─────────
@JsonClass(generateAdapter = true)
data class EarningsRecentDto(
    @Json(name = "booking_id") val bookingId: Int = 0,
    @Json(name = "total_paise") val totalPaise: Long = 0,
    val at: String? = null,
)

@JsonClass(generateAdapter = true)
data class EarningsDto(
    val currency: String = "INR",
    @Json(name = "today_paise") val todayPaise: Long = 0,
    @Json(name = "week_paise") val weekPaise: Long = 0,
    @Json(name = "month_paise") val monthPaise: Long = 0,
    @Json(name = "lifetime_paise") val lifetimePaise: Long = 0,
    @Json(name = "completed_jobs") val completedJobs: Int = 0,
    val recent: List<EarningsRecentDto> = emptyList(),
)

// ── Partner analytics ────────────────────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class ProfileViewPointDto(val date: String? = null, val views: Int = 0)

@JsonClass(generateAdapter = true)
data class AnalyticsFunnelDto(
    val pending: Int = 0,
    val accepted: Int = 0,
    val completed: Int = 0,
    val cancelled: Int = 0,
    val rejected: Int = 0,
)

@JsonClass(generateAdapter = true)
data class AnalyticsDto(
    @Json(name = "accept_rate") val acceptRate: Float = 0f,
    val accepted: Int = 0,
    val rejected: Int = 0,
    @Json(name = "avg_response_min") val avgResponseMin: Float = 0f,
    @Json(name = "profile_views_total") val profileViewsTotal: Int = 0,
    @Json(name = "profile_views_30d") val profileViews30d: Int = 0,
    @Json(name = "profile_views_trend") val profileViewsTrend: List<ProfileViewPointDto> = emptyList(),
    @Json(name = "rating_avg") val ratingAvg: Float = 0f,
    @Json(name = "rating_count") val ratingCount: Int = 0,
    @Json(name = "rating_distribution") val ratingDistribution: Map<String, Int> = emptyMap(),
    val funnel: AnalyticsFunnelDto = AnalyticsFunnelDto(),
)

// ── Partner portfolio ────────────────────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class PortfolioItemDto(
    val id: Int = 0,
    @Json(name = "image_url") val imageUrl: String? = null,
    val caption: String? = null,
    val sort: Int = 0,
)

@JsonClass(generateAdapter = true)
data class PortfolioResp(val items: List<PortfolioItemDto> = emptyList())

@JsonClass(generateAdapter = true)
data class PortfolioCreateReq(
    @Json(name = "upload_id") val uploadId: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    val caption: String? = null,
    val sort: Int = 0,
)

// ── Notifications (in-app inbox + FCM device registration) ───────────────────
// The backend's `data` object is intentionally omitted here: Moshi ignores
// unknown JSON fields, so we avoid pulling in a Map<String,Any> adapter.
@JsonClass(generateAdapter = true)
data class NotificationDto(
    val id: Int,
    val type: String? = null,
    val title: String? = null,
    val body: String? = null,
    val read: Boolean = false,
    @Json(name = "created_at") val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class NotificationsResp(
    val items: List<NotificationDto> = emptyList(),
    val unread: Int = 0,
)

@JsonClass(generateAdapter = true)
data class DeviceReq(
    @Json(name = "fcm_token") val fcmToken: String,
    val platform: String = "android",
)

@JsonClass(generateAdapter = true)
data class DeviceDeleteReq(
    @Json(name = "fcm_token") val fcmToken: String,
)

// ── §703 Flow-B open booking ─────────────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class OpenLineReq(
    @Json(name = "service_id") val serviceId: Int,
    val qty: Int = 1,
)

@JsonClass(generateAdapter = true)
data class OpenBookingReq(
    @Json(name = "service_lines") val serviceLines: List<OpenLineReq>,
    @Json(name = "slot_start") val slotStart: String,           // ISO "YYYY-MM-DDTHH:MM"
    @Json(name = "address_id") val addressId: Int? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    @Json(name = "customer_notes") val customerNotes: String? = null,
    @Json(name = "booking_source") val bookingSource: String? = "app",
    @Json(name = "device_info") val deviceInfo: Map<String, String?>? = null,
    @Json(name = "customer_share_number") val customerShareNumber: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class OpenBookingResp(
    val booking: BookingDto,
    val dispatched: Boolean = false,
    @Json(name = "offer_id") val offerId: Int? = null,
    @Json(name = "candidate_count") val candidateCount: Int = 0,
    val message: String? = null,
)

// ── §703 pre-visit confirm ───────────────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class ConfirmVisitResp(
    val ok: Boolean = true,
    @Json(name = "pre_visit_contact_ok") val preVisitContactOk: Boolean = true,
    val booking: BookingDto? = null,
)

// ── §703 SOS ─────────────────────────────────────────────────────────────────
@JsonClass(generateAdapter = true)
data class SosReq(
    @Json(name = "booking_id") val bookingId: Int? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val note: String? = null,
)

@JsonClass(generateAdapter = true)
data class SosResp(
    val ok: Boolean = true,
    @Json(name = "sos_id") val sosId: Int? = null,
    @Json(name = "emergency_number") val emergencyNumber: String = "112",
    val message: String? = null,
)

// ── §703 app config (feature flags / role visibility / params / policies) ────
@JsonClass(generateAdapter = true)
data class AppConfigSubscription(
    @Json(name = "price_paise") val pricePaise: Long = 9900,
    @Json(name = "trial_days") val trialDays: Int = 14,
)

@JsonClass(generateAdapter = true)
data class AppConfigResp(
    val version: Int = 0,
    val role: String? = null,
    @Json(name = "women_only") val womenOnly: Boolean = true,
    val currency: String = "INR",
    val subscription: AppConfigSubscription = AppConfigSubscription(),
    val flags: Map<String, Boolean> = emptyMap(),
    val surfaces: Map<String, Boolean> = emptyMap(),
    // §705 — `@JvmSuppressWildcards` is REQUIRED here. Without it Kotlin compiles
    // the `Any` value type to a Java wildcard (`? extends Object`); Moshi's
    // generated adapter then crashes at first use with "… type must not be a
    // type variable or wildcard" — the founder's "subscription error" (this
    // config is fetched when the subscription screen opens). Suppressing the
    // wildcard lets Moshi resolve its built-in Object adapter.
    val params: Map<String, @JvmSuppressWildcards Any> = emptyMap(),
    val policies: Map<String, String> = emptyMap(),
)
