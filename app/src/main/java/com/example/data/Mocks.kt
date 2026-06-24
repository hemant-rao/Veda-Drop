package com.example.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class Category(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val colorHex: String
)

data class Service(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String,
    val pricePaise: Long,
    val durationMin: Int,
    val rating: Float,
    val reviewsCount: Int,
    val inclusions: List<String>,
    val faqs: List<Pair<String, String>>,
    val imageUrl: String,
    // §690 — partner-set price RANGE (null when unknown / single price). The app
    // shows "₹X – ₹Y" instead of a single price; `partnerCount` = how many
    // partners offer this service right now.
    val priceMinPaise: Long? = null,
    val priceMaxPaise: Long? = null,
    val partnerCount: Int = 0,
    // §737 — real "see the work" gallery (portfolio images of partners who offer
    // this service). Trailing optional → no positional call-site breakage.
    val gallery: List<String> = emptyList(),
)

/**
 * §690 — human price label for a service. We never set the price; partners do, so
 * we render the RANGE of what offering partners charge:
 *   • both min & max present and different → "₹250 – ₹400"
 *   • single price (min==max, or only one side) → "₹250"
 *   • nothing known → fall back to the base price, else "Price on request".
 * Whole rupees (paise/100), no decimals. Never throws on null.
 */
fun Service.priceLabel(): String {
    fun r(paise: Long): String = "₹${paise / 100}"
    val lo = priceMinPaise
    val hi = priceMaxPaise
    return when {
        lo != null && hi != null && hi > lo -> "${r(lo)} – ${r(hi)}"
        lo != null -> r(lo)
        hi != null -> r(hi)
        pricePaise > 0 -> r(pricePaise)
        else -> "Price on request"
    }
}

// §743 — a parlour's staff member ("beauty expert"), shown to the customer on the
// parlour profile ("who is coming"). Customer view: name/title/bio/photo + verified.
data class Expert(
    val id: Int,
    val name: String,
    val title: String,
    val bio: String,
    val photoUrl: String,
    val experienceYears: Int,
    val kycVerified: Boolean,
)

data class Partner(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val rating: Float,
    val reviewsCount: Int,
    // §743 — real completed-jobs count (distinct from reviewsCount; the founder asked
    // for ONE consistent number under the name + in the feedback section).
    val completedJobs: Int = 0,
    // §743 — individual professional vs parlour, + the parlour's verified experts.
    val partnerType: String = "individual",
    val experts: List<Expert> = emptyList(),
    val distanceKm: Double,
    val etaMin: Int,
    val experienceYears: Int,
    val description: String,
    val categories: List<String>,
    val servicesOffered: List<String>,
    val portfolioUrls: List<String>,
    val recentReviews: List<Pair<String, Float>>,
    val fromPricePaise: Long = 0,
    // §701 — real verification + profile detail (backend-provided; empty/neutral by default).
    val kycStatus: String = "not_started",
    val certifications: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    // Real service radius (km) — drives the honest "Serves within N km" line.
    val travelRadiusKm: Double = 0.0,
    // §722 — the partner's minimum booking value (shown on her card so the customer
    // knows the floor before booking) + her public code ("Your ID").
    val minimumOrderPaise: Long = 0,
    val publicCode: String = "",
)

/**
 * Live catalog cache (no mock data). Categories, services and partners are
 * filled purely from the backend by [com.example.data.VedaDropRepository.hydrateCatalog].
 * The lists are Compose snapshot state, so every screen that reads them
 * recomposes automatically once the server data arrives — and they start EMPTY,
 * so the UI shows proper empty states until partners onboard and add services.
 */
object VedaDropDataSource {
    var categories: List<Category> by mutableStateOf(emptyList())
    var services: List<Service> by mutableStateOf(emptyList())
    var partners: List<Partner> by mutableStateOf(emptyList())
    // §713 — set true when discovery returned requires_location (the customer's
    // location is unknown), so the partner-select screen can prompt the customer
    // to set their location instead of showing an empty "no professionals" state.
    var partnersRequireLocation: Boolean by mutableStateOf(false)
}
