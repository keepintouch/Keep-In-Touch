package app.master.kit;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;

public class GetGeocode {
	private Geocoder geocoder;
	private Hashtable<String,String> recent_geocoder = new Hashtable<String,String>();
	private String return_address;
	private String hashKey;
	private URL serverAddress;

	public GetGeocode(Context context) {
		geocoder = new Geocoder(context);
	}

	public String[] getAddress(String lat, String lon) {
		String[] retval = {"1", ""};
		double dlat = Double.parseDouble(lat);
		double dlon = Double.parseDouble(lon);

		return_address = "No Address Found";
		hashKey = lat+lon;
		boolean found_addr = recent_geocoder.containsKey(hashKey);

		if (found_addr == true) {
			return_address = recent_geocoder.get(hashKey);
		}
		else {
			// If we are in the Android Emulator, we must use Google's Geocode HTTP Lookup
			if ("sdk".equals(Build.PRODUCT) || "google_sdk".equals(Build.PRODUCT)) { 
				return_address = "Android Emulator - Geocode Not Available";

				int connResponse = -1;
				// build the URL using the latitude & longitude you want to lookup
				// NOTE: I chose XML return format here but you can choose something else
				HttpURLConnection connection = null;
				try {
					serverAddress = new URL("http://maps.googleapis.com/maps/api/geocode/json?latlng=" + lat + "," + lon + "&sensor=false");
					connection = (HttpURLConnection)serverAddress.openConnection();
					connection.setRequestMethod("GET");
					connection.setDoOutput(true);
					connection.setReadTimeout(10000);
					connection.connect();
					connResponse = connection.getResponseCode();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (connResponse == HttpURLConnection.HTTP_OK) {
					// Read the server's response
					String info = "";
					try {
						DataInputStream inStream = null;
						inStream = new DataInputStream ( connection.getInputStream() );
						String str;
						while (( str = inStream.readLine()) != null)
						{ info = info + str + "\n"; }
						inStream.close();
					}
					catch (IOException e) {
					}
					try {
						JSONObject jsonObject = new JSONObject(info);
						if (jsonObject.has("results")) {
							JSONArray jarray = jsonObject.getJSONArray("results");
							String gapi_address;
							for(int i = 0 ; i < jarray.length() ; i++) {
								if (jarray.getJSONObject(i).has("formatted_address")) {
									gapi_address = jarray.getJSONObject(i).getString("formatted_address");
									String[] fields = gapi_address.split(", ", 2);
									if (fields.length == 2) {
										return_address = fields[0]+"\n"+fields[1];
									}
									recent_geocoder.put(hashKey, return_address);
									break;
								}
							}
						}
					} catch (Exception e) {
					}
				}
			}    			
			else {  // If we're not using the Android Emulator, then we can do a real geocoder lookup
				try {
					List<Address> addresses = geocoder.getFromLocation(dlat, dlon, 1);
					if  (addresses != null) {
						return_address = "";
						Address returnedAddress = addresses.get(0);
						for(int i=0; i<returnedAddress.getMaxAddressLineIndex(); i++) {
							return_address = return_address+ "\n" + returnedAddress.getAddressLine(i);
						}    		    	
					}
					else {
						return_address = "No Address Found";
					}    		      
					recent_geocoder.put(hashKey, return_address);
				} catch (IOException e) {
					return_address = "IO Error - No Address Found";
					retval[0] = "0";
				}
			}
		}
		retval[1] = return_address;
		return retval;
	}	
}
