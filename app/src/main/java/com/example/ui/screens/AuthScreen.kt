package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import com.example.shared.R as SharedR
import com.example.ui.components.KrishivantriBrandMark
import com.example.ui.theme.AlertRust
import com.example.ui.theme.AlertRustBg
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.SoftSage
import com.example.ui.viewmodel.AuthUiState

/**
 * Dedicated Farmer Login / Signup Screen for the standalone "Krishivantri" app.
 * No Drone Pilot signup options or public platform statistics are exposed here.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AuthScreen(
    state: AuthUiState,
    onToggleSignUp: (Boolean) -> Unit,
    onSubmitFarmer: (name: String, phone: String, farmerId: String) -> Unit,
    onSubmitAdmin: (phone: String, passcode: String, onError: (String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var farmerId by rememberSaveable { mutableStateOf("") }
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
                subtitle = "Smart Drone Spraying for Your Farms"
            )

            Surface(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.height(176.dp)) {
                    Image(
                        painter = painterResource(id = SharedR.drawable.img_krishivantri_hero_1794983437632),
                        contentDescription = "Agricultural drone spraying lush green crop terraces",
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
                                        ForestGreen.copy(alpha = 0.90f)
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
                                imageVector = Icons.Rounded.Agriculture,
                                contentDescription = null,
                                tint = SoftSage,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "FARMER EDITION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SoftSage
                            )
                        }
                        Text(
                            text = "Map Your Land & Book Spraying",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Outline your field on satellite maps, see exact acre pricing, and track pilot arrival live.",
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
                            label = "New Farmer Signup",
                            selected = state.isSignUpMode,
                            onClick = { onToggleSignUp(true) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("farmer_auth_tab_signup")
                        )
                        FarmerAuthTab(
                            label = "Existing Farmer",
                            selected = !state.isSignUpMode,
                            onClick = { onToggleSignUp(false) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("farmer_auth_tab_login")
                        )
                    }

                    AnimatedVisibility(visible = state.isSignUpMode) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Farmer Full Name") },
                            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("farmer_auth_name_input")
                        )
                    }

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { if (it.length <= 15) phone = it },
                        label = { Text("Mobile Number") },
                        leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("farmer_auth_phone_input")
                    )

                    AnimatedVisibility(visible = state.isSignUpMode) {
                        OutlinedTextField(
                            value = farmerId,
                            onValueChange = { farmerId = it },
                            label = { Text("Farmer ID Number") },
                            placeholder = { Text("e.g. KISAN-MH-4821") },
                            leadingIcon = { Icon(Icons.Rounded.Badge, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("farmer_auth_id_input")
                        )
                    }

                    val errorMsg = state.errorMessage
                    if (!errorMsg.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AlertRustBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = AlertRust,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { onSubmitFarmer(name, phone, farmerId) },
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("farmer_auth_submit_button")
                    ) {
                        Text(
                            text = if (state.isSignUpMode) {
                                "Verify Farmer ID & Continue"
                            } else {
                                "Sign In to My Farms"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Text(
                        text = "Are you a drone pilot? Use the Krishivantri Drone Pilot app to accept jobs.",
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
                    text = "Krishivantri • Precision Agritech for Farmers",
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
private fun FarmerAuthTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(44.dp)
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
                color = if (selected) ForestGreen else MaterialTheme.colorScheme.onSurfaceVariant
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
