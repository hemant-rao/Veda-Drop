package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Wire DTOs for the VedaDrop REST contract (`/api/vedadrop/v1/`). Field names use
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
    // §743 — individual | parlour (returned by ser_partner_profile / auth/me).
    @Json(name = "partner_type") val partnerType: String? = null,
    // §744 — the partner's rest/travel gap (minutes) between bookings.
    @Json(name = "gap_min") val gapMin: Int? = null,
    // §714 cpe-beauty-1 — the customer beauty profile is saved via PATCH /auth/me and
    // returned by ser_customer; parse it so a returning user (or fresh install) re-hydrates
    // from the server instead of silently showing SharedPreferences defaults.
    @Json(name = "skin_type") val skinType: String? = null,
    @Json(name = "beauty_concerns") val beautyConcerns: String? = null,
    @Json(name = "preferred_time") val preferredTime: String? = null,
    // §714 cust-auth-consent-fake — surface the recorded consent so a consented user
    // isn't re-prompted.
    val consented: Boolean? = null,
    @Json(name = "gender_verification") val genderVerification: String? = null,
    // §758 — verification state of the email+password identity (waterfall registration).
    @Json(name = "email_verified") val emailVerified: Boolean? = null,
    @Json(name = "phone_verified") val phoneVerified: Boolean? = null,
    @Json(name = "has_password") val hasPassword: Boolean? = null,
    // §759 — the Verification Center snapshot is embedded on every auth/me +
    // login/register token bundle so the app can gate booking without an extra call.
    val verification: VerificationDto? = null,
)

// ── §759 Verification Center ──────────────────────────────────────────────────
// A role-aware "what's verified / what's blocked / what to do next" snapshot.
// Returned standalone by GET auth/verification AND embedded at profile.verification.
@JsonClass(generateAdapter = true)
data class VerificationStepDto(
    // "phone" | "email" | "kyc" | "subscription" | "location"
    val key: String = "",
    val label: String = "",
    // phone/email/location: verified|required|optional ; kyc: verified|pending|rejected|required ;
    // subscription: active|expired
    val status: String = "",
    val critical: Boolean = false,
    // phone only — which waterfall rung verified it (sim|truecaller|sms|…), when present.
    val method: String? = null,
    @Json(name = "verified_at") val verifiedAt: String? = null,
    val help: String? = null,
)

@JsonClass(generateAdapter = true)
data class VerificationCapabilityDto(
    // customer: "browse" | "book" ; partner: "get_listed" | "accept_jobs"
    val key: String = "",
    val label: String = "",
    val allowed: Boolean = false,
    // the step keys that currently block this capability (empty when allowed).
    @Json(name = "blocked_by") val blockedBy: List<String> = emptyList(),
    val hint: String? = null,
)

@JsonClass(generateAdapter = true)
data class VerificationNextActionDto(
    // the step key to act on (kyc|subscription|location|phone|email) — drives the CTA route.
    val key: String = "",
    val title: String = "",
    val subtitle: String? = null,
    val cta: String? = null,
)

@JsonClass(generateAdapter = true)
data class VerificationDto(
    val steps: List<VerificationStepDto> = emptyList(),
    val capabilities: List<VerificationCapabilityDto> = emptyList(),
    @Json(name = "next_action") val nextAction: VerificationNextActionDto? = null,
    @Json(name = "can_book") val canBook: Boolean? = null,        // customer only
    @Json(name = "can_accept_jobs") val canAcceptJobs: Boolean? = null,  // partner only
)

// GET auth/verification returns the verification object directly (not wrapped); the
// alias keeps the API signature self-documenting alongside the other *Resp types.
typealias VerificationResp = VerificationDto

@JsonClass(generateAdapter = true)
data class MeResp(val profile: ProfileDto? = null)

// ── §758 Waterfall registration (email+password primary) ──────────────────────
// register/start → (email OTP) → phone SMS OTP → real account + token bundle.
@JsonClass(generateAdapter = true)
data class RegisterStartResp(
    @Json(name = "reg_token") val regToken: String,
    // "sent" (email OTP dispatched) | "pending" (send failed, retry) | "skipped" (no email gate)
    @Json(name = "email_verification") val emailVerification: String = "skipped",
    @Json(name = "email_verified") val emailVerified: Boolean = false,
    @Json(name = "phone_methods") val phoneMethods: List<String> = emptyList(),
    @Json(name = "expires_in") val expiresIn: Int = 0,
)

// Returned by register/email/verify AND register/phone/verify. When BOTH halves are
// verified the response ALSO carries the completed token bundle (accessToken != null).
@JsonClass(generateAdapter = true)
data class RegisterStepResp(
    @Json(name = "email_verified") val emailVerified: Boolean? = null,
    @Json(name = "phone_verified") val phoneVerified: Boolean? = null,
    val next: String? = null,
    @Json(name = "phone_methods") val phoneMethods: List<String>? = null,
    @Json(name = "email_verification") val emailVerification: String? = null,
    // completion bundle (present only when the pair is complete)
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "is_new_user") val isNewUser: Boolean = false,
    val profile: ProfileDto? = null,
)

// Typed body for register/phone/verify — `payload` is a nested object (e.g.
// {otp_token, code} for the SMS rung, {device_phone} for the SIM rung).
@JsonClass(generateAdapter = true)
data class RegisterPhoneVerifyReq(
    @Json(name = "reg_token") val regToken: String,
    val method: String,
    val payload: Map<String, String?> = emptyMap(),
)

// ── §763 Forgot / reset password (email OTP OR mobile SMS OTP) ─────────────────
// /auth/password/forgot returns the channel it used. For the SMS channel it also
// returns an otp_token the reset call must echo back; dev_otp is filled in only in
// dev/staging (no SMS provider) so the app can be tested end-to-end.
@JsonClass(generateAdapter = true)
data class PasswordForgotResp(
    val ok: Boolean = true,
    val channel: String = "email",                 // "email" | "sms"
    val message: String? = null,
    @Json(name = "otp_token") val otpToken: String? = null,   // sms channel only
    @Json(name = "expires_in") val expiresIn: Int = 0,
    @Json(name = "resend_after_s") val resendAfterS: Int = 0,
    @Json(name = "dev_otp") val devOtp: String? = null,
)

// /auth/password/reset signs the user straight back in (token bundle present) unless
// the account is disabled, in which case only {ok:true} comes back.
@JsonClass(generateAdapter = true)
data class PasswordResetResp(
    val ok: Boolean = true,
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    val profile: ProfileDto? = null,
)

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
    // §737 — service-detail enrichment: FAQ rows + a real portfolio gallery (the
    // backend filled these long-stubbed `[]` fields). Nullable for older backends.
    val faqs: List<FaqDto>? = null,
    val gallery: List<String>? = null,
    // §743/§747 — a PARTNER store's offering carries the products it uses (each tagged
    // sealed/sanitized/bulk) + a free-text hygiene note. Null/empty for generic catalog
    // services (which aren't tied to one partner). Surfaced on the service detail.
    val products: List<ProductDto>? = null,
    @Json(name = "hygiene_note") val hygieneNote: String? = null,
)

@JsonClass(generateAdapter = true)
data class CategoriesResp(val items: List<CategoryDto> = emptyList())

@JsonClass(generateAdapter = true)
data class ServicesResp(val items: List<ServiceDto> = emptyList())

// §726 — the partner's add-from dictionary: ALL active categories + services.
@JsonClass(generateAdapter = true)
data class PartnerCatalogResp(
    val categories: List<CategoryDto> = emptyList(),
    val services: List<ServiceDto> = emptyList(),
)

// ── §737 Packages (partner-curated bundles) + Deals/Featured + service FAQs ──
// PAYMENT-FREE: a package price is the informational SUM of the partner's own
// service prices; booking one expands into the existing cart → no new booking path.
@JsonClass(generateAdapter = true)
data class FaqDto(val q: String = "", val a: String = "")

@JsonClass(generateAdapter = true)
data class PackageItemDto(
    @Json(name = "service_id") val serviceId: Int = 0,
    val name: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    val qty: Int = 1,
    @Json(name = "unit_price_paise") val unitPricePaise: Long = 0,
    @Json(name = "line_total_paise") val lineTotalPaise: Long = 0,
    @Json(name = "duration_min") val durationMin: Int = 0,
    // false = partner currently doesn't offer this line (shown only in the partner editor).
    val available: Boolean = true,
)

@JsonClass(generateAdapter = true)
data class PackageDto(
    val id: Int,
    @Json(name = "partner_id") val partnerId: Int = 0,
    @Json(name = "partner_name") val partnerName: String? = null,
    @Json(name = "partner_avatar_url") val partnerAvatarUrl: String? = null,
    @Json(name = "partner_rating_avg") val partnerRatingAvg: Float = 0f,
    @Json(name = "partner_public_code") val partnerPublicCode: String? = null,
    val name: String = "",
    val description: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "is_featured") val isFeatured: Boolean = false,
    @Json(name = "featured_headline") val featuredHeadline: String? = null,
    val active: Boolean = true,
    val sort: Int = 0,
    @Json(name = "service_count") val serviceCount: Int = 0,
    // Informational only — the SUM of the partner's own service prices (no discount).
    @Json(name = "total_paise") val totalPaise: Long = 0,
    @Json(name = "total_duration_min") val totalDurationMin: Int = 0,
    @Json(name = "item_names") val itemNames: List<String> = emptyList(),
    val items: List<PackageItemDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class PackagesResp(
    val items: List<PackageDto> = emptyList(),
    @Json(name = "partner_id") val partnerId: Int? = null,
)

@JsonClass(generateAdapter = true)
data class FeaturedResp(
    val packages: List<PackageDto> = emptyList(),
    @Json(name = "new_partners") val newPartners: List<PartnerDto> = emptyList(),
)

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
    // §713 — partner business location block (GET /partner/profile returns this
    // alongside travel_radius_km). Null for customer-facing partner cards.
    val location: PartnerLocationDto? = null,
    // §743 — individual professional vs parlour (salon with staff experts), the
    // verified experts a customer can expect ("who is coming"), and the count.
    @Json(name = "partner_type") val partnerType: String? = null,
    val experts: List<ExpertDto>? = null,
    @Json(name = "expert_count") val expertCount: Int? = null,
)

// §743 — a parlour's staff member ("beauty expert"). Customer view = name/title/bio/
// photo + kyc_verified; the partner manager + admin also see the KYC document urls
// + status/reason + is_deleted (all nullable so the customer payload parses fine).
@JsonClass(generateAdapter = true)
data class ExpertDto(
    val id: Int,
    @Json(name = "partner_id") val partnerId: Int? = null,
    val name: String,
    val title: String? = null,
    val bio: String? = null,
    @Json(name = "photo_url") val photoUrl: String? = null,
    @Json(name = "experience_years") val experienceYears: Int = 0,
    @Json(name = "kyc_status") val kycStatus: String? = null,
    @Json(name = "kyc_verified") val kycVerified: Boolean = false,
    val active: Boolean = true,
    @Json(name = "kyc_selfie_url") val kycSelfieUrl: String? = null,
    @Json(name = "kyc_id_doc_url") val kycIdDocUrl: String? = null,
    @Json(name = "kyc_reason") val kycReason: String? = null,
    @Json(name = "is_deleted") val isDeleted: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class ExpertsResp(
    val items: List<ExpertDto> = emptyList(),
    @Json(name = "partner_type") val partnerType: String? = null,
)

@JsonClass(generateAdapter = true)
data class ExpertReq(
    val name: String,
    val title: String? = null,
    val bio: String? = null,
    @Json(name = "photo_url") val photoUrl: String? = null,
    @Json(name = "experience_years") val experienceYears: Int? = null,
    @Json(name = "kyc_selfie_url") val kycSelfieUrl: String? = null,
    @Json(name = "kyc_id_doc_url") val kycIdDocUrl: String? = null,
)

// §743 — a structured product line on an offering, with an honest hygiene tag:
// sealed (single-use, opened in front) | sanitized (reusable, sterilised) | bulk
// (from sealed bulk stock, e.g. wax/creams).
@JsonClass(generateAdapter = true)
data class ProductDto(
    val name: String,
    val hygiene: String? = null,
    val note: String? = null,
)

// §743 — admin-managed sample professional description (partner picks one).
@JsonClass(generateAdapter = true)
data class DescriptionTemplateDto(
    val id: Int,
    val text: String,
    val category: String? = null,
    val tone: String? = null,
)

@JsonClass(generateAdapter = true)
data class DescriptionSuggestionsResp(val items: List<DescriptionTemplateDto> = emptyList())

// §743 — can this customer chat with this partner? (chat-after-booking gate)
@JsonClass(generateAdapter = true)
data class CanChatResp(
    @Json(name = "can_chat") val canChat: Boolean = true,
    @Json(name = "requires_booking") val requiresBooking: Boolean = false,
    @Json(name = "has_booking") val hasBooking: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class PartnersResp(
    val items: List<PartnerDto> = emptyList(),
    // §713 — discovery geofencing: the backend returns requires_location=true
    // (with an empty items list) when the customer's location is unknown, so the
    // app can prompt the customer to set their location instead of showing "no
    // professionals". Defaults false for older payloads.
    @Json(name = "requires_location") val requiresLocation: Boolean = false,
)

// ── §713 Partner business location (service-area geofence) ───────────────────
// GET /partner/location → this shape; PUT /partner/location body = PartnerLocationReq
// (radius clamped server-side to ≤ radius_max_km). PATCH /partner/profile + the
// partner profile GET also surface the same block.
@JsonClass(generateAdapter = true)
data class PartnerLocationDto(
    val lat: Double? = null,
    val lon: Double? = null,
    val address: String? = null,
    @Json(name = "radius_km") val radiusKm: Double? = null,
    @Json(name = "radius_max_km") val radiusMaxKm: Double = 10.0,
    @Json(name = "has_location") val hasLocation: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class PartnerLocationReq(
    val lat: Double,
    val lon: Double,
    val address: String? = null,
    @Json(name = "radius_km") val radiusKm: Double? = null,
)

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

// ── Geo gateway app-config (§690 — GET /api/geo/app-config?app=vedadrop) ────
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
    val app: String = "vedadrop",
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
    // §710 #1 — id/line1 are nullable+defaulted: a booking's snapshot address (or any
    // address serialized without these) used to throw a Moshi JsonDataException →
    // crash the partner queue/detail. Now it parses; callers default safely.
    val id: Int? = null,
    val label: String? = null,
    val line1: String? = null,
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
    // §722 — which partner serves this line (multi-partner cart).
    @Json(name = "partner_id") val partnerId: Int? = null,
    @Json(name = "partner_name") val partnerName: String? = null,
)

@JsonClass(generateAdapter = true)
data class CartResp(
    @Json(name = "partner_id") val partnerId: Int? = null,
    @Json(name = "partner_name") val partnerName: String? = null,
    val items: List<CartItemDto> = emptyList(),
    @Json(name = "subtotal_paise") val subtotalPaise: Long = 0,
    val count: Int = 0,
    // §722 — per-partner groups (multi-partner cart) + distinct partner count.
    val groups: List<CartGroupDto> = emptyList(),
    @Json(name = "partner_count") val partnerCount: Int = 0,
)

@JsonClass(generateAdapter = true)
data class CartGroupDto(
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
    // §722 — quote ONLY this partner's cart lines (multi-partner per-group quote).
    @Json(name = "partner_id") val partnerId: Int? = null,
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

// §722 — multi-partner combination checkout: one quote per partner-group → /combo.
@JsonClass(generateAdapter = true)
data class ComboReq(
    @Json(name = "quote_ids") val quoteIds: List<String>,
    @Json(name = "customer_notes") val customerNotes: String? = null,
    @Json(name = "booking_source") val bookingSource: String? = "combo",
    @Json(name = "customer_share_number") val customerShareNumber: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class ComboResp(
    @Json(name = "combo_group_id") val comboGroupId: String? = null,
    val count: Int = 0,
    val bookings: List<BookingDto> = emptyList(),
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
    // §729 (parity C2) — FLEXIBLE arrival window. When true AND the backend's
    // `flexible_slots` flag is ON, the chosen slot_start is treated as the WINDOW
    // START and the arrival window is [slot_start, slot_start + flex_window_min].
    // Defaulted false so the exact-slot path serialises byte-identically (the
    // existing booking contract is unchanged — opt-in only).
    val flexible: Boolean = false,
    // §743 — the specific parlour expert chosen ("who is coming"). Null = none.
    @Json(name = "expert_id") val expertId: Int? = null,
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
    // §743 — the chosen parlour expert ("who is coming").
    @Json(name = "expert_id") val expertId: Int? = null,
    @Json(name = "expert_name") val expertName: String? = null,
    @Json(name = "expert_photo_url") val expertPhotoUrl: String? = null,
    @Json(name = "start_otp") val startOtp: String? = null,
    // §728 (parity C1) — the partner's live start-selfie (base64 data:/resolved URL),
    // surfaced by ser_booking to whoever can see the booking (customer/partner/admin)
    // as transparency proof. Null until the partner starts the job with a selfie.
    @Json(name = "start_selfie_url") val startSelfieUrl: String? = null,
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
    // §710 P1-5 — server-authoritative review state so the customer's "Leave a review"
    // button hides + the rating shows after submit, surviving an app refresh.
    @Json(name = "reviewed") val reviewed: Boolean = false,
    @Json(name = "review_rating") val reviewRating: Int? = null,
    @Json(name = "review_comment") val reviewComment: String? = null,
    // §710 #5/#6 — captured fields the app used to drop: the customer's notes + the
    // FULL multi-service line items (a multi-service cart showed only the primary service).
    @Json(name = "customer_notes") val customerNotes: String? = null,
    @Json(name = "gender_preference") val genderPreference: String? = null,
    // §722 req-2 — distance (km) from the partner's base location to the customer's
    // service location, computed server-side for the partner/admin viewer.
    @Json(name = "distance_km") val distanceKm: Double? = null,
    // §723 dual rating — the partner's rating OF the customer (detail view): hides the
    // "rate customer" prompt after submit + shows the customer the rating she received.
    @Json(name = "customer_rated") val customerRated: Boolean = false,
    @Json(name = "customer_rating") val customerRating: Int? = null,
    @Json(name = "customer_rating_comment") val customerRatingComment: String? = null,
    // §729 (parity C2) — FLEXIBLE arrival window state, surfaced by ser_booking. When
    // is_flexible the customer requested an arrival WINDOW; window_end is the ISO end of
    // that window (slot_start + flex_window_min). Defaulted so an exact-slot / older
    // payload parses cleanly (is_flexible=false, window_end=null).
    @Json(name = "is_flexible") val isFlexible: Boolean = false,
    @Json(name = "window_end") val windowEnd: String? = null,
    val items: List<BookingItemDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class BookingItemDto(
    @Json(name = "service_id") val serviceId: Int? = null,
    val name: String? = null,
    val qty: Int = 1,
    @Json(name = "unit_price_paise") val unitPricePaise: Long = 0,
    @Json(name = "line_total_paise") val lineTotalPaise: Long = 0,
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
    // §722/§11 — complete details before claiming: how many services + how far the job is.
    @Json(name = "item_count") val itemCount: Int = 1,
    @Json(name = "distance_km") val distanceKm: Double? = null,
    @Json(name = "eta_min") val etaMin: Int? = null,
    // §725 Batch-B — the booking was raised as URGENT (slot inside the lead-time window,
    // urgent_pool ON → pushed to the Flow-B pool). Drives the high-alert alarm.
    @Json(name = "is_urgent") val isUrgent: Boolean = false,
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
    // §725 Batch-B — top-level urgent flag (mirrors booking.is_urgent) if the backend
    // surfaces it on the offer; falls back to booking.isUrgent / a short-lead heuristic.
    @Json(name = "urgent") val urgent: Boolean = false,
    val booking: ReassignmentOfferBookingDto? = null,
)

@JsonClass(generateAdapter = true)
data class OffersResp(
    val items: List<ReassignmentOfferDto> = emptyList(),
    // §710 #27 — why the board is empty (feature_off / subscription_inactive /
    // kyc_required / no_jobs), so the partner sees a real reason + CTA, not a blank.
    @Json(name = "empty_reason") val emptyReason: String? = null,
    @Json(name = "empty_message") val emptyMessage: String? = null,
)

/**
 * §725 Batch-B — is this offer URGENT (deserves the high-alert alarm)?
 *   1. an explicit backend flag (offer.urgent or booking.is_urgent), else
 *   2. a fallback heuristic: a broadcast offer whose slot starts within the next
 *      ~2h (the lead-time window) — i.e. a "need someone now" job. Past/unknown
 *      slot times are NOT treated as urgent so stale offers stay quiet.
 */
fun ReassignmentOfferDto.isUrgentOffer(leadMinutes: Long = 120): Boolean {
    if (urgent || booking?.isUrgent == true) return true
    val startIso = booking?.slotStart ?: return false
    val start = runCatching {
        // Accept both "...T10:00:00Z" / "...T10:00:00" and a bare local date-time.
        java.time.OffsetDateTime.parse(startIso).toInstant()
    }.recoverCatching {
        java.time.LocalDateTime.parse(startIso.removeSuffix("Z"))
            .atZone(java.time.ZoneId.systemDefault()).toInstant()
    }.getOrNull() ?: return false
    val now = java.time.Instant.now()
    val lead = java.time.Duration.ofMinutes(leadMinutes)
    // Urgent = starts in the FUTURE but inside the lead-time window.
    return start.isAfter(now) && start.isBefore(now.plus(lead))
}

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
    // §747 — structured per-axis ratings (each 1..5). Sent alongside the overall
    // `rating`; backend persists them in dedicated columns instead of a text prefix.
    @Json(name = "rating_skill") val ratingSkill: Int? = null,
    @Json(name = "rating_hygiene") val ratingHygiene: Int? = null,
    @Json(name = "rating_products") val ratingProducts: Int? = null,
)

// §723 — the partner's rating OF the customer (POST partner/bookings/{id}/rate-customer).
data class RateCustomerReq(
    val rating: Int,
    val comment: String? = null,
)

// §747 — per-axis breakdown (skill / hygiene / products), each 1..5 or null.
@JsonClass(generateAdapter = true)
data class ReviewAxesDto(
    val skill: Int? = null,
    val hygiene: Int? = null,
    val products: Int? = null,
)

@JsonClass(generateAdapter = true)
data class ReviewDto(
    val id: Int,
    val rating: Int = 0,
    val comment: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    // §747 — backend now returns a clean comment (axis prefix stripped), the
    // structured axes object, and a privacy-abbreviated reviewer name ("Priya S.").
    val axes: ReviewAxesDto? = null,
    @Json(name = "reviewer_name") val reviewerName: String? = null,
    @Json(name = "image_urls") val imageUrls: List<String>? = null,
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
    // §710 #4 — the backend returns the FULL favourited partner/service objects; the
    // DTO dropped them, so the Favourites screen (which resolves against the in-memory
    // catalog) showed empty when a favourite wasn't in the current discovery result.
    val partners: List<PartnerDto> = emptyList(),
    val services: List<ServiceDto> = emptyList(),
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
    // §725 — three guided face-verification photos (base64 JPEG data URLs) captured
    // by FaceCaptureFlow: front view, then a left + right head-turn. selfie_upload_id
    // is kept = the front photo for backward compatibility with older backends.
    @Json(name = "selfie_front_url") val selfieFrontUrl: String? = null,
    @Json(name = "selfie_left_url") val selfieLeftUrl: String? = null,
    @Json(name = "selfie_right_url") val selfieRightUrl: String? = null,
    // §704 — the legal name printed on her ID; the admin locks the display name to it.
    @Json(name = "legal_name") val legalName: String? = null,
    // §713 — when geofence enforcement is ON the backend REQUIRES a base location
    // at KYC submit (400 LOCATION_REQUIRED otherwise), so the KYC screen collects
    // it before submitting. travel_radius_km is clamped server-side to ≤10km.
    @Json(name = "base_lat") val baseLat: Double? = null,
    @Json(name = "base_lon") val baseLon: Double? = null,
    @Json(name = "base_address") val baseAddress: String? = null,
    @Json(name = "travel_radius_km") val travelRadiusKm: Double? = null,
)

@JsonClass(generateAdapter = true)
data class KycFields(
    @Json(name = "aadhaar_no") val aadhaarNo: String? = null,
    @Json(name = "pan_no") val panNo: String? = null,
    @Json(name = "legal_name") val legalName: String? = null,
)

@JsonClass(generateAdapter = true)
data class KycStatusResp(
    val status: String = "not_started",
    val reason: String? = null,
    // §708 — last-saved KYC fields so the form can pre-fill on return (so a
    // resubmit/edit doesn't start blank and "look unsaved").
    val fields: KycFields? = null,
    // §704 — once approved, the verified (locked) name.
    @Json(name = "name_locked") val nameLocked: Boolean = false,
    @Json(name = "verified_name") val verifiedName: String? = null,
    // §714 kyc-img-resp-dto-4 — the submitted selfie/ID images (resolved URLs / data:
    // URLs) so a returning partner can see which documents are on file.
    @Json(name = "selfie_url") val selfieUrl: String? = null,
    @Json(name = "document_urls") val documentUrls: List<String> = emptyList(),
    // §725 — the three captured face-verification photos on file (resolved URLs).
    @Json(name = "selfie_front_url") val selfieFrontUrl: String? = null,
    @Json(name = "selfie_left_url") val selfieLeftUrl: String? = null,
    @Json(name = "selfie_right_url") val selfieRightUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class PartnerServiceDto(
    val id: Int,
    @Json(name = "service_id") val serviceId: Int,
    @Json(name = "price_paise") val pricePaise: Long = 0,
    val active: Boolean = true,
    val name: String? = null,
    // §714 pda-products-used-1 — round-trip the partner's "products used / seal notes".
    @Json(name = "products_used") val productsUsed: String? = null,
    // §742 — the catalog image plus the partner's OWN gallery (http(s) or base64
    // data: URLs), and the admin-approval state so the app can badge the listing.
    @Json(name = "image_url") val imageUrl: String? = null,
    val images: List<String>? = null,
    @Json(name = "approval_status") val approvalStatus: String? = null,
    @Json(name = "approval_reason") val approvalReason: String? = null,
    // §743 — per-offering time + discount + structured products + hygiene note.
    @Json(name = "duration_min") val durationMin: Int? = null,
    @Json(name = "catalog_duration_min") val catalogDurationMin: Int? = null,
    @Json(name = "discount_percent") val discountPercent: Int = 0,
    @Json(name = "actual_price_paise") val actualPricePaise: Long? = null,
    @Json(name = "final_price_paise") val finalPricePaise: Long? = null,
    val products: List<ProductDto>? = null,
    @Json(name = "hygiene_note") val hygieneNote: String? = null,
)

@JsonClass(generateAdapter = true)
data class PartnerServicesResp(val items: List<PartnerServiceDto> = emptyList())

// §747 — a partner-authored custom service (not in the catalog dictionary). The
// backend creates the catalog row + the partner's offering in one call; the offering
// enters admin approval (active=false) until reviewed, exactly like an edited listing.
@JsonClass(generateAdapter = true)
data class CustomServiceReq(
    val name: String,
    @Json(name = "category_id") val categoryId: Int,
    @Json(name = "price_paise") val pricePaise: Long,
    @Json(name = "duration_min") val durationMin: Int? = null,
    val description: String? = null,
    @Json(name = "products_used") val productsUsed: String? = null,
)

@JsonClass(generateAdapter = true)
data class PartnerServiceReq(
    @Json(name = "service_id") val serviceId: Int,
    @Json(name = "price_paise") val pricePaise: Long,
    val active: Boolean = true,
    // §714 pda-products-used-1 — send the products/seal notes so they persist.
    @Json(name = "products_used") val productsUsed: String? = null,
    // §742 — the partner's service images (http(s) or base64 data: URLs). Sending a
    // non-null list re-enters admin approval on the backend.
    val images: List<String>? = null,
    // §743 — per-offering time + discount + structured products + hygiene note.
    @Json(name = "discount_percent") val discountPercent: Int? = null,
    @Json(name = "duration_min") val durationMin: Int? = null,
    val products: List<ProductDto>? = null,
    @Json(name = "hygiene_note") val hygieneNote: String? = null,
)

@JsonClass(generateAdapter = true)
data class StatusReq(
    val to: String,
    @Json(name = "start_otp") val startOtp: String? = null,
    @Json(name = "proof_upload_ids") val proofUploadIds: List<String>? = null,
    // §728 (parity C1) — the partner's live start-selfie proof, captured at job start
    // (front cam, on-device face-detected liveness) and sent WITH the start-OTP. A
    // base64 `data:` URL; the backend (routes_partner StatusBody.start_selfie_url)
    // stores it on the booking only after the OTP validates. Defaulted null so an
    // ordinary "on_the_way"/"arrived"/"completed" transition omits it cleanly.
    @Json(name = "start_selfie_url") val startSelfieUrl: String? = null,
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
    // §710 #18 — real online/away state (so the dashboard toggle seeds from the server
    // instead of a hardcoded default after restart).
    @Json(name = "is_online") val isOnline: Boolean = true,
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
    @Json(name = "accept_rate") val acceptRate: Float? = null,
    val accepted: Int = 0,
    val rejected: Int = 0,
    @Json(name = "avg_response_min") val avgResponseMin: Float? = null,
    @Json(name = "profile_views_total") val profileViewsTotal: Int = 0,
    @Json(name = "profile_views_30d") val profileViews30d: Int = 0,
    @Json(name = "profile_views_trend") val profileViewsTrend: List<ProfileViewPointDto> = emptyList(),
    @Json(name = "rating_avg") val ratingAvg: Float? = null,
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
// §709 deep-link: the backend's `data` object carries the routing target
// (booking_id / complaint_id / offer_id …). We map it with a small typed
// adapter class — NOT a Map<String,Any> — so a notification tap can navigate
// to the resource it refers to. Codegen handles the nested class; unknown
// keys are ignored by Moshi.
@JsonClass(generateAdapter = true)
data class NotificationData(
    @Json(name = "booking_id") val bookingId: Int? = null,
    @Json(name = "complaint_id") val complaintId: Int? = null,
    @Json(name = "offer_id") val offerId: Int? = null,
    @Json(name = "service_id") val serviceId: Int? = null,
    @Json(name = "customer_id") val customerId: Int? = null,
    val status: String? = null,
    val mode: String? = null,
)

@JsonClass(generateAdapter = true)
data class NotificationDto(
    val id: Int,
    val type: String? = null,
    val title: String? = null,
    val body: String? = null,
    val read: Boolean = false,
    @Json(name = "created_at") val createdAt: String? = null,
    val data: NotificationData? = null,
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
    // §729 (parity C2) — FLEXIBLE arrival window for an OPEN (Flow-B) booking. Same
    // semantics as BookingCreateReq.flexible; defaulted false so the exact-slot pool
    // path is unchanged.
    val flexible: Boolean = false,
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
    // §746 — backend returns the FULL list ("emergency_numbers") + women_helpline,
    // not a single "emergency_number". The old singular field silently never parsed
    // (name mismatch) and dropped both, so the SOS toast had no real dial number.
    @Json(name = "emergency_numbers") val emergencyNumbers: List<String> = listOf("112", "1091"),
    @Json(name = "women_helpline") val womenHelpline: String? = null,
    val message: String? = null,
)

// ── §746 Razorpay subscription checkout/verify ───────────────────────────────
@JsonClass(generateAdapter = true)
data class SubscriptionCheckoutResp(
    @Json(name = "order_id") val orderId: String,
    @Json(name = "amount_paise") val amountPaise: Long = 9900,
    val currency: String = "INR",
    @Json(name = "key_id") val keyId: String,
    val name: String = "Veda Drop",
    val description: String = "",
    val prefill: Map<String, String>? = null,
)

@JsonClass(generateAdapter = true)
data class SubscriptionVerifyReq(
    @Json(name = "razorpay_order_id") val razorpayOrderId: String,
    @Json(name = "razorpay_payment_id") val razorpayPaymentId: String,
    @Json(name = "razorpay_signature") val razorpaySignature: String,
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
    // §756 — AdMob ads config (role-trimmed by the server). When ads are off or this
    // role isn't allowed, the server sends `{"enabled": false}`, so this defaults to a
    // disabled config and the app shows nothing.
    val ads: AdsConfig = AdsConfig(),
)

// §756 — the role-trimmed, single-app ads slice the backend serves at /config.ads.
@JsonClass(generateAdapter = true)
data class AdsConfig(
    val enabled: Boolean = false,
    val provider: String = "admob",
    @Json(name = "test_mode") val testMode: Boolean = true,
    @Json(name = "app_id") val appId: String = "",
    @Json(name = "ad_units") val adUnits: Map<String, String> = emptyMap(),
    val placements: Map<String, AdsPlacement> = emptyMap(),
    val frequency: AdsFrequency = AdsFrequency(),
)

@JsonClass(generateAdapter = true)
data class AdsPlacement(
    val enabled: Boolean = false,
    val format: String = "banner",
)

@JsonClass(generateAdapter = true)
data class AdsFrequency(
    @Json(name = "interstitial_every_n_actions") val interstitialEveryNActions: Int = 3,
    @Json(name = "min_interval_sec") val minIntervalSec: Int = 60,
    @Json(name = "max_per_session") val maxPerSession: Int = 5,
)

// §710 P0-8 — a partner's own menu with their ACTUAL per-service price, so the
// partner-store keys each row off the real number instead of one shared from-price.
@JsonClass(generateAdapter = true)
data class PartnerPricedServiceDto(
    @Json(name = "service_id") val serviceId: Int? = null,
    @Json(name = "price_paise") val pricePaise: Long? = null,
    val name: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "duration_min") val durationMin: Int? = null,
    // §743 — actual (pre-discount) vs final (discounted) price + % + structured
    // products (hygiene tags) + free-text hygiene note.
    @Json(name = "actual_price_paise") val actualPricePaise: Long? = null,
    @Json(name = "final_price_paise") val finalPricePaise: Long? = null,
    @Json(name = "discount_percent") val discountPercent: Int = 0,
    val products: List<ProductDto>? = null,
    @Json(name = "hygiene_note") val hygieneNote: String? = null,
)

@JsonClass(generateAdapter = true)
data class PartnerPricedServicesResp(
    val items: List<PartnerPricedServiceDto> = emptyList(),
)
