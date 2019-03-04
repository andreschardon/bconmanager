package ar.edu.unicen.exa.bconmanager.Model

import android.util.Log
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonData
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonDataBeacon

class AveragedTimestamp {
    var beacons: MutableList<JsonDataBeacon> = mutableListOf()
    var positionX: Double = -1.0
    var positionY: Double = -1.0
    var accelerationList: MutableList<Float> = mutableListOf<Float>()
    var timeList: MutableList<Float> = mutableListOf<Float>()
    var angleList: MutableList<Double> = mutableListOf<Double>()


    fun startFromData(data: JsonData, nextTimestamp: Number) {
        Log.d("AVERAGE-RSSI", "RESULT $beacons")
        accelerationList.clear()
        timeList.clear()
        angleList.clear()
        beacons.clear()
        data.beacons!!.forEach {
            beacons.add(it.clone())
        }
        positionX = data.positionX
        positionY = data.positionY
        accelerationList.add(data.acceleration)
        timeList.add(nextTimestamp.toFloat() - data.timestamp.toFloat())
        angleList.add(data.angle)
        Log.d("AVERAGE-RSSI", "Setting $beacons")
    }

    fun startFromBeacons(beaconList: MutableList<BeaconOnMap>) {
        beaconList.forEach {
            val dataBeacon = JsonDataBeacon(it.beacon.address, it.beacon.averageRssi, 1)
            beacons.add(dataBeacon)
        }
    }

    fun addData(data: JsonData, nextTimestamp: Number) {
        Log.d("AVERAGE-RSSI", "Setting ${data.beacons}")
        data.beacons!!.forEach {
            val index = beacons!!.indexOf(it)
            if (index != -1)
                beacons!![index].calculateAverage(it.rssi!!)
            else
                beacons.add(it.clone())
        }
        accelerationList.add(data.acceleration)
        timeList.add(nextTimestamp.toFloat() - data.timestamp.toFloat())
        angleList.add(data.angle)
        if (data.positionY != 0.0 && data.positionX != 0.0) {
            positionY = data.positionY
            positionX = data.positionX
        }


    }

    override fun toString(): String {
        var beaconString = ""
        beacons.forEach {
            beaconString += "$it "
        }
        return "$beaconString"
    }


}