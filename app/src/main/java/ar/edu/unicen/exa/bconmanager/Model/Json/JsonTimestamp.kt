package ar.edu.unicen.exa.bconmanager.Model.Json

class JsonTimestamp constructor(
        var timestamp: Long,
        var positionX: Double,
        var positionY: Double,
        var error: Double? = null,
        var estimateX: Double? = null,
        var estimateY: Double? = null
)
