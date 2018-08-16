package ar.edu.unicen.exa.bconmanager.Model

import android.util.Log
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonBeacon

class FingerZone constructor (position : Location) : Resource(position){
    private val PIXELS_ERROR = 50
    var hasData : Boolean = false



    fun isTouched(x : Float, y: Float) : Boolean {
        Log.d("PIXELS", "param ($x , $y)")
        Log.d("PIXELS", "posit (${position.getX()} , ${position.getY()})")
        Log.d("PIXELS", "${x - position.getX()}")
        if (Math.abs(x - position.getX()) <= PIXELS_ERROR) {
            Log.d("PIXELS", "X true")
            return true
        }
        if (Math.abs(y - position.getY()) <= PIXELS_ERROR) {
            Log.d("PIXELS", "Y true")
            return true
        }
        Log.d("PIXELS", "Returns false")
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