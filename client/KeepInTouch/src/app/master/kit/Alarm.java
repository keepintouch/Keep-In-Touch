package app.master.kit;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
//import android.widget.Toast;

public class Alarm extends BroadcastReceiver 
{    
	private static final String LOG_TAG = "KIT_LOG";
	@Override
	public void onReceive(Context context, Intent intent) 
	{   
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "YOUR TAG");
		wl.acquire();

		Intent i = new Intent("app.master.custom.intent.action.STARTSERVICE");
		context.sendBroadcast(i);
		//Toast.makeText(context, "Alarm !!", Toast.LENGTH_LONG).show(); // For example

		wl.release();
	}

	public void SetAlarm(Context context)
	{
		//Toast.makeText(context, "SetAlarm Called", Toast.LENGTH_LONG).show(); // For example
		// Load update interval from our preferences
		int updateInterval = 10; // Default update interval is 10 minutes
		String uInterval = "";
		StorageDB storageDB = new StorageDB(context);
		uInterval = storageDB.getPrefValue("update_interval");
		if (uInterval != null) {
			try {
				updateInterval = Integer.parseInt(uInterval);
			} catch (NumberFormatException e) {
				updateInterval = -1;
			}
			if (updateInterval > 0) {
				Log.i(LOG_TAG, "SetAlarm Called - "+uInterval+" minutes");    		 
				AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
				Intent i = new Intent(context, Alarm.class);
				PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
				am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * updateInterval, pi); // Millisec * Second * Minute 					
			}
		}
		storageDB.close();
	}

	public void CancelAlarm(Context context)
	{
		//Toast.makeText(context, "CancelAlarm Called", Toast.LENGTH_LONG).show(); // For example
		Log.i(LOG_TAG, "CancelAlarm Called");
		Intent intent = new Intent(context, Alarm.class);
		PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(sender);
		sender.cancel();
	}
}
