package ar.edu.unicen.exa.bconmanager.Controller

import android.app.Notification
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Bundle
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.getExternalStoragePublicDirectory
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import ar.edu.unicen.exa.bconmanager.Adapters.BeaconsAdapter
import ar.edu.unicen.exa.bconmanager.Model.*
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.R.drawable.*
import ar.edu.unicen.exa.bconmanager.Service.BluetoothScanner
import ar.edu.unicen.exa.bconmanager.Service.JsonUtility
import ar.edu.unicen.exa.bconmanager.Service.TrilaterationCalculator
import kotlinx.android.synthetic.main.activity_find_me.*
import java.math.BigDecimal
import java.util.*
import android.support.v4.app.NotificationManagerCompat
import ar.edu.unicen.exa.bconmanager.R.id.*
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import android.content.Context.NOTIFICATION_SERVICE
import android.app.PendingIntent
import android.content.Context


class FindMeActivity : AppCompatActivity() {

    private val TAG = "FindMeActivity"
    private val bluetoothScanner = BluetoothScanner.instance
    private var trilaterationCalculator = TrilaterationCalculator.instance
    private var filePath: String = ""
    private var drawQueue : Queue<Location> = ArrayDeque<Location>()

    private lateinit var floorMap: CustomMap
    lateinit var devicesListAdapter: BeaconsAdapter
    lateinit var positionView: ImageView
    lateinit var currentPosition: PositionOnMap
    private var inZoneOfInterest = false

    private lateinit var notificationManager : NotificationManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_me)
       if (!bluetoothScanner.isRunningOnBackground) {
           val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
           val intent: Intent
           chooseFile.type = "application/octet-stream" //as close to only Json as possible
           intent = Intent.createChooser(chooseFile, "Choose a file")
           startActivityForResult(intent, 101)
       } else {
           backgroundSwitch.toggle()
           val settings = getSharedPreferences(TAG, 0)
           filePath = settings.getString("filePath", "")
           Log.d("DESTROY", "Path is $filePath")
           displayMap()
       }
        setupNotifications()

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

    private fun setupNotifications() {

        // Create a notification builder for each point of interest
        for (point in floorMap.pointsOfInterest) {
            val mBuilder = NotificationCompat.Builder(this.applicationContext, "notify_001")
            val bigText = NotificationCompat.BigTextStyle()
            bigText.setBigContentTitle(point.id)
            bigText.setSummaryText(point.content)

            mBuilder.setSmallIcon(R.drawable.interest_icon)
            mBuilder.setContentTitle(point.id)
            mBuilder.setContentText(point.content)
            mBuilder.priority = Notification.PRIORITY_MAX
            mBuilder.setStyle(bigText)
            point.notification = mBuilder
        }

        // Set up notification manager
        notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("notify_001",
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * This method is called after getting the json file's path
     * It displays the map's image, the beacons and the current location
     */
    private fun displayMap() {

        // This method will create a test map on the downloads directory.
        // Make sure the TestPic.jpg is on the same location
        //createTestMap()

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
        // Starting point
        currentPosition = PositionOnMap(Location(-1.0, -1.0, floorMap))
        currentPosition.image = location_icon
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


    }

    private fun restoreBeacon(mapBeacon: BeaconOnMap, devicesList: MutableList<BeaconDevice>) {
        devicesList.forEach {
            if (it == mapBeacon.beacon) mapBeacon.beacon = it
        }
    }

    override fun onPause() {
        super.onPause()
        val settings = getSharedPreferences(TAG, 0)
        val editor = settings.edit()
        if (bluetoothScanner.isRunningOnBackground) {
            editor.putString("filePath", filePath)
            editor.commit()
        } else {
            bluetoothScanner.stopScan()
            bluetoothScanner.devicesList = mutableListOf<BeaconDevice>()
            editor.remove("filePath")
            editor.commit()
        }
    }

    private fun createTestMap() {
        // Creating a test map
        val testMap = CustomMap("${getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).absolutePath}/TestPic.jpg", 6.3, 9.75) // in meters

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
        val Room1 = PointOfInterest(Location(2.0, 1.0, testMap), 1.6)
        Room1.image = zone_icon
        testMap.addPoI(Room1)

        // TEST: Creating a interest point and displaying it
        val Living = PointOfInterest(Location(3.0, 1.0, testMap), 1.6)
        Living.image = zone_icon
        testMap.addPoI(Living)

        saveMapToFile(testMap, "${getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).absolutePath}/myRoom.json")

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


    fun refreshButtonClicked(view: View) {
        // For now we don't need this
        //bluetoothScanner.scanLeDevice(true, devicesListAdapter)
        //trilateratePosition()

        bluetoothScanner.isRunningOnBackground = !bluetoothScanner.isRunningOnBackground
    }

    private fun updatePosition() {
        // We should slowly update the position in... 5 stages?
        Log.d(TAG, "Updating current position")
        val layoutParams = RelativeLayout.LayoutParams(70, 70) // value is in pixels
        layoutParams.leftMargin = currentPosition.position.getX() - 35
        layoutParams.topMargin = currentPosition.position.getY() - 35
        Log.d("UPDATING VIEW DRAW", "${positionView.layoutParams}")
        positionView.layoutParams = layoutParams

        for (point in floorMap.pointsOfInterest) {
            if (point.isInside(currentPosition)) {
                if (!point.alreadyInside) {
                    point.alreadyInside = true
                    notificationManager.notify(point.id.toInt(), point.notification.build())
                }
            } else {
                point.alreadyInside = false
            }
        }

        // Demo to obtain current distance to a particular beacon
        //Log.d("DISTANCE NOW", "${floorMap.savedBeacons.get(0).beacon.approxDistance}")

    }

    fun threeClosestBeacons(): List<BeaconOnMap> {
        // TO DO
        var closestList = mutableListOf<BeaconOnMap>()
        return closestList
    }

    /**
     * This is called to update the current position with the new distances to the beacons
     * It populates a queue of positions between the previous location and the new one
     */
    fun trilateratePosition() {
        // Call this after currentPosition's x and y are updated
        val resultLocation = trilaterationCalculator.getPositionInMap(floorMap)
        if (resultLocation != null) {
            if(currentPosition.position.equals(Location(-1.0,-1.0,floorMap))) {
                //INITIAL LOCATION
                drawQueue.add(resultLocation)
            }
            else {
                // We will update the position slowly
                val startLocation = currentPosition.position
                val finishLocation = resultLocation
                val pointsToDraw = calculatePointsBetweenPositions(startLocation, finishLocation)
                Log.d("START  CALCULATION", "(${startLocation.x},${startLocation.y})")
                Log.d("MIDDLE CALCULATION", pointsToDraw.toString())
                Log.d("FINISH CALCULATION", "(${finishLocation.x},${finishLocation.y})")
                pointsToDraw.forEach {
                    drawQueue.add(it)
                }

                //Log.d("CURRENT PY","${floorMap.pointsOfInterest[0].position.y}")

            }
        }
        else {
            Toast.makeText(this, "Beacons out of range", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * This is called in the "middle" of the calls to trilateratePosition
     * It draws the points in the queue, so the movement between two locations is more fluid
     */
    fun updateIntermediate() {
        if (drawQueue.isNotEmpty()) {
            val positionToDraw = drawQueue.remove()!!
            currentPosition.position.x = positionToDraw.x
            currentPosition.position.y = positionToDraw.y
            updatePosition()
        }

    }

    /**
     * Used to obtain 5 points between two given locations ir order to draw the movement step by step
     */
    fun calculatePointsBetweenPositions(a : Location, b : Location) : List<Location> {
            val count = 6
            val d : Double = Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)) / count;
            val fi : Double = Math.atan2(b.y - a.y, b.x - a.x);

            val points = mutableListOf<Location>()

            for (i : Int in 1..5) {
                points.add(Location((a.x + i * d * Math.cos(fi)).roundTo2DecimalPlaces(), (a.y + i * d * Math.sin(fi)).roundTo2DecimalPlaces(), currentPosition.position.map))
            }

            return points
    }

    fun Double.roundTo2DecimalPlaces() =
            BigDecimal(this).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()
}
