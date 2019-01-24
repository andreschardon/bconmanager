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

//        trilaterationService.getNextPosition(data, nextTimestamp)

        beaconList.forEach {
            trilaterationService.setTxPower(it)
            it.beacon.calculateDistance(it.beacon.intensity)
        }
        val updatedBeacons = customMap.sortBeaconsByDistance(beaconList)

        printBeaconsDistances(updatedBeacons)

        var i = 0
        while (i < vectorToBeacon.size && i < 3) {
            Log.d("FPTRILTAT", vectorToBeacon[i].toString())
            var approx = updatedBeacons.get(i).beacon.approxDistance
            val dFactor = (vectorToBeacon[i].distance -  approx)/(vectorToBeacon[i].distance)
            Log.d("FPTRILTAT", "dFactor is $dFactor")
            vectorToBeacon[i].distance = dFactor
            i++
        }

        return updatePosition(currentFPZone,vectorToBeacon, i)

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

    fun updatePosition(currentFpZone: FingerprintZone, vectors: List<VectorToBeacon>, beaconAmount: Int): Location {
        var oldX = currentFpZone.position.getXMeters()
        var oldY = currentFpZone.position.getYMeters()
        var newX = 0.0
        var newY = 0.0

        var i = 0
        while (i < beaconAmount) {
            val distanceToMove = currentFpZone.getRadius() * vectors[i].distance
            newX = oldX + distanceToMove*Math.cos(vectors[i].angle)
            newY = oldY + distanceToMove*Math.sin(vectors[i].angle)
            oldX = newX
            oldY = newY
            i++
        }
        return Location(newX,newY,customMap)
    }


}