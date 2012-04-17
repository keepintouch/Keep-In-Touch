package app.master.kit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BroadcastStartupReceiver extends BroadcastReceiver{
	Alarm alarm = new Alarm();

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED") || intent.getAction().equals("app.master.custom.intent.action.STARTALARM")) {
			alarm.SetAlarm(context);
		}
		else if (intent.getAction().equals("app.master.custom.intent.action.CANCELALARM")) {
			alarm.CancelAlarm(context);    	
		}
		else if (intent.getAction().equals("app.master.custom.intent.action.STARTSERVICE")) {
			Intent svc = new Intent(context, PollService.class);
			context.startService(svc);
		}
	}
}