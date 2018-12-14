package ar.edu.unicen.exa.bconmanager.Controller

import android.content.Intent
import android.graphics.BitmapFactory
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import ar.edu.unicen.exa.bconmanager.Model.Location
import ar.edu.unicen.exa.bconmanager.Model.PositionOnMap
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.*

class ParticleFilterActivity : OnMapActivity() {
    private var sensorManager: SensorManager? = null
    private var stepDetectionHandler: StepDetectionHandler? = null
    private var stepPositioningHandler: StepPositioningHandler? = null
    private var deviceAttitudeHandler: DeviceAttitudeHandler? = null
    private var isWalking = true
    override var  TAG = "ParticleFilterActivity"
    //lateinit var positionView: ImageView
    private var startingPoint = false
    private var isRecordingAngle = false
    private var isPDREnabled = false
    //lateinit var currentPosition: PositionOnMap


    private var particleFilterService: ParticleFilterService? = null
    private var trilaterationCalculator = TrilaterationCalculator.instance


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
        Log.d(TAG,"DISPLAY MAP")

        // This method will create a test map on the downloads directory.
        // Make sure the TestPic.jpg is on the same location
        Log.d("FILEPATH",filePath)
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
        particleFilterService = ParticleFilterService.getInstance(this.applicationContext,floorMap)
        filter()
    }

    override fun updatePosition(beacons: List<BeaconDevice>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }



//Change Name
    fun filter () {
        var frameWidth  = 860 // ??
        var frameHeight = 540 // ??

        //Replace with trilat position
        var xPos = 2.0
        var yPos = 2.0
        particleFilterService!!.updatePosition(0.0, 0.0, xPos,yPos)
        particleFilterService!!.start()
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

    fun advanceStep(pdrPosition : Location) {
        val trilaterationLocation = trilaterationCalculator.getPositionInMap(floorMap)
        particleFilterService!!.updatePosition(pdrPosition.getXMeters(), pdrPosition.getYMeters(),
                trilaterationLocation!!.getXMeters(), trilaterationLocation.getYMeters())

    }
}
