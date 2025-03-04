<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	chemaxon.struc.DPoint3,
	chemaxon.struc.Molecule,
	chemaxon.formats.MolImporter,
	com.epoch.chem.ChemUtils,
	com.epoch.chem.MolString,
	com.epoch.db.QSetRW,
	com.epoch.db.ResponseLogger,
	com.epoch.evals.EvalResult,
	com.epoch.evals.impl.chemEvals.MapProperty,
	com.epoch.exceptions.VerifyException,
	com.epoch.genericQTypes.*,
	com.epoch.lewis.LewisMolecule,
	com.epoch.mechanisms.Mechanism,
	com.epoch.physics.*,
	com.epoch.qBank.CaptionsQDatum,
	com.epoch.qBank.Figure,
	com.epoch.qBank.QDatum,
	com.epoch.qBank.QSetDescr,
	com.epoch.qBank.Question,
	com.epoch.substns.SubstnUtils,
	com.epoch.session.GradeSet,
	com.epoch.session.HWSession,
	com.epoch.synthesis.RxnCondition,
	com.epoch.synthesis.Synthesis,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.text.NumberFormat,
	java.util.Arrays,
	java.util.Date,
	java.util.HashMap,
	java.util.List,
	java.util.Map"
%>

<%@ include file="/js/edJava.jsp.h" %><% // imports energyDiagrams.* and defines edPhrases %>
<%@ include file="/js/rxnCondsJava.jsp.h" %> <% // defines rcPhrases %>
<%!
	public boolean hasData(EvalResult evalResult) {
		return evalResult != null && evalResult.status != EvalResult.NO_STATUS;
	} // hasData(EvalResult)
%>
<%
	response.setHeader("Cache-Control","no-cache, must-revalidate"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	final int SOLVE = HWSession.SOLVE;
	final int VIEW = HWSession.VIEW;
	final int PRACTICE = HWSession.PRACTICE;
	final int SIMILAR = HWSession.SIMILAR;
	final int GRADEBOOK_VIEW = HWSession.GRADEBOOK_VIEW;
	final int PREVIEW = HWSession.PREVIEW;
	final int TEXTBOOK = HWSession.TEXTBOOK;
	final int NONE = -1;

	final char NO_STATUS = EvalResult.NO_STATUS;
	final char INITIALIZED = EvalResult.INITIALIZED;
	final char SAVED = EvalResult.SAVED;
	final char HUMAN_NEEDED = EvalResult.HUMAN_NEEDED;
	final char EVALUATED = EvalResult.EVALUATED;

	final int PTS_PER_Q = Assgt.PTS_PER_Q;
	final int ATTEMPT = Assgt.ATTEMPT;
	final int TIME = Assgt.TIME;
	final boolean PAST_TENSE = Assgt.PAST_TENSE;
	final String VAR = Question.STARS_SIMPLE;
	final String APPLET_NAME = "responseApplet";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; 
			// until Marvin JS understands export parameters

	HWSession hwsession;
	Integer modeObj;
	String pts;
	synchronized (session) {
		hwsession = (HWSession) session.getAttribute("hwsession");
		modeObj = (Integer) session.getAttribute("mode");
		pts = (String) session.getAttribute("pts");
	}
	if (hwsession == null) {
		%> <jsp:forward page="../errormsgs/noSession.html"/> <%
	}

	int currentQNum = 1;
	final String currentQNumStr = request.getParameter("currentQNum");
	try {
		currentQNum = Integer.parseInt(currentQNumStr);
		hwsession.setCurrentIndex(currentQNum);
	} catch (Exception e) {
		currentQNum = hwsession.getCurrentIndex(); 
	} // try
	// negative Q number comes from gradebook: one question in assignment, want
	// to display assignment's question number
	int qNumDisplay = MathUtils.parseInt(request.getParameter("qNum"));
	qNumDisplay = (qNumDisplay < 0 ? -qNumDisplay : currentQNum);

	final Assgt assgt = hwsession.getHW();
	final boolean isMasteryAssgt = assgt.isMasteryAssgt();

	final Question question = hwsession.getCurrentQuestion();
	final int qId = question.getQId();
	final QDatum[] qData = question.getQData(Question.GENERAL);
	final Figure[] figures = question.getFigures();
	final boolean isMarvin = question.isMarvin();
	final boolean isMechanism = question.isMechanism();
	final boolean isSynthesis = question.isSynthesis();
	final boolean isLewis = question.isLewis();
	final boolean isChoice = question.isChoice();
	final boolean isChooseExplain = question.isChooseExplain();
	final boolean isRank = question.isRank();
	final boolean isFillBlank = question.isFillBlank();
	final boolean isNumeric = question.isNumeric();
	final boolean isOED = question.isOED();
	final boolean isRCD = question.isRCD();
	final boolean isText = question.isText();
	final boolean isTable = question.isTable();
	final boolean isClickableImage = question.isClickableImage();
	final boolean isDrawVectors = question.isDrawVectors();
	final boolean isEquations = question.isEquations();
	final boolean isFormula = question.isFormula();
	final boolean isLogicalStmts = question.isLogicalStatements();
	final boolean isOther = !isMarvin && !isLewis && !isMechanism 
			&& !isSynthesis && !isRank && !isChoice && !isChooseExplain
			&& !isFillBlank && !isNumeric && !isRCD &&!isOED && !isText 
			&& !isTable && !isClickableImage && !isDrawVectors
			&& !isEquations && !isFormula && !isLogicalStmts; 
	final boolean usesChemAxon = isMarvin || isMechanism || isSynthesis 
			|| isOther;
	final boolean mappingIsVisible = question.showMapping();
	final boolean is3D = question.is3D();
	final boolean preload = question.preload()
			|| (isTable && question.preloadTable());
	final boolean disallowMultipleResponses = question.disallowMult();
	final boolean allowUnranked = question.allowUnranked();
	final boolean usesSubstns = question.usesSubstns();
	final boolean useSciNotn = question.useSciNotn();
	final boolean requireInt = question.requireInt();
	final boolean numsOnly = question.numbersOnly();
	final boolean labelOrbitals = question.labelOrbitals();
	final boolean chemFormatting = question.chemFormatting();
	final boolean needResponseForDisplay = isRank 
			|| isChoice || isChooseExplain || isFillBlank;
	final long qFlags = question.getQFlags();

	int mode = (modeObj == null ? SOLVE : modeObj.intValue());
	final int prevMode = MathUtils.parseInt(request.getParameter("prevMode"), NONE);
	boolean haveNewResponse = request.getParameter("haveNewResponse") != null;
	/* Utils.alwaysPrint("answerJava.jsp: "
			+ "initially, mode = ", HWSession.getModeName(mode),
			", prevMode = ", HWSession.getModeName(prevMode),
			", currentIndex = ", hwsession.getCurrentIndex(),
			", haveNewResponse = ", haveNewResponse); /**/
	// reset R groups of previous question if necessary
	if (prevMode == SIMILAR && !isMasteryAssgt) {
		final Question lastQ = hwsession.getCurrentQuestion();
		if (lastQ != null && lastQ.usesSubstns()) {
			final String[][] oldNewSubstns = hwsession.resetSubstns();
			/* Utils.alwaysPrint("answerJava.jsp: prevMode == SIMILAR && "
			 		+ "!isMasteryAssgt; resetting R groups from ",
					oldNewSubstns[0], " to ", oldNewSubstns[1]); /**/
		} // if R groups needs to be reset
	} // if previous mode is similar

	EvalResult evalResult = null;
	String lastResp = null;

	final boolean dueDatePast = 
			hwsession.isDueDatePast() && !assgt.recordAfterDue();
	final int maxTries = assgt.getMaxTries();
	final boolean allowUnlimitedTries = assgt.allowUnlimitedTries();
	final EvalResult currentResult = hwsession.getCurrentResult();
	final boolean areTriesRemaining = allowUnlimitedTries 
			|| currentResult == null
			|| currentResult.tries < maxTries;
	if (mode == PREVIEW) {
		// set current question to most recently authored version
		synchronized (session) {
			final Question previewQ = (Question) session.getAttribute("qBuffer");
			if (previewQ != null) hwsession.setCurrentQuestion(previewQ);
		}
	} // if preview mode
	if (dueDatePast && mode == SOLVE) {
		mode = (prevMode == VIEW ? VIEW : PRACTICE);
		/* Utils.alwaysPrint("answerJava.jsp: due date past, changing mode to ", 
				HWSession.getModeName(mode)); /**/
	} else if (Utils.among(mode, PRACTICE, SIMILAR)
			&& !dueDatePast && areTriesRemaining) {
		/* Utils.alwaysPrint("answerJava.jsp: mode is ",
				HWSession.getModeName(mode), ", but due date not past "
				+ "and tries not used up; checking prior, stored evalResult "
				+ "of current Q to make sure mode is allowed."); /**/
		if (currentResult == null) {
			/* Utils.alwaysPrint("answerJava.jsp: previous result is null; "
					+ "setting mode to SOLVE."); /**/
			mode = SOLVE;
		} else if (Utils.among(currentResult.status, INITIALIZED, SAVED)) {
			/* Utils.alwaysPrint("answerJava.jsp: previous result is saved "
					+ "or initialized; setting mode to SOLVE and lastResp to:\n",
					currentResult.lastResponse); /**/
			mode = SOLVE;
			evalResult = currentResult;
			lastResp = evalResult.lastResponse;
		} else if (!haveNewResponse && mode != SIMILAR) {
			/* Utils.alwaysPrint("answerJava.jsp: no new response, so "
					+ "reevaluating old one."); /**/
			final EvalResult newResult = 
					hwsession.submitResponse(currentResult.lastResponse, VIEW);
			if (newResult.grade < 1.0) {
				/* Utils.alwaysPrint("answerJava.jsp: previous result is incorrect, "
						+ "so setting mode to SOLVE; prior evalResult has "
						+ "tries = ", newResult.tries, 
						", grade = ", newResult.grade, 
						", status = ", newResult.status,
						", lastResponse = ", newResult.lastResponse); /**/
				mode = SOLVE;
				evalResult = newResult;
				lastResp = evalResult.lastResponse;
			} else if (mode == SIMILAR && !usesSubstns) {
				/* Utils.alwaysPrint("answerJava.jsp: previous result is correct, "
						+ "but this Q does not use R groups, so changing "
						+ "to PRACTICE mode."); /**/
				mode = PRACTICE;
			} /* else Utils.alwaysPrint("answerJava.jsp: previous result is correct; "
					+ "leaving in ", HWSession.getModeName(mode), " mode."); /**/
		} // if currentResult
	} /* else Utils.alwaysPrint("answerJava.jsp: neither dueDatePast && mode == SOLVE "
			+ "nor Utils.among(mode, PRACTICE, SIMILAR) && !dueDatePast "
			+ "&& areTriesRemaining"); /**/

	boolean solveOrViewMode = Utils.among(mode, SOLVE, VIEW);
	final boolean previewOrTextbookMode = Utils.among(mode, PREVIEW, TEXTBOOK);
	final boolean practiceOrSimilarMode = Utils.among(mode, PRACTICE, SIMILAR);
	boolean disallowSubmit = mode == VIEW 
			|| (mode == GRADEBOOK_VIEW && (!isInstructorOrTA || usesSubstns)); 
	boolean shouldntHaveResponded = false;
	String selectionsStr = "";
	/* Utils.alwaysPrint("answerJava.jsp: "
	 		+ "isInstructorOrTA = ", isInstructorOrTA, 
			", dueDatePast = ", dueDatePast, 
			", mode = ", HWSession.getModeName(mode),
			", disallowSubmit = ", disallowSubmit, 
			", currentQNum = ", currentQNum, 
			", qType = ", question.getQType(), 
			", qFlags = ", qFlags, 
			", usesSubstns = ", usesSubstns, 
			", haveNewResponse = ", haveNewResponse); /**/

	// get the evalResult of this submission or the last submission
	if (haveNewResponse) {
		String responseMol = request.getParameter("submission");
		if (usesChemAxon) {
			final Molecule modResponse = MolImporter.importMol(responseMol);
			ChemUtils.setFromMarvinJS(modResponse);
			if (isSynthesis) {
				final String rxnIdsStr = request.getParameter("rxnIds");
				Synthesis.addRxnIds(modResponse, rxnIdsStr);
			} // if synthesis
			// for MarvinJS or old Marvin
			selectionsStr = request.getParameter("selections");
			if (!Utils.isEmpty(selectionsStr)) {
				ChemUtils.setSelections(modResponse, selectionsStr);
			} else selectionsStr = "";
			responseMol = MolString.toString(modResponse, Utils.MRV);
			/* Utils.alwaysPrint("answerJava.jsp: after modifying:\n", responseMol); /**/
		} // if usesChemAxon
		if (isTable) {
			// inputToCERs(urisToText()) is applied within responseToXML()
			responseMol = TableQ.responseToXML(request, TableQ.URIS_ENCODED);
		} else if (isText || isLogicalStmts) {
			/* Utils.alwaysPrint("answerJava.jsp: before processing, "
					+ "responseMol =\n", responseMol); /**/
			responseMol = Utils.inputToCERs(responseMol);
		} // if is table or text
		/* Utils.alwaysPrint("answerJava.jsp: before submitting, "
				+ "responseMol =\n", responseMol); /**/
		final String doEvaluateStr = request.getParameter("evaluate");
		final boolean doEvaluate = doEvaluateStr == null 
				|| "true".equals(doEvaluateStr); // will be false for save without submitting
		if (hwsession.logsAllToDisk() && !hwsession.isUserInstructorOrTA()
				&& (mode == SOLVE || mode == PRACTICE)) {
			final EvalResult logEvalResult = new EvalResult();
			logEvalResult.lastResponse = responseMol;
			logEvalResult.qId = qId;
			logEvalResult.ipAddr = hwsession.getIpAddr();
			logEvalResult.timeOfResponse = new Date();
			final int increment = (doEvaluate ? 1 : 0);
			logEvalResult.tries = (currentResult != null
					? currentResult.tries + increment : increment);
			final StringBuilder userNameBld = new StringBuilder()
					.append(hwsession.getUserName());
			if (mode != SOLVE) {
				Utils.appendTo(userNameBld, " (mode = ", 
						HWSession.getModeName(mode), ')');
			} // if not SOLVE mode
			ResponseLogger.logExamResponse(assgt.id, hwsession.getCurrentIndex(), 
					userNameBld.toString(), logEvalResult, timeZone);
		} // if isExam and not TA or instructor
		final boolean submitIt = !dueDatePast || mode != SOLVE || !doEvaluate;
		try {
			evalResult = (submitIt 
					? hwsession.submitResponse(responseMol, mode, doEvaluate)
					: hwsession.getCurrentResult());
			if (hasData(evalResult)) {
				lastResp = evalResult.lastResponse;
				/* Utils.alwaysPrint("answerJava.jsp: evaluated lastResp:\n", lastResp, 
						"\nto give evalResult.tries = ", evalResult.tries, 
						", evalResult.status = ", evalResult.status, 
						", evalResult.grade = ", evalResult.grade, 
						", evalResult.modGrade = ", evalResult.modGrade, 
						", evalResult.feedback = ", evalResult.feedback); /**/
			} /* else Utils.alwaysPrint("answerJava.jsp: evalResult is null."); /**/
		} catch (VerifyException e1) {
			// last response was right or max tries reached previously
			evalResult = hwsession.getCurrentResult();
			shouldntHaveResponded = true;
			evalResult.feedback = e1.getMessage();
		} // try
	} else if (Utils.among(mode, VIEW, // look at a previous submission
				SOLVE) // returning to solve previously attempted Q 
			|| (mode == GRADEBOOK_VIEW && !disallowSubmit)) { // one student gradebook view
		/* Utils.alwaysPrint("answerJava.jsp: getting prior, stored evalResult; "
		 		+ "mode = ", HWSession.getModeName(mode)); /**/
		evalResult = hwsession.getCurrentResult();
		if (hasData(evalResult)) {
			 /* Utils.alwaysPrint("answerJava.jsp: prior evalResult has "
					+ "tries = ", evalResult.tries, 
					", grade = ", evalResult.grade, 
					", status = ", evalResult.status,
					", lastResponse = ", evalResult.lastResponse); /**/
			lastResp = evalResult.lastResponse;
			/* Utils.alwaysPrint("answerJava.jsp: lastResp from lastResponse "
			 		+ "after unicodeToCERs():\n", lastResp); /**/
			if (Utils.among(evalResult.status, EVALUATED, HUMAN_NEEDED)) { 
				/* Utils.alwaysPrint("answerJava.jsp: Reevaluating the last "
						+ "response to get feedback, but not recording."); /**/
				final EvalResult newResult = 
						hwsession.submitResponse(lastResp, VIEW);
				evalResult.feedback = newResult.feedback;
				evalResult.modifiedResponse = newResult.modifiedResponse;
				/* Utils.alwaysPrint("answerJava.jsp: feedback = ", 
				 		evalResult.feedback, "\nmodifiedResponse:\n",
						evalResult.modifiedResponse); /**/
				haveNewResponse = true;
				if (evalResult.status == EVALUATED && evalResult.grade == 1.0 
						&& mode != GRADEBOOK_VIEW && !isMasteryAssgt) {
					mode = VIEW;
					if (prevMode == PRACTICE) {
						/* Utils.alwaysPrint("answerJava.jsp: already solved, "
								+ "prevMode is PRACTICE, changing mode to PRACTICE."); /**/
						mode = PRACTICE;
						solveOrViewMode = false;
						haveNewResponse = false;
						lastResp = null;
					} /* else Utils.alwaysPrint("answerJava.jsp: already solved, "
							+ "prevMode is not PRACTICE, changing mode to VIEW."); /**/
				} // if question has already been solved
			} // if there has been a prior try
		} // if evalResult is not null
	} else if (mode == GRADEBOOK_VIEW && needResponseForDisplay) {
		evalResult = hwsession.getCurrentResult();
		if (hasData(evalResult)) {
			evalResult.tries = 0;
			lastResp = evalResult.lastResponse;
			/* Utils.alwaysPrint("answer.jsp: no response to evaluate, "
					+ "got evalResult, lastResp:\n", lastResp); /**/
		} // if there's a recorded result
	} // if have submission or need previous result

	final StringBuilder statusBld = new StringBuilder();
	final StringBuilder status2Bld = new StringBuilder();
	final StringBuilder gradingAlertBld = new StringBuilder();
	final StringBuilder feedbackBld = new StringBuilder();
	String statusColor = "#eeeeee";

	// fix responses that are formatted incorrectly
	boolean reformatted = false;
	if (lastResp != null && !hwsession.formatOK(lastResp)) {
		Utils.alwaysPrint("answerJava.jsp: improper format for lastResp:\n", 
				lastResp);
		lastResp = hwsession.getInitializedString();
		final String status2 = "Your last response had an improper format "
				+ "for this question type; it has been reinitialized.";
		status2Bld.append(!previewOrTextbookMode ? user.translate(status2) : status2);
		Utils.alwaysPrint("answerJava.jsp: new lastResp:\n", lastResp);
		reformatted = true;
	} /* else Utils.alwaysPrint("answerJava.jsp: lastResp:\n", lastResp); /**/

	// display feedback or initialize 
	boolean lastRespIsModified = false;
	String preloadMol = (preload && usesSubstns ? figures[0].instantiatedMolstruct
			: preload ? hwsession.getCurrentPreloadMol() : null);
	if (hasData(evalResult)
			&& (Utils.among(evalResult.status, EVALUATED, HUMAN_NEEDED) 
					|| practiceOrSimilarMode
					|| previewOrTextbookMode)
			&& (mode == VIEW || haveNewResponse)) {
		/* Utils.alwaysPrint("answerJava.jsp: There is a previous attempt "
				+ "with results to display."); /**/
		if (evalResult.modifiedResponse != null) {
			lastResp = evalResult.modifiedResponse;
			lastRespIsModified = true;
			/* Utils.alwaysPrint("answerJava.jsp: lastResp as modified:\n", lastResp); /**/
		} // if there's a modified response
		final double grade = evalResult.grade;
		if (evalResult.status != HUMAN_NEEDED && !shouldntHaveResponded) {
			final NumberFormat decimalFormat = NumberFormat.getInstance(); 
			decimalFormat.setMinimumFractionDigits(1);
			decimalFormat.setMaximumFractionDigits(2);
			final boolean showQGrading = assgt.hasGradingParams(PTS_PER_Q);
			final boolean showAttemptGrading = assgt.hasGradingParams(ATTEMPT);
			final boolean showTimeGrading = assgt.hasGradingParams(TIME)
					&& hwsession.isDueDatePast() && assgt.recordAfterDue(); 
			final boolean showGradeAlert = solveOrViewMode
					&& (showQGrading || showAttemptGrading || showTimeGrading);
			final boolean partial = !Utils.among(grade, 0.0, 1.0);
			final String[] correctnessStrs =
					(grade == 1.0 ? new String[] {"correct", "100", "#bee7c0"}
					: grade == 0.0 ? new String[] {"incorrect", "0", "#f3cbc5"}
					: new String[] {"partially correct", 
							(showGradeAlert ? "50" : "0.5"), "#bee7c0"});
			final String[] substitutions = 
					new String[partial && showGradeAlert ? 3 
						: showGradeAlert ? 2 : partial ? 1 : 0];
			final StringBuilder earnedBld = Utils.getBuilder("Your ", 
					mode == VIEW ? "last response was " : "response is ", 
					correctnessStrs[0]);
			if (partial) {
				earnedBld.append(" (you earned ");
				if (!previewOrTextbookMode && !isMasteryAssgt) {
 					Utils.appendTo(earnedBld, VAR, correctnessStrs[1], VAR);
					if (showGradeAlert) {
						earnedBld.append("%, ");
						substitutions[0] = String.valueOf((int) (grade * 100));
					} else {
						substitutions[0] = decimalFormat.format(grade);
					} // if showGradeAlert
				} else {
					earnedBld.append(decimalFormat.format(grade));
				} // if previewOrTextbookMode
			} // if partial
			if (showGradeAlert && grade != 0.0 
					&& solveOrViewMode && !isMasteryAssgt) {
				if (!partial) earnedBld.append(" (");
 				Utils.appendTo(earnedBld, VAR, "modified to", 
						VAR, ' ', VAR, "2.0", VAR, " points)");
				// make alert to insert around "modified to"
				final int substnNum = (partial ? 1 : 0);
				substitutions[substnNum] = Utils.toString(
						"<a href=\"javascript:gradeAlert();\">", 
						user.translate("modified to"), "</a>");
				// make alert message
				if (showQGrading) {
					gradingAlertBld.append(assgt.gradingParamsToDisplay(
							PTS_PER_Q, !PAST_TENSE, currentQNum, 
							user).replaceAll("<br/>", "\\\n"));
					if (showAttemptGrading || showTimeGrading)
						gradingAlertBld.append("\n\n");
				} // if showQGrading 
				if (showAttemptGrading) {
					gradingAlertBld.append(assgt.gradingParamsToDisplay(
							ATTEMPT, PAST_TENSE, user).replaceAll(
							"<br/>", "\\\n"));
					if (showTimeGrading)
						gradingAlertBld.append("\n\n");
				} // if showAttemptGrading 
				if (showTimeGrading) {
					gradingAlertBld.append(assgt.gradingParamsToDisplay(
							TIME, PAST_TENSE, user).replaceAll(
							"<br/>", "\\\n"));
				} // if showTimeGrading
				substitutions[substnNum + 1] = 
						decimalFormat.format(evalResult.modGrade);
			} else if (partial) earnedBld.append(')');
			final String earned = earnedBld.toString();
			statusBld.append(!previewOrTextbookMode 
					? user.translate(earned, substitutions) : earned);
			statusColor = correctnessStrs[2];
		} // evalResult.status is not human required
		final int numTries = evalResult.tries;
		if (solveOrViewMode && !shouldntHaveResponded && numTries >= 1) {
			final String[] triesStrs = (numTries == 1 
					? new String[] {"1", "attempt"} 
					: new String[] {"2", "attempts"});
			statusBld.append(" (");
			if (!previewOrTextbookMode) {
				final String triesStr = Utils.toString(VAR, triesStrs[0], 
						VAR, ' ', triesStrs[1]);
				statusBld.append(user.translate(triesStr, numTries));
				if (!allowUnlimitedTries && numTries < maxTries) {
					statusBld.append(", ");
					final int remaining = maxTries - numTries;
					final String attempts = Utils.toString(VAR, 
							remaining == 1 ? 1 : 2, VAR, " remaining",
							isMasteryAssgt ? " for mastery" : "");
					statusBld.append(user.translate(attempts, remaining));
				} // if attempts are remaining
			} else {
 				Utils.appendTo(statusBld, numTries, ' ', triesStrs[1]);
			} // if previewOrTextbookMode
			statusBld.append(')');
		} // practice or preview mode
		if (statusBld.length() == 0) statusBld.append("&nbsp;");
		else if (evalResult.status != HUMAN_NEEDED) statusBld.append('.');
		if (!allowUnlimitedTries && numTries > maxTries && !isMasteryAssgt) {
			final String exceed = Utils.toString(
					"You have exceeded the maximum number of tries allowed (", 
					!previewOrTextbookMode ? VAR + '5' + VAR : maxTries, 
					"). Your instructor can see in the gradebook "
						+ "that you have used the Back function of your "
						+ "browser to bypass the limit on the number of "
						+ "tries, and he or she may penalize you for it.");
 			Utils.appendTo(statusBld, "<p>", !previewOrTextbookMode 
					? user.translate(exceed, maxTries) : exceed, "</p>");
		} // if student badly behaved and is not mastery assignment
		if (evalResult.feedback != null) {
			feedbackBld.append(evalResult.feedback.trim());
		} else if (grade == 1.0) {
			final String feedback = "Excellent.";
			feedbackBld.append(!previewOrTextbookMode 
					? user.translate(feedback) : feedback);
		} else {
			final String feedback = 
					"ACE cannot give feedback for this response.";
			feedbackBld.append(!previewOrTextbookMode 
					? user.translate(feedback) : feedback);
		} // evalResult.feedback
		if (!Utils.isEmpty(evalResult.comment)) {
 			Utils.appendTo(feedbackBld, "<p class=\"boldtext\">", 
					user.translate("Your instructor says:"), "</p><p>", 
					evalResult.comment, "</p>");
		} // evalResult.comment
		if (isMasteryAssgt && (grade == 1.0 || numTries >= maxTries)) {
 			Utils.appendTo(status2Bld, "<p>", user.translate(
					Utils.toString("You have ", 
						grade == 1.0 && numTries <= maxTries ? "" : "not ", 
						"mastered the concepts required to "
							+ "answer this question within the requisite "
							+ "number of attempts.")));
			if (grade < 1.0 || numTries > maxTries) {
 				Utils.appendTo(status2Bld, ' ', 
						user.translate("You may start over on a related question "
							+ "so that you can demonstrate mastery."), "</p>");
			} // if not mastered
		} // if is mastery assignment
		if (evalResult.status == EVALUATED && grade == 1.0 
				&& (!isMasteryAssgt || numTries <= maxTries)) { 
			disallowSubmit = !previewOrTextbookMode;
			if (Utils.among(mode, SOLVE, PRACTICE)
					&& currentQNum < assgt.getNumQsSeen()) { 
				final String mayProceed = "You may proceed to the next question.";
 				Utils.appendTo(status2Bld, "<p>", !previewOrTextbookMode 
						? user.translate(mayProceed) : mayProceed);
				if (mode == PRACTICE) {
					final String remain = Utils.toString("You will ", 
							dueDatePast ? "remain in" : "exit", 
							" Practice mode.");
 					Utils.appendTo(status2Bld, " (", !previewOrTextbookMode 
							? user.translate(remain) : remain, ')');
				} // PRACTICE
				status2Bld.append("</p>");
			} // PRACTICE or SOLVE
		} else if (evalResult.status == EVALUATED && !isMasteryAssgt 
				&& !allowUnlimitedTries && numTries >= maxTries) {
			// never true for PREVIEW or TEXTBOOK
			if (mode == PRACTICE) { 
				disallowSubmit = false;
				String stillIn = "You are still in Practice mode.";
				if (!previewOrTextbookMode) stillIn = user.translate(stillIn);
 				Utils.appendTo(status2Bld, "<p>", stillIn);
			} else { // SOLVE or VIEW
				disallowSubmit = true;
				if (numTries == maxTries) {
					final String reach = Utils.toString(
							"You have reached the maximum number of tries allowed (", 
							!previewOrTextbookMode ? VAR + '5' + VAR : maxTries, 
							"), but you may return to work this "
								+ "question in Practice mode, if you like.");
 					Utils.appendTo(status2Bld, "<p>", !previewOrTextbookMode 
							? user.translate(reach, maxTries) : reach, "</p>");
				} // if tries
			} // if mode
		} // grade
	} else { // no previous submission whose evaluation to display
		/* Utils.alwaysPrint("answerJava.jsp: evalResult is null "
				+ "or tries <= 0; maybe initialize lastResp; preload = ", preload); /**/
		final String savedNotSubmittedStr = "You have saved a response, but "
				+ "you have not yet submitted it for a grade.";
		if ((isRank || isChoice || isChooseExplain || isFillBlank) 
				&& (practiceOrSimilarMode
					|| previewOrTextbookMode
					|| (mode == GRADEBOOK_VIEW && evalResult == null))) {
			// get new arrangement of items
			final String initValue = hwsession.getInitializedString();
			/* Utils.alwaysPrint("answerJava.jsp: rank practice or preview mode; "
					+ "reinitializing last response ", lastResp, " to ", initValue); /**/
			lastResp = initValue;
		} else if (usesSubstns) {
			if (mode == GRADEBOOK_VIEW && isInstructorOrTA) {
				// show generic R groups
				if (!isNumeric) {
					figures[0].instantiatedMolstruct = figures[0].data;
				}
			} else if (!isNumeric) {
				// get R group substitutions to use in this session
				final Molecule[] rGroups = hwsession.getCurrentRGroupMols();
				/* Utils.alwaysPrint("answerJava.jsp: either not gradebook view "
						+ "or is student; rGroups are ", rGroups,
						", evalResult.status = ", 
						evalResult == null ? "none" : evalResult.status); /**/
				figures[0].instantiatedMolstruct = 
						MolString.toString(SubstnUtils.substituteRGroups(
							figures[0].data, rGroups), Utils.MRV);
				if (preload && (evalResult == null || mode == PRACTICE
						|| Utils.among(evalResult.status, INITIALIZED, NO_STATUS))) {
					preloadMol = figures[0].instantiatedMolstruct;
					lastResp = figures[0].instantiatedMolstruct;
					/* Utils.alwaysPrint("answerJava.jsp: setting lastResp "
							+ "to instantiatedMolstruct."); /**/
				} // if preload
			} // if GRADEBOOK_VIEW and is instructor or TA
			/* Utils.alwaysPrint("answerJava.jsp: usesSubstns, "
					+ "new instantiatedMolstruct is:\n", 
					figures[0].instantiatedMolstruct); /**/
			if (evalResult != null && evalResult.status == SAVED) {
				statusBld.append(!previewOrTextbookMode 
						? user.translate(savedNotSubmittedStr) 
						: savedNotSubmittedStr);
			} // if have a saved and not submitted response
		} else if (hasData(evalResult)) {
			// there is a previous response, but hasn't been evaluated
			// may be initialized or saved 
			evalResult = hwsession.getCurrentResult();
			if (!reformatted && Utils.among(evalResult.status, INITIALIZED, SAVED)) {
				lastResp = evalResult.lastResponse;
			} // if should load last response
			/* Utils.alwaysPrint("answerJava.jsp: initialized or saved lastResp:\n", 
			 		lastResp); /**/
			if (evalResult.status == SAVED) {
				statusBld.append(!previewOrTextbookMode 
						? user.translate(savedNotSubmittedStr) 
						: savedNotSubmittedStr);
			} // if saved but not submitted
		} else {
			lastResp = preloadMol;
			/* Utils.alwaysPrint("answerJava.jsp: preloadMol & lastResp:\n", 
					preloadMol); /**/
		} // if there is initialization to be done
	} // if there is a previous evalResult to display
	final String status = statusBld.toString();
	final String status2 = status2Bld.toString();
	final String gradingAlert = gradingAlertBld.toString();
	final boolean okToDisplayNext = currentQNum >= hwsession.getCount() 
			|| hwsession.getOkToDisplay(currentQNum + 1);

	if (usesChemAxon) {
		selectionsStr = ChemUtils.getSelectionsStr(lastResp);
	} // if uses ChemAxon

	/* Utils.alwaysPrint("answerJava.jsp: after initializing, disallowSubmit = ", 
			disallowSubmit, ", selectionsStr = ", selectionsStr, 
			", mode = ", HWSession.getModeName(mode),
			usesSubstns ? ", initiated R groups = " : "",
			usesSubstns ? hwsession.getCurrentSubstns() : "",
			", lastResp is: \n", lastResp); /**/

	// prepare feedback for display
	String feedback = feedbackBld.toString();
	final String START_SMILES = "openCalcProds('";
	final String END_SMILES = "')\"";
	final int locn = feedback.indexOf(START_SMILES);
	if (locn == -1) { 
		if (chemFormatting) feedback = Utils.toDisplay(feedback);
	} else { 
		int begin = locn + START_SMILES.length();
		final String feedback1 = feedback.substring(0, begin);
		feedback = feedback.substring(begin);
		begin = feedback.indexOf(END_SMILES) + 1;
		final String feedback2 = feedback.substring(0, begin);
		final String feedback3 = feedback.substring(begin);
		feedback = Utils.toString(Utils.toDisplay(feedback1), feedback2, 
				Utils.toDisplay(feedback3));
	} // if the feedback contains a SMILES string
	/* Utils.alwaysPrint("answerJava.jsp.h: feedback = ", feedback); /**/

	// used in synthesis Qs only
	final Map<Integer, String> reactionNamesByIds =
			(isSynthesis ? RxnCondition.getRxnNamesKeyedByIds() : null);
	int[] allowedRxns = null;
	String[] chosenRxns = new String[0];
	final boolean onlyOneRxnCondn = false;
	if (isSynthesis) {
		// translate [simply mix]
		final Integer noRgts = Integer.valueOf(RxnCondition.NO_REAGENTS);
		String defaultRgt = reactionNamesByIds.get(noRgts);
		if (!previewOrTextbookMode) defaultRgt = user.translate(defaultRgt);
		reactionNamesByIds.put(noRgts, defaultRgt);
		allowedRxns = assgt.getAllowedRxnCondns();
		if (Utils.isEmpty(allowedRxns)) {
			allowedRxns = RxnCondition.getAllReactionIds();
		} // if assignment doesn't specify allowed reaction conditions
		final String rxnIdsStr = Synthesis.getRxnConditions(lastResp);
		/* Utils.alwaysPrint("answerJava.jsp: rxnIdsStr = ", rxnIdsStr, 
		 		" for synthesis:\n", lastResp); /**/
		if (!Utils.isEmpty(rxnIdsStr)) {
			chosenRxns = rxnIdsStr.split(Synthesis.RXN_ID_SEP);
		 	/* Utils.alwaysPrint("answerJava.jsp: chosenRxns = ", chosenRxns); /**/ 
		} // if there are selected synthesis reactions
	} // if synthesis Q 

	// Used in Table questions only.
	final String tableDisp = (!isTable ? null
			: lastRespIsModified ? lastResp
			: (new TableQ(lastResp)).convertToHTML(qData, chemFormatting, 
				TableQ.STUD_INPUT));
	 /* if (isTable) Utils.alwaysPrint("answerJava.jsp: lastResp:\n", lastResp,
			"tableDisp:\n", tableDisp); /**/

	// define and name phrases that need to be translated
	final String PROCESSING = "Processing... Please wait...";
	final String ALIAS = "You probably meant to add a shortcut group such "
			+ "as Ph or Et to your structure, but you added an atom alias "
			+ "instead. (Aliases and shortcut groups look alike, but ACE cannot "
			+ "interpret aliases properly.) To replace the alias with a "
			+ "shortcut group, press the Abbreviated Groups button (the first "
			+ "button on the southern toolbar; the icon is a pair of "
			+ "brackets with a book), type the name of the shortcut "
			+ "group, press OK to close the dialog box, and " 
			+ "then click on the alias.";
	final String STEREOBOND = "Your response appears to be in 3D mode and "
			+ "to contain stereo bonds such as bold or hashed wedges.  When "
			+ "a drawing is in 3D mode, ACE ignores the stereochemical "
			+ "information conveyed by stereo bonds.  Do you still want "
			+ "to submit your response?";
	final String CONVERT2D = "To convert your drawing back into 2D mode, "
			+ "press the Clean 2D button in the northern toolbar. "
			+ "Then you can redraw your wedge bonds.";
	final String SAME_RANK = "No two items may have the same rank.";
	final String RANK_EVERY = "Please choose a rank for every item.";
	final String RANK_1 = "Please start your ranking at 1.";
	final String RANK_CONSEC = "Please rank the items consecutively.";
	final String PULLDOWN = "Please choose an option from every pulldown menu.";
	final String ENTER_NUM = "Please enter a numerical value in "
			+ (isTable ? "each " : useSciNotn ? "the first " : "the ") 
			+ "textbox.";
	final String ENTER_INT = "Please enter an integer in "
			+ (isTable ? "each " : useSciNotn ? "the first " : "the ") 
			+ "textbox.";
	final String ENTER_EXPON = "Please enter an exponent (0 is acceptable).";
	final String INTEGRAL = "Please enter an integral exponent.";
	final String SORT_BY_NUM = "Sort by number";
	final String ENTER_RXN_ID = "Or add none to use reaction conditions "
			+ "already stored in the response.";
	final String PLEASE_SUBMIT = "Please submit your first response.";
	final String NO_RESP = "You did not submit a response to this question.";
	final String EMPTY_RESP = "ACE could not acquire your response to this "
			+ "question. Please submit a response that is not empty.";
	final String MORE_ROOM1 = Utils.toString("Need more room to draw your ", 
			isMechanism ? "mechanism" : "synthesis", "?");
	final String MORE_ROOM2 = "Press the button in the southern toolbar "
			+ "whose icon is a burst of four brown arrows, "
			+ "and draw away in the new window that pops up! Whatever you "
			+ "draw in the new window will appear automatically in "
			+ "the original window (but not vice versa). You may "
			+ "leave the new window open when you submit, or not, "
			+ "as you wish.";
	final String SOURCE = "Source";
	final String SOURCE_INFO = "You may copy the following code, paste it "
			+ "into the " + "Lewis JS clipboard, and import it into "
			+ "Lewis JS's drawing window, if you wish. "
			+ "The pasted code must begin with one blank line.";
	final String CLICK_ENLARGE = "Click image to enlarge";
	final String FIG = "Fig.";
	final String CANT_SUBMIT = "You cannot submit a response to this "
			+ "question. Please proceed to the next question.";
	final String TIME_UP = "The time allotted to you for this "
			+ "assignment has expired.";
	final String NOW_MODE = "You are now in "
			+ (practiceOrSimilarMode ? "practice"
				: mode == VIEW ? "view"
				: mode == SOLVE ? "question-solving" : "an unknown")
			+ " mode.";
	final String ID_NUMS_SELECTED = "ID numbers of selected reactions";
	final String NO_TEXT = "Please enter your response in the text box.";
	final String NO_POINT = "Please click somewhere on the figure.";
	final String ADD_STMT = "Add statement";
	final String REMOVE_STMT = "Remove last statement";
	final String THEREFORE = "therefore";
	final String BLANK_STMT = "Please enter a statement before adding another "
			+ "line.";
	final String BLANK_SUBMIT = Utils.toString("Please do not submit any blank ", 
			isEquations ? "equations" : "statements", ".");
	final String UNKNOWN_SUBMIT = "Please do not submit any statements that "
			+ "contain unknown words (those struck through and in red).";
	final String SEE_WORDS = "See acceptable words";
	final String NO_ARROW = "Please draw at least one arrow on the figure by "
			+ "clicking on it in two different places.";
	final String CLEAR_LAST = "Clear last";
	final String CLEAR_ALL = "Clear all";
	final String CANCEL = "Cancel";
	final String DELETE_SELECTED = "Delete";
	final String UNSELECT = "Unselect";
	final String CLICK_NEAR = "Click near a mark to select it.";
	final String DRAW_NEW = "Draw new";
	final String SELECT_EXISTING = "Select existing";
	final String CLICK_ENLARGE_VECS = "ACE will display vectors on this image. "
			+ "Click the image to see them.";
	final String INVERT_SELECTED = "Invert";
	final String CHOOSE_ACTION = "Choose an action";
	final String ADD_EQN = "Insert here";
	final String REMOVE_EQN = "Remove";
	final String CLICK_NEAR_ARROW = "Click near a vector to select it; "
			+ "<br/>click and hold near the vector's midpoint to move the vector, "
			+ "<br/>shift-click and hold to copy; "
			+ "<br/>click and hold the endpoint of a vector to move the endpoint.";
	final String CONSTANTS = "Constants";
	final String ENTER_ELEMENT = "Enter an element symbol.";
	final String NO_SUCH_ELEMENT = "There is no such element.  Please try again.";
	final String OTHER_LABEL = "Other";
	final String OPEN_NEW_WINDOW = "Open in new window";
	final String PASTE_SYN = "Paste a synthesis copied with chosen reaction "
			+ "conditions here:";
	final String AC_SHORTCUT_IS_ELEMENT = "Your structure contains the element "
			+ "actinium (Ac), an element that organic chemists almost never "
			+ "use. You almost certainly meant to use Ac to indicate an acetyl "
			+ "group. If so, press Cancel below, then use the space bar or the "
			+ "first button on the southern "
			+ "toolbar to bring up the Abbreviated Groups dialog, enter Ac, "
			+ "press OK, and click on the Ac atom in your drawing. Note the "
			+ "color change; shortcut groups are black, whereas elements are "
			+ "other colors.";
	final String PR_SHORTCUT_IS_ELEMENT = "Your structure contains the element "
			+ "praseodymium (Pr), an element that organic chemists almost "
			+ "never use. You almost certainly meant to use Pr to indicate a "
			+ "propyl group. If so, press Cancel below, then use the space bar "
			+ "or the first button on the southern "
			+ "toolbar to bring up the Abbreviated Groups dialog, enter Pr, "
			+ "press OK, and click on the Pr atom in your drawing. Note the "
			+ "color change; shortcut groups are black, whereas elements are "
			+ "other colors.";
	final String CN_SHORTCUT_IS_ELEMENT = "Your structure contains the element "
			+ "copernicium (Cn), an element that organic chemists almost never "
			+ "use. You almost certainly meant to use CN to indicate a cyano "
			+ "group. If so, press Cancel below, then use the space bar or the "
			+ "first button on the southern "
			+ "toolbar to bring up the Abbreviated Groups dialog, enter CN, "
			+ "press OK, and click on the Cn atom in your drawing. Note the "
			+ "color change; shortcut groups are black, whereas elements are "
			+ "other colors.";
	final String TS_SHORTCUT_IS_ELEMENT = "Your structure contains the element "
			+ "tennessine (Ts), an element that organic chemists almost never "
			+ "use. You almost certainly meant to use Ts to indicate a "
			+ "toluenesulfonyl or tosyl) group. If so, press Cancel below, "
			+ "then use the space bar or the first button on the southern "
			+ "toolbar to bring up the Abbreviated Groups dialog, enter Ts, "
			+ "press OK, and click on the Ts atom in your drawing. Note the "
			+ "color change; shortcut groups are black, whereas elements are "
			+ "other colors.";

	// put phrases in an array in preparation for translation
	final String[] ansPhrasesArr = new String[] {
			PROCESSING, 
			ALIAS, 
			STEREOBOND, 
			CONVERT2D, 
			SAME_RANK, 
			RANK_EVERY, 
			RANK_1, 
			RANK_CONSEC, 
			PULLDOWN, 
			ENTER_NUM, 
			ENTER_INT, 
			ENTER_EXPON, 
			INTEGRAL, 
			SORT_BY_NUM, 
			ENTER_RXN_ID, 
			PLEASE_SUBMIT, 
			NO_RESP, 
			EMPTY_RESP, 
			MORE_ROOM1,
			MORE_ROOM2, 
			SOURCE, 
			SOURCE_INFO, 
			CLICK_ENLARGE, 
			FIG, 
			CANT_SUBMIT, 
			TIME_UP, 
			NOW_MODE, 
			ID_NUMS_SELECTED, 
			NO_TEXT, 
			NO_POINT, 
			ADD_STMT, 
			REMOVE_STMT, 
			THEREFORE, 
			BLANK_STMT, 
			BLANK_SUBMIT,
			UNKNOWN_SUBMIT, 
			SEE_WORDS, 
			NO_ARROW, 
			CLEAR_LAST, 
			CLEAR_ALL, 
			CANCEL, 
			DELETE_SELECTED, 
			UNSELECT, 
			CLICK_NEAR, 
			DRAW_NEW, 
			SELECT_EXISTING, 
			CLICK_ENLARGE_VECS, 
			INVERT_SELECTED, 
			CHOOSE_ACTION, 
			ADD_EQN, 
			REMOVE_EQN, 
			CLICK_NEAR_ARROW, 
			CONSTANTS, 
			ENTER_ELEMENT, 
			NO_SUCH_ELEMENT, 
			OTHER_LABEL, 
			OPEN_NEW_WINDOW, 
			PASTE_SYN,
			AC_SHORTCUT_IS_ELEMENT,
			PR_SHORTCUT_IS_ELEMENT,
			CN_SHORTCUT_IS_ELEMENT
			};
	Map<String, String> ansPhrases = new HashMap<String, String>();
	if (previewOrTextbookMode) {
		ansPhrases = Utils.mapToSelf(ansPhrasesArr);
	} else {
		ansPhrases = user.translateToMap(ansPhrasesArr);
		edPhrases = user.translateToMap(edPhrasesArr); // from js/edJava.jsp.h
		rcPhrases = user.translateToMap(rcPhrasesArr); // from js/rxnCondsJava.jsp.h
	} // if mode is not preview

	int jmolNum = 0;
	int mviewNum = 0;
	final boolean showClock = "true".equals(request.getParameter("showClock"));

// vim:filetype=jsp

%>
