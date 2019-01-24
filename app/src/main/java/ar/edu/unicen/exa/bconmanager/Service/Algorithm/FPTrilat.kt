package ar.edu.unicen.exa.bconmanager.Service.Algorithm

import android.util.Log
import ar.edu.unicen.exa.bconmanager.Model.*
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonData

class FPTrilat : Algorithm() {

    lateinit var fingerPrintService : FingerprintingService
    lateinit var trilaterationService: TrilaterationService


    override fun startUp(map: CustomMap) {
        super.startUp(map)
        fingerPrintService = FingerprintingService()
        fingerPrintService.startUp(map)
        trilaterationService = TrilaterationService.instance
        trilaterationService.startUp(map)
    }
    override fun getNextPosition(data: JsonData, nextTimestamp: Number): Location {
        val loc = fingerPrintService.getNextPosition(data, nextTimestamp)
        Log.d("FPTRILTAT", "LOCATION FP: ${loc.toString()}")
        val beaconList = getBeacons(data)
        var vectorToBeacon : MutableList<VectorToBeacon> = mutableListOf<VectorToBeacon>()
        for(b in beaconList) {
            val distanceToBeacon = euclideanDistance(b.position,loc)
            val angle = getVectorsAngle(b.position,loc)
            val bData = VectorToBeacon(b.beacon.address,distanceToBeacon, angle)
            vectorToBeacon.add(bData)
        }
        val currentFPZone: FingerprintZone = fingerPrintService.getCalculatedZone()

        printBeaconsDistances(customMap.sortBeaconsByDistance())

        trilaterationService.getNextPosition(data, nextTimestamp)
        val updatedBeacons = customMap.sortBeaconsByDistance()

        printBeaconsDistances(updatedBeacons)

        var i = 0
        while (i < vectorToBeacon.size && i < 3) {
            val dFactor = (vectorToBeacon[i].distance)/updatedBeacons.get(i).beacon.approxDistance
            vectorToBeacon[i].distance = dFactor
            i++
        }

        for (b in updatedBeacons) {

        }

        return updatePosition(currentFPZone,vectorToBeacon)

    }

    fun printBeaconsDistances(sortedBeacons: List<BeaconOnMap>) {
        var stringList : MutableList<String> = mutableListOf()
        for (beacon in sortedBeacons) {
            stringList.add(beacon.toStringDistance())
        }
        Log.d("FPTRILTAT", stringList.toString())
    }

    fun getVectorsAngle(beaconLocation: Location, fpLocation: Location): Double {
        val x = beaconLocation.getXMeters() - fpLocation.getXMeters()
        val y = beaconLocation.getYMeters() - fpLocation.getYMeters()
        return Math.atan2(y,x)*180/Math.PI
    }

    fun updatePosition(currentFpZone: FingerprintZone, vectors: List<VectorToBeacon>): Location {
        var oldX = currentFpZone.position.getXMeters()
        var oldY = currentFpZone.position.getYMeters()
        var newX = 0.0
        var newY = 0.0

        for(vector in vectors) {
            val distanceToMove = currentFpZone.getRadius() * vector.distance
            newX = oldX + distanceToMove*Math.cos(vector.angle)
            newY = oldY + distanceToMove*Math.sin(vector.angle)
            oldX = newX
            oldY = newY
        }
        return Location(newX,newY,customMap)
    }


}