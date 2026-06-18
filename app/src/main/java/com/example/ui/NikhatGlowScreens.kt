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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import coil.compose.AsyncImage
import com.example.data.*
import com.example.data.remote.CartItemDto
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NikhatGlowMainShell(viewModel: NikhatGlowViewModel) {
    val activeUser by viewModel.activeUser.collectAsState()
    val cart by viewModel.cart.collectAsState()

    val currentThemeDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()

    val showLogin = !viewModel.isLoggedIn && !viewModel.isGuestMode

    Scaffold(
        bottomBar = {
            if (!showLogin) {
                NikhatGlowBottomBar(
                    currentScreen = viewModel.currentScreen,
                    userRole = activeUser?.role ?: "customer",
                    cartCount = cart?.count ?: 0,
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
                NikhatGlowLoginScreen(viewModel)
                return@Box
            }
            AnimatedContent(
                targetState = viewModel.currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.CustomerHome -> CustomerHomeScreen(viewModel)
                    is Screen.CategoryDetail -> CategoryDetailScreen(viewModel, screen.category)
                    is Screen.ServiceDetail -> ServiceDetailScreen(viewModel, screen.service)
                    is Screen.PartnerSelect -> PartnerSelectScreen(viewModel, screen.service)
                    is Screen.BookingConfirm -> BookingConfirmScreen(viewModel, screen.service, screen.partner)
                    is Screen.BookingDetail -> BookingDetailScreen(viewModel, screen.bookingId)
                    is Screen.Cart -> CartScreen(viewModel)
                    is Screen.MyBookings -> MyBookingsScreen(viewModel)
                    is Screen.ComplaintsList -> ComplaintsListScreen(viewModel)
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
                    is Screen.PartnerEarnings -> PartnerEarningsScreen(viewModel)
                    is Screen.PartnerAnalytics -> PartnerAnalyticsScreen(viewModel)
                    is Screen.PartnerPortfolio -> PartnerPortfolioScreen(viewModel)
                    is Screen.PreBookingChat -> PreBookingChatScreen(viewModel, screen.service, screen.partner)
                }
            }
        }
    }
}

@Composable
fun NikhatGlowBottomBar(
    currentScreen: Screen,
    userRole: String,
    cartCount: Int,
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
                icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                label = { Text("Profile", fontSize = 11.sp) },
                colors = selectedColors,
                modifier = Modifier.testTag("customer_profile_tab")
            )
        } else {
            NavigationBarItem(
                selected = currentScreen is Screen.PartnerDashboard,
                onClick = { onNavigate(Screen.PartnerDashboard) },
                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                label = { Text("Dashboard", fontSize = 11.sp) },
                colors = selectedColors,
            )
            NavigationBarItem(
                selected = currentScreen is Screen.PartnerServices,
                onClick = { onNavigate(Screen.PartnerServices) },
                icon = { Icon(Icons.Default.ContentCut, contentDescription = "Services") },
                label = { Text("Services", fontSize = 11.sp) },
                colors = selectedColors,
            )
            NavigationBarItem(
                selected = currentScreen is Screen.MyBookings,
                onClick = { onNavigate(Screen.MyBookings) },
                icon = { Icon(Icons.Default.EventNote, contentDescription = "Requests") },
                label = { Text("Requests", fontSize = 11.sp) },
                colors = selectedColors,
                modifier = Modifier.testTag("partner_requests_tab"),
            )
            NavigationBarItem(
                selected = currentScreen is Screen.PartnerProfile,
                onClick = { onNavigate(Screen.PartnerProfile) },
                icon = { Icon(Icons.Default.Person, contentDescription = "Account") },
                label = { Text("Account", fontSize = 11.sp) },
                colors = selectedColors,
            )
        }
    }
}

// ---------------- CUSTOMER SCREENS ----------------

@Composable
fun CustomerHomeScreen(viewModel: NikhatGlowViewModel) {
    val activeUser by viewModel.activeUser.collectAsState()
    val addresses by viewModel.addresses.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    
    val activeAddress = addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
    var searchPrompt by remember { mutableStateOf("") }
    var minRatingFilter by remember { mutableStateOf(0.0) }

    val filteredServices = NikhatGlowDataSource.services.filter { service ->
        val matchesSearch = searchPrompt.isBlank() ||
            service.name.contains(searchPrompt, ignoreCase = true) ||
            service.description.contains(searchPrompt, ignoreCase = true)
        val matchesRating = service.rating >= minRatingFilter
        matchesSearch && matchesRating
    }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Deliver To".uppercase(),
                            color = GlamGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = GlamRose, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activeAddress?.labelText?.let { "$it - ${activeAddress.line1}" } ?: "Select Location",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.currentScreen = Screen.Cart },
                        modifier = Modifier.testTag("cart_view_btn")
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "WELCOME, ${activeUser?.name?.uppercase() ?: "GUEST"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = GlamGold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "BEAUTY AT\nYOUR DOOR",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // SEARCH Bar
                TextField(
                    value = searchPrompt,
                    onValueChange = { searchPrompt = it },
                    placeholder = { Text("Search haircut, facials, massage...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("home_search_bar"),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
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
                                selectedContainerColor = GlamRose,
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
                            Text("Clear", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        
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
                        colors = ButtonDefaults.buttonColors(containerColor = GlamGold)
                    ) {
                        Text("Track", color = Color.Black)
                    }
                }
            }
        }

        // CATEGORY SECTION
        Text(
            text = "EXPLORE CATEGORIES",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 4.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NikhatGlowDataSource.categories.forEach { cat ->
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
        
        // QUICK ACTION: Booking Form Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { viewModel.currentScreen = Screen.ServiceBookingForm }
                .testTag("quick_booking_banner"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CUSTOM APPOINTMENT",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "BOOK ANY SERVICE",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Select custom date, time & treatment type directly.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New booking",
                        tint = Color.White
                    )
                }
            }
        }

        // SERVICES SUGGESTIONS
        Text(
            text = "TRENDING TREATMENTS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp)
        )
        
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
                                    "₹${service.pricePaise / 100}",
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
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun CategoryDetailScreen(viewModel: NikhatGlowViewModel, category: Category) {
    val services = NikhatGlowDataSource.services.filter { it.categoryId == category.id }
    
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
                        Text("Nikhat Glow Doorstep Certified", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text("Highly trained beauty experts carrying sanitized premium kits and single-use products for the clean environment salon safety.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
            
            items(services) { service ->
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
                                Text("₹${service.pricePaise / 100}", fontWeight = FontWeight.Bold, color = GlamGold, fontSize = 16.sp)
                                Button(
                                    onClick = { viewModel.currentScreen = Screen.ServiceDetail(service) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GlamGold)
                                ) {
                                    Text("Add", color = Color.Black)
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
fun ServiceDetailScreen(viewModel: NikhatGlowViewModel, service: Service) {
    var isFaqExpanded by remember { mutableStateOf(false) }
    
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
                    Icon(Icons.Default.Star, contentDescription = null, tint = GlamGold, modifier = Modifier.size(18.dp))
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
                    Text("₹${service.pricePaise / 100}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GlamGold)
                }
                
                Button(
                    onClick = { viewModel.currentScreen = Screen.PartnerSelect(service) },
                    colors = ButtonDefaults.buttonColors(containerColor = GlamRose),
                    modifier = Modifier.testTag("select_partner_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Choose Beauty Partner", fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("SERVICE DESCRIPTION", fontWeight = FontWeight.Bold, color = GlamGold, letterSpacing = 1.sp)
            Text(service.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("WHAT'S INCLUDED", fontWeight = FontWeight.Bold, color = GlamGold, letterSpacing = 1.sp)
            service.inclusions.forEach { incl ->
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(incl, fontSize = 14.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
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
                            Text("Q: ${faq.first}", fontWeight = FontWeight.Bold, color = GlamGold)
                            Text("A: ${faq.second}", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PartnerSelectScreen(viewModel: NikhatGlowViewModel, service: Service) {
    val bookings by viewModel.bookings.collectAsState()

    // Discovery: who actually offers THIS service right now (subscription-gated
    // server-side). Blank until a subscribed partner adds it.
    LaunchedEffect(service.id) { viewModel.loadPartnersForService(service.id) }
    val partners = NikhatGlowDataSource.partners

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

        // Switch info header - Swiggy/Zomato style
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
                    contentDescription = "Swiggy Model",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Swiggy & Zomato Open Marketplace",
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
            if (marketplaceOffers.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No professionals offer this service yet", fontWeight = FontWeight.Bold)
                        Text(
                            "New experts are joining Nikhat Glow every day — please check back soon.",
                            fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            items(marketplaceOffers) { offer ->
                val (partner, resolvedPrice, resolvedProducts) = offer
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
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
                                    Surface(
                                        color = Color(0xFFE3F2FD),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = "Verified Quality Seller",
                                                tint = Color(0xFF1E88E5),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "VERIFIED",
                                                color = Color(0xFF1E88E5),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = GlamGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${partner.rating} (${partner.reviewsCount} jobs completed)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${partner.experienceYears} Years Experience", fontSize = 11.sp, color = GlamGold, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("• 100% Seal-Verified", fontSize = 10.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
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
                                    color = GlamGold,
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
                            Text("PORTFOLIO SHOWCASE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GlamGold)
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
                        Text("RATINGS & CLIENT REVIEWS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GlamGold)
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
                                                        Icon(Icons.Default.Star, contentDescription = null, tint = GlamGold, modifier = Modifier.size(12.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.weight(1f))
                                                Text("Verified Client", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
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
                                                Row {
                                                    val intRate = rate.toInt()
                                                    repeat(intRate) {
                                                        Icon(Icons.Default.Star, contentDescription = null, tint = GlamGold, modifier = Modifier.size(12.dp))
                                                    }
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
                                border = BorderStroke(1.dp, GlamRose)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = GlamRose, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chat", color = GlamRose, fontSize = 12.sp)
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
                                border = BorderStroke(1.dp, GlamRose)
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = GlamRose, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add", color = GlamRose, fontSize = 12.sp)
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
                                colors = ButtonDefaults.buttonColors(containerColor = GlamRose)
                            ) {
                                Text("Book", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingConfirmScreen(viewModel: NikhatGlowViewModel, service: Service, partner: Partner) {
    val addresses by viewModel.addresses.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    
    val defaultAddress = addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
    var showNewAddressDialog by remember { mutableStateOf(false) }
    
    var labelInput by remember { mutableStateOf("Home") }
    var line1Input by remember { mutableStateOf("") }
    var line2Input by remember { mutableStateOf("") }
    var cityInput by remember { mutableStateOf("Bengaluru") }
    var pincodeInput by remember { mutableStateOf("") }

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
            Text("1. DELIVERY ADDRESS", fontWeight = FontWeight.Bold, color = GlamGold)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (defaultAddress != null) {
                        Text(defaultAddress.labelText, fontWeight = FontWeight.Bold)
                        Text("${defaultAddress.line1} ${defaultAddress.line2}", fontSize = 13.sp)
                        Text("${defaultAddress.city} - ${defaultAddress.pincode}", fontSize = 13.sp)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        Text("No delivery addresses saved yet", color = Color.Gray, fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = { showNewAddressDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_address_btn")
                    ) {
                        Icon(Icons.Default.AddLocation, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add New Custom Address")
                    }
                }
            }
            
            // STEP 2: Timing note (connector model — exact time arranged in chat)
            Spacer(modifier = Modifier.height(16.dp))
            Text("2. PREFERRED TIME", fontWeight = FontWeight.Bold, color = GlamGold)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = viewModel.selectedSlot,
                        onValueChange = { viewModel.selectedSlot = it },
                        label = { Text("e.g. Tomorrow evening, or a date & time") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "You'll confirm the exact time directly with the professional in chat once they accept your request.",
                        fontSize = 11.sp, color = Color.Gray, lineHeight = 15.sp
                    )
                }
            }

            // STEP 3: Estimate (you pay the professional directly)
            Spacer(modifier = Modifier.height(16.dp))
            Text("3. ESTIMATE", fontWeight = FontWeight.Bold, color = GlamGold)
            if (quote != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Estimated total", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                "₹${quote.totalPaise / 100}",
                                fontWeight = FontWeight.Bold,
                                color = GlamGold,
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
                                text = "Estimate only — you pay the professional directly after the service. Nikhat Glow never holds your money.",
                                fontSize = 10.sp,
                                color = Color.LightGray.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    if (defaultAddress != null) {
                        viewModel.confirmAndBook(service, partner, defaultAddress)
                    }
                },
                enabled = defaultAddress != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pay_book_action_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = GlamRose),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Lock Appointment & Pay", fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    
    // Dialog for adding address
    if (showNewAddressDialog) {
        Dialog(onDismissRequest = { showNewAddressDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Add New Custom Address", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GlamGold)
                    
                    OutlinedTextField(
                        value = labelInput,
                        onValueChange = { labelInput = it },
                        label = { Text("Address Label (e.g. Home, Office)") }
                    )
                    OutlinedTextField(
                        value = line1Input,
                        onValueChange = { line1Input = it },
                        label = { Text("Building/Street Name (Line 1)") }
                    )
                    OutlinedTextField(
                        value = line2Input,
                        onValueChange = { line2Input = it },
                        label = { Text("Floor/Flat/Area (Line 2)") }
                    )
                    OutlinedTextField(
                        value = pincodeInput,
                        onValueChange = { pincodeInput = it },
                        label = { Text("Pincode") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showNewAddressDialog = false }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (line1Input.isNotBlank() && pincodeInput.isNotBlank()) {
                                    viewModel.addNewAddress(labelInput, line1Input, line2Input, cityInput, pincodeInput)
                                    showNewAddressDialog = false
                                }
                            }
                        ) {
                            Text("Add Address")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingDetailScreen(viewModel: NikhatGlowViewModel, bookingId: String) {
    val bookings by viewModel.bookings.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val messages by viewModel.getMessagesForBooking(bookingId).collectAsState(initial = emptyList())
    
    val booking = bookings.firstOrNull { it.id == bookingId }
    var chatText by remember { mutableStateOf("") }
    
    var showChatTab by remember { mutableStateOf(false) }
    var reviewRatingSelected by remember { mutableStateOf(5) }
    var reviewCommentText by remember { mutableStateOf("") }
    
    var hygieneRating by remember { mutableStateOf(5) }
    var skillRating by remember { mutableStateOf(5) }
    var authenticityRating by remember { mutableStateOf(5) }

    if (booking == null) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Booking Not Found")
            Button(onClick = { viewModel.currentScreen = Screen.CustomerHome }) {
                Text("Go Home")
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
                        .height(120.dp)
                        .background(Brush.verticalGradient(listOf(DeepPlum, DarkSlate))),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val statusIcon = when (booking.status) {
                            "partner_on_the_way" -> Icons.Default.DirectionsBike
                            "arrived", "started" -> Icons.Default.Home
                            "completed" -> Icons.Default.Verified
                            else -> Icons.Default.EventNote
                        }
                        Icon(statusIcon, contentDescription = null, tint = GlamGold, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = booking.status.replace("_", " ").replaceFirstChar { it.uppercase() },
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    // DETAILS
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                            Text("Professional assigned", fontSize = 12.sp, color = GlamGold)
                        }
                        
                        // Deep link code trigger action
                        StateChip(booking.status)
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    Text("Session Code OTP: ${booking.startOtp}", fontWeight = FontWeight.Bold, color = GlamGold, fontSize = 16.sp)
                    Text("Show this OTP code to your stylist to authorize starting the doorstep session safety check.", fontSize = 12.sp, color = Color.Gray)
                    
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    // TIMELINE STATE MACHINE
                    Text("BOOKING STATUS TIMELINE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = GlamGold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    TimelineStep("Appointment Booked Successfully", "Confirmed", isActive = true)
                    TimelineStep("Beauty Partner Assigned", "Done", isActive = booking.status != "pending")
                    TimelineStep("Partner Commenced Journey", "On The Way", isActive = booking.status == "partner_on_the_way" || booking.status == "arrived" || booking.status == "started" || booking.status == "completed")
                    TimelineStep("Partner Arrived At Your Residence", "Arrived", isActive = booking.status == "arrived" || booking.status == "started" || booking.status == "completed")
                    TimelineStep("Doorstep Treatment In Progress", "Started", isActive = booking.status == "started" || booking.status == "completed")
                    TimelineStep("Completed & Sanitized", "Completed", isActive = booking.status == "completed")
                    
                    // REVENUE REVIEWS BOX
                    if (booking.status == "completed" && booking.reviewRating == 0) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, GlamGold.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Verified Multi-Dimension Review Form", fontWeight = FontWeight.Bold, color = GlamGold, style = MaterialTheme.typography.titleMedium)
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
                                                    tint = if (rate <= skillRating) GlamGold else Color.Gray,
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
                                                    tint = if (rate <= hygieneRating) GlamGold else Color.Gray,
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
                                                    tint = if (rate <= authenticityRating) GlamGold else Color.Gray,
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
                                    onClick = {
                                        val roundedAverage = kotlin.math.round((skillRating + hygieneRating + authenticityRating) / 3.0).toInt()
                                        val structuredComment = "[Skill: $skillRating/5, Hygiene: $hygieneRating/5, Products: $authenticityRating/5] $reviewCommentText"
                                        viewModel.submitBookingReview(booking.id, roundedAverage, structuredComment)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GlamRose),
                                    modifier = Modifier.align(Alignment.End).testTag("submit_triple_review_btn")
                                ) {
                                    Text("Submit Verified Review")
                                }
                            }
                        }
                    } else if (booking.status == "completed" && booking.reviewRating > 0) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Card {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Your Submitted Review", fontWeight = FontWeight.Bold, color = GlamGold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = GlamGold)
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
                            Text("Raise Complaint / Help Ticket")
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
                            .background(GlamRose, CircleShape)
                            .testTag("send_chat_msg_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
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

@Composable
fun Tab(selected: Boolean, onClick: () -> Unit, text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clickable { onClick() }
            .background(if (selected) GlamGold.copy(alpha = 0.2f) else Color.Transparent)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) GlamGold else Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun StateChip(status: String) {
    val color = when (status) {
        "pending" -> OrderOrange
        "accepted" -> GlamGold
        "partner_on_the_way" -> GlamRose
        "arrived" -> GlamGold
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

@Composable
fun ComplaintsListScreen(viewModel: NikhatGlowViewModel) {
    val complaints by viewModel.complaints.collectAsState()
    
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
        
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Your Support Tickets", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Button(
                    onClick = { showForm = !showForm },
                    colors = ButtonDefaults.buttonColors(containerColor = GlamGold)
                ) {
                    Text(if (showForm) "View Tickets" else "New Ticket", color = Color.Black)
                }
            }
            
            if (showForm) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Open a Help Desk Ticket", fontWeight = FontWeight.Bold, color = GlamGold)
                        
                        OutlinedTextField(
                            value = ticketSubject,
                            onValueChange = { ticketSubject = it },
                            placeholder = { Text("Subject (e.g. Booking delay, wrong kit)") },
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
                                    viewModel.submitComplaint("GG-" + (1000..9999).random(), ticketSubject, ticketDesc)
                                    ticketSubject = ""
                                    ticketDesc = ""
                                    showForm = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GlamRose),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("File Formal Ticket")
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                    items(complaints) { ticket ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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

// ---------------- PARTNER CABINET SCREENS ----------------

@Composable
fun PartnerDashboardScreen(viewModel: NikhatGlowViewModel) {
    val activeUser by viewModel.activeUser.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val scope = rememberCoroutineScope()
    
    val currentRoleKyc = activeUser?.kycStatus ?: "not_started"
    val ongoingJobs = bookings.filter { it.status != "completed" && it.status != "cancelled" && it.status != "rejected" }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(DeepPlum, DarkSlate)))
                .padding(16.dp, 24.dp)
        ) {
            Column {
                Text("PARTNER DESK", fontWeight = FontWeight.Bold, color = GlamGold)
                Text(activeUser?.name ?: "Provider", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = if (currentRoleKyc == "approved") SuccessGreen else OrderOrange)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "KYC Status: ${currentRoleKyc.uppercase()}",
                                fontWeight = FontWeight.Bold,
                                color = if (currentRoleKyc == "approved") SuccessGreen else OrderOrange
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (currentRoleKyc != "approved") {
                            Text("Aadhaar/PAN KYC is required in order to legally receive job payout transfers.", fontSize = 12.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.currentScreen = Screen.PartnerKyc },
                                colors = ButtonDefaults.buttonColors(containerColor = GlamRose),
                                modifier = Modifier.fillMaxWidth().testTag("partner_kyc_trigger")
                            ) {
                                Text("Complete Partner KYC Form")
                            }
                        } else {
                            Text("Your commercial KYC verify check is clear! You are active for real-time customer requests.", fontSize = 12.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // AVAILABILITY ENGINE & MICRO-SALON CONTROL PANEL
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, GlamGold.copy(alpha = 0.25f))
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
                                color = GlamGold,
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
                            onCheckedChange = { viewModel.isPartnerActive = it },
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
                                color = GlamGold
                            )
                        }
                        Slider(
                            value = viewModel.partnerServiceRadiusKm.toFloat(),
                            onValueChange = { viewModel.partnerServiceRadiusKm = it.toDouble() },
                            valueRange = 1f..30f,
                            colors = SliderDefaults.colors(
                                thumbColor = GlamRose,
                                activeTrackColor = GlamRose,
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
                            colors = ButtonDefaults.textButtonColors(contentColor = GlamRose)
                        ) {
                            Text("Configure")
                        }
                        
                        if (showHoursPicker) {
                            AlertDialog(
                                onDismissRequest = { showHoursPicker = false },
                                title = { Text("Select Working Hours Grid") },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        listOf(
                                            "9:00 AM - 6:00 PM (Standard)",
                                            "9:00 AM - 8:00 PM (Extended)",
                                            "10:00 AM - 9:00 PM (Late shift)",
                                            "12:00 PM - 10:00 PM (Late Night Luxe)"
                                        ).forEach { shift ->
                                            Button(
                                                onClick = {
                                                    viewModel.partnerWorkingHoursRange = shift
                                                    showHoursPicker = false
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (viewModel.partnerWorkingHoursRange == shift) GlamRose else MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = if (viewModel.partnerWorkingHoursRange == shift) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            ) {
                                                Text(shift)
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showHoursPicker = false }) {
                                        Text("Close")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Quick actions — earnings, analytics, availability, portfolio.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { viewModel.currentScreen = Screen.PartnerEarnings },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, GlamGold),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GlamGold),
                ) { Text("Earnings", fontSize = 13.sp) }
                OutlinedButton(
                    onClick = { viewModel.currentScreen = Screen.PartnerAnalytics },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, GlamGold),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GlamGold),
                ) { Text("Analytics", fontSize = 13.sp) }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { viewModel.currentScreen = Screen.PartnerAvailability },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, GlamRose),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GlamRose),
                ) { Text("Availability", fontSize = 13.sp) }
                OutlinedButton(
                    onClick = { viewModel.currentScreen = Screen.PartnerPortfolio },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, GlamRose),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GlamRose),
                ) { Text("Portfolio", fontSize = 13.sp) }
            }

            Text("JOB REQUEST QUEUE", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GlamGold)
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
                                Text("CUSTOMER APPOINTMENT", color = GlamRose, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(job.serviceName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Total Payout: ₹${job.totalPaise / 100}", fontWeight = FontWeight.Bold, color = GlamGold)
                                        Text("Address: ${job.addressText}", fontSize = 12.sp, color = Color.Gray)
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
                                            onClick = { scope.launch { viewModel.repository.acceptBooking(job.id) } },
                                            modifier = Modifier.weight(1f).testTag("accept_job_${job.id}"),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                        ) {
                                            Text("Accept Day Appointment")
                                        }
                                        Button(
                                            onClick = { scope.launch { viewModel.repository.rejectBooking(job.id) } },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                        ) {
                                            Text("Decline")
                                        }
                                    } else if (job.status == "accepted") {
                                        Button(
                                            onClick = { scope.launch { viewModel.repository.startTravel(job.id) } },
                                            modifier = Modifier.fillMaxWidth().testTag("commence_travel_${job.id}"),
                                            colors = ButtonDefaults.buttonColors(containerColor = GlamRose)
                                        ) {
                                            Text("Commence Doorstep Journey")
                                        }
                                    } else if (job.status == "partner_on_the_way") {
                                        Button(
                                            onClick = { scope.launch { viewModel.repository.arriveLocation(job.id) } },
                                            modifier = Modifier.fillMaxWidth().testTag("arrive_location_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                        ) {
                                            Text("Mark Arrived at Residence")
                                        }
                                    } else if (job.status == "arrived") {
                                        var codeInserted by remember { mutableStateOf("") }
                                        Column {
                                            OutlinedTextField(
                                                value = codeInserted,
                                                onValueChange = { codeInserted = it },
                                                placeholder = { Text("Insert client sequence OTP to start") },
                                                modifier = Modifier.fillMaxWidth().testTag("verify_otp_field")
                                            )
                                            Button(
                                                onClick = {
                                                    if (codeInserted == job.startOtp) {
                                                        scope.launch { viewModel.repository.startJob(job.id) }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().testTag("verify_otp_start_btn"),
                                                colors = ButtonDefaults.buttonColors(containerColor = GlamGold)
                                            ) {
                                                Text("Verify & Start Deep Cleansing", color = Color.Black)
                                            }
                                        }
                                    } else if (job.status == "started") {
                                        Button(
                                            onClick = { scope.launch { viewModel.repository.completeJob(job.id) } },
                                            modifier = Modifier.fillMaxWidth().testTag("complete_service_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                        ) {
                                            Text("Mark Job Completed & Sanitized")
                                        }
                                    }
                                    
                                    Button(
                                        onClick = { viewModel.currentScreen = Screen.BookingDetail(job.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                                    ) {
                                        Text("Chat Logistics")
                                    }
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
                Text("PRE-BOOKING INQUIRIES", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GlamGold)
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
                        Text("Clients comparing your Swiggy-style quotes will reach out here to ask about packaging brand and visual seal safety details.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp))
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
                        
                        val service = NikhatGlowDataSource.services.firstOrNull { it.id == serviceId } ?: return@forEach
                        val partner = NikhatGlowDataSource.partners.firstOrNull { it.id == partnerId } ?: Partner(
                            id = partnerId.ifBlank { "0" },
                            name = "Customer enquiry",
                            avatarUrl = "",
                            rating = 0f, reviewsCount = 0, distanceKm = 0.0, etaMin = 0, experienceYears = 0,
                            description = "", categories = emptyList(), servicesOffered = listOf(serviceId),
                            portfolioUrls = emptyList(), recentReviews = emptyList()
                        )
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, GlamGold.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("INCOMING CUSTOMER DISCUSSION", color = GlamGold, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp)
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
                                    colors = ButtonDefaults.buttonColors(containerColor = GlamRose)
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open Chat & Answer Client", fontSize = 13.sp)
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

@Composable
fun PartnerKycScreen(viewModel: NikhatGlowViewModel) {
    var aadhaar by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var success by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Complete Legal KYC Check", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { viewModel.currentScreen = Screen.PartnerDashboard }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Complete this legal form to get registered as an authorized service provider.", color = Color.Gray, fontSize = 13.sp)
            
            OutlinedTextField(
                value = aadhaar,
                onValueChange = { aadhaar = it },
                placeholder = { Text("Aadhaar Number (12 Digits)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("aadhaar_no_field")
            )
            OutlinedTextField(
                value = pan,
                onValueChange = { pan = it },
                placeholder = { Text("PAN Card Number") },
                modifier = Modifier.fillMaxWidth().testTag("pan_no_field")
            )
            
            // Camera simulator container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.Gray)
                    Text("Selfie Face Verification Check", fontSize = 12.sp, color = Color.Gray)
                    Text("(Simulated camera lock alignment safe)", fontSize = 10.sp, color = Color.Gray)
                }
            }
            
            Button(
                onClick = {
                    if (aadhaar.length == 12 && pan.isNotBlank()) {
                        viewModel.submitKyc(aadhaar, pan)
                        success = true
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("kyc_submit_action_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = GlamRose)
            ) {
                Text("Submit KYC File to Admin")
            }
            
            if (success) {
                Card(colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f))) {
                    Text(
                        text = "KYC documents submitted! Dynamic background verify is in progress. Check dashboard for live status.",
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PartnerServicesScreen(viewModel: NikhatGlowViewModel) {
    val activeServices by viewModel.partnerServices.collectAsState()
    
    val allServices = NikhatGlowDataSource.services

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
            items(allServices) { service ->
                val activeSetting = activeServices.firstOrNull { it.serviceId == service.id }
                var rateOverride by remember(activeSetting) { mutableStateOf((activeSetting?.pricePaise ?: service.pricePaise).toString()) }
                var productsOverride by remember(activeSetting) { mutableStateOf(activeSetting?.productsUsed ?: "Premium salon kit (L'Oreal/O3+), 100% seal-packed & verified prior to use.") }
                var activatedState by remember(activeSetting) { mutableStateOf(activeSetting?.active ?: (activeSetting != null)) }

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
                                Text("Swiggy/Zomato Style Open Pricing", fontSize = 11.sp, color = GlamGold, fontWeight = FontWeight.Bold)
                            }
                            Switch(
                                checked = activatedState,
                                onCheckedChange = {
                                    activatedState = it
                                    val finalPrice = rateOverride.toLongOrNull() ?: service.pricePaise
                                    viewModel.setPartnerServicePrice(service.id, service.name, service.categoryId, finalPrice, activatedState, productsOverride)
                                }
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Set Custom Rate: ₹ ", fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = rateOverride,
                                onValueChange = { 
                                    rateOverride = it 
                                    val finalPrice = it.toLongOrNull() ?: service.pricePaise
                                    viewModel.setPartnerServicePrice(service.id, service.name, service.categoryId, finalPrice, activatedState, productsOverride)
                                },
                                modifier = Modifier.weight(1f).height(50.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        Column {
                            Text("What products will you use & mutual seal quality notes:", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = productsOverride,
                                onValueChange = { 
                                    productsOverride = it 
                                    val finalPrice = rateOverride.toLongOrNull() ?: service.pricePaise
                                    viewModel.setPartnerServicePrice(service.id, service.name, service.categoryId, finalPrice, activatedState, it)
                                },
                                placeholder = { Text("e.g. O3+ complete sealed package, opened in front of you.") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2,
                                singleLine = false
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🔒 Customers will see your pricing & specific product promises in the marketplace comparison list.",
                                fontSize = 10.sp,
                                color = GlamGold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerProfileScreen(viewModel: NikhatGlowViewModel) {
    val activeUser by viewModel.activeUser.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
    val favorites by viewModel.favoritePartners.collectAsState()
    
    var nameState by remember { mutableStateOf(activeUser?.name ?: "") }
    var emailState by remember { mutableStateOf(activeUser?.email ?: "") }
    var showSavedNotification by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    
    // Watch activeUser to prepopulate once loaded
    LaunchedEffect(activeUser) {
        activeUser?.let {
            if (nameState.isEmpty()) nameState = it.name
            if (emailState.isEmpty()) emailState = it.email
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
                        color = GlamGold
                    )
                    Spacer(modifier = Modifier.width(48.dp)) // Equalizer
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "YOUR PROFILE",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White
                )
                Text(
                    text = "View upcoming treatments & edit your account settings",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
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
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "EDIT CONTACT DETAILS",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
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
                    
                    if (showSavedNotification) {
                        Text(
                            text = "✓ Profile details updated in server database!",
                            color = SuccessGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    
                    Button(
                        onClick = {
                            if (nameState.trim().isEmpty()) {
                                validationError = "Name field cannot be left blank."
                            } else if (!emailState.contains("@") || !emailState.contains(".")) {
                                validationError = "Please enter a valid email address."
                            } else {
                                viewModel.updateProfile(nameState.trim(), emailState.trim())
                                showSavedNotification = true
                                validationError = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_save_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Profile Changes", fontWeight = FontWeight.Bold)
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
                    border = BorderStroke(1.dp, GlamGold),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GlamGold),
                ) { Text("Dashboard", fontWeight = FontWeight.SemiBold) }
                OutlinedButton(
                    onClick = { viewModel.currentScreen = Screen.Favourites },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GlamRose),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GlamRose),
                ) { Text("Favourites", fontWeight = FontWeight.SemiBold) }
            }

            // Favorite Partners section
            val favPartnersMapped = remember(favorites) {
                NikhatGlowDataSource.partners.filter { p -> favorites.any { f -> f.partnerId == p.id } }
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
                            text = "Tap the heart icon on any expert during booking comparison to save them here.",
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
                            border = BorderStroke(1.dp, GlamGold.copy(alpha = 0.3f)),
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
                                            Icon(Icons.Default.Star, contentDescription = null, tint = GlamGold, modifier = Modifier.size(12.dp))
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
                                    colors = ButtonDefaults.buttonColors(containerColor = GlamRose),
                                    modifier = Modifier.fillMaxWidth().height(32.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Saved (Remove)", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Upcoming Appointments
            val upcoming = bookings.filter { it.status != "completed" && it.status != "cancelled" && it.status != "rejected" }
            
            Text(
                text = "UPCOMING APPOINTMENTS (${upcoming.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            if (upcoming.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No upcoming reservations scheduled.",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.currentScreen = Screen.ServiceBookingForm },
                            colors = ButtonDefaults.buttonColors(containerColor = GlamGold)
                        ) {
                            Text("Book a Treatment Now", color = Color.Black)
                        }
                    }
                }
            } else {
                upcoming.forEach { booking ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.currentScreen = Screen.BookingDetail(booking.id) }
                            .testTag("upcoming_booking_${booking.id}"),
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
                                            else -> MaterialTheme.colorScheme.secondary
                                        }
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.currentScreen = Screen.BookingDetail(booking.id) }) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "View Details", tint = GlamGold)
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
            border = BorderStroke(1.dp, GlamRose),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GlamRose),
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log out", fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun ServiceBookingFormScreen(viewModel: NikhatGlowViewModel) {
    val services = NikhatGlowDataSource.services
    
    var selectedService by remember { mutableStateOf<Service?>(null) }
    var dateState by remember { mutableStateOf("") }
    var timeState by remember { mutableStateOf("") }
    var customNotes by remember { mutableStateOf("") }
    
    var errorState by remember { mutableStateOf<String?>(null) }
    var isBookingProgress by remember { mutableStateOf(false) }
    
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
                        color = GlamGold
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
            // Section 1: Choose Service Type
            Column {
                Text(
                    text = "1. CHOOSE SERVICE TYPE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    items(services) { service ->
                        val isSelected = selectedService?.id == service.id
                        Card(
                            modifier = Modifier
                                .width(140.dp)
                                .clickable { 
                                    selectedService = service 
                                    errorState = null
                                }
                                .testTag("select_service_${service.id}"),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                AsyncImage(
                                    model = service.imageUrl,
                                    contentDescription = service.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = service.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "₹${service.pricePaise / 100}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Black
                                )
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
                
                OutlinedTextField(
                    value = dateState,
                    onValueChange = { 
                        dateState = it 
                        errorState = null
                    },
                    label = { Text("Appointed Date") },
                    placeholder = { Text("e.g. 25 June 2026") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = GlamGold) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("booking_date_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = timeState,
                    onValueChange = { 
                        timeState = it 
                        errorState = null
                    },
                    label = { Text("Preferred Time Slot") },
                    placeholder = { Text("e.g. 11:00 AM") },
                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = GlamGold) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("booking_time_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
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
                    if (selectedService == null) {
                        errorState = "Please select a wellness treatment service above."
                    } else if (dateState.trim().isEmpty()) {
                        errorState = "Appointment date cannot be left blank."
                    } else if (timeState.trim().isEmpty()) {
                        errorState = "Appointment preference slot time cannot be left blank."
                    } else {
                        isBookingProgress = true
                        viewModel.bookDirectlyFromForm(
                            service = selectedService!!,
                            slot = "${dateState.trim()} at ${timeState.trim()}",
                            onSuccess = { bookingId ->
                                isBookingProgress = false
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
                        color = Color.White
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun PreBookingChatScreen(viewModel: NikhatGlowViewModel, service: Service, partner: Partner) {
    val activeUser by viewModel.activeUser.collectAsState()
    val preBookingId = "pre_${partner.id}_${service.id}"
    val messages by viewModel.getMessagesForBooking(preBookingId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var chatText by remember { mutableStateOf("") }
    
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
                    Text(text = "Pre-Booking Direct Line: ${service.name}", fontSize = 11.sp, color = GlamGold, fontWeight = FontWeight.SemiBold)
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
                    color = Color(0xFFE3F2FD),
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
                            tint = Color(0xFF1E88E5),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "SEAL SECURE",
                            color = Color(0xFF1E88E5),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepPlum, titleContentColor = Color.White)
        )

        // Trust alert header about Swiggy open pricing and seal packs
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
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GlamGold, modifier = Modifier.size(20.dp))
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
            if (messages.isEmpty()) {
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
                items(messages) { msg ->
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
                                    color = if (isMe) MaterialTheme.colorScheme.onPrimary else GlamGold
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
            color = GlamGold,
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
                        viewModel.sendChatMessage(preBookingId, activeUser?.role ?: "customer", question)
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
                        viewModel.sendChatMessage(preBookingId, activeUser?.role ?: "customer", chatText)
                        chatText = ""
                    }
                },
                modifier = Modifier
                    .background(GlamRose, CircleShape)
                    .size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message", tint = Color.White)
            }
        }
    }
}

