package ar.edu.unicen.exa.bconmanager.Model

import ar.edu.unicen.exa.bconmanager.Model.Json.JsonData
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonDataBeacon

class AveragedTimestamp constructor(
        var beacons: List<JsonDataBeacon>? = null,
        var positionX: Double,
        var positionY: Double,
        var accelerationList: MutableList<Float> = mutableListOf<Float>(),
        var timestampList: MutableList<Long> = mutableListOf<Long>(),
        var angleList: MutableList<Double> = mutableListOf<Double>()
        ) {

    fun startFromData(data : JsonData) {
        beacons = data.beacons
        positionX = data.positionX
        positionY = data.positionY
        accelerationList.add(data.acceleration)
        timestampList.add(data.timestamp)
        angleList.add(data.angle)
    }

    fun addData(data: JsonData) {
        data.beacons!!.forEach {
            val index = beacons!!.indexOf(it)
            beacons!![index].calculateAverage(it.rssi!!)
        }
        accelerationList.add(data.acceleration)
        timestampList.add(data.timestamp)
        angleList.add(data.angle)
    }


}