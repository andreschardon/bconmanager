package ar.edu.unicen.exa.bconmanager.Model.Json

class JsonData constructor(
        var beacons: List<JsonDataBeacon>? = null,
        var angle : Double,
        var acceleration : Float,
        var timestamp: Long,
        var positionX: Double,
        var positionY: Double
        ) {

    override fun toString() : String {
        return "angle $angle acceleration $acceleration timestamp $timestamp"
    }

}
