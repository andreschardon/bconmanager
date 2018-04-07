package ar.edu.unicen.exa.bconmanager.Model

import android.bluetooth.BluetoothDevice

class BeaconDevice constructor (val address: String, var intensity: Int, val device:BluetoothDevice) {
    var name : String = address
    var brand : String = "Unknown"


    override fun toString(): String {
        return "$name ($address) --- $intensity"
    }

    override fun equals(other: Any?): Boolean {
        return address.equals((other as BeaconDevice).address)
    }

}

