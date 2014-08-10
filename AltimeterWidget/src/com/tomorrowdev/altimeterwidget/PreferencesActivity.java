package com.tomorrowdev.altimeterwidget;

import java.util.Calendar;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

public class PreferencesActivity extends Activity implements SensorEventListener, LocationListener{

	private float mitjana;
	private EditText editText, interval;
	private Button gps, OK, cancel;
	private RadioButton meters, feets;
	private TextView metersTV;
	private AdView adView;
	
	private SharedPreferences prefs;
	private SensorManager sM;
	private LocationManager locationManager;
	private String provider;
	
	private int numberOfMeasures;
	private float[] measures = new float[5];
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.preferences_activity_layout);
		
		prefs = getApplicationContext().getSharedPreferences("AltimeterSettings", Context.MODE_PRIVATE);
		sM = (SensorManager)getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
	    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	    Criteria criteria = new Criteria();
	    provider = locationManager.getBestProvider(criteria, false);
		
		editText = (EditText)findViewById(R.id.altitudeET);
		gps = (Button)findViewById(R.id.gps);
		OK = (Button)findViewById(R.id.OKButton);
		cancel = (Button)findViewById(R.id.cancelButton);
		meters = (RadioButton)findViewById(R.id.meters);
		feets = (RadioButton)findViewById(R.id.feets);
		metersTV = (TextView)findViewById(R.id.metersTV);
		interval = (EditText)findViewById(R.id.intervalET);
		
		if(prefs.getBoolean("meters", true)){
			meters.setChecked(true);
			metersTV.setText("meters");
		}else{
			feets.setChecked(true);
			metersTV.setText("feets");
		}
		
		OK.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				
				mitjana = (measures[0]+measures[1]+measures[2]+measures[3]+measures[4])/5;
				
				float PSL = -1;
				if(""+editText.getText() != ""){
					if(prefs.getBoolean("meters", true)){
						PSL = getPSLInhPaFromMeters(Integer.valueOf(""+editText.getText()));
					}else{ //feets
						PSL = getPSLInhPaFromFeets(Integer.valueOf(""+editText.getText()));
					}
				}
				
				Editor editor = prefs.edit();
				editor.putFloat("PSL", PSL);
				editor.commit();
				
				if(""+interval.getText() != ""){
					Intent i= new Intent(PreferencesActivity.this, UpdateWidgetService.class);
					Calendar cal = Calendar.getInstance();
			        cal.add(Calendar.SECOND, 10);
			    
			        PendingIntent pintent = PendingIntent.getService(PreferencesActivity.this, 0, i, 0);
			        
			        String text = ""+interval.getText();
			        int minutes = Integer.valueOf(text);
			        
			        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
			        alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), minutes*60*1000, pintent);
			        
					startService(i); 
				}
				
				finish();
			}
		});
		cancel.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		gps.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
				boolean enabled = service.isProviderEnabled(LocationManager.GPS_PROVIDER);

				if (!enabled) {					
					AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(PreferencesActivity.this);
				     
					alertDialogBuilder.setTitle("GPS not enabled");
					alertDialogBuilder.setMessage("Your GPS is not enabled. Do you want to go Settings and enable it?");
					alertDialogBuilder.setPositiveButton(getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,int id) {
							Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
							startActivity(intent);
						}
					});
					alertDialogBuilder.setNegativeButton(getResources().getString(android.R.string.no), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,int id) {
							dialog.cancel();
						}
					});					 
					AlertDialog alertDialog = alertDialogBuilder.create();
					alertDialog.show();
				}else{
				    locationManager.requestLocationUpdates(provider, 1000, 1, PreferencesActivity.this);
				}
			}
		});
		meters.setOnCheckedChangeListener(new OnCheckedChangeListener() {			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					metersTV.setText("meters");
				}else{
					metersTV.setText("feets");
				}
				Editor editor = prefs.edit();
				editor.putBoolean("meters", isChecked);
				editor.commit();
			}
		});
		
	    adView = (AdView) this.findViewById(R.id.adView);
	}
	
	@Override
	protected void onResume() {
		super.onResume();		
		numberOfMeasures = 0;	    
	    sM.registerListener(this, sM.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_NORMAL);
	    
	    AdRequest adRequest = new AdRequest.Builder()
				.addTestDevice("252B9BC3B255D185B51C2541B929CA39")
				.build();
	    adView.loadAd(adRequest);
	}
	
	@Override
	protected void onPause() {
		super.onPause();		
		sM.unregisterListener(this);
		locationManager.removeUpdates(this);
		
		adView.destroy();
	}

	private float getPSLInhPaFromMeters(int altitude){
		
		//float PSL = (float) (mitjana/(Math.pow((1 - 6.87535 * Math.pow(10, -6) * altitude * 0.3048), 5.2561)));		
		
		float PSL = (float) (mitjana/Math.pow((1 - (altitude/(145366.45*0.3048))), (1/0.190284)));
		return PSL;
	}
	
	private float getPSLInhPaFromFeets(int altitude){
		
		float PSL = (float) (mitjana/(Math.pow((1 - 6.87535 * Math.pow(10, -6) * altitude), 5.2561)));	
		return PSL;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
			measures[numberOfMeasures] = event.values[0];
			
			numberOfMeasures++;
			
			if(numberOfMeasures == 5){	
				numberOfMeasures = 0;
			}
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		if(prefs.getBoolean("meters", true)){
			editText.setText(""+(int)location.getAltitude());
		}else{
			editText.setText(""+(int)location.getAltitude()/0.3048);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor event, int arg1) {		
	}
	@Override
	public void onProviderDisabled(String provider) {
	}
	@Override
	public void onProviderEnabled(String provider) {
	}
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
}
