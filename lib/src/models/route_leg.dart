import 'way_point.dart';
import 'route_step.dart';
import '../helpers.dart';

///A RouteLeg object defines a single leg of a route between two waypoints.
///If the overall route has only two waypoints, it has a single RouteLeg object that covers the entire route.
///The route leg object includes information about the leg, such as its name, distance, and expected travel time.
///Depending on the criteria used to calculate the route, the route leg object may also include detailed turn-by-turn instructions.
class RouteLeg {
  String? profileIdentifier;
  String? name;
  double? distance;
  double? expectedTravelTime;
  WayPoint? source;
  WayPoint? destination;
  List<RouteStep>? steps;

  RouteLeg({
    required this.profileIdentifier,
    required this.name,
    required this.distance,
    required this.expectedTravelTime,
    required this.source,
    required this.destination,
    required this.steps,
  });

  RouteLeg.fromJson(Map<String, dynamic> json) {
    profileIdentifier = json["profileIdentifier"];
    name = json["name"];
    distance = isNullOrZero(json["distance"]) ? 0.0 : json["distance"] + .0;
    expectedTravelTime = isNullOrZero(json["expectedTravelTime"])
        ? 0.0
        : json["expectedTravelTime"] + .0;
    source = json['source'] == null
        ? null
        : WayPoint.fromJson(json['source'] as Map<String, dynamic>);
    destination = json['destination'] == null
        ? null
        : WayPoint.fromJson(json['destination'] as Map<String, dynamic>);
    steps = (json['steps'] as List?)
        ?.map((e) =>
            e == null ? null : RouteStep.fromJson(e as Map<String, dynamic>))
        .cast<RouteStep>()
        .toList();
  }
}
