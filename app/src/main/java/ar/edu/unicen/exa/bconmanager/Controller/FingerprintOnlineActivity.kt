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
import kotlinx.android.synthetic.main.activity_fingerprint_offline.*


class FingerprintOnlineActivity : OnMapActivity() {

    override var TAG = "FingerprintOnlineActivity"

    private lateinit var devicesListOnlineAdapter: FingerprintOnlineAdapter
    private var currentFingerprintingZone: FingerprintZone? = null

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

        // Loading the map from a JSON file
        floorMap = loadMapFromFile(filePath)

        // Drawing the map's image
        val bitmap = BitmapFactory.decodeFile(floorMap.image)
        val img = findViewById<View>(R.id.floorPlan) as ImageView
        img.setImageBitmap(bitmap)

        // Obtain real width and height of the map
        val mapSize = getRealMapSize()
        floorMap.calculateRatio(mapSize.x, mapSize.y)

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
        devicesListOnlineAdapter = FingerprintOnlineAdapter(this, bluetoothScanner.devicesList)
        bluetoothScanner.scanLeDevice(true, devicesListOnlineAdapter)
    }

    override fun onPause() {
        super.onPause()
        bluetoothScanner.stopScan()
        bluetoothScanner.devicesList = mutableListOf<BeaconDevice>()
    }

    fun updatePosition(beacons: List<BeaconDevice>) {
        // Un-touch the previous zone
        if (currentFingerprintingZone != null) {
            currentFingerprintingZone!!.unTouch()
            updateZone(currentFingerprintingZone!!)
        }
        // Using the current RSSI of the beacons, we need to get the closest zone
        currentFingerprintingZone = bestZone(beacons, floorMap.fingerprintZones)
        currentFingerprintingZone!!.touch()
        updateZone(currentFingerprintingZone!!)
    }

    /**
     *
     */
    private fun bestZone(beacons: List<BeaconDevice>, fingerprintZones: MutableList<FingerprintZone>): FingerprintZone? {
        val fingerprintRating = mutableListOf<Double>()
        Log.d("RATINGS", beacons.toString())
        Log.d("RATINGS", fingerprintZones.toString())
        for (zone in fingerprintZones) {
            // For each fingerprinting zone, calculate the "rating"
            var differenceRating = 0.0
            zone.fingerprints.forEach {
                val index = beacons.indexOf(BeaconDevice(it.mac, 0, null))
                if (index != -1) {
                    val beacon = beacons.get(index)
                    differenceRating += Math.abs(beacon.averageRssi - it.rssi)
                }
            }
            fingerprintRating.add(differenceRating)
        }
        Log.d("RATINGS", fingerprintRating.toString())

        // Get the one with less rating
        val index = fingerprintRating.indexOf(fingerprintRating.min())
        Log.d("RATINGS", "$index")

        val bestZone = fingerprintZones.get(index)
        Log.d("RATINGS", "Best zone is $bestZone")

        return bestZone
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

