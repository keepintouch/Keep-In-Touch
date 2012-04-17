package app.master.kit;

import java.util.Iterator;

import android.content.Context;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

class MyLocation{
	// Log Tag
	private static final String LOG_TAG = "KIT_LOG";

	// Minimum number of satellites to use
	private static final int min_gps_sat_count = 5;

	// Log Stats about GPS
	private static int log_stats = 1;

	// Delay for trying again
	private static final int delay = 500;

	LocationResult locationResult;
	private Location curLocation = null;
	private Handler handler = new Handler();
	private LocationManager lm;
	public Context context;
	private boolean gps_enabled = false;
	private int counts    = 0;
	private int sat_count = 0;
	private int max_counts = 120; // max_counts/2 = seconds to poll GPS
	private long current_time = System.currentTimeMillis()-((max_counts/2)*1000);  // 60 seconds ago

	private Runnable showTime = new Runnable() {
		public void run() {
			boolean stop = false;
			counts++;
			//Log.v("A","counts=" + counts);

			//if timeout (1 min) exceeded, stop trying
			if(counts > max_counts) {
				stop = true;
			}

			//update last best location
			curLocation = getLocation(context);

			//if best location is known, calculate if we need to continue to look for better location
			//if gps is enabled and min satellites count has not been connected or min check count is smaller then 4 (2 sec)  
			if(stop == false && !needToStop()) {
				//Log.v("A","Connected " + sat_count + " satellites. continue waiting..");
				handler.postDelayed(this, delay);
			}
			else {
				//Log.v("A","#########################################");
				//Log.v("A","BestLocation found return result to main. sat_count=" + sat_count);
				//Log.v("A","#########################################");
				// removing all updates and listeners
				lm.removeUpdates(gpsLocationListener);
				//myLocationManager.removeUpdates(networkLocationListener);    
				lm.removeGpsStatusListener(gpsStatusListener);
				// send best location to locationResult
				if (sat_count < min_gps_sat_count) {
					// If our sat_count isn't enough, then return null
					curLocation = null;
					Log.i(LOG_TAG, "GPS Location Failed: sat_count="+sat_count);
				}

				if (log_stats == 1) {
					// LOG STATS
					if (curLocation != null)
					{ Log.i(LOG_TAG, "STAT: "+(counts/2)+" seconds to aquire GPS Location (sat_count="+sat_count+")."); }
				}

				// Reset sat_count and return location
				sat_count = 0;
				locationResult.gotLocation("gps", curLocation, false);
			}
		}
	};

	/**
	 * Determine if continue to try to find best location
	 */
	private Boolean needToStop() {
		if(gps_enabled) {
			if(counts <= 4) {
				// Let GPS poll for at least 2 seconds
				return false;
			}
			if(sat_count < min_gps_sat_count) {
				//if 20-25 sec and 3 satellites found then stop
				if(counts >= 40 && sat_count >= 3) {
					if (curLocation != null) {
						// Make sure our GPS update time was within the last minute
						if (curLocation.getTime() >= current_time) {
							return true;
						}
					}
					return false;
				}
				return false;
			}
		}
		else {
			return true;
		}
		if (curLocation != null) {
			// Make sure our GPS update time was within the last minute
			if (curLocation.getTime() >= current_time) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Got location abstract result class
	 */
	public static abstract class LocationResult{
		public abstract void gotLocation(String locationProvider, Location location, boolean gps_off);
	}

	/**
	 * Initialize starting values and fire up location manager providers
	 * @param ctx
	 * @param result
	 */
	public void init(Context ctx, LocationResult result){
		context = ctx;
		locationResult = result;

		lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		gps_enabled = (Boolean) lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

		curLocation = null;
		counts = 0;

		if (gps_enabled) {
			// turning on location updates
			//myLocationManager.requestLocationUpdates("network", 0, 0, networkLocationListener);
			lm.requestLocationUpdates("gps", 0, 0, gpsLocationListener);
			lm.addGpsStatusListener(gpsStatusListener);

			// starting best location finder loop
			handler.postDelayed(showTime, delay);
		}
		else {
			locationResult.gotLocation("gps", curLocation, true);
		}

	}

	/**
	 * GpsStatus listener. OnChainged counts connected satellites count.
	 */
	public final GpsStatus.Listener gpsStatusListener = new GpsStatus.Listener() {
		public void onGpsStatusChanged(int event) {

			if(event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
				try {
					// Check number of satellites in list to determine fix state
					GpsStatus status = lm.getGpsStatus(null);
					Iterable<GpsSatellite>satellites = status.getSatellites();

					sat_count = 0;

					Iterator<GpsSatellite>satI = satellites.iterator();
					while(satI.hasNext()) {
						satI.next();
						//GpsSatellite satellite = satI.next();
						//Log.v("A","Satellite: snr=" + satellite.getSnr() + ", elevation=" + satellite.getElevation());                         
						sat_count++;
					}
				} catch (Exception e) {
					e.printStackTrace();
					sat_count = min_gps_sat_count + 1;
				}

				//Log.v("A","#### sat_count = " + sat_count);
			}
		}
	};

	/**
	 * Gps location listener.
	 */
	public final LocationListener gpsLocationListener = new LocationListener(){
		public void onLocationChanged(Location location){

		}
		public void onProviderDisabled(String provider){}
		public void onProviderEnabled(String provider){}
		public void onStatusChanged(String provider, int status, Bundle extras){}
	}; 

	/**
	 * Network location listener.
	 */
	public final LocationListener networkLocationListener = new LocationListener(){
		public void onLocationChanged(Location location){

		}
		public void onProviderDisabled(String provider){}
		public void onProviderEnabled(String provider){}
		public void onStatusChanged(String provider, int status, Bundle extras){}
	}; 


	/**
	 * Returns current location using LocationManager.getBestProvider()
	 * 
	 * @param context
	 * @return Location|null
	 */
	public static Location getLocation(Context context) {
		//Log.v("A","getLocation()");

		// fetch last known location and update it
		try {
			LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_FINE);
			criteria.setAltitudeRequired(false);
			criteria.setBearingRequired(false);
			criteria.setCostAllowed(true);
			String strLocationProvider = lm.getBestProvider(criteria, true);

			//Log.v("A","strLocationProvider=" + strLocationProvider);
			Location location = lm.getLastKnownLocation(strLocationProvider);
			if(location != null) {
				return location;
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
