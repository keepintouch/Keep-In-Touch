package app.master.kit;

import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemLongClickListener;

public class FriendsActivity extends KeepInTouchActivity implements OnClickListener {
	private ListView friends;
	private boolean receiverRegistered = false;
	private String deleteID;
	GetGeocode geo;
	ProgressDialog dialog;

	public class CustomCursorAdapter extends CursorAdapter {

		private LayoutInflater mInflater;
		private int mIDIndex;
		private int mMemberIDIndex;
		private int mNameIndex;

		public CustomCursorAdapter(Context context, Cursor c) {
			super(context, c);
			mIDIndex = c.getColumnIndex(StorageDB.FRIENDS_COL_ID);
			mMemberIDIndex = c.getColumnIndex(StorageDB.FRIENDS_COL_MEMBERID);
			mNameIndex = c.getColumnIndex(StorageDB.FRIENDS_COL_NICKNAME);
			mInflater = LayoutInflater.from(context);
		}

		private String getGeocoderData(String locationID, String lat, String lon) {
			String[] retval = geo.getAddress(lat, lon);
			if (retval[0].equals("1")) {
				ContentValues values = new ContentValues();
				values.put(StorageDB.LOCATIONS_COL_GEOCODE, retval[1]);
				storageDB.updateLocations(locationID, values);
			}
			return retval[1];
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TextView name=(TextView) view.findViewById(R.id.friends_listview_name);

			name.setText(cursor.getString(mNameIndex));
			// Lets retrieve their location
			String display_addr = "No Location Information";
			String nicedate = "";
			Cursor cursorL = storageDB.cursorSelectMemberIDLocations(cursor.getString(mMemberIDIndex));
			String lat = "";
			String lon = "";
			if (cursorL != null) {
				if (cursorL.getCount() > 0) {
					cursorL.moveToFirst();
					String geo_addr = cursorL.getString(cursorL.getColumnIndex(StorageDB.LOCATIONS_COL_GEOCODE));
					lat = cursorL.getString(cursorL.getColumnIndex(StorageDB.LOCATIONS_COL_LAT));
					lon = cursorL.getString(cursorL.getColumnIndex(StorageDB.LOCATIONS_COL_LON));
					if (geo_addr.equals("")) {
						// If no geocode data stored, then do a lookup
						String id = cursorL.getString(cursorL.getColumnIndex(StorageDB.LOCATIONS_COL_ID));
						geo_addr = getGeocoderData(id, lat, lon);
					}
					if (lat.length() > 11) { lat = lat.substring(0,11); }
					if (lon.length() > 11) { lon = lon.substring(0,11); }
					display_addr = geo_addr+"\n"+lat+","+lon;
					long epoch_time = cursorL.getLong(cursorL.getColumnIndex(StorageDB.LOCATIONS_COL_DATETIME));
					nicedate = new java.text.SimpleDateFormat("MM/dd/yyyy\nHH:mm:ss").format(new java.util.Date (epoch_time*1000));
				}
			}
			TextView addr=(TextView) view.findViewById(R.id.friends_listview_address);
			addr.setText(display_addr);
			TextView datetime=(TextView) view.findViewById(R.id.friends_listview_datetime);
			datetime.setText(nicedate);
			ImageView map=(ImageView) view.findViewById(R.id.friends_listview_map);
			if (lat.length() > 0 && lon.length() > 0) {
				map.setTag(lat+","+lon);
				map.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
					String latlon = (String) v.getTag();
					Intent intent = new Intent(android.content.Intent.ACTION_VIEW,                	
							Uri.parse("geo:"+latlon+"?q="+latlon));
					startActivity(intent);
					}
				});
				map.setVisibility(View.VISIBLE);
				ImageView sep=(ImageView) view.findViewById(R.id.friends_listview_separator);
				sep.setVisibility(View.VISIBLE);
			}
			else {
				map.setVisibility(View.INVISIBLE);
				ImageView sep=(ImageView) view.findViewById(R.id.friends_listview_separator);
				sep.setVisibility(View.INVISIBLE);
			}
			view.setTag(cursor.getString(mIDIndex));
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return mInflater.inflate(R.layout.friends_listview, null);
		}

	}

	public void onCreate(Bundle savedInstanceState) {
		dialog = new ProgressDialog(this);
		dialog.setMessage("Getting Locations of Friends...");
		dialog.setCancelable(false);
		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		dialog.show();
		try {
			storageDB.close();
		} catch (NullPointerException e)
		{ }
		storageDB = new StorageDB(this);
		geo = new GetGeocode(getApplicationContext());
		setContentView(R.layout.friends);
		friends = (ListView)findViewById(R.id.friends_allfriends_list);
		friends.setLongClickable(true);
		friends.setOnItemLongClickListener(new OnItemLongClickListener() {
	        public boolean onItemLongClick(AdapterView<?> arg0,	View arg1, int arg2, long arg3) {
				// TODO Auto-generated method stub
	        	deleteID = (String) arg1.getTag();
				AlertDialog.Builder alertbox = new AlertDialog.Builder(FriendsActivity.this);
				alertbox.setTitle("Delete Friend");
				alertbox.setMessage("Are you sure you want to delete this friend?");
				alertbox.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface arg0, int arg1) {
						// Let's send an update_friend message to the server
						Cursor cursorF = storageDB.cursorSelectFriend(deleteID);
						if (cursorF != null) {
							if (cursorF.getCount() > 0) {
								cursorF.moveToFirst();
								Cursor cursorS = storageDB.cursorSelectServerByURL(cursorF.getString(cursorF.getColumnIndex(StorageDB.FRIENDS_COL_SERVERURL)));
								if (cursorS != null) {
									if (cursorS.getCount() > 0) {
										cursorS.moveToFirst();
										String FriendName = cursorF.getString(cursorF.getColumnIndex(StorageDB.FRIENDS_COL_NICKNAME));
										StringBuilder jsonData = new StringBuilder();
										jsonData.append("[");
										JSONObject json = new JSONObject();
										try {			
											json.put("cmd", "friend_delete");
											json.put("id", cursorS.getString(cursorS.getColumnIndex(StorageDB.SERVERS_COL_SECUREID))); 
											json.put("userTo", cursorF.getString(cursorF.getColumnIndex(StorageDB.FRIENDS_COL_MEMBERID))); 
											jsonData.append(json.toString());
										} catch (Throwable terror) {
											Log.i(LOG_TAG, "JSON Error: Couldn't create JSON data for server poll!");
										}
										jsonData.append("]");
										RemoteServer remoteServer = new RemoteServer(getApplicationContext());
										remoteServer.send(cursorS, jsonData.toString());
										remoteServer.close();
										Toast.makeText(getApplicationContext(), "Successfully Unfriended "+FriendName, Toast.LENGTH_LONG).show();
									}
								}
							}
						}
							
						// Technically should delete from Locations Database too, but we'll just wait for a refresh to handle that
						storageDB.deleteFriend(deleteID);						
						LoadFriends();
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

		super.onCreate(savedInstanceState);
	}

	public void onStart() {
		LoadFriends();
		if (dialog.isShowing()) {
			dialog.dismiss();					
		}
		super.onStart();
	}

	private void LoadFriends() {
		Cursor cursor = storageDB.cursorSelectAllFriends();
		if (cursor != null) {
			cursor.moveToFirst();
			CustomCursorAdapter f_adapter = new CustomCursorAdapter(this, cursor);
			friends.setAdapter(f_adapter);
		}
		else {
			Toast.makeText(getApplicationContext(), "Database ERROR!", Toast.LENGTH_LONG).show();
		}		
	}
	
	
	protected void onDestroy() {
		storageDB.close();
		if (receiverRegistered) {
			unregisterReceiver(onComplete);
			receiverRegistered = false;
		}
		super.onDestroy();
	}

	BroadcastReceiver onComplete=new BroadcastReceiver() {
		public void onReceive(Context ctxt, Intent intent) {
			Cursor cursor = storageDB.cursorSelectAllFriends();
			if (cursor != null) {
				cursor.moveToFirst();
				CustomCursorAdapter f_adapter = new CustomCursorAdapter(FriendsActivity.this, cursor);
				friends.setAdapter(f_adapter);
			}	    		
			if (receiverRegistered) {
				unregisterReceiver(onComplete);
				receiverRegistered = false;
			}
			dialog.dismiss();
		}
	};

	public void onAddFriend(View v) {
		Intent myIntent = new Intent(this, FriendRequestActivity.class);
		startActivity(myIntent);
	}

	public void onViewFriendRequests(View v) {
		Intent myIntent = new Intent(this, FriendRequestReceivedActivity.class);
		startActivity(myIntent);
	}

	
	public void onRefresh(View v) {
		storageDB.clearAllLocations();
		
		// Start up the Polling Service
		dialog = new ProgressDialog(this);
		dialog.setMessage("Getting Locations of Friends...");
		dialog.setCancelable(false);
		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (receiverRegistered) {
					unregisterReceiver(onComplete);
					receiverRegistered = false;
				}
				dialog.dismiss();
			}
		});
		dialog.show();
		registerReceiver(onComplete, new IntentFilter("app.master.custom.intent.action.POLL_COMPLETE"));
		receiverRegistered = true;
		Thread t = new Thread() {
			public void run() {
				if (!isPollServiceRunning()) {
					StartPollService("POLL");
				}
				else {
					// Poll Service is already running
					if (receiverRegistered) {
						unregisterReceiver(onComplete);
						receiverRegistered = false;
					}
					dialog.dismiss();
				}
			}
		};
		t.start();
	}

	public void onClick(View v) {
		// TODO Auto-generated method stub

	}
}
