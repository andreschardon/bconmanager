package ar.edu.unicen.exa.bconmanager.Model

import android.util.Log


class Location constructor(var x : Double, var y : Double, val map : CustomMap?) {
    // X and Y are in meters

    fun getX() : Int {

//        Log.d("POSITION - WIDTH MTS", "${x.toString()} meters")
//        Log.d("POSITION - WIDTH PX", "${((map.widthMtsToPixelsRatio * x ).toInt()).toString()} pixels")
        if (map != null) {
            return (map.widthMtsToPixelsRatio * x ).toInt()
        }
        return 0
    }

    fun getY() : Int{
//        Log.d("POSITION - HEIGHT MTS", "${y.toString()} meters")
//        Log.d("POSITION - HEIGHT PX", "${((map.heightMtsToPixelsRatio * y ).toInt()).toString()} pixels")
        if (map != null) {
            return (map.heightMtsToPixelsRatio * y ).toInt()
        }
        return 0
    }

    override fun toString(): String {
        return "($x, $y)"
    }
}