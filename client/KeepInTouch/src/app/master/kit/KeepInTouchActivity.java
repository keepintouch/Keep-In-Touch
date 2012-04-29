package app.master.kit;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import app.master.kit.FriendsActivity;
import app.master.kit.LocationMapActivity;

public abstract class KeepInTouchActivity extends Activity {
	/** Called when the activity is first created. */
	public static final String LOG_TAG = "KIT_LOG";
	public static int prefsLoaded = 0;
	public StorageDB storageDB;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	//Lifecycle Methods

	protected void onDestroy() {
		super.onDestroy();
	}

	protected void onStop() {
		super.onStop();
	}

	protected void onStart() {
		super.onStart();
	}

	protected void onPause() {
		super.onPause();
	}

	protected void onResume() {
		super.onResume();
	}

	protected boolean isNetworkAvailable() {
		// Test if any network is available for use, or if they are all unavailable
		// Requires:     <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
	    ConnectivityManager connectivityManager 
	          = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null;
	}

	// Service Checker 
	protected boolean isPollServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if ("app.master.kit.PollService".equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	protected void StartPollService(String svcCommand) {
		// Commands: UPDATE_AND_POLL, UPDATE, POLL
		Intent svc = new Intent(this, PollService.class);
		if (svcCommand.equals("UPDATE_AND_POLL") || svcCommand.equals("UPDATE") || svcCommand.equals("POLL")) {
			svc.putExtra("CMD", svcCommand);
		}
		startService(svc);
	}    


	//App Click Methods

	public void return2Home(Context context) {
		final Intent myIntent = new Intent(context, MainActivity.class);
		myIntent.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(myIntent);
	}

	public void onHome(View v) {
		return2Home(this);
	}

	public void onFriends(View v) {
		Intent myIntent = new Intent(this, FriendsActivity.class);
		startActivity(myIntent);
	}

	public void onMap(View v) {
		if (isNetworkAvailable()) {
			Intent myIntent = new Intent(this, LocationMapActivity.class);
			startActivity(myIntent);
		}
		else {
			Toast.makeText(getApplicationContext(), "Network Not Available", Toast.LENGTH_LONG).show();
		}
	}

	public void onPOI(View v) {
		Intent myIntent = new Intent(this, FavoritesActivity.class);
		myIntent.setFlags (Intent.FLAG_ACTIVITY_NO_HISTORY);
		startActivity(myIntent);		
	}

	public void onHistory(View v) {
		Intent myIntent = new Intent(this, HistoryActivity.class);
		startActivity(myIntent);
	}

}