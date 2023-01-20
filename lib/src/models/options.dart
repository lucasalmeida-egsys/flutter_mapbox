import 'package:flutter_mapbox/flutter_mapbox.dart';

/// Configuration options for the MapBoxNavigation.
///
/// When used to change configuration, null values will be interpreted as
/// "do not change this configuration option".
///
class MapBoxOptions {
  /// 2-letter ISO 639-1 code for language. This property affects the sentence contained within the RouteStep.instructions property, but it does not affect any road names contained in that property or other properties such as RouteStep.name. Defaults to "en" if an unsupported language is specified. The languages in this link are supported: https://docs.mapbox.com/android/navigation/overview/localization/ or https://docs.mapbox.com/ios/api/navigation/0.14.1/localization-and-internationalization.html
  final String? language;

  /// Zoom controls the scale of the map and consumes any value between 0 and 22. At zoom level 0, the viewport shows continents and other world features. A middle value of 11 will show city level details, and at a higher zoom level, the map will begin to show buildings and points of interest.
  final double? zoom;

  /// Bearing is the direction that the camera is pointing in and measured in degrees clockwise from north.
  ///
  /// The camera's default bearing is 0 degrees (i.e. "true north") causing the map compass to hide until the camera bearing becomes a non-zero value. The mapbox_uiCompass boolean XML attribute allows adjustment of the compass' visibility. Bearing levels use six decimal point precision, which enables you to restrict/set/lock a map's bearing with extreme precision. Besides programmatically adjusting the camera bearing, the user can place two fingertips on the map and rotate their fingers.
  final double? bearing;

  /// Tilt is the camera's angle from the nadir (directly facing the Earth) and uses unit degrees. The camera's minimum (default) tilt is 0 degrees, and the maximum tilt is 60. Tilt levels use six decimal point of precision, which enables you to restrict/set/lock a map's bearing with extreme precision.
  ///
  /// The map camera tilt can also adjust by placing two fingertips on the map and moving both fingers up and down in parallel at the same time or
  final double? tilt;

  ///
  /// When true, alternate routes will be presented
  final bool? alternatives;

  ///
  /// The navigation mode desired. Defaults to drivingWithTraffic
  final MapNavigationMode? mode;

  /// The unit of measure said in voice instructions
  final MapVoiceUnits? units;

  ///The max vehicle height in meters. If this parameter is provided, the Directions API will compute a route that includes only roads with a height limit greater than or equal to the max vehicle height. The default value is 1.6 meters.
  String? maxHeight;

  /// The max vehicle weight, in metric tons (1000 kg). If this parameter is provided, the Directions API will compute a route that includes only roads with a weight limit greater than or equal to the max vehicle weight. max_weight must be between 0 and 100 metric tons.
  ///The default value is 2.5 metric tons.
  String? maxWeight;

  //The max vehicle width in meters. If this parameter is provided, the Directions API will compute a route that includes only roads with a width limit greater than or equal to the max vehicle width. The default value is 1.9 meters.
  String? maxWidth;

  /// If the value of this property is true, a returned route may require an immediate U-turn at an intermediate waypoint. At an intermediate waypoint, if the value of this property is false, each returned route may continue straight ahead or turn to either side but may not U-turn. This property has no effect if only two waypoints are specified.
  /// same as 'not continueStraight' on Android
  final bool? allowsUTurnAtWayPoints;

  /// The Url of the style the Navigation MapView
  final String? mapStyleUrl;

  /// When the user long presses on a point on the map, set that as the destination
  final bool? longPressDestinationEnabled;

  final List<String>? avoid;

  MapBoxOptions({
    required this.language,
    required this.zoom,
    required this.bearing,
    required this.tilt,
    required this.alternatives,
    required this.mode,
    required this.units,
    required this.allowsUTurnAtWayPoints,
    required this.longPressDestinationEnabled,
    this.maxHeight,
    this.maxWidth,
    this.maxWeight,
    this.mapStyleUrl,
    this.avoid,
  });

  Map<String, dynamic> toMap() {
    final Map<String, dynamic> optionsMap = {};

    optionsMap['language'] = language;
    optionsMap['longPressDestinationEnabled'] = longPressDestinationEnabled;
    optionsMap['zoom'] = zoom;
    optionsMap['bearing'] = bearing;
    optionsMap['tilt'] = tilt;
    optionsMap['alternatives'] = alternatives;
    optionsMap['mode'] = mode?.name;
    optionsMap['units'] = units?.name;
    optionsMap['allowsUTurnAtWayPoints'] = allowsUTurnAtWayPoints;
    optionsMap['avoid'] = avoid ?? [];
    optionsMap['mapStyleUrl'] = mapStyleUrl;
    optionsMap['maxHeight'] = maxHeight;
    optionsMap['maxWeight'] = maxWeight;
    optionsMap['maxWidth'] = maxWidth;

    return optionsMap;
  }

  Map<String, dynamic> updatesMap(MapBoxOptions newOptions) {
    final Map<String, dynamic> prevOptionsMap = toMap();

    return newOptions.toMap()
      ..removeWhere(
        (String key, dynamic value) => prevOptionsMap[key] == value,
      );
  }
}
