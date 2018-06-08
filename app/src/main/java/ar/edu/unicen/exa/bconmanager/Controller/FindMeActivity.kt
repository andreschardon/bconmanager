package ar.edu.unicen.exa.bconmanager.Controller

import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Bundle
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.getExternalStoragePublicDirectory
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import ar.edu.unicen.exa.bconmanager.Adapters.BeaconsAdapter
import ar.edu.unicen.exa.bconmanager.Model.*
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.R.drawable.*
import ar.edu.unicen.exa.bconmanager.Service.BluetoothScanner
import ar.edu.unicen.exa.bconmanager.Service.JsonUtility
import ar.edu.unicen.exa.bconmanager.Service.TrilaterationCalculator
import kotlinx.android.synthetic.main.activity_find_me.*


class FindMeActivity : AppCompatActivity() {

    private val TAG = "FindMeActivity"
    private val bluetoothScanner = BluetoothScanner()
    private val downloadsDirectory = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).absolutePath
    private lateinit var floorMap : CustomMap
    lateinit var devicesListAdapter : BeaconsAdapter
    lateinit var positionView : ImageView
    lateinit var currentPosition : PositionOnMap
    private var trilaterationCalculator = TrilaterationCalculator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_me)

        // Loading the map from a JSON file
        //floorMap = loadMapFromFile("$downloadsDirectory/myRoom.json")

        // OR creating it right here
        floorMap = createTestMap()

        // AND saving it to a JSON file
        saveMapToFile(floorMap, "$downloadsDirectory/myRoom.json")

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
            setupResource(beacon,imageView)
        }

        // Drawing all the points of interest for this map
        for (point in floorMap.pointsOfInterest) {
            val imageView = ImageView(this)
            setupResource(point, imageView)
        }
        // Starting point
        currentPosition = PositionOnMap(Location(2.0, 2.0, floorMap))
        currentPosition.image = location_icon
        positionView = ImageView(this)
        setupResource(currentPosition, positionView)

        // Scanning beacons
        floorMap.savedBeacons.forEach { bluetoothScanner.devicesList.add(it.beacon)  }
        devicesListAdapter = BeaconsAdapter(this, bluetoothScanner.devicesList)
        bluetoothScanner.scanLeDevice(true, devicesListAdapter)




    }

    override fun onPause() {
        super.onPause()
        bluetoothScanner.stopScan()
    }

    private fun createTestMap() : CustomMap {
        // Creating a test map
        val testMap = CustomMap("$downloadsDirectory/TestPic.jpg", 6.3, 9.75) // in meters

        // TEST: Creating a test beacon and displaying it
        val testBeacon = BeaconOnMap(Location(1.2, 9.49, testMap), BeaconDevice("D3:B5:67:2B:92:DA", 80, null))
        testBeacon.beacon.name = "Social Retail"
        testBeacon.image = beacon_icon
        testMap.addBeacon(testBeacon)

        // TEST: Creating a second test beacon and displaying it
        val testBeacon2 = BeaconOnMap(Location(5.77, 4.23, testMap), BeaconDevice("C1:31:86:2A:30:62", 80, null))
        testBeacon2.beacon.name = "iBKS"
        testBeacon2.image = beacon_icon
        testMap.addBeacon(testBeacon2)

        // TEST: Creating a third test beacon and displaying it
        val testBeacon3 = BeaconOnMap(Location(2.5, 2.5, testMap), BeaconDevice("0C:F3:EE:0D:84:50", 80, null))
        testBeacon3.beacon.name = "EM Micro"
        testBeacon3.image = beacon_icon
        testMap.addBeacon(testBeacon3)


        // TEST: Creating a interest point and displaying it
        val Room1 = PointOfInterest(Location(2.0,1.0,testMap),1.6)
        Room1.image = zone_icon
        testMap.addPoI(Room1)

        // TEST: Creating a interest point and displaying it
        val Living =  PointOfInterest(Location(3.0,1.0,testMap),1.6)
        Living.image = zone_icon
        testMap.addPoI(Living)


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

    private fun setupResource(resource: Resource, imageView: ImageView) {

        // Set up the resource image size and position
        val layoutParams : LinearLayout.LayoutParams
        if (resource is PointOfInterest) {

            val loc = Location(resource.zone, resource.zone, floorMap)
            layoutParams= LinearLayout.LayoutParams(loc.getX(), loc.getY()) // value is in pixels


        }
        else {
            layoutParams = LinearLayout.LayoutParams(70, 70) // value is in pixels
        }
        layoutParams.leftMargin = resource.position.getX() - (layoutParams.width/2)
        layoutParams.topMargin = resource.position.getY() - (layoutParams.height /2)
        imageView.setImageResource(resource.image!!)

        // Add ImageView to LinearLayout
        floorLayout.addView(imageView, layoutParams)

    }



    fun refreshButtonClicked(view: View) {
        // For now we don't need this
        bluetoothScanner.scanLeDevice(true, devicesListAdapter)

        //trilateratePosition()
    }

    fun updatePosition() {
        Log.d(TAG, "Updating current position")
        val layoutParams = RelativeLayout.LayoutParams(70, 70) // value is in pixels
        layoutParams.leftMargin = currentPosition.position.getX() - 35
        layoutParams.topMargin = currentPosition.position.getY() - 35
        Log.d("POSITION VIEW UPDATE","${positionView.layoutParams}")
        positionView.layoutParams = layoutParams



        // Demo to obtain current distance to a particular beacon
        //Log.d("DISTANCE NOW", "${floorMap.savedBeacons.get(0).beacon.approxDistance}")

    }

    fun threeClosestBeacons() : List<BeaconOnMap> {
        // TO DO
        var closestList = mutableListOf<BeaconOnMap>()
        return closestList
    }

    fun trilateratePosition() {
        // Call this after currentPosition's x and y are updated
        val resultLocation = trilaterationCalculator.getPositionInMap(floorMap)
        if (resultLocation != null) {
            currentPosition.position.x = resultLocation.x
            currentPosition.position.y = resultLocation.y
            updatePosition()
        }
    }

}
