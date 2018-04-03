package ar.edu.unicen.exa.bconmanager

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log


class MyBeaconsActivity : AppCompatActivity() {

    //val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_beacons)
        val scanner = BluetoothScanner()
        scanner.scanLeDevice(true)
    }
}
