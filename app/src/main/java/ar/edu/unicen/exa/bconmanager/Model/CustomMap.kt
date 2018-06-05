package ar.edu.unicen.exa.bconmanager.Model

import android.util.Log
import ar.edu.unicen.exa.bconmanager.R

class CustomMap constructor(var image : String, var width : Double , var height : Double ) {
    var widthPixels : Int = 0
    var heightPixels : Int = 0
    var savedBeacons : MutableList<BeaconOnMap> = mutableListOf<BeaconOnMap>()
    var pointsOfInterest : MutableList<PointOfInterest> = mutableListOf<PointOfInterest>()
    var widthMtsToPixelsRatio : Double = 0.0
    var heightMtsToPixelsRatio : Double = 0.0

    fun startFromFile(jsonMap: JsonMap) {
        for (beacon in jsonMap.beaconList!!) {
            val testBeacon = BeaconOnMap(Location(beacon.x!!, beacon.y!!, this),
                    BeaconDevice(beacon.mac!!, 80, null))
            testBeacon.image = R.drawable.beacon_icon
            this.addBeacon(testBeacon)
        }
        this.image = jsonMap.image!!
        this.width = jsonMap.width!!
        this.height = jsonMap.height!!
        Log.d("LOADING", "Json parsed to CustomMap")

    }

    fun toJson() : JsonMap {
        val beaconList = mutableListOf<JsonBeacon>()
        for (beacon in this.savedBeacons) {
            beaconList.add(beacon.toJson())
        }
        val map = JsonMap(image, width, height, beaconList)
        return map

    }

    fun addBeacon(beacon : BeaconOnMap) {
        savedBeacons.add(beacon)
    }

    fun addPoI(point : PointOfInterest) {
        pointsOfInterest.add(point)
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

    override fun toString(): String {
        return "CustomMap: $image - width: $width - height: $height /n" +
                "Beacons: ${savedBeacons.toString()}"
    }

}
