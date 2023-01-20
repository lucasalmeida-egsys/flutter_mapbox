package com.nick92.flutter_mapbox.models

import com.mapbox.api.directions.v5.DirectionsCriteria
import java.util.HashMap

data class MapBoxSettings(
    val navigationMode: String,
    val mapStyleUrl: String?,
    val navigationLanguage: String,
    val navigationVoiceUnits: String,
    val zoom: Double,
    val bearing : Double,
    val tilt : Double,
    val alternatives : Boolean,
    val allowsUTurnAtWayPoints: Boolean,
    val longPressDestinationEnabled: Boolean,
    val maxHeight: Double?,
    val maxWeight: Double?,
    val maxWidth: Double?,
    val excludeList: List<String> = listOf()
)
{
    companion object {
        fun from(map: Map<String, *>) = object {
            val mode : String by map;
            val language : String by map
            val units : String by map
            val mapStyleUrl : String? by map

            val maxHeight : Double? by map;
            val maxWeight : Double? by map;
            val maxWidth : Double? by map;

            val zoom : Double by map;
            val bearing : Double by map;
            val tilt : Double by map;
            val alternatives : Boolean by map;
            val longPressDestinationEnabled : Boolean by map;
            val avoid : List<String> by map;
            val allowsUTurnAtWayPoints : Boolean by map;

            val navigationMode = when (mode) {
                "walking" -> DirectionsCriteria.PROFILE_WALKING
                "cycling" -> DirectionsCriteria.PROFILE_CYCLING
                "driving" -> DirectionsCriteria.PROFILE_DRIVING
                else -> DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
            }

           val navigationVoiceUnits = when (units) {
                "imperial" -> DirectionsCriteria.IMPERIAL
                "metric" -> DirectionsCriteria.METRIC
                else -> DirectionsCriteria.METRIC
            }

            val data = MapBoxSettings(
                navigationMode,
                mapStyleUrl,
                language,
                navigationVoiceUnits,
                zoom,
                bearing,
                tilt,
                alternatives,
                allowsUTurnAtWayPoints,
                longPressDestinationEnabled,
                maxHeight,
                maxWeight,
                maxWidth,
                avoid,
            )
        }.data;
    }

}