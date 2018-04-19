package ar.edu.unicen.exa.bconmanager.Model

import android.bluetooth.BluetoothDevice
import java.math.BigDecimal

class BeaconDevice constructor (val address: String, var intensity: Int, val device:BluetoothDevice) {
    var name : String = address
    var brand : String = "Unknown"
    var approxDistance: Double = 999.toDouble()
    var txPower : Int = -60


    override fun toString(): String {
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
        return ((Math.pow(10.toDouble(), ((txPower - rssi)/30f).toDouble()))).roundTo2DecimalPlaces()
    }

}

