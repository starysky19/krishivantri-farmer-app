package com.example.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.data.model.LocationSnapshot
import com.example.domain.GeoPolygonCalculator
import com.example.domain.LocationMatch
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationRepository(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext)

    fun hasFineOrCoarsePermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasFinePermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Battery-efficient GPS/Network location stream using FusedLocationProviderClient
     * with automatic cleanup when collector stops or UI goes to background.
     */
    @SuppressLint("MissingPermission")
    fun observeLocationUpdates(highPrecisionForDrawing: Boolean): Flow<LocationSnapshot> = callbackFlow {
        if (!hasFineOrCoarsePermission()) {
            close()
            return@callbackFlow
        }

        val priority = if (highPrecisionForDrawing && hasFinePermission()) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        val request = LocationRequest.Builder(
            priority,
            if (highPrecisionForDrawing) 8_000L else 15_000L
        )
            .setMinUpdateIntervalMillis(if (highPrecisionForDrawing) 5_000L else 10_000L)
            .setMinUpdateDistanceMeters(if (highPrecisionForDrawing) 5f else 15f)
            .build()

        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                trySend(loc.toSnapshot())
            }
        }

        var fallbackManager: LocationManager? = null
        var fallbackListener: LocationListener? = null

        try {
            fusedClient.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper())
                .addOnFailureListener {
                    // Fallback to Android LocationManager on devices without Play Services
                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            trySend(location.toSnapshot())
                        }
                        @Deprecated("Deprecated in API 29")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    }
                    fallbackManager = lm
                    fallbackListener = listener
                    val provider = if (hasFinePermission() && lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                        LocationManager.GPS_PROVIDER
                    } else {
                        LocationManager.NETWORK_PROVIDER
                    }
                    runCatching {
                        lm?.requestLocationUpdates(provider, 10_000L, 10f, listener, Looper.getMainLooper())
                    }
                }
        } catch (_: SecurityException) {
            close()
        }

        awaitClose {
            fusedClient.removeLocationUpdates(fusedCallback)
            fallbackListener?.let { listener ->
                runCatching { fallbackManager?.removeUpdates(listener) }
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocationOnce(): LocationSnapshot? {
        if (!hasFineOrCoarsePermission()) return null

        return suspendCancellableCoroutine { cont ->
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }

            val priority = if (hasFinePermission()) {
                Priority.PRIORITY_HIGH_ACCURACY
            } else {
                Priority.PRIORITY_BALANCED_POWER_ACCURACY
            }

            fusedClient.getCurrentLocation(priority, cts.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        if (cont.isActive) cont.resume(loc.toSnapshot())
                    } else {
                        fusedClient.lastLocation
                            .addOnSuccessListener { last ->
                                if (cont.isActive) cont.resume(last?.toSnapshot() ?: lastKnownFromLocationManager())
                            }
                            .addOnFailureListener {
                                if (cont.isActive) cont.resume(lastKnownFromLocationManager())
                            }
                    }
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(lastKnownFromLocationManager())
                }
        }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownFromLocationManager(): LocationSnapshot? {
        if (!hasFineOrCoarsePermission()) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val gps = runCatching { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
        val net = runCatching { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()
        return (gps ?: net)?.toSnapshot()
    }

    suspend fun searchLocations(query: String): List<LocationMatch> =
        GeoPolygonCalculator.searchLocations(context, query)

    private fun Location.toSnapshot(): LocationSnapshot =
        LocationSnapshot(
            lat = latitude,
            lng = longitude,
            accuracyMeters = accuracy,
            bearing = bearing,
            provider = provider ?: "fused",
            timestamp = time
        )
}
