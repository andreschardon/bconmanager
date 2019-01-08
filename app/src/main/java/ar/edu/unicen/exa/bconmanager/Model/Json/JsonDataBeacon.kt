package ar.edu.unicen.exa.bconmanager.Model.Json

class JsonDataBeacon constructor(
        val mac : String? = null,
        val rssi : Int? = null) {
    override fun toString(): String {
        return "$mac!!"
    }
}