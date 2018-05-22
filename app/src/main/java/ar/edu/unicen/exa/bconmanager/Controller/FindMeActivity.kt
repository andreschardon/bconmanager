package ar.edu.unicen.exa.bconmanager.Controller

import android.graphics.Point
import android.os.Bundle
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.getExternalStoragePublicDirectory
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import ar.edu.unicen.exa.bconmanager.Adapters.BeaconsAdapter
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import ar.edu.unicen.exa.bconmanager.Model.BeaconOnMap
import ar.edu.unicen.exa.bconmanager.Model.CustomMap
import ar.edu.unicen.exa.bconmanager.Model.Location
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.R.drawable.beacon_icon
import ar.edu.unicen.exa.bconmanager.R.drawable.floor_plan
import ar.edu.unicen.exa.bconmanager.R.id.floorLayout
import ar.edu.unicen.exa.bconmanager.R.id.floorPlan
import ar.edu.unicen.exa.bconmanager.Service.BluetoothScanner
import ar.edu.unicen.exa.bconmanager.Service.JsonUtility
import kotlinx.android.synthetic.main.activity_find_me.*
import android.graphics.BitmapFactory
import android.graphics.Bitmap




class FindMeActivity : AppCompatActivity() {

    private val TAG = "FindMeActivity"
    private val bluetoothScanner = BluetoothScanner()
    private val downloadsDirectory = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).absolutePath
    private lateinit var floorMap : CustomMap
    lateinit var devicesListAdapter : BeaconsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_me)

        // Loading the map from a JSON file
        floorMap = loadMapFromFile("$downloadsDirectory/myRoom.json")

        // OR creating it right here
        //floorMap = createTestMap()

        // AND saving it to a JSON file
        //saveMapToFile(floorMap, "$downloadsDirectory/myRoom.json")

        // Drawing the map's image
        val bitmap = BitmapFactory.decodeFile(floorMap.image)
        val img = findViewById<View>(R.id.floorPlan) as ImageView
        img.setImageBitmap(bitmap)

        // Obtain real width and height of the map
        val mapSize = getRealMapSize()
        floorMap.calculateRatio(mapSize.x, mapSize.y)

        // Drawing all the beacons for this map
        for (beacon in floorMap.savedBeacons) {
            setupBeacon(beacon)
        }

        // Scanning beacons
        devicesListAdapter = BeaconsAdapter(this, bluetoothScanner.devicesList)
        bluetoothScanner.scanLeDevice(true, devicesListAdapter)
    }

    private fun createTestMap() : CustomMap {
        // Creating a test map
        val testMap = CustomMap("$downloadsDirectory/TestPic.jpg", 3.3, 3.45) // in meters

        // TEST: Creating a test beacon and displaying it
        val testBeacon = BeaconOnMap(Location(2.0, 0.0, testMap), BeaconDevice("D3:B5:67:2B:92:DA", 80, null))
        testBeacon.image = beacon_icon
        testMap.addBeacon(testBeacon)

        // TEST: Creating a second test beacon and displaying it
        val testBeacon2 = BeaconOnMap(Location(0.0, 3.0, testMap), BeaconDevice("C1:31:86:2A:30:62", 80, null))
        testBeacon2.image = beacon_icon
        testMap.addBeacon(testBeacon2)

        // TEST: Creating a third test beacon and displaying it
        val testBeacon3 = BeaconOnMap(Location(2.4, 1.5, testMap), BeaconDevice("0C:F3:EE:0D:84:50", 80, null))
        testBeacon3.image = beacon_icon
        testMap.addBeacon(testBeacon3)
        return testMap
    }

    private fun saveMapToFile(testMap: CustomMap, filePath: String) {
        val jsonMap = testMap.toJson()
        JsonUtility.saveToFile(filePath, jsonMap)
        Log.d(TAG, "Map saved to JSON file in $filePath")
    }

    private fun loadMapFromFile(filePath : String) : CustomMap {
        val jsonMap = JsonUtility.readFromFile(filePath)
        val fileMap = CustomMap("", 0.0, 0.0)
        fileMap.startFromFile(jsonMap)
        Log.d(TAG, "Map loaded from JSON file in $filePath")
        return fileMap
    }

    private fun getRealMapSize() : Point {
        val realSize = Point()
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        val width = size.x
        realSize.x = width
        realSize.y = floorPlan.drawable.intrinsicHeight * width / floorPlan.drawable.intrinsicWidth
        return realSize
    }

    private fun printDisplayProperties() {
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width = size.x
        val height = size.y
        Log.d("POSITION", "TOTAL WIDTH IS : $width")
        Log.d("POSITION", "TOTAL HEIGHT IS : $height")
        Log.d("POSITION", "-----------------------------")
        Log.d("POSITION", "WIDTH IS : ${floorPlan.drawable.intrinsicWidth}")
        Log.d("POSITION", "HEIGHT IS :  ${floorPlan.drawable.intrinsicHeight}")
        val real_width = width
        val real_height = floorPlan.drawable.intrinsicHeight * width / floorPlan.drawable.intrinsicWidth
        Log.d("POSITION", "-----------------------------")
        Log.d("POSITION", "WIDTH IS : $width")
        Log.d("POSITION", "HEIGHT IS :  ${real_height}")
        Log.d("POSITION", "-----------------------------")
    }

    private fun setupBeacon(testBeacon: BeaconOnMap) {

        // Set up the beacon's image size and position
        val imageView = ImageView(this)
        val layoutParams = LinearLayout.LayoutParams(100, 100) // value is in pixels

        layoutParams.leftMargin = testBeacon.position.getX() - 50
        layoutParams.topMargin = testBeacon.position.getY() - 50
        imageView.setImageResource(testBeacon.image!!)

        // Add ImageView to LinearLayout
        floorLayout.addView(imageView, layoutParams)
    }

    fun refreshButtonClicked(view: View) {
        bluetoothScanner.scanLeDevice(true, devicesListAdapter)
    }

}
