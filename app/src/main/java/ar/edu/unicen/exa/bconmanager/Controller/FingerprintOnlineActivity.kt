package ar.edu.unicen.exa.bconmanager.Controller

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import ar.edu.unicen.exa.bconmanager.Adapters.FingerprintOnlineAdapter
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import ar.edu.unicen.exa.bconmanager.Model.FingerprintZone
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.FingerprintingService
import kotlinx.android.synthetic.main.activity_fingerprint_offline.*


class FingerprintOnlineActivity : OnMapActivity() {

    override var TAG = "FingerprintOnlineActivity"

    private lateinit var devicesListOnlineAdapter: FingerprintOnlineAdapter
    private var currentFingerprintingZone: FingerprintZone? = null
    private val fingerprinting = FingerprintingService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fingerprint_online)
        val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        val intent: Intent
        chooseFile.type = "application/octet-stream" //as close to only Json as possible
        intent = Intent.createChooser(chooseFile, "Choose a file")
        startActivityForResult(intent, 101)

    }

    /**
     * This method is called after getting the json file's path
     * It displays the map's image, the beacons and the current location
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun displayMap() {

        super.displayMap()

        // Drawing all the beacons for this map
        for (beacon in floorMap.savedBeacons) {
            val imageView = ImageView(this)
            setupResource(beacon, imageView)
        }

        // Drawing all the points of interest for this map
        for (point in floorMap.pointsOfInterest) {
            val imageView = ImageView(this)
            setupResource(point, imageView)
        }

        // Drawing all fingerprinting zones for this map
        for (zone in floorMap.fingerprintZones) {
            val imageView = ImageView(this)
            setupResource(zone, imageView)
        }
        fingerprinting.map = floorMap
        devicesListOnlineAdapter = FingerprintOnlineAdapter(this, bluetoothScanner.devicesList)
        bluetoothScanner.scanLeDevice(true, devicesListOnlineAdapter)

    }

    override fun onPause() {
        super.onPause()
        bluetoothScanner.stopScan()
        bluetoothScanner.devicesList = mutableListOf<BeaconDevice>()
    }

    override fun updatePosition(beacons: List<BeaconDevice>) {
        // Un-touch the previous zone
        if (currentFingerprintingZone != null) {
            currentFingerprintingZone!!.unTouch()
            updateZone(currentFingerprintingZone!!)
        }
        // Using the current RSSI of the beacons, we need to get the closest zone
        if (!floorMap.fingerprintZones.isEmpty()) {
            currentFingerprintingZone = fingerprinting.getCurrentZone(beacons)
            currentFingerprintingZone!!.touch()
            updateZone(currentFingerprintingZone!!)
        }
    }

    /**
     * Updates the color of the fingerprinting zones
     */
    private fun updateZone(zone: FingerprintZone) {
        val imageView = zone.view!!
        floorLayout.removeView(imageView)
        imageView.setImageResource(zone.image!!)
        val layoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(70, 70)
        // value is in pixels
        layoutParams.leftMargin = zone.position.getX() - (layoutParams.width / 2)
        layoutParams.topMargin = zone.position.getY() - (layoutParams.height / 2)
        floorLayout.addView(imageView, layoutParams)
    }
}

