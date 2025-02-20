package com.nick92.flutter_mapbox.utilities;

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.NonNull
import com.nick92.flutter_mapbox.models.MapBoxEvents
import com.nick92.flutter_mapbox.models.MapBoxRouteProgressEvent
import com.google.gson.Gson
import com.nick92.flutter_mapbox.EmbeddedNavigationView
import io.flutter.plugin.common.MethodCall
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.*

class PluginUtilities {
    companion object {
        @JvmStatic
        fun getResourceFromContext(@NonNull context: Context, resName: String): String {
            val stringRes = context.resources.getIdentifier(resName, "string", context.packageName)
            if (stringRes == 0) {
                throw IllegalArgumentException(String.format("The 'R.string.%s' value it's not defined in your project's resources file.", resName))
            }
            return context.getString(stringRes)
        }

        fun sendEvent(event: MapBoxRouteProgressEvent) {
            val dataString = Gson().toJson(event)
            val jsonString = "{" +
                    "  \"eventType\": \"${MapBoxEvents.PROGRESS_CHANGE.value}\"," +
                    "  \"data\": $dataString" +
                    "}"
            EmbeddedNavigationView.eventSink?.success(jsonString)
        }

        fun sendEvent(event: MapBoxEvents, data: String = "") {
            val jsonString = if (MapBoxEvents.MILESTONE_EVENT == event || event == MapBoxEvents.USER_OFF_ROUTE) "{" +
                    "  \"eventType\": \"${event.value}\"," +
                    "  \"data\": $data" +
                    "}" else "{" +
                    "  \"eventType\": \"${event.value}\"," +
                    "  \"data\": \"$data\"" +
                    "}";
            EmbeddedNavigationView.eventSink?.success(jsonString)
        }

        fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val nw = connectivityManager.activeNetwork ?: return false
                val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
                return when {
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                    else -> false
                }
            } else {
                val nwInfo = connectivityManager.activeNetworkInfo ?: return false
                return nwInfo.isConnected
            }
        }
    }


}