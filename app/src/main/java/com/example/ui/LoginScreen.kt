@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepPlum
import com.example.ui.theme.VedaDropRose
import com.example.ui.theme.AccentBronze

import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import com.example.R

/**
 * OTP login + signup. Re-designed by World's Best UI/UX Designer & AI Coding Expert.
 * High clarity, simple natural language, easy for all ages and literacy levels.
 */
@Composable
fun VedaDropLoginScreen(viewModel: VedaDropViewModel) {
    val role = viewModel.loginRole
    val isPartner = role == "partner"
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    // §738 — explicit Terms/Privacy consent + a "read the full terms" dialog.
    var acceptedTerms by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        VedaDropRose.copy(alpha = 0.12f),
                        DeepPlum.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Fixed Logo Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BrandLogo(
                    modifier = Modifier.size(70.dp),
                    contentDescription = "Veda Drop Logo"
                )
                
                Spacer(Modifier.height(4.dp))
                
                Text(
                    text = "Veda Drop",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 0.5.sp
                )
                
                Text(
                    text = "Beauty & Wellness",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
            
            // Scrollable Content Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Main Interactive Form Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = if (!viewModel.otpSent) "Welcome" else "Verify OTP",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        
                        Text(
                            text = if (!viewModel.otpSent) 
                                "Select role & sign in" 
                            else 
                                "Code sent to +91 $phone",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        // Step 1: Role Selector - Beautiful high-end horizontal layout
                        if (!viewModel.otpSent) {
                            Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp), 
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VisualRoleCard(
                                label = "Customer",
                                hindiLabel = "ग्राहक (बुक करें)",
                                icon = Icons.Filled.Person,
                                selected = !isPartner,
                                modifier = Modifier.weight(1f)
                            ) { 
                                viewModel.loginRole = "customer" 
                            }
                            
                            VisualRoleCard(
                                label = "Partner",
                                hindiLabel = "पार्टनर (कमाएं)",
                                icon = Icons.Filled.ContentCut,
                                selected = isPartner,
                                modifier = Modifier.weight(1f)
                            ) { 
                                viewModel.loginRole = "partner" 
                            }
                        }
                    }

                    // Mobile Number Section
                    Text(
                        text = if (!viewModel.otpSent) "Mobile Number" else "Verification Code",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    if (!viewModel.otpSent) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { input -> 
                                phone = input.filter { it.isDigit() }.take(10) 
                            },
                            placeholder = { Text("Enter 10-digit number") },
                            leadingIcon = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PhoneAndroid,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "+91", 
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp
                                    )
                                }
                            },
                            singleLine = true,
                            enabled = !viewModel.otpSent,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VedaDropRose,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                        )
                    } else {
                        // OTP verification input
                        OutlinedTextField(
                            value = code,
                            onValueChange = { input -> 
                                code = input.filter { it.isDigit() }.take(6) 
                            },
                            placeholder = { Text("Enter 6-digit Code") },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Filled.LockOpen, 
                                    contentDescription = null,
                                    tint = VedaDropRose,
                                    modifier = Modifier.padding(start = 12.dp)
                                ) 
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VedaDropRose,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                        )
                        
                        viewModel.devOtpHint?.let { hint ->
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(VedaDropRose.copy(alpha = 0.1f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Auto-filled Dev OTP: $hint",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentBronze,
                                )
                            }
                        }
                    }

                    // Error display with clear styling
                    viewModel.authError?.let { err ->
                        Spacer(Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ $err", 
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    
                    // §738 — explicit Terms/Privacy consent before sign-in/sign-up. The
                    // checkbox gates the "Send code" action; a tap on "Read..." opens the
                    // full Terms & Privacy (which spells out the connector / who's-responsible
                    // model in plain language). Shown only on the phone step, not the OTP step.
                    if (!viewModel.otpSent) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = acceptedTerms,
                                onCheckedChange = { acceptedTerms = it },
                                colors = CheckboxDefaults.colors(checkedColor = VedaDropRose),
                                modifier = Modifier.testTag("terms_checkbox")
                            )
                            Text(
                                text = "I agree to the Terms & Privacy",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { acceptedTerms = !acceptedTerms }
                            )
                        }
                        TextButton(
                            onClick = { showTermsDialog = true },
                            contentPadding = PaddingValues(start = 12.dp),
                            modifier = Modifier.testTag("read_terms_btn")
                        ) {
                            Text(
                                "Read Terms & Privacy",
                                color = VedaDropRose,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    // Main Action Button (highly intuitive design)
                    Button(
                        onClick = {
                            if (!viewModel.otpSent) viewModel.sendOtp(phone, role)
                            else viewModel.verifyOtp(phone, code)
                        },
                        enabled = !viewModel.authBusy &&
                            (if (!viewModel.otpSent) (phone.length == 10 && acceptedTerms) else code.length == 6),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VedaDropRose,
                            disabledContainerColor = VedaDropRose.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                    ) {
                        if (viewModel.authBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (!viewModel.otpSent) "Send SMS Code" else "Verify & Continue",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // §738 — connector/marketplace transparency, right under the action
                    // button. States plainly that Veda Drop is not the service provider.
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Veda Drop connects you with independent, verified women professionals. Each professional is independent and fully responsible for her service.",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Optional Quick-Browse for Customer
                    if (!viewModel.otpSent && !isPartner) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { viewModel.isGuestMode = true },
                            border = BorderStroke(1.5.dp, VedaDropRose),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VedaDropRose),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Explore App First",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Option modifiers after OTP is sent (Reset flow / Resend)
                    if (viewModel.otpSent) {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { viewModel.sendOtp(phone, role) },
                                enabled = !viewModel.authBusy,
                            ) { 
                                Text(
                                    text = "Resend OTP", 
                                    color = MaterialTheme.colorScheme.primary, 
                                    fontWeight = FontWeight.SemiBold
                                ) 
                            }
                            
                            TextButton(
                                onClick = { viewModel.resetOtpFlow() },
                            ) { 
                                Text(
                                    text = "Change Number", 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                ) 
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            
            // Reassurance and trust note
            Text(
                text = "🛡️ Safe & women-only — every member is a verified woman. See Terms & Privacy for more details.",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Normal,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // §738 — full Terms of Service & Privacy, in plain language. Spells out the
            // connector model: Veda Drop connects the two sides and is NOT the provider;
            // each professional is independent and responsible for her own service.
            if (showTermsDialog) {
                AlertDialog(
                    onDismissRequest = { showTermsDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showTermsDialog = false }) {
                            Text("Got it", color = VedaDropRose, fontWeight = FontWeight.Bold)
                        }
                    },
                    title = { Text("Terms of Service & Privacy", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            TermsSection(
                                "How Veda Drop works",
                                "Veda Drop is a technology platform that connects customers with independent beauty & wellness professionals (“Professionals”). We provide discovery, booking, in-app chat, location and safety tools only. Veda Drop is NOT the provider of any beauty or wellness service."
                            )
                            TermsSection(
                                "Independent professionals",
                                "Every Professional on Veda Drop is independent and self-employed. The Professional you choose — not Veda Drop — is solely responsible for the service she provides, including its quality, safety, conduct, timing, pricing and any licences or compliance required by law. Customers engage Professionals directly."
                            )
                            TermsSection(
                                "A platform for both sides",
                                "Veda Drop connects both customers and Professionals. Each member is responsible for their own conduct and for honouring the bookings they make."
                            )
                            TermsSection(
                                "Payments",
                                "You pay the Professional directly. Veda Drop never handles your service payment and adds ₹0 to it — our only charge is the Professional’s ₹99/month listing fee."
                            )
                            TermsSection(
                                "Safety — women only",
                                "• Every customer and Professional is a verified woman.\n• Phone numbers stay hidden until a booking is accepted, and are hidden again afterwards.\n• A Professional sets out only after you confirm the visit on chat or call.\n• Tap SOS on the booking screen to reach 112 / women helpline 1091 anytime.\n• Never share OTPs, money or documents in chat. If anything feels unsafe, cancel and leave — no penalty."
                            )
                            TermsSection(
                                "Your privacy",
                                "We collect only what we need to run the service: your phone number (for OTP login), your service location (to match you with nearby Professionals) and your profile/booking details. We do not sell your personal data. Your number is shared with the other party only after a booking is accepted, and hidden again afterwards."
                            )
                            TermsSection(
                                "Liability",
                                "Veda Drop facilitates connections and provides safety tools, but is not a party to the service agreement between a customer and a Professional. To the extent permitted by law, Veda Drop is not responsible for the acts, omissions or conduct of any customer or Professional. Any dispute about a service is between the customer and the Professional; our Support team will help where it can."
                            )
                            Text(
                                "By ticking the box and continuing, you confirm you have read and agree to these Terms and the Privacy note above.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                )
            }
        }
        }
    }
}

/**
 * Beautiful visual card for Role Selection. High accessibility representation.
 */
@Composable
private fun VisualRoleCard(
    label: String,
    hindiLabel: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) VedaDropRose else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val bgColor = if (selected) VedaDropRose.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface

    Card(
        modifier = modifier
            .height(60.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() }
                .padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) VedaDropRose.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) VedaDropRose else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Spacer(Modifier.width(8.dp))
                
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = hindiLabel,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Selected",
                        tint = VedaDropRose,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/** §738 — one titled paragraph inside the Terms & Privacy dialog. */
@Composable
private fun TermsSection(title: String, body: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = VedaDropRose)
        Spacer(Modifier.height(2.dp))
        Text(
            body,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Accent highlight color helper
private val HighlightBronze = Color(0xFF64D2FF)
