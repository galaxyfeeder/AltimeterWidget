package com.tomorrowdev.altimeterwidget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class UpdateWidgetService extends Service implements SensorEventListener{
	
	private int[] allWidgetIds;
	private AppWidgetManager appWidgetManager;
	private SharedPreferences prefs;
	private SensorManager sM;
	
	private int numberOfMeasures;
	private float[] measures = new float[5];
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		prefs = getApplicationContext().getSharedPreferences("AltimeterSettings", Context.MODE_PRIVATE);
		sM = (SensorManager)getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
		
	    appWidgetManager = AppWidgetManager.getInstance(this.getApplicationContext());

	    ComponentName thisWidget = new ComponentName(getApplicationContext(), WidgetProvider.class);
	    allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

	    numberOfMeasures = 0;
	    
	    sM.registerListener(this, sM.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_NORMAL);
	    
		return Service.START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		sM.unregisterListener(this);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {

			measures[numberOfMeasures] = event.values[1];
			
			Log.d("Measures", "Measure "+numberOfMeasures+": "+event.values[0]);
			
			numberOfMeasures++;
			
			if(numberOfMeasures == 4){
				
				float mitjana = (measures[0]+measures[1]+measures[2]+measures[3]+measures[4])/5;
				
				float h = -1;
				if(prefs.getBoolean("meters", true)){
					h = getMetersFromhPa(mitjana);
				}else{ //feets
					h = getFeetsFromhPa(mitjana);
				}
				
				for (int widgetId : allWidgetIds) {

			    	RemoteViews remoteViews = new RemoteViews(this.getApplicationContext().getPackageName(), R.layout.widget_layout);
			    	// Set the text
			    	remoteViews.setTextViewText(R.id.altitude, ""+(int)h);

			    	// Register an onClickListener
			    	Intent clickIntent = new Intent(this.getApplicationContext(), WidgetProvider.class);

			    	clickIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			    	clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);

			    	PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			    	remoteViews.setOnClickPendingIntent(R.id.layout, pendingIntent);
			    	appWidgetManager.updateAppWidget(widgetId, remoteViews);
			    }
				
				stopSelf();
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}
	
	public float getMetersFromhPa(float hPa){
		
		float PSL = prefs.getFloat("PSL", 1013.25f);
		
		float hm = (float) (1 - Math.pow(hPa/PSL, 0.190284)*145366.45*0.3048);
		
		return hm;
	}
	
	public float getFeetsFromhPa(float hPa){
		
		float PSL = prefs.getFloat("PSL", 1013.25f);
		
		float halt = (float) (1 - Math.pow(hPa/PSL, 0.190284)*145366.45);
		
		return halt;
	}
}
