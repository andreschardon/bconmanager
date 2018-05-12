package ar.edu.unicen.exa.bconmanager.Model

import android.util.Log

class CustomMap constructor(val image : Int, val height : Double, val width : Double ) {
    var widthPixels : Int = 0
    var heightPixels : Int = 0
    var savedBeacons : List<BeaconOnMap> = mutableListOf<BeaconOnMap>()
    var widthMtsToPixelsRatio : Double = 0.0
    var heightMtsToPixelsRatio : Double = 0.0



    fun startFromFile(filename : String) {
        // TO DO
    }

    fun calculateRatio(widthPixels : Int, heightPixels : Int) {
        Log.d("HEIGHT IN METERS", height.toString())
        Log.d("WIDTH IN METERS", width.toString())
        this.widthPixels = widthPixels
        this.heightPixels = heightPixels
        widthMtsToPixelsRatio = widthPixels / width
        heightMtsToPixelsRatio = heightPixels / height
        Log.d("HEIGHT", "${heightPixels.toString()} -  ${heightMtsToPixelsRatio.toString()}")
        Log.d("WIDTH", "${widthPixels.toString()} -  ${widthMtsToPixelsRatio.toString()}")
    }

}
