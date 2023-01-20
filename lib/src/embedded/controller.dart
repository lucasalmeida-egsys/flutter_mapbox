import 'dart:async';
import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import '../models/models.dart';

/// Controller for a single MapBox Navigation instance running on the host platform.
class MapBoxNavigationViewController {
  late MethodChannel _methodChannel;
  late EventChannel _eventChannel;

  ValueSetter<RouteEvent>? _routeEventNotifier;

  MapBoxNavigationViewController(
    int id,
    ValueSetter<RouteEvent>? eventNotifier,
  ) {
    _methodChannel = MethodChannel('flutter_mapbox/$id');
    _methodChannel.setMethodCallHandler(_handleMethod);

    _eventChannel = EventChannel('flutter_mapbox/$id/events');
    _routeEventNotifier = eventNotifier;
  }

  Stream<RouteEvent>? _onRouteEvent;
  late StreamSubscription<RouteEvent> _routeEventSubscription;

  ///Current Device OS Version
  Future<String> get platformVersion => _methodChannel
      .invokeMethod('getPlatformVersion')
      .then<String>((dynamic result) => result);

  ///Total distance remaining in meters along route.
  Future<double?> get distanceRemaining => _methodChannel
      .invokeMethod<double?>('getDistanceRemaining')
      .then<double?>((dynamic result) => result);

  ///Total seconds remaining on all legs.
  Future<double?> get durationRemaining => _methodChannel
      .invokeMethod<double?>('getDurationRemaining')
      .then<double?>((dynamic result) => result);

  Future<MapNavigationMode> get currentNavigationMode => _methodChannel
      .invokeMethod<String>('getCurrentNavigationMode')
      .then<MapNavigationMode>(
          (dynamic result) => MapNavigationMode.values.firstWhere((element) {
                return element.name == result;
              }, orElse: () => MapNavigationMode.drivingWithTraffic));

  ///Get last selected annotation
  Future<String> get selectedAnnotation => _methodChannel
      .invokeMethod<String>('getSelectedAnnotation')
      .then<String>((dynamic result) => result);

  ///Set camera to desired lat / lng coordinates
  Future<bool> updateCameraPosition({
    required double latitude,
    required double longitude,
  }) async {
    Map<String, dynamic> args = <String, dynamic>{};
    args["latitude"] = latitude;
    args["longitude"] = longitude;

    return await _methodChannel
        .invokeMethod('updateCamera', args)
        .then<bool>((dynamic result) => result);
  }

  ///Build the Route Used for the Navigation
  ///
  /// [wayPoints] must not be null. A collection of [WayPoint](longitude, latitude and name). Must be at least 2 or at most 25. Cannot use drivingWithTraffic mode if more than 3-waypoints.
  /// [options] options used to generate the route and used while navigating
  ///
  Future<bool> buildRoute({
    required WayPoint destination,
  }) async {
    _routeEventSubscription = _streamRouteEvent!.listen(_onProgressData);

    return await _methodChannel
        .invokeMethod('buildRoute', destination.toMap())
        .then<bool>(
          (dynamic result) => result,
        );
  }

  /// starts listening for events
  Future<void> initialize() async {
    _routeEventSubscription = _streamRouteEvent!.listen(_onProgressData);
  }

  /// Clear the built route and resets the map
  Future<bool?> clearRoute() async {
    return _methodChannel.invokeMethod('clearRoute', null);
  }

  /// Starts the Navigation
  Future<bool?> startNavigation({MapBoxOptions? options}) async {
    Map<String, dynamic>? args;
    if (options != null) args = options.toMap();
    //_routeEventSubscription = _streamRouteEvent.listen(_onProgressData);
    return _methodChannel.invokeMethod('startNavigation', args);
  }

  /// Ends Navigation and Closes the Navigation View
  Future<bool?> finishNavigation() async {
    var success = await _methodChannel.invokeMethod('finishNavigation', null);
    return success;
  }

  /// Recenter map from user action during navigation
  Future<void> reCenterCamera() async {
    var success = await _methodChannel.invokeMethod('reCenter', null);
    return success;
  }

  /// Generic Handler for Messages sent from the Platform
  Future<dynamic> _handleMethod(MethodCall call) async {
    switch (call.method) {
      case 'sendFromNative':
        String? text = call.arguments as String?;
        return Future.value("Text from native: $text");
    }
  }

  void _onProgressData(RouteEvent event) {
    if (_routeEventNotifier != null) _routeEventNotifier!(event);

    if (event.eventType == MapBoxEvent.onArrival) {
      _routeEventSubscription.cancel();
    }
  }

  Stream<RouteEvent>? get _streamRouteEvent {
    _onRouteEvent ??= _eventChannel
        .receiveBroadcastStream()
        .map((dynamic event) => _parseRouteEvent(event));
    return _onRouteEvent;
  }

  RouteEvent _parseRouteEvent(String jsonString) {
    RouteEvent event;
    var map = json.decode(jsonString);
    var progressEvent = RouteProgressEvent.fromJson(map);
    if (progressEvent.isProgressEvent!) {
      event = RouteEvent(
        eventType: MapBoxEvent.progressChange,
        data: progressEvent,
      );
    } else {
      event = RouteEvent.fromJson(map);
    }

    return event;
  }
}
