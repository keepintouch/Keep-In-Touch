package app.master.kit;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FavoritesActivity extends KeepInTouchActivity implements OnClickListener {
	/** Called when the activity is first created. */
	private ListView favorites;
	private String lat = "";
	private String lon = "";
	private String geo_address = "";
	private String deletePOI = "";
	GetGeocode geo;

	public class CustomCursorAdapter extends CursorAdapter {

		private LayoutInflater mInflater;
		private int mIDIndex;
		private int mNameIndex;
		private int mDateTimeIndex;
		private int mLatIndex;
		private int mLonIndex;
		private int mGeocodeIndex;

		public CustomCursorAdapter(Context context, Cursor c) {
			super(context, c);
			mIDIndex = c.getColumnIndex(StorageDB.POI_COL_ID);
			mNameIndex = c.getColumnIndex(StorageDB.POI_COL_NAME);
			mDateTimeIndex = c.getColumnIndex(StorageDB.POI_COL_DATETIME);
			mLatIndex = c.getColumnIndex(StorageDB.POI_COL_LAT);
			mLonIndex = c.getColumnIndex(StorageDB.POI_COL_LON);
			mGeocodeIndex = c.getColumnIndex(StorageDB.POI_COL_GEOCODE);
			mInflater = LayoutInflater.from(context);
		}

		private String getGeocoderData(String poiID, String lat, String lon) {
			String[] retval = geo.getAddress(lat, lon);
			if (retval[0].equals("1")) {
				ContentValues values = new ContentValues();
				values.put(StorageDB.POI_COL_GEOCODE, retval[1]);
				storageDB.updatePOI(poiID, values);
			}
			return retval[1];
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TextView name=(TextView) view.findViewById(R.id.favorites_listview_name);
			name.setText(cursor.getString(mNameIndex));
			TextView addr=(TextView) view.findViewById(R.id.favorites_listview_address);
			String geo_addr = cursor.getString(mGeocodeIndex);
			if (geo_addr.equals("")) {
				// If no geocode data stored, then do a lookup
				String id = cursor.getString(mIDIndex);
				geo_addr = getGeocoderData(id, cursor.getString(mLatIndex), cursor.getString(mLonIndex));
			}
			String lat = cursor.getString(mLatIndex);
			String lon = cursor.getString(mLonIndex);
			if (lat.length() > 11) { lat = lat.substring(0,11); }
			if (lon.length() > 11) { lon = lon.substring(0,11); }
			String display_addr = geo_addr+"\n"+lat+","+lon;
			addr.setText(display_addr);
			TextView datetime=(TextView) view.findViewById(R.id.favorites_listview_datetime);
			long epoch_time = cursor.getLong(mDateTimeIndex);
			String nicedate = new java.text.SimpleDateFormat("MM/dd/yyyy\nHH:mm:ss").format(new java.util.Date (epoch_time*1000));
			datetime.setText(nicedate);
			ImageView map=(ImageView) view.findViewById(R.id.favorites_listview_map);
			map.setTag(lat+","+lon);
			map.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					String latlon = (String) v.getTag();
					Intent intent = new Intent(android.content.Intent.ACTION_VIEW,                	
							Uri.parse("geo:"+latlon+"?q="+latlon));
					startActivity(intent);
				}
			});
			view.setTag(cursor.getString(mIDIndex));
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return mInflater.inflate(R.layout.favorites_listview, null);
		}

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.favorites);
		favorites = (ListView)findViewById(R.id.favorites_allfavorites_list);
		favorites.setLongClickable(true);		
	}

	public void onStart() {
		try {
			storageDB.close();
		} catch (NullPointerException e)
		{ }
		storageDB = new StorageDB(this);
		geo = new GetGeocode(getApplicationContext());
		lat = getIntent().getStringExtra("lat");
		lon = getIntent().getStringExtra("lon");
        if (lat != null && lon != null)
        {
			AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
			alertbox.setTitle("New Favorite");
			final LinearLayout lay = new LinearLayout(this);
			lay.setOrientation(LinearLayout.VERTICAL);			
			final EditText locationName = new EditText(this);
			locationName.setSingleLine();
			locationName.setHint("Please enter location's name");
			lay.addView(locationName);
			final TextView locationInfo = new TextView(this);
			String[] retval = geo.getAddress(lat, lon);
			String display_nice_location = lat+","+lon;
			if (retval[0].equals("1")) {
				geo_address = retval[1];
				display_nice_location = retval[1]+"\n"+lat+","+lon;
			}
			locationInfo.setText(display_nice_location);
			lay.addView(locationInfo);			
			alertbox.setView(lay);
			alertbox.setPositiveButton("Save", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface arg0, int arg1) {
					String name = locationName.getText().toString();
					ContentValues values = new ContentValues();
					values.put(StorageDB.POI_COL_NAME, name);
					values.put(StorageDB.POI_COL_DATETIME, String.valueOf(System.currentTimeMillis()/1000));
					values.put(StorageDB.POI_COL_LAT, lat);
					values.put(StorageDB.POI_COL_LON, lon);
					values.put(StorageDB.POI_COL_GEOCODE, geo_address);
					storageDB.insertPOI(values);
					LoadPOIs();
				}
			});
			alertbox.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface arg0, int arg1) {
					//Toast.makeText(getApplicationContext(), "'Cancel' button clicked", Toast.LENGTH_LONG).show();
				}
			});
			alertbox.show();			

        	//Toast.makeText(getApplicationContext(), lat+","+lon, Toast.LENGTH_LONG).show();
        }
        super.onStart();
        LoadPOIs();
	}
	
	private void LoadPOIs() {
		Cursor cursor = storageDB.cursorSelectAllPOI();
		if (cursor != null) {
			cursor.moveToFirst();
			CustomCursorAdapter f_adapter = new CustomCursorAdapter(this, cursor);
			favorites.setAdapter(f_adapter);
			favorites.setOnItemLongClickListener(new OnItemLongClickListener() {
		        public boolean onItemLongClick(AdapterView<?> arg0,	View arg1, int arg2, long arg3) {
					// TODO Auto-generated method stub
		        	deletePOI = (String) arg1.getTag();
					AlertDialog.Builder alertbox = new AlertDialog.Builder(FavoritesActivity.this);
					alertbox.setTitle("Delete Favorite");
					alertbox.setMessage("Delete this location?");
					alertbox.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface arg0, int arg1) {
							storageDB.deletePOI(deletePOI);
							LoadPOIs();
						}
					});
					alertbox.setNegativeButton("No", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface arg0, int arg1) {
							//Toast.makeText(getApplicationContext(), "'No' button clicked", Toast.LENGTH_LONG).show();
						}
					});
					alertbox.show();			
					return false;
				}
			});
		}
		else {
			Toast.makeText(getApplicationContext(), "Database ERROR!", Toast.LENGTH_LONG).show();
		}
	}
	
	public void onDestroy() {
		storageDB.close();
		super.onDestroy();
	}
	
	public void onAddPOI(View v) {
		Toast.makeText(getApplicationContext(), "Long press, to select a location.", Toast.LENGTH_LONG).show();
		Intent myIntent = new Intent(this, LocationMapActivity.class);
		myIntent.setFlags (Intent.FLAG_ACTIVITY_NO_HISTORY);
		startActivity(myIntent);
	}
	
	public void onClick(View v) {
		// TODO Auto-generated method stub
	}
}