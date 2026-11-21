package com.example.data.model

enum class AppLanguage(val code: String) {
    EN("EN"),
    HI("HI")
}

data class SprayObjectiveOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String
)

data class AgriStoreProduct(
    val id: String,
    val badge: String,
    val highlightTag: String,
    val category: String,
    val title: String,
    val subtitle: String,
    val dosageLabel: String,
    val priceInr: Double,
    val mrpInr: Double,
    val rating: Double,
    val reviewCount: Int,
    val footerNote: String,
    val secondaryTag: String = "Drone Ready"
) {
    val discountPercent: Int
        get() = if (mrpInr > priceInr) {
            (((mrpInr - priceInr) / mrpInr) * 100.0).toInt()
        } else {
            0
        }
}

data class AgriStoreCartItem(
    val product: AgriStoreProduct,
    val quantity: Int
) {
    val lineTotal: Double get() = product.priceInr * quantity
}

data class AgriStoreOrderReceipt(
    val orderId: String,
    val itemCount: Int,
    val totalAmount: Double,
    val paymentMethod: PaymentMethod,
    val razorpayPaymentId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

object FarmerCatalogDefaults {
    val cropTypes = listOf("Sugarcane", "Cotton", "Wheat", "Paddy", "Grapes", "Soybean")

    val sprayObjectives = listOf(
        SprayObjectiveOption(
            id = "organic_pest",
            title = "Organic Pest Control",
            subtitle = "Neem extract blend • Safe for pollinators",
            badge = "Standard"
        ),
        SprayObjectiveOption(
            id = "foliar_boost",
            title = "Foliar Fertilizer Boost",
            subtitle = "Rapid N-P-K liquid uptake",
            badge = "+₹0"
        ),
        SprayObjectiveOption(
            id = "micro_nutrients",
            title = "Micro-Nutrients & Zinc",
            subtitle = "Ultra-fine droplet saturation",
            badge = "+₹0"
        )
    )

    val agriCategories = listOf("All", "Bio-Pesticides", "Nano Fertilizers", "Growth Tonics", "Bio-Shield")

    val agriProducts = listOf(
        AgriStoreProduct(
            id = "neem_astra_10k",
            badge = "ORGANIC",
            highlightTag = "Best for Bollworm & Aphids",
            category = "Bio-Pesticides",
            title = "Neem Astra 10,000 PPM",
            subtitle = "Cold-Pressed Bio-Pesticide • 1 Litre",
            dosageLabel = "250ml / acre",
            priceInr = 450.0,
            mrpInr = 600.0,
            rating = 4.8,
            reviewCount = 320,
            footerNote = "Covers ~4 acres with drone"
        ),
        AgriStoreProduct(
            id = "nano_zinc_boron",
            badge = "NANO TECH",
            highlightTag = "Fast Foliar Absorption",
            category = "Nano Fertilizers",
            title = "Nano Zinc & Boron Chelate",
            subtitle = "Micronutrient Spray • 500ml",
            dosageLabel = "100ml / acre",
            priceInr = 380.0,
            mrpInr = 450.0,
            rating = 4.9,
            reviewCount = 190,
            footerNote = "Micro-droplet spray formulation"
        ),
        AgriStoreProduct(
            id = "trichoderma_viride",
            badge = "BIO-SHIELD",
            highlightTag = "Root Rot & Wilt Shield",
            category = "Bio-Shield",
            title = "Trichoderma Viride",
            subtitle = "Bio-Fungicide Solution • 1 Litre",
            dosageLabel = "300ml / acre",
            priceInr = 320.0,
            mrpInr = 400.0,
            rating = 4.7,
            reviewCount = 140,
            footerNote = "Safe for drip & drone spray",
            secondaryTag = "Admin Verified"
        ),
        AgriStoreProduct(
            id = "seaweed_growth_tonic",
            badge = "BIO-TONIC",
            highlightTag = "Crop Vigour & Flowering",
            category = "Growth Tonics",
            title = "Seaweed Plant Growth Tonic",
            subtitle = "Cold-Water Bio-Stimulant • 500ml",
            dosageLabel = "150ml / acre",
            priceInr = 420.0,
            mrpInr = 500.0,
            rating = 4.9,
            reviewCount = 410,
            footerNote = "Boosts tillers & leaf area"
        )
    )
}
