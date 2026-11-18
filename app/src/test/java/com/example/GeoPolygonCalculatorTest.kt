package com.example

import com.example.data.model.DroneType
import com.example.data.model.LatLngPoint
import com.example.data.model.PilotProfileEntity
import com.example.domain.GeoPolygonCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoPolygonCalculatorTest {

    @Test
    fun calculatePolygonAreaAcres_returnsZeroForUnderThreePoints() {
        val twoPoints = listOf(
            LatLngPoint(20.0, 73.78),
            LatLngPoint(20.001, 73.78)
        )
        assertEquals(0.0, GeoPolygonCalculator.calculatePolygonAreaAcres(twoPoints), 0.001)
    }

    @Test
    fun calculatePolygonAreaAcres_computesAccurateAcreageForSquarePlot() {
        // ~200m x ~200m plot -> ~40,000 sqm -> ~9.88 acres / ~4.00 ha
        val dLat = 0.0018
        val dLng = 0.001915
        val square = listOf(
            LatLngPoint(20.0, 73.78),
            LatLngPoint(20.0, 73.78 + dLng),
            LatLngPoint(20.0 + dLat, 73.78 + dLng),
            LatLngPoint(20.0 + dLat, 73.78)
        )
        val acres = GeoPolygonCalculator.calculatePolygonAreaAcres(square)
        val hectares = GeoPolygonCalculator.calculatePolygonAreaHectares(square)
        assertTrue("Expected ~9.5-10.3 acres, got $acres", acres in 9.0..10.8)
        assertTrue("Expected ~3.6-4.4 hectares, got $hectares", hectares in 3.6..4.4)
        assertEquals(2.47, GeoPolygonCalculator.acresToHectares(6.103), 0.02)
    }

    @Test
    fun calculateTariff_enforces450PerAcreAnd90_10Split() {
        val breakdown = GeoPolygonCalculator.calculateTariff(7.58)
        assertEquals(7.58, breakdown.acres, 0.001)
        assertEquals(3411.0, breakdown.totalAmount, 0.01)
        assertEquals(341.1, breakdown.adminCommission, 0.01)
        assertEquals(3069.9, breakdown.pilotEarnings, 0.01)
    }

    @Test
    fun priorityDispatchScore_ranksVerifiedHexacopterAboveQuadcopter() {
        val hexPilot = PilotProfileEntity(
            userId = "p1",
            droneType = DroneType.HEXACOPTER.apiValue,
            tankCapacity = 16.0,
            licenseDocumentUri = "content://doc1",
            isLicenseVerified = true,
            bankUpiId = "hex@upi",
            ratingAverage = 4.9
        )
        val quadPilot = PilotProfileEntity(
            userId = "p2",
            droneType = DroneType.QUADCOPTER.apiValue,
            tankCapacity = 10.0,
            licenseDocumentUri = "content://doc2",
            isLicenseVerified = true,
            bankUpiId = "quad@upi",
            ratingAverage = 4.9
        )
        assertTrue(
            GeoPolygonCalculator.calculateDispatchPriorityScore(hexPilot) >
                GeoPolygonCalculator.calculateDispatchPriorityScore(quadPilot)
        )
    }

    @Test
    fun calculateDistanceKm_computesAccurateHaversineDistance() {
        val origin = LatLngPoint(20.0059, 73.7897)
        // ~0.045 deg latitude north is roughly ~5.0 km
        val destination = LatLngPoint(20.0509, 73.7897)
        val distanceKm = GeoPolygonCalculator.calculateDistanceKm(origin, destination)
        assertTrue("Expected ~5.0 km, got $distanceKm", distanceKm in 4.8..5.2)
        assertEquals(0.0, GeoPolygonCalculator.calculateDistanceKm(origin, origin), 0.001)
    }

    @Test
    fun estimateArrivalMinutes_scalesWithDistanceAndDroneSpeed() {
        val quadEta5Km = GeoPolygonCalculator.estimateArrivalMinutes(5.0, DroneType.QUADCOPTER)
        val hexEta5Km = GeoPolygonCalculator.estimateArrivalMinutes(5.0, DroneType.HEXACOPTER)
        assertTrue("Expected Quadcopter 5km ETA > 10 mins, got $quadEta5Km", quadEta5Km in 11..16)
        assertTrue("Expected Hexacopter to be <= Quadcopter ETA", hexEta5Km <= quadEta5Km)
        assertEquals(1, GeoPolygonCalculator.estimateArrivalMinutes(0.05, DroneType.QUADCOPTER))
    }

    @Test
    fun formatDistanceLabel_formatsMetersUnderOneKmAndKilometersAbove() {
        assertEquals("650 m away", GeoPolygonCalculator.formatDistanceLabel(0.65))
        assertEquals("3.4 km away", GeoPolygonCalculator.formatDistanceLabel(3.42))
    }
}
