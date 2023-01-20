package com.nick92.flutter_mapbox.models

data class MapBoxMarker(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
) {
    companion object {
        fun from(map: Map<String, *>) = object {
            val id : Long by map;
            val name : String by map;
            val latitude : Double by map;
            val longitude: Double by map;

            val data = MapBoxMarker(
                id,
                name,
                latitude,
                longitude
            )
        }.data
    }

    override fun toString(): String {
        return "{" +
                "  \"id\": $id," +
                "  \"name\": $name," +
                "  \"latitude\": $latitude," +
                "  \"longitude\": $longitude" +
                "}"
    }
}