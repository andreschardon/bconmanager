package ar.edu.unicen.exa.bconmanager.Model

import android.bluetooth.BluetoothDevice
import android.util.Log
import java.math.BigDecimal

class BeaconDevice constructor (val address: String, var intensity: Int, val device:BluetoothDevice?) {
    var name : String = address
    var brand : String = "Unknown"
    var approxDistance: Double = 999.0
    var averageRssi : Double = 0.0
    var average: Double = 0.0
    var averageAmount = 0
    var txPower : Int = -60


    override fun toString(): String {
        return "$name ($address) --- $intensity ($approxDistance mts)"
    }

    override fun equals(other: Any?): Boolean {
        return address == (other as BeaconDevice).address
    }

    fun Double.roundTo2DecimalPlaces() =
            BigDecimal(this).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()

    fun calculateDistance(rssi : Int) : Double {
        /**
         * d = 10 ^ ((TxPower - RSSI) / 20)
         */
        val approxDistance = ((Math.pow(10.toDouble(), ((txPower - rssi)/30f).toDouble())))
                .roundTo2DecimalPlaces()
        if (average >= 999.0)
            cleanAverages()
        average = (((average * averageAmount) + approxDistance ) / (averageAmount+1))
        averageRssi = (((averageRssi * averageAmount) + rssi) / (averageAmount+1))
        averageRssi = averageRssi.roundTo2DecimalPlaces()
        averageAmount++
        this.approxDistance = average.roundTo2DecimalPlaces()
        //Log.d("Beacon : ", "$name ::: $average ::: $averageAmount")

        return average
    }

    fun cleanAverages() {
        Log.d("Beacon", "Setting to 0")
        averageAmount = 0
        average = 0.0
        averageRssi = 0.0
    }

}

