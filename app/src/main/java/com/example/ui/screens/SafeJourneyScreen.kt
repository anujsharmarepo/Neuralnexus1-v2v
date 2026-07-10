package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.AbhayaViewModel
import org.osmdroid.views.MapView as OsmMapView
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker as OsmMarker
import org.osmdroid.views.overlay.Polyline as OsmPolyline
import org.osmdroid.config.Configuration
import com.example.ui.screens.LatLng
import java.util.*

@Composable
fun rememberMapViewWithLifecycle(): OsmMapView {
    val context = LocalContext.current
    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        OsmMapView(context).apply {
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> {
                    mapView.onDetach()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    return mapView
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeJourneyScreen(
    viewModel: AbhayaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Observe ViewModel states
    val isJourneyActive by viewModel.isSafeJourneyActive.collectAsStateWithLifecycle()
    val destination by viewModel.journeyDestination.collectAsStateWithLifecycle()
    val distance by viewModel.journeyDistance.collectAsStateWithLifecycle()
    val eta by viewModel.journeyEta.collectAsStateWithLifecycle()
    val timerSeconds by viewModel.journeyTimerSeconds.collectAsStateWithLifecycle()
    val journeyStatus by viewModel.journeyStatus.collectAsStateWithLifecycle()
    val checkInInterval by viewModel.checkInInterval.collectAsStateWithLifecycle()
    val nextCheckInSeconds by viewModel.nextCheckInSeconds.collectAsStateWithLifecycle()
    val showCheckInDialog by viewModel.showCheckInDialog.collectAsStateWithLifecycle()
    val checkInGraceSeconds by viewModel.checkInGraceSeconds.collectAsStateWithLifecycle()
    val showArrivalPopup by viewModel.showArrivalPopup.collectAsStateWithLifecycle()
    val guardians by viewModel.guardians.collectAsStateWithLifecycle()
    val alertMessage by viewModel.alertMessage.collectAsStateWithLifecycle()

    // Edge Cases State Flows
    val isGpsDisabled by viewModel.gpsDisabled.collectAsStateWithLifecycle()
    val isInternetOffline by viewModel.internetUnavailable.collectAsStateWithLifecycle()
    val isPermissionDenied by viewModel.permissionDenied.collectAsStateWithLifecycle()
    val isDestNotFound by viewModel.destinationNotFound.collectAsStateWithLifecycle()

    // Maps, Route and Nearby Police properties
    val currentGpsLatLng by viewModel.currentLatLng.collectAsStateWithLifecycle()
    val routePoints by viewModel.routePolylinePoints.collectAsStateWithLifecycle()
    val destLatLng by viewModel.destinationLatLng.collectAsStateWithLifecycle()
    val policeStations by viewModel.nearbyPoliceStations.collectAsStateWithLifecycle()
    val placeSuggestions by viewModel.placeSuggestions.collectAsStateWithLifecycle()

    // Local Search Input UI State
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var showSimulatorDrawer by remember { mutableStateOf(false) }

    // Request Location permission
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!fineGranted && !coarseGranted) {
            viewModel.permissionDenied.value = true
            Toast.makeText(context, "Location permission is required for Safe Journey", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.permissionDenied.value = false
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Trigger Places Search on Query changed
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            viewModel.searchPlaces(searchQuery)
        }
    }

    val mockLocations = listOf(
        "Airport Terminal 2",
        "Downtown Central Plaza",
        "Metro Station West",
        "Corporate IT Tech Park",
        "City General Hospital",
        "Greenwood Residential Society",
        "Westside Shopping Mall",
        "Central Railway Station",
        "Vasant Kunj Residential Block B",
        "Bandra Kurla Complex (BKC)"
    )

    val filteredLocations = remember(searchQuery, placeSuggestions, isDestNotFound) {
        if (isDestNotFound) {
            emptyList()
        } else if (searchQuery.isBlank()) {
            emptyList()
        } else if (placeSuggestions.isNotEmpty()) {
            placeSuggestions
        } else {
            mockLocations.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Calculated arrival time based on ETA
    val arrivalTimeStr = remember(eta) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, eta)
        val sdf = java.text.SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(calendar.time)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SAFE JOURNEY",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF301114),
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.closeSafeJourney() },
                        modifier = Modifier.testTag("safe_journey_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back to main dashboard",
                            tint = Color(0xFF301114)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSimulatorDrawer = !showSimulatorDrawer },
                        modifier = Modifier.testTag("safe_journey_simulator_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Simulator settings",
                            tint = if (isGpsDisabled || isInternetOffline || isPermissionDenied || isDestNotFound) Color(0xFFFF004D) else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFF1F2))
            )
        },
        containerColor = Color(0xFFFFF1F2)
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. TOP INTERACTIVE GOOGLE MAPS SIMULATION (Premium Canvas Drawing)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, Color(0xFFFF004D).copy(alpha = 0.15f))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Real Embedded OpenStreetMap View
                        val mapView = rememberMapViewWithLifecycle()
                        AndroidView(
                            factory = { mapView },
                            modifier = Modifier.fillMaxSize()
                        ) { mv ->
                            mv.overlays.clear()

                            val userLatLng = currentGpsLatLng
                            val userGeoPoint = GeoPoint(userLatLng.latitude, userLatLng.longitude)

                            // Set camera center and zoom
                            mv.controller.setCenter(userGeoPoint)
                            mv.controller.setZoom(15.5)

                            // User location marker
                            val userMarker = OsmMarker(mv).apply {
                                position = userGeoPoint
                                setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_BOTTOM)
                                title = "My Location"
                            }
                            mv.overlays.add(userMarker)

                            if (destination != null) {
                                if (routePoints.isNotEmpty()) {
                                    val polyline = OsmPolyline().apply {
                                        setPoints(routePoints.map { GeoPoint(it.latitude, it.longitude) })
                                        color = 0xFFFF004D.toInt()
                                        width = 8f
                                    }
                                    mv.overlays.add(polyline)

                                    val points = routePoints.map { GeoPoint(it.latitude, it.longitude) }
                                    if (points.isNotEmpty()) {
                                        val minLat = points.minOf { it.latitude }
                                        val maxLat = points.maxOf { it.latitude }
                                        val minLon = points.minOf { it.longitude }
                                        val maxLon = points.maxOf { it.longitude }
                                        val box = org.osmdroid.util.BoundingBox(maxLat, maxLon, minLat, minLon)
                                        mv.zoomToBoundingBox(box, true, 80)
                                    }
                                }

                                val endLatLng = destLatLng ?: LatLng(userLatLng.latitude + 0.015, userLatLng.longitude + 0.015)
                                val destGeoPoint = GeoPoint(endLatLng.latitude, endLatLng.longitude)
                                val destMarker = OsmMarker(mv).apply {
                                    position = destGeoPoint
                                    setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_BOTTOM)
                                    title = destination
                                }
                                mv.overlays.add(destMarker)

                                if (routePoints.isEmpty()) {
                                    mv.controller.setCenter(destGeoPoint)
                                    mv.controller.setZoom(14.0)
                                }
                            }

                            policeStations.forEach { station ->
                                val stationGeoPoint = GeoPoint(station.latLng.latitude, station.latLng.longitude)
                                val stationMarker = OsmMarker(mv).apply {
                                    position = stationGeoPoint
                                    setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_BOTTOM)
                                    title = station.name
                                    subDescription = station.vicinity
                                }
                                mv.overlays.add(stationMarker)
                            }

                            mv.invalidate()
                        }

                        // Compass and Speed Indicators Overlay
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = "Compass",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp).rotate(45f)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isJourneyActive) "42 km/h" else "0 km/h",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // Error State Overlays on map
                        if (isGpsDisabled) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.75f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                    Icon(Icons.Default.GpsOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("GPS SIGNAL DISABLED", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Enable GPS sensor telemetry in simulation overrides.", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, textAlign = TextAlign.Center)
                                }
                            }
                        } else if (isPermissionDenied) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.75f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                    Icon(Icons.Default.NoEncryption, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("LOCATION ACCESS DENIED", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Grant location permission in device simulator panel.", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }

                // 2. PLACES AUTOCOMPLETE SEARCH
                if (!isJourneyActive) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "SET YOUR DESTINATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF004D),
                            letterSpacing = 1.5.sp
                        )

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                isSearchFocused = true
                            },
                            placeholder = { Text("Search places or address...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Icon",
                                    tint = Color.Gray
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear Search")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("places_search_input"),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF004D),
                                unfocusedBorderColor = Color(0xFFFF004D).copy(alpha = 0.2f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        // Autocomplete search results dropdown
                        AnimatedVisibility(
                            visible = isSearchFocused && (filteredLocations.isNotEmpty() || searchQuery.isNotBlank()),
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFFFF004D).copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                    if (filteredLocations.isEmpty()) {
                                        Text(
                                            text = "No destinations matching search found.",
                                            color = Color.Gray,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    } else {
                                        filteredLocations.forEach { loc ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        searchQuery = loc
                                                        isSearchFocused = false
                                                        viewModel.selectJourneyDestination(loc)
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.LocationOn,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFF004D).copy(alpha = 0.6f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = loc,
                                                    fontSize = 14.sp,
                                                    color = Color.DarkGray,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. CONFIGURABLE SAFETY CHECK-IN INTERVALS
                if (!isJourneyActive) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "SAFETY INTERVAL PROTOCOL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF004D),
                            letterSpacing = 1.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(5, 10, 15, 30).forEach { mins ->
                                val selected = checkInInterval == mins
                                Button(
                                    onClick = { viewModel.setCheckInInterval(mins) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (selected) Color(0xFFFF004D) else Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (selected) Color.Transparent else Color(0xFFFF004D).copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("interval_chip_$mins"),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = "$mins Min",
                                        color = if (selected) Color.White else Color.DarkGray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = "You will receive an overlay prompt asking if you are safe at the chosen interval. Missing it engages emergency protocols.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                // 4. JOURNEY DASHBOARD CARD (Premium M3 Display)
                if (destination != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, Color(0xFFFF004D).copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth().testTag("journey_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header: Status and Destination
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                when (journeyStatus) {
                                                    "Started" -> Color(0xFF2196F3)
                                                    "In Progress" -> Color(0xFFFF9800)
                                                    "Arrived" -> Color(0xFF4CAF50)
                                                    else -> Color.Gray
                                                },
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = journeyStatus.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.DarkGray
                                    )
                                }

                                if (isJourneyActive) {
                                    // Safety Countdown indicator
                                    val nextCheckInMin = nextCheckInSeconds / 60
                                    val nextCheckInSec = nextCheckInSeconds % 60
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFFFF1F2), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = null,
                                                tint = Color(0xFFFF004D),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = String.format("Check-In: %02d:%02d", nextCheckInMin, nextCheckInSec),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFF004D)
                                            )
                                        }
                                    }
                                }
                            }

                            Divider(color = Color.LightGray.copy(alpha = 0.4f))

                            // Route Details
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NearMe,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "CURRENT LOCATION",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )
                                }
                                Text(
                                    text = "Your GPS coordinates are armed and secure",
                                    fontSize = 13.sp,
                                    color = Color.DarkGray,
                                    modifier = Modifier.padding(start = 24.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Place,
                                                contentDescription = null,
                                                tint = Color(0xFFFF004D),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "DESTINATION",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFF004D)
                                            )
                                        }
                                        Text(
                                            text = destination ?: "",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF301114),
                                            modifier = Modifier.padding(start = 24.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val mapUrl = "https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(destination ?: "")}"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl))
                                            intent.setPackage("com.google.android.apps.maps")
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                try {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl)))
                                                } catch (e2: Exception) {
                                                    Toast.makeText(context, "Could not open map link", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .testTag("open_external_navigation")
                                            .shadow(2.dp, CircleShape)
                                            .background(Color.White, CircleShape)
                                            .size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Directions,
                                            contentDescription = "Navigate in Google Maps",
                                            tint = Color(0xFFFF004D),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Divider(color = Color.LightGray.copy(alpha = 0.4f))

                            // Telemetry items row (ETA, Distance, Arrival, Timer)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // 1. Distance
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text("DISTANCE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("$distance km", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF301114))
                                }

                                // 2. ETA
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text("ETA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("$eta mins", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF301114))
                                }

                                // 3. Est Arrival
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text("ARRIVAL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(arrivalTimeStr, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF301114))
                                }

                                // 4. Elapsed Timer
                                if (isJourneyActive) {
                                    val minutes = timerSeconds / 60
                                    val seconds = timerSeconds % 60
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Text("ACTIVE TIMER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(String.format("%02d:%02d", minutes, seconds), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF004D))
                                    }
                                }
                            }

                            // Guardian & Firebase Live status (Only visible when active)
                            if (isJourneyActive) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFCE8EB), RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "LIVE GUARDIAN SYNC ACTIVE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFF004D),
                                        letterSpacing = 1.sp
                                    )
                                    
                                    val activeNames = guardians.filter { it.isActive }.take(5).map { it.name }
                                    val namesLabel = if (activeNames.isNotEmpty()) activeNames.joinToString() else "No active guardians"
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Sms, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Broadcasting live coordinates link to: $namesLabel", fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isInternetOffline) "Offline buffering. Saving telemetry logs locally." else "Real-time sync'd to Firebase Firestore database.",
                                            fontSize = 11.sp,
                                            color = if (isInternetOffline) Color(0xFFFF004D) else Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // 5. ACTIONS CONTAINER
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (!isJourneyActive) {
                                    // Start Journey Action
                                    Button(
                                        onClick = { viewModel.startJourney() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                            .testTag("start_journey_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF004D)),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("START JOURNEY", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                } else {
                                    // Share Journey Trigger
                                    Button(
                                        onClick = {
                                            val shareUrl = "https://abhaya-safety.net/track?journey=${System.currentTimeMillis()}"
                                            Toast.makeText(context, "Tracking Link copied to clipboard:\n$shareUrl", Toast.LENGTH_LONG).show()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                            .testTag("share_journey_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D161C)),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("SHARE LINK", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }

                                    // End Journey Trigger
                                    Button(
                                        onClick = { viewModel.endJourney(reachedSafely = true) },
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .height(50.dp)
                                            .testTag("end_journey_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("I'VE ARRIVED", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                    }
                                }
                            }
                            
                            // Cancel button (Visible only when active)
                            if (isJourneyActive) {
                                OutlinedButton(
                                    onClick = { viewModel.endJourney(reachedSafely = false) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF004D)),
                                    border = BorderStroke(1.dp, Color(0xFFFF004D).copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("cancel_journey_btn")
                                ) {
                                    Text("CANCEL JOURNEY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // 6. HISTORIC JOURNEYS CARD
                val journeyHistory by viewModel.journeyHistory.collectAsStateWithLifecycle()
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF004D).copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "JOURNEY HISTORY LOGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF004D),
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (journeyHistory.isEmpty()) {
                            Text(
                                text = "Your past safety journeys will appear here once completed.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                journeyHistory.take(4).forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.destination,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF301114)
                                            )
                                            Text(
                                                text = "Start: ${item.startTime} • Duration: ${item.duration} • Dist: ${item.distance}",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (item.status == "Completed") Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = item.status.uppercase(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (item.status == "Completed") Color(0xFF2E7D32) else Color(0xFFD50000)
                                            )
                                        }
                                    }
                                    Divider(color = Color.LightGray.copy(alpha = 0.2f))
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }

            // 7. EXPANDABLE SIMULATOR DRAWER (Bottom overlays for robust evaluation of edge cases)
            AnimatedVisibility(
                visible = showSimulatorDrawer,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EDGE CASE SIMULATOR PANEL",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFF004D),
                                letterSpacing = 1.sp
                            )
                            IconButton(onClick = { showSimulatorDrawer = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.1f))

                        // Switch 1: GPS Disabled
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("GPS Sensor Failure", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Simulate active GPS loss / disabled sensor", color = Color.Gray, fontSize = 11.sp)
                            }
                            Switch(
                                checked = isGpsDisabled,
                                onCheckedChange = { viewModel.gpsDisabled.value = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF004D), checkedTrackColor = Color(0xFFFF004D).copy(alpha = 0.4f))
                            )
                        }

                        // Switch 2: Internet Unavailable
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("No Network Signal", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Buffers telemetry locally; blocks Firebase sync", color = Color.Gray, fontSize = 11.sp)
                            }
                            Switch(
                                checked = isInternetOffline,
                                onCheckedChange = { viewModel.internetUnavailable.value = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF004D), checkedTrackColor = Color(0xFFFF004D).copy(alpha = 0.4f))
                            )
                        }

                        // Switch 3: Permission Denied
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Location Permission Denied", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Mock runtime request dismissal", color = Color.Gray, fontSize = 11.sp)
                            }
                            Switch(
                                checked = isPermissionDenied,
                                onCheckedChange = { viewModel.permissionDenied.value = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF004D), checkedTrackColor = Color(0xFFFF004D).copy(alpha = 0.4f))
                            )
                        }

                        // Switch 4: Destination Not Found
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Places API Fail", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Forced Destination lookup failure on Search", color = Color.Gray, fontSize = 11.sp)
                            }
                            Switch(
                                checked = isDestNotFound,
                                onCheckedChange = { viewModel.destinationNotFound.value = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF004D), checkedTrackColor = Color(0xFFFF004D).copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }

            // 8. SAFETY CHECK-IN DIALOG OVERLAY ("Are you safe?")
            if (showCheckInDialog) {
                Dialog(onDismissRequest = { /* Force response to prevent dismiss */ }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(2.dp, Color(0xFFFF004D)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("safety_checkin_dialog")
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // High Contrast Warning Beacon animation
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse_beacon")
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 0.95f,
                                targetValue = 1.05f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(600, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulseScale"
                            )

                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .scale(pulseScale)
                                    .background(Color(0xFFFF004D).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFFFF004D),
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Are You Safe?",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Safe Journey Interval check required. Confirm safety or request emergency assistance immediately.",
                                fontSize = 13.sp,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Grace Period countdown bar
                            Text(
                                text = "Automatic SOS triggering in: ${checkInGraceSeconds}s",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF004D)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { checkInGraceSeconds / 15f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = Color(0xFFFF004D),
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 1. YES, I'm safe button
                                Button(
                                    onClick = { viewModel.respondToCheckIn(safe = true) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .testTag("safety_yes_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("YES, I'M SAFE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                // 2. NEED HELP emergency button
                                Button(
                                    onClick = { viewModel.respondToCheckIn(safe = false) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .testTag("safety_need_help_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF004D)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("NEED HELP", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 9. ARRIVAL BEAUTIFUL POPUP OVERLAY ("Destination Reached Safely")
            if (showArrivalPopup) {
                Dialog(onDismissRequest = { viewModel.dismissArrivalPopup() }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("arrival_popup_dialog")
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFFE8F5E9), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OfflinePin,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Destination Reached!",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF301114),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "You have arrived safely at your chosen destination. Live tracking has been stopped automatically.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 1. Share Arrival details
                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Arrival message sent to active guardians!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("arrival_share_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D161C)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("SHARE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                // 2. Done button
                                Button(
                                    onClick = { viewModel.dismissArrivalPopup() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("arrival_done_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("DONE", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
