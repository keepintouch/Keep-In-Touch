package app.master.kit;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class LogActivity extends KeepInTouchActivity implements OnClickListener {
	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.log);
		loadLog();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	public void onClick(View v) {
		// TODO Auto-generated method stub
	}

	private void loadLog() {
		TextView lblInfo = (TextView)findViewById(R.id.lblInfo);
		try {
			Process process = Runtime.getRuntime().exec("logcat -v time -d "+LOG_TAG+":I *:S");
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(process.getInputStream()));

			StringBuilder log=new StringBuilder();
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				log.append(line+'\n');
			}
			lblInfo.setText(log.toString());
		}
		catch (IOException e) {
			lblInfo.setText("ERROR: Unable to load Log!");
		}
	}

	public void onRefresh(View v) {
		loadLog();
	}

}