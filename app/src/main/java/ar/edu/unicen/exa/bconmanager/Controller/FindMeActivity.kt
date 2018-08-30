package ar.edu.unicen.exa.bconmanager.Controller

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import ar.edu.unicen.exa.bconmanager.Adapters.FingerprintOfflineAdapter
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import ar.edu.unicen.exa.bconmanager.Model.FingerprintZone
import ar.edu.unicen.exa.bconmanager.Model.Location
import ar.edu.unicen.exa.bconmanager.R
import kotlinx.android.synthetic.main.activity_fingerprint_offline.*

class FindMeActivity : OnMapActivity() {

    override var TAG = "FindMeActivity"

    private lateinit var fingerprintScanDialog: AlertDialog
    private lateinit var devicesListOfflineAdapter: FingerprintOfflineAdapter
    private var currentFingerprintingZone: FingerprintZone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fingerprint_offline)
        startBtn.isEnabled = false
        deleteBtn.isEnabled = false
        createBtn.isEnabled = false
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
        img.setOnTouchListener { v, event ->
            val screenX = event.x
            val screenY = event.y
            val viewX = (screenX - v.left).toDouble().roundTo2DecimalPlaces()
            val viewY = (screenY - v.top).toDouble().roundTo2DecimalPlaces()


            startBtn.isEnabled = false
            deleteBtn.isEnabled = false
            createBtn.isEnabled = false

            // check if point exists
            // if it does, click it
            var touchedPoint: FingerprintZone? = null
            floorMap.fingerprintZones.forEach {
                if (it.isTouched(viewX, viewY) && touchedPoint == null) {
                    touchedPoint = it
                    it.touch()
                }
                updateZone(it)
            }
            // otherwise, create a new point there
            if (touchedPoint == null) {
                Log.d("ZONE", "Creating new point")
                createFingerprintingPoint(viewX, viewY)
            } else {
                Log.d("ZONE", "Opening point $touchedPoint")
                openFingerprintingMenu(touchedPoint!!)
            }
            false
        }

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
    }

    /**
     * Set as ´selected´ the touched fingerprinting zone and
     * enables the corresponding buttons (start, delete)
     */
    private fun openFingerprintingMenu(touchedZone: FingerprintZone) {
        // Removes the 'selected' status from previous selected point
        if (currentFingerprintingZone != null && !floorMap.fingerprintZones.contains(currentFingerprintingZone!!)) {
            floorLayout.removeView(currentFingerprintingZone!!.view)
        }
        currentFingerprintingZone = touchedZone
        if (touchedZone.hasData) {
            deleteBtn.isEnabled = true
        } else {
            deleteBtn.isEnabled = true
            startBtn.isEnabled = true
        }

    }

    /**
     * Sets the current point as a possible fingerprinting zone
     */
    private fun createFingerprintingPoint(viewX: Double, viewY: Double) {
        // Removes the 'selected' status from previous selected point
        if (currentFingerprintingZone != null && !floorMap.fingerprintZones.contains(currentFingerprintingZone!!)) {
            floorLayout.removeView(currentFingerprintingZone!!.view)
        }
        val loc = Location(0.0, 0.0, floorMap)
        loc.setX(viewX.toInt())
        loc.setY(viewY.toInt())
        loc.x = loc.x.roundTo2DecimalPlaces()
        loc.y = loc.y.roundTo2DecimalPlaces()
        val zone = FingerprintZone(loc)
        val imageView = ImageView(this)
        zone.view = imageView
        zone.image = R.drawable.finger_zone_blue
        setupResource(zone, imageView)
        createBtn.isEnabled = true
        deleteBtn.isEnabled = true
        startBtn.isEnabled = true
        currentFingerprintingZone = zone


    }

    override fun onPause() {
        super.onPause()
        bluetoothScanner.stopScan()
        bluetoothScanner.devicesList = mutableListOf<BeaconDevice>()
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

    /**
     * Starts the fingerprinting scan for the selected zone
     */
    fun startFingerprint(view: View) {
        Log.d(TAG, "Current zone is $currentFingerprintingZone")
        createFingerprint(view)
        bluetoothScanner.devicesList.clear()
        floorMap.savedBeacons.forEach {
            it.beacon.cleanAverages()
            bluetoothScanner.devicesList.add(it.beacon)
        }
        devicesListOfflineAdapter = FingerprintOfflineAdapter(this, bluetoothScanner.devicesList)
        bluetoothScanner.scanLeDevice(true, devicesListOfflineAdapter, true)
        // Loading popup (display averages for each beacon?)
        deleteBtn.isEnabled = false
        createBtn.isEnabled = false
        startBtn.isEnabled = false
        noBtn.isEnabled = false
        showDialog()

    }

    /**
     * Shows a popup dialog displaying the information of the current scan
     */
    private fun showDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("   ").setTitle("Obtaining data, please wait...")

        fingerprintScanDialog = builder.create()
        fingerprintScanDialog.show()
        fingerprintScanDialog.setMessage("   ")
    }

    /**
     * Creates a new fingerprinting zone
     */
    fun createFingerprint(view: View) {
        Log.d(TAG, "Creating fingerpriting zone in $currentFingerprintingZone")
        if (!floorMap.fingerprintZones.contains(currentFingerprintingZone)) {
            floorMap.fingerprintZones.add(currentFingerprintingZone!!)
        }

    }

    /**
     * Used to update the current fingerpriting dialog
     */
    fun updateFingerprintDialog(text: String) {
        Log.d(TAG, text)
        fingerprintScanDialog.setMessage(text)
    }

    /**
     * Deletes the current fingerprinting zone
     */
    fun deleteFingerprint(view: View) {
        Log.d(TAG, "Deleting fingerpriting zone in $currentFingerprintingZone")
        floorMap.fingerprintZones.remove(currentFingerprintingZone)
        floorLayout.removeView(currentFingerprintingZone!!.view)

        currentFingerprintingZone = null
        deleteBtn.isEnabled = false
        createBtn.isEnabled = false
        startBtn.isEnabled = false
    }

    /**
     * Fingerprinting data collection finished, save that data
     */
    fun finishFingerprint() {
        fingerprintScanDialog.hide()
        currentFingerprintingZone!!.updateFingerprints(devicesListOfflineAdapter.beacons)
        deleteBtn.isEnabled = true
        createBtn.isEnabled = false
        startBtn.isEnabled = true
        noBtn.isEnabled = true
    }

    /**
     * Saves all the fingerprinting information to the JSON file
     */
    fun saveFingerprint(view: View) {
        saveMapToFile(floorMap, filePath)
        Toast.makeText(this, "Saved to $filePath", Toast.LENGTH_SHORT).show()


    }

}
