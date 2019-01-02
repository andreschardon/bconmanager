package ar.edu.unicen.exa.bconmanager.Controller

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import ar.edu.unicen.exa.bconmanager.Adapters.DatasetCaptureAdapter
import ar.edu.unicen.exa.bconmanager.Adapters.PDRAdapter
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonData
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonDataBeacon
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonDataset
import ar.edu.unicen.exa.bconmanager.Model.Location
import ar.edu.unicen.exa.bconmanager.Model.PositionOnMap
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.DeviceAttitudeHandler
import ar.edu.unicen.exa.bconmanager.Service.JsonUtility
import ar.edu.unicen.exa.bconmanager.Service.PDRService
import ar.edu.unicen.exa.bconmanager.Service.StepDetectionHandler
import ar.edu.unicen.exa.bconmanager.Service.StepDetectionHandler.StepDetectionListener
import kotlinx.android.synthetic.main.activity_pdr.*

class DatasetActivity : OnMapActivity() {

    override var  TAG = "DatasetActivity"
    lateinit var positionView: ImageView
    private var startingPoint = false
    private var isRecordingAngle = false
    private var isPDREnabled = false
    lateinit var currentPosition: PositionOnMap
    private var angle = 0.0
    private var acceleration = 0.0f
    private var startupTime : Long = 0L
    private lateinit var devicesListOnlineAdapter: DatasetCaptureAdapter
    private var dataCollectionHandler = Handler()
    private var datalist = mutableListOf<JsonData>()
    private val delay = 500L //milliseconds. Interval in which data will be captured
    private var pdrService = PDRService.instance


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
            Log.d(TAG,"FILE PATH NOT EMPTY")
            displayMap()
        } else {
            Log.e(TAG, "The file path is incorrect")
        }
    }
    override fun displayMap() {
        super.displayMap()
        val img = findViewById<View>(R.id.floorPlan) as ImageView //CHECK
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



        // Drawing all the points of interest for this map
        for (point in floorMap.pointsOfInterest) {
            val imageView = ImageView(this)
            setupResource(point, imageView)
        }

        // Drawing all the beacons for this map
        for (beacon in floorMap.savedBeacons) {
            val imageView = ImageView(this)
            setupResource(beacon, imageView)
        }

        devicesListOnlineAdapter = DatasetCaptureAdapter(this, bluetoothScanner.devicesList)


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
        pdrService.startSensorsHandlers()
    }

    override fun onPause() {
        super.onPause()
        pdrService.stopSensorsHandlers()
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

        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        pdrService.setupSensorsHandlers(loc,devicesListOnlineAdapter,sensorManager,true)



        if (isRecordingAngle) {
            Toast.makeText(this, "Walk a few steps straight towards the right side of the map", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "You can start walking on any direction", Toast.LENGTH_SHORT).show()
        }
    }
    fun unsetStartingPoint() {
        floorLayout.removeView(positionView)
        pdrService.stopSensorsHandlers()
        startingPoint = false
    }


    fun startAngleMeasurement(view: View) {
        isRecordingAngle = true
        pdrService.startRecordingAngle()
        Toast.makeText(this, "Touch on your current position", Toast.LENGTH_SHORT).show()
    }

    /**
     * TO DO: Starts data collection. Every 100ms it should collect data from PDR and BLE scanner
     * and save it to some object
     */
    fun startDataCollection(view: View) {
        bluetoothScanner.scanLeDevice(true, devicesListOnlineAdapter)
        startupTime = System.currentTimeMillis()
        pdrService.startSensorsHandlers()
        dataCollectionHandler.postDelayed(object : Runnable {
            override fun run() {
                collectData(System.currentTimeMillis())
                dataCollectionHandler.postDelayed(this, delay)
            }
        }, delay)

    }

    /**
     * TO DO: Should update an object with the data from PDR and BLE scanner
     */
    fun collectData(currentTime : Long) {
        val timestamp = currentTime - startupTime
        val beaconData = mutableListOf<JsonDataBeacon>()
        angle = pdrService.getAngle()
        acceleration = pdrService.getAcc()
        Log.d("DATACOLLECT", "Current timestamp is $timestamp ms")
        Log.d("DATACOLLECT", "Current angle is $angle º")
        Log.d("DATACOLLECT", "Current speed is $acceleration ")
        devicesListOnlineAdapter.beacons.forEach {
            val beacon = JsonDataBeacon(it.address, it.intensity)
            beaconData.add(beacon)
            Log.d("DATACOLLECT", it.toString())
        }
        val data = JsonData(beaconData, angle, acceleration, timestamp,0.0,0.0)
        datalist.add(data)
    }

    /**
     * TO DO: Stops data collection. Stops BLE service. Saves everything to some file (json?)
     */
    fun stopDataCollection(view: View) {
        // Stops the postDelayed runnable
        dataCollectionHandler.removeCallbacksAndMessages(null)
        bluetoothScanner.stopScan()
        val dataset = JsonDataset(datalist)
        JsonUtility.saveDatasetToFile("$filePath.data", dataset)
        Log.d(TAG, "Map saved to JSON file in $filePath.data")
        Toast.makeText(this, "Saved to $filePath.data", Toast.LENGTH_SHORT).show()
    }

}
