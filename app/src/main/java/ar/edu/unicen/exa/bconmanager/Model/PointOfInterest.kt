package ar.edu.unicen.exa.bconmanager.Model

import android.support.v4.app.NotificationCompat
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonPoI
import ar.edu.unicen.exa.bconmanager.Service.TrilaterationCalculator


class PointOfInterest constructor (position : Location, zone : Double , id : String, content : String) : Resource(position) {
    var zone =  zone
    var id = id
    var content = content
    var alreadyInside = false
    lateinit var notification : NotificationCompat.Builder
    private var trilaterationCalculator = TrilaterationCalculator()

    fun isInside(positionOnMap: PositionOnMap) : Boolean {
        return (trilaterationCalculator.euclideanDistance(positionOnMap.position,position)<= zone)
    }
    fun toJson() : JsonPoI {
        val jsonPoi = JsonPoI(this.position.x,this.position.y,this.zone,this.id,this.content)
        return jsonPoi
    }
    override fun toString() : String {
        return "Position: (${position.x} , ${position.y}), Zone: ($zone)"
    }
}
