import 'dart:io';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

import 'controller.dart';
import '../models/models.dart';

/// Callback method for when the navigation view is ready to be used.
///
/// Pass to [MapBoxNavigationView.onMapCreated] to receive a [MapBoxNavigationViewController] when the
/// map is created.
typedef OnNavigationViewCreatedCallBack = void Function(
    MapBoxNavigationViewController controller);

///Embeddable Navigation View.
class MapBoxNavigationView extends StatelessWidget {
  static const StandardMessageCodec _decoder = StandardMessageCodec();
  final MapBoxOptions options;

  final OnNavigationViewCreatedCallBack? onCreated;
  final ValueSetter<RouteEvent>? onRouteEvent;

  const MapBoxNavigationView({
    Key? key,
    required this.options,
    this.onCreated,
    this.onRouteEvent,
  }) : super(key: key);
  @override
  Widget build(BuildContext context) {
    if (Platform.isAndroid) {
      return AndroidView(
          viewType: 'FlutterMapboxView',
          onPlatformViewCreated: _onPlatformViewCreated,
          creationParams: options.toMap(),
          creationParamsCodec: _decoder);
    } else if (Platform.isIOS) {
      return UiKitView(
        viewType: 'FlutterMapboxView',
        onPlatformViewCreated: _onPlatformViewCreated,
        creationParams: options.toMap(),
        creationParamsCodec: _decoder,
      );
    } else {
      return const SizedBox.shrink();
    }
  }

  void _onPlatformViewCreated(int id) {
    if (onCreated == null) {
      return;
    }
    onCreated!(MapBoxNavigationViewController(id, onRouteEvent));
  }
}
