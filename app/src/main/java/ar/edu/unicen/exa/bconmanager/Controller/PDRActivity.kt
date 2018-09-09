package ar.edu.unicen.exa.bconmanager.Controller


import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import ar.edu.unicen.exa.bconmanager.Model.*
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.DeviceAttitudeHandler
import ar.edu.unicen.exa.bconmanager.Service.StepDetectionHandler
import ar.edu.unicen.exa.bconmanager.Service.StepDetectionHandler.StepDetectionListener
import ar.edu.unicen.exa.bconmanager.Service.StepPositioningHandler
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.maps.model.LatLng

class PDRActivity : OnMapActivity() {

    private var sm: SensorManager? = null
    private var sdh: StepDetectionHandler? = null
    private var sph: StepPositioningHandler? = null
    private var dah: DeviceAttitudeHandler? = null
    private var isWalking = true
    private var lKloc: Location? = null
    private var lastKnown: LatLng? = null
    override var  TAG = "PDRActivity"
    lateinit var positionView: ImageView
    private var startingPoint = false
    lateinit var currentPosition: PositionOnMap
    //private lateinit var notificationManager: NotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdr) //CHANGE LAYOUT

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

    /*
    fun  getLocation(): Location? {
        Log.d("SM", "ADENTRO DE GET LOCATION")
        val locationManager = this
                .getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (locationManager != null) {
            Log.d("SM", locationManager.toString())
            Log.d("SM", "LOCATION NO ES NULL")
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null
            }
            Log.d("SM", "GET LOCATION BEFORE LAST KNOWN LOCATIOON")
            val lastKnownLocationGPS = locationManager
                    .getLastKnownLocation(LocationManager.GPS_PROVIDER)
            Log.d("SM", "GET LOCATION AFTER LAST KNOWN LOCATIOON")
            if (lastKnownLocationGPS != null) {
                Log.d("SM", "GET LOCATION LASTKNOWN LOCATION GPS")
                Log.d("SM", "lastKnownLocationGPS: $lastKnownLocationGPS")
                return lastKnownLocationGPS
            } else {
                val loc = locationManager
                        .getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                Log.d("SM", "LAST KNOWN GPS NULL, LOC: $loc")
                return loc
            }
        } else {
            Log.d("SM", "LOCATION ES NULL")
            return null
        }
    }
    */


    override fun displayMap() {
        Log.d(TAG,"DISPLAY MAP")

        // This method will create a test map on the downloads directory.
        // Make sure the TestPic.jpg is on the same location
        Log.d("FILEPATH",filePath)
        // Loading the map from a JSON file
        floorMap = loadMapFromFile(filePath)

        // Drawing the map's image
        val bitmap = BitmapFactory.decodeFile(floorMap.image)
        val img = findViewById<View>(R.id.floorPlan) as ImageView //CHECK
        img.setImageBitmap(bitmap)
        img.setOnTouchListener(object: View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent):Boolean {
                if(!startingPoint) {
                    val screenX = event.x
                    val screenY = event.y
                    val viewX = screenX - v.left
                    val viewY = screenY - v.top
                    Log.d(TAG, "Touching x: $viewX y: $viewY")
                    // check if point exists

                    // if it does, click it
                    // otherwise, create a new point there
                    setStartingPoint(viewX, viewY)
                    startingPoint = true
                }
                return false
            }
        })

        // Obtain real width and height of the map
        val mapSize = getRealMapSize()
        floorMap.calculateRatio(mapSize.x, mapSize.y)


        // Drawing all the points of interest for this map
        for (point in floorMap.pointsOfInterest) {
            val imageView = ImageView(this)
            setupResource(point, imageView)
        }


        /*if (servicesConnected()) {
            lKloc = getLocation()
            Log.d("SM", "lKloc: " + lKloc!!)
            lastKnown = LatLng(lKloc!!.latitude,
                    lKloc!!.longitude)
            Log.d("SM", "AFTER lAST KNOWN ASSIGNED, LastK: $lastKnown")
        }
        Log.d("SM", "ANTES DE SENSOR MANAGER")
        */

    }

    override fun updatePosition(beacons: List<BeaconDevice>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Log.d(TAG, "Touching ${event.toString()}")
        return super.onTouchEvent(event)
    }


    private fun servicesConnected(): Boolean {
        val resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this)
        return if (ConnectionResult.SUCCESS == resultCode) {
            true
        } else false
    }

    override fun onResume() {
        super.onResume()
        if((sdh != null) && (dah != null)) {
            sdh!!.start()
            dah!!.start()
        }

    }

    override fun onPause() {
        super.onPause()
        if((sdh != null) && (dah != null)) {
            sdh!!.stop()
            dah!!.stop()
        }
    }
    private fun setStartingPoint(viewX: Float, viewY: Float) {
        val loc = Location(0.0, 0.0, floorMap)
        loc.setX(viewX.toInt())
        loc.setY(viewY.toInt())
        val zone = FingerprintZone(loc)

        // Starting point
        currentPosition = PositionOnMap(loc)
        currentPosition.image = R.drawable.location_icon
        positionView = ImageView(this)
        setupResource(currentPosition, positionView)
        Log.d(TAG, "STARTING POINT IS : "+currentPosition.toString())
        //Log.d(TAG, "Touching ${zone.toString()}")

        sm = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sdh = StepDetectionHandler(sm)
        sdh!!.setStepListener(mStepDetectionListener)
        dah = DeviceAttitudeHandler(sm)
        sph = StepPositioningHandler()
        sph!!.setmCurrentLocation(loc)
        sdh!!.start()
        dah!!.start()
    }


    private fun updatePosition() {
        // We should slowly update the position in... 5 stages?
        val layoutParams = RelativeLayout.LayoutParams(70, 70) // value is in pixels
        Log.d(TAG, "Location before update: "+ sph!!.getmCurrentLocation().toString())
        currentPosition.position = sph!!.getmCurrentLocation()
        layoutParams.leftMargin = currentPosition.position.getX() - 35
        layoutParams.topMargin = currentPosition.position.getY() - 35
        positionView.layoutParams = layoutParams

        Log.d(TAG,"LAYOUT PARAMS LEFT MARGIN: "+layoutParams.leftMargin)
        Log.d(TAG,"LAYOUT PARAMS RIGHT MARGIN: "+layoutParams.rightMargin)

        /*for (point in floorMap.pointsOfInterest) {
            if (point.isInside(currentPosition)) {
                if (!point.alreadyInside) {
                    point.alreadyInside = true
                    notificationManager.notify(floorMap.pointsOfInterest.indexOf(point), point.notification.build())
                }
            } else {
                point.alreadyInside = false
            }
        }*/
    }


    private val mStepDetectionListener = StepDetectionListener { stepSize ->
        val newloc = sph!!.computeNextStep(stepSize, (dah!!.orientationVals[0] - 2.26893f + 3.14159f))
        Log.d(TAG, "Location: "+ newloc.toString()+ "  angle: " + (dah!!.orientationVals[0] - 2.26893f + 3.14159f)*57.2958)
        if (isWalking) {
            Log.d(TAG,"IS WALKING")
            updatePosition()
        }
    }


}