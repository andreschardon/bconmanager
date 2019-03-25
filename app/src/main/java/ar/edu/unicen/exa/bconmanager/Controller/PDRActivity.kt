package ar.edu.unicen.exa.bconmanager.Controller


import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import ar.edu.unicen.exa.bconmanager.Adapters.PDRAdapter
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import ar.edu.unicen.exa.bconmanager.Model.Location
import ar.edu.unicen.exa.bconmanager.Model.PositionOnMap
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.Algorithm.PDRService
import kotlinx.android.synthetic.main.activity_pdr.*

class PDRActivity : PDRInterface, OnMapActivity() {

    override var  TAG = "PDRActivity"
    lateinit var positionView: ImageView
    private var startingPoint = false
    private var isRecordingAngle = false
    private var isPDREnabled = false
    lateinit var currentPosition: PositionOnMap

    override var pdrService = PDRService.instance
    lateinit var pdrAdapter: PDRAdapter

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdr)

        val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
        val intent: Intent
        chooseFile.type = "application/octet-stream" //as close to only Json as possible
        intent = Intent.createChooser(chooseFile, "Choose a file")
        startActivityForResult(intent, 101)
    }

    override fun displayMap() {


      this.touchListener = (object: View.OnTouchListener {
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
        super.displayMap()

        pdrService.bearingAdjustment = (floorMap.angle /57.2958) .toFloat()
        Log.d("ADJUSTMENT", "SAVED Adjustment is ${pdrService.bearingAdjustment}")
        Log.d("ADJUSTMENT", "SAVED Adjustment is ${pdrService.bearingAdjustment*57.2958}")

        pdrAdapter = PDRAdapter(this)
        // Drawing all the points of interest for this map
        /*for (point in floorMap.pointsOfInterest) {
            val imageView = ImageView(this)
            setupResource(point, imageView)
        }*/
    }

    override fun updatePosition(beacons: List<BeaconDevice>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

        pdrService.setupSensorsHandlers(loc,pdrAdapter,sensorManager,false)

        if (isRecordingAngle) {
            Toast.makeText(this, "Walk a few steps straight towards the right side of the map", Toast.LENGTH_SHORT).show()
            startButtonPdr.isEnabled = true

        } else {
            Toast.makeText(this, "You can start walking on any direction", Toast.LENGTH_SHORT).show()
        }
    }
    override fun unsetStartingPoint() {
        floorLayout.removeView(positionView)
        pdrService.stopSensorsHandlers()
        startingPoint = false
        isRecordingAngle = false
    }

     override fun updatePosition() {
        val layoutParams = RelativeLayout.LayoutParams(70, 70) // value is in pixels
        currentPosition.position = validatePosition(pdrService.getmCurrentLocation())
        layoutParams.leftMargin = currentPosition.position.getX() - 35
        layoutParams.topMargin = currentPosition.position.getY() - 35
        positionView.layoutParams = layoutParams
    }

    private fun validatePosition(newPosition : Location): Location {
        return floorMap.restrictPosition(PositionOnMap(newPosition)).position
    }

    fun startPDR(view: View) {
        startButtonPdr.isEnabled = false
        isPDREnabled = true
        pdrService.startPDR()
        Toast.makeText(this, "Touch on your current position", Toast.LENGTH_SHORT).show()
    }

    fun startAngleMeasurement(view: View) {
        measureButtonPdr.isEnabled = false
        isRecordingAngle = true
        pdrService.startRecordingAngle()
        Toast.makeText(this, "Touch on your current position", Toast.LENGTH_SHORT).show()
    }


}