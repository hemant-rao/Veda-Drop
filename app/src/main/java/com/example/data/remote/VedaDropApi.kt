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
 * Retrofit interface for the VedaDrop mobile REST contract. All paths are
 * relative to BASE_URL + "/api/vedadrop/v1/". The Bearer token is injected by
 * [AuthInterceptor]; 401s trigger a refresh via [TokenAuthenticator].
 */
interface VedaDropApi {

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

    // §704 — Play Store account deletion (soft-deletes / deactivates the account).
    @DELETE("auth/me")
    suspend fun deleteAccount(): OkResp

    // ── Catalog (no auth required, token sent if present) ─────────────────────
    @GET("catalog/categories")
    suspend fun categories(): CategoriesResp

    @GET("catalog/categories/{id}/services")
    suspend fun categoryServices(@Path("id") id: Int): ServicesResp

    @GET("catalog/services/{id}")
    suspend fun service(@Path("id") id: Int): ServiceDto

    // §729 (parity C2) — "Frequently booked together": services most co-booked with
    // service {id} (booking-item co-occurrence; the backend falls back to the same
    // category's top services when sparse). Reuses ServicesResp ({items:[ServiceDto]}).
    @GET("customer/services/{id}/related")
    suspend fun relatedServices(@Path("id") id: Int): ServicesResp

    @GET("catalog/search")
    suspend fun search(@Query("q") q: String? = null, @Query("category") category: Int? = null): ServicesWrap

    // ── Geo (§687/§692 — server-side free OpenStreetMap proxy; no key) ──
    @GET("geo/autocomplete")
    suspend fun geoAutocomplete(
        @Query("q") q: String,
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
    ): GeoSuggestionsResp

    @GET("geo/reverse")
    suspend fun geoReverse(@Query("lat") lat: Double, @Query("lon") lon: Double): GeoReverseResp

    @GET("geo/geocode")
    suspend fun geoGeocode(@Query("address") address: String): GeoGeocodeResp

    @GET("geo/directions")
    suspend fun geoDirections(
        @Query("from_lat") fromLat: Double,
        @Query("from_lon") fromLon: Double,
        @Query("to_lat") toLat: Double,
        @Query("to_lon") toLon: Double,
    ): GeoDirectionsResp

    // §690 — remote map/feature config from the OdioBook-level geo gateway. This
    // lives at /api/geo/* (NOT under /api/vedadrop/v1/), so we pass an ABSOLUTE
    // URL via @Url (built by the repository from NetworkConfig.baseUrl).
    @GET
    suspend fun geoAppConfig(@retrofit2.http.Url url: String): GeoAppConfigDto

    // ── Partner discovery ─────────────────────────────────────────────────────
    @GET("customer/partners")
    suspend fun partners(
        @Query("service_id") serviceId: Int? = null,
        // §707 — search by partner ID (all digits) or name/code.
        @Query("q") q: String? = null,
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

    // §743 — a parlour's verified experts ("who is coming") + the chat-after-booking gate.
    @GET("customer/partners/{id}/experts")
    suspend fun partnerExperts(@Path("id") id: Int): ExpertsResp

    @GET("customer/partners/{id}/can-chat")
    suspend fun partnerCanChat(@Path("id") id: Int): CanChatResp

    // ── Addresses ──────────────────────────────────────────────────────────────
    @GET("customer/addresses")
    suspend fun addresses(): AddressesResp

    @POST("customer/addresses")
    suspend fun addAddress(@Body body: AddressCreateReq): AddressDto

    @DELETE("customer/addresses/{id}")
    suspend fun deleteAddress(@Path("id") id: Int): OkResp

    @PATCH("customer/addresses/{id}")
    suspend fun updateAddress(@Path("id") id: Int, @Body body: Map<String, @JvmSuppressWildcards Any?>): AddressDto

    // ── Cart (single-partner, multi-service) ─────────────────────────────────────
    @GET("customer/cart")
    suspend fun getCart(): CartResp

    @POST("customer/cart")
    suspend fun addToCart(@Body body: CartAddReq): CartResp

    @PATCH("customer/cart/{id}")
    suspend fun patchCartItem(@Path("id") id: Int, @Body body: CartItemPatchReq): CartResp

    @DELETE("customer/cart/{id}")
    suspend fun deleteCartItem(@Path("id") id: Int): CartResp

    @DELETE("customer/cart")
    suspend fun clearCart(): CartResp

    @POST("customer/cart/quote")
    suspend fun cartQuote(@Body body: CartQuoteReq): QuoteResp

    // §722 — multi-partner combination: create one sibling booking per per-partner quote.
    @POST("customer/bookings/combo")
    suspend fun createCombo(@Body body: ComboReq): ComboResp

    // ── Quote + Bookings ────────────────────────────────────────────────────────
    @POST("customer/quote")
    suspend fun quote(@Body body: QuoteReq): QuoteResp

    @POST("customer/bookings")
    suspend fun createBooking(@Body body: BookingCreateReq): BookingDto

    // §703 — Flow-B open booking: no partner chosen; broadcast to nearby, time-
    // matched, service-matched professionals (first-to-accept-wins).
    @POST("customer/bookings/open")
    suspend fun createOpenBooking(@Body body: OpenBookingReq): OpenBookingResp

    // §703 — the customer's one-tap "I've spoken to my professional and confirm
    // this visit" — the ONLY thing that lets the partner start travelling.
    @POST("customer/bookings/{id}/confirm-visit")
    suspend fun confirmVisit(@Path("id") id: Int): ConfirmVisitResp

    @GET("customer/bookings")
    suspend fun bookings(@Query("status") status: String? = null): BookingsResp

    @GET("customer/bookings/{id}")
    suspend fun booking(@Path("id") id: Int): BookingDto

    @POST("customer/bookings/{id}/cancel")
    suspend fun cancelBooking(@Path("id") id: Int, @Body body: CancelReq): BookingDto

    // §704 — customer blocks/reports a partner: permanently kills partner→customer
    // contact (the strongest protection). + the blocked list to gate the UI.
    @POST("customer/partners/{id}/block")
    suspend fun blockPartner(@Path("id") id: Int, @Body body: Map<String, String?> = emptyMap()): OkResp

    @HTTP(method = "DELETE", path = "customer/partners/{id}/block", hasBody = false)
    suspend fun unblockPartner(@Path("id") id: Int): OkResp

    @GET("customer/blocked-partners")
    suspend fun blockedPartners(): BlockedPartnersResp

    // §704 — reschedule a pending/accepted booking to a new slot (≤3h before).
    @POST("customer/bookings/{id}/reschedule")
    suspend fun reschedule(@Path("id") id: Int, @Body body: RescheduleReq): BookingDto

    @POST("customer/bookings/{id}/start-otp")
    suspend fun startOtp(@Path("id") id: Int): StartOtpResp

    // §691 — customer reassignment: drop the current partner and re-offer the job to
    // all eligible nearby professionals (first-to-accept-wins), at the same price.
    @POST("customer/bookings/{id}/change-partner")
    suspend fun changePartner(@Path("id") id: Int): ChangePartnerResp

    @GET("customer/bookings/{id}/reassignment")
    suspend fun reassignmentStatus(@Path("id") id: Int): ReassignmentStatusResp

    // Connector model: no customer wallet (the customer pays the partner directly).

    // ── Reviews / Complaints / Wishlist ─────────────────────────────────────────
    @POST("customer/bookings/{id}/review")
    suspend fun review(@Path("id") id: Int, @Body body: ReviewReq): ReviewDto

    // §723 — the partner rates the customer after a completed visit (dual-rating loop).
    @POST("partner/bookings/{id}/rate-customer")
    suspend fun rateCustomer(@Path("id") id: Int, @Body body: RateCustomerReq): BookingDto

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

    // ── Chat (REST history) ──────────────────────────────────────────────────────
    @GET("customer/bookings/{id}/messages")
    suspend fun bookingMessages(@Path("id") id: Int): ChatMessagesResp

    @POST("customer/bookings/{id}/messages")
    suspend fun sendBookingMessage(@Path("id") id: Int, @Body body: ChatSendReq): ChatMessageDto

    // §704 — post-booking talk request (chat is locked after a booking ends).
    @GET("customer/bookings/{id}/talk-request")
    suspend fun getTalkRequest(@Path("id") id: Int): TalkRequestStateResp

    @POST("customer/bookings/{id}/talk-request")
    suspend fun raiseTalkRequest(@Path("id") id: Int, @Body body: Map<String, String?> = emptyMap()): TalkRequestDto

    @POST("customer/bookings/{id}/talk-request/{reqId}/respond")
    suspend fun respondTalkRequest(@Path("id") id: Int, @Path("reqId") reqId: Int,
                                   @Body body: Map<String, String>): TalkRequestDto

    // §704 — partner "messages from customers" inbox.
    @GET("partner/messages")
    suspend fun partnerInbox(): PartnerInboxResp

    @GET("partner/messages/{customerId}")
    suspend fun partnerInboxThread(@Path("customerId") customerId: Int): ChatMessagesResp

    @POST("partner/messages/{customerId}")
    suspend fun partnerInboxReply(@Path("customerId") customerId: Int, @Body body: ChatSendReq): ChatMessageDto

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
    suspend fun updatePartnerProfile(@Body body: Map<String, @JvmSuppressWildcards Any?>): PartnerDto

    // §713 — partner business location (service-area geofence). The PUT body's
    // radius_km is clamped server-side to ≤ radius_max_km (10km).
    @GET("partner/location")
    suspend fun getPartnerLocation(): PartnerLocationDto

    @retrofit2.http.PUT("partner/location")
    suspend fun putPartnerLocation(@Body body: PartnerLocationReq): PartnerLocationDto

    // §726 — the FULL service dictionary the partner can pick from. Unlike the
    // customer catalog (offered-only), this returns EVERY active service so a partner
    // can list a service no one offers yet. Fixes "partner adds a service but it
    // never appears / doesn't save" (the add-from list was the filtered catalog).
    @GET("partner/catalog")
    suspend fun partnerCatalog(): PartnerCatalogResp

    @GET("partner/services")
    suspend fun partnerServices(): PartnerServicesResp

    @POST("partner/services")
    suspend fun addPartnerService(@Body body: PartnerServiceReq): PartnerServiceDto

    @PATCH("partner/services/{id}")
    suspend fun patchPartnerService(@Path("id") id: Int, @Body body: Map<String, @JvmSuppressWildcards Any?>): PartnerServiceDto

    @DELETE("partner/services/{id}")
    suspend fun deletePartnerService(@Path("id") id: Int): OkResp

    // §743 — parlour expert management (each new/edited expert re-enters admin KYC).
    @GET("partner/experts")
    suspend fun partnerExpertsManage(): ExpertsResp

    @POST("partner/experts")
    suspend fun addPartnerExpert(@Body body: ExpertReq): ExpertDto

    @PATCH("partner/experts/{id}")
    suspend fun patchPartnerExpert(@Path("id") id: Int, @Body body: Map<String, @JvmSuppressWildcards Any?>): ExpertDto

    @DELETE("partner/experts/{id}")
    suspend fun deletePartnerExpert(@Path("id") id: Int): OkResp

    // §743 — sample professional descriptions suggested by the partner's categories.
    @GET("partner/description-suggestions")
    suspend fun descriptionSuggestions(): DescriptionSuggestionsResp

    // §710 P0-8 — the partner's own menu with real per-service prices.
    @GET("customer/partners/{id}/services")
    suspend fun partnerPricedServices(@retrofit2.http.Path("id") id: Int): PartnerPricedServicesResp

    // ── §737 Packages (partner-curated bundles) + Deals/Featured — payment-free.
    // A package books through the EXISTING cart → quote → booking path (its items
    // expand into the single-partner cart); no new booking endpoint.
    @GET("customer/partners/{id}/packages")
    suspend fun partnerPackages(@Path("id") id: Int): PackagesResp

    @GET("customer/packages/{id}")
    suspend fun packageDetail(@Path("id") id: Int): PackageDto

    @POST("customer/packages/{id}/add-to-cart")
    suspend fun addPackageToCart(@Path("id") id: Int, @Body body: Map<String, @JvmSuppressWildcards Any?>): CartResp

    @GET("customer/featured")
    suspend fun featured(): FeaturedResp

    @GET("partner/packages")
    suspend fun partnerOwnPackages(): PackagesResp

    @POST("partner/packages")
    suspend fun createPartnerPackage(@Body body: Map<String, @JvmSuppressWildcards Any?>): PackageDto

    @PATCH("partner/packages/{id}")
    suspend fun patchPartnerPackage(@Path("id") id: Int, @Body body: Map<String, @JvmSuppressWildcards Any?>): PackageDto

    @DELETE("partner/packages/{id}")
    suspend fun deletePartnerPackage(@Path("id") id: Int): OkResp

    @GET("partner/availability")
    suspend fun partnerAvailability(): PartnerAvailabilityResp

    @retrofit2.http.PUT("partner/availability")
    suspend fun setPartnerAvailability(@Body body: Map<String, @JvmSuppressWildcards Any?>): OkResp

    // §710 P0-9 — real online/away switch. The old toggle PATCHed profile {is_active}
    // (silently ignored); this writes is_online and removes an away partner from job
    // dispatch while keeping them listed.
    @POST("partner/availability/online")
    suspend fun setPartnerOnline(@Body body: Map<String, @JvmSuppressWildcards Any?>): OkResp

    @GET("partner/bookings")
    suspend fun partnerBookings(@Query("status") status: String? = null): BookingsResp

    @POST("partner/bookings/{id}/accept")
    suspend fun acceptBooking(@Path("id") id: Int): BookingDto

    // §744 — the parlour assigns/re-assigns a specific expert to a booking. Body {expert_id}.
    @POST("partner/bookings/{id}/assign-expert")
    suspend fun assignExpert(@Path("id") id: Int, @Body body: Map<String, Int>): BookingDto

    @POST("partner/bookings/{id}/reject")
    suspend fun rejectBooking(@Path("id") id: Int, @Body body: CancelReq): BookingDto

    // §704 — a partner can cancel any time she feels unsafe (no penalty for felt_unsafe).
    @POST("partner/bookings/{id}/cancel")
    suspend fun partnerCancelBooking(@Path("id") id: Int, @Body body: CancelReq): BookingDto

    @POST("partner/bookings/{id}/status")
    suspend fun partnerBookingStatus(@Path("id") id: Int, @Body body: StatusReq): BookingDto

    // §691 — partner emergency transfer + Rescue Board (first-to-accept-wins).
    @POST("partner/bookings/{id}/transfer")
    suspend fun transferBooking(@Path("id") id: Int, @Body body: TransferReq): TransferResp

    @GET("partner/offers")
    suspend fun partnerOffers(): OffersResp

    @POST("partner/offers/{id}/accept")
    suspend fun acceptOffer(@Path("id") id: Int): BookingDto

    @POST("partner/offers/{id}/decline")
    suspend fun declineOffer(@Path("id") id: Int): OkResp

    // ── Partner subscription (₹99/month listing fee — connector revenue) ───────
    @GET("partner/subscription")
    suspend fun subscription(): SubscriptionDto

    @POST("partner/subscription/subscribe")
    suspend fun subscribe(@Body body: Map<String, @JvmSuppressWildcards Any?> = emptyMap()): SubscriptionDto

    @POST("partner/subscription/cancel")
    suspend fun cancelSubscription(@Body body: Map<String, @JvmSuppressWildcards Any?> = emptyMap()): SubscriptionDto

    @GET("partner/subscription/payments")
    suspend fun subscriptionPayments(): SubscriptionPaymentsResp

    // ── Partner earnings + analytics ───────────────────────────────────────────
    @GET("partner/earnings")
    suspend fun partnerEarnings(): EarningsDto

    @GET("partner/analytics")
    suspend fun partnerAnalytics(): AnalyticsDto

    // ── Partner portfolio ──────────────────────────────────────────────────────
    @GET("partner/portfolio")
    suspend fun partnerPortfolio(): PortfolioResp

    @POST("partner/portfolio")
    suspend fun addPortfolioItem(@Body body: PortfolioCreateReq): PortfolioItemDto

    @DELETE("partner/portfolio/{id}")
    suspend fun deletePortfolioItem(@Path("id") id: Int): OkResp

    // ── §703 App config (feature flags / role visibility / policies) ───────────
    // Public; the app reads it on cold-start/resume to self-gate features + build
    // its role-based nav. Pass the known role so the payload is trimmed.
    @GET("config")
    suspend fun appConfig(@Query("role") role: String? = null): AppConfigResp

    // §703 SOS / panic — either party; persisted + admins alerted.
    @POST("sos")
    suspend fun raiseSos(@Body body: SosReq): SosResp

    // ── Notifications (in-app inbox + FCM device registration) ─────────────────
    @GET("notifications")
    suspend fun notifications(@Query("limit") limit: Int = 30): NotificationsResp

    @POST("notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Int): OkResp

    // §714 cross-notif-markall-5 — clear the whole unread badge in one tap.
    @POST("notifications/read-all")
    suspend fun markAllNotificationsRead(): OkResp

    @POST("notifications/devices")
    suspend fun registerDevice(@Body body: DeviceReq): OkResp

    @HTTP(method = "DELETE", path = "notifications/devices", hasBody = true)
    suspend fun deleteDevice(@Body body: DeviceDeleteReq): OkResp
}


