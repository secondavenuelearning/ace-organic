<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	chemaxon.struc.DPoint3,
	com.epoch.chem.MolString,
	com.epoch.lewis.LewisMolecule,
	com.epoch.physics.DrawVectors,
	com.epoch.qBank.Figure,
	com.epoch.qBank.QDatum,
	com.epoch.qBank.Question,
	com.epoch.synthesis.RxnCondition,
	com.epoch.synthesis.Synthesis,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.Map"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%@ include file="/js/rxnCondsJava.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

	final String pathToRoot = "../../";
	final String pathToChooseRxnCondsUser = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	Question question;
	int qType;
	long qFlags;
	synchronized (session) {
		question = (Question) session.getAttribute("qBuffer");
		qType = MathUtils.parseInt((String) session.getAttribute("qType"));
		qFlags = MathUtils.parseLong((String) session.getAttribute("qFlags"));
	}

	if (question == null) {
		Utils.alwaysPrint("loadFigure.jsp: question is null");
	%>
		<jsp:forward page="../errorParam.jsp"/>
	<%
	}

	final int REACTION = Figure.REACTION;
	final int MOLECULE = Figure.MOLECULE;
	final int LEWIS = Figure.LEWIS;
	final int SYNTHESIS = Figure.SYNTHESIS;
	final int IMAGE = Figure.IMAGE;
	final int JMOL = Figure.JMOL;
	final int MRV_TXT = Figure.MRV_TXT;
	final int IMAGE_AND_VECTORS = Figure.IMAGE_AND_VECTORS;
	final String SELECTED = "selected=\"selected\" ";

	String data = Question.EMPTY_MRV;

	final int figNum = MathUtils.parseInt(request.getParameter("figNum"));
	int figType = MOLECULE;
	Figure figure = new Figure();
	if (figNum == 0) { // default figType for new addition is based on qType and qFlags
		if (Question.isLewis(qType)) {
			figType = LEWIS;
		} else if (Question.isDrawVectors(qType)) {
			figType = IMAGE_AND_VECTORS;
			data = "";
		} else if (Question.isClickableImage(qType)
				|| Question.isEquations(qType)) {
			figType = IMAGE;
			data = "";
		} else if (Question.isMarvin(qType)
				&& !Question.showMapping(qFlags) 
				&& !Question.is3D(qFlags)) {
			figType = REACTION;
		} // if qType or qFlags
	} else { // existing figure
		figure = question.getFigure(figNum);
		figType = figure.type;
		if (figType != IMAGE) data = figure.data;
	} // figNum
	
	// are we reloading the same figure with a new type?
	figType = MathUtils.parseInt(request.getParameter("figType"), figType);
	/* Utils.alwaysPrint("loadFigure.jsp: figNum = ", figNum, ", figType = ",
			figType, ", data:\n", data == null ? "" 
				: data.replaceAll("\n", "\\\\n").replaceAll("\r", "\\\\r"));
	*/

	String srcFile = null;
	final String uploadedfile = request.getParameter("uploadedfile");
	final String calcResults = request.getParameter("calcResults");
	if (calcResults != null) {
		/* Utils.alwaysPrint("loadFigure.jsp: calcResults:\n", calcResults); /**/
		data = calcResults;
	}
	if (uploadedfile != null) {
		srcFile = uploadedfile;
	} else if (figNum != 0 
			&& Utils.among(figType, IMAGE, IMAGE_AND_VECTORS)
			&& figure.hasImage()) {
		srcFile = figure.bufferedImage;
	}
	final boolean isUpload = Utils.among(figType, IMAGE, IMAGE_AND_VECTORS, JMOL);
	/* Utils.alwaysPrint("loadFigure.jsp: uploadedfile = '", uploadedfile,
			"', srcFile = '", srcFile, "'."); /**/

	// for SYNTHESIS figures
	Map<Integer, String> reactionNamesByIds = null;
	String[] chosenRxns = new String[0];
	final boolean onlyOneRxnCondn = false;
	if (figType == SYNTHESIS) {
		final String rxnIdsStr = Synthesis.getRxnConditions(figure.data);
		// Utils.alwaysPrint("loadFigure.jsp: rxnIdsStr = ", rxnIdsStr);
		if (!Utils.isEmpty(rxnIdsStr)) {
			chosenRxns = rxnIdsStr.split(Synthesis.RXN_ID_SEP);
		} // if there are selected synthesis reactions
		reactionNamesByIds = RxnCondition.getRxnNamesKeyedByIds();
	} // if SYNTHESIS
	if (figure.addlData == null) figure.addlData = "";
	if (Utils.among(figType, MOLECULE, REACTION, SYNTHESIS)) {
		data = MolString.updateFormat(data);
	} // if data is a molecule

	int jmolNum = 0;
	final String APPLET_NAME = "responseApplet";
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<title>ACE Figure Editor</title>
<style type="text/css">
body {
	margin:0;
	border:0;
	padding:0;
	height:100%; 
	max-height:100%; 
	font-family:arial, verdana, sans-serif; 
	font-size:76%;
	overflow: hidden; 
}

* html body {
	padding:80px 0 135px 0; 
}
</style>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<% if (Utils.among(figType, REACTION, MOLECULE, SYNTHESIS)) { %>
	<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" 
		type="text/css"/>
	<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/3DTemplatesMJS.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>nosession/marvinJS/js/promise.min.js"></script>
	<script src="https://marvinjs.chemicalize.com/v1/<%= AppConfig.marvinJSLicense %>/client-settings.js"></script>
	<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
	<% if (figType == SYNTHESIS) { %>
		<script src="<%= pathToRoot %>js/rxnCondsEditor.js" type="text/javascript"></script>
	<% } // if figType %>
<% } else if (figType == LEWIS) { %>
	<script src="<%= pathToRoot %>js/lewisJS.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/offsets.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/position.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/svgGraphics.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/wz_jsgraphics.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<% } else if (figType == JMOL) { %>
	<script src="<%= pathToRoot %>js/jmolStart.js" type="text/javascript"></script>
		<!-- the next two resources must be called in the given order -->
	<script src="<%= pathToRoot %>nosession/jsmol/JSmol.min.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>nosession/jsmol/Jmol2.js" type="text/javascript"></script>
<% } else if (figType == IMAGE_AND_VECTORS) { %>
	<script src="<%= pathToRoot %>js/drawOnFig.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/offsets.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/position.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/wz_jsgraphics.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<% } // if figType %>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
<script type="text/javascript">
	// <!-- >

	<%@ include file="/js/marvinQuestionConstants.jsp.h" %>

	function changeFigureType() {
		self.location.href = 'loadFigure.jsp?figNum=<%= figNum %>'
				+ '&figType=' + document.selectorForm.figTypeList.value
				+ '&qType=<%= qType %>&qFlags=<%= qFlags %>';
	}

<% if (isUpload) { %>

	function upload() {
		if (isWhiteSpace(document.fileupload.srcFile.value)) {
			alert('Type or browse to the file that contains the <%=
					Utils.among(figType, IMAGE, IMAGE_AND_VECTORS) 
						? "image" : "Jmol data" %>.');
		} else {
			document.fileupload.submit();
		}
	}

	function warn(kind) {
		alert('Jmol is very intolerant of incorrect ' + kind
				+ '.  Read the documentation before you use them. '
				+ 'If the question-editing page misbehaves '
				+ 'after you change or enter new ' + kind 
				+ ' and apply the changes, it probably means that '
				+ 'you did not format the ' + kind + ' correctly.');
	}

	<% if (figType == JMOL) { %>
		jmolInitialize('<%= pathToRoot %>nosession/jmol'); 
	<% } else if (figType == IMAGE_AND_VECTORS) { 
		final QDatum[] qData = question.getQData(Question.GENERAL); %>
	
 		<%@ include file="/js/drawOnFig.jsp.h" %>

		function initDrawOnFigure() {
			initDrawOnFigConstants();
			initDrawOnFigButtons(
					'<%= Utils.toValidJS(makeButton("Clear last", 
						"clearLast();")) %>',
					'<%= Utils.toValidJS(makeButton("Clear all", 
						"clearAllOfOne();")) %>',
					'<%= Utils.toValidJS(makeButton("Cancel", 
						"unselect();")) %>');
			initDrawOnFigGraphics('<%= Utils.isEmpty(qData) 
						|| !Question.isClickableImage(qType)
						|| !Question.isDrawVectors(qType)
					? "red" : qData[0].data %>');
			setClickPurposeMenu();
			captureClicks();
			var coords = new Array();
			<% if (!Utils.isEmpty(data) && !Question.EMPTY_MRV.equals(data)) {
				final DrawVectors drawVectors = new DrawVectors(data);
				final DPoint3[][] vectors = drawVectors.getVectorPoints();
				for (final DPoint3[] vector : vectors) { %>
					allShapes[<%= DrawVectors.ARROW %>].push(
							[canvasSetX(<%= vector[DrawVectors.ORIGIN].x %>), 
							canvasSetY(<%= vector[DrawVectors.ORIGIN].y %>),
							canvasSetX(<%= vector[DrawVectors.TARGET].x %>), 
							canvasSetY(<%= vector[DrawVectors.TARGET].y %>)]);
			<% 	} // for each vector %>
				paintAll();
			<% } // if there are vectors to display %>
		} // initDrawOnFigure()

	<% } // if Jmol or vectors %>

	function submitIt() {
		<% if (calcResults == null && data == null) { %>
			var anAlert = 'You have not uploaded a file.';
			alert(anAlert);
			return;
		<% } else if (figType == JMOL) { %>
			document.figureForm.figData.value = '<%= Utils.toValidJS(data) %>';
		<% } else if (figType == IMAGE_AND_VECTORS) { %>
			if (allShapes[ARROW].length === 0) {
				alert('Please draw at least one arrow on the figure by '
						+ 'clicking on it in two different places.');
				return;
			} 
			document.figureForm.figData.value = getVectorsXML();
		<% } // if no result to submit or Jmol or vectors %>
		document.figureForm.submit();
	} // submitIt()

<% } else if (figType == MRV_TXT) { %>

	function submitIt() {
		var form = document.figureForm;
		form.figData.value = form.enteredMRV.value;
		form.submit();
	} // submitIt

<% } else if (figType == LEWIS) { %>

	function initLewis() {
		initLewisConstants(
				[<%= LewisMolecule.CANVAS_WIDTH %>, 
					<%= LewisMolecule.CANVAS_HEIGHT %>],
				<%= LewisMolecule.MARVIN_WIDTH %>, 
				['<%= LewisMolecule.PAIRED_ELECS %>',
					'<%= LewisMolecule.UNPAIRED_ELECS %>',
					'<%= LewisMolecule.UNSHARED_ELECS %>' ],
				'<%= LewisMolecule.LEWIS_PROPERTY %>',
				'<%= LewisMolecule.HIGHLIGHT %>',
				'Enter an element symbol.',
				'There is no such element.  Please try again.',
				'Other');
		initLewisGraphics('<%= pathToRoot %>', 
				'lewisJSCanvas', 
				'lewisJSToolbars');
		parseLewisMRV('<%= Utils.toValidJS(data) %>');
	} // initLewis()

	function submitIt() {
		var form = document.figureForm;
		form.figData.value = getLewisMRV();
		form.submit();
	} // submitIt

<% } else { %>

	function loadSelections() { ; }

	<% if (figType == SYNTHESIS) { %>

	function initSynthesis() {
		constants = new Array();
		constants[NO_REAGENTS] = '<%= RxnCondition.NO_REAGENTS %>';
		constants[RXN_ID_SEP] = '<%= Synthesis.RXN_ID_SEP %>';
		constants[RXN_IDS] = '<%= Synthesis.RXN_IDS %>';
		constants[CLICK_HERE] = '<%= Utils.toValidJS(rcPhrases.get(CLICK_RXN)) %>';
		constants[INSERT_HERE] = '<%= rcPhrases.get(INSERT_HERE) == null ? "" 
					: Utils.toValidJS(rcPhrases.get(INSERT_HERE)) %>';
		constants[ADD_1ST] = '<%= Utils.toValidJS(rcPhrases.get(ADD_1ST)) %>';
		constants[RXN_CONDN] = '<%= Utils.toValidJS(rcPhrases.get(RXN_CONDN)) %>';
		constants[PATH_TO_CHOOSE_RXN_CONDS_USER] = '<%= pathToChooseRxnCondsUser %>';
		constants[REMOVE] = '<%= Utils.toValidJS(rcPhrases.get(REMOVE)) %>';
		constants[AFTER_HERE] = '<%= rcPhrases.get(AFTER_HERE) == null ? ""
				: Utils.toValidJS(rcPhrases.get(AFTER_HERE)) %>';
		initRxnConds(constants);
		<% final int[] allRxnIds = RxnCondition.getAllReactionIds();
		for (final int rxnId : allRxnIds) { %>
			setRxnName(<%= rxnId %>, '<%= Utils.toValidJS(
					Utils.toDisplay(reactionNamesByIds.get(
						Integer.valueOf(rxnId)))) %>');
		<% } // for each rxn id
		final int numRxns = (onlyOneRxnCondn && chosenRxns.length > 1
				? 1 : chosenRxns.length);
		for (final String chosenRxn : chosenRxns) { %>
			setChosenRxn(<%= chosenRxn %>); 
		<% } // for each initial reaction condition %>
		writeRxnConds(<%= onlyOneRxnCondn %>);
	} // initSynthesis()

	<% } // if synthesis %>

	function submitIt() {
		var form = document.figureForm;
		marvinSketcherInstances['<%= APPLET_NAME %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(source) {
			form.figData.value = source;
			<% if (figType == SYNTHESIS) { %>
				form.rxnIds.value = getRxnIds();
			<% } // if synthesis %>
			form.submit();
		}, function(error) {
			alert('Molecule export failed:' + error);	
		});
	} // submitIt

<% } // if figtype %>

	// -->
</script>
</head>

<body class="light" style="margin:0px; margin-top:0px; background-color:#f6f7ed; 
		text-align:center; overflow:auto;" <%= 
			figType == SYNTHESIS ? "onload=\"initSynthesis();\"" 
			: figType == LEWIS ? "onload=\"initLewis();\"" 
			: figType == IMAGE_AND_VECTORS ? "onload=\"initDrawOnFigure();\"" 
			: "" %>>

	<form name="selectorForm" action="loadFigure.jsp" method="post">
		<input type="hidden" name="figNum" value="<%= figNum %>"  />
		<input type="hidden" name="figData" value="" />
		<input type="hidden" name="qType" value="<%= qType %>" />
		<input type="hidden" name="qFlags" value="<%= qFlags %>" />

	<table style="margin-left:auto; margin-right:auto; width:35%;
	text-align:center;" summary="">
		<tr><td class="boldtext enlarged">
			<%= figNum == 0 ? "New Figure" : "Figure " + figNum %> 
		</td></tr>
		<tr><td>
			<table class="whiteTable" style="text-align:center; padding-left:10px;
					padding-right:10px; padding-top:5px; padding-bottom:5px;
					width:100%;" summary="">
				<tr><td>
					<span class="regtext">Type of figure: </span>
					<select name="figTypeList" onchange="changeFigureType()" >
						<option value="<%= MOLECULE %>"
								<%= figType == MOLECULE ? SELECTED : "" %>> 
							Marvin only </option> 
						<option value="<%= REACTION %>"
								<%= figType == REACTION ? SELECTED : "" %>> 
							Marvin plus reagents </option> 
						<option value="<%= SYNTHESIS %>"
								<%= figType == SYNTHESIS ? SELECTED : "" %>> 
							Marvin plus synthesis conditions </option>
						<option value="<%= LEWIS %>"
								<%= figType == LEWIS ? SELECTED : "" %>> 
							Lewis structure </option>
						<option value="<%= MRV_TXT %>"
								<%= figType == MRV_TXT ? SELECTED : "" %>> 
							MRV of one or more structures </option>
						<option value="<%= JMOL %>"
								<%= figType == JMOL ? SELECTED : "" %>> 
							Jmol (orbitals, etc.) </option>
						<option value="<%= IMAGE %>"
								<%= figType == IMAGE ? SELECTED : "" %>> 
							Image </option>
						<option value="<%= IMAGE_AND_VECTORS %>"
								<%= figType == IMAGE_AND_VECTORS ? SELECTED : "" %>> 
							Image and vectors </option>
					</select>
				</td></tr>
			</table>
		</td></tr>
	</table>
	</form>	

<% if (Utils.among(figType, MOLECULE, REACTION)) { %>

	<form name="figureForm" action="saveFigure.jsp" method="post">
		<input type="hidden" name="figNum" value="<%= figNum %>"  />
		<input type="hidden" name="figType" value="<%= figType %>"  />
		<input type="hidden" name="figData" value="" />
		<input type="hidden" name="qFlags" value="<%= qFlags %>" />
	<table style="margin-left:auto; margin-right:auto;" summary="">
	<tr><td class="boldtext" style="text-align:right; padding-left:10px; 
			padding-right:10px; padding-top:10px; font-style:italic;">
		MarvinJS&trade;
	</td></tr>
	<tr><td style="text-align:center; padding-left:10px; padding-right:10px;">
		<table class="whiteTable" summary=""><tr>
			<td>
			<div id="<%= APPLET_NAME %>">
			<script type="text/javascript">
				// <!-- >
				startMarvinJS('<%= Utils.toValidJS(data) %>', 
						 <%= qType %>, 
						 <%= qFlags %>, 
						 '<%= APPLET_NAME %>', 
						 '<%= pathToRoot %>'); 
				// -->
			</script>
			</div>
			</td>
		</tr></table>
	</td>

	<% if (figType == REACTION) { 
		String addlData1 = "";
		String addlData2 = "";
		if (figNum != 0 && figure.isReaction()) {
			final String[] rxndata = figure.getReactionElements();
			addlData1 = rxndata[0];
			addlData2 = rxndata[1];
		} // existing reaction
	%>
	<td>
		<table summary="" style="border-spacing:0px; table-layout:fixed;">
		<tr><td>
			<input name="addlData1" type="text" 
					value="<%= Utils.toValidTextbox(addlData1) %>" size="20"
					style="" />	
		</td></tr>
		<tr><td style="font-size:48px; text-align:center; 
					vertical-align:middle; height:15px;">
			&rarr;
		</td></tr>
		<tr><td>
			<input name="addlData2" type="text" 
					value="<%= Utils.toValidTextbox(addlData2) %>"
					style="" size="20" />
		</td></tr>
		</table>
	</td>
	<% } // if reaction %>
	</tr>

<% } else if (figType == SYNTHESIS) { %>

	<form name="figureForm" action="saveFigure.jsp" method="post">
		<input type="hidden" name="figNum" value="<%= figNum %>"  />
		<input type="hidden" name="figType" value="<%= figType %>"  />
		<input type="hidden" name="figData" value="" />
		<input type="hidden" name="qFlags" value="<%= qFlags %>" />
		<input type="hidden" name="addlData1" value="<%= figure.addlData %>" />
		<input type="hidden" name="rxnIds" value="" />
	<table style="margin-left:auto; margin-right:auto; width:445px;" summary="">
	<tr><td class="boldtext" style="text-align:right; padding-left:10px; 
			padding-right:10px; padding-top:10px; font-style:italic;">
		MarvinJS&trade;
	</td></tr>
	<tr><td style="text-align:center; padding-left:10px; padding-right:10px;">
		<table class="whiteTable" summary=""><tr>
			<td>
			<div id="<%= APPLET_NAME %>">
			<script type="text/javascript">
				// <!-- >
				startMarvinJS('<%= Utils.toValidJS(data) %>', 
						 <%= qType %>, 
						 <%= qFlags %>, 
						 '<%= APPLET_NAME %>', 
						 '<%= pathToRoot %>'); 
				// -->
			</script>
			</div>
			</td>
		</tr></table>
	</td>
	</tr>
	<tr>
	<td style="text-align:center; padding-left:10px; padding-right:10px;">
		<table class="whiteTable" 
				style="margin-left:auto; margin-right:auto; 
				width:100%; text-align:left;" summary="">
		<tr><td id="reagentTable" class="regtext" >
		</td></tr>
		</table>
	</td></tr>

<% } else if (figType == LEWIS) { %>

	<form name="figureForm" action="saveFigure.jsp" method="post">
		<input type="hidden" name="figNum" value="<%= figNum %>"  />
		<input type="hidden" name="figType" value="<%= figType %>"  />
		<input type="hidden" name="figData" value="" />
	<table style="margin-left:auto; margin-right:auto; width:445px;" summary="">
	<tr><td class="boldtext" style="text-align:right; padding-left:10px; 
			padding-right:10px; padding-top:10px; font-style:italic;">
		Lewis JS&trade;
	</td></tr>
	<tr><td style="text-align:center; padding-left:10px; padding-right:10px;">
		<table class="whiteTable" summary="">
		<tr><td>
			<table class="rowsTable" 
					style="margin-left:auto; margin-right:auto; 
						width:<%= LewisMolecule.CANVAS_WIDTH %>px;">
			<tr><td style="width:100%;" id="lewisJSToolbars">
			</td></tr>
			<tr><td style="width:100%;"><div id="lewisJSCanvas" 
					style="position:relative;height:<%= 
						LewisMolecule.CANVAS_HEIGHT %>px;width:100%;"></div>
			</td></tr>
			</table>
		</td></tr>
		</table>
	</td></tr>

<% } else if (figType == MRV_TXT) { %>

	<form name="figureForm" action="saveFigure.jsp" method="post">
		<input type="hidden" name="figNum" value="<%= figNum %>"  />
		<input type="hidden" name="figType" value="<%= figType %>"  />
		<input type="hidden" name="figData" value="" />
	<table style="margin-left:auto; margin-right:auto; width:445px;" summary="">
	<tr><td class="regtext" style="vertical-align:top;">
		Enter the MRV:
	</td></tr><tr><td class="regtext">
		<textarea name="enteredMRV" cols="65" rows="20"><%= 
			figNum == 0 ? "" : Utils.toValidTextbox(data) %></textarea>
	</td></tr>
	</table>

<% } else if (isUpload) { %>

	<table style="width:95%; margin-right:auto; margin-left:auto;" summary="">
	<tr><td class="boldtext" style="text-align:left;">
		File containing the <%= figType == JMOL ? "Jmol data" : "image" %>:
	</td><td>
		<form name="fileupload" action="<%= figType == JMOL
				? "calcUpload.jsp" : "imgUpload.jsp" %>" method="post" 
				enctype="multipart/form-data">
			<input type="hidden" name="figNum" value="<%= figNum %>" />
			<input type="file" name="srcFile" size="30" />
			<input type="button" value="Upload" onclick="upload();" />
			<input type="hidden" name="figType" value="<%= figType %>"  />
		</form>
 	</td></tr>
	<tr><td colspan="2" style="color:green; text-align:left;">
		To display MOs, upload a file in the .mo format.
	</td></tr>
	</table>

	<form name="figureForm" action="saveFigure.jsp" method="post">
		<input type="hidden" name="figNum" value="<%= figNum %>" />
		<input type="hidden" name="figType" value="<%= figType %>"  />
		<input type="hidden" name="srcFile" 
				value="<%= Utils.toValidHTMLAttributeValue(srcFile) %>" />
		<input type="hidden" name="figData" value="" />
		<% if (figType == IMAGE) { %>
			<table style="width:95%; margin-right:auto; margin-left:auto;" summary="">
				<tr><td class="regtext" style="text-align:left;">
			<% if (srcFile != null) { %>
				<p style="text-align:center";>
				<img src="<%= pathToRoot + srcFile %>" alt="picture" 
						onload="fixImageSize(this);" /></p>
			<% } else { %>
				No image loaded.
			<% } // if there's an image to show %>
		<% } else if (figType == IMAGE_AND_VECTORS) { %>
			<% if (srcFile != null) { %>
				<table summary="drawOnFigureTable">
				<tr><td class="regtext" style="vertical-align:top;">
					Click-hold, drag, and release to draw a vector.
				</td></tr>
				<tr><td style="text-align:left;">
					<div id="canvas" style="position:relative; left:0px; top:0px;">
					<img src="<%= pathToRoot + srcFile %>" 
							id="clickableImage" alt="picture" class="unselectable"
							onselect="return false;" ondragstart="return false;"/>
					</div>
				</td></tr>
				<tr><td class="regtext" style="width:100%;i text-align:left;">
					<input type="hidden" id="shapeChooser" value="<%= DrawVectors.ARROW %>" />
					<table><tr>
						<td id="clickPurposeCell"></td>
						<td id="clickActions1"></td>
						<td id="clickActions2"></td>
					</tr></table>
			<% } else { %>
				<table style="width:95%; margin-right:auto; margin-left:auto;" summary="">
					<tr><td class="regtext" style="text-align:left;">
					No image loaded.
			<% } // if there's an image to show %>
		<% } else if (figType == JMOL) { %>
			<table style="width:95%; margin-right:auto; margin-left:auto;" summary="">
				<tr><td class="regtext" style="text-align:left;">
			<% if (data != null) { 
				final String[] allJmolScripts = (figNum != 0 && figure.isJmol()
						? figure.getJmolScripts() : new String[2]); 
				jmolNum++; %>
				<p style="text-align:center;">
				<script type="text/javascript">
					// <!-- >
					setJmol(<%= jmolNum %>, 
							'<%= Utils.toValidJS(data) %>', 
							'#f6f7ed', 400, 250,
							'<%= Utils.toValidJS(allJmolScripts[0]) %>');
					// -->
				</script>
				</p>
				<p>Enter any <a href="http://wiki.jmol.org/index.php/Scripting"
				target="window2">Jmol scripts</a> (semicolon-separated) that you would 
				like <a href="http://jmol.sourceforge.net" target="window2">Jmol</a> 
				to run when it starts 
				(<a href="javascript:warn('scripts');"><b>warning</b></a>).
				</p>
				<p style="text-align:center;"><textarea name="addlData1" id="addlData1"
						style="overflow:auto;" cols="50" rows="4"><%= 
								Utils.toValidTextbox(allJmolScripts[0]) %></textarea>
				</p>
				<p>Enter any <a href="http://jmol.sourceforge.net/jslibrary/"
				target="window2">Jmol Javascript library</a> commands 
				(semicolon-separated) that you would like this page to run when the 
				<a href="http://jmol.sourceforge.net" target="window2">Jmol</a> 
				applet starts 
				(<a href="javascript:warn('Javascript%20library%20commands');"><b>warning</b></a>).
				</p>
				<p style="text-align:center;"><textarea name="addlData2" id="addlData2"
						style="overflow:auto;" cols="50" rows="4"><%= 
								Utils.toValidTextbox(allJmolScripts[1]) %></textarea>
				</p>
			<% } else { %>
				No data loaded.
			<% } // if there's data to show %>
 		<% } // if figType %>
 	</td></tr>

<% } // if figType %>

	<tr><td style="text-align:center; margin-top:10px;">
		<table style="margin-right:auto; margin-left:auto;" summary="">
		<tr>
		<td><%= makeButton("Apply Changes", "submitIt();") %></td>
		<td><%= makeButton("Cancel", "self.close();") %></td>
		</tr>
		</table>
	</td></tr>
	</table>
	</form>
</body>
</html>


