package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DroneType
import com.example.data.model.PilotProfileEntity
import com.example.data.model.UserEntity
import com.example.ui.theme.AlertRust
import com.example.ui.theme.AlertRustBg
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.HarvestAmber
import com.example.ui.theme.HarvestGoldBg
import com.example.ui.theme.SoftSage
import com.example.ui.theme.VerifiedTeal
import com.example.ui.theme.VerifiedTealBg
import java.util.Locale

@Composable
fun PilotAccountDetailsTab(
    user: UserEntity,
    profile: PilotProfileEntity?,
    onSaveAccountDetails: (
        name: String,
        droneType: DroneType,
        tankCapacityText: String,
        bankUpiId: String,
        licenseUri: String?
    ) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable(user.id) { mutableStateOf(user.name) }
    var bankUpiId by rememberSaveable(user.id) { mutableStateOf(profile?.bankUpiId.orEmpty()) }
    var selectedDroneType by rememberSaveable(user.id) {
        mutableStateOf(DroneType.fromApi(profile?.droneType ?: DroneType.QUADCOPTER.apiValue))
    }
    var tankCapacityText by rememberSaveable(user.id) {
        mutableStateOf(
            profile?.tankCapacity?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            } ?: "10"
        )
    }
    var licenseUri by rememberSaveable(user.id) { mutableStateOf(profile?.licenseDocumentUri.orEmpty()) }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(user.name, profile?.bankUpiId, profile?.droneType, profile?.tankCapacity, profile?.licenseDocumentUri) {
        name = user.name
        if (profile != null) {
            bankUpiId = profile.bankUpiId
            selectedDroneType = DroneType.fromApi(profile.droneType)
            tankCapacityText = if (profile.tankCapacity % 1.0 == 0.0) {
                profile.tankCapacity.toInt().toString()
            } else {
                profile.tankCapacity.toString()
            }
            licenseUri = profile.licenseDocumentUri
        }
    }

    val licensePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            licenseUri = uri.toString()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Account Summary & License Verification Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = if (profile?.isLicenseVerified == true) VerifiedTealBg else HarvestGoldBg,
                border = BorderStroke(
                    1.dp,
                    if (profile?.isLicenseVerified == true) VerifiedTeal.copy(alpha = 0.45f)
                    else HarvestAmber.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (profile?.isLicenseVerified == true) {
                            Icons.Rounded.Verified
                        } else {
                            Icons.Rounded.Badge
                        },
                        contentDescription = "Pilot verification status",
                        tint = if (profile?.isLicenseVerified == true) VerifiedTeal else HarvestAmber,
                        modifier = Modifier.size(30.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (profile?.isLicenseVerified == true) {
                                "Verified Drone Pilot"
                            } else {
                                "License Pending Verification"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen
                        )
                        Text(
                            text = "Rating: ${String.format(Locale.US, "%.1f", profile?.ratingAverage ?: 5.0)}★ (${profile?.ratingCount ?: 0} reviews) • ID: ${user.id.take(8)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Editable Credentials & Payout Details Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Account & Payout Credentials",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            validationError = null
                        },
                        label = { Text("Pilot Full Name") },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pilot_account_name_input")
                    )

                    OutlinedTextField(
                        value = user.phone,
                        onValueChange = {},
                        enabled = false,
                        label = { Text("Registered Mobile Number") },
                        leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pilot_account_phone_text")
                    )

                    OutlinedTextField(
                        value = bankUpiId,
                        onValueChange = {
                            bankUpiId = it
                            validationError = null
                        },
                        label = { Text("Payout Bank UPI ID") },
                        placeholder = { Text("e.g. pilotname@okaxis") },
                        supportingText = {
                            Text("Your 90% mission earnings are routed to this UPI handle.")
                        },
                        leadingIcon = { Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pilot_account_upi_input")
                    )

                    Text(
                        text = "Drone Platform",
                        style = MaterialTheme.typography.labelLarge,
                        color = ForestGreen
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DroneType.entries.forEach { drone ->
                            val active = selectedDroneType == drone
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedDroneType = drone }
                                    .border(
                                        width = if (active) 1.5.dp else 1.dp,
                                        color = if (active) ForestGreen else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .testTag("pilot_account_drone_${drone.apiValue}"),
                                color = if (active) SoftSage else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = drone.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                        color = if (active) ForestGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = tankCapacityText,
                        onValueChange = {
                            tankCapacityText = it
                            validationError = null
                        },
                        label = { Text("Spray Tank Capacity (Liters)") },
                        leadingIcon = { Icon(Icons.Rounded.WaterDrop, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pilot_account_tank_input")
                    )

                    OutlinedButton(
                        onClick = {
                            licensePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (licenseUri.isNotBlank()) EmeraldPulse else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pilot_account_license_button")
                    ) {
                        Icon(
                            imageVector = if (licenseUri.isNotBlank()) {
                                Icons.Rounded.CheckCircle
                            } else {
                                Icons.Rounded.UploadFile
                            },
                            contentDescription = "Update DGCA license document",
                            tint = if (licenseUri.isNotBlank()) EmeraldPulse else ForestGreen
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (licenseUri.isNotBlank()) {
                                "DGCA License Attached (Tap to Replace)"
                            } else {
                                "Attach DGCA License Photo"
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = ForestGreen
                        )
                    }

                    val err = validationError
                    if (!err.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AlertRustBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = AlertRust,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val cleanName = name.trim()
                            val cleanUpi = bankUpiId.trim()
                            val parsedTank = tankCapacityText.trim().toDoubleOrNull()
                            when {
                                cleanName.isBlank() -> validationError = "Enter your full name."
                                cleanUpi.isBlank() || !cleanUpi.contains("@") ->
                                    validationError = "Enter a valid UPI ID (for example: name@okaxis)."
                                parsedTank == null || parsedTank <= 0.0 ->
                                    validationError = "Enter a valid spray tank capacity in liters."
                                else -> {
                                    validationError = null
                                    onSaveAccountDetails(
                                        cleanName,
                                        selectedDroneType,
                                        tankCapacityText,
                                        cleanUpi,
                                        licenseUri.takeIf { it.isNotBlank() }
                                    )
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pilot_save_account_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Save Account Changes")
                    }
                }
            }

            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AlertRust.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pilot_account_sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = null,
                    tint = AlertRust,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", color = AlertRust, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
