package com.example.ui.screens.farmer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.data.model.AppLanguage
import com.example.data.model.FarmBoundaryEntity
import com.example.data.model.FarmerCatalogDefaults
import com.example.data.model.LatLngPoint
import com.example.data.model.MapStyleType
import com.example.data.model.OperatorLocationMarker
import com.example.data.model.PaymentMethod
import com.example.data.repository.MapRepository
import com.example.domain.GeoPolygonCalculator
import com.example.domain.LocationMatch
import com.example.ui.components.MapLibreFarmMap
import com.example.ui.components.MiniPolygonPreview
import com.example.ui.components.formatInr
import com.example.ui.theme.AlertRust
import com.example.ui.theme.CharcoalLeaf
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.HarvestAmber
import com.example.ui.theme.HarvestGoldBg
import com.example.ui.theme.MistSage
import com.example.ui.theme.MutedPine
import com.example.ui.theme.PureSurface
import com.example.ui.theme.SageOutline
import com.example.ui.theme.SoftSage
import com.example.ui.theme.VerifiedTeal
import com.example.ui.theme.VerifiedTealBg
import com.example.ui.viewmodel.MapBookingState

@Composable
fun FarmerBookSprayTab(
    mapRepository: MapRepository,
    mapState: MapBookingState,
    savedFarms: List<FarmBoundaryEntity>,
    realtimeOperators: List<OperatorLocationMarker>,
    appLanguage: AppLanguage,
    onSelectMapStyle: (MapStyleType) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCenterMap: () -> Unit,
    onSelectLocationSuggestion: (LocationMatch) -> Unit,
    onRefreshCurrentLocation: () -> Unit,
    onMapTapPoint: (LatLngPoint) -> Unit,
    onUpdateBoundaryPoint: (Int, LatLngPoint) -> Unit,
    onMapCameraMoved: (LatLngPoint) -> Unit,
    onMapLoadingChanged: (Boolean) -> Unit,
    onMapError: (String?) -> Unit,
    onUndoPolygonPoint: () -> Unit,
    onClearPolygon: () -> Unit,
    onZoomMap: (Boolean) -> Unit,
    onFarmNameChange: (String) -> Unit,
    onSaveFarmToSupabase: () -> Unit,
    onFocusSavedFarm: (FarmBoundaryEntity) -> Unit,
    onCacheCurrentRegionOffline: () -> Unit,
    onSelectSprayCrop: (String) -> Unit,
    onSelectSprayFormula: (String) -> Unit,
    onSelectPaymentMethod: (PaymentMethod) -> Unit,
    onConfirmOrderWithMethod: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSuggestions by remember { mutableStateOf(false) }
    var showSavedFarmsDialog by remember { mutableStateOf(false) }
    var showPaymentChoiceDialog by remember { mutableStateOf(false) }

    val mapPane: @Composable (Modifier, Dp) -> Unit = { paneModifier, bottomClearance ->
        Box(modifier = paneModifier) {
            Box(modifier = Modifier.fillMaxSize()) {
                MapLibreFarmMap(
                    mapRepository = mapRepository,
                    styleType = mapState.mapStyleType,
                    cameraTarget = mapState.mapCenter,
                    cameraFlyNonce = mapState.cameraFlyNonce,
                    zoomInNonce = mapState.zoomInNonce,
                    zoomOutNonce = mapState.zoomOutNonce,
                    resetCompassNonce = mapState.resetCompassNonce,
                    styleRetryNonce = mapState.styleRetryNonce,
                    hasLocationPermission = mapState.hasLocationPermission,
                    drawingPoints = mapState.polygonPoints,
                    computedAcres = mapState.computedAcres,
                    computedHectares = mapState.computedHectares,
                    savedFarms = emptyList(),
                    bookingOrders = emptyList(),
                    operators = realtimeOperators,
                    onMapTapPoint = onMapTapPoint,
                    onUpdateBoundaryPoint = onUpdateBoundaryPoint,
                    onMapLoadingChanged = onMapLoadingChanged,
                    onMapError = onMapError,
                    onCameraMoved = onMapCameraMoved,
                    modifier = Modifier.fillMaxSize()
                )

                // Top floating Search Pill + Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = PureSurface.copy(alpha = 0.96f),
                        tonalElevation = 4.dp,
                        border = BorderStroke(1.dp, SageOutline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .padding(start = 14.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search farmland",
                                tint = ForestGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (mapState.searchQuery.isEmpty()) {
                                    Text(
                                        text = if (appLanguage == AppLanguage.HI) {
                                            "गाँव, तालुका या खसरा खोजें…"
                                        } else {
                                            "Search village, taluka or Khasra…"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MutedPine,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                BasicTextField(
                                    value = mapState.searchQuery,
                                    onValueChange = {
                                        onSearchQueryChange(it)
                                        showSuggestions = true
                                    },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = CharcoalLeaf
                                    ),
                                    cursorBrush = SolidColor(ForestGreen),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("map_search_input")
                                )
                            }
                            IconButton(
                                onClick = {
                                    showSuggestions = false
                                    onCenterMap()
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("map_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "Search Location",
                                    tint = ForestGreen
                                )
                            }
                            IconButton(
                                onClick = onRefreshCurrentLocation,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("map_gps_center_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MyLocation,
                                    contentDescription = "Center on GPS",
                                    tint = ForestGreen
                                )
                            }
                        }
                    }

                    if (showSuggestions && mapState.locationSuggestions.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = PureSurface,
                            tonalElevation = 6.dp,
                            border = BorderStroke(1.dp, SageOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                mapState.locationSuggestions.take(4).forEach { suggestion ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = suggestion.displayName,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        onClick = {
                                            onSelectLocationSuggestion(suggestion)
                                            showSuggestions = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Reference-style Floating Action Pills Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(end = 4.dp)
                    ) {
                        item {
                            val nextStyle = when (mapState.mapStyleType) {
                                MapStyleType.HYBRID -> MapStyleType.SATELLITE
                                MapStyleType.SATELLITE -> MapStyleType.STANDARD
                                MapStyleType.STANDARD -> MapStyleType.HYBRID
                            }
                            FloatingMapChip(
                                icon = Icons.Rounded.Layers,
                                label = "${mapState.mapStyleType.label} Map",
                                active = true,
                                onClick = { onSelectMapStyle(nextStyle) }
                            )
                        }
                        if (savedFarms.isNotEmpty()) {
                            item {
                                FloatingMapChip(
                                    icon = Icons.Rounded.Bookmark,
                                    label = "My Farms (${savedFarms.size})",
                                    active = false,
                                    onClick = { showSavedFarmsDialog = true }
                                )
                            }
                        }
                        item {
                            FloatingMapChip(
                                icon = Icons.Rounded.Download,
                                label = "Offline",
                                active = false,
                                onClick = onCacheCurrentRegionOffline
                            )
                        }
                    }

                    mapState.offlineDownloadProgress?.let { progress ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PureSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Caching offline map tiles: $progress%",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                LinearProgressIndicator(
                                    progress = { progress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp),
                                    color = EmeraldPulse
                                )
                            }
                        }
                    }
                }

                // Bottom-start demarcation indicator + quick Undo/Clear
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = bottomClearance + 36.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = PureSurface.copy(alpha = 0.96f),
                        border = BorderStroke(1.dp, SageOutline),
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (mapState.polygonPoints.size >= 3) EmeraldPulse else HarvestAmber
                                    )
                            )
                            Text(
                                text = when {
                                    mapState.polygonPoints.isEmpty() -> "Tap map corners to mark plot"
                                    mapState.polygonPoints.size < 3 ->
                                        "${mapState.polygonPoints.size} pts • Tap ${3 - mapState.polygonPoints.size}+ more"
                                    else -> "${mapState.computedAcres} Acres • ${mapState.polygonPoints.size} pts"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = CharcoalLeaf
                            )
                        }
                    }

                    if (mapState.polygonPoints.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = PureSurface.copy(alpha = 0.96f),
                            border = BorderStroke(1.dp, SageOutline),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onUndoPolygonPoint)
                                .testTag("map_undo_point_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Undo,
                                    contentDescription = "Undo Corner",
                                    tint = ForestGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = PureSurface.copy(alpha = 0.96f),
                            border = BorderStroke(1.dp, AlertRust.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onClearPolygon)
                                .testTag("map_delete_area_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Clear Boundary",
                                    tint = AlertRust,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom-end Zoom Controls (48dp touch targets)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = bottomClearance + 36.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PureSurface.copy(alpha = 0.96f),
                        border = BorderStroke(1.dp, SageOutline),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { onZoomMap(true) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Add, contentDescription = "Zoom In", tint = ForestGreen)
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = PureSurface.copy(alpha = 0.96f),
                        border = BorderStroke(1.dp, SageOutline),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { onZoomMap(false) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Zoom Out", tint = ForestGreen)
                        }
                    }
                }

                // Subtle Telemetry / Flight Safety Strip sitting cleanly above collapsed sheet
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = bottomClearance)
                        .background(MistSage.copy(alpha = 0.94f))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Air,
                            contentDescription = null,
                            tint = MutedPine,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Wind: 4.2 km/h • Flight Safe",
                            style = MaterialTheme.typography.labelSmall,
                            color = CharcoalLeaf
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(EmeraldPulse)
                        )
                        Text(
                            text = "0.3m/px Satellite Ortho",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = ForestGreen
                        )
                    }
                }
            }
        }
    }

    val bookingSheetPane: @Composable (
        paneModifier: Modifier,
        isExpanded: Boolean?,
        onToggleExpand: (() -> Unit)?,
        onHeaderDrag: ((Float) -> Unit)?,
        onHeaderDragStopped: ((Float) -> Unit)?,
        sheetScrollState: androidx.compose.foundation.ScrollState,
        sheetNestedScroll: NestedScrollConnection?
    ) -> Unit = { paneModifier, isExpanded, onToggleExpand, onHeaderDrag, onHeaderDragStopped, sheetScrollState, sheetNestedScroll ->
        val sheetModifier = if (sheetNestedScroll != null) {
            paneModifier.nestedScroll(sheetNestedScroll)
        } else {
            paneModifier
        }
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = PureSurface,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, SageOutline),
            modifier = sheetModifier
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Interactive Sheet Handle + Expand/Collapse Header
                val headerBaseModifier = Modifier
                    .fillMaxWidth()
                    .let { mod ->
                        if (onHeaderDrag != null && onHeaderDragStopped != null) {
                            mod.draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta -> onHeaderDrag(delta) },
                                onDragStopped = { velocity -> onHeaderDragStopped(velocity) }
                            )
                        } else {
                            mod
                        }
                    }
                    .let { mod ->
                        if (onToggleExpand != null) {
                            mod
                                .clickable(onClick = onToggleExpand)
                                .testTag("booking_sheet_expand_toggle")
                        } else {
                            mod
                        }
                    }

                Column(
                    modifier = headerBaseModifier
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SageOutline)
                    )
                    if (isExpanded != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isExpanded) {
                                    "Immediate Dispatch • Swipe down for map"
                                } else {
                                    "Scroll up to enlarge booking options"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MutedPine
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = if (isExpanded) "Map" else "Enlarge",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreen
                                )
                                Icon(
                                    imageVector = if (isExpanded) {
                                        Icons.Rounded.KeyboardArrowDown
                                    } else {
                                        Icons.Rounded.KeyboardArrowUp
                                    },
                                    contentDescription = if (isExpanded) "Collapse to show larger map" else "Enlarge booking options",
                                    tint = ForestGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Scrollable Booking Form Sections
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(sheetScrollState)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Demarcated Area + Rate Breakdown Card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (mapState.polygonPoints.size >= 3) SoftSage.copy(alpha = 0.65f) else MistSage,
                        border = BorderStroke(
                            1.dp,
                            if (mapState.polygonPoints.size >= 3) ForestGreen.copy(alpha = 0.28f) else SageOutline
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(ForestGreen.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CropFree,
                                            contentDescription = null,
                                            tint = ForestGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = if (appLanguage == AppLanguage.HI) "चिह्नित क्षेत्र" else "Demarcated Area",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MutedPine
                                        )
                                        Text(
                                            text = if (mapState.polygonPoints.size >= 3) {
                                                "${mapState.computedAcres} Acres"
                                            } else {
                                                "0.0 Acres"
                                            },
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = CharcoalLeaf,
                                            modifier = Modifier.testTag("selected_area_amount_text")
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = VerifiedTealBg,
                                    border = BorderStroke(1.dp, VerifiedTeal.copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.VerifiedUser,
                                            contentDescription = null,
                                            tint = VerifiedTeal,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (mapState.polygonPoints.size >= 3) "GPS Verified" else "Tap Map",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = VerifiedTeal
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PureSurface,
                                border = BorderStroke(1.dp, SageOutline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Rate: ${formatInr(GeoPolygonCalculator.TARIFF_PER_ACRE_INR)}/Acre × ${mapState.computedAcres} ac",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedPine
                                    )
                                    Text(
                                        text = "= ${formatInr(mapState.computedAmount)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ForestGreen
                                    )
                                }
                            }

                            if (mapState.polygonPoints.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onUndoPolygonPoint,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 42.dp)
                                            .testTag("panel_undo_point_button")
                                    ) {
                                        Icon(Icons.Rounded.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Undo Point", style = MaterialTheme.typography.labelMedium)
                                    }
                                    OutlinedButton(
                                        onClick = onClearPolygon,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, AlertRust.copy(alpha = 0.45f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 42.dp)
                                            .testTag("panel_delete_area_button")
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = null,
                                            tint = AlertRust,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Reselect", style = MaterialTheme.typography.labelMedium, color = AlertRust)
                                    }
                                }
                            }
                        }
                    }

                    // Optional Save Farm row when valid polygon drawn
                    if (mapState.polygonPoints.size >= 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = mapState.farmNameInput,
                                onValueChange = onFarmNameChange,
                                placeholder = { Text("Plot name (e.g. North Plot)") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(
                                onClick = onSaveFarmToSupabase,
                                enabled = !mapState.isSavingFarm,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save")
                            }
                        }
                    }

                    // 2. Select Crop Type
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SELECT CROP TYPE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalLeaf
                            )
                            Text(
                                text = "Height auto-calibrated",
                                style = MaterialTheme.typography.labelSmall,
                                color = ForestGreen
                            )
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(FarmerCatalogDefaults.cropTypes) { crop ->
                                val selected = mapState.selectedCrop == crop
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = if (selected) ForestGreen else MistSage,
                                    border = BorderStroke(1.dp, if (selected) ForestGreen else SageOutline),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .clickable { onSelectSprayCrop(crop) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (selected) Icons.Rounded.Agriculture else Icons.Rounded.Eco,
                                            contentDescription = null,
                                            tint = if (selected) PureSurface else ForestGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = crop,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (selected) PureSurface else CharcoalLeaf
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Spray Objective & Formula
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "SPRAY OBJECTIVE & FORMULA",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalLeaf
                        )
                        FarmerCatalogDefaults.sprayObjectives.forEach { option ->
                            val selected = mapState.selectedFormulaId == option.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) SoftSage.copy(alpha = 0.65f) else PureSurface,
                                border = BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) ForestGreen else SageOutline
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectSprayFormula(option.id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 56.dp)
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (selected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (selected) ForestGreen else MutedPine,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = option.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = CharcoalLeaf
                                        )
                                        Text(
                                            text = option.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MutedPine
                                        )
                                    }
                                    Text(
                                        text = option.badge,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) ForestGreen else MutedPine
                                    )
                                }
                            }
                        }
                    }

                    // 4. Payment Mode Cards (Pay Online / Cash on Spraying)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "PAYMENT MODE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalLeaf
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PaymentModeCard(
                                title = "Pay Online",
                                subtitle = "UPI, GPay, PhonePe, Cards",
                                icon = Icons.Rounded.AccountBalanceWallet,
                                selected = mapState.selectedPaymentMethod == PaymentMethod.ONLINE,
                                onClick = { onSelectPaymentMethod(PaymentMethod.ONLINE) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("book_payment_card_online")
                            )
                            PaymentModeCard(
                                title = "Cash on Spraying",
                                subtitle = "Pay pilot upon job completion",
                                icon = Icons.Rounded.Payments,
                                selected = mapState.selectedPaymentMethod == PaymentMethod.CASH,
                                onClick = { onSelectPaymentMethod(PaymentMethod.CASH) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("book_payment_card_cash")
                            )
                        }
                    }

                    // 5. Immediate Dispatch Notice (Booked for Now)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MistSage,
                        border = BorderStroke(1.dp, SageOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VerifiedUser,
                                contentDescription = null,
                                tint = VerifiedTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Immediate dispatch • DGCA Certified Pilot assigned right now (within 15 mins).",
                                style = MaterialTheme.typography.labelSmall,
                                color = CharcoalLeaf
                            )
                        }
                    }
                }

                // Pinned Bottom Primary CTA Footer
                HorizontalDivider(color = SageOutline.copy(alpha = 0.7f))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PureSurface)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { showPaymentChoiceDialog = true },
                        enabled = mapState.polygonPoints.size >= 3 && mapState.computedAcres > 0.0,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("farmer_confirm_booking_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (mapState.polygonPoints.size < 3) {
                                    "Outline Farmland on Map"
                                } else {
                                    "Confirm & Book Spraying"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (mapState.polygonPoints.size >= 3) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = formatInr(mapState.computedAmount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "Non-cancellable after booking & paid • Escrow protected until Job Done",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedPine
                    )
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val sheetScrollState = rememberScrollState()
        if (maxWidth >= 720.dp) {
            Row(modifier = Modifier.fillMaxSize()) {
                mapPane(Modifier.weight(0.58f).fillMaxHeight(), 0.dp)
                bookingSheetPane(
                    Modifier.weight(0.42f).fillMaxHeight(),
                    null,
                    null,
                    null,
                    null,
                    sheetScrollState,
                    null
                )
            }
        } else {
            val density = LocalDensity.current
            val scope = rememberCoroutineScope()
            val collapsedHeightDp = (maxHeight * 0.34f).coerceIn(220.dp, 276.dp)
            val expandedHeightDp = (maxHeight * 0.90f).coerceAtLeast(collapsedHeightDp + 180.dp)
            val collapsedPx = with(density) { collapsedHeightDp.toPx() }
            val expandedPx = with(density) { expandedHeightDp.toPx() }
            val sheetHeightPx = remember { Animatable(collapsedPx) }

            LaunchedEffect(collapsedPx, expandedPx) {
                val clamped = sheetHeightPx.value.coerceIn(collapsedPx, expandedPx)
                if (clamped != sheetHeightPx.value) {
                    sheetHeightPx.snapTo(clamped)
                }
            }

            val isSheetExpanded by remember(collapsedPx, expandedPx) {
                derivedStateOf { sheetHeightPx.value > (collapsedPx + expandedPx) * 0.5f }
            }

            val nestedScrollConnection = remember(collapsedPx, expandedPx) {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        val deltaY = available.y
                        val current = sheetHeightPx.value
                        if (deltaY < 0f && current < expandedPx) {
                            val next = (current - deltaY).coerceIn(collapsedPx, expandedPx)
                            val consumed = -(next - current)
                            scope.launch { sheetHeightPx.snapTo(next) }
                            return Offset(0f, consumed)
                        }
                        if (deltaY > 0f && sheetScrollState.value == 0 && current > collapsedPx) {
                            val next = (current - deltaY).coerceIn(collapsedPx, expandedPx)
                            val consumed = -(next - current)
                            scope.launch { sheetHeightPx.snapTo(next) }
                            return Offset(0f, consumed)
                        }
                        return Offset.Zero
                    }

                    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                        val current = sheetHeightPx.value
                        if (current > collapsedPx && current < expandedPx) {
                            val target = when {
                                available.y < -250f -> expandedPx
                                available.y > 250f -> collapsedPx
                                current >= collapsedPx + (expandedPx - collapsedPx) * 0.35f -> expandedPx
                                else -> collapsedPx
                            }
                            sheetHeightPx.animateTo(
                                targetValue = target,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            )
                        }
                        return super.onPostFling(consumed, available)
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                mapPane(Modifier.fillMaxSize(), collapsedHeightDp)

                val currentSheetHeightDp = with(density) { sheetHeightPx.value.toDp() }
                bookingSheetPane(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(currentSheetHeightDp),
                    isSheetExpanded,
                    {
                        val target = if (isSheetExpanded) collapsedPx else expandedPx
                        scope.launch {
                            sheetHeightPx.animateTo(
                                targetValue = target,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            )
                        }
                    },
                    { delta ->
                        val next = (sheetHeightPx.value - delta).coerceIn(collapsedPx, expandedPx)
                        scope.launch { sheetHeightPx.snapTo(next) }
                    },
                    { velocity ->
                        val current = sheetHeightPx.value
                        val target = when {
                            velocity < -250f -> expandedPx
                            velocity > 250f -> collapsedPx
                            current >= collapsedPx + (expandedPx - collapsedPx) * 0.35f -> expandedPx
                            else -> collapsedPx
                        }
                        scope.launch {
                            sheetHeightPx.animateTo(
                                targetValue = target,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                            )
                        }
                    },
                    sheetScrollState,
                    nestedScrollConnection
                )
            }
        }
    }

    if (showSavedFarmsDialog) {
        SavedFarmsQuickPickerDialog(
            savedFarms = savedFarms,
            onSelectFarm = { farm ->
                showSavedFarmsDialog = false
                onFocusSavedFarm(farm)
            },
            onDismiss = { showSavedFarmsDialog = false }
        )
    }

    if (showPaymentChoiceDialog) {
        BookPaymentMethodDialog(
            acres = mapState.computedAcres,
            hectares = mapState.computedHectares,
            totalAmount = mapState.computedAmount,
            initialSelection = mapState.selectedPaymentMethod,
            onConfirmPaymentChoice = { chosenMethod ->
                showPaymentChoiceDialog = false
                onSelectPaymentMethod(chosenMethod)
                onConfirmOrderWithMethod(chosenMethod)
            },
            onDismiss = { showPaymentChoiceDialog = false }
        )
    }
}

@Composable
private fun FloatingMapChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (active) PureSurface.copy(alpha = 0.96f) else PureSurface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, if (active) ForestGreen.copy(alpha = 0.35f) else SageOutline),
        tonalElevation = 2.dp,
        modifier = Modifier
            .heightIn(min = 38.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ForestGreen,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = CharcoalLeaf
            )
        }
    }
}

@Composable
private fun PaymentModeCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) SoftSage.copy(alpha = 0.7f) else MistSage.copy(alpha = 0.6f),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) ForestGreen else SageOutline
        ),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = ForestGreen,
                    modifier = Modifier.size(20.dp)
                )
                Icon(
                    imageVector = if (selected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (selected) ForestGreen else MutedPine,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = CharcoalLeaf
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MutedPine,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SavedFarmsQuickPickerDialog(
    savedFarms: List<FarmBoundaryEntity>,
    onSelectFarm: (FarmBoundaryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select from My Farms", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                savedFarms.take(6).forEach { farm ->
                    val pts = GeoPolygonCalculator.decodePolygon(farm.polygonCoordinatesJson)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MistSage,
                        border = BorderStroke(1.dp, SageOutline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectFarm(farm) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MiniPolygonPreview(points = pts, modifier = Modifier.size(46.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(farm.farmName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${farm.areaAcres} Acres • ${farm.areaHectares} Ha",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedPine
                                )
                            }
                            Text(
                                text = "Load",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun BookPaymentMethodDialog(
    acres: Double,
    hectares: Double,
    totalAmount: Double,
    initialSelection: PaymentMethod,
    onConfirmPaymentChoice: (PaymentMethod) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMethod by remember { mutableStateOf(initialSelection) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Confirm Payment Method",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SoftSage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Selected Farmland", style = MaterialTheme.typography.labelSmall, color = ForestGreen)
                            Text(
                                text = "$acres Acres • $hectares Ha",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen
                            )
                        }
                        Text(
                            text = formatInr(totalAmount),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen
                        )
                    }
                }

                PaymentMethod.entries.forEach { method ->
                    val isSelected = selectedMethod == method
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) SoftSage else MistSage,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) ForestGreen else SageOutline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedMethod = method }
                            .testTag("payment_option_${method.apiValue}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (method == PaymentMethod.ONLINE) {
                                    Icons.Rounded.Payments
                                } else {
                                    Icons.Rounded.Agriculture
                                },
                                contentDescription = method.label,
                                tint = if (isSelected) ForestGreen else MutedPine,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = method.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) ForestGreen else CharcoalLeaf
                                )
                                Text(
                                    text = if (method == PaymentMethod.ONLINE) {
                                        "Pay via Razorpay (UPI, Card, NetBanking). Escrow protected until 'Job Done'."
                                    } else {
                                        "Pay cash directly to your pilot after spraying is completed."
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedPine
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = ForestGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmPaymentChoice(selectedMethod) },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("payment_dialog_confirm_button")
            ) {
                Text(
                    if (selectedMethod == PaymentMethod.ONLINE) {
                        "Pay Online • ${formatInr(totalAmount)}"
                    } else {
                        "Book with Cash • ${formatInr(totalAmount)}"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
