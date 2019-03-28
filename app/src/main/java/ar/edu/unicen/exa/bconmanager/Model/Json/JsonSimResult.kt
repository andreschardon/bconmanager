package ar.edu.unicen.exa.bconmanager.Model.Json

class JsonSimResult constructor(
        var timestamps: List<JsonTimestamp>? = null,
        var errorMax: Double? = null,
        var errorAverage: Double? = null,
        var errorMedian: Double? = null,
        var stdDev : Double? = null) {
}