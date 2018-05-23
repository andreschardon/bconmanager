package ar.edu.unicen.exa.bconmanager.Model

import android.util.Log

class Location constructor(var x : Double, var y : Double, val map : CustomMap) {
    // X and Y are in meters

    fun getX() : Int {

//        Log.d("POSITION - WIDTH MTS", "${x.toString()} meters")
//        Log.d("POSITION - WIDTH PX", "${((map.widthMtsToPixelsRatio * x ).toInt()).toString()} pixels")
        return (map.widthMtsToPixelsRatio * x ).toInt()
    }

    fun getY() : Int {
//        Log.d("POSITION - HEIGHT MTS", "${y.toString()} meters")
//        Log.d("POSITION - HEIGHT PX", "${((map.heightMtsToPixelsRatio * y ).toInt()).toString()} pixels")
        return (map.heightMtsToPixelsRatio * y ).toInt()
    }
}