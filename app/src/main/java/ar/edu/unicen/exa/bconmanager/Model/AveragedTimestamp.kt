package ar.edu.unicen.exa.bconmanager.Model

import ar.edu.unicen.exa.bconmanager.Model.Json.JsonData
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonDataBeacon

class AveragedTimestamp {
    var beacons: List<JsonDataBeacon>? = null
    var positionX: Double = -1.0
    var positionY: Double = -1.0
    var accelerationList: MutableList<Float> = mutableListOf<Float>()
    var timeList: MutableList<Float> = mutableListOf<Float>()
    var angleList: MutableList<Double> = mutableListOf<Double>()


    fun startFromData(data: JsonData, nextTimestamp: Number) {
        accelerationList.clear()
        timeList.clear()
        angleList.clear()
        beacons = data.beacons
        positionX = data.positionX
        positionY = data.positionY
        accelerationList.add(data.acceleration)
        timeList.add(nextTimestamp.toFloat() - data.timestamp.toFloat())
        angleList.add(data.angle)
    }

    fun addData(data: JsonData, nextTimestamp: Number) {
        data.beacons!!.forEach {
            val index = beacons!!.indexOf(it)
            beacons!![index].calculateAverage(it.rssi!!)
        }
        accelerationList.add(data.acceleration)
        timeList.add(nextTimestamp.toFloat() - data.timestamp.toFloat())
        angleList.add(data.angle)
        positionY = data.positionY
        positionX = data.positionX

    }


}