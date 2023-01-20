import 'dart:convert';
import '../enums/events.dart';
import 'route_progress_event.dart';

/// Represents an event sent by the navigation service
class RouteEvent {
  MapBoxEvent? eventType;
  dynamic data;

  RouteEvent({required this.eventType, required this.data});

  RouteEvent.fromJson(Map<String, dynamic> json) {
    if (json['eventType'] is int) {
      eventType = MapBoxEvent.values[json['eventType']];
    } else {
      try {
        eventType = MapBoxEvent.values.firstWhere(
          (e) => e.id == json['eventType'],
        );
      } on StateError {
        //When the list is empty or eventType not found (Bad State: No Element)
      } catch (_) {
        //
      }
    }
    var dataJson = json['data'];
    if (eventType == MapBoxEvent.progressChange) {
      data = RouteProgressEvent.fromJson(dataJson);
    } else {
      data = jsonEncode(json['data']);
    }
  }
}
