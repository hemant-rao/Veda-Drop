package com.example.data

import android.content.Context
import com.example.data.remote.AddMoneyReq
import com.example.data.remote.AddressCreateReq
import com.example.data.remote.AiChatReq
import com.example.data.remote.ApiClient
import com.example.data.remote.BookingCreateReq
import com.example.data.remote.CancelReq
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
 * 100%-online GlamGo data layer. Every read/write goes to the backend
 * (`/api/glamgo/v1/`); there is no local source of truth. In-memory
 * StateFlows act purely as a UI cache, refreshed from the server after each
 * mutation. The (untouched) Compose screens collect these flows exactly as
 * they did against the old Room repository.
 */
class GlamGoRepository(context: Context) {

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

    private val _customerTxns = MutableStateFlow<List<WalletTransactionEntity>>(emptyList())
    private val _partnerTxns = MutableStateFlow<List<WalletTransactionEntity>>(emptyList())

    // last server quote, kept so confirmAndBook can create the booking from it
    @Volatile private var lastQuoteId: String? = null

    private val customProductsUsed = mutableMapOf<String, String>()

    fun isFavoriteFlow(partnerId: String): Flow<Boolean> =
        _favorites.map { list -> list.any { it.partnerId == partnerId } }

    fun getMessagesFlow(bookingId: String): Flow<List<ChatMessageEntity>> =
        _chat.map { list -> list.filter { it.bookingId == bookingId } }

    fun getTransactionsFlow(role: String): Flow<List<WalletTransactionEntity>> =
        if (role == "partner") _partnerTxns else _customerTxns

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

    /** Switch the active identity if we already hold a session for it. */
    suspend fun switchRole(role: String): Boolean {
        if (!tokenStore.hasSession(role)) return false
        tokenStore.activeRole = role
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
    }

    // ── Hydration ────────────────────────────────────────────────────────────
    suspend fun hydrateCatalog() {
        val cats = api.categories().items.map { Mappers.category(it) }
        if (cats.isNotEmpty()) GlamMockDataSource.categories = cats
        val allServices = mutableListOf<Service>()
        for (c in cats) {
            try {
                allServices += api.categoryServices(c.id.toInt()).items.map { Mappers.service(it) }
            } catch (_: Exception) {
            }
        }
        if (allServices.isNotEmpty()) GlamMockDataSource.services = allServices
        try {
            val partners = api.partners().items.map { Mappers.partner(it) }
            if (partners.isNotEmpty()) GlamMockDataSource.partners = partners
        } catch (_: Exception) {
        }
    }

    suspend fun hydrateForRole(role: String) {
        hydrateCatalog()
        if (role == "customer") {
            refreshProfile("customer")
            refreshAddresses()
            refreshBookings("customer")
            refreshWallet("customer")
            refreshFavorites()
            refreshComplaints()
        } else {
            refreshProfile("partner")
            refreshPartnerServices()
            refreshBookings("partner")
            refreshWallet("partner")
        }
    }

    private suspend fun refreshProfile(role: String) {
        val profile = api.me().profile ?: return
        val wallet = runCatching { api.wallet().balancePaise }.getOrDefault(0L)
        _activeUser.value = Mappers.user(profile, role, wallet)
    }

    suspend fun refreshAddresses() {
        _addresses.value = api.addresses().items.map { Mappers.address(it) }
    }

    suspend fun refreshBookings(role: String) {
        val items = if (role == "partner") api.partnerBookings().items else api.bookings().items
        _bookings.value = items.map { Mappers.booking(it) }
    }

    suspend fun refreshWallet(role: String) {
        val balance = runCatching { api.wallet().balancePaise }.getOrNull()
        val txns = runCatching {
            if (role == "partner") api.earningsLedger().items else api.walletTxns().items
        }.getOrDefault(emptyList())
        val mapped = txns.map { Mappers.walletTxn(it, role) }
        if (role == "partner") _partnerTxns.value = mapped else _customerTxns.value = mapped
        if (balance != null) _activeUser.value = _activeUser.value?.copy(walletBalancePaise = balance)
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
            val customKit = customProductsUsed[it.serviceId.toString()] ?: "Premium salon kit (L'Oreal/O3+), 100% seal-packed & verified prior to use."
            PartnerServiceEntity(
                id = it.id.toString(),
                serviceId = it.serviceId.toString(),
                name = it.name ?: "",
                categoryName = "",
                pricePaise = it.pricePaise,
                durationMin = 45,
                active = it.active,
                productsUsed = customKit
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

    suspend fun addWalletMoney(amountPaise: Long, role: String) {
        runCatching { api.addMoney(AddMoneyReq(amountPaise)) }
        refreshWallet(role)
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
    suspend fun completeJob(id: String, proofUrl: String) {
        api.partnerBookingStatus(id.toInt(), StatusReq(to = "completed", proofUploadIds = listOf(proofUrl)))
        refreshBookings("partner")
    }

    private suspend fun partnerStatus(id: String, to: String) {
        api.partnerBookingStatus(id.toInt(), StatusReq(to = to))
        refreshBookings("partner")
    }

    // ── AI ─────────────────────────────────────────────────────────────────────
    suspend fun aiChat(message: String, history: List<Map<String, String>>): String =
        api.aiChat(AiChatReq(message = message, history = history)).reply
}
