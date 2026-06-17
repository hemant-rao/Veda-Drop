package com.example.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the GlamGo mobile REST contract. All paths are
 * relative to BASE_URL + "/api/glamgo/v1/". The Bearer token is injected by
 * [AuthInterceptor]; 401s trigger a refresh via [TokenAuthenticator].
 */
interface GlamGoApi {

    // ── Auth ─────────────────────────────────────────────────────────────────
    @POST("auth/otp/request")
    suspend fun otpRequest(@Body body: Map<String, String>): OtpRequestResp

    @POST("auth/otp/verify")
    suspend fun otpVerify(@Body body: Map<String, String>): OtpVerifyResp

    @POST("auth/refresh")
    suspend fun refresh(@Body body: Map<String, String>): RefreshResp

    @POST("auth/logout")
    suspend fun logout(@Body body: Map<String, String>): OkResp

    @GET("auth/me")
    suspend fun me(): MeResp

    @PATCH("auth/me")
    suspend fun updateMe(@Body body: Map<String, String?>): MeResp

    // ── Catalog (no auth required, token sent if present) ─────────────────────
    @GET("catalog/categories")
    suspend fun categories(): CategoriesResp

    @GET("catalog/categories/{id}/services")
    suspend fun categoryServices(@Path("id") id: Int): ServicesResp

    @GET("catalog/services/{id}")
    suspend fun service(@Path("id") id: Int): ServiceDto

    @GET("catalog/search")
    suspend fun search(@Query("q") q: String? = null, @Query("category") category: Int? = null): ServicesResp

    // ── Partner discovery ─────────────────────────────────────────────────────
    @GET("customer/partners")
    suspend fun partners(
        @Query("service_id") serviceId: Int? = null,
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
        @Query("sort") sort: String? = null,
        @Query("max_km") maxKm: Double? = null,
    ): PartnersResp

    @GET("customer/partners/{id}")
    suspend fun partner(@Path("id") id: Int): PartnerDto

    @GET("customer/partners/{id}/availability")
    suspend fun availability(
        @Path("id") id: Int,
        @Query("service_id") serviceId: Int? = null,
        @Query("date") date: String? = null,
    ): SlotsResp

    @GET("customer/partners/{id}/reviews")
    suspend fun partnerReviews(@Path("id") id: Int): ReviewsResp

    // ── Addresses ──────────────────────────────────────────────────────────────
    @GET("customer/addresses")
    suspend fun addresses(): AddressesResp

    @POST("customer/addresses")
    suspend fun addAddress(@Body body: AddressCreateReq): AddressDto

    @DELETE("customer/addresses/{id}")
    suspend fun deleteAddress(@Path("id") id: Int): OkResp

    @PATCH("customer/addresses/{id}")
    suspend fun updateAddress(@Path("id") id: Int, @Body body: Map<String, Any?>): AddressDto

    // ── Quote + Bookings ────────────────────────────────────────────────────────
    @POST("customer/quote")
    suspend fun quote(@Body body: QuoteReq): QuoteResp

    @POST("customer/bookings")
    suspend fun createBooking(@Body body: BookingCreateReq): BookingDto

    @GET("customer/bookings")
    suspend fun bookings(@Query("status") status: String? = null): BookingsResp

    @GET("customer/bookings/{id}")
    suspend fun booking(@Path("id") id: Int): BookingDto

    @POST("customer/bookings/{id}/cancel")
    suspend fun cancelBooking(@Path("id") id: Int, @Body body: CancelReq): BookingDto

    @POST("customer/bookings/{id}/start-otp")
    suspend fun startOtp(@Path("id") id: Int): StartOtpResp

    // ── Wallet ────────────────────────────────────────────────────────────────
    @GET("customer/wallet")
    suspend fun wallet(): WalletResp

    @GET("customer/wallet/transactions")
    suspend fun walletTxns(): WalletTxnsResp

    @POST("customer/wallet/add-money")
    suspend fun addMoney(@Body body: AddMoneyReq): WalletResp

    // ── Reviews / Complaints / Wishlist / Coupons ───────────────────────────────
    @POST("customer/bookings/{id}/review")
    suspend fun review(@Path("id") id: Int, @Body body: ReviewReq): ReviewDto

    @GET("customer/complaints")
    suspend fun complaints(): ComplaintsResp

    @POST("customer/complaints")
    suspend fun createComplaint(@Body body: ComplaintReq): ComplaintDto

    @GET("customer/complaints/{id}")
    suspend fun complaint(@Path("id") id: Int): ComplaintDto

    @POST("customer/complaints/{id}/messages")
    suspend fun addComplaintMessage(@Path("id") id: Int, @Body body: MessageReq): ComplaintMessageDto

    @GET("customer/wishlist")
    suspend fun wishlist(): WishlistResp

    @POST("customer/wishlist")
    suspend fun addWishlist(@Body body: WishlistReq): OkResp

    @HTTP(method = "DELETE", path = "customer/wishlist", hasBody = true)
    suspend fun removeWishlist(@Body body: WishlistReq): OkResp

    // ── AI assist ───────────────────────────────────────────────────────────────
    @POST("customer/ai/chat")
    suspend fun aiChat(@Body body: AiChatReq): AiChatResp

    @GET("customer/ai/recommendations")
    suspend fun aiRecommendations(): ServicesWrap

    // ── Chat (REST history) ──────────────────────────────────────────────────────
    @GET("customer/bookings/{id}/messages")
    suspend fun bookingMessages(@Path("id") id: Int): ChatMessagesResp

    @POST("customer/bookings/{id}/messages")
    suspend fun sendBookingMessage(@Path("id") id: Int, @Body body: ChatSendReq): ChatMessageDto

    @GET("customer/chat/pre-booking")
    suspend fun preBookingMessages(
        @Query("partner_id") partnerId: Int,
        @Query("service_id") serviceId: Int? = null,
    ): ChatMessagesResp

    @POST("customer/chat/pre-booking")
    suspend fun sendPreBookingMessage(
        @Query("partner_id") partnerId: Int,
        @Query("service_id") serviceId: Int? = null,
        @Body body: ChatSendReq,
    ): ChatMessageDto

    // ── Partner-side ──────────────────────────────────────────────────────────────
    @GET("partner/kyc")
    suspend fun getKyc(): KycStatusResp

    @POST("partner/kyc")
    suspend fun submitKyc(@Body body: KycReq): KycStatusResp

    @GET("partner/profile")
    suspend fun partnerProfile(): PartnerDto

    @PATCH("partner/profile")
    suspend fun updatePartnerProfile(@Body body: Map<String, Any?>): PartnerDto

    @GET("partner/services")
    suspend fun partnerServices(): PartnerServicesResp

    @POST("partner/services")
    suspend fun addPartnerService(@Body body: PartnerServiceReq): PartnerServiceDto

    @PATCH("partner/services/{id}")
    suspend fun patchPartnerService(@Path("id") id: Int, @Body body: Map<String, Any?>): PartnerServiceDto

    @GET("partner/bookings")
    suspend fun partnerBookings(@Query("status") status: String? = null): BookingsResp

    @POST("partner/bookings/{id}/accept")
    suspend fun acceptBooking(@Path("id") id: Int): BookingDto

    @POST("partner/bookings/{id}/reject")
    suspend fun rejectBooking(@Path("id") id: Int, @Body body: CancelReq): BookingDto

    @POST("partner/bookings/{id}/status")
    suspend fun partnerBookingStatus(@Path("id") id: Int, @Body body: StatusReq): BookingDto

    @GET("partner/earnings")
    suspend fun earnings(): EarningsResp

    @GET("partner/earnings/ledger")
    suspend fun earningsLedger(): WalletTxnsResp
}

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class ServicesWrap(val services: List<ServiceDto> = emptyList(), val reason: String? = null)
