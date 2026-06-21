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
import com.example.data.remote.RescheduleReq
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
 *
 * ID-conversion contract: server entity ids are ALWAYS numeric. Methods here
 * convert id strings with `.toInt()` on that contract; user-supplied/synthetic
 * ids that may be non-numeric (e.g. "pre_…" threads, locally-added custom
 * services, optional booking refs) use `.toIntOrNull()` and handle the null.
 * Do NOT introduce a bare `.toInt()` on any id whose source is not a backend
 * entity — use `.toIntOrNull()` (with a fallback / early return) instead.
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

    // §703 — resolved app config (feature flags / role visibility / policies). The
    // UI reads this to self-gate features + show policy copy; refreshed on launch.
    private val _appConfig = MutableStateFlow<com.example.data.remote.AppConfigResp?>(null)
    val appConfigFlow: StateFlow<com.example.data.remote.AppConfigResp?> = _appConfig.asStateFlow()

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

    // §691 — open reassignment offers this partner may claim (the Rescue Board).
    private val _offers = MutableStateFlow<List<com.example.data.remote.ReassignmentOfferDto>>(emptyList())
    val offersFlow: StateFlow<List<com.example.data.remote.ReassignmentOfferDto>> = _offers.asStateFlow()

    // ── Notifications (in-app inbox) ───────────────────────────────────────────
    private val _notifications = MutableStateFlow<List<com.example.data.remote.NotificationDto>>(emptyList())
    val notificationsFlow: StateFlow<List<com.example.data.remote.NotificationDto>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCountFlow: StateFlow<Int> = _unreadCount.asStateFlow()

    // last server quote, kept so confirmAndBook can create the booking from it
    @Volatile private var lastQuoteId: String? = null

    private val customProductsUsed = mutableMapOf<String, String>()
    private val localCustomPartnerServices = mutableListOf<PartnerServiceEntity>()

    fun insertLocalPartnerService(service: PartnerServiceEntity) {
        localCustomPartnerServices.add(service)
        customProductsUsed[service.serviceId] = service.productsUsed
        _partnerServices.value = _partnerServices.value + service
    }

    fun isFavoriteFlow(partnerId: String): Flow<Boolean> =
        _favorites.map { list -> list.any { it.partnerId == partnerId } }

    fun getMessagesFlow(bookingId: String): Flow<List<ChatMessageEntity>> =
        _chat.map { list -> list.filter { it.bookingId == bookingId } }

    // ── Session / auth ─────────────────────────────────────────────────────────
    fun isAuthenticated(): Boolean =
        tokenStore.activeRole != null && !tokenStore.accessToken().isNullOrBlank()

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
        // Guard against a backend bug returning empty tokens: persisting blanks
        // would leave the user "logged in" while every authed request fails.
        if (resp.accessToken.isBlank() || resp.refreshToken.isBlank()) {
            throw IllegalStateException("Invalid tokens from server")
        }
        tokenStore.save(role, resp.accessToken, resp.refreshToken, makeActive = true)
        // Login is COMPLETE the moment the token is persisted. Hydration is
        // best-effort: a transient failure on any profile/bookings/catalog call
        // must NOT abort login (otherwise the user sees a misleading auth error
        // — e.g. "Missing bearer token." — and is bounced back to the login
        // screen even though they are fully authenticated). Empty screens then
        // self-heal on the next pull-to-refresh / navigation.
        runCatching { hydrateForRole(role) }
        return true
    }

    suspend fun logout() {
        val role = tokenStore.activeRole
        try {
            val refresh = role?.let { tokenStore.refreshToken(it) }
            if (refresh != null) api.logout(mapOf("refresh_token" to refresh))
        } catch (_: Exception) {
        }
        clearSession()
    }

    /** §704 — Play-Store account deletion: ask the backend to soft-delete, then
     *  perform the exact same local cleanup as logout (tokens + every cache) so no
     *  account data lingers in this process. */
    suspend fun deleteAccount() {
        api.deleteAccount()
        clearSession()
    }

    /** Clear the persisted tokens AND every in-memory cache so the next account
     *  signing in on the same process can't see the previous account's data
     *  (the repository/VM live until process death — §704 account-bleed fix). */
    private fun clearSession() {
        tokenStore.clearAll()
        lastQuoteId = null
        customProductsUsed.clear()
        localCustomPartnerServices.clear()
        _activeUser.value = null
        _addresses.value = emptyList()
        _bookings.value = emptyList()
        _partnerServices.value = emptyList()
        _complaints.value = emptyList()
        _favorites.value = emptyList()
        _preBooking.value = emptyList()
        _chat.value = emptyList()
        _cart.value = null
        _subscription.value = null
        _subscriptionPayments.value = emptyList()
        _earnings.value = null
        _analytics.value = null
        _portfolio.value = emptyList()
        _availability.value = null
        _partnerReviews.value = emptyList()
        _offers.value = emptyList()
        _notifications.value = emptyList()
        _unreadCount.value = 0
        // §704 account-bleed: also drop the resolved app config (feature flags /
        // role visibility / policies) so the next account can't see the previous
        // user's config before refreshAppConfig() repopulates it.
        _appConfig.value = null
    }

    // ── Notifications (in-app inbox + FCM device registration) ─────────────────
    /** Pull the latest in-app notifications + unread count. Best-effort: a transient
     *  failure leaves the existing cache intact (the caller polls again). */
    suspend fun refreshNotifications() {
        runCatching { api.notifications() }.onSuccess { resp ->
            _notifications.value = resp.items
            _unreadCount.value = resp.unread
        }
    }

    /** Mark one notification read on the server, then refresh both flows. */
    suspend fun markNotificationRead(id: Int) {
        runCatching { api.markNotificationRead(id) }
        refreshNotifications()
    }

    /** Register this device's FCM token for push (best-effort — push is optional;
     *  the in-app inbox works without it). */
    suspend fun registerDevice(token: String) {
        if (token.isBlank()) return
        runCatching { api.registerDevice(com.example.data.remote.DeviceReq(fcmToken = token)) }
    }

    /** De-register the device token on logout (best-effort). */
    suspend fun unregisterDevice(token: String) {
        if (token.isBlank()) return
        runCatching { api.deleteDevice(com.example.data.remote.DeviceDeleteReq(fcmToken = token)) }
    }

    // ── Hydration ────────────────────────────────────────────────────────────
    suspend fun hydrateCatalog(lat: Double? = null, lon: Double? = null) {
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
        // §687 — pass the device fix (when known) so the backend can sort/limit
        // by distance ("near me"). Pre-§687 coords were never sent so distance
        // sorting silently did nothing. null coords → backend returns un-sorted.
        NikhatGlowDataSource.partners = runCatching {
            api.partners(lat = lat, lon = lon, sort = lat?.let { "distance" }).items.map { Mappers.partner(it) }
        }.getOrDefault(emptyList())
    }

    /** §690 — server-side service search. Returns null on failure so the caller
     *  can fall back to the local in-memory filter. Results are partner-filtered
     *  and carry the price range (Mappers.service maps min/max). */
    suspend fun searchServices(q: String): List<Service>? =
        runCatching { api.search(q).services.map { Mappers.service(it) } }.getOrNull()

    /** §707 — search professionals by their public ID (all digits) or name. Lets a
     *  customer who's been handed a partner's ID look them up directly and book.
     *  Only bookable (subscribed + KYC-approved) partners are ever returned. */
    suspend fun searchPartners(q: String): List<Partner> =
        runCatching { api.partners(q = q).items.map { Mappers.partner(it) } }.getOrDefault(emptyList())

    /** Discovery for a single service — used by the partner-select screen so the
     *  list reflects who actually offers that service right now (blank until a
     *  subscribed partner adds it). §687 — accepts the device fix for near-me. */
    suspend fun loadPartnersForService(serviceId: String, lat: Double? = null, lon: Double? = null) {
        NikhatGlowDataSource.partners = runCatching {
            api.partners(serviceId = serviceId.toIntOrNull(), lat = lat, lon = lon,
                sort = lat?.let { "distance" }).items.map { Mappers.partner(it) }
        }.getOrDefault(emptyList())
    }

    suspend fun hydrateForRole(role: String) {
        // Each call is isolated: one endpoint hiccupping (network blip, a 5xx,
        // a parse mismatch) must not stop the others from populating. The token
        // is already saved by the caller, so none of these gate login.
        suspend fun step(block: suspend () -> Unit) { runCatching { block() } }
        step { hydrateCatalog() }
        if (role == "customer") {
            step { refreshProfile("customer") }
            step { refreshAddresses() }
            step { refreshBookings("customer") }
            step { refreshCart() }
            step { refreshFavorites() }
            step { refreshComplaints() }
        } else {
            step { refreshProfile("partner") }
            step { refreshPartnerServices() }
            step { refreshBookings("partner") }
            step { refreshSubscription() }
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

    /**
     * Persist working hours, working days (JS dow 0=Sun..6=Sat), leave dates and the
     * per-date hour-overrides map (ISO-date -> bookable slot-start hours; [] = full-day leave).
     */
    suspend fun saveAvailability(
        start: String,
        end: String,
        days: List<Int>,
        leaves: List<String>,
        hourOverrides: Map<String, List<Int>> = emptyMap(),
    ) {
        api.setPartnerAvailability(
            mapOf(
                "working_hours" to mapOf("start" to start, "end" to end),
                "days" to days,
                "leaves" to leaves,
                "hour_overrides" to hourOverrides,
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

    /** Public, null-safe re-fetch of the signed-in identity (refreshes kyc_status,
     *  name-lock, etc.). Used by pull-to-refresh and on-entry refresh so an admin
     *  KYC approval shows up WITHOUT forcing a re-login. No-op if not signed in. */
    suspend fun refreshActiveProfile() {
        val role = activeRole() ?: return
        runCatching { refreshProfile(role) }
    }

    suspend fun refreshAddresses() {
        _addresses.value = api.addresses().items.map { Mappers.address(it) }
    }

    suspend fun refreshBookings(role: String) {
        val items = if (role == "partner") api.partnerBookings().items else api.bookings().items
        _bookings.value = items.map { Mappers.booking(it) }
    }

    /** Live-refresh a single booking (fresh status for the detail screen) and
     *  splice it back into the cache. Null-safe → returns null on any failure. */
    suspend fun refreshBooking(id: String): BookingEntity? = runCatching {
        val fresh = Mappers.booking(api.booking(id.toInt()))
        _bookings.value = _bookings.value.map { if (it.id == fresh.id) fresh else it }
            .let { list -> if (list.any { it.id == fresh.id }) list else list + fresh }
        fresh
    }.getOrNull()

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
    /** Returns the quote's total (paise) so the caller can pre-check the ₹599
     *  minimum before placing the booking (§707). */
    suspend fun cartQuote(couponCode: String?, addressId: Long?, slotId: String? = null): Long {
        val resp = api.cartQuote(
            CartQuoteReq(
                slotId = slotId?.ifBlank { null },
                addressId = addressId?.toInt(),
                couponCode = couponCode?.ifBlank { null },
            )
        )
        lastQuoteId = resp.quoteId
        return resp.totalPaise
    }

    /** §702 — real availability slots for the booking-time picker. Null-safe. */
    suspend fun fetchAvailability(
        partnerId: Int,
        serviceId: Int?,
        date: String,
    ): List<com.example.data.remote.SlotDto> =
        runCatching { api.availability(partnerId, serviceId, date).slots }.getOrDefault(emptyList())

    /** §702 — customer fetches the start-OTP on demand once a booking is accepted. */
    suspend fun fetchStartOtp(id: Int): String? =
        runCatching { api.startOtp(id).otp }.getOrNull()

    /** §702 — partner KYC status (incl. rejection reason for the trust pass). */
    suspend fun fetchKyc(): com.example.data.remote.KycStatusResp? =
        runCatching { api.getKyc() }.getOrNull()

    suspend fun refreshFavorites() {
        val wl = api.wishlist()
        _favorites.value = wl.partnerIds.map { FavoritePartnerEntity(it.toString()) }
        // §710 #4 — merge the full favourited partner cards into the in-memory catalog so
        // the Favourites screen (which resolves favourites by id) renders real data even
        // when a favourite isn't in the current discovery result.
        if (wl.partners.isNotEmpty()) {
            val merged = (NikhatGlowDataSource.partners + wl.partners.map { Mappers.partner(it) })
                .associateBy { it.id }
            NikhatGlowDataSource.partners = merged.values.toList()
        }
    }

    suspend fun refreshComplaints() {
        _complaints.value = api.complaints().items.map { Mappers.complaint(it) }
    }

    /** Full complaint detail incl. the message thread (for the detail screen). */
    suspend fun loadComplaint(id: String): com.example.data.remote.ComplaintDto? =
        runCatching { api.complaint(id.toInt()) }.getOrNull()

    /** Post a reply on a complaint thread, then refresh the list status. */
    suspend fun replyComplaint(id: String, text: String) {
        api.addComplaintMessage(id.toInt(), MessageReq(text.trim()))
        runCatching { refreshComplaints() }
    }

    /** Partner: remove one of their listed services, then refresh the list. */
    suspend fun deletePartnerService(id: String) {
        // Locally-added custom services have non-numeric ids ("me_srv_custom_…")
        // and never reached the server — drop them from the local cache instead
        // of calling DELETE with a NumberFormatException.
        val serverId = id.toIntOrNull()
        if (serverId == null) {
            localCustomPartnerServices.removeAll { it.id == id }
            customProductsUsed.remove(id)
            _partnerServices.value = _partnerServices.value.filterNot { it.id == id }
            return
        }
        api.deletePartnerService(serverId)
        refreshPartnerServices()
    }

    suspend fun refreshPartnerServices() {
        // Null-safe like the other refreshers: if the remote fetch fails we leave
        // the cache untouched rather than wiping it, and — critically — we do NOT
        // re-surface localCustomPartnerServices (unsaved, never reached the server)
        // as if they were persisted backend data.
        val remoteList = runCatching { api.partnerServices().items }.getOrNull()?.map {
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
        } ?: return
        _partnerServices.value = remoteList + localCustomPartnerServices
    }

    // ── Customer actions ───────────────────────────────────────────────────────
    // §687 — lat/lon are NULLABLE now: a "use current location" save passes the
    // real device fix; a pure-manual address passes null (distance features just
    // degrade gracefully — the backend guards on lat/lon presence). Pre-§687 this
    // hardcoded a Bangalore coordinate for EVERY address (the location bug).
    suspend fun addAddress(label: String, line1: String, line2: String, city: String, pincode: String, lat: Double? = null, lon: Double? = null) {
        api.addAddress(AddressCreateReq(label, line1, line2.ifBlank { null }, city, pincode, lat, lon, _addresses.value.isEmpty()))
        refreshAddresses()
    }

    /** Create an address and immediately make it the active "Deliver To" (default)
     *  one — used by the home location picker, where the place the user just chose
     *  MUST become the active location even if they already had other saved
     *  addresses (plain [addAddress] only defaults the very first one). Create with
     *  is_default=true, then PATCH is_default again so the backend clears any
     *  sibling default. */
    suspend fun addAndSelectAddress(label: String, line1: String, line2: String, city: String, pincode: String, lat: Double? = null, lon: Double? = null) {
        val created = api.addAddress(AddressCreateReq(label, line1, line2.ifBlank { null }, city, pincode, lat, lon, true))
        runCatching { api.updateAddress(created.id, mapOf("is_default" to true)) }
        refreshAddresses()
    }

    // §687/§692 — geo proxy passthroughs (server-side free OpenStreetMap: Photon/Nominatim/OSRM).
    suspend fun geoAutocomplete(q: String, lat: Double? = null, lon: Double? = null) =
        runCatching { api.geoAutocomplete(q, lat, lon).suggestions }.getOrDefault(emptyList())

    suspend fun geoReverse(lat: Double, lon: Double) =
        runCatching { api.geoReverse(lat, lon) }.getOrNull()

    suspend fun geoDirections(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double) =
        runCatching { api.geoDirections(fromLat, fromLon, toLat, toLon) }.getOrNull()

    /** §690 — remote map config from the OdioBook geo gateway (tile key + base url
     *  + feature flags). Builds the ABSOLUTE /api/geo URL from the app's server
     *  root (NetworkConfig.baseUrl is .../api/nikhatglow/v1/). Null on failure. */
    suspend fun geoAppConfig(): com.example.data.remote.GeoAppConfigDto? {
        val root = com.example.data.remote.NetworkConfig.baseUrl.substringBefore("/api/")
        val url = "$root/api/geo/app-config?app=nikhatglow"
        return runCatching { api.geoAppConfig(url) }.getOrNull()
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
            // Positive magnitude — the UI renders the discount line only when
            // > 0 and prints it with its own leading "−". Storing it negative
            // here hid the coupon line and produced "− ₹−50".
            couponDiscountPaise = b.couponPaise,
            walletDiscountPaise = b.walletAppliedPaise,
            taxPaise = b.taxPaise,
            totalPaise = resp.totalPaise,
            couponMessage = resp.coupon?.message,
        )
    }

    suspend fun createBookingFromLastQuote(
        customerNotes: String? = null,
        genderPreference: String? = null,
        deviceInfo: Map<String, String?>? = null,
        customerShareNumber: Boolean = false,
    ): BookingEntity {
        val qid = lastQuoteId ?: throw IllegalStateException("No quote — request a quote first.")
        val dto = api.createBooking(
            BookingCreateReq(
                quoteId = qid,
                customerNotes = customerNotes?.trim()?.ifBlank { null },
                genderPreference = genderPreference?.ifBlank { null },
                bookingSource = "app",
                deviceInfo = deviceInfo?.takeIf { it.isNotEmpty() },
                customerShareNumber = customerShareNumber,
            )
        )
        lastQuoteId = null
        refreshBookings("customer")
        // Backend clears the checked-out cart server-side; mirror that locally.
        refreshCart()
        return Mappers.booking(dto)
    }

    suspend fun cancelBooking(id: String, reason: String, reasonCode: String? = null) {
        val role = tokenStore.activeRole ?: "customer"
        // §704 — a partner cancels via her own endpoint; the customer via hers.
        if (role == "partner") api.partnerCancelBooking(id.toInt(), CancelReq(reason, reasonCode))
        else api.cancelBooking(id.toInt(), CancelReq(reason, reasonCode))
        refreshBookings(role)
    }

    // ── §704 block/report + emergency ───────────────────────────────────────────
    /** §704 — permanently cut a partner off (kills all partner→customer channels). */
    suspend fun blockPartner(partnerId: String) {
        api.blockPartner(partnerId.toInt())
    }

    suspend fun unblockPartner(partnerId: String) {
        api.unblockPartner(partnerId.toInt())
    }

    /** §704 — the admin-editable emergency numbers from /config (112 + women helpline
     *  1091/181 + childline 1098); falls back to the national defaults. */
    fun emergencyNumbers(): List<String> {
        val p = _appConfig.value?.params?.get("emergency_numbers")
        val list = (p as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
        return if (list.isNotEmpty()) list else listOf("112", "1091")
    }

    fun womenHelpline(): String =
        _appConfig.value?.params?.get("women_helpline")?.toString() ?: "1091"

    /** §707 — admin-configurable minimum booking value in paise (founder rule:
     *  no booking below ₹599). Moshi decodes JSON numbers to Double, so coerce via
     *  Number. Falls back to ₹599 until /config loads. The server is the final
     *  authority; this drives the review-screen pre-check + the floor message. */
    fun minBookingPaise(): Long =
        (_appConfig.value?.params?.get("min_booking_paise") as? Number)?.toLong() ?: 59900L

    // ── §704 post-booking talk-request + partner inbox ──────────────────────────
    suspend fun getTalkRequest(bookingId: String) = api.getTalkRequest(bookingId.toInt())

    suspend fun raiseTalkRequest(bookingId: String, reason: String?) =
        api.raiseTalkRequest(bookingId.toInt(),
            if (reason.isNullOrBlank()) emptyMap() else mapOf("reason" to reason))

    suspend fun respondTalkRequest(bookingId: String, reqId: Int, accept: Boolean) =
        api.respondTalkRequest(bookingId.toInt(), reqId, mapOf("action" to if (accept) "accept" else "reject"))

    suspend fun partnerInbox() = api.partnerInbox().items

    suspend fun partnerInboxThread(customerId: Int) = api.partnerInboxThread(customerId).items

    suspend fun partnerInboxReply(customerId: Int, text: String) =
        api.partnerInboxReply(customerId, com.example.data.remote.ChatSendReq(text = text))

    // ── §703 app config + Flow-B open booking + safety ──────────────────────────
    /** Fetch the resolved app config (best-effort; never throws). Drives feature
     *  gating + the role-based nav + policy copy. Call on launch + on resume. */
    suspend fun refreshAppConfig() {
        val role = tokenStore.activeRole
        runCatching { api.appConfig(role) }.getOrNull()?.let { _appConfig.value = it }
    }

    /** Resolved feature flag (false until config loads / if absent). */
    fun flag(key: String, default: Boolean = false): Boolean =
        _appConfig.value?.flags?.get(key) ?: default

    /** Whether the current role should see a UI surface. */
    fun surface(key: String, default: Boolean = false): Boolean =
        _appConfig.value?.surfaces?.get(key) ?: default

    /** Admin-editable policy copy (women_only / pre_visit_call / cancellation / …). */
    fun policy(key: String): String = _appConfig.value?.policies?.get(key) ?: ""

    /** §703 Flow-B — create a booking WITHOUT choosing a partner; it broadcasts to
     *  the eligible pool (first-to-accept-wins). Returns the server response (so the
     *  UI can show "sent to N professionals" or the no-partner-found message). */
    suspend fun createOpenBooking(
        serviceLines: List<Pair<Int, Int>>,   // (serviceId, qty)
        slotStartIso: String,
        addressId: Int? = null,
        lat: Double? = null,
        lon: Double? = null,
        customerNotes: String? = null,
        deviceInfo: Map<String, String?>? = null,
        customerShareNumber: Boolean = false,
    ): com.example.data.remote.OpenBookingResp {
        val resp = api.createOpenBooking(
            com.example.data.remote.OpenBookingReq(
                serviceLines = serviceLines.map { com.example.data.remote.OpenLineReq(it.first, it.second) },
                slotStart = slotStartIso,
                addressId = addressId, lat = lat, lon = lon,
                customerNotes = customerNotes?.trim()?.ifBlank { null },
                deviceInfo = deviceInfo?.takeIf { it.isNotEmpty() },
                customerShareNumber = customerShareNumber,
            )
        )
        refreshBookings("customer")
        return resp
    }

    /** §703 — the customer's one-tap visit confirmation (satisfies the pre-visit
     *  safety gate so the partner may start travelling). Returns the fresh booking. */
    suspend fun confirmVisit(id: String): BookingEntity? {
        val resp = api.confirmVisit(id.toInt())
        refreshBookings("customer")
        return resp.booking?.let { Mappers.booking(it) }
    }

    /** §703 — raise an SOS (either party). Returns the 112 emergency info. */
    suspend fun raiseSos(
        bookingId: String? = null, lat: Double? = null, lon: Double? = null, note: String? = null,
    ): com.example.data.remote.SosResp =
        api.raiseSos(com.example.data.remote.SosReq(
            bookingId = bookingId?.toIntOrNull(), lat = lat, lon = lon, note = note))

    /** §704 — move a pending/accepted booking to a new slot. Splices the fresh
     *  booking back into the cache; returns null on any failure (caller surfaces). */
    suspend fun rescheduleBooking(id: String, slotId: String): BookingEntity? =
        runCatching { rescheduleBookingChecked(id, slotId) }.getOrNull()

    /** Same as [rescheduleBooking] but RETHROWS so the VM can surface the server's
     *  409 reason (RESCHEDULE_WINDOW_CLOSED / SLOT_TAKEN / SLOT_PAST) via friendly(). */
    suspend fun rescheduleBookingChecked(id: String, slotId: String): BookingEntity {
        val fresh = Mappers.booking(api.reschedule(id.toInt(), RescheduleReq(slotId)))
        _bookings.value = _bookings.value.map { if (it.id == fresh.id) fresh else it }
            .let { list -> if (list.any { it.id == fresh.id }) list else list + fresh }
        return fresh
    }

    // ── §691 reassignment ──────────────────────────────────────────────────────
    /** Customer: drop the current partner and re-offer the job to all eligible
     *  nearby professionals (first-to-accept-wins) at the same price. */
    suspend fun changePartner(id: String) {
        api.changePartner(id.toInt())
        refreshBookings("customer")
    }

    /** Poll the live reassignment status for the "finding a new professional…" UI. */
    suspend fun reassignmentStatus(id: String) =
        runCatching { api.reassignmentStatus(id.toInt()) }.getOrNull()

    /** Partner: emergency transfer — broadcast to all, or target a colleague by
     *  UPPERCASE public code (who gets a 5-min head start, then it broadcasts). */
    suspend fun transferBooking(id: String, mode: String, targetPublicCode: String?) {
        api.transferBooking(
            id.toInt(),
            com.example.data.remote.TransferReq(
                mode = mode,
                targetPublicCode = targetPublicCode?.trim()?.uppercase()?.ifBlank { null },
            ),
        )
        refreshBookings("partner")
    }

    /** Refresh the Rescue Board (open offers this partner may claim). */
    // §710 #27 — why the offers board is empty (renew / finish KYC / no jobs).
    private val _offersEmptyMessage = MutableStateFlow<String?>(null)
    val offersEmptyMessageFlow: StateFlow<String?> = _offersEmptyMessage.asStateFlow()

    suspend fun loadOffers() {
        val resp = runCatching { api.partnerOffers() }.getOrNull()
        _offers.value = resp?.items ?: emptyList()
        _offersEmptyMessage.value = resp?.emptyMessage
    }

    /** Claim an offer (first-to-accept-wins). Throws on 409 OFFER_ALREADY_TAKEN. */
    suspend fun acceptOffer(offerId: Int) {
        api.acceptOffer(offerId)
        loadOffers()
        refreshBookings("partner")
    }

    suspend fun declineOffer(offerId: Int) {
        runCatching { api.declineOffer(offerId) }
        _offers.value = _offers.value.filterNot { it.offerId == offerId }
    }

    suspend fun addReview(id: String, rating: Int, comment: String) {
        api.review(id.toInt(), ReviewReq(rating, comment.ifBlank { null }))
        refreshBookings("customer")
    }

    suspend fun createComplaint(bookingId: String?, subject: String, message: String) {
        api.createComplaint(ComplaintReq(bookingId?.toIntOrNull(), subject, message))
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
        _chat.value = (_chat.value.filter { it.bookingId != threadId } + msgs).distinctBy { it.id }
        // Clear stale pre-booking messages when this is a real (numeric) booking
        // thread — otherwise a thread that graduated from "pre_…" to a booking id
        // keeps surfacing the old pre-booking chat.
        if (pre != null) _preBooking.value = msgs else _preBooking.value = emptyList()
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

    suspend fun updateProfile(
        name: String,
        email: String,
        bio: String,
        experience: Int,
        gender: String? = null,
        minimumOrderPaise: Long? = null,
        travelRadiusKm: Double? = null,
    ) {
        val role = tokenStore.activeRole ?: "customer"
        api.updateMe(mapOf("name" to name, "email" to email))
        if (role == "partner") {
            // Only send keys we actually have so we never clobber a server value with null.
            val body = mutableMapOf<String, Any?>(
                "bio" to bio,
                "experience_years" to experience,
            )
            gender?.ifBlank { null }?.let { body["gender"] = it }
            minimumOrderPaise?.let { body["minimum_order_paise"] = it }
            travelRadiusKm?.let { body["travel_radius_km"] = it }
            api.updatePartnerProfile(body)
        }
        refreshProfile(role)
    }

    /** Persist the partner's base location (lat/lon) so they become discoverable
     *  + distance-rankable. Sent only on the partner profile PATCH. */
    suspend fun setPartnerLocation(lat: Double, lon: Double) {
        api.updatePartnerProfile(mapOf("base_lat" to lat, "base_lon" to lon))
        refreshProfile("partner")
    }

    // ── Partner actions ──────────────────────────────────────────────────────
    suspend fun submitKyc(
        aadhaar: String,
        pan: String,
        legalName: String? = null,
        selfieDataUrl: String? = null,
        documentDataUrl: String? = null,
    ) {
        // §704 — legalName = the name on her ID; the admin locks her display name to it.
        // Photos (when captured) are sent as base64 JPEG data URLs; the backend
        // stores selfie_upload_id + document_upload_ids as-is, so no backend change.
        api.submitKyc(
            KycReq(
                aadhaar,
                pan,
                selfieUploadId = selfieDataUrl,
                documentUploadIds = listOfNotNull(documentDataUrl),
                legalName = legalName?.trim()?.ifBlank { null },
            )
        )
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

    /** §710 P0-8 — the partner's own per-service prices → { serviceId(String): pricePaise }.
     *  Lets the partner store show each service's real price instead of one shared one. */
    suspend fun loadPartnerServicePrices(partnerId: String): Map<String, Long> {
        val pid = partnerId.toIntOrNull() ?: return emptyMap()
        return runCatching {
            api.partnerPricedServices(pid).items
                .mapNotNull { it.serviceId?.let { sid -> sid.toString() to (it.pricePaise ?: 0L) } }
                .toMap()
        }.getOrDefault(emptyMap())
    }

    /** §710 P0-9 — online/away toggle → POST /partner/availability/online {online}.
     *  (The old path PATCHed profile {is_active}, which the backend silently dropped,
     *  so an "away" partner kept receiving jobs.) */
    suspend fun setPartnerOnline(active: Boolean) {
        api.setPartnerOnline(mapOf("online" to active))
        refreshProfile("partner")
    }

    /** Availability-engine: service-bounds radius → partner profile travel_radius_km. */
    suspend fun setPartnerTravelRadius(km: Double) {
        api.updatePartnerProfile(mapOf("travel_radius_km" to km))
        refreshProfile("partner")
    }

    /** Availability-engine: persist daily operating hours (HH:mm 24h). */
    suspend fun setPartnerWorkingHours(start: String, end: String) {
        api.setPartnerAvailability(mapOf("working_hours" to mapOf("start" to start, "end" to end)))
    }

    suspend fun acceptBooking(id: String) { api.acceptBooking(id.toInt()); refreshBookings("partner") }
    suspend fun rejectBooking(id: String) { api.rejectBooking(id.toInt(), CancelReq("Declined")); refreshBookings("partner") }
    suspend fun startTravel(id: String) { partnerStatus(id, "partner_on_the_way") }
    suspend fun arriveLocation(id: String) { partnerStatus(id, "arrived") }
    suspend fun startJob(id: String, otp: String) {
        // Backend requires the customer's start-OTP to begin the job. The partner
        // collects this code from the customer at the door and types it in; the
        // backend returns start_otp = null to the partner by design, so we MUST
        // send the typed value (not a cached blank).
        api.partnerBookingStatus(id.toInt(), StatusReq(to = "started", startOtp = otp.ifBlank { null }))
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
