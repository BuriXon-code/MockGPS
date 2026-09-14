package dev.burixon.mockgps

import android.annotation.SuppressLint
import android.Manifest
import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Color as AndroidColor
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.CompoundButton
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.camera.CameraPosition
import android.graphics.drawable.GradientDrawable
import android.widget.ImageButton
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.geojson.Point
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleOpacity
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import java.util.Locale
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import android.widget.FrameLayout
import android.widget.Toast

class MainActivity : ComponentActivity() {

    private lateinit var versionText: TextView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var mapView: MapView

    private lateinit var menuButton: TextView
    private lateinit var coordinatesText: TextView
    private lateinit var copyCoordinatesButton: ImageButton
    private lateinit var toggleButton: Button
    private lateinit var mockLocationButton: ImageButton
    private lateinit var bootSwitch: CompoundButton
    private lateinit var broadcastSwitch: CompoundButton
    private lateinit var driftSwitch: CompoundButton

    private lateinit var scaleContainer: View
    private lateinit var scaleLine: View
    private lateinit var scaleLabel: TextView
    private lateinit var bottomPanel: View

    private var statusBarInsetTop = 0

    private var map: MapLibreMap? = null
    private val mapPrefs by lazy {
        getSharedPreferences(
            "map_camera",
            MODE_PRIVATE
        )
    }
    private var selectedMarker: Marker? = null
    private var realLocationSource: GeoJsonSource? = null
    private var lastSelectedLat: Double? = null
    private var lastSelectedLon: Double? = null

    private var lastRealLat: Double? = null
    private var lastRealLon: Double? = null

    private val handler =
        Handler(
            Looper.getMainLooper()
        )

    private val uiRunnable =
        object : Runnable {

            override fun run() {

                refreshFromState()

                handler.postDelayed(
                    this,
                    1000L
                )
            }
        }

    private val locationManager by lazy {
        getSystemService(
            LocationManager::class.java
        )
    }

    private val locationListener =
        object : LocationListener {

            override fun onLocationChanged(
                location: Location
            ) {

                if (
                    State.isServiceRunning(
                        this@MainActivity
                    )
                ) {
                    return
                }

                updateRealLocation(
                    location.latitude,
                    location.longitude
                )
            }
        }

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts
                .RequestMultiplePermissions()
        ) {
            refreshFromState()
            startRealLocationUpdates()
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )

        MapLibre.getInstance(
            this
        )

        setContentView(
            R.layout.activity_main
        )

        drawerLayout =
            findViewById(
                R.id.drawerLayout
            )

        drawerLayout.addDrawerListener(
            object : DrawerLayout.SimpleDrawerListener() {

                override fun onDrawerOpened(
                    drawerView: View
                ) {
                    drawerView.bringToFront()
                    drawerView.invalidate()
                }
            }
        )

        val mapContainer =
            findViewById<FrameLayout>(
                R.id.mapContainer
            )

        mapView =
            MapView(
                this,
                MapLibreMapOptions()
                    .textureMode(true)
            )

        mapContainer.addView(
            mapView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        menuButton =
            findViewById(
                R.id.menuButton
            )

        coordinatesText =
            findViewById(
                R.id.coordinatesText
            )

        copyCoordinatesButton =
            findViewById(
                R.id.copyCoordinatesButton
            )

        versionText =
            findViewById(
                R.id.versionText
            )

        versionText.text =
            "MockGPS ${BuildConfig.VERSION_NAME}\n" +
                    "© 2026 Kamil BuriXon Burek\n" +
                    "GPL-3.0"

        toggleButton =
            findViewById(
                R.id.toggleButton
            )

        mockLocationButton =
            findViewById(
                R.id.mockLocationButton
            )

        bootSwitch =
            findViewById(
                R.id.bootSwitch
            )

        broadcastSwitch =
            findViewById(
                R.id.broadcastSwitch
            )

        driftSwitch =
            findViewById(
                R.id.driftSwitch
            )

        scaleContainer =
            findViewById(
                R.id.scaleContainer
            )

        scaleLine =
            findViewById(
                R.id.scaleLine
            )

        scaleLabel =
            findViewById(
                R.id.scaleLabel
            )

        bottomPanel =
            findViewById(
                R.id.bottomPanel
            )

        mapView.onCreate(
            savedInstanceState
        )

        setupInsets()
        setupUi()
        setupMap()

        requestPermissionsIfNeeded()

        refreshFromState()
    }

    private fun setupInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(
                R.id.drawer
            )
        ) { view, insets ->

            val status =
                insets.getInsets(
                    WindowInsetsCompat.Type.statusBars()
                )

            view.setPadding(
                view.paddingLeft,
                status.top,
                view.paddingRight,
                view.paddingBottom
            )

            view.bringToFront()

            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(
                R.id.drawer
            )
        ) { view, insets ->

            val status =
                insets.getInsets(
                    WindowInsetsCompat.Type.statusBars()
                )

            statusBarInsetTop =
                status.top

            view.setPadding(
                view.paddingLeft,
                statusBarInsetTop,
                view.paddingRight,
                view.paddingBottom
            )

            updateCompassPosition()

            insets
        }
    }

    private fun centerOnMockLocation() {

        val loadedMap =
            map ?: return

        val lat =
            State.getLat(this)

        val lon =
            State.getLon(this)

        if (
            lat == null ||
            lon == null
        ) {
            return
        }

        val currentZoom =
            loadedMap.cameraPosition.zoom

        val targetZoom =
            currentZoom.coerceAtLeast(
                15.0
            )

        loadedMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    lat,
                    lon
                ),
                targetZoom
            )
        )
    }

    private fun updateMockLocationButtonAppearance() {

        val color =
            if (
                State.isServiceRunning(this)
            ) {
                AndroidColor.rgb(
                    224,
                    72,
                    82
                )
            } else {
                AndroidColor.rgb(
                    90,
                    160,
                    235
                )
            }

        mockLocationButton.background =
            GradientDrawable().apply {
                shape =
                    GradientDrawable.OVAL

                setColor(color)

                setStroke(
                    1.dp,
                    AndroidColor.argb(
                        90,
                        255,
                        255,
                        255
                    )
                )
            }
    }

    private fun setupRealLocationLayer() {

        val loadedMap = map ?: return
        val style = loadedMap.style ?: return

        if (style.getSource("real-location-source") != null) {
            realLocationSource =
                style.getSource("real-location-source") as? GeoJsonSource
            return
        }

        val source =
            GeoJsonSource(
                "real-location-source"
            )

        style.addSource(source)

        val layer =
            CircleLayer(
                "real-location-layer",
                "real-location-source"
            ).withProperties(
                circleRadius(5f),
                circleColor(
                    AndroidColor.rgb(
                        135,
                        140,
                        148
                    )
                ),
                circleOpacity(1f),
                circleStrokeColor(
                    AndroidColor.WHITE
                ),
                circleStrokeWidth(1.5f)
            )

        style.addLayer(layer)

        realLocationSource = source
    }

    private fun updateCompassPosition() {

        val loadedMap =
            map ?: return

        loadedMap.uiSettings.setCompassMargins(
            8.dp,
            statusBarInsetTop + 8.dp,
            8.dp,
            8.dp
        )
    }

    private fun setupUi() {

        menuButton.setOnClickListener {
            drawerLayout.openDrawer(
                Gravity.START
            )
        }

        copyCoordinatesButton.setOnClickListener {

            val lat =
                State.getLat(this)

            val lon =
                State.getLon(this)

            if (
                lat == null ||
                lon == null
            ) {
                return@setOnClickListener
            }

            val coordinates =
                String.format(
                    Locale.US,
                    "%.6f, %.6f",
                    lat,
                    lon
                )

            val clipboard =
                getSystemService(
                    android.content.ClipboardManager::class.java
                )

            clipboard.setPrimaryClip(
                android.content.ClipData.newPlainText(
                    "MockGPS coordinates",
                    coordinates
                )
            )

            Toast.makeText(
                this,
                "Coordinates copied.",
                Toast.LENGTH_SHORT
            ).show()
        }

        toggleButton.setOnClickListener {

            animateButtonPress()

            if (
                State.isServiceRunning(this)
            ) {
                stopMocking()
            } else {
                startMocking()
            }
        }

        mockLocationButton.setOnClickListener {
            centerOnMockLocation()
        }

        bootSwitch.setOnCheckedChangeListener {
                _: CompoundButton,
                checked: Boolean ->

            State.setAutoStart(
                this,
                checked
            )
        }

        broadcastSwitch.setOnCheckedChangeListener {
                _: CompoundButton,
                checked: Boolean ->

            State.setBroadcastEnabled(
                this,
                checked
            )
        }

        driftSwitch.setOnCheckedChangeListener {
                _: CompoundButton,
                checked: Boolean ->

            State.setDrifting(
                this,
                checked
            )

            if (!checked) {
                refreshFromState()
            }
        }

        findViewById<TextView>(
            R.id.githubButton
        ).setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    android.net.Uri.parse(
                        "https://github.com/BuriXon-code/MockGPS/"
                    )
                )
            )
        }

        findViewById<TextView>(
            R.id.websiteButton
        ).setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    android.net.Uri.parse(
                        "https://burixon.dev/MockGPS/"
                    )
                )
            )
        }

        findViewById<TextView>(
            R.id.donationsButton
        ).setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    android.net.Uri.parse(
                        "https://buycoffee.to/burixon-code"
                    )
                )
            )
        }
    }

    private fun saveCameraPosition() {

        val loadedMap =
            map ?: return

        val position =
            loadedMap.cameraPosition

        val target =
            position.target ?: return

        mapPrefs.edit()
            .putLong(
                "lat",
                target.latitude.toBits()
            )
            .putLong(
                "lon",
                target.longitude.toBits()
            )
            .putLong(
                "zoom",
                position.zoom.toBits()
            )
            .putLong(
                "bearing",
                position.bearing.toBits()
            )
            .putLong(
                "tilt",
                position.tilt.toBits()
            )
            .apply()
    }

    private fun restoreCameraPosition(): Boolean {

        val loadedMap =
            map ?: return false

        if (
            !mapPrefs.contains("lat") ||
            !mapPrefs.contains("lon") ||
            !mapPrefs.contains("zoom")
        ) {
            return false
        }

        val lat =
            Double.fromBits(
                mapPrefs.getLong(
                    "lat",
                    0L
                )
            )

        val lon =
            Double.fromBits(
                mapPrefs.getLong(
                    "lon",
                    0L
                )
            )

        val zoom =
            Double.fromBits(
                mapPrefs.getLong(
                    "zoom",
                    10.0.toBits()
                )
            )

        val bearing =
            Double.fromBits(
                mapPrefs.getLong(
                    "bearing",
                    0.0.toBits()
                )
            )

        val tilt =
            Double.fromBits(
                mapPrefs.getLong(
                    "tilt",
                    0.0.toBits()
                )
            )

        loadedMap.cameraPosition =
            CameraPosition.Builder()
                .target(
                    LatLng(
                        lat,
                        lon
                    )
                )
                .zoom(zoom)
                .bearing(bearing)
                .tilt(tilt)
                .build()

        return true
    }

    private fun setupMap() {

        mapView.getMapAsync {
                loadedMap ->

            map = loadedMap

            loadedMap.uiSettings
                .isLogoEnabled = false

            loadedMap.uiSettings
                .isAttributionEnabled = false

            updateCompassPosition()

            loadedMap.setStyle(
                "https://tiles.openfreemap.org/styles/liberty"
            ) {
                setupRealLocationLayer()

                loadedMap.addOnMapClickListener {
                        point ->

                    selectLocation(
                        point.latitude,
                        point.longitude,
                        false
                    )

                    true
                }

                loadedMap.addOnCameraIdleListener {
                    updateScaleBar()
                    saveCameraPosition()
                }

                val restored =
                    restoreCameraPosition()

                showStoredLocations(
                    moveCamera = !restored
                )

                updateScaleBar()
                updateMockLocationButtonAppearance()
            }
        }
    }

    private fun refreshFromState() {

        val running =
            State.isServiceRunning(
                this
            )

        /*
         * Ignore listener callbacks while updating switches from external
         * broadcasts. The switches are therefore always synchronized with
         * State without generating accidental writes.
         */

        bootSwitch.setOnCheckedChangeListener(
            null
        )

        bootSwitch.isChecked =
            State.isAutoStart(
                this
            )

        bootSwitch.setOnCheckedChangeListener {
                _: CompoundButton,
                checked: Boolean ->

            State.setAutoStart(
                this,
                checked
            )
        }

        broadcastSwitch.setOnCheckedChangeListener(
            null
        )

        broadcastSwitch.isChecked =
            State.isBroadcastEnabled(
                this
            )

        broadcastSwitch.setOnCheckedChangeListener {
                _: CompoundButton,
                checked: Boolean ->

            State.setBroadcastEnabled(
                this,
                checked
            )
        }

        driftSwitch.setOnCheckedChangeListener(
            null
        )

        driftSwitch.isChecked =
            State.isDrifting(
                this
            )

        driftSwitch.setOnCheckedChangeListener {
                _: CompoundButton,
                checked: Boolean ->

            State.setDrifting(
                this,
                checked
            )
        }

        toggleButton.text =
            if (running) {
                "STOP MOCKING"
            } else {
                "START MOCKING"
            }

        toggleButton.setBackgroundResource(
            if (running) {
                R.drawable.button_stop
            } else {
                R.drawable.button_start
            }
        )

        updateMockLocationButtonAppearance()
        updateSelectedMarkerAppearance()

        val lat =
            State.getLat(this)

        val lon =
            State.getLon(this)

        if (
            lat != null &&
            lon != null
        ) {

            coordinatesText.text =
                String.format(
                    Locale.US,
                    "%.6f,%.6f",
                    lat,
                    lon
                )

            if (
                lat != lastSelectedLat ||
                lon != lastSelectedLon
            ) {

                lastSelectedLat = lat
                lastSelectedLon = lon

                updateSelectedMarker(
                    lat,
                    lon,
                    false
                )
            }
        }
    }

    private fun startMocking() {

        if (
            !hasLocationPermission()
        ) {
            requestPermissionsIfNeeded()
            return
        }

        if (
            !isMockLocationAllowed()
        ) {
            showDeveloperOptions()
            return
        }

        /*
         * Capture the real location before test providers are enabled.
         */

        captureRealLocation()
        stopRealLocationUpdates()

        State.setDesiredEnabled(
            this,
            true
        )

        State.setNotifyMode(
            this,
            State.NOTIFY_TOAST
        )

        val intent =
            Intent(
                this,
                MockLocationService::class.java
            ).apply {

                putExtra(
                    MockLocationService.COMMAND,
                    MockLocationService.COMMAND_ON
                )

                putExtra(
                    MockLocationService.EXTRA_TOAST,
                    true
                )
            }

        ContextCompat.startForegroundService(
            this,
            intent
        )

        refreshFromState()
    }

    private fun stopMocking() {

        State.setDesiredEnabled(
            this,
            false
        )

        State.setServiceRunning(
            this,
            false
        )

        stopService(
            Intent(
                this,
                MockLocationService::class.java
            )
        )

        startRealLocationUpdates()

        refreshFromState()
    }

    private fun selectLocation(
        lat: Double,
        lon: Double,
        moveCamera: Boolean
    ) {

        if (
            !State.validLat(lat) ||
            !State.validLon(lon)
        ) {
            return
        }

        State.setLocation(
            this,
            lat,
            lon
        )

        lastSelectedLat = lat
        lastSelectedLon = lon

        updateSelectedMarker(
            lat,
            lon,
            moveCamera
        )

        coordinatesText.text =
            String.format(
                Locale.US,
                "%.6f,%.6f",
                lat,
                lon
            )
    }

    private fun updateSelectedMarker(
        lat: Double,
        lon: Double,
        moveCamera: Boolean
    ) {

        val loadedMap =
            map ?: return

        val point =
            LatLng(
                lat,
                lon
            )

        val markerColor =
            if (
                State.isServiceRunning(
                    this
                )
            ) {
                AndroidColor.rgb(
                    224,
                    72,
                    82
                )
            } else {
                AndroidColor.rgb(
                    90,
                    160,
                    235
                )
            }

        @Suppress("DEPRECATION")
        if (
            selectedMarker == null
        ) {

            selectedMarker =
                loadedMap.addMarker(
                    MarkerOptions()
                        .position(point)
                        .icon(
                            markerIcon(
                                markerColor
                            )
                        )
                )

        } else {

            selectedMarker?.position =
                point

            selectedMarker?.icon =
                markerIcon(
                    markerColor
                )
        }

        if (moveCamera) {

            loadedMap.animateCamera(
                CameraUpdateFactory
                    .newLatLngZoom(
                        point,
                        loadedMap.cameraPosition.zoom
                    )
            )
        }
    }

    private fun updateSelectedMarkerAppearance() {

        val lat =
            State.getLat(this)

        val lon =
            State.getLon(this)

        if (
            lat == null ||
            lon == null
        ) {
            return
        }

        updateSelectedMarker(
            lat,
            lon,
            false
        )
    }

    private fun showStoredLocations(
        moveCamera: Boolean = true
    ) {

        val selectedLat =
            State.getLat(this)

        val selectedLon =
            State.getLon(this)

        if (
            selectedLat != null &&
            selectedLon != null
        ) {

            lastSelectedLat =
                selectedLat

            lastSelectedLon =
                selectedLon

            updateSelectedMarker(
                selectedLat,
                selectedLon,
                moveCamera
            )
        }

        refreshRealLocation()
    }

    private fun updateRealLocation(
        lat: Double,
        lon: Double
    ) {

        if (
            !State.validLat(lat) ||
            !State.validLon(lon)
        ) {
            return
        }

        State.setRealLocation(
            this,
            lat,
            lon
        )

        lastRealLat = lat
        lastRealLon = lon

        updateRealMarker(
            lat,
            lon
        )
    }

    private fun refreshRealLocation() {

        val lat =
            State.getRealLat(this)

        val lon =
            State.getRealLon(this)

        if (
            lat == null ||
            lon == null
        ) {
            return
        }

        updateRealMarker(
            lat,
            lon
        )
    }

    private fun updateRealMarker(
        lat: Double,
        lon: Double
    ) {

        val loadedMap =
            map ?: return

        val style =
            loadedMap.style
                ?: return

        if (
            realLocationSource == null
        ) {
            setupRealLocationLayer()
        }

        val source =
            realLocationSource
                ?: style.getSource(
                    "real-location-source"
                ) as? GeoJsonSource
                ?: return

        source.setGeoJson(
            Point.fromLngLat(
                lon,
                lat
            )
        )

        realLocationSource =
            source
    }

    @SuppressLint("MissingPermission")
    private fun captureRealLocation() {

        if (!hasLocationPermission()) {
            return
        }

        var best: Location? = null

        try {
            best =
                locationManager.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER
                )
        } catch (_: SecurityException) {
            return
        } catch (_: Exception) {
        }

        try {

            val network =
                locationManager.getLastKnownLocation(
                    LocationManager.NETWORK_PROVIDER
                )

            if (
                network != null &&
                (
                        best == null ||
                                network.time > best!!.time
                        )
            ) {
                best = network
            }

        } catch (_: SecurityException) {
            // Permission can change while the Activity is running.
        } catch (_: Exception) {
        }

        best?.let {
            updateRealLocation(
                it.latitude,
                it.longitude
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRealLocationUpdates() {

        if (
            State.isServiceRunning(this)
        ) {
            return
        }

        if (
            !hasLocationPermission()
        ) {
            return
        }

        try {

            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L,
                2f,
                locationListener,
                Looper.getMainLooper()
            )

        } catch (_: SecurityException) {
            // Permission may have been revoked after our check.
        } catch (_: Exception) {
        }
    }

    private fun stopRealLocationUpdates() {

        try {
            locationManager.removeUpdates(
                locationListener
            )
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
    }

    private fun requestPermissionsIfNeeded() {

        val permissions =
            mutableListOf(
                Manifest.permission
                    .ACCESS_FINE_LOCATION,

                Manifest.permission
                    .ACCESS_COARSE_LOCATION
            )

        if (
            android.os.Build.VERSION.SDK_INT >= 33
        ) {
            permissions.add(
                Manifest.permission
                    .POST_NOTIFICATIONS
            )
        }

        val missing =
            permissions.filter {
                ContextCompat.checkSelfPermission(
                    this,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            }

        if (
            missing.isNotEmpty()
        ) {
            permissionLauncher.launch(
                missing.toTypedArray()
            )
        }
    }

    private fun hasLocationPermission():
            Boolean {

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission
                .ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||

                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission
                        .ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isMockLocationAllowed():
            Boolean {

        val appOps =
            getSystemService(
                AppOpsManager::class.java
            )

        val mode =
            appOps.checkOpNoThrow(
                AppOpsManager
                    .OPSTR_MOCK_LOCATION,
                applicationInfo.uid,
                packageName
            )

        return mode ==
                AppOpsManager.MODE_ALLOWED
    }

    private fun showDeveloperOptions() {

        try {

            startActivity(
                Intent(
                    Settings
                        .ACTION_APPLICATION_DEVELOPMENT_SETTINGS
                )
            )

        } catch (_: Exception) {

            startActivity(
                Intent(
                    Settings.ACTION_SETTINGS
                )
            )
        }
    }

    private fun updateScaleBar() {

        val loadedMap =
            map ?: return

        val zoom =
            loadedMap
                .cameraPosition
                .zoom

        val target =
            loadedMap
                .cameraPosition
                .target
                ?: return

        val latitude =
            target.latitude

        val metersPerPixel =
            156543.03392 *
                    cos(
                        Math.toRadians(
                            latitude
                        )
                    ) /
                    2.0.pow(zoom)

        val maxWidthPx =
            scaleContainer.width *
                    0.85

        if (
            maxWidthPx <= 0.0
        ) {
            return
        }

        val maxDistance =
            metersPerPixel *
                    maxWidthPx

        val exponent =
            floor(
                log10(
                    maxDistance
                )
            )

        val base =
            10.0.pow(
                exponent
            )

        val normalized =
            maxDistance /
                    base

        val niceDistance =
            when {

                normalized >= 5.0 ->
                    5.0 * base

                normalized >= 2.0 ->
                    2.0 * base

                else ->
                    base
            }

        val barWidthPx =
            (
                    niceDistance /
                            metersPerPixel
                    )
                .roundToInt()

        scaleLine.layoutParams =
            scaleLine.layoutParams.apply {

                width =
                    barWidthPx
                        .coerceAtLeast(
                            24.dp
                        )
            }

        scaleLine.requestLayout()

        scaleLabel.text =
            when {

                niceDistance >= 1000.0 ->

                    if (
                        niceDistance %
                        1000.0 == 0.0
                    ) {

                        "${(
                                niceDistance /
                                        1000.0
                                ).roundToInt()} km"

                    } else {

                        String.format(
                            Locale.US,
                            "%.1f km",
                            niceDistance /
                                    1000.0
                        )
                    }

                else ->
                    "${niceDistance.roundToInt()} m"
            }
    }

    private fun markerIcon(
        color: Int
    ): org.maplibre.android.annotations.Icon {

        val bitmap =
            Bitmap.createBitmap(
                64,
                80,
                Bitmap.Config.ARGB_8888
            )

        val canvas =
            Canvas(bitmap)

        val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            )

        paint.color =
            color

        paint.setShadowLayer(
            5f,
            0f,
            3f,
            AndroidColor.argb(
                90,
                0,
                0,
                0
            )
        )

        val path =
            Path().apply {

                moveTo(
                    32f,
                    4f
                )

                cubicTo(
                    14f,
                    4f,
                    8f,
                    18f,
                    8f,
                    28f
                )

                cubicTo(
                    8f,
                    43f,
                    20f,
                    51f,
                    32f,
                    72f
                )

                cubicTo(
                    44f,
                    51f,
                    56f,
                    43f,
                    56f,
                    28f
                )

                cubicTo(
                    56f,
                    18f,
                    50f,
                    4f,
                    32f,
                    4f
                )

                close()
            }

        canvas.drawPath(
            path,
            paint
        )

        paint.clearShadowLayer()

        paint.color =
            AndroidColor.WHITE

        canvas.drawCircle(
            32f,
            28f,
            8f,
            paint
        )

        return IconFactory
            .getInstance(this)
            .fromBitmap(bitmap)
    }

    private fun animateButtonPress() {

        toggleButton.animate()
            .scaleX(0.97f)
            .scaleY(0.97f)
            .setDuration(70L)
            .setInterpolator(
                AccelerateDecelerateInterpolator()
            )
            .withEndAction {

                toggleButton.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(130L)
                    .start()
            }
            .start()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()

        mapView.onResume()

        handler.post(
            uiRunnable
        )

        captureRealLocation()
        startRealLocationUpdates()
    }

    override fun onPause() {

        saveCameraPosition()

        stopRealLocationUpdates()

        handler.removeCallbacks(
            uiRunnable
        )

        mapView.onPause()

        super.onPause()
    }

    override fun onStop() {

        mapView.onStop()

        super.onStop()
    }

    override fun onSaveInstanceState(
        outState: Bundle
    ) {

        mapView.onSaveInstanceState(
            outState
        )

        super.onSaveInstanceState(
            outState
        )
    }

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(
            null
        )

        stopRealLocationUpdates()

        mapView.onDestroy()

        super.onDestroy()
    }

    override fun onLowMemory() {

        super.onLowMemory()

        mapView.onLowMemory()
    }

    private val Int.dp: Int
        get() =
            (
                    this *
                            resources
                                .displayMetrics
                                .density
                    )
                .roundToInt()
}