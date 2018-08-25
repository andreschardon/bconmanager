package ar.edu.unicen.exa.bconmanager.Model.Json

class JsonFingerprintZone constructor(
        val x : Double? = null,
        val y : Double? = null,
        var fingerprints: List<JsonFingerprint>? = null) {
    override fun toString(): String {
        return "$x, $y"
    }
}