@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Place
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
import com.example.data.VedaDropDataSource
import com.example.data.Partner
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.DeepPlum
import com.example.ui.theme.VedaDropGold
import com.example.ui.theme.VedaDropRose
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
private fun VedaDropHeader(
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
fun FavouritesScreen(viewModel: VedaDropViewModel) {
    val favorites by viewModel.favoritePartners.collectAsState()
    // partners cache is filled from the backend; match by id.
    val favPartners = remember(favorites, VedaDropDataSource.partners) {
        VedaDropDataSource.partners.filter { p -> favorites.any { f -> f.partnerId == p.id } }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        VedaDropHeader(
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
            items(favPartners, key = { it.id }) { partner ->
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
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(VedaDropRose.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) { Text(partner.name.take(1).uppercase(), color = VedaDropRose, fontWeight = FontWeight.Bold) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(partner.name, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = VedaDropGold, modifier = Modifier.size(14.dp))
                                Text(" ${partner.rating} · ${partner.reviewsCount} reviews", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        // §729 (parity C2) — BOOK AGAIN from favourites: jump straight into
                        // the partner store (their menu) pre-filled with this saved pro.
                        Button(
                            onClick = { viewModel.currentScreen = Screen.PartnerStore(partner) },
                            colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp).testTag("fav_book_again_${partner.id}"),
                        ) {
                            Text("Book", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                        }
                        IconButton(onClick = { viewModel.toggleFavorite(partner.id) }) {
                            Icon(Icons.Default.Favorite, contentDescription = "Remove", tint = VedaDropRose)
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────────── PARTNER REVIEWS (customer browse) ────────────
@Composable
fun PartnerReviewsScreen(viewModel: VedaDropViewModel, partner: Partner) {
    val reviews by viewModel.partnerReviews.collectAsState()
    LaunchedEffect(partner.id) { viewModel.loadPartnerReviews(partner.id) }

    Column(modifier = Modifier.fillMaxSize()) {
        VedaDropHeader(
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
            items(reviews, key = { it.id }) { r ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) { i ->
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (i < r.rating) VedaDropRose else Color.Gray.copy(alpha = 0.4f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Text((r.createdAt ?: "").take(10), fontSize = 11.sp, color = Color.Gray)
                        }
                        if (!r.comment.isNullOrBlank()) {
                            Text(r.comment ?: "", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────────── CUSTOMER DASHBOARD ───────────────────────────
@Composable
fun CustomerDashboardScreen(viewModel: VedaDropViewModel) {
    val bookings by viewModel.bookings.collectAsState()
    val favorites by viewModel.favoritePartners.collectAsState()

    val total = bookings.size
    val completed = bookings.count { it.status == "completed" }
    val recent = bookings.take(5)

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        VedaDropHeader(
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
            Text("Recent bookings", fontWeight = FontWeight.Bold, color = VedaDropRose)
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
                            Text(b.status.replace("_", " ").uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VedaDropRose)
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
        border = BorderStroke(1.dp, VedaDropRose.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = VedaDropRose)
            Text(label, fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}

// ───────────────────────────── PARTNER AVAILABILITY ─────────────────────────
// Platform window: slot-start hours 7..17 inclusive (12 one-hour slots, 7am-6pm).
private val SLOT_HOURS: List<Int> = (7..17).toList()

/** §725 Batch-B — a per-hour bookable RANGE label, e.g. 7 -> "7-8 AM", 17 -> "5-6 PM".
 *  Shows the slot as the hour window it actually books (start..start+1), so the partner
 *  reads the 7am-6pm grid as explicit ranges instead of single start times. */
private fun hourRangeLabel(h: Int): String {
    fun twelve(x: Int) = (x % 12).let { if (it == 0) 12 else it }
    fun period(x: Int) = if (x % 24 < 12) "AM" else "PM"
    val end = h + 1
    // Collapse the AM/PM suffix when both ends share it ("7-8 AM"); else show both ("11-12 PM").
    return if (period(h) == period(end)) "${twelve(h)}-${twelve(end)} ${period(h)}"
    else "${twelve(h)} ${period(h)}-${twelve(end)} ${period(end)}"
}

/** Convert a java.time DayOfWeek (Mon=1..Sun=7) to JS dow (Sun=0..Sat=6). */
private fun jsDow(date: java.time.LocalDate): Int =
    if (date.dayOfWeek.value == 7) 0 else date.dayOfWeek.value

/** Parse a "HH:mm" 24h string to an Int hour, defaulting on bad input. */
private fun parseHour(hhmm: String?, default: Int): Int {
    if (hhmm == null) return default
    val h = hhmm.substringBefore(":").trim().toIntOrNull() ?: return default
    return h
}

/** Per-day editor state for one of the rolling 7 days. */
private data class DayPlan(
    val on: Boolean,
    val hours: Set<Int>,
)

@Composable
fun PartnerAvailabilityScreen(viewModel: VedaDropViewModel) {
    val avail by viewModel.availability.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadAvailability() }

    // Rolling next-7-days, recomputed each composition entry from "now".
    val today = remember { java.time.LocalDate.now() }
    val dates = remember(today) { (0..6).map { today.plusDays(it.toLong()) } }
    val labelFmt = remember { java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM") }

    // Editor: ISO-date -> DayPlan. Hydrated from server when it arrives.
    var plans by remember { mutableStateOf<Map<String, DayPlan>>(emptyMap()) }
    var hydrated by remember { mutableStateOf(false) }
    // §725 Batch-B — auto-save: bump this counter on every user edit so a debounced
    // LaunchedEffect persists the change ~600ms later. Hydration/seeding must NOT bump
    // it (we only save what the partner actually changed), so it starts at 0 and the
    // saver skips the very first composition.
    var editSeq by remember { mutableStateOf(0) }

    LaunchedEffect(avail, today) {
        val a = avail
        if (a != null && !hydrated) {
            val whStart = parseHour(a.workingHours?.start, 9)
            val whEnd = parseHour(a.workingHours?.end, 18)
            // Default bookable hours = working window ∩ 7..17.
            val defaultHours = SLOT_HOURS.filter { it in whStart until whEnd }.toSet()
            val daysSet = if (a.days.isNotEmpty()) a.days.toSet() else setOf(0, 1, 2, 3, 4, 5, 6)
            val newPlans = LinkedHashMap<String, DayPlan>()
            for (d in dates) {
                val iso = d.toString()
                val override = a.hourOverrides[iso]
                val plan = when {
                    override != null -> {
                        val hrs = override.filter { it in 7..17 }.toSet()
                        DayPlan(on = hrs.isNotEmpty(), hours = hrs)
                    }
                    a.leaves.contains(iso) -> DayPlan(on = false, hours = defaultHours)
                    !daysSet.contains(jsDow(d)) -> DayPlan(on = false, hours = defaultHours)
                    else -> DayPlan(on = true, hours = defaultHours)
                }
                newPlans[iso] = plan
            }
            plans = newPlans
            hydrated = true
        }
    }

    // Before server data lands, seed a sensible default so toggles work immediately.
    LaunchedEffect(dates) {
        if (plans.isEmpty()) {
            val seed = LinkedHashMap<String, DayPlan>()
            for (d in dates) {
                seed[d.toString()] = DayPlan(on = true, hours = SLOT_HOURS.toSet())
            }
            plans = seed
        }
    }

    fun planFor(iso: String): DayPlan = plans[iso] ?: DayPlan(on = true, hours = SLOT_HOURS.toSet())
    // Every edit goes through here → it both mutates state AND flags an auto-save.
    fun updatePlan(iso: String, transform: (DayPlan) -> DayPlan) {
        plans = plans.toMutableMap().apply { put(iso, transform(planFor(iso))) }
        editSeq++
    }

    // §725 Batch-B — debounced AUTO-SAVE. Each edit bumps editSeq; we wait 600ms (so a
    // burst of taps collapses into one PUT) then persist via the existing save fn. The
    // initial run (editSeq==0) is skipped so hydration doesn't trigger a spurious save.
    LaunchedEffect(editSeq) {
        if (editSeq == 0) return@LaunchedEffect
        delay(600)
        val overrides = LinkedHashMap<String, List<Int>>()
        for (d in dates) {
            val iso = d.toString()
            val p = planFor(iso)
            // Day off OR day-on-with-no-hours => unavailable => [].
            overrides[iso] = if (p.on) p.hours.filter { it in 7..17 }.sorted() else emptyList()
        }
        // §714 pda-7day-clobbers-weekly-1 — this per-date editor fully defines the visible
        // 7 days via hour_overrides; do NOT send days/leaves so the partner's underlying
        // weekly recurrence (e.g. Sundays off) is preserved.
        viewModel.saveAvailability(start = "07:00", end = "18:00", hourOverrides = overrides)
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        VedaDropHeader(
            title = "Availability",
            subtitle = "Set your hours for the next 7 days",
            onBack = { viewModel.currentScreen = Screen.PartnerProfile },
        )
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // §725 Batch-B — explain the bookable window as RANGES + that changes save
            // themselves (no Save button). The status line below mirrors save state.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Bookable window is 7 AM to 6 PM in 1-hour slots (7-8, 8-9 … 5-6 PM). Turn a day off, or pick the exact hours you can take bookings. Changes save automatically.",
                    fontSize = 12.sp, color = Color.Gray, modifier = Modifier.weight(1f)
                )
            }
            // Subtle auto-save indicator (no explicit submit button).
            val saveStatus: Pair<String, Color>? = when {
                viewModel.availabilityBusy -> "Saving…" to Color.Gray
                viewModel.availabilityError != null -> (viewModel.availabilityError ?: "Couldn't save") to MaterialTheme.colorScheme.error
                viewModel.availabilitySaved -> "✓ Saved" to VedaDropRose
                else -> null
            }
            saveStatus?.let { (label, color) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (viewModel.availabilityBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = VedaDropRose)
                    }
                    Text(label, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
                }
            }

            dates.forEachIndexed { idx, date ->
                val iso = date.toString()
                val plan = planFor(iso)
                val dayLabel = when (idx) {
                    0 -> "Today"
                    1 -> "Tomorrow"
                    else -> labelFmt.format(date)
                }
                val dateSuffix = if (idx <= 1) labelFmt.format(date) else null

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, VedaDropRose.copy(alpha = if (plan.on) 0.30f else 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dayLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = VedaDropRose, maxLines = 1)
                                val sub = dateSuffix ?: if (plan.on) "${plan.hours.size} hour slot(s) open" else "Day off"
                                Text(sub, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                            }
                            Switch(
                                checked = plan.on,
                                onCheckedChange = { on -> updatePlan(iso) { it.copy(on = on) } },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = VedaDropRose
                                )
                            )
                        }

                        if (plan.on) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SLOT_HOURS.forEach { h ->
                                    val selected = plan.hours.contains(h)
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            updatePlan(iso) { p ->
                                                val hrs = if (selected) p.hours - h else p.hours + h
                                                p.copy(hours = hrs)
                                            }
                                        },
                                        label = {
                                            Text(
                                                hourRangeLabel(h),
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = VedaDropRose,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // §725 Batch-B — no Save button: every toggle above auto-saves (debounced).
            // The status line near the top reflects "Saving…" / "✓ Saved" / errors.
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ───────────────────────────── PARTNER EARNINGS ─────────────────────────────
@Composable
fun PartnerEarningsScreen(viewModel: VedaDropViewModel) {
    val earnings by viewModel.earnings.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadEarnings() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        VedaDropHeader(
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
                border = BorderStroke(1.dp, VedaDropRose.copy(alpha = 0.25f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = VedaDropRose)
                    Spacer(Modifier.width(12.dp))
                    Text("Completed jobs", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text("${e?.completedJobs ?: 0}", fontWeight = FontWeight.Bold, color = VedaDropRose)
                }
            }

            Text("Recent completed jobs", fontWeight = FontWeight.Bold, color = VedaDropRose)
            val recent = e?.recent ?: emptyList()
            if (recent.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text("No completed jobs yet.", modifier = Modifier.padding(16.dp), color = Color.Gray)
                }
            } else {
                recent.forEach { r ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = VedaDropRose, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Booking #${r.bookingId}", fontWeight = FontWeight.SemiBold)
                                Text((r.at ?: "").take(10), fontSize = 12.sp, color = Color.Gray)
                            }
                            Text(rupees(r.totalPaise), fontWeight = FontWeight.Bold, color = VedaDropRose)
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
fun PartnerAnalyticsScreen(viewModel: VedaDropViewModel) {
    val analytics by viewModel.analytics.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadAnalytics() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        VedaDropHeader(
            title = "Analytics",
            subtitle = "Performance insights",
            onBack = { viewModel.currentScreen = Screen.PartnerDashboard },
        )
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val a = analytics
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // §710 #12 — backend already returns a percentage (e.g. 66.7); the extra
                // *100 rendered 6670%. Round for a clean 67%.
                StatTile("Accept rate", "${Math.round(a?.acceptRate ?: 0f)}%", Modifier.weight(1f))
                StatTile("Avg response", "${(a?.avgResponseMin ?: 0f).toInt()}m", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Rating", "${a?.ratingAvg ?: 0f}", Modifier.weight(1f))
                StatTile("Reviews", "${a?.ratingCount ?: 0}", Modifier.weight(1f))
            }

            // Profile views
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, VedaDropRose.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = VedaDropRose)
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
                                        .background(VedaDropRose)
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
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = VedaDropRose)
                        Spacer(Modifier.width(8.dp))
                        Text("Rating distribution", fontWeight = FontWeight.Bold)
                    }
                    val dist = a?.ratingDistribution ?: emptyMap()
                    val maxCount = (dist.values.maxOrNull() ?: 1).coerceAtLeast(1)
                    (5 downTo 1).forEach { star ->
                        val count = dist[star.toString()] ?: 0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$star", modifier = Modifier.width(16.dp), fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.Star, contentDescription = null, tint = VedaDropGold, modifier = Modifier.size(14.dp))
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
                                        .background(VedaDropRose)
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
                    Text("Booking funnel", fontWeight = FontWeight.Bold, color = VedaDropRose)
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
        Text("$count", fontWeight = FontWeight.Bold, color = VedaDropRose)
    }
}

// ───────────────────────────── PARTNER PORTFOLIO ────────────────────────────
@Composable
fun PartnerPortfolioScreen(viewModel: VedaDropViewModel) {
    val items by viewModel.portfolio.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadPortfolio() }

    var showAdd by remember { mutableStateOf(false) }
    var uploadId by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        VedaDropHeader(
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
                items(items, key = { it.id }) { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column {
                            if (!item.imageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    // §726 — resolve a self-hosted relative url against
                                    // the API origin (the partner portfolio flow holds
                                    // raw DTOs, not mapped models).
                                    model = com.example.data.remote.Mappers.absUrl(item.imageUrl),
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
                    Text("Add a photo of your own real work.", fontSize = 11.sp, color = Color.Gray)

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
                ) { Text(if (viewModel.portfolioBusy) "Adding…" else "Add", color = VedaDropRose, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("Cancel", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
            }
        )
    }
}

// ───────────────────────────── PARTNER STOREFRONT & MENU (restaurant-style) ────────────────────────
@Composable
fun PartnerStoreScreen(viewModel: VedaDropViewModel, partner: Partner) {
    val cart by viewModel.cart.collectAsState()
    val allServices = VedaDropDataSource.services
    var selectedCategory by remember { mutableStateOf("All") }

    val reviews by viewModel.partnerReviews.collectAsState()
    LaunchedEffect(partner.id) {
        viewModel.loadPartnerReviews(partner.id)
        viewModel.loadPartnerServicePrices(partner.id)   // §710 P0-8 — real per-service prices
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
                            model = partner.avatarUrl.ifBlank { "" },
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
                                        tint = VedaDropGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (partner.kycStatus == "approved") "Verified ✓" else "Not yet verified",
                                color = if (partner.kycStatus == "approved") VedaDropGold else Color.White.copy(alpha = 0.6f),
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
                        color = VedaDropRose,
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

                    // Chat trigger — only against a real service THIS partner offers
                    // (allServices is the global catalog; filter to the partner's own).
                    val myServices = allServices.filter { it.id in partner.servicesOffered }
                    Button(
                        onClick = {
                            val service = myServices.firstOrNull()
                            if (service != null) {
                                viewModel.currentScreen = Screen.PreBookingChat(service, partner)
                            }
                        },
                        enabled = myServices.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = VedaDropRose, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        val firstName = partner.name.substringBefore(" ")
                        Text("Chat with $firstName Pre-Booking", color = VedaDropRose, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                    }
                }
                Divider(color = Color.Gray.copy(alpha = 0.15f))
            }

            // Honest service-radius line (only when the partner set one)
            if (partner.travelRadiusKm > 0) {
                item {
                    PartnerCoverageMap(partner = partner)
                    Divider(color = Color.Gray.copy(alpha = 0.15f))
                }
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
                                    color = VedaDropRose,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = VedaDropGold, modifier = Modifier.size(16.dp))
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
                                        (if (ratingKycApproved) VedaDropRose else Color.Gray).copy(alpha = 0.12f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    if (ratingKycApproved) "VERIFIED RATING" else "RATING",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ratingKycApproved) VedaDropRose else Color.Gray
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
                                                            tint = if (i < r.rating) VedaDropGold else Color.Gray.copy(alpha = 0.3f),
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
                                    selectedContainerColor = VedaDropRose.copy(alpha = 0.25f),
                                    selectedLabelColor = VedaDropRose,
                                    labelColor = Color.Gray
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Menu Items List — restrict to the partner's OWN listed services so the
            // customer can't add something the §690 quote/cart gate will reject.
            val partnerServices = allServices.filter { it.id in partner.servicesOffered }
            val filteredServices = partnerServices.filter { srv ->
                if (selectedCategory == "All") true
                else srv.categoryId.lowercase().contains(selectedCategory.lowercase()) ||
                     srv.name.lowercase().contains(selectedCategory.lowercase())
            }

            if (partnerServices.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("This partner hasn't listed services yet", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else if (filteredServices.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No services matches this category selection", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                items(filteredServices, key = { it.id }) { service ->
                    // §710 P0-8 — the partner's REAL price for THIS service (fetched from
                    // /customer/partners/{id}/services). Falls back to the catalog price
                    // only while the per-service map is still loading / absent.
                    val resolvedPrice = viewModel.partnerServicePrices[service.id]
                        ?: if (partner.fromPricePaise > 0) partner.fromPricePaise else service.pricePaise
                    
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
                                            .background(VedaDropRose, RoundedCornerShape(4.dp))
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
                                        border = BorderStroke(1.dp, VedaDropRose),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VedaDropRose)
                                    ) {
                                        Text("ADD", fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Checkout Cart float footer bar
        val cartSnapshot = cart
        val hasCartItems = cartSnapshot != null && cartSnapshot.items.isNotEmpty() &&
                (cartSnapshot.partnerId?.toString() == partner.id || cartSnapshot.partnerId == partner.id.toIntOrNull())

        if (hasCartItems && cartSnapshot != null) {
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
                    colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "🛒 ${cartSnapshot.count} items in basket",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "Subtotal: Rs ${cartSnapshot.subtotalPaise / 100}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "View Cart & Book ➔",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PartnerCoverageMap(partner: Partner) {
    // Honest service-radius line driven by the partner's own data — no fake
    // geofence / canvas map / string-matched eligibility check.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("coverage_radius_card_${partner.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = VedaDropRose,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = "SERVICE AREA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = VedaDropRose,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Serves within ${partner.travelRadiusKm.toInt()} km",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
