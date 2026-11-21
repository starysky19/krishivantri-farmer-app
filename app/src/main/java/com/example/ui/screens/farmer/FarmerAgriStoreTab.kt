package com.example.ui.screens.farmer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.Biotech
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.HeadsetMic
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AgriStoreCartItem
import com.example.data.model.AgriStoreOrderReceipt
import com.example.data.model.AgriStoreProduct
import com.example.data.model.FarmerCatalogDefaults
import com.example.data.model.PaymentMethod
import com.example.ui.components.formatInr
import com.example.ui.theme.AlertRust
import com.example.ui.theme.AlertRustBg
import com.example.ui.theme.CharcoalLeaf
import com.example.ui.theme.DeepMoss
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.HarvestAmber
import com.example.ui.theme.HarvestGoldBg
import com.example.ui.theme.MeadowMint
import com.example.ui.theme.MistSage
import com.example.ui.theme.MutedPine
import com.example.ui.theme.PureSurface
import com.example.ui.theme.SageOutline
import com.example.ui.theme.SoftSage
import com.example.ui.theme.VerifiedTeal
import com.example.ui.theme.VerifiedTealBg

@Composable
fun FarmerAgriStoreTab(
    cart: List<AgriStoreCartItem>,
    receipts: List<AgriStoreOrderReceipt>,
    onAddToCart: (AgriStoreProduct) -> Unit,
    onUpdateCartQty: (String, Int) -> Unit,
    onClearCart: () -> Unit,
    onCheckoutAgriStore: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showCartDialog by remember { mutableStateOf(false) }
    var showAgronomistDialog by remember { mutableStateOf(false) }

    val filteredProducts = remember(searchQuery, selectedCategory) {
        FarmerCatalogDefaults.agriProducts.filter { prod ->
            val catMatch = selectedCategory == "All" || prod.category == selectedCategory
            val q = searchQuery.trim()
            val qMatch = q.isEmpty() ||
                prod.title.contains(q, ignoreCase = true) ||
                prod.highlightTag.contains(q, ignoreCase = true) ||
                prod.category.contains(q, ignoreCase = true)
            catMatch && qMatch
        }
    }

    val totalItems = cart.sumOf { it.quantity }
    val cartTotal = cart.sumOf { it.lineTotal }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("agri_store_list"),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = if (cart.isNotEmpty()) 96.dp else 18.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Search Bar Pill
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = PureSurface,
                    border = BorderStroke(1.dp, SageOutline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search Agri Store",
                            tint = MutedPine,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search organic inputs, bio-stimulants…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedPine,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = CharcoalLeaf),
                                cursorBrush = SolidColor(ForestGreen),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("agri_store_search_input")
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search", tint = MutedPine)
                            }
                        }
                    }
                }
            }

            // 2. Category Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FarmerCatalogDefaults.agriCategories) { category ->
                        val selected = selectedCategory == category
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) ForestGreen else PureSurface,
                            border = BorderStroke(1.dp, if (selected) ForestGreen else SageOutline),
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedCategory = category }
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) PureSurface else CharcoalLeaf
                                )
                            }
                        }
                    }
                }
            }

            // 3. Govt. Certified Bio-Inputs Banner (Reference Image 2)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(listOf(ForestGreen, DeepMoss)))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Verified, contentDescription = null, tint = MeadowMint, modifier = Modifier.size(16.dp))
                            Text(
                                text = "GOVT. CERTIFIED BIO-INPUTS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SoftSage
                            )
                        }
                        Text(
                            text = "Formulated for 16L–30L Drone Nozzles",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PureSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.LocalShipping, contentDescription = null, tint = SoftSage, modifier = Modifier.size(15.dp))
                            Text(
                                text = "Zero Nozzle Clog Guaranteed • Free Delivery to Village Hub",
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftSage
                            )
                        }
                    }
                }
            }

            // 4. Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Verified Spray Formulations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalLeaf
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SoftSage
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${filteredProducts.size} Available • Drone Tested",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ForestGreen
                            )
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(13.dp))
                        }
                    }
                }
            }

            // 5. Formulation Cards
            items(filteredProducts, key = { it.id }) { product ->
                val cartQty = cart.firstOrNull { it.product.id == product.id }?.quantity ?: 0
                AgriProductCard(
                    product = product,
                    cartQty = cartQty,
                    onAdd = { onAddToCart(product) },
                    onIncrease = { onUpdateCartQty(product.id, 1) },
                    onDecrease = { onUpdateCartQty(product.id, -1) }
                )
            }

            // 6. Recent Confirmed Agri Store Orders (if any)
            if (receipts.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = PureSurface,
                        border = BorderStroke(1.dp, SageOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Recent Bio-Input Orders",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalLeaf
                            )
                            receipts.take(3).forEach { receipt ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${receipt.orderId} • ${receipt.itemCount} items",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CharcoalLeaf
                                        )
                                        Text(
                                            text = receipt.razorpayPaymentId?.let { "Razorpay Paid ($it)" }
                                                ?: receipt.paymentMethod.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ForestGreen
                                        )
                                    }
                                    Text(
                                        text = formatInr(receipt.totalAmount),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ForestGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 7. Certified Drone Agronomist Banner (Reference Image 2)
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MistSage,
                    border = BorderStroke(1.dp, SageOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { showAgronomistDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ForestGreen,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.HeadsetMic, contentDescription = null, tint = PureSurface, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Unsure of tank-mix compatibility?",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalLeaf
                            )
                            Text(
                                text = "Check verified dilution & nozzle guidelines for 16L–30L spray tanks.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedPine
                            )
                            Text(
                                text = "View Drone Dosage Guide →",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen
                            )
                        }
                    }
                }
            }
        }

        // Sticky Floating Cart Bar (Reference Image 2)
        if (cart.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CharcoalLeaf,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("agri_store_floating_cart_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ForestGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.ShoppingBag, contentDescription = null, tint = PureSurface, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                text = "$totalItems item${if (totalItems > 1) "s" else ""} in cart",
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftSage
                            )
                            Text(
                                text = formatInr(cartTotal),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PureSurface
                            )
                        }
                    }

                    Button(
                        onClick = { showCartDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MeadowMint, contentColor = CharcoalLeaf),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("agri_store_view_cart_button")
                    ) {
                        Text("View Cart", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    if (showCartDialog) {
        AgriStoreCartCheckoutDialog(
            cart = cart,
            onUpdateQty = onUpdateCartQty,
            onClear = {
                onClearCart()
                showCartDialog = false
            },
            onCheckout = { method ->
                showCartDialog = false
                onCheckoutAgriStore(method)
            },
            onDismiss = { showCartDialog = false }
        )
    }

    if (showAgronomistDialog) {
        AlertDialog(
            onDismissRequest = { showAgronomistDialog = false },
            icon = { Icon(Icons.Rounded.Science, contentDescription = null, tint = ForestGreen) },
            title = { Text("Drone Nozzle & Tank-Mix Guide", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "• Standard 10L–16L Tank covers 1 Acre per flight at 2.5m AGL height.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Mix Neem Astra (250ml) or Nano Zinc (100ml) cleanly before filtering into the drone hopper.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Spray during morning (6:00–9:30 AM) or evening (4:00–6:30 PM) below 10 km/h wind for maximum leaf absorption.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAgronomistDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                ) {
                    Text("Got It")
                }
            }
        )
    }
}

@Composable
private fun AgriProductCard(
    product: AgriStoreProduct,
    cartQty: Int,
    onAdd: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    val artIcon = when (product.category) {
        "Bio-Pesticides" -> Icons.Rounded.Eco
        "Nano Fertilizers" -> Icons.Rounded.Biotech
        "Bio-Shield" -> Icons.Rounded.Shield
        else -> Icons.Rounded.Spa
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PureSurface,
        border = BorderStroke(1.dp, SageOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Visual Illustration Thumbnail Box
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.verticalGradient(listOf(SoftSage, MistSage))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = artIcon,
                        contentDescription = product.title,
                        tint = ForestGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ForestGreen,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(5.dp)
                    ) {
                        Text(
                            text = product.badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PureSurface,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SoftSage
                        ) {
                            Text(
                                text = product.highlightTag,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = ForestGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Star, contentDescription = null, tint = HarvestAmber, modifier = Modifier.size(14.dp))
                            Text(
                                text = "${product.rating} (${product.reviewCount})",
                                style = MaterialTheme.typography.labelSmall,
                                color = CharcoalLeaf
                            )
                        }
                    }

                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalLeaf
                    )
                    Text(
                        text = product.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedPine
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = RoundedCornerShape(6.dp), color = VerifiedTealBg) {
                            Text(
                                text = product.secondaryTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = VerifiedTeal,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = MistSage) {
                            Text(
                                text = product.dosageLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = CharcoalLeaf,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = SageOutline.copy(alpha = 0.6f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formatInr(product.priceInr),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalLeaf
                        )
                        Text(
                            text = formatInr(product.mrpInr),
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedPine,
                            textDecoration = TextDecoration.LineThrough
                        )
                        if (product.discountPercent > 0) {
                            Surface(shape = RoundedCornerShape(6.dp), color = AlertRustBg) {
                                Text(
                                    text = "${product.discountPercent}% OFF",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AlertRust,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = product.footerNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedPine
                    )
                }

                if (cartQty <= 0) {
                    Button(
                        onClick = onAdd,
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("agri_add_button_${product.id}")
                    ) {
                        Icon(Icons.Rounded.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add to Cart", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SoftSage,
                        border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.4f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onDecrease,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease quantity", tint = ForestGreen)
                            }
                            Text(
                                text = "$cartQty",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            IconButton(
                                onClick = onIncrease,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Increase quantity", tint = ForestGreen)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgriStoreCartCheckoutDialog(
    cart: List<AgriStoreCartItem>,
    onUpdateQty: (String, Int) -> Unit,
    onClear: () -> Unit,
    onCheckout: (PaymentMethod) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMethod by remember { mutableStateOf(PaymentMethod.ONLINE) }
    val total = cart.sumOf { it.lineTotal }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agri Store Cart", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                cart.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.product.title,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalLeaf
                            )
                            Text(
                                text = "${formatInr(item.product.priceInr)} × ${item.quantity} = ${formatInr(item.lineTotal)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedPine
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onUpdateQty(item.product.id, -1) }) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Remove one", tint = ForestGreen)
                            }
                            Text("${item.quantity}", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onUpdateQty(item.product.id, 1) }) {
                                Icon(Icons.Rounded.Add, contentDescription = "Add one", tint = ForestGreen)
                            }
                        }
                    }
                }

                HorizontalDivider(color = SageOutline)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethod.entries.forEach { method ->
                        val chosen = selectedMethod == method
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (chosen) SoftSage else MistSage,
                            border = BorderStroke(1.dp, if (chosen) ForestGreen else SageOutline),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedMethod = method }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (method == PaymentMethod.ONLINE) Icons.Rounded.AccountBalanceWallet else Icons.Rounded.Payments,
                                    contentDescription = null,
                                    tint = ForestGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (method == PaymentMethod.ONLINE) "Razorpay" else "Cash COD",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (chosen) ForestGreen else CharcoalLeaf
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Payable", fontWeight = FontWeight.Bold)
                    Text(formatInr(total), fontWeight = FontWeight.Bold, color = ForestGreen)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCheckout(selectedMethod) },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                modifier = Modifier.testTag("agri_store_confirm_checkout_button")
            ) {
                Text(
                    if (selectedMethod == PaymentMethod.ONLINE) {
                        "Pay Online • ${formatInr(total)}"
                    } else {
                        "Place Cash Order • ${formatInr(total)}"
                    }
                )
            }
        },
        dismissButton = {
            Row {
                OutlinedButton(onClick = onClear) { Text("Clear") }
                Spacer(Modifier.width(6.dp))
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}
