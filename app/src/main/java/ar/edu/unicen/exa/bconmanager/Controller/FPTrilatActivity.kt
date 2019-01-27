package ar.edu.unicen.exa.bconmanager.Controller

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import ar.edu.unicen.exa.bconmanager.Adapters.FingerprintOnlineAdapter
import ar.edu.unicen.exa.bconmanager.Model.*
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.Algorithm.FPTrilat
import kotlinx.android.synthetic.main.activity_fingerprint_offline.*
import kotlin.math.floor

class FPTrilatActivity: OnMapActivity() {

    private lateinit var devicesListOnlineAdapter: FingerprintOnlineAdapter
    private var currentFingerprintingZone: FingerprintZone? = null
    private val fpTrilat = FPTrilat()
    lateinit var currentPosition: PositionOnMap
    lateinit var positionView: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fptrilat)
        val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        val intent: Intent
        chooseFile.type = "application/octet-stream" //as close to only Json as possible
        intent = Intent.createChooser(chooseFile, "Choose a file")
        startActivityForResult(intent, 101)

    }

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
        fpTrilat.startUp(floorMap)
        devicesListOnlineAdapter = FingerprintOnlineAdapter(this, bluetoothScanner.devicesList)
        bluetoothScanner.scanLeDevice(true, devicesListOnlineAdapter)

        // Starting point
        currentPosition = PositionOnMap(Location(-1.0, -1.0, floorMap))
        currentPosition.image = R.drawable.location_icon
        positionView = ImageView(this)
        setupResource(currentPosition, positionView)
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
            var mapedBeacons: MutableList<BeaconOnMap> = mutableListOf()
            var sBeacons: MutableList<BeaconOnMap> = floorMap.savedBeacons
            var i = 0
            for(b in beacons) {
                while (!b.address.equals(sBeacons[i].beacon.address)) {
                    i++
                }
                if(b.address.equals(sBeacons[i].beacon.address)) {
                    val bMap = BeaconOnMap(sBeacons[i].position,b)
                    mapedBeacons.add(bMap)
                    i = 0
                }
            }
            currentPosition.position = fpTrilat.getNextPoint(mapedBeacons)
            val layoutParams = RelativeLayout.LayoutParams(70, 70) // value is in pixels
            currentPosition.position = validatePosition(currentPosition.position)
            layoutParams.leftMargin = currentPosition.position.getX() - 35
            layoutParams.topMargin = currentPosition.position.getY() - 35
            positionView.layoutParams = layoutParams
            currentFingerprintingZone = fpTrilat.getCurrentZone()
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

    private fun validatePosition(newPosition : Location): Location {
        return floorMap.restrictPosition(PositionOnMap(newPosition)).position
    }

}