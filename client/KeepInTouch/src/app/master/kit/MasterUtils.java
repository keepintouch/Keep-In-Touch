package app.master.kit;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

public class MasterUtils {
	public boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list =
				packageManager.queryIntentActivities(intent,
						PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	public String padLeft(String s, int length, String pad_with) {
		length = length - s.length(); 
		String padding = "";
		if (length > 0) {
			StringBuilder sb = new StringBuilder(pad_with);
			while (sb.length() < length)
			{ sb.append(pad_with); }
			padding = sb.toString();
		}
		return padding+s;
	}

	public String md5(final String s) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++) {
				String h = Integer.toHexString(0xFF & messageDigest[i]);
				while (h.length() < 2)
					h = "0" + h;
				hexString.append(h);
			}
			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			return "ERROR";
		}
	}
}
