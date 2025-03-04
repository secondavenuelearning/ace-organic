<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.evals.impl.implConstants.TableImplConstants,
	com.epoch.evals.impl.genericQEvals.*,
	com.epoch.evals.impl.genericQEvals.clickEvals.*,
	com.epoch.evals.impl.genericQEvals.multEvals.*,
	com.epoch.evals.impl.genericQEvals.numericEvals.*,
	com.epoch.evals.impl.genericQEvals.rankEvals.*,
	com.epoch.evals.impl.genericQEvals.tableEvals.*,
	com.epoch.evals.impl.genericQEvals.textEvals.*,
	com.epoch.genericQTypes.*,
	com.epoch.physics.DrawVectors,
	com.epoch.qBank.CaptionsQDatum,
	java.util.List"
%>

<% final String SELF = "loadEvaluatorGeneric.jsp"; %>

<%@ include file="loadEvaluatorJava.jsp.h" %>

<%
	final int CHOICE_WHICH_CHECKED = EvalManager.CHOICE_WHICH_CHECKED;
	final int CHOICE_NUM_CHECKED = EvalManager.CHOICE_NUM_CHECKED;
	final int RANK_ORDER = EvalManager.RANK_ORDER;
	final int RANK_POSITION = EvalManager.RANK_POSITION;
	final int CLICK_HERE = EvalManager.CLICK_HERE;
	final int CLICK_TEXT = EvalManager.CLICK_TEXT;
	final int CLICK_NUM = EvalManager.CLICK_NUM;
	final int CLICK_CT = EvalManager.CLICK_CT;
	final int CLICK_LABELS_COMP = EvalManager.CLICK_LABELS_COMP;
	final int NUM_IS = EvalManager.NUM_IS;
	final int NUM_SIGFIG = EvalManager.NUM_SIGFIG;
	final int NUM_UNIT = EvalManager.NUM_UNIT;
	final int STMTS_CT = EvalManager.STMTS_CT;
	final int TEXT_CONT = EvalManager.TEXT_CONT;
	final int TEXT_WORDS = EvalManager.TEXT_WORDS;
	final int TEXT_SEMANTICS = EvalManager.TEXT_SEMANTICS;
	final int TABLE_DIFF = EvalManager.TABLE_DIFF;
	final int TABLE_TEXT = EvalManager.TABLE_TEXT;
	final int TABLE_NUM = EvalManager.TABLE_NUM;
	final int TABLE_CT_NUM = EvalManager.TABLE_CT_NUM;
	final int TABLE_CT_TXT = EvalManager.TABLE_CT_TXT;
	final int TBL_TXT_TXT = EvalManager.TBL_TXT_TXT;
	final int TBL_TXT_NUM = EvalManager.TBL_TXT_NUM;
	final int TBL_NUM_TXT = EvalManager.TBL_NUM_TXT;
	final int TBL_NUM_NUM = EvalManager.TBL_NUM_NUM;

	final Figure[] figures = question.getFigures();
	final boolean chemFormatting = question.chemFormatting();
	String authText = "";
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>

<%@ include file="loadEvaluatorJS.jsp.h" %>

<% if (Utils.among(evalConstant, CLICK_HERE, CLICK_NUM, CLICK_TEXT, CLICK_LABELS_COMP)) { %>
	<script src="<%= pathToRoot %>js/drawOnFig.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/offsets.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/position.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/wz_jsgraphics.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<% } else if (evalConstant == TEXT_SEMANTICS) { %>
	<script src="<%= pathToRoot %>js/logicStmts.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/wordCheck.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<% } // if evalConstant %>
<script type="text/javascript">
	// <!-- >

	// used by various NUM and TABLE evaluators
	function validNumAndTolerance(numField, toleranceField) {
		return validNum(numField) && validTolerance(toleranceField);
	} // validNumAndTolerance()

	function validNum(numField) {
		if (isWhiteSpace(numField.value) || !canParseToFloat(numField.value)) {
			alert('Enter a valid number, not ' + numField.value + '.');
			return false;
		}
		return true;
	} // validNum()

	function validTolerance(toleranceField) {
		if (!isWhiteSpace(toleranceField.value) 
				&& !canParseToFloat(toleranceField.value)) {
			alert('Enter a valid tolerance value, or enter nothing for 0.');
			return false;
		}
		return true;
	} // validTolerance()

	// used by various TEXT and TABLE evaluators
	function writeCaseSelector(ignore) {
		var SELECTED = '<%= SELECTED %>';
		var bld = new String.builder().
				append('<select name="ignoreCase"><option value="true"');
		if (ignore) bld.append(SELECTED);
		bld.append('>ignoring<\/option><option value="false"');
		if (!ignore) bld.append(SELECTED);
		bld.append('>heeding<\/option><\/select>');
		setInnerHTML('caseSelector', bld.toString());
	} // writeCaseSelector()

	// used by various TEXT and TABLE evaluators
	function setIgnoreCase() {
		var form = document.evaluatorForm;
		var ignoreCell = document.getElementById('ignoreCaseSpan');
		var whereValue = form.where.value;
		if (['<%= TextContains.MATCHES_REGEX %>', 
				'<%= TextContains.CONT_REGEX %>'].contains(whereValue)) {
			ignoreCell.style.visibility = 'hidden';
			form.ignoreCase.value = 'false';
		} else if (ignoreCell.style.visibility === 'hidden') {
			form.ignoreCase.value = 'true';
			ignoreCell.style.visibility = 'visible';
		}
	} // setIgnoreCase()

	<% if (Utils.among(evalConstant, TABLE_NUM, TABLE_CT_NUM, 
			TABLE_TEXT, TABLE_CT_TXT)) { %>

	function rowChange() {
		var form = document.evaluatorForm;
		var rowSelValue = form.rowSelector.value;
		setVisibility('rowNumCell', rowSelValue === '0' ? 'visible' : 'hidden');
		if (document.getElementById('tableValAdvice')) {
			var colSelValue = form.columnSelector.value;
			if (form.rowSelector.value === '<%= TableImplConstants.EVERY %>'
					&& ['<%= TableImplConstants.ANY %>', 
						'<%= TableImplConstants.NO %>'].contains(colSelValue)) {
				setInnerHTML('tableValAdvice', getAdvice(colSelValue));
			} else clearInnerHTML('tableValAdvice');
		}
	} // rowChange()

	function columnChange() {
		var form = document.evaluatorForm;
		var colSelValue = form.columnSelector.value;
		setVisibility('columnNumCell', colSelValue === '0' ? 'visible' : 'hidden');
		if (document.getElementById('tableValAdvice')) {
			if (form.rowSelector.value === '<%= TableImplConstants.EVERY %>'
					&& ['<%= TableImplConstants.ANY %>', 
						'<%= TableImplConstants.NO %>'].contains(colSelValue)) {
				setInnerHTML('tableValAdvice', getAdvice(colSelValue));
			} else clearInnerHTML('tableValAdvice');
		}
	} // columnChange()

	function getAdvice(type) {
		return 'Better, "If ' + (type === '<%= TableImplConstants.ANY %>'
					? 'any' : 'no') + ' column has in every row <%= 
				evalConstant == TABLE_NUM
					? "a numerical value" : "text" %> that is ..."';
	} // getAdvice()

	<% } else if (evalConstant == TABLE_DIFF) { %>

	function setNumericInfo() {
		var numericInfoCell = document.getElementById('numericInfo');
		if (numericInfoCell) {
			numericInfoCell.style.visibility = 
					(document.evaluatorForm.where.value === '<%= TextContains.IS %>'
					? 'visible' : 'hidden');
		}
	} // setNumericInfo()

	function changeCalcGrade() {
		var calcsGrade = document.evaluatorForm.calcGrade.checked;
		toggleGradeSelector(!calcsGrade);
	} // changeCalcGrade()
	
	<% } else if (Utils.among(evalConstant, CLICK_HERE, CLICK_NUM, 
			CLICK_TEXT, CLICK_LABELS_COMP)) { 
		final String[] colorAndMaxMarks = 
				ClickImage.getColorAndMaxMarks(Utils.isEmpty(qData) 
					? null : qData[0].data); %>

 		<%@ include file="/js/drawOnFig.jsp.h" %>

		function initDrawOnFigure() {
			<% if (Utils.isEmpty(figures) || !figures[0].hasImage()) { %>
				alert('Figure 1 of your question must be an image '
						+ 'before you can write a click-here evaluator.');
				var form = document.evaluatorForm;
				form.evalList.value = <%= HUMAN_REQD %>;
				changeEvaluator();
			<% } else { %>
				initDrawOnFigConstants();
				initDrawOnFigGraphics('<%= colorAndMaxMarks[ClickImage.COLOR] %>');
				captureClicks();
				<% if (!Utils.isEmpty(inputSubeval.molStruct)) {
					final ClickHere clickHere = new ClickHere();
					authText = Utils.unicodeToCERs(
							clickHere.extractShapes(inputSubeval.molStruct));
					final int[][][] allShapes = clickHere.getAllShapes();
					int shapeNum = 0;
					for (final int[][] shapes : allShapes) {
						for (final int[] coordsDims : shapes) { %>
							allShapes[<%= shapeNum %>].push(
									[canvasSetX(<%= coordsDims[ClickImage.X] %>), 
									canvasSetY(<%= coordsDims[ClickImage.Y] %>),
								<%= coordsDims[ClickImage.WIDTH] %>, 
								<%= coordsDims[ClickImage.HEIGHT] %>]);
					<%	} // for each region of a shape
						shapeNum++;
					} // for each type of shape %>
					paintAll();
				<% } // if there's input 
			} // if don't have image to write evaluator %>
		} // initDrawOnFigure()

		function shapeAdvice() {
			alert('To draw a shape enclosing a region, click-hold-drag on the image. '
					+ 'In the case of rectangles, the click defines a corner '
					+ 'of the rectangle. In the case of circles and ellipses, the '
					+ 'click defines the center.');
		} // shapeAdvice()

	<% } // if evalConstant %>

	function initSelectors() {
		var form = document.evaluatorForm;
		var initIsPositive = (form.initIsPositive
				&& form.initIsPositive.value === 'true');
		<% switch (evalConstant) {
		case TEXT_SEMANTICS: %>
			writeIsPosSelector(initIsPositive, 'has', 'does not have');
		<% break;
		case CLICK_TEXT:
		case TBL_NUM_TXT:
		case TBL_TXT_TXT:
		case TABLE_TEXT:
		case TEXT_CONT: 
		case TABLE_CT_TXT: %>
			writeIsPosSelector(initIsPositive, 'does', 'does not');
		<% // no break;
		case TBL_TXT_NUM:
		case TABLE_DIFF: %>
			writeCaseSelector(form.initIgnoreCase.value === 'true'); 
		<% break;
		case CHOICE_WHICH_CHECKED:
		case CHOICE_NUM_CHECKED:
		case NUM_UNIT: %>
			writeIsPosSelector(initIsPositive, 'has', 'has not');
		<% break;
		case CLICK_LABELS_COMP: %>
			writeIsPosSelector(initIsPositive, 'identical to', 'different from');
			writeCaseSelector(form.initIgnoreCase.value === 'true'); 
		<% break;
		case RANK_ORDER: %>
			writeIsPosSelector(initIsPositive, 'orders', 'does not order');
		<% break;
		case RANK_POSITION: %>
			writeIsPosSelector(initIsPositive, 'numbers', 'does not number');
		<% break; } // switch evalConstant %>
	} // initSelectors()

	function submitIt(addNew) {
		var form = document.evaluatorForm;
		if (!gradeOK(form)) {
			alert('Enter a grade between 0 and 1.');
			return;
		}
		<% switch (evalConstant) { 
		case NUM_SIGFIG: 
		case STMTS_CT: 
		case CLICK_CT: 
		case TEXT_WORDS: %> 
			if (!isNonnegativeInteger(form.number.value)) {
				alert('Please enter a valid nonnegative integer.');
				return;
			}
			<% break; 
		case TEXT_CONT: %> 
			form.molname.value = form.molstruct.value;
			<% break; 
		case TEXT_SEMANTICS: %> 
			var respXML = getStmtsXML();
			if (!isEmpty(respXML)) {
				form.molstruct.value = respXML;
				form.molname.value = getParagraph();
			} else return;
			<% break; 
		case CLICK_LABELS_COMP: %> 
			if (!isPositiveInteger(form.startChar.value)) {
				alert('Please enter a valid positive integer.');
				return;
			}
			<% // no break; 
		case CLICK_HERE: 
		case CLICK_NUM: 
		case CLICK_TEXT: %> 
			var shapeTags = [];
			<% for (final String shapeTag : ClickHere.SHAPE_TAGS) { %>
				shapeTags.push('<%= shapeTag %>');
			<% } // for each shape's XML tag %>
			var haveText = form.authText;
			var textValue = (haveText ? form.authText.value : '');
			form.molstruct.value = getShapesXML(shapeTags, textValue);
			form.molname.value = (haveText ? textValue : form.molstruct.value);
			// alert(form.molstruct.value);
			<% break; 
		case RANK_POSITION: %> 
			if (form.option.value === '0') {
				alert('Please choose an option.');
				return;
			}
			<% break; 
		case CHOICE_WHICH_CHECKED:
		case CHOICE_NUM_CHECKED: %> 
			var selBld = new String.builder();
			for (var selIndex = 0; selIndex < <%= qData.length %>; selIndex++) { // <!-- >
				if (getChecked('select' + selIndex)) {
					selBld.append(selIndex + 1).append('<%= Choice.SEPARATOR %>');
				}
			} // each selIndex
			var selection = selBld.toString();
			<% if (evalConstant == CHOICE_WHICH_CHECKED) { %>
				if (selection === '') selection = '<%= MultipleCheck.NO_SELECTION %>';
			<% } // if CHOICE_WHICH_CHECKED %>
			form.selection.value = selection;
			<% break; 
		case RANK_ORDER: %> 
			var MAJOR_SEP = '<%= RankOrder.MAJOR_SEP %>';
			var MINOR_SEP = '<%= RankOrder.MINOR_SEP %>';
			var selStrBld = new String.builder();
			var notSelStrBld = new String.builder();
			<% for (int qdNum = 1; qdNum <= qData.length; qdNum++) { %>
				if (form.option<%= qdNum %>.value === '-1') {
					notSelStrBld.append('<%= RankOrder.UNRANKED_STR %>').
							append(<%= qdNum %>).
							append('<%= RankOrder.SEPARATOR %>');
				} else if (form.option<%= qdNum %>.value !== '0') {
					selStrBld.append(MAJOR_SEP).
							append(form.option<%= qdNum %>.value).
							append(MINOR_SEP).append(<%= qdNum %>);
				}
			<% } // for each qDatum %>
			var selStr = selStrBld.toString();
			var notSelStr = notSelStrBld.toString();
			var shortestLen = (MAJOR_SEP.length * 2 + '1'.length * 2) * 2;
			if (selStr.length < shortestLen) { // <!-- >
				alert('Please choose two or more options that should be'
						+ ' in a particular order.');
				return;
			} else selStr += MAJOR_SEP;
			var selectedOptsOrdered = new String.builder();
			for (var rank = 1; rank <= <%= qData.length %>; rank++) {
				var find = MAJOR_SEP + rank + MINOR_SEP;
				var where = selStr.indexOf(find);
				if (where >= 0) {
					var startAt = selStr.substring(where + 1);
					if (startAt.indexOf(find) >= 0) {
						alert('Each option must have a different rank.');
						return;
					}
					var optLoc = startAt.indexOf(MINOR_SEP) + 1;
					var endOptLoc = startAt.indexOf(MAJOR_SEP);
					selectedOptsOrdered.append(startAt.substring(optLoc, endOptLoc)).
							append('<%= RankOrder.SEPARATOR %>');
				} // if the option was ranked
			} // for each rank
			selectedOptsOrdered.append(notSelStr);
			form.selection.value = selectedOptsOrdered.toString();
			<% break; 
		case NUM_IS: %> 
			<% if (!Question.usesSubstns(qFlags)) { %> 
				if (!validNum(form.authNum)) return;
			<% } // if doesn't use substitutions %> 
			if (!validTolerance(form.tolerance)) return;
			if (isWhiteSpace(form.authExponent.value)) {
				form.authExponent.value = ' ';
			} else if (!canParseToInt(form.authExponent.value)) {
				alert('Enter an integral exponent, or leave it blank.');
				return;
			}
			<% break; 
		case NUM_UNIT: %> 
			var unitNumsBld = new String.builder();
			for (var selIndex = 0; selIndex <= <%= qData.length %>; selIndex++) { // <!-- >
				if (getChecked('select' + selIndex)) {
					unitNumsBld.append(selIndex).
							append('<%= NumberUnit.SEPARATOR %>');
				}
			} // each selIndex
			var unitNums = unitNumsBld.toString();
			if (unitNums === '') unitNums = '0<%= NumberUnit.SEPARATOR %>';
			form.unitNums.value = unitNums;
			<% break; 
		case TBL_NUM_NUM: %>
			if (!validNumAndTolerance(form.authNumRef, form.toleranceRef)) return;
			<% // no break; 
		case TBL_NUM_TXT: 
		case TBL_TXT_NUM: %>
			if (!validNumAndTolerance(form.authNum, form.tolerance)) return;
			<% // no break; 
		case TBL_TXT_TXT: %>
			if (form.columnRef.value === form.columnTest.value) {
				alert('Please choose different values '
					+ 'for the reference and test columns.');
				return;
			}
			if (form.molstruct) form.molname.value = form.molstruct.value;
			<% break; 
		case TABLE_CT_TXT:
		case TABLE_CT_NUM: %>
			if (!isNonnegativeInteger(form.numCells.value)) {
				alert('Please enter a valid nonnegative integer.');
				return;
			}
			<% // no break; 
		case TABLE_NUM: 
		case TABLE_TEXT: %>
			var rowSelected = form.rowSelector.value;
			form.row.value = (rowSelected !== '0' 
					? rowSelected : form.rowNum.value);
			var columnSelected = form.columnSelector.value;
			form.column.value = (columnSelected !== '0' 
					? columnSelected : form.columnNum.value);
			if (form.molstruct) form.molname.value = form.molstruct.value;
			<% if (evalConstant == TABLE_NUM) { %>
				if (!validNumAndTolerance(form.authNum, form.tolerance)) return;
			<% } // TABLE_NUM only %>
		<% break; } // switch 
		// NOTE: we pass the input elements of a TABLE_DIFF table directly to
		// saveEvaluator.jsp for conversion to XML
		%>
		if (form.feedback) {
			form.feedback.value = trimWhiteSpaces(form.feedback.value);
		}
		form.afterSave.value = addNew;
		form.submit();
	} // submitIt()

	// -->
</script>
</head>
<body class="light" style="margin:0px; margin-top:5px; 
		background-color:<%= bgColor %>; text-align:center; overflow:auto;" 
		onload="initGrade(); initSelectors(); <%= 
				Utils.among(evalConstant, TABLE_NUM, TABLE_TEXT, 
					TABLE_CT_NUM, TABLE_CT_TXT) ? " rowChange();" 
				: Utils.among(evalConstant, CLICK_HERE, CLICK_NUM, 
					CLICK_TEXT, CLICK_LABELS_COMP) ? "initDrawOnFigure();" 
				: evalConstant == TEXT_SEMANTICS ? "setUpStmts();" 
				: evalConstant == TABLE_DIFF ? "changeCalcGrade();" 
				: "" %>">
<form name="evaluatorForm" action="saveEvaluator.jsp" method="post"
		accept-charset="UTF-8">
	<input type="hidden" name="evalNum" value="<%= cloneEdit ? 0 : evalNum %>" />
	<input type="hidden" name="subevalNum" value="<%= cloneEdit ? 0 : subevalNum %>" />
	<input type="hidden" name="matchcode" value="<%= inputSubeval.matchCode %>" />
	<input type="hidden" name="afterSave" value="<%= RETURN %>" />
	<input type="hidden" name="qType" value="<%= qType %>" />

<%@ include file="selectorFooter.jsp.h" %>

<div id="evalContents">
<table style="width:100%;" summary="">
	<tr><td class="regtext" style="text-align:center;">

		<% if (evalConstant == CHOICE_WHICH_CHECKED) { 
			boolean isPositive = !Utils.isEmpty(responseIndex);
			int oper = MultipleCheck.EXACTLY;
			String selection = "";
			MultipleCheck impl = new MultipleCheck();
			if (useInput) { // existing evaluator
				impl = new MultipleCheck(inputSubeval.codedData);
				oper = impl.getOper();
				isPositive = impl.getIsPositive();
				selection = impl.getSelection();
			} else if (haveResp && !Utils.isEmpty(inputSubeval.molStruct)) {
				// have response from View Responses
				final Choice choiceResp = new Choice(inputSubeval.molStruct);
				selection = choiceResp.getStringChosenOptions(Choice.SORT);
				impl.setSelection(selection);
			} // if useInput
			final long[] binaryArray = impl.getBinaryArray(qData.length);
				// each bit = a qDatum; 0 or 1
			final int size = 64; // bits per long
			final String[] OPER_ENGLISH = MultipleCheck.OPER_ENGLISH;
		%>
			<input type="hidden" name="selection" value="" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
				If the student <span id="isPosSelector"></span> chosen
				<select name="oper" >
					<% for (int o = 1; o <= OPER_ENGLISH.length; o++) { %>
						<option value="<%= o %>" <%= o == oper ? SELECTED : "" %> >
						<%= OPER_ENGLISH[o - 1] %></option>
					<% } // for each operator o %>
				</select>
			</td></tr>
			<tr><td class="regtext">
				the checked options in this list:
			</td></tr>
			<tr><td class="regtext">
				<% for (int qdNum = 0; qdNum < qData.length; qdNum++) { %>
					<input type="checkbox" name="select<%= qdNum %>"
							id="select<%= qdNum %>" <%= 
							(binaryArray[qdNum / size] &
								(1L << (qdNum % size))) != 0 ? CHECKED : "" %> />
					<%= qData[qdNum].toShortDisplay(chemFormatting) %>
					<br/>
				<% } // each question datum %>
			</td></tr>
			</table>

		<% } else if (evalConstant == CHOICE_NUM_CHECKED) { 
			boolean isPositive = false;
			int oper = MultipleNumChosen.EQUALS;
			int numChosen = 1;
			String[] amongOptions = new String[0];
			if (useInput) { // existing evaluator
				final MultipleNumChosen impl = new MultipleNumChosen(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
				oper = impl.getOper();
				numChosen = impl.getNumChosen();
				amongOptions = impl.getAmongOptions().split(Choice.SEPARATOR);
			} // useInput 
			final String[] OPER_ENGLISH = MultipleNumChosen.OPER_ENGLISH[FEWER];
		%>
			<input type="hidden" name="selection" value="" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
				If the student  <span id="isPosSelector"></span> chosen
			</td></tr>
			<tr><td class="regtext">
				<select name="oper" >
				<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
					<option value="<%= o %>" <%= o == oper ? SELECTED : "" %> >
					<%= OPER_ENGLISH[o] %></option>
				<% } %>
				</select>
				<select name="numChosen">
					<% for (int qdNum = 0; qdNum <= qData.length; qdNum++) { %>
						<option value="<%= qdNum %>" <%= qdNum == numChosen 
								? SELECTED : "" %> > <%= qdNum %> </option>
					<% } %>
				</select>
				of the following options 
				<br/><i>(select those that should "count"; 
				select none if all should count):</i> 
			</td></tr>
			<tr><td> &nbsp;</td></tr>
			<tr><td>
				<table summary="">
				<% for (int qdNum = 0; qdNum < qData.length; qdNum++) { %>
					<tr><td class="regtext" style="vertical-align:middle;">
						<input type="checkbox" name="select<%= qdNum %>"
								id="select<%= qdNum %>" <%= 
								Utils.contains(amongOptions, String.valueOf(qdNum + 1)) 
									? CHECKED : "" %> />
						<%= qData[qdNum].toShortDisplay(chemFormatting) %>
					</td></tr>
				<% } // each question datum %>
				</table>
			</td></tr>
			</table>

		<% } else if (evalConstant == CLICK_HERE) { 
			int howMany = ClickHere.NONE;
			if (useInput) {
				final ClickHere impl = new ClickHere(inputSubeval.codedData);
				howMany = impl.getHowMany();
			} // useInput 
			%>
			<input type="hidden" name="molname" value="" />
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" id="scrollableDiv" value="evalContents" />
			<table style="margin-left:0; margin-right:auto; width:445px;
					text-align:left;" summary="">
				<% if (!Utils.isEmpty(figures)) { %>
					<tr><td class="regtext">If 
						<select name="howMany" >
							<% for (int o = 1; o <= ClickHere.HOWMANY_ENGL.length; o++) { %>
								<option value="<%= o %>"
									<%= o == howMany ? SELECTED : "" %> >
								<%= ClickHere.HOWMANY_ENGL[o - 1] %></option>
							<% } %>
						</select>
						student's mark is in the following 
						<a href="javascript:shapeAdvice();">regions</a>:
					</td></tr>
					<tr><td class="regtext">
						<table><tr><td>
						Draw or clear 
						<select id="shapeChooser">
							<option value="<%= ClickHere.RECT %>">a rectangle</option>
							<option value="<%= ClickHere.CIRC %>">a circle</option>
							<option value="<%= ClickHere.ELLIP %>">an ellipse</option>
						</select>
						</td><td>
						<%= makeButton("Clear last", "clearLast();") %>
						</td><td>
						<%= makeButton("Clear all of this shape", "clearAllOfOne();") %>
						</td><td>
						<%= makeButton("Clear all shapes", "clearAll();") %>
						</td></tr></table>
					</td></tr>
					<tr><td class="regtext">
						<div id="canvas" style="position:relative; left:0px; top:0px;">
						<img src="<%= pathToRoot + figures[0].bufferedImage %>" 
								id="clickableImage" alt="picture" class="unselectable"
								onselect="return false;" ondragstart="return false;"/>
						</div>
					</td></tr>
				<% } else { %>
					<tr><td class="boldtext" style="color:red;">
						Please upload an image before writing an evaluator.
					</td></tr>
				<% } // if there is a figure %>
			</table>

		<% } else if (evalConstant == CLICK_TEXT) { 
			int howMany = ClickText.NONE;
			boolean isPositive = true;
			int where = TextContains.IS;
			boolean ignoreCase = true;
			if (useInput) {
				final ClickText impl = new ClickText(inputSubeval.codedData);
				howMany = impl.getHowMany();
				isPositive = impl.getIsPositive();
				where = impl.getWhere();
				ignoreCase = impl.getIgnoreCase();
			} // useInput 
			%>
			<input type="hidden" name="molname" value="" />
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<input type="hidden" name="initIgnoreCase" value="<%= ignoreCase %>" />
			<input type="hidden" id="scrollableDiv" value="evalContents" />
			<table style="margin-left:0; margin-right:auto; width:445px;
					text-align:left;" summary="">
				<% if (!Utils.isEmpty(figures)) { %>
					<tr><td class="regtext">If 
						<select name="howMany" >
							<% for (int o = 1; 
									o <= ClickText.HOWMANY_ENGL.length; o++) { %>
								<option value="<%= o %>"
									<%= o == howMany ? SELECTED : "" %> >
								<%= ClickText.HOWMANY_ENGL[o - 1] %></option>
							<% } %>
						</select>
						student's mark in the following 
						<a href="javascript:shapeAdvice();">regions</a>
					</td></tr>
					<tr><td class="regtext"> 
						<span id="isPosSelector"></span>
						<select name="where" onchange="setIgnoreCase()">
						<% for (int o = 0; 
								o < ClickText.WHERE_ENGLISH.length; o++) { %>
							<option value="<%= o %>" 
									<%= where == o ? SELECTED : "" %>>
								<%= ClickText.WHERE_ENGLISH[o] %></option>
						<% } // for each option %>
						</select>
						<input name="authText" type="text" size="50"
								value="<%= authText %>" />
					</td></tr>
					<tr><td id="ignoreCaseSpan" class="regtext"
							style="visibility:<%= Utils.among(where, 
									ClickText.MATCHES_REGEX, 
									ClickText.CONT_REGEX) ? HIDDEN : VISIBLE %>;">
						<span id="caseSelector"></span>
						case
					</td></tr>
					<tr><td class="regtext">
						<table><tr><td>
						Draw or clear 
						<select id="shapeChooser">
							<option value="<%= ClickText.RECT %>">a rectangle</option>
							<option value="<%= ClickText.CIRC %>">a circle</option>
							<option value="<%= ClickText.ELLIP %>">an ellipse</option>
						</select>
						</td><td>
						<%= makeButton("Clear last", "clearLast();") %>
						</td><td>
						<%= makeButton("Clear all of this shape", "clearAllOfOne();") %>
						</td><td>
						<%= makeButton("Clear all shapes", "clearAll();") %>
						</td></tr></table>
					</td></tr>
					<tr><td class="regtext">
						<div id="canvas" style="position:relative; left:0px; top:0px;">
						<img src="<%= pathToRoot + figures[0].bufferedImage %>" 
								id="clickableImage" alt="picture" 
								onselect="return false;" ondragstart="return false;"/>
						</div>
					</td></tr>
				<% } else { %>
					<tr><td class="boldtext" style="color:red;">
						Please upload an image before writing an evaluator.
					</td></tr>
				<% } // if there is a figure %>
			</table>

		<% } else if (evalConstant == CLICK_LABELS_COMP) { 
			int howMany = ClickLabelsCompare.NONE;
			boolean isPositive = true;
			int startChar = 1;
			boolean ignoreCase = true;
			if (useInput) {
				final ClickLabelsCompare impl = 
						new ClickLabelsCompare(inputSubeval.codedData);
				howMany = impl.getHowMany();
				isPositive = impl.getIsPositive();
				startChar = impl.getStartChar();
				ignoreCase = impl.getIgnoreCase();
			} // useInput 
			%>
			<input type="hidden" name="molname" value="" />
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<input type="hidden" name="initIgnoreCase" value="<%= ignoreCase %>" />
			<input type="hidden" id="scrollableDiv" value="evalContents" />
			<table style="margin-left:auto; margin-right:auto; width:95%;
					text-align:left;" summary="">
				<% if (!Utils.isEmpty(figures)) { %>
					<tr><td class="regtext">If 
						<select name="howMany" >
							<option value="<%= ClickLabelsCompare.NONE %>"
								<%= howMany == ClickLabelsCompare.NONE ? SELECTED : "" %> >
							<%= ClickLabelsCompare.HOWMANY_ENGL[ClickLabelsCompare.NONE - 1] %></option>
							<option value="<%= ClickLabelsCompare.ANY %>"
								<%= howMany == ClickLabelsCompare.ANY ? SELECTED : "" %> >
							<%= ClickLabelsCompare.HOWMANY_ENGL[ClickLabelsCompare.ANY - 1] %></option>
						</select>
						mark's text in the following region(s) is
						<span id="isPosSelector"></span>
						another mark's text in the same region(s), making the comparison
						starting at character
						<input type="text" name="startChar" id="startChar" 
								size="3" value="<%= startChar %>" />
						and
						<span id="caseSelector"></span>
						case
					</td></tr>
					<tr><td class="regtext">
						<table><tr><td>
						Draw or clear 
						<select id="shapeChooser">
							<option value="<%= ClickText.RECT %>">a rectangle</option>
							<option value="<%= ClickText.CIRC %>">a circle</option>
							<option value="<%= ClickText.ELLIP %>">an ellipse</option>
						</select>
						</td><td>
						<%= makeButton("Clear last", "clearLast();") %>
						</td><td>
						<%= makeButton("Clear all of this shape", "clearAllOfOne();") %>
						</td><td>
						<%= makeButton("Clear all shapes", "clearAll();") %>
						</td></tr></table>
					</td></tr>
					<tr><td class="regtext">
						<div id="canvas" style="position:relative; left:0px; top:0px;">
						<img src="<%= pathToRoot + figures[0].bufferedImage %>" 
								id="clickableImage" alt="picture" 
								onselect="return false;" ondragstart="return false;"/>
						</div>
					</td></tr>
				<% } else { %>
					<tr><td class="boldtext" style="color:red;">
						Please upload an image before writing an evaluator.
					</td></tr>
				<% } // if there is a figure %>
			</table>

		<% } else if (evalConstant == CLICK_NUM) { 
			int howMany = ClickNumber.NONE;
			int oper = NumberIs.EQUALS;
			String authNum = "";
			String tolerance = "";
			if (useInput) {
				final ClickNumber impl = new ClickNumber(inputSubeval.codedData);
				howMany = impl.getHowMany();
				oper = impl.getOper();
				authNum = impl.getAuthNum();
				tolerance = impl.getTolerance();
			} // useInput 
			final String[] OPER_ENGLISH = ClickNumber.OPER_ENGLISH[LESSER];
			%>
			<input type="hidden" name="molname" value="" />
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" id="scrollableDiv" value="evalContents" />
			<table style="margin-left:0; margin-right:auto; width:445px;
					text-align:left;" summary="">
				<% if (!Utils.isEmpty(figures)) { %>
					<tr><td class="regtext">If 
						<select name="howMany" >
							<% for (int o = 1; 
									o <= ClickNumber.HOWMANY_ENGL.length; o++) { %>
								<option value="<%= o %>"
									<%= o == howMany ? SELECTED : "" %> >
								<%= ClickNumber.HOWMANY_ENGL[o - 1] %></option>
							<% } %>
						</select>
						student's mark in the following 
						<a href="javascript:shapeAdvice();">regions</a>
					</td></tr>
					<tr><td class="regtext">
						is
						<select name="oper" >
							<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
								<option value="<%= o %>" <%= 
										o == oper ? SELECTED : "" %> >
								<%= OPER_ENGLISH[o] %></option>
							<% } // for each option %>
						</select>
					</td></tr>
					<tr><td class="regtext">
						<input type="text" name="authNum" size="20"
								value="<%= authNum %>" /> &#177;
						<input type="text" name="tolerance" size="10"
								value="<%= tolerance %>" />
					</td></tr>
					<tr><td class="regtext">
						<table><tr><td>
						Draw or clear 
						<select id="shapeChooser">
							<option value="<%= ClickNumber.RECT %>">a rectangle</option>
							<option value="<%= ClickNumber.CIRC %>">a circle</option>
							<option value="<%= ClickNumber.ELLIP %>">an ellipse</option>
						</select>
						</td><td>
						<%= makeButton("Clear last", "clearLast();") %>
						</td><td>
						<%= makeButton("Clear all of this shape", "clearAllOfOne();") %>
						</td><td>
						<%= makeButton("Clear all shapes", "clearAll();") %>
						</td></tr></table>
					</td></tr>
					<tr><td class="regtext">
						<div id="canvas" style="position:relative; left:0px; top:0px;">
						<img src="<%= pathToRoot + figures[0].bufferedImage %>" 
								id="clickableImage" alt="picture" 
								onselect="return false;" ondragstart="return false;"/>
						</div>
					</td></tr>
				<% } else { %>
					<tr><td class="boldtext" style="color:red;">
						Please upload an image before writing an evaluator.
					</td></tr>
				<% } // if there is a figure %>
			</table>

		<% } else if (evalConstant == CLICK_CT) { 
			int oper = ClickCount.EQUALS;
			int number = 0;
			if (useInput) {
				final ClickCount impl = new ClickCount(inputSubeval.codedData);
				oper = impl.getOper();
				number = impl.getAuthNum();
			} // useInput 
			final String[] OPER_ENGLISH = ClickCount.OPER_ENGLISH[FEWER];
			%>
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
				If the number of marks in the response is
				<select name="oper">
					<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
						<option value="<%= o %>" <%= o == oper ? SELECTED : "" %> >
						<%= OPER_ENGLISH[o] %></option>
					<% } %>
				</select>
				<input name="number" type="text" size="2" value="<%= number %>" />
			</td></tr>
			</table>

		<% } else if (evalConstant == NUM_IS) { 
			int oper = NumberIs.EQUALS;
			String authNum = "";
			String tolerance = "";
			String authExponent = "";
			String authUnit = "";
			if (useInput) { // existing evaluator
				final NumberIs impl = new NumberIs(inputSubeval.codedData);
				oper = impl.getOper();
				authNum = impl.getAuthNum();
				tolerance = impl.getTolerance();
				authExponent = impl.getAuthExponent();
				authUnit = impl.getAuthUnit();
			} // useInput 
			final String[] OPER_ENGLISH = NumberIs.OPER_ENGLISH[LESSER];
			%>
			<table style="margin-left:auto; margin-right:auto; 
					width:90%; text-align:left;" summary="">
			<tr><td class="regtext">
			 	If the numerical response is
				<select name="oper" >
					<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
						<option value="<%= o %>" <%= o == oper ? SELECTED : "" %> >
						<%= OPER_ENGLISH[o] %></option>
					<% } %>
				</select>
			</td></tr>
			<tr><td class="regtext">
				(<input type="text" name="authNum" size="20"
						value="<%= authNum %>" /> &#177;
				<input type="text" name="tolerance" size="10"
						value="<%= tolerance %>" />) &times; 
				10<sup><input type="text" name="authExponent" size="10"
						value="<%= authExponent %>" /></sup>
				<% if (!Utils.isEmpty(qData)) { 
					if (qData.length == 1) { %>
						<%= Utils.toDisplay(qData[0].data) %>
						<input type="hidden" name="authUnit" 
								value="<%= Utils.toValidHTMLAttributeValue(
										qData[0].data) %>" />
				<% 	} else { %>
						<select name="authUnit">
							<option value="" <%=
									Utils.isEmpty(authUnit) ? SELECTED : "" %> />
							</option>
							<% for (final QDatum qDatum : qData) { %>
								<option value="<%= 
										Utils.toValidHTMLAttributeValue(
											qDatum.data) 
										%>" <%= authUnit.equals(qDatum.data) 
											? SELECTED : "" %> />
								<%= Utils.toDisplay(qDatum.data) %>
								</option>
							<% } // each question datum %>
						</select>
				<% 	} // if there is only one unit
				} // if there are units %>
			</td></tr>
			<tr><td class="regtext" style="padding-top:10px; color:green;">
				<p>You may leave the tolerance field blank for a tolerance of 0.
				You may leave the exponent field blank if you will not be 
				using scientific notation.
				<% if (Utils.getLength(qData) > 1) { %>
					You may leave the unit blank if you wish to disregard it.
				<% 	} // if there is more than one unit 
				if (question.usesSubstns()) { %>
					</p><p>Example of an entry in the number field when using
					variables:
					<blockquote>
					3.14159265 * [[x1]] * [[x2]]
					</blockquote>
					ACE uses <a href="http://maxima.sourceforge.net"
					target=window2>Maxima</a> to evaluate algebraic
					expressions. To see how it works, see 
					<a href="/public/maximaTest.jsp" target="window2">this page</a>.
					Note that Maxima does not understand implicit 
					multiplication; the * sign must always be included. 
				<% } // if the question uses substitutions %>
				</p>
			</td></tr>
			</table>

		<% } else if (evalConstant == NUM_SIGFIG) { 
			int number = 0;
			int oper = NumberSigFigs.EQUALS;
			if (useInput) { // existing evaluator
				final NumberSigFigs impl = 
						new NumberSigFigs(inputSubeval.codedData);
				number = impl.getAuthNum();
				oper = impl.getOper();
			} // useInput 
			final String[] OPER_ENGLISH = NumberSigFigs.OPER_ENGLISH[FEWER];
			%>
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
			 	If the number of significant figures in the response is
			</td></tr>
			<tr><td class="regtext">
				<select name="oper" >
					<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
						<option value="<%= o %>" <%= o == oper ? SELECTED : "" %> >
						<%= OPER_ENGLISH[o] %></option>
					<% } %>
				</select>
				<input type="text" name="number" size="5"
					value="<%= number %>" />
			</td></tr>
			</table>

		<% } else if (evalConstant == NUM_UNIT) { 
			boolean isPositive = false;
			String[] unitNums = new String[0];
			if (useInput) { // existing evaluator
				final NumberUnit impl = new NumberUnit(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
				unitNums = impl.getUnitNums().split(":"); // 1-based
			} // useInput 
		%>
			<input type="hidden" name="unitNums" value="" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
				If the student <span id="isPosSelector"></span> chosen
			</td></tr>
			<tr><td class="regtext">
				one of the checked options in this list:
				<% for (int qdNum = 0; qdNum <= qData.length; qdNum++) { %>
					<br/>
					<input type="checkbox" name="select<%= qdNum %>" 
							id="select<%= qdNum %>" <%=
							Utils.contains(unitNums, String.valueOf(qdNum)) ? CHECKED : ""
							%> />
					<%= qdNum == 0 ? "no units"
							: Utils.toDisplay(qData[qdNum - 1].data) %>
				<% } // each question datum %>
			</td></tr>
			</table>

		<% } else if (evalConstant == RANK_ORDER) { 
			boolean isPositive = false;
			boolean contiguous = true;
			// final boolean increasing = true;
			// may have input string from View Responses
			String selection = (inputSubeval.molStruct != null 
					? inputSubeval.molStruct : "");
			RankOrder impl = new RankOrder();
			if (useInput) { // existing evaluator
				impl = new RankOrder(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
				contiguous = impl.getContiguous();
				selection = impl.getSelection();
			} else {
				impl.setSelection(selection);
			}
			final String[] rankedUnranked = impl.getRankedUnranked();
			final String[] ranked = ("".equals(rankedUnranked[RankOrder.RANKED]) 
					? new String[0]
					: rankedUnranked[RankOrder.RANKED].split(RankOrder.SEPARATOR)); 
			final String[] unranked = ("".equals(rankedUnranked[RankOrder.UNRANKED]) 
					? new String[0]
					: rankedUnranked[RankOrder.UNRANKED].split(RankOrder.SEPARATOR)); 
		%>
			<input type="hidden" name="increasing" value="true" />
			<input type="hidden" name="selection" value="" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
				If the student's response <span id="isPosSelector"></span>
				the selected options
			</td></tr>
			<tr><td class="regtext">
				<select name="contiguous">
					<option value="true" <%= contiguous ? SELECTED : "" %> > 
						contiguously </option>
					<option value="false" <%= !contiguous ? SELECTED : "" %> > 
						contiguously or noncontiguously</option>
				</select>	
				in the <i>relative</i> order:
				<br/>
			</td></tr>
			<tr><td class="regtext">
				&nbsp;
			</td></tr>
			<tr><td class="regtext">
				<table summary="">
					<% for (int qdNum = 0; qdNum < qData.length; qdNum++) { 
						final String base1Str = String.valueOf(qdNum + 1);
						int rank = Utils.indexOf(ranked, base1Str);
						if (rank >= 0) rank++;
						else if (!Utils.contains(unranked, base1Str)) rank = 0;
					%>
						<tr><td>
							<select name="option<%= qdNum + 1 %>">
								<option value="0"> &nbsp; </option>
								<option value="-1" <%= rank == -1 
										? SELECTED : ""  %>> unnumbered </option>
								<%	for (int optNum = 1; optNum <= qData.length; 
											optNum++) { %>
									<option value="<%= optNum %>" <%= rank == optNum 
											? SELECTED : ""%>> <%= optNum %> </option>
								<% } // each option value optNum %>
							</select>
						</td>
						<td class="regtext" style="vertical-align:middle;">
							<b>Option <%= qdNum + 1 %>.</b>	
							<%= qData[qdNum].toShortDisplay(chemFormatting) %>
						</td></tr>
					<% } // each question datum %>
				</table>
			</td></tr>
			<tr><td class="regtext">
				&nbsp;
			</td></tr>
			<tr><td class="regtext" style="color:green;">
				Leave blank the numbers of those items that you do not wish 
				to consider in this evaluator.  
				Choose "unnumbered" for items that should remain unnumbered.
			</td></tr>
			</table>

		<% } else if (evalConstant == RANK_POSITION) { 
			boolean isPositive = false;
			String selection = "0:0";
			if (useInput) { // existing evaluator
				final RankPosition impl = new RankPosition(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
				selection = impl.getSelection();
			} // useInput 
			final String[] itemRank = selection.split(RankPosition.SEPARATOR); %>
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
				If the student <span id="isPosSelector"></span> option 
				<select name="option">
					<% for (int optNum = 1; optNum <= qData.length; optNum++) { %>
						<option value="<%= optNum %>" 
							<%= itemRank[RankPosition.ITEM]
									.equals(String.valueOf(optNum)) 
								? SELECTED : "" %> > <%= optNum %> </option>
					<% } %>
				</select>
				as
				<select name="rank">
					<% for (int optNum = 0; optNum <= qData.length; optNum++) { %>
						<option value="<%= optNum %>" 
							<%= itemRank[RankPosition.RANK]
									.equals(String.valueOf(optNum)) 
								? SELECTED : "" %> > <%= optNum > 0 
										? optNum : "unnumbered" %>
						</option>
					<% } %>
				</select>
				<br/>
			</td></tr>
			<tr><td> &nbsp;</td></tr>
			<tr><td>
				<table summary="">
				<% for (int qdNum = 0; qdNum < qData.length; qdNum++) { %>
					<tr><td class="regtext" style="vertical-align:middle;">
						<b>Option <%= qdNum + 1 %>.</b>	
						<%= qData[qdNum].toShortDisplay(chemFormatting) %>
					</td></tr>
				<% } // each question datum %>
				</table>
			</td></tr>
			</table>

		<% } else if (evalConstant == TBL_NUM_NUM) { 
			int rowOper = TableNumNum.ANY_ROW;
			int columnRef = 0; 
			int columnTest = 0; 
			int emptyCell = TableNumNum.IGNORE;
			int nonnumeric = TableNumNum.IGNORE;
			// int operRef = TableNumNum.EQUALS;
			String authNumRef = "";
			String toleranceRef = "";
			int oper = TableNumNum.NOT_EQUALS;
			String authNum = "";
			String tolerance = "";
			if (useInput) { 
				final TableNumNum impl = new TableNumNum(inputSubeval.codedData);
				rowOper = impl.getRowOper();
				columnRef = impl.getColumnRef();
				columnTest = impl.getColumnTest();
				emptyCell = impl.getEmptyCell();
				nonnumeric = impl.getNonnumeric();
				// operRef = impl.getOperRef();
				authNumRef = impl.getAuthNumRef();
				toleranceRef = impl.getToleranceRef();
				oper = impl.getOper();
				authNum = impl.getAuthNum();
				tolerance = impl.getTolerance();
			} // useInput 
			final boolean haveNumCols = qData.length > TableQ.COL_DATA;
			final int numCols = (!haveNumCols ? 0
					: ((CaptionsQDatum) qData[TableQ.COL_DATA]).getNumRowsOrCols());
			final String[] OPER_ENGLISH = TableNumNum.OPER_ENGLISH[LESSER];
			%>
			<input type="hidden" name="molname" value="" />
			<table style="margin-left:auto; margin-right:auto; width:445px;
					text-align:left;" summary="">
				<tr><td class="regtext">If, for 
				<select name="rowOper">
					<option value="<%= TableNumNum.ANY_ROW %>" 
						<%= rowOper == TableNumNum.ANY_ROW ? SELECTED : "" %>>any</option>
					<option value="<%= TableNumNum.EVERY_ROW %>" 
						<%= rowOper == TableNumNum.EVERY_ROW ? SELECTED : "" %>>every</option>
				</select>
				row in which the numerical value of the cell in column
				<% if (numCols > 0) { %>
				<select name="columnRef" id="columnRef">
					<% for (int o = 0; o < numCols; o++) { %>
						<option value="<%= o + 1 %>" <%= columnRef == o + 1 
								? SELECTED : "" %> ><%= o + 1 %></option>
					<% } // for each selection %>
				</select>
				<% } %>
				<br />is
				<select name="operRef">
					<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
						<option value="<%= o %>" <%= o == oper ? SELECTED : "" %> >
						<%= OPER_ENGLISH[o] %></option>
					<% } %>
				</select>
				</td></tr>
				<tr><td class="regtext">
				<input type="text" name="authNumRef" size="20"
						value="<%= authNumRef %>" /> &#177;
				<input type="text" name="toleranceRef" size="10"
						value="<%= toleranceRef %>" />, 
				</td></tr>
				<tr><td class="regtext">
				the numerical value in column
				<% if (numCols > 0) { %>
				<select name="columnTest" id="columnTest">
					<% for (int o = 0; o < numCols; o++) { %>
						<option value="<%= o + 1 %>" <%= columnTest == o + 1 
								? SELECTED : "" %> ><%= o + 1 %></option>
					<% } // for each selection %>
				</select>
				<% } %>
				<br />is
				<select name="oper" >
					<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
						<option value="<%= o %>" <%= o == oper ? SELECTED : "" %> >
						<%= OPER_ENGLISH[o] %></option>
					<% } %>
				</select>
				</td></tr>
				<tr><td class="regtext">
					<input type="text" name="authNum" size="20"
							value="<%= authNum %>" /> &#177;
					<input type="text" name="tolerance" size="10"
							value="<%= tolerance %>" />
				</td></tr>
				<tr><td class="regtext">
					<% for (int o = 0; o < TableNumNum.EMPTYCELL_JSP.length; o++) { %>
						<br /><input name="emptyCell" type="radio" value="<%= o %>" 
								<%= o == emptyCell ? CHECKED : "" %> />
						<%= TableNumNum.EMPTYCELL_JSP[o].replaceAll("\"\"", "0") %>
					<% } %>
				</td></tr>
				<tr><td class="regtext">
					<% for (int o = 0; o < TableNumNum.NONNUMERIC_JSP.length; o++) { %>
						<br /><input name="nonnumeric" type="radio" value="<%= o %>" 
								<%= o == nonnumeric ? CHECKED : "" %> />
						<%= TableNumNum.NONNUMERIC_JSP[o] %>
					<% } %>
				</td></tr>
			</table>

		<% } else if (evalConstant == TBL_NUM_TXT) { 
			int rowOper = TableNumText.ANY_ROW;
			int columnRef = 0; 
			int columnTest = 0; 
			int emptyCell = TableNumText.IGNORE;
			int nonnumeric = TableNumText.IGNORE;
			int oper = TableNumText.EQUALS;
			String authNum = "";
			String tolerance = "";
			int where = TableNumText.IS;
			boolean isPositive = false;
			boolean ignoreCase = true;
			String testString = "";
			if (useInput) { 
				final TableNumText impl = new TableNumText(inputSubeval.codedData);
				rowOper = impl.getRowOper();
				columnRef = impl.getColumnRef();
				columnTest = impl.getColumnTest();
				emptyCell = impl.getEmptyCell();
				nonnumeric = impl.getNonnumeric();
				oper = impl.getOper();
				authNum = impl.getAuthNum();
				tolerance = impl.getTolerance();
				isPositive = impl.getIsPositive();
				where = impl.getWhere();
				ignoreCase = impl.getIgnoreCase();
				testString = inputSubeval.molStruct;
			} // useInput 
			final boolean haveNumCols = qData.length > TableQ.COL_DATA;
			final int numCols = (!haveNumCols ? 0
					: ((CaptionsQDatum) qData[TableQ.COL_DATA]).getNumRowsOrCols());
			final String[] OPER_ENGLISH = TableNumText.OPER_ENGLISH[LESSER];
			%>
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<input type="hidden" name="initIgnoreCase" value="<%= ignoreCase %>" />
			<input type="hidden" name="molname" value="" />
			<table style="margin-left:auto; margin-right:auto; width:445px;
					text-align:left;" summary="">
				<tr><td class="regtext">If, for 
				<select name="rowOper">
					<option value="<%= TableNumText.ANY_ROW %>" 
						<%= rowOper == TableNumText.ANY_ROW ? SELECTED : "" %>>any</option>
					<option value="<%= TableNumText.EVERY_ROW %>" 
						<%= rowOper == TableNumText.EVERY_ROW ? SELECTED : "" %>>every</option>
				</select>
				row in which the numerical value of the cell in column
				<% if (numCols > 0) { %>
				<select name="columnRef" id="columnRef">
					<% for (int o = 0; o < numCols; o++) { %>
						<option value="<%= o + 1 %>" <%= columnRef == o + 1 
								? SELECTED : "" %> ><%= o + 1 %></option>
					<% } // for each selection %>
				</select>
				<% } %>
				<br />is
				<select name="oper">
					<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
						<option value="<%= o %>" <%= o == oper ? SELECTED : "" %> >
						<%= OPER_ENGLISH[o] %></option>
					<% } %>
				</select>
				<input type="text" name="authNum" size="20"
						value="<%= authNum %>" /> &#177;
				<input type="text" name="tolerance" size="10"
						value="<%= tolerance %>" />,
				</td></tr>
				<tr><td class="regtext">
				the text in the cell in column
				<% if (numCols > 0) { %>
				<select name="columnTest" id="columnTest">
					<% for (int o = 0; o < numCols; o++) { %>
						<option value="<%= o + 1 %>" <%= columnTest == o + 1 
								? SELECTED : "" %> ><%= o + 1 %></option>
					<% } // for each selection %>
				</select>
				<% } %>
				<span id="isPosSelector"></span>
				<select name="where" onchange="setIgnoreCase()">
					<% for (int o = 0; o < TableNumText.WHERE_ENGLISH.length; o++) { %>
						<option value="<%= o %>" <%= where == o 
								? SELECTED : "" %>>
							<%= TableNumText.WHERE_ENGLISH[o] %></option>
					<% } %>
				</select>
				<input name="molstruct" type="text" size="50"
						value="<%= Utils.toValidTextbox(testString) %>" />
				<span id="ignoreCaseSpan">
				<span id="caseSelector"></span>
				case
				</span>
				</td></tr>
				<tr><td class="regtext">
					<% for (int o = 0; o < TableNumText.EMPTYCELL_JSP.length; o++) { %>
						<br /><input name="emptyCell" type="radio" value="<%= o %>" 
								<%= o == emptyCell ? CHECKED : "" %> />
						<%= TableNumText.EMPTYCELL_JSP[o].replaceAll("\"\"", "\"\" or 0") %>
					<% } %>
				</td></tr>
				<tr><td class="regtext">
					<% for (int o = 0; o < TableNumText.NONNUMERIC_JSP.length; o++) { %>
						<br /><input name="nonnumeric" type="radio" value="<%= o %>" 
								<%= o == nonnumeric ? CHECKED : "" %> />
						<%= TableNumText.NONNUMERIC_JSP[o] %>
					<% } %>
				</td></tr>
			</table>

		<% } else if (evalConstant == TABLE_NUM) { 
			int row = TableNumVal.ANY;
			int column = TableNumVal.ANY; 
			String authNum = "";
			String tolerance = "";
			int oper = TableNumVal.NOT_EQUALS;
			int emptyCell = TableNumVal.IGNORE;
			int nonnumeric = TableNumVal.IGNORE;
			boolean colorSatisfying = true;
			if (useInput) { 
				final TableNumVal impl = new TableNumVal(inputSubeval.codedData);
				row = impl.getRow();
				column = impl.getColumn();
				authNum = impl.getAuthNum();
				tolerance = impl.getTolerance();
				oper = impl.getOper();
				emptyCell = impl.getEmptyCell();
				nonnumeric = impl.getNonnumeric();
				colorSatisfying = impl.getColorSatisfying();
			} // useInput 
			final boolean haveNumRows = qData.length > TableQ.ROW_DATA;
			final boolean haveNumCols = qData.length > TableQ.COL_DATA;
			final int numRows = (!haveNumRows ? 0
					: ((CaptionsQDatum) qData[TableQ.ROW_DATA]).getNumRowsOrCols());
			final int numCols = (!haveNumCols ? 0
					: ((CaptionsQDatum) qData[TableQ.COL_DATA]).getNumRowsOrCols());
			final String[] OPER_ENGLISH = TableNumVal.OPER_ENGLISH[LESSER];
			%>
			<input type="hidden" name="row" value="<%= row %>" />
			<input type="hidden" name="column" value="<%= column %>" />
			<input type="hidden" name="molname" value="" />
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
				<tr><td class="regtext">If the numerical value of the cell in
				<% final int ROW = 0;
				final int COL = 1;
				for (int grpType = ROW; grpType <= COL; grpType++) { 
					final int grp = (grpType == ROW ? row : column);
					final String grpText = (grpType == ROW ? "row" : "column"); %>
					<select name="<%= grpText %>Selector" 
							onchange="javascript:<%= grpText %>Change()">
						<% for (int o = 0; o < TableNumVal.ROW_COL.length; o++) { %>
							<option value="<%= -(o + 1) %>" <%= grp == -(o + 1) 
									? SELECTED : "" %> >
							<%= TableNumVal.ROW_COL[o] %> <%= grpText %></option>
						<% } // for each selection
						if ((grpType == ROW && haveNumRows) || haveNumCols) { %>
							<option value="0" <%= grp > 0 ? SELECTED : "" %>>
								<%= grpText %> number</option>
						<% } // if have number of rows %>
					</select>
					<span id="<%= grpText %>NumCell" style="visibility:<%= 
							grp > 0 ? VISIBLE : HIDDEN %>;">
					<select name="<%= grpText %>Num"
							id="<%= grpText %>Num">
						<% final int limit = (grpType == ROW ? numRows : numCols);
						for (int o = 0; o < limit; o++) { %>
							<option value="<%= o + 1 %>" <%= grp == o + 1 
									? SELECTED : "" %> ><%= o + 1 %></option>
						<% } // for each selection %>
					</select>
					</span>
					<% if (grpType == ROW) { %>
						<br />and
					<% } %>
				<% } // for row, then column %>
				</td></tr>
				<tr><td class="regtext">
					is
					<select name="oper">
						<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
							<option value="<%= o %>" <%= o == oper ? SELECTED : "" %> >
							<%= OPER_ENGLISH[o] %></option>
						<% } %>
					</select>
					<input type="text" name="authNum" size="15"
							value="<%= authNum %>" /> &#177;
					<input type="text" name="tolerance" size="10"
							value="<%= tolerance %>" />
				</td></tr>
				<tr><td id="tableValAdvice" class="regtext" 
						style="color:green; padding-top:10px; padding-bottom:10px;">
				</td></tr>
				<tr><td class="regtext">
					<% for (int o = 0; 
							o < TableNumVal.EMPTYCELL_JSP.length; o++) { %>
						<br /><input name="emptyCell" type="radio" value="<%= o %>" 
								<%= o == emptyCell ? CHECKED : "" %> />
						<%= TableNumVal.EMPTYCELL_JSP[o].replaceAll("\"\"", "0") %>
					<% } %>
				</td></tr>
				<tr><td class="regtext">
					<% for (int o = 0; 
							o < TableNumVal.NONNUMERIC_JSP.length; o++) { %>
						<br /><input name="nonnumeric" type="radio" value="<%= o %>" 
								<%= o == nonnumeric ? CHECKED : "" %> />
						<%= TableNumVal.NONNUMERIC_JSP[o] %>
					<% } %>
				</td></tr>
				<tr><td><br/><input type="checkbox" name="colorSatisfying" 
						value="true" <%= colorSatisfying ? CHECKED : "" %> />
					highlight cells that satisfy the condition
				</td></tr>
			</table>

		<% } else if (evalConstant == TABLE_CT_NUM) { 
			int operCells = TableCellNumCt.NOT_EQUALS;
			int numCells = 0;
			int row = TableCellNumCt.ANY;
			int column = TableCellNumCt.ANY; 
			String authNum = "";
			String tolerance = "";
			int oper = TableCellNumCt.NOT_EQUALS;
			int emptyCell = TableCellNumCt.IGNORE;
			int nonnumeric = TableCellNumCt.IGNORE;
			if (useInput) { 
				final TableCellNumCt impl = new TableCellNumCt(inputSubeval.codedData);
				operCells = impl.getOperCells();
				numCells = impl.getNumCells();
				row = impl.getRow();
				column = impl.getColumn();
				authNum = impl.getAuthNum();
				tolerance = impl.getTolerance();
				oper = impl.getOper();
				emptyCell = impl.getEmptyCell();
				nonnumeric = impl.getNonnumeric();
			} // useInput 
			final boolean haveNumRows = qData.length > TableQ.ROW_DATA;
			final boolean haveNumCols = qData.length > TableQ.COL_DATA;
			final int numRows = (!haveNumRows ? 0
					: ((CaptionsQDatum) qData[TableQ.ROW_DATA]).getNumRowsOrCols());
			final int numCols = (!haveNumCols ? 0
					: ((CaptionsQDatum) qData[TableQ.COL_DATA]).getNumRowsOrCols());
			final String[][] OPER_ENGLISH = TableCellNumCt.OPER_ENGLISH;
			%>
			<input type="hidden" name="row" value="<%= row %>" />
			<input type="hidden" name="column" value="<%= column %>" />
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
				<tr><td class="regtext">If the number of cells in
				<% final int ROW = 0;
				final int COL = 1;
				for (int grpType = ROW; grpType <= COL; grpType++) { 
					final int grp = (grpType == ROW ? row : column);
					final String grpText = (grpType == ROW ? "row" : "column"); %>
					<select name="<%= grpText %>Selector" 
							onchange="javascript:<%= grpText %>Change()">
						<option value="<%= TableCellNumCt.EVERY %>" <%= 
								grp == TableCellNumCt.EVERY ? SELECTED : "" %> >
						<%= TableCellNumCt.ROW_COL[-TableCellNumCt.EVERY - 1] 
							%> <%= grpText %></option>
						<% if ((grpType == ROW && haveNumRows) || haveNumCols) { %>
							<option value="0" <%= grp > 0 ? SELECTED : "" %>>
								<%= grpText %> number</option>
						<% } // if have number of rows %>
					</select>
					<span id="<%= grpText %>NumCell" style="visibility:<%= 
							grp > 0 ? VISIBLE : HIDDEN %>;">
					<select name="<%= grpText %>Num"
							id="<%= grpText %>Num">
						<% final int limit = (grpType == ROW ? numRows : numCols);
						for (int o = 0; o < limit; o++) { %>
							<option value="<%= o + 1 %>" <%= grp == o + 1 
									? SELECTED : "" %> ><%= o + 1 %></option>
						<% } // for each selection %>
					</select>
					</span>
					<% if (grpType == ROW) { %>
						<br />and
					<% } %>
				<% } // for row, then column %>
				</td></tr>
				<tr><td class="regtext">
					that have a numerical value of
					<select name="oper">
						<% for (int o = 0; o < OPER_ENGLISH[LESSER].length; o++) { %>
							<option value="<%= o %>" <%= o == oper ? SELECTED : "" %> >
							<%= OPER_ENGLISH[LESSER][o] %></option>
						<% } %>
					</select>
					<input type="text" name="authNum" size="15"
							value="<%= authNum %>" /> &#177;
					<input type="text" name="tolerance" size="10"
							value="<%= tolerance %>" />
				</td></tr>
				<tr><td class="regtext">
					is 
					<select name="operCells">
						<% for (int o = 0; o < OPER_ENGLISH[FEWER].length; o++) { %>
							<option value="<%= o %>" <%= o == operCells ? SELECTED : "" %> >
							<%= OPER_ENGLISH[FEWER][o] %></option>
						<% } %>
					</select>
					<input type="text" name="numCells" size="5"
							value="<%= numCells %>" />
				</td></tr>
				<tr><td class="regtext">
					<% for (int o = 0; 
							o < TableCellNumCt.EMPTYCELL_JSP.length; o++) { %>
						<br /><input name="emptyCell" type="radio" value="<%= o %>" 
								<%= o == emptyCell ? CHECKED : "" %> />
						<%= TableCellNumCt.EMPTYCELL_JSP[o].replaceAll("\"\"", "0") %>
					<% } %>
				</td></tr>
				<tr><td class="regtext">
					<% for (int o = 0; 
							o < TableCellNumCt.NONNUMERIC_JSP.length; o++) { %>
						<br /><input name="nonnumeric" type="radio" value="<%= o %>" 
								<%= o == nonnumeric ? CHECKED : "" %> />
						<%= TableCellNumCt.NONNUMERIC_JSP[o] %>
					<% } %>
				</td></tr>
			</table>

		<% } else if (evalConstant == TBL_TXT_NUM) { 
			int rowOper = TableTextNum.ANY_ROW;
			int columnRef = 0; 
			int columnTest = 0; 
			int emptyCell = TableTextNum.IGNORE;
			int nonnumeric = TableTextNum.IGNORE;
			int oper = TableTextNum.NOT_EQUALS;
			String authNum = "";
			String tolerance = "";
			boolean ignoreCase = true;
			String refString = "";
			if (useInput) { // existing evaluator
				final TableTextNum impl = new TableTextNum(inputSubeval.codedData);
				rowOper = impl.getRowOper();
				columnRef = impl.getColumnRef();
				columnTest = impl.getColumnTest();
				emptyCell = impl.getEmptyCell();
				nonnumeric = impl.getNonnumeric();
				oper = impl.getOper();
				authNum = impl.getAuthNum();
				tolerance = impl.getTolerance();
				ignoreCase = impl.getIgnoreCase();
				refString = inputSubeval.molStruct;
			} // useInput 
			final boolean haveNumCols = qData.length > TableQ.COL_DATA;
			final int numCols = (!haveNumCols ? 0
					: ((CaptionsQDatum) qData[TableQ.COL_DATA]).getNumRowsOrCols());
			final String[] OPER_ENGLISH = TableTextNum.OPER_ENGLISH[LESSER];
			%>
			<input type="hidden" name="initIgnoreCase" value="<%= ignoreCase %>" />
			<input type="hidden" name="molname" value="" />
			<table style="margin-left:auto; margin-right:auto; width:445px;
					text-align:left;" summary="">
				<tr><td class="regtext">If, for 
				<select name="rowOper">
					<option value="<%= TableTextNum.ANY_ROW %>" 
							<%= rowOper == TableTextNum.ANY_ROW 
							? SELECTED : "" %>>any</option>
					<option value="<%= TableTextNum.EVERY_ROW %>" 
							<%= rowOper == TableTextNum.EVERY_ROW 
							? SELECTED : "" %>>every</option>
				</select>
				row in which the text in the cell in column
				<% if (numCols > 0) { %>
				<select name="columnRef" id="columnRef">
					<% for (int o = 0; o < numCols; o++) { %>
						<option value="<%= o + 1 %>" <%= columnRef == o + 1 
								? SELECTED : "" %> ><%= o + 1 %></option>
					<% } // for each selection %>
				</select>
				<% } %>
				is 
				<input name="refString" type="text" size="50"
						value="<%= Utils.toValidTextbox(refString) %>" />, 
				<span id="caseSelector"></span> case, 
				the numerical value of the cell in column
				<% if (numCols > 0) { %>
				<select name="columnTest" id="columnTest">
					<% for (int o = 0; o < numCols; o++) { %>
						<option value="<%= o + 1 %>" <%= columnTest == o + 1 
								? SELECTED : "" %> ><%= o + 1 %></option>
					<% } // for each selection %>
				</select>
				<% } %>
				<br />is
				<select name="oper" >
					<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
						<option value="<%= o %>" <%= o == oper ? SELECTED : "" %> >
						<%= OPER_ENGLISH[o] %></option>
					<% } %>
				</select>
				</td></tr>
				<tr><td class="regtext">
					<input type="text" name="authNum" size="20"
							value="<%= authNum %>" /> &#177;
					<input type="text" name="tolerance" size="10"
							value="<%= tolerance %>" />
				</td></tr>
				<tr><td class="regtext">
					<% for (int o = 0; 
							o < TableTextNum.EMPTYCELL_JSP.length; o++) { %>
						<br /><input name="emptyCell" type="radio" 
								value="<%= o %>" 
								<%= o == emptyCell ? CHECKED : "" %> />
						<%= TableTextNum.EMPTYCELL_JSP[o].replaceAll("\"\"", "\"\" or 0") %>
					<% } %>
				</td></tr>
				<tr><td class="regtext">
					<% for (int o = 0; 
							o < TableTextNum.NONNUMERIC_JSP.length; o++) { %>
						<br /><input name="nonnumeric" type="radio" 
								value="<%= o %>" 
								<%= o == nonnumeric ? CHECKED : "" %> />
						<%= TableTextNum.NONNUMERIC_JSP[o] %>
					<% } %>
				</td></tr>
			</table>

		<% } else if (evalConstant == TBL_TXT_TXT) { 
			int rowOper = TableTextText.ANY_ROW;
			int columnRef = 0; 
			int columnTest = 0; 
			int emptyCell = TableTextText.IGNORE;
			boolean isPositive = false;
			int where = TableTextText.IS;
			boolean ignoreCase = true;
			String refString = "";
			String testString = "";
			if (useInput) {
				final TableTextText impl = new TableTextText(inputSubeval.codedData);
				rowOper = impl.getRowOper();
				columnRef = impl.getColumnRef();
				columnTest = impl.getColumnTest();
				emptyCell = impl.getEmptyCell();
				isPositive = impl.getIsPositive();
				where = impl.getWhere();
				ignoreCase = impl.getIgnoreCase();
				final String[] strs = 
						TableTextText.splitStrings(inputSubeval.molStruct);
				refString = strs[0].trim(); 
				testString = strs[1].trim();
			} // useInput 
			final boolean haveNumCols = qData.length > TableQ.COL_DATA;
			final int numCols = (!haveNumCols ? 0
					: ((CaptionsQDatum) qData[TableQ.COL_DATA]).getNumRowsOrCols());
			%>
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<input type="hidden" name="initIgnoreCase" value="<%= ignoreCase %>" />
			<input type="hidden" name="molname" value="" />
			<table style="margin-left:auto; margin-right:auto; width:445px;
					text-align:left;" summary="">
				<tr><td class="regtext">If, for 
				<select name="rowOper">
					<option value="<%= TableTextText.ANY_ROW %>" 
							<%= rowOper == TableTextText.ANY_ROW 
							? SELECTED : "" %>>any</option>
					<option value="<%= TableTextText.EVERY_ROW %>" 
							<%= rowOper == TableTextText.EVERY_ROW 
							? SELECTED : "" %>>every</option>
				</select>
				row in which the text in the cell in column
				<% if (numCols > 0) { %>
				<select name="columnRef" id="columnRef">
					<% for (int o = 0; o < numCols; o++) { %>
						<option value="<%= o + 1 %>" <%= columnRef == o + 1 
								? SELECTED : "" %> ><%= o + 1 %></option>
					<% } // for each selection %>
				</select>
				<% } %>
				is 
				<input name="refString" type="text" size="50"
						value="<%= Utils.toValidTextbox(refString) %>" />, 
				<br/>the text in the cell in column
				<% if (numCols > 0) { %>
				<select name="columnTest" id="columnTest">
					<% for (int o = 0; o < numCols; o++) { %>
						<option value="<%= o + 1 %>" <%= columnTest == o + 1 
								? SELECTED : "" %> ><%= o + 1 %></option>
					<% } // for each selection %>
				</select>
				<% } %>
				<span id="isPosSelector"></span>
				<select name="where" onchange="setIgnoreCase()">
					<% for (int o = 0; 
							o < TableTextText.WHERE_ENGLISH.length; o++) { %>
						<option value="<%= o %>" <%= where == o ? SELECTED : "" %>>
							<%= TableTextText.WHERE_ENGLISH[o] %></option>
					<% } %>
				</select>
				<input name="molstruct" type="text" size="50"
						value="<%= Utils.toValidTextbox(testString) %>" />
				</td></tr>
				<tr><td id="ignoreCaseSpan" class="regtext">
				<span id="caseSelector"></span>
				case
				</td></tr>
				<tr><td class="regtext">
					<% for (int o = 0; 
							o < TableTextText.EMPTYCELL_JSP.length; o++) { %>
						<br /><input name="emptyCell" type="radio" 
								value="<%= o %>" <%= o == emptyCell ? 
								CHECKED : "" %> />
						<%= TableTextText.EMPTYCELL_JSP[o] %>
					<% } %>
				</td></tr>
			</table>

		<% } else if (evalConstant == TABLE_TEXT) { 
			int row = TableTextVal.ANY;
			int column = TableTextVal.ANY; 
			int emptyCell = TableTextVal.IGNORE;
			int where = TableTextVal.IS;
			boolean isPositive = false;
			boolean ignoreCase = true;
			String testString = "";
			boolean colorSatisfying = true;
			if (useInput) {
				final TableTextVal impl = new TableTextVal(inputSubeval.codedData);
				row = impl.getRow();
				column = impl.getColumn();
				where = impl.getWhere();
				isPositive = impl.getIsPositive();
				ignoreCase = impl.getIgnoreCase();
				testString = inputSubeval.molStruct;
				emptyCell = impl.getEmptyCell();
				colorSatisfying = impl.getColorSatisfying();
			} // useInput 
			final boolean haveNumRows = qData.length > TableQ.ROW_DATA;
			final boolean haveNumCols = qData.length > TableQ.COL_DATA;
			final int numRows = (!haveNumRows ? 0
					: ((CaptionsQDatum) qData[TableQ.ROW_DATA]).getNumRowsOrCols());
			final int numCols = (!haveNumCols ? 0
					: ((CaptionsQDatum) qData[TableQ.COL_DATA]).getNumRowsOrCols());
			%>
			<input type="hidden" name="row" value="<%= row %>" />
			<input type="hidden" name="column" value="<%= column %>" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<input type="hidden" name="initIgnoreCase" value="<%= ignoreCase %>" />
			<input type="hidden" name="molname" value="" />
			<table style="margin-left:auto; margin-right:auto; width:445px;
					text-align:left;" summary="">
				<tr><td class="regtext">If the text in the cell in
				<% final int ROW = 0;
				final int COL = 1;
				for (int grpType = ROW; grpType <= COL; grpType++) { 
					final int grp = (grpType == ROW ? row : column);
					final String grpText = (grpType == ROW ? "row" : "column"); %>
					<select name="<%= grpText %>Selector" 
							onchange="javascript:<%= grpText %>Change()">
						<% for (int o = 0; 
								o < TableTextVal.ROW_COL.length; o++) { %>
							<option value="<%= -(o + 1) %>" <%= grp == -(o + 1) 
									? SELECTED : "" %> >
							<%= TableTextVal.ROW_COL[o] %> 
							<%= grpText %></option>
						<% } // for each selection
						if ((grpType == ROW && haveNumRows) || haveNumCols) { %>
							<option value="0" <%= grp > 0 ? SELECTED : "" %>>
								<%= grpText %> number</option>
						<% } // if have number of rows %>
					</select>
					<% final int limit = (grpType == ROW ? numRows : numCols);
					if (limit > 0) { %>
						<span id="<%= grpText %>NumCell" style="visibility:<%= 
							grp > 0 ? VISIBLE : HIDDEN %>;">
						<select name="<%= grpText %>Num"
							id="<%= grpText %>Num">
						<% for (int o = 0; o < limit; o++) { %>
							<option value="<%= o + 1 %>" <%= grp == o + 1 
									? SELECTED : "" %> ><%= o + 1 %></option>
						<% } // for each selection %>
						</select>
						</span>
					<% } %>
					<% if (grpType == ROW) { %>
						<br />and
					<%	} %>
				<% } // for row, then column %>
				</td></tr>
				<tr><td class="regtext">
				<span id="isPosSelector"></span>
				<select name="where" onchange="setIgnoreCase()">
					<% for (int o = 0; 
							o < TableTextVal.WHERE_ENGLISH.length; o++) { %>
						<option value="<%= o %>" <%= where == o ? SELECTED : "" %>>
							<%= TableTextVal.WHERE_ENGLISH[o] %></option>
					<% } %>
				</select>
				<input name="molstruct" type="text" size="50"
						value="<%= Utils.toValidTextbox(testString) %>" />
				</td></tr>
				<tr><td id="tableValAdvice" class="regtext" 
						style="color:green; padding-top:10px; padding-bottom:10px;">
				</td></tr>
				<tr><td id="ignoreCaseSpan" class="regtext">
				<span id="caseSelector"></span>
				case
				</td></tr>
				<tr><td class="regtext">
					<% for (int o = 0; 
							o < TableTextVal.EMPTYCELL_JSP.length; o++) { %>
						<br /><input name="emptyCell" type="radio" 
								value="<%= o %>" 
								<%= o == emptyCell ? CHECKED : "" %> />
						<%= TableTextVal.EMPTYCELL_JSP[o] %>
					<% } %>
				</td></tr>
				<tr><td><br/><input type="checkbox" name="colorSatisfying" 
						value="true" <%= colorSatisfying ? CHECKED : "" %> />
					highlight cells that satisfy the condition
				</td></tr>
			</table>

		<% } else if (evalConstant == TABLE_CT_TXT) { 
			int oper = TableCellTextCt.NOT_EQUALS;
			int numCells = 0;
			int row = TableCellTextCt.ANY;
			int column = TableCellTextCt.ANY; 
			int emptyCell = TableCellTextCt.IGNORE;
			int where = TableCellTextCt.IS;
			boolean isPositive = false;
			boolean ignoreCase = true;
			String testString = "";
			if (useInput) {
				final TableCellTextCt impl = new TableCellTextCt(inputSubeval.codedData);
				oper = impl.getOper();
				numCells = impl.getNumCells();
				row = impl.getRow();
				column = impl.getColumn();
				where = impl.getWhere();
				isPositive = impl.getIsPositive();
				ignoreCase = impl.getIgnoreCase();
				testString = inputSubeval.molStruct;
				emptyCell = impl.getEmptyCell();
			} // useInput 
			final boolean haveNumRows = qData.length > TableQ.ROW_DATA;
			final boolean haveNumCols = qData.length > TableQ.COL_DATA;
			final int numRows = (!haveNumRows ? 0
					: ((CaptionsQDatum) qData[TableQ.ROW_DATA]).getNumRowsOrCols());
			final int numCols = (!haveNumCols ? 0
					: ((CaptionsQDatum) qData[TableQ.COL_DATA]).getNumRowsOrCols());
			final String[] OPER_ENGLISH = TableCellNumCt.OPER_ENGLISH[FEWER];
			%>
			<input type="hidden" name="row" value="<%= row %>" />
			<input type="hidden" name="column" value="<%= column %>" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<input type="hidden" name="initIgnoreCase" value="<%= ignoreCase %>" />
			<table style="margin-left:auto; margin-right:auto; width:445px;
					text-align:left;" summary="">
				<tr><td class="regtext">If the number of cells in
				<% final int ROW = 0;
				final int COL = 1;
				for (int grpType = ROW; grpType <= COL; grpType++) { 
					final int grp = (grpType == ROW ? row : column);
					final String grpText = (grpType == ROW ? "row" : "column"); %>
					<select name="<%= grpText %>Selector" 
							onchange="javascript:<%= grpText %>Change()">
						<option value="<%= TableCellTextCt.EVERY %>" <%= 
								grp == TableCellTextCt.EVERY ? SELECTED : "" %> >
						<%= TableCellTextCt.ROW_COL[-TableCellTextCt.EVERY - 1] 
							%> <%= grpText %></option>
						<% if ((grpType == ROW && haveNumRows) || haveNumCols) { %>
							<option value="0" <%= grp > 0 ? SELECTED : "" %>>
								<%= grpText %> number</option>
						<% } // if have number of rows %>
					</select>
					<span id="<%= grpText %>NumCell" style="visibility:<%= 
							grp > 0 ? VISIBLE : HIDDEN %>;">
					<select name="<%= grpText %>Num"
							id="<%= grpText %>Num">
						<% final int limit = (grpType == ROW ? numRows : numCols);
						for (int o = 0; o < limit; o++) { %>
							<option value="<%= o + 1 %>" <%= grp == o + 1 
									? SELECTED : "" %> ><%= o + 1 %></option>
						<% } // for each selection %>
					</select>
					</span>
					<% if (grpType == ROW) { %>
						<br />and
					<%	} %>
				<% } // for row, then column %>
				</td></tr>
				<tr><td class="regtext">
					whose text,
					<span id="caseSelector"></span>
					case, 
					<span id="isPosSelector"></span>
					<select name="where" onchange="setIgnoreCase()">
						<% for (int o = 0; 
								o < TableCellTextCt.WHERE_ENGLISH.length; o++) { %>
							<option value="<%= o %>" <%= where == o ? SELECTED : "" %>>
								<%= TableCellTextCt.WHERE_ENGLISH[o] %></option>
						<% } %>
					</select>
					<input name="molstruct" type="text" size="50"
							value="<%= Utils.toValidTextbox(testString) %>" />
				</td></tr>
				<tr><td id="ignoreCaseSpan" class="regtext">
					is
					<select name="oper">
						<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
							<option value="<%= o %>" <%= o == oper ? SELECTED : "" %> >
							<%= OPER_ENGLISH[o] %></option>
						<% } %>
					</select>
					<input type="text" name="numCells" size="5"
							value="<%= numCells %>" />
				</td></tr>
				<tr><td class="regtext">
					<% for (int o = 0; 
							o < TableCellTextCt.EMPTYCELL_JSP.length; o++) { %>
						<br /><input name="emptyCell" type="radio" 
								value="<%= o %>" 
								<%= o == emptyCell ? CHECKED : "" %> />
						<%= TableCellTextCt.EMPTYCELL_JSP[o] %>
					<% } %>
				</td></tr>
			</table>

		<% } else if (evalConstant == TABLE_DIFF) { 
			boolean isPositive = false;
			int where = TextContains.IS;
			boolean ignoreCase = true;
			boolean calcGrade = true;
			boolean highlightWrong = true;
			if (useInput) {
				final TableDiff impl = new TableDiff(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
				where = impl.getWhere();
				ignoreCase = impl.getIgnoreCase();
				calcGrade = impl.getCalcGrade();
				highlightWrong = impl.getHighlightWrong();
			} // useInput 
			final String authTable = 
					(Utils.isEmpty(inputSubeval.molStruct) && qData.length >= 3
					? qData[2].data : inputSubeval.molStruct);
			final TableQ tq = new TableQ(authTable);
			%>
			<input type="hidden" name="<%= TableQ.NUM_ROWS_TAG %>" 
					value="<%= ((CaptionsQDatum) qData[TableQ.ROW_DATA]).getNumRowsOrCols() %>" />
			<input type="hidden" name="<%= TableQ.NUM_COLS_TAG %>" 
					value="<%= ((CaptionsQDatum) qData[TableQ.COL_DATA]).getNumRowsOrCols() %>" />
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="initIgnoreCase" value="<%= ignoreCase %>" />
			<table style="margin-left:auto; margin-right:auto; width:445px;
					text-align:left;" summary="">
				<tr><td class="regtext">If 
				<select name="isPositive">
				<option value="true" <%= isPositive ? SELECTED 
						: "" %>>every cell in the response table does</option>
				<option value="false" <%= isPositive ? "" 
						: SELECTED %>>any cell in the response table does not</option>
				</select>
				<select name="where" onchange="setIgnoreCase(); setNumericInfo();">
					<% for (int o = 0; 
							o < TextContains.WHERE_ENGLISH.length; o++) { %>
						<option value="<%= o %>" <%= where == o ? SELECTED : "" %>>
							<%= TextContains.WHERE_ENGLISH[o] %></option>
					<% } %>
				</select>
				<span id="ignoreCaseSpan" class="regtext">
				(<span id="caseSelector"></span> case)</span>
				</td></tr>
				<tr><td>
				the corresponding cell in the author's table:
				</td></tr>
				<tr><td style="text-align:center;">
					<%= tq.convertToHTML(qData, chemFormatting, TableQ.STUD_INPUT) %>
				</td></tr>
				<tr><td style="color:green; padding-top:10px;">[If you
				alter the size of the table or which of its cells are disabled, 
				delete this evaluator and rewrite it.]
				</td></tr>
				<tr><td id="calcsScoreCell" style="padding-top:10px;">
					<table summary="">
					<tr><td><input type="checkbox" name="calcGrade" value="yes"
							<%= calcGrade ? CHECKED : "" %> onchange="changeCalcGrade();" />
					</td><td>
					calculate grade from percentage of table cells in response
					that match the corresponding cell in the author's table
					</td></tr>
					<tr><td><input type="checkbox" name="highlightWrong" value="yes"
								<%= highlightWrong ? CHECKED : "" %> />
					</td><td>
					highlight cells containing incorrect values
					</td></tr>
					</table>
				</td></tr>
				<tr><td id="numericInfo" style="color:green; padding-top:10px; 
						visibility:<%= where == TextContains.IS 
								? VISIBLE : HIDDEN %>;">
					If you wish to compare numerical values, you can include
					a tolerance; enter the values in the form, "7.0 &#177; 0.5".
				</td></tr>
			</table>

		<% } else if (evalConstant == STMTS_CT) { 
			int oper = LogicStmtsCt.EQUALS;
			int number = 0;
			if (useInput) {
				final LogicStmtsCt impl = new LogicStmtsCt(inputSubeval.codedData);
				oper = impl.getOper();
				number = impl.getAuthNum();
			} // useInput 
			final String[] OPER_ENGLISH = LogicStmtsCt.OPER_ENGLISH[FEWER];
			%>
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
				If the number of statements in the response is
				<select name="oper">
					<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
						<option value="<%= o %>" <%= o == oper ? SELECTED : "" %> >
						<%= OPER_ENGLISH[o] %></option>
					<% } %>
				</select>
				<input name="number" type="text" size="2" value="<%= number %>" />
			</td></tr>
			</table>

		<% } else if (evalConstant == TEXT_CONT) { 
			boolean isPositive = false;
			int where = TextContains.IS;
			boolean ignoreCase = true;
			String testString = "";
			if (useInput) {
				final TextContains impl = new TextContains(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
				where = impl.getWhere();
				ignoreCase = impl.getIgnoreCase();
				testString = inputSubeval.molStruct;
			} // useInput %>
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<input type="hidden" name="initIgnoreCase" value="<%= ignoreCase %>" />
			<input type="hidden" name="molname" value="" />
			<table style="margin-left:auto; margin-right:auto; width:445px;
					text-align:left;" summary="">
			<tr><td class="regtext">If the response 
				<span id="isPosSelector"></span>
				<select name="where" onchange="setIgnoreCase()">
				<% for (int o = 0; 
						o < TextContains.WHERE_ENGLISH.length; o++) { %>
					<option value="<%= o %>" 
							<%= where == o ? SELECTED : "" %>>
						<%= TextContains.WHERE_ENGLISH[o] %></option>
				<% } %>
				</select>
				<input name="molstruct" type="text" size="50"
						value="<%= Utils.toValidTextbox(testString) %>" />
			</td></tr>
			<tr><td id="ignoreCaseSpan" class="regtext"
					style="visibility:<%= Utils.among(where, 
							TextContains.MATCHES_REGEX,
							TextContains.CONT_REGEX) ? HIDDEN : VISIBLE %>;">
				<span id="caseSelector"></span>
				case
			</td></tr>
			</table>

		<% } else if (evalConstant == TEXT_WORDS) { 
			int oper = TextWordCount.EQUALS;
			int number = 0;
			if (useInput) {
				final TextWordCount impl = new TextWordCount(inputSubeval.codedData);
				oper = impl.getOper();
				number = impl.getAuthNum();
			} // useInput 
			final String[] OPER_ENGLISH = TextWordCount.OPER_ENGLISH[FEWER];
			%>
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
				If the number of words in the response is
				<select name="oper">
					<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
						<option value="<%= o %>" <%= o == oper ? SELECTED : "" %> >
						<%= OPER_ENGLISH[o] %></option>
					<% } %>
				</select>
				<input name="number" type="text" size="2" value="<%= number %>" />
			</td></tr>
			</table>

		<% } else if (evalConstant == TEXT_SEMANTICS) { 
			boolean isPositive = false;
			String testString = "";
			if (useInput) {
				final TextSemantics impl = new TextSemantics(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
				testString = inputSubeval.molStruct;
			} // useInput %>
			<script type="text/javascript">
				// <!-- >
				function setUpStmts() {
					var texts = new Array(
							'Add statement',
							'Remove last statement',
							'therefore',
							'Please enter a statement before adding another line.',
							'Please do not submit any blank statements.',
							'Please do not submit any statements that contain '
									+ 'unknown words (those struck through and in red).',
							'See acceptable words'
							);
					var stmts = new Array();
					<% final Logic logic = new Logic(testString);
					final String[] stmts = logic.getStatements();
					for (final String stmt : stmts) { %>
						stmts.push('<%= Utils.toValidTextbox(
								Utils.unicodeToCERs(stmt)) %>');
					<% } // for each current statement 
					final String addlWordsStr = (Utils.isEmpty(qData) ? "" : qData[0].data); %>
					setStmts(stmts, texts, '<%= Utils.toValidJS(addlWordsStr) %>',
							'<%= Utils.toValidJS(pathToRoot) %>');
				} // setUpStmts()
				// -->
			</script>
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="molname" value="" />
			<table style="margin-left:auto; margin-right:auto; width:445px;
					text-align:left;" summary="">
			<tr><td class="regtext">If the response 
				<span id="isPosSelector"></span> the same semantics as
			</td></tr>
			<tr><td class="regtext" id="stmtsTable">
			</td></tr>
			</table>

		<% } else if (evalConstant == HUMAN_REQD) { %> 
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext" style="color:green;">
				[ACE will generate automatic feedback for responses that require 
				the instructor's evaluation.  ACE will append your feedback.]
			</td></tr>
			<tr><td class="regtext">
				<br/>If the response must be graded manually by the instructor
			</td></tr>
			</table>

		<% } // if evalConstant %>
	</td></tr>
</table>
</div>
</form>
</body>
</html>
