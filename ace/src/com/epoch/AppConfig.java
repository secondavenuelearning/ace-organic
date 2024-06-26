package com.epoch;

import chemaxon.standardizer.Standardizer;

/** Configuration parameters used by ACE.
	Scope: created by the UI layer, passed on to all lower layers. 
	Sessions: All.
	Note: Connection Pool can be embedded in this.
	Most values set upon login by Utils.java from configuration file 
	stored in WEB-INF.
*/
public final class AppConfig {

	/** Will remain constant as long as Oracle's thin driver is used.  Used in
	 * db/QSetRW.  Value also found in $CATALINA_BASE/conf/context.xml.  */ 
	public static final String jdbc_driverclass = "oracle.jdbc.driver.OracleDriver";
	
	/** Root of file directory structure of this application.  Needed for
	 * writing of tempfiles, such as pictures of molecules and imported files.
	 * Used in Question.java, db/QuestionWrite.java,
	 * db/QuestionRead.java, and session/QSet.java.  Also used in
	 * db/HWRead.java but just in a call to QuestionRead.  Set eventually by
	 * courseware/servlets/LoginServlet.java.  */ 
	public static String appRoot;
	/** All apps root, what we call $BASE. */
	public static String allAppsRoot;
	/** Directory underneath the application root (relative path) that 
	 * holds master-question figures. Locally authored question
	 * figures are held in user_relFiguresDir.  */
	public static String relFiguresDir; 
	/** Relative address of temp directory for import/export and for temporary
	 * storage of images.  */
	public static String relTempDir; 
	/** Directory where responses are logged.  Used in db/ResponseLogger
	 * only.  */
	public static String responseLogDir; 

	/** Maxima (mathematics) package. */
	public static String maximaProgram;
	/** Mutt (used to send email or text messages). */
	public static String muttProgram;
	/** Node (used to start Marvin Live) package. */
	public static String nodeProgram;

	/** License for MarvinJS. */
	public static String marvinJSLicense;
	/** Standardizer for normalizing molecules such as ylides. */
	public static Standardizer standardizer;
	/** Standardizer for normalizing allyl- and dienyl-metal complexes. */
	public static Standardizer allylDienylStdizer;
	
	/** Email address of Webmaster. */
	public static String webmasterEmail;
	/** ID of course containing the tutorials and system notices. */
	public static int tutorialId;
	/** Lifetime (in weeks) of an exam login. */
	public static final int EXAM_STUDENT_LIFE_WKS = 2;

	/** Name of instructor verifier. */
	public static String verifierName;
	/** Email address of instructor verifier. */
	public static String verifierEmail;
	/** Institution whose name should be selected in the registration pulldown
	 * menu. */
	public static String defaultInstitution;
	/** Name of the systemwide username/login ID of the default institution, 
	 * e.g., linkBlue. */
	public static String defaultUsernameLabel;
	/** Email domain of the default institution, e.g., uky.edu. */
	public static String defaultDomain;
	/** URLs needed to use LDAP at the default institution, comma-separated if
	 * more than one, e.g., ldap://ad.uky.edu,ldap://mc.uky.edu. */
	public static String defaultInstitutionLdapUrls;
	/** Default language of the institution. */
	public static String defaultLanguage;
	/** Whether the default language is English. */
	public static boolean notEnglish;

	/** Disables external instantiation. */
	private AppConfig() { }

} // AppConfig
