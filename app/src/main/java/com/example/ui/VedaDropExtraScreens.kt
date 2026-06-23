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
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.CartItemDto
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.DeepPlum
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VedaDropGold
import com.example.ui.theme.VedaDropRose
import androidx.compose.material.icons.filled.Verified

// ---------------- CART (single-partner, multi-service) ----------------

@Composable
fun GuestCartView(onTriggerLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP Header matching corporate design guidelines
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MY CART",
                    style = MaterialTheme.typography.labelLarge,
                    color = VedaDropRose,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(VedaDropRose.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = VedaDropRose,
                    modifier = Modifier.size(40.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Prepare Your Treatment Cart",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Unlock your cart and start picking dynamic home treatments. Sign in or sign up now to compile custom Ayurvedic therapies and book professional certified providers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            Button(
                onClick = onTriggerLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("guest_cart_login_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Access My Cart",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CartScreen(viewModel: VedaDropViewModel) {
    if (!viewModel.isLoggedIn) {
        GuestCartView(onTriggerLogin = { viewModel.triggerLoginPrompt() })
        return
    }
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
    // §725 Batch-B — shared full-screen "Add new location" picker.
    var showLocationPicker by remember { mutableStateOf(false) }

    // §734 — per-screen TopAppBar removed; shell header shows "My Cart" + back. The
    // "Clear" action moved into the cart body (see the Clear-cart row below).
    Column(modifier = Modifier.fillMaxSize()) {
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
                    colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose)
                ) { Text("Explore Services", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
            }
            return
        }

        // §710 — scroll the WHOLE body. Previously a weight(1f) cart-items LazyColumn
        // pinned the slot-picker + address + "Send Request" footer below it, so on a
        // tall footer (many slots/addresses, small screen, keyboard up) the Send button
        // was clipped off-screen and unreachable. Now the page scrolls and the cart list
        // is a bounded (scrollable) sub-list, so everything is reachable.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // §734 — relocated "Clear cart" action (was in the removed TopAppBar).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { viewModel.clearCart() }) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = VedaDropRose, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear cart", color = VedaDropRose, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                }
            }

            // §722 — a cart spans 2+ partners only when multi_partner is ON (the backend
            // blocks cross-partner adds otherwise), so partnerCount>1 IS multi-partner mode.
            val multiPartner = (cart?.partnerCount ?: 0) > 1
            if (multiPartner) {
                Surface(
                    color = VedaDropRose.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = VedaDropRose, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${cart?.partnerCount} professionals will serve this booking — they'll be booked together.",
                            fontSize = 12.sp, color = VedaDropRose, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                cart?.partnerName?.let {
                    Text("Booking from $it", fontWeight = FontWeight.Bold, color = VedaDropRose)
                }
            }

            LazyColumn(
                modifier = Modifier.heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.id }) { item ->
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
                                // §722 — show WHO serves this line when the cart is multi-partner.
                                if (multiPartner && !item.partnerName.isNullOrBlank()) {
                                    Text("by ${item.partnerName}", fontSize = 11.sp, color = VedaDropRose, fontWeight = FontWeight.Medium)
                                }
                                Text("Rs ${"%.2f".format(item.unitPricePaise / 100.0)} each", fontSize = 12.sp, color = Color.Gray)
                            }
                            IconButton(onClick = {
                                if (item.qty <= 1) viewModel.removeCartItem(item.id)
                                else viewModel.updateCartQty(item.id, item.qty - 1)
                            }) { Icon(Icons.Default.Remove, contentDescription = "Less") }
                            Text("${item.qty}", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { viewModel.updateCartQty(item.id, item.qty + 1) }) {
                                Icon(Icons.Default.Add, contentDescription = "More")
                            }
                            Text("Rs ${"%.2f".format(item.lineTotalPaise / 100.0)}", fontWeight = FontWeight.Bold, color = VedaDropRose)
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

            // §729 (parity C2) — SMART ADD-ONS: services frequently booked with the cart's
            // first service. Tapping a chip adds it to THIS cart's partner. Hidden when empty.
            if (firstServiceId != null && partnerIdStr != null) {
                RelatedServicesRow(
                    viewModel = viewModel,
                    serviceId = firstServiceId,
                    onPick = { svc -> viewModel.addToCart(partnerIdStr, svc.id) },
                )
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PICK A TIME SLOT", fontWeight = FontWeight.Bold, color = VedaDropRose, fontSize = 13.sp)

                    // §729 (parity C2) — flexible arrival toggle (only when enabled). Same
                    // semantics as the single-service booking screen; uses the shared VM flag.
                    val flexEnabled = viewModel.flexibleSlotsEnabled()
                    val flexOn = viewModel.bookingFlexible
                    if (flexEnabled) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Flexible arrival", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Pick a window — get matched faster.", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(
                                checked = flexOn,
                                onCheckedChange = {
                                    viewModel.bookingFlexible = it
                                    viewModel.selectedSlotId = null
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = VedaDropRose),
                                modifier = Modifier.testTag("cart_flexible_toggle"),
                            )
                        }
                    }

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
                                    selectedContainerColor = VedaDropRose,
                                    selectedLabelColor = Color.White,
                                )
                            )
                        }
                    }

                    if (flexEnabled && flexOn && partnerIdStr != null) {
                        // §729 — window chunks for the cart's serving partner.
                        val windows = remember(partnerIdStr, selectedDate, viewModel.flexWindowMinutes()) {
                            flexWindows(partnerIdStr, selectedDate, viewModel.flexWindowMinutes())
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            windows.forEach { w ->
                                FilterChip(
                                    selected = w.slotId == selectedSlotId,
                                    onClick = { viewModel.selectedSlotId = w.slotId },
                                    label = { Text(w.label(), fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VedaDropRose,
                                        selectedLabelColor = Color.White,
                                    )
                                )
                            }
                        }
                        Text("Your professional will arrive within the chosen window.",
                            fontSize = 11.sp, color = Color.Gray)
                    } else when {
                        viewModel.slotsLoading -> {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = VedaDropRose)
                                Text("Loading available slots...", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        viewModel.availableSlots.isEmpty() -> {
                            Text("No free slots that day - pick another date.", fontSize = 12.sp, color = VedaDropRose, fontWeight = FontWeight.Bold)
                        }
                        else -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                viewModel.availableSlots.forEach { slot ->
                                    // §722 — show the slot as a 1-hour RANGE ("7AM-8AM").
                                    val hourLabel = slotHourRange(slot.start, slot.slotId)
                                    // §725 — subtle hint for slots the server disabled.
                                    val hint = disabledSlotHint(slot)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        FilterChip(
                                            selected = slot.slotId == selectedSlotId,
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
                                // §725 — custom :15/:30/:45 fine-tune for the cart slot.
                                if (partnerIdStr != null) {
                                    CustomTimeChip(
                                        selectedDate = selectedDate,
                                        partnerId = partnerIdStr,
                                        selectedSlotId = selectedSlotId,
                                        onPicked = { viewModel.selectedSlotId = it },
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
                    Text("DELIVERY ADDRESS", fontWeight = FontWeight.Bold, color = VedaDropRose, fontSize = 13.sp)
                    if (addresses.isEmpty()) {
                        Text("No saved address - add one below.", fontSize = 12.sp, color = Color.Gray)
                    } else {
                        addresses.forEach { addr ->
                            val isSel = addr.id == selectedAddressId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedAddressId = addr.id
                                        // §725 — persist as next-time default ("last selected").
                                        viewModel.setDefaultAddress(addr.id)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = isSel,
                                    onClick = {
                                        selectedAddressId = addr.id
                                        viewModel.setDefaultAddress(addr.id)
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = VedaDropRose)
                                )
                                Column {
                                    Text(addr.labelText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${addr.line1} ${addr.line2}, ${addr.city} - ${addr.pincode}", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                    // §725 Batch-B — add a new location via the shared full-screen picker;
                    // setActiveLocation POSTs + sets it as default, the flow refresh
                    // re-seeds selectedAddressId to the new default.
                    OutlinedButton(
                        onClick = { showLocationPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add new location", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Estimated subtotal", fontWeight = FontWeight.Bold)
                        Text("Rs ${"%.2f".format((cart?.subtotalPaise ?: 0L) / 100.0)}", fontWeight = FontWeight.Bold, color = VedaDropRose)
                    }
                    Text(
                        "Estimate only - you pay the professional directly after the service.",
                        fontSize = 11.sp, color = Color.Gray
                    )
                    if (selectedSlotId == null) {
                        Text("Select a time slot to send your request.", fontSize = 11.sp, color = VedaDropRose)
                    }
                    if (selectedAddressId == null) {
                        Text("Add and select a service address to send your request.", fontSize = 11.sp, color = VedaDropRose)
                    }
                    checkoutError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    // §722 — one-time date+slot confirmation before placing the cart booking.
                    var showCartConfirm by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            if (!placing && selectedSlotId != null && selectedAddressId != null) showCartConfirm = true
                        },
                        enabled = !placing && selectedSlotId != null && selectedAddressId != null,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("send_booking_request_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose)
                    ) {
                        Text(if (placing) "Sending..." else "Review & confirm", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                    }
                    if (showCartConfirm) {
                        // §729 (parity C2) — flexible window label vs exact slot label.
                        val isFlex = viewModel.flexibleSlotsEnabled() && viewModel.bookingFlexible
                        val slotLabel = if (isFlex && partnerIdStr != null) {
                            selectedSlotId
                                ?.let { sid -> flexWindows(partnerIdStr, viewModel.selectedBookingDate, viewModel.flexWindowMinutes())
                                    .firstOrNull { it.slotId == sid }?.label() }
                                ?: "your window"
                        } else {
                            val selSlot = viewModel.availableSlots.firstOrNull { it.slotId == selectedSlotId }
                            if (selSlot != null) slotHourRange(selSlot.start, selSlot.slotId) else "your slot"
                        }
                        val dateLabel = runCatching {
                            java.time.LocalDate.parse(viewModel.selectedBookingDate)
                                .format(java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM"))
                        }.getOrDefault(viewModel.selectedBookingDate)
                        AlertDialog(
                            onDismissRequest = { showCartConfirm = false },
                            title = { Text("Confirm your appointment") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.EventNote, contentDescription = null, tint = VedaDropRose, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (isFlex) "$dateLabel  •  Arrive between $slotLabel" else "$dateLabel  •  $slotLabel")
                                    }
                                    Text("Pay the professional directly — no charges in the app.", fontSize = 11.sp, color = Color.Gray)
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showCartConfirm = false
                                        placing = true
                                        checkoutError = null
                                        val cb: (String?) -> Unit = { err -> placing = false; checkoutError = err }
                                        // §722 — route to the multi-partner /combo checkout when the cart
                                        // spans 2+ partners; else the normal single-partner checkout.
                                        if ((cart?.partnerCount ?: 0) > 1)
                                            viewModel.checkoutCartMulti(addressId = selectedAddressId, onResult = cb)
                                        else
                                            viewModel.checkoutCart(addressId = selectedAddressId, onResult = cb)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                                ) { Text("Confirm booking", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCartConfirm = false }) {
                                    Text("Change date/time", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                }
                            },
                        )
                    }
                }
            }
        }

        // §725 Batch-B / §729 (parity C2) — "Add new location" picker + MapLibre map-pin
        // confirm step for the cart checkout (catches a GPS-vs-typed mismatch).
        if (showLocationPicker) {
            LocationPickWithMapConfirm(
                viewModel = viewModel,
                title = "Add service location",
                onDismiss = { showLocationPicker = false },
                onConfirmed = { picked ->
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
}

@Composable
fun VedaDropEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(VedaDropRose.copy(alpha = 0.1f), CircleShape)
                .border(1.dp, VedaDropRose.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VedaDropRose,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VedaDropRose,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                modifier = Modifier
                    .height(44.dp)
                    .testTag("empty_state_action_btn")
            ) {
                Text(
                    text = actionText, 
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun GuestBookingsView(onTriggerLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP Header matching corporate design guidelines
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    // §736 — match the "Bookings" bottom-nav tab + shell title (was
                    // "MY APPOINTMENTS", which read like a different feature).
                    text = "MY BOOKINGS",
                    style = MaterialTheme.typography.labelLarge,
                    color = VedaDropRose,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(VedaDropRose.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EventNote,
                    contentDescription = null,
                    tint = VedaDropRose,
                    modifier = Modifier.size(40.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Track Your Custom Drops",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Signing in allows you to conveniently monitor therapist physical travel arrival in real-time, view past and upcoming sessions, and review customized treatment results.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            Button(
                onClick = onTriggerLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("guest_bookings_login_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Access Appointments Dashboard",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// MyBookingsScreen: Dual-Tab Appointments & Requests Tracker
@Composable
fun MyBookingsScreen(viewModel: VedaDropViewModel) {
    if (!viewModel.isLoggedIn) {
        GuestBookingsView(onTriggerLogin = { viewModel.triggerLoginPrompt() })
        return
    }
    val bookings by viewModel.bookings.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val isPartner = activeUser?.role == "partner"

    // §710 — re-fetch bookings on entry. This Schedule/Bookings tab only ever read the
    // in-memory list (filled at login + after actions), so an accepted job (partner) or
    // a fresh booking (customer) could be missing until something else refreshed it.
    LaunchedEffect(Unit) { viewModel.refreshActiveBookings() }

    var selectedTab by remember { mutableStateOf(0) }
    
    // Star rating dialog states
    var showReviewDialog by remember { mutableStateOf(false) }
    var selectedBookingId by remember { mutableStateOf("") }
    var reviewRatingState by remember { mutableStateOf(5) }
    var reviewCommentState by remember { mutableStateOf("") }

    // Separate active/upcoming vs past/completed. §710 P0-10 — include the Flow-B/
    // reassignment in-flight states so an open booking still searching (or one that
    // found nobody) shows in the active tab instead of disappearing entirely.
    val activeStatuses = listOf("pending", "accepted", "assigned", "partner_on_the_way",
        "arrived", "started", "reassigning", "no_partner_found", "reassign_failed")
    val pastStatuses = listOf("completed", "cancelled", "rejected", "refunded")

    val filteredBookings = bookings.filter { booking ->
        if (selectedTab == 0) {
            booking.status in activeStatuses
        } else {
            booking.status in pastStatuses
        }
    }.let { list ->
        // §13 — partner schedule: order the active tab chronologically (by date, then
        // morning→evening) so the date separators below read straight down the day.
        if (selectedTab == 0) list.sortedBy { it.slotStartIso } else list
    }
    // §13 — pair each booking with a date-header label shown only when the day changes
    // (active/schedule tab only), driving the date separators in the list below.
    val scheduleRows: List<Pair<String?, com.example.data.BookingEntity>> =
        if (selectedTab == 0) {
            var lastDay: String? = null
            filteredBookings.map { b ->
                val d = b.slotStartIso.take(10)
                val hdr = if (d.isNotBlank() && d != lastDay) { lastDay = d; d } else null
                hdr to b
            }
        } else filteredBookings.map { null to it }

    // §734 — per-screen TopAppBar removed; shell header shows the role-aware title
    // ("My Schedule" for partners, "My Bookings" for customers — §736) + back.
    Column(modifier = Modifier.fillMaxSize()) {
        // Custom Tab Selector (Upcoming vs History)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = VedaDropRose,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = VedaDropRose
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

        if (viewModel.bookingsLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                AppointmentListSkeleton(count = 4)
            }
        } else if (filteredBookings.isEmpty()) {
            VedaDropEmptyState(
                icon = if (selectedTab == 0) Icons.Default.EventNote else Icons.Default.History,
                title = if (selectedTab == 0) {
                    if (isPartner) "No Active Tasks Yet" else "Your Schedule is Quiet"
                } else {
                    if (isPartner) "No Completed Jobs" else "No Treated Appointments"
                },
                description = if (selectedTab == 0) {
                    if (isPartner) {
                        "Keep your profile complete and active, and check the Pool Jobs to start receiving booking requests nearest to you."
                    } else {
                        "Take a sensory pause. Treat yourself to our doorstep premium facials, haircuts, facial treatments and salon experts today."
                    }
                } else {
                    if (isPartner) {
                        "Deliver high-touch five-star treatments to your doorstep clients and watch your verified history grow."
                    } else {
                        "Your completed treatments, details of experts, and digital invoice cards will reside right here."
                    }
                },
                actionText = if (selectedTab == 0) {
                    if (isPartner) "Check Open Pool" else "Explore Services"
                } else {
                    if (isPartner) "Explore Open Pool" else "Book Treatment"
                },
                onActionClick = {
                    if (isPartner) {
                        viewModel.currentScreen = Screen.PartnerOffers
                    } else {
                        viewModel.currentScreen = Screen.CustomerHome
                    }
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(scheduleRows, key = { it.second.id }) { (dayHdr, booking) ->
                    // §13 — date separator: a header before the first booking of each day.
                    if (dayHdr != null) {
                        val dayLabel = runCatching {
                            java.time.LocalDate.parse(dayHdr)
                                .format(java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM"))
                        }.getOrDefault(dayHdr)
                        Text(dayLabel, fontWeight = FontWeight.Bold, color = VedaDropRose,
                            fontSize = 13.sp, modifier = Modifier.padding(bottom = 2.dp))
                    }
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
                                    text = "₹${"%.2f".format(booking.totalPaise / 100.0)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = VedaDropRose
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))

                            // Provider / customer detail name
                            Text(
                                text = if (isPartner) {
                                    // §722 req-1 — show the customer's NAME (server reveals it
                                    // post-accept); before that a neutral label, not an address slice.
                                    "Client: ${booking.counterpartyName.ifBlank { "Customer" }}"
                                } else {
                                    "Specialist: ${booking.partnerName.orEmpty().ifBlank { "Beauty Specialist" }}"
                                },
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
                                            colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose.copy(alpha = 0.12f)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(30.dp).testTag("quick_review_btn_${booking.id}")
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = VedaDropGold, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Feedback", fontSize = 10.sp, color = VedaDropRose, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                        }
                                    } else if (!isPartner && booking.reviewRating > 0) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = VedaDropGold, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("${booking.reviewRating}/5 Stars", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        }
                                    }

                                    // 'Book Again' button for completed treatments
                                    if (!isPartner && (booking.status == "completed" || booking.status == "cancelled")) {
                                        Button(
                                            onClick = { viewModel.bookAgain(booking) },
                                            colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(30.dp).testTag("book_again_btn_${booking.id}")
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Book Again", tint = Color.White, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Book Again", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.currentScreen = Screen.BookingDetail(booking.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowForward, contentDescription = "Open Detail", tint = VedaDropRose, modifier = Modifier.size(16.dp))
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
                    color = VedaDropRose
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
                                    tint = if (star <= reviewRatingState) VedaDropGold else Color.Gray,
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
                            focusedIndicatorColor = VedaDropRose
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
                    colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                    modifier = Modifier.testTag("dialog_review_submit_btn")
                ) {
                    Text("Submit", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showReviewDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                ) {
                    Text("Cancel", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                }
            }
        )
    }
}

// ---------------- PARTNER ACCOUNT ----------------

@Composable
fun PartnerProfileScreen(viewModel: VedaDropViewModel) {
    val activeUser by viewModel.activeUser.collectAsState()
    // §725 — KYC-verified business location (lat/lon/address) shown on the profile.
    val partnerLoc by viewModel.partnerLocation.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadPartnerLocation() }
    val kycApproved = (activeUser?.kycStatus ?: "") == "approved"
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
    // §713 — Business location moved to its own page (Screen.PartnerBusinessLocation),
    // so the inline GPS-capture launcher that used to live here is gone.

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
                    color = VedaDropRose, fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                // §707 — make the role explicit + show this partner's bookable ID
                // (a client can search this exact ID to find and book them).
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(color = VedaDropRose.copy(alpha = 0.25f), shape = RoundedCornerShape(12.dp)) {
                        Text(
                            "PARTNER", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                    // §725 — Verified ✓ badge once KYC is approved (the "checked mark =
                    // verified" the founder asked for, shown right on the profile header).
                    if (kycApproved) {
                        Surface(color = SuccessGreen, shape = RoundedCornerShape(12.dp)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = "Verified", tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("VERIFIED", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    // §722 — the partner's "Your ID" is her public_code (her unique,
                    // shareable identity), NOT the internal numeric id. Fall back to the
                    // numeric id only if a code hasn't been assigned yet.
                    val yourId = (activeUser?.partnerPublicCode ?: "").ifBlank {
                        (activeUser?.profileId ?: 0).let { if (it > 0) "#$it" else "" }
                    }
                    if (yourId.isNotBlank()) {
                        Surface(color = Color.White.copy(alpha = 0.14f), shape = RoundedCornerShape(12.dp)) {
                            Text(
                                "Your ID: ${yourId.uppercase()}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = VedaDropRose)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Verification (KYC)", fontWeight = FontWeight.Bold)
                        Text((activeUser?.kycStatus ?: "not_started").replace("_", " "), fontSize = 12.sp, color = Color.Gray)
                    }
                    TextButton(onClick = { viewModel.currentScreen = Screen.PartnerKyc }) {
                        Text("Manage", color = VedaDropRose, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                    }
                }
            }

            val sub by viewModel.subscription.collectAsState()
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = VedaDropRose)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Subscription (₹99/month)", fontWeight = FontWeight.Bold)
                        val status = sub?.status ?: "—"
                        val tail = if ((sub?.daysLeft ?: 0) > 0) " · ${sub?.daysLeft}d left" else ""
                        Text(status.replaceFirstChar { it.uppercase() } + tail, fontSize = 12.sp, color = Color.Gray)
                    }
                    TextButton(onClick = { viewModel.currentScreen = Screen.PartnerSubscription }) {
                        Text("Manage", color = VedaDropRose, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EventNote, contentDescription = null, tint = VedaDropRose)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Availability", fontWeight = FontWeight.Bold)
                        Text("Working hours, days & leaves", fontSize = 12.sp, color = Color.Gray)
                    }
                    TextButton(onClick = { viewModel.currentScreen = Screen.PartnerAvailability }) {
                        Text("Manage", color = VedaDropRose, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                    }
                }
            }

            // §713 — Business location now opens a dedicated page (GPS + search +
            // service-radius slider). Without a base location the partner can't be
            // distance-ranked OR geofence-matched in customer discovery.
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.clickable { viewModel.currentScreen = Screen.PartnerBusinessLocation },
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = VedaDropRose)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Business location", fontWeight = FontWeight.Bold)
                        val loc = partnerLoc
                        val lat = loc?.lat
                        val lon = loc?.lon
                        if (loc?.hasLocation == true && lat != null && lon != null) {
                            // §725 — show the saved address + the actual coordinates of the
                            // KYC-verified location, with a Verified marker once approved.
                            val addr = (loc.address ?: "").ifBlank { "Saved location" }
                            Text(addr, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (kycApproved) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Verified · ", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.Medium)
                                }
                                Text("%.5f, %.5f".format(lat, lon), fontSize = 11.sp, color = Color.Gray)
                            }
                        } else {
                            Text("Set where you're based + how far bookings can come", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                    TextButton(onClick = { viewModel.currentScreen = Screen.PartnerBusinessLocation }) {
                        Text("Manage", color = VedaDropRose, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = VedaDropRose)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Portfolio", fontWeight = FontWeight.Bold)
                        Text("Showcase your work", fontSize = 12.sp, color = Color.Gray)
                    }
                    TextButton(onClick = { viewModel.currentScreen = Screen.PartnerPortfolio }) {
                        Text("Manage", color = VedaDropRose, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
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
                    border = BorderStroke(1.dp, VedaDropRose.copy(alpha = 0.4f)),
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = VedaDropRose)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Your partner code", fontWeight = FontWeight.Bold)
                            Text(
                                publicCode.uppercase(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 3.sp,
                                color = VedaDropRose,
                            )
                            Text("Share this code to receive a transferred booking.", fontSize = 11.sp, color = Color.Gray)
                        }
                        TextButton(onClick = {
                            clipboard.setText(AnnotatedString(publicCode.uppercase()))
                            viewModel.notify("Code copied")   // §735 — unified toast Card
                        }) { Text("Copy", color = VedaDropRose, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
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
                                Text("Edit", color = VedaDropRose, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
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
                            ) { Text("Cancel", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
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
                                colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
                                modifier = Modifier.weight(1f)
                            ) { Text("Save", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
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

            // §714 cust-auth-logout-noconfirm — confirm before logging out (one tap used
            // to immediately revoke the refresh token + wipe all caches).
            var showLogout by remember { mutableStateOf(false) }
            OutlinedButton(
                onClick = { showLogout = true },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("logout_button"),
                border = BorderStroke(1.dp, VedaDropRose),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = VedaDropRose),
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log out", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false)
            }
            if (showLogout) {
                AlertDialog(
                    onDismissRequest = { showLogout = false },
                    title = { Text("Log out?") },
                    text = { Text("You'll need your phone number and OTP to sign back in.") },
                    confirmButton = {
                        TextButton(onClick = { showLogout = false; viewModel.logout() }) { Text("Log out") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogout = false }) { Text("Cancel") }
                    },
                )
            }

            // §704 — Play-Store-required account deletion (subtle, destructive).
            Spacer(modifier = Modifier.height(8.dp))
            var showDeleteAccount by remember { mutableStateOf(false) }
            TextButton(
                onClick = { showDeleteAccount = true },
                modifier = Modifier.fillMaxWidth().testTag("delete_account_button"),
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
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ---------------- §703 COMPLAINT DETAIL THREAD + REPLY ----------------

@Composable
fun ComplaintDetailScreen(viewModel: VedaDropViewModel, complaintId: String) {
    val detail = viewModel.complaintDetail
    val messages = viewModel.complaintMessages
    var replyText by remember { mutableStateOf("") }

    // Load the thread (and refresh on reply) whenever the screen opens.
    LaunchedEffect(complaintId) { viewModel.openComplaint(complaintId) }

    // §734 — per-screen TopAppBar removed; shell header shows "Complaint" + back.
    Column(modifier = Modifier.fillMaxSize()) {
        // Subject + status header
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(detail?.subject ?: "Ticket #$complaintId", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            val status = (detail?.status ?: "open").replaceFirstChar { it.uppercase() }
            Card(colors = CardDefaults.cardColors(containerColor = VedaDropRose.copy(alpha = 0.15f))) {
                Text(status, color = VedaDropRose, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
            detail?.message?.takeIf { it.isNotBlank() }?.let {
                Text(it, fontSize = 13.sp, color = Color.Gray)
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
            items(messages, key = { it.id }) { msg ->
                val mine = msg.senderType == "customer" || msg.senderType == "partner"
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (mine) VedaDropRose.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
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
                colors = ButtonDefaults.buttonColors(containerColor = VedaDropRose),
            ) { Text("Send", maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) }
        }
    }
}

// ---------------- NOTIFICATIONS (in-app inbox) ----------------

/**
 * Bell icon with an unread-count badge, shown in the customer Home header and the
 * partner Dashboard header. Tapping it opens [Screen.Notifications]. The badge is
 * driven by [VedaDropViewModel.unreadCount] (kept fresh by the home/dashboard
 * 30s poll). White-tinted to sit on the dark gradient headers.
 */
@Composable
fun NotificationBell(viewModel: VedaDropViewModel) {
    val unread by viewModel.unreadCount.collectAsState()
    IconButton(
        onClick = { viewModel.currentScreen = Screen.Notifications },
        modifier = Modifier.testTag("notifications_bell")
    ) {
        if (unread > 0) {
            // §734 — explicit, high-contrast count badge. The default Material3 Badge
            // rendered low-contrast / looked clipped on the dark header; this draws a
            // solid rose pill with a white number + a thin ring so the count is always
            // readable on the DeepPlum gradient.
            BadgedBox(
                badge = {
                    Badge(
                        containerColor = VedaDropRose,
                        contentColor = Color.White,
                        modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                    ) {
                        Text(
                            text = if (unread > 99) "99+" else "$unread",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifications ($unread unread)",
                    tint = Color.White
                )
            }
        } else {
            Icon(
                Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = Color.White
            )
        }
    }
}

/** Full-screen notification inbox. §709 — tapping a card marks it read AND
 *  deep-links to the booking/complaint/offer it refers to (see openNotification). */
@Composable
fun NotificationsScreen(viewModel: VedaDropViewModel) {
    val notifications by viewModel.notifications.collectAsState()

    // Refresh on entry so the list is current even if the user deep-linked here.
    LaunchedEffect(Unit) { viewModel.loadNotifications() }

    // §734 — per-screen TopAppBar removed; shell header shows "Notifications" + back.
    Column(modifier = Modifier.fillMaxSize().background(DarkSlate)) {
        if (notifications.isEmpty()) {
            VedaDropEmptyState(
                icon = Icons.Default.Notifications,
                title = "All Caught Up!",
                description = "You don't have any unread updates. We'll alert you here when expert assignments, booking status updates, or special partner offers become active.",
                actionText = "Go Home",
                onActionClick = {
                    val activeUser = viewModel.activeUser.value
                    if (activeUser?.role == "partner") {
                        viewModel.currentScreen = Screen.PartnerDashboard
                    } else {
                        viewModel.currentScreen = Screen.CustomerHome
                    }
                }
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INBOX",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f)
                )
                
                if (notifications.any { !it.read }) {
                    TextButton(
                        onClick = { viewModel.markAllNotificationsRead() },
                        colors = ButtonDefaults.textButtonColors(contentColor = VedaDropRose)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark all read", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

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
                            .clickable { viewModel.openNotification(n) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (unread) VedaDropRose.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = if (unread) BorderStroke(1.dp, VedaDropRose.copy(alpha = 0.45f)) else null
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            if (unread) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 6.dp, end = 10.dp)
                                        .size(8.dp)
                                        .background(VedaDropRose, RoundedCornerShape(4.dp))
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    n.title ?: "Notification",
                                    fontSize = 15.sp,
                                    fontWeight = if (unread) FontWeight.Bold else FontWeight.SemiBold,
                                    color = Color.White
                                )
                                n.body?.takeIf { it.isNotBlank() }?.let { body ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(body, fontSize = 13.sp, color = Color.White.copy(alpha = 0.82f), lineHeight = 18.sp)
                                }
                                n.createdAt?.takeIf { it.isNotBlank() }?.let { createdAt ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        notificationTimeShort(createdAt),
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
