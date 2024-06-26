<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolImporter,
	chemaxon.struc.Molecule,
	com.epoch.AppConfig,
	com.epoch.chem.ChemUtils,
	com.epoch.chem.MolString,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.io.DataOutputStream,
	java.io.File,
	java.io.FileOutputStream,
	java.io.IOException,
	java.nio.channels.FileLock"
%>
<%
	response.setHeader("Cache-Control","no-cache, must-revalidate"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

	final long now = System.currentTimeMillis();
	final long gracePeriod = 2 * 60 * 60 * 1000; // 2 hours, in milliseconds
	final String absTempDirName = 
			Utils.toString(AppConfig.appRoot, AppConfig.relTempDir);
	// Utils.alwaysPrint("writeMolFile.jsp: absTempDirName = ", absTempDirName);
	int fileNum = 0;
	final File absTempDir = new File(absTempDirName);
	final File[] tempFiles = absTempDir.listFiles();
	/* Utils.alwaysPrint("writeMolFile.jsp: ", absTempDirName, " contains ",
			tempFiles.length, " file(s)."); /**/
	for (final File tempFile : tempFiles) {
		fileNum++;
		final long age = now - tempFile.lastModified();
		if (age > gracePeriod) { // too old
			try {
				tempFile.delete();
				/* Utils.alwaysPrint("writeMolFile.jsp: deleted old file ", 
						fileNum, ": ", tempFile.getName()); /**/
			} catch (SecurityException e) {
				Utils.alwaysPrint("writeMolFile.jsp: caught exception "
						+ "when deleting old file ", fileNum, ": ",
						tempFile.getName());
				e.printStackTrace();
				// nothing to be done about this problem
			}
		} /* else Utils.alwaysPrint("writeMolFile.jsp: file ", fileNum,
				" is too young, ", ((double) age) / (1000.0 * 60.0 * 60.0), 
				" hrs, to be deleted."); /**/
	} // each file in the directory
	/* Utils.alwaysPrint("writeMolFile.jsp: after cleaning, ", absTempDirName, 
			" contains ", absTempDir.listFiles().length, " file(s)."); /**/
	String molStr = request.getParameter("mol");
	if (molStr == null) molStr = "";
	final Molecule initMol = MolImporter.importMol(molStr);
	initMol.setProperty(ChemUtils.FROM_MARVIN_JS, null);
	molStr = MolString.toString(initMol, Utils.MRV);
	final String userName = request.getParameter("userName");
	final boolean addSlash = !AppConfig.relTempDir.endsWith("/");
	final String molFileName = Utils.toString(AppConfig.relTempDir, 
			addSlash ? '/' : "", userName, '_', now, ".mrv");
	/* Utils.alwaysPrint("writeMol.jsp: molFileName = ", molFileName,
			", molStr:\n", molStr); /**/
	try {
		final File molFile = 
				new File(Utils.toString(AppConfig.appRoot, molFileName));
		final FileOutputStream fos = new FileOutputStream(molFile);
		final DataOutputStream dos = new DataOutputStream(fos);
		final FileLock lock = fos.getChannel().lock();
		dos.writeBytes(molStr);
		lock.release();
		dos.close();
	} catch (IOException e) {
		Utils.alwaysPrint("writeMolFile.jsp: caught exception when "
				+ "writing to ", molFileName);
		e.printStackTrace();
	}
	String callerPathToRoot = request.getParameter("pathToRoot");
	final String url = request.getRequestURL().toString();
	final String urlRoot = Utils.substringToWith(url, "ace/");
%>
<html>
<body>
pathToRootValue = @@@@<%= callerPathToRoot %>@@@@
userAgentValue = @@@@<%= request.getHeader("User-Agent") %>@@@@
appletValue = @@@@<%= request.getParameter("applet") %>@@@@
molFileUrlValue = @@@@<%= urlRoot %><%= molFileName %>@@@@
</body>
</html>
