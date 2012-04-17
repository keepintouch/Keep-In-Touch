package app.master.kit;

import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import app.master.kit.PreferencesActivity;

public class MainActivity extends KeepInTouchActivity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Intent i = new Intent(getApplicationContext(), Alarm.class);
		boolean alarmUp = (PendingIntent.getBroadcast(getApplicationContext(), 0, i, PendingIntent.FLAG_NO_CREATE) != null); 
		if (! alarmUp) {
			Intent ii = new Intent("app.master.custom.intent.action.STARTALARM");
			getApplicationContext().sendBroadcast(ii);
		}
		if (prefsLoaded == 0) {
			try {
				storageDB.close();
			} catch (NullPointerException e)
			{ }
			storageDB = new StorageDB(this);
			Cursor cursor = storageDB.cursorSelectAllPrefs();
			if (cursor != null) {
				if (cursor.getCount() <= 0) {
					// We don't have any prefs
					// Maybe we should display an alert box telling
					// them that they should go to the Preferences area to setup
					// their preferences.
				}
			}
			//prefsLoaded = 1;
		}
	}

	public void onStart() {
		LocationManager locationManager;
        locationManager = (LocationManager)this.getSystemService(LOCATION_SERVICE);

        // Display a message if GPS isn't able
        boolean gps_enabled = (Boolean) locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!gps_enabled) {
                Toast.makeText(getApplicationContext(), "Please enable GPS in Settings -> Location & Security", Toast.LENGTH_LONG).show();
        }
        locationManager = null;
		super.onStart();
	}
	
	protected void onDestroy() {
		storageDB.close();
		super.onDestroy();
	}	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menuUpdate) {
			// Start up the Polling Service
			if (!isPollServiceRunning()) {
				StartPollService("UPDATE");
				Toast.makeText(getApplicationContext(), "Manual Location Update", Toast.LENGTH_LONG).show();
			}
			else
			{ Toast.makeText(getApplicationContext(), "Another Update Is Already Running...", Toast.LENGTH_LONG).show(); }	        
		}
		else if (item.getItemId() == R.id.menuPrefs) {
			// Open Preferences Screen
			Intent myIntent = new Intent(MainActivity.this, PreferencesActivity.class);
			startActivity(myIntent);
		}
		// Consume the selection event.
		return true;
	}

}