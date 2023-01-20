/// All possible events that could occur in the course of navigation
///
enum MapBoxEvent {
  mapReady('map_ready'),
  routeBuilding('route_building'),
  routeBuilt('route_built'),
  routeBuildFailed('route_build_failed'),
  routeBuildCancelled('route_build_cancelled'),
  routeBuildNoRoutesFound('route_build_no_routes_found'),
  progressChange('progress_change'),
  userOffRoute('user_off_route'),
  milestoneEvent('milestone_event'),
  navigationRunning('navigation_running'),
  navigationCancelled('navigation_cancelled'),
  navigationFinished('navigation_finished'),
  fasterRouteFound('faster_route_found'),
  speechAnnouncement('speech_announcement'),
  bannerInstruction('banner_instruction'),
  onArrival('on_arrival'),
  failedToReroute('failed_to_reroute'),
  reRouteAlong('re_route_along'),
  annotationTapped('annotation_tapped');

  final String id;
  const MapBoxEvent(this.id);
}
