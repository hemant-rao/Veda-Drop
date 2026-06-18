@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepPlum
import com.example.ui.theme.GlamRose

/**
 * OTP login + signup. The user first chooses a role (customer or partner) — this
 * fixes the session's identity for good (no in-app role switching). Step 1: pick
 * role + phone → request OTP. Step 2: enter the code → verify.
 */
@Composable
fun NikhatGlowLoginScreen(viewModel: NikhatGlowViewModel) {
    val role = viewModel.loginRole
    val isPartner = role == "partner"
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(GlamRose.copy(alpha = 0.16f), MaterialTheme.colorScheme.background)
                )
            )
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(GlamRose, DeepPlum))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Spa, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
            }
            Spacer(Modifier.height(18.dp))
            Text("Nikhat Glow", fontSize = 32.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
            Text(
                "The Fragrance of Beauty",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text(
                        "Sign in / Sign up",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    // Choose role ONCE at signup. This fixes the session's identity
                    // (no in-app role switching). Hidden once an OTP is in flight.
                    if (!viewModel.otpSent) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            "I am a",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            RolePill(
                                label = "Customer",
                                selected = !isPartner,
                                modifier = Modifier.weight(1f),
                            ) { viewModel.loginRole = "customer" }
                            RolePill(
                                label = "Partner",
                                selected = isPartner,
                                modifier = Modifier.weight(1f),
                            ) { viewModel.loginRole = "partner" }
                        }
                        Text(
                            if (isPartner) "List your services and receive booking requests."
                            else "Book beauty & wellness services at your doorstep.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.filter { c -> c.isDigit() }.take(10) },
                        label = { Text("Phone number") },
                        leadingIcon = { Text("+91", modifier = Modifier.padding(start = 12.dp)) },
                        singleLine = true,
                        enabled = !viewModel.otpSent,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (viewModel.otpSent) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it.filter { c -> c.isDigit() }.take(6) },
                            label = { Text("Enter OTP") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        viewModel.devOtpHint?.let {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Dev OTP: $it",
                                style = MaterialTheme.typography.labelMedium,
                                color = GlamRose,
                            )
                        }
                    }

                    viewModel.authError?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            if (!viewModel.otpSent) viewModel.sendOtp(phone, role)
                            else viewModel.verifyOtp(phone, code)
                        },
                        enabled = !viewModel.authBusy,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GlamRose),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        if (viewModel.authBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = androidx.compose.ui.graphics.Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                if (!viewModel.otpSent) "Send OTP" else "Verify & Continue",
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    // Guests browse the customer side only; partners must sign in.
                    if (!viewModel.otpSent && !isPartner) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.isGuestMode = true },
                            border = BorderStroke(1.dp, GlamRose),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GlamRose),
                        ) {
                            Text(
                                "Bina Login ke Explore Karein (Browse as Guest)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    if (viewModel.otpSent) {
                        TextButton(
                            onClick = { viewModel.sendOtp(phone, role) },
                            enabled = !viewModel.authBusy,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Resend OTP", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        TextButton(
                            onClick = { viewModel.resetOtpFlow() },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Change number / role", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "By continuing you agree to Nikhat Glow's Terms & Privacy Policy.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RolePill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GlamRose),
        ) { Text(label, fontWeight = FontWeight.SemiBold) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(46.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, GlamRose),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = GlamRose),
        ) { Text(label, fontWeight = FontWeight.SemiBold) }
    }
}
