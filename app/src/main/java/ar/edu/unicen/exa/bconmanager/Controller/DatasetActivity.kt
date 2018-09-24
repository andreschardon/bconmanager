package ar.edu.unicen.exa.bconmanager.Controller

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import ar.edu.unicen.exa.bconmanager.Model.Location
import ar.edu.unicen.exa.bconmanager.Model.PositionOnMap
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.DeviceAttitudeHandler
import ar.edu.unicen.exa.bconmanager.Service.StepDetectionHandler
import ar.edu.unicen.exa.bconmanager.Service.StepPositioningHandler
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_pdr.*

class DatasetActivity : OnMapActivity() {

    private var sensorManager: SensorManager? = null
    private var stepDetectionHandler: StepDetectionHandler? = null
    private var stepPositioningHandler: StepPositioningHandler? = null
    private var deviceAttitudeHandler: DeviceAttitudeHandler? = null
    private var isWalking = true
    private var lKloc: Location? = null
    private var lastKnown: LatLng? = null
    override var  TAG = "DatasetActivity"
    lateinit var positionView: ImageView
    private var startingPoint = false
    private var isRecordingAngle = false
    private var isPDREnabled = false
    lateinit var currentPosition: PositionOnMap
    private var bearingAdjustment = 0.0f
    private var recordCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dataset)

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

    }

    override fun updatePosition(beacons: List<BeaconDevice>) {
        //only for inheritance purpose
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Log.d(TAG, "Touching ${event.toString()}")
        return super.onTouchEvent(event)
    }

    override fun onResume() {
        super.onResume()
        if((stepDetectionHandler != null) && (deviceAttitudeHandler != null)) {
            stepDetectionHandler!!.start()
            deviceAttitudeHandler!!.start()
        }

    }

    override fun onPause() {
        super.onPause()
        if((stepDetectionHandler != null) && (deviceAttitudeHandler != null)) {
            stepDetectionHandler!!.stop()
            deviceAttitudeHandler!!.stop()
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

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        //REPLACE WITH MOVEMENT DETECTION HANDLER
        /*stepDetectionHandler = StepDetectionHandler(sensorManager)
        stepDetectionHandler!!.setStepListener(mStepDetectionListener)
        deviceAttitudeHandler = DeviceAttitudeHandler(sensorManager)
        stepPositioningHandler = StepPositioningHandler()
        stepPositioningHandler!!.setmCurrentLocation(loc)
        stepDetectionHandler!!.start()
        deviceAttitudeHandler!!.start()*/


        if (isRecordingAngle) {
            Toast.makeText(this, "Walk a few steps straight towards the right side of the map", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "You can start walking on any direction", Toast.LENGTH_SHORT).show()
        }
    }
    private fun unsetStartingPoint() {
        floorLayout.removeView(positionView)
        deviceAttitudeHandler!!.stop()
        stepDetectionHandler!!.stop()
        startingPoint = false
    }

    /*private val mStepDetectionListener = StepDetectionListener { stepSize ->
        val newloc = stepPositioningHandler!!.computeNextStep(stepSize, (deviceAttitudeHandler!!.orientationVals[0] + bearingAdjustment))
        Log.d(TAG, "Location: "+ newloc.toString()+ "  angle: " + (deviceAttitudeHandler!!.orientationVals[0] + bearingAdjustment)*57.2958)
        if (isWalking && !isRecordingAngle) {
            Log.d(TAG,"IS WALKING")
        } else if (isWalking && isRecordingAngle) {
            recordCount++
            if (recordCount == 3) {
                setAdjustedBearing(deviceAttitudeHandler!!.orientationVals[0])
                recordCount = 0
                Toast.makeText(this, "Adjustment angle saved: ${bearingAdjustment*57.2958}", Toast.LENGTH_SHORT).show()
            }

        }


    }*/

    private fun setAdjustedBearing(measuredAngle : Float) {
        val adjustmentFactor = 0 // 90 degrees
        Log.d("ADJUSTMENT", "Measured angle is ${measuredAngle*57.2958}")
        Log.d("ADJUSTMENT", "It should be ${adjustmentFactor*57.2958}")
        bearingAdjustment = -measuredAngle
        Log.d("ADJUSTMENT", "Adjustment is ${bearingAdjustment*57.2958}")
        isRecordingAngle = false
        unsetStartingPoint()

    }

}
