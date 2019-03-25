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
import ar.edu.unicen.exa.bconmanager.Adapters.PDRAdapter
import ar.edu.unicen.exa.bconmanager.Adapters.ParticleFilterAdapter
import ar.edu.unicen.exa.bconmanager.Model.AveragedTimestamp
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import ar.edu.unicen.exa.bconmanager.Model.Location
import ar.edu.unicen.exa.bconmanager.Model.PositionOnMap
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.Algorithm.FingerprintingService
import ar.edu.unicen.exa.bconmanager.Service.Algorithm.PDRService
import ar.edu.unicen.exa.bconmanager.Service.Algorithm.ParticleFilterService
import ar.edu.unicen.exa.bconmanager.Service.Sensors.BluetoothScanner

class ParticleFilterActivity : PDRInterface, OnMapActivity() {
    private var stop = false
    override var TAG = "ParticleFilterActivity"

    lateinit var currentPosition: PositionOnMap
    lateinit var positionView: ImageView
    lateinit var currentTrilatPosition: PositionOnMap
    lateinit var currentTrilatView: ImageView
    private var particleViewList: MutableList<ImageView> = mutableListOf<ImageView>()

    private var pdrService = PDRService.instance
    private var particleFilterService: ParticleFilterService? = null
    private var referenceCalculator = FingerprintingService()
    private lateinit var pfAdapter: ParticleFilterAdapter
    private lateinit var pdrAdapter: PDRAdapter
    private var isFingerprint = true
    private var isSettingStartPoint = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_particle_filter)
        isFingerprint = intent.getBooleanExtra("isFingerprint", true)
        Log.d("ISFINGERPRINT", isFingerprint.toString())
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

    override fun onResume() {
        super.onResume()
        stop = false
    }

    override fun onPause() {
        super.onPause()
        if (particleFilterService != null)
            particleFilterService!!.stop()
        stop = true
        bluetoothScanner.stopScan()
        bluetoothScanner.devicesList = mutableListOf<BeaconDevice>()
    }


    override fun displayMap() {
        this.touchListener = (object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if (isSettingStartPoint) {
                    val screenX = event.x
                    val screenY = event.y
                    val viewX = screenX - v.left
                    val viewY = screenY - v.top
                    Log.d(TAG, "Touching x: $viewX y: $viewY")
                    isSettingStartPoint = false
                    setUpParticleFilter(viewX, viewY)
                }
                return false
            }
        })
        super.displayMap()

    }

    /**
     * Starts scanning the beacons
     */
    fun startScan(bluetoothScanner: BluetoothScanner) {
        bluetoothScanner.devicesList.clear()
        floorMap.savedBeacons.forEach {
            it.beacon.cleanAverages()
            bluetoothScanner.devicesList.add(it.beacon)
        }
        pfAdapter = ParticleFilterAdapter(this, bluetoothScanner.devicesList)
        bluetoothScanner.scanLeDevice(true, pfAdapter)
    }

    fun setUpParticleFilter(startX: Float = 0.0f, startY: Float = 0.0f) {

        val startLoc = Location(0.0, 0.0, floorMap)
        startLoc.setX(startX.toInt())
        startLoc.setY(startY.toInt())

        // Set up trilateration and fingerprinting
        startScan(bluetoothScanner)
        referenceCalculator.startUp(floorMap)

        // Set up PDR
        pdrService.bearingAdjustment = (floorMap.angle / 57.2958).toFloat()
        Log.d("ADJUSTMENT", "SAVED PF Adjustment is ${pdrService.bearingAdjustment}")
        Log.d("ADJUSTMENT", "SAVED PF Adjustment is ${pdrService.bearingAdjustment * 57.2958}°")
        pdrAdapter = PDRAdapter(this)
        pdrService.startPDR()

        particleFilterService = ParticleFilterService.getInstance(this.applicationContext, floorMap, pfAdapter)

        setStartingPoint(startLoc.getXMeters(), startLoc.getYMeters())

        Log.d("PFACTIVITY", "Startup point set")

        //particleFilterService.stop()
    }

    override fun updatePosition(beacons: List<BeaconDevice>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun drawParticle(location: Location) {
        val particle = PositionOnMap(location)
        particle.image = R.drawable.finger_zone_green_xs
        val particleView = ImageView(this)
        particleViewList.add(particleView)
        setupResource(particle, particleView, 30, 30)
    }

    private fun drawTrilaterationPoint(location: Location) {
        removeResource(currentTrilatView)

        val particle = PositionOnMap(location)
        particle.image = R.drawable.finger_zone_blue
        var current = ImageView(this)
        currentTrilatView = current
        setupResource(particle, currentTrilatView)
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

        // PDR
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        pdrService.setupSensorsHandlers(loc, pdrAdapter, sensorManager, false)

        // Trilateration
        currentTrilatPosition = PositionOnMap(Location(0.0, 0.0, floorMap))
        currentTrilatPosition.image = R.drawable.finger_zone_blue
        currentTrilatView = ImageView(this)
        setupResource(currentTrilatPosition, currentTrilatView)

        particleFilterService!!.setStartingLocation(loc)

    }

    /**
     *  This gets called when a new step is detected by the PDR service.
     *  It obtains the "new position" that will be used to calculate the moved distance
     *  in X and Y.
     */
    override fun updatePosition() {
        //var pdrPosition = validatePosition(pdrService.getmCurrentLocation())
        Log.d("PFACTIVITY", "PDR advanced (${pdrService.advancedX}, ${pdrService.advancedY})")
        if (!stop)
            advanceStep(pdrService.advancedX, pdrService.advancedY)
    }

    override fun unsetStartingPoint() {
        // To review
        updatePosition()
    }

    private fun validatePosition(newPosition: Location): Location {
        return floorMap.restrictPosition(PositionOnMap(newPosition)).position
    }

    /**
     * Gets called by the adapter when the trilateration service has a new position
     */
    fun trilateratePosition() {
        if (isFingerprint) {
            //val resultLocation = trilaterationCalculator.getPositionInMap(floorMap.savedBeacons)
            var currentTimestamp = AveragedTimestamp()
            currentTimestamp.startFromBeacons(floorMap.savedBeacons)
            val resultLocation = referenceCalculator.getNextPosition(currentTimestamp)
            Log.d("NEWTEST", "Updating reference location to $resultLocation")
            val trilatLocationOnMap: Location
            if (resultLocation != null) {
                trilatLocationOnMap = Location(resultLocation!!.x, resultLocation.y, floorMap)
                currentTrilatPosition = PositionOnMap(trilatLocationOnMap!!)
                drawTrilaterationPoint(currentTrilatPosition.position)
            }
        }
    }


    fun advanceStep(movedX: Double, movedY: Double) {

        Log.d("PFACTIVITY", "Advanced an step, calculate trilateration location")

        removeResource(currentTrilatView)
        if (isFingerprint)
            drawTrilaterationPoint(currentTrilatPosition.position)

        Log.d("PFACTIVITY-PRE", "Trilateration location is $currentTrilatPosition")
        Log.d("PFACTIVITY-PRE", "Current location is       (${currentPosition.position.getXMeters()}, ${currentPosition.position.getYMeters()})")
        Log.d("PFACTIVITY-PRE", "Current location2 is      (${currentPosition.position.x}, ${currentPosition.position.y})")
        Log.d("PFACTIVITY-PRE", "Moved length is           ($movedX, $movedY)")

        if (isFingerprint) {
            particleFilterService!!.updatePosition(movedX, movedY,
                    currentTrilatPosition.position.getXMeters(), currentTrilatPosition.position.getYMeters())
        } else {
            var currentTimestamp = AveragedTimestamp()
            currentTimestamp.startFromBeacons(floorMap.savedBeacons)
            particleFilterService!!.updatePosition(movedX, movedY, currentTimestamp.beacons)
        }


    }


    /**
     * This methods gets called when the particle filter service finishes its processing
     * and has a new position to update the view (viewX, viewY). This position is the middle point
     * between all the particles.
     */
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

        val layoutParams = positionView.layoutParams as RelativeLayout.LayoutParams

        Log.d(TAG, "Location before update: " + currentPosition.toString())
        currentPosition.position = validatePosition(loc)
        Log.d(TAG, "Location after update: " + currentPosition.toString())
        layoutParams.leftMargin = currentPosition.position.getX() - 35
        layoutParams.topMargin = currentPosition.position.getY() - 35
        //positionView.layoutParams = layoutParams

        if (shouldCheckZones) {
            Log.d("NOTIFICATIONS", "Should check")
            checkInteractionZones(currentPosition.position)
        }

    }

    fun recordStartPoint(view: View) {
        isSettingStartPoint = true
    }

}
