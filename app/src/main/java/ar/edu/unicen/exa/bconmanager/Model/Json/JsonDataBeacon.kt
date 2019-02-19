package ar.edu.unicen.exa.bconmanager.Model.Json

class JsonDataBeacon constructor(
        val mac : String? = null,
        var rssi : Int? = null,
        var counter: Int? = null) {



    override fun toString(): String {
        return "$mac!!"
    }


    override fun equals(other: Any?): Boolean {
        return ((other as JsonDataBeacon).mac == mac)
    }

    fun calculateAverage(newRssi: Int) {
        if (counter == null) {
            counter = 1
        }
        var rssiSum = (rssi!! * counter!!)
        rssiSum += newRssi
        counter = counter!! + 1
        rssi = (rssiSum / counter!!)
    }
}