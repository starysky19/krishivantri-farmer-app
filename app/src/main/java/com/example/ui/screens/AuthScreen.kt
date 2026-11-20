package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.shared.R as SharedR
import com.example.ui.components.KrishivantriBrandMark
import com.example.ui.theme.AlertRust
import com.example.ui.theme.AlertRustBg
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.SoftSage
import com.example.ui.viewmodel.AuthUiState
import java.util.Locale

/**
 * Dedicated Farmer Email OTP Login & Registration screen for Krishivantri by NexusTekFusion.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AuthScreen(
    state: AuthUiState,
    onToggleSignUp: (Boolean) -> Unit,
    onRegisterFarmer: (name: String, email: String, phone: String, farmerId: String) -> Unit,
    onRequestLoginOtp: (email: String) -> Unit,
    onVerifyLoginOtp: (otp: String) -> Unit,
    onEditLoginEmail: () -> Unit,
    onSubmitAdmin: (phone: String, passcode: String, onError: (String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable { mutableStateOf("") }
    var regEmail by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var farmerId by rememberSaveable { mutableStateOf("") }

    var loginEmail by rememberSaveable { mutableStateOf("") }
    var otpCode by rememberSaveable { mutableStateOf("") }
    var showHiddenAdminDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            KrishivantriBrandMark(
                title = "Krishivantri",
                subtitle = "NexusTekFusion • Precision Drone Spraying for Farmers"
            )

            Surface(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.height(168.dp)) {
                    Image(
                        painter = painterResource(id = SharedR.drawable.img_krishivantri_hero_1794983437632),
                        contentDescription = "Agricultural spraying drone flying above crop fields",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        ForestGreen.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Eco,
                                contentDescription = null,
                                tint = SoftSage,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "FARMER SERVICE PORTAL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SoftSage
                            )
                        }
                        Text(
                            text = "Precision Drone Crop Spraying",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Map your farm boundary, track arrival of licensed drone pilots, and release payout upon job completion.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftSage
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp)
                    ) {
                        FarmerAuthTab(
                            label = "Farmer Sign In",
                            selected = !state.isSignUpMode,
                            onClick = {
                                otpCode = ""
                                onToggleSignUp(false)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("farmer_auth_tab_login")
                        )
                        FarmerAuthTab(
                            label = "New Farmer Sign Up",
                            selected = state.isSignUpMode,
                            onClick = {
                                otpCode = ""
                                onToggleSignUp(true)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("farmer_auth_tab_signup")
                        )
                    }

                    AnimatedContent(
                        targetState = state.isSignUpMode,
                        label = "farmerAuthModeTransition"
                    ) { isSignUp ->
                        if (isSignUp) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Full Name") },
                                    placeholder = { Text("e.g. Ramesh Patel") },
                                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("farmer_auth_name_input")
                                )

                                OutlinedTextField(
                                    value = regEmail,
                                    onValueChange = { regEmail = it },
                                    label = { Text("Email Address *") },
                                    placeholder = { Text("e.g. ramesh@example.com") },
                                    leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("farmer_auth_email_input")
                                )

                                OutlinedTextField(
                                    value = farmerId,
                                    onValueChange = { farmerId = it },
                                    label = { Text("Farmer ID / Registration No.") },
                                    placeholder = { Text("e.g. MH-FRM-00248") },
                                    leadingIcon = { Icon(Icons.Rounded.Badge, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("farmer_auth_id_input")
                                )

                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { if (it.length <= 15) phone = it },
                                    label = { Text("Mobile Number (10 digits)") },
                                    placeholder = { Text("e.g. 9876543210") },
                                    leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("farmer_auth_phone_input")
                                )

                                AuthErrorBanner(errorMessage = state.errorMessage)

                                Button(
                                    onClick = {
                                        onRegisterFarmer(name, regEmail, phone, farmerId)
                                    },
                                    enabled = !state.isLoading,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("farmer_auth_submit_button")
                                ) {
                                    if (state.isLoading) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            text = "Register & Continue",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        } else {
                            // Existing User Login via Email OTP
                            if (state.otpRequestedEmail == null) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = loginEmail,
                                        onValueChange = { loginEmail = it },
                                        label = { Text("Registered Email Address") },
                                        placeholder = { Text("e.g. ramesh@example.com") },
                                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("farmer_login_email_input")
                                    )

                                    AuthErrorBanner(errorMessage = state.errorMessage)

                                    Button(
                                        onClick = {
                                            otpCode = ""
                                            onRequestLoginOtp(loginEmail)
                                        },
                                        enabled = !state.isLoading,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .testTag("farmer_get_otp_button")
                                    ) {
                                        if (state.isLoading) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.MarkEmailRead,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "Get OTP",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Receive a one-time verification code from the official NexusTekFusion mail service. Your session stays active until you log out.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                // OTP Verification Screen
                                val requestedEmail = state.otpRequestedEmail.orEmpty()
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = SoftSage.copy(alpha = 0.7f),
                                        border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.25f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.MarkEmailRead,
                                                    contentDescription = null,
                                                    tint = ForestGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = "Verify Email OTP",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = EmeraldPulse,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = requestedEmail,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = ForestGreen,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            TextButton(
                                                onClick = {
                                                    otpCode = ""
                                                    onEditLoginEmail()
                                                },
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("farmer_otp_change_email_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text("Change")
                                            }
                                        }
                                    }

                                    state.otpInfoMessage?.takeIf { it.isNotBlank() }?.let { info ->
                                        Text(
                                            text = info,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    OutlinedTextField(
                                        value = otpCode,
                                        onValueChange = { raw ->
                                            val digitsOnly = raw.filter { it.isDigit() }
                                            if (digitsOnly.length <= 6) {
                                                otpCode = digitsOnly
                                            }
                                        },
                                        label = { Text("Enter 6-Digit Email OTP") },
                                        placeholder = { Text("6-digit verification code") },
                                        leadingIcon = { Icon(Icons.Rounded.Pin, contentDescription = null) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("farmer_otp_code_input")
                                    )

                                    // 5-Minute Expiry Countdown & Attempt Indicator
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.LockClock,
                                                contentDescription = null,
                                                tint = if (state.otpExpiresInSeconds > 0) ForestGreen else AlertRust,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            val mins = state.otpExpiresInSeconds / 60
                                            val secs = state.otpExpiresInSeconds % 60
                                            val expiryLabel = if (state.otpExpiresInSeconds > 0) {
                                                String.format(Locale.US, "Expires in %02d:%02d", mins, secs)
                                            } else {
                                                "OTP expired"
                                            }
                                            Text(
                                                text = expiryLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (state.otpExpiresInSeconds > 0) ForestGreen else AlertRust,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        if (state.otpFailedAttempts > 0) {
                                            val remaining = (state.maxOtpAttempts - state.otpFailedAttempts).coerceAtLeast(0)
                                            Text(
                                                text = "$remaining attempt${if (remaining == 1) "" else "s"} left",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AlertRust,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    AuthErrorBanner(errorMessage = state.errorMessage)

                                    Button(
                                        onClick = { onVerifyLoginOtp(otpCode) },
                                        enabled = !state.isLoading &&
                                            otpCode.length >= 6 &&
                                            state.otpExpiresInSeconds > 0 &&
                                            state.otpFailedAttempts < state.maxOtpAttempts,
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(54.dp)
                                            .testTag("farmer_verify_otp_button")
                                    ) {
                                        if (state.isLoading) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.VerifiedUser,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = "Verify & Sign In",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            otpCode = ""
                                            onRequestLoginOtp(requestedEmail)
                                        },
                                        enabled = !state.isLoading && state.otpResendCooldownSeconds == 0,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("farmer_resend_otp_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = if (state.otpResendCooldownSeconds > 0) {
                                                "Resend OTP in ${state.otpResendCooldownSeconds}s"
                                            } else {
                                                "Resend OTP"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = "Krishivantri escrow protects your payments: pilot payout is only credited after you confirm 'Job Done'.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { showHiddenAdminDialog = true }
                    )
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NexusTekFusion • Krishivantri Farmer Edition",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }

    if (showHiddenAdminDialog) {
        FarmerHiddenAdminDialog(
            onSubmit = onSubmitAdmin,
            onDismiss = { showHiddenAdminDialog = false }
        )
    }
}

@Composable
private fun AuthErrorBanner(errorMessage: String?) {
    AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AlertRustBg,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("farmer_auth_error_banner")
        ) {
            Text(
                text = errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = AlertRust,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun FarmerAuthTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        tonalElevation = if (selected) 1.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) ForestGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FarmerHiddenAdminDialog(
    onSubmit: (phone: String, passcode: String, onError: (String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var adminPhone by remember { mutableStateOf("") }
    var passcode by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Restricted Operations Access",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = adminPhone,
                    onValueChange = { adminPhone = it },
                    label = { Text("Admin Identifier") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = passcode,
                    onValueChange = { passcode = it },
                    label = { Text("Security Passcode") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val err = errorText
                if (!err.isNullOrBlank()) {
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = AlertRust
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(adminPhone, passcode) { msg -> errorText = msg } },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                Text("Authenticate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
