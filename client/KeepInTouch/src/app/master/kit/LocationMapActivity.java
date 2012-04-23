package app.master.kit;

import java.io.IOException;
import java.util.List;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class LocationMapActivity extends MapActivity implements LocationListener, OnClickListener {

	public static Context context;
	
	LocationManager locationManager;
	Geocoder geocoder;
	TextView locationText;
	MapView map;  
	MapController mapController;
	MapOverlays myOverlays = null;
	MapTouchOverlay myTouchOverlay = null;
	MyLocationOverlay myLocationOverlay = null;
	ImageView map_menu_friend;
	ImageView map_menu_mapmode;
	boolean show_friends = false;
    boolean satellite_view = false;
	boolean found_initial_location = false;
	private StorageDB storageDB;

	class MapTouchOverlay extends com.google.android.maps.Overlay
	{
		// http://stackoverflow.com/questions/1678493/android-maps-how-to-long-click-a-map
		private long startTimeForLongClick = 0;
		private int millisecThresholdforLongclick = 500;
		private float xCoordForLongClick;
		private float yCoordForLongClick;
		private float xLow, xHigh, yLow, yHigh;
		@Override
		public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when) {
			return true;
		}

		@Override
		public boolean onTouchEvent(MotionEvent event, MapView mapView) {   
			// Get instance of Vibrator from current Context
			//Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			// Vibrate for 300 milliseconds
			//v.vibrate(300);

			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				startTimeForLongClick = event.getEventTime();
				xCoordForLongClick = event.getX();
				yCoordForLongClick = event.getY();
			}
			else if (event.getAction() == MotionEvent.ACTION_MOVE) {
				if (event.getPointerCount()>1) {  //  See if we're doing a multi-touch or not.
					startTimeForLongClick=0;                        
				} else {
					float xmove = event.getX(); //where is their finger now?                   
					float ymove = event.getY();
					// Using 10 as my "tolerance" for moving while long pressing
					xLow = xCoordForLongClick - 10;
					xHigh= xCoordForLongClick + 10;
					yLow = yCoordForLongClick - 10;
					yHigh= yCoordForLongClick + 10;
					if ((xmove<xLow || xmove> xHigh) || (ymove<yLow || ymove> yHigh)){
						//out of the range of an acceptable long press, reset the whole process
						startTimeForLongClick=0;
					}
				}
			}
			else if (event.getAction() == MotionEvent.ACTION_UP) {
				long eventTime = event.getEventTime();
				long downTime = event.getDownTime(); //this value will match the startTimeForLongClick variable as long as we didn't reset the startTimeForLongClick variable because we detected nonsense that invalidated a long press in the ACTION_MOVE block
				//Toast.makeText(getBaseContext()," HI", Toast.LENGTH_SHORT).show();
				//make sure the start time for the original "down event" is the same as this event's "downTime"
				if (startTimeForLongClick==downTime) { 
					//see if the event time minus the start time is within the threshold
					if ((eventTime-startTimeForLongClick)>millisecThresholdforLongclick){ 
						//make sure we are at the same spot where we started the long click
						float xup = event.getX();                  
						float yup = event.getY();
						//I don't want the overhead of a function call:
						xLow = xCoordForLongClick - 10;
						xHigh= xCoordForLongClick + 10;
						yLow = yCoordForLongClick - 10;
						yHigh= yCoordForLongClick + 10;
						if ((xup>xLow && xup<xHigh) && (yup>yLow && yup<yHigh)) {
							GeoPoint p = mapView.getProjection().fromPixels(
									(int) event.getX(),
									(int) event.getY());
							// Send Favorites Activity our lat/lon from the long click
							Intent myIntent = new Intent(LocationMapActivity.this, FavoritesActivity.class);
							myIntent.putExtra("lat", String.valueOf(p.getLatitudeE6() / 1E6));
							myIntent.putExtra("lon", String.valueOf(p.getLongitudeE6() / 1E6));
							myIntent.setFlags (Intent.FLAG_ACTIVITY_NO_HISTORY);
							startActivity(myIntent);
							//Toast.makeText(getBaseContext(),  
							//		p.getLatitudeE6() / 1E6 + "," + 
							//				p.getLongitudeE6() /1E6 , 
							//				Toast.LENGTH_SHORT).show();
						}
					}
				}
			}                            
			return false;
		}        
	}	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);
		context = getApplicationContext();
		try {
			storageDB.close();
		} catch (NullPointerException e)
		{ }
		storageDB = new StorageDB(this);

		map_menu_friend = (ImageView)this.findViewById(R.id.map_menu_friend);
		map_menu_mapmode = (ImageView)this.findViewById(R.id.map_menu_mapmode);
        locationText = (TextView)this.findViewById(R.id.lblLocationInfo);
		map = (MapView)this.findViewById(R.id.mapview);
		map.setBuiltInZoomControls(true);
		mapController = map.getController();
		mapController.setZoom(16);

		Drawable marker=getResources().getDrawable(R.drawable.ic_maps_pin);
		int markerWidth = marker.getIntrinsicWidth();
		int markerHeight = marker.getIntrinsicHeight();
		marker.setBounds(0, markerHeight, markerWidth, 0);

		myOverlays = new MapOverlays(marker);
		myTouchOverlay = new MapTouchOverlay();
		map.getOverlays().add(myTouchOverlay);
		
		locationManager = (LocationManager)this.getSystemService(LOCATION_SERVICE);

		// Display a message if GPS isn't able
		boolean gps_enabled = (Boolean) locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		if (!gps_enabled) {
			Toast.makeText(getApplicationContext(), "Please enable GPS in Settings -> Location & Security", Toast.LENGTH_LONG).show();
		}

		geocoder = new Geocoder(this);

		Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER); 
		if (location != null) {
			this.onLocationChanged(location);
		}

		myLocationOverlay = new MyLocationOverlay(this, map);
		map.getOverlays().add(myLocationOverlay);
		
		map.setSatellite(satellite_view);
		
		map.postInvalidate();
	}


	public void onClick(View v) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onResume() {
		super.onResume();
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this); //<7>
		myLocationOverlay.enableMyLocation();
		myLocationOverlay.enableCompass();

	}

	@Override
	protected void onPause() {
		super.onPause();
		locationManager.removeUpdates(this); //<8>
		myLocationOverlay.disableMyLocation();
		myLocationOverlay.disableCompass();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			locationManager.removeUpdates(this);
			locationManager = null;
		}
		catch (Exception e) {
			// ignore
		}
		storageDB.close();
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}


	public void onLocationChanged(Location location) {
		String text = String.format("%f, %f", location.getLatitude(), location.getLongitude());
        String address = "";
		try {
			List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
			if  (addresses != null) {
				Address returnedAddress = addresses.get(0);
				for(int i=0; i<returnedAddress.getMaxAddressLineIndex(); i++) {
					address = address+returnedAddress.getAddressLine(i)+"\n";
				}
			}
			else {
				address = "Address couldn't be determined.\n";
				this.locationText.append("\nNo addresses returned for this location.");
			}

		} catch (IOException e) {
			Log.e("LocateMe", "Could not get Geocoder data", e);
			address = "Could not get Geocoder data.\n";
		}

		this.locationText.setText(address+text);

		if (found_initial_location == false) {
			found_initial_location = true;
			int latitude = (int)(location.getLatitude() * 1000000);
			int longitude = (int)(location.getLongitude() * 1000000);

			GeoPoint point = new GeoPoint(latitude,longitude);
			mapController.animateTo(point);
			// Force map redraw to show any new symbols while we're at it
			map.postInvalidate();
		}

	}

	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}	

	public void onFriends(View v) {
		show_friends = !show_friends;
		myOverlays.clearItems();
		if (show_friends) {
			int friends_displayed = 0;
			map_menu_friend.setImageResource(R.drawable.ic_friends_selected);
			Cursor cursorF = storageDB.cursorSelectAllFriends();
			if (cursorF != null) {
				if (cursorF.getCount() > 0) {
					GeoPoint myPoint;
					boolean more_entries = cursorF.moveToFirst();
					while (more_entries) {
						Cursor cursorL = storageDB.cursorSelectMemberIDLocations(cursorF.getString(cursorF.getColumnIndex(StorageDB.FRIENDS_COL_MEMBERID)));
						if (cursorL != null) {
							if (cursorL.getCount() > 0) {
								friends_displayed++;
								cursorL.moveToFirst();
								String geo_addr = cursorL.getString(cursorL.getColumnIndex(StorageDB.LOCATIONS_COL_GEOCODE));
								String lat = cursorL.getString(cursorL.getColumnIndex(StorageDB.LOCATIONS_COL_LAT));
								String lon = cursorL.getString(cursorL.getColumnIndex(StorageDB.LOCATIONS_COL_LON));
								String address = "";
								if (!geo_addr.equals("")) {
									address = address + geo_addr + "\n";
								}
								if (lat.length() > 11) { lat = lat.substring(0,11); }
								if (lon.length() > 11) { lon = lon.substring(0,11); }
								address = address+lat+","+lon;
								long epoch_time = cursorL.getLong(cursorL.getColumnIndex(StorageDB.LOCATIONS_COL_DATETIME));
								String nicedate = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new java.util.Date (epoch_time*1000));

								myPoint = new GeoPoint((int) (Double.valueOf(lat) * 1E6), (int) (Double.valueOf(lon) * 1E6));
								myOverlays.addItem(myPoint, cursorF.getString(cursorF.getColumnIndex(StorageDB.FRIENDS_COL_NICKNAME)), address+"\n"+nicedate);
							}
						}
						
						more_entries = cursorF.moveToNext();
					}
				}
			}
			if (friends_displayed == 0) {
				Toast.makeText(getApplicationContext(), "There are no friends to display.", Toast.LENGTH_LONG).show();
				show_friends = false;
				map_menu_friend.setImageResource(R.drawable.ic_friends);
			}
			else {
				map.getOverlays().add(myOverlays);
			}
				
		}
		else {
			map_menu_friend.setImageResource(R.drawable.ic_friends);
			map.getOverlays().remove(myOverlays);
		}

		map.postInvalidate();
	}
	
	public void onSatellite(View v) { 
		// Turn on or off Satellite view.
		satellite_view = !satellite_view;
		if (satellite_view) {
			map_menu_mapmode.setImageResource(R.drawable.ic_mapmode_selected);
		}
		else {
			map_menu_mapmode.setImageResource(R.drawable.ic_mapmode);			
		}
		
		map.setSatellite(satellite_view);
	}

	public void onMyLocation(View v) {
		// Jump to Current Location
		Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (location != null) {
			int latitude = (int)(location.getLatitude() * 1000000);
			int longitude = (int)(location.getLongitude() * 1000000);        	
			GeoPoint point = new GeoPoint(latitude,longitude);
			mapController.animateTo(point);
			myLocationOverlay = new MyLocationOverlay(this, map);
			map.getOverlays().add(myLocationOverlay);
			// Force map redraw to show any new symbols while we're at it
			map.postInvalidate();
		}
	}

	public void return2Home(Context context) {
		final Intent intent = new Intent(context, MainActivity.class);
		intent.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity (intent);
	}

	public void onHome(View v) {
		return2Home(this);
	}
}
