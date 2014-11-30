package com.tomorrowdev.altimeterwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;

public class WidgetProvider extends AppWidgetProvider {

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

		SensorManager mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		if(mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) == null){
			//TODO show alert
		}
		
	    Intent intent = new Intent(context.getApplicationContext(), UpdateWidgetService.class);
	    context.startService(intent);
	}
}
