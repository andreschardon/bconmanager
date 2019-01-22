package ar.edu.unicen.exa.bconmanager.Model.Json

class JsonData constructor(
        var beacons: List<JsonDataBeacon>? = null,
        var angle : Double,
        var acceleration : Float,
        var timestamp: Long,
        var positionX: Double,
        var positionY: Double,
        var error: Double? = null,
        var estimateX: Double? = null,
        var estimateY: Double? = null,
        var pPDRX: Double? = null,
        var pPDRY: Double? = null,
        var pPaFX: Double? = null,
        var pPaFY: Double? = null,
        var pTRIX: Double? = null,
        var pTRIY: Double? = null,
        var pFiPX: Double? = null,
        var pFiPY: Double? = null
        ) {

    override fun toString() : String {
        return "angle $angle acceleration $acceleration timestamp $timestamp"
    }

}
