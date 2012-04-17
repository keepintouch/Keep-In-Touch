package app.master.kit;

import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import android.util.Log;


/**
 * encrypt and decrypt strings use DES.
 **/
public class DESProvider {
	Cipher ecipher;  // encryption cipher
	Cipher dcipher; //decryption cipher
	byte[] salt = { (byte)0xA9, (byte)0x9B, (byte)0xC8, (byte)0x32, (byte)0x56, (byte)0x35, (byte)0xE3, (byte)0x03 };
	int iterationCount = 10;  // iteration count

	/**
	 * Initializes a new instance of the DESEncryptionProvider.
	 * 
	 * @param passPhrase
	 *            The passphrase to protect the data with.
	 */
	public DESProvider(String passPhrase) {
		try {
			// Create the key
			KeySpec keySpec = new PBEKeySpec(passPhrase.toCharArray(), salt,
					iterationCount);
			SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES")
					.generateSecret(keySpec);
			ecipher = Cipher.getInstance(key.getAlgorithm());
			dcipher = Cipher.getInstance(key.getAlgorithm());

			// Create the ciphers
			ecipher.init(Cipher.ENCRYPT_MODE, key);
			dcipher.init(Cipher.DECRYPT_MODE, key);
		} catch (Exception e) {
			Log.e("KIT", "Failed to created DES encryptor.", e);
		}
	}

	/**
	 * Performs DES encryption on a string.
	 * 
	 * @param data
	 *            The string to encrypt.
	 * @return The encrypted string.
	 */
	public String encryptAsBase64(String str) {
		try {
			// Encode the string into bytes using utf-8
			byte[] utf8 = str.getBytes("UTF8");

			// Encrypt
			byte[] enc = ecipher.doFinal(utf8);

			// Encode bytes to base64 to get a string
			return Base64.encodeBytes(enc);
		} catch (Exception e) {
			Log.e("KIT", "Failed to created DES encryptor.", e);
			return new String("");
		}
	}

	/**
	 * Performs DES decryption on a string.
	 * 
	 * @param data
	 *            The string to decrypt.
	 * @return The decrypted string.
	 */
	public String decryptAsBase64(String str) {
		try {
			// Decode base64 to get bytes
			byte[] dec = Base64.decode(str);

			// Decrypt
			byte[] utf8 = dcipher.doFinal(dec);

			// Decode using utf-8
			return new String(utf8, "UTF8");
		} catch (Exception e) {
			Log.e("KIT", "Failed to created DES encryptor.", e);
			return new String("Decryption Failed");
		}
	}
}