@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
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
import android.widget.Toast
import androidx.compose.ui.text.style.TextAlign
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
    var checkoutError by remember { mutableStateOf<String?>(null) }
    var placing by remember { mutableStateOf(false) }
    val items: List<CartItemDto> = cart?.items ?: emptyList()

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
                            Text("Rs ${item.lineTotalPaise / 100}", fontWeight = FontWeight.Bold, color = NikhatGold)
                            IconButton(onClick = { viewModel.removeCartItem(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Gray)
                            }
                        }
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Estimated subtotal", fontWeight = FontWeight.Bold)
                        Text("Rs ${(cart?.subtotalPaise ?: 0L) / 100}", fontWeight = FontWeight.Bold, color = NikhatGold)
                    }
                    Text(
                        "Estimate only - you pay the professional directly after the service.",
                        fontSize = 11.sp, color = Color.Gray
                    )
                    checkoutError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            placing = true
                            checkoutError = null
                            viewModel.checkoutCart { err ->
                                placing = false
                                checkoutError = err
                            }
                        },
                        enabled = !placing,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("send_booking_request_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = NikhatRose)
                    ) {
                        Text(if (placing) "Sending..." else "Send Booking Request", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ---------------- MY BOOKINGS / REQUESTS (role-aware list) ----------------

@Composable
fun MyBookingsScreen(viewModel: NikhatGlowViewModel) {
    val bookings by viewModel.bookings.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val isPartner = activeUser?.role == "partner"

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (isPartner) "Booking Requests" else "My Bookings", fontWeight = FontWeight.Bold) }
        )
        if (bookings.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.EventNote, contentDescription = null, modifier = Modifier.size(72.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    if (isPartner) "No booking requests yet." else "No bookings yet.",
                    fontWeight = FontWeight.Bold
                )
            }
            return
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(bookings) { booking ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.currentScreen = Screen.BookingDetail(booking.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(booking.serviceName, fontWeight = FontWeight.Bold)
                            Text(
                                if (isPartner) "Customer booking - ${booking.dateTimeSlot}"
                                else "${booking.partnerName} - ${booking.dateTimeSlot}",
                                fontSize = 12.sp, color = Color.Gray
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                booking.status.replace("_", " ").uppercase(),
                                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        IconButton(onClick = { viewModel.currentScreen = Screen.BookingDetail(booking.id) }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Open", tint = NikhatGold)
                        }
                    }
                }
            }
        }
    }
}

// ---------------- PARTNER ACCOUNT ----------------

@Composable
fun PartnerProfileScreen(viewModel: NikhatGlowViewModel) {
    val activeUser by viewModel.activeUser.collectAsState()
    var nameState by remember(activeUser?.name) { mutableStateOf(activeUser?.name ?: "") }
    var emailState by remember(activeUser?.email) { mutableStateOf(activeUser?.email ?: "") }
    var saved by remember { mutableStateOf(false) }

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
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(activeUser?.name ?: "Partner", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(
                    "Rating ${activeUser?.averageRating ?: 0f}  -  ${activeUser?.completedJobs ?: 0} jobs done",
                    color = NikhatGold, fontSize = 13.sp
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
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = NikhatGold)
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

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = NikhatGold)
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
                    border = BorderStroke(1.dp, NikhatGold.copy(alpha = 0.4f)),
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = NikhatGold)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Your partner code", fontWeight = FontWeight.Bold)
                            Text(
                                publicCode.uppercase(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 3.sp,
                                color = NikhatGold,
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

            Text("Edit profile", fontWeight = FontWeight.Bold)
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
            Button(
                onClick = {
                    viewModel.updateProfile(nameState, emailState, activeUser?.partnerBio ?: "", activeUser?.partnerExperience ?: 0)
                    saved = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = NikhatRose),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text(if (saved) "Saved" else "Save Changes") }

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
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
