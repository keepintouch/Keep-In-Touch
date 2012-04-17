package app.master.kit;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.util.Log;

/**
 * encrypt and decrypt strings use AES.
 */
public class AESProvider {
	private Cipher cipher;
	private SecretKey secretKey;  // Secret key used for encryption/decryption
	private IvParameterSpec ivParameterSpec;  // iv parameter spec.
	private String cipherTransformation;  // cipher transformation
	private String cipherAlgorithm;  // cipher algorithm
	private String messageDigestAlgorithm; // message digest algorithm
	/* will be replaced with 16 byte key (128 bit) */
	private static byte[] rawSecretKey = { 0x00, 0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

	public byte[] encodeDigest(String text) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance(messageDigestAlgorithm);
			return digest.digest(text.getBytes());
		} catch (NoSuchAlgorithmException e) {
			Log.e("KIT", "No such algorithm "
					+ messageDigestAlgorithm, e);
		}

		return null;
	}
	/**
	 * Initializes an instance of the AESEncryptionProvider.
	 * 
	 * @param passphrase
	 *            The phassphrase to protect the data with.
	 */
	public AESProvider(String passphrase) {
		// Set up the cipher
		this.cipherTransformation = "AES/CBC/PKCS5Padding";
		this.cipherAlgorithm = "AES";
		this.messageDigestAlgorithm = "MD5";

		// Create the password byte array
		byte[] passwordKey = encodeDigest(passphrase);

		// Set up the algorithm
		try {
			this.cipher = Cipher.getInstance(this.cipherTransformation);
		} catch (NoSuchAlgorithmException e) {
			Log.e("KIT", "No such algorithm "
					+ this.cipherAlgorithm, e);
		} catch (NoSuchPaddingException e) {
			Log.e("KIT", "No such padding PKCS5", e);
		}

		// Finish setting up the encryption by making the secret key and iv
		// parameters
		secretKey = new SecretKeySpec(passwordKey, this.cipherAlgorithm);
		ivParameterSpec = new IvParameterSpec(rawSecretKey);
	}

	/**
	 * Performs AES encryption on a string.
	 * 
	 * @param data
	 *            The string to encrypt.
	 * @return The encrypted string.
	 */
	public String encryptAsBase64(String data) {
		byte[] clearData = data.getBytes();
		try {
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
		} catch (InvalidKeyException e) {
			Log.e("KIT", "Invalid key", e);
		} catch (InvalidAlgorithmParameterException e) {
			Log.e("KIT", "Invalid algorithm "+ cipherAlgorithm, e);
		}

		byte[] encryptedData = { 0x00 };
		try {
			encryptedData = cipher.doFinal(clearData);
		} catch (IllegalBlockSizeException e) {
			Log.e("KIT", "Illegal block size", e);
		} catch (BadPaddingException e) {
			Log.e("KIT", "Bad padding", e);
		}
		return Base64.encodeBytes(encryptedData);
	}

	/**
	 * Performs DES decryption on a string.
	 * 
	 * @param data
	 *            The string to decrypt.
	 * @return The decrypted string.
	 */
	public String decryptAsBase64(String data) {
		try {
			byte[] cipherData = Base64.decode(data.getBytes());
			try {
				cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
			} catch (InvalidKeyException e) {
				Log.e("KIT", "Invalid key", e);
			} catch (InvalidAlgorithmParameterException e) {
				Log.e("KIT", "Invalid algorithm "+ cipherAlgorithm, e);
			}

			byte[] decryptedData = {0x00};
			try {
				decryptedData = cipher.doFinal(cipherData);
			} catch (IllegalBlockSizeException e) {
				Log.e("KIT", "Illegal block size", e);
			} catch (javax.crypto.BadPaddingException e) {
				Log.i("KIT", "Bad padding error - AES decryption failed");
			} catch (Exception e) {
				// pass
			}

			if (decryptedData.length == 1)
			{ return new String("Decryption Failed"); }
			else
			{ return new String(decryptedData); }
		} catch (Exception e) {
			Log.e("KIT", "Decryption Failed", e);
			return new String("Decryption Failed with an Exception!");
		}
	}

}