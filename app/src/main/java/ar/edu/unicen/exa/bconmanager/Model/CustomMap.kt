package ar.edu.unicen.exa.bconmanager.Model

import android.util.Log
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonBeacon
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonFingerprintZone
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonMap
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonPoI
import ar.edu.unicen.exa.bconmanager.R

class CustomMap constructor(var image : String, var width : Double , var height : Double, var angle : Double = 0.0 ) {
    var widthPixels : Int = 0
    var heightPixels : Int = 0
    var savedBeacons : MutableList<BeaconOnMap> = mutableListOf<BeaconOnMap>()
    var fingerprintZones : MutableList<FingerprintZone> = mutableListOf<FingerprintZone>()
    var pointsOfInterest : MutableList<PointOfInterest> = mutableListOf<PointOfInterest>()
    var widthMtsToPixelsRatio : Double = 0.0
    var heightMtsToPixelsRatio : Double = 0.0

    fun startFromFile(jsonMap: JsonMap) {
        for (beacon in jsonMap.beaconList!!) {
            val testBeacon = BeaconOnMap(Location(beacon.x!!, beacon.y!!, this),
                    BeaconDevice(beacon.mac!!, 80, null))
            testBeacon.image = R.drawable.beacon_icon
            testBeacon.beacon.name = beacon.name
            this.addBeacon(testBeacon)
        }
        if (jsonMap.pointsOfInterest != null) {
            for (p in jsonMap.pointsOfInterest!!) {
                val point = PointOfInterest(Location(p.x, p.y, this), p.r, p.content,p.id)
                point.image = R.drawable.zone_icon
                this.addPoI(point)
            }
        }
        if (jsonMap.fingerprintZones != null) {
           for (z in jsonMap.fingerprintZones!!) {
               Log.d("JSONTEST", z.toString())
               val zone = FingerprintZone(Location(z.x!!, z.y!!, this))
               val fingerprints = mutableListOf<Fingerprint>()
               for (f in z.fingerprints!!) {
                   Log.d("JSONTEST", f.toString())
                   fingerprints.add(Fingerprint(f.mac!!, f.rssi!!))
               }
               zone.fingerprints = fingerprints
               zone.hasData = true
               zone.unTouch()
               this.fingerprintZones.add(zone)
           }
        }
        this.image = jsonMap.image!!
        this.width = jsonMap.width!!
        this.height = jsonMap.height!!
        this.angle = jsonMap.angle!!
        Log.d("LOADING", "Json parsed to CustomMap")

    }

    fun toJson() : JsonMap {
        val beaconList = mutableListOf<JsonBeacon>()
        for (beacon in this.savedBeacons) {
            beaconList.add(beacon.toJson())
        }
        val pointsList = mutableListOf<JsonPoI>()
        for (point in this.pointsOfInterest) {
            pointsList.add(point.toJson())
        }
        val fingerprintList = mutableListOf<JsonFingerprintZone>()
        for (zone in this.fingerprintZones) {
            fingerprintList.add(zone.toJson())
        }
        var map : JsonMap
        if (pointsList.size == 0 && fingerprintList.size == 0) {
            map = JsonMap(image, width, height, angle, beaconList)
        }
        else if (fingerprintList.size == 0) {
            map = JsonMap(image, width, height, angle, beaconList, pointsList)
        } else if (pointsList.size == 0) {
            map = JsonMap(image, width, height, angle, beaconList, null, fingerprintList)
        } else {
            map = JsonMap(image, width, height, angle, beaconList, pointsList, fingerprintList)
        }
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
        return "CustomMap: $image - width: $width - height: $height - angle: $angle /n" +
                "Beacons: ${savedBeacons.toString()}"
    }

    fun restrictPosition(newPosition : PositionOnMap) : PositionOnMap {

        if (newPosition.position.getX() > this.widthPixels) {
            newPosition.position.setX(this.widthPixels)
        } else if (newPosition.position.getX() < 0) {
            newPosition.position.setX(0)
        }

        if (newPosition.position.getY() > this.heightPixels) {
            newPosition.position.setY(this.heightPixels)
        } else if (newPosition.position.getY() < 0) {
            newPosition.position.setY(0)
        }

        return newPosition
    }

    fun sortBeaconsByDistance (savedBeacons : List<BeaconOnMap> = this.savedBeacons): List<BeaconOnMap> {
            Log.d("SAVED", "${savedBeacons}")
    val sortedList = savedBeacons.sortedWith(compareBy { it.beacon.approxDistance })

    val beacon0 = sortedList[0]
    val beacon1 = sortedList[1]
    val beacon2 = sortedList[2]
    val beacon3 = sortedList[3]

    /*Log.d("CLOSEST",
    "1: ${beacon0.beacon.name} at ${beacon0.beacon.approxDistance}mts // " +
    "2: ${beacon1.beacon.name} at ${beacon1.beacon.approxDistance}mts // " +
    "3: ${beacon2.beacon.name} at ${beacon2.beacon.approxDistance}mts" +
    "4: ${beacon3.beacon.name} at ${beacon3.beacon.approxDistance}mts")*/
        return sortedList
    }
}
