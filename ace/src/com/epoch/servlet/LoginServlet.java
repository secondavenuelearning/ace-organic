package com.epoch.servlet;

import com.epoch.AppConfig;
import com.epoch.courseware.User;
import com.epoch.courseware.courseConstants.UserConstants;
import com.epoch.db.EnrollmentRW;
import com.epoch.db.UserRead;
import com.epoch.session.AdminSession;
import com.epoch.session.InstructorSession;
import com.epoch.session.StudentSession;
import com.epoch.exceptions.DBException;
import com.epoch.utils.MathUtils;
import com.epoch.utils.AuthUtils;
import com.epoch.utils.Utils;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;

/** Methods to log a user into ACE. */
public class LoginServlet extends HttpServlet implements UserConstants {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Name of session variable that stores the user session. */
	private static final String USERSESSION = "usersession";
	/** Prefix to URLs. */
	// private static final String HTTP = "http://";
	/** Parameter for getSession(). */
	private static boolean CREATE = true;
	/** Required by interface. */
	public final static long serialVersionUID = 1L;
	/** Error messages to be sent to front end. */
	public final static String[] ERR_MSGS = new String[] {
			"No command received by the login servlet.",
			"Invalid login. Have you registered?",
			"Invalid password."};
	/** Index of error message. */
	private final static int NO_CMD = 0;
	/** Index of error message. */
	private final static int INVALID_USERNAME = 1;
	/** Index of error message. */
	private final static int INVALID_PASSWORD = 2;

	/** Logs the user in.
	 * @param	request	the request
	 * @param	response	the response
	 */
	public void doGet(HttpServletRequest request,
			HttpServletResponse response)
			throws IOException, ServletException {
		// TODO: get login must be avoided !!!
		process(request, response);
	} // doGet(HttpServletRequest, HttpServletResponse)

	/** Logs the user in.
	 * @param	request	the request
	 * @param	response	the response
	 */
	public void doPost(HttpServletRequest request,
			HttpServletResponse response)
			throws IOException, ServletException {
		process(request, response);
	} // doPost(HttpServletRequest, HttpServletResponse)

	/** Sets values upon application startup. */
	public void init() throws ServletException {
		// find the web-application root, get file containing the
		// AppConfig constants
		final ServletContext context = getServletContext();
		String appRoot = context.getRealPath("");
		if (!appRoot.endsWith("/")) appRoot = Utils.toString(appRoot, '/');
		AppConfig.appRoot = appRoot;
		final String confFile = appRoot
				+ context.getInitParameter("chimp_conf_file");
		initConfiguration(confFile);
	} // init()

	/** Reads and stores systemwide constants from configuration files.
	 * @param	confFile	name of configuration file (or
	 * names of files containing the values)
	 */
	public static void initConfiguration(String confFile) {
		try {
			final FileInputStream confFileStream = new FileInputStream(confFile);
			final Properties configValues = new Properties();
			try {
				configValues.load(confFileStream);
				AppConfig.verifierName =
						configValues.getProperty("courseware.verifier.name");
				AppConfig.verifierEmail =
						configValues.getProperty("courseware.verifier.email");
				AppConfig.defaultInstitution =
						configValues.getProperty("default.institution");
				AppConfig.defaultUsernameLabel =
						configValues.getProperty("default.username_label");
				AppConfig.defaultDomain =
						configValues.getProperty("default.domain");
				AppConfig.defaultInstitutionLdapUrls =
						configValues.getProperty("default.ldap_provider_urls");
				AppConfig.webmasterEmail =
						configValues.getProperty("courseware.admin.email");
				AppConfig.tutorialId = MathUtils.parseInt(
						configValues.getProperty(
							"courseware.tutorial.course_id"));
				AppConfig.responseLogDir =
						configValues.getProperty("epoch.responselog.dir");
				setDefaultLanguage(configValues.getProperty("default.language"));
				debugPrint("LoginServlet.init: "
						+ " tutorialId = ", AppConfig.tutorialId,
						", webmasterEmail = ", AppConfig.webmasterEmail,
						", defaultInstitution = ", AppConfig.defaultInstitution,
						", defaultUsernameLabel = ", 
						AppConfig.defaultUsernameLabel,
						", defaultDomain = ", AppConfig.defaultDomain,
						", defaultInstitutionLdapUrls = ", 
						AppConfig.defaultInstitutionLdapUrls,
						"");
			} finally {
				confFileStream.close();
			}
		} catch (IOException e1) {
			Utils.alwaysPrint(" **** error in loading toDisplay() rules ****");
			e1.printStackTrace();
		}
	} // initConfiguration(String)

	/** Sets the default language.
	 * @param	language	the default language
	 */
	public static void setDefaultLanguage(String language) {
		AppConfig.defaultLanguage = language;
		AppConfig.notEnglish = !ENGLISH.equals(language);
	} // setDefaultLanguage(String)

	/** Logs the user in.
	 * @param	request	the request
	 * @param	response	the response
	 * @throws	ServletException	if the servlet doesn't work as expected
	 * @throws	IOException	if the file containing forwarding pages can't be read
	 */
	public void process(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		final String SELF = "LoginServlet.process: ";
		final String cmd = request.getParameter("cmd");
		debugPrint(SELF + "Login servlet cmd: ", cmd);
		String chosenLang = request.getParameter("language");
		if (chosenLang == null && AppConfig.notEnglish) {
			chosenLang = AppConfig.defaultLanguage;
		}
		if (cmd == null) {
			response.sendRedirect(Utils.toString("login.jsp?language=",
					Utils.toValidURI(chosenLang), "&errmsg=", NO_CMD));
			return;
		} // if cmd is null
		final ServletContext context = getServletContext();
		String appRoot = context.getRealPath("");
		if (!appRoot.endsWith("/")) appRoot = Utils.toString(appRoot, '/');

		final String fwdpagesFile = Utils.toString(appRoot, 
				context.getInitParameter("forwardpages"));
		final FileInputStream ipstream = new FileInputStream(fwdpagesFile);
		final Properties fwdPages = new Properties();
		fwdPages.load(ipstream);
		ipstream.close();
		try {
			if ("login".equals(cmd)) {
				String userId = request.getParameter("userid");
				final String userIdToCERs = request.getParameter("useridToCERs");
				debugPrint(SELF + "userId = ", userId, ", userIdToCERs = ",
						userIdToCERs, ", Utils.inputToCERs(userId) = ",
						Utils.inputToCERs(userId));
				User user = UserRead.getUser(userIdToCERs);
				if (user == null) {
					response.sendRedirect(Utils.toString("login.jsp?language=",
							Utils.toValidURI(chosenLang), "&errmsg=", 
							INVALID_USERNAME));
					return;
				} // if no such user
				final char role = user.getRole();
				debugPrint(SELF + "userId = ", userId, ", user role = ", role);
				boolean authenticated = false;
				if (Utils.isEmpty(user.getPasswordHash())) {
					debugPrint(SELF + "logging in with Ldap and "
							+ "unencoded password");
					final String pwd = request.getParameter("pphraseUnencoded");
					authenticated = Ldap.authenticate(userId, pwd);
				} else {
					debugPrint(SELF + "logging in with old method and "
							+ "encoded password");
					/*
						Authentication details:
						 browser			   server
						<------------ nonce
						---------------> h(nonce|h(password))
					*/
					final String nonce = (String)
							request.getSession().getAttribute("nonce");
					debugPrint(SELF + "nonce is ", nonce);
					if (nonce == null) {
						final String loginpage = fwdPages.getProperty("login");
						debugPrint(SELF + "redirecting to login page ", 
								loginpage);
						context.getRequestDispatcher(loginpage).forward(
								request, response);
						/*/ response.sendRedirect(Utils.toString("login.jsp?language=",
								Utils.toValidURI(chosenLang), "&errmsg=", 
								INVALID_PASSWORD)); /**/
						return;
					} // if nonce is null
					final byte[] hashValueArr = decode(Utils.toString(
							request.getParameter("pphraseEncoded")));
					authenticated = AuthUtils.verifyHashValue(
							user.getPasswordHash(), nonce, hashValueArr);
				} // if a password is stored in the ACE database
				if (!authenticated) {
					final String loginpage = fwdPages.getProperty("denylogin");
					debugPrint(SELF + "redirecting to login page ", loginpage);
					context.getRequestDispatcher(loginpage).forward(
							request, response);
					return;
				} // if not the valid password
				
				// user verified
				// invalidate any previous sessions
				// create instructorSession and bind to it
				user.setLoginDateToNow();
				final HttpSession oldsession = request.getSession(!CREATE);
				if (oldsession != null) oldsession.invalidate();
				final HttpSession newsession = request.getSession(CREATE);

				// Load the session of user with all data
				// If an error occurs while loading, user is directed to
				// an error page
				if (role == ADMINISTRATOR) {
					final AdminSession sess = new AdminSession(user);
					newsession.setAttribute(USERSESSION, sess);
				} else if (role == INSTRUCTOR) {
					final InstructorSession sess = new InstructorSession(user);
					newsession.setAttribute(USERSESSION, sess);
				} else if (role == STUDENT) {
					final StudentSession sess = new StudentSession(user);
					newsession.setAttribute(USERSESSION, sess);
					if (!sess.isEnrolled(AppConfig.tutorialId)) {
						try {
							EnrollmentRW.enrollInCourse(userId, 
									AppConfig.tutorialId);
							sess.refreshCourses();
						} catch (Exception e) {
							e.printStackTrace();
						} // try
					} // if not enrolled in tutorial course
				} // if role
				context.removeAttribute(USERSESSION);
				final String forwarder = fwdPages.getProperty("user_home");
				debugPrint(SELF + "redirecting to fwd page ", forwarder);
				context.getRequestDispatcher(forwarder).forward(
						request, response);
			} else if ("logout".equals(cmd)) {
				// invalidate the session
				final HttpSession oldsession = request.getSession(!CREATE);
				if (oldsession != null)  {
					oldsession.removeAttribute(USERSESSION);
					oldsession.invalidate();
				}
				// redirect to login page
				final String loginpage = Utils.toString(
						fwdPages.getProperty("login"), "?language=", 
						Utils.toValidURI(chosenLang));
				debugPrint(SELF + "redirecting to login page ", loginpage);
				context.getRequestDispatcher(loginpage).forward(
						request, response);
			}
		} catch (DBException e1) {
			e1.printStackTrace();
			final String forwarder = fwdPages.getProperty("dberror");
			Utils.alwaysPrint(SELF + "DBError: redirecting to fwd page ", 
					forwarder);
			context.getRequestDispatcher(forwarder).forward(request, response);
		}
	} // process(HttpServletRequest, HttpServletResponse)

	/** Uses Base64 methods to decode the password.
	 * @param	s	the encoded string
	 * @return	the decoded string as an array of bytes
	 */
	private static byte[] decode(String s) {
		return Base64.getDecoder().decode(s);
	} // decode(String)

} // LoginServlet

