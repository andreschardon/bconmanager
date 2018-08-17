package ar.edu.unicen.exa.bconmanager.Model

import android.util.Log
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonBeacon
import ar.edu.unicen.exa.bconmanager.R

class FingerZone constructor (position : Location) : Resource(position){
    private val PIXELS_ERROR = 200
    private val red_image = R.drawable.finger_zone_red
    private val green_image = R.drawable.finger_zone_green
    private val blue_image = R.drawable.finger_zone_blue
    var hasData : Boolean = false



    fun isTouched(x : Float, y: Float) : Boolean {
        // TO DO: Compare in meters instead of pixels
        Log.d("PIXELS", "param ($x , $y)")
        Log.d("PIXELS", "posit (${position.getX()} , ${position.getY()})")
        if ((Math.abs(x - position.getX()) <= PIXELS_ERROR) && (Math.abs(y - position.getY()) <= PIXELS_ERROR)) {
            this.image = blue_image
            return true
        }
        if (hasData) {
            this.image = green_image
        } else {
            this.image = red_image
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