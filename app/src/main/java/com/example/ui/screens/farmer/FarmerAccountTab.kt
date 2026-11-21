package com.example.ui.screens.farmer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.FarmerProfileEntity
import com.example.data.model.OrderStatus
import com.example.data.model.OrderWithDetails
import com.example.data.model.UserEntity
import com.example.ui.theme.AlertRust
import com.example.ui.theme.CharcoalLeaf
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.HarvestAmber
import com.example.ui.theme.HarvestGoldBg
import com.example.ui.theme.MistSage
import com.example.ui.theme.MutedPine
import com.example.ui.theme.PureSurface
import com.example.ui.theme.SageOutline
import com.example.ui.theme.SoftSage
import java.util.Locale

@Composable
fun FarmerAccountTab(
    user: UserEntity,
    profile: FarmerProfileEntity?,
    orders: List<OrderWithDetails>,
    onOpenEditCredentials: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Farmer Profile & Credentials Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = PureSurface,
            border = BorderStroke(1.dp, SageOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SoftSage,
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = ForestGreen,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalLeaf
                            )
                            Text(
                                text = "Verified Farmer Account",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPulse
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenEditCredentials,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("account_change_credentials_button")
                    ) {
                        Icon(Icons.Rounded.ManageAccounts, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit")
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MistSage)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AccountCredentialRow(Icons.Rounded.Person, "Full Name", user.name)
                    AccountCredentialRow(Icons.Rounded.Email, "Email Address", user.email.ifBlank { "Not linked" })
                    AccountCredentialRow(Icons.Rounded.Phone, "Mobile Number", user.phone)
                    AccountCredentialRow(Icons.Rounded.Badge, "Farmer ID Number", profile?.farmerIdNumber ?: "Not Assigned")
                }

                Text(
                    text = "Profile and saved farm boundaries stay securely synced with your account.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedPine
                )
            }
        }

        // 2. Harvest Coins & Reward Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = HarvestGoldBg,
            border = BorderStroke(1.dp, HarvestAmber.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Harvest Reward Coins",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen
                    )
                    Text(
                        text = "Earn +1 Harvest Coin for every spray mission confirmed via 'Job Done'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ForestGreen.copy(alpha = 0.85f)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = HarvestAmber,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${profile?.coins ?: 0}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 4. Summary Stats Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PureSurface,
            border = BorderStroke(1.dp, SageOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            val completed = orders.filter { it.statusEnum == OrderStatus.COMPLETED }
            val totalAcreage = completed.sumOf { it.order.acresToSpray }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn("${orders.size}", "Total Bookings", ForestGreen)
                StatColumn("${completed.size}", "Completed", EmeraldPulse)
                StatColumn(String.format(Locale.US, "%.1f", totalAcreage), "Acres Protected", ForestGreen)
            }
        }

        // 5. Log Out CTA
        Button(
            onClick = onSignOut,
            colors = ButtonDefaults.buttonColors(containerColor = AlertRust),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("farmer_logout_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Log Out")
        }
    }
}

@Composable
private fun AccountCredentialRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MutedPine)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = CharcoalLeaf,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatColumn(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MutedPine
        )
    }
}
