package ar.edu.unicen.exa.bconmanager.Model

import android.bluetooth.BluetoothDevice
import android.graphics.Color
import android.util.Log
import java.math.BigDecimal

class BeaconDevice constructor (val address: String, var intensity: Int, val device:BluetoothDevice) {
    var name : String = address
    var brand : String = "Unknown"
    var approxDistance: Double = 999.toDouble()
    var average: Double = 0.toDouble()
    var averageAmount = 0
    var txPower : Int = -60


    override fun toString(): String {
        var proximity : String
        when {
            (approxDistance <= 0.8) -> proximity = "closest"
            (approxDistance > 0.8 && approxDistance <= 1.3) -> proximity = "close"
            (approxDistance > 1.3 && approxDistance <= 2) -> proximity = "not so close"
            (approxDistance > 2 && approxDistance <= 3) -> proximity = "far"
            else -> proximity = "really far"
        }
        return "$name ($address) --- $intensity ($approxDistance mts)"
    }

    override fun equals(other: Any?): Boolean {
        return address.equals((other as BeaconDevice).address)
    }

    fun Double.roundTo2DecimalPlaces() =
            BigDecimal(this).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()

    fun calculateDistance(rssi : Int) : Double {
        /**
         * d = 10 ^ ((TxPower - RSSI) / 20)
         */
        val approxDistance = ((Math.pow(10.toDouble(), ((txPower - rssi)/30f).toDouble())))
                .roundTo2DecimalPlaces()
        average = (((average * averageAmount) + approxDistance ) / (averageAmount+1))
        averageAmount = averageAmount + 1
        this.approxDistance = average.roundTo2DecimalPlaces()
        Log.d("Beacon : ", "$name ::: $average ::: $averageAmount")

        return average
    }

}

