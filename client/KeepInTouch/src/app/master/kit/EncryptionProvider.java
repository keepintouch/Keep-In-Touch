package app.master.kit;

public class EncryptionProvider {
	private String[] encryption_providers;
	private String password;

	public EncryptionProvider() {
		// Constructor
		encryption_providers = new String[3];
		encryption_providers[0] = "none";
		encryption_providers[1] = "aes";
		encryption_providers[2] = "des";
		password = "";
	}

	// Return a list of Encryption Providers
	public String[] getProviders() {
		return encryption_providers;
	}

	// Set Password
	public void setPassword(String pass) {
		password = pass;
	}

	// Encrypt data using provider
	public String encrypt(String provider, Object data) {
		String theresult = "";
		if (provider.equals("none")) {
			theresult = (String) data;
		} else if (provider.equals("aes")) {
			AESProvider mycrypt = null;
			mycrypt = new AESProvider(password);
			theresult = mycrypt.encryptAsBase64((String) data);
		} else if (provider.equals("des")) {
			DESProvider mycrypt = null;
			mycrypt = new DESProvider(password);
			theresult = mycrypt.encryptAsBase64((String) data);			
		}
		return theresult;
	}

	// Decrypt data using provider
	public String decrypt(String provider, Object data) {
		String theresult = "";
		if (provider.equals("aes")) {
			AESProvider mycrypt = null;
			mycrypt = new AESProvider(password);
			theresult = mycrypt.decryptAsBase64((String) data);
		} else if (provider.equals("des")) {
			DESProvider mycrypt = null;
			mycrypt = new DESProvider(password);
			theresult = mycrypt.decryptAsBase64((String) data);			
		}
		return theresult;
	}

}
