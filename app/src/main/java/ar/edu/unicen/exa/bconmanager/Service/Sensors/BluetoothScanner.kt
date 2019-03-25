package ar.edu.unicen.exa.bconmanager.Service.Sensors

import android.bluetooth.BluetoothAdapter
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.BaseAdapter
import ar.edu.unicen.exa.bconmanager.Adapters.BeaconsAdapter
import ar.edu.unicen.exa.bconmanager.Controller.TrilaterationActivity
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice


class BluetoothScanner : AppCompatActivity() {

    private val TAG = "BluetoothScanner"

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothHandler: Handler = Handler()
    private val SCAN_PERIOD: Long = 20000 // 20 seconds

    lateinit var devicesListAdapter: BaseAdapter

    var devicesList = mutableListOf<BeaconDevice>()
    var isRunningOnBackground = false
    var isScanning: Boolean = false

    private object Holder {
        val INSTANCE = BluetoothScanner()
    }

    companion object {
        val instance: BluetoothScanner by lazy { Holder.INSTANCE }
    }

    /**
     * Activity for scanning and displaying available BLE devices.
     */

    fun scanLeDevice(enable: Boolean, adapter: BaseAdapter, isFingerprint: Boolean = false) {
        devicesListAdapter = adapter

        if (enable) {
            Log.d(TAG, "Starting BluetoothLowEnergy scan")
            // Stops scanning after a pre-defined scan period.
            isScanning = true
            if (isFingerprint) {
                bluetoothHandler.postDelayed({
                    isScanning = false
                    bluetoothAdapter.stopLeScan(mLeScanCallback)
                    Log.d("BLE", "It finished")
                    devicesListAdapter.notifyDataSetInvalidated()
                }, SCAN_PERIOD)
            }
            bluetoothAdapter.startLeScan(mLeScanCallback)
        } else {
            isScanning = false
            bluetoothAdapter.stopLeScan(mLeScanCallback)
        }
    }

    fun stopScan() {
        bluetoothHandler.post {
            Log.d(TAG, "Stopping BluetoothLowEnergy scan")
            isScanning = false
            bluetoothAdapter.stopLeScan(mLeScanCallback)
        }
    }


    var mLeScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        runOnUiThread {
            val detectedBeacon = BeaconDevice(device.address, rssi, device)


            // Hard-coded, this should be removed later
            when {
                device.address.startsWith("0C:F3") -> {
                    detectedBeacon.name = "EM Micro"
                    detectedBeacon.txPower = -55
                } //  -63 a 1m
                device.address.startsWith("D3:B5") -> {
                    detectedBeacon.name = "Social Retail"
                    detectedBeacon.txPower = -52
                } // -75 a 1m
                device.address.startsWith("C1:31") -> {
                    detectedBeacon.name = "iBKS"
                    detectedBeacon.txPower = -50 //60
                }
                device.address.startsWith("DF:B5") -> {
                    detectedBeacon.name = "iBKS2"
                    detectedBeacon.txPower = -50
                }
                else -> detectedBeacon.name = "Unknown"
            }

            //if (!detectedBeacon.name.equals("Unknown")) {
                if (!devicesList.contains(detectedBeacon)) {
                    devicesList.add(detectedBeacon)
                    detectedBeacon.calculateDistance(rssi)
                    detectedBeacon.intensity = rssi
                } else {
                    val index = devicesList.indexOf(detectedBeacon)
                    devicesList[index].intensity = rssi
                    devicesList[index].calculateDistance(rssi)
                    devicesList[index].txPower = detectedBeacon.txPower
                }
                Log.d("DATACOLLECT-BLE", detectedBeacon.toString())
                devicesListAdapter.notifyDataSetChanged()
                //clearAverages()
            //}
        }
    }

    /**
     * Used when running on background and returning to the map screen
     */
    fun changeContext(context: TrilaterationActivity) {
        (devicesListAdapter as BeaconsAdapter).context = context
    }

    fun printRssiCount() {
        devicesList.forEach {
            it.getHashMap()
        }
    }

}

