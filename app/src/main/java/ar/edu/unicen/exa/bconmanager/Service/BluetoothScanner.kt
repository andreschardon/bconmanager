package ar.edu.unicen.exa.bconmanager.Service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.BaseAdapter
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import kotlinx.android.synthetic.main.activity_my_beacons.*


class BluetoothScanner  : AppCompatActivity() {

    private val SCAN_PERIOD = 10000L
    private val TAG = "BluetoothScanner"

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothHandler: Handler = Handler()
    private var isScanning:Boolean = false

    lateinit var devicesListAdapter : BaseAdapter
    var devicesList = mutableListOf<BeaconDevice>()

    /**
    * Activity for scanning and displaying available BLE devices.
    */

    fun scanLeDevice(enable:Boolean, adapter: BaseAdapter)  {
        devicesListAdapter = adapter

        if (enable) {
            Log.d(TAG, "Starting BluetoothLowEnergy scan")
            // Stops scanning after a pre-defined scan period.
            bluetoothHandler.postDelayed(object:Runnable {
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
    }

//    fun doSomething() {
//        Log.d("BLE-END", "It finished scanning, print the list")
//        Log.d("ARRAY: ", devicesList.toString())
//    }


    var mLeScanCallback = object:BluetoothAdapter.LeScanCallback {


        override fun onLeScan(device: BluetoothDevice, rssi:Int,
                              scanRecord:ByteArray) {
            runOnUiThread(object:Runnable {
                override fun run() {
                    val detectedBeacon = BeaconDevice(device.address, rssi, device)
                    //val approx : Double =  detectedBeacon.calculateDistance(rssi)
                    //detectedBeacon.approxDistance = approx


                    // Hard-coded, this should be removed later
                    when {
                        device.address.startsWith("0C:F3") -> {
                            detectedBeacon.name = "EM Micro"
                            detectedBeacon.txPower = -68
                        } //  -63 a 1m
                        device.address.startsWith("D3:B5") -> {
                            detectedBeacon.name = "Social Retail"
                            detectedBeacon.txPower = -75
                        } // -75 a 1m
                        device.address.startsWith("C1:31") -> {
                            detectedBeacon.name = "iBKS"
                            detectedBeacon.txPower = -45 //Default, TO DO
                        }
                        else -> detectedBeacon.name = "Unknown"
                    }

//                    val data = AdvertiseData.Builder()
//                            .addServiceUuid(ParcelUuid
//                                    .fromString(UUID
//                                            .nameUUIDFromBytes(scanRecord).toString())).build()
//                    Log.d(TAG, data.toString())

                    if (!detectedBeacon.name.equals("Unknown")) {
//                        val record= ScanRecordParser.ParseRecord(scanRecord)
//                        Log.d(TAG, ScanRecordParser.getServiceUUID(record))


                        if (!devicesList.contains(detectedBeacon)) {
                            devicesList.add(detectedBeacon)
                            detectedBeacon.calculateDistance(rssi)
                            devicesListAdapter.notifyDataSetChanged()
                        } else {
                            val index = devicesList.indexOf(detectedBeacon)
                            devicesList[index].intensity = rssi
                            devicesList[index].calculateDistance(rssi)
                            devicesListAdapter.notifyDataSetChanged()
                        }
                    }



                }
            })
        }
    }
}

