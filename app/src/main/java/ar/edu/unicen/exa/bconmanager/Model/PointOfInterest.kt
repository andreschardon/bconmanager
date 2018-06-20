package ar.edu.unicen.exa.bconmanager.Model

import ar.edu.unicen.exa.bconmanager.Service.TrilaterationCalculator


class PointOfInterest constructor (position : Location, zone : Double) : Resource(position) {
    var zone =  zone
    private var trilaterationCalculator = TrilaterationCalculator()

    fun isInside(positionOnMap: PositionOnMap) : Boolean {
        return (trilaterationCalculator.euclideanDistance(positionOnMap.position,position)<= zone)
    }
    fun toJson() : Circle {
        val circle = Circle(this.position.x,this.position.y,this.zone)
        return circle
    }
    override fun toString() : String {
        return "Position: (${position.x} , ${position.y}), Zone: ($zone)"
    }
}
