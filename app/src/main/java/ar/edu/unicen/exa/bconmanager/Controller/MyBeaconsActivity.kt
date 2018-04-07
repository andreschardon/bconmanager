package ar.edu.unicen.exa.bconmanager.Controller

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ArrayAdapter
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import ar.edu.unicen.exa.bconmanager.Service.BluetoothScanner
import ar.edu.unicen.exa.bconmanager.R
import kotlinx.android.synthetic.main.activity_my_beacons.*


class MyBeaconsActivity : AppCompatActivity() {

    val bluetoothScanner = BluetoothScanner()
    lateinit var devicesListAdapter : ArrayAdapter<BeaconDevice>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_beacons)

        devicesListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, bluetoothScanner.devicesList)
        beaconsList.adapter = devicesListAdapter
        bluetoothScanner.scanLeDevice(true, devicesListAdapter)
    }
}
