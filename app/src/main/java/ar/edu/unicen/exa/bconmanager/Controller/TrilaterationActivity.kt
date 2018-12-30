package ar.edu.unicen.exa.bconmanager.Controller

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Point
import android.hardware.SensorEvent
import android.os.Build
import android.os.Bundle
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.getExternalStoragePublicDirectory
import android.support.v4.app.NotificationCompat
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import ar.edu.unicen.exa.bconmanager.Adapters.BeaconsAdapter
import ar.edu.unicen.exa.bconmanager.Model.*
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.R.drawable.*
import ar.edu.unicen.exa.bconmanager.Service.MovementDetector
import ar.edu.unicen.exa.bconmanager.Service.TrilaterationCalculator
import kotlinx.android.synthetic.main.activity_trilateration.*
import java.util.*


class TrilaterationActivity : OnMapActivity() {

    override var TAG = "TrilaterationActivity"
    private var trilaterationCalculator = TrilaterationCalculator.instance
    private var drawQueue: Queue<Location> = ArrayDeque<Location>()

    lateinit var devicesListAdapter: BeaconsAdapter
    lateinit var positionView: ImageView
    lateinit var currentPosition: PositionOnMap
    private var jumpCounter = 0
    private val detector = MovementDetector(this)
    private val ACCELERATION_THRESHOLD = 1.5
    private val USELESS_MOVE_THRESHOLD = 0.3
    private val JUMP_THRESHOLD = 1.4 //meters

    private lateinit var notificationManager: NotificationManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trilateration)

        val intent = intent
        //get the attached extras from the intent
        //we should use the same key as we used to attach the data.
        val mapPath = intent.getStringExtra("path")
        if(!mapPath.isNullOrEmpty()) {
            filePath = mapPath
            displayMap()
        }
        else if (!bluetoothScanner.isRunningOnBackground) {
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

        /* Not working
        detector.addListener(object:MovementDetector.Listener {
            override fun onMotionDetected(event:SensorEvent, acceleration:Float) {
                /*Log.d("ACCELERATION", ("Acceleration: [" + String.format("%.3f", event.values[0])
                        + "," + String.format("%.3f", event.values[1]) + ","
                        + String.format("%.3f", event.values[2]) + "] "
                        + String.format("%.3f", acceleration)))*/
                //Log.d("ACCELERATION", String.format("%.3f", acceleration))
                if (acceleration > ACCELERATION_THRESHOLD)
                {
                    Log.d("ACCELERATION", "You are moving at $acceleration")
                    devicesListAdapter.refreshEverything()
                }
            }
        })
        detector.context = this
        detector.init()
        detector.start()
        */
    }

    private fun setupNotifications() {

        // Create a notification builder for each point of interest
        val intent = Intent(this, TrilaterationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        for (point in floorMap.pointsOfInterest) {
            val mBuilder = NotificationCompat.Builder(this.applicationContext, "notify_001")
            val bigText = NotificationCompat.BigTextStyle()
            bigText.setBigContentTitle(point.content)
            bigText.setSummaryText(point.id)
            mBuilder.setContentIntent(pendingIntent)
            mBuilder.setSmallIcon(R.drawable.interest_icon)
            mBuilder.setContentTitle(point.content)
            mBuilder.setContentText(point.id)
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
    override fun displayMap() {

        // This method will create a test map on the downloads directory.
        // Make sure the TestPic.jpg is on the same location
        //createTestMap()
        //Log.d("FILEPATH",filePath)
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

        //setupNotifications()


    }

    override fun updatePosition(beacons: List<BeaconDevice>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
            //detector.stop()
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
        val Room1 = PointOfInterest(Location(2.0, 1.0, testMap), 1.6, "Room1", "Content for Room1")
        Room1.image = zone_icon
        testMap.addPoI(Room1)

        // TEST: Creating a interest point and displaying it
        val Living = PointOfInterest(Location(3.0, 1.0, testMap), 1.6, "Living", "Content for Living")
        Living.image = zone_icon
        testMap.addPoI(Living)

        saveMapToFile(testMap, "${getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).absolutePath}/myRoom.json")

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

    fun refreshButtonClicked(view: View) {
        // For now we don't need this
        //bluetoothScanner.scanLeDevice(true, devicesListAdapter)
        //trilateratePosition()

        bluetoothScanner.isRunningOnBackground = !bluetoothScanner.isRunningOnBackground
    }

    private fun updatePosition() {
        // We should slowly update the position in... 5 stages?
        val layoutParams = RelativeLayout.LayoutParams(70, 70) // value is in pixels
        layoutParams.leftMargin = currentPosition.position.getX() - 35
        layoutParams.topMargin = currentPosition.position.getY() - 35
        positionView.layoutParams = layoutParams

        for (point in floorMap.pointsOfInterest) {
            if (point.isInside(currentPosition)) {
                if (!point.alreadyInside) {
                    point.alreadyInside = true
                    notificationManager.notify(floorMap.pointsOfInterest.indexOf(point), point.notification.build())
                }
            } else {
                point.alreadyInside = false
            }
        }

        // Demo to obtain current distance to a particular beacon
        //Log.d("DISTANCE NOW", "${floorMap.savedBeacons.get(0).beacon.approxDistance}")

    }

    /**
     * This is called to update the current position with the new distances to the beacons
     * It populates a queue of positions between the previous location and the new one
     */
    fun trilateratePosition() {
        // Call this after currentPosition's x and y are updated
        val resultLocation = trilaterationCalculator.getPositionInMap(floorMap)
        if (resultLocation != null) {
            if (currentPosition.position.equals(Location(-1.0, -1.0, floorMap))) {
                //INITIAL LOCATION
                drawQueue.add(resultLocation)
            } else {
                // We will update the position slowly
                val startLocation = currentPosition.position
                val finishLocation = resultLocation
                if (!isUselessMove(startLocation, finishLocation) && !isJump(startLocation, finishLocation)) {
                    val pointsToDraw = calculatePointsBetweenPositions(startLocation, finishLocation)
                    //Log.d("START  CALCULATION", "(${startLocation.x},${startLocation.y})")
                    //Log.d("MIDDLE CALCULATION", pointsToDraw.toString())
                    //Log.d("FINISH CALCULATION", "(${finishLocation.x},${finishLocation.y})")
                    pointsToDraw.forEach {
                        drawQueue.add(it)
                    }
                } else {
                    //Log.d("FINISH", "It is useless")
                }
            }
        } else {
            Toast.makeText(this, "Beacons out of range", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Used to ignore points really close
     */
    fun isUselessMove(origin : Location, destination : Location) : Boolean {
        if (Math.abs(origin.x - destination.x) < USELESS_MOVE_THRESHOLD && Math.abs(origin.y - destination.y) < USELESS_MOVE_THRESHOLD ) {
            return true
        }
        return false
    }

    /**
     * Checks that the new destination is not a "jump"
     * A location is considered a jump when it is really far from the origin point
     * and it is not repeated over time (if we get 3 consecutive jumps, we should perform the jump)
     */

    fun isJump(origin: Location, destination: Location) : Boolean {
        //Log.d("JUMP", "Differences are ${Math.abs(origin.x - destination.x)}m and ${Math.abs(origin.y - destination.y)}m")
        if (Math.abs(origin.x - destination.x) >= JUMP_THRESHOLD ||  Math.abs(origin.y - destination.y) >= JUMP_THRESHOLD) {
            jumpCounter++
        } else {
            jumpCounter = 0
            //Log.d("JUMP", "Not a jump, update")
            return false
        }
        if (jumpCounter > 3) {
            //Log.d("JUMP", "Repeated jump, let's do it")
            return false
        }
        //Log.d("JUMP", "It's a jump, do not update")
        return true
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
    fun calculatePointsBetweenPositions(a: Location, b: Location): List<Location> {
        val count = 6
        val d: Double = Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)) / count;
        val fi: Double = Math.atan2(b.y - a.y, b.x - a.x);

        val points = mutableListOf<Location>()

        for (i: Int in 1..5) {
            points.add(Location((a.x + i * d * Math.cos(fi)).roundTo2DecimalPlaces(), (a.y + i * d * Math.sin(fi)).roundTo2DecimalPlaces(), currentPosition.position.map))
        }

        return points
    }
}
