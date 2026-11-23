package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.KrishivantriDatabase
import com.example.data.model.AppLanguage
import com.example.data.model.FarmerCatalogDefaults
import com.example.data.model.FarmerProfileEntity
import com.example.data.model.LatLngPoint
import com.example.data.model.OrderEntity
import com.example.data.model.OrderStatus
import com.example.data.model.OrderWithDetails
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.remote.KrishivantriRuntimeConfig
import com.example.data.remote.SupabaseConnectionStatus
import com.example.data.repository.MapRepository
import com.example.domain.GeoPolygonCalculator
import com.example.ui.components.OnlinePaymentOption
import com.example.ui.components.RazorpayCheckoutSheet
import com.example.ui.screens.FarmerDashboardScreen
import com.example.ui.theme.KrishivantriTheme
import com.example.ui.viewmodel.DatabaseConnectionUiState
import com.example.ui.viewmodel.KrishivantriViewModel
import com.example.ui.viewmodel.MapBookingState
import com.example.ui.viewmodel.RazorpayPurpose
import com.example.ui.viewmodel.RazorpaySheetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class FarmerRedesignTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `viewModel supports agriStore cart checkout via Razorpay and Cash plus crop and rebook state`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        KrishivantriRuntimeConfig.configure(
            apiBaseUrl = BuildConfig.API_BASE_URL,
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY,
            razorpayKeyId = BuildConfig.RAZORPAY_KEY_ID,
            satelliteTileUrl = BuildConfig.SATELLITE_TILE_URL,
            satelliteApiKey = BuildConfig.SATELLITE_API_KEY
        )
        val viewModel = KrishivantriViewModel(app)

        // Language toggle
        assertEquals(AppLanguage.EN, viewModel.appLanguage.value)
        viewModel.toggleLanguage(AppLanguage.HI)
        assertEquals(AppLanguage.HI, viewModel.appLanguage.value)
        viewModel.toggleLanguage(AppLanguage.EN)

        // Crop and formula selection (immediate dispatch only, no schedule window)
        viewModel.selectSprayCrop("Cotton")
        viewModel.selectSprayFormula("foliar_boost")
        assertEquals("Cotton", viewModel.mapBookingState.value.selectedCrop)
        assertEquals("foliar_boost", viewModel.mapBookingState.value.selectedFormulaId)

        // Agri store cart + Cash checkout
        val firstProduct = FarmerCatalogDefaults.agriProducts.first()
        viewModel.addToAgriCart(firstProduct)
        viewModel.updateAgriCartQty(firstProduct.id, 1)
        assertEquals(2, viewModel.agriStoreCart.value.sumOf { it.quantity })

        viewModel.initiateAgriStoreCheckout(PaymentMethod.CASH)
        assertTrue(viewModel.agriStoreCart.value.isEmpty())
        assertEquals(1, viewModel.agriStoreReceipts.value.size)
        assertEquals(PaymentMethod.CASH, viewModel.agriStoreReceipts.value.first().paymentMethod)

        // Agri store cart + Online Razorpay sheet state
        viewModel.addToAgriCart(firstProduct)
        viewModel.initiateAgriStoreCheckout(PaymentMethod.ONLINE)
        val sheet = viewModel.razorpaySheet.value
        assertNotNull(sheet)
        assertTrue(sheet?.purpose is RazorpayPurpose.AgriStoreCheckout)
        assertEquals(firstProduct.priceInr, sheet?.amount ?: 0.0, 0.01)
        viewModel.dismissRazorpaySheet()
        assertNull(viewModel.razorpaySheet.value)

        // Rebook order polygon loads coordinates & cleans label
        val polygon = listOf(
            LatLngPoint(20.0059, 73.7897),
            LatLngPoint(20.0059, 73.7917),
            LatLngPoint(20.0079, 73.7917),
            LatLngPoint(20.0079, 73.7897)
        )
        val fakeOrder = OrderWithDetails(
            order = OrderEntity(
                id = "ord-1",
                farmerId = "frm-1",
                fieldPolygonCoordinatesJson = GeoPolygonCalculator.encodePolygon(polygon),
                acresToSpray = 3.8,
                locationAddress = "North Plot • Sugarcane (Organic Pest Control)",
                totalAmount = 1710.0,
                adminCommission = 171.0,
                pilotEarnings = 1539.0,
                paymentMethod = PaymentMethod.ONLINE.apiValue,
                paymentStatus = PaymentStatus.PAID.apiValue,
                orderStatus = OrderStatus.COMPLETED.apiValue,
                fieldCenterLat = 20.0069,
                fieldCenterLng = 73.7907
            ),
            farmerName = "Ramesh Patel",
            farmerPhone = "9876543210",
            pilotName = "Vikram Pilot",
            pilotPhone = "9876543211",
            pilotDroneType = "hexacopter",
            pilotRatingAverage = 4.9,
            polygonPoints = polygon
        )
        viewModel.rebookOrderPolygon(fakeOrder)
        assertEquals(4, viewModel.mapBookingState.value.polygonPoints.size)
        assertEquals("North Plot", viewModel.mapBookingState.value.searchQuery)
        assertTrue(viewModel.mapBookingState.value.computedAcres > 0.0)
    }

    @Test
    fun `farmerDashboardScreen renders 5 tabs with Agri Store cart, Bookings impact, and Cloud Status`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, KrishivantriDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val mapRepository = MapRepository(context, db.dao())
            val user = UserEntity(
                id = "farmer-1",
                phone = "9876543210",
                name = "Ramesh Patel",
                role = UserRole.FARMER.apiValue,
                email = "ramesh@example.com"
            )
            val profile = FarmerProfileEntity(
                userId = user.id,
                farmerIdNumber = "KV-8921",
                coins = 4
            )
            val cartState = androidx.compose.runtime.mutableStateOf<List<com.example.data.model.AgriStoreCartItem>>(emptyList())

            composeRule.setContent {
                KrishivantriTheme {
                    FarmerDashboardScreen(
                        user = user,
                        profile = profile,
                        mapRepository = mapRepository,
                        mapState = MapBookingState(hasLocationPermission = false),
                        savedFarms = emptyList(),
                        bookingOrdersForMap = emptyList(),
                        realtimeOperators = emptyList(),
                        orders = emptyList(),
                        databaseStatus = DatabaseConnectionUiState(
                            cloudStatus = SupabaseConnectionStatus.Connected("nzuhgpzwxdmgjdozodhn.supabase.co")
                        ),
                        agriStoreCart = cartState.value,
                        onAddToAgriCart = { prod ->
                            cartState.value = listOf(com.example.data.model.AgriStoreCartItem(prod, 1))
                        },
                        onLocationPermissionResult = {},
                        onResumeGpsUpdates = {},
                        onPauseGpsUpdates = {},
                        onRefreshCurrentLocation = {},
                        onSelectMapStyle = {},
                        onSearchQueryChange = {},
                        onCenterMap = {},
                        onSelectLocationSuggestion = {},
                        onMapTapPoint = {},
                        onUpdateBoundaryPoint = { _, _ -> },
                        onMapCameraMoved = {},
                        onMapLoadingChanged = {},
                        onMapError = {},
                        onUndoPolygonPoint = {},
                        onClearPolygon = {},
                        onZoomMap = {},
                        onFarmNameChange = {},
                        onSaveFarmToSupabase = {},
                        onFocusSavedFarm = {},
                        onCacheCurrentRegionOffline = {},
                        onSelectPaymentMethod = {},
                        onConsumeBookingSuccess = {},
                        onConfirmJobDone = {},
                        onRatePilot = { _, _ -> },
                        onUpdateCredentials = { _, _, _, _, _ -> },
                        onSignOut = {}
                    )
                }
            }

            composeRule.waitForIdle()
            // Bottom 5 tabs visible
            composeRule.onNodeWithTag("farmer_nav_book").assertIsDisplayed()
            composeRule.onNodeWithTag("farmer_nav_live").assertIsDisplayed()
            composeRule.onNodeWithTag("farmer_nav_bookings").assertIsDisplayed()
            composeRule.onNodeWithTag("farmer_nav_store").assertIsDisplayed()
            composeRule.onNodeWithTag("farmer_nav_account").assertIsDisplayed()

            // Book Spray screen content: larger map with expandable scrollable booking sheet & no schedule window
            composeRule.onNodeWithTag("selected_area_amount_text").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag("farmer_confirm_booking_button").assertIsDisplayed()
            composeRule.onNodeWithTag("booking_sheet_expand_toggle").assertIsDisplayed().performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("OPTIMAL SPRAY WINDOW").assertDoesNotExist()
            composeRule.onNodeWithTag("book_payment_card_online").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag("booking_sheet_expand_toggle").performClick()
            composeRule.waitForIdle()

            // Navigate to Agri Store & add item to cart
            composeRule.onNodeWithTag("farmer_nav_store").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("agri_store_list")
                .performScrollToNode(hasText("Verified Spray Formulations"))
            composeRule.onNodeWithText("Verified Spray Formulations").assertIsDisplayed()
            composeRule.onNodeWithTag("agri_store_list")
                .performScrollToNode(hasTestTag("agri_add_button_neem_astra_10k"))
            composeRule.onNodeWithTag("agri_add_button_neem_astra_10k").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("agri_store_floating_cart_bar").assertIsDisplayed()
            composeRule.onNodeWithTag("agri_store_view_cart_button").assertIsDisplayed()

            // Navigate to Bookings
            composeRule.onNodeWithTag("farmer_nav_bookings").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("FLIGHT LOG & FIELD IMPACT").assertIsDisplayed()

            // Navigate to Account & verify profile + Log Out without exposing developer DB URL/keys
            composeRule.onNodeWithTag("farmer_nav_account").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithText("Verified Farmer Account").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithText("Harvest Reward Coins").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithText("Supabase Cloud Database").assertDoesNotExist()
            composeRule.onNodeWithText("nzuhgpzwxdmgjdozodhn.supabase.co", substring = true).assertDoesNotExist()
            composeRule.onNodeWithText("RAZORPAY_KEY_ID", substring = true).assertDoesNotExist()
            composeRule.onNodeWithTag("farmer_logout_button").performScrollTo().assertIsDisplayed()
        } finally {
            db.close()
        }
    }

    @Test
    fun `razorpayCheckoutSheet lets farmer pick UPI app QR cards or netbanking without entering UPI ID`() {
        var confirmedOption: OnlinePaymentOption? = null
        composeRule.setContent {
            KrishivantriTheme {
                RazorpayCheckoutSheet(
                    state = RazorpaySheetState(
                        title = "Krishivantri Spray Booking",
                        subtitle = "3.8 acres • Nashik",
                        amount = 1900.0,
                        purpose = RazorpayPurpose.FarmerOrderCheckout(
                            locationAddress = "Nashik",
                            polygonPoints = emptyList()
                        )
                    ),
                    onConfirmPayment = { confirmedOption = it },
                    onDismiss = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("UPI VPA / Handle").assertDoesNotExist()
        composeRule.onNodeWithTag("payment_option_gpay").assertIsDisplayed()
        composeRule.onNodeWithTag("payment_option_phonepe").assertIsDisplayed()
        composeRule.onNodeWithTag("payment_option_paytm").assertIsDisplayed()
        composeRule.onNodeWithTag("payment_option_upi_qr").assertIsDisplayed()
        composeRule.onNodeWithTag("payment_option_cards").assertIsDisplayed()
        composeRule.onNodeWithTag("payment_option_netbanking").assertIsDisplayed()

        composeRule.onNodeWithTag("payment_option_netbanking").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("razorpay_pay_button").performClick()
        assertEquals(OnlinePaymentOption.NETBANKING, confirmedOption)
    }
}
