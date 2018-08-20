package ar.edu.unicen.exa.bconmanager.Controller

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import ar.edu.unicen.exa.bconmanager.Adapters.FingerprintAdapter
import ar.edu.unicen.exa.bconmanager.Model.*
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.BluetoothScanner
import ar.edu.unicen.exa.bconmanager.Service.JsonUtility
import kotlinx.android.synthetic.main.activity_offline.*
import java.math.BigDecimal


class OfflineActivity : AppCompatActivity() {

    private val TAG = "OfflineActivity"
    private val bluetoothScanner = BluetoothScanner.instance
    private var filePath: String = ""

    private lateinit var floorMap: CustomMap
    private var currentFingerprintingZone : FingerprintZone? = null
    lateinit var devicesListAdapter: FingerprintAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offline)
        startBtn.isEnabled = false
        deleteBtn.isEnabled = false
        createBtn.isEnabled = false
        val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        val intent: Intent
        chooseFile.type = "application/octet-stream" //as close to only Json as possible
        intent = Intent.createChooser(chooseFile, "Choose a file")
        startActivityForResult(intent, 101)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            101 -> if (resultCode == -1) {
                val uri = data!!.data
                filePath = uri.lastPathSegment.removePrefix("raw:")
            }
        }
        if (!filePath.isNullOrEmpty()) {
            displayMap()
        } else {
            Log.e(TAG, "The file path is incorrect")
        }
    }

    /**
     * This method is called after getting the json file's path
     * It displays the map's image, the beacons and the current location
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun displayMap() {

        // This method will create a test map on the downloads directory.
        // Make sure the TestPic.jpg is on the same location
        //createTestMap()
        Log.d("FILEPATH",filePath)
        // Loading the map from a JSON file
        floorMap = loadMapFromFile(filePath)

        // Drawing the map's image
        val bitmap = BitmapFactory.decodeFile(floorMap.image)
        val img = findViewById<View>(R.id.floorPlan) as ImageView
        img.setImageBitmap(bitmap)
        img.setOnTouchListener { v, event ->
            val screenX = event.x
            val screenY = event.y
            val viewX = screenX - v.left
            val viewY = screenY - v.top

            startBtn.isEnabled = false
            deleteBtn.isEnabled = false
            createBtn.isEnabled = false

            // check if point exists
            // if it does, click it
            var touchedPoint : FingerprintZone? = null
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

        /**
        // Starting point
        currentPosition = PositionOnMap(Location(-1.0, -1.0, floorMap))
        currentPosition.image = R.drawable.location_icon
        positionView = ImageView(this)
        setupResource(currentPosition, positionView)

        // Scanning beacons

        if (!bluetoothScanner.isRunningOnBackground) {
            floorMap.savedBeacons.forEach { bluetoothScanner.devicesList.add(it.beacon) }
            devicesListAdapter = BeaconsAdapter(this, bluetoothScanner.devicesList)
            bluetoothScanner.scanLeDevice(true, devicesListAdapter)
        } else {
            floorMap.savedBeacons.forEach { restoreBeacon(it, bluetoothScanner.devicesList) }
            bluetoothScanner.changeContext(this)
            devicesListAdapter = (bluetoothScanner.devicesListAdapter as BeaconsAdapter)
        }
        **/
    }

    private fun openFingerprintingMenu(touchedZone: FingerprintZone) {
        if (currentFingerprintingZone != null && !floorMap.fingerprintZones.contains(currentFingerprintingZone!!)) {
            floorLayout.removeView(currentFingerprintingZone!!.view)
        }
        currentFingerprintingZone = touchedZone
        // color is blue
        if (touchedZone.hasData) {
            // is green
            deleteBtn.isEnabled = true
        } else {
            // is red
            deleteBtn.isEnabled = true
            startBtn.isEnabled = true
        }

    }


    private fun createFingerprintingPoint(viewX: Float, viewY: Float) {
        if (currentFingerprintingZone != null && !floorMap.fingerprintZones.contains(currentFingerprintingZone!!)) {
            floorLayout.removeView(currentFingerprintingZone!!.view)
        }
        val loc = Location(0.0, 0.0, floorMap)
        loc.setX(viewX.toInt())
        loc.setY(viewY.toInt())
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

    private fun createTestMap() {
        // Creating a test map
        val testMap = CustomMap("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath}/TestPic.jpg", 6.3, 9.75) // in meters

        // TEST: Creating a test beacon and displaying it
        val testBeacon = BeaconOnMap(Location(1.2, 9.49, testMap), BeaconDevice("D3:B5:67:2B:92:DA", 80, null))
        testBeacon.beacon.name = "Social Retail"
        testBeacon.image = R.drawable.beacon_icon
        testMap.addBeacon(testBeacon)

        // TEST: Creating a second test beacon and displaying it
        val testBeacon2 = BeaconOnMap(Location(5.77, 4.23, testMap), BeaconDevice("C1:31:86:2A:30:62", 80, null))
        testBeacon2.beacon.name = "iBKS"
        testBeacon2.image = R.drawable.beacon_icon
        testMap.addBeacon(testBeacon2)

        // TEST: Creating a third test beacon and displaying it
        val testBeacon3 = BeaconOnMap(Location(2.5, 2.5, testMap), BeaconDevice("0C:F3:EE:0D:84:50", 80, null))
        testBeacon3.beacon.name = "EM Micro"
        testBeacon3.image = R.drawable.beacon_icon
        testMap.addBeacon(testBeacon3)


        // TEST: Creating a interest point and displaying it
        val Room1 = PointOfInterest(Location(2.0, 1.0, testMap), 1.6, "Room1", "Content for Room1")
        Room1.image = R.drawable.zone_icon
        testMap.addPoI(Room1)

        // TEST: Creating a interest point and displaying it
        val Living = PointOfInterest(Location(3.0, 1.0, testMap), 1.6, "Living", "Content for Living")
        Living.image = R.drawable.zone_icon
        testMap.addPoI(Living)

        saveMapToFile(testMap, "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath}/myRoom.json")

    }

    private fun saveMapToFile(testMap: CustomMap, filePath: String) {
        val jsonMap = testMap.toJson()
        JsonUtility.saveToFile(filePath, jsonMap)
        Log.d(TAG, "Map saved to JSON file in $filePath")
    }

    private fun loadMapFromFile(filePath: String): CustomMap {
        val jsonMap = JsonUtility.readFromFile(filePath)
        val fileMap = CustomMap("", 0.0, 0.0)
        fileMap.startFromFile(jsonMap)
        Log.d(TAG, "Map loaded from JSON file in $filePath")
        return fileMap
    }



    private fun getRealMapSize(): Point {
        val realSize = Point()
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        val width = size.x
        realSize.x = width
        realSize.y = floorPlan.drawable.intrinsicHeight * width / floorPlan.drawable.intrinsicWidth
        return realSize
    }

    private fun setupResource(resource: Resource, imageView: ImageView) {

        // Set up the resource image size and position
        val layoutParams: LinearLayout.LayoutParams
        if (resource is PointOfInterest) {
            val loc = Location(resource.zone * 2, resource.zone * 2, floorMap)
            layoutParams = LinearLayout.LayoutParams(loc.getX(), loc.getY()) // value is in pixel
        } else {
            layoutParams = LinearLayout.LayoutParams(70, 70) // value is in pixels
        }
        layoutParams.leftMargin = resource.position.getX() - (layoutParams.width / 2)
        layoutParams.topMargin = resource.position.getY() - (layoutParams.height / 2)
        imageView.setImageResource(resource.image!!)

        // Add ImageView to LinearLayout
        floorLayout.addView(imageView, layoutParams)

    }

    /**
     * Updates the color of the fingerprinting zones
     */
    private fun updateZone(zone : FingerprintZone) {
        val imageView = zone.view!!
        floorLayout.removeView(imageView)
        imageView.setImageResource(zone.image!!)
        val layoutParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(70, 70)
        // value is in pixels
        layoutParams.leftMargin = zone.position.getX() - (layoutParams.width / 2)
        layoutParams.topMargin = zone.position.getY() - (layoutParams.height / 2)
        floorLayout.addView(imageView, layoutParams)
    }

    fun Double.roundTo2DecimalPlaces() =
            BigDecimal(this).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()


    fun startFingerprint(view: View) {
        Log.d(TAG, "Current zone is $currentFingerprintingZone")
        createFingerprint(view)
        bluetoothScanner.devicesList.clear()
        floorMap.savedBeacons.forEach {
            it.beacon.cleanAverages()
            bluetoothScanner.devicesList.add(it.beacon)
        }
        devicesListAdapter = FingerprintAdapter(this, bluetoothScanner.devicesList)
        bluetoothScanner.scanLeDevice(true, devicesListAdapter, true)
        // Loading popup (display averages for each beacon?)
        deleteBtn.isEnabled = false
        createBtn.isEnabled = false
        startBtn.isEnabled = false
        saveBtn.isEnabled = false

    }

    fun createFingerprint(view : View) {
        Log.d(TAG, "Creating fingerpriting zone in $currentFingerprintingZone")
        if (!floorMap.fingerprintZones.contains(currentFingerprintingZone)) {
            floorMap.fingerprintZones.add(currentFingerprintingZone!!)
        }

    }

    fun deleteFingerprint(view : View) {
        Log.d(TAG, "Deleting fingerpriting zone in $currentFingerprintingZone")
        floorMap.fingerprintZones.remove(currentFingerprintingZone)
        floorLayout.removeView(currentFingerprintingZone!!.view)

        currentFingerprintingZone = null
        deleteBtn.isEnabled = false
        createBtn.isEnabled = false
        startBtn.isEnabled = false
    }

    fun finishFingerprint() {
        currentFingerprintingZone!!.updateFingerprints(devicesListAdapter.beacons)
        deleteBtn.isEnabled = true
        createBtn.isEnabled = false
        startBtn.isEnabled = true
        saveBtn.isEnabled = true
    }

    fun saveFingerprint(view : View) {
        saveMapToFile(floorMap, filePath)


    }

    
}

