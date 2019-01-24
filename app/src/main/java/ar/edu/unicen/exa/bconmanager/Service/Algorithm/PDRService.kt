package ar.edu.unicen.exa.bconmanager.Service.Algorithm

import android.hardware.SensorManager
import android.util.Log
import ar.edu.unicen.exa.bconmanager.Adapters.PDRAdapter
import ar.edu.unicen.exa.bconmanager.Model.Json.JsonData
import ar.edu.unicen.exa.bconmanager.Model.Location
import ar.edu.unicen.exa.bconmanager.Model.PositionOnMap
import ar.edu.unicen.exa.bconmanager.Service.DeviceAttitudeHandler
import ar.edu.unicen.exa.bconmanager.Service.StepDetectionHandler
import kotlin.math.roundToInt

class PDRService : Algorithm(){

    private var sensorManager: SensorManager? = null
    private var stepDetectionHandler: StepDetectionHandler? = null
    private var deviceAttitudeHandler: DeviceAttitudeHandler? = null
    private var isWalking = true
    var bearingAdjustment = 0.0f
    private var isRecordingAngle = false
    override var  TAG = "PDR Service"
    private var recordCount = 0
    private lateinit var nextPosition : Location
    private var mCurrentLocation: Location? = null
    private var mPrevLocation: Location? = null
    var advancedX = 0.0
    var advancedY = 0.0
    private var movedX = 0.0
    private var movedY = 0.0
    private var pdrAdapter: PDRAdapter? = null
    private var PDREnabled = false
    private var angle = 0.0
    private var acceleration = 0.0F
    private var stepSize = 0.2F
    private var initialPosition = true

    private object Holder {
        val INSTANCE = PDRService()
    }

    companion object {
        val instance: PDRService by lazy { Holder.INSTANCE }
    }


    private val mStepDetectionListener = StepDetectionHandler.StepDetectionListener { stepSize ->
        if (!isRecordingAngle) {
            angle = (deviceAttitudeHandler!!.orientationVals[0] + bearingAdjustment)*57.2958
            //in this case stepSize is acceleration
            acceleration = if (stepSize >= 0) stepSize else 0F // Only use positive acceleration values
            if(PDREnabled) {
                nextPosition = computeNextStep(stepSize, (deviceAttitudeHandler!!.orientationVals[0] + bearingAdjustment))
                //Log.d(TAG, "Location: " + nextPosition.toString() + "  angle: " + (deviceAttitudeHandler!!.orientationVals[0] + bearingAdjustment) * 57.2958)
                //Log.d(TAG, "IS WALKING")
                pdrAdapter!!.StepDetected()
            }
        } else if (isWalking && isRecordingAngle) {
            //Log.d("PDRActivity","IS RECORDING")
            recordCount++
            if (recordCount == 3) {
                recordCount = 0
                setAdjustedBearing(deviceAttitudeHandler!!.orientationVals[0])
                isRecordingAngle = false
            }

        }
    }

    fun setAdjustedBearing(measuredAngle : Float) {
        val adjustmentFactor = 0 // 90 degrees
        Log.d("ADJUSTMENT", "Measured angle is ${measuredAngle*57.2958}")
        Log.d("ADJUSTMENT", "It should be ${adjustmentFactor*57.2958}")
        bearingAdjustment = -measuredAngle
        Log.d("ADJUSTMENT", "Adjustment is ${bearingAdjustment*57.2958}")
        isRecordingAngle = false
        if(pdrAdapter != null) {
            pdrAdapter!!.stopRecordingAngle()
        }
    }

    fun startRecordingAngle() {
        isRecordingAngle = true
    }

    fun startPDR(){
        PDREnabled = true
    }

    override fun getNextPosition(dataEntry: JsonData, t2: Number): Location {
        if (initialPosition){
            this.mCurrentLocation = Location(dataEntry.positionX,dataEntry.positionY,customMap)
            initialPosition = false
        }
        val steps = getStepsDone(dataEntry.timestamp,t2,dataEntry.acceleration)
        var i =0
        while (i<steps) {
            this.mPrevLocation = this.mCurrentLocation
            var asd = computeNextStep(stepSize, dataEntry.angle.toFloat())
          //  Log.d("SIMULATION", "COMPUTED: $asd")
            nextPosition= customMap.restrictPosition(PositionOnMap(asd)).position
            this.mCurrentLocation = nextPosition
            movedDistance()
            i++
        }
        return this.mCurrentLocation!!
    }

     fun startSensorsHandlers() {
        if((stepDetectionHandler != null) && (deviceAttitudeHandler != null)) {
            stepDetectionHandler!!.start()
            deviceAttitudeHandler!!.start()
        }

    }

     fun stopSensorsHandlers() {
        if((stepDetectionHandler != null) && (deviceAttitudeHandler != null)) {
            stepDetectionHandler!!.stop()
            deviceAttitudeHandler!!.stop()
        }
         Log.d("PDRActivity","STOP SENSORS HANDLERS")
    }

    fun setupSensorsHandlers(loc: Location, adapter: PDRAdapter, sm: SensorManager, rawData: Boolean){
        pdrAdapter = adapter
        this.sensorManager = sm
        stepDetectionHandler = StepDetectionHandler(sensorManager, rawData)
        stepDetectionHandler!!.setStepListener(mStepDetectionListener)
        deviceAttitudeHandler = DeviceAttitudeHandler(sensorManager)
        setmCurrentLocation(loc)
        stepDetectionHandler!!.start()
        deviceAttitudeHandler!!.start()
        Log.d("PFACTIVITY", "Sensors handlers")
    }


    fun getmCurrentLocation(): Location {
        return mCurrentLocation!!
    }

    private fun setmCurrentLocation(mCurrentLocation: Location) {
        Log.d(TAG, "CURRENT LOCATION IS : " + mCurrentLocation.toString())
        this.mCurrentLocation = mCurrentLocation
    }

    /** Calculates the new user position from the current one
     * @param stepSize the size of the step the user has made
     * @param bearing the angle of direction
     * @return new location
     */
    private fun computeNextStep(stepSize : Float, bearing : Float) : Location {

        val newLoc = mCurrentLocation
        val oldX = mCurrentLocation!!.getXMeters()
        val oldY = mCurrentLocation!!.getYMeters()

        //reconversion en degrees
        advancedX = Math.cos(bearing.toDouble()) * stepSize
        advancedY = Math.sin(bearing.toDouble()) * stepSize

        val newX = oldX + advancedX
        newLoc!!.x = newX
        val newY = oldY + advancedY
        newLoc!!.y = newY

        mCurrentLocation = newLoc

        return newLoc!!
    }

    fun getAcc() : Float{
        return this.acceleration
    }

    fun getAngle() : Double {
        return this.angle
    }

    private fun getStepsDone(t1: Number, t2: Number, acc: Float) : Int {
        //Log.d("SIMULATION", "$t1 ${t2.toInt()} $acc")
        var vel = 0.0F
        val ts = (t2.toFloat() - t1.toFloat())
        val t = ts.div(1000F)
        //Log.d("SIMULATION", "Time: $t")
        vel = t * acc
        //Log.d("SIMULATION", "Speed: $vel")
        val tsq = Math.pow(t.toDouble(), 2.0)
        val traveledDistance = vel*t + (0.5*acc*tsq)
        //Log.d("SIMULATION", "Traveled: $traveledDistance")
        val stepsDone = (traveledDistance / stepSize)
        //Log.d("SIMULATION", "Steps done: $stepsDone")
        if(stepsDone.roundToInt() == 0 && acc >= 0.2) {
            return 1
        }
        return stepsDone.roundToInt()
    }
    fun getMovedX(): Double {
        return this.movedX
    }
    fun getMovedY(): Double {
        return this.movedY
    }

    private fun movedDistance() {
        this.movedX = this.mPrevLocation!!.x - this.mCurrentLocation!!.x
        this.movedY = this.mPrevLocation!!.y - this.mCurrentLocation!!.y
    }
}
