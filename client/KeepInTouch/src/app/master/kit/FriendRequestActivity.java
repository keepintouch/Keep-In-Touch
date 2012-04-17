package app.master.kit;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class FriendRequestActivity extends KeepInTouchActivity implements OnClickListener {
	/** Called when the activity is first created. */
	private Spinner spn1;
	private ListView members;
	String[] ServersList;
	String[] ServersURLList;
	String[] ServersMemberIDList;
	private ProgressDialog dialog;
	boolean cancelRequest = false;
	String secureID;
	String MemberID;
	int MemberPosition;
	String SendMsg = "";
	String[] MemberNames;
	String[] MemberRSAPUBMOD;
	String[] MemberRSAPUBEXP;
	String[] MemberDates;
    String[] MemberIDs;
	Cursor cursor;
	
	public class MembersArrayAdapter extends ArrayAdapter<String> {
		MembersArrayAdapter(Context context) {
			super(context, R.layout.friendrequest_listview, R.id.friendrequest_listview_name, MemberNames);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row=super.getView(position, convertView, parent);
			TextView datetime = (TextView) row.findViewById(R.id.friendrequest_listview_datetime);
			datetime.setText("Member Since:\n"+MemberDates[position]);
			row.setTag(position);
			return(row);
		} 
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.friendrequest);
		try {
			storageDB.close();
		} catch (NullPointerException e)
		{ }
		storageDB = new StorageDB(this);
		ServersList = new String[0];		
		ServersURLList = new String[0];
		ServersMemberIDList = new String[0];
		members = (ListView)findViewById(R.id.listViewServerMembers);
		spn1 = (Spinner)findViewById(R.id.spnServerList);
		Cursor cursorS = storageDB.cursorSelectAllServers();
		if (cursorS != null) {
			if (cursorS.getCount() > 0) {
				ServersList = new String[cursorS.getCount()];
				ServersMemberIDList = new String[cursorS.getCount()];
				ServersURLList = new String[cursorS.getCount()];
				boolean more_entries = cursorS.moveToFirst();
				int counter = 0;
				while (more_entries) {
					ServersList[counter] = cursorS.getString(cursorS.getColumnIndex(StorageDB.SERVERS_COL_NAME));
					ServersMemberIDList[counter] = cursorS.getString(cursorS.getColumnIndex(StorageDB.SERVERS_COL_MEMBERID));
					ServersURLList[counter] = cursorS.getString(cursorS.getColumnIndex(StorageDB.SERVERS_COL_URL));					
					counter++;
					more_entries = cursorS.moveToNext();
				}
			}
		}		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, ServersList);   
		spn1.setAdapter(adapter);
		
	}
	
	protected void onDestroy() {
		storageDB.close();
		super.onDestroy();
	}

	public void onShowMembers(View v) {
		MemberNames=new String[0];
		MemberRSAPUBMOD=new String[0];
		MemberRSAPUBEXP=new String[0];
		MemberDates=new String[0];
		MemberIDs=new String[0];		
		members.setAdapter(null);
		if (ServersList.length > 0) {
			cursor = storageDB.cursorSelectServerByURL(ServersURLList[spn1.getSelectedItemPosition()]);
			MemberID = ServersMemberIDList[spn1.getSelectedItemPosition()];
			if (cursor != null) {
				if (cursor.getCount() > 0) {
					cursor.moveToFirst();
					secureID = cursor.getString(cursor.getColumnIndex(StorageDB.SERVERS_COL_SECUREID));
					cancelRequest = false;
					dialog = new ProgressDialog(this);
					dialog.setMessage("Retrieving Member List... Please Wait...");
					dialog.setCancelable(false);
					dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							cancelRequest = true;
							dialog.dismiss();
						}
					});
					dialog.show();
					Thread t = new Thread() {
						public void run() {
							Message msg = new Message();
							StringBuilder jsonData = new StringBuilder();
							jsonData.append("[");
							JSONObject json = new JSONObject();
							try {			
								json.put("cmd", "member_list");
								json.put("id", secureID); 
								jsonData.append(json.toString());
							} catch (Throwable terror) {
								Log.i(LOG_TAG, "JSON Error: Couldn't create JSON data for server poll!");
							}
							jsonData.append("]");

							RemoteServer remoteServer = new RemoteServer(getApplicationContext());
							JSONArray jsonArray = remoteServer.sendreceive(cursor, jsonData.toString());
							remoteServer.close();
							MemberNames=new String[jsonArray.length()-2];
							MemberRSAPUBMOD=new String[jsonArray.length()-2];
							MemberRSAPUBEXP=new String[jsonArray.length()-2];
							MemberDates=new String[jsonArray.length()-2];
							MemberIDs=new String[jsonArray.length()-2];
							int array_index = 0;
							for (int i = 0; i < jsonArray.length(); i++) {
								if (cancelRequest) { break; }
								try {
									JSONObject jsonObject = jsonArray.getJSONObject(i);
									if (jsonObject.has("cmd")) {							
										if (jsonObject.getString("cmd").equals("member_list_response")) {
											// We don't want to include ourself in the list
											if (!MemberID.equals(jsonObject.getString("memberid"))) {
												MemberNames[array_index] = jsonObject.getString("name");
												MemberRSAPUBMOD[array_index] = jsonObject.getString("rsa_pub_mod");
												MemberRSAPUBEXP[array_index] = jsonObject.getString("rsa_pub_exp");
												MemberIDs[array_index] = jsonObject.getString("memberid");
												long epoch_time = Long.valueOf(jsonObject.getString("membersince"));
												String nicedate = new java.text.SimpleDateFormat("MM/dd/yyyy\nHH:mm:ss").format(new java.util.Date (epoch_time*1000));
												MemberDates[array_index] = nicedate; 
												array_index++;
											}
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
					};
					t.start();
				}
			}
		}
	}
	
	public void SendFriendRequest(String encryption, String encryptionpass) {
		StringBuilder jsonData = new StringBuilder();
		jsonData.append("[");
		JSONObject json = new JSONObject();
		try {			
			json.put("cmd", "friend_request");
			json.put("id", secureID); 
			json.put("userTo", MemberIDs[MemberPosition]); 
			json.put("msg", SendMsg); 
			json.put("encryption", encryption); 
			json.put("encryptionpass", encryptionpass); 
			jsonData.append(json.toString());
		} catch (Throwable terror) {
			Log.i(LOG_TAG, "JSON Error: Couldn't create JSON data for server poll!");
		}
		jsonData.append("]");
		RemoteServer remoteServer = new RemoteServer(getApplicationContext());
		remoteServer.send(cursor, jsonData.toString());
		remoteServer.close();
		Toast.makeText(getApplicationContext(), "Your Friend Request Has Been Sent", Toast.LENGTH_LONG).show();
	}
	
	Handler myThreadMessageHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (!cancelRequest) {
				if (MemberNames.length > 0) {
					MembersArrayAdapter adapter2 = new MembersArrayAdapter(FriendRequestActivity.this);
					members.setAdapter(adapter2);
					members.setOnItemClickListener(new OnItemClickListener() {
						public void onItemClick(AdapterView<?> arg0, View arg1,	int arg2, long arg3) {
							// TODO Auto-generated method stub
							MemberPosition = (Integer) arg1.getTag();
							AlertDialog.Builder alertbox = new AlertDialog.Builder(FriendRequestActivity.this);
							alertbox.setTitle("Send Friend Request");
							alertbox.setMessage("Please enter a message to send along with your request:");
							final EditText requesterMsg = new EditText(FriendRequestActivity.this);
							requesterMsg.setSingleLine();
							requesterMsg.setHint("Enter message here");
							alertbox.setView(requesterMsg);
							alertbox.setPositiveButton("Send", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface arg0, int arg1) {
									SendMsg = requesterMsg.getText().toString();
									// Decide on if we need to send our password or not to our friend
									if (!cursor.getString(cursor.getColumnIndex(StorageDB.SERVERS_COL_ENCRYPT)).equals("none")) {
										AlertDialog.Builder alertbox = new AlertDialog.Builder(FriendRequestActivity.this);
										alertbox.setTitle("Encryption Password");
										alertbox.setMessage("Would you like to send your encryption password to this friend?");
										alertbox.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface arg0, int arg1) {
												// Get our RSA Provider
												RSAProvider rsacrypt = new RSAProvider();
												String encrypted_password = "";
												if (rsacrypt.LoadPublicPrivate(MemberRSAPUBMOD[MemberPosition], MemberRSAPUBEXP[MemberPosition], null, null)) {
													boolean everything_okay = true;
													try {
														encrypted_password = rsacrypt.encrypt(cursor.getString(cursor.getColumnIndex(StorageDB.SERVERS_COL_ENCRYPTPASS)));
													}
													catch (ArrayIndexOutOfBoundsException e) {
														everything_okay = false;
														Toast.makeText(getApplicationContext(), "Friend's RSA Key seems invalid!  Response not sent!", Toast.LENGTH_LONG).show();
													}
													if (everything_okay) {
														SendFriendRequest(cursor.getString(cursor.getColumnIndex(StorageDB.SERVERS_COL_ENCRYPT)), encrypted_password);
													}
												}
											}
										});
										alertbox.setNegativeButton("No", new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface arg0, int arg1) {
												SendFriendRequest("", "");
											}
										});
										alertbox.show();
									}
									else {
										SendFriendRequest("", "");
									}									
								}
							});
							alertbox.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface arg0, int arg1) {
									//Toast.makeText(getApplicationContext(), "'No' button clicked", Toast.LENGTH_LONG).show();
								}
							});
							alertbox.show();			
						}
					});				
				}
				else {
					String EmptyList[] = new String[1];
					EmptyList[0] = "No members available to friend...";
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(FriendRequestActivity.this, R.layout.friendrequest_listview, R.id.friendrequest_listview_name, EmptyList);   
					members.setAdapter(adapter);
				}
				dialog.dismiss();				
			}
		}
	};
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	public void onClick(View v) {
		// TODO Auto-generated method stub
	}
}