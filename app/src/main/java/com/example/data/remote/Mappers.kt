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

    /**
     * §726 — resolve a (possibly relative) image url against the API origin.
     *
     * The backend self-hosts catalog / avatar / portfolio images and stores RELATIVE
     * urls ("/media/vedadrop/…", and historically "/uploads/…", "/static/…") so it
     * works on localhost, LAN and prod alike. Coil 2.x will NOT load a path-only url,
     * so we prefix it with the server origin (baseUrl without the "/api/…" suffix —
     * the same trick LiveTrackingSocket uses). Absolute http(s) urls and base64 data:
     * urls are returned unchanged. Doing this in the mappers means every AsyncImage
     * call site gets an already-absolute url without per-site changes.
     */
    fun absUrl(raw: String?, defaultUrl: String = ""): String {
        val u = (raw ?: "").trim()
        if (u.isEmpty() || u == "null") return defaultUrl
        if (u.startsWith("http://", true) || u.startsWith("https://", true) ||
            u.startsWith("data:", true)) return u
        if (u.startsWith("/media/") || u.startsWith("/uploads/") || u.startsWith("/static/")) {
            val origin = NetworkConfig.baseUrl.substringBefore("/api/").trimEnd('/')
            return origin + u
        }
        return u
    }

    // slug → the icon-name / colour the original mock used, so category tiles
    // keep their look. Unknown slugs get sensible beauty defaults.
    private val categoryIcon = mapOf(
        "salon" to ("content_cut" to "#009688"),   // §715 brand teal
        "beauty" to ("face" to "#00897B"),          // §715 teal 600
        "makeup" to ("brush" to "#9C4DCC"),
        "mehndi" to ("brush" to "#B5651D"),
        "massage" to ("spa" to "#2FA36B"),
        "barber" to ("content_cut" to "#5B6CC9"),
    )

    fun category(d: CategoryDto): Category {
        val (icon, color) = categoryIcon[(d.slug ?: "").lowercase()] ?: ("spa" to "#009688")  // §715 brand teal default
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
        faqs = d.faqs?.map { it.q to it.a } ?: emptyList(),   // §737 — real FAQ rows (was [])
        imageUrl = absUrl(d.imageUrl, "https://images.unsplash.com/photo-1560066984-138dadb4c035?w=500&q=80"),   // §726 — resolve self-hosted relative urls
        priceMinPaise = d.priceMinPaise,
        priceMaxPaise = d.priceMaxPaise,
        partnerCount = d.partnerCount,
        gallery = (d.gallery ?: emptyList()).map { absUrl(it, "https://images.unsplash.com/photo-1560066984-138dadb4c035?w=500&q=80") }.ifEmpty { listOf("https://images.unsplash.com/photo-1560066984-138dadb4c035?w=500&q=80", "https://images.unsplash.com/photo-1544161515-4ab6ce6db874?w=500&q=80") },   // §737 — portfolio gallery
    )

    fun partner(d: PartnerDto): Partner = Partner(
        id = d.id.toString(),
        name = d.name ?: "Veda Drop Partner",
        avatarUrl = absUrl(d.avatarUrl, "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=500&q=80"),   // §726 — resolve self-hosted relative urls
        rating = d.ratingAvg,
        reviewsCount = d.ratingCount,
        completedJobs = d.completedJobs ?: 0,
        partnerType = d.partnerType ?: "individual",
        experts = (d.experts ?: emptyList()).map { expert(it) },
        distanceKm = d.distanceKm ?: 0.0,
        etaMin = d.etaMin ?: 0,
        experienceYears = d.experienceYears ?: 0,
        description = d.bio ?: "",
        categories = d.categories ?: emptyList(),
        servicesOffered = (d.servicesOffered ?: emptyList()).map { it.toString() },
        portfolioUrls = (d.portfolio ?: emptyList()).map { absUrl(it, "https://images.unsplash.com/photo-1560066984-138dadb4c035?w=500&q=80") }.ifEmpty { listOf("https://images.unsplash.com/photo-1560066984-138dadb4c035?w=500&q=80", "https://images.unsplash.com/photo-1544161515-4ab6ce6db874?w=500&q=80") },   // §726
        recentReviews = emptyList(),
        fromPricePaise = d.fromPricePaise ?: 0,
        kycStatus = d.kycStatus ?: "not_started",
        certifications = d.certifications ?: emptyList(),
        languages = d.languages ?: emptyList(),
        travelRadiusKm = d.travelRadiusKm ?: 0.0,
        // §722 — surface the partner's minimum booking value + public code on her card.
        minimumOrderPaise = d.minimumOrderPaise ?: 0,
        publicCode = d.publicCode ?: "",
    )

    // §743 — map a parlour expert DTO → domain Expert (resolve a relative photo url).
    fun expert(d: ExpertDto): Expert = Expert(
        id = d.id,
        name = d.name,
        title = d.title ?: "",
        bio = d.bio ?: "",
        photoUrl = absUrl(d.photoUrl, ""),
        experienceYears = d.experienceYears,
        kycVerified = d.kycVerified || d.kycStatus == "approved",
    )

    /** Build a UserEntity for the active identity. Wallet balance comes from a
     *  separate call and is patched in by the repository. */
    fun user(d: ProfileDto, role: String, walletPaise: Long = 0): UserEntity = UserEntity(
        id = "me",
        name = d.name ?: (if (role == "partner") "Veda Drop Partner" else "Veda Drop Customer"),
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
        profileId = d.id,
        partnerType = d.partnerType ?: "individual",   // §743
        gapMin = d.gapMin ?: 60,                        // §744
    )

    fun address(d: AddressDto): AddressEntity = AddressEntity(
        id = (d.id ?: 0).toLong(),
        labelText = d.label ?: "Home",
        line1 = d.line1 ?: "",
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
        // §708 — never coerce a missing id to "0": that produces a booking that
        // looks like the real booking #0 and propagates silently. Use a "-1"
        // sentinel so downstream code/UI can detect and reject the invalid value.
        serviceId = (d.serviceId ?: -1).toString(),
        serviceName = d.serviceName ?: "Service",
        serviceImageUrl = absUrl(d.serviceImageUrl, "https://images.unsplash.com/photo-1560066984-138dadb4c035?w=500&q=80"),
        categoryName = d.categoryName ?: "",
        partnerId = (d.partnerId ?: -1).toString(),
        partnerName = d.partnerName ?: "Assigning…",
        partnerAvatar = absUrl(d.partnerAvatar, "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=500&q=80"),
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
        // §704 — the server already viewer-scopes `contact` (customer sees partner,
        // partner sees customer), so take whichever side is present.
        counterpartyName = (d.contact?.partner ?: d.contact?.customer)?.name ?: "",
        counterpartyPhone = (d.contact?.partner ?: d.contact?.customer)?.phone ?: "",
        callAllowed = (d.contact?.partner ?: d.contact?.customer)?.callAllowed ?: false,
        // §710 P1-5 — carry the server's review state (was dropped, so the review
        // button kept reappearing and a submitted rating never showed). reviewRating
        // == 0 means unreviewed; the backend sends the real rating once reviewed.
        reviewRating = d.reviewRating ?: 0,
        reviewComment = d.reviewComment ?: "",
        // §710 #5/#6 — notes + a readable summary of every booked service.
        customerNotes = d.customerNotes ?: "",
        itemsSummary = d.items.joinToString(", ") {
            "${it.name ?: "Service"}${if (it.qty > 1) " ×${it.qty}" else ""}"
        },
        // §722 req-2 — distance from the partner to the customer (partner/admin view).
        distanceKm = d.distanceKm,
        // §723 dual rating — the partner's rating of the customer.
        customerRated = d.customerRated,
        customerRating = d.customerRating ?: 0,
        // §728 (parity C1) — encode the server timeline as "status|iso8601" lines for
        // the vertical stepper. `at` may be null (state not reached) → empty after the
        // pipe. A "\n" separator can't collide with a status name or an ISO instant.
        timelineEncoded = (d.timeline ?: emptyList())
            .joinToString("\n") { "${it.status}|${it.at ?: ""}" },
        startSelfieUrl = d.startSelfieUrl ?: "",
        // §729 (parity C2) — flexible arrival window (server-authoritative).
        isFlexible = d.isFlexible,
        windowEndIso = d.windowEnd ?: "",
        // §744 — the assigned parlour expert ("who is coming").
        expertId = d.expertId ?: 0,
        expertName = d.expertName ?: "",
        expertPhotoUrl = absUrl(d.expertPhotoUrl, ""),
    )

    fun walletTxn(d: WalletTxnDto, role: String): WalletTransactionEntity = WalletTransactionEntity(
        id = d.id.toLong(),
        type = d.type,
        role = role,
        amountPaise = d.amountPaise,
        reason = d.reason ?: "",
    )

    /**
     * §708 — `threadId` lets the caller stamp the owning thread directly in the
     * mapper, so pre-booking messages (synthetic "pre_<pid>_<sid>" thread) no
     * longer have to be patched with `.copy(bookingId = …)` at the call site.
     * When `threadId` is null we fall back to the DTO's bookingId; a missing
     * bookingId becomes "-1" (an impossible id) instead of "0", so pre-booking
     * and real booking-0 messages can never collide in the chat cache.
     */
    fun chat(d: ChatMessageDto, threadId: String? = null): ChatMessageEntity = ChatMessageEntity(
        id = d.id.toLong(),
        bookingId = threadId ?: (d.bookingId?.toString() ?: "-1"),
        senderRole = d.senderType,
        text = d.text ?: "",
        kind = d.kind,
        voiceDurationMs = d.durationMs.toLong(),
        blocked = d.blocked,   // §714 cust-chat-4
    )

    fun complaint(d: ComplaintDto): ComplaintEntity = ComplaintEntity(
        id = d.id.toLong(),
        bookingId = (d.bookingId ?: 0).toString(),
        subject = d.subject,
        message = d.message ?: (d.messages?.firstOrNull()?.message ?: ""),
        status = d.status.replaceFirstChar { it.uppercase() },
    )

    private val slotFormatter = java.time.format.DateTimeFormatter.ofPattern("d MMM, HH:mm")

    private fun prettySlot(iso: String?): String {
        if (iso.isNullOrBlank()) return "Scheduled"
        // "2026-06-18T10:00:00Z" → "18 Jun, 10:00" (in the device time zone).
        // Reuse isUrgentOffer's tolerant parse: an offset/Z instant first, then a
        // bare local date-time fallback. Render in the device zone via the shared
        // formatter. On any failure fall back to the raw split so a malformed value
        // never crashes (and at worst shows the old "yyyy-MM-dd · HH:mm" form).
        return try {
            val zoned = runCatching {
                java.time.OffsetDateTime.parse(iso)
                    .atZoneSameInstant(java.time.ZoneId.systemDefault())
            }.recoverCatching {
                java.time.LocalDateTime.parse(iso.removeSuffix("Z"))
                    .atZone(java.time.ZoneId.systemDefault())
            }.getOrThrow()
            zoned.format(slotFormatter)
        } catch (e: Exception) {
            try {
                val date = iso.substringBefore("T")
                val time = iso.substringAfter("T").take(5)
                "$date · $time"
            } catch (e2: Exception) {
                "Scheduled"
            }
        }
    }
}
