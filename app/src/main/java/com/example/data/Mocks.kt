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
    val imageUrl: String
)

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
    val fromPricePaise: Long = 0
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
