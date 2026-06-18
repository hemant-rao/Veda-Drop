package com.example.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class Screen {
    object CustomerHome : Screen()
    data class CategoryDetail(val category: Category) : Screen()
    data class ServiceDetail(val service: Service) : Screen()
    data class PartnerSelect(val service: Service) : Screen()
    data class BookingConfirm(val service: Service, val partner: Partner) : Screen(), RouteWithParams
    data class BookingDetail(val bookingId: String) : Screen(), RouteWithParams
    object Cart : Screen()
    object MyBookings : Screen()
    object ComplaintsList : Screen()

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

    // Pre-booking messaging
    data class PreBookingChat(val service: Service, val partner: Partner) : Screen()
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
                .onFailure { subscriptionError = friendly(it) }
            subscriptionBusy = false
        }
    }

    fun cancelSubscriptionNow() {
        subscriptionBusy = true; subscriptionError = null
        viewModelScope.launch {
            runCatching { repository.cancelSubscription() }
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
    fun captureDeviceLocation(onResult: ((Pair<Double, Double>?) -> Unit)? = null) {
        viewModelScope.launch {
            val loc = com.example.data.LocationHelper.current(getApplication())
            if (loc != null) {
                _deviceLocation.value = loc
                runCatching { repository.hydrateCatalog(loc.first, loc.second) }
            }
            onResult?.invoke(loc)
        }
    }

    /** Address search-as-you-type via our Ola Maps proxy. */
    suspend fun searchPlaces(q: String) =
        if (q.isBlank()) emptyList() else repository.geoAutocomplete(q, _deviceLocation.value?.first, _deviceLocation.value?.second)

    /** Turn a GPS fix into a human-readable address (prefill the manual form). */
    suspend fun reverseGeocode(lat: Double, lon: Double) = repository.geoReverse(lat, lon)

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

    init {
        if (repository.isAuthenticated()) {
            val role = repository.activeRole() ?: "customer"
            currentScreen = if (role == "partner") Screen.PartnerDashboard else Screen.CustomerHome
            viewModelScope.launch { runCatching { repository.hydrateForRole(role) } }
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
            isLoggedIn = false
            isGuestMode = false
            otpSent = false
            otpToken = null
            loginRole = "customer"
            currentScreen = Screen.CustomerHome
        }
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

    private fun friendly(t: Throwable): String =
        t.message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."

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

    // ── Cart (single-partner, multi-service) ───────────────────────────────────
    val cart = repository.cartFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    fun addToCart(partnerId: String, serviceId: String, onResult: (String?) -> Unit = {}) {
        if (!isLoggedIn) { triggerLoginPrompt(); return }
        viewModelScope.launch {
            runCatching { repository.addToCart(partnerId, serviceId) }
                .onSuccess { onResult(null) }
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

    /** Quote the whole cart, place the booking request, then open its detail. */
    fun checkoutCart(onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                val addr = addresses.value.firstOrNull { it.isDefault } ?: addresses.value.firstOrNull()
                repository.cartQuote(couponCode, addr?.id)
                repository.createBookingFromLastQuote()
            }.onSuccess { booking ->
                currentScreen = Screen.BookingDetail(booking.id)
                onResult(null)
            }.onFailure { onResult(friendly(it)) }
        }
    }

    // ── Partner Availability Engine ──────────────────────────────────────────
    var isPartnerActive by mutableStateOf(true)
    var partnerServiceRadiusKm by mutableStateOf(10.0)
    var partnerWorkingHoursRange by mutableStateOf("9:00 AM - 8:00 PM")

    private val loadedThreads = mutableSetOf<String>()
    fun getMessagesForBooking(bookingId: String): Flow<List<ChatMessageEntity>> {
        if (loadedThreads.add(bookingId)) {
            viewModelScope.launch { runCatching { repository.loadThread(bookingId) } }
        }
        return repository.getMessagesFlow(bookingId)
    }

    fun updateProfile(name: String, email: String, bio: String = "", experience: Int = 0) {
        viewModelScope.launch { runCatching { repository.updateProfile(name, email, bio, experience) } }
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
        }
    }

    fun deleteAddress(id: Long) {
        viewModelScope.launch { runCatching { repository.deleteAddress(id) } }
    }

    fun setDefaultAddress(id: Long) {
        viewModelScope.launch { runCatching { repository.setDefaultAddress(id) } }
    }

    fun updateBookingQuote(service: Service, partner: Partner, customPricePaise: Long? = null) {
        viewModelScope.launch {
            val defaultAddr = addresses.value.firstOrNull { it.isDefault } ?: addresses.value.firstOrNull()
            runCatching {
                repository.createQuote(
                    partnerId = partner.id,
                    serviceId = service.id,
                    slotId = null,
                    addressId = defaultAddr?.id,
                    couponCode = couponCode,
                    useWallet = false,
                )
            }.onSuccess { quoteBreakdown = it }
        }
    }

    fun confirmAndBook(service: Service, partner: Partner, address: AddressEntity) {
        viewModelScope.launch {
            runCatching { repository.createBookingFromLastQuote() }
                .onSuccess { booking -> currentScreen = Screen.BookingDetail(booking.id) }
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
                repository.createBookingFromLastQuote()
            }.onSuccess { booking ->
                currentScreen = Screen.BookingDetail(booking.id)
                onSuccess(booking.id)
            }
        }
    }

    fun sendChatMessage(bookingId: String, senderRole: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { runCatching { repository.sendThread(bookingId, text) } }
    }

    fun submitKyc(aadhaar: String, pan: String) {
        viewModelScope.launch {
            runCatching {
                repository.submitKyc(aadhaar, pan, "selfie_dev")
            }
        }
    }

    fun setPartnerServicePrice(serviceId: String, name: String, category: String, pricePaise: Long, active: Boolean, productsUsed: String) {
        viewModelScope.launch { runCatching { repository.setServicePrice(serviceId, pricePaise, active, productsUsed) } }
    }

    fun submitBookingReview(bookingId: String, rating: Int, comment: String) {
        viewModelScope.launch { runCatching { repository.addReview(bookingId, rating, comment) } }
    }

    fun submitComplaint(bookingId: String, subject: String, message: String) {
        viewModelScope.launch { runCatching { repository.createComplaint(bookingId, subject, message) } }
    }
}
