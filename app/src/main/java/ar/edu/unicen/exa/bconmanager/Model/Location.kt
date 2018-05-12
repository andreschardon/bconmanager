package ar.edu.unicen.exa.bconmanager.Model

import android.util.Log

class Location constructor(val x : Double, val y : Double, val map : CustomMap) {
    // X and Y are in meters

    fun getX() : Int {
        Log.d("GETX", ((map.widthMtsToPixelsRatio * x ).toInt()).toString())
        return (map.widthMtsToPixelsRatio * x ).toInt()
    }

    fun getY() : Int {
        Log.d("GETY", ((map.heightMtsToPixelsRatio * y ).toInt()).toString())
        return (map.heightMtsToPixelsRatio * y ).toInt()
    }
}