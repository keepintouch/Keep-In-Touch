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
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FriendRequestReceivedActivity extends KeepInTouchActivity implements OnClickListener {
	/** Called when the activity is first created. */
	private ListView requests;
	private String requestID;
	private boolean receiverRegistered = false;
	private String decrypted_password = "";
	ProgressDialog dialog;

	public class CustomCursorAdapter extends CursorAdapter {

		private LayoutInflater mInflater;
		private int mIDIndex;
		private int mNameIndex;
		private int mMsgIndex;

		public CustomCursorAdapter(Context context, Cursor c) {
			super(context, c);
			mIDIndex = c.getColumnIndex(StorageDB.FRIENDREQUESTS_COL_ID);
			mNameIndex = c.getColumnIndex(StorageDB.FRIENDREQUESTS_COL_REQUESTERNAME);
			mMsgIndex = c.getColumnIndex(StorageDB.FRIENDREQUESTS_COL_REQUESTERMSG);
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			TextView name=(TextView) view.findViewById(R.id.friendrequestreceived_listview_name);
			name.setText(cursor.getString(mNameIndex));
			TextView msg=(TextView) view.findViewById(R.id.friendrequestreceived_listview_message);
			msg.setText(cursor.getString(mMsgIndex));
			view.setTag(cursor.getString(mIDIndex));
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return mInflater.inflate(R.layout.friendrequestreceived_listview, null);
		}

	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.friendrequestreceived);
		requests = (ListView)findViewById(R.id.friendrequestreceived_requests_list);
		requests.setLongClickable(true);		
	}

	public void onStart() {
		try {
			storageDB.close();
		} catch (NullPointerException e)
		{ }
		storageDB = new StorageDB(this);
		super.onStart();
		LoadFriendRequests();
	}

	private void SendToServer(boolean sendPassword) {
		String receivedEncryption = "";
		
		Cursor cursorFR = storageDB.cursorSelectFriendRequest(requestID);
		if (cursorFR != null) {
			if (cursorFR.getCount() > 0) {
				cursorFR.moveToFirst();
				ContentValues values = new ContentValues();
				values.put(StorageDB.FRIENDS_COL_MEMBERID, cursorFR.getString(cursorFR.getColumnIndex(StorageDB.FRIENDREQUESTS_COL_REQUESTERID)));
				values.put(StorageDB.FRIENDS_COL_SERVERURL, cursorFR.getString(cursorFR.getColumnIndex(StorageDB.FRIENDREQUESTS_COL_SERVERURL)));
				values.put(StorageDB.FRIENDS_COL_RSAPUBMOD, cursorFR.getString(cursorFR.getColumnIndex(StorageDB.FRIENDREQUESTS_COL_RSAPUBMOD)));
				values.put(StorageDB.FRIENDS_COL_RSAPUBEXP, cursorFR.getString(cursorFR.getColumnIndex(StorageDB.FRIENDREQUESTS_COL_RSAPUBEXP)));
				values.put(StorageDB.FRIENDS_COL_NICKNAME, cursorFR.getString(cursorFR.getColumnIndex(StorageDB.FRIENDREQUESTS_COL_REQUESTERNAME)));
				receivedEncryption = cursorFR.getString(cursorFR.getColumnIndex(StorageDB.FRIENDREQUESTS_COL_ENCRYPTION));
				storageDB.insertFriend(values);
			}
		}
		// Now let's send a response back to the server
		boolean friend_response_sent = true;
		// We are not sending our password, then obviously we should save the password we received
		Cursor cursorS = storageDB.cursorSelectServerByURL(cursorFR.getString(cursorFR.getColumnIndex(StorageDB.FRIENDREQUESTS_COL_SERVERURL)));
		if (!sendPassword) {
			if (cursorS != null) {
				if (cursorS.getCount() > 0) {
					cursorS.moveToFirst();
					if (!receivedEncryption.equals("") && !receivedEncryption.equals("none")) {
						ContentValues valuesS = new ContentValues();
						valuesS.put(StorageDB.SERVERS_COL_ENCRYPT, receivedEncryption);
						// Now let's decrypt and update our server password with what our friend sent us
						if (!decrypted_password.equals(""))
						{
							valuesS.put(StorageDB.SERVERS_COL_ENCRYPTPASS, decrypted_password);
							storageDB.updateServers(cursorS.getInt(cursorS.getColumnIndex(StorageDB.SERVERS_COL_ID)), valuesS);
						}
						else {
							Log.i(LOG_TAG,"ERROR: Our decrypted password was empty!  This is really bad!");
						}	
					}
				}
			}
			cursorS = storageDB.cursorSelectServerByURL(cursorFR.getString(cursorFR.getColumnIndex(StorageDB.FRIENDREQUESTS_COL_SERVERURL)));
		}
		if (cursorS != null) {
			if (cursorS.getCount() > 0) {
				cursorS.moveToFirst();

				// Get our RSA Provider
				RSAProvider rsacrypt = new RSAProvider();
				if (rsacrypt.LoadPublicPrivate(cursorFR.getString(cursorFR.getColumnIndex(StorageDB.FRIENDREQUESTS_COL_RSAPUBMOD)), cursorFR.getString(cursorFR.getColumnIndex(StorageDB.FRIENDREQUESTS_COL_RSAPUBEXP)), null, null)) {
					String encrypted_password = "";
					boolean everything_okay = true;
					try {
						encrypted_password = rsacrypt.encrypt(cursorS.getString(cursorS.getColumnIndex(StorageDB.SERVERS_COL_ENCRYPTPASS)));
					}
					catch (ArrayIndexOutOfBoundsException e) {
						everything_okay = false;
						Toast.makeText(getApplicationContext(), "Friend's RSA Key seems invalid!  Response not sent!", Toast.LENGTH_LONG).show();
					}
					if (everything_okay)
					{
						StringBuilder jsonData = new StringBuilder();
						jsonData.append("[");
						JSONObject json = new JSONObject();
						try {			
							json.put("cmd", "friend_response");
							json.put("id", cursorS.getString(cursorS.getColumnIndex(StorageDB.SERVERS_COL_SECUREID))); 
							json.put("userTo", cursorFR.getString(cursorFR.getColumnIndex(StorageDB.FRIENDREQUESTS_COL_REQUESTERID))); 
							json.put("msg", ""); 
							json.put("allowhistory", "Y"); 
							json.put("accepted", "Y");
							if (sendPassword) {
								json.put("encryption", cursorS.getString(cursorS.getColumnIndex(StorageDB.SERVERS_COL_ENCRYPT))); 
								json.put("encryptionpass", encrypted_password);
							}
							else {
								json.put("encryption", ""); 
								json.put("encryptionpass", "");								
							}
							jsonData.append(json.toString());
						} catch (Throwable terror) {
							Log.i(LOG_TAG, "JSON Error: Couldn't create JSON data for server poll!");
						}
						jsonData.append("]");
						RemoteServer remoteServer = new RemoteServer(getApplicationContext());
						if (!remoteServer.send(cursorS, jsonData.toString()))
						{ friend_response_sent = false; }
						remoteServer.close();
						if (friend_response_sent) {
							Toast.makeText(getApplicationContext(), "Friend Request Accepted", Toast.LENGTH_LONG).show();
						}
						else {
							Toast.makeText(getApplicationContext(), "Server Communication Failed.  Please Try Again.", Toast.LENGTH_LONG).show();
						}
					}
				}
				else {
					friend_response_sent = false;
					Toast.makeText(getApplicationContext(), "ERROR: Couldn't initialize RSA Provider, friend response not sent!", Toast.LENGTH_LONG).show();
				}
			}
		}
		if (friend_response_sent) {
			storageDB.deleteFriendRequest(requestID);
			LoadFriendRequests();
		}
	}
	
	private void LoadFriendRequests() {
		Cursor cursor = storageDB.cursorSelectAllFriendRequests();
		if (cursor != null) {
			cursor.moveToFirst();
			if (cursor.getCount() > 0) {
				CustomCursorAdapter f_adapter = new CustomCursorAdapter(this, cursor);
				requests.setAdapter(f_adapter);
				requests.setOnItemClickListener(new OnItemClickListener() {
					public void onItemClick(AdapterView<?> arg0, View arg1,	int arg2, long arg3) {
						// TODO Auto-generated method stub
		        		requestID = (String) arg1.getTag();
						AlertDialog.Builder alertbox = new AlertDialog.Builder(FriendRequestReceivedActivity.this);
						alertbox.setTitle("Friend Request");
						alertbox.setMessage("Do you accept this friend request?");
						alertbox.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface arg0, int arg1) {
								Cursor cursorFR = storageDB.cursorSelectFriendRequest(requestID);
								if (cursorFR != null) {
									if (cursorFR.getCount() > 0) {
										cursorFR.moveToFirst();
										String enc = cursorFR.getString(cursorFR.getColumnIndex(StorageDB.FRIENDREQUESTS_COL_ENCRYPTION));
										String encPass = cursorFR.getString(cursorFR.getColumnIndex(StorageDB.FRIENDREQUESTS_COL_ENCRYPTIONPASS));
										if (!enc.equals("") && !enc.equals("none")) {
											boolean display_dialog = true;
											Cursor cursorS = storageDB.cursorSelectServerByURL(cursorFR.getString(cursorFR.getColumnIndex(StorageDB.FRIENDREQUESTS_COL_SERVERURL)));
											if (cursorS != null) {
												if (cursorS.getCount() > 0) {
													cursorS.moveToFirst();
													String rsa_pub_mod = storageDB.getPrefValue("rsa_pub_mod");
													String rsa_pub_exp = storageDB.getPrefValue("rsa_pub_exp");
													String rsa_priv_mod = storageDB.getPrefValue("rsa_priv_mod");
													String rsa_priv_exp = storageDB.getPrefValue("rsa_priv_exp");
													boolean everything_okay = true;
													if (rsa_pub_mod != null && rsa_pub_exp != null && rsa_priv_mod != null && rsa_priv_exp != null) {
														RSAProvider rsacrypt = new RSAProvider();
														if (rsacrypt.LoadPublicPrivate(rsa_pub_mod, rsa_pub_exp, rsa_priv_mod, rsa_priv_exp)) {
															try {
																decrypted_password = rsacrypt.decrypt(encPass);
															}
															catch (ArrayIndexOutOfBoundsException e) {
																everything_okay = false;
																Log.i(LOG_TAG,"ERROR: Couldn't decrypt server password that was sent to us because our RSA decryption failed!");
															}
															catch (Exception e) {
																everything_okay = false;
																Log.i(LOG_TAG,"ERROR: Couldn't decrypt server password that was sent to us because our RSA decryption failed!");
															}
														}
													}
													else {
														Log.i(LOG_TAG,"ERROR: Couldn't load RSA keys to decrypt server password that was sent to us!");
													}
													
													if (everything_okay)
													{
														if (encPass.equals(decrypted_password)) {
															display_dialog = false;
														}
													}
													else {
														Log.i(LOG_TAG,"ERROR: We had some problems, so we aborted accepting the friend request!");
													}														
												}
											}
											if (display_dialog) {
												AlertDialog.Builder alertbox = new AlertDialog.Builder(FriendRequestReceivedActivity.this);
												alertbox.setTitle("Encryption Password");
												alertbox.setMessage("Your friend sent you an encryption password, do you want to use it?");
												alertbox.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
													public void onClick(DialogInterface arg0, int arg1) {
														SendToServer(false); // Don't send back our password
													}
												});
												alertbox.setNegativeButton("No", new DialogInterface.OnClickListener() {
													public void onClick(DialogInterface arg0, int arg1) {
														SendToServer(true);  // Send back our password
													}
												});
												alertbox.show();
											}
											else {
												SendToServer(false); // Don't send back our password (because they were already the same)
											}												
										}
										else {
											SendToServer(true);										
										}
									}
								}
							}
						});
						alertbox.setNegativeButton("No", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface arg0, int arg1) {
								storageDB.deleteFriendRequest(requestID);
								LoadFriendRequests();
								//Toast.makeText(getApplicationContext(), "'No' button clicked", Toast.LENGTH_LONG).show();
							}
						});
						alertbox.show();			
					}
				});
			}
			else {
				String EmptyList[] = new String[1];
				EmptyList[0] = "No pending friend requests...";
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.friendrequestreceived_listview, R.id.friendrequestreceived_listview_name, EmptyList);   
				requests.setAdapter(adapter);
			}
		}
		else {
			Toast.makeText(getApplicationContext(), "Database ERROR!", Toast.LENGTH_LONG).show();
		}
	}
	
	public void onDestroy() {
		storageDB.close();
		super.onDestroy();
	}
	BroadcastReceiver onComplete=new BroadcastReceiver() {
		public void onReceive(Context ctxt, Intent intent) {
			LoadFriendRequests();
			if (receiverRegistered) {
				unregisterReceiver(onComplete);
				receiverRegistered = false;
			}
			dialog.dismiss();
		}
	};
	
	public void onRefresh(View v) {
		// Start up the Polling Service
		dialog = new ProgressDialog(this);
		dialog.setMessage("Looking for new friend requests...");
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
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}


	public void onClick(View v) {
		// TODO Auto-generated method stub
	}
}