package app.master.kit;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

// NOTE: This module has one flaw: It strips all white space from the
// beginning and end of any data to encrypt.  This is because of how
// the data is padded, and if we didn't do this, the decrypted data
// would have a number of trailing white space characters at the end
// of it.  So, the only time this would be a problem is if someone
// decides to send a string like " This is my passphrase " (the beginning
// and ending spaces would be removed before it was encrypted).

public class RSAProvider {
	private String cipherAlgorithm;  // cipher algorithm
	private RSAPublicKeySpec pub = null;
	private RSAPrivateKeySpec priv = null;

	public RSAProvider() {
		// Set up the cipher
		this.cipherAlgorithm = "RSA";
	}

	public boolean GenerateKeyPair() {
		// Generate Public/Private Key Pair
		this.pub = null;
		this.priv = null;
		boolean keys_okay = true;
		KeyPairGenerator kpg;
		KeyPair kp = null;
		try {
			kpg = KeyPairGenerator.getInstance(this.cipherAlgorithm);
			kpg.initialize(1024);  // Could also be 2048, but will be slower
			kp = kpg.genKeyPair();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			keys_okay = false;
			e.printStackTrace();
		}
		if (keys_okay) {
			KeyFactory fact = null;
			try {
				fact = KeyFactory.getInstance(this.cipherAlgorithm);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				keys_okay = false;
				e.printStackTrace();
			}
			if (keys_okay) {
				try {
					this.pub = fact.getKeySpec(kp.getPublic(), RSAPublicKeySpec.class);
					this.priv = fact.getKeySpec(kp.getPrivate(), RSAPrivateKeySpec.class);
				} catch (InvalidKeySpecException e) {
					// TODO Auto-generated catch block
					keys_okay = false;
					e.printStackTrace();
				}
			}
		}
		return keys_okay;
	}

	public String PublicModulus() {
		return this.pub.getModulus().toString();
	}

	public String PublicExponent() {
		return this.pub.getPublicExponent().toString();
	}

	public String PrivateModulus() {
		return this.priv.getModulus().toString();
	}

	public String PrivateExponent() {
		return this.priv.getPrivateExponent().toString();
	}

	public boolean LoadPublicPrivate(String PublicModulus, String PublicExponent, String PrivateModulus, String PrivateExponent) {
		this.pub = null;
		this.priv = null;
		BigInteger m;
		BigInteger e;

		// Load our Public Key
		try {
			m = new BigInteger(PublicModulus);
			e = new BigInteger(PublicExponent);
			pub = new RSAPublicKeySpec(m, e);

        	if (PrivateModulus != null && PrivateExponent != null)
        	{
        		m = new BigInteger(PrivateModulus);
        		e = new BigInteger(PrivateExponent);
        		priv = new RSAPrivateKeySpec(m, e);
        	}
			return true;
		}
		catch (NumberFormatException err){
			return false;
		}
	}

	private byte[] append(byte[] prefix, byte[] suffix){
		byte[] toReturn = new byte[prefix.length + suffix.length];
		for (int i=0; i< prefix.length; i++) {
			toReturn[i] = prefix[i];
		}
		for (int i=0; i< suffix.length; i++) {
			toReturn[i+prefix.length] = suffix[i];
		}
		return toReturn;
	}

	public String encrypt(String data) {
		String the_string = null;
		if (this.pub != null) {        
			KeyFactory fact = null;
			PublicKey pubKey = null;            
			try {
				fact = KeyFactory.getInstance(this.cipherAlgorithm);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (fact != null) {
				try {
					pubKey = fact.generatePublic(this.pub);
				} catch (InvalidKeySpecException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (pubKey != null) {
					byte[] encryptedData = new byte[0]; // holds all of the encrypted data
					byte[] clearData = data.trim().getBytes(); // our plain text data as a byte array
					Cipher cipher = null;
					try {
						cipher = Cipher.getInstance(this.cipherAlgorithm);
					} catch (NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NoSuchPaddingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (cipher != null) {
						try {
							cipher.init(Cipher.ENCRYPT_MODE, pubKey);
							int length = 100; // when encrypting use 100 byte long blocks
							byte[] buffer = new byte[length];
							byte[] encryptedBlock = new byte[0];
							for (int i=0; i<clearData.length; i++) {
								if ((i > 0) && (i % length == 0)){
									//execute the operation
									encryptedBlock = cipher.doFinal(buffer);
									// add the result to our total result.
									encryptedData = append(encryptedData,encryptedBlock);
									// here we calculate the length of the next buffer required
									int newlength = length;
									// if newlength would be longer than remaining bytes in the bytes array we shorten it.
									if (i + length > clearData.length) {
										newlength = clearData.length - i;
									}
									// clean the buffer array
									buffer = new byte[newlength];
								}
								// copy byte into our buffer.
								buffer[i%length] = clearData[i];
							}
							// This last piece here catches any remaining buffer
							// Example we encrypt 230 bytes, the loop about will catch 200 bytes, but
							// we have 30 bytes left we need to encrypt
							encryptedBlock = cipher.doFinal(buffer);
							encryptedData = append(encryptedData,encryptedBlock);
							the_string = Base64.encodeBytes(encryptedData);						
						} catch (InvalidKeyException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (BadPaddingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalBlockSizeException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}					
					}
				}
			}
		}
		return the_string;
	}

	public String decrypt(String data) {
		String the_string = null;
		if (this.priv != null) {            	
			KeyFactory fact = null;
			PrivateKey privKey = null;
			try {
				fact = KeyFactory.getInstance(this.cipherAlgorithm);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (fact != null) {
				try {
					privKey = fact.generatePrivate(this.priv);
				} catch (InvalidKeySpecException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (privKey != null) {
					byte[] decryptedData = new byte[0];
					byte[] cipherData = { 0x00 };
					try {
						cipherData = Base64.decode(data.getBytes());
					} catch (Exception e) {        	        
					}
					if (cipherData.length > 1) {
						Cipher cipher = null;
						try {
							cipher = Cipher.getInstance(this.cipherAlgorithm);
						} catch (NoSuchAlgorithmException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (NoSuchPaddingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						if (cipher != null) {
							try {
								cipher.init(Cipher.DECRYPT_MODE, privKey);
								int length = 128; // when decrypting use 128 byte long blocks
								byte[] buffer = new byte[length];
								byte[] decryptedBlock = new byte[0];
								for (int i=0; i<cipherData.length; i++) {
									if ((i > 0) && (i % length == 0)){
										//execute the operation
										decryptedBlock = cipher.doFinal(buffer);
										// add the result to our total result.
										decryptedData = append(decryptedData,decryptedBlock);
										// here we calculate the length of the next buffer required
										int newlength = length;
										// if newlength would be longer than remaining bytes in the bytes array we shorten it.
										if (i + length > cipherData.length) {
											newlength = cipherData.length - i;
										}
										// clean the buffer array
										buffer = new byte[newlength];
									}
									// copy byte into our buffer.
									buffer[i%length] = cipherData[i];
								}
								// This last piece here catches any remaining buffer
								// Example we decrypt 230 bytes, the loop about will catch 200 bytes, but
								// we have 30 bytes left we need to decrypt
								decryptedBlock = cipher.doFinal(buffer);
								decryptedData = append(decryptedData,decryptedBlock);
								the_string = new String(decryptedData).trim();					
							} catch (InvalidKeyException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (BadPaddingException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IllegalBlockSizeException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}					
						}
					}
				}
			}
		}
		return the_string;
	}

}
