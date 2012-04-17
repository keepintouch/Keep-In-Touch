package app.master.kit;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class HistoryActivity extends KeepInTouchActivity implements OnClickListener {
	private Spinner spn1;
	private String FriendsList[];
	private String FriendsIDList[];
	private String latlon_array[];
	private String addr_array[];
	private String date_array[];
	private ListView history;
	private ProgressDialog dialog;
	private boolean cancelHistory = false;
	GetGeocode geo;

	public class HistoryArrayAdapter extends ArrayAdapter<String> {
		HistoryArrayAdapter(Context context) {
			super(context, R.layout.history_listview, R.id.history_listview_address, addr_array);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row=super.getView(position, convertView, parent);
			TextView datetime = (TextView) row.findViewById(R.id.history_listview_datetime);
			datetime.setText(date_array[position]);
			ImageView map=(ImageView) row.findViewById(R.id.history_listview_map);
			map.setTag(latlon_array[position]);
			map.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					String latlon = (String) v.getTag();
					Intent intent = new Intent(android.content.Intent.ACTION_VIEW,                	
							Uri.parse("geo:"+latlon+"?q="+latlon));
					startActivity(intent);
				}
			});

			return(row);
		} 
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.history);
		try {
			storageDB.close();
		} catch (NullPointerException e)
		{ }
		storageDB = new StorageDB(this);
		FriendsList = new String[0];		
		FriendsIDList = new String[0];
		spn1 = (Spinner)findViewById(R.id.spnHistoryName);
		Cursor cursor = storageDB.cursorSelectAllFriends();
		if (cursor != null) {
			if (cursor.getCount() > 0) {
				FriendsList = new String[cursor.getCount()];
				FriendsIDList = new String[cursor.getCount()];
				boolean more_entries = cursor.moveToFirst();
				int counter = 0;
				while (more_entries) {
					FriendsList[counter] = cursor.getString(cursor.getColumnIndex(StorageDB.FRIENDS_COL_NICKNAME));
					FriendsIDList[counter] = cursor.getString(cursor.getColumnIndex(StorageDB.FRIENDS_COL_ID));
					counter++;
					more_entries = cursor.moveToNext();
				}
			}
		}		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, FriendsList);   
		spn1.setAdapter(adapter);
		history = (ListView)findViewById(R.id.listViewHistory);
		geo = new GetGeocode(getApplicationContext());
	}
	
	protected void onDestroy() {
		storageDB.close();
		super.onDestroy();
	}
	
	public void onShowHistory(View v) {
		// TODO Auto-generated method stub
		cancelHistory = false;
		dialog = new ProgressDialog(this);
		dialog.setMessage("Retrieving History... Please Wait...");
		dialog.setCancelable(false);
		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				cancelHistory = true;
				//dialog.dismiss();
			}
		});
		dialog.show();
		Thread t = new Thread() {
			public void run() {
				Message msg = new Message();
				DatePicker datetime = (DatePicker)findViewById(R.id.HistorydatePicker);
				Date epochDate = new Date(datetime.getYear() - 1900, datetime.getMonth(), datetime.getDayOfMonth());
				String starttime = String.valueOf(epochDate.getTime()/1000);
				String endtime = String.valueOf((epochDate.getTime()/1000)+86400);		
				StorageDB storageDB = new StorageDB(HistoryActivity.this);				
				Cursor cursorF = storageDB.cursorSelectFriend(FriendsIDList[spn1.getSelectedItemPosition()]);
				if (cursorF != null) {
					if (cursorF.getCount() > 0) {
						cursorF.moveToFirst();
						Cursor cursorS = storageDB.cursorSelectServerByURL(cursorF.getString(cursorF.getColumnIndex(StorageDB.FRIENDS_COL_SERVERURL)));
						if (cursorS != null) {
							if (cursorS.getCount() > 0) {
								cursorS.moveToFirst();
								EncryptionProvider enc = new EncryptionProvider();
								String[] encryption_types = enc.getProviders();
								StringBuilder jsonData = new StringBuilder();
								jsonData.append("[");
								JSONObject json = new JSONObject();
								try {			
									json.put("cmd", "query_history");
									json.put("id", cursorS.getString(cursorS.getColumnIndex(StorageDB.SERVERS_COL_SECUREID))); 
									json.put("memberid", cursorF.getString(cursorF.getColumnIndex(StorageDB.FRIENDS_COL_MEMBERID)));
									json.put("starttime", starttime);
									json.put("endtime", endtime);
									jsonData.append(json.toString());
								} catch (Throwable terror) {
									Log.i(LOG_TAG, "JSON Error: Couldn't create JSON data for server poll!");
								}
								jsonData.append("]");

								RemoteServer remoteServer = new RemoteServer(getApplicationContext());
								JSONArray jsonArray = remoteServer.sendreceive(cursorS, jsonData.toString());
								remoteServer.close();
								latlon_array=new String[jsonArray.length()-1];
								addr_array=new String[jsonArray.length()-1];
								date_array=new String[jsonArray.length()-1];
								int array_index = 0;
								for (int i = 0; i < jsonArray.length(); i++) {
									if (cancelHistory) { break; }
									try {
										JSONObject jsonObject = jsonArray.getJSONObject(i);
										if (jsonObject.has("cmd")) {							
											if (jsonObject.getString("cmd").equals("query_history_response")) {
												// fields: lat,lon,accuracy,altitude,datetime
												String latlon = jsonObject.getString("latlon");
												// If necessary decrypt our latlon data
												String server_encryption = jsonObject.getString("encryption");
												boolean found_encryption = false;
												for (String a : encryption_types) {
													if (a.equals(server_encryption)) {
														found_encryption = true;
														break;
													}
												}
												// If we found the encryption in our Encryption Providers, then decrypt latlon data
												boolean everything_okay = true;
												if (!found_encryption) {
													server_encryption = "none";
												} else {
													if (!server_encryption.equals("none")) {  // If it's not none then decrypt
														try {
															enc.setPassword(cursorS.getString(cursorS.getColumnIndex(StorageDB.SERVERS_COL_ENCRYPTPASS)));
															String decrypted_latlon = enc.decrypt(server_encryption, latlon);
															latlon = decrypted_latlon;
														}
														catch (Exception e) {
															everything_okay = false;
														}
													}
												}
												if (everything_okay) {
													try {
														String latlon_fields[] = latlon.split(",");
														latlon_array[array_index] = latlon_fields[0]+","+latlon_fields[1];
														String[] retval = geo.getAddress(latlon_fields[0], latlon_fields[1]);
														String display_addr = "";
														if (retval[0] == "1")
														{ display_addr = retval[1]+"\n"; }
														if (latlon_fields[0].length() > 11) { latlon_fields[0] = latlon_fields[0].substring(0,11); }
														if (latlon_fields[1].length() > 11) { latlon_fields[1] = latlon_fields[1].substring(0,11); }
														display_addr = display_addr+latlon_fields[0]+","+latlon_fields[1];
														addr_array[array_index] = display_addr;
														long epoch_time = Long.valueOf(jsonObject.getString("datetime"));
														String nicedate = new java.text.SimpleDateFormat("MM/dd/yyyy\nHH:mm:ss").format(new java.util.Date (epoch_time*1000));
														date_array[array_index] = nicedate;
													}
													catch (Exception e) {
														latlon_array[array_index] = "0,0";
														addr_array[array_index] = "Location Data decryption failed.";
														date_array[array_index] = "";														
													}
												}
												else {
													latlon_array[array_index] = "0,0";
													addr_array[array_index] = "Location Data couldn't be decrypted.";
													date_array[array_index] = "";
												}
												array_index++;
											}
										}
									}
									catch (Exception e) {
										Log.i(LOG_TAG, "JSON parsing error in History response.");
										//e.printStackTrace();
									}
								}
								msg.what = 1;
								myThreadMessageHandler.sendMessage(msg);
							}
							else {
								cancelHistory = true;
								msg.what = 4;
								myThreadMessageHandler.sendMessage(msg);        				
							}
						}
						else {
							cancelHistory = true;
							msg.what = 5;
							myThreadMessageHandler.sendMessage(msg);        				
						}
					}
					else {
						cancelHistory = true;
						msg.what = 2;
						myThreadMessageHandler.sendMessage(msg);        				
					}
				}
				else {
					cancelHistory = true;
					msg.what = 3;
					myThreadMessageHandler.sendMessage(msg);        				        			
				}
				storageDB.close();
			}
		};
		t.start();

	}

	Handler myThreadMessageHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == 2) {
				Toast.makeText(getApplicationContext(), "No Friends Found!", Toast.LENGTH_LONG).show();
			}
			else if (msg.what == 3) {
				Toast.makeText(getApplicationContext(), "ERROR: Couldn't retrieve friend information.", Toast.LENGTH_LONG).show();
			}
			else if (msg.what == 4) {
				Toast.makeText(getApplicationContext(), "ERROR: Couldn't find server for this friend.", Toast.LENGTH_LONG).show();
			}
			else if (msg.what == 5) {
				Toast.makeText(getApplicationContext(), "ERROR: Couldn't retrieve server information.", Toast.LENGTH_LONG).show();
			}
			if (!cancelHistory) {
				HistoryArrayAdapter adapter2 = new HistoryArrayAdapter(HistoryActivity.this);
				history.setAdapter(adapter2);
			}
			dialog.dismiss();
		}
	};

	public void onClick(View v) {
		// TODO Auto-generated method stub

	}

}
