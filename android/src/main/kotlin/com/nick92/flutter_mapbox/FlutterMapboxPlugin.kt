package com.nick92.flutter_mapbox


import android.app.Activity
import android.content.Context

import androidx.annotation.NonNull
import com.nick92.flutter_mapbox.views.EmbeddedViewFactory
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.platform.PlatformViewRegistry

/** FlutterMapboxPlugin */
class FlutterMapboxPlugin: FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler, ActivityAware {

  private lateinit var channel : MethodChannel
  private lateinit var progressEventChannel: EventChannel
  private var currentActivity: Activity? = null
  private lateinit var currentContext: Context

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    val messenger = flutterPluginBinding.binaryMessenger
    channel = MethodChannel(messenger, "flutter_mapbox")
    channel.setMethodCallHandler(this)

    progressEventChannel = EventChannel(messenger, "flutter_mapbox/events")
    progressEventChannel.setStreamHandler(this)

    platformViewRegistry = flutterPluginBinding.platformViewRegistry
    binaryMessenger = messenger;
  }

  companion object {
    var platformViewRegistry: PlatformViewRegistry? = null
    var binaryMessenger: BinaryMessenger? = null

    var view_name = "FlutterMapboxView"
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
   //
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    currentActivity = null
    channel.setMethodCallHandler(null)
    progressEventChannel.setStreamHandler(null)
  }

  override fun onDetachedFromActivity() {
    currentActivity!!.finish()
    currentActivity = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    currentActivity = binding.activity
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    currentActivity = binding.activity
    currentContext = binding.activity.applicationContext

    if (platformViewRegistry != null && binaryMessenger != null && currentActivity != null) {
      platformViewRegistry?.registerViewFactory(
        view_name,
        EmbeddedViewFactory(binaryMessenger!!, currentActivity!!)
      )
    }
  }

  override fun onDetachedFromActivityForConfigChanges() {
    //
  }

  override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
   EmbeddedNavigationView.eventSink = events;
  }

  override fun onCancel(arguments: Any?) {
    EmbeddedNavigationView.eventSink = null;
  }
}