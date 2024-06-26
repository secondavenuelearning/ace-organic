<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.chem.FnalGroupDef,
	com.epoch.chem.MolString,
	com.epoch.db.CanonicalizedUnitRW,
	com.epoch.energyDiagrams.EDiagram,
	com.epoch.energyDiagrams.YAxisScale,
	com.epoch.evals.CombineExpr,
	com.epoch.evals.EvalManager,
	com.epoch.evals.impl.CompareNums,
	com.epoch.evals.impl.chemEvals.Atoms,
	com.epoch.evals.impl.chemEvals.Charge,
	com.epoch.evals.impl.chemEvals.CountMetals,
	com.epoch.evals.impl.chemEvals.FnalGroup,
	com.epoch.evals.impl.chemEvals.HasFormula,
	com.epoch.evals.impl.chemEvals.Is,
	com.epoch.evals.impl.chemEvals.Rings,
	com.epoch.genericQTypes.ClickImage,
	com.epoch.genericQTypes.TableQ,
	com.epoch.physics.CanonicalizedUnit,
	com.epoch.physics.EquationFunctions,
	com.epoch.physics.Equations,
	com.epoch.qBank.CaptionsQDatum,
	com.epoch.qBank.EDiagramQDatum,
	com.epoch.qBank.QDatum,
	com.epoch.qBank.Question,
	com.epoch.substns.RGroupCollection,
	com.epoch.substns.SubstnUtils,
	com.epoch.synthesis.Synthesis,
	com.epoch.synthesis.SynthStarterRule,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.Arrays,
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
	final String pathToRoot = "../../";
	final String pathToFolder = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

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

	if (question == null) {
		Utils.alwaysPrint("loadQData.jsp: question is null.");
	%>
		<jsp:forward page="../errorParam.jsp"/>
	<%
	}

	final int MARVIN = QDatum.MARVIN;
	final int TEXT = QDatum.TEXT;
	final int SUBSTN = QDatum.SUBSTN;
	final int SYNTH_OK_SM = QDatum.SYNTH_OK_SM;
	final int SM_EXPR = QDatum.SM_EXPR;

	final int RETURN = 0;
	final int ADD_NEW = 1;
	final int ADD_CLONE = 2;
	final int MAX_SUBSTN_VALUE_LENGTH = 50; // database column limitation

	final boolean masterEdit = entry1.isMasterEdit();
	final boolean isSynthesis = Question.isSynthesis(qType); 
	final boolean isTable = Question.isTable(qType); 
	final boolean isOED = Question.isOED(qType); 
	final boolean isRCD = Question.isRCD(qType); 
	final boolean isED = isOED || isRCD;
	final boolean isNumeric = Question.isNumeric(qType);
	final boolean isClickableImage = Question.isClickableImage(qType);
	final boolean isDrawVectors = Question.isDrawVectors(qType);
	final boolean isLogicalStmts = Question.isLogicalStatements(qType);
	final boolean isEquations = Question.isEquations(qType); 
	final boolean allowTextOnly = Question.isFillBlank(qType)
			|| isNumeric || isTable || isOED || isRCD 
			|| isClickableImage || isDrawVectors || isLogicalStmts;
	final boolean oneQDatumOnly = isED || isClickableImage 
			|| isDrawVectors || isLogicalStmts || isEquations;
	final boolean chemFormatting = Question.chemFormatting(qType, qFlags);
	final int qdNum = MathUtils.parseInt(request.getParameter("qDatumNum"));
	final boolean cloneEdit = "true".equals(request.getParameter("cloneEdit"));
	final boolean isFromQuestionJSP = "true".equals(request.getParameter("virgin"));
	final int tableNum = MathUtils.parseInt(request.getParameter("tableNum"));
	final boolean isSubstn = tableNum == Question.SUBSTNS;
	// needed for complete-the-table, R groups, permissible SMs, energy diagrams
	final QDatum[] qData = question.getQData(tableNum);
	final int numQData = qData.length;
	/* Utils.alwaysPrint("loadQData.jsp: qType = ", 
			Question.getQTypeDescription(qType, qFlags),
			", qFlags = ", qFlags,
			", allowTextOnly = ", allowTextOnly,
			", qdNum = ", qdNum,
			", numQData = ", numQData,
			", cloneEdit = ", cloneEdit,
			", isSubstn = ", isSubstn,
			", tableNum = ", tableNum); /**/

	// get if complete-the-table and row or column
	final boolean makePreloadTable = (cloneEdit && qdNum >= TableQ.MIN_QDATA)
			|| qdNum > TableQ.MIN_QDATA
			|| (qdNum == 0 && numQData >= TableQ.MIN_QDATA);
	final boolean isTableQRowOrCol = isTable && !makePreloadTable;
	final boolean isRow = (qdNum != 0 && !cloneEdit 
			? qdNum == TableQ.ROW_DATA + 1 
			: qdNum != 0 ? qdNum == TableQ.ROW_DATA
			: numQData == TableQ.ROW_DATA); 

	// get if energy diagram and captions or pulldown labels
	final boolean addYAxisScale = cloneEdit || qdNum > 1
			|| (qdNum == 0 && numQData >= 1);

	QDatum qDatum = (isED ? new EDiagramQDatum()
			: isTableQRowOrCol ? new CaptionsQDatum() 
			: new QDatum());
	final String qDatumTypeStr = request.getParameter("qDatumType");
	if (qDatumTypeStr != null) { // existing datum, changing type
		qDatum.dataType = MathUtils.parseInt(qDatumTypeStr);
	} else if (qdNum == 0) { // new datum
		qDatum.dataType = (isSubstn ? SUBSTN 
				: isSynthesis ? SYNTH_OK_SM
				: TEXT);
	} else qDatum = question.getQDatum(tableNum, qdNum);

	// get question-type-specific values
	// complete-the-table
	final int numRowsOrCols = (isTableQRowOrCol 
			? ((CaptionsQDatum) qDatum).getNumRowsOrCols() : 0);
	final String[] captions = (isTableQRowOrCol || isED
			? ((CaptionsQDatum) qDatum).captions : null);
	final YAxisScale yAxisData = (isED 
			? ((EDiagramQDatum) qDatum).yAxisScale : new YAxisScale());

	// synthesis
	final String[] EVAL_CODES = EvalManager.EVAL_CODES;
	final String[] SYMBOLS = CompareNums.SYMBOLS;
	final int EQUALS = CompareNums.EQUALS;
	final int NOT_EQUALS = CompareNums.NOT_EQUALS;
	final int NUM_ATOMS = EvalManager.NUM_ATOMS;
	final int NUM_RINGS = EvalManager.NUM_RINGS;
	final int IS_OR_HAS_SIGMA_NETWORK = EvalManager.IS_OR_HAS_SIGMA_NETWORK;
	final int FUNCTIONAL_GROUP = EvalManager.FUNCTIONAL_GROUP;
	final int HAS_FORMULA = EvalManager.HAS_FORMULA;
	final int TOTAL_CHARGE = EvalManager.TOTAL_CHARGE;
	final int COUNT_METALS = EvalManager.COUNT_METALS;
	final FnalGroupDef[] sortedGroups = FnalGroupDef.getAllGroups();
	SynthStarterRule rule = null;
	int setEvalType = 0;
	if (qDatum.dataType == SYNTH_OK_SM) { 
		rule = new SynthStarterRule(qDatum.data);
		final int evalType = rule.getEvalType();
		setEvalType = MathUtils.parseInt(request.getParameter("evalType"),
				evalType != -1 ? evalType : NUM_ATOMS);
	} // if setting permissible starting materials

	// R groups
	Map<Integer, String> rGroupNamesByIds = null;
	int[] rGroupIds = new int[0];
	if (isSubstn && !isNumeric) {
		rGroupNamesByIds = RGroupCollection.
				getRGroupCollectionNamesKeyedByIds();
		if (!rGroupNamesByIds.isEmpty()) {
			rGroupIds = RGroupCollection.getAllRGroupCollectionIdsAlphabetized(
					rGroupNamesByIds);
		}
	} // if setting R groups

	if (qDatum.data == null) qDatum.data = (qDatum.dataType == MARVIN 
				|| (qDatum.dataType == SYNTH_OK_SM 
					&& setEvalType == IS_OR_HAS_SIGMA_NETWORK)
			? Question.EMPTY_MRV : "");
	/* Utils.alwaysPrint("loadQData.jsp: qDatum.dataType = ", qDatum.dataType,
			" (MARVIN = ", MARVIN, ", ", "TEXT = ", TEXT, ", ",
			"SUBSTN = ", SUBSTN, ", ", "SYNTH_OK_SM = ", SYNTH_OK_SM, ", ",
			"SM_EXPR = ", SM_EXPR, ")", ", qDatum.data (in single quotes): '",
			qDatum.data, "'"); /**/
	final String CHECKED = " checked=\"checked\"";
	final String SELECTED = "selected=\"selected\" ";
	final String APPLET_NAME = "structureApplet";
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<title>ACE Question Data Editor</title>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico"
	type="image/x-icon"/>
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

#qDataHeader {
	position:absolute; 
	top:0; 
	left:0; 
	width:100%; 
	height:90px; 
	overflow:auto; 
}

#qDataFooter {
	position:absolute; 
	bottom:0; 
	left:0;
	width:100%; 
	height:55px; 
	overflow:auto; 
	text-align:right; 
	vertical-align:bottom;
	padding-top:10px;
}

#qDataContents {
	position:fixed; 
	top:90px;
	left:0;
	bottom:55px; 
	right:0; 
	overflow:auto; 
}

* html #qDataContents {
	height:100%; 
}

* html body {
	padding:90px 0 55px 0; 
}
</style>

<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<% if (qDatum.dataType == MARVIN
		|| (qDatum.dataType == SYNTH_OK_SM 
			&& setEvalType == IS_OR_HAS_SIGMA_NETWORK)) { %>
	<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/3DTemplatesMJS.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>nosession/marvinJS/js/promise.min.js"></script>
	<script src="https://marvinjs.chemicalize.com/v1/<%= AppConfig.marvinJSLicense %>/client-settings.js"></script>
	<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
<% } else if (qDatum.dataType == SYNTH_OK_SM) { %>
	<script src="<%= pathToRoot %>js/fnalGroups.js" type="text/javascript"></script>
<% } else if (qDatum.dataType == SM_EXPR) { %>
	<script src="<%= pathToRoot %>js/combineExpr.js" type="text/javascript"></script>
<% } else if (isEquations) { %>
	<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/equations.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
 	<script src="<%= pathToRoot %>nosession/mathjax/MathJax.js?config=TeX-AMS-MML_HTMLorMML.js" 
			type="text/javascript"></script>
<% } %>
<script type="text/javascript">
	// <!-- >

	<%@ include file="/js/marvinQuestionConstants.jsp.h" %>

	function changeQDatumType() {
		go = 'loadQData.jsp?qDatumNum=<%= cloneEdit ? 0 : qdNum %>'
				+ '&qDatumType=' + document.selectorForm.dataTypeList.value
				+ '&virgin=<%= isFromQuestionJSP %>&tableNum=<%= tableNum %>';
		// alert(go);
		self.location.href = go;
	} // changeQDatumType()
	
	function cancelMe() {
		<% if (!isFromQuestionJSP) { %>
			opener.location.href = '../question.jsp?qId=same';
		<% } %>
		self.close();
	} // cancelMe()

	<% if (isSubstn && !isNumeric) { %> 

		function editRGroupColl(rGroupNum) {
			openRGroupWindow('..\/universalData\/editRGroupColl.jsp?rGroupNum=' + rGroupNum);
		} // editRGroupColl()
 
		function submitIt(afterSave) {
			var chosenBld = new String.builder();
			var first = true;
			<% for (int rgNum = 1; rgNum <= rGroupIds.length; rgNum++) { %>
				if (document.qDataForm.option<%= rgNum %>.checked) {
					if (first) first = false;
					else chosenBld.append('<%= SubstnUtils.SUBSTNS_SEP %>');
					chosenBld.append('<%= rGroupIds[rgNum - 1] %>');
				} // if option is checked
			<% } // for each R-group collection ID %>
			var chosenRGroups = chosenBld.toString();
			document.qDataForm.afterSave.value = afterSave;
			if (!isEmpty(chosenRGroups)) {
				document.qDataForm.qdData.value = chosenRGroups;
				document.qDataForm.submit();
			} else alert('Please select one or more R group collections.');
		} // submitIt()

		var ON = 1;
		var TOGGLE = 2;
		function changeAll(how) {
			for (var rgNum = 1; rgNum <= <%= rGroupIds.length %>; rgNum++) {
				var formOption = document.getElementById('option' + rgNum);
				if (how === ON) formOption.checked = true; 
				else formOption.checked = !formOption.checked;
			} // for each R group collection 
		} // selectAll()

	<% } else if (qDatum.dataType == MARVIN) { %> 

		function loadSelections() { ; }

		function submitIt(afterSave) {
			var form = document.qDataForm;
			marvinSketcherInstances['<%= APPLET_NAME %>'].
					exportStructure('<%= MRV_EXPORT %>').then(function(source) {
				form.qdData.value = source;
				var molname = trimWhiteSpaces(form.molname.value);
				if (isEmpty(molname)) {
					alert('Enter a valid name for the molecule.');
					return;
				}
				var marvinFlags = 0;
				if (form.howShowHydrogens.checked) {
					marvinFlags |= parseInt(form.allNo.value);
				}
				if (form.withMapping.checked) marvinFlags |= SHOWMAPPING;
				if (form.withLonePairs.checked) marvinFlags |= SHOWLONEPAIRS;
				if (form.withRSLabels.checked) marvinFlags |= SHOWRSLABELS;
				form.displayOpts.value = marvinFlags;
				form.molname.value = molname;
				form.afterSave.value = afterSave;
				form.submit();
			}, function(error) {
				alert('Molecule export failed:' + error);	
			});
		} // submitIt()

	<% } else if (qDatum.dataType == SYNTH_OK_SM) { %> 

		<% if (setEvalType == IS_OR_HAS_SIGMA_NETWORK) { %> 
			function loadSelections() { ; }
		<% } // if setEvalType %>

		function changeSMRule() {
			var bld = new String.builder().
					append('loadQData.jsp?qDatumNum=<%= cloneEdit ? 0 : qdNum %>'
						+ '&virgin=<%= isFromQuestionJSP %>'
						+ '&tableNum=<%= tableNum %>&qDatumType=').
					append(document.selectorForm.dataTypeList.value).
					append('&evalType=').
					append(document.qDataForm.evalTypeSelector.value);
			var go = bld.toString();
			// alert(go);
			self.location.href = go;
		} // changeSMRule()

		<% if (setEvalType == FUNCTIONAL_GROUP) { %>
			var showCategoryWarning = false;
		<% } // if setting functional groups %>

		function submitIt(afterSave) {
			<% if (setEvalType == IS_OR_HAS_SIGMA_NETWORK) { %> 
				marvinSketcherInstances['<%= APPLET_NAME %>'].
						exportStructure('smiles').then(function(source) {
			<% } // if setEvalType %>
			var form = document.qDataForm;
			form.afterSave.value = afterSave;
			var evalType = parseInt(form.evalTypeSelector.value);
			var codedDataBld = new String.builder();
			switch (evalType) {
				case <%= IS_OR_HAS_SIGMA_NETWORK %>:
					// matchCode/howMany/flags/molname/source [in smiles format]
					var howMany = form.isIsPos.value;
					var flags = (form.enantiomer.checked 
							? parseInt(form.enantiomer.value) : 0);
					var molname = trimWhiteSpaces(form.molname.value);
					codedDataBld.append('<%= EVAL_CODES[IS_OR_HAS_SIGMA_NETWORK] %>/').
							append(howMany).append('/').
							append(flags).append('/').
							append(molname).append('/').
							append(source);
					break;
				case <%= FUNCTIONAL_GROUP %>:
					// matchCode/groupID/!isPos=/0/N/NOT_EQUALS/0
					if (form.fnalGroupId.value === '0') {
						alert('Please choose a functional group.');
						return;
					}
					codedDataBld.append('<%= EVAL_CODES[FUNCTIONAL_GROUP] %>/').
							append(form.fnalGroupId.value).append('/').
							append(form.fnalIsPos.value).append('/0/N/<%=
							SYMBOLS[NOT_EQUALS] %>/0');
					break;
				case <%= HAS_FORMULA %>:
					// matchCode/!isPos=/0/N/formula
					if (form.formula.value === '') {
						alert('Please enter a formula.');
						return;
					}
					codedDataBld.append('<%= EVAL_CODES[HAS_FORMULA] %>/').
							append(form.formulaIsPos.value).
							append('/0/N/').append(form.formula.value);
					break;
				case <%= NUM_ATOMS %>:
					// matchCode/atomsOper/numAtoms/element/contiguous/N/NOT_EQUALS/0
					codedDataBld.append('<%= EVAL_CODES[NUM_ATOMS] %>/').
							append(form.atomsOper.value).append('/').
							append(form.numAtoms.value).append('/').
							append(form.element.value).append('/').
							append(form.contiguous.value).
							append('/N/<%= SYMBOLS[NOT_EQUALS] %>/0');
					break;
				case <%= NUM_RINGS %>:
					// matchCode/N/ringsOper/numRings/NOT_EQUALS/0
					codedDataBld.append('<%= EVAL_CODES[NUM_RINGS] %>/N/').
							append(form.ringsOper.value).append('/').
							append(form.numRings.value).
							append('/<%= SYMBOLS[NOT_EQUALS] %>/0');
					break;
				case <%= TOTAL_CHARGE %>:
					// matchCode/EQUALS/0/N/NOT_EQUALS/0
					codedDataBld.append('<%= EVAL_CODES[TOTAL_CHARGE] %>/<%= 
							SYMBOLS[EQUALS] %>/0/N/<%= 
							SYMBOLS[NOT_EQUALS] %>/0');
					break;
				case <%= COUNT_METALS %>:
					// matchCode/EQUALS/0/all/NOT_EQUALS/0
					codedDataBld.append('<%= EVAL_CODES[COUNT_METALS] %>/<%= 
							SYMBOLS[EQUALS] %>/0/').
							append(form.metalKind.value).
							append('/<%= SYMBOLS[NOT_EQUALS] %>/0');
					break;
				default: 
					alert('Unrecognized evaluator type ' + evalType
							+ ' parsed from ' + form.synthOkSM.value);
					self.close();
			} // switch
			form.qdData.value = codedDataBld.toString();
			form.submit();
			<% if (setEvalType == IS_OR_HAS_SIGMA_NETWORK) { %> 
				}, function(error) {
					alert('Molecule export failed:' + error);	
				});
			<% } // if setEvalType %>
		} // submitIt()

	<% } else if (qDatum.dataType == SM_EXPR) { 
		if (Utils.isEmpty(qDatum.data)) {
			final StringBuilder dataBld = new StringBuilder();
			for (int num = 1; num <= numQData; num++) {
				if (qdNum != num && qData[num - 1].isSynOkSM()) {
					if (num > 1) dataBld.append(CombineExpr.OR);
					dataBld.append(num);
				} // if this is not the number of the current qDatum 
			} // for each qDatum
			qDatum.data = dataBld.toString();
		} // if need to generate default expression
		final String[] combParts = CombineExpr.getNestedArray(
				CombineExpr.postfixToNested(qDatum.data));
	%> 

		function initiateCombParts() {
			var combParts = new Array();
			<% for (final String combPart : combParts) { %>
				combParts.push('<%= combPart  %>');
			<% } // for each character %>
			initiateConstants('<%= CombineExpr.OPEN_PAREN %>', 
					'<%= CombineExpr.CLOSE_PAREN %>',
					'<%= CombineExpr.AND %>', 
					'<%= CombineExpr.OR %>', 
					'<%= CombineExpr.OF %>',
					'<%= CombineExpr.TO %>');
			var numRules = <%= numQData - (qdNum == 0 ? 0 : 1) %>;
			initiateSelectors(combParts, numRules);
		} // initiateCombParts()

		function submitIt(afterSave) {
			var expr = getCombineExpr();
			if ('(123456789'.indexOf(expr.charAt(0)) <  0) { // <!-- >
				alert(expr);
				return;
			} // if the expression doesn't begin properly
			// afterSave is not used in this method
			document.qDataForm.qdData.value = expr;
			document.qDataForm.submit();
		} // submitIt()

	<% } else { // qDatum.dataType == TEXT or SUBSTN %> 

		<% if (isOED || isRCD) { %>

			function addLabel() {
				var lblNum = 0;
				var haveBox = true;
				var lblOutBld = new String.builder().append('<table>');
				while (haveBox) {
					lblNum++;
					var lblBox = document.getElementById('label' + lblNum);
					if (!lblBox) haveBox = false;
					lblOutBld.append('<tr><td class="regtext" '
								+ 'style="padding-right:10px;">'
								+ 'Label ').append(lblNum).
							append(':<\/td><td class="regtext">'
								+ '<input type="text" name="label').
							append(lblNum).append('" id="label').
							append(lblNum).append('" size="20" value="');
					if (haveBox) lblOutBld.append(lblBox.value);
					lblOutBld.append('" /><\/td><td class="boldtext big" '
								+ 'style="color:red;">'
								+ '<span onclick="javascript:removeLabel(').
							append(lblNum).
							append(')">&times;<\/span><\/td><\/tr>');
				} // while haveBox
				lblOutBld.append('<tr><td colspan="3" class="regtext">'
						+ '<%= Utils.toValidJS(makeButton(
							"Add label", "addLabel();")) %>'
						+ '<\/td><\/tr><\/table>');
				setInnerHTML('labelTable', lblOutBld.toString());
			} // addLabel()

			function removeLabel(oldLabel) {
				var lblNum = 0;
				var lblOutBld = new String.builder().append('<table>');
				while (true) {
					lblNum++;
					if (lblNum === oldLabel) continue;
					var lblBox = document.getElementById('label' + lblNum);
					if (!lblBox) break;
					var newLblNum = (lblNum > oldLabel ? lblNum - 1 : lblNum);
					lblOutBld.append('<tr><td class="regtext" '
								+ 'style="padding-right:10px;">'
								+ 'Label ').append(newLblNum).
							append(':<\/td><td class="regtext">'
								+ '<input type="text" name="label').
							append(newLblNum).append('" id="label').
							append(newLblNum).append('" size="20" value="').
							append(lblBox.value).
							append('" /><\/td><td class="boldtext big" '
								+ 'style="color:red;">'
								+ '<span onclick="javascript:removeLabel(').
							append(newLblNum).
							append(')">&times;<\/span><\/td><\/tr>');
				} // while true
				lblOutBld.append('<tr><td colspan="3" class="regtext">'
						+ '<%= Utils.toValidJS(makeButton("Add label", "addLabel();")) %>'
						+ '<\/td><\/tr>'
						+ '<\/table>');
				setInnerHTML('labelTable', lblOutBld.toString());
			} // removeLabel()

			function setYAxisScaleData() {
				var form = document.qDataForm;
				setInnerHTML('yAxisScaleTable', form.useYAxisScale.checked
						? new String.builder().append(
						'<table style="margin-left:auto; '
						+ 'margin-right:auto; width:445px;" summary="">'
						+ '<tr><td class="regtext" style="padding-right:10px;">'
						+ 'Enter the unit of energy:'
						+ '<\/td><td class="regtext" style="width:50%;">'
						+ '<input type="text" name="unit" '
						+ 'size="15" value="<%= yAxisData.getUnit() %>" \/>'
						+ '<\/td><\/tr>'
						+ '<tr><td colspan="2" class="regtext" '
						+ 'style="padding-right:10px;">Label row '
						+ '<input type="text" name="rowInit" size="3" '
						+ 'value="<%= yAxisData.getRowInit() %>" \/> '
						+ '(1 [bottom] to ').append(form.numRows.value).
						append(' inclusive) with the quantity '
						+ '<input type="text" name="quantInit" size="10" '
						+ 'value="<%= yAxisData.getQuantInit() %>" \/>, '
						+ 'and every <input type="text" name="rowIncrement" '
						+ 'size="3" value="<%= yAxisData.getRowIncrement() %>" \/> '
						+ 'row(s), increment the quantity by '
						+ '<input type="text" name="quantIncrement" size="10" '
						+ 'value="<%= yAxisData.getQuantIncrement() %>" \/>.'
						+ '<\/td><\/tr><\/table>').toString()
						: '');
			} // setYAxisScaleData()

		<% } else if (isEquations) { %>

		 	<%@ include file="/js/equations.jsp.h" %>
			function setUpEqns() {
				initEqnConstants();
				setPathToRoot('<%= Utils.toValidJS(pathToRoot) %>');
				var eqns = new Array();
				<% final Equations eqnsObj = new Equations(qDatum.data);
				final String[] eqns = eqnsObj.getEntries();
				final String constants = eqnsObj.getConstants();
				final String variablesNotUnits = eqnsObj.getVariablesNotUnitsStr();
				final boolean disableConstantsField = eqnsObj.getDisableConstants();
				for (int eqnNum = 0; eqnNum < eqns.length; eqnNum++) { %>
					eqns.push('<%= Utils.toValidTextbox(eqns[eqnNum]) %>');
				<% } // for each current equation
				if (Question.omitConstantsField(qFlags)) { %>
					setConstantsFieldOptions(true);
				<% } else { %>
					setConstantsFieldOptions(false);
					setInnerHTML('disableConstantsCell', 
							'<input type="checkbox" name="disableConstantsField" '
							+ 'id="disableConstantsField" <%=
								disableConstantsField ? "checked=\"checked\"" : "" %> />'
							+ '&nbsp;&nbsp;make constants field uneditable');
				<% } // if omit constants field %>
				setEqns(eqns, '<%= Utils.toValidJS(constants) %>');
				setValue('<%= Equations.VARS_NOT_UNITS_TAG %>',
						cerToUnicode('<%= Utils.toValidTextbox(variablesNotUnits) %>'));
			} // setUpEqns()

		<% } else if (isTable) { %>

			function updateCaptionsTable() {
				"use strict";
				var captions = [],
						newNumCaptions = 0,
						captionNum = 0,
						tableBld = new String.builder();
				while (cellExists('caption' + captionNum)) {
					captions.push(getValue('caption' + captionNum++));
				} // while there are existing captions
				newNumCaptions = parseInt(getValue('numRowsOrCols'))
						+ (<%= isRow %> ? 0 : 1);
				tableBld.append('<table>');
				for (captionNum = 0; captionNum < newNumCaptions; captionNum++) { // <!-- >
					tableBld.append('<tr><td>');
					if (<%= !isRow %> && captionNum == 0) {
						tableBld.append('Column of<br\/>row captions');
					} else {
						tableBld.append(<%= isRow %> ? 'Row ' : 'Column ').
							append(captionNum + <%= isRow ? 1 : 0 %>);
					} 
					tableBld.append(':<\/td><td><input type="text" id="caption').
							append(captionNum).
							append('" name="caption').
							append(captionNum).
							append('" size="60" value="');
					if (captionNum < captions.length) {
						tableBld.append(captions[captionNum]);
					} // if there's a caption
					tableBld.append('" \/><\/td><\/tr>');
				} // for each caption
				tableBld.append('<\/table>');
				setInnerHTML('captionsTable', tableBld.toString());
				if (newNumCaptions > 0) showCell('enterCaptions');
				else hideCell('enterCaptions');
			} // updateCaptionsTable()

		<% } // if question type %>

		function submitIt(afterSave) {
			var form = document.qDataForm;
			<% if (isOED || isRCD) { %>
				var numRowsStr = form.numRows.value;
				if (isWhiteSpace(numRowsStr) || !canParseToInt(numRowsStr) 
						|| parseInt(numRowsStr) <= 0) { // <!-- >
					alert('Enter a valid number of rows.');
					return;
				}
				<% if (isRCD) { %>
					var numColsStr = form.numCols.value;
					if (isWhiteSpace(numColsStr) || !canParseToInt(numColsStr) 
							|| parseInt(numColsStr) <= 0) { // <!-- >
						alert('Enter a valid number of columns.');
						return;
					}
				<% } // if qType %>
				if (form.useYAxisScale.value.checked) {
					var numRows = parseInt(numRowsStr);
					var rowInit = form.rowInit.value;
					if (isWhiteSpace(rowInit) || !canParseToInt(rowInit) 
							|| parseInt(rowInit) > numRows
							|| parseInt(rowInit) <= 0) { // <!-- >
						alert('Enter a valid number of rows.');
						return;
					}
					var rowIncrement = form.rowIncrement.value;
					if (isWhiteSpace(rowIncrement) || !canParseToInt(rowIncrement) 
							|| parseInt(rowIncrement) <= 0) { // <!-- >
						alert('Enter a valid distance between rows.');
						return;
					}
					var quantInit = form.quantInit.value;
					if (isWhiteSpace(quantInit) || !canParseToFloat(quantInit)) { 
						alert('Enter a valid initial amount of energy.');
						return;
					}
					var quantIncrement = form.quantIncrement.value;
					if (isWhiteSpace(quantIncrement) 
							|| !canParseToFloat(quantIncrement)) { 
						alert('Enter a valid increment for the amount of energy.');
						return;
					}
				} // if adding y-axis scale
			<% } else if (isClickableImage) { %>
				var numMarks = form.numMarks.value;
				if (isWhiteSpace(numMarks) || !canParseToInt(numMarks)) {
					alert('Enter a valid number of marks.');
					return;
				}
				var color = trimWhiteSpaces(form.colorValue.value);
				form.qdData.value = color + '<%= ClickImage.QD_SEP %>' + numMarks;
			<% } else if (isEquations) { %>
				form.qdData.value = getEqnsXML();
				if (isEmpty(form.qdData.value)) return;
			<% } else if (!isTable) { // anything else other than table %>
				var textData = trimWhiteSpaces(form.qDataTextArea.value);
				if (isEmpty(textData)) {
					alert('Enter some text, please.');
					return;
				}
				<% if (isNumeric && qDatum.dataType == SUBSTN) { %>
					var textDataParts = textData.split('<%= SubstnUtils.SUBSTNS_SEP %>');
					for (var partNum = 0; partNum < textDataParts.length; partNum++) { // <!-- >
						if (textDataParts[partNum].length > <%= MAX_SUBSTN_VALUE_LENGTH %>) {
							alert('Sorry, ACE limits values or word&ndash;value pairs '
									+ ' to <%= MAX_SUBSTN_VALUE_LENGTH %>'
									+ ' characters between each '
									+ ' <%= SubstnUtils.SUBSTNS_SEP %> separator. You '
									+ 'might try removing any unnecessary space '
									+ 'characters.');
							return;
						} // if substitution value is too long
					} // for each substitution value
				<% } // if numeric and SUBSTN  %>
				form.qdData.value = textData;
			<% } else if (!makePreloadTable) { %>
				var numRowsOrCols = form.numRowsOrCols.value;
				if (isWhiteSpace(numRowsOrCols) || !canParseToInt(numRowsOrCols)) {
					alert('Enter a valid number of <%= isRow ? "rows" : "columns" %>');
					return;
				}
			<% } // if type of text input
			// NOTE: we pass the input elements of a table directly to
			// saveQData.jsp for conversion to XML
			%>
			form.afterSave.value = afterSave;
			form.submit();
		} // submitIt()
	
	<% } // if qDatum.dataType %> 

	// -->
</script>
</head>
<body class="light" style="margin:0px; margin-top:5px; background-color:#f6f7ed;"
	<%= qDatum.dataType == SM_EXPR ? "onload=\"initiateCombParts();\"" 
			: isEquations ? "onload=\"setUpEqns();\""
			: isED ? "onload=\"setYAxisScaleData();\""
			: "" %> >

<div id="qDataHeader">
<table style="margin-left:auto; margin-right:auto; width:90%;" summary="">
	<tr><td class="boldtext enlarged" 
			style="padding-top:10px;">
		<%= qdNum == 0 ? "New Question datum" : Utils.toString(cloneEdit ? "Clone of " : "", 
				"Question datum ", qdNum) %>
	</td></tr>
	<tr><td style="text-align:center;">
		<form name="selectorForm" action="dummy">
		<table class="whiteTable" style="text-align:center; padding-left:10px;
				padding-right:10px; padding-top:5px;" summary="">
			<tr><td class="boldtext">Datum type: 
				<% if (isSubstn) { %>
					<input type="hidden" name="dataTypeList" value="<%= SUBSTN %>">
					<% if (isNumeric) { %>
						Values for <i>x<sub>n</sub></i>
					<% } else { %>
						R Groups
					<% } // if numeric %>
				<% } else if (allowTextOnly || isEquations) { %>
					<input type="hidden" name="dataTypeList" value="<%= TEXT %>" /> 
					<% if (isClickableImage) { %>
						Number and Color of Marks
					<% } else if (isDrawVectors) { %>
						Color of Arrows
					<% } else if (isLogicalStmts) { %>
						Additional acceptable words
					<% } else if (isEquations) { %>
						Initial constants, equations, and reserved variable names
					<% } else if (!isTable && !isOED && !isRCD) { %>
						Text 
					<% } else if (isTable && makePreloadTable) { %>
						Prefill Values
					<% } else { %>
						Row/Column Information
					<% } %>
				<% } else if (isSynthesis && numQData <= 1) { %>
					<input type="hidden" name="dataTypeList" value="<%= SYNTH_OK_SM %>" /> 
					Rule about Permissible Starting Materials
				<% } else { %>
					<select name="dataTypeList" onchange="changeQDatumType();">
					<% if (isSynthesis) { %>
						<option value="<%= SYNTH_OK_SM %>" 
								<%= qDatum.dataType == SYNTH_OK_SM ? SELECTED : "" %>> 
							Rule about Permissible Starting Materials </option>
						<option value="<%= SM_EXPR %>" 
								<%= qDatum.dataType == SM_EXPR ? SELECTED : "" %>> 
							How to Combine Rules </option>
					<% } else { // rank or choice %>
						<option value="<%= TEXT %>"
							<%= qDatum.dataType == TEXT ? SELECTED : "" %>> Text 
						</option>
						<option value="<%= MARVIN %>"
							<%= qDatum.dataType == MARVIN ? SELECTED : "" %>> Marvin 
						</option>
					<% } // if question type %>
					</select>
				<% } // if more than one kind of question datum should be available %>
			</td></tr>
		</table>
		</form>
	</td></tr>
</table>
</div>
<div id="qDataContents">
<form name="qDataForm" action="saveQData.jsp" method="post" accept-charset="UTF-8">
	<input type="hidden" name="qdNum" value="<%= cloneEdit ? "0" : qdNum %>" />
	<input type="hidden" name="qdType" value="<%= qDatum.dataType %>" />
	<input type="hidden" name="qdData" value="" />
	<input type="hidden" name="displayOpts" value="" />
	<input type="hidden" name="newQDNum" value="<%= numQData + 1 %>" />
	<input type="hidden" name="tableNum" value="<%= tableNum %>" />
	<input type="hidden" name="afterSave" value="<%= RETURN %>" />
<table style="margin-left:auto; margin-right:auto; width:90%;" summary="">
	<tr><td style="text-align:center;">

	<% if (isSubstn && !isNumeric) { 
		/* Utils.alwaysPrint("loadQData.jsp: masterEdit = ", masterEdit,
				", chosen R Groups = ", qDatum.data); /**/
	%> 
		<table style="margin-left:auto; margin-right:auto; width:445px;"
			summary="">
		<tr><td class="regtext">
			Choose one or more collections of R groups from which ACE should 
			choose R<sup><%= qdNum != 0 ? qdNum : qData.length + 1 %></sup>.
		</td></tr>
		<tr><td>
			<table style="margin-right:auto; margin-left:auto;" summary=""><tr>
			<td><%= makeButton("Select All", "changeAll(ON);") %></td>
			<td><%= makeButton("Toggle All", "changeAll(TOGGLE);") %></td>
			</tr></table>
		</td></tr>
		<tr><td style="text-align:left;">
			<table summary="">
			<% if (!Utils.isEmpty(rGroupIds)) {
				final String modOptions = Utils.toString(':', qDatum.data, ':'); %>
				<tr><td class="boldtext"></td>
					<td class="boldtext" style="padding-left:10px;">
					R-Group Collection
					</td>
					<% if (masterEdit) { %>
						<td class="boldtext" style="padding-left:30px;">Edit</td>
					<% } %>
				</tr>
				<% for (int rgNum = 1; rgNum <= rGroupIds.length; rgNum++) { %>
					<tr><td class="regtext" style="text-align:center; 
							vertical-align:top;">
						<input type="checkbox" name="option<%= rgNum %>"
								id="option<%= rgNum %>"
						<%= modOptions.contains(
									Utils.toString(':', rGroupIds[rgNum - 1], ':'))
								? "checked=\"checked\"" : "" %> >
				 	</td>
					<td class="regtext" style="padding-left:10px; 
							vertical-align:middle;">
						<%= Utils.toDisplay(rGroupNamesByIds.get(rGroupIds[rgNum - 1])) %>
				 	</td>
					<% if (masterEdit) { %>
						<td class="regtext" style="text-align:center; padding-left:30px;
								vertical-align:middle;">
							<%= makeButtonIcon("edit", pathToRoot, 
									"editRGroupColl(", rGroupIds[rgNum - 1], ");") %>
						</td>
					<% } // if masterEdit %>
					</tr>
				<% } // for each rgNum 
			} // if there's at least one reaction %>
			</table>
		</td>
		</tr>
		<% if (!masterEdit) { %>
			<tr>
			<td class="regtext" colspan="2" style="vertical-align:middle;">
				<br />Contact the developers if you would like to add a 
				collection of R groups to this list.  
			</td>
			</tr>
		<% } // if not masterEdit %>
		</table>

	<% } else if (qDatum.dataType == MARVIN) { 
		String molname = "";
		if (qdNum != 0 && qDatum.dataType == MARVIN && qDatum.name != null) {
			molname = qDatum.name;
		}
		final long displayOpts = qDatum.getDisplayOptions();
		final boolean showNoH = Question.showNoHydrogens(displayOpts);
		final boolean showHeteroH = Question.showHeteroHydrogens(displayOpts);
		final boolean showAllH = Question.showAllHydrogens(displayOpts);
		final boolean showAllC = Question.showAllCarbons(displayOpts);
		final boolean showLonePairs = Question.showLonePairs(displayOpts);
		final boolean showMapping = Question.showMapping(displayOpts);
		final boolean showRSLabels = Question.showRSLabels(displayOpts);
	%> 
		<table style="margin-left:auto; margin-right:auto; width:445px;"
			summary="">
		<tr>
			<td class="regtext" style="width:90%;">
				Name: <input type="text" name="molname" size="55" 
						maxlength="80" value="<%= Utils.toValidTextbox(molname) %>" />
			</td>
		</tr>
   		<tr><td class="boldtext" style="text-align:right; padding-left:10px; 
				padding-right:10px; padding-top:10px; font-style:italic;">
			MarvinJS&trade;
		</td></tr>
		<tr><td style="text-align:center; padding-left:10px; padding-right:10px;">
			<table class="whiteTable" summary=""><tr><td>
				<div id="<%= APPLET_NAME %>">
				<script type="text/javascript">
					// <!-- >
					startMarvinJS('<%= Utils.toValidJS( 
								MolString.convertMol(qDatum.data, MRV_EXPORT)) %>', 
							 MARVIN, <%= displayOpts %>, '<%= APPLET_NAME %>', 
							 '<%= pathToRoot %>'); 
					// -->
				</script>
				</div>
			</td></tr>
			</table>
		</td></tr>
		<tr><td class="regtext" style="text-align:left; padding-top:10px;">
			Display options:
			<table>
			<tr><td>
				<input type="checkbox" value="true" name="howShowHydrogens" <%= 
						showNoH || showHeteroH || showAllH || showAllC ? CHECKED : "" %> /> 
			</td><td>show 
				<select name="allNo">
					<option value="<%= Question.SHOWNOH %>"
							<%= showNoH ? SELECTED : "" %>>explicit H atoms only</option>
					<option value="<%= Question.SHOWHETEROH %>"
							<%= showHeteroH ? SELECTED : "" %>>heteroatom H atoms</option>
					<option value="<%= Question.SHOWALLH %>"
							<%= showAllH ? SELECTED : "" %>>all H atoms</option>
					<option value="<%= Question.SHOWALLH | Question.SHOWALLC %>"
							<%= showAllC ? SELECTED : "" %>>all H and C atoms</option>
				</select>
			</td></tr>
			<tr><td>
				<input type="checkbox" name="withLonePairs"
						<%= showLonePairs ? CHECKED : "" %> /> 
			</td><td>show lone pairs
			</td></tr>
			<tr><td>
				<input type="checkbox" value="true" name="withMapping"
						<%= showMapping ? CHECKED : "" %> /> 
			</td><td>show mapping
			</td></tr>
			<tr><td>
				<input type="checkbox" value="true" name="withRSLabels"
						<%= showRSLabels ? CHECKED : "" %> /> 
			</td><td>show R,S labels
			</td></tr>
			</table>
		</td></tr>
		</table>

	<% } else if (qDatum.dataType == SYNTH_OK_SM) {
		final String[] OPER_ENGLISH = Atoms.OPER_ENGLISH[Atoms.FEWER]; // also Rings %> 

		<table style="margin-left:auto; margin-right:auto; width:445px;"
			summary="">
		<tr><td class="regtext" style="padding-bottom:20px;">
			<table class="whiteTable" summary="">
			<tr><td class="boldtext">
			Type of rule:
			<select name="evalTypeSelector" onchange="changeSMRule()">
				<option value="<%= NUM_ATOMS %>" <%= 
						setEvalType == NUM_ATOMS 
						? SELECTED : "" %>>Atom count</option>
				<option value="<%= HAS_FORMULA %>" <%= 
						setEvalType == HAS_FORMULA 
						? SELECTED : "" %>>Formula</option>
				<option value="<%= FUNCTIONAL_GROUP %>" <%= 
						setEvalType == FUNCTIONAL_GROUP 
						? SELECTED : "" %>>Functional group</option>
				<option value="<%= IS_OR_HAS_SIGMA_NETWORK %>" <%= 
						setEvalType == IS_OR_HAS_SIGMA_NETWORK 
						? SELECTED : "" %>>Marvin structures</option>
				<option value="<%= COUNT_METALS %>" <%= 
						setEvalType == COUNT_METALS 
						? SELECTED : "" %>>Metal-free</option>
				<option value="<%= NUM_RINGS %>" <%= 
						setEvalType == NUM_RINGS 
						? SELECTED : "" %>>Ring count</option>
				<option value="<%= TOTAL_CHARGE %>" <%= 
						setEvalType == TOTAL_CHARGE 
						? SELECTED : "" %>>Uncharged &amp; no alkali metal or Mg</option>
			</select>
			</td></tr>
			</table>
		</td></tr>


	<%	if (setEvalType == IS_OR_HAS_SIGMA_NETWORK) { 
			int flags = 0;
			boolean isPositive = true; 
			String molname = "";
			String molstruct = "";
			if (rule.getEvalType() == IS_OR_HAS_SIGMA_NETWORK) {
				// "this compound is" == "any of these compounds is"
				final Is evalIs = (Is) rule.getEvaluator();
				flags = evalIs.getFlags(); // whether either enantiomer acceptable
				isPositive = (evalIs.getHowMany() == Is.ANY);
				molname = rule.getMolName();
				molstruct = rule.getMolStruct();
			} // if have previous values to load
	%>
	
		<tr><td class="regtext">
			A permissible starting material
			<select name="isIsPos" id="isIsPos">
				<option value="<%= Is.ANY %>" <%= isPositive 
						? SELECTED : "" %>>is among</option>
				<option value="<%= Is.NONE %>" <%= !isPositive 
						? SELECTED : "" %>>is not among</option>
			</select>
		</td></tr>
		<tr><td class="regtext" style="width:90%;">
			Name(s) (do not use the / character): 
			<br /><input type="text" name="molname" size="55" 
					value="<%= Utils.toValidTextbox(molname) %>" />
		</td></tr>
   		<tr><td class="boldtext" style="text-align:right; 
				padding-left:10px; padding-right:10px; padding-top:10px; 
				font-style:italic;">
			MarvinJS&trade;
		</td></tr>
		<tr><td style="text-align:center; padding-left:10px; 
				padding-right:10px;">
			<table class="whiteTable" summary=""><tr><td>
				<div id="<%= APPLET_NAME %>">
				<script type="text/javascript">
					// <!-- >
					startMarvinJS('<%= Utils.toValidJS( 
								MolString.convertMol(molstruct, MRV_EXPORT)) %>', 
							MARVIN, 0, '<%= APPLET_NAME %>');
					// -->
				</script>
				</div>
			</td></tr>
			</table>
		</td></tr>
		<tr><td class="regtext" 
				style="vertical-align:middle;">
			<input type="checkbox" name="enantiomer"
					value="<%= Is.EITHER_ENANTIOMER %>"
					<%= (flags & Is.EITHER_ENANTIOMER) != 0 
							? "checked=\"checked\"" : "" %> />
				&nbsp;either enantiomer of each compound acceptable
		</td></tr>

	<%	} else if (setEvalType == FUNCTIONAL_GROUP) { 
			int initialGroupId = 0;
			boolean isPositive = true; 
			if (rule.getEvalType() == FUNCTIONAL_GROUP) {
				// "contains" == "number != 0"
				final FnalGroup evalFnalGroup = (FnalGroup) rule.getEvaluator();
				initialGroupId = evalFnalGroup.getGroupId();
				isPositive = evalFnalGroup.getOper() == FnalGroup.NOT_EQUALS;
				// number = 0; // understood
			} // if have previous values to load
	%>
	
		<tr><td class="regtext">
			A permissible starting material
			<select name="fnalIsPos" id="fnalIsPos">
				<option value="<%= SYMBOLS[NOT_EQUALS] %>" <%= isPositive 
						? SELECTED : "" %>>contains</option>
				<option value="<%= SYMBOLS[EQUALS] %>" <%= !isPositive 
						? SELECTED : "" %>>does not contain</option>
			</select>
			the functional group
		</td></tr>
		<tr><td style="text-align:left; padding-left:20px;">
			<b>Select a functional group category:</b>
			<select name="categories" id="categories" 
					size="1" style="width:300px;" 
					onchange="loadGroupsInChangedCat(document.qDataForm)" >
			</select>
		</td></tr>
		<tr><td style="text-align:left; padding-left:20px;">
			<p><b>Select a functional group:</b>
			<span id="groupsPopup">
			<select name="fnalGroupId" size="1" style="width:300px;">
				<option value="0">No group selected</option>
			</select>
			</span>
			<script type="text/javascript">
				// <!-- >
				<% for (int grpNum = 1; grpNum <= sortedGroups.length; grpNum++) { 
					final FnalGroupDef group = sortedGroups[grpNum - 1]; %>
					setArrayValues(<%= grpNum %>,
							'<%= Utils.toValidJS(group.getPulldownName()) %>',
							'<%= Utils.toValidJS(group.category) %>',
							<%= group.groupId %>);
				<% } // for each group %>
				initFnalGroupConstants(<%= initialGroupId %>);
				setCatSelector();
				initializeGroupSelector(document.qDataForm);
				// -->
			</script>
		</td></tr>

	<%	} else if (setEvalType == HAS_FORMULA) { 
			String formula = ""; 
			boolean isPositive = true;
			if (rule.getEvalType() == HAS_FORMULA) { // existing evaluator
				final HasFormula evalForm = (HasFormula) rule.getEvaluator();
				formula = evalForm.getFormula();
				isPositive = evalForm.getIsPositive();
			} // if have previous values to load
	%>

		<tr><td class="regtext">
			A permissible starting material
			<select name="formulaIsPos">
				<option value="<%= SYMBOLS[NOT_EQUALS] %>" <%= isPositive ? SELECTED : "" %> >
					has</option>
				<option value="<%= SYMBOLS[EQUALS] %>" <%= !isPositive ? SELECTED : "" %> >
					does not have</option>
			</select>
			the formula
			<br /><input type="text" name="formula"
					value="<%= formula %>" size="50" />
			<span style="color:green;">
				<p>Use the format 
				C<sub><i>m</i></sub>H<sub><i>n</i></sub>XY<sub><i>p</i></sub>, 
				e.g., C7H5ClO2. </p>
				<p>Use * to match with any number in the response, including zero.</p>
				<p>Do <i>not</i> repeat elements or use parentheses, 
				as in (CH3)3COH.</p>
			</span>
		</td></tr>

	<%	} else if (setEvalType == NUM_ATOMS) { 
			int atomsOper = Atoms.NOT_GREATER;
			int numAtoms = 4;
			String element = "C";
			boolean contiguous = true;
			if (rule.getEvalType() == NUM_ATOMS) {
				final Atoms evalAtoms = (Atoms) rule.getEvaluator();
				atomsOper = evalAtoms.getAtomsOper();
				numAtoms = evalAtoms.getNumAtoms();
				element = evalAtoms.getElement();
				contiguous = evalAtoms.getContiguous();
			} // if have previous values to load
	%>

		<tr><td class="regtext">
			A permissible starting material contains
			<br/><select name="atomsOper">
				<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
					<option value="<%= SYMBOLS[o] %>" 
							<%= o == atomsOper ? SELECTED : "" %> >
					<%= OPER_ENGLISH[o] %></option>
				<% } %>
			</select>
			<input name="numAtoms" type="text" size="2" 
					value="<%= numAtoms %>" />
			<select name="contiguous">
				<option value="Y" <%= contiguous ? 
						"selected" : "" %> > contiguous</option> 
				<option value="N" <%= !contiguous ? 
						"selected" : "" %> > total</option>
			</select>
			<input name="element" type="text" size="2" value="<%= element %>" />
			atom(s).
		</td></tr>

	<%	} else if (setEvalType == NUM_RINGS) { 
			int ringsOper = Rings.EQUALS;
			int numRings = 0;
			if (rule.getEvalType() == NUM_RINGS) {
				final Rings evalRings = (Rings) rule.getEvaluator();
				ringsOper = evalRings.getRingsOper();
				numRings = evalRings.getNumRings();
			} // if have previous values to load
	%>

		<tr><td class="regtext">
			A permissible starting material contains
			<br/><select name="ringsOper">
				<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
					<option value="<%= SYMBOLS[o] %>" 
							<%= o == ringsOper ? SELECTED : "" %> >
					<%= OPER_ENGLISH[o] %></option>
				<% } %>
			</select>
			<input name="numRings" type="text" size="2" value="<%= numRings %>" />
			ring(s).
		</td></tr>

	<%	} else if (setEvalType == COUNT_METALS) {
			final String[] ENGL_METALS = CountMetals.ENGL_METALS;
			int metalKind = CountMetals.ALL_METALS;
			if (rule.getEvalType() == COUNT_METALS) {
				final CountMetals evalCountMetals = (CountMetals) rule.getEvaluator();
				metalKind = evalCountMetals.getMetalKind();
				/* Utils.alwaysPrint("loadQData.jsp: metalKind = ",
						metalKind, ", meaning ", ENGL_METALS[metalKind]); /**/
			} // if have previous values to load
			/* else Utils.alwaysPrint("loadQData.jsp: changing different evaluator "
					+ "type to CountMetals."); /**/
	%>

		<tr><td class="regtext">
			A permissible starting material contains no 
			<br/><select name="metalKind">
				<% for (int o = 0; o < ENGL_METALS.length; o++) { %>
					<option value="<%= CountMetals.DB_METALS[o] %>" 
							<%= o == metalKind ? SELECTED : "" %> >
					<%= ENGL_METALS[o] %></option>
				<% } %>
			</select>
			metals.
		</td></tr>

	<%	} else { // setEvalType == TOTAL_CHARGE %>

		<tr><td class="regtext">
			A permissible starting material is uncharged and contains no alkali 
			metal or Mg.
		</td></tr>

	<%	} // if rule.getEvalType() %>
		
		</table>

	<% } else if (qDatum.dataType == SM_EXPR) {
	%> 
	<input type="hidden" name="qDatumData" value="<%= qDatum.data %>" />
	<tr><td class="regtext" style="color:green;
			padding-top:10px; padding-bottom:10px;">
		Enter an expression for combining the rules about permissible starting
		materials.  You may use as many sets of parentheses as you wish.  You do
		not need to combine the rules in the order in which they are written.
		You may use a rule more than once in the expression.
	</td></tr>
	<tr><td id="combinationExpression" style="text-align:left; padding-top:10px;">
	</td></tr>
	<tr><td style="padding-top:10px;">
		<table class="regtext" summary="">
			<% for (int qdIdx = 0; qdIdx < numQData; qdIdx++) { 
				String descrip = "";
				if (qData[qdIdx].data.indexOf('/') >= 0) {
					final SynthStarterRule theRule = 
							new SynthStarterRule(qData[qdIdx].data);
					descrip = "Permissible starting materials " 
							+ Utils.toDisplay(theRule.toEnglish());
				} else continue; 
			%>
				<tr><td style="padding-left:10px;"><b>Rule <%= qdIdx + 1 %></b>: 
					<%= descrip %>.
				</td></tr>
			<% } // for each question datum %>
		</table>

	<% } else { // qDatum.dataType == TEXT or SUBSTN %> 

		<% if (isTable) {
			if (!makePreloadTable) {
		%>
				<input type="hidden" name="isTableQRowOrCol" value="true" />
				<table
					style="margin-left:auto; margin-right:auto; width:445px;"
					summary="">
				<tr>
				<td class="regtext">
					Enter the number of <%= isRow ? "rows" : "columns" %> of data:
					<br />
					<input type="text" name="numRowsOrCols" id="numRowsOrCols"
							onkeyup="javascript:updateCaptionsTable();"
							size="10" value="<%= numRowsOrCols %>"/>
				</td></tr>
				<tr><td class="regtext" style="padding-top:10px;">
					<span id="enterCaptions" style="visibility:<%= 
							numRowsOrCols == 0 && isRow ? "hidden" : "visible" %>">
					Enter the <%= isRow ? "row" : "column" %> captions (optional).
					</span>
					<br />
					<span id="captionsTable">
						<table>
						<% final int numCaptionFields = 
								numRowsOrCols + (isRow ? 0 : 1);
						for (int captionNum = 0; captionNum < numCaptionFields; 
								captionNum++) { %>
							<tr><td>
								<% if (!isRow && captionNum == 0) { %>
									Column of<br/>row captions:
								<% } else { %>
									<%= isRow ? "Row" : "Column" %> 
									<%= captionNum + (isRow ? 1 : 0) %>:
								<% } // if column 0 %>
							</td>
							<td><input type="text" id="caption<%= captionNum %>"
									name="caption<%= captionNum %>" size="60" 
									value="<%= captionNum < captions.length
										? Utils.toValidTextbox(captions[captionNum]) 
										: "" %>" />
							</td></tr>
						<% } // for each caption %>
						</table>
					</span>
				</td></tr>
				</table>
			<% } else { 
				final TableQ tq = new TableQ(qDatum.data);
			%>
				<input type="hidden" name="<%= TableQ.NUM_ROWS_TAG %>" 
						value="<%= ((CaptionsQDatum) qData[TableQ.ROW_DATA]).getNumRowsOrCols() %>" />
				<input type="hidden" name="<%= TableQ.NUM_COLS_TAG %>" 
						value="<%= ((CaptionsQDatum) qData[TableQ.COL_DATA]).getNumRowsOrCols() %>" />
				<input type="hidden" name="preloadTable" value="true" /> 
				<table class="regtext" style="margin-left:auto; 
						margin-right:auto; width:445px;" summary="">
				<tr>
				<td style="text-align:left; padding-bottom:10px;">
					Enter values that you want to be in the table when the student 
					first loads it.  Check the boxes to the right of the cells whose 
					contents you do not wish to allow the students to modify. 
				</td></tr>
				<tr>
				<td style="text-align:center;">
					<%= tq.convertToHTML(qData, chemFormatting, TableQ.AUTH_INPUT) %>
				</td></tr>
				</table>
			<% } // if make preload table %>
		<% } else if (isOED || isRCD) {
			final EDiagramQDatum eqDatum = (EDiagramQDatum) qDatum;
			final int rows = eqDatum.getNumRows();
			String[] edCaptions = eqDatum.captions;
			if (Utils.isEmpty(edCaptions)) edCaptions = new String[] {"", "", ""};
			final String[] labels = eqDatum.labels;
			%>
			<table style="margin-left:auto; margin-right:auto; width:445px;"
				summary="">
			<tr><td class="regtext" style="padding-right:10px;">
				Enter the number of rows:
			</td><td class="regtext" style="width:50%;">
				<input type="text" id="numRows" name="numRows"
						size="10" value="<%= rows == 0
								? "" : rows %>"/>
			</td></tr>
			<% if (isRCD) { 
				final int cols = eqDatum.getNumColumns(); %>
				<tr><td class="regtext" style="padding-right:10px;">
					Enter the number of columns:
				</td><td class="regtext" style="width:50%;">
					<input type="text" name="numCols"
							size="10" value="<%= cols == 0
								? "" : cols %>" />
				</td></tr>
			<% } else { %>
				<tr><td class="regtext">
					Enter the caption for column 1:
				</td><td class="regtext">
					<input type="text" name="col1Caption"
							size="10" value="<%= edCaptions[0] %>"/>
				</td></tr>
				<tr><td class="regtext">
					Enter the caption for column 2:
				</td><td class="regtext">
					<input type="text" name="col2Caption"
							size="10" value="<%= edCaptions.length > 1 
								? edCaptions[1] : "" %>"/>
				</td></tr>
				<tr><td class="regtext">
					Enter the caption for column 3:
				</td><td class="regtext">
					<input type="text" name="col3Caption"
							size="10" value="<%= edCaptions.length > 2 
								? edCaptions[2] : "" %>"/>
				</td></tr>
			<% } // if qType %>
			<tr><td colspan="2" class="regtext" 
					style="padding-top:10px; padding-bottom:10px;">
				Enter the labels (if any) for the <%= isOED
						? "orbitals in the second column"
						: "maxima and minima" %>:
			</td></tr>
			<tr><td colspan="2" class="regtext"> 
				<div id="labelTable">
				<table>
				<% for (int lblNum = 0; lblNum < labels.length; lblNum++) { %>
					<tr><td class="regtext" style="padding-right:10px;">
						Label <%= lblNum + 1 %>:
					</td><td class="regtext">
						<input type="text" name="label<%= lblNum + 1 %>"
								id="label<%= lblNum + 1 %>" size="20" 
								value="<%= Utils.toValidTextbox(labels[lblNum]) %>" />
					</td><td class="boldtext big" style="color:red;">
						<span onclick="javascript:removeLabel(<%= 
								lblNum + 1 %>)">&times;</span>
					</td></tr>
				<% } // for each label %>
				<tr><td colspan="3" class="regtext">
					<%= makeButton("Add label", "addLabel();") %>
				</td></tr>
				</table>
				</div>
			</td></tr>
			</table>

			<table class="regtext" style="margin-left:auto; margin-right:auto; 
					width:445px;" summary="">
				<tr><td style="padding-top:20px;">
				<input type="checkbox" name="useYAxisScale" id="useYAxisScale" 
						<%= yAxisData.haveLabels() ? CHECKED : "" %>
						onchange="setYAxisScaleData();" />
				</td><td style="padding-top:20px;">Add a scale along the y axis
				</td></tr>
			</table>
			<span id="yAxisScaleTable"></span>

		<% } else if (isClickableImage) {
			final String[] colorAndMaxMarks = ClickImage.getColorAndMaxMarks(qDatum.data);
		%>
			<table style="margin-left:auto; margin-right:auto; width:445px;"
					summary="">
			<tr><td class="regtext" style="vertical-align:middle;">
				Enter the number of marks that the student may make (&minus;1 for any number): 
				<input type="text" name="numMarks" size="5" value="<%= 
						Utils.toValidTextbox(colorAndMaxMarks[ClickImage.NUM_MARKS]) %>" />
			</td></tr>
			<tr><td class="regtext" style="vertical-align:middle;">
				Enter the desired color: 
				<input type="text" name="colorValue" size="20" value="<%= 
						Utils.toValidTextbox(colorAndMaxMarks[ClickImage.COLOR]) %>" />
			</td></tr>
			</table>
		<% } else if (isEquations) { %>
			<table style="margin-left:auto; margin-right:auto; width:445px;"
					summary="">
			<tr><td class="regtext">
				Enter the constants and equations to preload.  The student 
				will be able to add new equations only after the ones you enter.
			</td></tr>
			<tr><td id="disableConstantsCell" class="regtext" style="padding-top:10px;">
			</td></tr>
			<tr><td class="regtext" style="color:green;">
				[preloaded equations are always uneditable]
			</td></tr>
			<tr><td id="eqnsTable" class="regtext" 
					style="padding-top:10px; vertical-align:middle;">
			</td></tr>
			<tr><td class="regtext">
				<hr>
			</td></tr>
			<tr><td class="regtext" style="padding-top:10px;">
				Enter comma-separated names of variables, in addition to those in 
				the constants list, that ACE should <b><i>not</i></b> interpret 
				as unit symbols. Include any units whose names begin with a letter 
				that ACE might interpret as an order-of-magnitude prefix.
			</td></tr>
			<tr><td>
				<input type="text" name="<%= Equations.VARS_NOT_UNITS_TAG %>" 
						id="<%= Equations.VARS_NOT_UNITS_TAG %>" size="50" value="" />
			</td></tr>
			<tr><td class="regtext" style="padding-top:10px;">
				Unit symbols recognized by ACE:
				<blockquote style="font-family:times, serif; font-size:14px;">
				<% final CanonicalizedUnit[] units = CanonicalizedUnitRW.getAllUnits();
				for (int unitNum = 1; unitNum <= units.length; unitNum++) { 
					final String symbol = units[unitNum - 1].getSymbol(); 
					if (!symbol.startsWith("&#") && !"&mu;".equals(symbol)) { %>
						<b><%= symbol %></b><%= unitNum < units.length ? "," : "" %>
					<% } // if unit is already displayed %>
				<% } // for each unit %>
				</blockquote>
				Order-of-magnitude prefixes recognized by ACE:
				<blockquote style="font-family:times, serif; font-size:14px;">
				<% final String[][] PREFIXES = EquationFunctions.PREFIXES_MAGNITUDES;
				for (int preNum = 1; preNum <= PREFIXES.length; preNum++) { 
					final String symbol = Utils.unicodeToCERs(PREFIXES[preNum - 1][0]); 
					if (!symbol.startsWith("&#9")) { %>
						<b><%= symbol %></b><%= preNum < PREFIXES.length ? "," : "" %>
					<% } // if unit is already displayed %>
				<% } // for each unit %>
				</blockquote>
			</td></tr>
			</table>
		<% } else { // not table or OED or RCD or click-image or equations %>
			<table style="margin-left:auto; margin-right:auto; width:445px;"
					summary="">
			<tr><td class="regtext" style="vertical-align:top;">
				Enter <%= isDrawVectors ? "the desired color:"
						: isLogicalStmts ? "the additional acceptable words, "
							+ "separated by spaces:"
						: isNumeric && isSubstn ? Utils.toString(
							"possible values for <i>x<sub>n</sub></i>. Format:"
							+ "<blockquote>"
							+ "value<sub>1</sub> ", SubstnUtils.SUBSTNS_SEP, 
							" value<sub>2</sub> ", SubstnUtils.SUBSTNS_SEP,
							" value<sub>3</sub> ", SubstnUtils.SUBSTNS_SEP,
							" etc. </blockquote>"
							+ "from which ACE will choose one arithmetic value "
							+ "to substitute into both the statement and "
							+ "evaluators that refer to <i>x<sub>n</sub></i>, or:"
							+ "<blockquote>"
							+ "word<sub>1</sub> ", SubstnUtils.WORD_VALUE_SEP,  
							" value<sub>1</sub> ", SubstnUtils.SUBSTNS_SEP, 
							" word<sub>2</sub> ", SubstnUtils.WORD_VALUE_SEP,  
							" value<sub>2</sub> ", SubstnUtils.SUBSTNS_SEP,
							" word<sub>3</sub> ", SubstnUtils.WORD_VALUE_SEP,  
							" value<sub>3</sub> ", SubstnUtils.SUBSTNS_SEP,
							" etc. </blockquote>"
							+ "from which ACE will choose one word&ndash;value pair, "
							+ "substituting the word into the statement and the "
							+ "corresponding arithmetic value into evaluators that "
							+ "refer to <i>x<sub>n</sub></i>.")
						: "data:" %> 
			</td></tr><tr><td class="regtext">
				<textarea name="qDataTextArea" cols="65" rows="10"><%= 
						Utils.toValidTextbox(qDatum.data) %></textarea>
			</td></tr>
			</table>
		<% } // if qType %>

	<% } // if qDatum.dataType %> 

	</td></tr>
</table>
</form>
</div>
<div id="qDataFooter">
<table style="text-align:center; margin-left:auto; margin-right:auto;" summary="">
	<tr>
		<td><%= makeButton("Save, close", "submitIt('", RETURN, "');") %></td>
		<% if (qDatum.dataType != SM_EXPR && !oneQDatumOnly
				&& (!isTable || numQData < TableQ.MAX_QDATA - 1
						|| (numQData < TableQ.MAX_QDATA && qdNum != 0))) { %>
			<td style="padding-left:5px;"><%= makeButton("Save, add new", 
					"submitIt('", ADD_NEW, "');") %></td>
			<% if (!oneQDatumOnly && !isTable) { %>
				<td style="padding-left:5px;"><%= makeButton("Save, clone", 
						"submitIt('", ADD_CLONE, "');") %></td>
			<% } // if not table or energy diagram %>
		<% } // if should allow to add a new qDatum %>
		<td style="padding-left:5px;"><%= makeButton("Cancel", "cancelMe();") %></td>
	<% if (isSubstn && !isNumeric && masterEdit) { %>
		<td style="padding-left:5px;"><%= makeButton(
				"Add New Collection", "editRGroupColl(0);") %></td>
	<% } // if R group and masterEdit %>
	</tr>
</table>
</div>
</body>
</html>

