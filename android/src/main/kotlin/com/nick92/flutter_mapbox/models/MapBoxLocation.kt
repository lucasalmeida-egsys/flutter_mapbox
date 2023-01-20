package com.nick92.flutter_mapbox.models

data class MapBoxLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double
    )
{
    override fun toString(): String {
        return "{" +
                "  \"latitude\": $latitude," +
                "  \"longitude\": $longitude" +
                "}"
    }

    companion object {
        fun from(map: Map<String, *>) = object {
            val name : String by map;
            val latitude : Double by map;
            val longitude: Double by map;

            val data = MapBoxLocation(
                name,
                latitude,
                longitude
            )
        }.data
    }

}