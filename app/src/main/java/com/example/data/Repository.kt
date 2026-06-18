package com.example.data

import android.content.Context
import com.example.data.remote.AddressCreateReq
import com.example.data.remote.ApiClient
import com.example.data.remote.BookingCreateReq
import com.example.data.remote.CancelReq
import com.example.data.remote.CartAddReq
import com.example.data.remote.CartItemPatchReq
import com.example.data.remote.CartQuoteReq
import com.example.data.remote.CartResp
import com.example.data.remote.ChatSendReq
import com.example.data.remote.ComplaintReq
import com.example.data.remote.KycReq
import com.example.data.remote.Mappers
import com.example.data.remote.MessageReq
import com.example.data.remote.PartnerServiceReq
import com.example.data.remote.QuoteReq
import com.example.data.remote.ReviewReq
import com.example.data.remote.StatusReq
import com.example.data.remote.WishlistReq
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

data class OtpHandle(val otpToken: String, val devOtp: String?)

data class QuoteBreakdown(
    val basePaise: Long,
    val distancePaise: Long,
    val surgePaise: Long,
    val couponDiscountPaise: Long,
    val walletDiscountPaise: Long,
    val taxPaise: Long,
    val totalPaise: Long,
    val couponMessage: String?,
)

/**
 * 100%-online NikhatGlow data layer. Every read/write goes to the backend
 * (`/api/nikhatglow/v1/`); there is no local source of truth. In-memory
 * StateFlows act purely as a UI cache, refreshed from the server after each
 * mutation. The (untouched) Compose screens collect these flows exactly as
 * they did against the old Room repository.
 */
class NikhatGlowRepository(context: Context) {

    private val client = ApiClient.get(context)
    private val api get() = client.api
    private val tokenStore = com.example.data.remote.TokenStore(context)

    // ── UI caches ────────────────────────────────────────────────────────────
    private val _activeUser = MutableStateFlow<UserEntity?>(null)
    val activeUserFlow: StateFlow<UserEntity?> = _activeUser.asStateFlow()

    private val _addresses = MutableStateFlow<List<AddressEntity>>(emptyList())
    val addressesFlow: StateFlow<List<AddressEntity>> = _addresses.asStateFlow()

    private val _bookings = MutableStateFlow<List<BookingEntity>>(emptyList())
    val bookingsFlow: StateFlow<List<BookingEntity>> = _bookings.asStateFlow()

    private val _partnerServices = MutableStateFlow<List<PartnerServiceEntity>>(emptyList())
    val partnerServicesFlow: StateFlow<List<PartnerServiceEntity>> = _partnerServices.asStateFlow()

    private val _complaints = MutableStateFlow<List<ComplaintEntity>>(emptyList())
    val complaintsFlow: StateFlow<List<ComplaintEntity>> = _complaints.asStateFlow()

    private val _favorites = MutableStateFlow<List<FavoritePartnerEntity>>(emptyList())
    val favoritePartnersFlow: StateFlow<List<FavoritePartnerEntity>> = _favorites.asStateFlow()

    private val _preBooking = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val allPreBookingMessagesFlow: StateFlow<List<ChatMessageEntity>> = _preBooking.asStateFlow()

    private val _chat = MutableStateFlow<List<ChatMessageEntity>>(emptyList())

    // single-partner cart (null = empty / not yet loaded)
    private val _cart = MutableStateFlow<CartResp?>(null)
    val cartFlow: StateFlow<CartResp?> = _cart.asStateFlow()

    // partner ₹99/month subscription (null = not yet loaded)
    private val _subscription = MutableStateFlow<com.example.data.remote.SubscriptionDto?>(null)
    val subscriptionFlow: StateFlow<com.example.data.remote.SubscriptionDto?> = _subscription.asStateFlow()

    private val _subscriptionPayments = MutableStateFlow<List<com.example.data.remote.SubscriptionPaymentDto>>(emptyList())
    val subscriptionPaymentsFlow: StateFlow<List<com.example.data.remote.SubscriptionPaymentDto>> = _subscriptionPayments.asStateFlow()

    // partner earnings / analytics (null = not yet loaded)
    private val _earnings = MutableStateFlow<com.example.data.remote.EarningsDto?>(null)
    val earningsFlow: StateFlow<com.example.data.remote.EarningsDto?> = _earnings.asStateFlow()

    private val _analytics = MutableStateFlow<com.example.data.remote.AnalyticsDto?>(null)
    val analyticsFlow: StateFlow<com.example.data.remote.AnalyticsDto?> = _analytics.asStateFlow()

    // partner portfolio
    private val _portfolio = MutableStateFlow<List<com.example.data.remote.PortfolioItemDto>>(emptyList())
    val portfolioFlow: StateFlow<List<com.example.data.remote.PortfolioItemDto>> = _portfolio.asStateFlow()

    // partner availability (working hours / days / leaves)
    private val _availability = MutableStateFlow<com.example.data.remote.PartnerAvailabilityResp?>(null)
    val availabilityFlow: StateFlow<com.example.data.remote.PartnerAvailabilityResp?> = _availability.asStateFlow()

    // reviews for the partner profile currently being browsed
    private val _partnerReviews = MutableStateFlow<List<com.example.data.remote.ReviewDto>>(emptyList())
    val partnerReviewsFlow: StateFlow<List<com.example.data.remote.ReviewDto>> = _partnerReviews.asStateFlow()

    // last server quote, kept so confirmAndBook can create the booking from it
    @Volatile private var lastQuoteId: String? = null

    private val customProductsUsed = mutableMapOf<String, String>()

    fun isFavoriteFlow(partnerId: String): Flow<Boolean> =
        _favorites.map { list -> list.any { it.partnerId == partnerId } }

    fun getMessagesFlow(bookingId: String): Flow<List<ChatMessageEntity>> =
        _chat.map { list -> list.filter { it.bookingId == bookingId } }

    // ── Session / auth ─────────────────────────────────────────────────────────
    fun isAuthenticated(): Boolean =
        tokenStore.activeRole != null && tokenStore.accessToken() != null

    fun activeRole(): String? = tokenStore.activeRole
    fun hasSession(role: String): Boolean = tokenStore.hasSession(role)

    /** Returns the otp_token (needed for verify) + the dev OTP if the backend
     *  is in dev mode (so testing needs no real SMS). */
    suspend fun requestOtp(phone: String, role: String): OtpHandle {
        val r = api.otpRequest(mapOf("phone" to phone, "role" to role))
        return OtpHandle(r.otpToken, r.devOtp)
    }

    suspend fun verifyOtp(phone: String, role: String, otpToken: String, code: String): Boolean {
        val resp = api.otpVerify(mapOf("otp_token" to otpToken, "code" to code))
        tokenStore.save(role, resp.accessToken, resp.refreshToken, makeActive = true)
        hydrateForRole(role)
        return true
    }

    suspend fun logout() {
        val role = tokenStore.activeRole
        try {
            val refresh = role?.let { tokenStore.refreshToken(it) }
            if (refresh != null) api.logout(mapOf("refresh_token" to refresh))
        } catch (_: Exception) {
        }
        tokenStore.clearAll()
        _activeUser.value = null
        _addresses.value = emptyList()
        _bookings.value = emptyList()
        _partnerServices.value = emptyList()
        _complaints.value = emptyList()
        _favorites.value = emptyList()
        _cart.value = null
        _subscription.value = null
        _subscriptionPayments.value = emptyList()
    }

    // ── Hydration ────────────────────────────────────────────────────────────
    suspend fun hydrateCatalog() {
        // Always overwrite with the server's truth — including EMPTY. The catalog
        // is admin-controlled and the partner list is discovery (subscription-
        // gated), so an empty result is the correct "no partners yet" state.
        val cats = api.categories().items.map { Mappers.category(it) }
        NikhatGlowDataSource.categories = cats
        val allServices = mutableListOf<Service>()
        for (c in cats) {
            try {
                allServices += api.categoryServices(c.id.toInt()).items.map { Mappers.service(it) }
            } catch (_: Exception) {
            }
        }
        NikhatGlowDataSource.services = allServices
        NikhatGlowDataSource.partners = runCatching {
            api.partners().items.map { Mappers.partner(it) }
        }.getOrDefault(emptyList())
    }

    /** Discovery for a single service — used by the partner-select screen so the
     *  list reflects who actually offers that service right now (blank until a
     *  subscribed partner adds it). */
    suspend fun loadPartnersForService(serviceId: String) {
        NikhatGlowDataSource.partners = runCatching {
            api.partners(serviceId = serviceId.toIntOrNull()).items.map { Mappers.partner(it) }
        }.getOrDefault(emptyList())
    }

    suspend fun hydrateForRole(role: String) {
        hydrateCatalog()
        if (role == "customer") {
            refreshProfile("customer")
            refreshAddresses()
            refreshBookings("customer")
            refreshCart()
            refreshFavorites()
            refreshComplaints()
        } else {
            refreshProfile("partner")
            refreshPartnerServices()
            refreshBookings("partner")
            refreshSubscription()
        }
    }

    // ── Partner subscription (₹99/month listing fee) ──────────────────────────
    suspend fun refreshSubscription() {
        _subscription.value = runCatching { api.subscription() }.getOrNull()
    }

    suspend fun loadSubscriptionPayments() {
        _subscriptionPayments.value =
            runCatching { api.subscriptionPayments().items }.getOrDefault(emptyList())
    }

    suspend fun subscribe() {
        _subscription.value = api.subscribe()
        loadSubscriptionPayments()
    }

    suspend fun cancelSubscription() {
        _subscription.value = api.cancelSubscription()
    }

    // ── Partner earnings / analytics / portfolio / availability ────────────────
    suspend fun loadEarnings() {
        _earnings.value = runCatching { api.partnerEarnings() }.getOrNull()
    }

    suspend fun loadAnalytics() {
        _analytics.value = runCatching { api.partnerAnalytics() }.getOrNull()
    }

    suspend fun loadPortfolio() {
        _portfolio.value = runCatching { api.partnerPortfolio().items }.getOrDefault(emptyList())
    }

    suspend fun addPortfolioItem(uploadId: String?, imageUrl: String?, caption: String) {
        api.addPortfolioItem(
            com.example.data.remote.PortfolioCreateReq(
                uploadId = uploadId?.ifBlank { null },
                imageUrl = imageUrl?.ifBlank { null },
                caption = caption.ifBlank { null },
            )
        )
        loadPortfolio()
    }

    suspend fun deletePortfolioItem(id: Int) {
        api.deletePortfolioItem(id)
        loadPortfolio()
    }

    /** Load + cache the partner's working-hours availability. */
    suspend fun loadAvailability() {
        _availability.value = runCatching { api.partnerAvailability() }.getOrNull()
    }

    /** Persist working hours, working days (JS dow 0=Sun..6=Sat) and leave dates. */
    suspend fun saveAvailability(start: String, end: String, days: List<Int>, leaves: List<String>) {
        api.setPartnerAvailability(
            mapOf(
                "working_hours" to mapOf("start" to start, "end" to end),
                "days" to days,
                "leaves" to leaves,
            )
        )
        loadAvailability()
    }

    /** Reviews for a partner profile (customer-side browse). */
    suspend fun loadPartnerReviews(partnerId: String) {
        _partnerReviews.value =
            runCatching { api.partnerReviews(partnerId.toInt()).items }.getOrDefault(emptyList())
    }

    private suspend fun refreshProfile(role: String) {
        val profile = api.me().profile ?: return
        _activeUser.value = Mappers.user(profile, role, 0L)
    }

    suspend fun refreshAddresses() {
        _addresses.value = api.addresses().items.map { Mappers.address(it) }
    }

    suspend fun refreshBookings(role: String) {
        val items = if (role == "partner") api.partnerBookings().items else api.bookings().items
        _bookings.value = items.map { Mappers.booking(it) }
    }

    // ── Cart (single-partner, multi-service) ───────────────────────────────────
    suspend fun refreshCart() {
        _cart.value = runCatching { api.getCart() }.getOrNull()
    }

    suspend fun addToCart(partnerId: String, serviceId: String) {
        _cart.value = api.addToCart(
            CartAddReq(partnerId = partnerId.toInt(), serviceId = serviceId.toInt(), qty = 1)
        )
    }

    suspend fun updateCartQty(itemId: Int, qty: Int) {
        _cart.value = api.patchCartItem(itemId, CartItemPatchReq(qty))
    }

    suspend fun removeCartItem(itemId: Int) {
        _cart.value = api.deleteCartItem(itemId)
    }

    suspend fun clearCart() {
        _cart.value = api.clearCart()
    }

    /** Build a single multi-line quote from the whole cart; stores quote_id so
     *  [createBookingFromLastQuote] can place the booking request. */
    suspend fun cartQuote(couponCode: String?, addressId: Long?) {
        val resp = api.cartQuote(
            CartQuoteReq(addressId = addressId?.toInt(), couponCode = couponCode?.ifBlank { null })
        )
        lastQuoteId = resp.quoteId
    }

    suspend fun refreshFavorites() {
        val wl = api.wishlist()
        _favorites.value = wl.partnerIds.map { FavoritePartnerEntity(it.toString()) }
    }

    suspend fun refreshComplaints() {
        _complaints.value = api.complaints().items.map { Mappers.complaint(it) }
    }

    suspend fun refreshPartnerServices() {
        _partnerServices.value = api.partnerServices().items.map {
            PartnerServiceEntity(
                id = it.id.toString(),
                serviceId = it.serviceId.toString(),
                name = it.name ?: "",
                categoryName = "",
                pricePaise = it.pricePaise,
                durationMin = 0,
                active = it.active,
                productsUsed = customProductsUsed[it.serviceId.toString()] ?: ""
            )
        }
    }

    // ── Customer actions ───────────────────────────────────────────────────────
    suspend fun addAddress(label: String, line1: String, line2: String, city: String, pincode: String, lat: Double, lon: Double) {
        api.addAddress(AddressCreateReq(label, line1, line2.ifBlank { null }, city, pincode, lat, lon, _addresses.value.isEmpty()))
        refreshAddresses()
    }

    suspend fun deleteAddress(id: Long) {
        api.deleteAddress(id.toInt())
        refreshAddresses()
    }

    suspend fun setDefaultAddress(id: Long) {
        api.updateAddress(id.toInt(), mapOf("is_default" to true))
        refreshAddresses()
    }

    /** Server-authoritative quote. Stores quote_id for the subsequent booking. */
    suspend fun createQuote(partnerId: String, serviceId: String, slotId: String?, addressId: Long?, couponCode: String?, useWallet: Boolean): QuoteBreakdown {
        val resp = api.quote(
            QuoteReq(
                partnerId = partnerId.toIntOrNull() ?: 0,
                serviceId = serviceId.toIntOrNull() ?: 0,
                slotId = slotId,
                addressId = addressId?.toInt(),
                couponCode = couponCode?.ifBlank { null },
                useWallet = useWallet,
            )
        )
        lastQuoteId = resp.quoteId
        val b = resp.breakdown
        return QuoteBreakdown(
            basePaise = b.basePaise,
            distancePaise = b.distancePaise,
            surgePaise = b.surgePaise,
            couponDiscountPaise = -b.couponPaise,
            walletDiscountPaise = b.walletAppliedPaise,
            taxPaise = b.taxPaise,
            totalPaise = resp.totalPaise,
            couponMessage = resp.coupon?.message,
        )
    }

    suspend fun createBookingFromLastQuote(): BookingEntity {
        val qid = lastQuoteId ?: throw IllegalStateException("No quote — request a quote first.")
        val dto = api.createBooking(BookingCreateReq(qid))
        lastQuoteId = null
        refreshBookings("customer")
        // Backend clears the checked-out cart server-side; mirror that locally.
        refreshCart()
        return Mappers.booking(dto)
    }

    suspend fun cancelBooking(id: String, reason: String) {
        api.cancelBooking(id.toInt(), CancelReq(reason))
        refreshBookings(tokenStore.activeRole ?: "customer")
    }

    suspend fun addReview(id: String, rating: Int, comment: String) {
        api.review(id.toInt(), ReviewReq(rating, comment.ifBlank { null }))
        refreshBookings("customer")
    }

    suspend fun createComplaint(bookingId: String, subject: String, message: String) {
        api.createComplaint(ComplaintReq(bookingId.toIntOrNull(), subject, message))
        refreshComplaints()
    }

    suspend fun toggleFavorite(partnerId: String) {
        val exists = _favorites.value.any { it.partnerId == partnerId }
        val pid = partnerId.toIntOrNull()
        if (exists) api.removeWishlist(WishlistReq(partnerId = pid))
        else api.addWishlist(WishlistReq(partnerId = pid))
        refreshFavorites()
    }

    // ── Chat (thread-id aware) ──────────────────────────────────────────────
    // threadId is either a numeric booking id ("42") or a pre-booking synthetic
    // id "pre_<partnerId>_<serviceId>". Messages are tagged with threadId so the
    // (untouched) screens can filter by it.
    private fun parsePre(threadId: String): Pair<Int, Int?>? {
        if (!threadId.startsWith("pre_")) return null
        val parts = threadId.removePrefix("pre_").split("_")
        val pid = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val sid = parts.getOrNull(1)?.toIntOrNull()
        return pid to sid
    }

    suspend fun loadThread(threadId: String) {
        val pre = parsePre(threadId)
        val msgs = if (pre != null) {
            api.preBookingMessages(pre.first, pre.second).items
                .map { Mappers.chat(it).copy(bookingId = threadId) }
        } else {
            api.bookingMessages(threadId.toInt()).items.map { Mappers.chat(it) }
        }
        _chat.value = _chat.value.filter { it.bookingId != threadId } + msgs
        if (pre != null) _preBooking.value = msgs
    }

    suspend fun sendThread(threadId: String, text: String) {
        val pre = parsePre(threadId)
        if (pre != null) {
            api.sendPreBookingMessage(pre.first, pre.second, ChatSendReq(text = text))
        } else {
            api.sendBookingMessage(threadId.toInt(), ChatSendReq(text = text))
        }
        loadThread(threadId)
    }

    suspend fun updateProfile(name: String, email: String, bio: String, experience: Int) {
        val role = tokenStore.activeRole ?: "customer"
        runCatching { api.updateMe(mapOf("name" to name, "email" to email)) }
        if (role == "partner") {
            runCatching {
                api.updatePartnerProfile(mapOf("bio" to bio, "experience_years" to experience))
            }
        }
        refreshProfile(role)
    }

    // ── Partner actions ──────────────────────────────────────────────────────
    suspend fun submitKyc(aadhaar: String, pan: String, selfieUploadId: String) {
        api.submitKyc(KycReq(aadhaar, pan, selfieUploadId))
        refreshProfile("partner")
    }

    suspend fun setServicePrice(serviceId: String, pricePaise: Long, active: Boolean, productsUsed: String) {
        customProductsUsed[serviceId] = productsUsed
        val existing = _partnerServices.value.firstOrNull { it.serviceId == serviceId }
        if (existing != null) {
            api.patchPartnerService(existing.id.toInt(), mapOf("price_paise" to pricePaise, "active" to active))
        } else {
            api.addPartnerService(PartnerServiceReq(serviceId.toInt(), pricePaise, active))
        }
        refreshPartnerServices()
    }

    suspend fun acceptBooking(id: String) { api.acceptBooking(id.toInt()); refreshBookings("partner") }
    suspend fun rejectBooking(id: String) { api.rejectBooking(id.toInt(), CancelReq("Declined")); refreshBookings("partner") }
    suspend fun startTravel(id: String) { partnerStatus(id, "partner_on_the_way") }
    suspend fun arriveLocation(id: String) { partnerStatus(id, "arrived") }
    suspend fun startJob(id: String) {
        // Backend requires the customer's start-OTP to begin the job. The booking
        // detail carries it (start_otp) so the demo flow proceeds; a hardened
        // build should collect this from the customer at the door.
        val otp = _bookings.value.firstOrNull { it.id == id }?.startOtp?.ifBlank { null }
        api.partnerBookingStatus(id.toInt(), StatusReq(to = "started", startOtp = otp))
        refreshBookings("partner")
    }
    suspend fun completeJob(id: String, proofUrl: String = "") {
        val proof = proofUrl.takeIf { it.isNotBlank() }?.let { listOf(it) }
        api.partnerBookingStatus(id.toInt(), StatusReq(to = "completed", proofUploadIds = proof))
        refreshBookings("partner")
    }

    private suspend fun partnerStatus(id: String, to: String) {
        api.partnerBookingStatus(id.toInt(), StatusReq(to = to))
        refreshBookings("partner")
    }
}
