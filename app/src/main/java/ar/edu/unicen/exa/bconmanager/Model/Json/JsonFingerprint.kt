package ar.edu.unicen.exa.bconmanager.Model.Json

class JsonFingerprint constructor(
        val mac : String? = null,
        val rssi : Double? = null) {
    override fun toString(): String {
        return "mac: $mac rssi: $rssi"
    }
}