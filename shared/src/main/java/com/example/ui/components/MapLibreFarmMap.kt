package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.FarmBoundaryEntity
import com.example.data.model.LatLngPoint
import com.example.data.model.MapStyleType
import com.example.data.model.OperatorLocationMarker
import com.example.data.model.OrderWithDetails
import com.example.data.repository.MapRepository
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.FeatureCollection
import java.util.Locale
import kotlin.math.hypot

private const val SOURCE_SAVED_FARMS = "krishivantri-saved-farms-src"
private const val SOURCE_BOOKINGS = "krishivantri-bookings-src"
private const val SOURCE_DRAWING = "krishivantri-drawing-src"
private const val SOURCE_OPERATORS = "krishivantri-operators-src"

private const val LAYER_SAVED_FARMS_FILL = "krishivantri-saved-farms-fill"
private const val LAYER_SAVED_FARMS_LINE = "krishivantri-saved-farms-line"

private const val LAYER_BOOKINGS_FILL = "krishivantri-bookings-fill"
private const val LAYER_BOOKINGS_LINE = "krishivantri-bookings-line"
private const val LAYER_BOOKINGS_PIN = "krishivantri-bookings-pin"

private const val LAYER_DRAWING_FILL = "krishivantri-drawing-fill"
private const val LAYER_DRAWING_LINE = "krishivantri-drawing-line"
private const val LAYER_DRAWING_VERTICES = "krishivantri-drawing-vertices"
private const val LAYER_DRAWING_AREA_BADGE = "krishivantri-drawing-area-badge"

private const val LAYER_OPERATORS_PIN = "krishivantri-operators-pin"

private const val ICON_DRONE_OPERATOR = "icon-drone-operator"
private const val ICON_DRONE_MITRA = "icon-drone-mitra"
private const val ICON_BOOKING_PIN = "icon-booking-pin"
private const val ICON_AREA_BADGE = "icon-area-badge"

@SuppressLint("ClickableViewAccessibility")
@Composable
fun MapLibreFarmMap(
    mapRepository: MapRepository,
    styleType: MapStyleType,
    cameraTarget: LatLngPoint,
    cameraFlyNonce: Int,
    zoomInNonce: Int,
    zoomOutNonce: Int,
    resetCompassNonce: Int,
    styleRetryNonce: Int,
    hasLocationPermission: Boolean,
    drawingPoints: List<LatLngPoint>,
    computedAcres: Double,
    computedHectares: Double,
    savedFarms: List<FarmBoundaryEntity>,
    bookingOrders: List<OrderWithDetails>,
    operators: List<OperatorLocationMarker>,
    onMapTapPoint: (LatLngPoint) -> Unit,
    onUpdateBoundaryPoint: (Int, LatLngPoint) -> Unit,
    onMapLoadingChanged: (Boolean) -> Unit,
    onMapError: (String?) -> Unit,
    onCameraMoved: (LatLngPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val isInspection = LocalInspectionMode.current
    if (isInspection) {
        Box(modifier = modifier.fillMaxSize())
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    remember(context) {
        runCatching { MapLibre.getInstance(context.applicationContext) }
    }

    val mapView = remember {
        val options = MapLibreMapOptions.createFromAttributes(context, null)
            .textureMode(true)
        MapView(context, options).apply {
            onCreate(null)
        }
    }

    val latestOnTap by rememberUpdatedState(onMapTapPoint)
    val latestOnUpdatePoint by rememberUpdatedState(onUpdateBoundaryPoint)
    val latestDrawingPoints by rememberUpdatedState(drawingPoints)
    val latestOnLoading by rememberUpdatedState(onMapLoadingChanged)
    val latestOnError by rememberUpdatedState(onMapError)
    val latestOnCameraMoved by rememberUpdatedState(onCameraMoved)

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyle by remember { mutableStateOf<Style?>(null) }
    var draggingVertexIndex by remember { mutableIntStateOf(-1) }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView) {
        mapView.addOnDidFinishLoadingMapListener {
            latestOnLoading(false)
        }
        mapView.addOnDidFailLoadingMapListener { errorMessage ->
            latestOnLoading(false)
            if (loadedStyle == null) {
                latestOnError(errorMessage.ifBlank { "Unable to load map tiles. Check network and retry." })
            }
        }

        mapView.getMapAsync { map ->
            if (mapView.isDestroyed) return@getMapAsync
            mapLibreMap = map
            map.uiSettings.apply {
                isZoomGesturesEnabled = true
                isScrollGesturesEnabled = true
                isRotateGesturesEnabled = true
                isTiltGesturesEnabled = true
                isCompassEnabled = true
                isAttributionEnabled = true
                isLogoEnabled = true
                setAttributionMargins(24, 0, 0, 220)
            }

            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(cameraTarget.lat, cameraTarget.lng))
                .zoom(16.4)
                .build()

            map.addOnMapClickListener { latLng ->
                latestOnTap(LatLngPoint(latLng.latitude, latLng.longitude))
                true
            }

            map.addOnCameraIdleListener {
                val target = map.cameraPosition.target ?: return@addOnCameraIdleListener
                latestOnCameraMoved(LatLngPoint(target.latitude, target.longitude))
            }
        }
    }

    // Apply or retry MapLibre Style (Hybrid / Satellite / Standard)
    LaunchedEffect(mapLibreMap, styleType, styleRetryNonce) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (mapView.isDestroyed) return@LaunchedEffect
        latestOnLoading(true)
        latestOnError(null)
        loadedStyle = null

        val styleBuilder = mapRepository.getMapLibreStyleBuilder(styleType)
        map.setStyle(styleBuilder) { style ->
            if (mapView.isDestroyed) return@setStyle
            setupCustomGeoJsonLayers(context, style, styleType)
            loadedStyle = style
            latestOnLoading(false)
            latestOnError(null)
        }
    }

    // Activate native GPS LocationComponent when permission & style are ready
    LaunchedEffect(mapLibreMap, loadedStyle, hasLocationPermission) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val style = loadedStyle ?: return@LaunchedEffect
        if (hasLocationPermission) {
            activateLocationComponentSafely(context, map, style)
        }
    }

    // Sync GeoJSON sources & crisp dynamic area pill badge whenever polygon or layers update
    LaunchedEffect(
        loadedStyle,
        drawingPoints,
        computedAcres,
        computedHectares,
        savedFarms,
        bookingOrders,
        operators
    ) {
        val style = loadedStyle ?: return@LaunchedEffect
        if (drawingPoints.size >= 3 && computedAcres > 0.0) {
            val areaLabel = String.format(
                Locale.US,
                "%.2f ha • %.2f ac",
                computedHectares,
                computedAcres
            )
            style.addImage(ICON_AREA_BADGE, createAreaPillBitmap(context, areaLabel))
        }
        style.getSourceAs<GeoJsonSource>(SOURCE_DRAWING)?.setGeoJson(
            mapRepository.buildDrawingPolygonFeatureCollection(drawingPoints)
        )
        style.getSourceAs<GeoJsonSource>(SOURCE_SAVED_FARMS)?.setGeoJson(
            mapRepository.buildSavedFarmsFeatureCollection(savedFarms)
        )
        style.getSourceAs<GeoJsonSource>(SOURCE_BOOKINGS)?.setGeoJson(
            mapRepository.buildServiceBookingsFeatureCollection(bookingOrders)
        )
        style.getSourceAs<GeoJsonSource>(SOURCE_OPERATORS)?.setGeoJson(
            mapRepository.buildOperatorsFeatureCollection(operators)
        )
    }

    LaunchedEffect(mapLibreMap, cameraFlyNonce) {
        if (cameraFlyNonce == 0) return@LaunchedEffect
        val map = mapLibreMap ?: return@LaunchedEffect
        val pos = CameraPosition.Builder()
            .target(LatLng(cameraTarget.lat, cameraTarget.lng))
            .zoom(16.6)
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(pos), 850)
    }

    LaunchedEffect(mapLibreMap, zoomInNonce) {
        if (zoomInNonce == 0) return@LaunchedEffect
        mapLibreMap?.animateCamera(CameraUpdateFactory.zoomIn(), 250)
    }

    LaunchedEffect(mapLibreMap, zoomOutNonce) {
        if (zoomOutNonce == 0) return@LaunchedEffect
        mapLibreMap?.animateCamera(CameraUpdateFactory.zoomOut(), 250)
    }

    LaunchedEffect(mapLibreMap, resetCompassNonce) {
        if (resetCompassNonce == 0) return@LaunchedEffect
        val map = mapLibreMap ?: return@LaunchedEffect
        val pos = CameraPosition.Builder(map.cameraPosition)
            .bearing(0.0)
            .tilt(0.0)
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(pos), 400)
    }

    AndroidView(
        modifier = modifier
            .fillMaxSize()
            .testTag("maplibre_map_view"),
        factory = {
            val touchSlopPx = 32f * context.resources.displayMetrics.density
            mapView.apply {
                setOnTouchListener { v, event ->
                    val map = mapLibreMap
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                            if (map != null && latestDrawingPoints.isNotEmpty()) {
                                val hitIdx = latestDrawingPoints.indexOfFirst { pt ->
                                    val screen = map.projection.toScreenLocation(LatLng(pt.lat, pt.lng))
                                    hypot(screen.x - event.x, screen.y - event.y) <= touchSlopPx
                                }
                                if (hitIdx >= 0) {
                                    draggingVertexIndex = hitIdx
                                    return@setOnTouchListener true
                                }
                            }
                            draggingVertexIndex = -1
                        }
                        MotionEvent.ACTION_MOVE -> {
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                            if (map != null && draggingVertexIndex >= 0) {
                                val dragged = map.projection.fromScreenLocation(PointF(event.x, event.y))
                                latestOnUpdatePoint(
                                    draggingVertexIndex,
                                    LatLngPoint(dragged.latitude, dragged.longitude)
                                )
                                return@setOnTouchListener true
                            }
                        }
                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL -> {
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                            if (draggingVertexIndex >= 0) {
                                draggingVertexIndex = -1
                                return@setOnTouchListener true
                            }
                        }
                    }
                    false
                }
            }
        }
    )
}

@SuppressLint("MissingPermission")
private fun activateLocationComponentSafely(
    context: Context,
    map: MapLibreMap,
    style: Style
) {
    runCatching {
        val options = LocationComponentActivationOptions
            .builder(context, style)
            .useDefaultLocationEngine(true)
            .build()
        val lc = map.locationComponent
        if (!lc.isLocationComponentActivated) {
            lc.activateLocationComponent(options)
        }
        lc.isLocationComponentEnabled = true
        lc.cameraMode = CameraMode.NONE
        lc.renderMode = RenderMode.COMPASS
    }
}

private fun setupCustomGeoJsonLayers(
    context: Context,
    style: Style,
    styleType: MapStyleType
) {
    style.addImage(ICON_DRONE_OPERATOR, createMarkerBitmap(context, 0xFF1B4332.toInt(), "D"))
    style.addImage(ICON_DRONE_MITRA, createMarkerBitmap(context, 0xFF0F766E.toInt(), "M"))
    style.addImage(ICON_BOOKING_PIN, createMarkerBitmap(context, 0xFFD97706.toInt(), "B"))
    style.addImage(ICON_AREA_BADGE, createAreaPillBitmap(context, "0.00 ha"))

    // 1. Saved Farms GeoJSON Source & Layers
    if (style.getSource(SOURCE_SAVED_FARMS) == null) {
        style.addSource(GeoJsonSource(SOURCE_SAVED_FARMS, FeatureCollection.fromFeatures(emptyList())))
        style.addLayer(
            FillLayer(LAYER_SAVED_FARMS_FILL, SOURCE_SAVED_FARMS).withProperties(
                PropertyFactory.fillColor("#40916C"),
                PropertyFactory.fillOpacity(0.24f)
            )
        )
        style.addLayer(
            LineLayer(LAYER_SAVED_FARMS_LINE, SOURCE_SAVED_FARMS).withProperties(
                PropertyFactory.lineColor("#2D6A4F"),
                PropertyFactory.lineWidth(2.4f),
                PropertyFactory.lineDasharray(arrayOf(2f, 1.5f))
            )
        )
    }

    // 2. Service Bookings GeoJSON Source & Layers
    if (style.getSource(SOURCE_BOOKINGS) == null) {
        style.addSource(GeoJsonSource(SOURCE_BOOKINGS, FeatureCollection.fromFeatures(emptyList())))
        style.addLayer(
            FillLayer(LAYER_BOOKINGS_FILL, SOURCE_BOOKINGS)
                .withFilter(Expression.eq(Expression.geometryType(), Expression.literal("Polygon")))
                .withProperties(
                    PropertyFactory.fillColor("#F59E0B"),
                    PropertyFactory.fillOpacity(0.18f)
                )
        )
        style.addLayer(
            LineLayer(LAYER_BOOKINGS_LINE, SOURCE_BOOKINGS)
                .withFilter(Expression.eq(Expression.geometryType(), Expression.literal("Polygon")))
                .withProperties(
                    PropertyFactory.lineColor("#D97706"),
                    PropertyFactory.lineWidth(2.2f)
                )
        )
        style.addLayer(
            SymbolLayer(LAYER_BOOKINGS_PIN, SOURCE_BOOKINGS)
                .withFilter(Expression.eq(Expression.geometryType(), Expression.literal("Point")))
                .withProperties(
                    PropertyFactory.iconImage(ICON_BOOKING_PIN),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconSize(0.95f)
                )
        )
    }

    // 3. Active Field Boundary Selection (Crisp Red Dashed Outline + Corner Dots + Center Area Pill)
    if (style.getSource(SOURCE_DRAWING) == null) {
        style.addSource(GeoJsonSource(SOURCE_DRAWING, FeatureCollection.fromFeatures(emptyList())))
        style.addLayer(
            FillLayer(LAYER_DRAWING_FILL, SOURCE_DRAWING)
                .withFilter(Expression.eq(Expression.geometryType(), Expression.literal("Polygon")))
                .withProperties(
                    PropertyFactory.fillColor("#DC2626"),
                    PropertyFactory.fillOpacity(0.16f)
                )
        )
        style.addLayer(
            LineLayer(LAYER_DRAWING_LINE, SOURCE_DRAWING)
                .withFilter(
                    Expression.any(
                        Expression.eq(Expression.geometryType(), Expression.literal("Polygon")),
                        Expression.eq(Expression.geometryType(), Expression.literal("LineString"))
                    )
                )
                .withProperties(
                    PropertyFactory.lineColor("#DC2626"),
                    PropertyFactory.lineWidth(3.5f),
                    PropertyFactory.lineDasharray(arrayOf(2.5f, 1.5f)),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                )
        )
        style.addLayer(
            CircleLayer(LAYER_DRAWING_VERTICES, SOURCE_DRAWING)
                .withFilter(
                    Expression.all(
                        Expression.eq(Expression.geometryType(), Expression.literal("Point")),
                        Expression.eq(Expression.get("feature_kind"), Expression.literal("vertex_point"))
                    )
                )
                .withProperties(
                    PropertyFactory.circleRadius(8.5f),
                    PropertyFactory.circleColor("#DC2626"),
                    PropertyFactory.circleStrokeWidth(2.0f),
                    PropertyFactory.circleStrokeColor("#FFFFFF")
                )
        )
        style.addLayer(
            SymbolLayer(LAYER_DRAWING_AREA_BADGE, SOURCE_DRAWING)
                .withFilter(
                    Expression.all(
                        Expression.eq(Expression.geometryType(), Expression.literal("Point")),
                        Expression.eq(Expression.get("feature_kind"), Expression.literal("area_badge"))
                    )
                )
                .withProperties(
                    PropertyFactory.iconImage(ICON_AREA_BADGE),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true)
                )
        )
    }

    // 4. Realtime Drone Operators & Drone Mitras Source & Symbol Layer
    if (style.getSource(SOURCE_OPERATORS) == null) {
        style.addSource(GeoJsonSource(SOURCE_OPERATORS, FeatureCollection.fromFeatures(emptyList())))
        val opProps = mutableListOf(
            PropertyFactory.iconImage(
                Expression.match(
                    Expression.get("role_type"),
                    Expression.literal("drone_mitra"),
                    Expression.literal(ICON_DRONE_MITRA),
                    Expression.literal(ICON_DRONE_OPERATOR)
                )
            ),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconSize(1.0f)
        )
        if (styleType == MapStyleType.STANDARD) {
            opProps.add(PropertyFactory.textField(Expression.get("name")))
            opProps.add(PropertyFactory.textFont(arrayOf("Noto Sans Regular")))
            opProps.add(PropertyFactory.textOffset(arrayOf(0f, 1.6f)))
            opProps.add(PropertyFactory.textSize(11f))
            opProps.add(PropertyFactory.textColor("#13261C"))
            opProps.add(PropertyFactory.textHaloColor("#FFFFFF"))
            opProps.add(PropertyFactory.textHaloWidth(1.5f))
        }
        style.addLayer(SymbolLayer(LAYER_OPERATORS_PIN, SOURCE_OPERATORS).withProperties(*opProps.toTypedArray()))
    }
}

private fun createAreaPillBitmap(context: Context, text: String): Bitmap {
    val density = context.resources.displayMetrics.density
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFDC2626.toInt()
        textSize = 14f * density
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val textWidth = textPaint.measureText(text)
    val padH = 14f * density
    val padV = 8f * density
    val w = (textWidth + padH * 2f).toInt().coerceAtLeast(80)
    val h = (textPaint.fontSpacing + padV * 2f).toInt().coerceAtLeast(36)

    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val rect = RectF(2f, 2f, w - 2f, h - 2f)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xF5FFFFFF.toInt()
        style = Paint.Style.FILL
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFDC2626.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    canvas.drawRoundRect(rect, 8f * density, 8f * density, bgPaint)
    canvas.drawRoundRect(rect, 8f * density, 8f * density, borderPaint)

    val textY = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(text, rect.centerX(), textY, textPaint)
    return bmp
}

private fun createMarkerBitmap(context: Context, fillColor: Int, badgeLetter: String): Bitmap {
    val density = context.resources.displayMetrics.density
    val sizePx = (32f * density).toInt().coerceAtLeast(48)
    val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val cx = sizePx / 2f
    val cy = sizePx / 2f - density * 2f
    val r = sizePx * 0.34f

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33000000
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy + density * 2f, r, shadowPaint)

    val pinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    }
    val pointer = Path().apply {
        moveTo(cx - r * 0.55f, cy + r * 0.5f)
        lineTo(cx, sizePx - density * 2f)
        lineTo(cx + r * 0.55f, cy + r * 0.5f)
        close()
    }
    canvas.drawPath(pointer, pinPaint)
    canvas.drawCircle(cx, cy, r, pinPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.2f * density
    }
    canvas.drawCircle(cx, cy, r, borderPaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        textSize = 12f * density
        typeface = Typeface.DEFAULT_BOLD
    }
    val bounds = RectF(cx - r, cy - r, cx + r, cy + r)
    val textY = bounds.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(badgeLetter, cx, textY, textPaint)
    return bmp
}
