package app.master.kit;

import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import app.master.kit.MyLocation.LocationResult;
//import android.widget.Toast;

public class PollService extends Service {
	private static final String LOG_TAG = "KIT_LOG";
	private PowerManager pm;
	private PowerManager.WakeLock wl;
	private String lat;
	private String lon;
	private String accuracy;
	private String altitude;
	private String latlontime;
	private StorageDB storageDB;
	private String IDNUM = "";
	private String CMD = "";  // Command passed to this service
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "YOUR TAG HERE");
		wl.acquire();
		//Toast.makeText(this, "Service Created", Toast.LENGTH_LONG).show();
		storageDB = new StorageDB(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		//Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
		storageDB.close();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Commands: UPDATE_AND_POLL, UPDATE, POLL
		CMD = intent.getStringExtra("CMD");
		if (CMD == null) { CMD = "UPDATE_AND_POLL"; }; // Default Command
		//Toast.makeText(this, "Service Started - "+CMD, Toast.LENGTH_LONG).show();
		PollServer();
		return super.onStartCommand(intent, flags, startId);
	}

	private void CheckForPendingNotifications() {
		Cursor cursor = storageDB.cursorSelectAllPendingFriendRequests();
		if (cursor != null) {
			if (cursor.getCount() > 0) {
				boolean more_entries = cursor.moveToFirst();
				int NOTIFY_ID = 1;
				while (more_entries) {
					String ns = Context.NOTIFICATION_SERVICE;
					NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
					int icon = R.drawable.ic_kit;
					String RequesterName = cursor.getString(cursor.getColumnIndex(StorageDB.FRIENDREQUESTS_COL_REQUESTERNAME));
					CharSequence tickerText = "Friend Request from "+RequesterName;;
					long when = System.currentTimeMillis();
					Notification notification = new Notification(icon, tickerText, when);
					Context context = getApplicationContext();
					CharSequence contentTitle = this.getString(R.string.app_name);
					CharSequence contentText = tickerText;
					Intent notificationIntent = new Intent(this, FriendRequestReceivedActivity.class);
					PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
					notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
					mNotificationManager.notify(NOTIFY_ID, notification);
					NOTIFY_ID++;
					// Now we setup "notified" to "Y" so that we don't redisplay this notification 
					ContentValues values = new ContentValues();
					values.put(StorageDB.FRIENDREQUESTS_COL_NOTIFIED, "Y");
					storageDB.updateFriendRequest(cursor.getString(cursor.getColumnIndex(StorageDB.FRIENDREQUESTS_COL_ID)), values);					
					more_entries = cursor.moveToNext();
				}
			}
		}
	}
	
	private void PollServer() {
		//Toast.makeText(PollService.this, "Polling Location...", Toast.LENGTH_LONG).show();
		//Log.i(LOG_TAG, "Polling Location...");
		if (CMD.equals("POLL")) {
			Cursor cursor = storageDB.cursorSelectAllServers();
			if (cursor != null) {
				if (cursor.getCount() > 0) {
					RemoteServer remoteServer = new RemoteServer(getApplicationContext());
					boolean more_entries = cursor.moveToFirst();
					while (more_entries) {
						IDNUM = cursor.getString(cursor.getColumnIndex(StorageDB.SERVERS_COL_SECUREID));
						String server_url = cursor.getString(cursor.getColumnIndex(StorageDB.SERVERS_COL_URL));
						StringBuilder jsonData = new StringBuilder();
						jsonData.append("[");

						// Loop through our poll commands
						Cursor cursor_p = storageDB.cursorSelectServerURLPollCommands(server_url);
						if (cursor_p != null) {
							if (cursor_p.getCount() > 0) {
								boolean more_poll_entries = cursor_p.moveToFirst();
								while (more_poll_entries) {
									if (jsonData.length() > 2) {
										jsonData.append(",");
									}
									jsonData.append(cursor_p.getString(cursor_p.getColumnIndex(StorageDB.POLLCOMMANDS_COL_COMMAND)));
									more_poll_entries = cursor_p.moveToNext();
								}
							}
						}
						
						// Send a query_location command to get locations of all friends
						JSONObject json = new JSONObject();
						try {			
							json.put("cmd", "query_location");
							json.put("id", IDNUM);
							if (jsonData.length() > 2) {
								jsonData.append(",");
							}
							jsonData.append(json.toString());
						} catch (Throwable t) {
							Log.i(LOG_TAG, "JSON Error: Couldn't create JSON data for server poll!");
						}

						// Send a "poll" command to see if the server has anything to send us
						json = new JSONObject();
						try {			
							json.put("cmd", "poll");
							json.put("id", IDNUM);
							if (jsonData.length() > 2) {
								jsonData.append(",");
							}
							jsonData.append(json.toString());
						} catch (Throwable t) {
							Log.i(LOG_TAG, "JSON Error: Couldn't create JSON data for server poll!");
						}

						jsonData.append("]");
						
						remoteServer.send(cursor, jsonData.toString());
						more_entries = cursor.moveToNext(); 						
					}
					remoteServer.close();
				}
			}
			
			// Remove all of our PollCommands
			storageDB.clearAllPollCommands();

			// Any Pending Notifications?
			CheckForPendingNotifications();
			
			try {
				wl.release();				
			} catch (RuntimeException e) {
				Log.i(LOG_TAG, "Wake Lock Release Error!");
			}

			// Send a broadcast that we're done with our update, incase an activity was waiting for it!
			Intent i = new Intent("app.master.custom.intent.action.POLL_COMPLETE");
			this.sendBroadcast(i);

			stopSelf();
		}
		else if (CMD.equals("UPDATE_AND_POLL") || CMD.equals("UPDATE")) {
			MyLocation cLocation = new MyLocation();
			cLocation.init(PollService.this, locationResult);
		}
	}
	
	//public LocationResult LocationUpdateReceived = new LocationResult(){
	public LocationResult locationResult = new LocationResult() {
		private StorageDB storageDB;

		@Override
		public void gotLocation(String locationProvider, final Location location, boolean gps_off) {
			storageDB = new StorageDB(PollService.this);
			//Got the location!
			if (gps_off) {
				Log.i(LOG_TAG, "GPS IS TURNED OFF");
			}
			double latitude = 0;
			double longitude = 0;
			long updatetime = 0;
			long epoch = 0;
			String current_time = "";
			
			if (location != null) {
				latitude = location.getLatitude();
				longitude = location.getLongitude();
				updatetime = location.getTime()/1000;
				epoch = System.currentTimeMillis()/1000;
				lat = String.valueOf(latitude);
				lon = String.valueOf(longitude);
				accuracy = String.valueOf(location.getAccuracy());
				altitude = String.valueOf(location.getAltitude());
				latlontime = String.valueOf(updatetime);
				current_time = String.valueOf(epoch);
			}
			
			// Get our Encryption Providers incase we need them
			EncryptionProvider enc = new EncryptionProvider();
			String[] encryption_types = enc.getProviders();
			
			// Loop through our servers
			Cursor cursor = storageDB.cursorSelectAllServers();
			if (cursor != null) {
				if (cursor.getCount() > 0) {
					RemoteServer remoteServer = new RemoteServer(getApplicationContext());
					boolean more_entries = cursor.moveToFirst();
					while (more_entries) {
						IDNUM = cursor.getString(cursor.getColumnIndex(StorageDB.SERVERS_COL_SECUREID));
						String server_url = cursor.getString(cursor.getColumnIndex(StorageDB.SERVERS_COL_URL));

						StringBuilder jsonData = new StringBuilder();
						jsonData.append("[");

						// Loop through our poll commands
						Cursor cursor_p = storageDB.cursorSelectServerURLPollCommands(server_url);
						if (cursor_p != null) {
							if (cursor_p.getCount() > 0) {
								boolean more_poll_entries = cursor_p.moveToFirst();
								while (more_poll_entries) {
									if (jsonData.length() > 2) {
										jsonData.append(",");
									}
									jsonData.append(cursor_p.getString(cursor_p.getColumnIndex(StorageDB.POLLCOMMANDS_COL_COMMAND)));
									more_poll_entries = cursor_p.moveToNext();
								}
							}
						}
						
						if (location != null) {
							try {
								JSONObject json = new JSONObject();
								json.put("cmd", "update_location");
								json.put("id", IDNUM);
								json.put("datetime", current_time);
								String latlon = lat+","+lon+","+accuracy+","+altitude+","+latlontime;
								String server_encryption = cursor.getString(cursor.getColumnIndex(StorageDB.SERVERS_COL_ENCRYPT));
								boolean found_encryption = false;
								for (String a : encryption_types) {
									if (a.equals(server_encryption)) {
										found_encryption = true;
										break;
									}
								}
								// If we found the encryption in our Encryption Providers, then encrypt latlon data
								if (!found_encryption) {
									server_encryption = "none";
								} else {
									if (!server_encryption.equals("none")) {  // If it's not none then encrypt
										enc.setPassword(cursor.getString(cursor.getColumnIndex(StorageDB.SERVERS_COL_ENCRYPTPASS)));
										String encrypted_latlon = enc.encrypt(server_encryption, latlon);
										latlon = encrypted_latlon;
									}
								}
								json.put("encryption", server_encryption);
								json.put("latlon", latlon);
								json.put("locationtype", "gps");
								if (jsonData.length() > 2) {
									jsonData.append(",");
								}								
								jsonData.append(json.toString());
							} catch (Throwable t) {
								Log.i(LOG_TAG, "JSON Error: Couldn't create JSON data for server poll!");
							}
						}
						//else
						//{  Toast.makeText(PollService.this, "Location Update Returned NULL", Toast.LENGTH_LONG).show(); }
						
						if (CMD.equals("UPDATE_AND_POLL")) {
							if (jsonData.length() > 2) {
								jsonData.append(",");
							}
							JSONObject json = new JSONObject();
							try {			
								json.put("cmd", "query_location");
								json.put("id", IDNUM);
								jsonData.append(json.toString());
							} catch (Throwable t) {
								Log.i(LOG_TAG, "JSON Error: Couldn't create JSON data for server poll!");
							}
						}
						
						// Send a "poll" command to see if the server has anything to send us
						JSONObject json = new JSONObject();
						try {			
							json.put("cmd", "poll");
							json.put("id", IDNUM);
							if (jsonData.length() > 2) {
								jsonData.append(",");
							}
							jsonData.append(json.toString());
						} catch (Throwable t) {
							Log.i(LOG_TAG, "JSON Error: Couldn't create JSON data for server poll!");
						}
						
						// End our JSON array
						jsonData.append("]");

						remoteServer.send(cursor, jsonData.toString());
						more_entries = cursor.moveToNext(); 						
					}
					remoteServer.close();
				}
			}
			
			// Remove all of our PollCommands
			storageDB.clearAllPollCommands();
			
			// Any Pending Notifications?
			CheckForPendingNotifications();
			
			try {
				wl.release();				
			} catch (RuntimeException e) {
				Log.i(LOG_TAG, "Wake Lock Release Error!");
			}
			storageDB.close();
			stopSelf();
		}
	};
}
