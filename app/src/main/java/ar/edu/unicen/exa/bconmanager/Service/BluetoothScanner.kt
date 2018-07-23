package ar.edu.unicen.exa.bconmanager.Service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.BaseAdapter
import ar.edu.unicen.exa.bconmanager.Adapters.BeaconsAdapter
import ar.edu.unicen.exa.bconmanager.Controller.FindMeActivity
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import kotlinx.android.synthetic.main.activity_my_beacons.*


class BluetoothScanner : AppCompatActivity() {

    private val TAG = "BluetoothScanner"
    private val REFRESH_RATE = 70

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothHandler: Handler = Handler()
    private var isScanning: Boolean = false
    private var refreshCounter = 0

    lateinit var devicesListAdapter: BaseAdapter
    var devicesList = mutableListOf<BeaconDevice>()
    var isRunningOnBackground = false

    private object Holder {
        val INSTANCE = BluetoothScanner()
    }

    companion object {
        val instance: BluetoothScanner by lazy { Holder.INSTANCE }
    }

    /**
     * Activity for scanning and displaying available BLE devices.
     */

    fun scanLeDevice(enable: Boolean, adapter: BaseAdapter) {
        devicesListAdapter = adapter

        if (enable) {
            Log.d(TAG, "Starting BluetoothLowEnergy scan")
            // Stops scanning after a pre-defined scan period.
            isScanning = true
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
                    detectedBeacon.txPower = -65
                } //  -63 a 1m
                device.address.startsWith("D3:B5") -> {
                    detectedBeacon.name = "Social Retail"
                    detectedBeacon.txPower = -62
                } // -75 a 1m
                device.address.startsWith("C1:31") -> {
                    detectedBeacon.name = "iBKS"
                    detectedBeacon.txPower = -60
                }
                device.address.startsWith("DF:B5:15:8C:D8:35") -> {
                    detectedBeacon.name = "iBKS2"
                    detectedBeacon.txPower = -50
                }
                else -> detectedBeacon.name = "Unknown"
            }

            if (!detectedBeacon.name.equals("Unknown")) {
                refreshCounter++
                if (!devicesList.contains(detectedBeacon)) {
                    devicesList.add(detectedBeacon)
                    detectedBeacon.calculateDistance(rssi)
                } else {
                    val index = devicesList.indexOf(detectedBeacon)
                    devicesList[index].intensity = rssi
                    devicesList[index].calculateDistance(rssi)
                    devicesList[index].txPower = detectedBeacon.txPower
                }
                //Log.d("Beacon", "Refresh counter is $refreshCounter")
                devicesListAdapter.notifyDataSetChanged()
                //clearAverages()
            }
        }
    }

    /**
     * Used when running on background and returning to the map screen
     */
    fun changeContext(context: FindMeActivity) {
        (devicesListAdapter as BeaconsAdapter).context = context
    }

    /**
     * Refreshes the counters for each beacon
     */
    fun clearAverages() {
        if (refreshCounter == REFRESH_RATE) {
            Log.d("Beacon", "Entro a borrar todo $refreshCounter")
            refreshCounter = 0
            devicesList.forEach { it.cleanAverages() }
        }
    }
}

