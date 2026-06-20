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

data class Partner(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val rating: Float,
    val reviewsCount: Int,
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
)

/**
 * Live catalog cache (no mock data). Categories, services and partners are
 * filled purely from the backend by [com.example.data.NikhatGlowRepository.hydrateCatalog].
 * The lists are Compose snapshot state, so every screen that reads them
 * recomposes automatically once the server data arrives — and they start EMPTY,
 * so the UI shows proper empty states until partners onboard and add services.
 */
object NikhatGlowDataSource {
    var categories: List<Category> by mutableStateOf(emptyList())
    var services: List<Service> by mutableStateOf(emptyList())
    var partners: List<Partner> by mutableStateOf(emptyList())
}
