<%@ page import="
	com.epoch.access.EpochEntry,
	com.epoch.chem.MolString,
	com.epoch.courseware.User,
	com.epoch.evals.EvalManager,
	com.epoch.evals.Evaluator,
	com.epoch.evals.Subevaluator,
	com.epoch.evals.impl.CompareNums,
	com.epoch.lewis.LewisMolecule,
	com.epoch.qBank.Figure,
	com.epoch.qBank.QDatum,
	com.epoch.qBank.Question,
	com.epoch.responses.StoredResponse,
	com.epoch.session.QSet,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>

<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%@ include file="/includes/functions.inc.jsp.h" %>

<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

	final String pathToRoot = "../../";
	final String pathToChooseRxnCondsUser = "../";
	
	Question question;
	EpochEntry entry1;
	int qType;
	long qFlags;
	synchronized (session) {
		question = (Question) session.getAttribute("qBuffer");
		entry1 = (EpochEntry) session.getAttribute("entry");
		qType = MathUtils.parseInt((String) session.getAttribute("qType"));
		qFlags = MathUtils.parseLong((String) session.getAttribute("qFlags"));
	}
	final QDatum[] qData = question.getQData(Question.GENERAL);

	final String evalNumStr = request.getParameter("evalNum");
	if (evalNumStr == null || question == null) {
		Utils.alwaysPrint(SELF + ": no ", 
				evalNumStr == null ? "evalNum" : "question");
	%>
		<jsp:forward page="../errorParam.jsp"/>
	<%
	} // if evalNumStr or question is null

	final int RETURN = 0;
	final int ADD_NEW = 1;
	final int ADD_CLONE = 2;
	final String SELECTED = " selected=\"selected\" ";
	final String CHECKED = " checked=\"checked\" ";
	final String[] EVAL_CODES = EvalManager.EVAL_CODES;

	final int HUMAN_REQD = EvalManager.HUMAN_REQD;

	final boolean masterEdit = entry1.isMasterEdit();
	final String bgColor = (masterEdit ? "#f6edf7" : "#f6f7ed");
	final boolean cloneEdit = "true".equals(request.getParameter("cloneEdit"));
	final boolean isFromQuestionJSP = "true".equals(request.getParameter("virgin"));
	// display only a subset of evaluators for each Q type.
	final int[] allowedEvalConstants = EvalManager.getAllowedEvaluators(qType, qFlags);

	final String responseIndex = request.getParameter("responseIndex");
			// from View Responses
	final String respCorrectnessStr = request.getParameter("responseCorrectness");
	int evalConstant = MathUtils.parseInt(request.getParameter("evalConstant"), -1);
	final int evalNum = MathUtils.parseInt(evalNumStr);
	final int subevalNum = 
			MathUtils.parseInt(request.getParameter("subevalNum"));
	/* Utils.alwaysPrint(SELF + ": qType = ", qType, ", qFlags = ", qFlags,
			", responseIndex = ", responseIndex, 
			", respCorrectnessStr = ", respCorrectnessStr,
			", evalNumStr = ", evalNumStr,
			", evalConstant = ", evalConstant,
	 		", evalNum = ", evalNum, 
			", subevalNum = ", subevalNum,
			", cloneEdit = ", cloneEdit); /**/
	Evaluator inputParentEval = null;
	Subevaluator inputSubeval = null;
	boolean editingParent = true;
	boolean foreignEval = false;
	boolean useInput = true;
	final boolean haveResp = !Utils.isEmpty(responseIndex);
	if (evalNum == 0 || evalConstant >= 0) {
		// Utils.alwaysPrint(SELF + ": new evaluator or resetting type of existing evaluator.");
		if (evalNum != 0) {
			inputParentEval = new Evaluator(question.getEvaluator(evalNum));
			inputSubeval = inputParentEval.getSubevaluator(
					subevalNum == 0 ? 1 : subevalNum);
		} else {
			inputSubeval = new Subevaluator();
			inputParentEval = new Evaluator(inputSubeval);
			inputParentEval.feedback = "";
			inputParentEval.grade = 
					MathUtils.parseDouble(request.getParameter("grade"));
		} // if it's a new evaluator
		editingParent = !inputParentEval.isComplex() || subevalNum == 0;
		useInput = false;
		if (haveResp) { // from View Responses
			// Utils.alwaysPrint(SELF + ": we have come from View Responses.");
			QSet qSet;
			synchronized (session) {
				qSet = (QSet) session.getAttribute("qSet");
			}
			final int respCorrectness = ("correct".equals(respCorrectnessStr) 
					? StoredResponse.CORRECT : StoredResponse.WRONG);
			if (respCorrectness == StoredResponse.CORRECT) {
				inputParentEval.grade = 1;
			} // if evaluator is for correct response
			qSet.loadStoredResponses();
			final StoredResponse[] resps = qSet.getStoredResponses(respCorrectness);
			inputSubeval.molStruct = resps[MathUtils.parseInt(responseIndex)].response;
			if (Question.isLewis(qType) && !inputSubeval.molStruct.contains("Lewis")) {
				// SMILES structure needs to be converted to MOL and rescaled
				final double scaleFactor = 
						((double) LewisMolecule.CANVAS_WIDTH)
							/ ((double) LewisMolecule.MARVIN_WIDTH);
				inputSubeval.molStruct = MolString.convertMol(inputSubeval.molStruct, 
						MolString.MOL, scaleFactor);
			}
			// the default evaluator depends on the qType and qFlags
			if (evalConstant < 0) {
				evalConstant = EvalManager.getDefaultEvalConstant(qType, qFlags, haveResp);
			}
		} else { // no responseIndex; still in evalNum is 0 or evalConstant >= 0 
			// see if the current qFlags are set to preload; if so, get preload 
			if (Question.preload(qFlags) && !Question.isClickableImage(qType)) {
				inputSubeval.molStruct = question.getPreloadMolRegardless();
			}
			if (evalNum != 0) {
				// Utils.alwaysPrint(SELF + "previously existing evaluator whose type has changed.");
				// get previously stored molecule, if any
				final Evaluator prevInputEval = question.getEvaluator(evalNum);
				final Subevaluator prevInputSubeval = prevInputEval.getSubevaluator(1);
				if (!Utils.isEmpty(prevInputSubeval.molStruct)) {
					inputSubeval.molStruct = prevInputSubeval.molStruct;
					if (prevInputSubeval.molName != null) 
						inputSubeval.molName = prevInputSubeval.molName;
				}
			} else if (evalConstant < 0) {
				evalConstant = EvalManager.getDefaultEvalConstant(qType, qFlags, haveResp);
			} // if evalConstant
		} // responseIndex
		inputSubeval.matchCode = EVAL_CODES[evalConstant];
	} else { // evalNum != 0 and evalConstant < 0
		// existing evaluator; get its info
		inputParentEval = new Evaluator(question.getEvaluator(evalNum));
		editingParent = !inputParentEval.isComplex() || subevalNum == 0;
		inputSubeval = inputParentEval.getSubevaluator(
				subevalNum == 0 ? 1 : subevalNum);
		evalConstant = Utils.indexOf(EVAL_CODES, inputSubeval.matchCode);
		/* Utils.alwaysPrint(SELF + ": existing evaluator: "
		 		+ "evalConstant set to ", evalConstant, "; editingParent = ",
				editingParent); /**/
		if (evalConstant < 0) { // unrecognized match code
			useInput = false;
		} else {
			final int evalIndex = 
					Utils.indexOf(allowedEvalConstants, evalConstant);
			if (evalIndex < 0) {
				Utils.alwaysPrint(SELF + ": existing evaluator "
						+ "not a type that is displayed for this Q type");
				foreignEval = true;
				useInput = false;
			} // if evaluator is allowed
		} // if evaluator is known
		if (!useInput) {
			evalConstant = allowedEvalConstants[0];
			// preserve molStruct from changed evaluator, if possible
			final String molStruct = (!Utils.isEmpty(inputSubeval.molStruct) 
					? inputSubeval.molStruct : Question.EMPTY_MRV); 
			final String molName = (inputSubeval.molName != null 
					? inputSubeval.molName : "");
			inputSubeval = new Subevaluator();
			inputSubeval.molStruct = molStruct;
			inputSubeval.molName = molName;
			inputSubeval.matchCode = EVAL_CODES[evalConstant];
			inputParentEval.grade = 
					MathUtils.parseDouble(request.getParameter("grade"));
			inputParentEval.feedback = "";
		} // if shouldn't use input
	} // if new evaluator or new evaluator type
	final User dummyUser = new User();
	// Utils.alwaysPrint(SELF + ": inputSubeval.molStruct = ", inputSubeval.molStruct);
	// Utils.alwaysPrint(SELF + ": done with loadEvaluatorJava.jsp.h");

	// values for selectorFooter.jsp.h
	final boolean editingSubeval = true;
	final int headerHeight = (editingParent ? 80 : 30);
	final int footerHeight = (editingParent ? 135 : 50);

	// constants for OPER_ENGLISH
	final int FEWER = CompareNums.FEWER;
	final int LESSER = CompareNums.LESSER;

	final String VISIBLE = "visible";
	final String HIDDEN = "hidden";

// vim:filetype=jsp
%>
