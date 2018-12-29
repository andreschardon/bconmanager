package ar.edu.unicen.exa.bconmanager.Controller

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import ar.edu.unicen.exa.bconmanager.Adapters.ParticleFilterAdapter
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import ar.edu.unicen.exa.bconmanager.Model.Location
import ar.edu.unicen.exa.bconmanager.Model.PositionOnMap
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.*
import kotlin.math.floor

class ParticleFilterActivity : OnMapActivity() {
    private var sensorManager: SensorManager? = null
    private var stepDetectionHandler: StepDetectionHandler? = null
    //private var stepPositioningHandler: StepPositioningHandler? = null
    private var deviceAttitudeHandler: DeviceAttitudeHandler? = null
    private var isWalking = true
    override var TAG = "ParticleFilterActivity"
    //lateinit var positionView: ImageView
    private var startingPoint = false
    private var isRecordingAngle = false
    private var isPDREnabled = false
    //lateinit var currentPosition: PositionOnMap

    lateinit var currentPosition: PositionOnMap
    lateinit var positionView: ImageView
    private var particleViewList : MutableList<ImageView> = mutableListOf<ImageView>()
    private var bearingAdjustment = 0.0f //should be in the map


    private var particleFilterService: ParticleFilterService? = null
    private var trilaterationCalculator = TrilaterationCalculator.instance
    private var pfAdapter = ParticleFilterAdapter(this)

    private var times = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_particle_filter)
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

    override fun onPause() {
        super.onPause()
        if (particleFilterService != null)
            particleFilterService!!.stop()
    }


    override fun displayMap() {
        Log.d(TAG, "DISPLAY MAP")

        // This method will create a test map on the downloads directory.
        // Make sure the TestPic.jpg is on the same location
        Log.d("FILEPATH", filePath)
        // Loading the map from a JSON file
        floorMap = loadMapFromFile(filePath)

        // Drawing the map's image
        val bitmap = BitmapFactory.decodeFile(floorMap.image)
        val img = findViewById<View>(R.id.floorPlan) as ImageView
        img.setImageBitmap(bitmap)
        /*img.setOnTouchListener(object: View.OnTouchListener {
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
        })*/

        // Obtain real width and height of the map
        val mapSize = getRealMapSize()
        floorMap.calculateRatio(mapSize.x, mapSize.y)
        particleFilterService = ParticleFilterService.getInstance(this.applicationContext, floorMap, pfAdapter)
        filter()
    }

    override fun updatePosition(beacons: List<BeaconDevice>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun drawParticle(location: Location) {
        val particle = PositionOnMap(location)
        particle.image = R.drawable.finger_zone_green
        var particleView = ImageView(this)
        particleViewList.add(particleView);
        setupResource(particle, particleView)
    }

    private fun setStartingPoint(viewX: Double, viewY: Double) {
        val loc = Location(0.0, 0.0, floorMap)
        loc.x = viewX
        loc.y = viewY

        // Starting point
        currentPosition = PositionOnMap(loc)
        currentPosition.image = R.drawable.location_icon
        positionView = ImageView(this)
        setupResource(currentPosition, positionView)
        //Log.d(TAG, "Touching ${zone.toString()}")


        // Should be elsewhere
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepDetectionHandler = StepDetectionHandler(sensorManager, false)
        stepDetectionHandler!!.setStepListener(mStepDetectionListener)
        deviceAttitudeHandler = DeviceAttitudeHandler(sensorManager)
        stepPositioningHandler = StepPositioningHandler()
        stepPositioningHandler!!.setmCurrentLocation(loc)
        stepDetectionHandler!!.start()
        deviceAttitudeHandler!!.start()

    }

    // Should be elsewhere
    private val mStepDetectionListener = StepDetectionHandler.StepDetectionListener { stepSize ->
        // To correct previous invalid position
        stepPositioningHandler!!.setmCurrentLocation(currentPosition.position)

        val newloc = stepPositioningHandler!!.computeNextStep(stepSize, (deviceAttitudeHandler!!.orientationVals[0] + bearingAdjustment))
        Log.d(TAG, "Location: " + newloc.toString() + "  angle: " + (deviceAttitudeHandler!!.orientationVals[0] + bearingAdjustment) * 57.2958)
        if (isWalking) {
            updatePosition()
        }
    }

    fun updateParticleFilterPosition(viewX: Double, viewY: Double) {
        val loc = Location(0.0, 0.0, floorMap)
        loc.x = viewX
        loc.y = viewY

        Log.d("PFACTIVITY", "Particle filter finished, remove old particles")

        // Remove old particles
        particleViewList.forEach { removeResource(it) }

        // Draw particles
        Log.d("PFACTIVITY", "Draw new particles")
        val particlesToDraw = particleFilterService!!.particles
        particlesToDraw.forEach {
            drawParticle(Location(it.x, it.y, floorMap))
        }

        // Update old position to new
        Log.d("PFACTIVITY", "Update new position to PF's middle point: $loc")

        val layoutParams = RelativeLayout.LayoutParams(70, 70) // value is in pixels
        Log.d(TAG, "Location before update: "+ currentPosition.toString())
        currentPosition.position = validatePosition(loc)
        Log.d(TAG, "Location after update: "+ currentPosition.toString())
        layoutParams.leftMargin = currentPosition.position.getX() - 35
        layoutParams.topMargin = currentPosition.position.getY() - 35
        positionView.layoutParams = layoutParams
    }


    // Should be an adapter
    private fun updatePosition() {

        Log.d("PFACTIVITY", "PDR detected an step. Calculate new position")

        currentPosition.position = validatePosition(stepPositioningHandler!!.getmCurrentLocation())

        Log.d("PFACTIVITY", "New position according to PDR is ${currentPosition.position}")


        if (times > 0) {
            advanceStep(currentPosition.position)
            times--
        }



        /*val layoutParams = RelativeLayout.LayoutParams(70, 70) // value is in pixels
        layoutParams.leftMargin = currentPosition.position.getX() - 35
        layoutParams.topMargin = currentPosition.position.getY() - 35
        positionView.layoutParams = layoutParams*/
    }

    private fun validatePosition(newPosition: Location): Location {
        return floorMap.restrictPosition(PositionOnMap(newPosition)).position
    }


    //Change Name
    fun filter() {
        //var frameWidth  = 860 // ??
        //var frameHeight = 540 // ??

        //Replace with trilat position
        var xPos = 0.5
        var yPos = 0.5
        setStartingPoint(xPos, yPos)

        Log.d("PFACTIVITY", "Startup point set")

        //particleFilterService!!.updatePosition(0.0, 0.0, xPos, yPos)
        //particleFilterService!!.start()

        /*
        xPos = 2.3
        yPos = 2.4
        particleFilterService!!.updatePosition(0.5, 0.5, xPos,yPos)
        particleFilterService!!.start()


        xPos = 2.5
        yPos = 2.6
        particleFilterService!!.updatePosition(0.4, 0.4, xPos,yPos)
        particleFilterService!!.start()
        */
        /*try {
            Thread.sleep(1000 * 20)
        } catch (e : InterruptedException) {
            e.printStackTrace()
        }
        //Replace with trilat position
        xPos += 50
        particleFilterService.updatePosition(xPos, yPos)
        try {
            Thread.sleep(1000 * 20)
        } catch (e : InterruptedException) {
            e.printStackTrace()
        }
        //Replace with trilat position
        xPos += 50
        yPos += 80
        particleFilterService.updatePosition(xPos, yPos)
        try {
            Thread.sleep(1000 * 20);
        } catch (e : InterruptedException) {
            e.printStackTrace()
        }*/
        //particleFilterService.stop()
    }

    fun advanceStep(pdrPosition: Location) {

        Log.d("PFACTIVITY", "Advanced an step, calculate trilateration location")

        //val trilaterationLocation = trilaterationCalculator.getPositionInMap(floorMap)
        val trilaterationLocation = Location(0.42, 0.31, floorMap) // MOCK VALUE

        Log.d("PFACTIVITY", "Trilateration location is $trilaterationLocation")
        Log.d("PFACTIVITY", "Moved length is           (${pdrPosition.getXMeters()}, ${pdrPosition.getYMeters()})")

        particleFilterService!!.updatePosition(pdrPosition.getXMeters(), pdrPosition.getYMeters(),
                trilaterationLocation!!.getXMeters(), trilaterationLocation.getYMeters())


        Log.d("PFACTIVITY", "Called pfService.updatePosition correctly. Lets start the pf.")

        particleFilterService!!.start()

    }
}
