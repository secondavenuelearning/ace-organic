<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.chem.ChemUtils,
	com.epoch.chem.MolString,
	com.epoch.db.CourseRW,
	com.epoch.db.QSetRW,
	com.epoch.energyDiagrams.EDiagram,
	com.epoch.energyDiagrams.OED,
	com.epoch.energyDiagrams.RCD,
	com.epoch.evals.CombineExpr,
	com.epoch.evals.EvalManager,
	com.epoch.evals.Evaluator,
	com.epoch.evals.Subevaluator,
	com.epoch.genericQTypes.ClickImage,
	com.epoch.genericQTypes.TableQ,
	com.epoch.lewis.LewisMolecule,
	com.epoch.physics.*,
	com.epoch.qBank.CaptionsQDatum,
	com.epoch.qBank.EDiagramQDatum,
	com.epoch.qBank.Figure,
	com.epoch.qBank.QDatum,
	com.epoch.qBank.QSetDescr,
	com.epoch.qBank.Question,
	com.epoch.session.QSet,
	com.epoch.substns.RGroupCollection,
	com.epoch.substns.SubstnUtils,
	com.epoch.synthesis.Synthesis,
	com.epoch.synthesis.SynthStarterRule,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.ArrayList,
	java.util.Collections,
	java.util.HashMap,
	java.util.List,
	java.util.Map"
%>

<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String EDIT = "edit";
	final String DELETE = "delete";
	final String ADDNEW = "Add New";
	final String SELECTED = " selected=\"selected\"";
	final String CHECKED = " checked=\"checked\"";
	final int GENERAL = Question.GENERAL;
	final int SUBSTNS = Question.SUBSTNS;
	final Integer TABLE = Integer.valueOf(Question.TABLE);
	final Integer SYNTHESIS = Integer.valueOf(Question.SYNTHESIS);
	final Integer ORB_E_DIAGRAM = Integer.valueOf(Question.ORB_E_DIAGRAM);
	final Integer RXN_COORD = Integer.valueOf(Question.RXN_COORD);
	final Integer CLICK_IMAGE = Integer.valueOf(Question.CLICK_IMAGE);
	final Integer DRAW_VECTORS = Integer.valueOf(Question.DRAW_VECTORS);
	final Integer EQUATIONS = Integer.valueOf(Question.EQUATIONS);
	final Integer CHOICE = Integer.valueOf(Question.CHOICE);

	/*	qId = "same" use buffer
						or a qId in question store
					(0 for a new question)
			addnew = yes if this is a new question
			addEvaluator = yes; evalMol = <smilesmol>
				if a new add evaluator window is to be opened
	*/
	QSet qSet;
	EpochEntry entry1;
	synchronized (session) {
		qSet = (QSet) session.getAttribute("qSet");
		entry1 = (EpochEntry) session.getAttribute("entry");
	}
	// Utils.alwaysPrint("question.jsp: qSet is ", qSet == null ? "" : "not ", "null.");
	final boolean masterEdit = entry1.isMasterEdit();
	boolean showMessage = request.getParameter("showmessage") != null;

	Question question = null;
	boolean sameQuestion = false;
	String qIdStr = request.getParameter("qId");
	synchronized (session) {
		if (qIdStr == null) {
			qIdStr = (String) session.getAttribute("qId");
		} else {
			session.setAttribute("qId", qIdStr);
		}
	}
	final boolean clone = request.getParameter("clone") != null;
	boolean addNew = request.getParameter("addnew") != null;
	// Utils.alwaysPrint("question.jsp: qId = ", qIdStr);
	int qId = 0;
	if (clone) {
		qId = MathUtils.parseInt(qIdStr);
		// Utils.alwaysPrint("question.jsp: cloning qId = ", qId);
		question = new Question(qSet.getQuestion(qId), !Question.PRESERVE_ID);
		if (request.getParameter("cloneOld") == null) {
			final boolean savedWasNew = "true".equals(request.getParameter("savedWasNew"));
			question.miscMessage = Utils.toString(savedWasNew ? "New question " : "Question ",
					qId, " saved; clone ready for editing.");
			showMessage = true;
		}
		qId = 0;
		synchronized (session) {
			session.setAttribute("qBuffer", question);
			// Utils.alwaysPrint("question.jsp: set qBuffer");
			session.setAttribute("clone", "true");
		}
	} else if (addNew) {
		// Utils.alwaysPrint("question.jsp: new question");
		question = new Question();
		synchronized (session) {
			session.setAttribute("qBuffer", question);
			// Utils.alwaysPrint("question.jsp: set qBuffer");
		}
	} else if ("same".equals(qIdStr)) {
		// Utils.alwaysPrint("question.jsp: same id, from session ");
		synchronized (session) {
			question = (Question) session.getAttribute("qBuffer");
			// Utils.alwaysPrint("question.jsp: got question from qBuffer");
		}
		sameQuestion = true;
		qId = question.getQId();
	} else {
		qId = Integer.parseInt(qIdStr);
		final boolean resetQ = request.getParameter("reset") != null;
		if (resetQ) {
			question = qSet.resetQuestion(qId);
		} else {
			/* Utils.alwaysPrint("question.jsp: qId = ", qId, 
					"; different qId, reloading..."); /**/
			question = qSet.getQuestion(qId);
		} // if reset
		if (question == null) {
			Utils.alwaysPrint("question.jsp: Q is null!");
			%><jsp:forward page="errorQNull.jsp"/><%
		}
		if (question.isCorrupted()) {
			Utils.alwaysPrint("question.jsp: Q is corrupted!");
			%><jsp:forward page="errorQNull.jsp"/><%
		}
		synchronized (session) {
			session.setAttribute("qBuffer", question);
			// Utils.alwaysPrint("question.jsp: set qBuffer");
		}
	} // how to load question

	synchronized (session) {
		addNew = (qId == 0 || addNew || clone
				|| session.getAttribute("clone") != null);
	}
	/* Utils.alwaysPrint("question.jsp: addNew = ", addNew,
			", sameQuestion = ", sameQuestion); /**/
	if (question == null) {
		Utils.alwaysPrint("question.jsp: question is null.");
	}

	// get editables
	String qTypeStr, qFlagsStr, qStmt, book, chapter, bookQNumber, keywords;
	synchronized (session) {
		qTypeStr = (String) session.getAttribute("qType");
		qFlagsStr = (String) session.getAttribute("qFlags");
		qStmt = (String) session.getAttribute("qStmt");
		book = (String) session.getAttribute("book");
		chapter = (String) session.getAttribute("chapter");
		bookQNumber = (String) session.getAttribute("bookQNumber");
		keywords = (String) session.getAttribute("keywords");
	}
	if (qStmt == null || !sameQuestion) qStmt = question.getStatement();
	if (book == null || !sameQuestion) book = question.getBook();
	if (chapter == null || !sameQuestion) chapter = question.getChapter();
	if (bookQNumber == null || !sameQuestion)
		bookQNumber = question.getRemarks();
	int qType = question.getQType();
	long qFlags = question.getQFlags();
	if (sameQuestion || addNew) {
		/* Utils.alwaysPrint("question.jsp: sameQuestion = ", sameQuestion,
				", addNew = ", addNew, 
				", setting qType to ", qTypeStr,
				", or, if that fails, to ", qType,
				", and setting qFlags to ", qFlagsStr,
				", or, if that fails, to ", qFlags); /**/
		qType = MathUtils.parseInt(qTypeStr, qType); 
		qFlags = MathUtils.parseLong(qFlagsStr, qFlags); 
		question.setQType(qType);
		question.setQFlags(qFlags);
	} // if same question or new question
	if (keywords == null || !sameQuestion) keywords = question.getKeywords();
	if (keywords == null || "".equals(keywords)) { 
		final QSetDescr descr = qSet.getQSetDescr();
		keywords = Utils.toString("Other".equals(book) ? question.getChapter()
					: Utils.toString(book, " chapter", question.getChapter()),
				' ', QSetRW.getTopicNameById(descr.topicId), ' ', descr.name);
	} // if keywords are null
	else if ("Other".equals(keywords.substring(0, 5)))
		keywords = keywords.substring(13); // remove "Other chapter"
	
	synchronized (session) {
		session.removeAttribute("qType");
		session.removeAttribute("qFlags");
		session.removeAttribute("book");
		session.removeAttribute("chapter");
		session.removeAttribute("bookQNumber");
		session.removeAttribute("qStmt");
		session.removeAttribute("keywords");
	}

	// use static methods to interpret qType and qFlags because internal value may differ
	final boolean isMarvin = question.isMarvin(qType);
	final boolean isMechanism = question.isMechanism(qType);
	final boolean isSynthesis = question.isSynthesis(qType);
	final boolean isLewis = question.isLewis(qType);
	final boolean isChoice = question.isChoice(qType);
	final boolean isChooseExplain = question.isChooseExplain(qType);
	final boolean isRank = question.isRank(qType);
	final boolean isFillBlank = question.isFillBlank(qType);
	final boolean isNumeric = question.isNumeric(qType);
	final boolean isOED = question.isOED(qType);
	final boolean isRCD = question.isRCD(qType);
	final boolean isText = question.isText(qType);
	final boolean isTable = question.isTable(qType);
	final boolean isClickableImage = question.isClickableImage(qType);
	final boolean isLogicalStmts = question.isLogicalStatements(qType);
	final boolean isDrawVectors = question.isDrawVectors(qType);
	final boolean isEquations = question.isEquations(qType);
	final boolean isFormula = question.isFormula(qType);
	final boolean isOther = !isMarvin && !isLewis && !isMechanism 
			&& !isSynthesis && !isRank && !isChoice && !isChooseExplain 
			&& !isFillBlank && !isNumeric && !isOED && !isRCD && !isText 
			&& !isTable && !isClickableImage && !isLogicalStmts
			&& !isDrawVectors && !isEquations && !isFormula; 
	final boolean chemFormatting = question.chemFormatting(qType, qFlags);
	final boolean showMapping = question.showMapping(qFlags);
	final boolean badValenceInvisible = question.badValenceInvisible(qFlags);
	final boolean lonePairsVisible = question.showLonePairs(qFlags);
	final boolean showRSLabels = question.showRSLabels(qFlags);
	final boolean showNoH = question.showNoHydrogens(qFlags);
	final boolean showHeteroH = question.showHeteroHydrogens(qFlags);
	final boolean showAllH = question.showAllHydrogens(qFlags);
	final boolean showAllC = question.showAllCarbons(qFlags);
	final boolean scramble = question.scrambleOptions(qFlags);
	final boolean exceptLast = question.exceptLast(qFlags);
	final boolean is3D = question.is3D(qFlags);
	final boolean preload = question.preload(qFlags);
	final boolean disallowMultipleResponses = question.disallowMult(qFlags);
	final boolean allowUnranked = question.allowUnranked(qFlags);
	final boolean usesSubstns = question.usesSubstns(qFlags);
	final boolean useSciNotn = question.useSciNotn(qFlags);
	final boolean numsOnly = question.numbersOnly(qFlags);
	final boolean labelOrbs = question.labelOrbitals(qFlags);
	final boolean requireInt = question.requireInt(qFlags);
	final boolean disallowSuperfluousWedges = 
			question.disallowSuperfluousWedges(qFlags);
	final boolean omitConstantsField = question.omitConstantsField(qFlags);
	final boolean hideFromOthers = question.hide(qFlags);
	final int preloadFig = question.getPreloadFig(qFlags);
	
	/* Utils.alwaysPrint("question.jsp: qType value = ", qType, 
			", major type is ", question.getQTypeDescription()); /**/

	final int figureIndex = MathUtils.parseInt(request.getParameter("figureIndex"), 1); 
	final boolean reloadFigs = "true".equals(request.getParameter("reloadFigs"));
	// Utils.alwaysPrint("question.jsp: figure index = ", figureIndex);

	final Figure[] figures = question.getFigures(); 
	final Evaluator[] evals = question.getAllEvaluators();
	final QDatum[][] qData = question.getAllQData();
	final int numEvals = Utils.getLength(evals);
	final int numQDataGeneral = question.getNumQData(GENERAL);
	final int numQDataSubstns = question.getNumQData(SUBSTNS);

	// If addEvaluator is true, we were just at View Responses, and
	// we are now adding an unanticipated response to the evaluators.
	final String addEvalStr = request.getParameter("addans");
	boolean addEvaluator = false;
	int responseIndex = -1;
	int evaluatorMajorIndex = 0;
	if (addEvalStr != null && "yes".equals(addEvalStr)) {
		final String responseIndexStr = request.getParameter("responseIndex");
		final String evaluatorMajorIndexStr =
				request.getParameter("evaluatorMajorIndex");
		if (responseIndexStr != null && evaluatorMajorIndexStr != null) {
			addEvaluator = true;
			responseIndex = MathUtils.parseInt(responseIndexStr);
			evaluatorMajorIndex = MathUtils.parseInt(evaluatorMajorIndexStr);
		}
		/* Utils.alwaysPrint("Was in View Responses; need to add"
				+ " or modify an evaluator; responseIndexStr = ",
				responseIndexStr, ", evaluatorMajorIndexStr = ",
				evaluatorMajorIndexStr); /**/
	} // if addEvalStr
	/* Utils.alwaysPrint("question.jsp: responseIndex = ", responseIndex,
			"; evaluatorMajorIndex = ", evaluatorMajorIndex); /**/

	final String mainColor = (masterEdit ? "#f6edf7" : "#f6f7ed");
	final String rowstyle = "vertical-align:middle; "
			+ "text-align:left; "
			+ "padding-top:0px; "
			+ "padding-bottom:0px; "
			+ "margin-top:0px; "
			+ "margin-bottom:0px; ";
	final String[] allBooks = CourseRW.getAllBooks(); // from course books in db 
	int mviewNum = 0;

	final Map<String, Integer> qTypeNums = new HashMap<String, Integer>();
	int qTypeNum = 0;
	for (final String qTypeName : Question.QTYPE_NAMES) { 
		qTypeNum++;
		qTypeNums.put(qTypeName, Integer.valueOf(qTypeNum));
	} // for each qTypeName
	final List<String> qTypeNames = new ArrayList<String>(qTypeNums.keySet());
	Collections.sort(qTypeNames);
	final int exprFound = question.findSynthCombExpr(); // for synthesis question data

%> 

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<title>ACE Question-authoring Tool</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<style type="text/css">
		#footer {
			position:absolute; 
			bottom:0; 
			left:0;
			width:100%; 
			height:50px; 
			overflow:auto; 
			text-align:right; 
		}

		#qEditorContents {
			position:fixed; 
			top:55px;
			left:0;
			bottom:50px; 
			right:0; 
			overflow:auto; 
		}

		* html body {
			padding:55px 0 50px 0; 
		}

		* html #qEditorContents {
			height:100%; 
		}
	</style>
	<% if (question.hasJmolFigure()) { %>
		<script src="<%= pathToRoot %>js/jmolStart.js" type="text/javascript"></script>
			<!-- the next two resources must be called in the given order -->
		<script src="<%= pathToRoot %>nosession/jsmol/JSmol.min.js" type="text/javascript"></script>
		<script src="<%= pathToRoot %>nosession/jsmol/Jmol2.js" type="text/javascript"></script>
	<% } // if question has a Jmol figure %>
	<% if (question.hasLewisFigure()) { %>
		<script src="<%= pathToRoot %>js/offsets.js" type="text/javascript"></script>
		<script src="<%= pathToRoot %>js/position.js" type="text/javascript"></script>
		<script src="<%= pathToRoot %>js/svgGraphics.js" type="text/javascript"></script>
		<script src="<%= pathToRoot %>js/wz_jsgraphics.js" type="text/javascript"></script>
		<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
	<% } // if question has a Lewis figure %>
	<script type="text/x-mathjax-config">
		// <!-- >
		MathJax.Hub.Config({ TeX: { equationNumbers: {autoNumber: "AMS"} } });
		// -->
	</script>
	<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/equations.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/lewisJS.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/3DTemplatesMJS.js" type="text/javascript"></script>
 	<script src="<%= pathToRoot %>nosession/mathjax/MathJax.js?config=TeX-AMS-MML_HTMLorMML.js" 
			type="text/javascript"></script>
	<script src="https://marvinjs.chemicalize.com/v1/<%= 
			AppConfig.marvinJSLicense %>/client-settings.js"></script>
	<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
	<script src="question.js" type="text/javascript"></script>
	<script type="text/javascript">

	// <!-- >
	<%@ include file="/navigation/menuHeaderJS.jsp.h" %>
	<%@ include file="/js/marvinQuestionConstants.jsp.h" %>
	<%@ include file="/js/otherQuestionConstants.jsp.h" %>

	// constants used in question.js
	var initScramble = <%= scramble %>;
	var initExceptLast = <%= exceptLast %>;
	var initBadValence = <%= badValenceInvisible %>;
	var numEvaluators = <%= numEvals %>;
	var numQDataGeneral = <%= numQDataGeneral %>;
	var numQDataSubstns = <%= numQDataSubstns %>;
	var numFigures = <%= Utils.getLength(figures) %>;
	var firstFigIsImage = <%= Utils.getLength(figures) > 0 
			&& figures[0].hasImage() %>;
	var GENERAL = <%= GENERAL %>;
	var SUBSTNS = <%= SUBSTNS %>;
	var CHECKED = '<%= CHECKED %>';

	function rewriteQdata(qDataTableNum) {
		var qType = getMajorQType();
		var out = new String.builder()
				.append('<table style="width:100%; border-collapse:collapse;" '
					+ 'summary="">'
					+ '<tr style="<%= rowstyle %> border-style:none;">'
					+ '<td class="boldtext big" style="padding-top:10px;">'
					+ '<table summary=""><tr><td>');
		if (qDataTableNum === SUBSTNS) {
			out.append(qType === NUMERIC ?
					'Possible values for <i>x<sub>n<\/sub><\/i>' : 
					'Possible groups for each R<sup><i>n<\/i><\/sup>');
		} else switch (qType) {
			case CHOICE: 
			case CHOOSE_EXPLAIN: 
				out.append('Options'); break;
			case FILLBLANK: out.append('Menu options'); break;
			case RANK: out.append('Items to order'); break;
			case SYNTHESIS: out.append('Permissible starting materials'); break;
			case CLICK_IMAGE:
				out.append('Number and color of marks'); break;
			case DRAW_VECTORS:
				out.append('Color of vectors'); break;
			case NUMERIC: out.append('Units'); break;
			case ORB_E_DIAGRAM: 
			case RXN_COORD: 
			case TABLE: out.append('Size &amp; captions'); break;
			case LOGIC: 
				out.append('Additional acceptable words'); break;
			case EQUATIONS: 
				out.append('Initial<span id="constantsLabel">');
				if (!omitConstantsField(calculateQFlags())) {
					out.append(' constants,');
				}
				out.append('<\/span> equations and reserved variable names'); break;
			default: out.append('Question data');
		} // switch qType
		out.append('<\/td><td class="boldtext big" style="padding-left:30px;">')
				.append(qDataTableNum === SUBSTNS
					? '<%= Utils.toValidJS(makeButton(ADDNEW, 
						"addQDatum(SUBSTNS);")) %>'
					: (qType === TABLE 
							&& numQDataGeneral < <%= TableQ.MAX_QDATA %>) // <!-- >
						|| (qType === CLICK_IMAGE 
							&& numQDataGeneral < <%= ClickImage.MAX_QDATA %>) // <!-- >
						|| (qType === DRAW_VECTORS 
							&& numQDataGeneral < <%= DrawVectors.MAX_QDATA %>) // <!-- >
						|| ([EQUATIONS, LOGIC].contains(qType) 
							&& numQDataGeneral < 1) // <!-- >
						|| ([ORB_E_DIAGRAM, RXN_COORD].contains(qType)
							&& numQDataGeneral < <%= EDiagram.MAX_QDATA %>) // <!-- >
						|| [SYNTHESIS, CHOICE, CHOOSE_EXPLAIN, FILLBLANK, RANK, 
							NUMERIC, OTHER].contains(qType)
					? '<%= Utils.toValidJS(makeButton(ADDNEW, 
						"addQDatum(GENERAL);")) %>'
					: '&nbsp;')
				.append('<\/td><\/tr><\/table><\/td>');
		var cool = true;
		var advice = '';
		if (qDataTableNum === SUBSTNS) {
			advice = (qType === NUMERIC ?
					'Enter one set of values for each <i>x<sub>n<\/sub><\/i> '
						+ 'in the statement.' :
					'Enter one R group definition for each R<sup><i>n<\/i><\/sup> '
						+ 'in Figure 1.');
		} else switch (qType) {
			case NUMERIC: 
				advice = 'Enter as a separate item each unit from which a '
						+ 'student may choose, or enter just one or none.';
				break;	
			case TABLE: 
				advice = 'Enter the number of rows (mandatory) and row captions '
						+ '(optional) in item <%= TableQ.ROW_DATA + 1 %>, and '
						+ 'the same for columns in item <%= TableQ.COL_DATA + 1 %>.'
						+ '<br\/>Enter preload values (optional) in item '
						+ '<%= TableQ.PRELOAD_DATA + 1 %>.';
				break;	
			case ORB_E_DIAGRAM: 
				advice = 'Enter the number of rows; you may also enter '
						+ 'captions for the columns, pulldown menu labels '
						+ 'for molecular orbitals, and values and units '
						+ 'for a scale on the y-axis.';
				break;	
			case RXN_COORD: 
				advice = 'Enter the number of rows and columns; you may '
						+ 'also enter pulldown menu labels for the '
						+ 'minima and maxima and values and units for a '
						+ 'scale on the y-axis.';
				break;	
			case SYNTHESIS: 
				advice = (numQDataGeneral === 0 ? 'Enter at least one rule '
							+ 'about permissible starting materials.'
						: numQDataGeneral === 1 ? 'You may enter more rules '
							+ 'about permissible starting materials.'
						: <%= exprFound == Question.NO_EXPR %> ? 'If you do '
							+ 'not enter an expression that describes how to '
							+ 'combine the rules <br\/>about permissible '
							+ 'starting materials, ACE combines them by OR.'
						: <%= exprFound == Question.TWO_EXPRNS %> ? 'You may '
							+ 'have only one expression that describes <br\/>'
							+ 'how to combine the rules about permissible '
							+ 'starting materials.'
						: <%= exprFound == Question.NOT_LAST_QDATUM %> ? 'The '
							+ 'expression that describes how to combine the '
							+ 'rules <br\/>about permissible starting '
							+ 'materials must be last in the list.'
						: 'You may enter more rules about permissible starting '
							+ 'materials, if you wish.<br\/>Update the '
							+ 'combination expression if you do.');
				cool = numQDataGeneral <= 1 // <!-- >
						|| <%= exprFound == Question.NO_EXPR 
						|| exprFound == Question.LAST_QDATUM %>;
				break;
			default: advice = '&nbsp;';
		} // switch qType
		out.append('<td class="').append(cool ? 'regtext' : 'boldtext')
				.append('" style="color:').append(cool ? 'green' : 'red')
				.append('; padding-left:10px; padding-top:10px; text-align:right;">')
				.append(advice)
				.append('<\/td><\/tr><tr><td colspan="3"><table class="whiteTable" '
					+ 'style="border-collapse:collapse; width:100%" summary="">');
		if ((qDataTableNum === GENERAL && numQDataGeneral === 0) 
				|| (qDataTableNum === SUBSTNS && numQDataSubstns === 0)) {
			out.append('<tr style="height:30px;"><td class="boldtext" style='
					+ '"padding-left:10px; background-color:<%= mainColor %>;">'
					+ 'Press the button above to ');
			if (qDataTableNum === SUBSTNS) {
				out.append(qType === NUMERIC ?
						'add possible values for <i>x<sub>n<\/sub><\/i>' : 
						'add possible shortcut groups to replace each R');
			} else switch (qType) {
				case CHOICE: 
				case CHOOSE_EXPLAIN: 
					out.append('add an option'); break;
				case FILLBLANK: out.append('add a menu option'); break;
				case RANK: out.append('add an item to order'); break;
				case NUMERIC: out.append('add a unit'); break;
				case ORB_E_DIAGRAM:
					out.append('add the number of rows'); break;
				case RXN_COORD:
				case TABLE:
					out.append('add the number of rows and columns'); break;
				case CLICK_IMAGE:
					out.append('choose the number of marks and their color '
							+ '(default is 1 and red)'); break;
				case DRAW_VECTORS:
					out.append('choose a color for the vectors '
							+ '(default is red)'); break;
				case LOGIC:
					out.append('add additional acceptable words'); break;
				case EQUATIONS:
					out.append('add initial equations'); break;
				case SYNTHESIS: 
					out.append('add a rule about permissible starting materials');
					break;
				default: out.append('add question data');
			}
			out.append('.<\/td><\/tr>');
		} else if (qDataTableNum === GENERAL) { 
			// have qData of any type other than R groups or 
			// numeric with variables substitution
	<% 		for (int qdNum = 1; qdNum <= numQDataGeneral; qdNum++) {
				final QDatum qDatum = qData[GENERAL][qdNum - 1];
				// process qDatum description differently depending on question 
				// type, allowing for possibility that user has changed the type
				final Map<Integer, String> qdDescrips = 
						new HashMap<Integer, String>();
				// TABLE
				StringBuilder qdDescripBld = new StringBuilder(); 
				if (isTable) {
					if (qdNum <= TableQ.MIN_QDATA) {
						final CaptionsQDatum tQDatum = new CaptionsQDatum(qDatum);
						qdDescripBld.append(tQDatum.toDisplay(chemFormatting));
					} else qdDescripBld.append("<a href=\"javascript:"
							+ "viewPreloadTable();\">View preload table</a>");
				} // if isTable
				qdDescrips.put(TABLE, qdDescripBld.toString());
				final int numOpts = (numQDataGeneral < TableQ.MIN_QDATA 
						? numQDataGeneral : TableQ.MIN_QDATA);
				// SYNTHESIS
				qdDescripBld = new StringBuilder(); 
				if (isSynthesis) {
					if (qDatum.isSynOkSM()) {
						final SynthStarterRule rule = 
								new SynthStarterRule(qDatum.data);
						Utils.appendTo(qdDescripBld, "... ", 
								Utils.toDisplay(rule.toEnglish(), usesSubstns));
					} else if (qDatum.isSynSMExpression()) {  
						Utils.appendTo(qdDescripBld, 
								"Permissible starting materials: ", 
								CombineExpr.postfixToEnglish(qDatum.data));
					} // if qDatum can be interpreted 
				} // if isSynthesis
				qdDescrips.put(SYNTHESIS, qdDescripBld.toString());
				// ORB_E_DIAGRAM
				qdDescrips.put(ORB_E_DIAGRAM, !isOED ? ""
						: (new EDiagramQDatum(qDatum)).toDisplay());
				// RXN_COORD
				qdDescrips.put(RXN_COORD, !isRCD ? ""
						: (new EDiagramQDatum(qDatum)).toDisplay());
				// CLICK_IMAGE, DRAW_VECTORS
				if (isClickableImage || isDrawVectors) {
					final String[] colorAndMaxMarks = 
							ClickImage.getColorAndMaxMarks(qDatum.data);
					final String numMarks = 
							colorAndMaxMarks[ClickImage.NUM_MARKS];
					final String color = colorAndMaxMarks[ClickImage.COLOR];
					qdDescripBld = new StringBuilder()
							.append("The student may place ");
					if (MathUtils.parseInt(numMarks) < 0) {
						qdDescripBld.append("any number of <b>");
					} else {
						Utils.appendTo(qdDescripBld, "up to <b>", 
								numMarks, ' ');
					} // if any number of marks is allowed
					Utils.appendTo(qdDescripBld, "<span style=\"color:", color, 
							";\">", color, "</span></b> mark(s) on the image.");
					qdDescrips.put(CLICK_IMAGE, qdDescripBld.toString());
					qdDescrips.put(DRAW_VECTORS, 
							Utils.toString("The vectors will be <b><span style=\"color:", 
								qDatum.data, ";\">", qDatum.data, "</span></b>."));
				} else {
					qdDescrips.put(CLICK_IMAGE, "");
					qdDescrips.put(DRAW_VECTORS, "");
				} // isClickableImage or isDrawVectors
				// EQUATIONS
				String varsNotUnits = "";
				if (isEquations) {
					final Equations eqns = new Equations(qDatum.data); 
					qdDescrips.put(EQUATIONS, eqns.getEntriesForMathJax()); 
					varsNotUnits = eqns.getVariablesNotUnitsStr();
				} else qdDescrips.put(EQUATIONS, "");
				// CHOICE, RANK, FILL_BLANK, CHOOSE_EXPLAIN
				qdDescrips.put(CHOICE, qDatum.isText() || qDatum.isMarvin()
						? qDatum.toShortDisplay(chemFormatting) : "");
	%>
				// begin question datum <%= qdNum %>
				out.append('<tr style="height:30px; background-color:<%= 
						qdNum % 2 != 0 ? mainColor : "#ffffff" %>;">');
				// index, move pulldown
				if (qType === TABLE
						&& <%= qdNum - 1 >= TableQ.MIN_QDATA %>) {
					out.append('<td class="boldtext" style='
							+ '"padding-left:5px; text-align:center; width:20px;">'
							+ '<%= qDatum.serialNo %>.&nbsp;<\/td>');		
				} else if (qType === TABLE 
						&& <%= qdNum <= TableQ.MIN_QDATA %>) {
					out.append('<td style="text-align:center; width:10px;">'
							+ '<select onchange='
							+ '"moveQDatum(GENERAL, this, <%= qdNum %>)">');
	<% 				for (int optNum = 1; optNum <= numOpts; optNum++) { %>
						out.append('<option value="<%= optNum %>"<%= optNum == qdNum 
								? SELECTED : "" %>><%= optNum %><\/option>');
	<% 				} // for question data position optNum %>
					out.append('<\/select><\/td>');
				} else if ([ORB_E_DIAGRAM, RXN_COORD,
						CLICK_IMAGE, DRAW_VECTORS, LOGIC].contains(qType)) { 
					out.append('<td class="boldtext" '
							+ 'style="text-align:center; width:10px;">'
							+ '&nbsp;<\/td>');
				} else {
					out.append('<td style="text-align:center; width:10px;">'
							+ '<select onchange='
							+ '"moveQDatum(GENERAL, this, <%= qdNum %>)">');
	<%	 			for (int optNum = 1; optNum <= numQDataGeneral; optNum++) { %>
						out.append('<option value="<%= optNum %>"<%= optNum == qdNum 
								? SELECTED : "" %>><%= optNum %><\/option>');
	<%	 			} // for Question data position optNum %>
					out.append('<\/select><\/td>');
				} // if qType
				// question data description
				out.append('<td class="regtext" '
						+ 'style="width:80%; padding-left:5px;">');
				switch (qType) {
					case TABLE:
						out.append('<%= Utils.toValidJS(
								qdDescrips.get(TABLE)) %>'); break;
					case SYNTHESIS: 
						out.append('<%= Utils.toValidJS(
								qdDescrips.get(SYNTHESIS)) %>'); break;
					case ORB_E_DIAGRAM: 
						out.append('<%= Utils.toValidJS(
								qdDescrips.get(ORB_E_DIAGRAM)) %>'); break;
					case RXN_COORD: 
						out.append('<%= Utils.toValidJS(
								qdDescrips.get(RXN_COORD)) %>'); break;
					case CLICK_IMAGE: 
						out.append('<%= Utils.toValidJS(
								qdDescrips.get(CLICK_IMAGE)) %>'); break;
					case DRAW_VECTORS: 
						out.append('<%= Utils.toValidJS(
								qdDescrips.get(DRAW_VECTORS)) %>'); break;
					case EQUATIONS: 
						out.append('<%= Utils.toValidJS(
								qdDescrips.get(EQUATIONS)) %>');
						var varNames = '<%= Utils.toValidJS(varsNotUnits) %>';
						if (varNames !== '') {
							out.append('<p>Variable names not to be '
										+ 'treated as units: ')
									.append(varNames).append('<\/p>');
						} // if there are reserved variable names
						break;
					default: out.append('<%= Utils.toValidJS(
							qdDescrips.get(CHOICE)) %>');
				} // switch qType
				out.append('<\/td>');
				// edit, clone, delete icons
				out.append('<td style="text-align:center; width:115px;">'
							+ '<table summary=""><tr><td>'
							+ '<%= Utils.toValidJS(makeButtonIcon(EDIT, pathToRoot, 
								"editQDatum(GENERAL, ", qdNum, ")")) %><\/td>')
						.append(qType === SYNTHESIS 
								&& <%= qDatum.data.indexOf('/') == -1 %>
							? '<td style="visibility:hidden;">' : '<td>')
						.append(usesSubstns() || [TABLE, ORB_E_DIAGRAM, RXN_COORD, 
								CLICK_IMAGE, DRAW_VECTORS, LOGIC, EQUATIONS, 
								SYNTHESIS].contains(qType) ? '&nbsp;'
							: '<%= Utils.toValidJS(makeButtonIcon("duplicate", 
								pathToRoot, "cloneQDatum(GENERAL, ", 
								qDatum.serialNo, ")")) %>')
						.append('<\/td>');
				if ((qType !== ORB_E_DIAGRAM || <%= qdNum > OED.MIN_QDATA %>)
						&& (qType !== RXN_COORD || <%= qdNum > RCD.MIN_QDATA %>)
						&& (qType !== TABLE || <%= qdNum > TableQ.MIN_QDATA %>)) {
					out.append('<td><%= Utils.toValidJS(makeButtonIcon(DELETE, 
							pathToRoot, "deleteQDatum(GENERAL, ", qdNum, ")")) 
							%><\/td>');
				} // if qType and qdNum
				out.append('<\/tr><\/table><\/td><\/tr>');
	<%	 	} // for each question datum qdNum 
	%>
		} else { // have R groups qData
	<% 		for (int qdNum = 1; qdNum <= numQDataSubstns; qdNum++) {
				final QDatum qDatum = qData[SUBSTNS][qdNum - 1];
				final StringBuilder namesBld = new StringBuilder().append(' ');
				if (isNumeric) {
					namesBld.append(qDatum.toShortDisplay(chemFormatting));
				} else {
					final Map<Integer, String> rGroupColNamesByIds =
							RGroupCollection.getRGroupCollectionNamesKeyedByIds();
					int[] rGroupColIds = new int[0];
					if (!rGroupColNamesByIds.isEmpty()) {
						rGroupColIds = RGroupCollection.
								getAllRGroupCollectionIdsAlphabetized(
									rGroupColNamesByIds);
						final String[] usedIds = qDatum.data.split(":");
						namesBld.append('(');
						for (int idNum = 0; idNum < usedIds.length; idNum++) {
							Utils.appendTo(namesBld, rGroupColNamesByIds.
										get(Integer.parseInt(usedIds[idNum])), 
									idNum < usedIds.length - 1 ? ", " : ")");
						} // for each usedId
					} // if there are R groups
				} // if isNumeric
	%>
				// begin question datum <%= qdNum %> in R groups block
				out.append('<tr style="height:30px; background-color:<%= 
						qdNum % 2 != 0 ? mainColor : "#ffffff" %>;">'
						+ '<td style="text-align:center; width:10px;">'
						+ '<select onchange='
						+ '"moveQDatum(SUBSTNS, this, <%= qdNum %>)">');
	<%	 		for (int optNum = 1; optNum <= numQDataSubstns; optNum++) { 
	%>
					out.append('<option value="<%= optNum %>"<%= optNum == qdNum 
							? SELECTED : "" %>><%= optNum %><\/option>');
	<% 			} // for question data position optNum 
	%>
				out.append('<\/select><\/td><td class="regtext" '
						+ 'style="width:80%; padding-left:5px;">'
						+ '<%= Utils.toValidJS(isNumeric
							? Utils.toString("<i>x</i><sub>", qdNum, 
								"</sub> may have one of the values: ", 
								namesBld.toString())
							: Utils.toString("R<sup>", qdNum, "</sup>", 
								Utils.toDisplay(namesBld.toString()))) %>'
						+ '<\/td><td style="text-align:center; width:115px;">'
						+ '<table summary=""><tr><td>'
						+ '<%= Utils.toValidJS(makeButtonIcon(EDIT, pathToRoot, 
							"editQDatum(SUBSTNS, ", qdNum, ")")) %><\/td>'
						+ '<td>&nbsp;<\/td>'
						+ '<td><%= Utils.toValidJS(makeButtonIcon(DELETE, 
							pathToRoot, "deleteQDatum(SUBSTNS, ", 
							qdNum, ")")) %><\/td>'
						+ '<\/tr><\/table><\/td><\/tr>');
	<% 		} // for each synthesis R group question datum qdNum 
	%>
		} // if there are question data to display
		out.append('<\/table><\/td><\/tr>');
		if ([RANK, CHOICE, CHOOSE_EXPLAIN, FILLBLANK].contains(qType)) {
			out.append('<tr><td colspan="3" class="regtext" '
						+ 'style="padding-top:5px;">'
						+ '<span style="color:red;"><b>Warning:<\/b><\/span> '
						+ 'If you reorder or delete this question\'s ')
					.append(qType === RANK ? 'items to order' : 'options')
					.append(', be sure to update your evaluators accordingly.'
						+ '<\/td><\/tr>');
		} else if (qType === TABLE) {
			out.append('<tr><td colspan="3" class="regtext" '
					+ 'style="padding-top:5px;">'
					+ '<span style="color:red;"><b>Warning:<\/b><\/span> ');
			if (numQDataGeneral < <%= TableQ.MIN_QDATA %>) { // <!-- >
				out.append('Enter the table\'s dimensions '
						+ 'before you begin to write your evaluators. ');
			} // if lacking dimensions
			if (numQDataGeneral > 0) {
				out.append('If you alter the number of rows or columns, '
						+ 'be sure to update your evaluators accordingly.');
			} // if there are qData
			out.append('<\/td><\/tr>');
		} else if ([ORB_E_DIAGRAM, RXN_COORD].contains(qType)) {
			out.append('<tr><td colspan="3" class="regtext" '
					+ 'style="padding-top:5px;">'
					+ '<span style="color:red;"><b>Warning:<\/b><\/span> ');
			if (numQDataGeneral < <%= EDiagram.MIN_QDATA %>) { // <!-- >
				out.append('Enter the diagram\'s dimensions '
						+ 'before you begin to write your evaluators. ');
			} // if lacking dimensions
			if (numQDataGeneral > 0) {
				out.append('If you alter the number of rows or columns in '
						+ 'the diagram, be sure to update your evaluators '
						+ 'accordingly.');
			} // if there are qData
			out.append('<\/td><\/tr>');
		} // if qType
		out.append('<\/table>'); 
		setInnerHTML('QData' + qDataTableNum, out.toString());
		if (qType === NUMERIC) {
			if (calculateQFlags() && USES_SUBSTNS !== 0) {
				setInnerHTML('statementAdvice', 
						'<br/>enter variables as [[x1]], [[x2]], etc.');
			} else {
				clearInnerHTML('statementAdvice');
			} // if with R groups
		}
		rewriteUniversalDataButtons();
	} // rewriteQdata()

	function rewriteUniversalDataButtons() {
		<% final String toDo = (masterEdit ? "Add or edit " : "View "); %>
		var qType = getMajorQType();
		switch (qType) {
			case SYNTHESIS:
				var out = new String.builder()
						.append('<table summary=""><tr><td><%= Utils.toValidJS(
								makeButton(Utils.toString(toDo, "reaction conditions"), 
								"viewReactions(", masterEdit, ");")) 
							%><\/td><td><%= Utils.toValidJS(makeButton(
								Utils.toString(toDo, "impossible SMs"), 
								"viewBadSMs(", masterEdit, ");")) 
							%><\/td><td><%= Utils.toValidJS(makeButton(
								Utils.toString(toDo, "menu-only reagents"), 
								"viewMenuOnlyRgts(", masterEdit, ");")) 
							%><\/td><td><%= Utils.toValidJS(makeButton(
								Utils.toString(toDo, "functional groups"), 
								"viewFnalGroups(", masterEdit, ");")) 
							%><\/td><\/tr><\/table>');
				setUniversalDataButtons(out.toString());
				break;
			case MARVIN:
			case LEWIS:
			case MECHANISM:
				setUniversalDataButtons('<%= Utils.toValidJS(makeButton(
						Utils.toString(toDo, "functional groups"), 
						"viewFnalGroups(", masterEdit, ");")) %>');
				break;
			case NUMERIC:
				setUniversalDataButtons('<%= Utils.toValidJS(makeButton(
						Utils.toString(masterEdit ? "Edit" : "View", 
							" unit conversion factors"), 
						"viewUnitConversions(", masterEdit, ");")) %>');
				break;
			case EQUATIONS:
				setUniversalDataButtons('<%= Utils.toValidJS(makeButton(
						Utils.toString(masterEdit ? "Edit" : "View", 
							" unit canonicalizations"), 
						"viewCanonicalizedUnits(", masterEdit, ");")) %>');
				break;
			default: setUniversalDataButtons('');
		} // switch
	} // rewriteUniversalDataButtons()

	<% if (evals != null) { %>
		function repaintEvalDescrips() {
			var evalDescrip;
			var element;
			<% final String[] qDataTexts = question.getQDataTexts();
			for (int evalNum = 1; evalNum <= numEvals; evalNum++) { 
				final Evaluator eval = evals[evalNum - 1];
				final List<Subevaluator> subevals =
						eval.getSubevaluators();
				final int numSubevals = subevals.size(); 
				for (int subevalNum = 1; subevalNum <= numSubevals; subevalNum++) { 
					final Subevaluator subeval = subevals.get(subevalNum - 1); 
					final int matchType = subeval.getEvalType(); 
			%>
					evalDescrip = new String.builder();
					evalDescrip.append('<%= Utils.toValidJS(
							isText || isTable || !chemFormatting 
							? subeval.toEnglish(qDataTexts)
							: Utils.toDisplay(subeval.toEnglish(qDataTexts), 
								usesSubstns)) %>');
					if (usesSubstns()) { 
						evalDescrip.append('<%= Utils.toValidJS(
								subeval.getRGroupsExcludedText()) %>');
					} // if Q uses R groups
			<% 		if (subeval.usesPermissibleSMs()) { %>
						evalDescrip.append(' (as defined above)');
			<% 		} // if evaluator uses permissible starting materials %>
					var elemName = 'evalDescrip<%= evalNum %>_<%= 
							eval.isComplex() ? subevalNum : 0 %>';
					setInnerHTML(elemName, evalDescrip.toString());
			<% 	} // for each subevaluator
			} // for each evaluator %>
		} // repaintEvalDescrips()
	<% } // if there are evals %>

	function makeEvalFromViewResponses() {
		<% if (addEvaluator) { %>
			// we were just at View Responses, so load 
			// the new add-evaluator window with the unanticipated response.
			var destBld = new String.builder()
					.append('evaluators/loadEvaluator')
					.append(getLoadEvaluatorSuffix(getMajorQType()))
					.append('.jsp?evalNum=<%= evaluatorMajorIndex %>'
						+ '&responseCorrectness=<%= request.getParameter(
							"responseCorrectness") %>'); 
			<% if (responseIndex != -1) { %>
				destBld.append('&responseIndex=<%= responseIndex %>'); 
			<% } // responseIndex %>
			var destination = destBld.toString();
			// alert(destination);
			setEditables(destination, EVAL_WINDOW);
		<% } // addEvaluator %>
	} // makeEvalFromViewResponses()

	function initLewis() {
		<% if (question.hasLewisFigure()
				&& figures[figureIndex - 1].isLewis()) { %>
			initLewisConstants(
					[<%= LewisMolecule.CANVAS_WIDTH %>, 
						<%= LewisMolecule.CANVAS_HEIGHT %>],
					<%= LewisMolecule.MARVIN_WIDTH %>, 
					['<%= LewisMolecule.PAIRED_ELECS %>',
						'<%= LewisMolecule.UNPAIRED_ELECS %>',
						'<%= LewisMolecule.UNSHARED_ELECS %>' ],
					'<%= LewisMolecule.LEWIS_PROPERTY %>',
					'<%= LewisMolecule.HIGHLIGHT %>');
			updateFigure('<%= pathToRoot %>');
		<% } // if have Lewis figure %>
	} // initLewis()

	<% if (figureIndex > 0 && figureIndex <= figures.length
			&& figures[figureIndex - 1].isJmol()) { %>
		jmolInitialize('<%= pathToRoot %>nosession/jsmol'); 
	<% } // if have Jmol figure %>

	function getPrefersPNG() {
		return <%= user.prefersPNG() %>;
	} // getPrefersPNG()

	// -->
</script>
</head>
<body class="light" style="margin:0px; background-color:white; text-align:center;" 
		onload="initLewis();
			setupOptions(); 
			setFigureSelector(<%= Math.min(figures.length, 4) %>, <%= preloadFig 
				%>, '<%= Utils.toValidHTMLAttributeValue(SELECTED) %>'); 
			repaintEvalDescrips();
			makeEvalFromViewResponses();">
<%@ include file="/navigation/menuHeaderHtmlNoTranslate.jsp.h" %>

<div id="qEditorContents">
<form name="saveEditablesForm" action="saveEditables.jsp" method="post" accept-charset="UTF-8">
	<input type="hidden" name="qType" value="<%= qType %>" />
	<input type="hidden" name="qFlags" value="<%= qFlags %>" />
	<input type="hidden" name="book" value="<%= 
			Utils.toValidHTMLAttributeValue(book) %>" />
	<input type="hidden" name="chapter" value="<%= 
			Utils.toValidHTMLAttributeValue(chapter) %>" />
	<input type="hidden" name="bookQNumber" value="<%= 
			Utils.toValidHTMLAttributeValue(bookQNumber) %>" />
	<input type="hidden" name="qStmt" value="<%= 
			Utils.toValidHTMLAttributeValue(qStmt) %>" />
	<input type="hidden" name="keywords" value="<%= 
			Utils.toValidHTMLAttributeValue(keywords) %>" />
	<input type="hidden" name="destination" value="" />
</form>

<form name="questionForm" action="dummy" method="post" accept-charset="UTF-8">

<div id="msgTable">
<% if (showMessage) { %>
	<table class="whiteTable"
		style="width:100%; background-color:<%= mainColor %>;" summary="">
	<tr><td style="text-align:left;">
		<b>Message</b> [<a href="javascript:clearMessage()">clear</a>]:
		<font size="2"><%= Utils.toDisplay(question.miscMessage) %></font>
	</td></tr>
	</table>
<% } %>
</div>

<!-- General question information and properties -->

<table style="width:95%; margin-left:auto; margin-right:auto;" summary="">
<tr class="boldtext big" style="<%= rowstyle %>">
	<td>
		Question type
	</td><td>
		<span id="qPropTitle">Question properties</span>
	</td><td colspan="2">
		<span id="usesApplet"></span>
	</td><td style="padding-left:10px;">
		Question source
	</td>
</tr><tr class="regtext" style="<%= rowstyle %>">
	<td style="width:20%;">
		<select id="qType" name="qType"
				onchange="hideMsgTable(); calculateQFlags(); setupOptions();">
			<% for (final String qTypeName : qTypeNames) { 
				qTypeNum = qTypeNums.get(qTypeName).intValue(); %>
				<option value="<%= qTypeNum %>" <%= 
						question.isMajorQType(qTypeNum) ? SELECTED : "" %> >
					<%= qTypeName %>
				</option>
			<% } // for each question type %>
			<option value="<%= Question.OTHER %>" <%= isOther ? SELECTED : "" %> >
				Other
			</option>
		</select>
	</td><td id="qProperties1" style="margin-right:0px; width:20%;">
		<% if (isChoice || isChooseExplain) { %>
			<span id="disallowMult">
				<input type="checkbox" name="disallowMult"
						onclick="hideMsgTable();"
						<%= disallowMultipleResponses ? CHECKED : "" %> /> 
				disallow multiple responses
			</span>
		<% } else if (isRank) { %>
			<span id="allowUnranked">
				<input type="checkbox" name="allowUnranked"
						onclick="hideMsgTable();" <%= allowUnranked ? CHECKED : "" %> /> 
				allow unnumbered items
			</span>
		<% } else if (isTable) { %>
			<span id="numsOnly">
				<input type="checkbox" name="numsOnly"
						onclick="hideMsgTable();" <%= numsOnly ? CHECKED : "" %> /> 
				numerical entries only
			</span>
		<% } else if (isOED) { %>
			<span id="labelOrbs">
				<input type="checkbox" name="labelOrbs"
						onclick="hideMsgTable();" <%= labelOrbs ? CHECKED : "" %> /> 
				enable orbital labeling
		<% } else if (isMarvin || isMechanism || isSynthesis) { %>
			<span id="useSubstns">
				<input type="checkbox" name="useSubstns"
						onclick="hideMsgTable(); repaintEvalDescrips();
							if (this.checked) rewriteQdata(SUBSTNS);
							else collapseQdata(SUBSTNS);" 
					<%= usesSubstns ? CHECKED : "" %> /> 
				uses R groups
			</span>
		<% } else if (isNumeric) { %>
			<span id="useSubstns">
				<input type="checkbox" name="useSubstns"
						onclick="hideMsgTable(); repaintEvalDescrips();
							if (this.checked) rewriteQdata(SUBSTNS);
							else collapseQdata(SUBSTNS);" 
					<%= usesSubstns ? CHECKED : "" %> /> 
				substitutes values for variables
			</span>
		<% } // if question type %>
	</td><td style="margin-right:0px; width:20%;">
		<span id="withPreload">
			<input type="checkbox" name="withPreload"
					onclick="hideMsgTable(); 
					if (this.checked) showOption('preloadFig');
					else hideOption('preloadFig');" 
					<%= preload ? CHECKED : "" %> /> 
			preload <span id="vectorsOrFigure"><%= isDrawVectors 
					? "vectors" : "figure" %></span>
		</span>
		<span id="figureSelector"></span>
	</td><td style="margin-left:0px; width:20%;">
		<span id="withLonePairs">
			<input type="checkbox" name="withLonePairs"
					onclick="hideMsgTable(); updateFigure('<%= pathToRoot %>');"
					<%= lonePairsVisible ? CHECKED : "" %> /> 
			show lone pairs
		</span>
	</td><td style="width:20%; padding-left:10px;">
		<span class="boldtext">Book:&nbsp;</span> 
		<select id="booktag" name="book"
				onchange="hideMsgTable(); setSourceTags();">
			<% for (final String bk : allBooks) { %>
				<option value="<%= Utils.toValidHTMLAttributeValue(bk) %>"
					<%= book.equals(bk) ? SELECTED : "" %> >
				<%= bk %>
				</option>
			<% } // for each book %>
			<option value="Literature" <%= "Literature".equals(book) ? SELECTED : "" %>> 
			Literature 
			</option>
			<option value="Other" <%= "Other".equals(book) ? SELECTED : "" %>> 
			Other 
			</option>
		</select>
	</td>
</tr><tr class="regtext" style="<%= rowstyle %>">
	<td <%= !masterEdit ? "style=\"visibility:hidden;\"" : "" %>>
		<input type="checkbox" name="hideFromOthers"
				onclick="hideMsgTable();" <%= hideFromOthers ? CHECKED : "" %> /> 
		hide from other instructors
	</td><td id="qProperties2">
		<% if (isChoice || isChooseExplain || isRank || isFillBlank) { %>
			<span id="scramble">
				<input type="checkbox" name="scramble" 
						onclick="hideMsgTable();" <%= scramble ? CHECKED : "" %> /> 
				scramble <%= isFillBlank ? "menu items"
						: isRank ? "items to order" 
						: "options"  %>
			</span>
		<% } else if (isNumeric) { %>
			<span id="requireInt">
				<input type="checkbox" name="requireInt"
						onclick="hideMsgTable();" <%= requireInt ? CHECKED : "" %> /> 
				integral responses only
			</span>
		<% } else if (isTable) { %>
			<span id="requireInt">
				<input type="checkbox" name="requireInt"
						onclick="hideMsgTable();" <%= requireInt ? CHECKED : "" %> /> 
				integral entries only
			</span>
		<% } else if (isMarvin) { %>
			<span id="badValenceInvisible">
				<input type="checkbox" name="badValenceInvisible"
						onclick="hideMsgTable(); updateFigure('<%= pathToRoot %>');"
						<%= badValenceInvisible ? CHECKED : "" %> /> 
				don't highlight bad valences
			</span>
		<% } // if question type %>
	</td><td>
		<span id="withMapping">
			<input type="checkbox" value="true" name="withMapping"
					onclick="hideMsgTable(); updateFigure('<%= pathToRoot %>');" <%= 
						showMapping ? CHECKED : "" %> /> 
			show mapping
		</span>
	</td><td style="margin-right:0px;">
		<span id="howShowHydrogens">
			<input type="checkbox" value="true" name="howShowHydrogens"
					onclick="hideMsgTable(); updateFigure('<%= pathToRoot %>');" 
					<%= showNoH || showHeteroH || showAllH || showAllC ? CHECKED : "" %> /> 
			show 
				<select name="allNo" onchange="updateFigure('<%= pathToRoot %>');">
					<option value="<%= Question.SHOWNOH %>"
							<%= showNoH ? SELECTED : "" %>>explicit H atoms only</option>
					<option value="<%= Question.SHOWHETEROH %>"
							<%= showHeteroH ? SELECTED : "" %>>heteroatom H atoms</option>
					<option value="<%= Question.SHOWALLH %>"
							<%= showAllH ? SELECTED : "" %>>all H atoms</option>
					<option value="<%= Question.SHOWALLH | Question.SHOWALLC %>"
							<%= showAllC ? SELECTED : "" %>>all H and C atoms</option>
				</select>
		</span>
	</td><td style="margin-left:0px; padding-left:10px;">
		<span class="boldtext" id="chaptertag"></span>:&nbsp; 
		<input type="text" name="chapter" 
				style="width:80px;"
				onchange="hideMsgTable();" value="<%= 
						Utils.toValidTextbox(chapter) %>" />
	</td>
</tr><tr class="regtext" style="<%= rowstyle %>">
	<td id="qProperties3">
		<% if (!isMarvin && !isMechanism) { %>
			<span id="noChemFormatting">
				<input type="checkbox" value="true" name="noChemFormatting"
						onclick="hideMsgTable();" <%= !chemFormatting ? CHECKED : "" %> /> 
				eschew chemistry formatting of text
			</span>
		<% } // if neither Marvin nor mechanism %>
	</td><td id="qProperties4">
		<% if (isEquations) { %>
			<span id="omitConstantsField">
				<input type="checkbox" value="true" name="omitConstantsField"
						onclick="hideMsgTable();" <%= omitConstantsField ? CHECKED : "" %> /> 
				omit constants field
			</span>
		<% } else if (isNumeric) { %>
			<span id="useSciNotn">
				<input type="checkbox" name="useSciNotn"
						onclick="hideMsgTable();" <%= useSciNotn ? CHECKED : "" %> /> 
				use scientific notation
			</span>
		<% } else { %>
			<span id="disallowSuperfluousWedges">
				<input type="checkbox" value="true" name="disallowSuperfluousWedges"
						onclick="hideMsgTable();" <%= disallowSuperfluousWedges ? CHECKED : "" %> /> 
				disallow unnecessary wedge bonds
			</span>
		<% } // if isEquations %>
	</td><td style="margin-right:0px;">
		<span id="threeDim">
			<input type="checkbox" value="true" name="threeDim"
					onclick="hideMsgTable();" <%= is3D ? CHECKED : "" %> /> 
			uses 3D conformations 
		</span>
	</td><td style="margin-left:0px;">
		<span id="withRSLabels">
			<input type="checkbox" value="true" name="withRSLabels"
					onclick="hideMsgTable(); updateFigure('<%= pathToRoot %>');" <%= 
						showRSLabels ? CHECKED : "" %> /> 
			show R,S labels
		</span>
	</td><td style="padding-left:10px;">
		<span id="bookQNumberTag" class="boldtext"></span>:&nbsp; 
		<input type="text" name="bookQNumber" 
				style="width:120px;"
				onchange="hideMsgTable();" value="<%= 
						Utils.toValidTextbox(bookQNumber) %>" />
	</td>
</tr>

<!-- keywords -->
<tr style="padding-top:10px;">
	<td colspan="5">
		<table style="width:100%;" summary="">
		<tr><td class="boldtext big" style="vertical-align:bottom; padding-top:15px; 
			padding-left:0px; margin-left:0px; <%= rowstyle %>">
		Keywords:
		</td><td style="padding-left:10px; width:100%;">
		<input type="text" name="keywords" 
				style="width:95%;"
				onchange="hideMsgTable();" value="<%= 
						Utils.toValidTextbox(keywords) %>"/>
		</td></tr>
		</table>	
	</td>
</tr>

<!-- Question statement and figures -->

<tr class="boldtext big" style="<%= rowstyle %>">
	<td colspan="3" style="margin-right:0px;">
		<%= qId != 0 ? Utils.toString("Question #", Utils.formatNegative(qId)) : "New Question" %>
		Statement
		<span class="regtext" style="color:green;" id="statementAdvice">
		<%= isFillBlank ? "&nbsp;&nbsp;&nbsp;pulldown menu syntax: [[1, 2, 5]]" 
				: isNumeric && usesSubstns
				? "&nbsp;&nbsp;&nbsp;enter variables as [[x1]], [[x2]], etc."
				: "" %>
		</span>
	</td>
	<td colspan="2" style="margin-left:0px;">Figures</td>
</tr>
<tr>
	<td colspan="3" id="statementTextBox"
			style="width:50%; height:100%; background-color:<%= mainColor %>;
			padding-left:10px; padding-right:10px; padding-top:10px;
			padding-bottom:10px; vertical-align:top; text-align:center; 
			border-style:solid; border-width:1px; border-color:black; margin-right:0px;">
		<textarea name="statement" id="statement" rows="1" cols="10"
				style="height:180px; width:100%;"
				onchange="hideMsgTable();"><%= Utils.toValidTextbox(qStmt) %></textarea>
	</td>
	<td colspan="2" style="width:50%; height:100%; 
			background-color:<%= mainColor %>;
			padding-left:10px; padding-right:10px; padding-top:10px;
			padding-bottom:10px; vertical-align:top; border-style:solid; 
			border-width:1px; border-color:black; margin-left:0px;">
		<table style="width:90%;" summary="">
		<tr style="width:100%;">
		<td class="regtext" style="vertical-align:middle; text-align:left;">
			<% final int numFigs = figures.length;
			if (numFigs > 0) { %> 
				<table summary="">
				<tr><td class="boldtext">
					<% if (numFigs > 1) { %>
						<select name="figurelist" onchange="calculateQFlags(); 
								changeSelection(<%= reloadFigs %>)">
						<% for (int figNum = 1; figNum <= numFigs; figNum++) { %>
							<option value="<%= figNum %>"
								<%= figureIndex == figNum ? SELECTED : "" %> > 
								Figure&nbsp;<%= figNum %> 
							</option>
						<% } // for figNum %>
						</select>
						of <%= numFigs %>
					<% } else { %>
						Figure 1
					<% } // if there is > 1 figure %>
				</td><td style="padding-left:10px;">
					<%= makeButtonIcon(EDIT, pathToRoot, "editFigure();") %>
				</td><td>
					<%= makeButtonIcon(DELETE, pathToRoot, "deleteFigure();") %>
				</td></tr>
				</table>
			<% } else { %>
				[None]
			<% } // if numFigs %>
		</td><td> 
			<table style="margin-left:auto; margin-right:0px;" summary="">
			<tr><td>
				<%= makeButton(ADDNEW, "addFigure();") %>
			</td></tr>
			</table>
		</td>
		</tr>
		<% if (figureIndex <= numFigs) {
			final Figure figure = figures[figureIndex - 1]; %>
			<tr style="vertical-align:top;">
				<td colspan="2">
					<%@ include file="dispFigure.jsp.h" %> 
				</td>
			</tr> 
		<% } // if figureIndex %>
		</table>
	</td>
</tr>
</table>

<!-- Question Data -->

<% for (int qDataTableNum = 0; qDataTableNum < Question.NUM_QDATA_LISTS; qDataTableNum++) { 
	// Data in tables are dynamically generated and displayed by the function  
	// rewriteQdata() and erased by the function collapseQdata(). 
	// Second table used only for multistep synthesis questions that use R groups.
%>
	<table style="width:95%; margin-left:auto; margin-right:auto;
			border-collapse:collapse;" summary="">
		<tr><td id="QData<%= qDataTableNum %>">
		</td></tr>
	</table>
<% } // for each kind of qData table %>

<!-- Evaluators -->

<table style="width:95%; text-align:center; margin-left:auto; 
	margin-right:auto; border-collapse:collapse;" summary="">
<tr>
<td style="vertical-align:middle; text-align:left; width:100%; padding-top:10px;">
	<table style="border-collapse:collapse; width:100%; margin-left:0px;
			margin-right:0px;" summary="">
	<tr> 
	<td class="boldtext big" style="vertical-align:middle; width:20%">
		Evaluators
	</td> 
	<td style="vertical-align:middle; text-align:left; 
			width:20%; padding-left:10px;">
		<%= makeButton(ADDNEW, "addEval();") %>
	</td><td>
		<table style="margin:0px;" align="right" summary="">
		<tr><td><%= makeButton("Split", "splitEval();") %>
		</td><td><%= makeButton("Combine", "joinEvals();") %>
		</table>
	</td></tr>
	</table>
</td>
</tr><tr>
<td colspan="3">
	<table class="whiteTable" style="border-collapse:collapse; width:100%;
			vertical-align:top;" summary="">
	<% if (numEvals == 0) { %>
		<tr style="height:30px;">
			<td class="boldtext" style="text-align:left; padding-left:10px;
					background-color:<%= mainColor %>;">
				Press the button above to add an evaluator.
			</td>
		</tr>
	<% } // if there are evaluators %>
<% 
	for (int evalNum = 1; evalNum <= numEvals; evalNum++) { 
		final Evaluator eval = evals[evalNum - 1];
%>
	<!-- Begin evaluator <%= evalNum %> -->
		<tr style="height:30px; vertical-align:top; background-color:<%= 
				evalNum % 2 != 0 ? mainColor : "#ffffff" %>;">

	<!-- first column: index -->
		<td class="boldtext" 
				style="text-align:center; width:10px; vertical-align:middle;"> 
			<select onchange="moveEval(this, <%= evalNum %>)"> 
				<% for (int eNum = 1; eNum <= numEvals; eNum++) { %>
					<option value="<%= eNum %>"<%= eNum == evalNum 
							? SELECTED : "" %>><%= eNum %></option>
				<% } // for evaluator position eNum %>
			</select>
		</td>

	<!-- second column: correct/partial/wrong/human -->
		<td class="boldtext" style="text-align:center; width:15px; 
				padding-top:5px; vertical-align:middle;"> 
			<% final boolean isHumanReqd = eval.isHumanGradingReqd();
			final boolean calcsGrade = eval.calculatesGrade();
			final String color = (isHumanReqd || calcsGrade ? "gray"
					: eval.grade == 1.0 ? "#008800;" 
					: eval.grade == 0.0 ? "#CC0000;"
					: "#0000CC;"); %> 
			<span class="boldtext" style="color:<%= color %>"><%= 
					isHumanReqd ? "[?]"
					: calcsGrade ? "<i>x</i>%"
					: eval.grade == 1.0 ? "[C]" 
					: eval.grade == 0.0 ? "[W]"
					: Utils.toString((int) (eval.grade * 100), "%") %></span>
		</td>

	<!-- third column: description or expression for combination of subevaluators -->
		<td style="text-align:left; width:40%; padding-right:10px; 
				padding-left:5px; padding-top:5px; vertical-align:middle;">
			<% if (eval.isComplex()) { %>
				<span class="regtext"><%= eval.exprCodeToEnglish(evalNum) %></span>
			<% } else { %>
				<span class="regtext" id="evalDescrip<%= evalNum %>_0"></span>
			<% } // evaluator is complex %>
		</td>

	<!-- fourth column: feedback -->
		<td style="text-align:left; width:40%; padding-top:5px; 
				vertical-align:middle;">
			<%= eval.feedback == null ? ""
					: Utils.chopDisplayStr(chemFormatting 
							? Utils.toDisplay(eval.feedback)
							: eval.feedback, 
						60) %> 
		</td>

	<!-- fifth column: edit, duplicate, delete icons -->
		<td style="text-align:center; width:70px;">
			<table summary="">
			<tr><td style="width:33%">
				<%= makeButtonIcon(EDIT, pathToRoot, eval.isComplex() 
						? Utils.toString("openJoinedEval(", evalNum, ");")
						: Utils.toString("openEval(", evalNum, ", 0);")) %>
			</td><td>
				<%= makeButtonIcon("duplicate", pathToRoot, eval.isComplex() 
						? "cloneJoinedEval(" : "cloneEval(", evalNum, ");") %>
			</td><td>
				<%= makeButtonIcon(DELETE, pathToRoot, "deleteEval(", evalNum, ");") %>
			</td></tr>
			</table>
		</td>

	<!-- sixth column: split/combine check box -->
		<td style="text-align:center; width:10px; padding-top:5px;">
			<input type="checkbox" name="all_checker" value="<%= evalNum %>" />
		</td>
		</tr>

		<% if (eval.isComplex()) { // write out subevaluators
			final int numSubevals = eval.getNumSubevaluators();
			for (int subevalNum = 1; subevalNum <= numSubevals; subevalNum++) {
		%>
				<tr style="height:30px; vertical-align:top; background-color:<%= 
						evalNum % 2 != 0 ? mainColor : "#ffffff" %>;">
				<!-- first column: empty -->
				<td></td>
				<!-- second column: index -->
				<td class="regtext" style="vertical-align:middle;">
					<b><%= evalNum %>.<%= subevalNum %></b>
				</td>
				<!-- third column: description -->
				<td colspan="2" style="text-align:left; width:40%; padding-right:10px; 
						padding-left:5px; padding-top:5px; vertical-align:middle;">
					<span class="regtext" id="evalDescrip<%= 
							evalNum %>_<%= subevalNum %>"></span>
				</td>
				<!-- fifth column: edit icon -->
				<td colspan="2" style="text-align:left; padding-left:4px; width:70px;">
					<%= makeButtonIcon(EDIT, pathToRoot, 
							"openEval(", evalNum, ", ", subevalNum, ");") %>
				</td>
				</tr>
			<% } // for each subevaluator
		} // if evaluator is complex
 	} // for each evaluator %>
	</table>
</td>
</tr>
<tr><td colspan="6" id="editUniversalData" style="text-align:left; padding-top:10px;"></td></tr> 
</table>

</form>
</div>
<div id="footer">
<form name="actionForm" method="post" action="dbactions.jsp" accept-charset="UTF-8">
	<input type="hidden" name="statement" /> 
	<input type="hidden" name="qType" /> 
	<input type="hidden" name="qFlags" /> 
	<input type="hidden" name="book"/>
	<input type="hidden" name="chapter"/>
	<input type="hidden" name="bookQNumber"/>
	<input type="hidden" name="keywords"/>
	<input type="hidden" name="action" />
<table style="margin-top:10px; margin-left:auto; margin-right:auto; 
		border-collapse:collapse;" summary="">
	<tr>
	<% final int qSetId = qSet.getQSetId(); 
	if (addNew) { %>
		<td style="padding-left:30px;">
			<%= makeButton("Preview", "preview(", qSetId, ", ", masterEdit, ");") %></td>
		<td><%= makeButton("Save", "addContinue();") %></td>
		<td><%= makeButton("Save and Add New", "saveContinue();") %></td>
		<td><%= makeButton("Save and Duplicate", "saveAndClone(", qId, ");") %></td>
		<td><%= makeButton("Save and Exit", "saveReturn();") %></td>
		<td style="padding-right:30px;">
			<%= makeButton("Cancel", "returnUserMain(", qSetId, ");") %></td>
	<% } else { %>
		<td style="padding-left:30px;">
			<% final int prevQId = qSet.getPrevQId();
			if (prevQId != 0) { %>
				<%= makeButtonIcon("back", pathToRoot, "movePrev(", prevQId, ");") %>
			<% } else { %>
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
			<% } // if not the first question %>
		</td><td>
			<table style="width:100%; border-collapse:collapse" summary="">
				<tr><td class="regtext">
				</td><td><select name="jumpvalue" onchange="jump()">
					<% for (int qNum = 1; qNum <= qSet.getCount(); qNum++) { %>
						<option value="<%= qNum %>" <%= 
							qId == qSet.getQId(qNum) ? SELECTED : "" %>>
						<%= qNum %> </option>
					<% } // for each Q in the set %>
				</select></td></tr>
			</table>
		</td><td>
			<% final int nextQId = qSet.getNextQId();
			if (nextQId != 0) { %>
				<%= makeButtonIcon("next", pathToRoot, "moveNext(", nextQId, ");") %>
			<% } else { %>
				&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
			<% } // if not the last question %>
		</td>
		<td><%= makeButton("View Responses", "viewResponses();") %></td>
		<td><%= makeButton("Preview", "preview(", qSetId, ", ", masterEdit, ");") %></td>
		<td><%= makeButton("Reset", "resetQ();") %></td>
		<td><%= makeButton("Save Changes", "saveChanges();") %></td>
		<td><%= makeButton("Save and Duplicate", "saveAndClone(", qId, ");") %></td>
		<td><%= makeButton("Save and Exit", "saveExit();") %></td>
		<td style="padding-right:30px;">
			<%= makeButton("Cancel", "returnUserMain(", qSetId, ");") %>
		</td>
	<% } // if addNew %>
	</tr>
</table>
</form>
</div>
</body>
</html>
