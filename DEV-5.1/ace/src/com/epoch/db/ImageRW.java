package com.epoch.db;

import static com.epoch.db.dbConstants.ImageConstants.*;
import com.epoch.AppConfig;
import com.epoch.constants.AuthorConstants;
import com.epoch.utils.Utils;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Contains methods to read and write images (and movies).  */
public final class ImageRW extends DBCommon implements AuthorConstants {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** Connection to the database (pooled). */
	transient final private Connection con;
	/** Master or local database tables. */
	transient final private DBTables tables;

/* *********** Constructors ****************/

	/** Constructor.  
	 * @param	connxn	an established database connection (pooled)
	 */
	ImageRW(Connection connxn) { 
		con = connxn;
		tables = getTables(!LOCAL);
	} // ImageRW(Connection)

	/** Constructor.  
	 * @param	connxn	an established database connection (pooled)
	 * @param	isLocal	whether to use local tables
	 */
	ImageRW(Connection connxn, boolean isLocal) { 
		con = connxn;
		tables = getTables(isLocal);
	} // ImageRW(Connection, boolean)

	/** Constructor.  
	 * @param	connxn	an established database connection (pooled)
	 * @param	authId	login ID of the question author
	 */
	ImageRW(Connection connxn, String authId) { 
		con = connxn;
		tables = getTables(authId != MASTER_AUTHOR);
	} // ImageRW(Connection, String)

	/** Constructor.  
	 * @param	authId	login ID of the question author
	 */
	ImageRW(String authId) { 
		con = null;
		tables = getTables(authId != MASTER_AUTHOR);
	} // ImageRW(String)

	/** Gets a new image ID number.
	 * @return	a new image ID number
	 */
	int getNewImageId() {
		return nextSequence(con, FIGURES_SEQ);
	} // getNewImageID()

	/** Writes an image's location to the database and changes its old filename
	 * to a new one.
	 * @param	imageId	unique ID of this image
	 * @param	newLocation	name of permanent file containing the image
	 * relative to the application root
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	void addImage(int imageId, String newLocation) throws SQLException {
		final String SELF = "ImageRW.addImage: ";
		// create db record
		final String simpleLocation = Utils.stripFilePath(newLocation);
		final String[] fields = new String[] {
				IMG_ID,
				IMG_RELFILENAME};
		final String qry = getInsertIntoValuesQMarksSQL(tables.IMAGES, fields);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry,
				imageId,
				simpleLocation);
		debugPrint(SELF, sql_vals);
		tryUpdate(con, sql_vals);
	} // addImage(int, String, String)

	/** Writes an image's location to the database and changes its old filename
	 * to a new one.
	 * @param	imageId	unique ID of this image
	 * @param	currentLocation	name of temporary file containing the image 
	 * relative to the application root
	 * @param	newLocation	name of permanent file containing the image
	 * relative to the application root
	 * @throws	SQLException	if there's a problem writing to the database
	 * @throws	FileNotFoundException	if the file isn't at the given location
	 * @throws	IOException	if the file can't be written
	 */
	void addImage(int imageId, String currentLocation, String newLocation) 
			throws SQLException, FileNotFoundException, IOException {
		final String SELF = "ImageRW.addImage: ";
		addImage(imageId, newLocation);
		try {
			if (!Utils.isEmpty(currentLocation)) {
				if (isUsed(con, newLocation)) {
					copyImageFile(currentLocation, newLocation);
				} else if (!currentLocation.equals(newLocation)) {
					renameImageFile(currentLocation, newLocation);
				} // if image needs to be renamed
			} // if should rename the file now
		} catch (FileNotFoundException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw e;
		} catch (IOException e) {
			rollbackConnection(con);
			e.printStackTrace();
			throw e;
		}
	} // addImage(int, String, String)

	/** Copies a file.
	 * @param	currentLocation	current location of the file relative to app
	 * root
	 * @param	newLocation	new location of the file relative to app root
	 * @throws	FileNotFoundException	if the file isn't at the given location
	 * @throws	IOException	if the files can't be read or written
	 */
	static void copyImageFile(String currentLocation, String newLocation) 
			throws FileNotFoundException, IOException {
		final String SELF = "ImageRW.copyImageFile: ";
		debugPrint(SELF + "copying file ", currentLocation, " to ", 
				newLocation);
		final File inFile = 
				new File(toString(AppConfig.appRoot, '/', currentLocation));
		final File outFile = 
				new File(toString(AppConfig.appRoot, '/', newLocation));
		FileInputStream inStream = null;
		FileOutputStream outStream = null;
		final byte[] buffer = new byte[4096]; // because Rafi says so
		int length;
		try {
			inStream = new FileInputStream(inFile);
			outStream = new FileOutputStream(outFile);
			// fetch data
			while ((length = inStream.read(buffer)) != -1) {
				outStream.write(buffer, 0, length);
			} // while
		} finally {
			try {
				if (inStream != null) {
					inStream.close();
				}
			} finally {
				if (outStream != null) {
					outStream.close();
				}
			}
		} // finally
	} // copyImageFile(String, String)

	/** Renames a file; if rename operation fails, copies the file and deletes
	 * the original.
	 * @param	currentLocation	current location of the file relative to app
	 * root
	 * @param	newLocation	new location of the file relative to app root
	 * @throws	FileNotFoundException	if the file isn't at the location
	 * @throws	IOException	if the files can't be read or written
	 */
	static void renameImageFile(String currentLocation, String newLocation) 
			throws FileNotFoundException, IOException {
		final String SELF = "ImageRW.renameImageFile: ";
		debugPrint(SELF + "renaming file ", currentLocation, " to ", 
				newLocation);
		final File inFile = 
				new File(toString(AppConfig.appRoot, '/', currentLocation));
		final File outFile = 
				new File(toString(AppConfig.appRoot, '/', newLocation));
		if (!inFile.renameTo(outFile)) {
			FileInputStream inStream = null;
			FileOutputStream outStream = null;
			final byte[] buffer = new byte[4096]; // because Rafi says so
			int length;
			try {
				inStream = new FileInputStream(inFile);
				outStream = new FileOutputStream(outFile);
				while ((length = inStream.read(buffer)) != -1) {
					outStream.write(buffer, 0, length);
				} // while
			} finally {
				try {
					if (inStream != null) {
						inStream.close();
					}
				} finally {
					if (outStream != null) {
						outStream.close();
					}
				}
			} // finally
			inFile.delete();
		} // if renameTo operation didn't work
	} // renameImageFile(String, String)

	/** Renames files.
	 * @param	con	database connection
	 * @param	renameImages	list of old and new file names
	 * @throws	SQLException	if there's a problem reading the database
	 * @throws	FileNotFoundException	if the files aren't at the locations
	 * @throws	IOException	if the files can't be read or written
	 */
	static void renameImageFiles(Connection con, List<String[]> renameImages) 
			throws FileNotFoundException, IOException, SQLException {
		for (final String[] names : renameImages) {
			if (isUsed(con, names[0])) {
				copyImageFile(names[0], names[1]);
			} else {
				renameImageFile(names[0], names[1]);
			} // if file name is already being used
		} // for each image in the list
	} // renameImageFiles(List<String[]>)

	/** Deletes images from a table and, if the connection is set to autocommit, 
	 * the disk.
	 * @param	imgsSql_vals	SQL for selecting the images to delete, not already 
	 * parenthesized, and values to substitute for question marks in the SQL
	 * @return	names of files that have been deleted from disk or that need 
	 * to be deleted from disk after commit is complete
	 * @throws	SQLException	if there's a problem writing to the database
	 */
	List<String> deleteImages(SQLWithQMarks imgsSql_vals) throws SQLException {
		final String SELF = "ImageRW.deleteImages: ";
		final List<String> fullNames = new ArrayList<String>();
		final String imgsSql = parens(imgsSql_vals.getSql());
		final String qry = toString(
				SELECT + IMG_RELFILENAME 
				+ FROM, tables.IMAGES,
				WHERE + IMG_ID + IN, imgsSql);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, imgsSql_vals);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			final StringBuilder figsDirBld = getAbsFigsDirBld();
			while (rs.next()) {
				fullNames.add(toString(figsDirBld, 
						rs.getString(IMG_RELFILENAME)));
			} // while more results
		} catch (SQLException e) {
			Utils.alwaysPrint(SELF + "caught SQLException while trying to get "
					+ "names of image files.");
			rollbackConnection(con);
			e.printStackTrace();
			throw e;
		} finally {
			closeStmtAndRs(stmt, rs);
		}
		sql_vals.setSql(
				DELETE_FROM, tables.IMAGES, 
				WHERE + IMG_ID + IN, imgsSql);
		debugPrint(SELF, sql_vals);
		tryUpdate(con, sql_vals);
		if (con.getAutoCommit()) {
			deleteImagesFromDisk(fullNames);
			fullNames.clear();
		} // if connection already set to commit
		debugPrint(SELF + "returning ", fullNames, " as names of files to be "
				+ "deleted from disk after commit.");
		return fullNames;
	} // deleteImages(SQLWithQMarks)

	/** Gets the name of the directory containing the figures. 
	 * @return	name of the directory containing the figures
	 */
	StringBuilder getAbsFigsDirBld() {
		return getAbsFigsDirBld(tables.local);
	} // getAbsFigsDirBld()

	/** Gets the name of the directory containing the figures. 
	 * @param	isLocal	whether to use a user_ directory
	 * @return	name of the directory containing the figures
	 */
	static StringBuilder getAbsFigsDirBld(boolean isLocal) {
		final StringBuilder filenameBld = getBuilder(AppConfig.appRoot);
		if (!AppConfig.appRoot.endsWith("/")) filenameBld.append('/');
		if (isLocal) filenameBld.append(DBLocalTables.LOCAL_PREFIX);
		filenameBld.append(AppConfig.relFiguresDir);
		if (!AppConfig.relFiguresDir.endsWith("/")) filenameBld.append('/');
		return filenameBld;
	} // getAbsFigsDirBld(boolean)

	/** Deletes a file from disk.
	 * @param	fullName	full name of the file to be deleted
	 */
	public static void deleteImageFromDisk(String fullName) {
		deleteImagesFromDisk(Arrays.asList(fullName));
	} // deleteImageFromDisk(String)

	/** Deletes from disk a list of files.
	 * @param	fullNames	full names of the files to be deleted
	 */
	public static void deleteImagesFromDisk(List<String> fullNames) {
		final String SELF = "ImageRW.deleteImagesFromDisk: ";
		for (final String fullName : fullNames) {
			debugPrint(SELF + "deleting ", fullName, " from disk");
			try {
				final File file = new File(fullName);
				if (file.exists()) file.delete();
			} catch (Exception e) {
				alwaysPrint(SELF + "cannot tell if ", fullName, 
						" exists or not");
			} // try
		} // for each file to remove from disk
	} // deleteImagesFromDisk(List<String>)

	/** Deletes from disk all files in the figures directory whose names start 
	 * with a pattern. Code reference:
	 * http://www.coderanch.com/t/278095//java/Wildcard-delete-File-Object.
	 * @param	startName	the beginning of the name of the files to be deleted
	 */
	public void deleteImagesFromDisk(final String startName) {
		final String SELF = "ImageRW.deleteImagesFromDisk: ";
		try {
			final File figDir = new File(getAbsFigsDirBld().toString());
			final File[] toBeDeleted = figDir.listFiles(
					new FileFilter() {
						public boolean accept(File theFile) {
							return theFile.isFile()
								 && theFile.getName().startsWith(startName);
						} // accept(File)
					});
			debugPrint(SELF + "found ", Utils.getLength(toBeDeleted), 
					" file(s) on disk starting with ", startName);
			if (toBeDeleted != null) {
				for (final File deletableFile : toBeDeleted) {
					deletableFile.delete();
				}
			}
		} catch (Exception e) {
			alwaysPrint(SELF + "error deleting files with names "
					+ "starting with ", startName);
		} // try
	} // deleteImagesFromDisk(String)

	/** Gets an image's extension from the database.
	 * @param	imageId	unique ID of this image
	 * @return	extension of the image
	 * @throws	SQLException	if there's a problem reading the database
	 */
	String getExtension(int imageId) throws SQLException {
		final String SELF = "ImageRW.getExtension: ";
		String extension = null;
		final String qry = toString(
				SELECT + IMG_RELFILENAME 
				+ FROM, tables.IMAGES,
				WHERE + IMG_ID + EQUALS + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				imageId);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {  
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			if (rs.next()) {
				final String fileName = rs.getString(IMG_RELFILENAME);
				extension = Utils.getExtension(fileName);
			} // if there's a result
		} finally {
			closeStmtAndRs(stmt, rs);
		}
		return extension;
	} // getExtension(int)

	/** Gets whether an image's filename is already in the database.
	 * @param	con	database connection
	 * @param	imgFile	image's filename
	 * @return	true if the image's filename is already in the database
	 * @throws	SQLException	if there's a problem reading the database
	 */
	static boolean isUsed(Connection con, String imgFile) throws SQLException {
		final String SELF = "ImageRW.isUsed: ";
		boolean isUsed = false;
		final String simpleFile = Utils.stripFilePath(imgFile);
		final DBTables masterTables = new DBTables();
		final DBLocalTables localTables = new DBLocalTables();
		final String qry = toString(
				SELECT + IMG_ID + FROM, masterTables.IMAGES,
				WHERE + IMG_RELFILENAME + LIKE + QMARK
				+ UNION + SELECT + IMG_ID + FROM, localTables.IMAGES,
				WHERE + IMG_RELFILENAME + LIKE + QMARK);
		final SQLWithQMarks sql_vals = new SQLWithQMarks(qry, 
				simpleFile,
				simpleFile);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {  
			debugPrint(SELF, sql_vals);
			stmt = getStatement(con, sql_vals);
			rs = stmt.executeQuery();
			isUsed = rs.next();
		} finally {
			closeStmtAndRs(stmt, rs);
		}
		debugPrint(SELF + "image file ", imgFile, " is ",
				isUsed ? "" : "not ", "already in use by another image");
		return isUsed;
	} // isUsed(String)

	/** Gets an SQL phrase retrieving all image IDs.
	 * @return	an SQL phrase retrieving all image IDs
	 */
	StringBuilder getAllImageIds() {
		return getBuilder(SELECT + IMG_ID + FROM, tables.IMAGES);
	} // getAllImageIds()

} // ImageRW
