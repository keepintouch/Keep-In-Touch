package app.master.kit;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class MasterURLPoster {
	private String theURL;
	private String theFilesVariable;
	private String[][] theFiles;
	private String[][] theVariables;
	private int BlindlyTrustSSL;

	//********************************************************
	// MasterURLPoster:
	//   x_theURL: URL that you will be POSTing data to
	//   x_theFilesVariable: Name of the variable to upload files as, like "upload_file"
	//   x_theFiles: Array like { {"my.pdf". "/location/to/my.pdf"}, {"1.pdf", "/location/to/1.pdf"} }
	//   x_theVariables: Array lie { {"variable1_name", "variable1_value"}, {"variable2_name", "variable2_value"} }
	//   x_BlindlyTrustSSL: 1 = Always trust SSL cert, 0 = Only trust validated SSL certs
	//********************************************************
	public MasterURLPoster(String x_theURL, String x_theFilesVariable, String[][] x_theFiles, String[][] x_theVariables, int x_BlindlyTrustSSL) {
		theURL = x_theURL;
		theFilesVariable = x_theFilesVariable;
		theFiles = x_theFiles;
		theVariables = x_theVariables;
		BlindlyTrustSSL = x_BlindlyTrustSSL;
	}

	public String[] Go() {
		String[] retval = {"1", ""};

		// First let's check and make sure all of the upload files exist!
		for (String[] theFile : theFiles) {
			File f = new File(theFile[1]);
			if (!f.exists()) {
				retval[0] = "0";
				retval[1] = "ERROR: File '"+theFile[1]+"' doesn't exist!\n";
				return retval;
			}
		}

		// If everything's okay, then let's go
		URL url = null;		
		HttpURLConnection conn = null;
		DataOutputStream dos = null;
		DataInputStream inStream = null;

		String lineEnd = "\r\n";
		String twoHyphens = "--";
		String boundary =  "*****";
		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		int maxBufferSize = 1*1024*1024;
		int connResponse = -1;
		String connResponseMessage = "";

		String urlString = theURL;
		try {
			url = new URL(urlString);
		}
		catch (MalformedURLException e) { 
			retval[0] = "0";
			retval[1] = "URL ERROR: "+e.getMessage();
			return retval;
		}

		// If URL is https, and we're going to blindly trust the SSL cert
		if (urlString.indexOf("https:") == 0 && BlindlyTrustSSL == 1) {
			try {
				SSLContext sc = SSLContext.getInstance("TLS");
				sc.init(null, new TrustManager[] { new MyTrustManager() }, new SecureRandom());
				HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
				HttpsURLConnection.setDefaultHostnameVerifier(new MyHostnameVerifier());
			}
			catch (Exception e) {
				retval[0] = "0";
				retval[1] = "BLINDLY TRUST SSL ERROR: "+e.toString();
				return retval;
			} 
		}

		// Let's try opening the HTTP connection to the URL
		try {
			conn = (HttpURLConnection) url.openConnection();
		}
		catch (IOException e) {
			retval[0] = "0";
			retval[1] = "URL OPEN ERROR: "+e.getMessage();
			return retval;
		}

		// Let's setup our server connection
		// Allow Inputs
		conn.setDoInput(true);
		// Allow Outputs
		conn.setDoOutput(true);
		// Don't use a cached copy.
		conn.setUseCaches(false);
		// Set our timeout to 30 seconds
		conn.setConnectTimeout(30000);
		// Use a post method.
		try {
			conn.setRequestMethod("POST");
		}
		catch (ProtocolException e) {
			retval[0] = "0";
			retval[1] = "POST METHOD ERROR: "+e.getMessage();
			return retval;
		}
		conn.setRequestProperty("Connection", "Keep-Alive");
		conn.setRequestProperty("Content-Type", "multipart/form-data; boundary="+boundary);

		// Now it's time to open our data stream to the server
		try {
			dos = new DataOutputStream( conn.getOutputStream() );
		}
		catch (IOException e) {
			retval[0] = "0";
			retval[1] = "CONNECTION OUTPUT ERROR: "+e.getMessage();
			return retval;
		}

		// Now let's write to our data stream that is open to the server 
		try {
			// First POST all of the FILES
			for (String[] theFile : theFiles) {
				File f = new File(theFile[1]);
				dos.writeBytes(twoHyphens + boundary + lineEnd);
				dos.writeBytes("Content-Disposition: form-data; name=\""+URLEncoder.encode(theFilesVariable,"UTF-8")+"\";filename=\"" + URLEncoder.encode(theFile[0], "UTF-8") +"\"" + lineEnd);
				dos.writeBytes("Content-Type: application/octet-stream" + lineEnd);
				dos.writeBytes(lineEnd);
				// Open File Stream
				FileInputStream fileInputStream = new FileInputStream(f);			
				// create a buffer of maximum size
				bytesAvailable = fileInputStream.available();
				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				buffer = new byte[bufferSize];
				// read file and write it into form...
				bytesRead = fileInputStream.read(buffer, 0, bufferSize);
				while (bytesRead > 0) {
					dos.write(buffer, 0, bufferSize);
					bytesAvailable = fileInputStream.available();
					bufferSize = Math.min(bytesAvailable, maxBufferSize);
					bytesRead = fileInputStream.read(buffer, 0, bufferSize);
				}
				dos.writeBytes(lineEnd);
				fileInputStream.close();
			}
			// Next POST all of the VARIABLES
			for (String[] theVariable : theVariables) {
				dos.writeBytes(twoHyphens + boundary + lineEnd);
				dos.writeBytes("Content-Disposition: form-data; name=\""+URLEncoder.encode(theVariable[0], "UTF-8")+"\"" + lineEnd);
				dos.writeBytes(lineEnd);    	   
				dos.writeBytes(URLEncoder.encode(theVariable[1], "UTF-8"));
				dos.writeBytes(lineEnd);
			}
			// End the multipart form data
			dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

			// close streams
			dos.flush();
			dos.close();
		}
		catch (IOException e) {
			retval[0] = "0";
			retval[1] = "CONNECTION WRITE ERROR: "+e.getMessage();
			return retval;
		}

		// Let's get a response back, if we error here, we can't connect
		// to the server (most likely error 404)
		try {
			connResponse = conn.getResponseCode();
			connResponseMessage = conn.getResponseMessage();
		}
		catch (IOException e) {
			retval[0] = "0";
			retval[1] = "URL RESPONSE ERROR: "+e.getMessage();
			return retval;
		}

		if (connResponse == HttpURLConnection.HTTP_OK) {
			// Read the server's response
			try {
				inStream = new DataInputStream ( conn.getInputStream() );
				String str;
				while (( str = inStream.readLine()) != null)
				{ retval[1] = retval[1] + str + "\n"; }
				inStream.close();
			}
			catch (IOException e) {
				retval[0] = "0";
				retval[1] = "SERVER RESPONSE ERROR: "+e.getMessage();
				return retval;
			}
		}
		else {
			retval[0] = "0";
			retval[1] = "HTTP ERROR, SERVER RETURNED: "+Integer.toString(connResponse)+" - "+connResponseMessage;
			return retval;
		}

		// Let's return
		return retval;
	}

	//********************************************************
	// MyHostnameVerifier: Used to blindly trust SSL certs
	//********************************************************
	private class MyHostnameVerifier implements HostnameVerifier {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	}

	//********************************************************
	// MyTrustManager: Used to blindly trust SSL certs
	//********************************************************
	private class MyTrustManager implements X509TrustManager {
		public void checkClientTrusted(X509Certificate[] chain, String authType)
		{ }
		public void checkServerTrusted(X509Certificate[] chain, String authType)
		{ }
		public X509Certificate[] getAcceptedIssuers()
		{ return null; }
	} 
}
