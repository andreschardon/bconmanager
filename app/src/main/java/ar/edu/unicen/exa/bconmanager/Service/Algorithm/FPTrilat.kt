package ar.edu.unicen.exa.bconmanager.Service.Algorithm

import android.util.Log
import ar.edu.unicen.exa.bconmanager.Model.*
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonData

class FPTrilat : Algorithm() {

    private lateinit var fingerPrintService: FingerprintingService
    private lateinit var trilaterationService: TrilaterationService
    override var TAG = "FPTRILAT"

    private object Holder {
        val INSTANCE = FPTrilat()
    }

    companion object {
        val instance: FPTrilat by lazy { Holder.INSTANCE }
    }


    override fun startUp(map: CustomMap) {
        super.startUp(map)
        fingerPrintService = FingerprintingService()
        fingerPrintService.startUp(map)
        trilaterationService = TrilaterationService.instance
        trilaterationService.startUp(map)
    }

    fun getCurrentZone() : FingerprintZone {
        return fingerPrintService.currentFingerprintingZone!!
    }

    fun getNextPoint(beaconList: List<BeaconOnMap>): Location {

        // List of BeaconDevice for Fingerprinting service
        val sBeacons: MutableList<BeaconDevice> = mutableListOf<BeaconDevice>()
        for (b in beaconList) {
            sBeacons.add(b.beacon)
        }

        val loc = fingerPrintService.getCurrentZone(sBeacons, true)!!.position
        //Log.d("NEWTEST", "Fingerprint location is $loc")
        //Log.d(TAG, "LOCATION FP: ${loc.toString()}")
        val vectorToBeacon: MutableList<VectorToBeacon> = mutableListOf<VectorToBeacon>()
        for (b in beaconList) {
            val distanceToBeacon = euclideanDistance(b.position, loc) * b.beacon.reliability
            val angle = getVectorsAngle(b.position, loc)
            val bData = VectorToBeacon(b.beacon.address, distanceToBeacon, angle)
            vectorToBeacon.add(bData)
        }
        val currentFPZone: FingerprintZone = fingerPrintService.getCalculatedZone()


        beaconList.forEach {
            trilaterationService.setTxPower(it)
            it.beacon.calculateDistance(it.beacon.intensity)
        }
        val updatedBeacons = customMap.sortBeaconsByDistance(beaconList)

        //printBeaconsDistances(updatedBeacons)

        var i = 0
        while (i < vectorToBeacon.size && i < 3) {
            //Log.d("NEWTEST", "Distancia REAL " + vectorToBeacon[i].toString())
            val approx = updatedBeacons[i].beacon.approxDistance
            //Log.d("NEWTEST", "Distancia TRILAT " + approx)
            var dFactor = (vectorToBeacon[i].distance - approx) / (vectorToBeacon[i].distance)
            //var dFactor = Math.abs(vectorToBeacon[i].distance - approx) / (vectorToBeacon[i].distance)
            //Log.d("NEWTEST", "dFactor is $dFactor")
            // If the trilat distance is far greater than the "real" one
            if (dFactor > 1.0 ) {
                dFactor = 1.0
            } else if (dFactor < -1.0) {
                dFactor = -1.0
            }

            vectorToBeacon[i].distance = dFactor
            i++
        }

        return updatePosition(currentFPZone, vectorToBeacon, i)
    }

    /**
     * Returns the next position for the Simulation
     */
    override fun getNextPosition(data: AveragedTimestamp): Location {
        val beaconList = getBeacons(data)
        return this.getNextPoint(beaconList)
    }

    private fun printBeaconsDistances(sortedBeacons: List<BeaconOnMap>) {
        val stringList: MutableList<String> = mutableListOf()
        for (beacon in sortedBeacons) {
            stringList.add(beacon.toStringDistance())
        }
        Log.d(TAG, stringList.toString())
    }

    private fun getVectorsAngle(beaconLocation: Location, fpLocation: Location): Double {
        val x = beaconLocation.getXMeters() - fpLocation.getXMeters()
        val y = beaconLocation.getYMeters() - fpLocation.getYMeters()
        return Math.atan2(y, x) * 180 / Math.PI
    }

    fun updatePosition(currentFpZone: FingerprintZone, vectors: List<VectorToBeacon>, beaconAmount: Int): Location {
        var oldX = currentFpZone.position.getXMeters()
        var oldY = currentFpZone.position.getYMeters()
        var newX = 0.0
        var newY = 0.0

        var i = 0
        while (i < beaconAmount) {
            val distanceToMove = currentFpZone.getRadius() * vectors[i].distance
            newX = oldX + distanceToMove * Math.cos(vectors[i].angle)
            newY = oldY + distanceToMove * Math.sin(vectors[i].angle)
            oldX = newX
            oldY = newY
            i++
        }

        val newPosition =  Location(newX, newY, customMap)
        val restrictedPosition = customMap.restrictPosition(PositionOnMap(newPosition))

        return restrictedPosition.position
    }


}