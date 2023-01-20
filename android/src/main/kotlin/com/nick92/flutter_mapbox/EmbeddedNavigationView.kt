package com.nick92.flutter_mapbox

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.maps.*
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.base.TimeFormat
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.route.*
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.formatter.MapboxDistanceFormatter
import com.mapbox.navigation.core.routealternatives.NavigationRouteAlternativesObserver
import com.mapbox.navigation.core.routealternatives.RouteAlternativesError
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.view.MapboxManeuverView
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationBasicGesturesHandler
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraTransitionOptions
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowApi
import com.mapbox.navigation.ui.maps.route.arrow.api.MapboxRouteArrowView
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowOptions
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import com.mapbox.navigation.ui.tripprogress.model.*
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView
import com.mapbox.navigation.ui.utils.internal.extensions.getBitmap
import com.mapbox.navigation.ui.voice.api.MapboxSpeechApi
import com.mapbox.navigation.ui.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.ui.voice.model.SpeechAnnouncement
import com.mapbox.navigation.ui.voice.model.SpeechError
import com.mapbox.navigation.ui.voice.model.SpeechValue
import com.mapbox.navigation.ui.voice.model.SpeechVolume
import com.nick92.flutter_mapbox.databinding.MapActivityBinding
import com.nick92.flutter_mapbox.models.MapBoxEvents
import com.nick92.flutter_mapbox.models.MapBoxLocation
import com.nick92.flutter_mapbox.models.MapBoxRouteProgressEvent
import com.nick92.flutter_mapbox.models.MapBoxSettings
import com.nick92.flutter_mapbox.utilities.PluginUtilities
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.util.*


open class EmbeddedNavigationView(
    ctx: Context,
    act: Activity,
    bind: MapActivityBinding,
    accessToken: String
) : MethodChannel.MethodCallHandler, EventChannel.StreamHandler,
    Application.ActivityLifecycleCallbacks {

    open fun initFlutterChannelHandlers() {
        methodChannel?.setMethodCallHandler(this)
        eventChannel?.setStreamHandler(this)
    }

    open fun initNavigation(mv: MapView, arguments: Map<String, *>) {
        mapView = mv
        mapboxMap = mapView.getMapboxMap()

        if (arguments != null)
            settings = MapBoxSettings.from(arguments)

        // initialize the location puck
        mapView.location.apply {
            this.locationPuck = LocationPuck2D(
                bearingImage = ContextCompat.getDrawable(
                    context,
                    R.drawable.mapbox_navigation_puck_icon
                ),
            )
            setLocationProvider(navigationLocationProvider)
            enabled = true
        }

        // initialize Mapbox Navigation
        mapboxNavigation = if (MapboxNavigationProvider.isCreated()) {
            MapboxNavigationProvider.retrieve()
        } else {
            MapboxNavigationProvider.create(
                NavigationOptions.Builder(context)
                    .accessToken(token)
                    .build()
            )
        }

        // initialize Navigation Camera
        viewportDataSource = MapboxNavigationViewportDataSource(mapboxMap)
        navigationCamera = NavigationCamera(
            mapboxMap,
            mapView.camera,
            viewportDataSource,
        )

        // set the animations lifecycle listener to ensure the NavigationCamera stops
        // automatically following the user location when the map is interacted with
        mapView.camera.addCameraAnimationsLifecycleListener(
            NavigationBasicGesturesHandler(navigationCamera)
        )

        navigationCamera.registerNavigationCameraStateChangeObserver { navigationCameraState ->
            // shows/hide the recenter button depending on the camera state
            when (navigationCameraState) {
                NavigationCameraState.TRANSITION_TO_FOLLOWING,
                NavigationCameraState.FOLLOWING -> binding.recenter.visibility = View.INVISIBLE
                NavigationCameraState.TRANSITION_TO_OVERVIEW,
                NavigationCameraState.OVERVIEW,
                NavigationCameraState.IDLE -> binding.recenter.visibility = View.INVISIBLE
            }
        }
        // set the padding values depending on screen orientation and visible view layout
        if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.overviewPadding = landscapeOverviewPadding
        } else {
            viewportDataSource.overviewPadding = overviewPadding
        }
        if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewportDataSource.followingPadding = landscapeFollowingPadding
        } else {
            viewportDataSource.followingPadding = followingPadding
        }

        // make sure to use the same DistanceFormatterOptions across different features
        val distanceFormatterOptions = mapboxNavigation.navigationOptions.distanceFormatterOptions

        // initialize maneuver api that feeds the data to the top banner maneuver view
        maneuverApi = MapboxManeuverApi(
            MapboxDistanceFormatter(distanceFormatterOptions)
        )

        // initialize bottom progress view
        tripProgressApi = MapboxTripProgressApi(
            TripProgressUpdateFormatter.Builder(activity)
                .distanceRemainingFormatter(
                    DistanceRemainingFormatter(distanceFormatterOptions)
                )
                .timeRemainingFormatter(
                    TimeRemainingFormatter(activity)
                )
                .percentRouteTraveledFormatter(
                    PercentDistanceTraveledFormatter()
                )
                .estimatedTimeToArrivalFormatter(
                    EstimatedTimeToArrivalFormatter(activity, TimeFormat.NONE_SPECIFIED)
                )
                .build()
        )

        // initialize voice instructions api and the voice instruction player
        speechApi = MapboxSpeechApi(
            activity,
            token,
            settings.navigationLanguage
        )
        voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(
            activity,
            token,
            settings.navigationLanguage
        )

        // initialize route line, the withRouteLineBelowLayerId is specified to place
        // the route line below road labels layer on the map
        // the value of this option will depend on the style that you are using
        // and under which layer the route line should be placed on the map layers stack
        val mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(activity)
            .withRouteLineBelowLayerId("road-label-navigation")
            .withVanishingRouteLineEnabled(true)
            .build()

        routeLineApi = MapboxRouteLineApi(mapboxRouteLineOptions)
        routeLineView = MapboxRouteLineView(mapboxRouteLineOptions)

        // initialize maneuver arrow view to draw arrows on the map
        val routeArrowOptions = RouteArrowOptions.Builder(activity).build()
        routeArrowView = MapboxRouteArrowView(routeArrowOptions)

        var styleUrl = settings.mapStyleUrl
        if (styleUrl == null) styleUrl = Style.MAPBOX_STREETS
        // load map style if set if not default
        mapboxMap.loadStyleUri(
            styleUrl
        ) {
            if (settings.longPressDestinationEnabled) {
                // add long click listener that search for a route to the clicked destination
                binding.mapView.gestures.addOnMapLongClickListener { point ->
                    // TODO: implementar multiroute
                    true
                }
            }
        }

        // initialize view interactions
        binding.stop.setOnClickListener {
            clearRouteAndStopNavigation()
        }

        binding.recenter.setOnClickListener {
            navigationCamera.requestNavigationCameraToFollowing()
            binding.routeOverview.showTextAndExtend(Companion.ANIMATION_DURATION)
        }
        binding.routeOverview.setOnClickListener {
            navigationCamera.requestNavigationCameraToOverview()
            binding.recenter.showTextAndExtend(Companion.ANIMATION_DURATION)
        }
        binding.soundButton.setOnClickListener {
            isVoiceInstructionsMuted = !isVoiceInstructionsMuted
        }

        // set initial sounds button state
        binding.soundButton.mute()

        mapView.gestures.addOnMapClickListener(mapClickListener)

        val annotationApi = mapView?.annotations
        pointAnnotationManager = annotationApi.createPointAnnotationManager()
        pointAnnotationManager.addClickListener(onPointAnnotationClickListener)

        // initialize navigation trip observers
        registerObservers()
        mapboxNavigation.startTripSession(withForegroundService = true)
    }

    override fun onMethodCall(methodCall: MethodCall, result: MethodChannel.Result) {
        when (methodCall.method) {
            "getPlatformVersion" -> {
                result.success("Android ${Build.VERSION.RELEASE}")
            }
            "enableOfflineRouting" -> {
                downloadRegionForOfflineRouting(methodCall, result)
            }
            "addMarker" -> {
                addMarker(methodCall, result)
            }
            "removeMarker" -> {
                addMarker(methodCall, result)
            }
            "buildRoute" -> {
                buildRoute(methodCall, result)
            }
            "updateCamera" -> {
                updateCamera(methodCall, result)
            }
            "clearRoute" -> {
                clearRoute(methodCall, result)
            }
            "startNavigation" -> {
                startNavigation(methodCall, result)
            }
            "finishNavigation" -> {
                finishNavigation(methodCall, result)
            }
            "getDistanceRemaining" -> {
                result.success(distanceRemaining)
            }
            "getCurrentNavigationMode" -> {
                result.success(settings.navigationMode)
            }
            "getDurationRemaining" -> {
                result.success(durationRemaining)
            }
            "getSelectedAnnotation" -> {
                result.success(selectedAnnotation)
            }
            "reCenter" -> {
                navigationCamera.requestNavigationCameraToOverview(
                    stateTransitionOptions = NavigationCameraTransitionOptions.Builder()
                        .maxDuration(1000) // instant transition
                        .build()
                )
            }
            else -> result.notImplemented()
        }
    }

    private fun downloadRegionForOfflineRouting(
        @NonNull call: MethodCall,
        @NonNull result: MethodChannel.Result
    ) {
        result.error("TODO", "Not Implemented in Android", "will implement soon")
    }

    private fun buildRoute(methodCall: MethodCall, result: MethodChannel.Result) {
        isNavigationCanceled = false
        isNavigationInProgress = false

        val arguments = methodCall.arguments as Map<String, *>;
        val destination = MapBoxLocation.from(arguments);

        generateRoute(context, destination)
        result.success(true)
    }

    private fun generateRoute(context: Context, destination :MapBoxLocation) {
        if (!PluginUtilities.isNetworkAvailable(context))
            return PluginUtilities.sendEvent(MapBoxEvents.ROUTE_BUILD_FAILED, "No Internet Connection")

        val originLocation = navigationLocationProvider.lastLocation
        val originPoint = originLocation?.let {
            Point.fromLngLat(it.longitude, it.latitude)
        } ?: return

        PluginUtilities.sendEvent(MapBoxEvents.ROUTE_BUILDING)
        mapboxNavigation.stopTripSession()

        mapboxNavigation.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .applyLanguageAndVoiceUnitOptions(context)
                .coordinatesList(listOf(originPoint, Point.fromLngLat(destination.longitude, destination.latitude)))
                .bearingsList(
                    listOf(
                        Bearing.builder()
                            .angle(originLocation.bearing.toDouble())
                            .degrees(45.0)
                            .build(),
                        null
                    )
                )
                .layersList(listOf(mapboxNavigation.getZLevel(), null))
                .excludeList(settings.excludeList)
                .language(settings.navigationLanguage)
                .alternatives(settings.alternatives)
                .profile(settings.navigationMode)
                .maxHeight(settings.maxHeight)
                .maxWidth(settings.maxWidth)
                .maxWeight(settings.maxWeight)
                .continueStraight(!settings.allowsUTurnAtWayPoints)
                .voiceUnits(settings.navigationVoiceUnits)
                .annotations(DirectionsCriteria.ANNOTATION_DISTANCE)
                .enableRefresh(true)
                .build(), object : NavigationRouterCallback {
                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    if (routes.isEmpty())
                        PluginUtilities.sendEvent(MapBoxEvents.ROUTE_BUILD_NO_ROUTES_FOUND)
                    else
                        setRoute(routes)
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                    var message = "an error occurred while building the route. Errors: "
                    for (reason in reasons) {
                        message += reason.message
                    }
                    PluginUtilities.sendEvent(MapBoxEvents.ROUTE_BUILD_FAILED, message)
                    isBuildingRoute = false
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    PluginUtilities.sendEvent(MapBoxEvents.ROUTE_BUILD_CANCELLED)
                }
            })
    }

    private fun addMarker(methodCall: MethodCall, result: MethodChannel.Result) {
        val iconImage = ContextCompat.getDrawable(
            context,
            R.drawable.mapbox_marker_icon_default,
        )?.getBitmap()

        val point = methodCall.arguments as Map<String, *>

        var name = point["name"] as String
        val latitude = point["latitude"] as Double
        val longitude = point["longitude"] as Double

        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(Point.fromLngLat(longitude, latitude))
            .withIconImage(iconImage!!)

        pointAnnotationOptions.iconSize = 0.4
        pointAnnotationOptions.textOffset = listOf(0.0, 2.5)
        pointAnnotationOptions.textField = name
        pointAnnotationOptions.textSize = 12.0

        pointAnnotationManager.create(pointAnnotationOptions)

        result.success(true)
    }

    private fun removeMarker(methodCall: MethodCall, result: MethodChannel.Result) {
        result.success(true)
    }

    private fun setRoute(routes: List<NavigationRoute>) {
        currentRoute = routes.first();

        durationRemaining = currentRoute!!.directionsRoute.duration()
        distanceRemaining = currentRoute!!.directionsRoute.distance()

        PluginUtilities.sendEvent(MapBoxEvents.ROUTE_BUILT)

        // Draw the route on the map
        mapboxNavigation.setNavigationRoutes(routes)
        navigationCamera.requestNavigationCameraToOverview()

        isBuildingRoute = false
    }

    private fun moveCameraToOriginOfRoute() {
        currentRoute?.let {
            val originCoordinate = it.routeOptions?.coordinatesList()?.get(0)
            originCoordinate?.let {
                val location = LatLng(originCoordinate.latitude(), originCoordinate.longitude())
                updateCamera(location)
            }
        }
    }

    private fun clearRoute(methodCall: MethodCall, result: MethodChannel.Result) {
        mapboxNavigation.setNavigationRoutes(listOf())
        PluginUtilities.sendEvent(MapBoxEvents.NAVIGATION_CANCELLED)
    }

    private fun clearRouteAndStopNavigation() {
        mapboxNavigation.setNavigationRoutes(listOf())
        mapboxNavigation.stopTripSession()
        PluginUtilities.sendEvent(MapBoxEvents.NAVIGATION_CANCELLED)
    }

    private fun startNavigation(methodCall: MethodCall, result: MethodChannel.Result) {
        val arguments = methodCall.arguments as? Map<String, *>

        if (arguments != null)
            settings = MapBoxSettings.from(arguments)

        if (currentRoute != null) {
            startNavigation()
            result.success(true)
        } else {
            result.success(false)
        }
    }

    private fun finishNavigation(methodCall: MethodCall, result: MethodChannel.Result) {
        if (currentRoute != null) {
            finishNavigation()
            result.success(true)
        } else {
            result.success(false)
        }
    }

    private fun startNavigation() {
        isNavigationCanceled = false
        mapboxNavigation.startTripSession()

        // move the camera to overview when new route is available
        navigationCamera.requestNavigationCameraToOverview()
        isNavigationInProgress = true

        PluginUtilities.sendEvent(MapBoxEvents.NAVIGATION_RUNNING)
    }

    private fun finishNavigation(isOffRouted: Boolean = false) {
        // clear
        mapboxNavigation.setNavigationRoutes(listOf())
        isNavigationCanceled = true

        if (!isOffRouted) {
            isNavigationInProgress = false
            moveCameraToOriginOfRoute()
        }

        mapboxNavigation.stopTripSession()

        PluginUtilities.sendEvent(MapBoxEvents.NAVIGATION_FINISHED);
    }

    private fun updateCamera(methodCall: MethodCall, result: MethodChannel.Result) {
        var arguments = methodCall.arguments as? Map<*, *>

        if (arguments != null) {
            var latitude = arguments["latitude"] as Double;
            var longitude = arguments["longitude"] as Double;

            updateCamera(LatLng(latitude, longitude));
        }
    }

    private fun updateCamera(location: LatLng) {
        val mapAnimationOptions = MapAnimationOptions.Builder().duration(Companion.ANIMATION_DURATION).build()
        mapView.camera.easeTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(location.longitude, location.latitude))
                .zoom(settings.zoom)
                .bearing(settings.bearing * Companion.BEARING_AMPLIFIER)
                .pitch(settings.tilt * Companion.PITCH_AMPLIFIER)
                .build(),
            mapAnimationOptions
        )
    }

    open fun registerObservers() {
        // register event listeners
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        mapboxNavigation.registerRouteAlternativesObserver(alternativesObserver)
    }

    open fun unregisterObservers() {
        // unregister event listeners to prevent leaks or unnecessary resource consumption
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
        mapboxNavigation.unregisterRouteAlternativesObserver(alternativesObserver)
    }

    fun onDestroy() {
        MapboxNavigationProvider.destroy()
        maneuverApi.cancel()
        routeLineApi.cancel()
        routeLineView.cancel()
        speechApi.cancel()
        voiceInstructionsPlayer.shutdown()
    }

    //Flutter stream listener delegate methods
    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events;
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null;
    }

    companion object {
        var eventSink: EventChannel.EventSink? = null

        const val PITCH_AMPLIFIER = -90
        const val BEARING_AMPLIFIER = 90
        const val ANIMATION_DURATION = 1500L
    }

    private val context: Context = ctx
    private val activity: Activity = act

    open var methodChannel: MethodChannel? = null
    open var eventChannel: EventChannel? = null

    private val token: String = accessToken
    private var currentRoute: NavigationRoute? = null

    lateinit var settings : MapBoxSettings

    private var distanceRemaining: Double? = null
    private var durationRemaining: Double? = null

    private var isBuildingRoute = false
    private var isNavigationInProgress = false
    private var isNavigationCanceled = false

    private val routeClickPadding = 30 * Resources.getSystem().displayMetrics.density


    /**
     * Bindings to the example layout.
     */
    open val binding: MapActivityBinding = bind

    /**
     * MapView entry point obtained from the embedded view.
     * You need to get a new reference to this object whenever the [MapView] is recreated.
     */
    private lateinit var mapView: MapView

    /**
     * Mapbox Maps entry point obtained from the [MapView].
     * You need to get a new reference to this object whenever the [MapView] is recreated.
     */
    private lateinit var mapboxMap: MapboxMap

    /**
     * Mapbox Navigation entry point. There should only be one instance of this object for the app.
     * You can use [MapboxNavigationProvider] to help create and obtain that instance.
     */
    private lateinit var mapboxNavigation: MapboxNavigation

    /**
     * Used to execute camera transitions based on the data generated by the [viewportDataSource].
     * This includes transitions from route overview to route following and continuously updating the camera as the location changes.
     */
    private lateinit var navigationCamera: NavigationCamera

    /**
     * Produces the camera frames based on the location and routing data for the [navigationCamera] to execute.
     */
    private lateinit var viewportDataSource: MapboxNavigationViewportDataSource

    /*
     * Below are generated camera padding values to ensure that the route fits well on screen while
     * other elements are overlaid on top of the map (including instruction view, buttons, etc.)
     */
    private val pixelDensity = Resources.getSystem().displayMetrics.density
    private val overviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            140.0 * pixelDensity,
            40.0 * pixelDensity,
            120.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val landscapeOverviewPadding: EdgeInsets by lazy {
        EdgeInsets(
            130.0 * pixelDensity,
            380.0 * pixelDensity,
            110.0 * pixelDensity,
            20.0 * pixelDensity
        )
    }
    private val followingPadding: EdgeInsets by lazy {
        EdgeInsets(
            180.0 * pixelDensity,
            40.0 * pixelDensity,
            150.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }
    private val landscapeFollowingPadding: EdgeInsets by lazy {
        EdgeInsets(
            30.0 * pixelDensity,
            380.0 * pixelDensity,
            110.0 * pixelDensity,
            40.0 * pixelDensity
        )
    }

    private lateinit var selectedAnnotation: String
    private lateinit var pointAnnotationManager: PointAnnotationManager

    /**
     * Generates updates for the [MapboxManeuverView] to display the upcoming maneuver instructions
     * and remaining distance to the maneuver point.
     */
    private lateinit var maneuverApi: MapboxManeuverApi

    /**
     * Generates updates for the [MapboxTripProgressView] that include remaining time and distance to the destination.
     */
    private lateinit var tripProgressApi: MapboxTripProgressApi

    /**
     * Generates updates for the [routeLineView] with the geometries and properties of the routes that should be drawn on the map.
     */
    private lateinit var routeLineApi: MapboxRouteLineApi

    /**
     * Draws route lines on the map based on the data from the [routeLineApi]
     */
    private lateinit var routeLineView: MapboxRouteLineView

    /**
     * Generates updates for the [routeArrowView] with the geometries and properties of maneuver arrows that should be drawn on the map.
     */
    private val routeArrowApi: MapboxRouteArrowApi = MapboxRouteArrowApi()

    /**
     * Draws maneuver arrows on the map based on the data [routeArrowApi].
     */
    private lateinit var routeArrowView: MapboxRouteArrowView

    /**
     * Stores and updates the state of whether the voice instructions should be played as they come or muted.
     */
    private var isVoiceInstructionsMuted = false
        set(value) {
            field = value
            if (value) {
                binding.soundButton.muteAndExtend(Companion.ANIMATION_DURATION)
                voiceInstructionsPlayer.volume(SpeechVolume(0f))
            } else {
                binding.soundButton.unmuteAndExtend(Companion.ANIMATION_DURATION)
                voiceInstructionsPlayer.volume(SpeechVolume(1f))
            }
        }

    /**
     * Extracts message that should be communicated to the driver about the upcoming maneuver.
     * When possible, downloads a synthesized audio file that can be played back to the driver.
     */
    private lateinit var speechApi: MapboxSpeechApi

    /**
     * Plays the synthesized audio files with upcoming maneuver instructions
     * or uses an on-device Text-To-Speech engine to communicate the message to the driver.
     */
    private lateinit var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer

    /**
     * Observes when a new voice instruction should be played.
     */
    private val voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
        speechApi.generate(voiceInstructions, speechCallback)
    }

    /**
     * Based on whether the synthesized audio file is available, the callback plays the file
     * or uses the fall back which is played back using the on-device Text-To-Speech engine.
     */
    private val speechCallback =
        MapboxNavigationConsumer<Expected<SpeechError, SpeechValue>> { expected ->
            expected.fold(
                { error ->
                    // play the instruction via fallback text-to-speech engine
                    voiceInstructionsPlayer.play(
                        error.fallback,
                        voiceInstructionsPlayerCallback
                    )
                },
                { value ->
                    // play the sound file from the external generator
                    voiceInstructionsPlayer.play(
                        value.announcement,
                        voiceInstructionsPlayerCallback
                    )
                }
            )
        }

    /**
     * When a synthesized audio file was downloaded, this callback cleans up the disk after it was played.
     */
    private val voiceInstructionsPlayerCallback =
        MapboxNavigationConsumer<SpeechAnnouncement> { value ->
            // remove already consumed file to free-up space
            speechApi.clean(value)
        }

    /**
     * [NavigationLocationProvider] is a utility class that helps to provide location updates generated by the Navigation SDK
     * to the Maps SDK in order to update the user location indicator on the map.
     */
    private val navigationLocationProvider = NavigationLocationProvider()

    /**
     * Gets notified with location updates.
     *
     * Exposes raw updates coming directly from the location services
     * and the updates enhanced by the Navigation SDK (cleaned up and matched to the road).
     */
    private val locationObserver = object : LocationObserver {
        var firstLocationUpdateReceived = false

        override fun onNewRawLocation(rawLocation: Location) {
            // not handled
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val enhancedLocation = locationMatcherResult.enhancedLocation
            // update location puck's position on the map
            navigationLocationProvider.changePosition(
                location = enhancedLocation,
                keyPoints = locationMatcherResult.keyPoints,
            )

            // update camera position to account for new location
            viewportDataSource.onLocationChanged(enhancedLocation)
            viewportDataSource.evaluate()

            // if this is the first location update the activity has received,
            // it's best to immediately move the camera to the current user location
            if (!firstLocationUpdateReceived) {
                firstLocationUpdateReceived = true
                val location = LatLng(enhancedLocation.latitude, enhancedLocation.longitude)
                updateCamera(location)
            }
        }
    }

    /**
     * Gets notified with progress along the currently active route.
     */
    private val routeProgressObserver = RouteProgressObserver { routeProgress ->
        // update the camera position to account for the progressed fragment of the route
        viewportDataSource.onRouteProgressChanged(routeProgress)
        viewportDataSource.evaluate()

        // draw the upcoming maneuver arrow on the map
        val style = mapboxMap.getStyle()
        if (style != null) {
            val maneuverArrowResult = routeArrowApi.addUpcomingManeuverArrow(routeProgress)
            routeArrowView.renderManeuverUpdate(style, maneuverArrowResult)
        }

        // update bottom trip progress summary
        binding.tripProgressView.render(
            tripProgressApi.getTripProgress(routeProgress)
        )

        //update flutter events
        if (!isNavigationCanceled) {
            try {
                distanceRemaining = routeProgress.distanceRemaining.toDouble()
                durationRemaining = routeProgress.durationRemaining

                val progressEvent = MapBoxRouteProgressEvent(routeProgress)
                PluginUtilities.sendEvent(progressEvent)

            } catch (e: Exception) {

            }
        }

    }

    /**
     * Gets notified whenever the tracked routes change.
     *
     * A change can mean:
     * - routes get changed with [MapboxNavigation.setRoutes]
     * - routes annotations get refreshed (for example, congestion annotation that indicate the live traffic along the route)
     * - driver got off route and a reroute was executed
     */
    private val routesObserver = RoutesObserver { routeUpdateResult ->
        if (routeUpdateResult.navigationRoutes.isNotEmpty()) {
            // generate route geometries asynchronously and render them
            val routeLines = routeUpdateResult.routes.map { RouteLine(it, null) }

            routeLineApi.setRoutes(
                routeLines
            ) { value ->
                mapboxMap.getStyle()?.apply {
                    routeLineView.renderRouteDrawData(this, value)
                }
            }

            // update the camera position to account for the new route
            viewportDataSource.onRouteChanged(routeUpdateResult.routes.first())
            viewportDataSource.evaluate()

            //send a reroute event
            PluginUtilities.sendEvent(MapBoxEvents.REROUTE_ALONG, routeUpdateResult.reason)

        } else {
            // remove the route line and route arrow from the map
            val style = mapboxMap.getStyle()
            if (style != null) {
                routeLineApi.clearRouteLine { value ->
                    routeLineView.renderClearRouteLineValue(
                        style,
                        value
                    )
                }
                routeArrowView.render(style, routeArrowApi.clearArrows())
            }

            // remove the route reference from camera position evaluations
            viewportDataSource.clearRouteData()
            viewportDataSource.evaluate()
        }
    }

    /**
     * The SDK triggers [NavigationRouteAlternativesObserver] when available alternatives change.
     */
    private val alternativesObserver = object : NavigationRouteAlternativesObserver {
        override fun onRouteAlternatives(
            routeProgress: RouteProgress,
            alternatives: List<NavigationRoute>,
            routerOrigin: RouterOrigin
        ) {
            // Set the suggested alternatives
            val updatedRoutes = mutableListOf<NavigationRoute>()
            updatedRoutes.add(routeProgress.navigationRoute) // only primary route should persist
            updatedRoutes.addAll(alternatives) // all old alternatives should be replaced by the new ones
            mapboxNavigation.setNavigationRoutes(updatedRoutes)
        }

        override fun onRouteAlternativesError(error: RouteAlternativesError) {
            // no impl
        }
    }

    /**
     * Click on any point of the alternative route on the map to make it primary.
     */
    private val mapClickListener = OnMapClickListener { point ->
        routeLineApi.findClosestRoute(
            point,
            mapboxMap,
            routeClickPadding
        ) {
            val routeFound = it.value?.navigationRoute
            // if we clicked on some route that is not primary,
            // we make this route primary and all the others - alternative
            if (routeFound != null && routeFound != routeLineApi.getPrimaryNavigationRoute()) {
                val reOrderedRoutes = routeLineApi.getNavigationRoutes()
                    .filter { navigationRoute -> navigationRoute != routeFound }
                    .toMutableList()
                    .also { list ->
                        list.add(0, routeFound)
                    }
                currentRoute = routeFound
                durationRemaining = currentRoute!!.directionsRoute.duration()
                distanceRemaining = currentRoute!!.directionsRoute.distance()

                mapboxNavigation.setNavigationRoutes(reOrderedRoutes)
                PluginUtilities.sendEvent(MapBoxEvents.ROUTE_BUILT)
            }
        }
        false
    }


    private val onPointAnnotationClickListener = OnPointAnnotationClickListener { annotation ->
        selectedAnnotation = annotation.textField!!
        PluginUtilities.sendEvent(MapBoxEvents.ANNOTATION_TAPPED)
        false
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        TODO("Not yet implemented")
    }

    override fun onActivityStarted(activity: Activity) {
        TODO("Not yet implemented")
    }

    override fun onActivityResumed(activity: Activity) {
        TODO("Not yet implemented")
    }

    override fun onActivityPaused(activity: Activity) {
        TODO("Not yet implemented")
    }

    override fun onActivityStopped(activity: Activity) {
        TODO("Not yet implemented")
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        TODO("Not yet implemented")
    }

    override fun onActivityDestroyed(activity: Activity) {
        TODO("Not yet implemented")
    }
}

