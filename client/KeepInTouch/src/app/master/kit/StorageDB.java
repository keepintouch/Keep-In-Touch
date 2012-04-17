package app.master.kit;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class StorageDB extends SQLiteOpenHelper {
	private SQLiteDatabase db;
	private static final int DATABASE_VERSION = 9;
	private static final String DB_NAME = "kit.db";

	public static final String LOCATIONS_TABLE = "locations";
	public static final String LOCATIONS_COL_ID = "_id";
	public static final String LOCATIONS_COL_ID_DEF = "integer primary key autoincrement";
	public static final String LOCATIONS_COL_SYNCED = "synced";
	public static final String LOCATIONS_COL_SYNCED_DEF = "integer not null";
	public static final String LOCATIONS_COL_MEMBERID = "memberid";
	public static final String LOCATIONS_COL_MEMBERID_DEF = "text not null";
	public static final String LOCATIONS_COL_DATETIME = "datetime";
	public static final String LOCATIONS_COL_DATETIME_DEF = "integer not null";
	public static final String LOCATIONS_COL_LAT = "lat";
	public static final String LOCATIONS_COL_LAT_DEF = "text not null";
	public static final String LOCATIONS_COL_LON = "lon";
	public static final String LOCATIONS_COL_LON_DEF = "text not null";        
	public static final String LOCATIONS_COL_GEOCODE = "geocode";
	public static final String LOCATIONS_COL_GEOCODE_DEF = "text";        
	public static final String LOCATIONS_COL_ACCURACY = "accuracy";
	public static final String LOCATIONS_COL_ACCURACY_DEF = "text not null";        
	public static final String LOCATIONS_COL_ALTITUDE = "altitude";
	public static final String LOCATIONS_COL_ALTITUDE_DEF = "text not null";        

	public static final String PREFS_TABLE = "prefs";
	public static final String PREFS_COL_ID = "_id";
	public static final String PREFS_COL_ID_DEF = "integer primary key autoincrement";
	public static final String PREFS_COL_NAME = "name";
	public static final String PREFS_COL_NAME_DEF = "text not null";
	public static final String PREFS_COL_DATA = "data";
	public static final String PREFS_COL_DATA_DEF = "text not null";

	public static final String SERVERS_TABLE = "servers";
	public static final String SERVERS_COL_ID = "_id";
	public static final String SERVERS_COL_ID_DEF = "integer primary key autoincrement";
	public static final String SERVERS_COL_NAME = "name";
	public static final String SERVERS_COL_NAME_DEF = "text not null";
	public static final String SERVERS_COL_MEMBERID = "memberid";
	public static final String SERVERS_COL_MEMBERID_DEF = "text";
	public static final String SERVERS_COL_SECUREID = "secureid";
	public static final String SERVERS_COL_SECUREID_DEF = "text";
	public static final String SERVERS_COL_URL = "url";
	public static final String SERVERS_COL_URL_DEF = "text not null";
	public static final String SERVERS_COL_ENCRYPT = "encrypt";
	public static final String SERVERS_COL_ENCRYPT_DEF = "text";
	public static final String SERVERS_COL_ENCRYPTPASS = "encrypt_pass";
	public static final String SERVERS_COL_ENCRYPTPASS_DEF = "text";
	public static final String SERVERS_COL_ALLOWFRIENDREQUESTS = "allow_friend_requests";
	public static final String SERVERS_COL_ALLOWFRIENDREQUESTS_DEF = "text not null";
	public static final String SERVERS_COL_PUBLICACCOUNT = "public_account";
	public static final String SERVERS_COL_PUBLICACCOUNT_DEF = "text not null";        

	public static final String SERVERS_PASSWORDS_TABLE = "servers_passwords";
	public static final String SERVERS_PASSWORDS_COL_ID = "_id";
	public static final String SERVERS_PASSWORDS_COL_ID_DEF = "integer primary key autoincrement";
	public static final String SERVERS_PASSWORDS_COL_DATETIME = "datetime";
	public static final String SERVERS_PASSWORDS_COL_DATETIME_DEF = "integer not null";
	public static final String SERVERS_PASSWORDS_COL_SERVERID = "serverid";
	public static final String SERVERS_PASSWORDS_COL_SERVERID_DEF = "text not null";
	public static final String SERVERS_PASSWORDS_COL_PASS = "pass";
	public static final String SERVERS_PASSWORDS_COL_PASS_DEF = "text not null";

	public static final String POLLCOMMANDS_TABLE = "pollcommands";
	public static final String POLLCOMMANDS_COL_ID = "_id";
	public static final String POLLCOMMANDS_COL_ID_DEF = "integer primary key autoincrement";
	public static final String POLLCOMMANDS_COL_SERVERURL = "server_url";
	public static final String POLLCOMMANDS_COL_SERVERURL_DEF = "text not null";
	public static final String POLLCOMMANDS_COL_COMMAND = "command";
	public static final String POLLCOMMANDS_COL_COMMAND_DEF = "text not null";

	public static final String FRIENDS_TABLE = "friends";
	public static final String FRIENDS_COL_ID = "_id";
	public static final String FRIENDS_COL_ID_DEF = "integer primary key autoincrement";
	public static final String FRIENDS_COL_MEMBERID = "memberid";
	public static final String FRIENDS_COL_MEMBERID_DEF = "text not null";
	public static final String FRIENDS_COL_SERVERURL = "server_url";
	public static final String FRIENDS_COL_SERVERURL_DEF = "text not null";
	public static final String FRIENDS_COL_RSAPUBMOD = "rsa_pub_mod";
	public static final String FRIENDS_COL_RSAPUBMOD_DEF = "text not null";
	public static final String FRIENDS_COL_RSAPUBEXP = "rsa_pub_exp";
	public static final String FRIENDS_COL_RSAPUBEXP_DEF = "text not null";
	public static final String FRIENDS_COL_NICKNAME = "nickname";
	public static final String FRIENDS_COL_NICKNAME_DEF = "text not null";

	public static final String FRIENDREQUESTS_TABLE = "friendrequests";
	public static final String FRIENDREQUESTS_COL_ID = "_id";
	public static final String FRIENDREQUESTS_COL_ID_DEF = "integer primary key autoincrement";
	public static final String FRIENDREQUESTS_COL_REQUESTERID = "requesterid";
	public static final String FRIENDREQUESTS_COL_REQUESTERID_DEF = "text not null";
	public static final String FRIENDREQUESTS_COL_REQUESTERNAME = "requestername";
	public static final String FRIENDREQUESTS_COL_REQUESTERNAME_DEF = "text not null";
	public static final String FRIENDREQUESTS_COL_REQUESTERMSG = "requestermsg";
	public static final String FRIENDREQUESTS_COL_REQUESTERMSG_DEF = "text not null";
	public static final String FRIENDREQUESTS_COL_SERVERURL = "server_url";
	public static final String FRIENDREQUESTS_COL_SERVERURL_DEF = "text not null";
	public static final String FRIENDREQUESTS_COL_RSAPUBMOD = "rsa_pub_mod";
	public static final String FRIENDREQUESTS_COL_RSAPUBMOD_DEF = "text not null";
	public static final String FRIENDREQUESTS_COL_RSAPUBEXP = "rsa_pub_exp";
	public static final String FRIENDREQUESTS_COL_RSAPUBEXP_DEF = "text not null";
	public static final String FRIENDREQUESTS_COL_ENCRYPTION = "encryption";
	public static final String FRIENDREQUESTS_COL_ENCRYPTION_DEF = "text not null";
	public static final String FRIENDREQUESTS_COL_ENCRYPTIONPASS = "encryption_pass";
	public static final String FRIENDREQUESTS_COL_ENCRYPTIONPASS_DEF = "text not null";
	public static final String FRIENDREQUESTS_COL_NOTIFIED = "notified";
	public static final String FRIENDREQUESTS_COL_NOTIFIED_DEF = "text not null";

	public static final String POI_TABLE = "poi";
	public static final String POI_COL_ID = "_id";
	public static final String POI_COL_ID_DEF = "integer primary key autoincrement";
	public static final String POI_COL_NAME = "name";
	public static final String POI_COL_NAME_DEF = "text not null";
	public static final String POI_COL_DATETIME = "datetime";
	public static final String POI_COL_DATETIME_DEF = "integer not null";	
	public static final String POI_COL_LAT = "lat";
	public static final String POI_COL_LAT_DEF = "text not null";
	public static final String POI_COL_LON = "lon";
	public static final String POI_COL_LON_DEF = "text not null";
	public static final String POI_COL_GEOCODE = "geocode";
	public static final String POI_COL_GEOCODE_DEF = "text";        

	/**
	 * Constructor
	 * @param context the application context
	 */
	public StorageDB(Context context) {
		super(context, DB_NAME, null, DATABASE_VERSION);
		db = getWritableDatabase();
	}

	private void createTableLocations(SQLiteDatabase db) {
		db.execSQL("create table "+LOCATIONS_TABLE+"("+
				LOCATIONS_COL_ID+" "+LOCATIONS_COL_ID_DEF+", "+
				LOCATIONS_COL_SYNCED+" "+LOCATIONS_COL_SYNCED_DEF+", "+
				LOCATIONS_COL_MEMBERID+" "+LOCATIONS_COL_MEMBERID_DEF+", "+
				LOCATIONS_COL_DATETIME+" "+LOCATIONS_COL_DATETIME_DEF+", "+
				LOCATIONS_COL_LAT+" "+LOCATIONS_COL_LAT_DEF+", "+
				LOCATIONS_COL_LON+" "+LOCATIONS_COL_LON_DEF+", "+
				LOCATIONS_COL_GEOCODE+" "+LOCATIONS_COL_GEOCODE_DEF+", "+
				LOCATIONS_COL_ACCURACY+" "+LOCATIONS_COL_ACCURACY_DEF+", "+
				LOCATIONS_COL_ALTITUDE+" "+LOCATIONS_COL_ALTITUDE_DEF+");");
	}

	private void createTablePrefs(SQLiteDatabase db) {
		// name fields:
		// name, rsa_pub_mod, rsa_pub_exp, rsa_priv_mod, rsa_priv_exp
		// near_me_radius, lock_code, update_interval, last_sync
		// md5hash
		db.execSQL("create table "+PREFS_TABLE+"("+
				PREFS_COL_ID+" "+PREFS_COL_ID_DEF+", "+
				PREFS_COL_NAME+" "+PREFS_COL_NAME_DEF+", "+
				PREFS_COL_DATA+" "+PREFS_COL_DATA_DEF+");");    	
	}

	private void createTableServers(SQLiteDatabase db) {
		db.execSQL("create table "+SERVERS_TABLE+"("+
				SERVERS_COL_ID+" "+SERVERS_COL_ID_DEF+", "+
				SERVERS_COL_NAME+" "+SERVERS_COL_NAME_DEF+", "+
				SERVERS_COL_MEMBERID+" "+SERVERS_COL_MEMBERID_DEF+", "+
				SERVERS_COL_SECUREID+" "+SERVERS_COL_SECUREID_DEF+", "+
				SERVERS_COL_URL+" "+SERVERS_COL_URL_DEF+", "+
				SERVERS_COL_ENCRYPT+" "+SERVERS_COL_ENCRYPT_DEF+", "+
				SERVERS_COL_ENCRYPTPASS+" "+SERVERS_COL_ENCRYPTPASS_DEF+", "+
				SERVERS_COL_ALLOWFRIENDREQUESTS+" "+SERVERS_COL_ALLOWFRIENDREQUESTS_DEF+", "+
				SERVERS_COL_PUBLICACCOUNT+" "+SERVERS_COL_PUBLICACCOUNT_DEF+");");    	
	}

	private void createTableServersPasswords(SQLiteDatabase db) {
		db.execSQL("create table "+SERVERS_PASSWORDS_TABLE+"("+
				SERVERS_PASSWORDS_COL_ID+" "+SERVERS_PASSWORDS_COL_ID_DEF+", "+
				SERVERS_PASSWORDS_COL_DATETIME+" "+SERVERS_PASSWORDS_COL_DATETIME_DEF+", "+
				SERVERS_PASSWORDS_COL_SERVERID+" "+SERVERS_PASSWORDS_COL_SERVERID_DEF+", "+
				SERVERS_PASSWORDS_COL_PASS+" "+SERVERS_PASSWORDS_COL_PASS_DEF+");");    	
	}

	private void createTablePollcommands(SQLiteDatabase db) {
		db.execSQL("create table "+POLLCOMMANDS_TABLE+"("+
				POLLCOMMANDS_COL_ID+" "+POLLCOMMANDS_COL_ID_DEF+", "+
				POLLCOMMANDS_COL_SERVERURL+" "+POLLCOMMANDS_COL_SERVERURL_DEF+", "+
				POLLCOMMANDS_COL_COMMAND+" "+POLLCOMMANDS_COL_COMMAND_DEF+");");    	
	}

	private void createTableFriends(SQLiteDatabase db) {
		db.execSQL("create table "+FRIENDS_TABLE+"("+
				FRIENDS_COL_ID+" "+FRIENDS_COL_ID_DEF+", "+
				FRIENDS_COL_MEMBERID+" "+FRIENDS_COL_MEMBERID_DEF+", "+
				FRIENDS_COL_SERVERURL+" "+FRIENDS_COL_SERVERURL_DEF+", "+
				FRIENDS_COL_RSAPUBMOD+" "+FRIENDS_COL_RSAPUBMOD_DEF+", "+
				FRIENDS_COL_RSAPUBEXP+" "+FRIENDS_COL_RSAPUBEXP_DEF+", "+
				FRIENDS_COL_NICKNAME+" "+FRIENDS_COL_NICKNAME_DEF+");");   	
	}

	private void createTableFriendRequests(SQLiteDatabase db) {
		db.execSQL("create table "+FRIENDREQUESTS_TABLE+"("+
				FRIENDREQUESTS_COL_ID+" "+FRIENDREQUESTS_COL_ID_DEF+", "+
				FRIENDREQUESTS_COL_REQUESTERID+" "+FRIENDREQUESTS_COL_REQUESTERID_DEF+", "+
				FRIENDREQUESTS_COL_REQUESTERNAME+" "+FRIENDREQUESTS_COL_REQUESTERNAME_DEF+", "+				
				FRIENDREQUESTS_COL_REQUESTERMSG+" "+FRIENDREQUESTS_COL_REQUESTERMSG_DEF+", "+
				FRIENDREQUESTS_COL_SERVERURL+" "+FRIENDREQUESTS_COL_SERVERURL_DEF+", "+
				FRIENDREQUESTS_COL_RSAPUBMOD+" "+FRIENDREQUESTS_COL_RSAPUBMOD_DEF+", "+
				FRIENDREQUESTS_COL_RSAPUBEXP+" "+FRIENDREQUESTS_COL_RSAPUBEXP_DEF+", "+
				FRIENDREQUESTS_COL_ENCRYPTION+" "+FRIENDREQUESTS_COL_ENCRYPTION_DEF+", "+
				FRIENDREQUESTS_COL_ENCRYPTIONPASS+" "+FRIENDREQUESTS_COL_ENCRYPTIONPASS_DEF+", "+
				FRIENDREQUESTS_COL_NOTIFIED+" "+FRIENDREQUESTS_COL_NOTIFIED_DEF+");");
	}
	
	private void createTablePOI(SQLiteDatabase db) {
		db.execSQL("create table "+POI_TABLE+"("+
				POI_COL_ID+" "+POI_COL_ID_DEF+", "+
				POI_COL_NAME+" "+POI_COL_NAME_DEF+", "+
				POI_COL_DATETIME+" "+POI_COL_DATETIME_DEF+", "+
				POI_COL_LAT+" "+POI_COL_LAT_DEF+", "+
				POI_COL_LON+" "+POI_COL_LON_DEF+", "+
				POI_COL_GEOCODE+" "+POI_COL_GEOCODE_DEF+");");
	}

	/**
	 * Called at the time to create the DB.
	 * The create DB statement
	 * @param the SQLite DB
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		//Toast.makeText(this.context, "DB CREATE", Toast.LENGTH_LONG).show();
		createTableLocations(db);
		createTablePrefs(db);
		createTableServers(db);
		createTableServersPasswords(db);
		createTablePollcommands(db);
		createTableFriends(db);
		createTableFriendRequests(db);
		createTablePOI(db);
	}

	/**
	 * The Insert DB statement
	 */
	public void insertLocations(ContentValues values) {
		db.insert(LOCATIONS_TABLE, null, values);
	}

	/**
	 * The Update DB statement
	 */
	public void updateLocations(String id, ContentValues values) {
		db.update(LOCATIONS_TABLE, values, LOCATIONS_COL_ID+"="+id, null);
	}

	/**
	 * Wipe out the DB
	 */
	public void clearAllLocations() {
		db.delete(LOCATIONS_TABLE, null, null);
	}

	/**
	 * Select All the returns a Cursor
	 * @return the cursor for the DB selection
	 */
	public Cursor cursorSelectAllLocations() {
		Cursor cursor = this.db.query(
				LOCATIONS_TABLE, // Table Name
				new String[] { LOCATIONS_COL_ID, LOCATIONS_COL_MEMBERID, LOCATIONS_COL_DATETIME, LOCATIONS_COL_LAT, LOCATIONS_COL_LON, LOCATIONS_COL_GEOCODE}, // Columns to return
				null,       // SQL WHERE
				null,       // Selection Args
				null,       // SQL GROUP BY
				null,       // SQL HAVING
				LOCATIONS_COL_DATETIME + " DESC");    // SQL ORDER BY
		return cursor;
	}

	/**
	 * Select All the returns a Cursor
	 * @return the cursor for the DB selection
	 */
	public Cursor cursorSelectAllLocationsButMyOwn(String MemberIDs) {
		Cursor cursor = this.db.query(
				LOCATIONS_TABLE, // Table Name
				new String[] { LOCATIONS_COL_ID, LOCATIONS_COL_MEMBERID, LOCATIONS_COL_DATETIME, LOCATIONS_COL_LAT, LOCATIONS_COL_LON, LOCATIONS_COL_GEOCODE}, // Columns to return
				MemberIDs,       // SQL WHERE
				null,       // Selection Args
				null,       // SQL GROUP BY
				null,       // SQL HAVING
				LOCATIONS_COL_DATETIME + " DESC");    // SQL ORDER BY
		return cursor;
	}

	/**
	 * Select All the returns a Cursor
	 * @return the cursor for the DB selection
	 */
	public Cursor cursorSelectMemberIDLocations(String MemberID) {
		Cursor cursor = this.db.query(
				LOCATIONS_TABLE, // Table Name
				new String[] { LOCATIONS_COL_ID, LOCATIONS_COL_MEMBERID, LOCATIONS_COL_DATETIME, LOCATIONS_COL_LAT, LOCATIONS_COL_LON, LOCATIONS_COL_GEOCODE}, // Columns to return
				LOCATIONS_COL_MEMBERID+" = \""+MemberID+"\"",       // SQL WHERE
				null,       // Selection Args
				null,       // SQL GROUP BY
				null,       // SQL HAVING
				LOCATIONS_COL_DATETIME + " DESC");    // SQL ORDER BY
		return cursor;
	}

	/**
	 * Select All the returns a Cursor
	 * @return the cursor for the DB selection
	 */
	public Cursor cursorSelectAllPrefs() {
		Cursor cursor = this.db.query(
				PREFS_TABLE, // Table Name
				new String[] { PREFS_COL_ID, PREFS_COL_NAME, PREFS_COL_DATA}, // Columns to return
				null,       // SQL WHERE
				null,       // Selection Args
				null,       // SQL GROUP BY
				null,       // SQL HAVING
				null);      // SQL ORDER BY
		return cursor;
	}

	public String getPrefValue(String name) {
		Cursor cursor = this.db.query(
				PREFS_TABLE, // Table Name
				new String[] { PREFS_COL_DATA }, // Columns to return
				PREFS_COL_NAME+" = \""+name+"\"",       // SQL WHERE
				null,       // Selection Args
				null,       // SQL GROUP BY
				null,       // SQL HAVING
				null);    // SQL ORDER BY
		String return_val = null;
		if (cursor != null) {
			if (cursor.getCount() > 0) {
				cursor.moveToFirst();
				return_val = cursor.getString(cursor.getColumnIndex(PREFS_COL_DATA));
			}
		}
		return return_val;
	}

	public Cursor getPref(String name) {
		Cursor cursor = this.db.query(
				PREFS_TABLE, // Table Name
				new String[] { PREFS_COL_ID, PREFS_COL_NAME, PREFS_COL_DATA }, // Columns to return
				PREFS_COL_NAME+" = \""+name+"\"",       // SQL WHERE
				null,       // Selection Args
				null,       // SQL GROUP BY
				null,       // SQL HAVING
				null);    // SQL ORDER BY
		return cursor;
	}

	public void setPref(String name, String data) {
		Cursor cursor = getPref(name);
		if (cursor != null) {
			if (cursor.getCount() > 0) {
				cursor.moveToFirst();
				String id = cursor.getString(cursor.getColumnIndex(PREFS_COL_ID));
				ContentValues values = new ContentValues();
				values.put(PREFS_COL_NAME, cursor.getString(cursor.getColumnIndex(PREFS_COL_NAME)));
				values.put(PREFS_COL_DATA, data);
				db.update(PREFS_TABLE, values, PREFS_COL_ID+"="+id, null);
			}
			else {				
				ContentValues values = new ContentValues();
				values.put(PREFS_COL_NAME, name);
				values.put(PREFS_COL_DATA, data);
				db.insert(PREFS_TABLE, null, values);
			}
		}
	}

	/**
	 * Select All the returns a Cursor
	 * @return the cursor for the DB selection
	 */
	public Cursor cursorSelectAllServers() {
		Cursor cursor = this.db.query(
				SERVERS_TABLE, // Table Name
				new String[] { SERVERS_COL_ID, SERVERS_COL_NAME, SERVERS_COL_MEMBERID, SERVERS_COL_SECUREID, SERVERS_COL_URL, SERVERS_COL_ENCRYPT, SERVERS_COL_ENCRYPTPASS, SERVERS_COL_ALLOWFRIENDREQUESTS, SERVERS_COL_PUBLICACCOUNT }, // Columns to return
				null,       // SQL WHERE
				null,       // Selection Args
				null,       // SQL GROUP BY
				null,       // SQL HAVING
				null);    // SQL ORDER BY
		return cursor;
	}

	/**
	 * @return the cursor for the DB selection
	 */
	public Cursor cursorSelectSecureIDOfServer(int id) {
		Cursor cursor = this.db.query(
				SERVERS_TABLE, // Table Name
				new String[] { SERVERS_COL_SECUREID }, // Columns to return
				SERVERS_COL_ID+"="+id,       // SQL WHERE
				null,       // Selection Args
				null,       // SQL GROUP BY
				null,       // SQL HAVING
				null);    // SQL ORDER BY
		return cursor;
	}

	/**
	 * @return the cursor for the DB selection
	 */
	public Cursor cursorSelectServerByID(int id) {
		Cursor cursor = this.db.query(
				SERVERS_TABLE, // Table Name
				new String[] { SERVERS_COL_ID, SERVERS_COL_NAME, SERVERS_COL_MEMBERID, SERVERS_COL_SECUREID, SERVERS_COL_URL, SERVERS_COL_ENCRYPT, SERVERS_COL_ENCRYPTPASS, SERVERS_COL_ALLOWFRIENDREQUESTS, SERVERS_COL_PUBLICACCOUNT }, // Columns to return
				SERVERS_COL_ID+"="+id,       // SQL WHERE
				null,       // Selection Args
				null,       // SQL GROUP BY
				null,       // SQL HAVING
				null);    // SQL ORDER BY
		return cursor;
	}


	/**
	 * @return the cursor for the DB selection
	 */
	public Cursor cursorSelectServerByURL(String URL) {
		Cursor cursor = this.db.query(
				SERVERS_TABLE, // Table Name
				new String[] { SERVERS_COL_ID, SERVERS_COL_NAME, SERVERS_COL_MEMBERID, SERVERS_COL_SECUREID, SERVERS_COL_URL, SERVERS_COL_ENCRYPT, SERVERS_COL_ENCRYPTPASS, SERVERS_COL_ALLOWFRIENDREQUESTS, SERVERS_COL_PUBLICACCOUNT }, // Columns to return
				SERVERS_COL_URL+"=\""+URL+"\"",       // SQL WHERE
				null,       // Selection Args
				null,       // SQL GROUP BY
				null,       // SQL HAVING
				null);    // SQL ORDER BY
		return cursor;
	}
	
	/**
	 * The Insert DB statement
	 */
	public void insertServers(ContentValues values) {
		db.insert(SERVERS_TABLE, null, values);
	}

	/**
	 * The Update DB statement
	 */
	public void updateServers(Integer id, ContentValues values) {
		db.update(SERVERS_TABLE, values, SERVERS_COL_ID+"="+id, null);
	}

	/**
	 * The Delete entry from  DB statement
	 */
	public void deleteServers(Integer id) {
		db.delete(SERVERS_TABLE, SERVERS_COL_ID+"="+id, null);
	}

	/**
	 * The Delete entry from  DB statement
	 */
	public void deleteServersPasswords(String id) {
		db.delete(SERVERS_PASSWORDS_TABLE, SERVERS_PASSWORDS_COL_SERVERID+"="+id, null);
	}

	/**
	 * The Insert DB statement
	 */
	public void insertPollCommands(ContentValues values) {
		db.insert(POLLCOMMANDS_TABLE, null, values);
	}
	
	/**
	 * @return the cursor for the DB selection
	 */
	public Cursor cursorSelectServerURLPollCommands(String ServerURL) {
		Cursor cursor = this.db.query(
				POLLCOMMANDS_TABLE, // Table Name
				new String[] { POLLCOMMANDS_COL_COMMAND }, // Columns to return
				POLLCOMMANDS_COL_SERVERURL+" = \""+ServerURL+"\"",       // SQL WHERE
				null,       // Selection Args
				null,       // SQL GROUP BY
				null,       // SQL HAVING
				POLLCOMMANDS_COL_ID);    // SQL ORDER BY
		return cursor;
	}

	/**
	 * Wipe out the DB
	 */
	public void clearAllPollCommands() {    	
		db.delete(POLLCOMMANDS_TABLE, null, null);
	}

	/**
	 * The Insert DB statement
	 */
	public void insertFriend(ContentValues values) {
		db.insert(FRIENDS_TABLE, null, values);
	}

	/**
	 * The Update DB statement
	 */
	public void updateFriend(String id, ContentValues values) {
		db.update(FRIENDS_TABLE, values, FRIENDS_COL_ID+"="+id, null);
	}

	/**
	 * The Delete entry from  DB statement
	 */
	public void deleteFriend(String id) {
		db.delete(FRIENDS_TABLE, FRIENDS_COL_ID+"="+id, null);
	}

	/**
	 * @return the cursor for the DB selection
	 */
	public Cursor cursorSelectFriend(String id) {
		Cursor cursor = this.db.query(
				FRIENDS_TABLE, // Table Name
				new String[] { FRIENDS_COL_ID, FRIENDS_COL_MEMBERID, FRIENDS_COL_SERVERURL, FRIENDS_COL_RSAPUBMOD, FRIENDS_COL_RSAPUBEXP, FRIENDS_COL_NICKNAME }, // Columns to return
				FRIENDS_COL_ID+" = \""+id+"\"",       // SQL WHERE
				null,       // Selection Args
				null,       // SQL GROUP BY
				null,       // SQL HAVING
				FRIENDS_COL_NICKNAME);    // SQL ORDER BY
		return cursor;
	}

	/**
	 * @return the cursor for the DB selection
	 */
	public Cursor cursorSelectFriendID(String MemberID) {
		Cursor cursor = this.db.query(
				FRIENDS_TABLE, // Table Name
				new String[] { FRIENDS_COL_ID, FRIENDS_COL_MEMBERID, FRIENDS_COL_SERVERURL, FRIENDS_COL_RSAPUBMOD, FRIENDS_COL_RSAPUBEXP, FRIENDS_COL_NICKNAME }, // Columns to return
				FRIENDS_COL_MEMBERID+" = \""+MemberID+"\"",       // SQL WHERE
				null,       // Selection Args
				null,       // SQL GROUP BY
				null,       // SQL HAVING
				FRIENDS_COL_NICKNAME);    // SQL ORDER BY
		return cursor;
	}

	/**
	 * Select All the returns a Cursor
	 * @return the cursor for the DB selection
	 */
	public Cursor cursorSelectAllFriends() {
		Cursor cursor = this.db.query(
				FRIENDS_TABLE, // Table Name
				new String[] { FRIENDS_COL_ID, FRIENDS_COL_MEMBERID, FRIENDS_COL_SERVERURL, FRIENDS_COL_RSAPUBMOD, FRIENDS_COL_RSAPUBEXP, FRIENDS_COL_NICKNAME }, // Columns to return
				null,       // SQL WHERE
				null,       // Selection Args
				null,       // SQL GROUP BY
				null,       // SQL HAVING
				FRIENDS_COL_NICKNAME);    // SQL ORDER BY
		return cursor;
	}
	
	/**
	 * Select All the returns a Cursor
	 * @return the cursor for the DB selection
	 */
	public Cursor cursorSelectAllPOI() {
		Cursor cursor = this.db.query(
				POI_TABLE, // Table Name
				new String[] { POI_COL_ID, POI_COL_NAME, POI_COL_DATETIME, POI_COL_LAT, POI_COL_LON, POI_COL_GEOCODE }, // Columns to return
				null,       // SQL WHERE
				null,       // Selection Args
				null,       // SQL GROUP BY
				null,       // SQL HAVING
				POI_COL_NAME);    // SQL ORDER BY
		return cursor;
	}
	
	/**
	 * The Insert DB statement
	 */
	public void insertPOI(ContentValues values) {
		db.insert(POI_TABLE, null, values);
	}

	/**
	 * The Update DB statement
	 */
	public void updatePOI(String id, ContentValues values) {
		db.update(POI_TABLE, values, POI_COL_ID+"="+id, null);
	}

	/**
	 * The Delete entry from  DB statement
	 */
	public void deletePOI(String id) {
		db.delete(POI_TABLE, POI_COL_ID+"="+id, null);
	}

	/**
	 * The Insert DB statement
	 */
	public void insertFriendRequest(ContentValues values) {
		db.insert(FRIENDREQUESTS_TABLE, null, values);
	}

	/**
	 * The Update DB statement
	 */
	public void updateFriendRequest(String id, ContentValues values) {
		db.update(FRIENDREQUESTS_TABLE, values, FRIENDREQUESTS_COL_ID+"="+id, null);
	}

	/**
	 * The Delete entry from  DB statement
	 */
	public void deleteFriendRequest(String id) {
		db.delete(FRIENDREQUESTS_TABLE, FRIENDREQUESTS_COL_ID+"="+id, null);
	}

	/**
	 * Select All the returns a Cursor
	 * @return the cursor for the DB selection
	 */
	public Cursor cursorSelectAllPendingFriendRequests() {
		Cursor cursor = this.db.query(
				FRIENDREQUESTS_TABLE, // Table Name
				new String[] { FRIENDREQUESTS_COL_ID, FRIENDREQUESTS_COL_REQUESTERID, FRIENDREQUESTS_COL_REQUESTERNAME, FRIENDREQUESTS_COL_REQUESTERMSG, FRIENDREQUESTS_COL_SERVERURL, FRIENDREQUESTS_COL_RSAPUBMOD, FRIENDREQUESTS_COL_RSAPUBEXP, FRIENDREQUESTS_COL_ENCRYPTION, FRIENDREQUESTS_COL_ENCRYPTIONPASS }, // Columns to return
				FRIENDREQUESTS_COL_NOTIFIED+" = \"N\"",       // SQL WHERE
				null,       // Selection Args
				null,       // SQL GROUP BY
				null,       // SQL HAVING
				FRIENDREQUESTS_COL_REQUESTERNAME);    // SQL ORDER BY
		return cursor;
	}

	/**
	 * Select All the returns a Cursor
	 * @return the cursor for the DB selection
	 */
	public Cursor cursorSelectFriendRequest(String id) {
		Cursor cursor = this.db.query(
				FRIENDREQUESTS_TABLE, // Table Name
				new String[] { FRIENDREQUESTS_COL_ID, FRIENDREQUESTS_COL_REQUESTERID, FRIENDREQUESTS_COL_REQUESTERNAME, FRIENDREQUESTS_COL_REQUESTERMSG, FRIENDREQUESTS_COL_SERVERURL, FRIENDREQUESTS_COL_RSAPUBMOD, FRIENDREQUESTS_COL_RSAPUBEXP, FRIENDREQUESTS_COL_ENCRYPTION, FRIENDREQUESTS_COL_ENCRYPTIONPASS }, // Columns to return
				FRIENDREQUESTS_COL_ID+" = \""+id+"\"",       // SQL WHERE
				null,       // Selection Args
				null,       // SQL GROUP BY
				null,       // SQL HAVING
				FRIENDREQUESTS_COL_REQUESTERNAME);    // SQL ORDER BY
		return cursor;
	}

	/**
	 * Select All the returns a Cursor
	 * @return the cursor for the DB selection
	 */
	public Cursor cursorSelectAllFriendRequests() {
		Cursor cursor = this.db.query(
				FRIENDREQUESTS_TABLE, // Table Name
				new String[] { FRIENDREQUESTS_COL_ID, FRIENDREQUESTS_COL_REQUESTERID, FRIENDREQUESTS_COL_REQUESTERNAME, FRIENDREQUESTS_COL_REQUESTERMSG, FRIENDREQUESTS_COL_SERVERURL, FRIENDREQUESTS_COL_RSAPUBMOD, FRIENDREQUESTS_COL_RSAPUBEXP, FRIENDREQUESTS_COL_ENCRYPTION, FRIENDREQUESTS_COL_ENCRYPTIONPASS }, // Columns to return
				null,       // SQL WHERE
				null,       // Selection Args
				null,       // SQL GROUP BY
				null,       // SQL HAVING
				FRIENDREQUESTS_COL_REQUESTERNAME);    // SQL ORDER BY
		return cursor;
	}

	/**
	 * Invoked if a DB upgrade (version change) has been detected
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Upgrading database between versions

		if (oldVersion == 1) {
			// Version 1 database didn't have "friends" or "poi" tables
			// Version 1 database's "servers" table didn't have "secureid" field
			// Version 1 database's "locations" table didn't have "accuracy" and "altitude" fields
			db.execSQL("DROP TABLE IF EXISTS "+SERVERS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS "+LOCATIONS_TABLE);
			createTableLocations(db);
			createTableServers(db);
			createTableFriends(db);
			createTablePOI(db);
		}
		if (oldVersion == 2) {
			// Version 2 database's "locations" table didn't have "geocode" fields
			// Version 2 database's "pref" table didn't have the right rsa pub/priv fields
			db.execSQL("DROP TABLE IF EXISTS "+LOCATIONS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS "+PREFS_TABLE);
			createTableLocations(db);
			createTablePrefs(db);
		}
		if (oldVersion == 3) {
			// Version 3 database's "pref" table didn't have the correct data type for the data field
			// Version 3 database's "servers" table didn't have memberid, encrypt, encrypt_pass
			// Version 3 database's "friends" table didn't have rsa_pub_mod, rsa_pub_exp fields
			db.execSQL("DROP TABLE IF EXISTS "+PREFS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS "+SERVERS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS "+FRIENDS_TABLE);
			createTablePrefs(db);
			createTableServers(db);
			createTableFriends(db);
		}
		if (oldVersion == 4) {
			// Version 4 database's "servers_passwords" table didn't have the correct fields
			// Version 4 database's "friends" table didn't have a "serverid" field.
			db.execSQL("DROP TABLE IF EXISTS "+SERVERS_PASSWORDS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS "+FRIENDS_TABLE);
			createTableServersPasswords(db);
			createTableFriends(db);
		}    	
		if (oldVersion == 5) {
			// Version 5 database's "poi" table didn't have the geocode and datetime fields
			db.execSQL("DROP TABLE IF EXISTS "+POI_TABLE);
			createTablePOI(db);
		}
		if (oldVersion == 6) {
			// Version 6 database didn't have the friendrequests table
			createTableFriendRequests(db);
		}
		if (oldVersion == 7) {
			// Version 7 database didn't have all of the required friendrequests fields
			// Version 7 database didn't have the correct server_url field in the friends table
			db.execSQL("DROP TABLE IF EXISTS "+FRIENDS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS "+FRIENDREQUESTS_TABLE);
			createTableFriends(db);
			createTableFriendRequests(db);
		}
		if (oldVersion == 8) {
			// Version 8 database didn't have all of the required friendrequests fields
			db.execSQL("DROP TABLE IF EXISTS "+FRIENDREQUESTS_TABLE);
			createTableFriendRequests(db);
		}

	}

}