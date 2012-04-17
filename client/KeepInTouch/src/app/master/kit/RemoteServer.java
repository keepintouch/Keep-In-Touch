package app.master.kit;

import org.json.JSONArray;
import org.json.JSONObject;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class RemoteServer {
	private Context context;
	private static final String LOG_TAG = "KIT_LOG";
	private StorageDB storageDB;

	public RemoteServer(Context context) {
		this.context = context;
		storageDB = new StorageDB(this.context);
	}

	public boolean send(Cursor serverInfo, String jsonData) {		
		if (communicate(serverInfo, jsonData) != null) {
			return true;
		}
		else {
			return false;
		}
	}

	public JSONArray sendreceive(Cursor serverInfo, String jsonData) {
		return communicate(serverInfo, jsonData);
	}

	private JSONArray communicate (Cursor serverInfo, String jsonData) {
		String thedata = jsonData;
		String[][] theFiles = { };

		String theURL = serverInfo.getString(serverInfo.getColumnIndex(StorageDB.SERVERS_COL_URL));

		String[][] theVariables = { {"data", thedata} };
		String[] poster_output = null;
		MasterURLPoster POSTURL = null;
		POSTURL = null;
		POSTURL = new MasterURLPoster(theURL, "upload_file", theFiles, theVariables, 1);
		try { Thread.sleep(1000); } catch (InterruptedException e1) { }
		poster_output = POSTURL.Go();
		
		EncryptionProvider enc = new EncryptionProvider();
		String[] encryption_types = enc.getProviders();

		JSONArray jsonArray = null;
		if (poster_output[0] == "1") {
			try {
				jsonArray = new JSONArray(poster_output[1]);
				//Log.i(LOG_TAG,"SERVER RESPONSES - Number of entries " + jsonArray.length());
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject jsonObject = jsonArray.getJSONObject(i);
					if (jsonObject.has("response")) {							
						Log.i(LOG_TAG, "SERVER="+jsonObject.getString("response")+","+jsonObject.getString("msg"));
					}
					else {
						if (jsonObject.has("cmd")) {							
							if (jsonObject.getString("cmd").equals("query_location_response")) {										
								Cursor cursor = storageDB.cursorSelectMemberIDLocations(jsonObject.getString("memberid"));
								if (cursor != null) {
									ContentValues values = new ContentValues();
									values.put(StorageDB.LOCATIONS_COL_SYNCED, 1);
									values.put(StorageDB.LOCATIONS_COL_MEMBERID, jsonObject.getString("memberid"));
									values.put(StorageDB.LOCATIONS_COL_DATETIME, Long.parseLong(jsonObject.getString("datetime")));
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
												enc.setPassword(serverInfo.getString(serverInfo.getColumnIndex(StorageDB.SERVERS_COL_ENCRYPTPASS)));
												String decrypted_latlon = enc.decrypt(server_encryption, latlon);
												latlon = decrypted_latlon;
											}
											catch (Exception e) {
												Log.i(LOG_TAG,"We failed to decrypt a location from user - "+jsonObject.getString("memberid"));
												everything_okay = false;
											}
										}
									}
									if (everything_okay) {
										try {
											String latlon_fields[] = latlon.split(",");
											values.put(StorageDB.LOCATIONS_COL_LAT, latlon_fields[0]);
											values.put(StorageDB.LOCATIONS_COL_LON, latlon_fields[1]);
											values.put(StorageDB.LOCATIONS_COL_ACCURACY, latlon_fields[2]);
											values.put(StorageDB.LOCATIONS_COL_ALTITUDE, latlon_fields[3]);
											if (cursor.getCount() > 0) {
												cursor.moveToFirst();
												String id = cursor.getString(cursor.getColumnIndex(StorageDB.LOCATIONS_COL_ID));
												// We try and preserve our geocode data in our database, if lat/lon are still the same
												String oldlat = cursor.getString(cursor.getColumnIndex(StorageDB.LOCATIONS_COL_LAT));
												String oldlon = cursor.getString(cursor.getColumnIndex(StorageDB.LOCATIONS_COL_LON));										
												String oldgeocode = cursor.getString(cursor.getColumnIndex(StorageDB.LOCATIONS_COL_GEOCODE));
												if (oldlat.equals(latlon_fields[0]) && oldlon.equals(latlon_fields[1]))
												{ values.put(StorageDB.LOCATIONS_COL_GEOCODE, oldgeocode); }
												else
												{ values.put(StorageDB.LOCATIONS_COL_GEOCODE, ""); }
												storageDB.updateLocations(id, values);
											}
											else {
												values.put(StorageDB.LOCATIONS_COL_GEOCODE, "");
												storageDB.insertLocations(values);
											}
										}
										catch (Exception e) {
											Log.i(LOG_TAG,"Decryption of location data from user - "+jsonObject.getString("memberid")+" failed.");
										}
									}
								}
							}
							else if (jsonObject.getString("cmd").equals("introduce_response")) {
								int serverID = serverInfo.getInt(serverInfo.getColumnIndex(StorageDB.SERVERS_COL_ID));
								ContentValues values = new ContentValues();
								values.put(StorageDB.SERVERS_COL_MEMBERID, jsonObject.getString("MemberID"));
								values.put(StorageDB.SERVERS_COL_SECUREID, jsonObject.getString("SecureID"));
								storageDB.updateServers(serverID, values);
								//Log.i(LOG_TAG,"Server gave us secureid="+jsonObject.getString("SecureID")+" and memberid="+jsonObject.getString("MemberID"));
							}
							else if (jsonObject.getString("cmd").equals("poll_response")) {
								if (jsonObject.getString("task").equals("friend_request")) {
									Log.i(LOG_TAG,"We have a friend request!");
									ContentValues values = new ContentValues();
									values.put(StorageDB.FRIENDREQUESTS_COL_REQUESTERID, jsonObject.getString("data1"));
									values.put(StorageDB.FRIENDREQUESTS_COL_REQUESTERNAME, jsonObject.getString("data2"));
									values.put(StorageDB.FRIENDREQUESTS_COL_REQUESTERMSG, jsonObject.getString("data3"));
									values.put(StorageDB.FRIENDREQUESTS_COL_SERVERURL, theURL);
									values.put(StorageDB.FRIENDREQUESTS_COL_RSAPUBMOD, jsonObject.getString("data4"));
									values.put(StorageDB.FRIENDREQUESTS_COL_RSAPUBEXP, jsonObject.getString("data5"));
									values.put(StorageDB.FRIENDREQUESTS_COL_ENCRYPTION, jsonObject.getString("data6"));
									values.put(StorageDB.FRIENDREQUESTS_COL_ENCRYPTIONPASS, jsonObject.getString("data7"));
									values.put(StorageDB.FRIENDREQUESTS_COL_NOTIFIED, "N");
									storageDB.insertFriendRequest(values);
								}
								else if (jsonObject.getString("task").equals("friend_response")) {
									Log.i(LOG_TAG,"We have a friend response!");
									// Currently not doing anything with the jsonObject.getString("data5") [MemberMsg],
									// But if we wanted to, we could do another "Notification" to let the user know the
									// friend accepted his friend request.
									ContentValues values = new ContentValues();
									values.put(StorageDB.FRIENDS_COL_MEMBERID, jsonObject.getString("data1"));
									values.put(StorageDB.FRIENDS_COL_SERVERURL, theURL);
									values.put(StorageDB.FRIENDS_COL_RSAPUBMOD, jsonObject.getString("data3"));
									values.put(StorageDB.FRIENDS_COL_RSAPUBEXP, jsonObject.getString("data4"));
									values.put(StorageDB.FRIENDS_COL_NICKNAME, jsonObject.getString("data2"));
									storageDB.insertFriend(values);
									// Now let's decrypt and update our server password with what our friend sent us
									String rsa_pub_mod = storageDB.getPrefValue("rsa_pub_mod");
									String rsa_pub_exp = storageDB.getPrefValue("rsa_pub_exp");
									String rsa_priv_mod = storageDB.getPrefValue("rsa_priv_mod");
									String rsa_priv_exp = storageDB.getPrefValue("rsa_priv_exp");
									String decrypted_password = "";
									boolean everything_okay = true;
									if (!jsonObject.getString("data6").equals("none") && !jsonObject.getString("data6").equals("")) {
										if (rsa_pub_mod != null && rsa_pub_exp != null && rsa_priv_mod != null && rsa_priv_exp != null) {
											RSAProvider rsacrypt = new RSAProvider();
											if (rsacrypt.LoadPublicPrivate(rsa_pub_mod, rsa_pub_exp, rsa_priv_mod, rsa_priv_exp)) {
												try {
													decrypted_password = rsacrypt.decrypt(jsonObject.getString("data7"));
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
									}
									else {
										// Since we didn't get anything good from our friend (no new password), don't record anything
										everything_okay = false;
									}
									if (everything_okay)
									{
										ContentValues valuesS = new ContentValues();
										valuesS.put(StorageDB.SERVERS_COL_ENCRYPT, jsonObject.getString("data6"));
										valuesS.put(StorageDB.SERVERS_COL_ENCRYPTPASS, decrypted_password);
										storageDB.updateServers(serverInfo.getInt(serverInfo.getColumnIndex(StorageDB.SERVERS_COL_ID)), valuesS);
										// Enable following for debugging only
										//Log.i(LOG_TAG,"Our Friend sent us: "+jsonObject.getString("data6")+" - "+decrypted_password);
									}
								}
								else if (jsonObject.getString("task").equals("friend_delete")) {
									Log.i(LOG_TAG,"We have a friend delete request!");
									Cursor cursor = storageDB.cursorSelectFriendID(jsonObject.getString("data1"));
									if (cursor != null) {
										if (cursor.getCount() > 0) {
											cursor.moveToFirst();
											storageDB.deleteFriend(cursor.getString(cursor.getColumnIndex(StorageDB.FRIENDS_COL_ID)));
										}
									}
								}
							}
						}
					}

				}
			} catch (Exception e) {
				Log.i(LOG_TAG, "JSON error parsing server response: "+poster_output[1]);
				e.printStackTrace();
			}
		}
		else {
			Log.i(LOG_TAG, "HTTP Communication Failed: " + poster_output[1]);
		}
		return jsonArray;
	}

	public void close() {
		storageDB.close();
	}

}
