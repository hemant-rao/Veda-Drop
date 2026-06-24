package com.example.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.UrgentAlarmService
import com.example.data.*
import com.example.data.remote.GeoAppConfigDto
import com.example.data.remote.LiveTrackingSocket
import com.example.data.remote.isUrgentOffer
import com.example.ui.map.GeoPoint
import com.example.ui.map.VedaDropMaps
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed class Screen {
    object CustomerHome : Screen()
    // Dedicated global search-results screen (the ONE place search lives).
    data class SearchResults(val query: String = "") : Screen()
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
    // §713 — partner business location (GPS / search + service-radius slider).
    object PartnerBusinessLocation : Screen()
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
 * Online VedaDrop ViewModel. All data is server-backed via [VedaDropRepository];
 * the public surface (flows + methods) is unchanged from the offline version so
 * the Compose screens are untouched. Adds OTP-login session state.
 */
class VedaDropViewModel(application: Application) : AndroidViewModel(application) {
    val repository = VedaDropRepository(application)

    // Serialises the createQuote()+createBookingFromLastQuote() sequence against
    // any other quote call. createQuote stores its result in the repository's
    // shared `lastQuoteId`; without this lock an updateBookingQuote() coroutine
    // could re-quote (clobbering lastQuoteId) in between confirmAndBook's quote
    // and booking calls, so the booking would consume the wrong quote.
    private val quoteMutex = Mutex()

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
        notes: String? = null,
        // §729 (parity C2) — opt-in flexible arrival window for an open (pool) booking.
        flexible: Boolean = false,
        onResult: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            runCatching {
                repository.createOpenBooking(
                    serviceLines, slotStartIso, addressId, lat, lon, notes,
                    flexible = flexible && flexibleSlotsEnabled(),
                )
            }.onSuccess {
                notify(it.message ?: "Booking sent.", isError = !it.dispatched)
                onResult(it.dispatched)
            }.onFailure { friendly(it); onResult(false) }
        }
    }

    /** §703 — customer confirms the visit (satisfies the pre-visit safety gate so
     *  the partner may start travelling). */
    fun confirmVisit(bookingId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.confirmVisit(bookingId) }
                .onSuccess { notify("Visit confirmed — your professional can head out."); onDone() }
                .onFailure { friendly(it) }
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
                .onFailure { friendly(it) }
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

    // §731 — track which in-app notifications we've already shown as a system
    // notification (so we never buzz twice), with a startup grace so the inbox the
    // user already had is never re-announced on launch.
    private val seenNotifIds = mutableSetOf<Int>()
    private var notifBaselineDone = false

    /** Post a NEW unread in-app notification as an Android system notification, so a
     *  booking update or a chat message is seen immediately while the app is alive
     *  (foreground or backgrounded-but-running) — not only when the app is opened.
     *  Honours the user's push toggle and deep-links into the referenced booking /
     *  complaint when tapped. True CLOSED-app push still needs Firebase creds
     *  (google-services.json + a backend service-account). */
    private fun postLocalNotification(n: com.example.data.remote.NotificationDto) {
        runCatching {
            val ctx = getApplication<Application>()
            val remindersOn = ctx.getSharedPreferences("nikhatglow_prefs", Context.MODE_PRIVATE)
                .getBoolean("push_reminders_enabled", true)
            if (!remindersOn) return
            val intent = android.content.Intent(ctx, com.example.MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                n.type?.let { putExtra("notif_type", it) }
                n.data?.bookingId?.let { putExtra("notif_booking_id", it.toString()) }
                n.data?.complaintId?.let { putExtra("notif_complaint_id", it.toString()) }
            }
            val pending = android.app.PendingIntent.getActivity(
                ctx, n.id, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val notif = androidx.core.app.NotificationCompat.Builder(ctx, com.example.MainActivity.FCM_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(n.title?.ifBlank { "Veda Drop" } ?: "Veda Drop")
                .setContentText(n.body ?: "")
                .setAutoCancel(true)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pending)
                .build()
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager)
                .notify(n.id, notif)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch { runCatching { repository.markAllNotificationsRead() } }
    }

    fun markNotificationRead(id: Int) {
        viewModelScope.launch { runCatching { repository.markNotificationRead(id) } }
    }

    /** §709 — handle a notification tap: mark it read AND deep-link to whatever
     *  it refers to. The backend puts the target id in `data` (booking_id /
     *  complaint_id / offer_id). Types without a resolvable target just mark
     *  read (no dead-end). `reassignment_offer` is partner-only → Rescue Board. */
    fun openNotification(n: com.example.data.remote.NotificationDto) {
        if (!n.read) markNotificationRead(n.id)
        val d = n.data
        when (n.type) {
            "reassignment_offer" -> currentScreen = Screen.PartnerOffers
            "complaint" -> d?.complaintId?.let { currentScreen = Screen.ComplaintDetail(it.toString()) }
            // booking_update, chat, review, talk_request all carry booking_id
            else -> d?.bookingId?.let { currentScreen = Screen.BookingDetail(it.toString()) }
        }
    }

    /** §709 — true while ANY tracked action is in flight. Drives the global thin
     *  top loading bar in the app shell ("something is loading"). Reading these
     *  State-backed flags here makes the shell recompose as they flip. */
    val anyBusy: Boolean
        get() = authBusy || subscriptionBusy || reassignmentBusy || portfolioBusy ||
                availabilityBusy || complaintReplyBusy

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
    // §710 #27 — reason the offers board is empty (renew / finish KYC / no jobs).
    val offersEmptyMessage = repository.offersEmptyMessageFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
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

    // §725 Batch-B — urgent-offer alarm coordination. We remember which urgent offer
    // ids we've ALREADY rung for, so the 20s poll doesn't re-trigger the alarm for the
    // same job every cycle, and so a "viewed" state truly silences it until a NEW urgent
    // job appears. Cleared on logout via resetSessionState (offers list empties).
    private val _ringingUrgentOfferIds = mutableSetOf<Int>()
    private var _urgentOffersOnScreen = false

    fun loadOffers() {
        viewModelScope.launch {
            runCatching { repository.loadOffers() }
            evaluateUrgentOffers()
        }
    }

    /** Start/stop the high-alert alarm based on the freshly-loaded offers list. Called
     *  after every offers refresh AND drivable from an FCM urgent push. Foreground-only
     *  ring decision (FCM handles backgrounded). Safe to call repeatedly (idempotent). */
    private fun evaluateUrgentOffers() {
        val app = getApplication<Application>()
        val open = offers.value.filter { it.status == "open" }
        val urgent = open.filter { it.isUrgentOffer() }
        // Drop ids that are no longer open (claimed/taken/expired) so they can re-ring
        // if the same booking is ever re-broadcast as urgent later.
        val openIds = open.map { it.offerId }.toSet()
        _ringingUrgentOfferIds.retainAll(openIds)

        if (urgent.isEmpty()) {
            // Nothing urgent anymore → silence.
            UrgentAlarmService.stop(app)
            _ringingUrgentOfferIds.clear()
            return
        }
        // If the partner is already looking at the Rescue Board, never ring — they're
        // viewing it. Just mark these as seen so we don't ring when they navigate away.
        if (_urgentOffersOnScreen) {
            _ringingUrgentOfferIds.addAll(urgent.map { it.offerId })
            UrgentAlarmService.stop(app)
            return
        }
        // Ring only if there's at least one urgent offer we haven't rung for yet.
        val hasNew = urgent.any { it.offerId !in _ringingUrgentOfferIds }
        if (hasNew) {
            _ringingUrgentOfferIds.addAll(urgent.map { it.offerId })
            UrgentAlarmService.start(app)
        }
    }

    /** §725 — the Rescue Board (Open Jobs) is now on screen → stop the alarm + remember
     *  these urgent offers as seen so re-loads don't re-ring them. */
    fun onUrgentOffersViewed() {
        _urgentOffersOnScreen = true
        _ringingUrgentOfferIds.addAll(offers.value.filter { it.isUrgentOffer() }.map { it.offerId })
        UrgentAlarmService.stop(getApplication())
    }

    /** §725 — the Rescue Board left the screen; future urgent offers may ring again. */
    fun onUrgentOffersLeft() { _urgentOffersOnScreen = false }

    /** §725 — open the Rescue Board (from a tapped urgent push / full-screen intent) and
     *  silence the alarm. */
    fun openUrgentOffers() {
        currentScreen = Screen.PartnerOffers
        onUrgentOffersViewed()
    }

    fun acceptOffer(offerId: Int, onResult: (String?) -> Unit = {}) {
        // §725 — claiming silences the alarm locally (the job is being handled).
        UrgentAlarmService.stop(getApplication())
        viewModelScope.launch {
            runCatching { repository.acceptOffer(offerId) }
                .onSuccess { reassignmentToast = "Job claimed — it's yours."; onResult(null); evaluateUrgentOffers() }
                .onFailure { val m = friendly(it); reassignmentToast = m; onResult(m); evaluateUrgentOffers() }
        }
    }

    fun declineOffer(offerId: Int) {
        viewModelScope.launch {
            runCatching { repository.declineOffer(offerId) }
            evaluateUrgentOffers()
        }
    }

    /** Poll the live reassignment status for a booking (null-safe). */
    suspend fun fetchReassignmentStatus(bookingId: String) =
        repository.reassignmentStatus(bookingId)

    var bookingsLoading by mutableStateOf(false)
        private set

    /** Refresh the active bookings list (used to poll a reassigning booking). */
    fun refreshActiveBookings() {
        val role = repository.activeRole() ?: "customer"
        bookingsLoading = true
        viewModelScope.launch {
            runCatching { repository.refreshBookings(role) }
            bookingsLoading = false
        }
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
        viewModelScope.launch {
            runCatching { repository.loadAvailability() }
            // §710 #18 — seed the online/away toggle from the SERVER state (was a hardcoded
            // `true` default that went stale after restart, mismatching reality).
            repository.availabilityFlow.value?.isOnline?.let { isPartnerActive = it }
        }
    }

    fun saveAvailability(
        start: String,
        end: String,
        days: List<Int>? = null,        // §714 pda-7day-clobbers-weekly-1 — omittable
        leaves: List<String>? = null,
        hourOverrides: Map<String, List<Int>> = emptyMap(),
    ) {
        availabilityBusy = true; availabilityError = null; availabilitySaved = false
        viewModelScope.launch {
            runCatching { repository.saveAvailability(start, end, days, leaves, hourOverrides) }
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

    /** §714 cust-catalog-1 — fetch the full service detail (inclusions the list omits)
     *  and hand it back to the detail screen. No-op on a bad id / network failure. */
    fun loadServiceDetail(id: String, onLoaded: (com.example.data.Service) -> Unit) {
        val sid = id.toIntOrNull() ?: return
        viewModelScope.launch { repository.fetchServiceDetail(sid)?.let(onLoaded) }
    }

    /** §729 (parity C2) — "Frequently booked together" for a service. Hands the caller
     *  the related services (empty ⇒ the smart-add-ons row hides). No-op on a bad id. */
    fun loadRelatedServices(id: String, onLoaded: (List<com.example.data.Service>) -> Unit) {
        val sid = id.toIntOrNull() ?: run { onLoaded(emptyList()); return }
        viewModelScope.launch { onLoaded(repository.relatedServices(sid)) }
    }

    // §710 P0-8 — { serviceId(String): pricePaise } for the open partner store, so each
    // menu row shows that partner's real price instead of one shared "from" price.
    var partnerServicePrices by mutableStateOf<Map<String, Long>>(emptyMap())
        private set

    fun loadPartnerServicePrices(partnerId: String) {
        viewModelScope.launch {
            partnerServicePrices = repository.loadPartnerServicePrices(partnerId)
            // §743 — also load the rich per-offering details (discount/duration/products).
            partnerPricedRich = repository.loadPartnerPricedServicesRich(partnerId)
        }
    }

    // §743 — full per-offering details keyed by serviceId(String): discount, duration,
    // products (with hygiene tags), hygiene note. Drives the service-richness UI.
    var partnerPricedRich by mutableStateOf<Map<String, com.example.data.remote.PartnerPricedServiceDto>>(emptyMap())
        private set

    // §743 — a parlour's verified experts ("who is coming"), shown on the profile.
    var partnerExperts by mutableStateOf<List<com.example.data.Expert>>(emptyList())
        private set

    // §743 — the expert the customer picked for the booking-in-progress (null = none /
    // "any available"). Threaded into createBookingFromLastQuote at checkout.
    var selectedExpertId by mutableStateOf<Int?>(null)

    fun loadPartnerExperts(partnerId: String) {
        viewModelScope.launch { partnerExperts = repository.loadPartnerExperts(partnerId) }
    }

    // §743 — chat-after-booking gate result for the currently-open partner. Null until
    // checked; the Chat button reads it to either open chat or show "book first".
    var canChatResult by mutableStateOf<com.example.data.remote.CanChatResp?>(null)
    var showBookFirstDialog by mutableStateOf(false)

    /** Check the gate, then either run [onAllowed] or pop the "book first" dialog. */
    fun chatGateThen(partnerId: String, onAllowed: () -> Unit) {
        viewModelScope.launch {
            val r = repository.canChatWithPartner(partnerId)
            canChatResult = r
            if (r.canChat) onAllowed() else showBookFirstDialog = true
        }
    }

    // §743 — partner-side expert manager (full DTO incl. KYC status/docs).
    var myExperts by mutableStateOf<List<com.example.data.remote.ExpertDto>>(emptyList())
        private set
    var expertBusy by mutableStateOf(false); private set
    var expertError by mutableStateOf<String?>(null)

    fun loadMyExperts() {
        viewModelScope.launch { myExperts = repository.myExperts() }
    }

    fun addExpert(req: com.example.data.remote.ExpertReq, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            expertBusy = true; expertError = null
            try {
                repository.addExpert(req)
                myExperts = repository.myExperts()
                onDone()
            } catch (e: Exception) {
                expertError = com.example.data.remote.ApiErrors.friendlyMessage(e)
            } finally { expertBusy = false }
        }
    }

    fun deleteExpert(id: Int) {
        viewModelScope.launch {
            expertBusy = true; expertError = null
            try {
                repository.deleteExpert(id)
                myExperts = repository.myExperts()
            } catch (e: Exception) {
                expertError = com.example.data.remote.ApiErrors.friendlyMessage(e)
            } finally { expertBusy = false }
        }
    }

    // §745 — re-submit a (typically rejected) expert's KYC photos → backend flips her
    // kyc_status back to 'pending' for fresh admin review (keeps her record + history).
    fun resubmitExpertKyc(id: Int, selfieUrl: String?, idDocUrl: String?) {
        viewModelScope.launch {
            expertBusy = true; expertError = null
            try {
                val patch = buildMap<String, Any?> {
                    if (selfieUrl != null) put("kyc_selfie_url", selfieUrl)
                    if (idDocUrl != null) put("kyc_id_doc_url", idDocUrl)
                }
                if (patch.isNotEmpty()) repository.patchExpert(id, patch)
                myExperts = repository.myExperts()
                notify("Expert KYC resubmitted for review.")
            } catch (e: Exception) {
                expertError = com.example.data.remote.ApiErrors.friendlyMessage(e)
            } finally { expertBusy = false }
        }
    }

    // §744 — the parlour assigns/re-assigns a specific expert to a booking.
    fun assignExpert(bookingId: String, expertId: Int) {
        viewModelScope.launch {
            runCatching { repository.assignExpert(bookingId, expertId) }
                .onSuccess { notify("Expert assigned to the booking.") }
                .onFailure { notify("Could not assign expert: ${com.example.data.remote.ApiErrors.friendlyMessage(it)}", isError = true) }
        }
    }

    // §743 — sample professional descriptions suggested by the partner's categories.
    var descriptionSamples by mutableStateOf<List<com.example.data.remote.DescriptionTemplateDto>>(emptyList())
        private set

    fun loadDescriptionSamples() {
        viewModelScope.launch { descriptionSamples = repository.descriptionSuggestions() }
    }

    var partnersLoading by mutableStateOf(false)
        private set

    // §734 — booking IDs whose "session starts in <24h" reminder Toast has already
    // fired this app session. The ViewModel outlives screen navigation, so this
    // stops the reminder re-firing every single time the user returns to Home (the
    // visual banner still shows; only the noisy Toast is de-duplicated). add()
    // returns true only on first insert → exactly one Toast per booking per session.
    val shownSessionReminderIds: MutableSet<String> = mutableSetOf()

    /** Refresh discovery for the service the customer is about to browse.
     *  §687 — sends the device fix (if captured) so the list is distance-sorted. */
    fun loadPartnersForService(serviceId: String) {
        val loc = _deviceLocation.value
        partnersLoading = true
        viewModelScope.launch {
            runCatching { repository.loadPartnersForService(serviceId, loc?.first, loc?.second) }
            partnersLoading = false
        }
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

    // ── §713 Partner business location page (service-area geofence) ────────────
    /** The partner's saved business location (lat/lon/address + current/max radius).
     *  Null until [loadPartnerLocation] resolves; the page defaults to "not set". */
    private val _partnerLocation = MutableStateFlow<com.example.data.remote.PartnerLocationDto?>(null)
    val partnerLocation: StateFlow<com.example.data.remote.PartnerLocationDto?> = _partnerLocation.asStateFlow()

    /** True while a business-location read/write is in flight (drives spinners). */
    var partnerLocationBusy by mutableStateOf(false)
        private set

    /** Load the partner's saved business location for the Business Location page. */
    fun loadPartnerLocation() {
        viewModelScope.launch {
            partnerLocationBusy = true
            _partnerLocation.value = repository.getPartnerLocation()
            partnerLocationBusy = false
        }
    }

    /** Persist a business location chosen on the page (GPS or a search result),
     *  with the chosen service radius (clamped server-side to ≤ radius_max_km).
     *  Updates [partnerLocation] from the server's resolved (clamped) response so
     *  the UI never shows a radius the server rejected. [onDone] fires on success. */
    fun savePartnerBusinessLocation(
        lat: Double,
        lon: Double,
        address: String? = null,
        radiusKm: Double? = null,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            partnerLocationBusy = true
            runCatching { repository.setPartnerLocation(lat, lon, address, radiusKm) }
                .onSuccess { resolved ->
                    _partnerLocation.value = resolved
                    _deviceLocation.value = lat to lon
                    resolved.radiusKm?.let { if (it > 0) partnerServiceRadiusKm = it }
                    notify("Business location saved.")
                    onDone()
                }
                .onFailure { friendly(it) }
            partnerLocationBusy = false
        }
    }

    /** Update ONLY the service radius from the Business Location page's slider,
     *  reusing the saved lat/lon/address. No-op with a clear message if no base
     *  location is set yet (radius needs a centre to mean anything). */
    fun savePartnerLocationRadius(radiusKm: Double) {
        val loc = _partnerLocation.value
        if (loc?.lat == null || loc.lon == null) {
            notify("Set your business location first, then adjust the radius.", isError = true)
            return
        }
        savePartnerBusinessLocation(loc.lat, loc.lon, loc.address, radiusKm)
    }

    /** §690 — server-side service search (price-range + partner-filtered). Returns
     *  null on failure so the screen falls back to the local in-memory filter. */
    suspend fun searchServices(q: String): List<Service>? = repository.searchServices(q)

    /** §707 — search professionals by ID or name (results page "Experts" section). */
    suspend fun searchPartners(q: String): List<Partner> = repository.searchPartners(q)

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
                .onSuccess {
                    notify("Location set"); onDone()
                    // §731 — remember the chosen location so the picker can offer it again next time.
                    if (lat != null && lon != null) {
                        rememberRecentLocation(PickedLocation(
                            lat = lat, lon = lon,
                            address = line1, city = city, pincode = pincode,
                            title = label, subtitle = line2,
                        ))
                    }
                }
                .onFailure { notify("Could not set your location. Please try again.", isError = true) }
        }
    }

    // ── §731 recent / previous locations (persisted) — shown in the location picker ──
    private val _recentLocations = MutableStateFlow<List<PickedLocation>>(emptyList())
    val recentLocations: StateFlow<List<PickedLocation>> = _recentLocations.asStateFlow()

    private fun recentLocationPrefs() =
        getApplication<Application>().getSharedPreferences("nikhat_prefs", Context.MODE_PRIVATE)

    private fun loadRecentLocations() {
        val raw = recentLocationPrefs().getString("recent_locations", null) ?: return
        runCatching {
            val arr = org.json.JSONArray(raw)
            val list = ArrayList<PickedLocation>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(PickedLocation(
                    lat = o.getDouble("lat"), lon = o.getDouble("lon"),
                    address = o.optString("address"), city = o.optString("city"),
                    pincode = o.optString("pincode"), title = o.optString("title"),
                    subtitle = o.optString("subtitle"),
                ))
            }
            _recentLocations.value = list
        }
    }

    private fun persistRecentLocations(list: List<PickedLocation>) {
        val arr = org.json.JSONArray()
        list.forEach { p ->
            arr.put(org.json.JSONObject().apply {
                put("lat", p.lat); put("lon", p.lon); put("address", p.address)
                put("city", p.city); put("pincode", p.pincode)
                put("title", p.title); put("subtitle", p.subtitle)
            })
        }
        recentLocationPrefs().edit().putString("recent_locations", arr.toString()).apply()
    }

    /** Push a freshly-chosen location to the FRONT of the recent list (newest first,
     *  de-duplicated by address / near-identical coords, capped at 8) and persist it. */
    fun rememberRecentLocation(p: PickedLocation) {
        val key = p.address.trim().lowercase()
        val deduped = _recentLocations.value.filterNot {
            (key.isNotEmpty() && it.address.trim().lowercase() == key) ||
            (kotlin.math.abs(it.lat - p.lat) < 1e-5 && kotlin.math.abs(it.lon - p.lon) < 1e-5)
        }
        val updated = (listOf(p) + deduped).take(8)
        _recentLocations.value = updated
        persistRecentLocations(updated)
    }

    /** Delete ONE saved location from the recent list. */
    fun removeRecentLocation(p: PickedLocation) {
        val updated = _recentLocations.value.filterNot {
            it.lat == p.lat && it.lon == p.lon && it.address == p.address
        }
        _recentLocations.value = updated
        persistRecentLocations(updated)
    }

    /** Clear the ENTIRE recent-location history ("Clear all"). */
    fun clearRecentLocations() {
        _recentLocations.value = emptyList()
        recentLocationPrefs().edit().remove("recent_locations").apply()
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
            } else if (msgRole == "customer") {
                // §722 — the partner sees the customer's LIVE position as she streams it
                // (auto-updating), so she can reach the customer even if she's stepped out.
                _trackCustomer.value = GeoPoint(lat, lon)
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
        } else if (role == "customer") {
            // §722 — the customer streams her OWN live position continuously so the
            // PARTNER sees where she actually is (received via the listener's "customer"
            // branch above). Her own map keeps her saved booking/home address as the
            // anchor she watches the partner approach; only if the booking had NO saved
            // coords do we seed her anchor from the first live fix (graceful degradation).
            locationUpdates = com.example.data.LocationHelper.startUpdates(getApplication(), 5_000L) { lat, lon ->
                trackingSocket?.sendLocation(lat, lon)
                if (destLat == null || destLon == null) {
                    _trackCustomer.value = GeoPoint(lat, lon)
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
                _trackRoute.value = runCatching { VedaDropMaps.decodePolyline(poly) }.getOrNull()
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
                    if (_navStack.size > 50) _navStack.removeFirst()
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

    /** §738 — top-level bottom-bar navigation. Tabs are ROOTS, not pushed onto the
     *  back-stack: switching tabs resets history so Back from any tab returns to the
     *  role home (and Back on the home tab triggers the shell's double-back-to-exit).
     *  Previously every tab tap pushed the previous tab, so casual toggling
     *  (Explore→Cart→Bookings→Profile→…) inflated Back history unboundedly and the user
     *  had to press Back many times, walking back through tabs they'd already left. */
    fun navigateTab(screen: Screen) {
        if (!isLoggedIn && isGuestMode && isScreenRestricted(screen)) { triggerLoginPrompt(); return }
        val home: Screen = if (activeUser.value?.role == "partner") Screen.PartnerDashboard else Screen.CustomerHome
        _navStack.clear()
        if (screen != home) _navStack.addLast(home)
        _currentScreen = screen
    }
    // §732 — first-run onboarding: false until the user finishes the intro once
    // (persisted), so a brand-new user sees the welcome flow before login.
    var onboardingComplete by mutableStateOf(
        getApplication<Application>().getSharedPreferences("nikhat_prefs", Context.MODE_PRIVATE)
            .getBoolean("onboarding_seen", false)
    )
        private set

    fun completeOnboarding() {
        onboardingComplete = true
        getApplication<Application>().getSharedPreferences("nikhat_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("onboarding_seen", true).apply()
    }

    // ── §738 Light / Dark / System theme ────────────────────────────────────────
    // Persisted user choice. Defaults to DARK so existing installs keep the
    // established "dark luxury teal" brand look until the user opts into Light or
    // System from Profile → Appearance. MainActivity collects this and feeds it to
    // MyApplicationTheme, which flips the Material colorScheme + LocalVedaDropPalette.
    private val _themeMode = MutableStateFlow(
        runCatching {
            ThemeMode.valueOf(
                getApplication<Application>()
                    .getSharedPreferences("nikhat_prefs", Context.MODE_PRIVATE)
                    .getString("theme_mode", ThemeMode.DARK.name) ?: ThemeMode.DARK.name
            )
        }.getOrDefault(ThemeMode.DARK)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        getApplication<Application>()
            .getSharedPreferences("nikhat_prefs", Context.MODE_PRIVATE)
            .edit().putString("theme_mode", mode.name).apply()
    }

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
        // §731 — load the saved recent-location history for the picker.
        loadRecentLocations()
        // §731 — after a short startup grace (so the inbox the user already had isn't
        // re-announced on launch), surface NEW unread notifications (booking updates +
        // chat messages) as real system notifications while the app is alive.
        viewModelScope.launch {
            kotlinx.coroutines.delay(8000)
            repository.notificationsFlow.value.forEach { seenNotifIds.add(it.id) }
            notifBaselineDone = true
        }
        viewModelScope.launch {
            repository.notificationsFlow.collect { list ->
                if (!notifBaselineDone) return@collect
                val fresh = list.filter { it.id !in seenNotifIds }
                fresh.forEach { seenNotifIds.add(it.id) }
                fresh.filter { !it.read }.forEach { postLocalNotification(it) }
            }
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
        if (phone.trim().length != 10) { authError = "Phone number must be exactly 10 digits."; return }
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
                    // §738 — root the new session at its OWN home with a CLEAN back-stack.
                    // Without this, a guest who browsed customer screens and then logged in
                    // as a partner kept that customer history on the stack, so hardware-Back
                    // walked back into customer screens under the partner shell (role bleed).
                    clearNavHistory()
                    currentScreen = if (role == "partner") Screen.PartnerDashboard else Screen.CustomerHome
                    // §738 — honour a push/notification deep-link that arrived while logged
                    // out (the LoginScreen had overridden the content) instead of dropping it.
                    pendingDeepLink?.let { target ->
                        pendingDeepLink = null
                        currentScreen = target
                    }
                    loadNotifications()
                    registerFcmToken()   // §710 P0-5 — enable background push for this account
                }
                .onFailure { authError = friendly(it) }
            authBusy = false
        }
    }

    // §710 P0-5 — FCM token lifecycle. Best-effort + wrapped, so a not-yet-configured
    // Firebase (no google-services.json) can never crash auth. registerDevice/
    // unregisterDevice are authed no-ops when logged out.
    private var lastFcmToken: String? = null

    fun registerFcmToken() {
        runCatching {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (!token.isNullOrBlank()) {
                        lastFcmToken = token
                        viewModelScope.launch { runCatching { repository.registerDevice(token) } }
                    }
                }
        }
    }

    private suspend fun unregisterFcmToken() {
        val t = lastFcmToken ?: return
        runCatching { repository.unregisterDevice(t) }
        lastFcmToken = null
    }

    // §738 — a deep-link target captured while the user is logged OUT (push tapped on
    // the login screen). Consumed by verifyOtp on a successful sign-in.
    private var pendingDeepLink: Screen? = null

    /** §710 P0-5 — a tapped push (carrying notif_booking_id) deep-links to that booking. */
    fun openBookingFromPush(bookingId: String?) {
        if (bookingId.isNullOrBlank()) return
        val target = Screen.BookingDetail(bookingId)
        // §738 — if tapped while logged out, the LoginScreen overrides the content and the
        // target would be lost; stash it and open it right after login instead.
        if (!isLoggedIn) { pendingDeepLink = target; return }
        currentScreen = target
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
            unregisterFcmToken()   // §710 P0-5 — stop pushes to this device before clearing
            runCatching { repository.logout() }
            resetSessionState()
        }
    }

    /** §704 — Play-Store account deletion. Soft-deletes server-side, wipes all local
     *  state (same as logout), then drops back to the login/landing screen. */
    fun deleteAccount(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            unregisterFcmToken()   // §710 P0-5 — release this device's push registration
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
     *  the previous account's data. Repo caches are cleared in [VedaDropRepository]. */
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
        detailStartOtpFor = null
        partnerKycReason = null
        quoteBreakdown = null
        availableSlots = emptyList()
        selectedSlotId = null
        bookingNotes = ""
        bookingGenderPref = "any"
        complaintDetail = null
        complaintMessages = emptyList()
        _deviceLocation.value = null
        // §725 Batch-B — never leave the urgent alarm ringing after logout.
        _ringingUrgentOfferIds.clear()
        _urgentOffersOnScreen = false
        runCatching { UrgentAlarmService.stop(getApplication()) }
        clearNavHistory()
        stopLiveTracking()
        currentScreen = Screen.CustomerHome
    }

    /** A guest tapped a login-gated action — drop guest mode so the login screen
     *  shows. Role defaults to customer (guests browse the customer side). */
    fun triggerLoginPrompt() {
        // §738 — remember the login-gated screen the guest wanted so we can return them
        // there right after sign-in (consumed by verifyOtp) instead of stranding them on
        // the home with their intent lost.
        if (isScreenRestricted(currentScreen)) pendingDeepLink = currentScreen
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
            is Screen.CustomerProfile -> false
            is Screen.MyBookings -> false
            is Screen.Cart -> false
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
        val err = com.example.data.remote.ApiErrors.parse(t)
        // §714 cust-auth-403-no-logout — a deleted/suspended account returns 403
        // FORBIDDEN (or ACCOUNT_REVIEW) on every authed call; OkHttp's authenticator
        // only fires on 401, so without this the app stays "logged in" with a perpetual
        // error toast and no route back to login. Force a clean logout instead.
        if (err.code == "FORBIDDEN" || err.code == "ACCOUNT_REVIEW") {
            forceLogout(err.message)
            return err.message
        }
        notify(err.message, isError = true)
        return err.message
    }

    /** §714 — clear the session locally when the server says the account is gone. */
    private fun forceLogout(reason: String?) {
        viewModelScope.launch {
            runCatching { unregisterFcmToken() }
            resetSessionState()
            if (!reason.isNullOrBlank()) notify(reason, isError = true)
        }
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
        // §714 cpe-push-toggle-1 — actually (de)register the device so the toggle isn't a
        // lie: turning it OFF used to leave the device registered + still receiving pushes.
        if (enabled) registerFcmToken()
        else viewModelScope.launch { unregisterFcmToken() }
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
    /** Booking id that [detailStartOtp] belongs to. Keyed so a cached OTP can never
     *  leak onto another booking's screen when that booking's entity start_otp is
     *  momentarily blank (stale list row / pre-detail-refresh). booking.id is a String. */
    var detailStartOtpFor: String? by mutableStateOf(null)
    fun loadStartOtp(bookingId: String) {
        viewModelScope.launch {
            val id = bookingId.toIntOrNull() ?: return@launch
            detailStartOtp = repository.fetchStartOtp(id)
            detailStartOtpFor = bookingId
        }
    }

    /** §702 — partner KYC rejection reason (Mapper never populates kycReason). */
    var partnerKycReason by mutableStateOf<String?>(null)
    /** §708 — last-saved KYC fields so the form pre-fills on return. */
    var kycPrefill by mutableStateOf<com.example.data.remote.KycFields?>(null)
        private set
    fun loadPartnerKyc() {
        viewModelScope.launch {
            repository.fetchKyc()?.let { partnerKycReason = it.reason; kycPrefill = it.fields }
        }
    }

    /** §708 — re-fetch the signed-in profile (kyc_status, name-lock, …). Used on
     *  screen entry + pull-to-refresh so an admin KYC approval (or any server-side
     *  change) shows up without a re-login. Fire-and-forget, null-safe. */
    fun refreshProfile() {
        viewModelScope.launch {
            repository.refreshActiveProfile()
            // §710 cycle-2 #11 — seed the service-radius slider from the SERVER profile
            // (was a hardcoded 10.0 that went stale after restart, so the slider lied
            // about the saved coverage). Hours-preset clamp tracked separately (P1-8).
            repository.activeUserFlow.value?.let { u ->
                if (u.travelRadiusKm > 0) partnerServiceRadiusKm = u.travelRadiusKm
            }
        }
    }

    /** §708 — pull-to-refresh dispatcher: re-fetch whatever the given screen shows.
     *  Always refreshes the profile too (cheap, keeps kyc/name in sync). */
    fun refreshForScreen(screen: Screen) {
        refreshProfile()
        when (screen) {
            is Screen.CustomerHome -> { loadAppConfig(); refreshActiveBookings(); viewModelScope.launch { repository.refreshFavorites() } }
            is Screen.MyBookings -> refreshActiveBookings()
            is Screen.BookingDetail -> loadBookingDetail(screen.bookingId)
            is Screen.Notifications -> loadNotifications()
            is Screen.Favourites -> viewModelScope.launch { repository.refreshFavorites() }
            is Screen.Cart -> viewModelScope.launch { repository.refreshCart() }
            is Screen.ComplaintsList -> refreshComplaints()
            is Screen.PartnerDashboard -> { loadOffers(); loadNotifications(); refreshActiveBookings() }
            is Screen.PartnerOffers -> loadOffers()
            is Screen.PartnerServices -> viewModelScope.launch { repository.loadPartnerCatalog(); repository.refreshPartnerServices() }
            is Screen.PartnerEarnings -> loadEarnings()
            is Screen.PartnerAnalytics -> loadAnalytics()
            is Screen.PartnerPortfolio -> loadPortfolio()
            is Screen.PartnerKyc -> loadPartnerKyc()
            is Screen.PartnerBusinessLocation -> loadPartnerLocation()
            else -> {}
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

    // ── §737 Packages (bundles) + Deals/Featured — payment-free ─────────────
    // Customer: packages on the partner store + the home Deals row. Adding a package
    // expands into the EXISTING cart, so the booking path is unchanged.
    val partnerStorePackages = repository.partnerPackagesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val featuredPackages = repository.featuredFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )
    val myPackages = repository.myPackagesFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun loadPartnerPackages(partnerId: String) {
        viewModelScope.launch { runCatching { repository.loadPartnerPackages(partnerId) } }
    }

    fun loadFeatured() {
        viewModelScope.launch { runCatching { repository.loadFeatured() } }
    }

    /** Add a whole package to the cart (expands into the existing single-partner cart).
     *  Mirrors [addToCart]: on a single-partner conflict the friendly message already
     *  says to clear the cart; pass replace=true to start fresh. */
    fun addPackageToCart(packageId: Int, replace: Boolean = false, onResult: (String?) -> Unit = {}) {
        if (!isLoggedIn) { triggerLoginPrompt(); return }
        viewModelScope.launch {
            runCatching { repository.addPackageToCart(packageId, replace) }
                .onSuccess { notify("Package added to cart"); onResult(null) }
                .onFailure { onResult(friendly(it)) }
        }
    }

    // Partner package builder.
    var packageBuilderBusy by mutableStateOf(false)
        private set

    fun loadMyPackages() {
        viewModelScope.launch { runCatching { repository.loadMyPackages() } }
    }

    fun createMyPackage(
        name: String, description: String?, isFeatured: Boolean, featuredHeadline: String?,
        items: List<Pair<Int, Int>>, onResult: (String?) -> Unit = {},
    ) {
        if (name.isBlank()) { onResult("Please give your package a name."); return }
        if (items.isEmpty()) { onResult("Add at least one of your services to the package."); return }
        packageBuilderBusy = true
        viewModelScope.launch {
            runCatching {
                repository.createMyPackage(name.trim(), description?.trim(), null, isFeatured,
                    featuredHeadline?.trim(), items)
            }
                .onSuccess { notify("Package created"); onResult(null) }
                .onFailure { onResult(friendly(it)) }
            packageBuilderBusy = false
        }
    }

    fun setPackageFeatured(packageId: Int, featured: Boolean) {
        viewModelScope.launch {
            runCatching { repository.updateMyPackage(packageId, isFeatured = featured) }
                .onFailure { notify(friendly(it), isError = true) }
        }
    }

    fun deleteMyPackage(packageId: Int, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.deleteMyPackage(packageId) }
                .onSuccess { notify("Package removed"); onResult(null) }
                .onFailure { onResult(friendly(it)) }
        }
    }

    fun bookAgain(booking: com.example.data.BookingEntity) {
        if (!isLoggedIn) { triggerLoginPrompt(); return }
        // §714 cust-book-1 — a cancelled Flow-B (open) booking has no fixed partner
        // (partnerId is the "-1"/"0" sentinel). Adding that to the cart creates a poisoned
        // line that dead-ends checkout at a 404; route the user back to browse instead.
        val pid = booking.partnerId
        if (pid.isBlank() || pid == "-1" || pid == "0") {
            notify("That was an open booking with no fixed professional — please book again from the catalogue.",
                   isError = true)
            currentScreen = Screen.CustomerHome
            return
        }
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
                // cartQuote also writes the shared lastQuoteId, so hold the lock
                // across quote+book to stop a concurrent re-quote clobbering it.
                quoteMutex.withLock {
                    val total = repository.cartQuote(couponCode, chosenAddrId, selectedSlotId)
                    // §707 — enforce the ₹599 minimum on the cart total before booking.
                    val min = repository.minBookingPaise()
                    require(total >= min) {
                        "Minimum booking amount is ₹${min / 100}. Please add more services to reach ₹${min / 100}."
                    }
                    repository.createBookingFromLastQuote(
                        customerNotes = bookingNotes,
                        genderPreference = bookingGenderPref,
                        deviceInfo = deviceInfoJson(),
                        flexible = bookingFlexible && flexibleSlotsEnabled(),   // §729
                        expertId = selectedExpertId,                            // §743
                    )
                }
            }.onSuccess { booking ->
                bookingNotes = ""; bookingGenderPref = "any"; bookingFlexible = false; selectedExpertId = null
                notify("Booking request sent")
                currentScreen = Screen.BookingDetail(booking.id)
                onResult(null)
            }.onFailure { onResult(friendly(it)) }
        }
    }

    /** §722 — MULTI-PARTNER cart checkout: when the cart spans 2+ partners (multi_partner
     *  ON), place one booking per partner-group together via /combo. Notifies the
     *  customer that 2+ professionals are booked and opens the first booking. */
    fun checkoutCartMulti(addressId: Long? = null, onResult: (String?) -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                val chosenAddrId = addressId
                    ?: (addresses.value.firstOrNull { it.isDefault } ?: addresses.value.firstOrNull())?.id
                val partnerIds = (cart.value?.groups ?: emptyList())
                    .mapNotNull { it.partnerId }.distinct()
                require(partnerIds.size >= 2) { "Add services from at least two professionals for a combined booking." }
                quoteMutex.withLock {
                    repository.checkoutCombo(
                        partnerIds = partnerIds,
                        addressId = chosenAddrId,
                        slotId = selectedSlotId,
                        couponCode = couponCode,
                        customerNotes = bookingNotes,
                        shareNumber = false,
                    )
                }
            }.onSuccess { combo ->
                bookingNotes = ""
                notify("${combo.count} professionals booked for your appointment")
                val first = combo.bookings.firstOrNull()
                currentScreen = if (first != null) Screen.BookingDetail(first.id.toString())
                                else Screen.CustomerHome
                onResult(null)
            }.onFailure { onResult(friendly(it)) }
        }
    }

    // ── Partner Availability Engine ──────────────────────────────────────────
    @get:JvmName("getPartnerActiveVal")
    @set:JvmName("setPartnerActiveVal")
    var isPartnerActive by mutableStateOf(true)
    var partnerServiceRadiusKm by mutableStateOf(10.0)
    var partnerWorkingHoursRange by mutableStateOf("9:00 AM - 8:00 PM")

    /** §710 P0-9 — online/away toggle → POST /partner/availability/online {online}.
     *  Optimistic flip is reverted if the call fails (P0-11). */
    fun setPartnerActive(active: Boolean) {
        val prev = isPartnerActive
        isPartnerActive = active
        runPartnerAction(if (active) "You are now ONLINE" else "You are now AWAY",
            onError = { isPartnerActive = prev }) {
            repository.setPartnerOnline(active)
        }
    }

    /** Service-radius → PATCH /partner/profile {travel_radius_km}. */
    fun savePartnerRadius(km: Double) {
        val prev = partnerServiceRadiusKm
        partnerServiceRadiusKm = km
        runPartnerAction("Service radius updated",
            onError = { partnerServiceRadiusKm = prev }) { repository.setPartnerTravelRadius(km) }
    }

    /** Working-hours picker label → availability endpoint (24h HH:mm). */
    fun savePartnerWorkingHours(label: String) {
        val prev = partnerWorkingHoursRange
        partnerWorkingHoursRange = label
        val (start, end) = parseShiftLabel(label) ?: run {
            notify("Could not read those hours", isError = true); return
        }
        runPartnerAction("Working hours updated",
            onError = { partnerWorkingHoursRange = prev }) { repository.setPartnerWorkingHours(start, end) }
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
        partnerType: String? = null,   // §743 — individual | parlour
        gapMin: Int? = null,           // §744 — rest/travel gap (minutes)
    ) {
        viewModelScope.launch {
            runCatching {
                repository.updateProfile(name, email, bio, experience, gender, minimumOrderPaise, travelRadiusKm, partnerType, gapMin)
            }.onSuccess {
                notify("Profile updated successfully ✨")
            }.onFailure {
                friendly(it)
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

    init {
        // §714 cpe-beauty-1 — re-hydrate the beauty profile from the SERVER (so it
        // survives reinstall / a new device) instead of only seeding from local prefs.
        viewModelScope.launch {
            repository.serverBeautyFlow.collect { b ->
                if (b != null) {
                    b.first?.takeIf { it.isNotBlank() }?.let { customerSkinType = it }
                    b.second?.let { customerBeautyConcerns = it }
                    b.third?.takeIf { it.isNotBlank() }?.let { customerPreferredTime = it }
                }
            }
        }
    }

    fun updateBeautyProfile(skinType: String, concerns: String, prefTime: String) {
        customerSkinType = skinType
        customerBeautyConcerns = concerns
        customerPreferredTime = prefTime
        // Local cache (offline + instant), used to seed the fields next launch.
        getApplication<Application>().getSharedPreferences("nikhat_prefs", Context.MODE_PRIVATE).edit()
            .putString("skin_type", skinType)
            .putString("beauty_concerns", concerns)
            .putString("pref_time", prefTime)
            .apply()
        // §710 cycle-2 #1/#4 — ALSO persist to the server so it reaches the partner who
        // performs the service + admin, and survives reinstall (was device-local only).
        viewModelScope.launch {
            runCatching { repository.updateBeautyProfile(skinType, concerns, prefTime) }
                .onFailure { friendly(it) }
        }
    }

    // ── §694 booking-time data capture (customer-set on the booking form) ──────
    var bookingNotes by mutableStateOf("")
    var bookingGenderPref by mutableStateOf("female")   // §722 women-only: always female

    // ── §729 (parity C2) flexible arrival window (customer opt-in on the booking form) ──
    /** True when the customer toggled "Flexible arrival" on the booking screen. Sent as
     *  `flexible=true` (with the slot_start = chosen WINDOW START) when she confirms.
     *  Reset to false after every successful booking so the next booking starts exact. */
    var bookingFlexible by mutableStateOf(false)

    /** §729 — is the flexible-arrival feature enabled by the backend right now? Drives
     *  whether the toggle is shown at all. Defaults false until /config loads. */
    fun flexibleSlotsEnabled(): Boolean = flag("flexible_slots", default = false)

    /** §729 — the flexible arrival WINDOW length in minutes (admin param `flex_window_min`,
     *  default 180). Moshi decodes JSON numbers to Double, so coerce via Number. The window
     *  options (e.g. 7-10 / 10-1 / 1-4 / 4-6) are derived from this in the UI. */
    fun flexWindowMinutes(): Int =
        (appConfig.value?.params?.get("flex_window_min") as? Number)?.toInt()?.takeIf { it > 0 } ?: 180

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
            // §714 cust-addr-2 — make a freshly added address the active default. A 2nd+
            // address added mid-booking used to save as non-default with no way to promote
            // it, so the customer was stuck delivering to their first-ever address.
            // §714 cust-addr-4 — surface a save failure instead of a silent false-success.
            runCatching { repository.addAndSelectAddress(label, line1, line2, city, pincode, lat, lon) }
                .onSuccess { notify("Address saved") }
                .onFailure { notify("Could not save address. Please try again.", isError = true) }
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
                quoteMutex.withLock {
                    repository.createQuote(
                        partnerId = partner.id,
                        serviceId = service.id,
                        slotId = selectedSlotId,
                        addressId = defaultAddr?.id,
                        couponCode = couponCode,
                        useWallet = false,
                    )
                }
            }.onSuccess { quoteBreakdown = it }
        }
    }

    fun confirmAndBook(service: Service, partner: Partner, address: AddressEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                // Hold the lock across BOTH the quote and the booking so a
                // concurrent updateBookingQuote() can't clobber the shared
                // lastQuoteId in between and make us book the wrong quote.
                quoteMutex.withLock {
                    // Re-quote with the slot the user actually picked. The initial
                    // quote was built at PartnerSelect BEFORE any slot existed (so its
                    // slot_id was null); without this the booking would be stored
                    // time-less and "Change partner" would never unlock.
                    val quote = repository.createQuote(
                        partnerId = partner.id,
                        serviceId = service.id,
                        slotId = selectedSlotId,
                        addressId = address.id,
                        couponCode = couponCode,
                        useWallet = false,
                    ).also { quoteBreakdown = it }
                    // §707 — founder rule: no booking below ₹599. Pre-check the
                    // authoritative quote total so the customer is told here instead of
                    // after submitting (the server enforces the same floor as backstop).
                    val min = repository.minBookingPaise()
                    require(quote.totalPaise >= min) {
                        "Minimum booking amount is ₹${min / 100}. Please add more services to reach ₹${min / 100}."
                    }
                    repository.createBookingFromLastQuote(
                        customerNotes = bookingNotes,
                        genderPreference = bookingGenderPref,
                        deviceInfo = deviceInfoJson(),
                        // §729 (parity C2) — only flexible when the feature is enabled AND
                        // the customer opted in (defaults false ⇒ exact-slot unchanged).
                        flexible = bookingFlexible && flexibleSlotsEnabled(),
                        // §744 — pass the chosen expert (null = let the salon assign).
                        // (The cart path already passed it; this BookingConfirm path dropped it.)
                        expertId = selectedExpertId,
                    )
                }
            }.onSuccess { booking ->
                bookingNotes = ""; bookingGenderPref = "any"; bookingFlexible = false; selectedExpertId = null
                notify("Booking request sent")
                currentScreen = Screen.BookingDetail(booking.id)
            }.onFailure { friendly(it) }
            // §738 — signal THIS booking's completion (success or failure) so the caller's
            // double-submit guard resets on the real result, not on any unrelated toast.
            onComplete()
        }
    }

    fun bookDirectlyFromForm(
        service: Service,
        slot: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit = {},
    ) {
        selectedSlot = slot
        viewModelScope.launch {
            val partner = VedaDropDataSource.partners.firstOrNull { it.servicesOffered.contains(service.id) }
                ?: VedaDropDataSource.partners.firstOrNull()
            if (partner == null) {
                onError("No professional is available right now.")
                return@launch
            }
            val addr = addresses.value.firstOrNull { it.isDefault } ?: addresses.value.firstOrNull()
            runCatching {
                // Atomic quote+book so a concurrent re-quote can't clobber the
                // shared lastQuoteId between these two calls.
                quoteMutex.withLock {
                    val quote = repository.createQuote(partner.id, service.id, null, addr?.id, null, false)
                    // §707 — enforce the ₹599 minimum before creating the booking.
                    val min = repository.minBookingPaise()
                    require(quote.totalPaise >= min) {
                        "Minimum booking amount is ₹${min / 100}. Please add more services to reach ₹${min / 100}."
                    }
                    repository.createBookingFromLastQuote(
                        customerNotes = bookingNotes,
                        genderPreference = bookingGenderPref,
                        deviceInfo = deviceInfoJson(),
                    )
                }
            }.onSuccess { booking ->
                bookingNotes = ""; bookingGenderPref = "any"
                notify("Booking request sent")
                currentScreen = Screen.BookingDetail(booking.id)
                onSuccess(booking.id)
            }.onFailure { onError(friendly(it)) }
        }
    }

    fun sendChatMessage(bookingId: String, senderRole: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.sendThread(bookingId, text) }
                // §710 #7 — surface a blocked/locked/cap-limited send (was silent: the
                // message just vanished). friendly() toasts the reason.
                .onFailure { friendly(it) }
        }
    }

    // ── Partner booking actions (error-surfacing wrappers) ─────────────────────
    fun acceptJob(id: String) = runPartnerAction("Appointment accepted") { repository.acceptBooking(id) }
    fun rejectJob(id: String) = runPartnerAction("Appointment declined") { repository.rejectBooking(id) }
    fun startTravelToJob(id: String) = runPartnerAction("On the way") { repository.startTravel(id) }
    fun arriveAtJob(id: String) = runPartnerAction("Marked arrived") { repository.arriveLocation(id) }
    fun completePartnerJob(id: String) = runPartnerAction("Job completed") { repository.completeJob(id) }

    /** Partner types the customer's start-OTP at the door; send it to the backend.
     *  §728 (parity C1) — [selfieDataUrl] is the partner's live start-selfie proof
     *  (base64 data: URL) captured at the door; sent alongside the OTP. Null when the
     *  capture device has no camera / older callers (backend leaves prior value). */
    fun startJob(id: String, otp: String, selfieDataUrl: String? = null) {
        if (otp.isBlank()) { notify("Enter the customer's OTP to start", isError = true); return }
        runPartnerAction("Job started") { repository.startJob(id, otp.trim(), selfieDataUrl) }
    }

    /** §710 P0-11 — runs a partner action and ALWAYS surfaces a failure. Previously
     *  `onFailure { friendly(it) }` discarded the message, so a failed accept (402
     *  subscription / 403 KYC / 409 conflict) looked like the button did nothing.
     *  `onError` lets callers revert an optimistic UI flip on failure. */
    private fun runPartnerAction(successMsg: String, onError: (() -> Unit)? = null,
                                 block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { notify(successMsg) }
                .onFailure {
                    friendly(it)
                    onError?.invoke()
                }
        }
    }

    fun submitKyc(
        aadhaar: String,
        pan: String,
        legalName: String? = null,
        // §725 — three guided face photos (front/left/right) from FaceCaptureFlow.
        faceFrontUrl: String? = null,
        faceLeftUrl: String? = null,
        faceRightUrl: String? = null,
        documentDataUrl: String? = null,
        // §713 — business location collected on the KYC screen (required by the
        // backend when geofencing is on, else 400 LOCATION_REQUIRED).
        baseLat: Double? = null,
        baseLon: Double? = null,
        baseAddress: String? = null,
        travelRadiusKm: Double? = null,
        onResult: (Boolean) -> Unit = {},
    ) {
        viewModelScope.launch {
            runCatching {
                // §704 — legalName is the name on her ID; admin locks display name to it.
                // §725 — face photos + document are base64 JPEG data URLs captured on-device.
                repository.submitKyc(
                    aadhaar, pan, legalName,
                    faceFrontUrl = faceFrontUrl, faceLeftUrl = faceLeftUrl, faceRightUrl = faceRightUrl,
                    documentDataUrl = documentDataUrl,
                    baseLat = baseLat, baseLon = baseLon,
                    baseAddress = baseAddress, travelRadiusKm = travelRadiusKm,
                )
            }.onSuccess {
                // §725 — the backend keeps the status "submitted"; verification 24–48h.
                notify("KYC submitted — verification mein 24–48 ghante lag sakte hain.")
                onResult(true)
            }.onFailure {
                // §725 — surfaces backend 409 KYC_ALREADY_PENDING / KYC_ALREADY_VERIFIED.
                friendly(it)
                onResult(false)
            }
        }
    }

    /** §726 — pull the FULL service dictionary so the partner can add ANY active
     *  service (not just the offered-only customer catalog). Call on opening the
     *  Catalog Pricing Editor. */
    fun loadPartnerCatalog() {
        viewModelScope.launch {
            runCatching { repository.loadPartnerCatalog() }
            runCatching { repository.refreshPartnerServices() }
        }
    }

    fun setPartnerServicePrice(serviceId: String, name: String, category: String, pricePaise: Long, active: Boolean, productsUsed: String,
                               images: List<String>? = null,
                               // §743 — per-offering richness (all optional; only sent when provided).
                               discountPercent: Int? = null,
                               durationMin: Int? = null,
                               products: List<com.example.data.remote.ProductDto>? = null,
                               hygieneNote: String? = null) {
        viewModelScope.launch {
            // §726 — surface failures. Previously only .onSuccess was wired, so a
            // bad service id / network error silently dropped the save and the
            // partner saw nothing happen ("added a service but it didn't save").
            // §742 — pass the partner's images; sending them re-enters admin approval,
            // so the success copy tells the partner it's pending review.
            runCatching { repository.setServicePrice(serviceId, pricePaise, active, productsUsed, images,
                discountPercent, durationMin, products, hygieneNote) }
                .onSuccess {
                    notify(if (images != null)
                        "Service saved — sent to admin for approval."
                    else "Service updated")
                }
                .onFailure { notify("Could not save service: ${com.example.data.remote.ApiErrors.friendlyMessage(it)}", isError = true) }
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
            faqs = listOf("Is this verified?" to "Yes, 100% verified by Veda Drop quality team."),
            // §726 — use a default image for custom services
            imageUrl = "https://images.unsplash.com/photo-1560066984-138dadb4c035?w=500&q=80",
            priceMinPaise = pricePaise,
            priceMaxPaise = pricePaise,
            partnerCount = 1
        )
        
        // Update global datasource services list
        VedaDropDataSource.services = VedaDropDataSource.services + newSrv
        
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
            // §738 — be HONEST: a free-form custom service is saved only on this device.
            // The backend only accepts catalog (dictionary) services, so customers can
            // NEVER see or book a custom one. The old "successfully added to your menu!"
            // toast falsely implied it was live. Guide the partner to the catalog flow,
            // which DOES persist + reach customers.
            notify("'$name' saved to your drafts on this device. Customers can only book listed services — use Manage Catalog to add and price your services so they go live.")
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
                .onFailure { friendly(it) }
        }
    }

    /** §704 — block/report a partner: permanently cuts off all their contact. */
    fun blockPartner(partnerId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.blockPartner(partnerId) }
                .onSuccess { notify("This professional has been blocked and reported."); onDone() }
                .onFailure { friendly(it) }
        }
    }

    /** §704 — the admin-editable emergency numbers + women helpline (from /config). */
    fun emergencyNumbers(): List<String> = repository.emergencyNumbers()
    fun womenHelpline(): String = repository.womenHelpline()

    /** §704 — after a booking ends, request to talk again (the other party accepts). */
    fun requestToTalk(bookingId: String, reason: String?, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.raiseTalkRequest(bookingId, reason) }
                .onSuccess { notify("Request sent — we'll let you know when they respond."); onDone() }
                .onFailure { friendly(it) }
        }
    }

    /** §704 — accept/decline a talk request on a past booking. */
    fun respondToTalk(bookingId: String, reqId: Int, accept: Boolean, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.respondTalkRequest(bookingId, reqId, accept) }
                .onSuccess { notify(if (accept) "You can chat now." else "Request declined."); onDone() }
                .onFailure { friendly(it) }
        }
    }

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
                .onSuccess { notify("Thanks for your review"); refreshActiveBookings() }
                // §710 #9 — surface failures (was silent: dialog closed, booking stayed
                // 'unreviewed'). friendly() already toasts (isError), so don't double-notify.
                .onFailure { friendly(it) }
        }
    }

    // §723 — the partner rates the customer after a completed visit (dual-rating loop).
    fun rateCustomerForBooking(bookingId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            runCatching { repository.rateCustomer(bookingId, rating, comment) }
                .onSuccess { notify("Thanks for rating your customer"); loadBookingDetail(bookingId) }
                .onFailure { friendly(it) }
        }
    }

    fun submitComplaint(bookingId: String?, subject: String, message: String) {
        viewModelScope.launch {
            runCatching { repository.createComplaint(bookingId, subject, message) }
                .onSuccess { notify("Complaint submitted") }
                .onFailure { friendly(it) }   // §710 #9 — was silent
        }
    }

    /** Pull the latest complaints/tickets for the support-desk list. */
    fun refreshComplaints() {
        viewModelScope.launch { runCatching { repository.refreshComplaints() } }
    }

    // ── Live booking detail refresh ─────────────────────────────────────────────
    /** §709 — true while the FIRST load of a booking-detail screen is in flight, so
     *  a deep-link (e.g. notification tap) shows a loader instead of flashing
     *  "Booking Not Found" before the fetch lands. */
    var bookingDetailLoading by mutableStateOf(false); private set

    /** Pull fresh status for the open booking-detail screen. */
    fun loadBookingDetail(id: String) {
        val haveIt = repository.bookingsFlow.value.any { it.id == id }
        if (!haveIt) bookingDetailLoading = true
        viewModelScope.launch {
            runCatching { repository.refreshBooking(id) }
            bookingDetailLoading = false
        }
    }

    // ── Partner: delete a listed service ────────────────────────────────────────
    fun deletePartnerService(id: String) {
        viewModelScope.launch {
            runCatching { repository.deletePartnerService(id) }
                .onSuccess { notify("Service removed") }
                .onFailure { friendly(it) }
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
                .onFailure { friendly(it) }
        }
    }

    fun sendComplaintReply(id: String, text: String) {
        if (text.isBlank()) return
        complaintReplyBusy = true
        viewModelScope.launch {
            runCatching { repository.replyComplaint(id, text) }
                .onSuccess { openComplaint(id) }
                .onFailure { friendly(it) }
            complaintReplyBusy = false
        }
    }
}
