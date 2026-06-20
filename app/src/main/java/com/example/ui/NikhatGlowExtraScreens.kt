@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import android.widget.Toast
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.CartItemDto
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.DeepPlum
import com.example.ui.theme.NikhatGold
import com.example.ui.theme.NikhatRose

// ---------------- CART (single-partner, multi-service) ----------------

@Composable
fun CartScreen(viewModel: NikhatGlowViewModel) {
    val cart by viewModel.cart.collectAsState()
    val addresses by viewModel.addresses.collectAsState()
    var checkoutError by remember { mutableStateOf<String?>(null) }
    var placing by remember { mutableStateOf(false) }
    val items: List<CartItemDto> = cart?.items ?: emptyList()

    // Chosen delivery address — default to the user's default, but let them switch.
    var selectedAddressId by remember(addresses) {
        mutableStateOf(
            (addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull())?.id
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("My Cart", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { viewModel.currentScreen = Screen.CustomerHome }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (items.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearCart() }) {
                        Text("Clear", color = NikhatRose)
                    }
                }
            }
        )

        if (items.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(72.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Your cart is empty", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "Explore services and add them from a professional profile.",
                    fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.currentScreen = Screen.CustomerHome },
                    colors = ButtonDefaults.buttonColors(containerColor = NikhatRose)
                ) { Text("Explore Services") }
            }
            return
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            cart?.partnerName?.let {
                Text("Booking from $it", fontWeight = FontWeight.Bold, color = NikhatRose)
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name ?: "Service", fontWeight = FontWeight.Bold)
                                Text("Rs ${item.unitPricePaise / 100} each", fontSize = 12.sp, color = Color.Gray)
                            }
                            IconButton(onClick = {
                                if (item.qty <= 1) viewModel.removeCartItem(item.id)
                                else viewModel.updateCartQty(item.id, item.qty - 1)
                            }) { Icon(Icons.Default.Remove, contentDescription = "Less") }
                            Text("${item.qty}", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { viewModel.updateCartQty(item.id, item.qty + 1) }) {
                                Icon(Icons.Default.Add, contentDescription = "More")
                            }
                            Text("Rs ${item.lineTotalPaise / 100}", fontWeight = FontWeight.Bold, color = NikhatRose)
                            IconButton(onClick = { viewModel.removeCartItem(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Gray)
                            }
                        }
                    }
                }
            }

            // ── Pick a time slot (reuses the §702 availability picker pattern) ──
            val partnerIdStr = cart?.partnerId?.toString()
            val firstServiceId = items.firstOrNull()?.serviceId?.toString()
            val selectedDate = viewModel.selectedBookingDate
            val selectedSlotId = viewModel.selectedSlotId

            // Load real slots on entry + whenever the date changes.
            LaunchedEffect(partnerIdStr, firstServiceId, selectedDate) {
                if (partnerIdStr != null) {
                    viewModel.loadSlots(partnerIdStr, firstServiceId, selectedDate)
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PICK A TIME SLOT", fontWeight = FontWeight.Bold, color = NikhatRose, fontSize = 13.sp)

                    // Next-7-days date stepper.
                    val today = remember { java.time.LocalDate.now() }
                    val dayFmt = remember { java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM", java.util.Locale.US) }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (offset in 0..6) {
                            val d = today.plusDays(offset.toLong())
                            val iso = d.toString()
                            FilterChip(
                                selected = iso == selectedDate,
                                onClick = { if (partnerIdStr != null) viewModel.loadSlots(partnerIdStr, firstServiceId, iso) },
                                label = { Text(d.format(dayFmt), fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NikhatRose,
                                    selectedLabelColor = Color.White,
                                )
                            )
                        }
                    }

                    when {
                        viewModel.slotsLoading -> {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NikhatRose)
                                Text("Loading available slots...", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        viewModel.availableSlots.isEmpty() -> {
                            Text("No free slots that day - pick another date.", fontSize = 12.sp, color = NikhatRose, fontWeight = FontWeight.Bold)
                        }
                        else -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                viewModel.availableSlots.forEach { slot ->
                                    val hourLabel = slot.start
                                        ?.substringAfter("T", "")
                                        ?.take(5)
                                        ?.takeIf { it.length == 5 && it.contains(":") }
                                        ?: slot.slotId.split(":").getOrNull(2)?.let { h ->
                                            val hh = h.toIntOrNull()
                                            if (hh != null) String.format("%02d:00", hh) else h
                                        } ?: slot.slotId
                                    FilterChip(
                                        selected = slot.slotId == selectedSlotId,
                                        enabled = slot.available,
                                        onClick = { viewModel.selectedSlotId = slot.slotId },
                                        label = { Text(hourLabel, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NikhatRose,
                                            selectedLabelColor = Color.White,
                                            disabledLabelColor = Color.Gray.copy(alpha = 0.5f),
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Choose delivery address ──
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DELIVERY ADDRESS", fontWeight = FontWeight.Bold, color = NikhatRose, fontSize = 13.sp)
                    if (addresses.isEmpty()) {
                        Text("No saved address - add one from your profile first.", fontSize = 12.sp, color = Color.Gray)
                    } else {
                        addresses.forEach { addr ->
                            val isSel = addr.id == selectedAddressId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedAddressId = addr.id }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = isSel,
                                    onClick = { selectedAddressId = addr.id },
                                    colors = RadioButtonDefaults.colors(selectedColor = NikhatRose)
                                )
                                Column {
                                    Text(addr.labelText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${addr.line1} ${addr.line2}, ${addr.city} - ${addr.pincode}", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Estimated subtotal", fontWeight = FontWeight.Bold)
                        Text("Rs ${(cart?.subtotalPaise ?: 0L) / 100}", fontWeight = FontWeight.Bold, color = NikhatRose)
                    }
                    Text(
                        "Estimate only - you pay the professional directly after the service.",
                        fontSize = 11.sp, color = Color.Gray
                    )
                    if (selectedSlotId == null) {
                        Text("Select a time slot to send your request.", fontSize = 11.sp, color = NikhatRose)
                    }
                    checkoutError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            placing = true
                            checkoutError = null
                            viewModel.checkoutCart(addressId = selectedAddressId) { err ->
                                placing = false
                                checkoutError = err
                            }
                        },
                        enabled = !placing && selectedSlotId != null,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("send_booking_request_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = NikhatRose)
                    ) {
                        Text(if (placing) "Sending..." else "Send Request", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

// MyBookingsScreen: Dual-Tab Appointments & Requests Tracker
@Composable
fun MyBookingsScreen(viewModel: NikhatGlowViewModel) {
    val bookings by viewModel.bookings.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val isPartner = activeUser?.role == "partner"

    var selectedTab by remember { mutableStateOf(0) }
    
    // Star rating dialog states
    var showReviewDialog by remember { mutableStateOf(false) }
    var selectedBookingId by remember { mutableStateOf("") }
    var reviewRatingState by remember { mutableStateOf(5) }
    var reviewCommentState by remember { mutableStateOf("") }

    // Separate active/upcoming vs past/completed
    val activeStatuses = listOf("pending", "accepted", "assigned", "partner_on_the_way", "arrived", "started")
    val pastStatuses = listOf("completed", "cancelled", "refunded")

    val filteredBookings = bookings.filter { booking ->
        if (selectedTab == 0) {
            booking.status in activeStatuses
        } else {
            booking.status in pastStatuses
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (isPartner) "Booking Requests" else "My Bookings", fontWeight = FontWeight.Bold) }
        )

        // Custom Tab Selector (Upcoming vs History)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = NikhatRose,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = NikhatRose
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(if (isPartner) "Active Tasks" else "Active Schedule", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Past Treatments", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            )
        }

        if (filteredBookings.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (selectedTab == 0) Icons.Default.Schedule else Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = Color.Gray.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (selectedTab == 0) {
                        if (isPartner) "No upcoming service requests right now." else "No upcoming appointments scheduled."
                    } else {
                        "No past appointments found."
                    },
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredBookings) { booking ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.currentScreen = Screen.BookingDetail(booking.id) }
                            .testTag("booking_item_card_${booking.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.12f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Service and Price Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = booking.serviceName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "₹${booking.totalPaise / 100}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = NikhatRose
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))

                            // Provider / customer detail name
                            Text(
                                text = if (isPartner) "Client: ${booking.addressText.substringBefore(",")}" else "Specialist: ${booking.partnerName}",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )

                            // Date Slot row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Icon(Icons.Default.EventNote, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = booking.dateTimeSlot,
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Status Chip and Action Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dynamic lifecycle color coding
                                val statusBgColor = when (booking.status) {
                                    "pending" -> Color(0xFFF39C12).copy(alpha = 0.15f)
                                    "accepted", "assigned" -> Color(0xFF3498DB).copy(alpha = 0.15f)
                                    "partner_on_the_way" -> Color(0xFF9B59B6).copy(alpha = 0.15f)
                                    "arrived", "started" -> Color(0xFF1ABC9C).copy(alpha = 0.15f)
                                    "completed" -> Color(0xFF2ECC71).copy(alpha = 0.15f)
                                    "cancelled", "refunded" -> Color(0xFFE74C3C).copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                }
                                val statusTextColor = when (booking.status) {
                                    "pending" -> Color(0xFFF1C40F)
                                    "accepted", "assigned" -> Color(0xFF5DADE2)
                                    "partner_on_the_way" -> Color(0xFFC39BD3)
                                    "arrived", "started" -> Color(0xFF48C9B0)
                                    "completed" -> Color(0xFF2ECC71)
                                    "cancelled", "refunded" -> Color(0xFFEC7063)
                                    else -> MaterialTheme.colorScheme.secondary
                                }

                                Box(
                                    modifier = Modifier
                                        .background(color = statusBgColor, shape = RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = booking.status.replace("_", " ").uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusTextColor
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Submit verified star review direct prompt (Customer, completed, unreviewed)
                                    if (!isPartner && booking.status == "completed" && booking.reviewRating == 0) {
                                        Button(
                                            onClick = {
                                                selectedBookingId = booking.id
                                                reviewRatingState = 5
                                                reviewCommentState = ""
                                                showReviewDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NikhatRose.copy(alpha = 0.12f)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(30.dp).testTag("quick_review_btn_${booking.id}")
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = NikhatGold, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Feedback", fontSize = 10.sp, color = NikhatRose, fontWeight = FontWeight.Bold)
                                        }
                                    } else if (!isPartner && booking.reviewRating > 0) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = NikhatGold, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("${booking.reviewRating}/5 Stars", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        }
                                    }

                                    // 'Book Again' button for completed treatments
                                    if (!isPartner && (booking.status == "completed" || booking.status == "cancelled")) {
                                        Button(
                                            onClick = { viewModel.bookAgain(booking) },
                                            colors = ButtonDefaults.buttonColors(containerColor = NikhatRose),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(30.dp).testTag("book_again_btn_${booking.id}")
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Book Again", tint = Color.White, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Book Again", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.currentScreen = Screen.BookingDetail(booking.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowForward, contentDescription = "Open Detail", tint = NikhatRose, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Review Submission Dialog Box
    if (showReviewDialog) {
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = {
                Text(
                    "Leave Treatment Feedback",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = NikhatRose
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Rate your service quality of cleanliness, kit-seals and therapist punctuality:",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                    
                    // Clickable Stars Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        (1..5).forEach { star ->
                            IconButton(
                                onClick = { reviewRatingState = star },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (star <= reviewRatingState) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Star $star",
                                    tint = if (star <= reviewRatingState) NikhatGold else Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = reviewCommentState,
                        onValueChange = { reviewCommentState = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .testTag("dialog_review_comment_input"),
                        placeholder = { Text("How was the sanitation kit, seals, and overall grooming process?", fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = NikhatRose
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitBookingReview(selectedBookingId, reviewRatingState, reviewCommentState)
                        showReviewDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NikhatRose),
                    modifier = Modifier.testTag("dialog_review_submit_btn")
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showReviewDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ---------------- PARTNER ACCOUNT ----------------

@Composable
fun PartnerProfileScreen(viewModel: NikhatGlowViewModel) {
    val activeUser by viewModel.activeUser.collectAsState()
    var nameState by remember(activeUser?.name) { mutableStateOf(activeUser?.name ?: "") }
    var emailState by remember(activeUser?.email) { mutableStateOf(activeUser?.email ?: "") }
    var saved by remember { mutableStateOf(false) }
    // §694 — read-only until the partner taps Edit; Save returns to read-only.
    var isEditing by remember { mutableStateOf(false) }
    // §694 — partner business prefs.
    var genderState by remember(activeUser?.gender) {
        mutableStateOf((activeUser?.gender ?: "").ifBlank { "any" })
    }
    var minOrderRupees by remember(activeUser?.minimumOrderPaise) {
        mutableStateOf(((activeUser?.minimumOrderPaise ?: 0L) / 100).let { if (it > 0) it.toString() else "" })
    }
    var radiusState by remember(activeUser?.travelRadiusKm) {
        mutableStateOf((activeUser?.travelRadiusKm ?: 0.0).let { if (it > 0) it.toString() else "" })
    }
    val profileCtx = LocalContext.current
    // Re-request location in-context if the partner denied it earlier, then save.
    val partnerLocPermLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) viewModel.capturePartnerLocation()
        else viewModel.notify("Location permission denied.", isError = true)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(DeepPlum, DarkSlate)))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                InitialsAvatar(activeUser?.name, size = 72)
                Spacer(modifier = Modifier.height(8.dp))
                Text(activeUser?.name ?: "Partner", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                val phone = (activeUser?.phone ?: "").ifBlank { null }
                if (phone != null) {
                    Text(phone, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                }
                Text(
                    "Rating ${activeUser?.averageRating ?: 0f}  ·  ${activeUser?.completedJobs ?: 0} jobs done",
                    color = NikhatRose, fontSize = 13.sp
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = NikhatRose)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Verification (KYC)", fontWeight = FontWeight.Bold)
                        Text((activeUser?.kycStatus ?: "not_started").replace("_", " "), fontSize = 12.sp, color = Color.Gray)
                    }
                    TextButton(onClick = { viewModel.currentScreen = Screen.PartnerKyc }) {
                        Text("Manage", color = NikhatRose)
                    }
                }
            }

            val sub by viewModel.subscription.collectAsState()
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = NikhatRose)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Subscription (₹99/month)", fontWeight = FontWeight.Bold)
                        val status = sub?.status ?: "—"
                        val tail = if ((sub?.daysLeft ?: 0) > 0) " · ${sub?.daysLeft}d left" else ""
                        Text(status.replaceFirstChar { it.uppercase() } + tail, fontSize = 12.sp, color = Color.Gray)
                    }
                    TextButton(onClick = { viewModel.currentScreen = Screen.PartnerSubscription }) {
                        Text("Manage", color = NikhatRose)
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EventNote, contentDescription = null, tint = NikhatRose)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Availability", fontWeight = FontWeight.Bold)
                        Text("Working hours, days & leaves", fontSize = 12.sp, color = Color.Gray)
                    }
                    TextButton(onClick = { viewModel.currentScreen = Screen.PartnerAvailability }) {
                        Text("Manage", color = NikhatRose)
                    }
                }
            }

            // Business location — without it the partner can't be distance-ranked
            // in customer discovery (the "can't set my location" symptom).
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = NikhatRose)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Business location", fontWeight = FontWeight.Bold)
                        Text("Set where you're based so nearby clients can find you", fontSize = 12.sp, color = Color.Gray)
                    }
                    TextButton(onClick = {
                        if (com.example.data.LocationHelper.hasPermission(profileCtx)) {
                            viewModel.capturePartnerLocation()
                        } else {
                            partnerLocPermLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                )
                            )
                        }
                    }) {
                        Text("Use current", color = NikhatRose)
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = NikhatRose)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Portfolio", fontWeight = FontWeight.Bold)
                        Text("Showcase your work", fontSize = 12.sp, color = Color.Gray)
                    }
                    TextButton(onClick = { viewModel.currentScreen = Screen.PartnerPortfolio }) {
                        Text("Manage", color = NikhatRose)
                    }
                }
            }

            // §691 — the partner's own unique transfer code. Shared with a colleague
            // so they can receive a targeted emergency booking transfer.
            val publicCode = activeUser?.partnerPublicCode ?: ""
            if (publicCode.isNotBlank()) {
                val clipboard = LocalClipboardManager.current
                val ctx = LocalContext.current
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, NikhatRose.copy(alpha = 0.4f)),
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = NikhatRose)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Your partner code", fontWeight = FontWeight.Bold)
                            Text(
                                publicCode.uppercase(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 3.sp,
                                color = NikhatRose,
                            )
                            Text("Share this code to receive a transferred booking.", fontSize = 11.sp, color = Color.Gray)
                        }
                        TextButton(onClick = {
                            clipboard.setText(AnnotatedString(publicCode.uppercase()))
                            Toast.makeText(ctx, "Code copied", Toast.LENGTH_SHORT).show()
                        }) { Text("Copy", color = NikhatRose) }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Profile details", fontWeight = FontWeight.Bold)
                        if (!isEditing) {
                            TextButton(onClick = {
                                nameState = activeUser?.name ?: ""
                                emailState = activeUser?.email ?: ""
                                saved = false
                                isEditing = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", color = NikhatRose, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isEditing) {
                        OutlinedTextField(
                            value = nameState, onValueChange = { nameState = it; saved = false },
                            label = { Text("Business / Display name") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = emailState, onValueChange = { emailState = it; saved = false },
                            label = { Text("Email (optional)") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Preferred client gender", fontSize = 12.sp, color = Color.Gray)
                        GenderPreferenceSelector(selected = genderState, onSelect = { genderState = it; saved = false })

                        OutlinedTextField(
                            value = minOrderRupees,
                            onValueChange = { v -> minOrderRupees = v.filter { it.isDigit() }; saved = false },
                            label = { Text("Minimum order (₹)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = radiusState,
                            onValueChange = { v -> radiusState = v.filter { it.isDigit() || it == '.' }; saved = false },
                            label = { Text("Service radius (km)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    nameState = activeUser?.name ?: ""
                                    emailState = activeUser?.email ?: ""
                                    genderState = (activeUser?.gender ?: "").ifBlank { "any" }
                                    minOrderRupees = ((activeUser?.minimumOrderPaise ?: 0L) / 100).let { if (it > 0) it.toString() else "" }
                                    radiusState = (activeUser?.travelRadiusKm ?: 0.0).let { if (it > 0) it.toString() else "" }
                                    isEditing = false
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                            Button(
                                onClick = {
                                    if (nameState.trim().isEmpty()) {
                                        viewModel.notify("Name cannot be blank.", isError = true)
                                    } else {
                                        viewModel.updateProfile(
                                            name = nameState.trim(),
                                            email = emailState.trim(),
                                            bio = activeUser?.partnerBio ?: "",
                                            experience = activeUser?.partnerExperience ?: 0,
                                            gender = genderState,
                                            minimumOrderPaise = (minOrderRupees.toLongOrNull() ?: 0L) * 100,
                                            travelRadiusKm = radiusState.toDoubleOrNull() ?: 0.0,
                                        )
                                        viewModel.notify("Profile updated.", isError = false)
                                        saved = true
                                        isEditing = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NikhatRose),
                                modifier = Modifier.weight(1f)
                            ) { Text("Save") }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Business / Display name", fontSize = 11.sp, color = Color.Gray)
                            Text((activeUser?.name ?: "").ifBlank { "Not set" },
                                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Email", fontSize = 11.sp, color = Color.Gray)
                            Text((activeUser?.email ?: "").ifBlank { "Not set" },
                                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Preferred client gender", fontSize = 11.sp, color = Color.Gray)
                            Text((activeUser?.gender ?: "").ifBlank { "Any" }.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Minimum order", fontSize = 11.sp, color = Color.Gray)
                            Text(((activeUser?.minimumOrderPaise ?: 0L) / 100).let { if (it > 0) "₹$it" else "No minimum" },
                                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Service radius", fontSize = 11.sp, color = Color.Gray)
                            Text((activeUser?.travelRadiusKm ?: 0.0).let { if (it > 0) "$it km" else "Not set" },
                                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { viewModel.logout() },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("logout_button"),
                border = BorderStroke(1.dp, NikhatRose),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NikhatRose),
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log out", fontWeight = FontWeight.SemiBold)
            }

            // §704 — Play-Store-required account deletion (subtle, destructive).
            Spacer(modifier = Modifier.height(8.dp))
            var showDeleteAccount by remember { mutableStateOf(false) }
            TextButton(
                onClick = { showDeleteAccount = true },
                modifier = Modifier.fillMaxWidth().testTag("delete_account_button"),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Delete account", fontSize = 13.sp)
            }
            if (showDeleteAccount) {
                DeleteAccountDialog(
                    onConfirm = { viewModel.deleteAccount(); showDeleteAccount = false },
                    onDismiss = { showDeleteAccount = false },
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ---------------- §703 COMPLAINT DETAIL THREAD + REPLY ----------------

@Composable
fun ComplaintDetailScreen(viewModel: NikhatGlowViewModel, complaintId: String) {
    val detail = viewModel.complaintDetail
    val messages = viewModel.complaintMessages
    var replyText by remember { mutableStateOf("") }

    // Load the thread (and refresh on reply) whenever the screen opens.
    LaunchedEffect(complaintId) { viewModel.openComplaint(complaintId) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Support Ticket", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { viewModel.currentScreen = Screen.ComplaintsList }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepPlum, titleContentColor = Color.White),
        )

        // Subject + status header
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(detail?.subject ?: "Ticket #$complaintId", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            val status = (detail?.status ?: "open").replaceFirstChar { it.uppercase() }
            Card(colors = CardDefaults.cardColors(containerColor = NikhatRose.copy(alpha = 0.15f))) {
                Text(status, color = NikhatRose, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
            if (!detail?.message.isNullOrBlank()) {
                Text(detail!!.message!!, fontSize = 13.sp, color = Color.Gray)
            }
        }
        Divider()

        // Message thread
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
            if (messages.isEmpty()) {
                item {
                    Text(
                        "No replies yet. Send a message below and our team will respond.",
                        fontSize = 13.sp, color = Color.Gray,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
            items(messages) { msg ->
                val mine = msg.senderType == "customer" || msg.senderType == "partner"
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (mine) NikhatRose.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(msg.message, fontSize = 14.sp, modifier = Modifier.padding(10.dp))
                    }
                    Text(
                        "${msg.senderType.replaceFirstChar { it.uppercase() }} · ${msg.createdAt ?: ""}",
                        fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }

        // Reply input
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = replyText,
                onValueChange = { replyText = it },
                placeholder = { Text("Write a reply…") },
                modifier = Modifier.weight(1f),
                maxLines = 3,
            )
            Button(
                onClick = {
                    viewModel.sendComplaintReply(complaintId, replyText)
                    replyText = ""
                },
                enabled = replyText.isNotBlank() && !viewModel.complaintReplyBusy,
                colors = ButtonDefaults.buttonColors(containerColor = NikhatRose),
            ) { Text("Send") }
        }
    }
}

// ---------------- NOTIFICATIONS (in-app inbox) ----------------

/**
 * Bell icon with an unread-count badge, shown in the customer Home header and the
 * partner Dashboard header. Tapping it opens [Screen.Notifications]. The badge is
 * driven by [NikhatGlowViewModel.unreadCount] (kept fresh by the home/dashboard
 * 30s poll). White-tinted to sit on the dark gradient headers.
 */
@Composable
fun NotificationBell(viewModel: NikhatGlowViewModel) {
    val unread by viewModel.unreadCount.collectAsState()
    IconButton(
        onClick = { viewModel.currentScreen = Screen.Notifications },
        modifier = Modifier.testTag("notifications_bell")
    ) {
        if (unread > 0) {
            BadgedBox(badge = { Badge { Text(if (unread > 99) "99+" else "$unread") } }) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Notifications,
                    contentDescription = "Notifications ($unread unread)",
                    tint = Color.White
                )
            }
        } else {
            Icon(
                androidx.compose.material.icons.Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = Color.White
            )
        }
    }
}

/** Full-screen notification inbox. Tapping an unread card marks it read. */
@Composable
fun NotificationsScreen(viewModel: NikhatGlowViewModel) {
    val notifications by viewModel.notifications.collectAsState()

    // Refresh on entry so the list is current even if the user deep-linked here.
    LaunchedEffect(Unit) { viewModel.loadNotifications() }

    Column(modifier = Modifier.fillMaxSize().background(DarkSlate)) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(DeepPlum, DarkSlate)))
                .padding(horizontal = 8.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { if (!viewModel.goBack()) viewModel.currentScreen = Screen.CustomerHome }) {
                Icon(ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text("Notifications", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No notifications yet", color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications, key = { it.id }) { n ->
                    val unread = !n.read
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { if (unread) viewModel.markNotificationRead(n.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (unread) NikhatRose.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = if (unread) BorderStroke(1.dp, NikhatRose.copy(alpha = 0.45f)) else null
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            if (unread) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp, end = 10.dp)
                                        .size(8.dp)
                                        .background(NikhatRose, RoundedCornerShape(4.dp))
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    n.title ?: "Notification",
                                    fontSize = 15.sp,
                                    fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold,
                                    color = Color.White
                                )
                                if (!n.body.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(n.body!!, fontSize = 13.sp, color = Color.White.copy(alpha = 0.82f), lineHeight = 18.sp)
                                }
                                if (!n.createdAt.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        notificationTimeShort(n.createdAt!!),
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.55f)
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

/** Compact relative/short time for a notification ISO timestamp. Falls back to the
 *  raw date portion if parsing fails (never throws). */
private fun notificationTimeShort(iso: String): String = kotlin.runCatching {
    val then = java.time.Instant.parse(iso)
    val mins = java.time.Duration.between(then, java.time.Instant.now()).toMinutes()
    when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m ago"
        mins < 1440 -> "${mins / 60}h ago"
        mins < 10080 -> "${mins / 1440}d ago"
        else -> iso.take(10)
    }
}.getOrDefault(iso.take(10))
