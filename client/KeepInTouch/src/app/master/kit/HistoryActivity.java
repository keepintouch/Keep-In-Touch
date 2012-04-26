package app.master.kit;

import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.DatePickerDialog;
import android.app.Dialog;
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
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class HistoryActivity extends KeepInTouchActivity implements OnClickListener {
	private Spinner spn1;
	private String FriendsList[];
	private String FriendsServerList[];
	private String FriendsIDList[];
	private String latlon_array[];
	private String addr_array[];
	private String date_array[];
	private ListView history;
	private ProgressDialog dialog;
	private Button setDate;
    private int mYear;
    private int mMonth;
    private int mDay;
	private boolean cancelHistory = false;
	GetGeocode geo;

    private DatePickerDialog.OnDateSetListener mDateSetListener =
            new DatePickerDialog.OnDateSetListener() {

                public void onDateSet(DatePicker view, int year, 
                                      int monthOfYear, int dayOfMonth) {
                    mYear = year;
                    mMonth = monthOfYear;
                    mDay = dayOfMonth;
                    updateDisplay();
                    ShowHistory();
                    
                }
            };
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
		FriendsServerList = new String[0];
		FriendsIDList = new String[0];
		spn1 = (Spinner)findViewById(R.id.spnHistoryName);
		int server_count = 0;
		String device_owner = storageDB.getPrefValue("display_name");
		Cursor cursorS = storageDB.cursorSelectAllServers();
		if (cursorS != null) {
			server_count = cursorS.getCount();
		}
		
		Cursor cursor = storageDB.cursorSelectAllFriends();
		if (cursor != null) {
			int counter = 0;
			if (cursor.getCount() > 0) {
				FriendsList = new String[cursor.getCount()+server_count];
				FriendsServerList = new String[cursor.getCount()+server_count];
				FriendsIDList = new String[cursor.getCount()+server_count];
				boolean more_entries = cursor.moveToFirst();
				while (more_entries) {
					FriendsList[counter] = cursor.getString(cursor.getColumnIndex(StorageDB.FRIENDS_COL_NICKNAME));
					FriendsServerList[counter] = cursor.getString(cursor.getColumnIndex(StorageDB.FRIENDS_COL_SERVERURL));
					FriendsIDList[counter] = cursor.getString(cursor.getColumnIndex(StorageDB.FRIENDS_COL_MEMBERID));
					counter++;
					more_entries = cursor.moveToNext();
				}
			}
			if (server_count > 0) {
				boolean more_entries = cursorS.moveToFirst();
				while (more_entries) {
					String display_name = "Yourself";
					if (server_count == 1) {
						display_name = device_owner;
					}
					else {
						display_name = device_owner + " - "+cursorS.getString(cursorS.getColumnIndex(StorageDB.SERVERS_COL_NAME));						
					}
					FriendsList[counter] = display_name;
					FriendsServerList[counter] = cursorS.getString(cursorS.getColumnIndex(StorageDB.SERVERS_COL_URL));
					FriendsIDList[counter] = cursorS.getString(cursorS.getColumnIndex(StorageDB.SERVERS_COL_MEMBERID));
					counter++;
					more_entries = cursorS.moveToNext();
				}				
			}
			
		}		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, FriendsList);   
		spn1.setAdapter(adapter);
		history = (ListView)findViewById(R.id.listViewHistory);
		geo = new GetGeocode(getApplicationContext());
		setDate = (Button)findViewById(R.id.SetDate);

		// get the current date
        final Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
	}
	
	protected void onDestroy() {
		storageDB.close();
		super.onDestroy();
	}

	// updates the date in the TextView
    private void updateDisplay() {
    	setDate.setText(
            new StringBuilder()
                    // Month is 0 based so add 1
                    .append(mMonth + 1).append("-")
                    .append(mDay).append("-")
                    .append(mYear).append(" "));
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case 0:
            return new DatePickerDialog(this,
                        mDateSetListener,
                        mYear, mMonth, mDay);
        }
        return null;
    }
    
	public void onSetDate(View v) {
		showDialog(0);
	}

	public void ShowHistory() {
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
				Date epochDate = new Date(mYear - 1900, mMonth, mDay);
				String starttime = String.valueOf(epochDate.getTime()/1000);
				String endtime = String.valueOf((epochDate.getTime()/1000)+86400);		
				StorageDB storageDB = new StorageDB(HistoryActivity.this);				
				Cursor cursorS = storageDB.cursorSelectServerByURL(FriendsServerList[spn1.getSelectedItemPosition()]);
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
							json.put("memberid", FriendsIDList[spn1.getSelectedItemPosition()]);
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
						// The jsonArray includes a "response" from the server as well, so I want
						// to subtract 1 from the array.
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
				storageDB.close();
			}
		};
		t.start();

	}

	Handler myThreadMessageHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == 4) {
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
