package ar.edu.unicen.exa.bconmanager

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log

class BluetoothScanner  : AppCompatActivity() {

    private var mScanning:Boolean = false
    private val mHandler: Handler = Handler()
    private val SCAN_PERIOD:Long = 10000

    val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    var mArrayAdapter = mutableListOf<BluetoothDevice>()

    /**
    * Activity for scanning and displaying available BLE devices.
    */

    fun scanLeDevice(enable:Boolean) {
        if (enable)
        {
            Log.d("BLE", "Starting")
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(object:Runnable {
                override fun run() {
                    Log.d("BLE", "Stopping")
                    mScanning = false
                    mBluetoothAdapter.stopLeScan(mLeScanCallback)
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
    }

    var mLeScanCallback = object:BluetoothAdapter.LeScanCallback {
        override fun onLeScan(device: BluetoothDevice, rssi:Int,
                              scanRecord:ByteArray) {
            runOnUiThread(object:Runnable {
                override fun run() {
                    if (!mArrayAdapter.contains(device)) {
                        mArrayAdapter.add(device)
                        Log.d("MAC", device.address)
                    }
                }
            })
        }
    }
}