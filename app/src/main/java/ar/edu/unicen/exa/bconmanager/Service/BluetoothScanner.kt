package ar.edu.unicen.exa.bconmanager.Service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.ArrayAdapter
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import java.math.BigDecimal

class BluetoothScanner  : AppCompatActivity() {

    private val SCAN_PERIOD = 10000L
    private val TAG = "BluetoothScanner"

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var isScanning:Boolean = false
    private val mHandler: Handler = Handler()

    lateinit var devicesListAdapter : ArrayAdapter<BeaconDevice>
    var devicesList = mutableListOf<BeaconDevice>()

    /**
    * Activity for scanning and displaying available BLE devices.
    */

    fun scanLeDevice(enable:Boolean, adapter: ArrayAdapter<BeaconDevice>)  {
        devicesListAdapter = adapter

        if (enable) {
            Log.d(TAG, "Starting BluetoothLowEnergy scan")
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(object:Runnable {
                override fun run() {
                    Log.d(TAG, "Stopping BluetoothLowEnergy scan")
                    isScanning = false
                    bluetoothAdapter.stopLeScan(mLeScanCallback)
                    //doSomething()
                }
            }, SCAN_PERIOD)
            isScanning = true
            bluetoothAdapter.startLeScan(mLeScanCallback)
        }
        else {
            isScanning = false
            bluetoothAdapter.stopLeScan(mLeScanCallback)
        }

        //val bluetoothDevice = mBluetoothAdapter.getRemoteDevice(beacon.getAddress())
    }

//    fun doSomething() {
//        Log.d("BLE-END", "It finished scanning, print the list")
//        Log.d("ARRAY: ", devicesList.toString())
//    }

    fun Double.roundTo2DecimalPlaces() =
            BigDecimal(this).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()

    var mLeScanCallback = object:BluetoothAdapter.LeScanCallback {
        override fun onLeScan(device: BluetoothDevice, rssi:Int,
                              scanRecord:ByteArray) {
            runOnUiThread(object:Runnable {
                override fun run() {

                    val detectedBeacon = BeaconDevice(device.address, rssi, device)
                    val approx : Double =  ((Math.pow(10.toDouble(), ((2f-rssi)/30f).toDouble()))/100).roundTo2DecimalPlaces()
                    detectedBeacon.approxDistance = approx


                    // Hard-coded, this should be removed later
                    when {
                        device.address.startsWith("0C:F3") -> detectedBeacon.name = "EM Micro"
                        device.address.startsWith("D3:B5") -> detectedBeacon.name = "Social Retail"
                        device.address.startsWith("C1:31") -> detectedBeacon.name = "iBKS"
                        else -> detectedBeacon.name = "Unknown"
                    }

                    if (!devicesList.contains(detectedBeacon)) {
                        devicesList.add(detectedBeacon)
                        devicesListAdapter.notifyDataSetChanged()
                    } else {
                        val index = devicesList.indexOf(detectedBeacon)
                        devicesList[index].intensity = rssi
                        devicesList[index].approxDistance = approx
                        devicesListAdapter.notifyDataSetChanged()
                    }
                }
            })
        }
    }
}