@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSlate
import com.example.ui.theme.DeepPlum
import com.example.ui.theme.GlamGold
import com.example.ui.theme.GlamRose

/**
 * Partner ₹99/month listing subscription — the connector model's only revenue.
 * Shows trial/active status + days left, lets the partner subscribe / cancel,
 * and lists past payments. All data is server-backed (no hardcoded values).
 */
@Composable
fun PartnerSubscriptionScreen(viewModel: NikhatGlowViewModel) {
    val sub by viewModel.subscription.collectAsState()
    val payments by viewModel.subscriptionPayments.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadSubscription() }

    val status = sub?.status ?: "none"
    val isActive = sub?.isActive == true
    val priceRupees = (sub?.pricePaise ?: 9900L) / 100

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(DeepPlum, DarkSlate)))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.currentScreen = Screen.PartnerDashboard }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text("Subscription", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text("₹$priceRupees / month", color = GlamGold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Stay discoverable and accept booking requests. You collect payment directly from customers — the platform never takes a cut.",
                    color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Status card
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Status", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        AssistChip(
                            onClick = {},
                            label = { Text(status.replaceFirstChar { it.uppercase() }) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isActive) GlamRose.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
                            )
                        )
                    }
                    if ((sub?.daysLeft ?: 0) > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text("${sub?.daysLeft} day(s) remaining in the current period.", fontSize = 13.sp, color = Color.Gray)
                    }
                    sub?.currentPeriodEnd?.let {
                        Spacer(Modifier.height(4.dp))
                        Text("Renews / ends: ${it.take(10)}", fontSize = 12.sp, color = Color.Gray)
                    }
                    if (status == "trial") {
                        Spacer(Modifier.height(4.dp))
                        Text("You're on a free trial. Subscribe to keep your listing active afterwards.", fontSize = 12.sp, color = GlamGold)
                    }
                }
            }

            viewModel.subscriptionError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            // Actions
            if (isActive && sub?.autoRenew == true) {
                OutlinedButton(
                    onClick = { viewModel.cancelSubscriptionNow() },
                    enabled = !viewModel.subscriptionBusy,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    border = BorderStroke(1.dp, GlamRose),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GlamRose),
                ) { Text(if (viewModel.subscriptionBusy) "Please wait…" else "Cancel auto-renew") }
            } else {
                Button(
                    onClick = { viewModel.subscribeNow() },
                    enabled = !viewModel.subscriptionBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = GlamRose),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                ) { Text(if (viewModel.subscriptionBusy) "Please wait…" else "Subscribe — ₹$priceRupees/month") }
            }

            // Payment history
            if (payments.isNotEmpty()) {
                Text("Payment history", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                payments.forEach { p ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GlamRose, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("₹${p.amountPaise / 100}", fontWeight = FontWeight.SemiBold)
                                Text((p.at ?: p.periodStart ?: "").take(10), fontSize = 12.sp, color = Color.Gray)
                            }
                            Text(p.status.replaceFirstChar { it.uppercase() }, fontSize = 12.sp, color = GlamGold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
