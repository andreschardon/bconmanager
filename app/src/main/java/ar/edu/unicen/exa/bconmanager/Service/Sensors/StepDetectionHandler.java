package ar.edu.unicen.exa.bconmanager.Service.Sensors;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class StepDetectionHandler extends AppCompatActivity implements
        SensorEventListener {
    private String TAG = "StepDetectionHandler";
    SensorManager sm;
    Sensor sensor;
    boolean rawData;

    private StepDetectionListener mStepDetectionListener;

    int step = 0;

    public StepDetectionHandler(SensorManager sm, boolean rawData) {
        super();
        this.sm = sm;
        this.rawData = rawData;
        sensor = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }

    public void start() {
        sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);

    }

    public void stop() {
        sm.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        float z;

        if (e.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {

            z = e.values[2];

            //threshold from which it is considered that it is a step
            if (z > 1 && mStepDetectionListener != null && !rawData) {
                onNewStepDetected();
            } else if (rawData) {
                Log.d("ADJUSTMENT", "Detected acceleration " + z);
                mStepDetectionListener.newStep(z);
            }

        }
    }

    public void onNewStepDetected() {
        float distanceStep = 0.2f;
        step++;
        mStepDetectionListener.newStep(distanceStep);
    }


    public void setStepListener(StepDetectionListener listener) {
        mStepDetectionListener = listener;
    }

    public interface StepDetectionListener {
        public void newStep(float stepSize);
    }

}