package ar.edu.unicen.exa.bconmanager.Model

import ar.edu.unicen.exa.bconmanager.Service.TrilaterationCalculator


class PointOfInterest constructor (position : Location, zone : Double) : Resource(position) {
    var zone =  zone
    private var trilaterationCalculator = TrilaterationCalculator()

    fun isInside(positionOnMap: PositionOnMap) : Boolean {
        return (trilaterationCalculator.euclideanDistance(positionOnMap.position,position)<= zone)
    }
}
