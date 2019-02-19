package ar.edu.unicen.exa.bconmanager.Model.Json

class JsonSheet constructor(
        var timestamp: List<Long>,
        var error: List<Double?>,
        var errorMax: Double? = null,
        var errorAverage: Double? = null,
        var errorMedian: Double? = null
)