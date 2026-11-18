package com.example

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Krishivantri", appName)
  }

  @Test
  fun `mainActivity launches without crashing`() {
    var uncaughtError: Throwable? = null
    val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
      uncaughtError = throwable
    }
    try {
      val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
      Shadows.shadowOf(Looper.getMainLooper()).idle()
      val activity = controller.get()
      assertNotNull(activity)
      assertNull("Unexpected startup crash: ${uncaughtError?.stackTraceToString()}", uncaughtError)
    } finally {
      Thread.setDefaultUncaughtExceptionHandler(previousHandler)
    }
  }

  @Test
  fun `acceptOrder calculates pilot-to-field distance and updates ETA in real time`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.local.KrishivantriDatabase::class.java
    ).allowMainThreadQueries().build()
    try {
      val repo = com.example.data.repository.KrishivantriRepository(db)
      val farmerResult = repo.registerFarmer("Ramesh Farmer", "9876543210", "FRM-101")
      val pilotResult = repo.registerPilot(
        name = "Suresh Pilot",
        phone = "9876543211",
        droneType = com.example.data.model.DroneType.QUADCOPTER,
        tankCapacity = 12.0,
        licenseDocumentUri = "content://license",
        bankUpiId = "suresh@upi"
      )
      val farmer = (farmerResult as com.example.data.repository.AuthResult.Success).user
      val pilot = (pilotResult as com.example.data.repository.AuthResult.Success).user

      val fieldPolygon = listOf(
        com.example.data.model.LatLngPoint(20.0059, 73.7897),
        com.example.data.model.LatLngPoint(20.0059, 73.7917),
        com.example.data.model.LatLngPoint(20.0079, 73.7917),
        com.example.data.model.LatLngPoint(20.0079, 73.7897)
      )
      val order = repo.placeOrder(
        farmerId = farmer.id,
        locationAddress = "Nashik Plot",
        polygonPoints = fieldPolygon,
        paymentMethod = com.example.data.model.PaymentMethod.ONLINE
      )

      // Pilot accepts from ~5 km south
      val pilotStart = com.example.data.model.LatLngPoint(19.9619, 73.7907)
      val acceptResult = repo.acceptOrder(pilot.id, order.id, pilotStart)
      assertEquals(com.example.data.repository.AcceptOrderResult.Success, acceptResult)

      val acceptedOrder = db.dao().getOrderById(order.id)!!
      assertNotNull(acceptedOrder.distanceKm)
      assertNotNull(acceptedOrder.acceptedAt)
      assertTrue(
        "Expected ~5 km distance, got ${acceptedOrder.distanceKm}",
        acceptedOrder.distanceKm!! in 4.5..5.5
      )
      assertTrue(
        "Expected ETA >= 10 mins, got ${acceptedOrder.pilotEtaMinutes}",
        acceptedOrder.pilotEtaMinutes >= 10
      )

      // Pilot moves closer (~1 km south)
      repo.updatePilotLiveLocation(pilot.id, 19.9979, 73.7907)
      val updatedOrder = db.dao().getOrderById(order.id)!!
      val initialDist = acceptedOrder.distanceKm!!
      val updatedDist = updatedOrder.distanceKm!!
      assertTrue(
        "Expected updated distance < initial distance",
        updatedDist < initialDist
      )
      assertTrue(
        "Expected updated ETA < initial ETA",
        updatedOrder.pilotEtaMinutes < acceptedOrder.pilotEtaMinutes
      )
    } finally {
      db.close()
    }
  }

  @Test
  fun `updateFarmerCredentials updates name phone and farmerId and blocks duplicate phone`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.local.KrishivantriDatabase::class.java
    ).allowMainThreadQueries().build()
    try {
      val repo = com.example.data.repository.KrishivantriRepository(db)
      val f1 = (repo.registerFarmer("Ramesh", "9876500001", "KISAN-01") as com.example.data.repository.AuthResult.Success).user
      repo.registerFarmer("Mahesh", "9876500002", "KISAN-02")

      val duplicateResult = repo.updateFarmerCredentials(
        userId = f1.id,
        name = "Ramesh Patel",
        phone = "9876500002",
        farmerIdNumber = "kisan-99"
      )
      assertTrue(duplicateResult is com.example.data.repository.AuthResult.Error)

      val updateResult = repo.updateFarmerCredentials(
        userId = f1.id,
        name = "Ramesh Patel",
        phone = "9876500009",
        farmerIdNumber = "kisan-99"
      )
      assertTrue(updateResult is com.example.data.repository.AuthResult.Success)
      val updatedUser = (updateResult as com.example.data.repository.AuthResult.Success).user
      assertEquals("Ramesh Patel", updatedUser.name)
      assertEquals("9876500009", updatedUser.phone)

      val updatedProfile = db.dao().getFarmerProfile(f1.id)!!
      assertEquals("KISAN-99", updatedProfile.farmerIdNumber)
    } finally {
      db.close()
    }
  }

  @Test
  fun `cancelOrder is blocked after booking and paying and wallet transfers only on farmer Job Done`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.local.KrishivantriDatabase::class.java
    ).allowMainThreadQueries().build()
    try {
      val repo = com.example.data.repository.KrishivantriRepository(db)
      val farmer = (repo.registerFarmer("Sita Farmer", "9811111111", "FRM-201") as com.example.data.repository.AuthResult.Success).user
      val pilot = (repo.registerPilot(
        name = "Vikram Pilot",
        phone = "9822222222",
        droneType = com.example.data.model.DroneType.HEXACOPTER,
        tankCapacity = 16.0,
        licenseDocumentUri = "content://license",
        bankUpiId = "vikram@upi"
      ) as com.example.data.repository.AuthResult.Success).user

      val polygon = listOf(
        com.example.data.model.LatLngPoint(20.0059, 73.7897),
        com.example.data.model.LatLngPoint(20.0059, 73.7917),
        com.example.data.model.LatLngPoint(20.0079, 73.7917),
        com.example.data.model.LatLngPoint(20.0079, 73.7897)
      )
      val order = repo.placeOrder(
        farmerId = farmer.id,
        locationAddress = "Nashik North Plot",
        polygonPoints = polygon,
        paymentMethod = com.example.data.model.PaymentMethod.ONLINE,
        razorpayPaymentId = "pay_test_123"
      )

      // Cancellation after booking & paying must be rejected
      val cancelResult = repo.cancelOrder(order.id)
      assertTrue(cancelResult is com.example.data.repository.CancelOrderResult.NotAllowed)
      assertEquals(com.example.data.model.OrderStatus.PENDING.apiValue, db.dao().getOrderById(order.id)!!.orderStatus)

      // Pilot accepts, starts spraying, and completes spraying
      val acceptResult = repo.acceptOrder(pilot.id, order.id, com.example.data.model.LatLngPoint(20.0, 73.78))
      assertEquals(com.example.data.repository.AcceptOrderResult.Success, acceptResult)
      repo.startSpraying(order.id)
      repo.completeOrder(order.id)

      val afterPilotDone = db.dao().getOrderById(order.id)!!
      assertEquals(com.example.data.model.OrderStatus.PILOT_COMPLETED.apiValue, afterPilotDone.orderStatus)
      // Pilot wallet must STILL be 0.0 until farmer clicks Job Done
      assertEquals(0.0, db.dao().getPilotProfile(pilot.id)!!.walletBalance, 0.001)
      assertEquals(0, db.dao().getFarmerProfile(farmer.id)!!.coins)

      // Farmer clicks "Job Done" -> wallet transfer occurs immediately
      val jobDoneResult = repo.confirmJobDoneByFarmer(order.id, farmer.id)
      assertTrue(jobDoneResult is com.example.data.repository.JobDoneResult.Success)
      val success = jobDoneResult as com.example.data.repository.JobDoneResult.Success
      assertEquals(order.pilotEarnings, success.transferredAmount, 0.01)

      val finalOrder = db.dao().getOrderById(order.id)!!
      assertEquals(com.example.data.model.OrderStatus.COMPLETED.apiValue, finalOrder.orderStatus)
      assertEquals(order.pilotEarnings, db.dao().getPilotProfile(pilot.id)!!.walletBalance, 0.01)
      assertEquals(1, db.dao().getFarmerProfile(farmer.id)!!.coins)

      // Re-clicking Job Done must not double-credit wallet
      val duplicateJobDone = repo.confirmJobDoneByFarmer(order.id, farmer.id)
      assertTrue(duplicateJobDone is com.example.data.repository.JobDoneResult.Error)
      assertEquals(order.pilotEarnings, db.dao().getPilotProfile(pilot.id)!!.walletBalance, 0.01)
    } finally {
      db.close()
    }
  }

  @Test
  fun `buildConfig excludes RAZORPAY_KEY_SECRET and runtime config rejects placeholder keyId`() {
    val fieldNames = BuildConfig::class.java.declaredFields.map { it.name }.toSet()
    assertTrue("RAZORPAY_KEY_SECRET must not be in BuildConfig", !fieldNames.contains("RAZORPAY_KEY_SECRET"))
    assertTrue("DJANGO_SECRET_KEY must not be in BuildConfig", !fieldNames.contains("DJANGO_SECRET_KEY"))

    com.example.data.remote.KrishivantriRuntimeConfig.configure(
      apiBaseUrl = "https://api.krishivantri.in/api/v1/",
      supabaseUrl = "https://leawuzhjkuzwgzdduwzq.supabase.co",
      supabaseAnonKey = "MY_SUPABASE_ANON_KEY",
      razorpayKeyId = "MY_RAZORPAY_KEY_ID"
    )
    assertTrue(!com.example.data.remote.KrishivantriRuntimeConfig.isRazorpayConfigured())

    com.example.data.remote.KrishivantriRuntimeConfig.configure(
      apiBaseUrl = "https://api.krishivantri.in/api/v1/",
      supabaseUrl = "https://leawuzhjkuzwgzdduwzq.supabase.co",
      supabaseAnonKey = "MY_SUPABASE_ANON_KEY",
      razorpayKeyId = "rzp_test_1234567890"
    )
    assertTrue(com.example.data.remote.KrishivantriRuntimeConfig.isRazorpayConfigured())
  }
}
