package com.example.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.remote.GeoAppConfigDto
import com.example.data.remote.LiveTrackingSocket
import com.example.ui.map.GeoPoint
import com.example.ui.map.NikhatMaps
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class Screen {
    object CustomerHome : Screen()
    data class CategoryDetail(val category: Category) : Screen()
    data class ServiceDetail(val service: Service) : Screen()
    data class PartnerSelect(val service: Service) : Screen()
    data class PartnerStore(val partner: Partner) : Screen(), RouteWithParams
    data class BookingConfirm(val service: Service, val partner: Partner) : Screen(), RouteWithParams
    data class BookingDetail(val bookingId: String) : Screen(), RouteWithParams
    object Cart : Screen()
    object MyBookings : Screen()
    object ComplaintsList : Screen()
    data class ComplaintDetail(val id: String) : Screen(), RouteWithParams

    object CustomerProfile : Screen()
    object ServiceBookingForm : Screen()
    object Favourites : Screen()
    object CustomerDashboard : Screen()
    data class PartnerReviews(val partner: Partner) : Screen()

    // Partner screens
    object PartnerDashboard : Screen()
    object PartnerKyc : Screen()
    object PartnerServices : Screen()
    object PartnerProfile : Screen()
    object PartnerSubscription : Screen()
    object PartnerAvailability : Screen()
    object PartnerEarnings : Screen()
    object PartnerAnalytics : Screen()
    object PartnerPortfolio : Screen()
    object PartnerOffers : Screen()   // §691 Rescue Board (claim reassigned jobs)

    // Pre-booking messaging
    data class PreBookingChat(val service: Service, val partner: Partner) : Screen()

    // In-app notification inbox (shared by customer + partner)
    object Notifications : Screen()
}

interface RouteWithParams

/**
 * Online NikhatGlow ViewModel. All data is server-backed via [NikhatGlowRepository];
 * the public surface (flows + methods) is unchanged from the offline version so
 * the Compose screens are untouched. Adds OTP-login session state.
 */
class NikhatGlowViewModel(application: Application) : AndroidViewModel(application) {
    val repository = NikhatGlowRepository(application)

    // ── Server-backed state flows ──────────────────────────────────────────────
    val activeUser = repository.activeUserFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )
    val addresses = repository.addressesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val bookings = repository.bookingsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val partnerServices = repository.partnerServicesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val complaints = repository.complaintsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val preBookingInquiries = repository.allPreBookingMessagesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val favoritePartners = repository.favoritePartnersFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    // ── §703 app config (feature flags / role visibility / policies) ────────────
    val appConfig = repository.appConfigFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    /** Refresh the resolved app config. Call on launch + after login/role change. */
    fun loadAppConfig() {
        viewModelScope.launch { runCatching { repository.refreshAppConfig() } }
    }

    /** Feature-flag / surface / policy passthroughs for the Compose screens. */
    fun flag(key: String, default: Boolean = false) = repository.flag(key, default)
    fun surface(key: String, default: Boolean = false) = repository.surface(key, default)
    fun policy(key: String) = repository.policy(key)

    /** §703 Flow-B — book WITHOUT choosing a partner; broadcasts to nearby pros
     *  (first-to-accept-wins). `onResult(dispatched)` tells the UI whether it went
     *  out (false ⇒ no professional available for that slot). */
    fun createOpenBooking(
        serviceLines: List<Pair<Int, Int>>, slotStartIso: String,
        addressId: Int? = null, lat: Double? = null, lon: Double? = null,
        notes: String? = null, onResult: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            runCatching {
                repository.createOpenBooking(serviceLines, slotStartIso, addressId, lat, lon, notes)
            }.onSuccess {
                notify(it.message ?: "Booking sent.", isError = !it.dispatched)
                onResult(it.dispatched)
            }.onFailure { notify(friendly(it), isError = true); onResult(false) }
        }
    }

    /** §703 — customer confirms the visit (satisfies the pre-visit safety gate so
     *  the partner may start travelling). */
    fun confirmVisit(bookingId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.confirmVisit(bookingId) }
                .onSuccess { notify("Visit confirmed — your professional can head out."); onDone() }
                .onFailure { notify(friendly(it), isError = true) }
        }
    }

    /** §703 — raise an SOS (either party). Surfaces the 112 prompt. */
    fun raiseSos(bookingId: String? = null, lat: Double? = null, lon: Double? = null) {
        viewModelScope.launch {
            runCatching { repository.raiseSos(bookingId, lat, lon) }
                .onSuccess {
                    notify(it.message ?: "Help is being notified. Call 112 now if you are in danger.",
                           isError = true)
                }
                .onFailure { notify(friendly(it), isError = true) }
        }
    }

    // ── In-app notification inbox ──────────────────────────────────────────────
    val notifications = repository.notificationsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val unreadCount = repository.unreadCountFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0
    )

    /** Pull the latest notifications + unread badge. Safe to call repeatedly (the
     *  home screen polls it every ~30s). No-op when logged out. */
    fun loadNotifications() {
        if (!isLoggedIn) return
        viewModelScope.launch { runCatching { repository.refreshNotifications() } }
    }

    fun markNotificationRead(id: Int) {
        viewModelScope.launch { runCatching { repository.markNotificationRead(id) } }
    }

    // partner ₹99/month subscription
    val subscription = repository.subscriptionFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )
    val subscriptionPayments = repository.subscriptionPaymentsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    var subscriptionBusy by mutableStateOf(false); private set
    var subscriptionError by mutableStateOf<String?>(null); private set

    fun loadSubscription() {
        viewModelScope.launch {
            runCatching { repository.refreshSubscription(); repository.loadSubscriptionPayments() }
        }
    }

    fun subscribeNow() {
        subscriptionBusy = true; subscriptionError = null
        viewModelScope.launch {
            runCatching { repository.subscribe() }
                .onSuccess { notify("Subscription active") }
                .onFailure { subscriptionError = friendly(it) }
            subscriptionBusy = false
        }
    }

    fun cancelSubscriptionNow() {
        subscriptionBusy = true; subscriptionError = null
        viewModelScope.launch {
            runCatching { repository.cancelSubscription() }
                .onSuccess { notify("Subscription cancelled") }
                .onFailure { subscriptionError = friendly(it) }
            subscriptionBusy = false
        }
    }

    // ── Partner earnings / analytics ───────────────────────────────────────────
    val earnings = repository.earningsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )
    val analytics = repository.analyticsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    fun loadEarnings() {
        viewModelScope.launch { runCatching { repository.loadEarnings() } }
    }

    // ── §691 booking reassignment (change-partner / transfer / Rescue Board) ────
    val offers = repository.offersFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    var reassignmentBusy by mutableStateOf(false); private set
    var reassignmentError by mutableStateOf<String?>(null); private set
    var reassignmentToast by mutableStateOf<String?>(null)

    fun clearReassignmentMessages() { reassignmentError = null; reassignmentToast = null }

    /** Customer: re-offer the booking to nearby partners (first-to-accept-wins). */
    fun changePartner(bookingId: String, onResult: (String?) -> Unit = {}) {
        reassignmentBusy = true; reassignmentError = null
        viewModelScope.launch {
            runCatching { repository.changePartner(bookingId) }
                .onSuccess { reassignmentToast = "Finding a new professional…"; onResult(null) }
                .onFailure { reassignmentError = friendly(it); onResult(friendly(it)) }
            reassignmentBusy = false
        }
    }

    /** Partner: transfer an accepted job (broadcast or targeted by public code). */
    fun transferBooking(bookingId: String, mode: String, targetPublicCode: String?, onResult: (String?) -> Unit = {}) {
        reassignmentBusy = true; reassignmentError = null
        viewModelScope.launch {
            runCatching { repository.transferBooking(bookingId, mode, targetPublicCode) }
                .onSuccess { reassignmentToast = "Booking offered to other professionals."; onResult(null) }
                .onFailure { reassignmentError = friendly(it); onResult(friendly(it)) }
            reassignmentBusy = false
        }
    }

    fun loadOffers() {
        viewModelScope.launch { runCatching { repository.loadOffers() } }
    }

    fun acceptOffer(offerId: Int, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.acceptOffer(offerId) }
                .onSuccess { reassignmentToast = "Job claimed — it's yours."; onResult(null) }
                .onFailure { val m = friendly(it); reassignmentToast = m; onResult(m) }
        }
    }

    fun declineOffer(offerId: Int) {
        viewModelScope.launch { runCatching { repository.declineOffer(offerId) } }
    }

    /** Poll the live reassignment status for a booking (null-safe). */
    suspend fun fetchReassignmentStatus(bookingId: String) =
        repository.reassignmentStatus(bookingId)

    /** Refresh the active bookings list (used to poll a reassigning booking). */
    fun refreshActiveBookings() {
        val role = repository.activeRole() ?: "customer"
        viewModelScope.launch { runCatching { repository.refreshBookings(role) } }
    }

    fun loadAnalytics() {
        viewModelScope.launch { runCatching { repository.loadAnalytics() } }
    }

    // ── Partner portfolio ──────────────────────────────────────────────────────
    val portfolio = repository.portfolioFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    var portfolioBusy by mutableStateOf(false); private set
    var portfolioError by mutableStateOf<String?>(null); private set

    fun loadPortfolio() {
        viewModelScope.launch { runCatching { repository.loadPortfolio() } }
    }

    fun addPortfolioItem(uploadId: String, imageUrl: String, caption: String) {
        if (uploadId.isBlank() && imageUrl.isBlank()) {
            portfolioError = "Provide an upload id or image URL."
            return
        }
        portfolioBusy = true; portfolioError = null
        viewModelScope.launch {
            runCatching { repository.addPortfolioItem(uploadId, imageUrl, caption) }
                .onSuccess { notify("Portfolio updated") }
                .onFailure { portfolioError = friendly(it) }
            portfolioBusy = false
        }
    }

    fun deletePortfolioItem(id: Int) {
        viewModelScope.launch { runCatching { repository.deletePortfolioItem(id) } }
    }

    // ── Partner availability (working hours) ───────────────────────────────────
    val availability = repository.availabilityFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )
    var availabilityBusy by mutableStateOf(false); private set
    var availabilityError by mutableStateOf<String?>(null); private set
    var availabilitySaved by mutableStateOf(false)

    fun loadAvailability() {
        viewModelScope.launch { runCatching { repository.loadAvailability() } }
    }

    fun saveAvailability(start: String, end: String, days: List<Int>, leaves: List<String>) {
        availabilityBusy = true; availabilityError = null; availabilitySaved = false
        viewModelScope.launch {
            runCatching { repository.saveAvailability(start, end, days, leaves) }
                .onSuccess { availabilitySaved = true }
                .onFailure { availabilityError = friendly(it) }
            availabilityBusy = false
        }
    }

    // ── Partner reviews (customer-side browse) ─────────────────────────────────
    val partnerReviews = repository.partnerReviewsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun loadPartnerReviews(partnerId: String) {
        viewModelScope.launch { runCatching { repository.loadPartnerReviews(partnerId) } }
    }

    /** Refresh discovery for the service the customer is about to browse.
     *  §687 — sends the device fix (if captured) so the list is distance-sorted. */
    fun loadPartnersForService(serviceId: String) {
        val loc = _deviceLocation.value
        viewModelScope.launch { runCatching { repository.loadPartnersForService(serviceId, loc?.first, loc?.second) } }
    }

    // ── §687 device location (the GPS fix) ────────────────────────────────────
    // Real device fix, captured once permission is granted. Null until then —
    // discovery still works (just un-sorted) and manual address entry is allowed.
    private val _deviceLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val deviceLocation: StateFlow<Pair<Double, Double>?> = _deviceLocation.asStateFlow()

    /** Read the current device fix into state, then refresh nearby discovery so
     *  "near me" engages immediately. Safe to call after a permission grant —
     *  returns silently (state stays null) if permission/location is unavailable. */
    fun captureDeviceLocation(
        notifyOnFail: Boolean = false,
        onResult: ((Pair<Double, Double>?) -> Unit)? = null,
    ) {
        viewModelScope.launch {
            val loc = com.example.data.LocationHelper.current(getApplication())
            if (loc != null) {
                _deviceLocation.value = loc
                runCatching { repository.hydrateCatalog(loc.first, loc.second) }
            } else if (notifyOnFail) {
                // The button silently did nothing before this — tell the user WHY so
                // they can act (grant permission, or turn GPS on). Distinguish the
                // two so the message is actionable.
                val msg = if (!com.example.data.LocationHelper.hasPermission(getApplication()))
                    "Location permission is off. Allow location access, then tap “Use current location” again — or type your address below."
                else
                    "Couldn't get your location. Turn on GPS/location and try again — or type your address below."
                notify(msg, isError = true)
            }
            onResult?.invoke(loc)
        }
    }

    /** Partner sets their base location from the current device fix, then we save
     *  it server-side so discovery can rank them. Gives feedback on every path so
     *  the button is never a silent no-op (the old "can't set location" symptom). */
    fun capturePartnerLocation() {
        viewModelScope.launch {
            val loc = com.example.data.LocationHelper.current(getApplication())
            if (loc == null) {
                val msg = if (!com.example.data.LocationHelper.hasPermission(getApplication()))
                    "Location permission is off. Allow location access and try again."
                else
                    "Couldn't get your location. Turn on GPS/location and try again."
                notify(msg, isError = true)
                return@launch
            }
            _deviceLocation.value = loc
            runCatching { repository.setPartnerLocation(loc.first, loc.second) }
                .onSuccess { notify("Business location saved.") }
                .onFailure { notify("Could not save location. Please try again.", isError = true) }
        }
    }

    /** §690 — server-side service search (price-range + partner-filtered). Returns
     *  null on failure so the screen falls back to the local in-memory filter. */
    suspend fun searchServices(q: String): List<Service>? = repository.searchServices(q)

    /** Address search-as-you-type via our geo proxy (free OpenStreetMap). */
    suspend fun searchPlaces(q: String) =
        if (q.isBlank()) emptyList() else repository.geoAutocomplete(q, _deviceLocation.value?.first, _deviceLocation.value?.second)

    /** Turn a GPS fix into a human-readable address (prefill the manual form). */
    suspend fun reverseGeocode(lat: Double, lon: Double) = repository.geoReverse(lat, lon)

    /** Save a location chosen in the home location picker (GPS or a search result)
     *  and make it the ACTIVE "Deliver To" address, so the home header updates
     *  immediately. [onDone] fires on success so the picker can dismiss itself. */
    fun setActiveLocation(
        label: String, line1: String, line2: String, city: String, pincode: String,
        lat: Double? = null, lon: Double? = null,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            runCatching { repository.addAndSelectAddress(label, line1, line2, city, pincode, lat, lon) }
                .onSuccess { notify("Location set"); onDone() }
                .onFailure { notify("Could not set your location. Please try again.", isError = true) }
        }
    }

    // §697 — one-shot auto-detect guard. We try to auto-pick the device location
    // exactly once per process when the customer has NO saved address yet, so a
    // fresh user lands on Home with their real location already selected instead of
    // a dead "Select Location" label. Re-arming on logout is handled by the VM being
    // recreated; within a session we never re-prompt or clobber a manual choice.
    private var _autoLocationAttempted = false

    /** If the customer has no saved address and location permission is granted,
     *  detect the device fix, reverse-geocode it, and save it as the active
     *  address. No-op (silent) without permission, when an address already exists,
     *  or once already attempted this session — so it never fights a manual pick. */
    fun ensureLocationFromDevice() {
        if (_autoLocationAttempted) return
        if (addresses.value.isNotEmpty()) return
        if (!com.example.data.LocationHelper.hasPermission(getApplication())) return
        _autoLocationAttempted = true
        viewModelScope.launch {
            val loc = com.example.data.LocationHelper.current(getApplication()) ?: return@launch
            // Re-check: the user may have added an address while the fix was in flight.
            if (addresses.value.isNotEmpty()) return@launch
            _deviceLocation.value = loc
            runCatching { repository.hydrateCatalog(loc.first, loc.second) }
            val rev = runCatching { repository.geoReverse(loc.first, loc.second) }.getOrNull()
            runCatching {
                repository.addAndSelectAddress(
                    label = "Current Location",
                    line1 = rev?.address ?: "Current Location",
                    line2 = "",
                    city = rev?.city ?: "",
                    pincode = rev?.pincode ?: "",
                    lat = loc.first, lon = loc.second,
                )
            }
        }
    }

    // ── §690 geo gateway remote config (tile key + base url + feature flags) ───
    private val _geoConfig = MutableStateFlow<GeoAppConfigDto?>(null)
    val geoConfig: StateFlow<GeoAppConfigDto?> = _geoConfig.asStateFlow()

    fun refreshGeoConfig() {
        viewModelScope.launch {
            val cfg = repository.geoAppConfig()
            if (cfg != null) _geoConfig.value = cfg
        }
    }

    // ── §690 live tracking (mutual: customer device fix + partner via WS) ──────
    private val _trackCustomer = MutableStateFlow<GeoPoint?>(null)
    val trackCustomer: StateFlow<GeoPoint?> = _trackCustomer.asStateFlow()
    private val _trackPartner = MutableStateFlow<GeoPoint?>(null)
    val trackPartner: StateFlow<GeoPoint?> = _trackPartner.asStateFlow()
    private val _trackRoute = MutableStateFlow<List<GeoPoint>?>(null)
    val trackRoute: StateFlow<List<GeoPoint>?> = _trackRoute.asStateFlow()
    private val _trackEta = MutableStateFlow<String?>(null)
    val trackEta: StateFlow<String?> = _trackEta.asStateFlow()
    private var trackingSocket: LiveTrackingSocket? = null
    private var locationUpdates: com.example.data.LocationUpdates? = null

    /**
     * §698 — live tracking for a booking, modelled on Solaris travel tracking but
     * adapted to two devices. The customer's saved booking/home location is the FIXED
     * destination (green marker); the PARTNER is the moving party and streams their
     * live GPS (≈5s, PRIORITY_HIGH_ACCURACY) over the chat WS so the customer watches
     * them approach. The route + distance/ETA are recomputed whenever the partner moves.
     */
    fun startLiveTracking(bookingId: String) {
        if (trackingSocket != null) return
        val booking = bookings.value.firstOrNull { it.id == bookingId }
        val role = activeUser.value?.role
        // Destination = the customer's saved booking/home address.
        val destLat = booking?.addressLat
        val destLon = booking?.addressLon
        if (destLat != null && destLon != null) _trackCustomer.value = GeoPoint(destLat, destLon)

        val socket = LiveTrackingSocket(getApplication(), bookingId) { msgRole, lat, lon ->
            if (msgRole == "partner") {
                _trackPartner.value = GeoPoint(lat, lon)
                recomputeTrackRoute()
            }
        }
        trackingSocket = socket
        socket.connect()

        if (role == "partner") {
            // Partner travels to the customer → stream continuous GPS; also show
            // ourselves on our own map immediately.
            locationUpdates = com.example.data.LocationHelper.startUpdates(getApplication(), 5_000L) { lat, lon ->
                _trackPartner.value = GeoPoint(lat, lon)
                trackingSocket?.sendLocation(lat, lon)
                recomputeTrackRoute()
            }
        } else if (destLat == null || destLon == null) {
            // Customer device + a booking with no saved coords → fall back to our own
            // device fix as the destination anchor (graceful degradation for old data).
            viewModelScope.launch {
                val loc = com.example.data.LocationHelper.current(getApplication())
                if (loc != null) {
                    _trackCustomer.value = GeoPoint(loc.first, loc.second)
                    recomputeTrackRoute()
                }
            }
        }
    }

    fun stopLiveTracking() {
        locationUpdates?.stop()
        locationUpdates = null
        trackingSocket?.close()
        trackingSocket = null
        _trackPartner.value = null
        _trackRoute.value = null
        _trackEta.value = null
    }

    private fun recomputeTrackRoute() {
        val c = _trackCustomer.value
        val p = _trackPartner.value
        if (c == null || p == null) return
        viewModelScope.launch {
            val dir = repository.geoDirections(p.latitude, p.longitude, c.latitude, c.longitude) ?: return@launch
            dir.polyline.takeIf { it.isNotBlank() }?.let { poly ->
                _trackRoute.value = runCatching { NikhatMaps.decodePolyline(poly) }.getOrNull()
            }
            // §698 — distance-remaining + ETA (Solaris parity).
            if (dir.distanceM > 0) {
                val km = dir.distanceM / 1000.0
                val mins = kotlin.math.max(1, kotlin.math.round(dir.durationS / 60.0).toInt())
                val distStr = if (km < 1.0) "${dir.distanceM} m" else String.format("%.1f km", km)
                _trackEta.value = "$distStr • ~$mins min away"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLiveTracking()
    }

    fun isFavorite(partnerId: String): Flow<Boolean> = repository.isFavoriteFlow(partnerId)

    fun toggleFavorite(partnerId: String) {
        if (!isLoggedIn) {
            triggerLoginPrompt()
            return
        }
        viewModelScope.launch { runCatching { repository.toggleFavorite(partnerId) } }
    }

    // ── Navigation + transient UI ────────────────────────────────────────────
    private var _currentScreen by mutableStateOf<Screen>(Screen.CustomerHome)
    // §687 — back-stack so the hardware Back button returns to the PREVIOUS screen
    // in order (was: no history at all). Bounded so deep navigation can't grow it
    // unbounded. goBack() returns false at a root → shell applies double-back-exit.
    private val _navStack = ArrayDeque<Screen>()
    var currentScreen: Screen
        get() = _currentScreen
        set(value) {
            if (!isLoggedIn && isGuestMode && isScreenRestricted(value)) {
                triggerLoginPrompt()
            } else {
                if (value != _currentScreen) {
                    _navStack.addLast(_currentScreen)
                    if (_navStack.size > 30) _navStack.removeFirst()
                }
                _currentScreen = value
            }
        }

    /** Pop to the previous screen. Returns false when there's no history (a root),
     *  so the shell can apply double-back-to-exit. Bypasses the push-setter so a
     *  back-navigation isn't re-recorded. */
    fun goBack(): Boolean {
        val prev = _navStack.removeLastOrNull() ?: return false
        _currentScreen = prev
        return true
    }

    /** Reset history (called on login/logout/role switch so the new session starts
     *  with a clean back-stack rooted at its home). */
    fun clearNavHistory() { _navStack.clear() }
    var onboardingComplete by mutableStateOf(true)

    // ── Auth / session ─────────────────────────────────────────────────────────
    // Single fixed session: the role is chosen ONCE on the login screen at signup
    // and never switched in-app. To use the other role the user logs out and signs
    // in again choosing it. No dual-identity / "partner mode" toggle.
    var isLoggedIn by mutableStateOf(repository.isAuthenticated())
        private set
    var isGuestMode by mutableStateOf(false)
    var loginRole by mutableStateOf("customer")   // user-selected on the login screen
    var authBusy by mutableStateOf(false); private set
    var authError by mutableStateOf<String?>(null); private set
    var otpSent by mutableStateOf(false); private set
    var devOtpHint by mutableStateOf<String?>(null); private set
    private var otpToken: String? = null

    private val notifiedBookingIds = mutableSetOf<String>()

    private fun checkUpcomingBookingsAndTriggerAlerts(list: List<com.example.data.BookingEntity>) {
        val now = java.time.Instant.now()
        val role = repository.activeRole() ?: "customer"
        list.forEach { booking ->
            if (booking.status == "accepted" || booking.status == "assigned") {
                if (booking.slotStartIso.isNotBlank() && !notifiedBookingIds.contains(booking.id)) {
                    kotlin.runCatching {
                        val target = java.time.Instant.parse(booking.slotStartIso)
                        val diffHours = java.time.Duration.between(now, target).toHours()
                        if (diffHours in 0..24) {
                            notifiedBookingIds.add(booking.id)
                            val message = if (role == "partner") {
                                "Upcoming Attendance Alert ⏰: You have a confirmed premium service '${booking.serviceName}' with client scheduled in $diffHours hours!"
                            } else {
                                "Appointment Reminder 💅: Your beauty treatment '${booking.serviceName}' is confirmed for ${booking.dateTimeSlot} (in $diffHours hours)!"
                            }
                            notify(message, isError = false)
                        }
                    }
                }
            }
        }
    }

    init {
        if (repository.isAuthenticated()) {
            val role = repository.activeRole() ?: "customer"
            currentScreen = if (role == "partner") Screen.PartnerDashboard else Screen.CustomerHome
            viewModelScope.launch { runCatching { repository.hydrateForRole(role) } }
            // §702 — populate the partner KYC rejection reason (Mapper never sets it).
            if (role == "partner") loadPartnerKyc()
            loadNotifications()
        }
        // §690 — pull the geo gateway config (tile key + feature flags) so the
        // live map can render. Public endpoint; safe before/without login.
        refreshGeoConfig()

        // Reactive 24h attendance notification loop
        viewModelScope.launch {
            bookings.collect { list ->
                checkUpcomingBookingsAndTriggerAlerts(list)
            }
        }
    }

    fun sendOtp(phone: String, role: String) {
        if (phone.isBlank()) { authError = "Enter your phone number."; return }
        authBusy = true; authError = null
        viewModelScope.launch {
            runCatching { repository.requestOtp(phone.trim(), role) }
                .onSuccess { handle ->
                    otpToken = handle.otpToken
                    devOtpHint = handle.devOtp
                    otpSent = true
                }
                .onFailure { authError = friendly(it) }
            authBusy = false
        }
    }

    fun verifyOtp(phone: String, code: String) {
        val token = otpToken
        if (token == null) { authError = "Request an OTP first."; return }
        if (code.isBlank()) { authError = "Enter the OTP."; return }
        authBusy = true; authError = null
        val role = loginRole
        viewModelScope.launch {
            runCatching { repository.verifyOtp(phone.trim(), role, token, code.trim()) }
                .onSuccess {
                    isLoggedIn = true
                    isGuestMode = false
                    otpSent = false
                    otpToken = null
                    devOtpHint = null
                    currentScreen = if (role == "partner") Screen.PartnerDashboard else Screen.CustomerHome
                    loadNotifications()
                }
                .onFailure { authError = friendly(it) }
            authBusy = false
        }
    }

    /** Go back from the OTP-entry step to the phone/role step. */
    fun resetOtpFlow() {
        otpSent = false
        otpToken = null
        devOtpHint = null
        authError = null
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { repository.logout() }
            resetSessionState()
        }
    }

    /** §704 — Play-Store account deletion. Soft-deletes server-side, wipes all local
     *  state (same as logout), then drops back to the login/landing screen. */
    fun deleteAccount(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.deleteAccount() }
                .onSuccess {
                    notify("Your account has been deleted.")
                    resetSessionState()
                    onDone()
                }
                .onFailure { friendly(it) }
        }
    }

    /** Reset every in-VM cache/flag + session flags so the next account signing in
     *  on the SAME process (the VM isn't recreated until process death) never sees
     *  the previous account's data. Repo caches are cleared in [NikhatGlowRepository]. */
    private fun resetSessionState() {
        isLoggedIn = false
        isGuestMode = false
        otpSent = false
        otpToken = null
        loginRole = "customer"
        // §704 — clear in-VM caches that survive a logout (the repo clears its own).
        loadedThreads.clear()
        notifiedBookingIds.clear()
        _autoLocationAttempted = false
        detailStartOtp = null
        partnerKycReason = null
        quoteBreakdown = null
        availableSlots = emptyList()
        selectedSlotId = null
        bookingNotes = ""
        bookingGenderPref = "any"
        complaintDetail = null
        complaintMessages = emptyList()
        _deviceLocation.value = null
        clearNavHistory()
        stopLiveTracking()
        currentScreen = Screen.CustomerHome
    }

    /** A guest tapped a login-gated action — drop guest mode so the login screen
     *  shows. Role defaults to customer (guests browse the customer side). */
    fun triggerLoginPrompt() {
        isGuestMode = false
        otpSent = false
        loginRole = "customer"
    }

    private fun isScreenRestricted(screen: Screen): Boolean {
        return when (screen) {
            is Screen.CustomerHome -> false
            is Screen.CategoryDetail -> false
            is Screen.ServiceDetail -> false
            is Screen.PartnerSelect -> false
            is Screen.PartnerReviews -> false
            else -> true
        }
    }

    // §694 — app-wide transient messages (Snackbar/toast). The root composable
    // collects [uiMessages] and shows each one. Every error path already funnels
    // through friendly(), so routing it through ApiErrors + emitting here wires
    // user-friendly error toasts across the WHOLE app from one place. Successful
    // actions call notify(..., isError=false) explicitly where confirmation helps.
    data class UiMessage(val text: String, val isError: Boolean = false)

    private val _uiMessages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 16)
    val uiMessages = _uiMessages.asSharedFlow()

    fun notify(text: String, isError: Boolean = false) {
        if (text.isNotBlank()) _uiMessages.tryEmit(UiMessage(text, isError))
    }

    private fun friendly(t: Throwable): String {
        val msg = com.example.data.remote.ApiErrors.friendlyMessage(t)
        notify(msg, isError = true)
        return msg
    }

    // ── Push reminders (device-local preference) ───────────────────────────────
    var pushRemindersEnabled by mutableStateOf(getPushRemindersPref()); private set

    private fun getPushRemindersPref(): Boolean =
        getApplication<Application>().getSharedPreferences("nikhatglow_prefs", Context.MODE_PRIVATE)
            .getBoolean("push_reminders_enabled", true)

    fun updatePushReminders(enabled: Boolean) {
        pushRemindersEnabled = enabled
        getApplication<Application>().getSharedPreferences("nikhatglow_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("push_reminders_enabled", enabled).apply()
    }

    // ── Booking flow caches ──────────────────────────────────────────────────
    var selectedSlot by mutableStateOf("")          // customer's free-text preferred time
    var couponCode by mutableStateOf("")            // unused in connector model; kept for API compat
    var quoteBreakdown by mutableStateOf<QuoteBreakdown?>(null)

    // ── §702 real time-slot picker ─────────────────────────────────────────────
    var selectedBookingDate by mutableStateOf(java.time.LocalDate.now().toString()) // ISO yyyy-MM-dd
    var availableSlots by mutableStateOf<List<com.example.data.remote.SlotDto>>(emptyList())
    var selectedSlotId by mutableStateOf<String?>(null)
    var slotsLoading by mutableStateOf(false)

    /** Load real availability for a partner+service+date; clears any prior pick. */
    fun loadSlots(partnerId: String, serviceId: String?, date: String) {
        selectedBookingDate = date
        selectedSlotId = null
        slotsLoading = true
        viewModelScope.launch {
            val pid = partnerId.toIntOrNull() ?: 0
            val sid = serviceId?.toIntOrNull()
            availableSlots = repository.fetchAvailability(pid, sid, date)
            slotsLoading = false
        }
    }

    /** §702 — customer start-OTP shown on demand on the booking detail. */
    var detailStartOtp by mutableStateOf<String?>(null)
    fun loadStartOtp(bookingId: String) {
        viewModelScope.launch {
            val id = bookingId.toIntOrNull() ?: return@launch
            detailStartOtp = repository.fetchStartOtp(id)
        }
    }

    /** §702 — partner KYC rejection reason (Mapper never populates kycReason). */
    var partnerKycReason by mutableStateOf<String?>(null)
    fun loadPartnerKyc() {
        viewModelScope.launch {
            repository.fetchKyc()?.let { partnerKycReason = it.reason }
        }
    }

    // ── Cart (single-partner, multi-service) ───────────────────────────────────
    val cart = repository.cartFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    fun addToCart(partnerId: String, serviceId: String, onResult: (String?) -> Unit = {}) {
        if (!isLoggedIn) { triggerLoginPrompt(); return }
        viewModelScope.launch {
            runCatching { repository.addToCart(partnerId, serviceId) }
                .onSuccess { notify("Added to cart"); onResult(null) }
                .onFailure { onResult(friendly(it)) }
        }
    }

    fun updateCartQty(itemId: Int, qty: Int) {
        viewModelScope.launch { runCatching { repository.updateCartQty(itemId, qty) } }
    }

    fun removeCartItem(itemId: Int) {
        viewModelScope.launch { runCatching { repository.removeCartItem(itemId) } }
    }

    fun clearCart() {
        viewModelScope.launch { runCatching { repository.clearCart() } }
    }

    fun bookAgain(booking: com.example.data.BookingEntity) {
        if (!isLoggedIn) { triggerLoginPrompt(); return }
        viewModelScope.launch {
            runCatching {
                repository.clearCart()
                repository.addToCart(booking.partnerId, booking.serviceId)
            }.onSuccess {
                notify("Populated cart with '${booking.serviceName}'!")
                currentScreen = Screen.Cart
            }.onFailure {
                // ApiErrors.friendlyMessage does NOT toast (friendly() would), so
                // composing it here yields a single snackbar, not two.
                notify("Failed to book again: ${com.example.data.remote.ApiErrors.friendlyMessage(it)}", isError = true)
            }
        }
    }

    /**
     * Quote the whole cart, place the booking request, then open its detail.
     * Callers pass the explicitly chosen [addressId] (cart checkout flow); when null
     * we fall back to the default address (back-compat for older call sites).
     */
    fun checkoutCart(addressId: Long? = null, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                val chosenAddrId = addressId
                    ?: (addresses.value.firstOrNull { it.isDefault } ?: addresses.value.firstOrNull())?.id
                repository.cartQuote(couponCode, chosenAddrId, selectedSlotId)
                repository.createBookingFromLastQuote(
                    customerNotes = bookingNotes,
                    genderPreference = bookingGenderPref,
                    deviceInfo = deviceInfoJson(),
                )
            }.onSuccess { booking ->
                bookingNotes = ""; bookingGenderPref = "any"
                notify("Booking request sent")
                currentScreen = Screen.BookingDetail(booking.id)
                onResult(null)
            }.onFailure { onResult(friendly(it)) }
        }
    }

    // ── Partner Availability Engine ──────────────────────────────────────────
    var isPartnerActive by mutableStateOf(true)
    var partnerServiceRadiusKm by mutableStateOf(10.0)
    var partnerWorkingHoursRange by mutableStateOf("9:00 AM - 8:00 PM")

    /** Online/Away toggle → PATCH /partner/profile {is_active}. */
    fun setPartnerActive(active: Boolean) {
        isPartnerActive = active
        runPartnerAction(if (active) "You are now ONLINE" else "You are now AWAY") {
            repository.setPartnerActive(active)
        }
    }

    /** Service-radius → PATCH /partner/profile {travel_radius_km}. */
    fun savePartnerRadius(km: Double) {
        partnerServiceRadiusKm = km
        runPartnerAction("Service radius updated") { repository.setPartnerTravelRadius(km) }
    }

    /** Working-hours picker label → availability endpoint (24h HH:mm). */
    fun savePartnerWorkingHours(label: String) {
        partnerWorkingHoursRange = label
        val (start, end) = parseShiftLabel(label) ?: run {
            notify("Could not read those hours", isError = true); return
        }
        runPartnerAction("Working hours updated") { repository.setPartnerWorkingHours(start, end) }
    }

    /** "9:00 AM - 8:00 PM (Extended)" → ("09:00","20:00"); null if unparseable. */
    private fun parseShiftLabel(label: String): Pair<String, String>? {
        val parts = label.substringBefore("(").split("-").map { it.trim() }
        if (parts.size != 2) return null
        val s = to24h(parts[0]) ?: return null
        val e = to24h(parts[1]) ?: return null
        return s to e
    }

    private fun to24h(t: String): String? {
        val m = Regex("(\\d{1,2}):(\\d{2})\\s*([AaPp][Mm])").find(t.trim()) ?: return null
        var h = m.groupValues[1].toIntOrNull() ?: return null
        val min = m.groupValues[2]
        val ampm = m.groupValues[3].uppercase()
        if (ampm == "PM" && h != 12) h += 12
        if (ampm == "AM" && h == 12) h = 0
        return "%02d:%s".format(h, min)
    }

    private val loadedThreads = mutableSetOf<String>()
    fun getMessagesForBooking(bookingId: String): Flow<List<ChatMessageEntity>> {
        if (loadedThreads.add(bookingId)) {
            viewModelScope.launch { runCatching { repository.loadThread(bookingId) } }
        }
        return repository.getMessagesFlow(bookingId)
    }

    /**
     * Re-pull a chat thread on demand. Chat text only streams in when we send or
     * reopen otherwise, so the open chat view polls this to surface incoming
     * counterparty messages. Silently no-ops on transient failures.
     */
    fun refreshThread(bookingId: String) {
        viewModelScope.launch { runCatching { repository.loadThread(bookingId) } }
    }

    fun updateProfile(
        name: String,
        email: String,
        bio: String = "",
        experience: Int = 0,
        gender: String? = null,
        minimumOrderPaise: Long? = null,
        travelRadiusKm: Double? = null,
    ) {
        viewModelScope.launch {
            runCatching {
                repository.updateProfile(name, email, bio, experience, gender, minimumOrderPaise, travelRadiusKm)
            }
        }
    }

    // Customer beauty profile preferences (replaces simple state with persistent local storage)
    var customerSkinType by mutableStateOf(getApplication<Application>().getSharedPreferences("nikhat_prefs", Context.MODE_PRIVATE).getString("skin_type", "Normal") ?: "Normal")
        private set
    var customerBeautyConcerns by mutableStateOf(getApplication<Application>().getSharedPreferences("nikhat_prefs", Context.MODE_PRIVATE).getString("beauty_concerns", "") ?: "")
        private set
    var customerPreferredTime by mutableStateOf(getApplication<Application>().getSharedPreferences("nikhat_prefs", Context.MODE_PRIVATE).getString("pref_time", "No Preference") ?: "No Preference")
        private set

    fun updateBeautyProfile(skinType: String, concerns: String, prefTime: String) {
        customerSkinType = skinType
        customerBeautyConcerns = concerns
        customerPreferredTime = prefTime
        getApplication<Application>().getSharedPreferences("nikhat_prefs", Context.MODE_PRIVATE).edit()
            .putString("skin_type", skinType)
            .putString("beauty_concerns", concerns)
            .putString("pref_time", prefTime)
            .apply()
    }

    // ── §694 booking-time data capture (customer-set on the booking form) ──────
    var bookingNotes by mutableStateOf("")
    var bookingGenderPref by mutableStateOf("any")   // "any" | "male" | "female"

    /** Device descriptor sent with each booking for analytics. Backend wants an
     *  OBJECT (Optional[dict]) — so return a Map and let Moshi emit real JSON. */
    private fun deviceInfoJson(): Map<String, String?> {
        val model = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim()
        val os = android.os.Build.VERSION.RELEASE ?: ""
        return mapOf("platform" to "android", "os_version" to os, "model" to model)
    }

    // §687 — lat/lon now flow through from a real source: a "use current location"
    // save passes the device fix (or a search-suggestion's coords); a pure-manual
    // entry passes null. Pre-§687 this hardcoded Bangalore (12.9716, 77.5946) for
    // EVERY address — the root of the "location not working" bug.
    fun addNewAddress(
        label: String, line1: String, line2: String, city: String, pincode: String,
        lat: Double? = null, lon: Double? = null,
    ) {
        viewModelScope.launch {
            runCatching { repository.addAddress(label, line1, line2, city, pincode, lat, lon) }
                .onSuccess { notify("Address saved") }
        }
    }

    fun deleteAddress(id: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteAddress(id) }.onSuccess { notify("Address removed") }
        }
    }

    fun setDefaultAddress(id: Long) {
        viewModelScope.launch {
            runCatching { repository.setDefaultAddress(id) }.onSuccess { notify("Default address updated") }
        }
    }

    fun updateBookingQuote(service: Service, partner: Partner, customPricePaise: Long? = null) {
        viewModelScope.launch {
            val defaultAddr = addresses.value.firstOrNull { it.isDefault } ?: addresses.value.firstOrNull()
            runCatching {
                repository.createQuote(
                    partnerId = partner.id,
                    serviceId = service.id,
                    slotId = selectedSlotId,
                    addressId = defaultAddr?.id,
                    couponCode = couponCode,
                    useWallet = false,
                )
            }.onSuccess { quoteBreakdown = it }
        }
    }

    fun confirmAndBook(service: Service, partner: Partner, address: AddressEntity) {
        viewModelScope.launch {
            runCatching {
                // Re-quote with the slot the user actually picked. The initial
                // quote was built at PartnerSelect BEFORE any slot existed (so its
                // slot_id was null); without this the booking would be stored
                // time-less and "Change partner" would never unlock.
                repository.createQuote(
                    partnerId = partner.id,
                    serviceId = service.id,
                    slotId = selectedSlotId,
                    addressId = address.id,
                    couponCode = couponCode,
                    useWallet = false,
                ).also { quoteBreakdown = it }
                repository.createBookingFromLastQuote(
                    customerNotes = bookingNotes,
                    genderPreference = bookingGenderPref,
                    deviceInfo = deviceInfoJson(),
                )
            }.onSuccess { booking ->
                bookingNotes = ""; bookingGenderPref = "any"
                notify("Booking request sent")
                currentScreen = Screen.BookingDetail(booking.id)
            }
        }
    }

    fun bookDirectlyFromForm(service: Service, slot: String, onSuccess: (String) -> Unit) {
        selectedSlot = slot
        viewModelScope.launch {
            val partner = NikhatGlowDataSource.partners.firstOrNull { it.servicesOffered.contains(service.id) }
                ?: NikhatGlowDataSource.partners.firstOrNull()
            if (partner == null) return@launch
            val addr = addresses.value.firstOrNull { it.isDefault } ?: addresses.value.firstOrNull()
            runCatching {
                repository.createQuote(partner.id, service.id, null, addr?.id, null, false)
                repository.createBookingFromLastQuote(
                    customerNotes = bookingNotes,
                    genderPreference = bookingGenderPref,
                    deviceInfo = deviceInfoJson(),
                )
            }.onSuccess { booking ->
                bookingNotes = ""; bookingGenderPref = "any"
                notify("Booking request sent")
                currentScreen = Screen.BookingDetail(booking.id)
                onSuccess(booking.id)
            }
        }
    }

    fun sendChatMessage(bookingId: String, senderRole: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { runCatching { repository.sendThread(bookingId, text) } }
    }

    // ── Partner booking actions (error-surfacing wrappers) ─────────────────────
    fun acceptJob(id: String) = runPartnerAction("Appointment accepted") { repository.acceptBooking(id) }
    fun rejectJob(id: String) = runPartnerAction("Appointment declined") { repository.rejectBooking(id) }
    fun startTravelToJob(id: String) = runPartnerAction("On the way") { repository.startTravel(id) }
    fun arriveAtJob(id: String) = runPartnerAction("Marked arrived") { repository.arriveLocation(id) }
    fun completePartnerJob(id: String) = runPartnerAction("Job completed") { repository.completeJob(id) }

    /** Partner types the customer's start-OTP at the door; send it to the backend. */
    fun startJob(id: String, otp: String) {
        if (otp.isBlank()) { notify("Enter the customer's OTP to start", isError = true); return }
        runPartnerAction("Job started") { repository.startJob(id, otp.trim()) }
    }

    private fun runPartnerAction(successMsg: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { notify(successMsg) }
                .onFailure { friendly(it) }
        }
    }

    fun submitKyc(aadhaar: String, pan: String) {
        viewModelScope.launch {
            runCatching {
                // Backend KYC stores only aadhaar_no + pan_no; no real selfie capture here.
                repository.submitKyc(aadhaar, pan)
            }.onSuccess { notify("KYC submitted — pending admin approval") }
                .onFailure { friendly(it) }
        }
    }

    fun setPartnerServicePrice(serviceId: String, name: String, category: String, pricePaise: Long, active: Boolean, productsUsed: String) {
        viewModelScope.launch {
            runCatching { repository.setServicePrice(serviceId, pricePaise, active, productsUsed) }
                .onSuccess { notify("Service updated") }
        }
    }

    fun createCustomPartnerService(name: String, categoryName: String, pricePaise: Long, durationMin: Int, description: String, productsUsed: String) {
        val nextId = "srv_custom_${System.currentTimeMillis()}"
        
        // Find or create category if it doesn't exist, else assign to "Salon"
        val categoryId = when (categoryName.lowercase()) {
            "salon" -> "cat_salon"
            "makeup" -> "cat_makeup"
            "beauty" -> "cat_beauty"
            "massage" -> "cat_massage"
            else -> "cat_salon"
        }
        
        val newSrv = Service(
            id = nextId,
            categoryId = categoryId,
            name = name,
            description = description,
            pricePaise = pricePaise,
            durationMin = durationMin,
            rating = 5.0f,
            reviewsCount = 1,
            inclusions = listOf("Expert consult", "Custom service delivery", "Premium seal checked products"),
            faqs = listOf("Is this verified?" to "Yes, 100% verified by Nikhat Glow quality team."),
            imageUrl = "https://images.unsplash.com/photo-1560066984-138dadb4c035?auto=format&fit=crop&q=80&w=300",
            priceMinPaise = pricePaise,
            priceMaxPaise = pricePaise,
            partnerCount = 1
        )
        
        // Update global datasource services list
        NikhatGlowDataSource.services = NikhatGlowDataSource.services + newSrv
        
        // Also add to active settings for this partner
        viewModelScope.launch {
            val customEntity = PartnerServiceEntity(
                id = "me_$nextId",
                serviceId = nextId,
                name = name,
                categoryName = categoryName,
                pricePaise = pricePaise,
                durationMin = durationMin,
                active = true,
                productsUsed = productsUsed
            )
            repository.insertLocalPartnerService(customEntity)
            notify("Custom service '$name' successfully added to your menu!")
        }
    }

    fun cancelBooking(bookingId: String, reason: String) {
        viewModelScope.launch {
            runCatching { repository.cancelBooking(bookingId, reason) }
                .onSuccess {
                    notify("Booking cancelled successfully")
                    refreshActiveBookings()
                    currentScreen = Screen.MyBookings
                }
                .onFailure {
                    // friendlyMessage doesn't toast → one snackbar, not two.
                    notify("Failed to cancel: ${com.example.data.remote.ApiErrors.friendlyMessage(it)}", isError = true)
                }
        }
    }

    /** §704 — "I feel unsafe" cancel (either role): no penalty, surfaces the women
     *  helpline so she can call as she leaves. */
    fun cancelBookingUnsafe(bookingId: String) {
        viewModelScope.launch {
            runCatching { repository.cancelBooking(bookingId, "Felt unsafe", "felt_unsafe") }
                .onSuccess {
                    notify("Booking cancelled. You can leave safely — Women helpline ${repository.womenHelpline()}.",
                           isError = true)
                    refreshActiveBookings()
                }
                .onFailure { notify(friendly(it), isError = true) }
        }
    }

    /** §704 — block/report a partner: permanently cuts off all their contact. */
    fun blockPartner(partnerId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.blockPartner(partnerId) }
                .onSuccess { notify("This professional has been blocked and reported."); onDone() }
                .onFailure { notify(friendly(it), isError = true) }
        }
    }

    /** §704 — the admin-editable emergency numbers + women helpline (from /config). */
    fun emergencyNumbers(): List<String> = repository.emergencyNumbers()
    fun womenHelpline(): String = repository.womenHelpline()

    /** §704 — reschedule a pending/accepted booking to the [slotId] the customer
     *  picked (reuses loadSlots/availableSlots/selectedSlotId). Surfaces 409s
     *  (RESCHEDULE_WINDOW_CLOSED / SLOT_TAKEN / SLOT_PAST) via the friendly path. */
    fun rescheduleBooking(bookingId: String, slotId: String, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            // Repo returns null on failure (it swallows the throwable) — surface the
            // 409 reasons (RESCHEDULE_WINDOW_CLOSED / SLOT_TAKEN / SLOT_PAST) clearly.
            val updated = runCatching { repository.rescheduleBookingChecked(bookingId, slotId) }
            updated
                .onSuccess {
                    notify("Appointment rescheduled")
                    refreshActiveBookings()
                    selectedSlotId = null
                    onResult(null)
                }
                .onFailure { onResult(friendly(it)) }
        }
    }

    fun submitBookingReview(bookingId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            runCatching { repository.addReview(bookingId, rating, comment) }
                .onSuccess { notify("Thanks for your review") }
        }
    }

    fun submitComplaint(bookingId: String?, subject: String, message: String) {
        viewModelScope.launch {
            runCatching { repository.createComplaint(bookingId, subject, message) }
                .onSuccess { notify("Complaint submitted") }
        }
    }

    /** Pull the latest complaints/tickets for the support-desk list. */
    fun refreshComplaints() {
        viewModelScope.launch { runCatching { repository.refreshComplaints() } }
    }

    // ── Live booking detail refresh ─────────────────────────────────────────────
    /** Pull fresh status for the open booking-detail screen. */
    fun loadBookingDetail(id: String) {
        viewModelScope.launch { runCatching { repository.refreshBooking(id) } }
    }

    // ── Partner: delete a listed service ────────────────────────────────────────
    fun deletePartnerService(id: String) {
        viewModelScope.launch {
            runCatching { repository.deletePartnerService(id) }
                .onSuccess { notify("Service removed") }
                .onFailure { notify(friendly(it)) }
        }
    }

    // ── Complaint detail thread + reply ─────────────────────────────────────────
    var complaintDetail by mutableStateOf<com.example.data.remote.ComplaintDto?>(null); private set
    var complaintMessages by mutableStateOf<List<com.example.data.remote.ComplaintMessageDto>>(emptyList()); private set
    var complaintReplyBusy by mutableStateOf(false); private set

    fun openComplaint(id: String) {
        viewModelScope.launch {
            runCatching { repository.loadComplaint(id) }
                .onSuccess { dto ->
                    complaintDetail = dto
                    complaintMessages = dto?.messages ?: emptyList()
                }
                .onFailure { notify(friendly(it)) }
        }
    }

    fun sendComplaintReply(id: String, text: String) {
        if (text.isBlank()) return
        complaintReplyBusy = true
        viewModelScope.launch {
            runCatching { repository.replyComplaint(id, text) }
                .onSuccess { openComplaint(id) }
                .onFailure { notify(friendly(it)) }
            complaintReplyBusy = false
        }
    }
}
