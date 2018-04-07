package ar.edu.unicen.exa.bconmanager.Service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.ArrayAdapter
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice

class BluetoothScanner  : AppCompatActivity() {

    private var mScanning:Boolean = false
    private val mHandler: Handler = Handler()
    private val SCAN_PERIOD:Long = 1000000

    var devicesList = mutableListOf<BeaconDevice>()
    lateinit var devicesListAdapter : ArrayAdapter<BeaconDevice>
    val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    /**
    * Activity for scanning and displaying available BLE devices.
    */

    fun scanLeDevice(enable:Boolean, adapter: ArrayAdapter<BeaconDevice>)  {
        devicesListAdapter = adapter
        if (enable)
        {
            Log.d("BLE", "Starting")
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(object:Runnable {
                override fun run() {
                    Log.d("BLE", "Stopping")
                    mScanning = false
                    mBluetoothAdapter.stopLeScan(mLeScanCallback)
                    doSomething()
                }
            }, SCAN_PERIOD)
            mScanning = true
            mBluetoothAdapter.startLeScan(mLeScanCallback)
        }
        else
        {
            mScanning = false
            mBluetoothAdapter.stopLeScan(mLeScanCallback)
        }

        //val bluetoothDevice = mBluetoothAdapter.getRemoteDevice(beacon.getAddress())
    }

    fun doSomething() {
        Log.d("BLE-END", "It finished scanning, print the list")
        Log.d("ARRAY: ", devicesList.toString())
    }

    var mLeScanCallback = object:BluetoothAdapter.LeScanCallback {
        override fun onLeScan(device: BluetoothDevice, rssi:Int,
                              scanRecord:ByteArray) {
            runOnUiThread(object:Runnable {
                override fun run() {
                    val detectedBeacon = BeaconDevice(device.address, rssi, device)
                    if (!devicesList.contains(detectedBeacon)) {
                        devicesList.add(detectedBeacon)
                        devicesListAdapter.notifyDataSetChanged()
                    } else {
                        val index = devicesList.indexOf(detectedBeacon)
                        devicesList[index].intensity = rssi
                        devicesListAdapter.notifyDataSetChanged()
                    }
                    Log.d("MAC", device.address)
                    Log.d("RSSI", rssi.toString())
                }
            })
        }
    }
}