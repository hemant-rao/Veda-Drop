package com.example.data.remote

import com.example.data.AddressEntity
import com.example.data.BookingEntity
import com.example.data.Category
import com.example.data.ChatMessageEntity
import com.example.data.ComplaintEntity
import com.example.data.Partner
import com.example.data.Service
import com.example.data.UserEntity
import com.example.data.WalletTransactionEntity

/**
 * Maps wire DTOs to the existing in-app model types so the (untouched) Compose
 * screens keep working against server data. Server ids are Int; app models use
 * String ids — we stringify consistently so every reference round-trips.
 */
object Mappers {

    // slug → the icon-name / colour the original mock used, so category tiles
    // keep their look. Unknown slugs get sensible beauty defaults.
    private val categoryIcon = mapOf(
        "salon" to ("content_cut" to "#E84A78"),
        "beauty" to ("face" to "#C9418B"),
        "makeup" to ("brush" to "#9C4DCC"),
        "mehndi" to ("brush" to "#B5651D"),
        "massage" to ("spa" to "#2FA36B"),
        "barber" to ("content_cut" to "#5B6CC9"),
    )

    fun category(d: CategoryDto): Category {
        val (icon, color) = categoryIcon[(d.slug ?: "").lowercase()] ?: ("spa" to "#E84A78")
        return Category(
            id = d.id.toString(),
            name = d.name,
            description = d.slug?.replaceFirstChar { it.uppercase() } ?: d.name,
            iconName = icon,
            colorHex = color,
        )
    }

    fun service(d: ServiceDto): Service = Service(
        id = d.id.toString(),
        categoryId = d.categoryId.toString(),
        name = d.name,
        description = d.description ?: "",
        // base price kept as a fallback; the UI prefers the partner-set range below.
        pricePaise = d.fromPricePaise ?: d.priceMinPaise ?: d.basePricePaise,
        durationMin = d.durationMin,
        rating = d.ratingAvg,
        reviewsCount = d.ratingCount,
        inclusions = d.inclusions ?: emptyList(),
        faqs = emptyList(),
        imageUrl = d.imageUrl ?: "",
        priceMinPaise = d.priceMinPaise,
        priceMaxPaise = d.priceMaxPaise,
        partnerCount = d.partnerCount,
    )

    fun partner(d: PartnerDto): Partner = Partner(
        id = d.id.toString(),
        name = d.name ?: "Nikhat Glow Partner",
        avatarUrl = d.avatarUrl ?: "",
        rating = d.ratingAvg,
        reviewsCount = d.ratingCount,
        distanceKm = d.distanceKm ?: 0.0,
        etaMin = d.etaMin ?: 0,
        experienceYears = d.experienceYears ?: 0,
        description = d.bio ?: "",
        categories = d.categories ?: emptyList(),
        servicesOffered = (d.servicesOffered ?: emptyList()).map { it.toString() },
        portfolioUrls = d.portfolio ?: emptyList(),
        recentReviews = emptyList(),
        fromPricePaise = d.fromPricePaise ?: 0,
        kycStatus = d.kycStatus ?: "not_started",
        certifications = d.certifications ?: emptyList(),
        languages = d.languages ?: emptyList(),
        travelRadiusKm = d.travelRadiusKm ?: 0.0,
    )

    /** Build a UserEntity for the active identity. Wallet balance comes from a
     *  separate call and is patched in by the repository. */
    fun user(d: ProfileDto, role: String, walletPaise: Long = 0): UserEntity = UserEntity(
        id = "me",
        name = d.name ?: (if (role == "partner") "Nikhat Glow Partner" else "Nikhat Glow Customer"),
        email = d.email ?: "",
        role = role,
        kycStatus = d.kycStatus ?: "not_started",
        walletBalancePaise = walletPaise,
        partnerBio = d.bio ?: "",
        partnerExperience = d.experienceYears ?: 0,
        averageRating = d.ratingAvg ?: 0f,
        completedJobs = d.completedJobs ?: 0,
        partnerPublicCode = d.publicCode ?: "",
        phone = d.phone ?: "",
        gender = d.gender ?: "",
        minimumOrderPaise = d.minimumOrderPaise ?: 0,
        travelRadiusKm = d.travelRadiusKm ?: 0.0,
    )

    fun address(d: AddressDto): AddressEntity = AddressEntity(
        id = d.id.toLong(),
        labelText = d.label ?: "Home",
        line1 = d.line1,
        line2 = d.line2 ?: "",
        city = d.city ?: "",
        pincode = d.pincode ?: "",
        lat = d.lat ?: 0.0,
        lon = d.lon ?: 0.0,
        isDefault = d.isDefault,
    )

    fun addressText(d: AddressDto?): String {
        if (d == null) return ""
        return listOfNotNull(d.line1, d.line2?.takeIf { it.isNotBlank() }, d.city)
            .joinToString(", ") + (d.pincode?.let { " - $it" } ?: "")
    }

    fun booking(d: BookingDto): BookingEntity = BookingEntity(
        id = d.id.toString(),
        status = d.status,
        serviceId = (d.serviceId ?: 0).toString(),
        serviceName = d.serviceName ?: "Service",
        serviceImageUrl = d.serviceImageUrl ?: "",
        categoryName = d.categoryName ?: "",
        partnerId = (d.partnerId ?: 0).toString(),
        partnerName = d.partnerName ?: "Assigning…",
        partnerAvatar = d.partnerAvatar ?: "",
        dateTimeSlot = prettySlot(d.slotStart),
        slotStartIso = d.slotStart ?: "",
        addressText = addressText(d.address),
        city = d.address?.city ?: "",
        pincode = d.address?.pincode ?: "",
        addressLat = d.address?.lat,
        addressLon = d.address?.lon,
        totalPaise = d.totalPaise,
        paymentStatus = d.paymentStatus,
        startOtp = d.startOtp ?: "",
        completionProofUrls = (d.completionProof ?: emptyList()).joinToString(","),
        preVisitRequired = d.preVisitRequired,
        preVisitContactOk = d.preVisitContactOk,
    )

    fun walletTxn(d: WalletTxnDto, role: String): WalletTransactionEntity = WalletTransactionEntity(
        id = d.id.toLong(),
        type = d.type,
        role = role,
        amountPaise = d.amountPaise,
        reason = d.reason ?: "",
    )

    fun chat(d: ChatMessageDto): ChatMessageEntity = ChatMessageEntity(
        id = d.id.toLong(),
        bookingId = (d.bookingId ?: 0).toString(),
        senderRole = d.senderType,
        text = d.text ?: "",
        kind = d.kind,
        voiceDurationMs = d.durationMs.toLong(),
    )

    fun complaint(d: ComplaintDto): ComplaintEntity = ComplaintEntity(
        id = d.id.toLong(),
        bookingId = (d.bookingId ?: 0).toString(),
        subject = d.subject,
        message = d.message ?: (d.messages?.firstOrNull()?.message ?: ""),
        status = d.status.replaceFirstChar { it.uppercase() },
    )

    private fun prettySlot(iso: String?): String {
        if (iso.isNullOrBlank()) return "Scheduled"
        // "2026-06-18T10:00:00Z" → "18 Jun, 10:00"
        return try {
            val date = iso.substringBefore("T")
            val time = iso.substringAfter("T").take(5)
            "$date · $time"
        } catch (e: Exception) {
            "Scheduled"
        }
    }
}
