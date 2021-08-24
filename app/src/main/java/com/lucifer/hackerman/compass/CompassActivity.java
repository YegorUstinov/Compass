package com.lucifer.hackerman.compass;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

public class CompassActivity extends AppCompatActivity {
    private float[] aValue = new float[3];
    private float[] mValue = new float[3];
    private CompassView compassView;
    private SensorManager sensorManager;
    private int rotation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        compassView = (CompassView) findViewById(R.id.compassView);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        String windoSrvc = Context.WINDOW_SERVICE;
        WindowManager wm = ((WindowManager) getSystemService(windoSrvc));
        Display display = wm.getDefaultDisplay();
        rotation = display.getRotation();

        updateOrientation(new float[]{0, 0, 0});
    }


    private float[] calculateOrientation() {
        float[] values = new float[3];
        float[] inR = new float[9];
        float[] outR = new float[9];

        SensorManager.getRotationMatrix(inR, null, aValue, mValue);

        int x_axis = SensorManager.AXIS_X;
        int y_axis = SensorManager.AXIS_Y;

        switch (rotation) {
            case (Surface.ROTATION_90):
                x_axis = SensorManager.AXIS_Y;
                y_axis = SensorManager.AXIS_MINUS_X;
                break;
            case (Surface.ROTATION_180):
                y_axis = SensorManager.AXIS_MINUS_Y;
                break;
            case (Surface.ROTATION_270):
                x_axis = SensorManager.AXIS_MINUS_Y;
                y_axis = SensorManager.AXIS_X;
                break;
            default:
                break;
        }
        SensorManager.remapCoordinateSystem(inR, x_axis, y_axis, outR);

        SensorManager.getOrientation(outR, values);

        values[0] = (float) Math.toDegrees(values[0]);
        values[1] = (float) Math.toDegrees(values[1]);
        values[2] = (float) Math.toDegrees(values[2]);
        return values;
    }

    // low pass filter
    static final float ALFA = 0.01f;

    protected float[] LowPass(float[] input, float[] output) {
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALFA * (input[i] - output[i]);
        }
        return output;
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                aValue = LowPass(event.values, aValue);
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                mValue = LowPass(event.values, mValue);
            updateOrientation(calculateOrientation());
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

    };

    private void updateOrientation(float[] values) {
        if (compassView != null) {
            compassView.setBearing(values[0]);
            compassView.setPitch(values[1]);
            compassView.setRoll(-values[2]);
            compassView.invalidate();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor magField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(sensorEventListener, magField, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        sensorManager.unregisterListener(sensorEventListener);
        super.onPause();
    }
}
