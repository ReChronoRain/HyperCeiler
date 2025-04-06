package com.sevtinge.hyperceiler.ui.holiday;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Surface;

public final class GravitySensor implements SensorEventListener {
	private final SensorManager sensorManager;
	private float[] magneticValues;
	private float[] accelerometerValues;
	private int orientation;
	private int speed;
	private boolean started;
	private final Context context;
	private final WeatherView weatherView;

	public GravitySensor(Context context, WeatherView weatherView) {
		super();
		this.context = context;
		this.weatherView = weatherView;
		this.sensorManager = (SensorManager)this.context.getSystemService(Context.SENSOR_SERVICE);
	}

	public final boolean getStarted() {
		return this.started;
	}

	public void setOrientation(int orient) {
		this.orientation = orient;
	}

	public void setSpeed(int spd) {
		this.speed = spd;
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {}

	public void onSensorChanged(SensorEvent event) {
		if (event == null || event.sensor == null) return;
		switch (event.sensor.getType()) {
			case 1: this.accelerometerValues = event.values; break;
			case 2: this.magneticValues = event.values; break;
		}
		if (this.magneticValues == null || this.accelerometerValues == null) return;

		float[] rotationMatrix = new float[9];
		float[] remappedRotationMatrix = new float[9];
		float[] orientationAngles = new float[3];

		SensorManager.getRotationMatrix(rotationMatrix, null, this.accelerometerValues, this.magneticValues);
		SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remappedRotationMatrix);
		SensorManager.getOrientation(remappedRotationMatrix, orientationAngles);

		double roll = Math.toDegrees(orientationAngles[2]) + Math.random() * 20 - 10;
		switch (this.orientation) {
			case Surface.ROTATION_90:
				roll += 90;
				break;
			case Surface.ROTATION_270:
				roll -= 90;
				break;
			case Surface.ROTATION_180:
				roll += roll > 0 ? 180 : -180;
				break;
		}

		if (roll > 90) roll -= 180;
		else if (roll < -90) roll += 180;

		this.weatherView.setAngle((int) roll);
		this.weatherView.setSpeed(this.speed + (int) Math.round(Math.random() * 20 - 10));
	}

	private void registerListener() {
		this.sensorManager.registerListener(this, this.sensorManager.getDefaultSensor(1), 2);
		this.sensorManager.registerListener(this, this.sensorManager.getDefaultSensor(2), 2);
	}

	private void unregisterListener() {
		this.sensorManager.unregisterListener(this);
	}

	public void start() {
		this.started = true;
		this.registerListener();
	}

	public void stop() {
		this.started = false;
		this.unregisterListener();
	}

	public void onResume() {
		if (this.started) {
			this.registerListener();
		}
	}

	public void onPause() {
		this.unregisterListener();
	}

	public Context getContext() {
		return this.context;
	}

	public WeatherView getWeatherView() {
		return this.weatherView;
	}

}
