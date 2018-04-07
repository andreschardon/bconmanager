package ar.edu.unicen.exa.bconmanager.Model

import android.bluetooth.BluetoothDevice

class BeaconDevice constructor (val address: String, var intensity: Int, val device:BluetoothDevice) {

    override fun toString(): String {
        return address + " - " + intensity.toString()
    }

    override fun equals(other: Any?): Boolean {
        return address.equals((other as BeaconDevice).address)
    }

}

