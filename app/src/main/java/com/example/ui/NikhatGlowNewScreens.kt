@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.NikhatGlowDataSource
import com.example.data.Partner
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.DeepPlum
import com.example.ui.theme.GlamGold
import com.example.ui.theme.GlamRose

// ───────────────────────────── small shared header ──────────────────────────
@Composable
private fun NikhatHeader(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(DeepPlum, DarkSlate)))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f))
                trailing?.invoke()
            }
            subtitle?.let {
                Text(it, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp, top = 2.dp))
            }
        }
    }
}

private fun rupees(paise: Long): String = "₹${paise / 100}"

// ───────────────────────────── FAVOURITES (customer) ────────────────────────
@Composable
fun FavouritesScreen(viewModel: NikhatGlowViewModel) {
    val favorites by viewModel.favoritePartners.collectAsState()
    // partners cache is filled from the backend; match by id.
    val favPartners = remember(favorites, NikhatGlowDataSource.partners) {
        NikhatGlowDataSource.partners.filter { p -> favorites.any { f -> f.partnerId == p.id } }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NikhatHeader(
            title = "Favourites",
            subtitle = "Professionals you saved",
            onBack = { viewModel.currentScreen = Screen.CustomerProfile },
        )
        if (favorites.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                Spacer(Modifier.height(12.dp))
                Text("No favourites yet", fontWeight = FontWeight.Bold)
                Text(
                    "Tap the heart on a professional to save them here.",
                    fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center
                )
            }
            return
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(favPartners) { partner ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.currentScreen = Screen.PartnerReviews(partner) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (partner.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = partner.avatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(48.dp).clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(GlamRose.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) { Text(partner.name.take(1).uppercase(), color = GlamRose, fontWeight = FontWeight.Bold) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(partner.name, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = GlamGold, modifier = Modifier.size(14.dp))
                                Text(" ${partner.rating} · ${partner.reviewsCount} reviews", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        IconButton(onClick = { viewModel.toggleFavorite(partner.id) }) {
                            Icon(Icons.Default.Favorite, contentDescription = "Remove", tint = GlamRose)
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────────── PARTNER REVIEWS (customer browse) ────────────
@Composable
fun PartnerReviewsScreen(viewModel: NikhatGlowViewModel, partner: Partner) {
    val reviews by viewModel.partnerReviews.collectAsState()
    LaunchedEffect(partner.id) { viewModel.loadPartnerReviews(partner.id) }

    Column(modifier = Modifier.fillMaxSize()) {
        NikhatHeader(
            title = partner.name,
            subtitle = "Rating ${partner.rating} · ${partner.reviewsCount} reviews",
            onBack = { viewModel.currentScreen = Screen.Favourites },
        )
        if (reviews.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                Spacer(Modifier.height(12.dp))
                Text("No reviews yet", fontWeight = FontWeight.Bold)
            }
            return
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(reviews) { r ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) { i ->
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (i < r.rating) GlamGold else Color.Gray.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Text((r.createdAt ?: "").take(10), fontSize = 11.sp, color = Color.Gray)
                        }
                        if (!r.comment.isNullOrBlank()) {
                            Text(r.comment!!, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────────── CUSTOMER DASHBOARD ───────────────────────────
@Composable
fun CustomerDashboardScreen(viewModel: NikhatGlowViewModel) {
    val bookings by viewModel.bookings.collectAsState()
    val favorites by viewModel.favoritePartners.collectAsState()

    val total = bookings.size
    val completed = bookings.count { it.status == "completed" }
    val recent = bookings.take(5)

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        NikhatHeader(
            title = "My Dashboard",
            subtitle = "Your activity at a glance",
            onBack = { viewModel.currentScreen = Screen.CustomerProfile },
        )
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Bookings", total.toString(), Modifier.weight(1f))
                StatTile("Completed", completed.toString(), Modifier.weight(1f))
                StatTile("Favourites", favorites.size.toString(), Modifier.weight(1f))
            }
            Text("Recent bookings", fontWeight = FontWeight.Bold, color = GlamGold)
            if (recent.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text("No bookings yet.", modifier = Modifier.padding(16.dp), color = Color.Gray)
                }
            } else {
                recent.forEach { b ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.currentScreen = Screen.BookingDetail(b.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(b.serviceName, fontWeight = FontWeight.Bold)
                                Text("${b.partnerName} · ${b.dateTimeSlot}", fontSize = 12.sp, color = Color.Gray)
                            }
                            Text(b.status.replace("_", " ").uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GlamRose)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, GlamGold.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = GlamGold)
            Text(label, fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}

// ───────────────────────────── PARTNER AVAILABILITY ─────────────────────────
private val DOW_LABELS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
// JS day-of-week index for each label above (Mon=1 .. Sat=6, Sun=0).
private val DOW_JS = listOf(1, 2, 3, 4, 5, 6, 0)

@Composable
fun PartnerAvailabilityScreen(viewModel: NikhatGlowViewModel) {
    val avail by viewModel.availability.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadAvailability() }

    var start by remember { mutableStateOf("09:00") }
    var end by remember { mutableStateOf("20:00") }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6)) } // JS dow
    var leaves by remember { mutableStateOf(listOf<String>()) }
    var newLeave by remember { mutableStateOf("") }

    // Hydrate local editor when server data arrives.
    LaunchedEffect(avail) {
        avail?.let { a ->
            a.workingHours?.start?.let { start = it }
            a.workingHours?.end?.let { end = it }
            if (a.days.isNotEmpty()) selectedDays = a.days.toSet()
            leaves = a.leaves
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        NikhatHeader(
            title = "Availability",
            subtitle = "Working hours, days & leaves",
            onBack = { viewModel.currentScreen = Screen.PartnerProfile },
        )
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Working hours (24h, e.g. 09:00)", fontWeight = FontWeight.Bold, color = GlamGold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = start, onValueChange = { start = it },
                            label = { Text("Start") }, singleLine = true, modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = end, onValueChange = { end = it },
                            label = { Text("End") }, singleLine = true, modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Working days", fontWeight = FontWeight.Bold, color = GlamGold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DOW_LABELS.forEachIndexed { idx, label ->
                            val js = DOW_JS[idx]
                            val on = selectedDays.contains(js)
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (on) GlamRose else Color.Gray.copy(alpha = 0.15f))
                                    .clickable {
                                        selectedDays = if (on) selectedDays - js else selectedDays + js
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label.take(1),
                                    color = if (on) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Leave dates (ISO, e.g. 2026-07-01)", fontWeight = FontWeight.Bold, color = GlamGold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newLeave, onValueChange = { newLeave = it },
                            label = { Text("YYYY-MM-DD") }, singleLine = true, modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            val v = newLeave.trim()
                            if (v.isNotBlank() && !leaves.contains(v)) {
                                leaves = leaves + v
                                newLeave = ""
                            }
                        }) { Icon(Icons.Default.Add, contentDescription = "Add leave", tint = GlamRose) }
                    }
                    leaves.forEach { d ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = GlamGold, modifier = Modifier.size(18.dp))
                            Text("  $d", modifier = Modifier.weight(1f))
                            IconButton(onClick = { leaves = leaves - d }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Gray)
                            }
                        }
                    }
                }
            }

            viewModel.availabilityError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
            if (viewModel.availabilitySaved) {
                Text("✓ Availability saved.", color = GlamGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    viewModel.saveAvailability(start.trim(), end.trim(), selectedDays.sorted(), leaves)
                },
                enabled = !viewModel.availabilityBusy,
                colors = ButtonDefaults.buttonColors(containerColor = GlamRose),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text(if (viewModel.availabilityBusy) "Saving…" else "Save Availability", fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ───────────────────────────── PARTNER EARNINGS ─────────────────────────────
@Composable
fun PartnerEarningsScreen(viewModel: NikhatGlowViewModel) {
    val earnings by viewModel.earnings.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadEarnings() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        NikhatHeader(
            title = "Earnings",
            subtitle = "You collect payment directly from customers",
            onBack = { viewModel.currentScreen = Screen.PartnerDashboard },
        )
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val e = earnings
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Today", rupees(e?.todayPaise ?: 0), Modifier.weight(1f))
                StatTile("This week", rupees(e?.weekPaise ?: 0), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("This month", rupees(e?.monthPaise ?: 0), Modifier.weight(1f))
                StatTile("Lifetime", rupees(e?.lifetimePaise ?: 0), Modifier.weight(1f))
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, GlamGold.copy(alpha = 0.25f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = GlamGold)
                    Spacer(Modifier.width(12.dp))
                    Text("Completed jobs", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text("${e?.completedJobs ?: 0}", fontWeight = FontWeight.Bold, color = GlamGold)
                }
            }

            Text("Recent completed jobs", fontWeight = FontWeight.Bold, color = GlamGold)
            val recent = e?.recent ?: emptyList()
            if (recent.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text("No completed jobs yet.", modifier = Modifier.padding(16.dp), color = Color.Gray)
                }
            } else {
                recent.forEach { r ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GlamRose, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Booking #${r.bookingId}", fontWeight = FontWeight.SemiBold)
                                Text((r.at ?: "").take(10), fontSize = 12.sp, color = Color.Gray)
                            }
                            Text(rupees(r.totalPaise), fontWeight = FontWeight.Bold, color = GlamGold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ───────────────────────────── PARTNER ANALYTICS ────────────────────────────
@Composable
fun PartnerAnalyticsScreen(viewModel: NikhatGlowViewModel) {
    val analytics by viewModel.analytics.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadAnalytics() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        NikhatHeader(
            title = "Analytics",
            subtitle = "Performance insights",
            onBack = { viewModel.currentScreen = Screen.PartnerDashboard },
        )
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val a = analytics
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Accept rate", "${((a?.acceptRate ?: 0f) * 100).toInt()}%", Modifier.weight(1f))
                StatTile("Avg response", "${(a?.avgResponseMin ?: 0f).toInt()}m", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Rating", "${a?.ratingAvg ?: 0f}", Modifier.weight(1f))
                StatTile("Reviews", "${a?.ratingCount ?: 0}", Modifier.weight(1f))
            }

            // Profile views
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, GlamGold.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = GlamGold)
                        Spacer(Modifier.width(8.dp))
                        Text("Profile views", fontWeight = FontWeight.Bold)
                    }
                    Text("Total: ${a?.profileViewsTotal ?: 0}  ·  Last 30d: ${a?.profileViews30d ?: 0}", fontSize = 13.sp, color = Color.Gray)
                    val trend = a?.profileViewsTrend ?: emptyList()
                    if (trend.isNotEmpty()) {
                        val maxViews = (trend.maxOfOrNull { it.views } ?: 1).coerceAtLeast(1)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            trend.takeLast(14).forEach { pt ->
                                val frac = pt.views.toFloat() / maxViews
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(frac.coerceIn(0.04f, 1f))
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                        .background(GlamRose)
                                )
                            }
                        }
                    }
                }
            }

            // Rating distribution
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = GlamGold)
                        Spacer(Modifier.width(8.dp))
                        Text("Rating distribution", fontWeight = FontWeight.Bold)
                    }
                    val dist = a?.ratingDistribution ?: emptyMap()
                    val maxCount = (dist.values.maxOrNull() ?: 1).coerceAtLeast(1)
                    (5 downTo 1).forEach { star ->
                        val count = dist[star.toString()] ?: 0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$star", modifier = Modifier.width(16.dp), fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.Star, contentDescription = null, tint = GlamGold, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Color.Gray.copy(alpha = 0.15f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(count.toFloat() / maxCount)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(GlamGold)
                                )
                            }
                            Text("  $count", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // Booking funnel
            val f = a?.funnel
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Booking funnel", fontWeight = FontWeight.Bold, color = GlamGold)
                    FunnelRow("Pending", f?.pending ?: 0)
                    FunnelRow("Accepted", f?.accepted ?: 0)
                    FunnelRow("Completed", f?.completed ?: 0)
                    FunnelRow("Cancelled", f?.cancelled ?: 0)
                    FunnelRow("Rejected", f?.rejected ?: 0)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FunnelRow(label: String, count: Int) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Text("$count", fontWeight = FontWeight.Bold, color = GlamGold)
    }
}

// ───────────────────────────── PARTNER PORTFOLIO ────────────────────────────
@Composable
fun PartnerPortfolioScreen(viewModel: NikhatGlowViewModel) {
    val items by viewModel.portfolio.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadPortfolio() }

    var showAdd by remember { mutableStateOf(false) }
    var uploadId by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        NikhatHeader(
            title = "Portfolio",
            subtitle = "Showcase your work",
            onBack = { viewModel.currentScreen = Screen.PartnerProfile },
            trailing = {
                IconButton(onClick = { showAdd = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                }
            }
        )
        if (items.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No portfolio items yet", fontWeight = FontWeight.Bold)
                Text("Tap + to add a photo of your work.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items) { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column {
                            if (!item.imageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = item.caption,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().height(180.dp)
                                )
                            }
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(item.caption ?: "Untitled", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                IconButton(onClick = { viewModel.deletePortfolioItem(item.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add portfolio item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uploadId, onValueChange = { uploadId = it },
                        label = { Text("Upload id") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = imageUrl, onValueChange = { imageUrl = it },
                        label = { Text("Image URL (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = caption, onValueChange = { caption = it },
                        label = { Text("Caption") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    viewModel.portfolioError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !viewModel.portfolioBusy,
                    onClick = {
                        viewModel.addPortfolioItem(uploadId, imageUrl, caption)
                        uploadId = ""; imageUrl = ""; caption = ""
                        showAdd = false
                    }
                ) { Text(if (viewModel.portfolioBusy) "Adding…" else "Add", color = GlamRose) }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("Cancel") }
            }
        )
    }
}
