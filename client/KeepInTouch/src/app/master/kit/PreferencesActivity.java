package app.master.kit;

import org.json.JSONObject;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

public class PreferencesActivity extends PreferenceActivity {
	EditTextPreference prefName;
	private String prefName_value = "Please enter your name here...";
	ListPreference prefNearMeRadius;
	private String prefNearMeRadius_value = "5";  // Default
	ListPreference prefUpdateInterval;
	private String prefUpdateInterval_value = "10";  // Default
	EditTextPreference prefLockCode;
	private String prefLockCode_value = "";
	EditTextPreference prefServerName[];
	EditTextPreference prefServerURL[];
	CheckBoxPreference prefServerAllowFriendRequests[];
	CheckBoxPreference prefServerPublicAccount[];
	ListPreference prefServerEncryption[];
	EditTextPreference prefServerEncryptionPass[];
	private boolean firstTimeSetup = true;
	private boolean prefsChanged = false; 
	private boolean prefsServersChanged = false;
	private int prefServerIDs[];
	private int NewServerNumber = 0;
	private int DeleteServerNumber = -1;
	private String[] ServersList;
	private int MAX_SERVERS = 10;

	private StorageDB storageDB;
	private String rsa_pub_mod = "";
	private String rsa_pub_exp = "";
	private String rsa_priv_mod = "";
	private String rsa_priv_exp = "";
	private ProgressDialog dialog;

	private PreferenceScreen root;
	private PreferenceCategory ServerPrefCat;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			storageDB.close();
		} catch (NullPointerException e)
		{ }
		storageDB = new StorageDB(this);
		LoadPreferences();
		setPreferenceScreen(createPreferenceHierarchy());
		if (rsa_pub_mod.length() == 0) {
			GenerateRSA();
		}
	}

	private void LoadPreferences() {
		Cursor cursor = storageDB.cursorSelectAllPrefs();
		if (cursor != null) {
			if (cursor.getCount() > 0) {
				boolean more_entries = cursor.moveToFirst();
				while (more_entries) {
					if (cursor.getString(cursor.getColumnIndex(StorageDB.PREFS_COL_NAME)).equals("display_name"))
					{ prefName_value = cursor.getString(cursor.getColumnIndex(StorageDB.PREFS_COL_DATA)); }
					else if (cursor.getString(cursor.getColumnIndex(StorageDB.PREFS_COL_NAME)).equals("rsa_pub_mod"))
					{ rsa_pub_mod = cursor.getString(cursor.getColumnIndex(StorageDB.PREFS_COL_DATA)); }
					else if (cursor.getString(cursor.getColumnIndex(StorageDB.PREFS_COL_NAME)).equals("rsa_pub_exp"))
					{ rsa_pub_exp = cursor.getString(cursor.getColumnIndex(StorageDB.PREFS_COL_DATA)); }
					else if (cursor.getString(cursor.getColumnIndex(StorageDB.PREFS_COL_NAME)).equals("rsa_priv_mod"))
					{ rsa_priv_mod = cursor.getString(cursor.getColumnIndex(StorageDB.PREFS_COL_DATA)); }
					else if (cursor.getString(cursor.getColumnIndex(StorageDB.PREFS_COL_NAME)).equals("rsa_priv_exp"))
					{ rsa_priv_exp = cursor.getString(cursor.getColumnIndex(StorageDB.PREFS_COL_DATA)); }
					else if (cursor.getString(cursor.getColumnIndex(StorageDB.PREFS_COL_NAME)).equals("near_me_radius"))
					{ prefNearMeRadius_value = cursor.getString(cursor.getColumnIndex(StorageDB.PREFS_COL_DATA)); }
					else if (cursor.getString(cursor.getColumnIndex(StorageDB.PREFS_COL_NAME)).equals("lock_code"))
					{ prefLockCode_value = cursor.getString(cursor.getColumnIndex(StorageDB.PREFS_COL_DATA)); }
					else if (cursor.getString(cursor.getColumnIndex(StorageDB.PREFS_COL_NAME)).equals("update_interval"))
					{ prefUpdateInterval_value = cursor.getString(cursor.getColumnIndex(StorageDB.PREFS_COL_DATA)); firstTimeSetup = false; }
					//else if (cursor.getString(cursor.getColumnIndex(StorageDB.PREFS_COL_NAME)).equals("last_sync"))
					//{  = cursor.getString(cursor.getColumnIndex(StorageDB.PREFS_COL_DATA)); }
					more_entries = cursor.moveToNext();
				}
			}
		}    	
	}


	private void showServer(int index, int id, String name, String url, boolean allow_friend_request, boolean public_account, String encryption_type, String encryption_pass) {
		// Note: 'id' is the storage database _id field for this server
		ServersList[index] = name;
		prefServerIDs[index] = id;
		PreferenceScreen screenPref = getPreferenceManager().createPreferenceScreen(this);
		screenPref.setTitle(name);
		screenPref.setSummary("Click To Modify Server Settings");
		ServerPrefCat.addPreference(screenPref);

		prefServerName[index] = new EditTextPreference(this);
		// prefServerName - Edit text preference
		prefServerName[index] = new EditTextPreference(this);
		prefServerName[index].setDialogTitle("Server Name");
		prefServerName[index].setTitle("Server Name");
		prefServerName[index].setSummary(name);
		prefServerName[index].setText(name);
		prefServerName[index].getEditText().setSingleLine();
		prefServerName[index].getEditText().setHint("Please enter server name");
		prefServerName[index].setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference pref, Object newValue) {
				pref.setSummary((String) newValue);
				prefsServersChanged = true;
				return true;
			}
		});
		screenPref.addPreference(prefServerName[index]);

		// prefServerURL - Edit text preference
		prefServerURL[index] = new EditTextPreference(this);
		prefServerURL[index].setDialogTitle("Server URL");
		prefServerURL[index].setTitle("Server URL");
		prefServerURL[index].setSummary(url);
		prefServerURL[index].setText(url);
		prefServerURL[index].getEditText().setSingleLine();
		prefServerURL[index].getEditText().setHint("Please enter server url");
		prefServerURL[index].setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference pref, Object newValue) {
				pref.setSummary((String) newValue);
				prefsServersChanged = true;
				return true;
			}
		});
		screenPref.addPreference(prefServerURL[index]);

		// Toggle preference
		prefServerAllowFriendRequests[index] = new CheckBoxPreference(this);
		prefServerAllowFriendRequests[index].setTitle("Allow Friend Request");
		prefServerAllowFriendRequests[index].setSummary("Allow New Friends to Send you a Request");
		if (allow_friend_request) {
			prefServerAllowFriendRequests[index].setChecked(true);
		} else {
			prefServerAllowFriendRequests[index].setChecked(false);
		}
		prefServerAllowFriendRequests[index].setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference pref, Object newValue) {
				prefsServersChanged = true;
				return true;
			}
		});
		screenPref.addPreference(prefServerAllowFriendRequests[index]);

		// Toggle preference
		prefServerPublicAccount[index] = new CheckBoxPreference(this);
		prefServerPublicAccount[index].setTitle("Public Account");
		prefServerPublicAccount[index].setSummary("Display this Account on the Server");
		if (public_account) {
			prefServerPublicAccount[index].setChecked(true);
		} else {
			prefServerPublicAccount[index].setChecked(false);
		}
		prefServerPublicAccount[index].setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference pref, Object newValue) {
				prefsServersChanged = true;
				return true;
			}
		});
		screenPref.addPreference(prefServerPublicAccount[index]);

		PreferenceCategory ServerEncPrefCat = new PreferenceCategory(this);
		ServerEncPrefCat.setTitle("Encryption");
		screenPref.addPreference(ServerEncPrefCat);

		// prefEncryption - List preference
		EncryptionProvider enc = new EncryptionProvider();
		String[] encryption_types_values = enc.getProviders();
		String[] encryption_types= new String[encryption_types_values.length];
		for (int i=0; i<encryption_types.length; i++) {
			encryption_types[i] = encryption_types_values[i].toUpperCase();
		}
		prefServerEncryption[index] = new ListPreference(this);
		prefServerEncryption[index].setEntries(encryption_types);
		prefServerEncryption[index].setEntryValues(encryption_types_values);
		prefServerEncryption[index].setDialogTitle("Encryption Algorithm");
		prefServerEncryption[index].setTitle("Encryption Algorithm");
		prefServerEncryption[index].setSummary(encryption_type.toUpperCase());
		prefServerEncryption[index].setValue(encryption_type);
		prefServerEncryption[index].setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference pref, Object newValue) {
				String e = (String) newValue;
				pref.setSummary(e.toUpperCase());
				prefsServersChanged = true;
				return true;
			}
		});
		ServerEncPrefCat.addPreference(prefServerEncryption[index]);

		// prefServerEncryptionPass - Edit text preference
		prefServerEncryptionPass[index] = new EditTextPreference(this);
		prefServerEncryptionPass[index].setDialogTitle("Encryption Pass Phrase");
		prefServerEncryptionPass[index].setTitle("Encryption Pass Phrase");
		prefServerEncryptionPass[index].setText(encryption_pass);
		if (encryption_pass.length() == 0) {        	
			prefServerEncryptionPass[index].setSummary("<blank>");
		} else {
			prefServerEncryptionPass[index].setSummary("<set>");
		}
		prefServerEncryptionPass[index].getEditText().setSingleLine();
		prefServerEncryptionPass[index].getEditText().setHint("Please enter a secure pass phrase");
		prefServerEncryptionPass[index].getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		prefServerEncryptionPass[index].setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference pref, Object newValue) {
				String v = (String) newValue;
				v = v.trim();
				if (v.length() == 0) {
					pref.setSummary("<blank>");
				}
				else {
					pref.setSummary("<set>");
				}
				prefsServersChanged = true;
				return true;
			}
		});
		ServerEncPrefCat.addPreference(prefServerEncryptionPass[index]);

	}

	private PreferenceScreen createPreferenceHierarchy() {
		// Root
		root = getPreferenceManager().createPreferenceScreen(this);

		// Main Preference Category 
		PreferenceCategory inlinePrefCat = new PreferenceCategory(this);
		inlinePrefCat.setTitle("Preferences");
		root.addPreference(inlinePrefCat);

		// prefName - Edit text preference
		prefName = new EditTextPreference(this);
		prefName.setDialogTitle("Your Name");
		prefName.setTitle("Name");
		prefName.setSummary(prefName_value);
		prefName.getEditText().setSingleLine();
		prefName.getEditText().setHint("Please enter your name");
		prefName.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference pref, Object newValue) {
				prefName_value = (String) newValue;
				pref.setSummary(prefName_value);
				prefsChanged = true;
				return true;
			}
		});
		inlinePrefCat.addPreference(prefName);

		// prefNearMeRadius - List preference
		prefNearMeRadius = new ListPreference(this);
		prefNearMeRadius.setEntries(R.array.array_near_me_radius);
		prefNearMeRadius.setEntryValues(R.array.array_near_me_radius_values);
		prefNearMeRadius.setDialogTitle("'Near Me' Radius");
		prefNearMeRadius.setTitle("'Near Me' Radius");
		prefNearMeRadius.setSummary("Show Friends Within "+prefNearMeRadius_value+" miles");
		prefNearMeRadius.setValue(prefNearMeRadius_value);
		prefNearMeRadius.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference pref, Object newValue) {
				prefNearMeRadius_value = (String) newValue;
				if (prefNearMeRadius_value.length() == 0)
				{ pref.setSummary("Show Friends Within This Radius"); }
				else
				{ pref.setSummary("Show Friends Within "+prefNearMeRadius_value+" miles"); }
				prefsChanged = true;
				return true;
			}
		});
		inlinePrefCat.addPreference(prefNearMeRadius);

		// prefUpdateInterval - List preference
		prefUpdateInterval = new ListPreference(this);
		prefUpdateInterval.setEntries(R.array.array_update_interval);
		prefUpdateInterval.setEntryValues(R.array.array_update_interval_values);
		prefUpdateInterval.setDialogTitle("Update Interval");
		prefUpdateInterval.setTitle("Update Interval");
		if (prefUpdateInterval_value.equals("0")) {
			prefUpdateInterval.setSummary("Disabled");
		}
		else {
			prefUpdateInterval.setSummary(prefUpdateInterval_value+" minutes");
		}
		prefUpdateInterval.setValue(prefUpdateInterval_value);
		prefUpdateInterval.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference pref, Object newValue) {
				String value = (String) newValue;
				prefUpdateInterval_value = value;
				if (value.length() == 0)
				{ pref.setSummary("Server Update/Poll Interval"); }
				else {
					if (value.equals("0")) {
						pref.setSummary("Disabled");            			
					}
					else {
						pref.setSummary(value+" minutes");
					}
				}
				prefsChanged = true;
				return true;
			}
		});
		inlinePrefCat.addPreference(prefUpdateInterval);

		// prefLockCode - Edit text preference
		prefLockCode = new EditTextPreference(this);
		prefLockCode.setDialogTitle("Lock Code");
		prefLockCode.setTitle("Lock Code");
		prefLockCode.setSummary("Not Set");
		prefLockCode.getEditText().setSingleLine();
		prefLockCode.getEditText().setHint("Please enter a lock code");
		prefLockCode.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		prefLockCode.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference pref, Object newValue) {
				prefLockCode_value = (String) newValue;
				prefLockCode_value = prefLockCode_value.trim();
				if (prefLockCode_value.length() == 0)
				{ pref.setSummary("Not Set"); }
				else
				{ pref.setSummary("Is Set"); }
				prefsChanged = true;
				return true;
			}
		});
		inlinePrefCat.addPreference(prefLockCode);

		// Server - New Screen Preferences
		ServerPrefCat = new PreferenceCategory(this);
		ServerPrefCat.setTitle(this.getString(R.string.app_name)+" Servers");
		root.addPreference(ServerPrefCat);

		// Loop through all of our servers

		NewServerNumber = 0;
		prefServerName = new EditTextPreference[MAX_SERVERS];
		prefServerURL = new EditTextPreference[MAX_SERVERS];
		prefServerAllowFriendRequests = new CheckBoxPreference[MAX_SERVERS];
		prefServerPublicAccount = new CheckBoxPreference[MAX_SERVERS];
		prefServerEncryption = new ListPreference[MAX_SERVERS];
		prefServerEncryptionPass = new EditTextPreference[MAX_SERVERS];
		prefServerIDs = new int[MAX_SERVERS];
		ServersList = new String[0];

		Cursor cursor = storageDB.cursorSelectAllServers();
		if (cursor != null) {
			if (cursor.getCount() > 0) {
				boolean more_entries = cursor.moveToFirst();
				while (more_entries) {
					growServersList();
					// SERVERS_COL_ENCRYPT, SERVERS_COL_ENCRYPTPASS,
					// SERVERS_COL_ALLOWFRIENDREQUESTS, SERVERS_COL_PUBLICACCOUNT
					int sID = cursor.getInt(cursor.getColumnIndex(StorageDB.SERVERS_COL_ID));
					String sNAME = cursor.getString(cursor.getColumnIndex(StorageDB.SERVERS_COL_NAME));
					String sURL = cursor.getString(cursor.getColumnIndex(StorageDB.SERVERS_COL_URL));
					boolean sALLOWFRIENDREQUEST = true;
					boolean sPUBLICACCOUNT = true;
					if (cursor.getString(cursor.getColumnIndex(StorageDB.SERVERS_COL_ALLOWFRIENDREQUESTS)).equals("N")) {
						sALLOWFRIENDREQUEST = false;
					}
					if (cursor.getString(cursor.getColumnIndex(StorageDB.SERVERS_COL_PUBLICACCOUNT)).equals("N")) {
						sPUBLICACCOUNT = false;
					}
					String sENC = cursor.getString(cursor.getColumnIndex(StorageDB.SERVERS_COL_ENCRYPT));
					String sENCPASS = cursor.getString(cursor.getColumnIndex(StorageDB.SERVERS_COL_ENCRYPTPASS));					
					showServer(NewServerNumber-1, sID, sNAME, sURL, sALLOWFRIENDREQUEST, sPUBLICACCOUNT, sENC, sENCPASS);
					more_entries = cursor.moveToNext();
				}
			}
		}    	



		return root;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.preferences, menu);		
		return true;
	}

	public void growServersList() {
		String[] bigger = new String[ServersList.length+1];
		for (int i=0; i<ServersList.length; i++) {
			bigger[i] = ServersList[i];
		}
		ServersList = bigger;
		NewServerNumber++;					
	}

	public void shrinkServersList(String ServerToDelete) {
		String[] smaller = new String[ServersList.length-1];
		int temp = 0;
		for (int i=0; i<ServersList.length; i++) {
			if (!ServersList[i].equals(ServerToDelete)) {
				smaller[temp] = ServersList[i];
				temp++;
			}
		}
		ServersList = smaller;
		NewServerNumber--;					
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menuPrefAddServer) {
			AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
			alertbox.setTitle("Add Server");
			alertbox.setMessage("Please Enter Server Name:");
			final EditText serverName = new EditText(this);
			serverName.setSingleLine();
			serverName.setHint("Please enter server name");
			alertbox.setView(serverName);
			alertbox.setPositiveButton("Add", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface arg0, int arg1) {
					growServersList();
					String name = serverName.getText().toString();
					showServer(NewServerNumber-1, -1, name, "", true, true, "none", "");
				}
			});
			alertbox.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface arg0, int arg1) {
					//Toast.makeText(getApplicationContext(), "'Delete' button clicked", Toast.LENGTH_LONG).show();
				}
			});
			alertbox.show();			
		}
		else if (item.getItemId() == R.id.menuPrefDeleteServer) {	
			if (ServersList.length > 0) {
				AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
				alertbox.setTitle("Select Server To Delete");
				DeleteServerNumber = 0;
				alertbox.setSingleChoiceItems(ServersList, 0, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						DeleteServerNumber = which;
					}
				});
				alertbox.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface arg0, int arg1) {
						if (prefServerIDs[DeleteServerNumber] != -1) {
							// Let's delete the server
							storageDB.deleteServers(prefServerIDs[DeleteServerNumber]);
							storageDB.deleteServersPasswords(Integer.toString(prefServerIDs[DeleteServerNumber]));
						}
						setPreferenceScreen(createPreferenceHierarchy());
					}
				});
				alertbox.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface arg0, int arg1) {
						// Clicked Cancel
					}
				});
				alertbox.show();
			} else {
				Toast.makeText(getApplicationContext(), "There are no servers to delete...", Toast.LENGTH_LONG).show();				
			}
		}
		else if (item.getItemId() == R.id.menuPrefLog) {			
			// Open Preferences Screen
			Intent myIntent = new Intent(PreferencesActivity.this, LogActivity.class);
			startActivity(myIntent);
		}

		// Consume the selection event.
		return true;
	}

	private void GenerateRSA() {
		dialog = ProgressDialog.show(this, "RSA Pub/Priv Key", "Generating Keys... Please wait...", true);

		Thread t = new Thread() {
			public void run() {
				Message msg = new Message();
				RSAProvider rsacrypt = new RSAProvider();
				if (rsacrypt.GenerateKeyPair()) {
					rsa_pub_mod = rsacrypt.PublicModulus();
					rsa_pub_exp = rsacrypt.PublicExponent();
					rsa_priv_mod = rsacrypt.PrivateModulus();
					rsa_priv_exp = rsacrypt.PrivateExponent();
					msg.what = 1;
					myThreadMessageHandler.sendMessage(msg);
				}
				else {
					msg.what = 0;
				}
			}
		};
		t.start();
	}

	Handler myThreadMessageHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				storageDB.setPref("rsa_pub_mod", rsa_pub_mod);
				storageDB.setPref("rsa_pub_exp", rsa_pub_exp);
				storageDB.setPref("rsa_priv_mod", rsa_priv_mod);
				storageDB.setPref("rsa_priv_exp", rsa_priv_exp);
			}
			else {
				Toast.makeText(getApplicationContext(), "RSA Pub/Priv Generator Failed!", Toast.LENGTH_LONG).show();                           
			}
			dialog.dismiss();
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (prefName_value.equals("Please enter your name here...")) {
			prefName_value = "Name Unknown";
			prefsChanged = true;
		}

		if (prefsChanged) {
			storageDB.setPref("display_name", prefName_value);
			storageDB.setPref("near_me_radius", prefNearMeRadius_value);
			storageDB.setPref("update_interval", prefUpdateInterval.getValue());
			storageDB.setPref("lock_code", prefLockCode_value);
		}
		
		// If we are changing our update interval, then cancel and restart our alarm
		if (!prefUpdateInterval_value.equals(prefUpdateInterval.getValue())) {
			firstTimeSetup = true;
		}
		
		if (firstTimeSetup) {
			Intent i = new Intent("app.master.custom.intent.action.CANCELALARM");
			getApplicationContext().sendBroadcast(i);
			Intent ii = new Intent("app.master.custom.intent.action.STARTALARM");
			getApplicationContext().sendBroadcast(ii);
		}

		// Let's loop through and save all of our servers
		if (prefsServersChanged) {
			for (int i=0; i<ServersList.length; i++) {
				String sNAME = prefServerName[i].getText();
				String sURL = prefServerURL[i].getText();
				String sALLOWFRIENDREQUEST = "N";
				if (prefServerAllowFriendRequests[i].isChecked()) {
					sALLOWFRIENDREQUEST = "Y";
				}
				String sPUBLICACCOUNT = "N";
				if (prefServerPublicAccount[i].isChecked()) {
					sPUBLICACCOUNT = "Y";
				}
				String sENC = prefServerEncryption[i].getValue();
				String sENCPASS = prefServerEncryptionPass[i].getText();

				ContentValues values = new ContentValues();
				values.put(StorageDB.SERVERS_COL_NAME, sNAME);
				values.put(StorageDB.SERVERS_COL_URL, sURL);
				values.put(StorageDB.SERVERS_COL_ENCRYPT, sENC);
				values.put(StorageDB.SERVERS_COL_ENCRYPTPASS, sENCPASS);
				values.put(StorageDB.SERVERS_COL_ALLOWFRIENDREQUESTS, sALLOWFRIENDREQUEST);
				values.put(StorageDB.SERVERS_COL_PUBLICACCOUNT, sPUBLICACCOUNT);

				if (prefServerIDs[i] == -1) {
					values.put(StorageDB.SERVERS_COL_MEMBERID, "");
					values.put(StorageDB.SERVERS_COL_SECUREID, "");        		
					storageDB.insertServers(values);
					// Schedule Poll Job for "introduce(...)"
					StringBuilder jsonData = new StringBuilder();
					try {
						JSONObject json = new JSONObject();
						json.put("cmd", "introduce");
						json.put("id", "");
						json.put("name", prefName_value);
						json.put("rsa_pub_mod", rsa_pub_mod);
						json.put("rsa_pub_exp", rsa_pub_exp);
						json.put("allowfriendrequests", sALLOWFRIENDREQUEST);
						json.put("public", sPUBLICACCOUNT);
						json.put("updateinterval", prefUpdateInterval_value);
						jsonData.append(json.toString());
					} catch (Throwable t) {
					}
					ContentValues v = new ContentValues();
					v.put(StorageDB.POLLCOMMANDS_COL_SERVERURL, sURL);
					v.put(StorageDB.POLLCOMMANDS_COL_COMMAND, jsonData.toString());
					storageDB.insertPollCommands(v);				
				} else {
					boolean new_introduce = false;
					Cursor cursor_sinfo = storageDB.cursorSelectServerByID(prefServerIDs[i]);
					if (cursor_sinfo != null) {
						if (cursor_sinfo.getCount() > 0) {
							cursor_sinfo.moveToFirst();
							if (!sURL.equals(cursor_sinfo.getString(cursor_sinfo.getColumnIndex(StorageDB.SERVERS_COL_URL)))) {
								new_introduce = true;
							}
						}
					}
					
					storageDB.updateServers(prefServerIDs[i], values);

					if (!new_introduce) {
						// Schedule Poll Job for "update_settings(...)"
						Cursor cursor_s = storageDB.cursorSelectSecureIDOfServer(prefServerIDs[i]);
						if (cursor_s != null) {
							if (cursor_s.getCount() > 0) {
								cursor_s.moveToFirst();
								StringBuilder jsonData = new StringBuilder();
								try {
									JSONObject json = new JSONObject();
									json.put("cmd", "update_settings");
									json.put("id", cursor_s.getString(cursor_s.getColumnIndex(StorageDB.SERVERS_COL_SECUREID)));
									json.put("name", prefName_value);
									json.put("allowfriendrequests", sALLOWFRIENDREQUEST);
									json.put("public", sPUBLICACCOUNT);
									json.put("updateinterval", prefUpdateInterval_value);
									jsonData.append(json.toString());
								} catch (Throwable t) {
								}
								ContentValues v = new ContentValues();
								v.put(StorageDB.POLLCOMMANDS_COL_SERVERURL, sURL);
								v.put(StorageDB.POLLCOMMANDS_COL_COMMAND, jsonData.toString());
								storageDB.insertPollCommands(v);
							}
							else
							{ Toast.makeText(getApplicationContext(), "ERROR GETTING SERVER ID, COUNT=0", Toast.LENGTH_LONG).show(); }
						}
						else
						{ Toast.makeText(getApplicationContext(), "ERROR GETTING SERVER ID, CURSOR=null", Toast.LENGTH_LONG).show(); }
					}
					else {
						// We changed the Server URL, so we need to send a new introduce command!
						// Schedule Poll Job for "introduce(...)"
						StringBuilder jsonData = new StringBuilder();
						try {
							JSONObject json = new JSONObject();
							json.put("cmd", "introduce");
							json.put("id", "");
							json.put("name", prefName_value);
							json.put("rsa_pub_mod", rsa_pub_mod);
							json.put("rsa_pub_exp", rsa_pub_exp);
							json.put("allowfriendrequests", sALLOWFRIENDREQUEST);
							json.put("public", sPUBLICACCOUNT);
							json.put("updateinterval", prefUpdateInterval_value);
							jsonData.append(json.toString());
						} catch (Throwable t) {
						}
						ContentValues v = new ContentValues();
						v.put(StorageDB.POLLCOMMANDS_COL_SERVERURL, sURL);
						v.put(StorageDB.POLLCOMMANDS_COL_COMMAND, jsonData.toString());
						storageDB.insertPollCommands(v);				
					}
					
					
				}
			}
		}
		
		// Schedule an immediate update on the server
		if (prefsChanged || prefsServersChanged)
		{
			boolean service_is_already_running = false;
			ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
			for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
				if ("app.master.kit.PollService".equals(service.service.getClassName())) {
					service_is_already_running = true;
					break;
				}
			}
			if (!service_is_already_running) {
				Intent svc = new Intent(this, PollService.class);
				svc.putExtra("CMD", "POLL");
				startService(svc);
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		storageDB.close();
	}

}