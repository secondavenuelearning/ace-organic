package com.epoch.servlet;

import com.epoch.AppConfig;
import com.epoch.utils.Utils;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.directory.InitialDirContext;
import javax.naming.NamingException;

/** Utility class to authenticate a username/password combination from the
 * university directory.  ACE queries the university directory only after 
 * it looks for a correct password in the ACE database.
 * At UK, the command-line equivalent is:
 * <blockquote>ldapsearch -z 1 -h ad.uky.edu -p 3268 -D USERNAME@ad.uky.edu 
 * -w LINKBLUEPASS foo
 * </blockquote>or
 * <blockquote>ldapsearch -z 1 -h mc.uky.edu -p 3268 -D USERNAME@mc.uky.edu 
 * -w LINKBLUEPASS foo
 * </blockquote>
 */
public final class Ldap {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Authenticates the user. Modified from 
	 * https://docs.oracle.com/javase/tutorial/jndi/ldap/digest.html.
	 * @param	username	the user's campus login ID
	 * @param	password	the user's campus password, as a string
	 * @return	true if the user/password combination is validated
	 */
	public static boolean authenticate(final String username, 
			final String password) {
		final String SELF = "Ldap.authenticate: ";
		// Set up the environment for creating the initial context
		final Hashtable<String, Object> env = new Hashtable<String, Object>();
		env.put(Context.INITIAL_CONTEXT_FACTORY,
				"com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.SECURITY_CREDENTIALS, password);
		final String[] urls = AppConfig.defaultInstitutionLdapUrls.split(",");
		boolean validated = false;
		for (final String url: urls) {
			env.put(Context.PROVIDER_URL, url.trim());
			final String domain = url.split("//")[1].trim();
			final String modUsername = Utils.toString(username, '@', domain);
			env.put(Context.SECURITY_PRINCIPAL, modUsername);
			try {
				// Creating initial context will throw exception if pwd wrong
				new InitialDirContext(env);
				validated = true;
				debugPrint(SELF + "login of ", modUsername, 
						" succeeded at URL ", url);
				break;
			} catch (NamingException e) {
				debugPrint(SELF + "login of ", modUsername, 
						" failed at URL ", url);
			} // try
		} // for each LDAP URL
		return validated;
	} // authenticate(String, String)

	/** Command-line interface.
	 * @param	args	dummy variable
	 */
	public static void main(String[] args) {
		final String username = "myUserName";
		final String password = "myPassword";
		final boolean result = authenticate(username, password);
		System.out.printf("Result of authentication: " + result + "\n");
	} // main(String[])

	private Ldap() {
		// not instantiable
	}

} // Ldap
