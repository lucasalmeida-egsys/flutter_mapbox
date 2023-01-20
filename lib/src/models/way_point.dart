///A Geo-coordinate Point used for navigation.
class WayPoint {
  String name;
  double latitude;
  double longitude;

  WayPoint({
    required this.name,
    required this.latitude,
    required this.longitude,
  });

  @override
  String toString() {
    return 'WayPoint{name: $name, latitude: $latitude, longitude: $longitude}';
  }

  factory WayPoint.fromJson(Map<String, dynamic> json) {
    return WayPoint(
      name: json['name'],
      latitude: json['latitude'],
      longitude: json['longitude'],
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'name': name,
      'latitude': latitude,
      'longitude': longitude,
    };
  }
}
