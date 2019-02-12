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

    fun getXMeters() : Double {
        if (map != null) {
            return x
        }
        return 0.0
    }

    fun getYMeters() : Double {
        if (map != null) {
            return y
        }
        return 0.0
    }


    fun setX(pixelsX : Int) {
        if (map != null) {
            x = (pixelsX / map.widthMtsToPixelsRatio )
        }
    }

    fun getY() : Int{
//        Log.d("POSITION - HEIGHT MTS", "${y.toString()} meters")
//        Log.d("POSITION - HEIGHT PX", "${((map.heightMtsToPixelsRatio * y ).toInt()).toString()} pixels")
        if (map != null) {
            return (map.heightMtsToPixelsRatio * y ).toInt()
        }
        return 0
    }


    fun setY(pixelsY : Int) {
        if (map != null) {
            y = (pixelsY / map.widthMtsToPixelsRatio )
        }
    }

    override fun toString(): String {
        return "($x, $y) and (${this.getX()}, ${this.getY()})"
    }

    override fun equals(other: Any?): Boolean {
        return (this.x == (other as Location).x) && (this.y == other.y)
    }

    fun clone() : Location {
        return Location(this.x, this.y, this.map)
    }
}