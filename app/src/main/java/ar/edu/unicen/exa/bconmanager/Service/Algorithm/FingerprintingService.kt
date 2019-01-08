package ar.edu.unicen.exa.bconmanager.Service.Algorithm

import android.util.Log
import ar.edu.unicen.exa.bconmanager.Adapters.FingerprintOfflineAdapter
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import ar.edu.unicen.exa.bconmanager.Model.CustomMap
import ar.edu.unicen.exa.bconmanager.Model.FingerprintZone
import ar.edu.unicen.exa.bconmanager.Service.BluetoothScanner
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonData
import ar.edu.unicen.exa.bconmanager.Model.Location

class FingerprintingService() : Algorithm(){

    var savedBeacons : MutableList<BeaconDevice> = mutableListOf<BeaconDevice>()
    private var currentFingerprintingZone: FingerprintZone? = null


    override fun getNextPosition(data: JsonData, nextTimestamp: Number): Location {
        for(beacon in data.beacons!!) {
            savedBeacons.add(BeaconDevice(beacon.mac!!, beacon.rssi!!, null))
        }
        currentFingerprintingZone= getCurrentZone(savedBeacons)


    }

    lateinit var map : CustomMap

    /**
     * Creates a new fingerprinting zone
     */
    fun createFingerprint(currentFingerprintingZone: FingerprintZone) {
        if (!map.fingerprintZones.contains(currentFingerprintingZone)) {
            map.fingerprintZones.add(currentFingerprintingZone)
        }
    }

    /**
     * Removes a fingerprinting zone
     */
    fun removeFingerprint(currentFingerprintingZone: FingerprintZone) {
        map.fingerprintZones.remove(currentFingerprintingZone)
    }

    /**
     * Starts scanning the beacons
     */
    fun startScan(bluetoothScanner : BluetoothScanner, devicesListOfflineAdapter: FingerprintOfflineAdapter) {
        bluetoothScanner.devicesList.clear()
        map.savedBeacons.forEach {
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
    fun getCurrentZone(beacons: List<BeaconDevice>): FingerprintZone? {
        val fingerprintZones =  map.fingerprintZones
        val fingerprintRating = mutableListOf<Double>()
        Log.d("RATINGS", beacons.toString())
        Log.d("RATINGS", fingerprintZones.toString())
        for (zone in fingerprintZones) {
            // For each fingerprinting zone, calculate the "rating"
            var differenceRating = 0.0
            zone.fingerprints.forEach {
                val index = beacons.indexOf(BeaconDevice(it.mac, 0, null))
                if (index != -1) {
                    val beacon = beacons.get(index)
                    differenceRating += Math.abs(beacon.averageRssi - it.rssi)
                }
            }
            fingerprintRating.add(differenceRating)
        }
        Log.d("RATINGS", fingerprintRating.toString())

        // Get the one with less rating
        val index = fingerprintRating.indexOf(fingerprintRating.min())
        Log.d("RATINGS", "$index")

        val bestZone = fingerprintZones.get(index)
        Log.d("RATINGS", "Best zone is $bestZone")

        return bestZone
    }

}