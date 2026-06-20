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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.NikhatGlowDataSource
import com.example.data.Partner
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.DeepPlum
import com.example.ui.theme.NikhatGold
import com.example.ui.theme.NikhatRose
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.OrderOrange
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

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
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(NikhatRose.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) { Text(partner.name.take(1).uppercase(), color = NikhatRose, fontWeight = FontWeight.Bold) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(partner.name, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = NikhatGold, modifier = Modifier.size(14.dp))
                                Text(" ${partner.rating} · ${partner.reviewsCount} reviews", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        IconButton(onClick = { viewModel.toggleFavorite(partner.id) }) {
                            Icon(Icons.Default.Favorite, contentDescription = "Remove", tint = NikhatRose)
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
                                    tint = if (i < r.rating) NikhatRose else Color.Gray.copy(alpha = 0.4f),
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
            Text("Recent bookings", fontWeight = FontWeight.Bold, color = NikhatRose)
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
                            Text(b.status.replace("_", " ").uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NikhatRose)
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
        border = BorderStroke(1.dp, NikhatRose.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = NikhatRose)
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
                    Text("Working hours (24h, e.g. 09:00)", fontWeight = FontWeight.Bold, color = NikhatRose)
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
                    Text("Working days", fontWeight = FontWeight.Bold, color = NikhatRose)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DOW_LABELS.forEachIndexed { idx, label ->
                            val js = DOW_JS[idx]
                            val on = selectedDays.contains(js)
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (on) NikhatRose else Color.Gray.copy(alpha = 0.15f))
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

            // Visual Interactive Calendar Blockout Widget
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, NikhatRose.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Interactive Calendar Blockouts 🗓️", fontWeight = FontWeight.Bold, color = NikhatRose, style = MaterialTheme.typography.titleMedium)
                    Text("Tap any date node to visually declare a full-day leave (turns ORANGE, turning you off-duty). Tap again to reactive availability.", fontSize = 11.sp, color = Color.Gray)
                    
                    val today = remember { java.time.LocalDate.now() }
                    val currentYearMonth = remember { java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy").format(today) }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(currentYearMonth, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        AssistChip(
                            onClick = {},
                            label = { Text("Active Month", fontSize = 11.sp) }
                        )
                    }

                    // Weekday Headers
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { d ->
                            Text(
                                text = d,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    // Month Days Grid (determines day alignment offsets dynamically)
                    val firstDayOffset = remember {
                        today.withDayOfMonth(1).dayOfWeek.value % 7 // offset: 0=Sun, 1=Mon...
                    }
                    val daysInMonth = remember { today.lengthOfMonth() }
                    val totalCells = firstDayOffset + daysInMonth
                    val totalRows = (totalCells + 6) / 7
                    
                    for (rowIdx in 0 until totalRows) {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            for (colIdx in 0 until 7) {
                                val cellIdx = rowIdx * 7 + colIdx
                                val dayNum = cellIdx - firstDayOffset + 1
                                if (dayNum in 1..daysInMonth) {
                                    val localDate = today.withDayOfMonth(dayNum)
                                    val dateIso = localDate.toString()
                                    val isBlocked = leaves.contains(dateIso)
                                    val jsDow = if (localDate.dayOfWeek.value == 7) 0 else localDate.dayOfWeek.value
                                    val isCurrentWorkingDay = selectedDays.contains(jsDow)
                                    
                                    val bgColor = when {
                                        isBlocked -> OrderOrange // Blocked/Out-of-Office
                                        !isCurrentWorkingDay -> Color.Gray.copy(alpha = 0.12f) // Off day by default schedule
                                        else -> NikhatRose.copy(alpha = 0.22f) // Clear/Scheduled
                                    }
                                    
                                    val borderStroke = if (localDate == today) BorderStroke(1.5.dp, Color.White) else null
                                    
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(2.dp)
                                            .aspectRatio(1f),
                                        shape = CircleShape,
                                        color = bgColor,
                                        border = borderStroke,
                                        onClick = {
                                            leaves = if (isBlocked) {
                                                leaves - dateIso
                                            } else {
                                                leaves + dateIso
                                            }
                                        }
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = dayNum.toString(),
                                                color = if (isBlocked) Color.White else if (!isCurrentWorkingDay) Color.Gray else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            if (isBlocked) {
                                                Box(modifier = Modifier.size(4.dp).background(Color.White, CircleShape))
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Leave List & Manual Entry Card
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Manual Blockout (Optional overrides)", fontWeight = FontWeight.Bold, color = NikhatRose)
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
                        }) { Icon(Icons.Default.Add, contentDescription = "Add leave", tint = NikhatRose) }
                    }
                    leaves.sorted().forEach { d ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = NikhatRose, modifier = Modifier.size(18.dp))
                            Text("  $d (Blocked)", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = OrderOrange)
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
                Text("✓ Availability saved.", color = NikhatRose, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    viewModel.saveAvailability(start.trim(), end.trim(), selectedDays.sorted(), leaves)
                },
                enabled = !viewModel.availabilityBusy,
                colors = ButtonDefaults.buttonColors(containerColor = NikhatRose),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text(if (viewModel.availabilityBusy) "Saving…" else "Save Availability", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
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
                border = BorderStroke(1.dp, NikhatRose.copy(alpha = 0.25f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = NikhatRose)
                    Spacer(Modifier.width(12.dp))
                    Text("Completed jobs", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text("${e?.completedJobs ?: 0}", fontWeight = FontWeight.Bold, color = NikhatRose)
                }
            }

            Text("Recent completed jobs", fontWeight = FontWeight.Bold, color = NikhatRose)
            val recent = e?.recent ?: emptyList()
            if (recent.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text("No completed jobs yet.", modifier = Modifier.padding(16.dp), color = Color.Gray)
                }
            } else {
                recent.forEach { r ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NikhatRose, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Booking #${r.bookingId}", fontWeight = FontWeight.SemiBold)
                                Text((r.at ?: "").take(10), fontSize = 12.sp, color = Color.Gray)
                            }
                            Text(rupees(r.totalPaise), fontWeight = FontWeight.Bold, color = NikhatRose)
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
                border = BorderStroke(1.dp, NikhatRose.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = NikhatRose)
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
                                        .background(NikhatRose)
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
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = NikhatRose)
                        Spacer(Modifier.width(8.dp))
                        Text("Rating distribution", fontWeight = FontWeight.Bold)
                    }
                    val dist = a?.ratingDistribution ?: emptyMap()
                    val maxCount = (dist.values.maxOrNull() ?: 1).coerceAtLeast(1)
                    (5 downTo 1).forEach { star ->
                        val count = dist[star.toString()] ?: 0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$star", modifier = Modifier.width(16.dp), fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.Star, contentDescription = null, tint = NikhatGold, modifier = Modifier.size(14.dp))
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
                                        .background(NikhatRose)
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
                    Text("Booking funnel", fontWeight = FontWeight.Bold, color = NikhatRose)
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
        Text("$count", fontWeight = FontWeight.Bold, color = NikhatRose)
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
                    Text("Or tap an AI work placeholder preset to pre-fill instantly ✨", fontSize = 11.sp, color = Color.Gray)
                    
                    val presets = listOf(
                        Triple("Bridal Glow 🌸", "https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?q=80&w=600&auto=format&fit=crop", "Exquisite Royal Bridal GLOW Makeup"),
                        Triple("Organic Facial 💆", "https://images.unsplash.com/photo-1512290923902-8a9f81dc236c?q=80&w=600&auto=format&fit=crop", "Luxurious Organic Flower Mist Facial Therapy"),
                        Triple("Balayage Hair 💇", "https://images.unsplash.com/photo-1562322140-8baeececf3df?q=80&w=600&auto=format&fit=crop", "Chic Balayage Hair Highlights Showcase"),
                        Triple("Luxe Pedicure 💅", "https://images.unsplash.com/photo-1604654894610-df63bc536371?q=80&w=600&auto=format&fit=crop", "Premium Luxe Gel Pedicure Styling"),
                        Triple("Glow Therapy 🌿", "https://images.unsplash.com/photo-1556228720-195a672e8a03?q=80&w=600&auto=format&fit=crop", "Detox Glow Skin tightening Therapy Work")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { (label, url, cap) ->
                            val isChosen = imageUrl == url
                            FilterChip(
                                selected = isChosen,
                                onClick = {
                                    imageUrl = url
                                    caption = cap
                                    uploadId = "ai_" + System.currentTimeMillis().toString().takeLast(6)
                                },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NikhatRose.copy(alpha = 0.25f),
                                    selectedLabelColor = NikhatRose
                                )
                            )
                        }
                    }
                    Divider(color = Color.Gray.copy(alpha = 0.15f))

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
                ) { Text(if (viewModel.portfolioBusy) "Adding…" else "Add", color = NikhatRose) }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("Cancel") }
            }
        )
    }
}

// ───────────────────────────── PARTNER STOREFRONT & MENU (restaurant-style) ────────────────────────
@Composable
fun PartnerStoreScreen(viewModel: NikhatGlowViewModel, partner: Partner) {
    val cart by viewModel.cart.collectAsState()
    val allServices = NikhatGlowDataSource.services
    var selectedCategory by remember { mutableStateOf("All") }

    val reviews by viewModel.partnerReviews.collectAsState()
    LaunchedEffect(partner.id) {
        viewModel.loadPartnerReviews(partner.id)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(partner.name, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { viewModel.goBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Hero Profile Card Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(DeepPlum, DarkSlate)))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = partner.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=200" },
                            contentDescription = partner.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = partner.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (partner.kycStatus == "approved") {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified",
                                        tint = NikhatGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (partner.kycStatus == "approved") "Verified ✓" else "Not yet verified",
                                color = if (partner.kycStatus == "approved") NikhatGold else Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "⭐ ${partner.rating} (${partner.reviewsCount} jobs completed)",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${partner.experienceYears} Years Experience • Independent Expert",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                            // §701 — certifications & languages (skip silently if empty).
                            if (partner.certifications.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Certifications: ${partner.certifications.joinToString(", ")}",
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                            if (partner.languages.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Languages: ${partner.languages.joinToString(", ")}",
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }

            // Quick Info & Chat Button
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ABOUT THE PROFESSIONAL:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NikhatRose,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = partner.description.ifBlank { "No description added yet" },
                        fontSize = 13.sp,
                        color = Color.LightGray,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Chat trigger — only against a real service the partner offers.
                    Button(
                        onClick = {
                            val service = allServices.firstOrNull()
                            if (service != null) {
                                viewModel.currentScreen = Screen.PreBookingChat(service, partner)
                            }
                        },
                        enabled = allServices.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = NikhatRose, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        val firstName = partner.name.substringBefore(" ")
                        Text("Chat with $firstName Pre-Booking", color = NikhatRose, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Divider(color = Color.Gray.copy(alpha = 0.15f))
            }

            // Mock service area coverage map
            item {
                PartnerCoverageMap(partner = partner)
                Divider(color = Color.Gray.copy(alpha = 0.15f))
            }

            // Real Client Satisfaction & Verified Reviews Subsection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "CLIENT FEEDBACK & SATISFACTION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NikhatRose,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = NikhatGold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${partner.rating} Stars",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = " (${reviews.size} reviews)",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            val ratingKycApproved = partner.kycStatus == "approved"
                            Box(
                                modifier = Modifier
                                    .background(
                                        (if (ratingKycApproved) NikhatRose else Color.Gray).copy(alpha = 0.12f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    if (ratingKycApproved) "VERIFIED RATING" else "RATING",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ratingKycApproved) NikhatRose else Color.Gray
                                )
                            }
                        }
                        
                        if (reviews.isEmpty()) {
                            Text(
                                "No prior treatment reviews verified for this independent professional yet. Hire them and leave the first star rating!",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                lineHeight = 15.sp
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                reviews.take(5).forEach { r ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Row {
                                                    repeat(5) { i ->
                                                        Icon(
                                                            Icons.Default.Star,
                                                            contentDescription = null,
                                                            tint = if (i < r.rating) NikhatGold else Color.Gray.copy(alpha = 0.3f),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.weight(1f))
                                                Text(
                                                    text = (r.createdAt ?: "").take(10),
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            if (!r.comment.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = r.comment ?: "",
                                                    fontSize = 11.sp,
                                                    color = Color.White,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Divider(color = Color.Gray.copy(alpha = 0.15f))
            }

            // Category Selector Filter Chips
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = "SELECT SERVICES MENU:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("All", "Salon", "Beauty", "Makeup", "Massage").forEach { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NikhatRose.copy(alpha = 0.25f),
                                    selectedLabelColor = NikhatRose,
                                    labelColor = Color.Gray
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Menu Items List
            val filteredServices = allServices.filter { srv ->
                if (selectedCategory == "All") true
                else srv.categoryId.lowercase().contains(selectedCategory.lowercase()) ||
                     srv.name.lowercase().contains(selectedCategory.lowercase())
            }

            if (filteredServices.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No services matches this category selection", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                items(filteredServices) { service ->
                    // Resolve partner custom rate
                    val resolvedPrice = if (partner.fromPricePaise > 0) partner.fromPricePaise else service.pricePaise
                    
                    // Check if this item is in the cart
                    val cartItem = cart?.items?.firstOrNull { 
                        it.serviceId.toString() == service.id || it.serviceId == service.id.toIntOrNull() 
                    }
                    val isSamePartnerCart = cart?.partnerId == partner.id.toIntOrNull() || cart?.partnerId.toString() == partner.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = service.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "₹${resolvedPrice / 100}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "⏱️ ${service.durationMin} mins",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = service.description,
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    lineHeight = 15.sp,
                                    maxLines = 2
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            // Right side: image + add controls
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                AsyncImage(
                                    model = service.imageUrl,
                                    contentDescription = service.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Gray.copy(alpha = 0.1f))
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                if (cartItem != null && isSamePartnerCart) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .background(NikhatRose, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "-",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            modifier = Modifier
                                                .clickable {
                                                    if (cartItem.qty <= 1) {
                                                        viewModel.removeCartItem(cartItem.id)
                                                    } else {
                                                        viewModel.updateCartQty(cartItem.id, cartItem.qty - 1)
                                                    }
                                                }
                                                .padding(horizontal = 6.dp)
                                        )
                                        Text(
                                            text = "${cartItem.qty}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                        Text(
                                            text = "+",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            modifier = Modifier
                                                .clickable {
                                                    viewModel.updateCartQty(cartItem.id, cartItem.qty + 1)
                                                }
                                                .padding(horizontal = 6.dp)
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.addToCart(partner.id, service.id)
                                        },
                                        modifier = Modifier
                                            .width(72.dp)
                                            .height(28.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        border = BorderStroke(1.dp, NikhatRose),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NikhatRose)
                                    ) {
                                        Text("ADD", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Checkout Cart float footer bar
        val hasCartItems = cart != null && cart!!.items.isNotEmpty() &&
                (cart!!.partnerId.toString() == partner.id || cart!!.partnerId == partner.id.toIntOrNull())
                
        if (hasCartItems) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.currentScreen = Screen.Cart
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NikhatRose),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "🛒 ${cart!!.count} items in basket",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "Subtotal: Rs ${cart!!.subtotalPaise / 100}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "View Cart & Book ➔",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PartnerCoverageMap(partner: Partner) {
    var pinQuery by remember { mutableStateOf("") }
    var checkingCoverage by remember { mutableStateOf(false) }
    var coverageResult by remember { mutableStateOf<String?>(null) }
    var isWithinRange by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("coverage_map_card_${partner.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Content
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = NikhatRose,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "SERVICE AREA COVERAGE MAP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NikhatRose,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${partner.name} covers a 15 km doorstep logistics radius around their active service salon hub.",
                fontSize = 11.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Realistic Stylized Canvas Map Illustration
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSlate)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Draw street grid lines (schematic map background)
                    val gridColor = Color.White.copy(alpha = 0.05f)
                    val streetColor = Color.White.copy(alpha = 0.08f)

                    // Draw grid/terrain lines
                    for (i in 0..10) {
                        val x = (width / 10) * i
                        drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, height), strokeWidth = 1f)
                        val y = (height / 10) * i
                        drawLine(color = gridColor, start = Offset(0f, y), end = Offset(width, y), strokeWidth = 1f)
                    }

                    // Draw mock main streets (crossings)
                    drawLine(color = streetColor, start = Offset(0f, height * 0.3f), end = Offset(width, height * 0.3f), strokeWidth = 8f)
                    drawLine(color = streetColor, start = Offset(0f, height * 0.75f), end = Offset(width, height * 0.75f), strokeWidth = 12f)
                    drawLine(color = streetColor, start = Offset(width * 0.4f, 0f), end = Offset(width * 0.4f, height), strokeWidth = 10f)
                    drawLine(color = streetColor, start = Offset(width * 0.8f, 0f), end = Offset(width * 0.8f, height), strokeWidth = 6f)

                    // Draw service radius circle (centered at width*0.4, height*0.5)
                    val centerX = width * 0.4f
                    val centerY = height * 0.5f
                    val radius = width * 0.35f

                    // Draw service area zone
                    drawCircle(
                        color = NikhatRose.copy(alpha = 0.12f),
                        radius = radius,
                        center = Offset(centerX, centerY)
                    )
                    drawCircle(
                        color = NikhatRose.copy(alpha = 0.4f),
                        radius = radius,
                        center = Offset(centerX, centerY),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )

                    // Draw pulsing beacon of the Partner center
                    drawCircle(
                        color = Color.White,
                        radius = 16f,
                        center = Offset(centerX, centerY)
                    )
                    drawCircle(
                        color = NikhatRose,
                        radius = 8f,
                        center = Offset(centerX, centerY)
                    )

                    // Draw customer indicator (represented dynamically as ~2.5km distance)
                    val customerX = centerX + radius * 0.6f
                    val customerY = centerY - radius * 0.4f
                    drawCircle(
                        color = SuccessGreen.copy(alpha = 0.2f),
                        radius = 24f,
                        center = Offset(customerX, customerY)
                    )
                    drawCircle(
                        color = Color(0xFF2ECC71),
                        radius = 6f,
                        center = Offset(customerX, customerY)
                    )
                }

                // Small Map Floating Overlays
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text("Coverage Zone: 15 km Radius", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Text("Hub Pin: Active ✔", fontSize = 9.sp, color = NikhatRose, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Address Check Input Field
            Text(
                text = "CHECK COURIER/TRANSIT ELIGIBILITY:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = pinQuery,
                    onValueChange = {
                        pinQuery = it
                        coverageResult = null
                    },
                    placeholder = { Text("Enter post code / locality...", fontSize = 12.sp, color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NikhatRose,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                        focusedContainerColor = Color.Black.copy(alpha = 0.15f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("coverage_pin_input"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, color = Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )

                Button(
                    onClick = {
                        if (pinQuery.isNotBlank()) {
                            checkingCoverage = true
                            scope.launch {
                                kotlinx.coroutines.delay(1000)
                                checkingCoverage = false
                                val sanitized = pinQuery.trim().lowercase()
                                if (sanitized.contains("out") || sanitized.contains("999") || sanitized.contains("away") || sanitized.startsWith("0")) {
                                    coverageResult = "Out of premium transit boundaries! Our specialist can only cover up to 15km."
                                    isWithinRange = false
                                } else {
                                    coverageResult = "Within coverage zone! Direct checkout is active. (${partner.name} is ${partner.distanceKm} km away from you). ✅"
                                    isWithinRange = true
                                }
                            }
                        }
                    },
                    enabled = pinQuery.isNotBlank() && !checkingCoverage,
                    colors = ButtonDefaults.buttonColors(containerColor = NikhatRose),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("coverage_check_btn")
                ) {
                    if (checkingCoverage) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Check 📡", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Interactive results output
            coverageResult?.let { result ->
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isWithinRange) SuccessGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isWithinRange) SuccessGreen.copy(alpha = 0.3f) else MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isWithinRange) SuccessGreen else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = result,
                                fontSize = 11.sp,
                                color = if (isWithinRange) Color.White else MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
