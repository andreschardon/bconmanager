package ar.edu.unicen.exa.bconmanager.Model

import ar.edu.unicen.exa.bconmanager.Model.Json.JsonBeacon

class FingerZone constructor (position : Location) : Resource(position){
    private val PIXELS_ERROR = 50
    var hasData : Boolean = false



    fun isTouched(x : Float, y: Float) : Boolean {
        if (Math.abs(x.compareTo(position.x)) <= PIXELS_ERROR) {
            return true
        }
        if (Math.abs(y.compareTo(position.y)) <= PIXELS_ERROR) {
            return true
        }
        return false
    }


//    fun toJson() : JsonBeacon {
//        val beac = JsonBeacon(beacon.address, position.x, position.y)
//        return beac
//    }

    override fun toString(): String {
        return "FingerZone: (${position.getX()} , ${position.getY()})"
    }
}