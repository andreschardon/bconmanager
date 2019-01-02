package ar.edu.unicen.exa.bconmanager.Model.Json

class JsonMap constructor (
        val image : String? = null,
        val width : Double? = null,
        val height : Double? = null,
        val angle : Double? = null,
        var beaconList: List<JsonBeacon>? = null,
        var pointsOfInterest : List <JsonPoI>? = null,
        var fingerprintZones: List<JsonFingerprintZone>? = null)