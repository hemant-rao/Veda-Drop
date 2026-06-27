@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepPlum
import com.example.ui.theme.VedaDropRose
import com.example.ui.theme.AccentBronze

/**
 * §758 — email/mobile + password authentication.
 *
 * Sign-in: one identifier field (the user types EITHER their email OR their 10-digit
 * mobile — both were verified at registration) + password.
 *
 * Register: the DLT-free waterfall — Name/Email/Password/Mobile → email OTP (auto-skipped
 * when the email provider isn't configured) → phone SMS OTP → account created + signed in.
 *
 * The legacy phone-OTP login was retired (founder directive, §758).
 */
@Composable
fun VedaDropLoginScreen(viewModel: VedaDropViewModel) {
    val role = viewModel.loginRole
    val isPartner = role == "partner"
    val isRegister = viewModel.authMode == "register"
    val isForgot = viewModel.authMode == "forgot"   // §763 — forgot/reset password

    // Sign-in fields
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Register fields
    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var emailCode by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }

    // §763 forgot/reset fields
    var forgotId by remember { mutableStateOf("") }
    var resetCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    // §738 — explicit Terms/Privacy consent (shown on sign-up only).
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
                        // Heading reflects exactly where the user is in the flow.
                        val (title, subtitle) = when {
                            isForgot && viewModel.forgotStep == "request" ->
                                "Reset password" to "We'll send a code to your email or mobile"
                            isForgot ->
                                "Enter reset code" to (viewModel.forgotSentMessage
                                    ?: "Enter the code we sent and choose a new password")
                            !isRegister -> "Welcome back" to "Sign in with your email or mobile"
                            viewModel.regStep == "email" -> "Verify your email" to "Enter the 6-digit code we emailed to $regEmail"
                            viewModel.regStep == "phone" -> "Verify your mobile" to "Enter the 6-digit code sent to +91 $regPhone"
                            else -> "Create your account" to "It only takes a minute"
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        // Role selector — only when picking how to start (sign-in, the register
                        // form, or the forgot-password request step; the role scopes the lookup).
                        val showRolePicker = (!isRegister && !isForgot) ||
                            (isRegister && viewModel.regStep == "form") ||
                            (isForgot && viewModel.forgotStep == "request")
                        if (showRolePicker) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VisualRoleCard(
                                    label = "Customer",
                                    hindiLabel = "ग्राहक (बुक करें)",
                                    icon = Icons.Filled.Person,
                                    selected = !isPartner,
                                    modifier = Modifier.weight(1f)
                                ) { viewModel.loginRole = "customer" }
                                VisualRoleCard(
                                    label = "Partner",
                                    hindiLabel = "पार्टनर (कमाएं)",
                                    icon = Icons.Filled.ContentCut,
                                    selected = isPartner,
                                    modifier = Modifier.weight(1f)
                                ) { viewModel.loginRole = "partner" }
                            }
                        }

                        // ── Body per mode/step ───────────────────────────────────
                        when {
                            // ===== FORGOT — STEP 1: request a code (email or mobile) =====
                            isForgot && viewModel.forgotStep == "request" -> {
                                FieldLabel("Email or Mobile Number")
                                AuthTextField(
                                    value = forgotId,
                                    onValueChange = { forgotId = it },
                                    placeholder = "you@email.com or 98765 43210",
                                    leading = Icons.Filled.Email,
                                    keyboardType = KeyboardType.Text,
                                    testTag = "forgot_identifier",
                                )
                                ErrorBox(viewModel.authError)
                                Spacer(Modifier.height(12.dp))
                                PrimaryButton(
                                    text = "Send Code",
                                    busy = viewModel.authBusy,
                                    enabled = !viewModel.authBusy && forgotId.isNotBlank(),
                                    onClick = { viewModel.requestPasswordReset(forgotId) },
                                    testTag = "forgot_request",
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Remembered it?",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(
                                        onClick = { viewModel.cancelForgot() },
                                        modifier = Modifier.testTag("forgot_back_to_login")
                                    ) {
                                        Text("Sign in", color = VedaDropRose, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // ===== FORGOT — STEP 2: enter code + new password =====
                            isForgot -> {
                                FieldLabel(
                                    if (viewModel.forgotChannel == "sms") "SMS Verification Code"
                                    else "Email Verification Code"
                                )
                                CodeField(
                                    value = resetCode,
                                    onValueChange = { resetCode = it.filter { c -> c.isDigit() }.take(6) },
                                    testTag = "forgot_code",
                                )
                                viewModel.forgotDevOtp?.let { hint ->
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(VedaDropRose.copy(alpha = 0.1f))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Dev OTP: $hint",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentBronze,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                FieldLabel("New Password (min 8 characters)")
                                PasswordField(
                                    value = newPassword,
                                    onValueChange = { newPassword = it },
                                    visible = passwordVisible,
                                    onToggleVisible = { passwordVisible = !passwordVisible },
                                    placeholder = "Choose a new password",
                                    testTag = "forgot_new_password",
                                )
                                ErrorBox(viewModel.authError)
                                Spacer(Modifier.height(12.dp))
                                PrimaryButton(
                                    text = "Reset Password",
                                    busy = viewModel.authBusy,
                                    enabled = !viewModel.authBusy && resetCode.length == 6 &&
                                        newPassword.length >= 8,
                                    onClick = { viewModel.submitPasswordReset(resetCode, newPassword) },
                                    testTag = "forgot_submit",
                                )
                                Spacer(Modifier.height(8.dp))
                                StepFooter(
                                    onResend = { viewModel.resendPasswordReset() },
                                    onBack = { viewModel.cancelForgot() },
                                    backLabel = "Cancel",
                                    busy = viewModel.authBusy,
                                )
                            }

                            // ===== SIGN IN =====
                            !isRegister -> {
                                FieldLabel("Email or Mobile Number")
                                AuthTextField(
                                    value = identifier,
                                    onValueChange = { identifier = it },
                                    placeholder = "you@email.com or 98765 43210",
                                    leading = Icons.Filled.Email,
                                    keyboardType = KeyboardType.Text,
                                    testTag = "login_identifier",
                                )
                                Spacer(Modifier.height(10.dp))
                                FieldLabel("Password")
                                PasswordField(
                                    value = password,
                                    onValueChange = { password = it },
                                    visible = passwordVisible,
                                    onToggleVisible = { passwordVisible = !passwordVisible },
                                    placeholder = "Your password",
                                    testTag = "login_password",
                                )

                                ErrorBox(viewModel.authError)
                                Spacer(Modifier.height(12.dp))
                                PrimaryButton(
                                    text = "Sign In",
                                    busy = viewModel.authBusy,
                                    enabled = !viewModel.authBusy && identifier.isNotBlank() && password.isNotBlank(),
                                    onClick = { viewModel.login(identifier, password) },
                                    testTag = "login_submit",
                                )
                                // §763 — forgot-password entry point.
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    TextButton(
                                        onClick = { viewModel.startForgotPassword() },
                                        modifier = Modifier.testTag("forgot_password_link")
                                    ) {
                                        Text(
                                            "Forgot password?",
                                            color = VedaDropRose,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "New to Veda Drop?",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(
                                        onClick = { viewModel.switchAuthMode("register") },
                                        modifier = Modifier.testTag("go_register")
                                    ) {
                                        Text("Create account", color = VedaDropRose, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // ===== REGISTER — STEP 1: form =====
                            viewModel.regStep == "form" -> {
                                FieldLabel("Full Name")
                                AuthTextField(
                                    value = regName,
                                    onValueChange = { regName = it },
                                    placeholder = "Your name",
                                    leading = Icons.Filled.Person,
                                    keyboardType = KeyboardType.Text,
                                    testTag = "reg_name",
                                )
                                Spacer(Modifier.height(8.dp))
                                FieldLabel("Email")
                                AuthTextField(
                                    value = regEmail,
                                    onValueChange = { regEmail = it.trim() },
                                    placeholder = "you@email.com",
                                    leading = Icons.Filled.Email,
                                    keyboardType = KeyboardType.Email,
                                    testTag = "reg_email",
                                )
                                Spacer(Modifier.height(8.dp))
                                FieldLabel("Password (min 8 characters)")
                                PasswordField(
                                    value = regPassword,
                                    onValueChange = { regPassword = it },
                                    visible = passwordVisible,
                                    onToggleVisible = { passwordVisible = !passwordVisible },
                                    placeholder = "Create a password",
                                    testTag = "reg_password",
                                )
                                Spacer(Modifier.height(8.dp))
                                FieldLabel("Mobile Number")
                                PhoneField(
                                    value = regPhone,
                                    onValueChange = { regPhone = it.filter { c -> c.isDigit() }.take(10) },
                                    testTag = "reg_phone",
                                )

                                ErrorBox(viewModel.authError)
                                Spacer(Modifier.height(10.dp))

                                // Terms consent (sign-up only).
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
                                    TextButton(
                                        onClick = { showTermsDialog = true },
                                        modifier = Modifier.testTag("read_terms_btn")
                                    ) {
                                        Text("Read", color = VedaDropRose, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    }
                                }

                                PrimaryButton(
                                    text = "Continue",
                                    busy = viewModel.authBusy,
                                    enabled = !viewModel.authBusy && regName.isNotBlank() &&
                                        regEmail.contains("@") && regPassword.length >= 8 &&
                                        regPhone.length == 10 && acceptedTerms,
                                    onClick = { viewModel.registerStart(regName, regEmail, regPassword, regPhone) },
                                    testTag = "reg_submit",
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Already have an account?",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(onClick = { viewModel.switchAuthMode("login") }) {
                                        Text("Sign in", color = VedaDropRose, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // ===== REGISTER — STEP 2a: email OTP =====
                            viewModel.regStep == "email" -> {
                                FieldLabel("Email Verification Code")
                                CodeField(
                                    value = emailCode,
                                    onValueChange = { emailCode = it.filter { c -> c.isDigit() }.take(6) },
                                    testTag = "reg_email_code",
                                )
                                ErrorBox(viewModel.authError)
                                Spacer(Modifier.height(12.dp))
                                PrimaryButton(
                                    text = "Verify Email",
                                    busy = viewModel.authBusy,
                                    enabled = !viewModel.authBusy && emailCode.length == 6,
                                    onClick = { viewModel.registerEmailVerify(emailCode) },
                                    testTag = "reg_email_verify",
                                )
                                Spacer(Modifier.height(8.dp))
                                StepFooter(
                                    onResend = { viewModel.registerEmailResend() },
                                    onBack = { viewModel.cancelRegister() },
                                    backLabel = "Cancel",
                                    busy = viewModel.authBusy,
                                )
                            }

                            // ===== REGISTER — STEP 2b: phone SMS OTP =====
                            else -> {
                                FieldLabel("SMS Verification Code")
                                CodeField(
                                    value = smsCode,
                                    onValueChange = { smsCode = it.filter { c -> c.isDigit() }.take(6) },
                                    testTag = "reg_sms_code",
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
                                            text = "Dev OTP: $hint",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentBronze,
                                        )
                                    }
                                }
                                ErrorBox(viewModel.authError)
                                Spacer(Modifier.height(12.dp))
                                PrimaryButton(
                                    text = "Verify & Finish",
                                    busy = viewModel.authBusy,
                                    enabled = !viewModel.authBusy && smsCode.length == 6,
                                    onClick = { viewModel.registerPhoneVerifySms(smsCode) },
                                    testTag = "reg_sms_verify",
                                )
                                Spacer(Modifier.height(8.dp))
                                StepFooter(
                                    onResend = { viewModel.resendRegPhoneSms() },
                                    onBack = { viewModel.cancelRegister() },
                                    backLabel = "Cancel",
                                    busy = viewModel.authBusy,
                                )
                            }
                        }

                        // §738 — connector/marketplace transparency (shown on the entry screens).
                        if ((!isRegister && !isForgot) || (isRegister && viewModel.regStep == "form")) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Veda Drop connects you with independent, verified women professionals. Each professional is independent and fully responsible for her service.",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        // Guest quick-browse (customer, sign-in screen only).
                        if (!isRegister && !isForgot && !isPartner) {
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
                                Text("Explore App First", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    text = "🛡️ Safe & women-only — every member is a verified woman. See Terms & Privacy for more details.",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                if (showTermsDialog) {
                    TermsAndPrivacyDialog(onDismiss = { showTermsDialog = false })
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable pieces
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = VedaDropRose,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface
)

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leading: ImageVector,
    keyboardType: KeyboardType,
    testTag: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(leading, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp))
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = authFieldColors(),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag(testTag),
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    placeholder: String,
    testTag: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp))
        },
        trailingIcon = {
            TextButton(onClick = onToggleVisible) {
                Text(if (visible) "Hide" else "Show", color = VedaDropRose, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold)
            }
        },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        colors = authFieldColors(),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag(testTag),
    )
}

@Composable
private fun PhoneField(value: String, onValueChange: (String) -> Unit, testTag: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Enter 10-digit number") },
        leadingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, end = 8.dp)
            ) {
                Icon(Icons.Filled.PhoneAndroid, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("+91", fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        colors = authFieldColors(),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag(testTag),
    )
}

@Composable
private fun CodeField(value: String, onValueChange: (String) -> Unit, testTag: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Enter 6-digit code") },
        leadingIcon = {
            Icon(Icons.Filled.LockOpen, contentDescription = null, tint = VedaDropRose,
                modifier = Modifier.padding(start = 12.dp))
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = authFieldColors(),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag(testTag),
    )
}

@Composable
private fun ErrorBox(error: String?) {
    error?.let { err ->
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
}

@Composable
private fun PrimaryButton(
    text: String,
    busy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = VedaDropRose,
            disabledContainerColor = VedaDropRose.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag(testTag),
    ) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = Color.White,
                    modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun StepFooter(
    onResend: () -> Unit,
    onBack: () -> Unit,
    backLabel: String,
    busy: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onResend, enabled = !busy) {
            Text("Resend Code", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = onBack) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(2.dp))
                Text(backLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** Beautiful visual card for Role Selection. High accessibility representation. */
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
        modifier = modifier.height(60.dp),
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
                Column(verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
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

/** §738 — full Terms of Service & Privacy, in plain language. */
@Composable
private fun TermsAndPrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
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
                    "We collect only what we need to run the service: your name, email and mobile number (for login and verification), your service location (to match you with nearby Professionals) and your profile/booking details. We do not sell your personal data. Your number is shared with the other party only after a booking is accepted, and hidden again afterwards."
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

/** §738 — one titled paragraph inside the Terms & Privacy dialog. */
@Composable
private fun TermsSection(title: String, body: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = VedaDropRose)
        Spacer(Modifier.height(2.dp))
        Text(body, fontSize = 12.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
