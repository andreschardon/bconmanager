package ar.edu.unicen.exa.bconmanager.Controller

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import ar.edu.unicen.exa.bconmanager.Adapters.BeaconsAdapter
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import ar.edu.unicen.exa.bconmanager.Model.BeaconOnMap
import ar.edu.unicen.exa.bconmanager.Model.Location
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.R.drawable.beacon_icon
import ar.edu.unicen.exa.bconmanager.Service.BluetoothScanner
import kotlinx.android.synthetic.main.activity_find_me.*
import kotlinx.android.synthetic.main.activity_my_beacons.*


class FindMeActivity : AppCompatActivity() {

    private val bluetoothScanner = BluetoothScanner()
    lateinit var devicesListAdapter : BeaconsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_me)

        // TEST: Creating a test beacon and displaying it
        val testBeacon = BeaconOnMap(Location(500.toDouble(), 300.toDouble()), BeaconDevice("AA:BB:CC", 80, null))
        testBeacon.image = beacon_icon

        setupBeacon(testBeacon)

        devicesListAdapter = BeaconsAdapter(this, bluetoothScanner.devicesList)
        bluetoothScanner.scanLeDevice(true, devicesListAdapter)



    }

    private fun setupBeacon(testBeacon: BeaconOnMap) {

        // Set up the beacon's image size and position
        val imageView = ImageView(this)
        val layoutParams = LinearLayout.LayoutParams(100, 100) // value is in pixels
        layoutParams.leftMargin = testBeacon.position.x.toInt() - 50
        layoutParams.topMargin = testBeacon.position.y.toInt() - 50
        imageView.setImageResource(testBeacon.image!!)

        // Add ImageView to LinearLayout
        floorLayout.addView(imageView, layoutParams)
    }

    fun refreshButtonClicked(view: View) {
        bluetoothScanner.scanLeDevice(true, devicesListAdapter)
    }
}
