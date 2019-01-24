package ar.edu.unicen.exa.bconmanager.Service.Algorithm

import ar.edu.unicen.exa.bconmanager.Model.VectorToBeacon
import ar.edu.unicen.exa.bconmanager.Model.FingerprintZone
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonData
import ar.edu.unicen.exa.bconmanager.Model.Location

class FPTrilat : Algorithm() {

    lateinit var fingerPrintService : FingerprintingService
    lateinit var trilaterationService: TrilaterationService

    override fun getNextPosition(data: JsonData, nextTimestamp: Number): Location {
        fingerPrintService = FingerprintingService()
        val loc = fingerPrintService.getNextPosition(data, nextTimestamp)
        val beaconList = getBeacons(data)
        var vectorToBeacon : MutableList<VectorToBeacon> = mutableListOf<VectorToBeacon>()
        for(b in beaconList) {
            val distanceToBeacon = euclideanDistance(b.position,loc)
            val angle = getVectorsAngle(b.position,loc)
            val bData = VectorToBeacon(b.beacon.address,distanceToBeacon, angle)
            vectorToBeacon.add(bData)
        }
        val currentFPZone: FingerprintZone = fingerPrintService.getCalculatedZone()
        val fpRadius = currentFPZone.getRadius()
        trilaterationService.getNextPosition(data, nextTimestamp)
        val updatedBeacons = customMap.sortBeaconsByDistance()
        var i = 0

        for (b in updatedBeacons) {
            val dFactor = (vectorToBeacon[i].distance)/b.beacon.approxDistance
            vectorToBeacon[i].distance = dFactor
        }

        return updatePosition(currentFPZone,vectorToBeacon)

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