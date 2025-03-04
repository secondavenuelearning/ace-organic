package com.epoch.servlet;

import chemaxon.standardizer.Standardizer;
import com.epoch.AppConfig;
import com.epoch.qBank.TextChapters;
import com.epoch.substns.RGroupDefs;
import com.epoch.utils.DisplayRules;
import com.epoch.utils.Utils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

/** A servlet which is automatically loaded by the server on deployment.
		 The init() method loads various static classes with data
		 from various text files.
	<br>Scope: Initiated by the web server (see WEB-INF/web.xml). 
	initStandalone() is also called by the admin tool.
*/
public class GroupLoader extends HttpServlet {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Required by interface. */
	public static final long serialVersionUID = 1L;

	/** Loads definitions that are stored in text files upon program startup.
	 * Called by the web application.  */
	public void init() throws ServletException {
		Utils.alwaysPrint("GroupLoader: Entering init().");
		// find the web application root	
		final ServletContext context = getServletContext();
		String appRoot = context.getRealPath("");
		if (!appRoot.endsWith("/")) appRoot = Utils.toString(appRoot, '/');
		final String confFile = Utils.toString(appRoot,
				context.getInitParameter("chimp_conf_file"));
		initStandalone(appRoot, confFile);
	} // init()

	/** Loads definitions that are stored in text files upon program startup.
	 * Called by init() and the admin tool.
	 * @param	appRoot	application path to root
	 * @param	confFile	configuration file containing the name of the file
	 * containing the functional group definitions
	 */
	public static void initStandalone(String appRoot, String confFile) {
		String myAppRoot = appRoot;
		debugPrint("GroupLoader.initStandalone: chimp_conf_file = ", confFile);
		if (!myAppRoot.endsWith("/")) myAppRoot += "/";
		try {
			final Properties configValues = new Properties();
			FileInputStream confFileStream = null;
			try {
				confFileStream = new FileInputStream(confFile);
				configValues.load(confFileStream);
			} finally {
				if (confFileStream != null) {
					confFileStream.close(); // raphael 6/2006
				}
			}
			loadAppConfig(myAppRoot, configValues);
			final String displayRules =
					configValues.getProperty("displayRules.deffile");
			final String rGroups =
					configValues.getProperty("abbrevgroup.deffile");
			final String defaultEditions =
					configValues.getProperty("defaultEditions.deffile");
			final String textChapNames =
					configValues.getProperty("textChapNames.deffile");
			// need to load toDisplay() rules before functional groups
			loadDisplayModule(displayRules);
			loadRGroupModule(rGroups);
			loadDefaultEditions(defaultEditions);
			loadTextChapNames(textChapNames);
		} catch (IOException e1) {
			Utils.alwaysPrint(" ***** error in loading configuration file *****");
			e1.printStackTrace();
		}
	} // initStandalone(String, String)

	/** Get and store the AppConfig values from the given configuration file.
	 * @param	appRoot	the path root of the application
	 * @param	configValues	properties stored in the configuration file
	 */
	private static void loadAppConfig(String appRoot, Properties configValues) {
		final String SELF = "GroupLoader.loadAppConfig: ";
		String myAppRoot = appRoot;
		if (!myAppRoot.endsWith("/")) {
			myAppRoot = Utils.toString(myAppRoot, '/');
		}
		debugPrint(SELF + "appRoot = ", myAppRoot);
		final String[] pathDescrips = new String[] {
				"epoch.responselog.dir",
				"epoch.tempdir",
				"epoch.figuresdir",
				"epoch.maximaProgram",
				"epoch.nodeProgram",
				"epoch.muttProgram",
				"epoch.allAppsRoot"
				};
		final String[] paths = new String[pathDescrips.length];
		int pathNum = 0;
		for (final String pathDescrip : pathDescrips) {
			paths[pathNum] = configValues.getProperty(pathDescrip);
			if (paths[pathNum] == null) {
				Utils.alwaysPrint(SELF + "Error: no property ", pathDescrip);
				paths[pathNum] = ""; // to allow us to continue a little.
			}
			debugPrint(SELF + "path for ", pathDescrip, " = ", paths[pathNum]);
			pathNum++;
		} // for each path
		AppConfig.appRoot = myAppRoot;
		AppConfig.responseLogDir = paths[0];
		AppConfig.relTempDir = paths[1];
		AppConfig.relFiguresDir = paths[2];
		AppConfig.maximaProgram = paths[3];
		AppConfig.nodeProgram = paths[4];
		AppConfig.muttProgram = paths[5];
		AppConfig.allAppsRoot = paths[6];
		final String[] stdizerDescrips = new String[] {
				"normalizeConfig.deffile",
				"allylDienylConfig.deffile"
				};
		final Standardizer[] stdizers = 
				new Standardizer[stdizerDescrips.length];
		int stdizerNum = 0;
		for (final String stdizerDescrip : stdizerDescrips) {
			final String fileName = configValues.getProperty(stdizerDescrip);
			if (fileName != null) {
				try {
					stdizers[stdizerNum] = 
							new Standardizer(new File(fileName));
					debugPrint(SELF + "got standardizer from file ",
							fileName);
				} catch (IllegalArgumentException e) {
					Utils.alwaysPrint(SELF + "can't find or instantiate "
							+ "Standardizer with ", fileName);
					e.printStackTrace();
				} // try
			} else Utils.alwaysPrint(SELF + "Error: no property for ", 
					stdizerDescrip);
			stdizerNum++;
		} // for each directory
		AppConfig.standardizer = stdizers[0];
		AppConfig.allylDienylStdizer = stdizers[1];
		AppConfig.marvinJSLicense = configValues.getProperty("marvinJSLicense");
		Utils.alwaysPrint(" ***** ", stdizers.length, " structure "
				+ "normalization definitions loaded *********");
	} // loadAppConfig(String, Properties)

	/** Loads from a text file the rules for converting lightly formatted
	 * organic chemistry text to HTML in the order in which the rules should
	 * be applied.
	 * @param	rulesFile	text file containing the rules
	 */
	private static void loadDisplayModule(String rulesFile) {
		final String SELF = "GroupLoader.loadDisplayModule: ";
		BufferedReader rdr = null;
		int ruleCt = 0;
		try {
			// rdr = new BufferedReader(new FileReader(rulesFile));
			rdr = new BufferedReader(
				new InputStreamReader(
					new FileInputStream(rulesFile), StandardCharsets.UTF_8
				)
			);
			// file format is (tab-separated)
			// regEx	replacement
			DisplayRules.reset();
			do {
				final String line = rdr.readLine();
				if (line == null) break;
				if (Utils.isWhitespace(line)) continue;
				if (line.charAt(0) == '*') continue;
				String[] rule = line.split("\t");
				if (rule.length == 1) {
					rule = new String[] {rule[0], ""};
				}
				if (rule.length != 2) {
					Utils.alwaysPrint(SELF + "malformed rule:\n'", line, "'");
				} else try {
					DisplayRules.addRule(rule);
					ruleCt++;
					debugPrint("Stored rule ", ruleCt, ".");
				} catch (Exception e) {
					Utils.alwaysPrint(SELF + "failed to load rule:\n'", line, "'");
					e.printStackTrace();
				}
			} while (true);
		} catch (IOException e1) {
			Utils.alwaysPrint(" ***** error in loading display rules *****");
			e1.printStackTrace();
		} finally {
			try {
				rdr.close();
			} catch (Exception e) {
				debugPrint(SELF + "close failed");
			}
		}
		Utils.alwaysPrint(" ***** ", ruleCt, " toDisplay() rules loaded *****");
	} // loadDisplayModule(String)

	/** Loads from a text file shortcut group names, SMILES definitions, and
	 * attachment points.
	 * @param	deffile	text file containing the shortcut group definitions
	 */
	private static void loadRGroupModule(String deffile) {
		final String SELF = "GroupLoader.loadRGroupModule: ";
		BufferedReader rdr = null;
		final int NAME = 0;
		final int SMILES = 1;
		final int ATTACH = 2;
		final int ALTNAME = 3;
		int grpCt = 0;
		int lineCt = 0;
		try {
			// rdr = new BufferedReader(new FileReader(deffile));
			rdr = new BufferedReader(
				new InputStreamReader(
					new FileInputStream(deffile), StandardCharsets.UTF_8
				)
			);
			// file format is (tab-separated)
			// ShortName	SMILES	AttachPoint
			// ShortName	SMILES	AttachPoint	AttachPoint
			// ShortName	SMILES	AttachPoint	dirName=altName
			RGroupDefs.reset();
			do {
				final String line = rdr.readLine();
				if (line == null) break;
				lineCt++;
				if (Utils.isWhitespace(line)) continue;
				final String[] parts = line.split("\t");
				// .matches("\\d+") is a regex that checks to see if a string
				// contains one or more digits (check if integer).
				if (parts.length == 2 || (parts.length >= 4
						&& parts[3].matches("\\d+"))) continue;
				try {
					debugPrint(SELF + "in line ", lineCt, ", short name = ", 
							parts[NAME], ", smiles = ", parts[SMILES],
							", single attachment point = ", parts[ATTACH]);
					RGroupDefs.addGroup(parts[NAME], parts[SMILES], 
							parts[ATTACH]);
					if (parts.length >= 4
							&& (parts[ALTNAME].startsWith("leftName")
								|| parts[ALTNAME].startsWith("rightName"))) {
						final String altName = parts[ALTNAME].substring(
								parts[ALTNAME].indexOf("=") + 1);
						debugPrint(SELF, parts[NAME], " is also known as ", 
								altName, "; adding to list");
						RGroupDefs.addGroup(altName, parts[SMILES], 
								parts[ATTACH]);
					} // if there's an alternate name
					grpCt++;
				} catch (Exception e) {
					Utils.alwaysPrint(SELF + "failed to load functional group "
							+ "definition: \n'", line, "'");
					e.printStackTrace();
				}
			} while (true);
		} catch (IOException e1) {
			Utils.alwaysPrint(" ***** error in loading shortcut group definitions *****");
			e1.printStackTrace();
		} finally {
			try {
				rdr.close();
			} catch (Exception e) {
				debugPrint(SELF + "close failed");
			}
		}
		Utils.alwaysPrint(" ***** ", grpCt, " shortcut group definitions "
				+ "loaded from ", lineCt, " lines *****");
	} // loadRGroupModule(String)

	/** Loads from a text file the default editions of Pearson organic chemistry
	 * textbooks.
	 * @param	authorEditionsFile	text file containing the textbook
	 * information
	 */
	private static void loadDefaultEditions(String authorEditionsFile) {
		final String SELF = "GroupLoader.loadDefaultEditionsModule: ";
		BufferedReader rdr = null;
		final int AUTH = 0;
		final int EDITION = 1;
		int entryCt = 0;
		try {
			// rdr = new BufferedReader(new FileReader(authorEditionsFile));
			rdr = new BufferedReader(
				new InputStreamReader(
					new FileInputStream(authorEditionsFile),
					StandardCharsets.UTF_8
				)
			);
			// file format is (tab-separated)
			// author	edition
			TextChapters.resetDefaultEditions();
			do {
				final String line = rdr.readLine();
				if (line == null) break;
				if (Utils.isWhitespace(line)) continue;
				if (line.charAt(0) == '*') continue;
				final String[] authorEdition = line.split("\t");
				if (authorEdition.length != 2) {
					Utils.alwaysPrint(SELF + "malformed entry:\n'", line, "'");
				} else try {
					debugPrint(SELF, authorEdition);
					TextChapters.addDefaultEdition(authorEdition[AUTH].trim(),
							authorEdition[EDITION].trim());
					entryCt++;
				} catch (Exception e) {
					Utils.alwaysPrint(SELF + "failed to load entry:\n '", 
							line, "'");
					e.printStackTrace();
				}
			} while (true);
		} catch (IOException e1) {
			Utils.alwaysPrint(" ***** error in loading text editions *****");
			e1.printStackTrace();
		} finally {
			try {
				rdr.close();
			} catch (Exception e) {
				debugPrint(SELF + "close failed");
			}
		}
		Utils.alwaysPrint(" ***** ", entryCt, " textbooks loaded *****");
	} // loadDefaultEditionsModule(String)

	/** Loads from a text file the titles of chapters in various editions of
	 * Pearson oragnic chemistry textbooks.
	 * @param	textChapNamesFile	text file containing the textbook
	 * chapter titles
	 */
	private static void loadTextChapNames(String textChapNamesFile) {
		final String SELF = "GroupLoader.loadTextChapNames: ";
		BufferedReader rdr = null;
		final int AUTH = 0;
		final int EDITION = 1;
		final int CHAPNAME = 2;
		int entryCt = 0;
		try {
			// rdr = new BufferedReader(new FileReader(textChapNamesFile));
			rdr = new BufferedReader(
				new InputStreamReader(
					new FileInputStream(textChapNamesFile),
					StandardCharsets.UTF_8
				)
			);
			// file format is (tab-separated)
			// author	edition	chapter_name
			TextChapters.resetTextChapters();
			do {
				final String line = rdr.readLine();
				if (line == null) break;
				if (Utils.isWhitespace(line)) continue;
				if (line.charAt(0) == '*') continue;
				final String[] authEdnName = line.split("\t");
				if (authEdnName.length != 3) {
					Utils.alwaysPrint(SELF + "malformed entry:\n'", line, "'");
				} else try {
					debugPrint(SELF, authEdnName);
					TextChapters.addChapName(
							authEdnName[AUTH].trim() + authEdnName[EDITION].trim(),
							authEdnName[CHAPNAME].trim());
					entryCt++;
				} catch (Exception e) {
					Utils.alwaysPrint(SELF + "failed to load entry:\n '", 
							line, "'");
					e.printStackTrace();
				}
			} while (true);
		} catch (IOException e1) {
			Utils.alwaysPrint(" ***** error in loading text chapter names *****");
			e1.printStackTrace();
		} finally {
			try {
				rdr.close();
			} catch (Exception e) {
				debugPrint(SELF + "close failed");
			}
		}
		Utils.alwaysPrint(" ***** ", entryCt, " chapter names loaded *****");
	} // loadTextChapNames(String)

	public void doGet(HttpServletRequest request,
			HttpServletResponse response)
			throws ServletException, IOException {
		Utils.alwaysPrint("Initiating group loader manually ******** ");
		init();
	}

	public void doPost(HttpServletRequest request,
			HttpServletResponse response)
			throws ServletException, IOException {
		Utils.alwaysPrint("do post ");
	}

} // GroupLoader
