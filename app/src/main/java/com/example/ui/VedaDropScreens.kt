@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.*
import com.example.data.remote.CartItemDto
import com.example.data.remote.isUrgentOffer
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun VedaDropMainShell(viewModel: VedaDropViewModel) {
    val activeUser by viewModel.activeUser.collectAsState()
    val cart by viewModel.cart.collectAsState()
    // §705 — keep the partner "Open Jobs" pool badge live across every tab by
    // polling the offers list at a low frequency while the partner is signed in.
    val offers by viewModel.offers.collectAsState()
    LaunchedEffect(activeUser?.role) {
        if (activeUser?.role == "partner") {
            while (true) {
                viewModel.loadOffers()
                kotlinx.coroutines.delay(20000)
            }
        }
    }

    val currentThemeDark = true
    val scope = rememberCoroutineScope()

    // §694 — single app-wide Snackbar host. The ViewModel emits friendly API
    // messages (every error path funnels through friendly()); we surface them
    // here so the whole app shows user-readable toasts from one place.
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.uiMessages.collect { msg ->
            snackbarHostState.showSnackbar(message = msg.text, withDismissAction = true)
        }
    }

    val showLogin = !viewModel.isLoggedIn && !viewModel.isGuestMode

    // §687 — hardware Back: pop the nav history; when at a root, press Back twice
    // within 2s to exit (per the founder's "home se 2 back press exit").
    val context = LocalContext.current
    var lastBackMs by remember { mutableStateOf(0L) }
    if (!showLogin) {
        BackHandler {
            if (!viewModel.goBack()) {
                val now = System.currentTimeMillis()
                if (now - lastBackMs < 2000L) {
                    (context as? android.app.Activity)?.finish()
                } else {
                    lastBackMs = now
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!showLogin) {
                VedaDropBottomBar(
                    currentScreen = viewModel.currentScreen,
                    userRole = activeUser?.role ?: "customer",
                    cartCount = cart?.count ?: 0,
                    offersCount = offers.size,
                    onNavigate = { screen -> viewModel.currentScreen = screen },
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (currentThemeDark) DarkSlate else SoftCream)
        ) {
            if (showLogin) {
                VedaDropLoginScreen(viewModel)
                return@Box
            }
            // §705 — retain each screen's scroll position across navigation. The
            // shell swaps screens via AnimatedContent, which DISPOSES the leaving
            // screen; its rememberScrollState/rememberLazyListState (both already
            // saveable) would lose their slot. Wrapping each screen in a
            // SaveableStateProvider keyed by the screen restores its scroll (and
            // any rememberSaveable) when the user comes back — fixes "page top par
            // scroll ho jata hai jab vapas aate hain".
            val saveableStateHolder = rememberSaveableStateHolder()
            AnimatedContent(
                targetState = viewModel.currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "ScreenTransition"
            ) { screen ->
                // §708 — pull-to-refresh for every hosted screen via one wrapper.
                // The dispatcher re-fetches whatever `screen` shows (and the profile).
                VedaDropPullRefresh(onRefresh = { viewModel.refreshForScreen(screen) }) {
                saveableStateHolder.SaveableStateProvider(screen.toString()) {
                ComposeErrorBoundary(onGoHome = { viewModel.currentScreen = Screen.CustomerHome }) {
                when (screen) {
                    is Screen.CustomerHome -> CustomerHomeScreen(viewModel)
                    is Screen.SearchResults -> SearchResultsScreen(viewModel, screen.query)
                    is Screen.CategoryDetail -> CategoryDetailScreen(viewModel, screen.category)
                    is Screen.ServiceDetail -> ServiceDetailScreen(viewModel, screen.service)
                    is Screen.PartnerSelect -> PartnerSelectScreen(viewModel, screen.service)
                    is Screen.PartnerStore -> PartnerStoreScreen(viewModel, screen.partner)
                    is Screen.BookingConfirm -> BookingConfirmScreen(viewModel, screen.service, screen.partner)
                    is Screen.BookingDetail -> BookingDetailScreen(viewModel, screen.bookingId)
                    is Screen.Cart -> CartScreen(viewModel)
                    is Screen.MyBookings -> MyBookingsScreen(viewModel)
                    is Screen.ComplaintsList -> ComplaintsListScreen(viewModel)
                    is Screen.ComplaintDetail -> ComplaintDetailScreen(viewModel, screen.id)
                    is Screen.CustomerProfile -> CustomerProfileScreen(viewModel)
                    is Screen.ServiceBookingForm -> ServiceBookingFormScreen(viewModel)
                    is Screen.Favourites -> FavouritesScreen(viewModel)
                    is Screen.CustomerDashboard -> CustomerDashboardScreen(viewModel)
                    is Screen.PartnerReviews -> PartnerReviewsScreen(viewModel, screen.partner)

                    // Partner Screens
                    is Screen.PartnerDashboard -> PartnerDashboardScreen(viewModel)
                    is Screen.PartnerKyc -> PartnerKycScreen(viewModel)
                    is Screen.PartnerServices -> PartnerServicesScreen(viewModel)
                    is Screen.PartnerProfile -> PartnerProfileScreen(viewModel)
                    is Screen.PartnerSubscription -> PartnerSubscriptionScreen(viewModel)
                    is Screen.PartnerAvailability -> PartnerAvailabilityScreen(viewModel)
                    is Screen.PartnerBusinessLocation -> PartnerBusinessLocationScreen(viewModel)
                    is Screen.PartnerEarnings -> PartnerEarningsScreen(viewModel)
                    is Screen.PartnerAnalytics -> PartnerAnalyticsScreen(viewModel)
                    is Screen.PartnerPortfolio -> PartnerPortfolioScreen(viewModel)
                    is Screen.PartnerOffers -> PartnerOffersScreen(viewModel)
                    is Screen.PreBookingChat -> PreBookingChatScreen(viewModel, screen.service, screen.partner)
                    is Screen.Notifications -> NotificationsScreen(viewModel)
                }
                }
                }
                }
            }

            // §709 — global thin loading bar pinned to the top of the content area:
            // shows whenever any tracked action is in flight ("the app is working").
            VedaDropTopLoadingBar(
                visible = viewModel.anyBusy,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun VedaDropBottomBar(
    currentScreen: Screen,
    userRole: String,
    cartCount: Int,
    offersCount: Int = 0,   // §705 — live count of open pool jobs (partner badge)
    onNavigate: (Screen) -> Unit,
) {
    val selectedColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    )
    NavigationBar(
        tonalElevation = 8.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        if (userRole == "customer") {
            NavigationBarItem(
                selected = currentScreen is Screen.CustomerHome,
                onClick = { onNavigate(Screen.CustomerHome) },
                icon = { Icon(Icons.Default.Home, contentDescription = "Explore") },
                label = { Text("Explore", fontSize = 11.sp) },
                colors = selectedColors,
            )
            NavigationBarItem(
                selected = currentScreen is Screen.Cart,
                onClick = { onNavigate(Screen.Cart) },
                icon = {
                    if (cartCount > 0) {
                        BadgedBox(badge = { Badge { Text("$cartCount") } }) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                        }
                    } else {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                    }
                },
                label = { Text("Cart", fontSize = 11.sp) },
                colors = selectedColors,
                modifier = Modifier.testTag("cart_tab"),
            )
            NavigationBarItem(
                selected = currentScreen is Screen.MyBookings,
                onClick = { onNavigate(Screen.MyBookings) },
                icon = { Icon(Icons.Default.EventNote, contentDescription = "Bookings") },
                label = { Text("Bookings", fontSize = 11.sp) },
                colors = selectedColors,
                modifier = Modifier.testTag("bookings_tab"),
            )
            NavigationBarItem(
                selected = currentScreen is Screen.CustomerProfile,
                onClick = { onNavigate(Screen.CustomerProfile) },
                icon = { Icon(Icons.Default.Person, contentDescription = "User") },
                label = { Text("User", fontSize = 11.sp) },
                colors = selectedColors,
                modifier = Modifier.testTag("customer_profile_tab")
            )
        } else {
            // §705 — partner = a WORK app. Nav reflects "jobs come to me": the home
            // inbox, the OPEN-JOBS POOL (was buried), today's schedule, earnings, and
            // a business hub. No new Screen entries — every target already exists.
            NavigationBarItem(
                selected = currentScreen is Screen.PartnerDashboard,
                onClick = { onNavigate(Screen.PartnerDashboard) },
                icon = { Icon(Icons.Default.Work, contentDescription = "Jobs") },
                label = { Text("Jobs", fontSize = 11.sp) },
                colors = selectedColors,
            )
            NavigationBarItem(
                selected = currentScreen is Screen.PartnerOffers,
                onClick = { onNavigate(Screen.PartnerOffers) },
                icon = {
                    if (offersCount > 0) {
                        BadgedBox(badge = { Badge { Text("$offersCount") } }) {
                            Icon(Icons.Default.Bolt, contentDescription = "Open Jobs")
                        }
                    } else {
                        Icon(Icons.Default.Bolt, contentDescription = "Open Jobs")
                    }
                },
                label = { Text("Open Jobs", fontSize = 11.sp) },
                colors = selectedColors,
                modifier = Modifier.testTag("partner_pool_tab"),
            )
            NavigationBarItem(
                selected = currentScreen is Screen.MyBookings,
                onClick = { onNavigate(Screen.MyBookings) },
                icon = { Icon(Icons.Default.EventNote, contentDescription = "Schedule") },
                label = { Text("Schedule", fontSize = 11.sp) },
                colors = selectedColors,
                modifier = Modifier.testTag("partner_requests_tab"),
            )
            NavigationBarItem(
                selected = currentScreen is Screen.PartnerEarnings,
                onClick = { onNavigate(Screen.PartnerEarnings) },
                icon = { Icon(Icons.Default.Payments, contentDescription = "Earnings") },
                label = { Text("Earnings", fontSize = 11.sp) },
                colors = selectedColors,
            )
            NavigationBarItem(
                selected = currentScreen is Screen.PartnerProfile,
                onClick = { onNavigate(Screen.PartnerProfile) },
                icon = { Icon(Icons.Default.Person, contentDescription = "Business") },
                label = { Text("Business", fontSize = 11.sp) },
                colors = selectedColors,
            )
        }
    }
}

// ---------------- CUSTOMER SCREENS ----------------

@Composable
fun CustomerHomeScreen(viewModel: VedaDropViewModel) {
    val activeUser by viewModel.activeUser.collectAsState()
    val addresses by viewModel.addresses.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val deviceLoc by viewModel.deviceLocation.collectAsState()
    val ctx = LocalContext.current

    // §697 — the location picker (tap the "Deliver To" header to open it).
    var showLocationPicker by remember { mutableStateOf(false) }

    // §697 — on first Home entry, ask for location once if we don't have it, then
    // auto-select the device location so a fresh user isn't stranded on a dead
    // "Select Location" label. On grant → detect+save; if already granted → detect
    // straight away. ensureLocationFromDevice() is a one-shot no-op when an address
    // already exists, so this never fights a manual pick.
    val homeLocationPermLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) viewModel.ensureLocationFromDevice()
    }
    LaunchedEffect(Unit) {
        viewModel.captureDeviceLocation()
        if (com.example.data.LocationHelper.hasPermission(ctx)) {
            viewModel.ensureLocationFromDevice()
        } else {
            // Let the addresses flow hydrate first so we don't prompt a returning
            // user who already has a saved location; only a genuinely fresh user
            // (no address) gets the one-time permission request.
            delay(600)
            if (viewModel.addresses.value.isEmpty()) {
                homeLocationPermLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
                )
            }
        }
    }

    // In-app notifications: pull on entry, then poll every ~30s while on Home.
    // The loop auto-cancels when this composable leaves composition.
    LaunchedEffect(Unit) {
        while (isActive) {
            viewModel.loadNotifications()
            delay(30_000)
        }
    }

    val activeAddress = addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
    var minRatingFilter by remember { mutableStateOf(0.0) }

    // Search no longer filters the home list in place — it lives ONLY on the
    // dedicated SearchResults screen (tap the bar below). Home always shows the
    // full catalog (still honouring the rating filter chip).
    val filteredServices = VedaDropDataSource.services.filter { it.rating >= minRatingFilter }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // TOP Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DeepPlum, DarkSlate)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Column {
                // Luxury Branded Fallback Logo Layout
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("app_brand_logo")
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(VedaDropGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Spa,
                            contentDescription = "Veda Drop Logo",
                            tint = DeepPlum,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VEDA DROP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Deliver To".uppercase(),
                            color = VedaDropRose,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { showLocationPicker = true }
                                .testTag("location_header")
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = VedaDropRose, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activeAddress?.labelText?.let { "$it - ${activeAddress.line1}" } ?: "Select Location",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Change location",
                                tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        // §690 — "near you" indicator once a real device fix is known
                        // (discovery is then distance-sorted). Hidden if no GPS fix.
                        if (deviceLoc != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MyLocation, contentDescription = null,
                                    tint = SuccessGreen, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Near you · sorted by distance",
                                    color = SuccessGreen, fontSize = 10.sp)
                            }
                        }
                    }
                    NotificationBell(viewModel)
                    IconButton(
                        onClick = { viewModel.currentScreen = Screen.Cart },
                        modifier = Modifier.testTag("cart_view_btn")
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // SEARCH bar — a tap-to-open affordance (NOT an in-place filter).
                // Tapping opens the dedicated SearchResults screen so search lives
                // in exactly one place and never filters the home page itself.
                Surface(
                    onClick = { viewModel.currentScreen = Screen.SearchResults("") },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_search_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = VedaDropRose,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Search services & experts",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "WELCOME, ${activeUser?.name?.uppercase() ?: "GUEST"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = VedaDropRose
                )
                Spacer(modifier = Modifier.height(2.dp))
                // §687 — shorter, single-line tagline (was a tall 2-line displayLarge).
                Text(
                    text = "Beauty at your door",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))
                
                // Horizontal Filter Row (Discovery Brands & Ratings)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rating Filter Chip
                    var showRatingMenu by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            selected = minRatingFilter > 0.0,
                            onClick = { showRatingMenu = true },
                            label = { 
                                Text(
                                    text = if (minRatingFilter == 0.0) "Any Rating" else "⭐ $minRatingFilter+", 
                                    fontSize = 11.sp, 
                                    fontWeight = FontWeight.Bold
                                ) 
                            },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VedaDropRose,
                                selectedLabelColor = Color.White
                            )
                        )
                        DropdownMenu(
                            expanded = showRatingMenu,
                            onDismissRequest = { showRatingMenu = false }
                        ) {
                            listOf(
                                "Any Rating" to 0.0,
                                "⭐ 4.5+ Stars" to 4.5,
                                "⭐ 4.8+ Stars" to 4.8
                            ).forEach { (label, rating) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        minRatingFilter = rating
                                        showRatingMenu = false
                                    }
                                )
                            }
                        }
                    }
                    
                    if (minRatingFilter > 0.0) {
                        TextButton(
                            onClick = { minRatingFilter = 0.0 },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.6f))
                        ) {
                            Text("Clear", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                        }
                    }
                }
            }
        }
        
        UpcomingSessionReminderBanner(viewModel = viewModel)
        
        // Active bookings banner (Quick Entry)
        val activeBookings = bookings.filter { it.status != "completed" && it.status != "cancelled" && it.status != "rejected" }
        if (activeBookings.isNotEmpty()) {
            val mostRecent = activeBookings.first()
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { viewModel.currentScreen = Screen.BookingDetail(mostRecent.id) },
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ONGOING SERVICE", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                        Text(mostRecent.serviceName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Status: ${mostRecent.status.replace("_", " ").uppercase()}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Button(
                        onClick = { viewModel.currentScreen = Screen.BookingDetail(mostRecent.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose)
                    ) {
                        Text("Track", color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                    }
                }
            }
        }

        // CATEGORY SECTION — header hidden when the (partner-driven) catalog is empty.
        if (VedaDropDataSource.categories.isNotEmpty()) {
            Text(
                text = "EXPLORE CATEGORIES",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 4.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VedaDropDataSource.categories.forEach { cat ->
                val icon = when (cat.iconName) {
                    "content_cut" -> Icons.Default.ContentCut
                    "face" -> Icons.Default.Face
                    "brush" -> Icons.Default.Brush
                    "spa" -> Icons.Default.Spa
                    else -> Icons.Default.Star
                }
                Card(
                    modifier = Modifier
                        .width(135.dp)
                        .clickable { viewModel.currentScreen = Screen.CategoryDetail(cat) }
                        .testTag("category_card_${cat.id}"),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(cat.colorHex)).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = cat.name, tint = Color(android.graphics.Color.parseColor(cat.colorHex)))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(cat.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                        Text(cat.description, fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }
        }
        
        // §690 #3 — the "Custom Appointment / Book Any Service" banner routed into
        // the random-partner ServiceBookingForm (a half-built Flow-B broadcast stub
        // that picks an arbitrary partner, discards the slot and silently dead-ends
        // when no partner is available). Flow-B (broadcast → first partner to accept
        // at their price) is not built yet, so the entry point is removed to avoid a
        // dead-end. Re-add this banner when Flow-B ships.

        // §690 #4 — Home empty state: when no partner offers anything yet the catalog
        // is empty; show a single "coming soon" card instead of bare section headers.
        if (VedaDropDataSource.categories.isEmpty() && VedaDropDataSource.services.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("home_empty_state"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Spa, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Services coming soon to your area",
                        style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Beauty professionals near you are still setting up. Check back shortly.",
                        style = MaterialTheme.typography.bodySmall, color = Color.Gray,
                        textAlign = TextAlign.Center)
                }
            }
        }

        // SERVICES SUGGESTIONS — header hidden when there's nothing to show.
        if (filteredServices.isNotEmpty()) {
            Text(
                text = "TRENDING SERVICES",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp)
            )
        }

        filteredServices.chunked(2).forEach { rowServices ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowServices.forEach { service ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.currentScreen = Screen.ServiceDetail(service) }
                            .testTag("service_card_${service.id}"),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                            ) {
                                AsyncImage(
                                    model = service.imageUrl,
                                    contentDescription = service.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                )
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFBC02D),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        "${service.rating}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = service.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = service.description,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                minLines = 2
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    service.priceLabel(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "${service.durationMin} mins",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
                if (rowServices.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
        
        VedaDropMarketplaceFeed(viewModel = viewModel)

        FaqAccordionSection()
        
        Spacer(modifier = Modifier.height(32.dp))
    }

    // §697 — tap-the-header location picker (current location + search).
    if (showLocationPicker) {
        LocationPickerSheet(viewModel = viewModel, onDismiss = { showLocationPicker = false })
    }
}

/**
 * The ONE global search screen. The home + marketplace feed no longer filter
 * themselves in place; tapping the home search bar opens this dedicated screen.
 * Debounced server-backed service search (reuses viewModel.searchServices), with
 * a local fallback so results still appear if the call fails.
 */
@Composable
fun SearchResultsScreen(viewModel: VedaDropViewModel, initialQuery: String) {
    var query by remember { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<Service>>(emptyList()) }
    // §707 — partner (expert) matches, incl. lookup by partner ID.
    var partnerResults by remember { mutableStateOf<List<Partner>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Auto-focus the field on entry (robust: ignore the rare race before layout).
    LaunchedEffect(Unit) {
        runCatching {
            delay(150)
            focusRequester.requestFocus()
        }
    }

    val q = query.trim()
    // Debounced search: >= 2 chars hits the backend (300ms), with a local
    // catalog filter as a fallback when the call returns null / fails.
    LaunchedEffect(q) {
        if (q.length < 2) {
            results = emptyList()
            partnerResults = emptyList()
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        delay(300)
        val remote = viewModel.searchServices(q)
        results = remote ?: VedaDropDataSource.services.filter { service ->
            service.name.contains(q, ignoreCase = true) ||
                service.description.contains(q, ignoreCase = true)
        }
        // §707 — also look up professionals by ID or name so a customer handed a
        // partner ID can find + book that exact expert.
        partnerResults = viewModel.searchPartners(q)
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Compact top bar: back arrow + the single search field.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { if (!viewModel.goBack()) viewModel.currentScreen = Screen.CustomerHome }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .testTag("search_results_input"),
                placeholder = { Text("Search facials, makeup, mehndi…", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = VedaDropRose) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { query = "" },
                            modifier = Modifier.testTag("search_results_clear_btn")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                maxLines = 1,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = VedaDropRose,
                    unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.3f)
                )
            )
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VedaDropRose, modifier = Modifier.size(28.dp))
                }
            }
            q.length < 2 -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Search for facials, makeup, mehndi…", color = Color.Gray, textAlign = TextAlign.Center, maxLines = 2)
                }
            }
            results.isEmpty() && partnerResults.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp).testTag("search_results_empty"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.SearchOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No results for \"$q\"", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Try a different service, expert name or expert ID.", color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 2)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("search_results_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // §707 — "Experts" section first (so an ID lookup lands on top),
                    // each row opens the partner's store to book.
                    if (partnerResults.isNotEmpty()) {
                        item {
                            Text(
                                "Experts",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        }
                        items(partnerResults, key = { it.id }) { partner ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.currentScreen = Screen.PartnerStore(partner) }
                                    .testTag("search_partner_${partner.id}"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = partner.avatarUrl,
                                        contentDescription = partner.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(56.dp).clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = partner.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("ID: ${partner.id}", fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = VedaDropGold, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("${partner.rating}", fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                                        }
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                                }
                            }
                        }
                        if (results.isNotEmpty()) {
                            item {
                                Text(
                                    "Services",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                                )
                            }
                        }
                    }
                    items(results, key = { it.id }) { service ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                // Match the home flow: a service tap opens its detail.
                                .clickable { viewModel.currentScreen = Screen.ServiceDetail(service) }
                                .testTag("search_result_${service.id}"),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = service.imageUrl,
                                    contentDescription = service.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = service.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = VedaDropGold, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("${service.rating}", fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                                        Text(" · ${service.durationMin} mins", fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = service.priceLabel(),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VedaDropMarketplaceFeed(viewModel: VedaDropViewModel) {
    val favoritePartners by viewModel.favoritePartners.collectAsState()
    val allPartners = VedaDropDataSource.partners
    val allServices = VedaDropDataSource.services
    
    // Search lives ONLY on the dedicated SearchResults screen now; this feed
    // keeps just its category filter (no in-place search box).
    var selectedCategoryId by remember { mutableStateOf("All") }
    var showCompareModal by remember { mutableStateOf(false) }

    // §714 cust-catalog-4 — the catalog is 100% partner-driven (§690): show ONLY real
    // categories. The old hardcoded salon/beauty/makeup/massage fallback created phantom
    // chips (string ids that match no real partner) so tapping one yielded an empty list.
    val categoriesList = listOf(Category("All", "All", "All categories", "grid_view", "#CCCCCC")) +
        VedaDropDataSource.categories

    val filteredPartners = allPartners.filter { partner ->
        val matchesCategory = if (selectedCategoryId == "All") true else {
            val selectedCatInfo = categoriesList.find { it.id == selectedCategoryId }
            val catName = selectedCatInfo?.name ?: ""
            partner.categories.any { it.equals(selectedCategoryId, ignoreCase = true) || it.equals(catName, ignoreCase = true) } ||
            partner.servicesOffered.any { svcId -> allServices.any { s -> s.id == svcId && s.categoryId.equals(selectedCategoryId, ignoreCase = true) } }
        }
        
        matchesCategory
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.3f)) {
                Text(
                    text = "VEDA DROP BEAUTY MARKETPLACE",
                    style = MaterialTheme.typography.labelMedium,
                    color = VedaDropRose,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Top-tier salons & studios near you",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { showCompareModal = true },
                colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("compare_specialists_trigger_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.CompareArrows,
                    contentDescription = "Compare",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Compare ⚖️", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
            }
        }

        // Horizontal Category Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categoriesList.forEach { category ->
                val isSelected = selectedCategoryId == category.id
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategoryId = category.id },
                    label = { Text(category.name, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        val icon = when (category.id.lowercase()) {
                            "all" -> Icons.Default.GridView
                            "salon" -> Icons.Default.ContentCut
                            "beauty" -> Icons.Default.Face
                            "makeup" -> Icons.Default.Brush
                            "massage" -> Icons.Default.SelfImprovement
                            else -> Icons.Default.Category
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = category.name,
                            modifier = Modifier.size(14.dp),
                            tint = if (isSelected) VedaDropRose else Color.Gray
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VedaDropRose.copy(alpha = 0.25f),
                        selectedLabelColor = VedaDropRose,
                        labelColor = Color.Gray
                    ),
                    modifier = Modifier.testTag("category_chip_${category.id}")
                )
            }
        }
        
        if (filteredPartners.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("empty_marketplace_results"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Spa, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selectedCategoryId != "All") "No matches found for the selected category." else "No salons or beauty studios are currently active nearby.",
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            filteredPartners.forEach { partner ->
                val isFavorite = favoritePartners.any { it.partnerId == partner.id }
                
                // Get this partner's specific services matching category and search queries
                val partnerServices = allServices.filter { service ->
                    partner.servicesOffered.contains(service.id) &&
                    (selectedCategoryId == "All" || service.categoryId.equals(selectedCategoryId, ignoreCase = true))
                }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.currentScreen = Screen.PartnerStore(partner) }
                        .testTag("marketplace_partner_card_${partner.id}"),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            AsyncImage(
                                model = partner.avatarUrl.ifBlank { "" },
                                contentDescription = partner.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = partner.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    IconButton(
                                        onClick = { viewModel.toggleFavorite(partner.id) },
                                        modifier = Modifier.size(24.dp).testTag("partner_heart_toggle_${partner.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Save Partner",
                                            tint = if (isFavorite) VedaDropRose else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, null, tint = VedaDropGold, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("${partner.rating}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                        Text(" (${partner.reviewsCount})", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Text("•", color = Color.Gray, fontSize = 11.sp)
                                    Text("${partner.experienceYears} Yrs Exp", fontSize = 11.sp, color = Color.Gray)
                                    Text("•", color = Color.Gray, fontSize = 11.sp)
                                    Text("₹${partner.fromPricePaise / 100} min", fontSize = 11.sp, color = VedaDropRose, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = partner.description,
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        if (partnerServices.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = Color.Gray.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Respective Services Subsection in horizontal carousel styled exactly like food items
                            Text(
                                text = "MENU SELECTIONS & INSTANT ADD",
                                style = MaterialTheme.typography.labelSmall,
                                color = VedaDropRose,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                partnerServices.forEach { service ->
                                    var addBusy by remember { mutableStateOf(false) }
                                    Card(
                                        modifier = Modifier
                                            .width(160.dp)
                                            .clickable { viewModel.currentScreen = Screen.PartnerStore(partner) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                        border = BorderStroke(0.5.dp, Color.Gray.copy(alpha = 0.15f))
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            AsyncImage(
                                                model = service.imageUrl,
                                                contentDescription = service.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(70.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                val catIcon = when(service.categoryId.lowercase()) {
                                                    "salon" -> Icons.Default.ContentCut
                                                    "beauty" -> Icons.Default.Face
                                                    "makeup" -> Icons.Default.Brush
                                                    "massage" -> Icons.Default.SelfImprovement
                                                    else -> Icons.Default.Category
                                                }
                                                Icon(
                                                    imageVector = catIcon,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(10.dp),
                                                    tint = VedaDropRose
                                                )
                                                Text(
                                                    text = service.categoryId.uppercase(),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Gray
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = service.name,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "₹${service.pricePaise / 100}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = VedaDropRose
                                                    )
                                                    Text(
                                                        text = "${service.durationMin}m",
                                                        fontSize = 10.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        addBusy = true
                                                        viewModel.addToCart(partner.id, service.id) { _ ->
                                                            addBusy = false
                                                        }
                                                    },
                                                    enabled = !addBusy,
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(VedaDropRose.copy(alpha = 0.15f), CircleShape)
                                                        .testTag("feed_add_btn_${partner.id}_${service.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Quick add service to booking cart",
                                                        tint = VedaDropRose,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCompareModal) {
        PartnerComparisonModal(
            onDismiss = { showCompareModal = false },
            allPartners = allPartners,
            allServices = allServices
        )
    }
}

@Composable
fun PartnerComparisonModal(
    onDismiss: () -> Unit,
    allPartners: List<Partner>,
    allServices: List<com.example.data.Service>
) {
    var partner1 by remember { mutableStateOf<Partner?>(null) }
    var partner2 by remember { mutableStateOf<Partner?>(null) }

    var p1Expanded by remember { mutableStateOf(false) }
    var p2Expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CompareArrows, contentDescription = null, tint = VedaDropRose)
                Text("Specialist Comparison ⚖️", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Compare ratings, experience levels, and service catalogs side-by-side to find your ideal match.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                // Selectors Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Partner 1 Selector
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { p1Expanded = true },
                            modifier = Modifier.fillMaxWidth().testTag("compare_selector_p1"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, if (partner1 != null) VedaDropRose else Color.Gray.copy(alpha = 0.5f))
                        ) {
                            Text(partner1?.name ?: "Select Slot 1 👤", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                        }
                        DropdownMenu(
                            expanded = p1Expanded,
                            onDismissRequest = { p1Expanded = false },
                            modifier = Modifier.background(DeepPlum)
                        ) {
                            allPartners.forEach { partner ->
                                DropdownMenuItem(
                                    text = { Text("${partner.name} (⭐${partner.rating})", color = Color.White, fontSize = 12.sp) },
                                    onClick = {
                                        partner1 = partner
                                        p1Expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Partner 2 Selector
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { p2Expanded = true },
                            modifier = Modifier.fillMaxWidth().testTag("compare_selector_p2"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, if (partner2 != null) VedaDropRose else Color.Gray.copy(alpha = 0.5f))
                        ) {
                            Text(partner2?.name ?: "Select Slot 2 👤", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                        }
                        DropdownMenu(
                            expanded = p2Expanded,
                            onDismissRequest = { p2Expanded = false },
                            modifier = Modifier.background(DeepPlum)
                        ) {
                            allPartners.forEach { partner ->
                                DropdownMenuItem(
                                    text = { Text("${partner.name} (⭐${partner.rating})", color = Color.White, fontSize = 12.sp) },
                                    onClick = {
                                        partner2 = partner
                                        p2Expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // If both are selected, show side-by-side comparison tables
                if (partner1 != null || partner2 != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Metrics header row
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Feature Metric", fontWeight = FontWeight.Bold, color = VedaDropRose, fontSize = 11.sp, modifier = Modifier.weight(1.3f))
                                Text(partner1?.name?.substringBefore(" ") ?: "[P1]", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                Text(partner2?.name?.substringBefore(" ") ?: "[P2]", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }
                            Divider(color = Color.Gray.copy(alpha = 0.12f))

                            // Row: Rating
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Client Rating", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.weight(1.3f))
                                Text(partner1?.let { "⭐ ${it.rating} (${it.reviewsCount})" } ?: "—", color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                Text(partner2?.let { "⭐ ${it.rating} (${it.reviewsCount})" } ?: "—", color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }

                            // Row: Experience
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Experience Level", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.weight(1.3f))
                                Text(partner1?.let { "${it.experienceYears} Years" } ?: "—", color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                Text(partner2?.let { "${it.experienceYears} Years" } ?: "—", color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }

                            // Row: Minimum starting rate
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Starting Price", color = Color.LightGray, fontSize = 11.sp, modifier = Modifier.weight(1.3f))
                                Text(partner1?.let { "₹${it.fromPricePaise / 100}" } ?: "—", color = VedaDropRose, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                Text(partner2?.let { "₹${it.fromPricePaise / 100}" } ?: "—", color = VedaDropRose, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }

                            // Title: Service offerings and pricing table
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "SERVICE OFFERINGS & PRICING",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = VedaDropRose,
                                letterSpacing = 1.sp
                            )
                            Divider(color = Color.Gray.copy(alpha = 0.12f))

                            // Dynamic list of combined services offered by selected partners
                            val comparisonServices = allServices.filter { s ->
                                (partner1?.servicesOffered?.contains(s.id) == true) ||
                                (partner2?.servicesOffered?.contains(s.id) == true)
                            }

                            if (comparisonServices.isEmpty()) {
                                Text("No service pricing matches found.", color = Color.Gray, fontSize = 11.sp)
                            } else {
                                comparisonServices.forEach { service ->
                                    val hasP1 = partner1?.servicesOffered?.contains(service.id) == true
                                    val hasP2 = partner2?.servicesOffered?.contains(service.id) == true

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = service.name,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            modifier = Modifier.weight(1.3f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (hasP1) "₹${service.pricePaise / 100}" else "—",
                                            color = if (hasP1) Color.White else Color.Gray,
                                            fontSize = 11.sp,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.End
                                        )
                                        Text(
                                            text = if (hasP2) "₹${service.pricePaise / 100}" else "—",
                                            color = if (hasP2) Color.White else Color.Gray,
                                            fontSize = 11.sp,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .border(1.dp, Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Select at least one specialist above", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose)
            ) {
                Text("Close", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
            }
        },
        containerColor = DeepPlum
    )
}

data class ShowcaseItem(
    val title: String,
    val description: String,
    val expertName: String,
    val rating: Double,
    val imageUrl: String,
    val category: String,
    val reviewer: String,
    val review: String,
    val productsUsed: List<String>,
    val duration: String
)

@Composable
fun UpcomingSessionReminderBanner(viewModel: VedaDropViewModel) {
    val bookings by viewModel.bookings.collectAsState()
    val ctx = LocalContext.current
    
    val urgentBooking = remember(bookings) {
        bookings.firstOrNull { booking ->
            if (booking.status == "completed" || booking.status == "cancelled" || booking.status == "rejected" || booking.status == "refunded") {
                false
            } else {
                val isUrgentIso = if (booking.slotStartIso.isNotBlank()) {
                    val result = kotlin.runCatching {
                        val target = java.time.Instant.parse(booking.slotStartIso)
                        val now = java.time.Instant.now()
                        val diffHours = java.time.Duration.between(now, target).toHours()
                        diffHours in 0..24
                    }
                    result.getOrDefault(false)
                } else false

                isUrgentIso || booking.dateTimeSlot.contains("Tomorrow", ignoreCase = true)
            }
        }
    }

    if (urgentBooking != null) {
        // Trigger Toast reminder
        LaunchedEffect(urgentBooking.id) {
            Toast.makeText(
                ctx,
                "🔔 Veda Drop: Your beauty session starts in less than 24 hours!",
                Toast.LENGTH_LONG
            ).show()
        }

        var isVisible by remember { mutableStateOf(true) }

        if (isVisible) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("urgent_session_reminder_banner"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                ),
                border = BorderStroke(1.5.dp, Color(0xFFFFB74D))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFFFB74D).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Session Reminder",
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "UPCOMING SESSION REMINDER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE65100),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${urgentBooking.serviceName} scheduled at ${urgentBooking.dateTimeSlot}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Expert: ${urgentBooking.partnerName} • Tap to view tracking details",
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.clickable {
                                viewModel.currentScreen = Screen.BookingDetail(urgentBooking.id)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { isVisible = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss Reminder",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BeautyShowcaseSection(viewModel: VedaDropViewModel) {
    val items = remember {
        listOf(
            ShowcaseItem(
                title = "Glass Skin Hydra Facial",
                description = "Intense professional hydration and deep pore cleansing for an ultimate dewy finish.",
                expertName = "Anya Varma",
                rating = 5.0,
                category = "Facials",
                imageUrl = "",
                reviewer = "Meera Kapoor",
                review = "My skin looked absolutely like glass. Extremely hydrating, no post-treatment redness! Truly a premium glow session.",
                productsUsed = listOf("Dior Forever Glow Star Filter", "Estée Lauder Advanced Night Repair", "Clinique Moisture Surge"),
                duration = "60 mins"
            ),
            ShowcaseItem(
                title = "Royal Hair Creame Spa",
                description = "Deep nourishing scalp massage and intense hot oil hydration followed by a gorgeous blowout.",
                expertName = "Nisha Sen",
                rating = 4.9,
                category = "Hair Spa",
                imageUrl = "",
                reviewer = "Surbhi Gupta",
                review = "Unbelievable shine, my hair feels ten times healthier. Highly recommend Nisha!",
                productsUsed = listOf("Kérastase Chronologiste Caviar", "L'Oréal Expert Absolute Repair Oil"),
                duration = "75 mins"
            ),
            ShowcaseItem(
                title = "Flawless HD Bridal Makeup",
                description = "Custom traditional bridal make-up with dewy high-end cosmetic styling and heavy details.",
                expertName = "Priya Sharma",
                rating = 5.0,
                category = "Makeup Artistry",
                imageUrl = "",
                reviewer = "Aparna Roy",
                review = "Priya did my makeup for my wedding and it was flawless from morning till midnight. Not cakey at all, loved it!",
                productsUsed = listOf("Chanel Les Beiges Foundation", "Dior Backstage Highlight Palette", "Charlotte Tilbury Setting Spray"),
                duration = "120 mins"
            ),
            ShowcaseItem(
                title = "Therapeutic Lavender Massage",
                description = "Premium aromatherapy hot stones deep-tissue massage designed to completely dissolve body fatigue.",
                expertName = "Kiran Goel",
                rating = 4.8,
                category = "Body Wellness",
                imageUrl = "",
                reviewer = "Nalini Joshi",
                review = "Absolute heaven. The hot stone technique relaxed my back pain completely. A five-star wellness specialist.",
                productsUsed = listOf("Therapeutic Grade Lavender Essential Oil", "Organic Cold-Pressed Almond Oil"),
                duration = "90 mins"
            )
        )
    }

    var selectedItem by remember { mutableStateOf<ShowcaseItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .testTag("beauty_showcase_section")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = VedaDropGold,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "BEAUTY SHOWCASE",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = "Explore actual glowing transformations and premium treatment outcomes designed by elite specialists.",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Grid layout for 4 items
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowItems.forEach { item ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedItem = item }
                                .testTag("showcase_item_${item.title.replace(" ", "_")}"),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                ) {
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        color = VedaDropRose.copy(alpha = 0.9f),
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .align(Alignment.TopStart),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = item.category.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    
                                    Row(
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                            .align(Alignment.BottomEnd),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = VedaDropGold, modifier = Modifier.size(10.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("${item.rating}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = item.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "By ${item.expertName}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.description,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Expand Zoom dialog
    selectedItem?.let { item ->
        Dialog(
            onDismissRequest = { selectedItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .testTag("showcase_detail_dialog"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, VedaDropRose.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                        )
                        IconButton(
                            onClick = { selectedItem = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = VedaDropRose.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = item.category.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = VedaDropRose,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = VedaDropGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${item.rating} Rating", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Expert Designer: ${item.expertName} • Duration: ${item.duration}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "TREATMENT DESCRIPTION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "PRODUCTS FEATURED & USED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        item.productsUsed.forEach { product ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            ) {
                                Text(
                                    text = product,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "CLIENT REVIEW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.reviewer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row {
                                    repeat(5) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = VedaDropGold, modifier = Modifier.size(10.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"${item.review}\"",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            selectedItem = null
                            val matched = VedaDropDataSource.services.firstOrNull {
                                it.name.contains(item.category, ignoreCase = true) ||
                                item.title.contains(it.name, ignoreCase = true) ||
                                it.name.contains("Glow", ignoreCase = true)
                            } ?: VedaDropDataSource.services.firstOrNull()
                            
                            matched?.let {
                                viewModel.currentScreen = Screen.ServiceDetail(it)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("showcase_book_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Book Again", color = Color.Black, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
fun RealTimeJustBookedBanner(viewModel: VedaDropViewModel) {
    val services = VedaDropDataSource.services

    val notifications = remember(services) {
        val locations = listOf("Vasant Kunj", "Karol Bagh", "Saket", "Dwarka", "Noida", "Gurugram", "Defence Colony", "Connaught Place")
        val customers = listOf("Anya", "Nisha", "Priya", "Kiran", "Aditi", "Meera", "Divya", "Sneha", "Kriti", "Riya")
        val times = listOf("Just now", "2 mins ago", "5 mins ago", "3 mins ago", "1 min ago", "4 mins ago")
        
        if (services.isNotEmpty()) {
            services.mapIndexed { index, service ->
                val cust = customers[index % customers.size]
                val loc = locations[index % locations.size]
                val time = times[index % times.size]
                JustBookedNotification(
                    id = service.id + "_$index",
                    name = cust,
                    serviceName = service.name,
                    location = loc,
                    timeAgo = time,
                    service = service
                )
            }
        } else {
            listOf(
                "Bridal Glow Treatment Special",
                "Heal & Repair Hair Spa",
                "Glass Skin Hydra Facial",
                "Bridal Mehandi Artisan",
                "Deep Tissue Stress Relief Massage"
            ).mapIndexed { index, fallbackServiceName ->
                val cust = customers[index % customers.size]
                val loc = locations[index % locations.size]
                val time = times[index % times.size]
                JustBookedNotification(
                    id = "fallback_$index",
                    name = cust,
                    serviceName = fallbackServiceName,
                    location = loc,
                    timeAgo = time,
                    service = null
                )
            }
        }
    }

    if (notifications.isEmpty()) return

    var currentIndex by remember { mutableStateOf(0) }

    LaunchedEffect(notifications) {
        while (true) {
            delay(4000)
            currentIndex = (currentIndex + 1) % notifications.size
        }
    }

    val currentNotification = notifications[currentIndex]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable {
                val s = currentNotification.service
                if (s != null) {
                    viewModel.currentScreen = Screen.ServiceDetail(s)
                } else {
                    val matching = VedaDropDataSource.services.firstOrNull()
                    if (matching != null) {
                        viewModel.currentScreen = Screen.ServiceDetail(matching)
                    }
                }
            }
            .testTag("just_booked_marquee"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(VedaDropGold.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Verified,
                    contentDescription = "Social Proof Verified",
                    tint = VedaDropGold,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(10.dp))
            
            Box(
                modifier = Modifier.weight(1f)
            ) {
                AnimatedContent(
                    targetState = currentNotification,
                    transitionSpec = {
                        (slideInVertically { height -> height } + fadeIn()) togetherWith
                            (slideOutVertically { height -> -height } + fadeOut())
                    },
                    label = "JustBookedTransition"
                ) { notification ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = notification.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = " from ",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = notification.location,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "booked ${notification.serviceName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Surface(
                            color = VedaDropRose.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = notification.timeAgo.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = VedaDropRose,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FaqAccordionSection() {
    val faqs = listOf(
        FaqItem(
            question = "How do I book and modify my service?",
            answer = "You can select any premium beauty service, choose your preferred time and address, and book with confidence. To modify or reschedule, visit 'My Bookings' up to 4 hours before the service."
        ),
        FaqItem(
            question = "What safety & hygiene protocols are followed?",
            answer = "Our beauty specialists adhere to strict 5-star hygiene standards. They use single-use, medically sterilized kits, wear face protection / shields, sanitize all equipment before use, and confirm vaccination status."
        ),
        FaqItem(
            question = "What is the cancellation & refund policy?",
            answer = "We offer 100% free cancellations up to 4 hours prior to your scheduled appointment. Cancellations made within 4 hours are subject to a nominal fee to compensate the assigned beauty technician."
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .testTag("faq_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                Icons.Default.Help,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "FREQUENTLY ASKED QUESTIONS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        faqs.forEachIndexed { index, faq ->
            var isExpanded by remember { mutableStateOf(false) }
            val rotationState by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                label = "ArrowRotation"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .testTag("faq_item_$index"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.04f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = faq.question,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f).testTag("faq_header_$index")
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse question" else "Expand question",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(rotationState)
                                .testTag("faq_icon_$index")
                        )
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = faq.answer,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp,
                                modifier = Modifier.testTag("faq_content_$index")
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * §697 — Location picker, modelled on Solaris-Gemini's travel location UX.
 * Two ways to set the active "Deliver To" address:
 *   1. a prominent "Use my current location (GPS)" button — detects the device fix,
 *      reverse-geocodes it, and saves it as the active address;
 *   2. search-as-you-type over the free OpenStreetMap geo proxy — tap a result to
 *      set it. Both make the chosen place the default so Home updates immediately.
 */
@Composable
fun LocationPickerSheet(
    viewModel: VedaDropViewModel,
    onDismiss: () -> Unit,
) {
    // §725 — now delegates to the ONE shared ride-app-style full-screen picker
    // (LocationSearchOverlay: input pinned to the top, prominent "use current
    // location", search-as-you-type). Home "Deliver To" + booking-detail address
    // both call this, so they both get the new UX. A chosen place is reverse-
    // geocoded and saved as the active "Deliver To" address.
    LocationSearchOverlay(
        viewModel = viewModel,
        title = "Set your location",
        onDismiss = onDismiss,
    ) { p ->
        viewModel.setActiveLocation(
            label = p.title.ifBlank { "Location" },
            line1 = p.address.ifBlank { p.title.ifBlank { "Selected location" } },
            line2 = p.subtitle,
            city = p.city,
            pincode = p.pincode,
            lat = p.lat,
            lon = p.lon,
        )
    }
}

/**
 * §713 — Partner Business Location page. Reuses the EXACT §697 picker UX (an input
 * box with a current-location crosshair docked inside it + search-as-you-type over
 * the free OSM geo proxy) but as a full page instead of a dialog, and adds a
 * service-radius range card below (slider 1..radius_max_km, default 10). Setting a
 * location is what makes the partner geofence-matchable in customer discovery; the
 * radius decides how far away bookings can come from. Changeable any time.
 */
@Composable
fun PartnerBusinessLocationScreen(viewModel: VedaDropViewModel) {
    val saved by viewModel.partnerLocation.collectAsState()

    // Load the saved business location on entry.
    LaunchedEffect(Unit) { viewModel.loadPartnerLocation() }

    // §725 — the search/GPS picker is now the ONE shared full-screen overlay
    // (LocationSearchOverlay). A chosen place is reverse-geocoded and saved as the
    // business location with the current radius.
    var showPicker by remember { mutableStateOf(false) }

    // Radius slider — server caps it at radius_max_km (10km). Seed from the saved
    // value once it loads (default 10 until then), clamped to the server max.
    val maxKm = (saved?.radiusMaxKm ?: 10.0).coerceAtLeast(1.0)
    var radiusKm by remember(saved?.radiusKm, saved?.radiusMaxKm) {
        mutableStateOf(((saved?.radiusKm ?: 10.0).takeIf { it > 0 } ?: 10.0).coerceIn(1.0, maxKm))
    }

    if (showPicker) {
        LocationSearchOverlay(
            viewModel = viewModel,
            title = "Set business location",
            onDismiss = { showPicker = false },
        ) { p ->
            viewModel.savePartnerBusinessLocation(
                lat = p.lat, lon = p.lon,
                address = p.address.ifBlank { null },
                radiusKm = radiusKm,
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Business Location", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { viewModel.currentScreen = Screen.PartnerProfile }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Set where you're based so nearby clients can find you. Only bookings inside your service radius will be offered to you.",
                fontSize = 13.sp, color = Color.Gray
            )

            // Currently-saved location summary (so the page never looks "unsaved").
            val savedLat = saved?.lat
            val savedLon = saved?.lon
            Card(
                colors = CardDefaults.cardColors(containerColor = LightSage),
                border = BorderStroke(1.dp, VedaDropRose.copy(alpha = 0.25f)),
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = VedaDropRose)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Current business location", fontSize = 11.sp, color = Color.Gray)
                        if (saved?.hasLocation == true && savedLat != null && savedLon != null) {
                            Text(
                                (saved?.address ?: "").ifBlank {
                                    "%.5f, %.5f".format(savedLat, savedLon)
                                },
                                color = PlumDeepInk,   // §715 — dark text on the LIGHT LightSage card (forced-dark theme would render it near-white = unreadable)
                                fontWeight = FontWeight.Medium, fontSize = 14.sp,
                                maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text("Not set yet", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = OrderOrange)
                        }
                    }
                    if (viewModel.partnerLocationBusy) {
                        CircularProgressIndicator(color = VedaDropRose, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // §725 — open the ONE shared ride-app location picker (full-screen, input
            // pinned to the top, "use current location" + search-as-you-type). On pick
            // it saves the business location with the current radius.
            Button(
                onClick = { showPicker = true },
                modifier = Modifier.fillMaxWidth().testTag("partner_set_location_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (saved?.hasLocation == true) "Change location" else "Set location",
                    maxLines = 1, softWrap = false,
                )
            }

            // Radius range card — how far bookings can come from. Capped at the
            // server's radius_max_km (10km). Applies on release; needs a location.
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Service radius", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${radiusKm.roundToInt()} km", fontWeight = FontWeight.Bold, color = VedaDropRose)
                    }
                    Text(
                        "Bookings within ${radiusKm.roundToInt()} km of your business location will be offered to you (max ${maxKm.roundToInt()} km).",
                        fontSize = 12.sp, color = Color.Gray
                    )
                    Slider(
                        value = radiusKm.toFloat().coerceIn(1f, maxKm.toFloat()),
                        onValueChange = { radiusKm = it.toDouble() },
                        onValueChangeFinished = { viewModel.savePartnerLocationRadius(radiusKm) },
                        valueRange = 1f..maxKm.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = VedaDropRose,
                            activeTrackColor = VedaDropRose,
                            inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("partner_radius_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1 km", fontSize = 11.sp, color = Color.Gray)
                        Text("${maxKm.roundToInt()} km", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryDetailScreen(viewModel: VedaDropViewModel, category: Category) {
    val services = VedaDropDataSource.services.filter { it.categoryId == category.id }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(category.name, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { viewModel.currentScreen = Screen.CustomerHome }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepPlum.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Veda Drop Doorstep Certified", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text("Highly trained beauty experts carrying sanitized premium kits and single-use products for the clean environment salon safety.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
            
            items(services, key = { it.id }) { service ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { viewModel.currentScreen = Screen.ServiceDetail(service) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        AsyncImage(
                            model = service.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(service.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(service.description, fontSize = 12.sp, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(service.priceLabel(), fontWeight = FontWeight.Bold, color = VedaDropRose, fontSize = 16.sp)
                                Button(
                                    onClick = { viewModel.currentScreen = Screen.ServiceDetail(service) },
                                    colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose)
                                ) {
                                    Text("Add", color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceDetailScreen(viewModel: VedaDropViewModel, service: Service) {
    var isFaqExpanded by remember { mutableStateOf(false) }
    // §714 cust-catalog-1 — the catalog LIST serializer omits `inclusions`; only
    // GET /catalog/services/{id} returns them. Fetch the detail on entry so the
    // admin-curated "What's included" list actually reaches the customer.
    var detailInclusions by remember(service.id) { mutableStateOf(service.inclusions) }
    LaunchedEffect(service.id) {
        viewModel.loadServiceDetail(service.id) { full -> detailInclusions = full.inclusions }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(modifier = Modifier.height(260.dp).fillMaxWidth()) {
            AsyncImage(
                model = service.imageUrl,
                contentDescription = service.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )
            IconButton(
                onClick = { viewModel.currentScreen = Screen.CustomerHome },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Text(service.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = VedaDropGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${service.rating} (${service.reviewsCount} reviews)", color = Color.White, fontSize = 14.sp)
                }
            }
        }
        
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Duration: ${service.durationMin} mins", fontWeight = FontWeight.Medium)
                    Text(service.priceLabel(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = VedaDropRose)
                    // §690 — prices are set by partners; show the range source so the
                    // customer understands "kitne se kitne" and that we don't fix it.
                    if (service.partnerCount > 0) {
                        Text(
                            "Price set by ${service.partnerCount} " +
                                if (service.partnerCount == 1) "partner" else "partners",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                Button(
                    onClick = { viewModel.currentScreen = Screen.PartnerSelect(service) },
                    colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                    modifier = Modifier.testTag("select_partner_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Choose Partner", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                }
            }

            // §703 Flow-B — book WITHOUT picking a partner: we broadcast the request
            // to nearby available professionals and the first to accept wins. THIS is
            // what feeds the partner "open jobs" pool (Rescue Board).
            run {
                val openAddresses by viewModel.addresses.collectAsState()
                val openDeviceLoc by viewModel.deviceLocation.collectAsState()
                var showOpen by remember { mutableStateOf(false) }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { showOpen = true },
                    modifier = Modifier.fillMaxWidth().testTag("book_any_expert_btn"),
                    border = BorderStroke(1.dp, VedaDropRose),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VedaDropRose),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Book any available expert", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                }
                if (showOpen) {
                    OpenBookingDialog(viewModel, service, openAddresses, openDeviceLoc) { showOpen = false }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("SERVICE DESCRIPTION", fontWeight = FontWeight.Bold, color = VedaDropRose, letterSpacing = 1.sp)
            Text(service.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (detailInclusions.isNotEmpty()) {
                Text("WHAT'S INCLUDED", fontWeight = FontWeight.Bold, color = VedaDropRose, letterSpacing = 1.sp)
                detailInclusions.forEach { incl ->
                    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(incl, fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // §714 cust-catalog-2 — only show the FAQ accordion when there are real FAQs.
            // service.faqs is currently always empty (no data source), so the control used
            // to expand to a blank section — a dead, misleading affordance.
            if (service.faqs.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { isFaqExpanded = !isFaqExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Frequently Asked Questions", fontWeight = FontWeight.Bold)
                        Icon(
                            if (isFaqExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                    if (isFaqExpanded) {
                        service.faqs.forEach { faq ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Q: ${faq.first}", fontWeight = FontWeight.Bold, color = VedaDropRose)
                            Text("A: ${faq.second}", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            }
            }
        }
    }
}

// §703 Flow-B — pick a date + time for an OPEN booking (no partner). Broadcasts to
// nearby pros; the first to accept wins, at their listed price. Feeds the partner pool.
@Composable
private fun OpenBookingDialog(
    viewModel: VedaDropViewModel,
    service: Service,
    addresses: List<com.example.data.AddressEntity>,
    deviceLoc: Pair<Double, Double>?,
    onDismiss: () -> Unit,
) {
    val today = remember { java.time.LocalDate.now() }
    // §725 Batch-B — allow SAME-DAY open bookings (was tomorrow-only). The backend
    // accepts a future-but-soon slot as is_urgent when urgent_pool is ON, broadcasting
    // it to the Flow-B pool instead of rejecting it.
    var selDate by remember { mutableStateOf(today) }
    var selHour by remember { mutableStateOf(11) }
    val defaultAddr = addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
    val canBook = defaultAddr != null || deviceLoc != null

    @Composable
    fun chip(label: String, selected: Boolean, onClick: () -> Unit) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            color = if (selected) VedaDropRose else Color.Transparent,
            border = BorderStroke(1.dp, VedaDropRose),
            modifier = Modifier.padding(end = 6.dp),
        ) {
            Text(label, color = if (selected) Color.White else VedaDropRose, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Book any available expert") },
        text = {
            Column {
                Text("We'll send your request to nearby ${service.name} professionals. The first to accept becomes your expert, at their listed price.",
                    fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(14.dp))
                Text("Date", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    (0..6).forEach { d ->
                        val date = today.plusDays(d.toLong())
                        val lbl = when (d) {
                            0 -> "Today"
                            1 -> "Tom"
                            else -> "${date.dayOfMonth}/${date.monthValue}"
                        }
                        chip(lbl, date == selDate) { selDate = date }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Time", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    // §727 — align the open-booking dialog to the 7AM-6PM service window
                    // (was 9-5, which hid the 7-8 / 8-9 AM slots from the pool path).
                    (7..17).forEach { h -> chip("%02d:00".format(h), h == selHour) { selHour = h } }
                }
                if (!canBook) {
                    Spacer(Modifier.height(10.dp))
                    Text("Add a service address first (Profile → Addresses) or enable location.",
                        color = Color.Red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canBook,
                onClick = {
                    val iso = "%sT%02d:00:00".format(selDate.toString(), selHour)
                    viewModel.createOpenBooking(
                        serviceLines = listOf((service.id.toIntOrNull() ?: 0) to 1),
                        slotStartIso = iso,
                        addressId = defaultAddr?.id?.toInt(),
                        lat = if (defaultAddr == null) deviceLoc?.first else null,
                        lon = if (defaultAddr == null) deviceLoc?.second else null,
                    ) { dispatched ->
                        onDismiss()
                        if (dispatched) viewModel.currentScreen = Screen.MyBookings
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
            ) { Text("Send request", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) } },
    )
}

@Composable
fun PartnerSelectScreen(viewModel: VedaDropViewModel, service: Service) {
    val bookings by viewModel.bookings.collectAsState()

    // Discovery: who actually offers THIS service right now (subscription-gated
    // server-side). Blank until a subscribed partner adds it.
    LaunchedEffect(service.id) { viewModel.loadPartnersForService(service.id) }
    val partners = VedaDropDataSource.partners
    // §713 — geofencing: the backend returns requires_location (empty items) when
    // the customer's location is unknown, so we prompt them to set it. Re-run
    // discovery automatically once a location is picked.
    val requiresLocation = VedaDropDataSource.partnersRequireLocation
    val deviceLoc by viewModel.deviceLocation.collectAsState()
    var showLocationPicker by remember { mutableStateOf(false) }
    LaunchedEffect(deviceLoc) {
        if (deviceLoc != null && VedaDropDataSource.partnersRequireLocation) {
            viewModel.loadPartnersForService(service.id)
        }
    }
    if (showLocationPicker) {
        LocationPickerSheet(viewModel = viewModel, onDismiss = {
            showLocationPicker = false
            viewModel.loadPartnersForService(service.id)
        })
    }

    // Each professional sets their own price; the card shows their "from" price
    // (falling back to the catalog indicative price). The final estimate is
    // computed server-side at quote time from the partner's own rate.
    val marketplaceOffers = remember(partners) {
        partners.map { partner ->
            val price = if (partner.fromPricePaise > 0) partner.fromPricePaise else service.pricePaise
            Triple(partner, price, "")
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Choose a Beauty Expert", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { viewModel.currentScreen = Screen.ServiceDetail(service) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        // Open-marketplace info header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Marketplace info",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Open Marketplace",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Independent beauty experts decide their own rates & product promises. Seal-pack check is visually completed upon arrival.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 15.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Sellers Ready to Deliver: ${marketplaceOffers.size}",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            if (viewModel.partnersLoading) {
                item {
                    VedaDropInlineLoader(message = "Connecting with marketplace sellers...")
                }
            } else if (requiresLocation && marketplaceOffers.isEmpty()) {
                // §713 — geofencing needs the customer's location to match nearby pros.
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.LocationOff, contentDescription = null, tint = VedaDropRose, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Set your location to find experts near you", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(
                            "We match you with professionals who serve your area. Tell us where you are to see who's available.",
                            fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                        Button(
                            onClick = { showLocationPicker = true },
                            colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Set my location", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                        }
                    }
                }
            } else if (marketplaceOffers.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No professionals offer this service yet", fontWeight = FontWeight.Bold)
                        Text(
                            "New experts are joining Veda Drop every day — please check back soon.",
                            fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            items(marketplaceOffers, key = { it.first.id }) { offer ->
                val (partner, resolvedPrice, resolvedProducts) = offer
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { viewModel.currentScreen = Screen.PartnerStore(partner) }
                        .testTag("partner_card_${partner.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = partner.avatarUrl,
                                contentDescription = partner.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(partner.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val isFav by viewModel.isFavorite(partner.id).collectAsState(initial = false)
                                    IconButton(
                                        onClick = { viewModel.toggleFavorite(partner.id) },
                                        modifier = Modifier.size(24.dp).testTag("fav_toggle_${partner.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Favorite Toggle",
                                            tint = if (isFav) Color.Red else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    val isKycApproved = partner.kycStatus == "approved"
                                    Surface(
                                        color = if (isKycApproved) Color(0xFFE0F2F1) else Color.Gray.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isKycApproved) Icons.Default.Verified else Icons.Default.Info,
                                                contentDescription = if (isKycApproved) "Verified Partner" else "Not yet verified",
                                                tint = if (isKycApproved) Color(0xFF00796B) else Color.Gray,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = if (isKycApproved) "VERIFIED" else "Not yet verified",
                                                color = if (isKycApproved) Color(0xFF00796B) else Color.Gray,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = VedaDropGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${partner.rating} (${partner.reviewsCount} jobs completed)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${partner.experienceYears} Years Experience", fontSize = 11.sp, color = VedaDropRose, fontWeight = FontWeight.Bold)
                                    if (partner.kycStatus == "approved") {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("• KYC Verified", fontSize = 10.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                                // §722 — show the partner's minimum booking value so the
                                // customer sees the floor before booking (0 = no minimum).
                                if (partner.minimumOrderPaise > 0) {
                                    Text("Min booking ₹${partner.minimumOrderPaise / 100}",
                                        fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("${partner.etaMin} mins away", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                                Text("${partner.distanceKm} km", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                        
                        // Custom Pricing Badge Row
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SuccessGreen.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Custom Seller Rate",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SuccessGreen
                                )
                            }
                            Text(
                                text = "₹${resolvedPrice / 100}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                        
                        // Products / seal promise (only if the professional listed one)
                        if (resolvedProducts.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f), RoundedCornerShape(4.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "📦 PRODUCTS & SEAL QUALITY PROMISE:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VedaDropRose,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = resolvedProducts,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "🤝 Check visual security seal during on-spot arrival before accepting service.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        
                        // Portfolio Badges
                        if (partner.portfolioUrls.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("PORTFOLIO SHOWCASE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VedaDropRose)
                            Row(modifier = Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                partner.portfolioUrls.forEach { url ->
                                    AsyncImage(
                                        model = url,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(45.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        }

                        // Ratings & Reviews section
                        val partnerDbReviews = bookings.filter { it.partnerId == partner.id && it.reviewRating > 0 }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("RATINGS & CLIENT REVIEWS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VedaDropRose)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        if (partner.recentReviews.isEmpty() && partnerDbReviews.isEmpty()) {
                            Text("No reviews yet. Be the first to share your feedback!", fontSize = 11.sp, color = Color.Gray)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Dynamic Database Reviews
                                partnerDbReviews.forEach { dbRev ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Row {
                                                    repeat(dbRev.reviewRating) {
                                                        Icon(Icons.Default.Star, contentDescription = null, tint = VedaDropGold, modifier = Modifier.size(12.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.weight(1f))
                                                if (dbRev.status == "completed") {
                                                    Text("Verified booking", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(dbRev.reviewComment, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
                                        }
                                    }
                                }
                                
                                // Static Recent Reviews
                                partner.recentReviews.forEach { (text, rate) ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val intRate = rate.roundToInt().coerceIn(0, 5)
                                                    repeat(intRate) {
                                                        Icon(Icons.Default.Star, contentDescription = null, tint = VedaDropGold, modifier = Modifier.size(12.dp))
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("${rate}/5", fontSize = 9.sp, color = Color.Gray)
                                                }
                                                Spacer(modifier = Modifier.weight(1f))
                                                Text("Client feedback", fontSize = 9.sp, color = Color.Gray)
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(text, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.currentScreen = Screen.PreBookingChat(service, partner)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                border = BorderStroke(1.dp, VedaDropRose)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = VedaDropRose, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chat", color = VedaDropRose, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.addToCart(partner.id, service.id)
                                    viewModel.currentScreen = Screen.Cart
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(44.dp)
                                    .testTag("add_to_cart_${partner.id}"),
                                border = BorderStroke(1.dp, VedaDropRose)
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = VedaDropRose, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add", color = VedaDropRose, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                            }

                            Button(
                                onClick = {
                                    viewModel.updateBookingQuote(service, partner, resolvedPrice)
                                    viewModel.currentScreen = Screen.BookingConfirm(service, partner)
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(44.dp)
                                    .testTag("book_button_${partner.id}"),
                                colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose)
                            ) {
                                Text("Book", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingConfirmScreen(viewModel: VedaDropViewModel, service: Service, partner: Partner) {
    val addresses by viewModel.addresses.collectAsState()

    // §725 Batch-B — the customer can switch between saved addresses (radio list) and
    // add a new one via the shared full-screen location picker. selectedAddressId is
    // seeded to the current default ("last selected"); picking one persists it as the
    // next-time default via setDefaultAddress.
    var selectedAddressId by remember(addresses) {
        mutableStateOf((addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull())?.id)
    }
    val chosenAddress = addresses.firstOrNull { it.id == selectedAddressId }
        ?: addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
    var showLocationPicker by remember { mutableStateOf(false) }

    // §694 — booking-time data capture for this flow.
    var bookingNotes by remember { mutableStateOf("") }
    var genderPref by remember { mutableStateOf("female") }   // §722 women-only
    // Guard against duplicate submissions: confirmAndBook() navigates away on
    // success (this composable is disposed) but stays put + emits a uiMessage on
    // failure, so we re-enable the button whenever a message arrives.
    var bookingBusy by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.uiMessages.collect { bookingBusy = false }
    }

    val quote = viewModel.quoteBreakdown

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopAppBar(
            title = { Text("Confirm Booking Details", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { viewModel.currentScreen = Screen.PartnerSelect(service) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        
        Column(modifier = Modifier.padding(16.dp)) {
            // STEP 1: Address Card
            Text("1. DELIVERY ADDRESS", fontWeight = FontWeight.Bold, color = VedaDropRose)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (addresses.isEmpty()) {
                        Text("No delivery addresses saved yet", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        // §725 — selectable radio list; the current default is pre-selected.
                        addresses.forEach { addr ->
                            val isSel = addr.id == selectedAddressId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedAddressId = addr.id
                                        // Persist as next-time default ("last selected").
                                        viewModel.setDefaultAddress(addr.id)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                RadioButton(
                                    selected = isSel,
                                    onClick = {
                                        selectedAddressId = addr.id
                                        viewModel.setDefaultAddress(addr.id)
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = VedaDropRose)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(addr.labelText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${addr.line1} ${addr.line2}".trim(), fontSize = 12.sp, color = Color.Gray)
                                    Text("${addr.city} - ${addr.pincode}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    OutlinedButton(
                        onClick = { showLocationPicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_address_btn")
                    ) {
                        Icon(Icons.Default.AddLocation, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add new location", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                    }
                }
            }
            
            // STEP 2: §702 — real availability slot picker (date stepper + slot chips).
            Spacer(modifier = Modifier.height(16.dp))
            Text("2. PICK A TIME SLOT", fontWeight = FontWeight.Bold, color = VedaDropRose)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val slots = viewModel.availableSlots
                    val selectedSlotId = viewModel.selectedSlotId
                    val selectedDate = viewModel.selectedBookingDate

                    // Load real slots on entry + whenever the date changes.
                    LaunchedEffect(partner.id, service.id, selectedDate) {
                        viewModel.loadSlots(partner.id, service.id, selectedDate)
                    }

                    // Next-7-days date stepper (ISO yyyy-MM-dd drives loadSlots).
                    val today = remember { java.time.LocalDate.now() }
                    val dayFmt = remember { java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM", java.util.Locale.US) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // §725 Batch-B — allow SAME-DAY booking: offer the next 7 days
                        // starting TODAY (offset 0). The backend marks each slot's
                        // available=false for past / too-soon (<2h lead) / over-window
                        // times, so today still shows but unbookable slots are disabled.
                        for (offset in 0..6) {
                            val d = today.plusDays(offset.toLong())
                            val iso = d.toString()
                            val isSel = iso == selectedDate
                            val dayLabel = when (offset) {
                                0 -> "Today"
                                1 -> "Tomorrow"
                                else -> d.format(dayFmt)
                            }
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.loadSlots(partner.id, service.id, iso) },
                                label = { Text(dayLabel, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VedaDropRose,
                                    selectedLabelColor = Color.White,
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    when {
                        viewModel.slotsLoading -> {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = VedaDropRose)
                                Text("Loading available slots…", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        slots.isEmpty() -> {
                            Text(
                                "No free slots that day — pick another date.",
                                fontSize = 12.sp, color = Color(0xFFEC7063), fontWeight = FontWeight.Bold
                            )
                        }
                        else -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                slots.forEach { slot ->
                                    // Hour label: parse HH:mm out of the ISO start
                                    // ("2026-06-18T10:00:00Z" -> "10:00"); else the
                                    // 3rd ":" segment of slot_id; else the raw id.
                                    // §722 — show the slot as a 1-hour RANGE ("7AM-8AM").
                                    val hourLabel = slotHourRange(slot.start, slot.slotId)
                                    val isSel = slot.slotId == selectedSlotId
                                    // §725 — subtle hint for slots the server disabled.
                                    val hint = disabledSlotHint(slot)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        FilterChip(
                                            selected = isSel,
                                            enabled = slot.available,
                                            onClick = { viewModel.selectedSlotId = slot.slotId },
                                            label = { Text(hourLabel, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = VedaDropRose,
                                                selectedLabelColor = Color.White,
                                                disabledLabelColor = Color.Gray.copy(alpha = 0.5f),
                                            )
                                        )
                                        if (hint != null) {
                                            Text(hint, fontSize = 9.sp, color = Color.Gray.copy(alpha = 0.7f))
                                        }
                                    }
                                }
                                // §725 — let the customer fine-tune to :15/:30/:45.
                                CustomTimeChip(
                                    selectedDate = selectedDate,
                                    partnerId = partner.id,
                                    selectedSlotId = selectedSlotId,
                                    onPicked = { viewModel.selectedSlotId = it },
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "You'll confirm any fine-tuning directly with the professional in chat once they accept your request.",
                        fontSize = 11.sp, color = Color.Gray, lineHeight = 15.sp
                    )
                }
            }

            // STEP 3: Estimate (you pay the professional directly)
            Spacer(modifier = Modifier.height(16.dp))
            Text("3. ESTIMATE", fontWeight = FontWeight.Bold, color = VedaDropRose)
            if (quote != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // §701 — itemized breakdown (only non-zero lines).
                        val discountPaise = quote.couponDiscountPaise + quote.walletDiscountPaise
                        @Composable
                        fun lineItem(label: String, paise: Long, negative: Boolean = false) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, fontSize = 13.sp, color = Color.LightGray.copy(alpha = 0.9f))
                                Text(
                                    (if (negative) "- ₹${paise / 100}" else "₹${paise / 100}"),
                                    fontSize = 13.sp,
                                    color = if (negative) SuccessGreen else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (quote.basePaise > 0) lineItem("Service", quote.basePaise)
                        if (quote.distancePaise > 0) lineItem("Distance", quote.distancePaise)
                        if (quote.surgePaise > 0) lineItem("Surge", quote.surgePaise)
                        if (discountPaise > 0) lineItem("Discount", discountPaise, negative = true)
                        if (quote.taxPaise > 0) lineItem("Tax", quote.taxPaise)
                        Divider(modifier = Modifier.padding(vertical = 2.dp), color = Color.Gray.copy(alpha = 0.15f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                "₹${quote.totalPaise / 100}",
                                fontWeight = FontWeight.Bold,
                                color = VedaDropRose,
                                fontSize = 18.sp
                            )
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color.Gray.copy(alpha = 0.15f))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "You pay the partner directly after the service. Veda Drop never holds your money.",
                                fontSize = 10.sp,
                                color = Color.LightGray.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            // §694 — special requests + professional gender preference.
            Text("SPECIAL REQUESTS (OPTIONAL)", fontWeight = FontWeight.Bold, color = VedaDropRose)
            OutlinedTextField(
                value = bookingNotes,
                onValueChange = { bookingNotes = it },
                label = { Text("Notes for the professional") },
                placeholder = { Text("e.g. allergic to a product, gate code…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(96.dp),
                shape = RoundedCornerShape(12.dp)
            )
            Text("PROFESSIONAL PREFERENCE", fontWeight = FontWeight.Bold, color = VedaDropRose)
            Spacer(modifier = Modifier.height(8.dp))
            GenderPreferenceSelector(selected = genderPref, onSelect = { genderPref = it })

            Spacer(modifier = Modifier.height(20.dp))
            // §701 — connector-model trust strip.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SuccessGreen.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your phone number is shared only after the partner accepts. Pay the partner directly — no charges in the app.",
                    fontSize = 11.sp,
                    color = Color.LightGray.copy(alpha = 0.9f),
                    lineHeight = 15.sp,
                    maxLines = 3
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // §701 — cancellation policy disclosure.
            Text(
                text = "Free cancellation up to 4 hours before. Later cancellations may affect your trust score.",
                fontSize = 10.sp,
                color = Color.Gray,
                lineHeight = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            // §722 — one-time confirmation of the chosen DATE + SLOT (+ address) before
            // the booking is actually created (founder: "ek baar confirm bhi karna hai").
            var showBookingConfirm by remember { mutableStateOf(false) }
            Button(
                onClick = {
                    if (!bookingBusy && chosenAddress != null && viewModel.selectedSlotId != null) {
                        showBookingConfirm = true
                    }
                },
                enabled = !bookingBusy && chosenAddress != null && viewModel.selectedSlotId != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pay_book_action_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (bookingBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    if (bookingBusy) "Sending…" else "Review & confirm",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showBookingConfirm && chosenAddress != null) {
                val selSlot = viewModel.availableSlots.firstOrNull { it.slotId == viewModel.selectedSlotId }
                val slotLabel = if (selSlot != null) slotHourRange(selSlot.start, selSlot.slotId) else "your slot"
                val dateLabel = runCatching {
                    java.time.LocalDate.parse(viewModel.selectedBookingDate)
                        .format(java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM"))
                }.getOrDefault(viewModel.selectedBookingDate)
                AlertDialog(
                    onDismissRequest = { showBookingConfirm = false },
                    title = { Text("Confirm your appointment") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(service.name, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EventNote, contentDescription = null, tint = VedaDropRose, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("$dateLabel  •  $slotLabel")
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Home, contentDescription = null, tint = VedaDropRose, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${chosenAddress.labelText}, ${chosenAddress.line1}", maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Text("Pay the professional directly — no charges in the app.", fontSize = 11.sp, color = Color.Gray)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showBookingConfirm = false
                                bookingBusy = true
                                viewModel.bookingNotes = bookingNotes
                                viewModel.bookingGenderPref = genderPref
                                viewModel.confirmAndBook(service, partner, chosenAddress)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                            modifier = Modifier.testTag("confirm_booking_final_btn"),
                        ) { Text("Confirm booking", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBookingConfirm = false }) {
                            Text("Change date/time", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                        }
                    },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // §725 Batch-B — add a new delivery location via the shared full-screen picker
    // (GPS / search-as-you-type), replacing the old manual address dialog. On pick we
    // call setActiveLocation, which POSTs the address AND sets it as the default; the
    // addresses flow refresh re-seeds selectedAddressId to that new default.
    if (showLocationPicker) {
        LocationSearchOverlay(
            viewModel = viewModel,
            title = "Add delivery location",
            onDismiss = { showLocationPicker = false },
            onPicked = { picked ->
                viewModel.setActiveLocation(
                    label = picked.title.ifBlank { "Home" },
                    line1 = picked.address,
                    line2 = "",
                    city = picked.city,
                    pincode = picked.pincode,
                    lat = picked.lat,
                    lon = picked.lon,
                )
            },
        )
    }
}

// §703 — small reusable horizontal step indicator for a booking's lifecycle.
// Requested → Accepted → On the way → Started → Completed. Cancelled / rejected
// states short-circuit to a single red chip instead of the stepper.
@Composable
fun BookingStatusStepper(status: String) {
    if (status == "cancelled" || status == "rejected") {
        val label = status.replaceFirstChar { it.uppercase() }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F).copy(alpha = 0.18f))) {
            Text(
                label,
                color = Color(0xFFEF5350),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                maxLines = 1,
            )
        }
        return
    }
    // §710 P0-10 — Flow-B / reassignment states. Previously these fell through to the
    // "unmapped status" branch (every step gray, no explanation), so an open booking
    // that was searching — or that found no professional — looked broken. Give each a
    // clear chip instead.
    if (status == "reassigning") {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF9A825).copy(alpha = 0.18f))) {
            Text(
                "Finding you a professional…",
                color = Color(0xFFF9A825), fontWeight = FontWeight.Bold, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), maxLines = 1,
            )
        }
        return
    }
    if (status == "no_partner_found" || status == "reassign_failed") {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F).copy(alpha = 0.18f))) {
            Text(
                "No professional available yet",
                color = Color(0xFFEF5350), fontWeight = FontWeight.Bold, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), maxLines = 1,
            )
        }
        return
    }
    val steps = listOf("Requested", "Accepted", "On the way", "Started", "Completed")
    val current = when (status) {
        "pending", "requested" -> 0
        "accepted", "assigned" -> 1
        "partner_on_the_way", "on_the_way", "enroute", "arrived" -> 2
        "in_progress", "started" -> 3
        "completed", "refunded" -> 4
        else -> {
            // Unknown status (new server state shipped without a UI update, or a
            // typo). Fail loud instead of silently masquerading as "Requested":
            // -1 leaves every step gray so the drift is visible, and we log it.
            android.util.Log.e("BookingStatusStepper", "Unmapped booking status: '$status'")
            -1
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        steps.forEachIndexed { index, label ->
            val done = index < current
            val active = index == current
            val dotColor = when {
                done -> SuccessGreen
                active -> VedaDropRose
                else -> Color.Gray.copy(alpha = 0.4f)
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    // left connector (hidden for first step)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(if (index == 0) Color.Transparent else if (index <= current) SuccessGreen else Color.Gray.copy(alpha = 0.3f))
                    )
                    Box(
                        modifier = Modifier.size(18.dp).clip(CircleShape).background(dotColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                    // right connector (hidden for last step)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(if (index == steps.lastIndex) Color.Transparent else if (index < current) SuccessGreen else Color.Gray.copy(alpha = 0.3f))
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    label,
                    color = if (active) VedaDropRose else if (done) Color.White else Color.Gray,
                    fontSize = 9.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun BookingDetailScreen(viewModel: VedaDropViewModel, bookingId: String) {
    val bookings by viewModel.bookings.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val isPartnerView = activeUser?.role == "partner"
    val messages by viewModel.getMessagesForBooking(bookingId).collectAsState(initial = emptyList())
    
    val booking = bookings.firstOrNull { it.id == bookingId }
    var chatText by remember { mutableStateOf("") }

    // §690 — live map state (mutual tracking) + remote geo config (tile key/flags).
    val geoConfig by viewModel.geoConfig.collectAsState()
    val trackCustomer by viewModel.trackCustomer.collectAsState()
    val trackPartner by viewModel.trackPartner.collectAsState()
    val trackRoute by viewModel.trackRoute.collectAsState()
    val trackEta by viewModel.trackEta.collectAsState()
    val trackableStates = remember { setOf("accepted", "assigned", "partner_on_the_way", "arrived", "started") }
    val isTrackable = booking?.status in trackableStates
    // Open the tracking socket only while the job is in a trackable state; close on
    // leaving the screen or the state changing.
    DisposableEffect(bookingId, isTrackable) {
        if (isTrackable) viewModel.startLiveTracking(bookingId)
        onDispose { viewModel.stopLiveTracking() }
    }

    // §703 — pull a fresh booking status whenever this screen opens.
    LaunchedEffect(bookingId) { viewModel.loadBookingDetail(bookingId) }

    // Poll the chat thread while this screen is open so incoming counterparty
    // messages appear without us having to send or reopen (only location streams
    // over WS). Auto-cancels when leaving the screen.
    LaunchedEffect(bookingId) {
        while (isActive) {
            kotlinx.coroutines.delay(5000)
            viewModel.refreshThread(bookingId)
        }
    }

    // §691/§703 — while the job is being reassigned, poll the dedicated status
    // endpoint so the screen flips to the new partner the moment someone claims it.
    // Bounded (~10 tries × 4s); the LaunchedEffect auto-cancels on leaving the screen.
    LaunchedEffect(booking?.status) {
        if (booking?.status == "reassigning") {
            var tries = 0
            while (booking?.status == "reassigning" && tries < 10) {
                kotlinx.coroutines.delay(4_000)
                val st = viewModel.fetchReassignmentStatus(bookingId)
                // A new partner accepted → status moves off "reassigning"; pull the
                // fresh booking so the assigned-partner card + map update immediately.
                if (st?.bookingStatus != null && st.bookingStatus != "reassigning") {
                    viewModel.loadBookingDetail(bookingId)
                    break
                }
                viewModel.refreshActiveBookings()
                tries++
            }
        }
    }

    var showChatTab by remember { mutableStateOf(false) }
    var reviewRatingSelected by remember { mutableStateOf(5) }
    var reviewCommentText by remember { mutableStateOf("") }
    
    var hygieneRating by remember { mutableStateOf(5) }
    var skillRating by remember { mutableStateOf(5) }
    var authenticityRating by remember { mutableStateOf(5) }

    if (booking == null) {
        // §709 — a deep-link (notification tap) may land here before the booking
        // has been fetched: show a themed loader, not a "Not Found" flash.
        if (viewModel.bookingDetailLoading) {
            VedaDropFullScreenLoader(message = "Loading your booking…")
        } else {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Booking Not Found")
                Button(onClick = { viewModel.currentScreen = Screen.CustomerHome }) {
                    Text("Go Home", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Booking: ${booking.id}", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { 
                    if (activeUser?.role == "customer") {
                        viewModel.currentScreen = Screen.CustomerHome 
                    } else {
                        viewModel.currentScreen = Screen.PartnerDashboard
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepPlum, titleContentColor = Color.White)
        )
        
        Row(modifier = Modifier.fillMaxWidth().background(DeepPlum)) {
            Tab(
                selected = !showChatTab,
                onClick = { showChatTab = false },
                text = "Progress & Map",
                modifier = Modifier.weight(1f).testTag("tab_progress")
            )
            Tab(
                selected = showChatTab,
                onClick = { showChatTab = true },
                text = "Chat Log",
                modifier = Modifier.weight(1f).testTag("tab_chat")
            )
        }
        
        if (!showChatTab) {
            // PROGRESS & MAP
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // Current status banner (real booking state — no simulated GPS)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(DeepPlum, DarkSlate)))
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val statusIcon = when (booking.status) {
                            "partner_on_the_way" -> Icons.Default.DirectionsBike
                            "arrived", "started" -> Icons.Default.Home
                            "completed" -> Icons.Default.Verified
                            else -> Icons.Default.EventNote
                        }
                        Icon(statusIcon, contentDescription = null, tint = VedaDropRose, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        // §703 — Swiggy/Zomato-style horizontal step indicator.
                        BookingStatusStepper(booking.status)
                    }
                }

                // §690/§692/§698 — LIVE MAP (MapLibre + free OpenStreetMap tiles).
                // Shown while the job is in a trackable state AND the admin has Maps +
                // live-tracking enabled. No API key needed (free OpenFreeMap style).
                // Destination (green) = the customer's home/booking address; the partner
                // (blue) streams their live GPS toward it; line = route, with distance/ETA.
                val cfg = geoConfig
                val mapReady = cfg != null && cfg.mapsEnabled && cfg.features.liveTracking &&
                    cfg.tileStyleUrl.isNotBlank()
                if (isTrackable && mapReady) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        com.example.ui.map.VedaDropMapView(
                            styleUrl = cfg!!.tileStyleUrl,
                            customer = trackCustomer,
                            partner = trackPartner,
                            route = trackRoute,
                            followCurrent = false,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Green marker = customer home/destination; blue = the partner.
                        Text(
                            if (isPartnerView) "● Destination" else "● You (home)",
                            color = Color(0xFF1E8E3E), fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        )
                        Text(
                            if (isPartnerView) "● You" else "● Partner",
                            color = Color(0xFF009688), fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        )
                        when {
                            trackPartner == null ->
                                Text("Waiting for partner location…", color = Color.Gray, fontSize = 12.sp)
                            else -> trackEta?.let { eta ->
                                Text(eta, color = VedaDropRose, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    // DETAILS
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isPartnerView) {
                            // §722 req-1 — the PARTNER's header must show the CUSTOMER she is
                            // serving, not her own name. The name is server-revealed only
                            // post-accept (via `contact`); before that show a neutral label.
                            val custName = booking.counterpartyName.ifBlank { "Customer" }
                            Box(
                                modifier = Modifier.size(50.dp).clip(CircleShape)
                                    .background(VedaDropRose.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.Person, contentDescription = custName,
                                    tint = VedaDropRose, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(custName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Your customer", fontSize = 12.sp, color = VedaDropRose)
                            }
                        } else {
                            AsyncImage(
                                model = booking.partnerAvatar,
                                contentDescription = booking.partnerName,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(booking.partnerName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Professional assigned", fontSize = 12.sp, color = VedaDropRose)
                            }
                        }
                        
                        // Deep link code trigger action
                        StateChip(booking.status)
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // §722 req-1/req-2 — the partner's job detail must show the customer's
                    // service address + how far it is. The exact address is server-gated to
                    // post-accept (status not in pending/reassigning); pre-accept = area only.
                    if (isPartnerView) {
                        val addressRevealed = booking.status !in setOf("pending", "reassigning")
                        Text("CUSTOMER ADDRESS", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        if (addressRevealed && booking.addressText.isNotBlank()) {
                            Text(booking.addressText, fontSize = 14.sp)
                        } else {
                            val area = listOf(booking.city, booking.pincode).filter { it.isNotBlank() }.joinToString(" • ")
                            Text(
                                if (area.isNotBlank()) "$area — full address shown after you accept"
                                else "Shown after you accept",
                                fontSize = 13.sp, color = Color.Gray,
                            )
                        }
                        booking.distanceKm?.let { d ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(14.dp), tint = VedaDropRose)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${"%.1f".format(d)} km from you", fontSize = 12.sp, color = VedaDropRose, fontWeight = FontWeight.Medium)
                            }
                        }
                        if (addressRevealed && booking.addressLat != null && booking.addressLon != null) {
                            val navCtx = LocalContext.current
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        val uri = android.net.Uri.parse(
                                            "geo:${booking.addressLat},${booking.addressLon}?q=${booking.addressLat},${booking.addressLon}(Customer)")
                                        navCtx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
                                    }
                                },
                                border = BorderStroke(1.dp, VedaDropRose),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = VedaDropRose),
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Navigate to customer", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                    }

                    // §710 #5/#6 — show ALL booked services + the customer's note (were
                    // dropped by the app DTO, so a multi-service booking looked single
                    // and the note never appeared to the partner who performs the job).
                    if (booking.itemsSummary.isNotBlank()) {
                        Text("SERVICES", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(booking.itemsSummary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    if (booking.customerNotes.isNotBlank()) {
                        Text("NOTES", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(booking.customerNotes, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // §702 — customer-facing start-OTP: only once the partner has
                    // accepted (accepted / in_progress family). Source from the DTO,
                    // else fetch on demand via a "Show code" button.
                    val otpVisibleStates = setOf("accepted", "assigned", "in_progress", "partner_on_the_way", "arrived", "started")
                    if (!isPartnerView && booking.status in otpVisibleStates) {
                        // Only fall back to the VM-cached OTP when it was fetched for
                        // THIS booking — otherwise a code cached from a previously-opened
                        // booking would leak onto this screen and get relayed wrongly.
                        val otp = booking.startOtp.ifBlank {
                            if (viewModel.detailStartOtpFor == booking.id) (viewModel.detailStartOtp ?: "") else ""
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = VedaDropRose.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "Share this code with your partner when they arrive:",
                                    fontSize = 12.sp, color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                if (otp.isNotBlank()) {
                                    Text(otp, fontWeight = FontWeight.Bold, color = VedaDropRose, fontSize = 24.sp, letterSpacing = 4.sp)
                                } else {
                                    OutlinedButton(onClick = { viewModel.loadStartOtp(booking.id) }) {
                                        Text("Show code", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // §728 (parity C1) — TRANSPARENCY: vertical status-stage timeline
                    // driven by the server `timeline` array (each reached stage carries
                    // its real timestamp), current stage highlighted.
                    Text("BOOKING STATUS TIMELINE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = VedaDropRose)
                    Spacer(modifier = Modifier.height(12.dp))
                    val timelineTimes = remember(booking.timelineEncoded) { decodeTimelineTimes(booking.timelineEncoded) }
                    BookingTimelineVertical(status = booking.status, timelineTimes = timelineTimes)

                    // §728 (parity C1) — start-selfie PROOF: once the partner has started
                    // the job with a live selfie, show it as arrival proof to whoever is
                    // viewing (customer sees who served them; partner sees their own proof).
                    if (booking.startSelfieUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("ARRIVAL SELFIE PROOF", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = VedaDropRose)
                        Spacer(modifier = Modifier.height(6.dp))
                        SelfieProofImage(
                            url = booking.startSelfieUrl,
                            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(12.dp))
                                .testTag("start_selfie_proof"),
                        )
                        Text("Live selfie captured by the professional at the start of the service.",
                            fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    }

                    // §704 — Call button: shown only while the booking is live AND the
                    // server revealed the counterparty's number (the customer's number
                    // only if she opted in at booking; gone once the booking is over).
                    if (booking.callAllowed && booking.counterpartyPhone.isNotBlank()) {
                        val callCtx = LocalContext.current
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                runCatching {
                                    callCtx.startActivity(android.content.Intent(
                                        android.content.Intent.ACTION_DIAL,
                                        android.net.Uri.parse("tel:${booking.counterpartyPhone}")))
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("call_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Call ${booking.counterpartyName.ifBlank { "now" }}", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                        }
                    }

                    // §722/§13 — PARTNER lifecycle actions on the job detail, so she can
                    // accept, set out ("I'm on the way"), mark reached, start with the
                    // customer's OTP, and complete — right from the tapped booking.
                    // Reuses the same ViewModel calls as the dashboard queue.
                    if (isPartnerView) {
                        Spacer(modifier = Modifier.height(16.dp))
                        when (booking.status) {
                            "pending" -> {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.acceptJob(booking.id) },
                                        modifier = Modifier.weight(1f).testTag("detail_accept_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    ) { Text("Accept", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
                                    OutlinedButton(
                                        onClick = { viewModel.rejectJob(booking.id) },
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    ) { Text("Decline", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
                                }
                            }
                            "accepted", "assigned" -> {
                                Button(
                                    onClick = { viewModel.startTravelToJob(booking.id) },
                                    modifier = Modifier.fillMaxWidth().testTag("detail_on_the_way_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                                ) {
                                    Icon(Icons.Default.DirectionsBike, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("I'm on the way", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                }
                            }
                            "partner_on_the_way" -> {
                                Button(
                                    onClick = { viewModel.arriveAtJob(booking.id) },
                                    modifier = Modifier.fillMaxWidth().testTag("detail_reached_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                ) {
                                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Mark reached", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                }
                            }
                            "arrived" -> {
                                // §728 (parity C1) — Start now captures a live selfie
                                // (face-detected) before sending OTP + selfie together.
                                StartJobWithSelfie(
                                    viewModel = viewModel,
                                    bookingId = booking.id,
                                    fieldTag = "detail_otp_field",
                                    startTag = "detail_start_btn",
                                    startLabel = "Verify & start service",
                                    startContainerColor = VedaDropRose,
                                )
                            }
                            "started" -> {
                                Button(
                                    onClick = { viewModel.completePartnerJob(booking.id) },
                                    modifier = Modifier.fillMaxWidth().testTag("detail_complete_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Mark completed", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                }
                            }
                        }

                        // §728 (parity C1) — the PARTNER also gets a one-tap SOS on her
                        // active job (she is on-site/en route too). Same red affordance.
                        if (booking.status in SOS_ACTIVE_STATES) {
                            Spacer(modifier = Modifier.height(12.dp))
                            SosButton(viewModel = viewModel, bookingId = booking.id)
                        }
                    }

                    // §704 — after the booking ends, chat is LOCKED. Either party can
                    // request to talk again (forgot something / need help); the other
                    // person accepts or declines (response arrives in notifications).
                    if (booking.status in setOf("completed", "cancelled", "refunded")) {
                        var talkSent by remember(booking.id) { mutableStateOf(false) }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { viewModel.requestToTalk(booking.id, null) { talkSent = true } },
                            enabled = !talkSent,
                            modifier = Modifier.fillMaxWidth().testTag("request_talk_btn"),
                            border = BorderStroke(1.dp, VedaDropRose),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VedaDropRose),
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (talkSent) "Request sent" else "Request to talk", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                        }
                        Text("Chat is closed after a booking. Send a request and the other person can accept.",
                            fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    }

                    // §691 — customer reassignment. While reassigning, show a
                    // "finding…" banner; otherwise (accepted/assigned, >3h before the
                    // slot) offer a "Change Partner" action that re-broadcasts the job.
                    if (activeUser?.role == "customer") {
                        // §710 P0-10 — terminal Flow-B/reassign states. An open booking
                        // that found nobody used to sit with no message or action. Tell
                        // the customer plainly and give a way forward (pick a pro yourself);
                        // Cancel stays available via the cancel control below.
                        if (booking.status == "no_partner_found" || booking.status == "reassign_failed") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F).copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.35f)),
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("No professional available right now", fontWeight = FontWeight.Bold, color = Color(0xFFEF5350))
                                    Text(
                                        "We couldn't find a professional for this slot. Pick one yourself, or cancel below.",
                                        fontSize = 12.sp, color = Color.Gray,
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.currentScreen = Screen.CustomerHome },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                                    ) {
                                        Text("Browse & pick a professional", color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                    }
                                }
                            }
                        }
                        if (booking.status == "reassigning") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = VedaDropRose.copy(alpha = 0.10f)),
                                border = BorderStroke(1.dp, VedaDropRose.copy(alpha = 0.4f)),
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = VedaDropRose)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Finding a new professional…", fontWeight = FontWeight.Bold, color = VedaDropRose)
                                        Text("We're offering your booking to nearby professionals at the same price.", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        } else if ((booking.status == "accepted" || booking.status == "assigned") &&
                            withinLeadWindow(booking.slotStartIso, CUSTOMER_CHANGE_LEAD_MS)) {
                            var showChange by remember(booking.id) { mutableStateOf(false) }
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = { showChange = true },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, VedaDropRose),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = VedaDropRose),
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Change Partner", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                            }
                            if (showChange) {
                                val ctx = LocalContext.current
                                AlertDialog(
                                    onDismissRequest = { showChange = false },
                                    title = { Text("Change your professional?") },
                                    text = { Text("We'll find you a different nearby professional at the same price. This can't be undone. (Allowed up to 3 hours before your appointment.)") },
                                    confirmButton = {
                                        Button(
                                            enabled = !viewModel.reassignmentBusy,
                                            onClick = {
                                                viewModel.changePartner(booking.id) { err ->
                                                    Toast.makeText(ctx, err ?: "Finding a new professional…", Toast.LENGTH_SHORT).show()
                                                    if (err == null) showChange = false
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                                        ) { Text("Yes, change", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
                                    },
                                    dismissButton = { TextButton(onClick = { showChange = false }) { Text("Keep partner", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) } },
                                )
                            }
                        }

                        // §703 — pre-visit safety gate: the customer must confirm
                        // the visit (after speaking to the professional) before that
                        // professional is allowed to set out. One tap satisfies it.
                        if ((booking.status == "accepted" || booking.status == "assigned") &&
                            booking.preVisitRequired && !booking.preVisitContactOk) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.confirmVisit(booking.id) },
                                modifier = Modifier.fillMaxWidth().testTag("confirm_visit_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Confirm visit", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                            }
                            Text(
                                "For your safety, please speak with your professional first, then confirm. They can only set out after you confirm.",
                                fontSize = 11.sp, color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }

                        // §728 (parity C1) — prominent one-tap SOS (red + confirm) while a
                        // professional is assigned or en route (replaces the §703 outlined one).
                        if (booking.status in SOS_ACTIVE_STATES) {
                            Spacer(modifier = Modifier.height(12.dp))
                            SosButton(viewModel = viewModel, bookingId = booking.id)
                        }

                        // §704 — EMERGENCY: dial the police / women helpline directly
                        // (numbers come from /config, admin-editable). Also logs an
                        // admin-visible note that the user pressed Emergency.
                        if (booking.status in setOf("accepted", "assigned", "partner_on_the_way", "arrived", "started")) {
                            val emCtx = LocalContext.current
                            val emNum = viewModel.emergencyNumbers().firstOrNull() ?: "112"
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    runCatching {
                                        emCtx.startActivity(android.content.Intent(
                                            android.content.Intent.ACTION_DIAL,
                                            android.net.Uri.parse("tel:$emNum")))
                                    }
                                    viewModel.raiseSos(booking.id)   // server records kind + admin note
                                },
                                modifier = Modifier.fillMaxWidth().testTag("emergency_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Emergency — Call $emNum", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                            }
                            Text(
                                "Calls the police / women helpline. Women helpline: ${viewModel.womenHelpline()}.",
                                fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp),
                            )

                            // §704 — "I feel unsafe": cancel instantly with no penalty.
                            var showUnsafe by remember(booking.id) { mutableStateOf(false) }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { showUnsafe = true },
                                modifier = Modifier.fillMaxWidth().testTag("unsafe_cancel_btn"),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("I feel unsafe — cancel & leave", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                            }
                            if (showUnsafe) {
                                AlertDialog(
                                    onDismissRequest = { showUnsafe = false },
                                    title = { Text("Cancel and leave safely?") },
                                    text = { Text("This cancels the booking immediately with no penalty. If you are in danger, also tap Emergency to call ${emNum}.") },
                                    confirmButton = {
                                        Button(
                                            onClick = { viewModel.cancelBookingUnsafe(booking.id); showUnsafe = false },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        ) { Text("Cancel now", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
                                    },
                                    dismissButton = { TextButton(onClick = { showUnsafe = false }) { Text("Stay", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) } },
                                )
                            }

                            // §704 — block & report the professional (kills all contact).
                            if (booking.partnerId.isNotBlank() && booking.partnerId != "0") {
                                var showBlock by remember(booking.id) { mutableStateOf(false) }
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(
                                    onClick = { showBlock = true },
                                    modifier = Modifier.fillMaxWidth().testTag("block_partner_btn"),
                                ) {
                                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Block & report this professional", color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                }
                                if (showBlock) {
                                    AlertDialog(
                                        onDismissRequest = { showBlock = false },
                                        title = { Text("Block this professional?") },
                                        text = { Text("They will never be able to message or contact you again, and we'll flag this to our team.") },
                                        confirmButton = {
                                            Button(
                                                onClick = { viewModel.blockPartner(booking.partnerId) { showBlock = false } },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            ) { Text("Block & report", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
                                        },
                                        dismissButton = { TextButton(onClick = { showBlock = false }) { Text("Cancel", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) } },
                                    )
                                }
                            }
                        }

                        // §704 — Reschedule a pending/accepted booking (≤3h before
                        // the slot, same window as Change Partner). Opens a dialog that
                        // reuses the booking date-stepper + slot-picker.
                        // §714 cust-book-3 — include "assigned": the backend allows reschedule
                        // from pending/accepted/assigned, but the button omitted assigned.
                        if ((booking.status == "pending" || booking.status == "accepted" ||
                                booking.status == "assigned") &&
                            withinLeadWindow(booking.slotStartIso, CUSTOMER_CHANGE_LEAD_MS)) {
                            var showReschedule by remember(booking.id) { mutableStateOf(false) }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { showReschedule = true },
                                modifier = Modifier.fillMaxWidth().testTag("reschedule_booking_btn"),
                                border = BorderStroke(1.dp, VedaDropRose),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = VedaDropRose),
                            ) {
                                Icon(Icons.Default.EventNote, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reschedule", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                            }
                            if (showReschedule) {
                                RescheduleDialog(
                                    viewModel = viewModel,
                                    booking = booking,
                                    onDismiss = { showReschedule = false },
                                )
                            }
                        }

                        // Required Booking Cancellation Flow
                        // §710 P0-10 — let the customer cancel a stuck open booking
                        // that found no professional (no_partner_found / reassign_failed).
                        val activeCancelStates = setOf("pending", "accepted", "assigned", "reassigning",
                            "partner_on_the_way", "no_partner_found", "reassign_failed")
                        if (booking.status in activeCancelStates) {
                            var showCancelDialog by remember(booking.id) { mutableStateOf(false) }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier.fillMaxWidth().testTag("cancel_booking_trigger_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cancel Appointment", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                            }
                            
                            if (showCancelDialog) {
                                var selectedReason by remember { mutableStateOf("") }
                                var dropdownExpanded by remember { mutableStateOf(false) }
                                val reasons = listOf(
                                    "Personal delay / Emergency",
                                    "Schedule conflict / Changed mind",
                                    "Price is higher than expected",
                                    "Assigned partner has poor ratings",
                                    "Found another outlet / better offer",
                                    "Other beauty service concerns"
                                )
                                
                                AlertDialog(
                                    onDismissRequest = { showCancelDialog = false },
                                    title = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                            Text("Cancel This Appointment?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    },
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text(
                                                text = "Please help us improve service quality by selecting a cancellation reason below. This feedback is shared anonymously with partners to optimize scheduling.",
                                                fontSize = 12.sp,
                                                color = Color.LightGray
                                            )
                                            // §701 — cancellation policy disclosure.
                                            Text(
                                                text = "Free cancellation up to 4 hours before. Later cancellations may affect your trust score.",
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                lineHeight = 15.sp
                                            )

                                            Text(
                                                text = "SELECT CANCELLATION REASON (REQUIRED):",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = VedaDropRose,
                                                letterSpacing = 1.sp
                                            )
                                            
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                OutlinedButton(
                                                    onClick = { dropdownExpanded = true },
                                                    modifier = Modifier.fillMaxWidth().testTag("cancel_reason_selector"),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                                    border = BorderStroke(1.dp, if (selectedReason.isNotBlank()) VedaDropRose else Color.Gray.copy(alpha = 0.5f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = selectedReason.ifBlank { "Select Cancellation Reason ▾" },
                                                            fontSize = 12.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            softWrap = false
                                                        )
                                                    }
                                                }
                                                
                                                DropdownMenu(
                                                    expanded = dropdownExpanded,
                                                    onDismissRequest = { dropdownExpanded = false },
                                                    modifier = Modifier.background(DeepPlum).fillMaxWidth(0.85f)
                                                ) {
                                                    reasons.forEach { reason ->
                                                        DropdownMenuItem(
                                                            text = { Text(reason, color = Color.White, fontSize = 12.sp) },
                                                            onClick = {
                                                                selectedReason = reason
                                                                dropdownExpanded = false
                                                            },
                                                            modifier = Modifier.testTag("cancel_reason_item_${reason.replace(" ", "_")}")
                                                        )
                                                    }
                                                }
                                            }
                                            
                                            if (selectedReason.isBlank()) {
                                                Text(
                                                    text = "* Selection of a reason is required to submit cancellation.",
                                                    color = Color.Gray,
                                                    fontSize = 10.sp
                                                )
                                            } else {
                                                Text(
                                                    text = "Selected: $selectedReason",
                                                    color = SuccessGreen,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                if (selectedReason.isNotBlank()) {
                                                    viewModel.cancelBooking(booking.id, selectedReason)
                                                    showCancelDialog = false
                                                }
                                            },
                                            enabled = selectedReason.isNotBlank(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.testTag("confirm_cancel_booking_btn")
                                        ) {
                                            Text("Cancel Booking 🔕", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(
                                            onClick = { showCancelDialog = false },
                                            modifier = Modifier.testTag("dismiss_cancel_booking_btn")
                                        ) {
                                            Text("Keep Appointment", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                        }
                                    },
                                    containerColor = DeepPlum
                                )
                            }
                        }
                    }

                    // §723 — PARTNER rates the CUSTOMER after a completed visit (the second
                    // half of the dual-rating loop). Hidden once submitted (server flag).
                    if (isPartnerView && booking.status == "completed") {
                        Spacer(modifier = Modifier.height(24.dp))
                        if (booking.customerRated) {
                            Card {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("You rated this customer", fontWeight = FontWeight.Bold, color = VedaDropRose)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = VedaDropGold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${booking.customerRating}/5 Stars", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            var custRating by remember(booking.id) { mutableStateOf(5) }
                            var custComment by remember(booking.id) { mutableStateOf("") }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, VedaDropRose.copy(alpha = 0.3f)),
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Rate your customer", fontWeight = FontWeight.Bold, color = VedaDropRose, style = MaterialTheme.typography.titleMedium)
                                    Text("How was your experience with this customer?", fontSize = 12.sp, color = Color.Gray)
                                    Row {
                                        (1..5).forEach { r ->
                                            IconButton(onClick = { custRating = r }, modifier = Modifier.size(36.dp)) {
                                                Icon(Icons.Default.Star, contentDescription = "$r star",
                                                    tint = if (r <= custRating) VedaDropGold else Color.Gray,
                                                    modifier = Modifier.size(22.dp))
                                            }
                                        }
                                    }
                                    OutlinedTextField(
                                        value = custComment,
                                        onValueChange = { custComment = it },
                                        placeholder = { Text("Optional note (punctuality, behaviour, etc.)") },
                                        modifier = Modifier.fillMaxWidth().testTag("rate_customer_comment"),
                                    )
                                    Button(
                                        onClick = { viewModel.rateCustomerForBooking(booking.id, custRating, custComment.trim()) },
                                        colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                                        modifier = Modifier.align(Alignment.End).testTag("submit_rate_customer_btn"),
                                    ) {
                                        Text("Submit rating", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                    }
                                }
                            }
                        }
                    }

                    // REVENUE REVIEWS BOX (customer rates the partner — customer view only)
                    if (!isPartnerView && booking.status == "completed" && booking.reviewRating == 0) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, VedaDropRose.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Verified Multi-Dimension Review Form", fontWeight = FontWeight.Bold, color = VedaDropRose, style = MaterialTheme.typography.titleMedium)
                                Text("Ratings are verified & locked behind transaction ID: #${booking.id}", fontSize = 11.sp, color = Color.Gray)
                                
                                // 1. Skill Rating Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Technical Grooming Skill", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Row {
                                        (1..5).forEach { rate ->
                                            IconButton(
                                                onClick = { skillRating = rate },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = "Grooming Skill",
                                                    tint = if (rate <= skillRating) VedaDropRose else Color.Gray,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // 2. Hygiene Rating Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Hygiene & Sanitation Care", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Row {
                                        (1..5).forEach { rate ->
                                            IconButton(
                                                onClick = { hygieneRating = rate },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = "Hygiene Care",
                                                    tint = if (rate <= hygieneRating) VedaDropRose else Color.Gray,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // 3. Product Authenticity Rating Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Product Authenticity & Seal Check", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Row {
                                        (1..5).forEach { rate ->
                                            IconButton(
                                                onClick = { authenticityRating = rate },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = "Product Authenticity",
                                                    tint = if (rate <= authenticityRating) VedaDropRose else Color.Gray,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                OutlinedTextField(
                                    value = reviewCommentText,
                                    onValueChange = { reviewCommentText = it },
                                    placeholder = { Text("Comment on the therapist's brand seals, packaging authenticity, or sanitation behavior…") },
                                    modifier = Modifier.fillMaxWidth().testTag("review_input_comment")
                                )
                                Button(
                                    onClick = onClick@{
                                        if (reviewCommentText.trim().isEmpty()) {
                                            viewModel.notify("Please write a comment before submitting.", isError = true)
                                            return@onClick
                                        }
                                        val roundedAverage = kotlin.math.round((skillRating + hygieneRating + authenticityRating) / 3.0).toInt()
                                        val structuredComment = "[Skill: $skillRating/5, Hygiene: $hygieneRating/5, Products: $authenticityRating/5] ${reviewCommentText.trim()}"
                                        viewModel.submitBookingReview(booking.id, roundedAverage, structuredComment)
                                    },
                                    enabled = reviewCommentText.trim().isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                                    modifier = Modifier.align(Alignment.End).testTag("submit_triple_review_btn")
                                ) {
                                    Text("Submit Verified Review", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                }
                            }
                        }
                    } else if (booking.status == "completed" && booking.reviewRating > 0) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Card {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Your Submitted Review", fontWeight = FontWeight.Bold, color = VedaDropRose)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = VedaDropGold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${booking.reviewRating}/5 Stars", fontWeight = FontWeight.Bold)
                                }
                                Text(booking.reviewComment, fontSize = 13.sp, color = Color.Gray)
                            }
                        }
                    }
                    
                    // COMPLAINT ACTIVATE
                    if (booking.status == "completed" || booking.status == "cancelled") {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { viewModel.currentScreen = Screen.ComplaintsList },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Report, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Raise Complaint / Help Ticket", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        } else {
            // CHAT LOG TAB
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.weight(1f).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        val isMe = msg.senderRole == activeUser?.role
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (isMe) 12.dp else 0.dp,
                                    bottomEnd = if (isMe) 0.dp else 12.dp
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(msg.text, color = if (isMe) Color.Black else MaterialTheme.colorScheme.onSurface)
                                    // §714 cust-chat-4 — the server flags a moderation-blocked
                                    // message back to its sender; show it wasn't delivered.
                                    if (isMe && msg.blocked) {
                                        Text(
                                            "Not delivered — flagged",
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = chatText,
                        onValueChange = { chatText = it },
                        placeholder = { Text("Type your message here...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_text")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (chatText.isNotBlank()) {
                                viewModel.sendChatMessage(booking.id, activeUser?.role ?: "customer", chatText)
                                chatText = ""
                            }
                        },
                        modifier = Modifier
                            .background(VedaDropRose, CircleShape)
                            .testTag("send_chat_msg_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}

// §722 — render a one-hour booking slot as a RANGE label, e.g. "7AM-8AM" … "5PM-6PM".
// Parses the start hour from the ISO start ("…T10:00…") or the slot_id's 3rd ":" segment.
fun slotHourRange(startIso: String?, slotId: String): String {
    val h = startIso?.substringAfter("T", "")?.take(2)?.toIntOrNull()
        ?: slotId.split(":").getOrNull(2)?.toIntOrNull()
        ?: return slotId
    // §725 — 4-segment slot ids ("{partner}:{date}:{hour}:{minute}") carry a minute.
    val minute = slotId.split(":").getOrNull(3)?.toIntOrNull()
    fun lbl(x: Int, m: Int): String {
        val hr = (x % 12).let { if (it == 0) 12 else it }
        val ap = if (x % 24 < 12) "AM" else "PM"
        return if (m > 0) "%d:%02d%s".format(hr, m, ap) else "$hr$ap"
    }
    return if (minute != null && minute > 0) {
        // A custom-minute slot is a 1-hour window from HH:MM (e.g. 1:15-2:15 PM).
        "${lbl(h, minute)}-${lbl(h + 1, minute)}"
    } else {
        "${lbl(h, 0)}-${lbl(h + 1, 0)}"
    }
}

/** §725 Batch-B — short, subtle reason a disabled slot can't be booked. The backend
 *  sets available=false for past / too-soon(<2h lead) / over-window slots; we infer a
 *  friendly label from the slot's start vs now ("too soon" if it's later today but
 *  inside the lead window, else "closed"). Returns null for an available slot. */
fun disabledSlotHint(slot: com.example.data.remote.SlotDto): String? {
    if (slot.available) return null
    val start = parseSlotStartInstant(slot)
        ?: return "closed"
    val now = java.time.Instant.now()
    return when {
        !start.isAfter(now) -> "closed"                                  // already passed
        start.isBefore(now.plus(java.time.Duration.ofHours(2))) -> "too soon"  // inside lead window
        else -> "closed"                                                  // over-window / day off
    }
}

/** Best-effort parse of a slot's start into an Instant from its ISO start or slot_id. */
private fun parseSlotStartInstant(slot: com.example.data.remote.SlotDto): java.time.Instant? {
    val iso = slot.start
    if (!iso.isNullOrBlank()) {
        runCatching { return java.time.OffsetDateTime.parse(iso).toInstant() }
        runCatching {
            return java.time.LocalDateTime.parse(iso.removeSuffix("Z"))
                .atZone(java.time.ZoneId.systemDefault()).toInstant()
        }
    }
    // Fall back to the slot_id grammar "{partner}:{YYYY-MM-DD}:{hour}[:{minute}]".
    val parts = slot.slotId.split(":")
    val date = parts.getOrNull(1) ?: return null
    val hour = parts.getOrNull(2)?.toIntOrNull() ?: return null
    val minute = parts.getOrNull(3)?.toIntOrNull() ?: 0
    return runCatching {
        java.time.LocalDate.parse(date).atTime(hour, minute)
            .atZone(java.time.ZoneId.systemDefault()).toInstant()
    }.getOrNull()
}

/**
 * §725 Batch-B (founder: "1:15 / 1:30 / 1:45 bhi chunna ho to"). A "Custom time"
 * affordance shown after the hour chips. Tapping it opens a small picker — pick the
 * HOUR (7am-6pm window) and a MINUTE (0/15/30/45) — and builds a 4-segment slot id
 * "{partner}:{date}:{hour}:{minute}" (the LOCKED grammar), set as the selected slot.
 *
 * The chosen custom slot is still subject to the server's available rules at quote/
 * book time (past / <2h lead / over-window are rejected there), so this only widens
 * the *granularity* a customer can request, not the validity window.
 */
@Composable
fun CustomTimeChip(
    selectedDate: String,
    partnerId: String,
    selectedSlotId: String?,
    onPicked: (String) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    // Is the current selection a custom (4-segment, non-zero minute) slot? If so, show it.
    val selParts = selectedSlotId?.split(":") ?: emptyList()
    val selMinute = selParts.getOrNull(3)?.toIntOrNull() ?: 0
    val isCustomSelected = selectedSlotId != null && selMinute > 0 &&
        selParts.getOrNull(1) == selectedDate
    val chipLabel = if (isCustomSelected) "Custom · ${slotHourRange(null, selectedSlotId!!)}" else "Custom time"

    FilterChip(
        selected = isCustomSelected,
        onClick = { showPicker = true },
        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) },
        label = { Text(chipLabel, fontSize = 12.sp, maxLines = 1, softWrap = false) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = VedaDropRose,
            selectedLabelColor = Color.White,
        )
    )

    if (showPicker) {
        var pickHour by remember { mutableStateOf(selParts.getOrNull(2)?.toIntOrNull() ?: 10) }
        var pickMinute by remember { mutableStateOf(if (selMinute > 0) selMinute else 15) }
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Pick a custom time") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Hour (7 AM – 6 PM)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Start hours 7..17 (each is a 1-hour window ending by 6 PM).
                        (7..17).forEach { h ->
                            FilterChip(
                                selected = h == pickHour,
                                onClick = { pickHour = h },
                                label = {
                                    val hr = (h % 12).let { if (it == 0) 12 else it }
                                    val ap = if (h < 12) "AM" else "PM"
                                    Text("$hr $ap", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VedaDropRose, selectedLabelColor = Color.White,
                                )
                            )
                        }
                    }
                    Text("Minutes", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0, 15, 30, 45).forEach { m ->
                            FilterChip(
                                selected = m == pickMinute,
                                onClick = { pickMinute = m },
                                label = { Text(":%02d".format(m), fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VedaDropRose, selectedLabelColor = Color.White,
                                )
                            )
                        }
                    }
                    val hr = (pickHour % 12).let { if (it == 0) 12 else it }
                    val ap = if (pickHour < 12) "AM" else "PM"
                    Text("Requesting %d:%02d %s — the professional confirms availability.".format(hr, pickMinute, ap),
                        fontSize = 11.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // LOCKED grammar: 3-segment when minute 0 (back-compat), else 4-segment.
                        val slotId = if (pickMinute == 0) "$partnerId:$selectedDate:$pickHour"
                        else "$partnerId:$selectedDate:$pickHour:$pickMinute"
                        onPicked(slotId)
                        showPicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                ) { Text("Use this time", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                }
            },
        )
    }
}


@Composable
fun TimelineStep(title: String, desc: String, isActive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Circle,
            contentDescription = null,
            tint = if (isActive) SuccessGreen else Color.LightGray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                title,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = if (isActive) MaterialTheme.colorScheme.onBackground else Color.Gray,
                fontSize = 14.sp
            )
            Text(desc, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

// §728 (parity C1) — TRANSPARENCY. Decode BookingEntity.timelineEncoded
// ("status|iso8601\n…") into a {status -> formatted local time} map. A blank `at`
// (state not yet reached) is dropped. Best-effort parse (offset-aware then bare).
private fun decodeTimelineTimes(encoded: String): Map<String, String> {
    if (encoded.isBlank()) return emptyMap()
    val fmt = java.time.format.DateTimeFormatter.ofPattern("dd MMM, h:mm a", java.util.Locale.US)
    val out = LinkedHashMap<String, String>()
    encoded.split("\n").forEach { line ->
        val pipe = line.indexOf('|')
        if (pipe <= 0) return@forEach
        val status = line.substring(0, pipe)
        val at = line.substring(pipe + 1)
        if (at.isBlank()) return@forEach
        val inst = runCatching { java.time.OffsetDateTime.parse(at).toInstant() }
            .recoverCatching {
                java.time.LocalDateTime.parse(at.removeSuffix("Z"))
                    .atZone(java.time.ZoneId.systemDefault()).toInstant()
            }.getOrNull()
        if (inst != null) {
            out[status] = inst.atZone(java.time.ZoneId.systemDefault()).format(fmt)
        }
    }
    return out
}

/**
 * §728 (parity C1) — VERTICAL status-stage stepper for the booking-detail screen
 * (customer + partner). Renders the full lifecycle ladder
 * (Booked → Assigned → On the way → Reached → Started → Completed) as a connected
 * vertical timeline: each reached stage filled (rose) with its timestamp; the
 * CURRENT stage highlighted (ring + bold); future stages greyed.
 *
 * Timestamps come from the server `timeline` array (consumed via [timelineTimes],
 * keyed by the contract status — pending/accepted/assigned/started/completed). The
 * server ladder has no separate on_the_way/arrived instants, so those two stages
 * derive their reached/current state from the live [status] and simply omit a time.
 */
@Composable
fun BookingTimelineVertical(status: String, timelineTimes: Map<String, String>) {
    // Display ladder: label + the contract-status key used to look up its timestamp
    // (null when the server timeline has no instant for that stage).
    data class Stage(val label: String, val timeKey: String?)
    val stages = listOf(
        Stage("Appointment booked", "pending"),
        Stage("Professional assigned", "assigned"),
        Stage("On the way to you", null),       // no server instant (derived from status)
        Stage("Reached your location", null),   // no server instant (derived from status)
        Stage("Service started", "started"),
        Stage("Completed", "completed"),
    )
    // How far the live status has progressed along the ladder (the index of the
    // CURRENT stage). Cancelled/rejected are handled by the badge above this; if we
    // somehow get here for them, leave everything at stage 0.
    val current = when (status) {
        "pending", "requested", "reassigning", "no_partner_found", "reassign_failed" -> 0
        "accepted", "assigned" -> 1
        "partner_on_the_way" -> 2
        "arrived" -> 3
        "started" -> 4
        "completed" -> 5
        else -> 0
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        stages.forEachIndexed { i, stage ->
            val reached = i <= current
            val isCurrent = i == current && status !in setOf("completed")
            val dotColor = if (reached) VedaDropRose else Color.LightGray
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                // Rail: dot + connecting line down to the next stage.
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(22.dp).clip(CircleShape)
                            .background(if (isCurrent) VedaDropRose.copy(alpha = 0.18f) else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (reached) Icons.Default.CheckCircle else Icons.Default.Circle,
                            contentDescription = null,
                            tint = dotColor,
                            modifier = Modifier.size(if (isCurrent) 18.dp else 14.dp),
                        )
                    }
                    if (i < stages.lastIndex) {
                        Box(
                            modifier = Modifier.width(2.dp).weight(1f)
                                .background(if (i < current) VedaDropRose else Color.LightGray.copy(alpha = 0.5f)),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f).padding(bottom = if (i < stages.lastIndex) 12.dp else 0.dp)) {
                    Text(
                        stage.label,
                        fontWeight = if (isCurrent || reached) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isCurrent -> VedaDropRose
                            reached -> MaterialTheme.colorScheme.onBackground
                            else -> Color.Gray
                        },
                        fontSize = 14.sp,
                    )
                    val time = stage.timeKey?.let { timelineTimes[it] }
                    if (time != null) {
                        Text(time, fontSize = 11.sp, color = Color.Gray)
                    } else if (isCurrent) {
                        Text("In progress", fontSize = 11.sp, color = VedaDropRose.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
fun Tab(selected: Boolean, onClick: () -> Unit, text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clickable { onClick() }
            .background(if (selected) VedaDropRose.copy(alpha = 0.2f) else Color.Transparent)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) VedaDropRose else Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StateChip(status: String) {
    val color = when (status) {
        "pending" -> OrderOrange
        "accepted" -> VedaDropRose
        "partner_on_the_way" -> VedaDropRose
        "arrived" -> VedaDropRose
        "started" -> SuccessGreen
        "completed" -> SuccessGreen
        else -> Color.Gray
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, color),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status.replace("_", " ").uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// §728 (parity C1) — the booking states during which a job is LIVE (someone is en
// route / on-site), so a one-tap SOS is offered to BOTH parties on every such screen.
val SOS_ACTIVE_STATES = setOf("accepted", "assigned", "partner_on_the_way", "arrived", "started")

/**
 * §728 (parity C1) — prominent, one-tap SOS affordance for ACTIVE bookings, shown to
 * BOTH the customer and the partner. Visually distinct (filled RED). Taps open a
 * confirm dialog ("Send SOS alert?"), then fire the EXISTING [VedaDropViewModel.raiseSos]
 * (→ POST .../sos); the VM surfaces the "help is being notified / call 112" confirmation.
 */
@Composable
fun SosButton(viewModel: VedaDropViewModel, bookingId: String, modifier: Modifier = Modifier) {
    var confirm by remember(bookingId) { mutableStateOf(false) }
    val sosRed = Color(0xFFD32F2F)
    Button(
        onClick = { confirm = true },
        modifier = modifier.fillMaxWidth().testTag("sos_btn"),
        colors = ButtonDefaults.buttonColors(containerColor = sosRed, contentColor = Color.White),
        shape = RoundedCornerShape(8.dp),
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Text("SOS — Emergency help", fontWeight = FontWeight.Bold, color = Color.White,
            maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = sosRed) },
            title = { Text("Send SOS alert?") },
            text = { Text("This immediately alerts our safety team about this booking. If you are in danger, also call 112.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.raiseSos(bookingId); confirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = sosRed),
                ) { Text("Send SOS", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) } },
        )
    }
}

@Composable
fun ComplaintsListScreen(viewModel: VedaDropViewModel) {
    val complaints by viewModel.complaints.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshComplaints() }

    var ticketSubject by remember { mutableStateOf("") }
    var ticketDesc by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Help & Support Desk", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { viewModel.currentScreen = Screen.CustomerHome }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        
        // §710 — fill remaining space so the header stays fixed and the form/list each
        // scroll within the body (the form's "File Formal Ticket" button could be clipped
        // by the keyboard before; the list now lives in a properly-bounded LazyColumn).
        Column(modifier = Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Your Support Tickets", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Button(
                    onClick = { showForm = !showForm },
                    colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose)
                ) {
                    Text(if (showForm) "View Tickets" else "New Ticket", color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                }
            }
            
            if (showForm) {
              Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Open a Help Desk Ticket", fontWeight = FontWeight.Bold, color = VedaDropRose)
                        
                        OutlinedTextField(
                            value = ticketSubject,
                            onValueChange = { ticketSubject = it },
                            placeholder = { Text("Subject (e.g. Booking delay, wrong kit)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("complaint_sub_input")
                        )
                        OutlinedTextField(
                            value = ticketDesc,
                            onValueChange = { ticketDesc = it },
                            placeholder = { Text("Detailed issue description...") },
                            modifier = Modifier.fillMaxWidth().height(100.dp).testTag("complaint_desc_input")
                        )
                        Button(
                            onClick = {
                                if (ticketSubject.isNotBlank() && ticketDesc.isNotBlank()) {
                                    // No booking context on this standalone ticket form → pass null
                                    // so the complaint isn't mis-associated with a fake booking id.
                                    viewModel.submitComplaint(null, ticketSubject, ticketDesc)
                                    ticketSubject = ""
                                    ticketDesc = ""
                                    showForm = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("File Formal Ticket", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                        }
                    }
                }
              }
            } else {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (complaints.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Outlined.SupportAgent, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                                Text("No complaints recorded.", fontWeight = FontWeight.Bold)
                                Text("If you have hygiene or pricing concerns, please file a ticket immediately.", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                    items(complaints, key = { it.id }) { ticket ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                .clickable { viewModel.currentScreen = Screen.ComplaintDetail(ticket.id.toString()) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            ListItem(
                                headlineContent = { Text(ticket.subject, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text(ticket.message) },
                                trailingContent = {
                                    Card(colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f))) {
                                        Text(ticket.status, color = SuccessGreen, modifier = Modifier.padding(6.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- §704 RESCHEDULE DIALOG ----------------

/** §704 — reschedule a pending/accepted booking. Reuses the BookingConfirm date-
 *  stepper + slot-picker (loadSlots / availableSlots / selectedSlotId). On confirm
 *  calls rescheduleBooking(); server 409s surface via the friendly toast path. */
@Composable
fun RescheduleDialog(
    viewModel: VedaDropViewModel,
    booking: com.example.data.BookingEntity,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    val slots = viewModel.availableSlots
    val selectedSlotId = viewModel.selectedSlotId
    val selectedDate = viewModel.selectedBookingDate
    var placing by remember { mutableStateOf(false) }

    // Fresh slot list each time the dialog opens; clears any stale prior pick.
    LaunchedEffect(booking.id) {
        viewModel.selectedSlotId = null
        viewModel.loadSlots(booking.partnerId, booking.serviceId.ifBlank { null }, selectedDate)
    }

    val today = remember { java.time.LocalDate.now() }
    val dayFmt = remember { java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM", java.util.Locale.US) }

    AlertDialog(
        onDismissRequest = { if (!placing) onDismiss() },
        title = { Text("Reschedule appointment") },
        text = {
            Column {
                Text("Pick a new date and time. Allowed up to 3 hours before your slot.",
                    fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (offset in 0..6) {
                        val d = today.plusDays(offset.toLong())
                        val iso = d.toString()
                        FilterChip(
                            selected = iso == selectedDate,
                            onClick = { viewModel.loadSlots(booking.partnerId, booking.serviceId.ifBlank { null }, iso) },
                            label = { Text(d.format(dayFmt), fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VedaDropRose, selectedLabelColor = Color.White),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                when {
                    viewModel.slotsLoading -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = VedaDropRose)
                        Text("Loading available slots…", fontSize = 12.sp, color = Color.Gray)
                    }
                    slots.isEmpty() -> Text("No free slots that day — pick another date.",
                        fontSize = 12.sp, color = Color(0xFFEC7063), fontWeight = FontWeight.Bold)
                    else -> Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        slots.forEach { slot ->
                            val hourLabel = slot.start
                                ?.substringAfter("T", "")?.take(5)
                                ?.takeIf { it.length == 5 && it.contains(":") }
                                ?: slot.slotId.split(":").getOrNull(2)?.let { h ->
                                    h.toIntOrNull()?.let { String.format("%02d:00", it) } ?: h
                                } ?: slot.slotId
                            FilterChip(
                                selected = slot.slotId == selectedSlotId,
                                enabled = slot.available,
                                onClick = { viewModel.selectedSlotId = slot.slotId },
                                label = { Text(hourLabel, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VedaDropRose, selectedLabelColor = Color.White,
                                    disabledLabelColor = Color.Gray.copy(alpha = 0.5f)),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !placing && selectedSlotId != null,
                onClick = {
                    val sid = selectedSlotId ?: return@Button
                    placing = true
                    viewModel.rescheduleBooking(booking.id, sid) { err ->
                        placing = false
                        Toast.makeText(ctx, err ?: "Appointment rescheduled", Toast.LENGTH_SHORT).show()
                        if (err == null) onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                modifier = Modifier.testTag("confirm_reschedule_btn"),
            ) { Text(if (placing) "Rescheduling…" else "Confirm", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !placing) { Text("Cancel", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) } },
    )
}

// ---------------- §691 REASSIGNMENT HELPERS ----------------

// Lead windows mirror the backend gates (service.CUSTOMER_CHANGE_LEAD / PARTNER_
// TRANSFER_LEAD). Client-side gating only hides pointless actions — the server is
// authoritative and returns CHANGE_WINDOW_CLOSED / TRANSFER_WINDOW_CLOSED anyway.
const val CUSTOMER_CHANGE_LEAD_MS = 3L * 60 * 60 * 1000   // 3 hours
const val PARTNER_TRANSFER_LEAD_MS = 60L * 60 * 1000      // 1 hour

/** Parse an ISO-8601 UTC slot start ("2026-06-21T10:00:00Z") → epoch millis.
 *  Uses SimpleDateFormat (all API levels) to avoid java.time desugaring concerns. */
fun slotStartEpochMs(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    return try {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        fmt.timeZone = java.util.TimeZone.getTimeZone("UTC")
        fmt.parse(iso.substringBefore("Z").take(19))?.time
    } catch (e: Exception) {
        null
    }
}

/** True when now is still earlier than (slotStart − leadMs). Unknown slot → false
 *  (hide the action rather than offer something the server will reject). */
fun withinLeadWindow(iso: String?, leadMs: Long): Boolean {
    val start = slotStartEpochMs(iso) ?: return false
    return System.currentTimeMillis() < start - leadMs
}

@Composable
fun TransferBookingDialog(viewModel: VedaDropViewModel, bookingId: String, onDismiss: () -> Unit) {
    var targeted by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf("") }
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Transfer this booking") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Can't make it? Hand this job to another professional. The customer keeps the same price and is told their partner changed.",
                    fontSize = 13.sp, color = Color.Gray,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !targeted, onClick = { targeted = false })
                    Text("Open to all nearby professionals", fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = targeted, onClick = { targeted = true })
                    Text("Send to a specific partner (by code)", fontSize = 13.sp)
                }
                if (targeted) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        label = { Text("Partner code (e.g. K7Q9ZT)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "They get a 5-minute head start; after that it opens to everyone.",
                        fontSize = 11.sp, color = Color.Gray,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !viewModel.reassignmentBusy && (!targeted || code.isNotBlank()),
                onClick = {
                    viewModel.transferBooking(
                        bookingId,
                        mode = if (targeted) "targeted" else "broadcast",
                        targetPublicCode = if (targeted) code else null,
                    ) { err ->
                        Toast.makeText(context, err ?: "Booking offered to other professionals.", Toast.LENGTH_SHORT).show()
                        if (err == null) onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
            ) { Text("Transfer", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
        },
        dismissButton = { TextButton(onClick = { onDismiss() }) { Text("Cancel", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) } },
    )
}

@Composable
fun PartnerOffersScreen(viewModel: VedaDropViewModel) {
    val offers by viewModel.offers.collectAsState()
    val emptyMessage by viewModel.offersEmptyMessage.collectAsState()   // §710 #27
    val context = LocalContext.current

    // Foreground polling — a job's 5-min/30-min windows move fast, so refresh
    // every ~20s while the board is open (FCM push is a documented follow-up).
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.loadOffers()
            kotlinx.coroutines.delay(20_000)
        }
    }

    // §725 Batch-B — viewing the Rescue Board is the "partner has seen the urgent job"
    // signal: silence the high-alert alarm while it's on screen, and re-arm when leaving.
    DisposableEffect(Unit) {
        viewModel.onUrgentOffersViewed()
        onDispose { viewModel.onUrgentOffersLeft() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Open Jobs", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { viewModel.currentScreen = Screen.PartnerDashboard }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepPlum, titleContentColor = Color.White),
        )
        Text(
            "Jobs other professionals (or customers) put up for reassignment. First to accept wins — the price is fixed.",
            fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(16.dp, 8.dp),
        )
        if (offers.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                // §710 #27 — the server explains WHY (renew ₹99 / finish KYC / no jobs)
                // instead of a misleading "nothing to claim".
                Text(emptyMessage ?: "No jobs to claim right now",
                    fontWeight = FontWeight.Bold, color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp))
                if (emptyMessage == null) {
                    Text("Pull back later — open jobs appear here.", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(offers, key = { it.offerId }) { offer ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, VedaDropRose.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // §725 Batch-B — flag urgent jobs prominently (these are what
                            // ring the high-alert alarm). Claiming silences the alarm.
                            if (offer.isUrgentOffer()) {
                                Surface(color = Color(0xFFD32F2F).copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(8.dp, 3.dp),
                                    ) {
                                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "URGENT · customer needs someone now",
                                            color = Color(0xFFD32F2F), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                            if (offer.isTargetedToMeWindow) {
                                Surface(color = VedaDropRose.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        "EXCLUSIVE TO YOU · claim before it opens to all",
                                        color = VedaDropRose, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(8.dp, 3.dp),
                                    )
                                }
                            }
                            Text(offer.booking?.serviceName ?: "Beauty service", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Payout: ₹${offer.agreedTotalPaise / 100}", fontWeight = FontWeight.Bold, color = VedaDropRose)
                            val b = offer.booking
                            if (b?.slotStart != null) {
                                val dateStr = b.slotStart.substringBefore("T", b.slotStart)
                                val timeStr = b.slotStart.substringAfter("T", "").take(5)
                                val finalWhen = if (timeStr.isNotEmpty()) "$dateStr • $timeStr" else dateStr
                                Text("When: $finalWhen", fontSize = 12.sp, color = Color.Gray)
                            }
                            val place = listOfNotNull(b?.city, b?.pincode).joinToString(" · ")
                            if (place.isNotBlank()) Text("Area: $place", fontSize = 12.sp, color = Color.Gray)
                            // §722/§11 — show complete details (services + distance) so the
                            // partner makes an informed decision BEFORE claiming the job.
                            val detailBits = listOfNotNull(
                                b?.itemCount?.takeIf { it > 0 }?.let { "$it service${if (it > 1) "s" else ""}" },
                                b?.distanceKm?.let { "${"%.1f".format(it)} km away" },
                            ).joinToString(" · ")
                            if (detailBits.isNotBlank()) {
                                Text(detailBits, fontSize = 12.sp, color = VedaDropRose, fontWeight = FontWeight.Medium)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.acceptOffer(offer.offerId) { err ->
                                            Toast.makeText(context, err ?: "Job claimed — it's yours.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                ) { Text("Claim Job", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
                                OutlinedButton(
                                    onClick = { viewModel.declineOffer(offer.offerId) },
                                    modifier = Modifier.weight(1f),
                                ) { Text("Dismiss", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- PARTNER CABINET SCREENS ----------------

@Composable
fun PartnerDashboardScreen(viewModel: VedaDropViewModel) {
    val activeUser by viewModel.activeUser.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val scope = rememberCoroutineScope()
    
    val currentRoleKyc = activeUser?.kycStatus ?: "not_started"
    val ongoingJobs = bookings.filter { it.status != "completed" && it.status != "cancelled" && it.status != "rejected" }

    // §691 — keep the Rescue Board badge fresh while the dashboard is shown.
    LaunchedEffect(Unit) { viewModel.loadOffers() }

    // §708 — re-fetch the profile on entry so an admin KYC approval (or any
    // server-side status change) reflects immediately. Without this the dashboard
    // kept showing the login-time cached kyc_status, so an approved partner was
    // still nagged to "complete KYC" until the next re-login.
    LaunchedEffect(Unit) { viewModel.refreshProfile() }
    // §710 #26 — re-fetch subscription on entry so an admin grant/cancel reflects on the
    // dashboard banner without a re-login.
    LaunchedEffect(Unit) { viewModel.loadSubscription() }

    // §710 — re-fetch BOOKINGS on entry (+ poll). The dashboard refreshed profile/
    // offers/earnings but NOT bookings, so the in-memory list went stale and a job the
    // partner had accepted could vanish from the queue ("I accepted it and now it's
    // gone"). Now the accepted/ongoing jobs are always server-truth on the dashboard.
    LaunchedEffect(Unit) {
        while (isActive) {
            viewModel.refreshActiveBookings()
            delay(30_000)
        }
    }

    // In-app notifications: pull on entry, then poll every ~30s while on the
    // dashboard. Auto-cancels when the dashboard leaves composition.
    LaunchedEffect(Unit) {
        while (isActive) {
            viewModel.loadNotifications()
            delay(30_000)
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(DeepPlum, DarkSlate)))
                .padding(16.dp, 24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PARTNER DESK", fontWeight = FontWeight.Bold, color = VedaDropRose)
                        Text(activeUser?.name ?: "Provider", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    // §705 — explicit entry to the partner's own business/provider
                    // profile from the dashboard (founder: "provider page kholne ka
                    // koi button nahi hai"). The bottom-nav "Business" tab also goes
                    // here; this header icon makes it discoverable from the home.
                    IconButton(
                        onClick = { viewModel.currentScreen = Screen.PartnerProfile },
                        modifier = Modifier.testTag("dashboard_business_profile_btn"),
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Business profile", tint = Color.White)
                    }
                    NotificationBell(viewModel)
                }

                // §705 — "jobs come to me": the OPEN-JOBS POOL + today's numbers sit
                // right under the partner's name so the home reads as a WORK INBOX,
                // not a setup form. (The buried setup cards remain lower / in Business.)
                val poolOffers by viewModel.offers.collectAsState()
                val earningsNow by viewModel.earnings.collectAsState()
                LaunchedEffect(Unit) { viewModel.loadEarnings() }
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    onClick = { viewModel.currentScreen = Screen.PartnerOffers },
                    shape = RoundedCornerShape(14.dp),
                    color = VedaDropRose,
                    modifier = Modifier.fillMaxWidth().testTag("dashboard_open_pool_card"),
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (poolOffers.isNotEmpty()) "${poolOffers.size} open job(s) to claim" else "Open Jobs Pool",
                                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                if (poolOffers.isNotEmpty()) "First to accept wins — tap to claim" else "Nearby bookings appear here",
                                color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        "Today" to "₹${(earningsNow?.todayPaise ?: 0L) / 100}",
                        "Active" to "${ongoingJobs.size}",
                        "In pool" to "${poolOffers.size}",
                    ).forEachIndexed { i, pair ->
                        if (i > 0) Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(pair.second, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(pair.first, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // §701 — composite "why you're not visible" banner (only when not visible).
                val dashSub by viewModel.subscription.collectAsState()
                val kycApprovedNow = currentRoleKyc == "approved"
                val subActiveNow = dashSub?.isActive == true
                if (!kycApprovedNow || !subActiveNow) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = OrderOrange.copy(alpha = 0.14f)),
                        border = BorderStroke(1.dp, OrderOrange.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = OrderOrange, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (!kycApprovedNow)
                                    "You're not visible to customers yet: complete KYC & wait for admin approval."
                                else
                                    "Your ₹99/month listing has expired — renew to appear in search.",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // §709 — declutter: show the KYC card ONLY when action is needed.
                // Once approved it leaves the daily work inbox (the conditional
                // "not visible" banner above still surfaces a lapsed subscription).
                if (!kycApprovedNow) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // §701 — explicit, clear KYC state strings.
                        val kycLabel = when (currentRoleKyc) {
                            "approved" -> "KYC approved ✓"
                            "submitted", "under_review" -> "KYC submitted — pending admin approval"
                            "rejected" -> "KYC rejected"
                            else -> "Start KYC"
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = if (kycApprovedNow) SuccessGreen else OrderOrange)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                kycLabel,
                                fontWeight = FontWeight.Bold,
                                color = if (kycApprovedNow) SuccessGreen else OrderOrange
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (kycApprovedNow) {
                            Text("Your commercial KYC verify check is clear! You are active for real-time customer requests.", fontSize = 12.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                        } else {
                            if (currentRoleKyc == "rejected") {
                                val reason = viewModel.partnerKycReason ?: activeUser?.kycReason
                                if (!reason.isNullOrBlank()) {
                                    Text("Reason: $reason", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            } else if (currentRoleKyc == "submitted" || currentRoleKyc == "under_review") {
                                Text("KYC submitted — pending admin approval. We'll notify you once reviewed.", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(6.dp))
                            } else {
                                Text("Aadhaar/PAN KYC is required in order to legally receive job payout transfers.", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            // Hide the action button while a submission is under review.
                            if (currentRoleKyc != "submitted" && currentRoleKyc != "under_review") {
                                Button(
                                    onClick = { viewModel.currentScreen = Screen.PartnerKyc },
                                    colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                                    modifier = Modifier.fillMaxWidth().testTag("partner_kyc_trigger")
                                ) {
                                    Text(
                                        if (currentRoleKyc == "rejected") "Re-submit KYC" else "Start KYC",
                                        maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
                }   // §709 — close if (!kycApprovedNow)
            }
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Live Growth & Recharts-style Analytics Widget (Volume & Payout trends)
            MonthlyGrowthLineChart(bookings = bookings)

            // AVAILABILITY ENGINE & MICRO-SALON CONTROL PANEL
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, VedaDropRose.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "AVAILABILITY CONTROL ENGINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = VedaDropRose,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                if (viewModel.isPartnerActive) "Active Status: ONLINE" else "Active Status: AWAY",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (viewModel.isPartnerActive) SuccessGreen else Color.Red
                            )
                        }
                        Switch(
                            checked = viewModel.isPartnerActive,
                            onCheckedChange = { viewModel.setPartnerActive(it) },
                            modifier = Modifier.testTag("partner_availability_toggle")
                        )
                    }
                    
                    if (viewModel.isPartnerActive) {
                        Text(
                            text = "🟢 You are visible to nearby customers and open for instant home booking requests.",
                            fontSize = 11.sp,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Surface(
                            color = Color.Red.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.Red,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AWAY MODE: You will not receive any discovery listings or job alerts.",
                                    fontSize = 10.sp,
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Divider(color = Color.Gray.copy(alpha = 0.15f))

                    // §709 — coverage radius + shift hours are one-time SETUP, not
                    // daily work: tuck them behind a collapsible toggle so the
                    // dashboard reads as a work inbox. Collapsed by default; the
                    // online/away toggle above (daily-operational) stays visible.
                    var showCoverageSetup by remember { mutableStateOf(false) }
                    TextButton(
                        onClick = { showCoverageSetup = !showCoverageSetup },
                        colors = ButtonDefaults.textButtonColors(contentColor = VedaDropRose),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (showCoverageSetup) "Hide coverage & hours ▴" else "Coverage & working hours ▾",
                            maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false
                        )
                    }
                    if (showCoverageSetup) {
                    // Service Radius Control Slider
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Service Bounds Radius", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                text = "${viewModel.partnerServiceRadiusKm.toInt()} km max",
                                fontWeight = FontWeight.Bold,
                                color = VedaDropRose
                            )
                        }
                        Slider(
                            value = viewModel.partnerServiceRadiusKm.toFloat(),
                            onValueChange = { viewModel.partnerServiceRadiusKm = it.toDouble() },
                            onValueChangeFinished = { viewModel.savePartnerRadius(viewModel.partnerServiceRadiusKm) },
                            valueRange = 1f..10f,   // §714 radius-clamp-echo-3 — match the 10km server cap (was 1..30 → false coverage)
                            colors = SliderDefaults.colors(
                                thumbColor = VedaDropRose,
                                activeTrackColor = VedaDropRose,
                                inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        )
                    }
                    
                    Divider(color = Color.Gray.copy(alpha = 0.15f))
                    
                    // Operating custom shift hours
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Operating Shift Hours", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                text = viewModel.partnerWorkingHoursRange,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        
                        var showHoursPicker by remember { mutableStateOf(false) }
                        TextButton(
                            onClick = { showHoursPicker = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = VedaDropRose)
                        ) {
                            Text("Configure", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                        }
                        
                        if (showHoursPicker) {
                            AlertDialog(
                                onDismissRequest = { showHoursPicker = false },
                                title = { Text("Select Working Hours Grid") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        // §714 pda-working-hours-clamp-1 — only offer windows
                                        // inside the 7 AM–6 PM platform window. The old >6 PM
                                        // presets (Extended/Late/Late Night) were silently
                                        // clamped to 6 PM, so a partner believed in coverage
                                        // she never had.
                                        listOf(
                                            "7:00 AM - 4:00 PM (Early)",
                                            "8:00 AM - 5:00 PM (Day)",
                                            "9:00 AM - 6:00 PM (Standard)",
                                            "10:00 AM - 6:00 PM (Late start)"
                                        ).forEach { shift ->
                                            Button(
                                                onClick = {
                                                    viewModel.savePartnerWorkingHours(shift)
                                                    showHoursPicker = false
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (viewModel.partnerWorkingHoursRange == shift) VedaDropRose else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (viewModel.partnerWorkingHoursRange == shift) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            ) {
                                                Text(shift, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showHoursPicker = false }) {
                                        Text("Close", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                    }
                                }
                            )
                        }
                    }
                    }   // §709 — close if (showCoverageSetup)
                }
            }

            // VEDA DROP PREMIUM LISTING & PARTNER SUBSCRIPTION CHECKER
            val subState by viewModel.subscription.collectAsState()
            val subIsActive = subState?.isActive == true
            val subPeriodEnd = subState?.currentPeriodEnd?.take(10) ?: "Not set"
            val subStatusLabel = subState?.status ?: "trial"
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().testTag("vedadrop_subscription_card"),
                border = BorderStroke(1.dp, if (subIsActive) SuccessGreen.copy(alpha = 0.25f) else VedaDropRose.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "VEDA DROP PREMIUM SUBSCRIPTION",
                                style = MaterialTheme.typography.labelSmall,
                                color = VedaDropRose,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(if (subIsActive) SuccessGreen else Color.Red, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (subIsActive) "Listing: ACTIVE" else "Listing: INACTIVE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (subIsActive) SuccessGreen else Color.Red
                                )
                            }
                        }
                        
                        AssistChip(
                            onClick = { viewModel.currentScreen = Screen.PartnerSubscription },
                            label = { Text("₹99/month Tier", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = VedaDropRose.copy(alpha = 0.15f))
                        )
                    }
                    
                    Text(
                        text = "Your beauty services are active and searchable on the Veda Drop marketplace. The subscription allows unlimited incoming booking reservations with zero commission.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 15.sp
                    )
                    
                    if (subIsActive) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Current Period End:", fontSize = 11.sp, color = Color.Gray)
                            Text(subPeriodEnd, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                        }
                    } else {
                        Surface(
                            color = VedaDropRose.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ Non-subscribers listings may be hidden in the user marketplace feeds. Activate your ₹99/month monthly tier card to resume bookings.",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = VedaDropRose,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    
                    Button(
                        onClick = { viewModel.currentScreen = Screen.PartnerSubscription },
                        colors = ButtonDefaults.buttonColors(containerColor = if (subIsActive) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else VedaDropRose),
                        modifier = Modifier.fillMaxWidth().testTag("vedadrop_subscription_btn")
                    ) {
                        Text(
                            text = if (subIsActive) "Manage Plan" else "Subscribe ₹99/mo",
                            color = if (subIsActive) Color.White else Color.Black,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }
            }

            // SALON/STUDIO REGISTRATION & SERVICE CATALOG MANAGER
            var expandRegistrationForm by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, VedaDropRose.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Spa, contentDescription = null, tint = VedaDropRose, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "SALON / STUDIO SETUP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VedaDropRose,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    "Registration & Service Menu",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }
                        IconButton(onClick = { expandRegistrationForm = !expandRegistrationForm }) {
                            Icon(
                                imageVector = if (expandRegistrationForm) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand registration details",
                                tint = Color.White
                            )
                        }
                    }
                    
                    Text(
                        "Manage your official beauty salon/studio branding, years of experience, and customize catalog services shown to nearby customers on Veda Drop marketplace.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 15.sp
                    )
                    
                    if (expandRegistrationForm) {
                        Divider(color = Color.Gray.copy(alpha = 0.12f))
                        
                        // Register Salon/Studio inputs
                        Text(
                            "1. STUDIO PROFILE BRANDING",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VedaDropRose,
                            letterSpacing = 1.sp
                        )
                        
                        var studioNameInput by remember { mutableStateOf(activeUser?.name ?: "") }
                        var studioBioInput by remember { mutableStateOf(activeUser?.partnerBio ?: "") }
                        var studioExpInput by remember { mutableStateOf(activeUser?.partnerExperience?.toString() ?: "0") }
                        
                        LaunchedEffect(activeUser) {
                            activeUser?.let {
                                studioNameInput = it.name
                                studioBioInput = it.partnerBio
                                studioExpInput = it.partnerExperience.toString()
                            }
                        }
                        
                        OutlinedTextField(
                            value = studioNameInput,
                            onValueChange = { studioNameInput = it },
                            label = { Text("Salon / Studio Brand Name") },
                            placeholder = { Text("e.g. Simran's Bridal Lounge") },
                            modifier = Modifier.fillMaxWidth().testTag("salon_brand_name_input"),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = studioBioInput,
                            onValueChange = { studioBioInput = it },
                            label = { Text("About your Studio & Specialties") },
                            placeholder = { Text("e.g. Specialists in deluxe organic glow facials, celebrity makeup, and stress relief massages.") },
                            modifier = Modifier.fillMaxWidth().testTag("salon_bio_input"),
                            maxLines = 3
                        )
                        
                        OutlinedTextField(
                            value = studioExpInput,
                            onValueChange = { studioExpInput = it },
                            label = { Text("Years of Professional Experience") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("salon_exp_input"),
                            singleLine = true
                        )
                        
                        Button(
                            onClick = {
                                if (studioNameInput.isNotBlank()) {
                                    viewModel.updateProfile(
                                        name = studioNameInput,
                                        email = activeUser?.email ?: "",
                                        bio = studioBioInput,
                                        experience = studioExpInput.toIntOrNull() ?: 0
                                    )
                                    viewModel.notify("Salon & Studio registry updated successfully!")
                                } else {
                                    viewModel.notify("Please fill out a valid Salon / Studio Brand Name.", isError = true)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                            modifier = Modifier.fillMaxWidth().testTag("salon_branding_save_btn")
                        ) {
                            Text("Save Studio Details", fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Divider(color = Color.Gray.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Add service
                        Text(
                            "2. SERVICE DICTIONARY ADDER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VedaDropRose,
                            letterSpacing = 1.sp
                        )
                        
                        var customSvcName by remember { mutableStateOf("") }
                        var customSvcCategory by remember { mutableStateOf("Salon") }
                        var customSvcPrice by remember { mutableStateOf("") }
                        var customSvcDuration by remember { mutableStateOf("45") }
                        var customSvcDesc by remember { mutableStateOf("") }
                        var customSvcProducts by remember { mutableStateOf("") }
                        
                        OutlinedTextField(
                            value = customSvcName,
                            onValueChange = { customSvcName = it },
                            label = { Text("Beauty Treatment / Service Name") },
                            placeholder = { Text("e.g. Veda Drop Ultra Glow Facial") },
                            modifier = Modifier.fillMaxWidth().testTag("custom_svc_name"),
                            singleLine = true
                        )
                        
                        Text("Category Tag:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Salon", "Beauty", "Makeup", "Massage").forEach { cat ->
                                val isSelected = customSvcCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { customSvcCategory = cat },
                                    label = { Text(cat) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VedaDropRose.copy(alpha = 0.2f),
                                        selectedLabelColor = VedaDropRose
                                    )
                                )
                            }
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customSvcPrice,
                                onValueChange = { customSvcPrice = it.filter { c -> c.isDigit() }.take(7) },
                                label = { Text("Price (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("custom_svc_price"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = customSvcDuration,
                                onValueChange = { customSvcDuration = it.filter { c -> c.isDigit() }.take(3) },
                                label = { Text("Duration (mins)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f).testTag("custom_svc_duration"),
                                singleLine = true
                            )
                        }
                        
                        OutlinedTextField(
                            value = customSvcDesc,
                            onValueChange = { customSvcDesc = it },
                            label = { Text("Menu Description") },
                            placeholder = { Text("Briefly describe steps of facial, massage, haircut etc.") },
                            modifier = Modifier.fillMaxWidth().testTag("custom_svc_desc")
                        )
                        
                        OutlinedTextField(
                            value = customSvcProducts,
                            onValueChange = { customSvcProducts = it },
                            label = { Text("Supplies & Premium Products Used") },
                            placeholder = { Text("e.g. Lotus / Biotique pack, opened brand new.") },
                            modifier = Modifier.fillMaxWidth().testTag("custom_svc_products")
                        )
                        
                        Button(
                            onClick = {
                                val priceVal = customSvcPrice.toLongOrNull() ?: 0L
                                val durationVal = customSvcDuration.toIntOrNull() ?: 45
                                if (customSvcName.isNotBlank() && priceVal > 0) {
                                    viewModel.createCustomPartnerService(
                                        name = customSvcName,
                                        categoryName = customSvcCategory,
                                        pricePaise = priceVal * 100L,
                                        durationMin = durationVal,
                                        description = customSvcDesc.ifBlank { "Professional $customSvcName treatment." },
                                        productsUsed = customSvcProducts.ifBlank { "Professional double-safety verified kit." }
                                    )
                                    customSvcName = ""
                                    customSvcPrice = ""
                                    customSvcDesc = ""
                                    customSvcProducts = ""
                                    viewModel.notify("New service listed successfully!")
                                } else {
                                    viewModel.notify("Please fill valid service name and rupee price", isError = true)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                            modifier = Modifier.fillMaxWidth().testTag("custom_svc_add_btn")
                        ) {
                            Text("Add Service", fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Button(
                            onClick = { viewModel.currentScreen = Screen.PartnerServices },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth().testTag("standard_services_btn")
                        ) {
                            Text("Manage Catalog", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                        }
                    }
                }
            }

            // Quick actions — earnings, analytics, availability, portfolio.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { viewModel.currentScreen = Screen.PartnerEarnings },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, VedaDropRose),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VedaDropRose),
                ) { Text("Earnings", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
                OutlinedButton(
                    onClick = { viewModel.currentScreen = Screen.PartnerAnalytics },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, VedaDropRose),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VedaDropRose),
                ) { Text("Analytics", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { viewModel.currentScreen = Screen.PartnerAvailability },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, VedaDropRose),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VedaDropRose),
                ) { Text("Availability", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
                OutlinedButton(
                    onClick = { viewModel.currentScreen = Screen.PartnerPortfolio },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, VedaDropRose),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VedaDropRose),
                ) { Text("Portfolio", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
            }

            // §691 — Rescue Board: jobs other partners (or customers) put up for
            // reassignment that this partner can claim (first-to-accept-wins).
            val openOffers by viewModel.offers.collectAsState()
            Button(
                onClick = { viewModel.currentScreen = Screen.PartnerOffers },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (openOffers.isNotEmpty()) "Rescue Board — ${openOffers.size} job(s) to claim"
                    else "Rescue Board — claim nearby jobs",
                    color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false,
                )
            }

            Text("JOB REQUEST QUEUE", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = VedaDropRose)
            if (currentRoleKyc != "approved") {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Text("KYC Verification Required", fontWeight = FontWeight.Bold)
                        Text("Your profile must be approved before you can accept beauty job requests from nearby customers.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            } else {
                if (ongoingJobs.isEmpty()) {
                    Card {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.DirectionsBike, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                            Text("No job requests currently active", fontWeight = FontWeight.Bold)
                            Text("Ensure GPS signals are active to receive instant salon appointments", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                } else {
                    ongoingJobs.forEach { job ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("CUSTOMER APPOINTMENT", color = VedaDropRose, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(job.serviceName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Total Payout: ₹${job.totalPaise / 100}", fontWeight = FontWeight.Bold, color = VedaDropRose)
                                        // §701 — hide precise address until the partner accepts.
                                        val addressRevealed = job.status !in setOf("pending", "reassigning")
                                        // §722 req-1 — show the customer's name once accepted (the
                                        // server reveals it only then); pre-accept it stays hidden.
                                        if (addressRevealed && job.counterpartyName.isNotBlank()) {
                                            Text("Customer: ${job.counterpartyName}", fontSize = 12.sp, color = Color.Gray)
                                        }
                                        if (addressRevealed) {
                                            Text("Address: ${job.addressText}", fontSize = 12.sp, color = Color.Gray)
                                        } else {
                                            val area = listOf(job.city, job.pincode).filter { it.isNotBlank() }.joinToString(" • ")
                                            Text(
                                                if (area.isNotBlank()) "Area: $area" else "Address revealed after you accept",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                            Text("Full address revealed after you accept", fontSize = 10.sp, color = Color.Gray.copy(alpha = 0.7f))
                                        }
                                        // §722 req-2 — distance from the partner to the customer.
                                        job.distanceKm?.let { d ->
                                            Text("${"%.1f".format(d)} km away", fontSize = 12.sp, color = VedaDropRose, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                                
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                
                                Text("JOB TIMELINE STEP: ${job.status.replace("_", " ").uppercase()}", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (job.status == "pending") {
                                        Button(
                                            onClick = { viewModel.acceptJob(job.id) },
                                            modifier = Modifier.weight(1f).testTag("accept_job_${job.id}"),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                        ) {
                                            Text("Accept Day Appointment")
                                        }
                                        Button(
                                            onClick = { viewModel.rejectJob(job.id) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                        ) {
                                            Text("Decline")
                                        }
                                    } else if (job.status == "accepted") {
                                        Button(
                                            onClick = { viewModel.startTravelToJob(job.id) },
                                            modifier = Modifier.fillMaxWidth().testTag("commence_travel_${job.id}"),
                                            colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose)
                                        ) {
                                            Text("Commence Doorstep Journey")
                                        }
                                    } else if (job.status == "partner_on_the_way") {
                                        Button(
                                            onClick = { viewModel.arriveAtJob(job.id) },
                                            modifier = Modifier.fillMaxWidth().testTag("arrive_location_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                        ) {
                                            Text("Mark Arrived at Residence", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                        }
                                    } else if (job.status == "arrived") {
                                        // §728 (parity C1) — capture a live arrival selfie
                                        // before starting (sent with the OTP).
                                        Column(modifier = Modifier.weight(1f)) {
                                            StartJobWithSelfie(
                                                viewModel = viewModel,
                                                bookingId = job.id,
                                                fieldTag = "verify_otp_field",
                                                startTag = "verify_otp_start_btn",
                                                startLabel = "Verify & Start Deep Cleansing",
                                                startContainerColor = VedaDropRose,
                                                startContentColor = Color.Black,
                                            )
                                        }
                                    } else if (job.status == "started") {
                                        Button(
                                            onClick = { viewModel.completePartnerJob(job.id) },
                                            modifier = Modifier.fillMaxWidth().testTag("complete_service_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                        ) {
                                            Text("Mark Job Completed & Sanitized", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                        }
                                    }
                                    
                                    Button(
                                        onClick = { viewModel.currentScreen = Screen.BookingDetail(job.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                                    ) {
                                        Text("Chat Logistics", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                    }
                                }

                                // §691 — emergency transfer: hand this job to a
                                // colleague (or open it to all) up to 1h before the
                                // slot. Hidden once inside the 1h window.
                                if ((job.status == "accepted" || job.status == "assigned") &&
                                    withinLeadWindow(job.slotStartIso, PARTNER_TRANSFER_LEAD_MS)) {
                                    var showTransfer by remember(job.id) { mutableStateOf(false) }
                                    OutlinedButton(
                                        onClick = { showTransfer = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        border = BorderStroke(1.dp, OrderOrange),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OrderOrange),
                                    ) {
                                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Transfer booking (emergency)", fontSize = 13.sp)
                                    }
                                    if (showTransfer) {
                                        TransferBookingDialog(
                                            viewModel = viewModel,
                                            bookingId = job.id,
                                            onDismiss = { showTransfer = false },
                                        )
                                    }
                                }

                                // §728 (parity C1) — one-tap SOS on the partner's active
                                // job card (she is en route / on-site). Prominent red.
                                if (job.status in SOS_ACTIVE_STATES) {
                                    SosButton(viewModel = viewModel, bookingId = job.id)
                                }
                            }
                        }
                    }
                }
            }

            // PRE-BOOKING CUSTOMER CHATS SECTION
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PRE-BOOKING INQUIRIES", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = VedaDropRose)
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Seal pack discussion active",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            val preInquiries by viewModel.preBookingInquiries.collectAsState()
            val groupedInquiries = remember(preInquiries) {
                preInquiries.groupBy { it.bookingId }
            }
            
            if (groupedInquiries.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No active pre-booking customer chats", fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 13.sp)
                        Text("Clients comparing your quotes will reach out here to ask about packaging brand and visual seal safety details.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    groupedInquiries.forEach { (bookingId, msgs) ->
                        val lastMsg = msgs.firstOrNull() ?: return@forEach
                        val parts = bookingId.split("_")
                        val partnerId = parts.getOrNull(1) ?: ""
                        val serviceId = parts.getOrNull(2) ?: ""
                        
                        val service = VedaDropDataSource.services.firstOrNull { it.id == serviceId } ?: return@forEach
                        val partner = VedaDropDataSource.partners.firstOrNull { it.id == partnerId } ?: Partner(
                            id = partnerId.ifBlank { "0" },
                            name = "Customer enquiry",
                            avatarUrl = "",
                            rating = 0f, reviewsCount = 0, distanceKm = 0.0, etaMin = 0, experienceYears = 0,
                            description = "", categories = emptyList(), servicesOffered = listOf(serviceId),
                            portfolioUrls = emptyList(), recentReviews = emptyList()
                        )
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, VedaDropRose.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("INCOMING CUSTOMER DISCUSSION", color = VedaDropRose, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
                                        Text(service.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                    Surface(color = SuccessGreen.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                        Text("Pre-Booking Verify", color = SuccessGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp, 2.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "“${lastMsg.text}”",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        viewModel.currentScreen = Screen.PreBookingChat(service, partner)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose)
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Chat", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * §706 — compress a captured Bitmap into a base64 JPEG data URL.
 * Scales the longest side down to <=1024px, JPEG quality 60, no line-wrap.
 * Returns a "data:image/jpeg;base64,…" string the backend stores as-is.
 */
private fun Bitmap.toJpegDataUrl(maxSide: Int = 1024, quality: Int = 60): String {
    val longest = maxOf(width, height)
    val scaled = if (longest > maxSide) {
        val ratio = maxSide.toFloat() / longest.toFloat()
        Bitmap.createScaledBitmap(this, (width * ratio).toInt().coerceAtLeast(1), (height * ratio).toInt().coerceAtLeast(1), true)
    } else this
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
    val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    return "data:image/jpeg;base64,$b64"
}

/**
 * §728 (parity C1) — render a start-selfie proof image. Coil 2.x has NO `data:`
 * fetcher (see §725/§726), so a base64 "data:" URL must be decoded to a Bitmap and
 * drawn with [Image]; a resolved http(s) URL still goes through Coil's [AsyncImage].
 */
@Composable
fun SelfieProofImage(url: String, modifier: Modifier = Modifier) {
    if (url.startsWith("data:")) {
        val bmp = remember(url) {
            runCatching {
                val b64 = url.substringAfter(",", "")
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Professional's start selfie",
                contentScale = ContentScale.Crop,
                modifier = modifier,
            )
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = "Professional's start selfie",
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}

/**
 * §728 (parity C1) — SELFIE-AT-START control for the partner's job-start flow, used
 * at BOTH start sites (booking detail + dashboard job card). The partner enters the
 * customer's start-OTP, taps "Verify & start", which FIRST captures a live front-cam
 * selfie (face-detected) → shows the thumbnail to confirm → only then starts the job
 * (OTP + selfie sent together). Cancelling the capture aborts the start.
 *
 * Encapsulates camera-permission handling and the [SelfieCaptureFlow] overlay so the
 * call sites stay a single composable. [startTag]/[fieldTag] keep the existing test
 * tags stable per site.
 */
@Composable
fun StartJobWithSelfie(
    viewModel: VedaDropViewModel,
    bookingId: String,
    fieldTag: String,
    startTag: String,
    startLabel: String,
    startContainerColor: Color,
    startContentColor: Color = Color.White,
) {
    val context = LocalContext.current
    var otpInput by remember(bookingId) { mutableStateOf("") }
    var showCapture by remember(bookingId) { mutableStateOf(false) }
    var selfie by remember(bookingId) { mutableStateOf<Bitmap?>(null) }

    // Ask for camera permission, then open the capture overlay.
    val camPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showCapture = true
        else Toast.makeText(context, "Camera permission is required to start the job", Toast.LENGTH_SHORT).show()
    }
    fun launchCapture() {
        val has = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (has) showCapture = true else camPermLauncher.launch(android.Manifest.permission.CAMERA)
    }

    // Live capture overlay → store the bitmap; cancel aborts (no start).
    if (showCapture) {
        SelfieCaptureFlow(
            onCapture = { bmp -> selfie = bmp; showCapture = false },
            onCancel = { showCapture = false },
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = otpInput,
            onValueChange = { otpInput = it },
            placeholder = { Text("Enter the customer's start code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag(fieldTag),
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Once a selfie is captured, show the thumbnail + a retake option before the
        // partner confirms the start.
        val captured = selfie
        if (captured != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    bitmap = captured.asImageBitmap(),
                    contentDescription = "Captured start selfie",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).testTag("start_selfie_thumb"),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Selfie ready", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuccessGreen)
                    Text("Confirm to start the service.", fontSize = 11.sp, color = Color.Gray)
                }
                TextButton(onClick = { selfie = null; launchCapture() }) {
                    Text("Retake", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.startJob(bookingId, otpInput, captured.toJpegDataUrl())
                    selfie = null
                },
                enabled = otpInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth().testTag(startTag),
                colors = ButtonDefaults.buttonColors(containerColor = startContainerColor, contentColor = startContentColor),
            ) { Text(startLabel, color = startContentColor, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
        } else {
            // No selfie yet — the start button captures one first.
            Button(
                onClick = { launchCapture() },
                enabled = otpInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth().testTag(startTag),
                colors = ButtonDefaults.buttonColors(containerColor = startContainerColor, contentColor = startContentColor),
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp), tint = startContentColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(startLabel, color = startContentColor, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
            }
            Text("A quick live selfie confirms your arrival before the job starts.",
                fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun PartnerKycScreen(viewModel: VedaDropViewModel) {
    val activeUser by viewModel.activeUser.collectAsState()
    // §708 — load the saved KYC (reason + prefill) AND refresh the profile so the
    // current kyc_status is authoritative (admin approvals show without re-login).
    LaunchedEffect(Unit) { viewModel.loadPartnerKyc(); viewModel.refreshProfile() }

    val kycStatus = activeUser?.kycStatus ?: "not_started"
    // §725 — flips true the moment THIS session submits, so the form closes
    // immediately (before the profile re-fetch flips kycStatus to "submitted").
    var submittedNow by remember { mutableStateOf(false) }
    val pending = submittedNow || kycStatus == "submitted" || kycStatus == "under_review"
    val approved = kycStatus == "approved"

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Legal KYC Verification", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { viewModel.currentScreen = Screen.PartnerDashboard }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        when {
            // §725 — once approved, the editable form is GONE; show the verified card.
            approved -> KycApprovedView(viewModel)
            // §725 — while pending, the form is CLOSED (no re-submit) + the 24–48h note.
            pending -> KycPendingView(viewModel)
            else -> KycForm(viewModel, onSubmitted = { submittedNow = true })
        }
    }
}

/** §725 — KYC approved: a verified card (the Verified ✓ badge lives on the profile). */
@Composable
private fun KycApprovedView(viewModel: VedaDropViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Icon(Icons.Default.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(64.dp))
        Text("You're verified ✓", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Card(colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.12f))) {
            Text(
                "Aapki KYC approved ho chuki hai — aap ab ek verified provider hain. " +
                    "Aapki profile par Verified badge dikh raha hai.",
                color = SuccessGreen, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                modifier = Modifier.padding(16.dp),
            )
        }
        Button(
            onClick = { viewModel.currentScreen = Screen.PartnerDashboard },
            colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
        ) { Text("Back to Dashboard", maxLines = 1, softWrap = false) }
    }
}

/** §725 — KYC submitted: form closed, "24–48h" message, re-submit blocked. */
@Composable
private fun KycPendingView(viewModel: VedaDropViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Icon(Icons.Default.Schedule, contentDescription = null, tint = VedaDropGold, modifier = Modifier.size(64.dp))
        Text("KYC submitted", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Card(colors = CardDefaults.cardColors(containerColor = VedaDropGold.copy(alpha = 0.14f))) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Aapki details review ke liye bhej di gayi hain.",
                    color = VedaDropGold, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                )
                Text(
                    "Verification mein 24–48 ghante lag sakte hain. Approve hone par aapko notify " +
                        "kiya jayega aur aapki profile par ✓ Verified badge aa jayega.",
                    color = VedaDropGold, fontSize = 13.sp,
                )
            }
        }
        Text(
            "Review ke dauraan aap dobara KYC submit nahi kar sakte.",
            color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center,
        )
        Button(
            onClick = { viewModel.currentScreen = Screen.PartnerDashboard },
            colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
        ) { Text("Back to Dashboard", maxLines = 1, softWrap = false) }
    }
}

/** §725 — the editable KYC form (only shown when status is not_started / rejected). */
@Composable
private fun KycForm(viewModel: VedaDropViewModel, onSubmitted: () -> Unit) {
    val context = LocalContext.current
    var aadhaar by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var legalName by remember { mutableStateOf("") }   // §704 — name on the ID
    var submitting by remember { mutableStateOf(false) }

    // §725 — three guided face photos (front/left/right) + an ID-document photo.
    var faceFront by remember { mutableStateOf<Bitmap?>(null) }
    var faceLeft by remember { mutableStateOf<Bitmap?>(null) }
    var faceRight by remember { mutableStateOf<Bitmap?>(null) }
    var documentBmp by remember { mutableStateOf<Bitmap?>(null) }
    var showFaceScan by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    // §713/§725 — required business location, set via the shared ride-app picker overlay.
    var kycLat by remember { mutableStateOf<Double?>(null) }
    var kycLon by remember { mutableStateOf<Double?>(null) }
    var kycAddress by remember { mutableStateOf("") }
    var showLocPicker by remember { mutableStateOf(false) }

    val activeUser by viewModel.activeUser.collectAsState()
    // §708 — pre-fill from the last saved submission (e.g. after a rejection) so the
    // form never looks "unsaved". Only fills empty fields (won't stomp live typing).
    LaunchedEffect(viewModel.kycPrefill) {
        viewModel.kycPrefill?.let { f ->
            if (aadhaar.isBlank()) f.aadhaarNo?.let { aadhaar = it.filter { c -> c.isDigit() }.take(12) }
            if (pan.isBlank()) f.panNo?.let { pan = it.uppercase().take(10) }
            if (legalName.isBlank()) f.legalName?.let { legalName = it.take(120) }
        }
    }
    val rejectionReason = viewModel.partnerKycReason ?: activeUser?.kycReason

    // PAN [A-Z]{5}[0-9]{4}[A-Z]; Aadhaar exactly 12 digits.
    val panPattern = remember { Regex("^[A-Z]{5}[0-9]{4}[A-Z]$") }
    val aadhaarDigits = aadhaar.filter { it.isDigit() }
    val panUpper = pan.uppercase()
    val aadhaarValid = aadhaarDigits.length == 12
    val panValid = panPattern.matches(panUpper)
    val aadhaarError = aadhaar.isNotBlank() && !aadhaarValid
    val panError = pan.isNotBlank() && !panValid

    // §725 — camera permission, then launch the guided face scan (CameraX + ML Kit).
    val faceCamPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) { permissionDenied = false; showFaceScan = true } else permissionDenied = true }
    fun startFaceScan() {
        val has = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (has) { permissionDenied = false; showFaceScan = true }
        else faceCamPermLauncher.launch(android.Manifest.permission.CAMERA)
    }

    // §706 — ID-document capture (single still via the lightweight preview contract).
    val docLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? -> if (bmp != null) documentBmp = bmp }
    val docPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) { permissionDenied = false; docLauncher.launch(null) } else permissionDenied = true }
    fun captureDocument() {
        val has = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (has) docLauncher.launch(null) else docPermLauncher.launch(android.Manifest.permission.CAMERA)
    }

    val facesReady = faceFront != null && faceLeft != null && faceRight != null
    val photosReady = facesReady && documentBmp != null
    // §713 — a business location is mandatory (backend enforces it with geofencing on).
    val locationReady = kycLat != null && kycLon != null
    val canSubmit = aadhaarValid && panValid && legalName.isNotBlank() && photosReady && locationReady && !submitting

    // §725 — full-screen overlays for the guided face scan + the location picker.
    if (showFaceScan) {
        FaceCaptureFlow(
            onComplete = { f, l, r -> faceFront = f; faceLeft = l; faceRight = r; showFaceScan = false },
            onCancel = { showFaceScan = false },
        )
    }
    if (showLocPicker) {
        LocationSearchOverlay(
            viewModel = viewModel,
            title = "Set business location",
            onDismiss = { showLocPicker = false },
        ) { p -> kycLat = p.lat; kycLon = p.lon; kycAddress = p.address }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Complete this legal form to get registered as an authorized service provider.", color = Color.Gray, fontSize = 13.sp)

        if (!rejectionReason.isNullOrBlank()) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Previous submission rejected", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(rejectionReason, color = Color.Red, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Please correct and resubmit.", color = Color.Gray, fontSize = 11.sp)
                }
            }
        }

        // §704 — the name EXACTLY as printed on the ID. The admin verifies it and
        // your display name is locked to it, so customers can trust who's at the door.
        OutlinedTextField(
            value = legalName,
            onValueChange = { legalName = it.trimStart().take(120) },
            placeholder = { Text("Full name as on your ID") },
            singleLine = true,
            supportingText = { Text("This becomes your verified name once approved.", color = Color.Gray, fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth().testTag("legal_name_field")
        )

        OutlinedTextField(
            value = aadhaar,
            onValueChange = { aadhaar = it.filter { c -> c.isDigit() }.take(12) },
            placeholder = { Text("Aadhaar Number (12 Digits)") },
            singleLine = true,
            isError = aadhaarError,
            supportingText = { if (aadhaarError) Text("Aadhaar must be exactly 12 digits", color = Color.Red) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("aadhaar_no_field")
        )
        OutlinedTextField(
            value = pan,
            onValueChange = { pan = it.uppercase().take(10) },
            placeholder = { Text("PAN Card Number (e.g. ABCDE1234F)") },
            singleLine = true,
            isError = panError,
            supportingText = { if (panError) Text("Invalid PAN format (ABCDE1234F)", color = Color.Red) },
            modifier = Modifier.fillMaxWidth().testTag("pan_no_field")
        )

        // §725 — guided 3-photo face verification (front / left / right).
        Text("Face verification", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            "We'll guide you through 3 quick photos — front, left and right. Move your head as " +
                "prompted; the camera detects each pose and captures automatically.",
            color = Color.Gray, fontSize = 11.sp
        )
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (facesReady) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FaceThumb("Front", faceFront)
                        FaceThumb("Left", faceLeft)
                        FaceThumb("Right", faceRight)
                    }
                    OutlinedButton(
                        onClick = { startFaceScan() },
                        modifier = Modifier.fillMaxWidth().testTag("kyc_face_rescan_btn"),
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Re-scan face", maxLines = 1, softWrap = false)
                    }
                } else {
                    Button(
                        onClick = { startFaceScan() },
                        modifier = Modifier.fillMaxWidth().testTag("kyc_face_scan_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start face scan", maxLines = 1, softWrap = false)
                    }
                }
            }
        }

        // §706 — ID document capture (Aadhaar/PAN).
        Text("ID document", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Take a clear photo of your Aadhaar/PAN ID.", color = Color.Gray, fontSize = 11.sp)
        KycPhotoCapture(
            label = "ID Document (Aadhaar/PAN)",
            bitmap = documentBmp,
            onCapture = { captureDocument() },
            onRetake = { captureDocument() },
            captureText = "Capture ID Document",
            testTag = "kyc_document_capture_btn"
        )

        if (permissionDenied) {
            Text(
                "Camera permission is required to capture your photos. Enable it in Settings and try again.",
                color = Color.Red, fontSize = 11.sp
            )
        }

        // §713/§725 — REQUIRED business location, set through the shared ride-app picker.
        Text("Business location", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(
            "Required. Set where you're based so only nearby clients can book you.",
            color = Color.Gray, fontSize = 11.sp
        )
        if (locationReady) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LightSage),
                border = BorderStroke(1.dp, VedaDropRose.copy(alpha = 0.25f)),
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        kycAddress.ifBlank { "Location set" },
                        // §715 — explicit dark text on the LIGHT LightSage card (the app
                        // forces dark theme, whose default onSurface is near-white).
                        color = PlumDeepInk,
                        fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            OutlinedButton(
                onClick = { showLocPicker = true },
                modifier = Modifier.fillMaxWidth().testTag("kyc_change_location_btn"),
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Change location", maxLines = 1, softWrap = false)
            }
        } else {
            Button(
                onClick = { showLocPicker = true },
                modifier = Modifier.fillMaxWidth().testTag("kyc_set_location_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Set business location", maxLines = 1, softWrap = false)
            }
            Text("Location is required to submit.", color = OrderOrange, fontSize = 11.sp)
        }

        Button(
            onClick = {
                submitting = true
                viewModel.submitKyc(
                    aadhaar = aadhaarDigits,
                    pan = panUpper,
                    legalName = legalName.trim(),
                    faceFrontUrl = faceFront?.toJpegDataUrl(),
                    faceLeftUrl = faceLeft?.toJpegDataUrl(),
                    faceRightUrl = faceRight?.toJpegDataUrl(),
                    documentDataUrl = documentBmp?.toJpegDataUrl(),
                    baseLat = kycLat,
                    baseLon = kycLon,
                    baseAddress = kycAddress.ifBlank { null },
                ) { ok ->
                    submitting = false
                    if (ok) onSubmitted()
                }
            },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth().testTag("kyc_submit_action_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose)
        ) {
            Text(
                if (submitting) "Submitting…" else "Submit KYC",
                maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false
            )
        }
    }
}

/** §725 — one captured face-photo thumbnail (front/left/right) inside a Row. */
@Composable
private fun RowScope.FaceThumb(label: String, bmp: Bitmap?) {
    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "$label face photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(86.dp).clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth().height(86.dp).clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = 0.2f))
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(label, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

/** §706 — one photo slot: capture button, captured-thumbnail preview + retake. */
@Composable
private fun KycPhotoCapture(
    label: String,
    bitmap: Bitmap?,
    onCapture: () -> Unit,
    onRetake: () -> Unit,
    captureText: String,
    testTag: String,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            if (bitmap == null) {
                OutlinedButton(
                    onClick = onCapture,
                    modifier = Modifier.fillMaxWidth().testTag(testTag)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(captureText, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                }
            } else {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "$label preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Captured", color = SuccessGreen, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = onRetake) {
                        Text("Retake", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
fun PartnerServicesScreen(viewModel: VedaDropViewModel) {
    val activeServices by viewModel.partnerServices.collectAsState()

    // §726 — load the FULL service dictionary (all active services) so the partner
    // can add ANY service, not just the offered-only customer catalog. loadPartnerCatalog
    // also refreshes her own offerings so the list reflects the server, not a stale cache.
    LaunchedEffect(Unit) { viewModel.loadPartnerCatalog() }

    val allServices = VedaDropDataSource.services

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Catalog Pricing Editor", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { viewModel.currentScreen = Screen.PartnerDashboard }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        
        Text(
            text = "Specify custom service rates and toggle availability",
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                var showAddForm by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, VedaDropRose.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AddCircle, contentDescription = null, tint = VedaDropRose)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create completely custom service", fontWeight = FontWeight.Bold, color = VedaDropRose)
                            }
                            TextButton(onClick = { showAddForm = !showAddForm }) {
                                Text(if (showAddForm) "Hide Form" else "Open Form", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                            }
                        }
                        
                        if (showAddForm) {
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            var customName by remember { mutableStateOf("") }
                            var customCategory by remember { mutableStateOf("Salon") }
                            var customPrice by remember { mutableStateOf("") }
                            var customDuration by remember { mutableStateOf("45") }
                            var customDesc by remember { mutableStateOf("") }
                            var customProducts by remember { mutableStateOf("") }
                            
                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text("Service Name (e.g., Glow Max Anti-Tan Facial)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text("Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                listOf("Salon", "Beauty", "Makeup", "Massage").forEach { cat ->
                                    val isSelected = customCategory == cat
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { customCategory = cat },
                                        label = { Text(cat) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = VedaDropRose.copy(alpha = 0.25f),
                                            selectedLabelColor = VedaDropRose
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = customPrice,
                                    onValueChange = { customPrice = it },
                                    label = { Text("Price (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = customDuration,
                                    onValueChange = { customDuration = it },
                                    label = { Text("Duration (mins)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = customDesc,
                                onValueChange = { customDesc = it },
                                label = { Text("Description") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = customProducts,
                                onValueChange = { customProducts = it },
                                label = { Text("Products Used & Quality Seal Notes") },
                                placeholder = { Text("e.g. Biotique Premium Pack, opened brand new.") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val priceVal = customPrice.toLongOrNull() ?: 0L
                                    val durationVal = customDuration.toIntOrNull() ?: 45
                                    if (customName.isNotBlank() && priceVal > 0) {
                                        viewModel.createCustomPartnerService(
                                            name = customName,
                                            categoryName = customCategory,
                                            pricePaise = priceVal * 100L,
                                            durationMin = durationVal,
                                            description = customDesc.ifBlank { "Professional $customName service delivered safely at your home." },
                                            productsUsed = customProducts.ifBlank { "Professional kit, sealed pack opened with double visual verify check." }
                                        )
                                        customName = ""
                                        customPrice = ""
                                        customDesc = ""
                                        customProducts = ""
                                        showAddForm = false
                                    } else {
                                        viewModel.notify("Please fill in a valid service name and price in rupees.")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose)
                            ) {
                                Text("Add Service", fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = Color.Gray.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))
                Text("Toggle & customize standard services:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
            }
            items(allServices, key = { it.id }) { service ->
                val activeSetting = activeServices.firstOrNull { it.serviceId == service.id }
                // Rupee-denominated input (money is stored as paise; ₹ = paise / 100).
                var rateOverride by remember(activeSetting) { mutableStateOf(((activeSetting?.pricePaise ?: service.pricePaise) / 100).toString()) }
                var productsOverride by remember(activeSetting) { mutableStateOf(activeSetting?.productsUsed ?: "Premium salon kit (L'Oreal/O3+), 100% seal-packed & verified prior to use.") }
                var activatedState by remember(activeSetting) { mutableStateOf(activeSetting?.active ?: (activeSetting != null)) }
                // Save once via an explicit action: PATCH if listed, else create once.
                val saveService = saveService@{
                    val finalPrice = (rateOverride.trim().toLongOrNull() ?: 0L) * 100L
                    if (finalPrice <= 0L) {
                        viewModel.notify("Please enter a rate greater than ₹0.", isError = true)
                        return@saveService
                    }
                    viewModel.setPartnerServicePrice(service.id, service.name, service.categoryId, finalPrice, activatedState, productsOverride)
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(service.name, fontWeight = FontWeight.Bold)
                                Text("Open Pricing — you set your rate", fontSize = 11.sp, color = VedaDropRose, fontWeight = FontWeight.Bold)
                            }
                            // §703 — delete affordance: only when this service is one the
                            // partner has actually listed (has a server row to remove).
                            if (activeSetting != null) {
                                var showDeleteConfirm by remember { mutableStateOf(false) }
                                IconButton(onClick = { showDeleteConfirm = true }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove service", tint = Color(0xFFD32F2F))
                                }
                                if (showDeleteConfirm) {
                                    AlertDialog(
                                        onDismissRequest = { showDeleteConfirm = false },
                                        title = { Text("Remove service?") },
                                        text = { Text("Remove \"${service.name}\" from your offered services? Customers will no longer be able to book it from you.") },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    viewModel.deletePartnerService(activeSetting.id)
                                                    showDeleteConfirm = false
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                            ) { Text("Remove", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
                                        },
                                        dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) } },
                                    )
                                }
                            }
                            Switch(
                                checked = activatedState,
                                onCheckedChange = { activatedState = it }
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Set Custom Rate: ₹ ", fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = rateOverride,
                                onValueChange = { rateOverride = it.filter { c -> c.isDigit() }.take(7) },
                                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        Column {
                            Text("What products will you use & mutual seal quality notes:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = productsOverride,
                                onValueChange = { productsOverride = it },
                                placeholder = { Text("e.g. O3+ complete sealed package, opened in front of you.") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2,
                                singleLine = false
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🔒 Customers will see your pricing & specific product promises in the marketplace comparison list.",
                                fontSize = 10.sp,
                                color = VedaDropRose
                            )
                        }

                        // §704 — single explicit Save: price (₹→paise) + products + active
                        // in ONE call. No save-on-keystroke (no ₹/paise bug, no duplicate-create).
                        Button(
                            onClick = saveService,
                            modifier = Modifier.fillMaxWidth().testTag("save_service_${service.id}"),
                            colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose)
                        ) {
                            Text(if (activeSetting != null) "Save changes" else "List this service", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                        }
                    }
                }
            }
        }
    }
}

// §694 — a circular avatar showing the user's initials over a translucent fill.
// Used on the account headers for a clean, modern profile look.
@Composable
fun InitialsAvatar(name: String?, size: Int = 72) {
    val initials = (name ?: "")
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size / 2.6f).sp
        )
    }
}

// §694 — a 3-way professional gender preference selector (Any / Female / Male).
// Reused on the booking form and the partner profile edit form.
@Composable
fun GenderPreferenceSelector(selected: String, onSelect: (String) -> Unit) {
    // §722 women-only platform: the Male/Any options were removed entirely. Every
    // professional is a woman, so the preference is fixed to Female — we show a
    // reassurance chip and coerce the caller's state to "female".
    LaunchedEffect(Unit) { if (selected != "female") onSelect("female") }
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Verified, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "All our professionals are women — this is a women-only platform.",
                color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium
            )
        }
    }
}

// §694 — a read-only labelled field row, used by the profile screens when not
// in edit mode (normal profile UX: view first, tap Edit to change).
@Composable
fun ProfileReadonlyRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun CustomerProfileScreen(viewModel: VedaDropViewModel) {
    val activeUser by viewModel.activeUser.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val favorites by viewModel.favoritePartners.collectAsState()
    
    var nameState by remember { mutableStateOf(activeUser?.name ?: "") }
    var emailState by remember { mutableStateOf(activeUser?.email ?: "") }
    var showSavedNotification by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    // §694 — normal profile UX: the card is READ-ONLY until the user taps Edit;
    // Save persists and returns to read-only; Cancel discards edits.
    var isEditing by remember { mutableStateOf(false) }

    // Watch activeUser to prepopulate once loaded. While editing we don't clobber
    // the user's in-progress typing.
    LaunchedEffect(activeUser) {
        activeUser?.let {
            if (!isEditing) {
                nameState = it.name
                emailState = it.email
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // TOP Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DeepPlum, DarkSlate)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.currentScreen = Screen.CustomerHome },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = "CUSTOMER PROFILE",
                        style = MaterialTheme.typography.labelMedium,
                        color = VedaDropRose
                    )
                    Spacer(modifier = Modifier.width(48.dp)) // Equalizer
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    InitialsAvatar(activeUser?.name, size = 64)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = (activeUser?.name ?: "").ifBlank { "Your profile" },
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = (activeUser?.phone ?: "").ifBlank { "Customer account" },
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        // §707 — explicit role badge (a customer account, not a partner).
                        Surface(color = VedaDropRose.copy(alpha = 0.25f), shape = RoundedCornerShape(12.dp)) {
                            Text(
                                "CUSTOMER", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Edit Details Card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("edit_details_card"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header row: title + Edit pencil (only when read-only).
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CONTACT DETAILS",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (!isEditing) {
                            TextButton(
                                onClick = {
                                    // enter edit mode from the latest saved values
                                    nameState = activeUser?.name ?: nameState
                                    emailState = activeUser?.email ?: emailState
                                    validationError = null
                                    showSavedNotification = false
                                    isEditing = true
                                },
                                modifier = Modifier.testTag("profile_edit_btn")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit",
                                    modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                            }
                        }
                    }

                    if (isEditing) {
                        OutlinedTextField(
                            value = nameState,
                            onValueChange = {
                                nameState = it
                                validationError = null
                            },
                            label = { Text("Display Name") },
                            placeholder = { Text("Enter your name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_name_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = emailState,
                            onValueChange = {
                                emailState = it
                                validationError = null
                            },
                            label = { Text("Email Address") },
                            placeholder = { Text("Enter your email") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_email_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        if (validationError != null) {
                            Text(
                                text = validationError!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    // discard edits, revert to saved values
                                    nameState = activeUser?.name ?: ""
                                    emailState = activeUser?.email ?: ""
                                    validationError = null
                                    isEditing = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                            }
                            Button(
                                onClick = {
                                    if (nameState.trim().isEmpty()) {
                                        validationError = "Name field cannot be left blank."
                                    } else if (!Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(emailState.trim())) {
                                        validationError = "Please enter a valid email address."
                                    } else {
                                        viewModel.updateProfile(nameState.trim(), emailState.trim())
                                        viewModel.notify("Profile updated.", isError = false)
                                        showSavedNotification = true
                                        validationError = null
                                        isEditing = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_save_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Save", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                            }
                        }
                    } else {
                        // Read-only view.
                        ProfileReadonlyRow(Icons.Default.Person, "Display Name",
                            (activeUser?.name ?: "").ifBlank { "Not set" })
                        ProfileReadonlyRow(Icons.Default.Email, "Email Address",
                            (activeUser?.email ?: "").ifBlank { "Not set" })
                        ProfileReadonlyRow(Icons.Default.Phone, "Phone",
                            (activeUser?.phone ?: "").ifBlank { "Not set" })
                        if (showSavedNotification) {
                            Text(
                                text = "✓ Profile updated",
                                color = SuccessGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }

            // ── NEW BEAUTY PREFERENCES CARD (allow clients to manage skin goals) ──────
            var isEditingBeauty by remember { mutableStateOf(false) }
            var skinTypeSelection by remember { mutableStateOf(viewModel.customerSkinType) }
            var concernsSelection by remember { mutableStateOf(viewModel.customerBeautyConcerns) }
            var preferredTimeSelection by remember { mutableStateOf(viewModel.customerPreferredTime) }
            var showSavedBeautyNotification by remember { mutableStateOf(false) }

            LaunchedEffect(viewModel.customerSkinType, viewModel.customerBeautyConcerns, viewModel.customerPreferredTime) {
                if (!isEditingBeauty) {
                    skinTypeSelection = viewModel.customerSkinType
                    concernsSelection = viewModel.customerBeautyConcerns
                    preferredTimeSelection = viewModel.customerPreferredTime
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().testTag("beauty_preferences_card"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VedaDropGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "BEAUTY CARE & CONCERNS",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (!isEditingBeauty) {
                            TextButton(
                                onClick = {
                                    skinTypeSelection = viewModel.customerSkinType
                                    concernsSelection = viewModel.customerBeautyConcerns
                                    preferredTimeSelection = viewModel.customerPreferredTime
                                    isEditingBeauty = true
                                    showSavedBeautyNotification = false
                                },
                                modifier = Modifier.testTag("beauty_profile_edit_btn")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Beauty Profile", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                            }
                        }
                    }

                    if (isEditingBeauty) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Your Skin/Scalp Type:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Normal", "Oily", "Dry", "Sensitive", "Combination").forEach { type ->
                                    val isSelected = skinTypeSelection == type
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { skinTypeSelection = type },
                                        label = { Text(type, fontSize = 11.sp, color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = VedaDropRose
                                        )
                                    )
                                }
                            }

                            Text("Preferred Treatment Hours:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Morning (9AM-12PM)", "Afternoon (12PM-4PM)", "Evening (4PM-8PM)", "No Preference").forEach { time ->
                                    val isSelected = preferredTimeSelection == time
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { preferredTimeSelection = time },
                                        label = { Text(time, fontSize = 11.sp, color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = VedaDropRose
                                        )
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = concernsSelection,
                                onValueChange = { concernsSelection = it },
                                label = { Text("Beauty Concerns & Skincare Goals") },
                                placeholder = { Text("e.g. Hydration, anti-acne, bridal glow, sensitive roots") },
                                modifier = Modifier.fillMaxWidth().testTag("beauty_concerns_input"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { isEditingBeauty = false },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Discard", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                }
                                Button(
                                    onClick = {
                                        viewModel.updateBeautyProfile(skinTypeSelection, concernsSelection, preferredTimeSelection)
                                        isEditingBeauty = false
                                        showSavedBeautyNotification = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.weight(1.5f).testTag("beauty_profile_save_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Save Beauty Profile", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                }
                            }
                        }
                    } else {
                        ProfileReadonlyRow(Icons.Default.Face, "Skin Type", viewModel.customerSkinType)
                        ProfileReadonlyRow(Icons.Default.AccessTime, "Availability", viewModel.customerPreferredTime)
                        ProfileReadonlyRow(
                            Icons.Default.Info, 
                            "Goals & Concerns", 
                            viewModel.customerBeautyConcerns.ifBlank { "Not set. Share concerns to assist your partners." }
                        )

                        if (showSavedBeautyNotification) {
                            Text(
                                text = "✓ Beauty Profile updated locally",
                                color = SuccessGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
            
            // Notification Reminders Card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("notifications_setting_card"),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Push Reminders",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Get notified about upcoming bookings",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Switch(
                        checked = viewModel.pushRemindersEnabled,
                        onCheckedChange = { viewModel.updatePushReminders(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("notification_toggle_switch")
                    )
                }
            }
            
            // Quick links — dashboard + favourites.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { viewModel.currentScreen = Screen.CustomerDashboard },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, VedaDropRose),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VedaDropRose),
                ) { Text("Dashboard", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
                OutlinedButton(
                    onClick = { viewModel.currentScreen = Screen.Favourites },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, VedaDropRose),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VedaDropRose),
                ) { Text("Favourites", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
            }

            // Favorite Partners section
            val favPartnersMapped = remember(favorites) {
                VedaDropDataSource.partners.filter { p -> favorites.any { f -> f.partnerId == p.id } }
            }
            
            Text(
                text = "YOUR FAVORITE BEAUTY EXPERTS (${favPartnersMapped.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp)
            )
            
            if (favPartnersMapped.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("empty_favorites_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No favorite partners added yet.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tap the heart icon on any expert to save them here.",  // §714 cpe-fav-copy-1 — favourites are persisted, not "temporary"
                            fontSize = 10.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .testTag("favorites_scroll_row"),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    favPartnersMapped.forEach { partner ->
                        Card(
                            modifier = Modifier
                                .width(200.dp)
                                .testTag("fav_partner_card_${partner.id}"),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, VedaDropRose.copy(alpha = 0.3f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = partner.avatarUrl,
                                        contentDescription = partner.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(partner.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = VedaDropGold, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("${partner.rating}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = partner.description,
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { viewModel.toggleFavorite(partner.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                                    modifier = Modifier.fillMaxWidth().height(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Black)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Saved (Remove)", fontSize = 11.sp, color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }

            // ── NEW DIRECT DISCOVER & HEART PREFERRED EXPERTS ROW ──────
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "DISCOVER & SAVE EXPERTS",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Build your preferred beauty expert roster. Heart any professional to save them directly to favorites.",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val allPartners = remember { VedaDropDataSource.partners }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("discover_partners_save_row"),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                allPartners.forEach { partner ->
                    val isFavorite = favorites.any { it.partnerId == partner.id }
                    Card(
                        modifier = Modifier
                            .width(180.dp)
                            .clickable { viewModel.currentScreen = Screen.PartnerStore(partner) }
                            .testTag("discover_partner_card_${partner.id}"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isFavorite) VedaDropRose.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = partner.avatarUrl,
                                    contentDescription = partner.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                )
                                IconButton(
                                    onClick = { viewModel.toggleFavorite(partner.id) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("toggle_fav_btn_${partner.id}")
                                ) {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Save partner",
                                        tint = if (isFavorite) VedaDropRose else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = partner.name, 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.Bold, 
                                maxLines = 1, 
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = VedaDropGold, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("${partner.rating} • ${partner.experienceYears} YRS EXP", fontSize = 10.sp, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = partner.description,
                                fontSize = 10.sp,
                                color = Color.DarkGray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // ── REFACTORED APPOINTMENTS TAB (Upcoming vs Booking History) ──────
            var selectedBookingTab by remember { mutableStateOf(0) } // 0 = Active, 1 = History

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "APPOINTMENTS",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(3.dp)
                ) {
                    listOf("Active", "History").forEachIndexed { index, label ->
                        val isSelected = selectedBookingTab == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) VedaDropRose else Color.Transparent)
                                .clickable { selectedBookingTab = index }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("booking_tab_$index")
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            val upcoming = bookings.filter { it.status != "completed" && it.status != "cancelled" && it.status != "rejected" && it.status != "refunded" }
            val past = bookings.filter { it.status == "completed" || it.status == "cancelled" || it.status == "rejected" || it.status == "refunded" }

            val displayList = if (selectedBookingTab == 0) upcoming else past
            
            if (displayList.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("empty_bookings_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selectedBookingTab == 0) Icons.Default.CalendarToday else Icons.Default.History,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedBookingTab == 0) "No upcoming reservations scheduled." else "No appointment history found.",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        if (selectedBookingTab == 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                // §710 P0-12 — was Screen.ServiceBookingForm, a half-built
                                // dead-end that booked against an empty mock partner list and
                                // discarded the slot. Send the customer to the real catalog
                                // (browse → pick partner → real quote/slot/address) instead.
                                onClick = { viewModel.currentScreen = Screen.CustomerHome },
                                colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose)
                            ) {
                                Text("Book a Treatment Now", color = Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                            }
                        }
                    }
                }
            } else {
                displayList.forEach { booking ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.currentScreen = Screen.BookingDetail(booking.id) }
                            .testTag("booking_item_${booking.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = booking.serviceImageUrl,
                                contentDescription = booking.serviceName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = booking.serviceName.uppercase(),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Scheduled: ${booking.dateTimeSlot}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Expert: ${booking.partnerName}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = when(booking.status) {
                                                "pending" -> Color(0xFFFFF3E0)
                                                "accepted" -> Color(0xFFE8F5E9)
                                                "completed" -> Color(0xFFE0F2F1)
                                                "cancelled", "rejected", "refunded" -> Color(0xFFFFEBEE)
                                                else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = booking.status.replace("_", " ").uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when(booking.status) {
                                            "pending" -> Color(0xFFE65100)
                                            "accepted" -> Color(0xFF2E7D32)
                                            "completed" -> Color(0xFF00796B)
                                            "cancelled", "rejected", "refunded" -> Color(0xFFC62828)
                                            else -> MaterialTheme.colorScheme.secondary
                                        }
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.currentScreen = Screen.BookingDetail(booking.id) }) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "View Details", tint = VedaDropRose)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(
            onClick = { viewModel.logout() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(50.dp)
                .testTag("logout_button"),
            border = BorderStroke(1.dp, VedaDropRose),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = VedaDropRose),
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log out", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
        }

        // §704 — Play-Store-required account deletion (subtle, destructive).
        Spacer(modifier = Modifier.height(8.dp))
        var showDeleteAccount by remember { mutableStateOf(false) }
        TextButton(
            onClick = { showDeleteAccount = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("delete_account_button"),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text("Delete account", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
        }
        if (showDeleteAccount) {
            DeleteAccountDialog(
                onConfirm = { viewModel.deleteAccount(); showDeleteAccount = false },
                onDismiss = { showDeleteAccount = false },
            )
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

/** §704 — shared confirm dialog for permanent account deletion. */
@Composable
fun DeleteAccountDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete account?") },
        text = { Text("This permanently deletes your account. Continue?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_delete_account_btn"),
            ) { Text("Delete", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) } },
    )
}

data class Testimonial(
    val name: String,
    val rating: Int,
    val feedback: String,
    val serviceName: String,
    val relativeTime: String
)

data class JustBookedNotification(
    val id: String,
    val name: String,
    val serviceName: String,
    val location: String,
    val timeAgo: String,
    val service: Service?
)

data class FaqItem(
    val question: String,
    val answer: String
)

@Composable
fun ServiceBookingFormScreen(viewModel: VedaDropViewModel) {
    val services = VedaDropDataSource.services
    val activeUser by viewModel.activeUser.collectAsState()
    
    var selectedService by remember { mutableStateOf<Service?>(null) }
    var dateState by remember { mutableStateOf("") }
    var timeState by remember { mutableStateOf("") }
    var customNotes by remember { mutableStateOf("") }
    // §694 — professional gender preference captured at booking time.
    var genderPref by remember { mutableStateOf("female") }   // §722 women-only

    // Contact form fields
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }

    // Validation errors
    var contactNameError by remember { mutableStateOf<String?>(null) }
    var contactPhoneError by remember { mutableStateOf<String?>(null) }
    var contactEmailError by remember { mutableStateOf<String?>(null) }

    var errorState by remember { mutableStateOf<String?>(null) }
    var isBookingProgress by remember { mutableStateOf(false) }

    // Success confirmation popup
    var showSuccessModal by remember { mutableStateOf(false) }
    var successBookingId by remember { mutableStateOf("") }

    // Category filter
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    // Prefill form details with active user details once loaded
    LaunchedEffect(activeUser) {
        if (activeUser != null) {
            if (contactName.isBlank()) contactName = activeUser?.name ?: ""
            if (contactPhone.isBlank()) contactPhone = activeUser?.phone ?: ""
            if (contactEmail.isBlank()) contactEmail = activeUser?.email ?: ""
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // TOP Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DeepPlum, DarkSlate)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.currentScreen = Screen.CustomerHome },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = "APPOINTMENT CONCIERGE",
                        style = MaterialTheme.typography.labelMedium,
                        color = VedaDropRose
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                // Luxury Branded Fallback Logo Layout
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("booking_brand_logo")
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(VedaDropGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Spa,
                            contentDescription = "Veda Drop Logo",
                            tint = DeepPlum,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VEDA DROP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "BOOK TREATMENT",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White
                )
                Text(
                    text = "Select custom styling configurations with instant validation",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Choose Service Type (Using Beautiful 2-Column Responsive Grid with Descriptions & Toggle Filters)
            Column {
                Text(
                    text = "1. CHOOSE SERVICE TYPE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Category Filter Pills
                val categoriesFromDb = VedaDropDataSource.categories.map { it.name }
                val defaultCategories = listOf("Hair Care", "Skin Care", "Makeup")
                val uniqueCategories = (listOf("All") + defaultCategories + categoriesFromDb).distinct()

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uniqueCategories) { categoryName ->
                        val isFilterSelected = selectedCategoryFilter == categoryName
                        Surface(
                            modifier = Modifier
                                .clickable { selectedCategoryFilter = categoryName }
                                .testTag("filter_chip_${categoryName.lowercase().replace(" ", "_")}"),
                            shape = RoundedCornerShape(24.dp),
                            color = if (isFilterSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isFilterSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isFilterSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = categoryName.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFilterSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Filter the services based on the selected category filter
                val filteredServices = if (selectedCategoryFilter == "All") {
                    services
                } else {
                    services.filter { service ->
                        val matchingCat = VedaDropDataSource.categories.find { it.name.equals(selectedCategoryFilter, ignoreCase = true) }
                        if (matchingCat != null && service.categoryId == matchingCat.id) {
                            true
                        } else {
                            val term = when(selectedCategoryFilter) {
                                "Hair Care" -> "hair"
                                "Skin Care" -> "skin"
                                "Makeup" -> "makeup"
                                else -> selectedCategoryFilter.substringBefore(" ")
                            }
                            service.name.contains(term, ignoreCase = true) || 
                            service.description.contains(term, ignoreCase = true)
                        }
                    }
                }
                
                val serviceChunks = filteredServices.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (filteredServices.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Spa, contentDescription = null, tint = VedaDropRose, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No treatments listed under \"$selectedCategoryFilter\"",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        serviceChunks.forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                chunk.forEach { service ->
                                    val isSelected = selectedService?.id == service.id
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { 
                                                selectedService = service 
                                                errorState = null
                                            }
                                            .testTag("select_service_${service.id}"),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Column {
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                AsyncImage(
                                                    model = service.imageUrl,
                                                    contentDescription = service.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(110.dp)
                                                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                                )
                                                if (isSelected) {
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.primary,
                                                        shape = RoundedCornerShape(bottomEnd = 12.dp),
                                                        modifier = Modifier.align(Alignment.TopStart)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                Icons.Default.CheckCircle,
                                                                contentDescription = "Selected",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "SELECTED",
                                                                color = Color.White,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = service.name,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = service.priceLabel(),
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = service.description,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 3,
                                                    overflow = TextOverflow.Ellipsis,
                                                    lineHeight = 14.sp
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            Icons.Default.Star,
                                                            contentDescription = null,
                                                            tint = VedaDropGold,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(
                                                            text = "${service.rating}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Schedule,
                                                            contentDescription = "Estimated Duration",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = "Duration: ${service.durationMin} mins",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (chunk.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
            
            // Section 2: Date and Time Configuration
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "2. DATE & TIME SELECTION",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                // §690 — real calendar + time pickers (were free-text; "calendar
                // never opened"). Tapping opens a Material3 DatePicker / TimePicker.
                VedaDropDateField(
                    value = dateState,
                    onChange = { dateState = it; errorState = null },
                    label = "Appointed Date",
                    iconTint = VedaDropRose,
                    modifier = Modifier.fillMaxWidth().testTag("booking_date_input"),
                )

                VedaDropTimeField(
                    value = timeState,
                    onChange = { timeState = it; errorState = null },
                    label = "Preferred Time Slot",
                    iconTint = VedaDropRose,
                    modifier = Modifier.fillMaxWidth().testTag("booking_time_input"),
                )
            }
            
            // Section 3: Optional Notes
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "3. SPECIAL INSTRUCTIONS (OPTIONAL)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                OutlinedTextField(
                    value = customNotes,
                    onValueChange = { customNotes = it },
                    label = { Text("Styling instructions or location guidelines") },
                    placeholder = { Text("e.g. Please bring extra hair gel") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .testTag("booking_notes_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Section 4: Professional gender preference (§694)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "4. PROFESSIONAL PREFERENCE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                GenderPreferenceSelector(selected = genderPref, onSelect = { genderPref = it })
            }

            // Section 5: Contact Information Validation
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "5. CONTACT INFORMATION",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = {
                                contactName = it
                                contactNameError = if (it.trim().length < 3) "Name must be at least 3 characters." else null
                            },
                            label = { Text("Client Name") },
                            isError = contactNameError != null,
                            supportingText = {
                                if (contactNameError != null) {
                                    Text(contactNameError!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = VedaDropRose) },
                            modifier = Modifier.fillMaxWidth().testTag("booking_contact_name_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VedaDropRose,
                                focusedLabelColor = VedaDropRose
                            )
                        )

                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { newValue ->
                                val filtered = newValue.filter { c -> c.isDigit() }.take(10)
                                contactPhone = filtered
                                contactPhoneError = if (filtered.length != 10) "Phone must be exactly 10 digits." else null
                            },
                            label = { Text("Phone Number") },
                            isError = contactPhoneError != null,
                            supportingText = {
                                if (contactPhoneError != null) {
                                    Text(contactPhoneError!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = VedaDropRose) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth().testTag("booking_contact_phone_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VedaDropRose,
                                focusedLabelColor = VedaDropRose
                            )
                        )

                        OutlinedTextField(
                            value = contactEmail,
                            onValueChange = {
                                contactEmail = it
                                val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
                                contactEmailError = if (!emailRegex.matches(it.trim())) "Enter a valid email address." else null
                            },
                            label = { Text("Email Address") },
                            isError = contactEmailError != null,
                            supportingText = {
                                if (contactEmailError != null) {
                                    Text(contactEmailError!!, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = VedaDropRose) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth().testTag("booking_contact_email_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VedaDropRose,
                                focusedLabelColor = VedaDropRose
                            )
                        )
                    }
                }
            }

            // Error Indicator
            if (errorState != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorState!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Action button
            Button(
                onClick = {
                    contactNameError = if (contactName.trim().length < 3) "Name must be at least 3 characters." else null
                    contactPhoneError = if (contactPhone.trim().length != 10 || !contactPhone.trim().all { char -> char.isDigit() }) 
                        "Phone must be exactly 10 digits." else null
                    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
                    contactEmailError = if (!emailRegex.matches(contactEmail.trim())) "Enter a valid email address." else null

                    if (selectedService == null) {
                        errorState = "Please select a wellness treatment service above."
                    } else if (dateState.trim().isEmpty()) {
                        errorState = "Appointment date cannot be left blank."
                    } else if (timeState.trim().isEmpty()) {
                        errorState = "Appointment preference slot time cannot be left blank."
                    } else if (contactNameError != null || contactPhoneError != null || contactEmailError != null) {
                        errorState = "Please fix the contact information validation errors below."
                    } else {
                        errorState = null
                        isBookingProgress = true
                        // §694 — hand the captured notes + gender preference to the VM
                        // so they ride along on the create-booking request.
                        viewModel.bookingNotes = if (customNotes.isNotBlank()) {
                            "$customNotes [Contact: $contactName, Phone: $contactPhone, Email: $contactEmail]"
                        } else {
                            "[Contact: $contactName, Phone: $contactPhone, Email: $contactEmail]"
                        }
                        viewModel.bookingGenderPref = genderPref
                        viewModel.bookDirectlyFromForm(
                            service = selectedService!!,
                            slot = "${dateState.trim()} at ${timeState.trim()}",
                            onSuccess = { bookingId ->
                                isBookingProgress = false
                                successBookingId = bookingId
                                showSuccessModal = true
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_booking_form_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = !isBookingProgress
            ) {
                if (isBookingProgress) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "CONFIRM & SECURE BOOKING",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            }
        }

        // Popup Confirmation Dialog Modal
        if (showSuccessModal) {
            AlertDialog(
                onDismissRequest = { showSuccessModal = false },
                confirmButton = {
                    Button(
                        onClick = {
                            showSuccessModal = false
                            viewModel.currentScreen = Screen.MyBookings
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().testTag("modal_view_bookings_btn")
                    ) {
                        Text("VIEW MY BOOKINGS", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showSuccessModal = false
                            // reset form on close
                            selectedService = null
                            dateState = ""
                            timeState = ""
                            customNotes = ""
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("modal_close_btn")
                    ) {
                        Text("BOOK ANOTHER TREATMENT", color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                    }
                },
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(VedaDropGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "Success",
                                tint = VedaDropGold,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Booking Confirmed! ✨",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Your beauty concierge reservation has been successfully booked with supreme confidence.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        selectedService?.let { service ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Treatment:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                        Text(service.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Duration & Price:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                        Text("${service.durationMin}m • ${service.priceLabel()}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Date & Time:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                        Text("$dateState at $timeState", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Client Contact:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                        Text("$contactName ($contactPhone)", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Booking ID:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                        Text("#$successBookingId", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                properties = DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
            )
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun PreBookingChatScreen(viewModel: VedaDropViewModel, service: Service, partner: Partner) {
    val activeUser by viewModel.activeUser.collectAsState()
    val preBookingId = "pre_${partner.id}_${service.id}"
    val messagesFromDb by viewModel.getMessagesForBooking(preBookingId).collectAsState(initial = emptyList())
    val localMessages = remember { mutableStateListOf<ChatMessageEntity>() }
    val scope = rememberCoroutineScope()
    var chatText by remember { mutableStateOf("") }

    // Merge messages from database to avoid duplicates and show past messages
    LaunchedEffect(messagesFromDb) {
        messagesFromDb.forEach { dbMsg ->
            if (localMessages.none { it.id == dbMsg.id || (it.timestamp == dbMsg.timestamp && it.text == dbMsg.text) }) {
                localMessages.add(dbMsg)
            }
        }
    }

    // Poll the thread while open so incoming partner replies surface without
    // re-sending/reopening. Auto-cancels on leaving the screen.
    LaunchedEffect(preBookingId) {
        while (isActive) {
            kotlinx.coroutines.delay(5000)
            viewModel.refreshThread(preBookingId)
        }
    }

    fun handleSendMessage(text: String) {
        if (text.isBlank()) return
        val userRole = activeUser?.role ?: "customer"
        val userMsg = ChatMessageEntity(
            id = System.currentTimeMillis(),
            bookingId = preBookingId,
            senderRole = userRole,
            text = text,
            kind = "text",
            timestamp = System.currentTimeMillis()
        )
        if (localMessages.none { it.text == text && it.timestamp == userMsg.timestamp }) {
            localMessages.add(userMsg)
        }
        viewModel.sendChatMessage(preBookingId, userRole, text)
        // The real partner answers over the real thread/WS — no fabricated replies.
    }

    // Auto-reply suggestions
    val suggestions = listOf(
        "Is the service package 100% brand new & sealed pack?",
        "Will you open and verify the seal status in front of me?",
        "Which premium beauty brands/kits will you bring?",
        "Is there any hidden or extra home travel charge?"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(text = partner.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Pre-Booking Direct Line: ${service.name}", fontSize = 11.sp, color = VedaDropRose, fontWeight = FontWeight.SemiBold)
                }
            },
            navigationIcon = {
                IconButton(onClick = { 
                    if (activeUser?.role == "customer") {
                        viewModel.currentScreen = Screen.PartnerSelect(service) 
                    } else {
                        viewModel.currentScreen = Screen.PartnerDashboard
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            actions = {
                // Verified Status Badge in chat header
                Surface(
                    color = Color(0xFFE0F2F1),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Status",
                            tint = Color(0xFF00796B),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "SEAL SECURE",
                            color = Color(0xFF00796B),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepPlum, titleContentColor = Color.White)
        )

        // Trust alert header about open pricing and seal packs
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = VedaDropRose, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🔒 Trust Clause: Confirm details & product brands here before booking. Mutual agreement constitutes full job compliance.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 15.sp
                )
            }
        }

        // Chat contents
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            reverseLayout = false,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (localMessages.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No discussions yet with ${partner.name}", fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("Touch a question chip below or type message to confirm brand products used.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(localMessages, key = { it.id }) { msg ->
                    val isMe = msg.senderRole == (activeUser?.role ?: "customer")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(
                                topStart = 12.dp,
                                topEnd = 12.dp,
                                bottomStart = if (isMe) 12.dp else 0.dp,
                                bottomEnd = if (isMe) 0.dp else 12.dp
                            ),
                            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (isMe) "You" else partner.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary else VedaDropRose
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = msg.text,
                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Suggestions Quick Chips
        Text(
            text = "💡 QUICK SERVICE QUESTIONS:",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = VedaDropRose,
            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp, top = 8.dp)
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestions) { question ->
                SuggestionChip(
                    onClick = {
                        handleSendMessage(question)
                    },
                    label = { Text(question, fontSize = 11.sp, maxLines = 1) }
                )
            }
        }

        // Bottom text field entry
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = chatText,
                onValueChange = { chatText = it },
                placeholder = { Text("Ask about brand, seal, timing...", fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("pre_chat_input"),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (chatText.isNotBlank()) {
                        handleSendMessage(chatText)
                        chatText = ""
                    }
                },
                enabled = chatText.isNotBlank(),
                modifier = Modifier
                    .background(VedaDropRose, CircleShape)
                    .size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message", tint = Color.White)
            }
        }
    }
}


// ────────────────────────────────────────── PARTNER MONTHLY GROWTH & EARNINGS LINE CHART ──────────────────────────────────────────

data class MonthlyStats(val monthLabel: String, val bookingsCount: Int, val earnings: Double)

fun getMonthlyStats(bookings: List<com.example.data.BookingEntity>): List<MonthlyStats> {
    val completed = bookings.filter { it.status.equals("completed", ignoreCase = true) || it.status.equals("accepted", ignoreCase = true) }
    val fmt = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.US)
    val grouped = completed.groupBy { 
        if (it.createdAt > 0) fmt.format(java.util.Date(it.createdAt)) else fmt.format(java.util.Date()) 
    }
    
    val cal = java.util.Calendar.getInstance()
    val pastSixMonths = (0..5).map { offset ->
        val c = cal.clone() as java.util.Calendar
        c.add(java.util.Calendar.MONTH, -offset)
        c.time
    }.reversed()
    
    // No fabricated seed data: empty input → empty list so the chart shows its empty state.
    if (completed.isEmpty()) return emptyList()

    return pastSixMonths.map { date ->
        val key = fmt.format(date)
        val list = grouped[key] ?: emptyList()
        val count = list.size
        val earningsSum = list.sumOf { it.totalPaise }.toDouble() / 100.0
        MonthlyStats(key, count, earningsSum)
    }
}

@Composable
fun MonthlyGrowthLineChart(bookings: List<com.example.data.BookingEntity>) {
    val stats = remember(bookings) { getMonthlyStats(bookings) }

    if (stats.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, VedaDropRose.copy(alpha = 0.22f)),
            modifier = Modifier.fillMaxWidth().testTag("earnings_line_chart_card")
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(130.dp).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No earnings data yet",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        return
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, VedaDropRose.copy(alpha = 0.22f)),
        modifier = Modifier.fillMaxWidth().testTag("earnings_line_chart_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "BUSINESS GROWTH METRICS 📈",
                        style = MaterialTheme.typography.labelSmall,
                        color = VedaDropRose,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "Monthly Volume & Earnings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(VedaDropRose, CircleShape))
                        Text("Earnings", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(VedaDropGold, CircleShape))
                        Text("Volume", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            val maxEarnings = remember(stats) { (stats.maxOfOrNull { it.earnings } ?: 5000.0).coerceAtLeast(100.0) }
            val maxVolume = remember(stats) { (stats.maxOfOrNull { it.bookingsCount } ?: 10).coerceAtLeast(2) }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                    val width = size.width
                    val height = size.height
                    
                    val marginL = 50f
                    val marginR = 10f
                    val marginB = 35f
                    val marginT = 15f
                    
                    val chartW = width - marginL - marginR
                    val chartH = height - marginT - marginB
                    
                    val gridCount = 3
                    for (i in 0..gridCount) {
                        val y = marginT + chartH * (i.toFloat() / gridCount)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.12f),
                            start = androidx.compose.ui.geometry.Offset(marginL, y),
                            end = androidx.compose.ui.geometry.Offset(width - marginR, y),
                            strokeWidth = 1f
                        )
                    }
                    
                    val pts = stats.mapIndexed { idx, stat ->
                        val x = marginL + chartW * (idx.toFloat() / (stats.size - 1).coerceAtLeast(1))
                        val yEarnings = marginT + chartH * (1f - (stat.earnings.toFloat() / maxEarnings.toFloat()))
                        val yVolume = marginT + chartH * (1f - (stat.bookingsCount.toFloat() / maxVolume.toFloat()))
                        Triple(x, yEarnings, yVolume)
                    }
                    
                    val earningsShadePath = androidx.compose.ui.graphics.Path().apply {
                        pts.forEachIndexed { idx, pt ->
                            if (idx == 0) moveTo(pt.first, pt.second)
                            else lineTo(pt.first, pt.second)
                        }
                        lineTo(pts.last().first, marginT + chartH)
                        lineTo(pts.first().first, marginT + chartH)
                        close()
                    }
                    drawPath(
                        path = earningsShadePath,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(VedaDropRose.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
                    
                    val earningsPath = androidx.compose.ui.graphics.Path().apply {
                        pts.forEachIndexed { idx, pt ->
                            if (idx == 0) moveTo(pt.first, pt.second)
                            else lineTo(pt.first, pt.second)
                        }
                    }
                    drawPath(
                        path = earningsPath,
                        color = VedaDropRose,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.5f)
                    )
                    
                    val volumePath = androidx.compose.ui.graphics.Path().apply {
                        pts.forEachIndexed { idx, pt ->
                            if (idx == 0) moveTo(pt.first, pt.third)
                            else lineTo(pt.first, pt.third)
                        }
                    }
                    drawPath(
                        path = volumePath,
                        color = VedaDropGold,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                    )
                    
                    pts.forEach { pt ->
                        drawCircle(
                            color = VedaDropRose,
                            radius = 6f,
                            center = androidx.compose.ui.geometry.Offset(pt.first, pt.second)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5f,
                            center = androidx.compose.ui.geometry.Offset(pt.first, pt.second)
                        )
                    }
                    
                    pts.forEach { pt ->
                        drawCircle(
                            color = VedaDropGold,
                            radius = 5f,
                            center = androidx.compose.ui.geometry.Offset(pt.first, pt.third)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2f,
                            center = androidx.compose.ui.geometry.Offset(pt.first, pt.third)
                        )
                    }
                }
                
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 2.dp, top = 4.dp, bottom = 22.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("₹${(maxEarnings).toInt()}", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = VedaDropRose)
                    Text("₹${(maxEarnings / 2).toInt()}", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = VedaDropRose)
                    Text("₹0", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = VedaDropRose)
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                stats.forEach { stat ->
                    Text(
                        text = stat.monthLabel.substringBefore(" "),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

