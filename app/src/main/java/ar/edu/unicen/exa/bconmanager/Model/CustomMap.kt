package ar.edu.unicen.exa.bconmanager.Model

import android.util.Log

class CustomMap constructor(val image : Int, val width : Double , val height : Double ) {
    var widthPixels : Int = 0
    var heightPixels : Int = 0
    var savedBeacons : MutableList<BeaconOnMap> = mutableListOf<BeaconOnMap>()
    var widthMtsToPixelsRatio : Double = 0.0
    var heightMtsToPixelsRatio : Double = 0.0



    fun startFromFile(filename : String) {
        // TO DO
    }

    fun addBeacon(beacon : BeaconOnMap) {
        savedBeacons.add(beacon)
    }

    fun calculateRatio(widthPixels : Int, heightPixels : Int) {
        /*Log.d("POSITION - MAP HEIGHT MTS", "${height.toString()} mts")
        Log.d("POSITION - MAP HEIGHT PX", "${heightPixels.toString()} pixels")
        Log.d("POSITION - MAP WIDTH MTS", "${width.toString()} mts")
        Log.d("POSITION - MAP WIDTH PX", "${widthPixels.toString()} pixels")*/
        this.widthPixels = widthPixels
        this.heightPixels = heightPixels
        widthMtsToPixelsRatio = widthPixels / width
        heightMtsToPixelsRatio = heightPixels / height
    }

}
