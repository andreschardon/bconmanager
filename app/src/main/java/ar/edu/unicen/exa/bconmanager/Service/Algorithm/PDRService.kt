package ar.edu.unicen.exa.bconmanager.Service.Algorithm

import android.hardware.SensorManager
import android.util.Log
import ar.edu.unicen.exa.bconmanager.Adapters.PDRAdapter
import ar.edu.unicen.exa.bconmanager.Model.AveragedTimestamp
import ar.edu.unicen.exa.bconmanager.Model.CustomMap
import ar.edu.unicen.exa.bconmanager.Model.Location
import ar.edu.unicen.exa.bconmanager.Model.PositionOnMap
import ar.edu.unicen.exa.bconmanager.Service.Sensors.DeviceOrientationHandler
import ar.edu.unicen.exa.bconmanager.Service.Sensors.StepDetectionHandler
import java.math.BigDecimal
import kotlin.math.roundToInt

class PDRService : Algorithm() {

    private var sensorManager: SensorManager? = null
    private var stepDetectionHandler: StepDetectionHandler? = null
    private var deviceOrientationHandler: DeviceOrientationHandler? = null
    private var isWalking = true
    var bearingAdjustment = 0.0f
    private var isRecordingAngle = false
    override var TAG = "PDR Service"
    private var recordCount = 0
    private lateinit var nextPosition: Location
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
    private var isSimulation = false
    private var index = 0

    private object Holder {
        val INSTANCE = PDRService()
    }

    companion object {
        val instance: PDRService by lazy { Holder.INSTANCE }
    }

    override fun startUp(map: CustomMap) {
        super.startUp(map)
        this.initialPosition = true
    }


    private val mStepDetectionListener = StepDetectionHandler.StepDetectionListener { stepSize ->
        if (stepSize > 0) {
            Log.d("ADJUSTMENT", "Its walking")
            isWalking = true
        } else {
            isWalking = false
        }

        if (!isRecordingAngle) {
            angle = (deviceOrientationHandler!!.orientationVals[0] + bearingAdjustment) * 57.2958
            //in this case stepSize is acceleration
            acceleration = if (stepSize >= 0) stepSize else 0F // Only use positive acceleration values
            if (PDREnabled) {
                nextPosition = computeNextStep(stepSize, (deviceOrientationHandler!!.orientationVals[0] + bearingAdjustment))
                pdrAdapter!!.StepDetected()
            }
        } else if (isWalking && isRecordingAngle) {
            recordCount++
            if (recordCount == 3) {
                recordCount = 0
                setAdjustedBearing(deviceOrientationHandler!!.orientationVals[0])
                isRecordingAngle = false
            }

        }
    }

    fun setAdjustedBearing(measuredAngle: Float) {
        val adjustmentFactor = 0 // 90 degrees
        Log.d("ADJUSTMENT", "Measured angle is ${measuredAngle * 57.2958}")
        Log.d("ADJUSTMENT", "It should be ${adjustmentFactor * 57.2958}")
        bearingAdjustment = -measuredAngle
        Log.d("ADJUSTMENT", "Adjustment is ${bearingAdjustment * 57.2958}")
        isRecordingAngle = false
        if (pdrAdapter != null) {
            pdrAdapter!!.stopRecordingAngle()
        }
    }

    fun startRecordingAngle() {
        isRecordingAngle = true
    }

    fun startPDR() {
        PDREnabled = true
    }

    override fun getNextPosition(dataEntry: AveragedTimestamp): Location {
        isSimulation = true
        if (initialPosition) {
            this.mCurrentLocation = Location(dataEntry.positionX, dataEntry.positionY, customMap)
            initialPosition = false
        }

        this.movedX = 0.0
        this.movedY = 0.0
        val startLocation = this.mCurrentLocation!!.clone()
        Log.d("AVERAGED", "Moved distance is 0, and startPosition is $startLocation")

        var resultPosition = Location(-1.0, -1.0, customMap)

        repeat(dataEntry.timeList.size) {
            Log.d("AVERAGED", "GetNextNthPosition step $it angle ${dataEntry.angleList[it]} acc ${dataEntry.accelerationList[it]}")
            resultPosition = getNextNthPosition(dataEntry, it)
        }
        Log.d("AVERAGED", "Result location is $resultPosition")
        movedDistance(startLocation)

        return resultPosition

    }

    private fun getNextNthPosition(dataEntry: AveragedTimestamp, index: Int): Location {
        val steps = getStepsDone(dataEntry.timeList[index], dataEntry.accelerationList[index])
        var i = 0
        if (steps != 0) {
            this.mPrevLocation = this.mCurrentLocation!!.clone()
            while (i < steps) {
                var newPositionUnrestricted = computeNextStep(stepSize, dataEntry.angleList[index].toFloat())
                nextPosition = customMap.restrictPosition(PositionOnMap(newPositionUnrestricted)).position
                this.mCurrentLocation = nextPosition
                i++
            }
        }

        return this.mCurrentLocation!!
    }

    fun startSensorsHandlers() {
        if ((stepDetectionHandler != null) && (deviceOrientationHandler != null)) {
            stepDetectionHandler!!.start()
            deviceOrientationHandler!!.start()
        }

    }

    fun stopSensorsHandlers() {
        if ((stepDetectionHandler != null) && (deviceOrientationHandler != null)) {
            stepDetectionHandler!!.stop()
            deviceOrientationHandler!!.stop()
        }
        Log.d("PDRActivity", "STOP SENSORS HANDLERS")
    }

    fun setupSensorsHandlers(loc: Location, adapter: PDRAdapter, sm: SensorManager, rawData: Boolean) {
        pdrAdapter = adapter
        this.sensorManager = sm
        stepDetectionHandler = StepDetectionHandler(sensorManager, rawData)
        stepDetectionHandler!!.setStepListener(mStepDetectionListener)
        deviceOrientationHandler = DeviceOrientationHandler(sensorManager)
        setmCurrentLocation(loc)
        stepDetectionHandler!!.start()
        deviceOrientationHandler!!.start()
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
    private fun computeNextStep(stepSize: Float, bearing: Float): Location {

        val newLoc = mCurrentLocation
        val oldX = mCurrentLocation!!.getXMeters()
        val oldY = mCurrentLocation!!.getYMeters()
        val bearingD = bearing.toDouble()
        var adjustedAngle = bearingD
        var factor = 0.5
        if (isSimulation) {
            // To radians and considering adjustment
            adjustedAngle = ((bearingD) / 57.2958)
            factor = 1.9
        }

        //reconversion to degrees
        advancedX = Math.cos(adjustedAngle.toDouble()) * stepSize * factor
        advancedY = Math.sin(adjustedAngle.toDouble()) * stepSize * factor

        val newX = oldX + advancedX
        newLoc!!.x = newX.roundTo2DecimalPlaces()
        val newY = oldY + advancedY
        newLoc!!.y = newY.roundTo2DecimalPlaces()
        this.mCurrentLocation = newLoc
        return newLoc!!
    }

    fun Double.roundTo2DecimalPlaces() =
            BigDecimal(this).setScale(2, BigDecimal.ROUND_HALF_UP).toDouble()

    fun getAcc(): Float {
        return this.acceleration
    }

    fun getAngle(): Double {
        return this.angle
    }

    private fun getStepsDone(ts: Float, acc: Float): Int {
        var vel = 0.0F
        val t = ts.div(1000F)
        vel = t * acc
        val tsq = Math.pow(t.toDouble(), 2.0)
        val traveledDistance = vel * t + (0.5 * acc * tsq)
        val stepsDone = (traveledDistance / stepSize)
        if (stepsDone.roundToInt() == 0 && acc >= 0.2) {
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

    private fun movedDistance(startLocation: Location) {
        this.movedX = this.mCurrentLocation!!.x.roundTo2DecimalPlaces() - startLocation.x.roundTo2DecimalPlaces()
        this.movedY = this.mCurrentLocation!!.y.roundTo2DecimalPlaces() - startLocation.y.roundTo2DecimalPlaces()
    }
}
