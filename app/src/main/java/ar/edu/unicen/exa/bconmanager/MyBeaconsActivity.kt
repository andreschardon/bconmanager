package ar.edu.unicen.exa.bconmanager

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.IntentFilter
import android.os.Parcelable
import android.util.Log


class MyBeaconsActivity : AppCompatActivity() {

    val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    var mArrayAdapter = mutableListOf<String>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_beacons)
        scanBluetooth()
        Log.d("TAG1", mArrayAdapter.toString())
    }

    // Create a BroadcastReceiver for ACTION_FOUND
    private val mReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent:Intent) {
            Log.d("TAG1", " Entro aca")
            val action = intent.getAction()
            // When discovery finds a device
            BluetoothDevice.start
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                // Get the BluetoothDevice object from the Intent
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress())
                Log.d("TAG1", "Add device")
            }
        }
    }




    fun scanBluetooth() {
        if (!mBluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            val REQUEST_ENABLE_BT = 1
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        // Register the BroadcastReceiver
        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(mReceiver, filter) // Don't forget to unregister during onDestroy

    }
}
