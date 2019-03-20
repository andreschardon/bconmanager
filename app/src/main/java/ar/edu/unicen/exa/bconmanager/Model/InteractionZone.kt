package ar.edu.unicen.exa.bconmanager.Model

import android.util.Log
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonIntZone

class InteractionZone  {
    var name : String = ""
    var url : String = ""
    var startX : Double = 0.0
    var startY : Double = 0.0
    var endX : Double = 0.0
    var endY: Double = 0.0

    fun startFromJson(json : JsonIntZone) {
        name = json.name
        url = json.url
        startX = json.area.from[0]
        startY = json.area.from[1]
        endX = json.area.to[0]
        endY = json.area.to[1]
    }

    fun isInside (positionOnMap: Location) : Boolean {
        Log.d("NOTIFICATIONS", "Range ($startX, $startY) to ($endX, $endY)")
        Log.d("NOTIFICATIONS", "Checking point ${positionOnMap.toString()}")
        return positionOnMap.getXMeters() >= startX &&
                positionOnMap.getYMeters() >= startY &&
                positionOnMap.getXMeters() <= endX &&
                positionOnMap.getYMeters() <= endY
    }

}