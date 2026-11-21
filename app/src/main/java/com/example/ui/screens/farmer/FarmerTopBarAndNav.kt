package com.example.ui.screens.farmer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppLanguage
import com.example.ui.theme.DeepMoss
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.MistSage
import com.example.ui.theme.MutedPine
import com.example.ui.theme.PureSurface
import com.example.ui.theme.SageOutline
import com.example.ui.theme.SoftSage

object FarmerTabIndex {
    const val BOOK_SPRAY = 0
    const val LIVE_DRONE = 1
    const val BOOKINGS = 2
    const val AGRI_STORE = 3
    const val ACCOUNT = 4
}

fun farmerTabSubtitle(tab: Int, lang: AppLanguage): String = when (tab) {
    FarmerTabIndex.BOOK_SPRAY -> if (lang == AppLanguage.HI) "ड्रोन बुकिंग" else "BOOK SPRAY"
    FarmerTabIndex.LIVE_DRONE -> if (lang == AppLanguage.HI) "लाइव ड्रोन" else "LIVE DRONE"
    FarmerTabIndex.BOOKINGS -> if (lang == AppLanguage.HI) "मेरी बुकिंग" else "BOOKINGS"
    FarmerTabIndex.AGRI_STORE -> if (lang == AppLanguage.HI) "कृषि स्टोर" else "AGRI STORE"
    else -> if (lang == AppLanguage.HI) "मेरा खाता" else "ACCOUNT"
}

@Composable
fun FarmerTopHeader(
    selectedTab: Int,
    userName: String,
    appLanguage: AppLanguage,
    onToggleLanguage: (AppLanguage) -> Unit,
    onOpenAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initials = userName.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "KV" }

    Surface(
        color = PureSurface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Brush.linearGradient(listOf(ForestGreen, DeepMoss))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Eco,
                            contentDescription = "KrishiVantri",
                            tint = SoftSage,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "KrishiVantri",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = farmerTabSubtitle(selectedTab, appLanguage),
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 0.8.sp,
                            color = MutedPine,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // EN | हिंदी Language Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MistSage,
                        border = BorderStroke(1.dp, SageOutline),
                        modifier = Modifier.heightIn(min = 38.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LanguageChip(
                                label = "EN",
                                selected = appLanguage == AppLanguage.EN,
                                onClick = { onToggleLanguage(AppLanguage.EN) },
                                modifier = Modifier.testTag("farmer_lang_toggle_en")
                            )
                            LanguageChip(
                                label = "हिंदी",
                                selected = appLanguage == AppLanguage.HI,
                                onClick = { onToggleLanguage(AppLanguage.HI) },
                                modifier = Modifier.testTag("farmer_lang_toggle_hi")
                            )
                        }
                    }

                    // Profile Avatar Button (48dp touch target)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onOpenAccount)
                            .testTag("farmer_avatar_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SoftSage,
                            border = BorderStroke(1.5.dp, ForestGreen.copy(alpha = 0.35f)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreen
                                )
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = SageOutline.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun LanguageChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) PureSurface else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, SageOutline) else null,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) ForestGreen else MutedPine,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun FarmerBottomNavBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    hasActiveMission: Boolean,
    cartItemCount: Int,
    appLanguage: AppLanguage,
    modifier: Modifier = Modifier
) {
    val labels = if (appLanguage == AppLanguage.HI) {
        listOf("बुक स्प्रे", "लाइव ड्रोन", "बुकिंग", "कृषि स्टोर", "खाता")
    } else {
        listOf("Book Spray", "Live Drone", "Bookings", "Agri Store", "Account")
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = SageOutline)
        NavigationBar(
            containerColor = PureSurface,
            tonalElevation = 0.dp
        ) {
            val navColors = NavigationBarItemDefaults.colors(
                selectedIconColor = ForestGreen,
                selectedTextColor = ForestGreen,
                unselectedIconColor = MutedPine,
                unselectedTextColor = MutedPine,
                indicatorColor = SoftSage
            )

            NavigationBarItem(
                selected = selectedTab == FarmerTabIndex.BOOK_SPRAY,
                onClick = { onSelectTab(FarmerTabIndex.BOOK_SPRAY) },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.MyLocation,
                        contentDescription = labels[0]
                    )
                },
                label = {
                    Text(
                        text = labels[0],
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = navColors,
                modifier = Modifier.testTag("farmer_nav_book")
            )

            NavigationBarItem(
                selected = selectedTab == FarmerTabIndex.LIVE_DRONE,
                onClick = { onSelectTab(FarmerTabIndex.LIVE_DRONE) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (hasActiveMission) {
                                Badge(containerColor = EmeraldPulse)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sensors,
                            contentDescription = labels[1]
                        )
                    }
                },
                label = {
                    Text(
                        text = labels[1],
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = navColors,
                modifier = Modifier.testTag("farmer_nav_live")
            )

            NavigationBarItem(
                selected = selectedTab == FarmerTabIndex.BOOKINGS,
                onClick = { onSelectTab(FarmerTabIndex.BOOKINGS) },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.ReceiptLong,
                        contentDescription = labels[2]
                    )
                },
                label = {
                    Text(
                        text = labels[2],
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = navColors,
                modifier = Modifier.testTag("farmer_nav_bookings")
            )

            NavigationBarItem(
                selected = selectedTab == FarmerTabIndex.AGRI_STORE,
                onClick = { onSelectTab(FarmerTabIndex.AGRI_STORE) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (cartItemCount > 0) {
                                Badge(
                                    containerColor = ForestGreen,
                                    contentColor = PureSurface
                                ) {
                                    Text("$cartItemCount")
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Eco,
                            contentDescription = labels[3]
                        )
                    }
                },
                label = {
                    Text(
                        text = labels[3],
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = navColors,
                modifier = Modifier.testTag("farmer_nav_store")
            )

            NavigationBarItem(
                selected = selectedTab == FarmerTabIndex.ACCOUNT,
                onClick = { onSelectTab(FarmerTabIndex.ACCOUNT) },
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = labels[4]
                    )
                },
                label = {
                    Text(
                        text = labels[4],
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = navColors,
                modifier = Modifier.testTag("farmer_nav_account")
            )
        }
    }
}
