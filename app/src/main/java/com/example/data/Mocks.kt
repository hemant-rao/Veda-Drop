package com.example.data

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
    val recentReviews: List<Pair<String, Float>>
)

object GlamMockDataSource {
    // These are now HYDRATED from the backend at startup (see GlamGoRepository
    // .hydrateCatalog). The literals below remain as an offline fallback so the
    // UI never renders empty before the first catalog fetch returns.
    var categories: List<Category> = listOf(
        Category("cat_salon", "Salon", "Haircuts, Hair Styling, Hair Spa", "content_cut", "#E91E63"),
        Category("cat_beauty", "Beauty & Waxing", "Facials, Cleanups, Pedicure", "face", "#9C27B0"),
        Category("cat_makeup", "Makeup", "Bridal, Party Makeup, Mehndi", "brush", "#3F51B5"),
        Category("cat_massage", "Massage / Therapy", "Relaxation, Deep Tissue, Ayurvedic", "spa", "#4CAF50")
    )

    var services: List<Service> = listOf(
        // Salon Row
        Service(
            id = "srv_001",
            categoryId = "cat_salon",
            name = "Luxury Haircut & Beard Grooming",
            description = "Get a precision haircut styled to your face structure coupled with charcoal beard wash, dynamic steam trimmer shaping, and relaxing neck pressure massage.",
            pricePaise = 49900,
            durationMin = 45,
            rating = 4.85f,
            reviewsCount = 1250,
            inclusions = listOf("Consultation", "Hair wash & styling", "Beard alignment & steam trim", "Argan oil massage"),
            faqs = listOf(
                "Do we need a power connection?" to "Yes, our stylists carry professional trimmers and blow dryers which require an electric socket.",
                "Is clean-up included?" to "We sweep and organize the entire workstation area after completion, leaving no mess behind."
            ),
            imageUrl = "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?auto=format&fit=crop&q=80&w=400"
        ),
        Service(
            id = "srv_002",
            categoryId = "cat_salon",
            name = "Intense Nourishing Hair Spa",
            description = "Reviving therapy for dry/damaged locks utilizing high-concentration keratin cream, warm ozone misty steam, and pressure-point follicle head massage.",
            pricePaise = 89900,
            durationMin = 60,
            rating = 4.90f,
            reviewsCount = 840,
            inclusions = listOf("Follicle wash", "Keratin cream layer application", "Ozone mist steaming", "Blow dry finish"),
            faqs = listOf(
                "Is it suitable for colored hair?" to "Absolutely, our products are sulfate-free and safely suited for treated and colored hair locks."
            ),
            imageUrl = "https://images.unsplash.com/photo-1562322140-8baeececf3df?auto=format&fit=crop&q=80&w=400"
        ),

        // Beauty
        Service(
            id = "srv_003",
            categoryId = "cat_beauty",
            name = "M3 Premium Brightening Facial",
            description = "Multi-step premium glow treatment with active Vitamin C serums, hydra gel application, ultrasound-pulse massager absorption, and peel-off algae rubber mask.",
            pricePaise = 129900,
            durationMin = 75,
            rating = 4.92f,
            reviewsCount = 2100,
            inclusions = listOf("Double cleanse & scrub", "Serum massage", "Ultrasound-pulse penetration", "Peel-off Algae formulation Mask"),
            faqs = listOf(
                "How long does the glow last?" to "Generally, the intense radiance visible stays active for 15-20 days when basic care is taken."
            ),
            imageUrl = "https://images.unsplash.com/photo-1512290923902-8a9f81dc236c?auto=format&fit=crop&q=80&w=400"
        ),
        Service(
            id = "srv_004",
            categoryId = "cat_beauty",
            name = "Anti-Tan Clay Cleanup",
            description = "Erase sun exposure damage with cooling organic mint scrub, targeted blackhead/whitehead manual extraction, and calming kaolin herb clay modeling pack.",
            pricePaise = 59900,
            durationMin = 40,
            rating = 4.79f,
            reviewsCount = 1450,
            inclusions = listOf("Steaming & scrubbing", "Manual whitehead extraction", "Kaolin cooling clay pack", "Tone & moisturize"),
            faqs = listOf(
                "Does it hurt?" to "The manual extraction may cause extremely mild momentary irritation but we soothe it immediately with tea-tree toner."
            ),
            imageUrl = "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?auto=format&fit=crop&q=80&w=400"
        ),

        // Makeup
        Service(
            id = "srv_005",
            categoryId = "cat_makeup",
            name = "Signature Party Glam Makeup",
            description = "High-definition flawless party makeup executed by premium artistry products (MAC, Kryolan, Huda). Includes custom eyelash application and standard hair braiding/setting.",
            pricePaise = 249900,
            durationMin = 90,
            rating = 4.88f,
            reviewsCount = 312,
            inclusions = listOf("HD foundation base", "Contour and shadow", "Premium 3D eyelashes", "Hair setting / braiding"),
            faqs = listOf(
                "Are products hygienic?" to "Yes, our makeup artists sanitize all brushes and palettes using 99% isopropyl alcohol before each session."
            ),
            imageUrl = "https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?auto=format&fit=crop&q=80&w=400"
        ),

        // Massage
        Service(
            id = "srv_007",
            categoryId = "cat_massage",
            name = "Deep Tissue Healing Massage",
            description = "Release structural core blockages and chronic muscular tension with systematic high-pressure strokes, warmed sesame oils, and targeted trigger-point therapy.",
            pricePaise = 149900,
            durationMin = 90,
            rating = 4.95f,
            reviewsCount = 980,
            inclusions = listOf("Sesame-herb oil", "Deep muscle kneading", "Warm compress wipe", "Herbal massage balm apply"),
            faqs = listOf(
                "Is a bed needed?" to "No, our therapists bring portable lightweight professional massage beds and fresh sanitized disposable linen."
            ),
            imageUrl = "https://images.unsplash.com/photo-1600334089648-b0d9d3028eb2?auto=format&fit=crop&q=80&w=400"
        )
    )

    var partners: List<Partner> = listOf(
        Partner(
            id = "part_001",
            name = "Meera Sen",
            avatarUrl = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&q=80&w=200",
            rating = 4.92f,
            reviewsCount = 342,
            distanceKm = 1.8,
            etaMin = 15,
            experienceYears = 5,
            description = "Hi, I am Meera, a licensed beautician and hair designer specializing in organic skincare treatments and bridal makeup styles. I love making my clients feel confident and relaxed.",
            categories = listOf("Salon", "Beauty & Waxing", "Makeup"),
            servicesOffered = listOf("srv_001", "srv_002", "srv_003", "srv_004", "srv_005"),
            portfolioUrls = listOf(
                "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?auto=format&fit=crop&q=80&w=200",
                "https://images.unsplash.com/photo-1562322140-8baeececf3df?auto=format&fit=crop&q=80&w=200"
            ),
            recentReviews = listOf(
                "Extremely professional and the M3 facial was so soothing!" to 5f,
                "Loved the haircut! Very precise and exactly what I needed." to 4.8f
            )
        ),
        Partner(
            id = "part_002",
            name = "Rohan Verma",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200",
            rating = 4.84f,
            reviewsCount = 512,
            distanceKm = 2.5,
            etaMin = 22,
            experienceYears = 7,
            description = "Rohan style. 7 years experience in modern men's salon styling, texturizing trims, skin-fade cuts, and hot towel beard luxury grooming. High speed, high quality.",
            categories = listOf("Salon"),
            servicesOffered = listOf("srv_001", "srv_002"),
            portfolioUrls = listOf(
                "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?auto=format&fit=crop&q=80&w=200"
            ),
            recentReviews = listOf(
                "Great beard trim, the steam massage feels fantastic." to 5f,
                "On time and clean work." to 4.5f
            )
        ),
        Partner(
            id = "part_004",
            name = "David D'Souza",
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=200",
            rating = 4.96f,
            reviewsCount = 670,
            distanceKm = 3.2,
            etaMin = 30,
            experienceYears = 8,
            description = "Expert wellness masseer. Graduate of Kerala Ayurvedic Academy. Specialized in restorative physiotherapy, trigger points release, deep tissue kneading, and complete fatigue extraction.",
            categories = listOf("Massage / Therapy"),
            servicesOffered = listOf("srv_007"),
            portfolioUrls = listOf(
                "https://images.unsplash.com/photo-1600334089648-b0d9d3028eb2?auto=format&fit=crop&q=80&w=200"
            ),
            recentReviews = listOf(
                "Unbelievable deep tissue massage. My shoulder pain completely vanished." to 5f,
                "Very respectful, sets up table and linen seamlessly. Real therapeutic mastery." to 5f
            )
        )
    )
}
