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
}
