package ar.edu.unicen.exa.bconmanager.Service.Algorithm

import android.util.Log
import ar.edu.unicen.exa.bconmanager.Adapters.FingerprintOfflineAdapter
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import ar.edu.unicen.exa.bconmanager.Model.FingerprintZone
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonData
import ar.edu.unicen.exa.bconmanager.Model.Location
import ar.edu.unicen.exa.bconmanager.Service.BluetoothScanner


class FingerprintingService() : Algorithm(){

    private var currentFingerprintingZone: FingerprintZone? = null


    override fun getNextPosition(data: JsonData, nextTimestamp: Number): Location {
        var savedBeacons = getBeacons(data)
        var sBeacons : MutableList<BeaconDevice> = mutableListOf<BeaconDevice>()
        for (b in savedBeacons) {
            sBeacons.add(b.beacon)
        }
        currentFingerprintingZone = getCurrentZone(sBeacons, true)
        Log.d("FINGERP-ZONE", "Is ${currentFingerprintingZone!!.position}")
        return currentFingerprintingZone!!.position
    }

    /**
     * Creates a new fingerprinting zone
     */
    fun createFingerprint(currentFingerprintingZone: FingerprintZone) {
        if (!customMap.fingerprintZones.contains(currentFingerprintingZone)) {
            customMap.fingerprintZones.add(currentFingerprintingZone)
        }
    }

    /**
     * Removes a fingerprinting zone
     */
    fun removeFingerprint(currentFingerprintingZone: FingerprintZone) {
        customMap.fingerprintZones.remove(currentFingerprintingZone)
    }

    /**
     * Starts scanning the beacons
     */
    fun startScan(bluetoothScanner : BluetoothScanner, devicesListOfflineAdapter: FingerprintOfflineAdapter) {
        bluetoothScanner.devicesList.clear()
        customMap.savedBeacons.forEach {
            it.beacon.cleanAverages()
            bluetoothScanner.devicesList.add(it.beacon)
        }
        bluetoothScanner.scanLeDevice(true, devicesListOfflineAdapter, true)
    }

    /**
     * Finishes the scan
     */
    fun finishScan(devicesListOfflineAdapter: FingerprintOfflineAdapter, currentFingerprintingZone: FingerprintZone) {
        currentFingerprintingZone.updateFingerprints(devicesListOfflineAdapter.beacons)
    }


    /**
     * Returns the best fingerprinting zone according to RSSI values of the beacons
     */
    fun getCurrentZone(beacons: List<BeaconDevice>, instant: Boolean = false): FingerprintZone? {
        val fingerprintZones =  customMap.fingerprintZones
        val fingerprintRating = mutableListOf<Double>()
        //Log.d("RATINGS", beacons.toString())
        //Log.d("RATINGS", fingerprintZones.toString())
        var maxDistance = 0.0
        for (zone in fingerprintZones) {
            // For each fingerprinting zone, calculate the "rating"
            var differenceRating = 0.0
            zone.fingerprints.forEach {
                val index = beacons.indexOf(BeaconDevice(it.mac, 0, null))
                if (index != -1) {
                    val beacon = beacons[index]
                    if (instant) {
                        // Simulation
                        differenceRating += Math.abs(beacon.intensity - it.rssi)
                    } else {
                        // Real time
                        differenceRating += Math.abs(beacon.averageRssi - it.rssi)
                    }

                }
            }
            fingerprintRating.add(differenceRating)
            // To avoid jumps
            if (currentFingerprintingZone != null) {
                val thisDist = distanceBetweenZones(currentFingerprintingZone!!, zone)
                if (thisDist > maxDistance)
                    maxDistance = thisDist
            }
        }
        Log.d("RATINGS", beacons.toString())
        Log.d("RATINGS", fingerprintRating.toString())
        Log.d("RATINGS", "MAX DISTANCE IS $maxDistance")

        // Modify ratings to prioritize closer zones
        if (currentFingerprintingZone != null) {
            var index = 0
            fingerprintZones.forEach {
                fingerprintRating[index] = prioritizeCloserZones(fingerprintRating[index], it, maxDistance)
                index++
            }

        }

        Log.d("RATINGS", fingerprintRating.toString())

        // Get the one with less rating
        val index = fingerprintRating.indexOf(fingerprintRating.min())
        Log.d("RATINGS", "$index")

        val bestZone = fingerprintZones.get(index)
        Log.d("RATINGS", "Best zone is $bestZone ${fingerprintRating[index]}")

        currentFingerprintingZone = bestZone

        return bestZone
    }

    fun getCalculatedZone() : FingerprintZone {
        return this.currentFingerprintingZone!!
    }

    private fun prioritizeCloserZones(rating: Double, zone: FingerprintZone, maxDistance: Double): Double {
        val distance = distanceBetweenZones(currentFingerprintingZone!!, zone)
        var priotitize = (40 * Math.pow((distance/ maxDistance), 2.0) )
        //var priotitize = ((20 * distance) / maxDistance)
        Log.d("RATING", "Formula result is $priotitize")
        var newRating = rating + priotitize
        return newRating
    }

    private fun distanceBetweenZones(first: FingerprintZone, second: FingerprintZone) : Double {
        return Math.sqrt(Math.pow(second.position.x - first.position.x, 2.0) + Math.pow(second.position.y - first.position.y, 2.0))
    }
}