package ar.edu.unicen.exa.bconmanager.Service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class MovementDetector (var context: Context) : SensorEventListener {

    protected val TAG = javaClass.simpleName

    private var sensorMan: SensorManager? = null
    private var accelerometer: Sensor? = null

    //////////////////////
    private val mListeners = HashSet<MovementDetector.Listener>()

    fun init() {
        sensorMan = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorMan!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    }

    fun start() {
        sensorMan!!.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun stop() {
        sensorMan!!.unregisterListener(this)
    }

    fun addListener(listener: Listener) {
        mListeners.add(listener)
    }

    /* (non-Javadoc)
 * @see android.hardware.SensorEventListener#onSensorChanged(android.hardware.SensorEvent)
 */
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val diff = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            if (diff > 0.1)
            // 0.5 is a threshold, you can test it and change it
                Log.d(TAG, "Device motion detected!!!!")
            for (listener in mListeners) {
                listener.onMotionDetected(event, diff)
            }
        }

    }

    /* (non-Javadoc)
 * @see android.hardware.SensorEventListener#onAccuracyChanged(android.hardware.Sensor, int)
 */
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // TODO Auto-generated method stub

    }

    interface Listener {
        fun onMotionDetected(event: SensorEvent, acceleration: Float)
    }

}