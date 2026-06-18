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
    object Wallet : Screen()
    object ComplaintsList : Screen()
    object AIChat : Screen()

    object CustomerProfile : Screen()
    object ServiceBookingForm : Screen()

    // Partner screens
    object PartnerDashboard : Screen()
    object PartnerKyc : Screen()
    object PartnerServices : Screen()
    object PartnerEarnings : Screen()

    // Pre-booking messaging
    data class PreBookingChat(val service: Service, val partner: Partner) : Screen()
}

interface RouteWithParams

/**
 * Online GlamGo ViewModel. All data is server-backed via [GlamGoRepository];
 * the public surface (flows + methods) is unchanged from the offline version so
 * the Compose screens are untouched. Adds OTP-login session state.
 */
class GlamGoViewModel(application: Application) : AndroidViewModel(application) {
    val repository = GlamGoRepository(application)

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
    var currentScreen: Screen
        get() = _currentScreen
        set(value) {
            if (!isLoggedIn && isGuestMode && isScreenRestricted(value)) {
                triggerLoginPrompt()
            } else {
                _currentScreen = value
            }
        }
    var onboardingComplete by mutableStateOf(true)

    // ── Auth / session ─────────────────────────────────────────────────────────
    var isLoggedIn by mutableStateOf(repository.isAuthenticated())
        private set
    var isGuestMode by mutableStateOf(false)
    var pendingLoginRole by mutableStateOf<String?>(null)   // non-null → show login for this role
        private set
    var authBusy by mutableStateOf(false); private set
    var authError by mutableStateOf<String?>(null); private set
    var otpSent by mutableStateOf(false); private set
    var devOtpHint by mutableStateOf<String?>(null); private set
    private var otpToken: String? = null

    val loginRole: String get() = pendingLoginRole ?: "customer"

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
                    pendingLoginRole = null
                    otpSent = false
                    otpToken = null
                    devOtpHint = null
                    currentScreen = if (role == "partner") Screen.PartnerDashboard else Screen.CustomerHome
                }
                .onFailure { authError = friendly(it) }
            authBusy = false
        }
    }

    fun cancelPendingLogin() {
        pendingLoginRole = null
        otpSent = false
        otpToken = null
        authError = null
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { repository.logout() }
            isLoggedIn = false
            isGuestMode = false
            otpSent = false
            currentScreen = Screen.CustomerHome
        }
    }

    fun triggerLoginPrompt() {
        pendingLoginRole = "customer"
    }

    private fun isScreenRestricted(screen: Screen): Boolean {
        return when (screen) {
            is Screen.CustomerHome -> false
            is Screen.CategoryDetail -> false
            is Screen.ServiceDetail -> false
            is Screen.PartnerSelect -> false
            is Screen.AIChat -> false
            else -> true
        }
    }

    private fun friendly(t: Throwable): String =
        t.message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."

    // ── Push reminders (device-local preference) ───────────────────────────────
    var pushRemindersEnabled by mutableStateOf(getPushRemindersPref()); private set

    private fun getPushRemindersPref(): Boolean =
        getApplication<Application>().getSharedPreferences("glamgo_prefs", Context.MODE_PRIVATE)
            .getBoolean("push_reminders_enabled", true)

    fun updatePushReminders(enabled: Boolean) {
        pushRemindersEnabled = enabled
        getApplication<Application>().getSharedPreferences("glamgo_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("push_reminders_enabled", enabled).apply()
    }

    // ── Booking flow caches ──────────────────────────────────────────────────
    var selectedSlot by mutableStateOf("Tomorrow, 10:00 AM - 11:30 AM")
    var couponCode by mutableStateOf("")
    var applyWalletDiscount by mutableStateOf(true)
    var quoteBreakdown by mutableStateOf<QuoteBreakdown?>(null)

    // ── Partner Availability Engine ──────────────────────────────────────────
    var isPartnerActive by mutableStateOf(true)
    var partnerServiceRadiusKm by mutableStateOf(10.0)
    var partnerWorkingHoursRange by mutableStateOf("9:00 AM - 8:00 PM")

    // ── AI chat ─────────────────────────────────────────────────────────────────
    private val _aiMessages = MutableStateFlow<List<Pair<String, Boolean>>>(
        listOf("Hello! I am your Nikhat Glow Beauty AI assistant. Ask me for custom style suggestions, fragrance palettes, haircut advice, or body spa recommendations!" to false)
    )
    val aiMessages = _aiMessages.asStateFlow()
    var aiLoading by mutableStateOf(false)

    private val loadedThreads = mutableSetOf<String>()
    fun getMessagesForBooking(bookingId: String): Flow<List<ChatMessageEntity>> {
        if (loadedThreads.add(bookingId)) {
            viewModelScope.launch { runCatching { repository.loadThread(bookingId) } }
        }
        return repository.getMessagesFlow(bookingId)
    }

    fun switchRole(newRole: String) {
        viewModelScope.launch {
            val ok = runCatching { repository.switchRole(newRole) }.getOrDefault(false)
            if (ok) {
                currentScreen = if (newRole == "customer") Screen.CustomerHome else Screen.PartnerDashboard
            } else {
                // No session for that identity yet — prompt an OTP login for it.
                pendingLoginRole = newRole
                otpSent = false
            }
        }
    }

    fun updateProfile(name: String, email: String, bio: String = "", experience: Int = 0) {
        viewModelScope.launch { runCatching { repository.updateProfile(name, email, bio, experience) } }
    }

    fun addWalletMoney(amountPaise: Long, role: String) {
        viewModelScope.launch { runCatching { repository.addWalletMoney(amountPaise, role) } }
    }

    fun addNewAddress(label: String, line1: String, line2: String, city: String, pincode: String) {
        viewModelScope.launch {
            runCatching { repository.addAddress(label, line1, line2, city, pincode, 12.9716, 77.5946) }
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
                    useWallet = applyWalletDiscount,
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
            val partner = GlamMockDataSource.partners.firstOrNull { it.servicesOffered.contains(service.id) }
                ?: GlamMockDataSource.partners.firstOrNull()
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

    fun sendAiMessage(message: String) {
        if (message.isBlank()) return
        val log = _aiMessages.value.toMutableList()
        log.add(message to true)
        _aiMessages.value = log
        aiLoading = true
        viewModelScope.launch {
            val history = _aiMessages.value.map { (txt, isUser) ->
                mapOf("role" to (if (isUser) "user" else "model"), "text" to txt)
            }
            val reply = runCatching { repository.aiChat(message, history) }
                .getOrDefault("Sorry, I couldn't reach the stylist right now. Please try again.")
            val updated = _aiMessages.value.toMutableList()
            updated.add(reply to false)
            _aiMessages.value = updated
            aiLoading = false
        }
    }

    fun clearAiLog() {
        _aiMessages.value = listOf(
            "Hello! I am your Nikhat Glow Beauty AI assistant. Ask me for custom style suggestions, fragrance palettes, haircut advice, or body spa recommendations!" to false
        )
    }
}
