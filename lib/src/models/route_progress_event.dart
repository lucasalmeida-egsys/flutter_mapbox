import 'route_leg.dart';
import '../helpers.dart';

///This class contains all progress information at any given time during a navigation session.
///This progress includes information for the current route, leg and step the user is traversing along.
///With every new valid location update, a new route progress will be generated using the latest information.
class RouteProgressEvent {
  bool? arrived;
  double? distance;
  double? duration;
  double? distanceTraveled;
  double? currentLegDistanceTraveled;
  double? currentLegDistanceRemaining;
  String? currentStepInstruction;
  RouteLeg? currentLeg;
  RouteLeg? priorLeg;
  List<RouteLeg>? remainingLegs;
  int? legIndex;
  int? stepIndex;
  bool? isProgressEvent;

  RouteProgressEvent({
    required this.arrived,
    required this.distance,
    required this.duration,
    required this.distanceTraveled,
    required this.currentLegDistanceTraveled,
    required this.currentLegDistanceRemaining,
    required this.currentStepInstruction,
    required this.currentLeg,
    required this.priorLeg,
    required this.remainingLegs,
    required this.legIndex,
    required this.stepIndex,
    required this.isProgressEvent,
  });

  RouteProgressEvent.fromJson(Map<String, dynamic> json) {
    isProgressEvent = json['arrived'] != null;
    arrived = json['arrived'] == null ? false : json['arrived'] as bool?;
    distance = isNullOrZero(json['distance']) ? 0.0 : json["distance"] + .0;
    duration = isNullOrZero(json['duration']) ? 0.0 : json["duration"] + .0;
    distanceTraveled = isNullOrZero(json['distanceTraveled'])
        ? 0.0
        : json["distanceTraveled"] + .0;
    currentLegDistanceTraveled =
        isNullOrZero(json['currentLegDistanceTraveled'])
            ? 0.0
            : json["currentLegDistanceTraveled"] + .0;
    currentLegDistanceRemaining =
        isNullOrZero(json['currentLegDistanceRemaining'])
            ? 0.0
            : json["currentLegDistanceRemaining"] + .0;
    currentStepInstruction = json['currentStepInstruction'];
    currentLeg = json['currentLeg'] == null
        ? null
        : RouteLeg.fromJson(json['currentLeg'] as Map<String, dynamic>);
    priorLeg = json['priorLeg'] == null
        ? null
        : RouteLeg.fromJson(json['priorLeg'] as Map<String, dynamic>);
    remainingLegs = (json['remainingLegs'] as List?)
        ?.map((e) =>
            e == null ? null : RouteLeg.fromJson(e as Map<String, dynamic>))
        .cast<RouteLeg>()
        .toList();
    legIndex = json['legIndex'];
    stepIndex = json['stepIndex'];
  }
}
