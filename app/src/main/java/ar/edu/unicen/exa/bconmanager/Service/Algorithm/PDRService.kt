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
    lateinit var pdrAdapter: PDRAdapter
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
                Log.d(TAG, "Location: " + nextPosition.toString() + "  angle: " + (deviceAttitudeHandler!!.orientationVals[0] + bearingAdjustment) * 57.2958)
                Log.d(TAG, "IS WALKING")
                pdrAdapter.StepDetected()
            }
        } else if (isWalking && isRecordingAngle) {
            Log.d("PDRActivity","IS RECORDING")
            recordCount++
            if (recordCount == 3) {
                recordCount = 0
                setAdjustedBearing(deviceAttitudeHandler!!.orientationVals[0])
                isRecordingAngle = false
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
        pdrAdapter.stopRecordingAngle()
    }

    fun startRecordingAngle() {
        isRecordingAngle = true
    }

    fun startPDR(){
        PDREnabled = true
    }

    override fun getNextPosition(dataEntry: JsonData, t2: Number): Location {
        Log.d("SIMULATION", "TIMESTAMP: ${dataEntry.timestamp}")
        if (initialPosition){
            this.mCurrentLocation = Location(dataEntry.posX,dataEntry.posY,customMap)
            initialPosition = false
        }
        var steps = getStepsDone(dataEntry.timestamp,t2,dataEntry.acceleration)
        var i =0
        while (i<steps) {
            this.mPrevLocation = this.mCurrentLocation
            nextPosition= customMap.restrictPosition(PositionOnMap(computeNextStep(stepSize,bearingAdjustment))).position
            movedDistance()
            i++
        }
        if(steps == 0) {
            Log.d("SIMULATION", "STEPS == 0 , TIMESTAMP: ${dataEntry.timestamp}")
            return this.mCurrentLocation!!
        }
        else {
            Log.d("SIMULATION", "STEPS: $steps")
            Log.d("SIMULATION", "LOCATION IN SIMULATION: " + nextPosition.toString())
            return nextPosition
        }
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

    fun setmCurrentLocation(mCurrentLocation: Location) {
        Log.d(TAG, "CURRENT LOCATION IS : " + mCurrentLocation.toString())
        this.mCurrentLocation = mCurrentLocation
    }

    /** Calculates the new user position from the current one
     * @param stepSize the size of the step the user has made
     * @param bearing the angle of direction
     * @return new location
     */
    fun computeNextStep(stepSize : Float, bearing : Float) : Location {
        Log.d(TAG, "COMPUTE NEXT STEP")
        val newLoc = mCurrentLocation

        val oldX = mCurrentLocation!!.getXMeters()
        val oldY = mCurrentLocation!!.getYMeters()

        //reconversion en degres

        Log.d(TAG, "STEP: $stepSize")
        Log.d(TAG, "ANgle: $bearing")
        Log.d(TAG, "COS ANgle: " + Math.cos(bearing.toDouble()))

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
        Log.d("PDRActivity", "ACCELERATION: $acceleration")
        return this.acceleration
    }

    fun getAngle() : Double {
        Log.d("PDRActivity", "ANGLE: $angle")
        return this.angle
    }

    fun getStepsDone(t1: Number, t2: Number, acc: Float) : Int {
        var vel = 0.0F
        var t= t2.toInt()-t1.toInt()
        vel = t * acc
        var tsq = Math.pow(t.toDouble(), 2.0)
        var traveledDistance = vel*t + (0.5*acc*tsq)
        return (traveledDistance / stepSize).roundToInt()
    }
    fun getMovedX(): Double {
        return this.movedX
    }
    fun getMovedY(): Double {
        return this.movedY
    }

    fun movedDistance() {
        this.movedX = this.mPrevLocation!!.x - this.mCurrentLocation!!.x
        this.movedY = this.mPrevLocation!!.y - this.mCurrentLocation!!.y
    }
}
