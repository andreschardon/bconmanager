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
import android.widget.Toast
import ar.edu.unicen.exa.bconmanager.Model.*
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.DeviceAttitudeHandler
import ar.edu.unicen.exa.bconmanager.Service.StepDetectionHandler
import ar.edu.unicen.exa.bconmanager.Service.StepDetectionHandler.StepDetectionListener
import ar.edu.unicen.exa.bconmanager.Service.StepPositioningHandler
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_pdr.*

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
    private var isRecordingAngle = false
    private var isPDREnabled = false
    lateinit var currentPosition: PositionOnMap
    private var bearingAdjustment = 0.0f
    private var recordCount = 0

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
                if(isRecordingAngle || (isPDREnabled && !startingPoint)) {
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

        if (isRecordingAngle) {
            Toast.makeText(this, "Walk a few steps straight towards the right side of the map", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "You can start walking on any direction", Toast.LENGTH_SHORT).show()
        }
    }
    private fun unsetStartingPoint() {
        floorLayout.removeView(positionView)
        dah!!.stop()
        sdh!!.stop()
        startingPoint = false
    }


    private fun updatePosition() {
        val layoutParams = RelativeLayout.LayoutParams(70, 70) // value is in pixels
        Log.d(TAG, "Location before update: "+ sph!!.getmCurrentLocation().toString())
        currentPosition.position = sph!!.getmCurrentLocation()
        layoutParams.leftMargin = currentPosition.position.getX() - 35
        layoutParams.topMargin = currentPosition.position.getY() - 35
        positionView.layoutParams = layoutParams


    }


    private val mStepDetectionListener = StepDetectionListener { stepSize ->
        val newloc = sph!!.computeNextStep(stepSize, (dah!!.orientationVals[0] + bearingAdjustment))
        Log.d(TAG, "Location: "+ newloc.toString()+ "  angle: " + (dah!!.orientationVals[0] + bearingAdjustment)*57.2958)
        if (isWalking && !isRecordingAngle) {
            Log.d(TAG,"IS WALKING")
            updatePosition()
        } else if (isWalking && isRecordingAngle) {
            recordCount++
            if (recordCount == 3) {
                setAdjustedBearing(dah!!.orientationVals[0])
                recordCount = 0
                Toast.makeText(this, "Adjustment angle saved: ${bearingAdjustment*57.2958}", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun setAdjustedBearing(measuredAngle : Float) {
        val adjustmentFactor = 0 // 90 degrees
        Log.d("ADJUSTMENT", "Measured angle is ${measuredAngle*57.2958}")
        Log.d("ADJUSTMENT", "It should be ${adjustmentFactor*57.2958}")
        bearingAdjustment = -measuredAngle
        Log.d("ADJUSTMENT", "Adjustment is ${bearingAdjustment*57.2958}")
        isRecordingAngle = false
        unsetStartingPoint()

    }

    fun startPDR(view: View) {
        isPDREnabled = true
        Toast.makeText(this, "Touch on your current position", Toast.LENGTH_SHORT).show()
    }

    fun startAngleMeasurement(view: View) {
        isRecordingAngle = true
        Toast.makeText(this, "Touch on your current position", Toast.LENGTH_SHORT).show()


    }


}