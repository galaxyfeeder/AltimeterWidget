package com.tomorrowdev.altimeterwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public class WidgetProvider extends AppWidgetProvider {

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

		// Build the intent to call the service
	    Intent intent = new Intent(context.getApplicationContext(), UpdateWidgetService.class);

	    // Update the widgets via the service
	    context.startService(intent);
	}
}
