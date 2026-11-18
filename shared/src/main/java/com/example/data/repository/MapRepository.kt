package com.example.data.repository

import android.content.Context
import com.example.data.local.KrishivantriDao
import com.example.data.remote.KrishivantriRuntimeConfig
import com.example.data.model.FarmBoundaryEntity
import com.example.data.model.LatLngPoint
import com.example.data.model.MapStyleType
import com.example.data.model.OfflineMapRegionInfo
import com.example.data.model.OperatorLocationMarker
import com.example.data.model.OperatorRoleType
import com.example.data.model.OrderWithDetails
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseOperatorLocationDto
import com.example.domain.GeoPolygonCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.Style
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MapRepository(
    private val context: Context,
    private val dao: KrishivantriDao
) {
    companion object {
        /**
         * OpenFreeMap Liberty uses MIT/ODbL-licensed vector tiles whose terms explicitly permit
         * caching and offline region downloads (never bulk-downloading public tile.openstreetmap.org).
         */
        const val OPENFREEMAP_LIBERTY_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
        const val OPENFREEMAP_PLANET_TILEJSON = "https://tiles.openfreemap.org/planet"
    }

    suspend fun ensureMapLibreInitialized(): Boolean = withContext(Dispatchers.Main) {
        runCatching {
            MapLibre.getInstance(context.applicationContext)
            true
        }.getOrDefault(false)
    }

    fun getMapLibreStyleBuilder(styleType: MapStyleType): Style.Builder {
        return when (styleType) {
            MapStyleType.STANDARD -> Style.Builder().fromUri(OPENFREEMAP_LIBERTY_STYLE_URL)
            MapStyleType.SATELLITE -> Style.Builder().fromJson(buildSatelliteStyleJson())
            MapStyleType.HYBRID -> Style.Builder().fromJson(buildHybridStyleJson())
        }
    }

    private fun resolveLicensedSatelliteTileUrl(): String {
        val rawTemplate = KrishivantriRuntimeConfig.satelliteTileUrl.trim()
        val apiKey = KrishivantriRuntimeConfig.satelliteApiKey.trim()
        if (rawTemplate.isBlank()) {
            return "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
        }
        return if (apiKey.isNotBlank() && apiKey != "MY_SATELLITE_API_KEY") {
            if (rawTemplate.contains("{key}")) {
                rawTemplate.replace("{key}", apiKey)
            } else if (rawTemplate.contains("?")) {
                "$rawTemplate&key=$apiKey"
            } else {
                "$rawTemplate?key=$apiKey"
            }
        } else {
            rawTemplate
        }
    }

    private fun buildSatelliteStyleJson(): String {
        val tileUrl = resolveLicensedSatelliteTileUrl()
        return """
            {
              "version": 8,
              "name": "Krishivantri Clear Satellite",
              "sources": {
                "licensed_satellite": {
                  "type": "raster",
                  "tiles": ["$tileUrl"],
                  "tileSize": 256,
                  "maxzoom": 19,
                  "attribution": "© Esri, Maxar, Earthstar Geographics | © OpenStreetMap contributors"
                }
              },
              "layers": [
                {
                  "id": "satellite-raster-layer",
                  "type": "raster",
                  "source": "licensed_satellite",
                  "minzoom": 0,
                  "maxzoom": 22
                }
              ]
            }
        """.trimIndent()
    }

    private fun buildHybridStyleJson(): String {
        val tileUrl = resolveLicensedSatelliteTileUrl()
        val roadsOverlay = "https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Transportation/MapServer/tile/{z}/{y}/{x}"
        val labelsOverlay = "https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}"
        return """
            {
              "version": 8,
              "name": "Krishivantri Hybrid Satellite + Labels",
              "sources": {
                "licensed_satellite": {
                  "type": "raster",
                  "tiles": ["$tileUrl"],
                  "tileSize": 256,
                  "maxzoom": 19,
                  "attribution": "© Esri, Maxar, Earthstar Geographics | © OpenStreetMap contributors"
                },
                "hybrid_roads": {
                  "type": "raster",
                  "tiles": ["$roadsOverlay"],
                  "tileSize": 256,
                  "maxzoom": 19
                },
                "hybrid_labels": {
                  "type": "raster",
                  "tiles": ["$labelsOverlay"],
                  "tileSize": 256,
                  "maxzoom": 19
                }
              },
              "layers": [
                {
                  "id": "satellite-base",
                  "type": "raster",
                  "source": "licensed_satellite",
                  "minzoom": 0,
                  "maxzoom": 22
                },
                {
                  "id": "hybrid-roads-overlay",
                  "type": "raster",
                  "source": "hybrid_roads",
                  "minzoom": 0,
                  "maxzoom": 22,
                  "paint": {
                    "raster-opacity": 0.85
                  }
                },
                {
                  "id": "hybrid-labels-overlay",
                  "type": "raster",
                  "source": "hybrid_labels",
                  "minzoom": 0,
                  "maxzoom": 22
                }
              ]
            }
        """.trimIndent()
    }

    // --- GeoJSON Layer Builders ---

    fun buildDrawingPolygonFeatureCollection(points: List<LatLngPoint>): FeatureCollection {
        if (points.isEmpty()) return FeatureCollection.fromFeatures(emptyList())
        val features = mutableListOf<Feature>()

        val geoPoints = points.map { Point.fromLngLat(it.lng, it.lat) }
        if (geoPoints.size >= 3) {
            val closedRing = geoPoints + geoPoints.first()
            features.add(
                Feature.fromGeometry(Polygon.fromLngLats(listOf(closedRing))).apply {
                    addStringProperty("feature_kind", "boundary_polygon")
                }
            )
            val center = GeoPolygonCalculator.calculatePolygonCenter(points)
            if (center != null) {
                features.add(
                    Feature.fromGeometry(Point.fromLngLat(center.lng, center.lat)).apply {
                        addStringProperty("feature_kind", "area_badge")
                    }
                )
            }
        } else if (geoPoints.size == 2) {
            features.add(
                Feature.fromGeometry(LineString.fromLngLats(geoPoints)).apply {
                    addStringProperty("feature_kind", "boundary_line")
                }
            )
        }

        points.forEachIndexed { idx, pt ->
            val feat = Feature.fromGeometry(Point.fromLngLat(pt.lng, pt.lat))
            feat.addStringProperty("feature_kind", "vertex_point")
            feat.addNumberProperty("vertex_index", idx + 1)
            features.add(feat)
        }
        return FeatureCollection.fromFeatures(features)
    }

    fun buildSavedFarmsFeatureCollection(farms: List<FarmBoundaryEntity>): FeatureCollection {
        val features = farms.mapNotNull { farm ->
            val pts = GeoPolygonCalculator.decodePolygon(farm.polygonCoordinatesJson)
            if (pts.size < 3) return@mapNotNull null
            val ring = pts.map { Point.fromLngLat(it.lng, it.lat) } +
                Point.fromLngLat(pts.first().lng, pts.first().lat)
            Feature.fromGeometry(Polygon.fromLngLats(listOf(ring))).apply {
                addStringProperty("farm_id", farm.id)
                addStringProperty("farm_name", farm.farmName)
                addNumberProperty("area_acres", farm.areaAcres)
                addNumberProperty("area_hectares", farm.areaHectares)
            }
        }
        return FeatureCollection.fromFeatures(features)
    }

    fun buildServiceBookingsFeatureCollection(orders: List<OrderWithDetails>): FeatureCollection {
        val features = mutableListOf<Feature>()
        orders.forEach { item ->
            val pts = item.polygonPoints
            if (pts.size >= 3) {
                val ring = pts.map { Point.fromLngLat(it.lng, it.lat) } +
                    Point.fromLngLat(pts.first().lng, pts.first().lat)
                features.add(
                    Feature.fromGeometry(Polygon.fromLngLats(listOf(ring))).apply {
                        addStringProperty("order_id", item.order.id)
                        addStringProperty("status", item.order.orderStatus)
                        addStringProperty("address", item.order.locationAddress)
                    }
                )
            }
            val center = GeoPolygonCalculator.calculatePolygonCenter(pts)
            if (center != null) {
                features.add(
                    Feature.fromGeometry(Point.fromLngLat(center.lng, center.lat)).apply {
                        addStringProperty("kind", "booking_pin")
                        addStringProperty("order_id", item.order.id)
                        addStringProperty("label", "${item.order.acresToSpray} ac")
                    }
                )
            }
        }
        return FeatureCollection.fromFeatures(features)
    }

    fun buildOperatorsFeatureCollection(operators: List<OperatorLocationMarker>): FeatureCollection {
        val features = operators.map { op ->
            Feature.fromGeometry(Point.fromLngLat(op.lng, op.lat)).apply {
                addStringProperty("id", op.id)
                addStringProperty("name", op.name)
                addStringProperty("role_type", op.roleType.apiValue)
                addStringProperty("drone_type", op.droneType)
                addBooleanProperty("is_available", op.isAvailable)
            }
        }
        return FeatureCollection.fromFeatures(features)
    }

    // --- Supabase Realtime + Local Pilot Location Streaming ---

    fun observeRealtimeOperators(referenceCenter: LatLngPoint): Flow<List<OperatorLocationMarker>> =
        combine(
            observeSupabaseRealtimeOperatorStream(),
            dao.observeAllUsers(),
            dao.observeAllPilots()
        ) { remoteMap, users, pilots ->
            val userMap = users.associateBy { it.id }
            val merged = remoteMap.toMutableMap()

            // Merge online registered pilots from Room so local pilot availability updates reflect live
            pilots.filter { it.isOnline }.forEachIndexed { idx, pilot ->
                val user = userMap[pilot.userId] ?: return@forEachIndexed
                if (!merged.containsKey(pilot.userId)) {
                    val latOffset = ((idx % 3) - 1) * 0.0032 + 0.0018
                    val lngOffset = (((idx + 1) % 3) - 1) * 0.0035 - 0.0014
                    merged[pilot.userId] = OperatorLocationMarker(
                        id = pilot.userId,
                        name = user.name,
                        roleType = if (pilot.droneType == "hexacopter") {
                            OperatorRoleType.DRONE_OPERATOR
                        } else {
                            OperatorRoleType.DRONE_MITRA
                        },
                        lat = pilot.currentLat ?: (referenceCenter.lat + latOffset),
                        lng = pilot.currentLng ?: (referenceCenter.lng + lngOffset),
                        droneType = pilot.droneType,
                        isAvailable = pilot.isOnline,
                        updatedAt = pilot.locationUpdatedAt
                    )
                }
            }
            merged.values.toList()
        }

    suspend fun publishOperatorLocation(
        operatorId: String,
        name: String,
        roleType: OperatorRoleType,
        lat: Double,
        lng: Double,
        droneType: String,
        isAvailable: Boolean,
        headingDegrees: Float = 0f
    ) {
        val api = SupabaseClient.api ?: return
        runCatching {
            api.upsertOperatorLocation(
                SupabaseOperatorLocationDto(
                    id = operatorId,
                    name = name,
                    role_type = roleType.apiValue,
                    lat = lat,
                    lng = lng,
                    drone_type = droneType,
                    is_available = isAvailable,
                    heading_degrees = headingDegrees
                )
            )
        }
    }

    private fun observeSupabaseRealtimeOperatorStream(): Flow<Map<String, OperatorLocationMarker>> =
        callbackFlow {
            val currentMap = mutableMapOf<String, OperatorLocationMarker>()
            trySend(emptyMap())

            if (!SupabaseClient.isConfigured()) {
                awaitClose { }
                return@callbackFlow
            }

            // Initial PostgREST snapshot
            launch(Dispatchers.IO) {
                runCatching {
                    val resp = SupabaseClient.api?.getOperatorLocations()
                    if (resp?.isSuccessful == true) {
                        resp.body()?.forEach { dto ->
                            currentMap[dto.id] = dto.toMarker()
                        }
                        trySend(currentMap.toMap())
                    }
                }
            }

            var heartbeatJob: kotlinx.coroutines.Job? = null
            val ws = runCatching {
                val request = Request.Builder().url(SupabaseClient.realtimeWebSocketUrl()).build()
                SupabaseClient.okHttpClient.newWebSocket(
                    request,
                    object : WebSocketListener() {
                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            val joinPayload = JSONObject().apply {
                                put("topic", "realtime:public:operator_locations")
                                put("event", "phx_join")
                                put(
                                    "payload",
                                    JSONObject().apply {
                                        put(
                                            "config",
                                            JSONObject().apply {
                                                put(
                                                    "postgres_changes",
                                                    org.json.JSONArray().put(
                                                        JSONObject().apply {
                                                            put("event", "*")
                                                            put("schema", "public")
                                                            put("table", "operator_locations")
                                                        }
                                                    )
                                                )
                                            }
                                        )
                                    }
                                )
                                put("ref", "1")
                            }
                            webSocket.send(joinPayload.toString())

                            heartbeatJob = CoroutineScope(Dispatchers.IO).launch {
                                var refCounter = 2
                                while (isActive) {
                                    delay(25_000L)
                                    val hb = JSONObject().apply {
                                        put("topic", "phoenix")
                                        put("event", "heartbeat")
                                        put("payload", JSONObject())
                                        put("ref", (refCounter++).toString())
                                    }
                                    webSocket.send(hb.toString())
                                }
                            }
                        }

                        override fun onMessage(webSocket: WebSocket, text: String) {
                            runCatching {
                                val root = JSONObject(text)
                                if (root.optString("event") == "postgres_changes") {
                                    val data = root.optJSONObject("payload")?.optJSONObject("data")
                                    val record = data?.optJSONObject("record") ?: return@runCatching
                                    val id = record.optString("id")
                                    if (id.isNotBlank()) {
                                        val marker = OperatorLocationMarker(
                                            id = id,
                                            name = record.optString("name", "Drone Operator"),
                                            roleType = OperatorRoleType.fromApi(
                                                record.optString("role_type", "drone_operator")
                                            ),
                                            lat = record.optDouble("lat"),
                                            lng = record.optDouble("lng"),
                                            droneType = record.optString("drone_type", "quadcopter"),
                                            isAvailable = record.optBoolean("is_available", true),
                                            headingDegrees = record.optDouble("heading_degrees", 0.0).toFloat()
                                        )
                                        currentMap[id] = marker
                                        trySend(currentMap.toMap())
                                    }
                                }
                            }
                        }
                    }
                )
            }.getOrNull()

            awaitClose {
                heartbeatJob?.cancel()
                ws?.close(1000, "Map observer closed")
            }
        }

    private fun SupabaseOperatorLocationDto.toMarker(): OperatorLocationMarker =
        OperatorLocationMarker(
            id = id,
            name = name,
            roleType = OperatorRoleType.fromApi(role_type),
            lat = lat,
            lng = lng,
            droneType = drone_type,
            isAvailable = is_available,
            headingDegrees = heading_degrees
        )

    // --- MapLibre Offline Region Caching (Explicitly using offline-permitted OpenFreeMap Liberty) ---

    suspend fun listOfflineRegions(): List<OfflineMapRegionInfo> {
        if (!ensureMapLibreInitialized()) return emptyList()
        return withContext(Dispatchers.Main) {
            suspendCoroutine { cont ->
                try {
                    val manager = OfflineManager.getInstance(context.applicationContext)
                    manager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
                        override fun onList(offlineRegions: Array<OfflineRegion>?) {
                            if (offlineRegions.isNullOrEmpty()) {
                                cont.resume(emptyList())
                                return
                            }
                            val results = mutableListOf<OfflineMapRegionInfo>()
                            var pending = offlineRegions.size
                            offlineRegions.forEach { region ->
                                val name = runCatching {
                                    JSONObject(String(region.metadata, Charsets.UTF_8)).optString("name", "Field Cache")
                                }.getOrDefault("Field Cache #${region.id}")

                                region.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
                                    override fun onStatus(status: OfflineRegionStatus?) {
                                        results.add(
                                            OfflineMapRegionInfo(
                                                id = region.id,
                                                name = name,
                                                completedBytes = status?.completedResourceSize ?: 0L,
                                                completedTiles = status?.completedTileCount ?: 0L,
                                                requiredTiles = status?.requiredResourceCount ?: 0L,
                                                isComplete = status?.isComplete ?: false
                                            )
                                        )
                                        pending--
                                        if (pending == 0) cont.resume(results)
                                    }

                                    override fun onError(error: String?) {
                                        pending--
                                        if (pending == 0) cont.resume(results)
                                    }
                                })
                            }
                        }

                        override fun onError(error: String) {
                            cont.resume(emptyList())
                        }
                    })
                } catch (_: Throwable) {
                    cont.resume(emptyList())
                }
            }
        }
    }

    fun cacheRegionOffline(
        regionName: String,
        center: LatLngPoint,
        onProgress: (Int) -> Unit,
        onComplete: (OfflineMapRegionInfo) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val appContext = context.applicationContext
            MapLibre.getInstance(appContext)
            val manager = OfflineManager.getInstance(appContext)
            val delta = 0.025
            val bounds = LatLngBounds.Builder()
                .include(LatLng(center.lat + delta, center.lng + delta))
                .include(LatLng(center.lat - delta, center.lng - delta))
                .build()

            val definition = OfflineTilePyramidRegionDefinition(
                OPENFREEMAP_LIBERTY_STYLE_URL,
                bounds,
                11.0,
                15.0,
                appContext.resources.displayMetrics.density,
                false
            )

            val metadata = JSONObject()
                .put("name", regionName)
                .toString()
                .toByteArray(Charsets.UTF_8)

            manager.createOfflineRegion(
                definition,
                metadata,
                object : OfflineManager.CreateOfflineRegionCallback {
                    override fun onCreate(offlineRegion: OfflineRegion) {
                        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
                        offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {
                            override fun onStatusChanged(status: OfflineRegionStatus) {
                                val pct = if (status.requiredResourceCount > 0) {
                                    ((status.completedResourceCount * 100.0) / status.requiredResourceCount)
                                        .toInt()
                                        .coerceIn(0, 100)
                                } else 0
                                onProgress(pct)
                                if (status.isComplete) {
                                    offlineRegion.setDownloadState(OfflineRegion.STATE_INACTIVE)
                                    onComplete(
                                        OfflineMapRegionInfo(
                                            id = offlineRegion.id,
                                            name = regionName,
                                            completedBytes = status.completedResourceSize,
                                            completedTiles = status.completedTileCount,
                                            requiredTiles = status.requiredResourceCount,
                                            isComplete = true
                                        )
                                    )
                                }
                            }

                            override fun onError(error: OfflineRegionError) {
                                onError(error.message)
                            }

                            override fun mapboxTileCountLimitExceeded(limit: Long) {
                                onError("Offline tile limit exceeded ($limit tiles).")
                            }
                        })
                    }

                    override fun onError(error: String) {
                        onError(error)
                    }
                }
            )
        } catch (t: Throwable) {
            onError(t.message ?: "Offline map caching unavailable.")
        }
    }
}
