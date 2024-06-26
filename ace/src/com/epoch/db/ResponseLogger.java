package com.epoch.db;

import static com.epoch.db.dbConstants.ResponsesConstants.*;
import chemaxon.formats.MolFormatException;
import com.epoch.AppConfig;
import com.epoch.chem.MolCompare;
import com.epoch.chem.MolString;
import com.epoch.constants.AuthorConstants;
import com.epoch.energyDiagrams.OED;
import com.epoch.energyDiagrams.RCD;
import com.epoch.evals.EvalResult;
import com.epoch.exceptions.ConfigurationException;
import com.epoch.exceptions.DBException;
import com.epoch.exceptions.ParameterException;
import com.epoch.exceptions.VerifyException;
import com.epoch.genericQTypes.Choice;
import com.epoch.genericQTypes.ChooseExplain;
import com.epoch.genericQTypes.Rank;
import com.epoch.mechanisms.Mechanism;
import com.epoch.qBank.QDatum;
import com.epoch.qBank.Question;
import com.epoch.qBank.qBankConstants.QuestionConstants;
import com.epoch.responses.StoredResponse;
import com.epoch.synthesis.Synthesis;
import com.epoch.utils.Utils;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/** Logs all responses to a file for each question.
   <br>Uses locks to prevent race conditions.
   <br>Reads and returns a summary of the stored responses.

   <P>All responses stored in a single directory (given is chimp.properties)

		<br>&lt;qId&gt;.log		   master
		<br>&lt;qId&gt;.summary	   master
		<br>&lt;user-id&gt;.&lt;qId&gt;.log  locally modified and locally added
		<br>&lt;user-id&gt;.&lt;qId&gt;.summary locally modified and locally added

	<P>in file, single line is
	&lt;smiles|MOLorMRV&gt;
*/
public final class ResponseLogger extends DBCommon 
		implements AuthorConstants, QuestionConstants {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** Character to replace \n in responses. */
	private static final char NEWLINE_SUB = 12;
	/** Maximum number of responses in the summary. */
	private static final int MAX_SUMMARY = 1000;
	/** Maximum number of responses (in rank order) pulled from the log. */
	private static final int MAX_RETRIEVE_RESPONSES = 20;

	/** Log a student response to an ordinary question.
	 * @param	qId	unique ID number of the question to which the student
	 * responded
	 * @param	qType	type of the question (skeletal, ranking, etc.)
	 * @param	qFlags	flags of the question
	 * @param	response	String representation of the student's response
	 * @throws	ConfigurationException	if the response log directory doesn't
	 * exist
	 * @throws	ParameterException	if the response can't be converted into the
	 * appropriate response object
	 */
	public static void logResponse(int qId, int qType, long qFlags, String response)
			throws ConfigurationException, ParameterException {
		logResponse(qId, qType, qFlags, MASTER_AUTHOR, response);
	} // logResponse(int, int, long, String)

	/** Log a student response to an ordinary question.
	 * @param	qId	unique ID number of the question to which the student
	 * responded
	 * @param	qType	type of the question (skeletal, ranking, etc.)
	 * @param	qFlags	flags of the question
	 * @param	authorId	login ID of the author of the
	 * question to which the student responded, if it is locally authored;
	 * otherwise null
	 * @param	response	String representation of the student's response
	 * @throws	ConfigurationException	if the response log directory doesn't
	 * exist
	 * @throws	ParameterException	if the response can't be converted into the
	 * appropriate response object
	 */
	public static void logResponse(int qId, int qType, long qFlags, 
			String authorId, String response) 
			throws ConfigurationException, ParameterException {
		final String SELF = "ResponseLogger.logResponse: ";
		if (!(new File(AppConfig.responseLogDir)).isDirectory())
			throw new ConfigurationException(" Response log dir "
					+ AppConfig.responseLogDir + " does not exist");
		final String fullpath = toString(
				AppConfig.responseLogDir,
				authorId == MASTER_AUTHOR ? "" : getBuilder(authorId, '.'),
				qId, ".log");
		debugPrint(SELF + "Being logged in ", fullpath);
		// compute lineData: the logged information
		final String lineData = getModifiedVersion(qType, qFlags, response);
		try {
			doLog(fullpath, lineData);
		} catch (IOException e) {
			Utils.alwaysPrint(SELF + "cannot log response: ", e.getMessage());
		}
	} // logResponse(int, int, long, String, String)

	/** Log a student response to an exam question.
	 * @param	examId	unique (external) ID of the assignment acting as an exam
	 * @param	qNum	1-based serial number of the question to which the student
	 * responded
	 * @param	studentName	name of the student responding to the question
	 * @param	evalResult	contains the response, response time, number of
	 * tries
	 * @param	timeZone	the time zone of the course of the assignment to
	 * which this response was a reply
	 * @throws	ConfigurationException	if the response log directory doesn't
	 * exist
	 */
	public static void logExamResponse(int examId, int qNum,
			String studentName, EvalResult evalResult, 
			TimeZone timeZone) throws ConfigurationException {
		final String SELF = "ResponseLogger.logExamResponses: ";
		if (!(new File(AppConfig.responseLogDir)).isDirectory())
			throw new ConfigurationException(" Response log dir "
					+ AppConfig.responseLogDir + " does not exist");
		final String fullpath = toString(
				AppConfig.responseLogDir, "exam", examId, ".html");
		debugPrint(SELF + "Being logged in ", fullpath);
		final String when = dateToString(evalResult.timeOfResponse, timeZone);
		final String NEW_TH = "</th><th>";
		final String NEW_TD = "</td><td>";
		final File logfile = new File(fullpath);
		final String toLog = toString(logfile.exists() ? ""
 					: "<html><body><table border=\"1\"><tr><th>Name" 
						+ NEW_TH + "Q #" 
						+ NEW_TH + "Q ID" 
						+ NEW_TH + "Tries" 
						+ NEW_TH + "At" 
						+ NEW_TH + "From" 
						+ NEW_TH + "Process Data" 
						+ NEW_TH + "Response" 
						+ "</th></tr>", 
				"<tr><td>", studentName, 
				NEW_TD, qNum, 
				NEW_TD, evalResult.qId, 
				NEW_TD, evalResult.tries, 
				NEW_TD, when, 
				NEW_TD, evalResult.ipAddr, 
				NEW_TD, getProcessDataBld(), 
				NEW_TD, "<pre>", makeTableFriendly(evalResult.lastResponse), 
					"</pre>",
				"</td></tr>");
		debugPrint(SELF, toLog);
		try {
			doLog(logfile, toLog);
		} catch (IOException e) {
			Utils.alwaysPrint(SELF + "cannot log response: ", e.getMessage());
		} // try
	} // logExamResponse(int, int, String, EvalResult, TimeZone)

	/** Gets a StringBuilder containing the process data.
	 * @return	StringBuilder containing the process data
	 */
	private static StringBuilder getProcessDataBld() {
		final String SELF = "ResponseLogger.getProcessDataBld: ";
		final StringBuilder processDataBld = new StringBuilder();
		try {
			final BufferedReader rdr = new BufferedReader(
				// new FileReader("/proc/loadavg"));
				new InputStreamReader(
					new FileInputStream("/proc/loadavg"),
					StandardCharsets.UTF_8
				)
			);
			while (true) {
				final String line = rdr.readLine();
				if (line == null) break;
				if (Utils.isEmpty(line.trim())) continue;
				processDataBld.append(line);
			} // read while not eof
			rdr.close();
		} catch (IOException e) {
			Utils.alwaysPrint(SELF + "cannot get process data: ", 
					e.getMessage());
		} // try
		return processDataBld;
	} // getProcessDataBld()

	/** Modifies the response string to view in an HTML table by converting &lt;
	 * and &gt; to &amp;lt; and &amp;gt; and by inserting return characters.
	 * @param	resp	the response
	 * @return	the modifed response
	 */
	private static String makeTableFriendly(String resp) {
		if (resp == null) return "";
		String respMod = resp.trim().replaceAll("<", "&lt;")
				.replaceAll(">&lt;", ">\n&lt;");
		if (respMod.length() > 100
				&& respMod.indexOf('\r') < 0 
				&& respMod.indexOf('\n') < 0) {
			final StringBuilder allBld = new StringBuilder();
			StringBuilder lineBld = new StringBuilder();
			final String[] respPieces = respMod.split(" ");
			for (final String respPiece : respPieces) {
				lineBld.append(respPiece).append(' ');
				if (lineBld.length() > 100) {
					allBld.append(lineBld).append('\r');
					lineBld = new StringBuilder();
				} // if long enough
			} // for each response piece
			respMod = allBld.append(lineBld).toString();
		} // if need to add return characters
		return respMod;
	} // makeTableFriendly(String)

	/** Log student entry into an exam assignment.
	 * @param	examId	unique (external) ID of the assignment acting as an exam
	 * @param	studentName	name of the student responding to the question
	 * @param	timeZone	the time zone of the course of the assignment to
	 * which this response was a reply
	 * @param	host	name of host of the student's computer
	 * @param	ipAddr	IP address of the student's computer
	 * @throws	ConfigurationException	if the response log directory doesn't
	 * exist
	 */
	public static void logExamEntry(int examId, String studentName,
			TimeZone timeZone, String host, String ipAddr) 
			throws ConfigurationException {
		final String SELF = "ResponseLogger.logExamEntry: ";
		if (!(new File(AppConfig.responseLogDir)).isDirectory())
			throw new ConfigurationException(" Entry log dir "
					+ AppConfig.responseLogDir + " does not exist");
		final String fullpath = toString(
				AppConfig.responseLogDir, "exam", examId, "Entry.html");
		debugPrint(SELF + "Being logged in ", fullpath);
		final String when = dateToString(new Date(), timeZone);
		final File logfile = new File(fullpath);
		final String NEW_TH = "</th><th>";
		final String NEW_TD = "</td><td>";
		final String toLog = toString(logfile.exists() ? ""
				: "<html><body><table border=\"1\"><tr><th>Name" 
					+ NEW_TH + "Host / IP" + NEW_TH + "At</th></tr>",
				"<tr><td>", studentName, NEW_TD, host, " / ", ipAddr, 
				NEW_TD, when, "</td></tr>");
		try {
			doLog(logfile, toLog);
		} catch (IOException e) {
			Utils.alwaysPrint(SELF + "cannot log response: ", e.getMessage());
		} // try
	} // logExamEntry(int, String, TimeZone, String, String)

	/** Convert the student's MOL or MRV response string into something
	 * appropriate for logging.
	 * @param	qType	type of the question (skeletal, ranking, etc.)
	 * @param	qFlags	flags of the question
	 * @param	respStr	String representation of the student's response
	 * @return	modified version of the student's response suitable for logging
	 * @throws	ParameterException	if the response can't be converted into the
	 * appropriate response object
	 */
	private static String getModifiedVersion(int qType, long qFlags,
			String respStr) throws ParameterException {
		final String SELF = "ResponseLogger.getModifiedVersion: ";
		String resp = respStr;
		if (Utils.among(qType, CHOICE, FILLBLANK)) {
			final Choice choiceResp = new Choice(resp);
			resp = choiceResp.getStringChosenOptions(Choice.SORT);
			debugPrint(SELF + "choice question new value = ", resp);
		} else if (qType == CHOOSE_EXPLAIN) {
			final ChooseExplain chooseExplainResp = new ChooseExplain(resp);
			resp = toString(
					chooseExplainResp.choice.getStringChosenOptions(Choice.SORT), 
					ChooseExplain.SEPARATOR, chooseExplainResp.text);
			debugPrint(SELF + "choice question new value = ", resp);
		} else if (qType == RANK) {
			final Rank rankResp = new Rank(resp);
			resp = rankResp.getRankStringOrderedByItem();
			debugPrint(SELF + "ranking question new value = ", resp);
		} else if (qType == MARVIN) {
			resp = (Question.is3D(qFlags) ? removeNewlines(resp)
					: MolString.toSmiles(resp));
		} else if (qType == TEXT) {
			resp = Utils.condenseWhitespace(resp);
		} else if (!Utils.among(qType, NUMERIC, CLICK_IMAGE, FORMULA)) {
			// LEWIS, SYNTHESIS, MECHANISM, THREEDIM, TABLE, OED, RCD
			resp = removeNewlines(resp);
		} // if qType
		return resp;
	} // getModifiedVersion(int, long, String)

	/** Replaces newline characters in a string with control-L characters and 
	 * removes carriage return characters.
	 * @param	str	the string
	 * @return	the modified string
	 */
	private static String removeNewlines(String str) {
		return str.replaceAll("\r", "").replace('\n', NEWLINE_SUB);
	} // removeNewlines(String)

	/** Replaces control-L characters with newline characters. 
	 * @param	str	the string
	 * @return	the modified string
	 */
	private static String restoreNewlines(String str) {
		return str.replace(NEWLINE_SUB, '\n');
	} // restoreNewlines(String)

	/** Log the response string.
	 * @param	fullpath	location of log file
	 * @param	lineData	single-line representation of response for logging
	 * @throws	IOException	if the log file can't be read to written to
	 */
	private static void doLog(String fullpath, String lineData)
			throws IOException {
		doLog(new File(fullpath), lineData);
	} // doLog(String, String)

	/** Log the response string.
	 * @param	logfile	the log file
	 * @param	lineData	single-line representation of response for logging
	 * @throws	IOException	if the log file can't be read to written to
	 */
	private static void doLog(File logfile, String lineData)
			throws IOException {
		final String SELF = "ResponseLogger.doLog: ";
		debugPrint(SELF, lineData);
		FileLock lock;
		if (logfile.exists()) {
			try {
				final RandomAccessFile file = 
						new RandomAccessFile(logfile, "rw");
				lock = file.getChannel().lock();
				file.seek(file.length());
				file.writeBytes(lineData);
				file.writeBytes("\n");
				lock.release();
				file.close();
			} catch (NonWritableChannelException e) {
				Utils.alwaysPrint(SELF + "Can't log response ", lineData, 
						" due to lock on file " + logfile.getAbsolutePath());
			}
		} else { // no logfile yet
			final FileOutputStream fos = new FileOutputStream(logfile);
			final DataOutputStream dos = new DataOutputStream(fos);
			lock = fos.getChannel().lock();
			dos.writeBytes(lineData);
			dos.writeBytes("\n");
			lock.release();
			dos.close();
		} // no logfile yet
	} // doLog(File, String)

	/** Get the most frequent logged responses to this question.
	 * @param	qId	unique ID of the question the response to which are desired
	 * @param	qType	type of the question (skeletal, ranking, etc.)
	 * @param	qFlags	flags of the question
	 * @return	array of stored responses
	 * @throws	ConfigurationException	if the response log directory doesn't
	 * exist
	 */
	public static StoredResponse[] getResponses(int qId, int qType, long qFlags)
			throws ConfigurationException, NonReadableChannelException, 
			NonWritableChannelException {
		return getResponses(qId, MASTER_AUTHOR, qType, qFlags);
	} // getResponses(int, int, long)

	/** Get the most frequent logged responses to this question.
	 * Read the responses logged since the last time this method was called for
	 * this question, and incorporate them into the summary (responses and
	 * number of occurrences of each) of previously processed responses.
	 * @param	qId	unique ID of the question the response to which are desired
	 * @param	authorId	login ID of the author of the
	 * question to which the student responded, if it is locally authored;
	 * otherwise null
	 * @param	qType	type of the question (skeletal, ranking, etc.)
	 * @param	qFlags	flags of the question
	 * @return	array of stored responses
	 * @throws	ConfigurationException	if the response log directory doesn't
	 * exist
	 */
	public static StoredResponse[] getResponses(int qId, String authorId,
			int qType, long qFlags) throws ConfigurationException,
			NonReadableChannelException, NonWritableChannelException {
		final String SELF = "ResponseLogger.getResponses: ";
		StoredResponse[] responses = new StoredResponse[0];
		try {
			if (!(new File(AppConfig.responseLogDir)).isDirectory())
				throw new ConfigurationException(" Response log dir "
						+ AppConfig.responseLogDir + "does not exist");
			final StringBuilder fullpathBld = getBuilder(
					AppConfig.responseLogDir,
					authorId == MASTER_AUTHOR ? "" : getBuilder(authorId, '.'),
					qId);
			debugPrint(SELF + "Retrieving from file ",
					fullpathBld.toString(), ".summary");
			responses = doGetResponses(fullpathBld, qType, qFlags, qId, 
					authorId);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return responses;
	} // getResponses(int, String, int, long)

	/** Get the most frequent logged responses to this question.
	 * @param	filePrefix	name of log file without the extension
	 * @param	qType	type of the question (skeletal, ranking, etc.)
	 * @param	qFlags	flags of the question
	 * @param	qId	unique ID of the question the response to which are desired
	 * @param	authorId	login ID of the author of the
	 * question to which the student responded, if it is locally authored;
	 * otherwise null
	 * @return	array of stored responses
	 * @throws	ConfigurationException	if the response log directory doesn't
	 * exist
	 * @throws	IOException	if the log file can't be read to written to
	 */
	private static StoredResponse[] doGetResponses(StringBuilder filePrefix,
			int qType, long qFlags, int qId, String authorId) 
			throws IOException, NonReadableChannelException,
			NonWritableChannelException, ConfigurationException {
		final String SELF = "ResponseLogger.getResponses: ";
		final String summaryFile = toString(filePrefix, ".summary");
		final String logFile = toString(filePrefix, ".log");
		final File summaryf = new File(summaryFile);
		final File logf = new File(logFile);
		if (!summaryf.exists() && !logf.exists())  {
			// no responses recorded yet
			debugPrint(SELF + "no responses recorded yet in files ", 
					summaryFile, ' ', logFile);
			return new StoredResponse[0];
		}
		// Load previous summary, if any
		MolCountTable summary = null;
		if (summaryf.exists()) {
			debugPrint(SELF + "Summary file exists already; "
					+ "getting as input stream.");
			final FileInputStream fis = new FileInputStream(summaryf);
			final ObjectInputStream ipstream = new ObjectInputStream(fis);
			try {
				debugPrint(SELF + "reading from summary");
				summary = (MolCountTable) ipstream.readObject();
				if (summary != null) {
					debugPrint(SELF + "read from summary: ", summary.size(), 
							" stored responses");
					summary.showMolCountTable();
				} else {
					debugPrint(SELF + "summary file is null.");
					summary = new MolCountTable(qId, authorId);
				}
			} catch (ClassNotFoundException e) {
				debugPrint(SELF + "ClassNotFoundException");
			}
			ipstream.close();
			fis.close();
		} else {
			debugPrint(SELF + "Summary file doesn't exist already.");
			summary = new MolCountTable(qId, authorId);
		} // no summary file exists
		boolean reachedLogEnd = true;
		if (logf.exists()) {
			try {
				final BufferedReader rdr = new BufferedReader(
					// new FileReader(logFile));
					new InputStreamReader(
						new FileInputStream(logFile), StandardCharsets.UTF_8
					)
				);
				debugPrint(SELF +
					"reading logged responses and adding to summary.");
				// going through each line in the log
				while (true) {
					final String line = rdr.readLine();
					if (line == null) break;
					if (Utils.isEmpty(line.trim())) continue;
					final String logResp = restoreNewlines(line);
					debugPrint(SELF + "logged response = \n", logResp);
					final int presentIndex = 
							summary.indexOfMol(logResp, qType, qFlags);
					if (presentIndex != -1) {
						summary.incrementCount(presentIndex);
						debugPrint(SELF + "number of such responses now = ",
								summary.getCount(presentIndex));
					} else summary.addResponse(logResp);
					if (summary.size() > MAX_SUMMARY) {
						debugPrint(SELF, MAX_SUMMARY,  
								" unique responses in summary; breaking.");
						reachedLogEnd = false;
						break;
					} // if we have a lot of unique responses
				} // read while not eof
				rdr.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new ConfigurationException("IO error while reading logfile:\n" 
						+ e.getMessage());
			}
		} // if log file exists
		debugPrint(SELF + "after adding logged responses to summary:");
		summary.showMolCountTable();
		// write the new summary file, delete the log file
		final FileOutputStream fos = new FileOutputStream(summaryf);
		debugPrint(SELF + "Getting channel for summary file output.");
		final FileChannel outChannel = fos.getChannel();
		debugPrint(SELF + "Getting lock on summary file output.");
		final FileLock outLock = outChannel.lock();
		final ObjectOutputStream opstream = new ObjectOutputStream(fos);
		opstream.writeObject(summary);
		debugPrint(SELF + " new summary written; releasing output lock, "
				+ "closing outputstreams.");
		outLock.release();
		opstream.close();
		fos.close();
		debugPrint(SELF + "Output lock released and outputstreams closed.");
		if (reachedLogEnd) {
			logf.delete();
			debugPrint(SELF + "log file deleted.");
		} else debugPrint(SELF + "did not reach end of log file before maxing "
				+ "out summary, so log file not deleted.");
		// select and return the most frequent responses
		final int[] allCounts = summary.getAllCounts();
		final int numResps = allCounts.length;
		Arrays.sort(allCounts);
		final List<StoredResponse> mostFreqResps = 
				new ArrayList<StoredResponse>();
		int prevCount = 0;
		for (int ctNum = numResps - 1; ctNum >= 0; ctNum--) {
			final int count = allCounts[ctNum];
			if (mostFreqResps.size() > MAX_RETRIEVE_RESPONSES
					&& count != prevCount) {
				break;
			} // if we've retrieved enough responses
			final String respWithCt = summary.getRespWithCount(count);
			if (respWithCt != null) { // should always be true
				final StoredResponse resp = new StoredResponse();
				resp.response = respWithCt;
				resp.numEntries = count;
				mostFreqResps.add(resp);
				debugPrint(SELF, count, " of:\n", respWithCt);
			} // if there's a responses with this number of occurrences
			prevCount = count;
		} // for each occurrence
		final int numMostFreq = mostFreqResps.size();
		if (numMostFreq > 0) {
			debugPrint(SELF + "returning ", numMostFreq,
					" most frequent responses with occurrences of ", 
					prevCount, " to ", allCounts[numResps - 1]);
		} else debugPrint(SELF + "returning no responses.");
		return mostFreqResps.toArray(new StoredResponse[numMostFreq]);
	} // doGetResponses(StringBuilder, int, long, int, String)

	/** Constructor to disable external instantiation. */
	private ResponseLogger() { }

} // ResponseLogger


/** Holds lists of String representations of molecules retrieved
 * from the log file and their number of occurrences. */
class MolCountTable implements QuestionConstants, Serializable {

	private static void debugPrint(Object... msg) {
		// alwaysPrint(msg);
	}

	/** Responses retrieved from the summary or log file. */
	transient private List<String> responses;
	/** Parallel list of number of occurrences of members of responses. */
	transient private List<Integer> counts;
	/** Unique ID number of the question whose responses are being analyzed. */
	transient private final int qId;
	/** Author of the question whose responses are being analyzed. */
	transient private final String authorId;

	/** Lord knows. */
	private static final long serialVersionUID = 591166858765853377L;

	/** Constructor.
	 * @param	qId	unique ID of the question the response to which are desired
	 * @param	authorId	login ID of the author of the
	 * question to which the student responded, if it is locally authored;
	 * otherwise null
	 */
	public MolCountTable(int qId, String authorId) {
		responses = new ArrayList<String>();
		counts = new ArrayList<Integer>();
		this.qId = qId;
		this.authorId = authorId;
	} // MolCountTable(int, String)

	/** Gets the number of distinct responses in the table.
	 * @return	number of distinct responses 
	 */
	public int size() { 
		return Utils.getSize(responses); 
	} // size()

	/** Gets the number of occurrences of a response.
	/** Gets a response.
	 * @param	respNum	1-based index of response in table
	 * @return	the response
	 */
	public String getResponse(int respNum) { 
		return responses.get(respNum - 1); 
	} // getResponse(int)

	/** Gets an array of the occurrences of all responses in the table.
	 * @return	the array of occurrences
	 */
	public int[] getAllCounts() {
		int[] countsArr = null;
		if (counts == null) {
			if (responses == null) {
				countsArr = new int[0];
				counts = new ArrayList<Integer>();
				responses = new ArrayList<String>();
			} else {
				countsArr = new int[responses.size()];
				Arrays.fill(countsArr, 1);
				counts = Utils.intArrayToList(countsArr);
			} // if there are no responses
		} // if counts can't be found
		return (countsArr == null ? Utils.listToIntArray(counts) : countsArr);
	} // getAllCounts()

	/** Gets the number of occurrences of a response.
	 * @param	respNum	1-based index of response in table
	 * @return	number of occurrences of the response
	 */
	public int getCount(int respNum) { 
		return counts.get(respNum - 1).intValue(); 
	} // getCount(int)

	/** Increase by 1 the number of occurrences of a response.
	 * @param	respNum	1-based index of response in table
	 */
	public void incrementCount(int respNum) { 
		addCount(respNum, 1); 
	} // incrementCount(int)

	/** Increase by <i>n</i> the number of occurrences of a response.
	 * @param	respNum	1-based index of response in table
	 * @param	count	number of occurrences
	 */
	public void addCount(int respNum, int count)	{ 
		 setCount(respNum, getCount(respNum) + count);
	} // addCount(int, int)

	/** Sets the number of occurrences of a response.
	 * @param	respNum	1-based index of response in table
	 * @param	count	number of occurrences
	 */
	public void setCount(int respNum, int count) { 
		counts.set(respNum - 1, Integer.valueOf(count));
	} // setCount(int, int)

	/** Determines whether a compound is already in the table.
	 * @param	newResponse	a response
	 * @param	qType	type of the question (skeletal, ranking, etc.)
	 * @param	qFlags	flags of the question
	 * @return	1-based index of the compound in the list such that 
	 * responses[respNum] == newResponse; -1 if structure isn't in the table
	 */
	public int indexOfMol(String newResponse, int qType, long qFlags) {
		final String SELF = "ResponseLogger.indexOfMol: ";
		int respNumBase1 = -1;
		final boolean isMechanism = Question.isMechanism(qType);
		final boolean isSynthesis = Question.isSynthesis(qType);
		final boolean isOED = Question.isOED(qType);
		final boolean isRCD = Question.isRCD(qType);
		Mechanism newMech = null;
		Synthesis newSynth = null;
		OED newOED = null;
		RCD newRCD = null;
		QDatum[] qData = null;
		try {
			if (isMechanism) {
				newMech = new Mechanism(newResponse);
			} else if (isSynthesis) {
				newSynth = new Synthesis(newResponse, !Synthesis.EMPTY_BOX_OK);
			} else if (isOED || isRCD) {
				final Question theQ = 
						QuestionRW.getLightQuestion(qId, authorId);
				qData = theQ.getQData(GENERAL);
				if (isOED) {
					newOED = new OED(qData);
					newOED.setOrbitals(newResponse);
				} else {
					newRCD = new RCD(qData);
					newRCD.setStates(newResponse);
				} // if question type
			} // if isOED or is RCD
		} catch (MolFormatException e) {
			return respNumBase1;
		} catch (ParameterException e) {
			return respNumBase1;
		} catch (VerifyException e) {
			return respNumBase1;
		} catch (DBException e) {
			return respNumBase1;
		} // try
		for (int respNum = 0; respNum < size(); respNum++) {
			final String oldResponse = responses.get(respNum);
			try {
				if (Question.isLewis(qType)) {
					if (MolCompare.matchPerfectLewis(
							oldResponse, newResponse)) {
						respNumBase1 = respNum + 1;
			 			debugPrint(SELF + "Lewis structure:\n", 
								newResponse, "found at respNumBase1 ", 
								respNumBase1, ":\n", oldResponse);
						break;
					} // if Lewis structures are identical
				} else if (Question.is3D(qType)) {
					if (MolCompare.matchConformers(
			 				oldResponse, newResponse)) {
						respNumBase1 = respNum + 1;
			 			debugPrint(SELF + "conformer:\n", 
								newResponse, "found at respNumBase1 ", 
								respNumBase1, ":\n", oldResponse);
						break;
					} // if conformers match
				} else if (Question.isMarvin(qType)) {
					if (MolCompare.matchPerfect(
							oldResponse, newResponse)) {
						respNumBase1 = respNum + 1;
			 			debugPrint(SELF + "skeletal structure:\n", 
								newResponse, "found at respNumBase1 ", 
								respNumBase1, ":\n", oldResponse);
						break;
					} // if skeletal structures match
				} else if (isMechanism) {
					final Mechanism oldMech = new Mechanism(oldResponse);
					if (newMech.equals(oldMech)) {
						respNumBase1 = respNum + 1;
			 			debugPrint(SELF + "mechanism:\n", 
								newResponse, "found at respNumBase1 ", 
								respNumBase1, ":\n", oldResponse);
						break;
					} // if mechanisms match
				} else if (isSynthesis) {
					final Synthesis oldSynth = new Synthesis(oldResponse);
					if (newSynth.equals(oldSynth)) {
						respNumBase1 = respNum + 1;
			 			debugPrint(SELF + "synthesis:\n", 
								newResponse, "found at respNumBase1 ", 
								respNumBase1, ":\n", oldResponse);
						break;
					} // if syntheses match
				} else if (isOED) {
					final OED oldOED = new OED(qData);
					oldOED.setOrbitals(oldResponse);
					if (newOED.equals(oldOED)) {
						respNumBase1 = respNum + 1;
			 			debugPrint(SELF + "orbital energy diagram:\n", 
								newResponse, "found at respNumBase1 ", 
								respNumBase1, ":\n", oldResponse);
						break;
					} // if diagrams match
				} else if (isRCD) {
					final RCD oldRCD = new RCD(qData);
					oldRCD.setStates(oldResponse);
					if (newRCD.equals(oldRCD)) {
						respNumBase1 = respNum + 1;
			 			debugPrint(SELF + "reaction coordinate diagram:\n", 
								newResponse, "found at respNumBase1 ", 
								respNumBase1, ":\n", oldResponse);
						break;
					} // if diagrams match
				} else if (oldResponse.equals(newResponse)) {
					respNumBase1 = respNum + 1;
			 		debugPrint(SELF + "non-Lewis, nonconformer, "
							+ "non-Marvin response ", newResponse, 
							"found at respNumBase1 ", respNumBase1, 
							": ", oldResponse);
					break;
				} // if responses are identical
			} catch (Exception e) {
				e.printStackTrace();
			} // try
		} // for each structure respNum
		if (respNumBase1 == -1) {
			debugPrint(" -- not present ");
		}
		return respNumBase1;
	} // indexOfMol(String, int, long)
	
	/** Remove a response from the lists.
	 * @param	respNum	1-based index of response in table
	 */
	public void removeResponse(int respNum) {
		if (respNum > size()) return;
		responses.remove(respNum - 1);
		counts.remove(respNum - 1);
	} // removeResponse(int)

	/** Add a response to the lists.
	 * @param	response	a response
	 */
	public void addResponse(String response) {
		addResponse(response, 1);
	} // addResponse(String)

	/** Add a response to the lists.
	 * @param	response	a response
	 * @param	ct	number of occurrences
	 */
	public void addResponse(String response, int ct) {
		if (response != null) {
			responses.add(response);
			counts.add(Integer.valueOf(ct));
		}
	} // addResponse(String, int)

	/** Gets the response that has been logged a certain number of times, and
	 * removes it and its occurrence from the lists.
	 * @param	count	number of times a response has been found
	 * @return	the response that has been found the given number of times
	 */
	public String getRespWithCount(int count) {
		String resp = null;
		final int index = counts.indexOf(Integer.valueOf(count));
		if (index >= 0) {
			resp = responses.remove(index);
			counts.remove(index);
		} // if a response with this number of occurrences was found
		return resp;
	} // getRespWithCount(int)

	/** Debug output of MolCountTable lists. */
	public void showMolCountTable() {
		debugPrint("=== MolCountTable print ==");
		for (int respNum = 0; respNum < size(); respNum++) {
			debugPrint(responses.get(respNum), " : ", counts.get(respNum));
		}
		debugPrint("==========================");
	} // showMolCountTable()

} // MolCountTable
