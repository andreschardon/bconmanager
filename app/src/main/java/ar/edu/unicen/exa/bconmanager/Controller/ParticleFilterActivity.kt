package ar.edu.unicen.exa.bconmanager.Controller

import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.RelativeLayout
import ar.edu.unicen.exa.bconmanager.Adapters.PDRAdapter
import ar.edu.unicen.exa.bconmanager.Adapters.ParticleFilterAdapter
import ar.edu.unicen.exa.bconmanager.Model.BeaconDevice
import ar.edu.unicen.exa.bconmanager.Model.Location
import ar.edu.unicen.exa.bconmanager.Model.PositionOnMap
import ar.edu.unicen.exa.bconmanager.R
import ar.edu.unicen.exa.bconmanager.Service.BluetoothScanner
import ar.edu.unicen.exa.bconmanager.Service.Algorithm.PDRService
import ar.edu.unicen.exa.bconmanager.Service.Algorithm.ParticleFilterService
import ar.edu.unicen.exa.bconmanager.Service.Algorithm.TrilaterationCalculator

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
    private var trilaterationCalculator = TrilaterationCalculator.instance
    private lateinit var pfAdapter: ParticleFilterAdapter
    private lateinit var pdrAdapter: PDRAdapter

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
        super.displayMap()
        setUpParticleFilter()
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

    fun setUpParticleFilter() {

        // Set up trilateration and fingerprinting
        startScan(bluetoothScanner)

        // Set up PDR
        pdrService.bearingAdjustment = (floorMap.angle / 57.2958).toFloat()
        Log.d("ADJUSTMENT", "SAVED PF Adjustment is ${pdrService.bearingAdjustment}")
        Log.d("ADJUSTMENT", "SAVED PF Adjustment is ${pdrService.bearingAdjustment * 57.2958}°")
        pdrAdapter = PDRAdapter(this)
        pdrService.startPDR()

        particleFilterService = ParticleFilterService.getInstance(this.applicationContext, floorMap, pfAdapter)


        //Replace with trilat / fingerprinting position
        val xPos = 0.5
        val yPos = 0.5
        setStartingPoint(xPos, yPos)

        Log.d("PFACTIVITY", "Startup point set")

        //particleFilterService.stop()
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

    private fun drawTrilaterationPoint(location: Location) {
        val particle = PositionOnMap(location)
        particle.image = R.drawable.finger_zone_blue
        var current = ImageView(this)
        currentTrilatView = current;
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

        val resultLocation = trilaterationCalculator.getPositionInMap(floorMap)
        Log.d("PFACTIVITY", "Updating trilateration location to $resultLocation")
        val trilatLocationOnMap: Location
        if (resultLocation != null) {
            trilatLocationOnMap = Location(resultLocation!!.x, resultLocation.y, floorMap)
            currentTrilatPosition = PositionOnMap(trilatLocationOnMap!!)
        }
    }


    fun advanceStep(movedX: Double, movedY: Double) {

        Log.d("PFACTIVITY", "Advanced an step, calculate trilateration location")

        removeResource(currentTrilatView)
        drawTrilaterationPoint(currentTrilatPosition.position)

        Log.d("PFACTIVITY-PRE", "Trilateration location is $currentTrilatPosition")
        Log.d("PFACTIVITY-PRE", "Current location is       (${currentPosition.position.getXMeters()}, ${currentPosition.position.getYMeters()})")
        Log.d("PFACTIVITY-PRE", "Current location2 is      (${currentPosition.position.x}, ${currentPosition.position.y})")
        Log.d("PFACTIVITY-PRE", "Moved length is           ($movedX, $movedY)")

        particleFilterService!!.updatePosition(movedX, movedY,
                currentTrilatPosition.position.getXMeters(), currentTrilatPosition.position.getYMeters())

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

        val layoutParams = RelativeLayout.LayoutParams(70, 70) // value is in pixels
        Log.d(TAG, "Location before update: " + currentPosition.toString())
        currentPosition.position = validatePosition(loc)
        Log.d(TAG, "Location after update: " + currentPosition.toString())
        layoutParams.leftMargin = currentPosition.position.getX() - 35
        layoutParams.topMargin = currentPosition.position.getY() - 35
        positionView.layoutParams = layoutParams
    }

}
