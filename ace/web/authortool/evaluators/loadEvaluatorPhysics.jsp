<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.physics.*,
	com.epoch.evals.impl.physicsEvals.*,
	com.epoch.evals.impl.physicsEvals.vectorsEvals.*,
	com.epoch.evals.impl.physicsEvals.eqnsEvals.*,
	java.util.Arrays,
	java.util.List"
%>

<% final String SELF = "loadEvaluatorPhysics.jsp"; %>

<%@ include file="loadEvaluatorJava.jsp.h" %>

<%
	final int VECTORS_CT = EvalManager.VECTORS_CT;
	final int VECTORS_COMP = EvalManager.VECTORS_COMP;
	final int VECTORS_AXES = EvalManager.VECTORS_AXES;
	final int EQN_IS = EvalManager.EQN_IS;
	final int EQN_SOLVED = EvalManager.EQN_SOLVED;
	final int EQNS_FOLLOW = EvalManager.EQNS_FOLLOW;
	final int EQNS_CT = EvalManager.EQNS_CT;
	final int EQN_VARS = EvalManager.EQN_VARS;

	final Figure[] figures = question.getFigures();
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>

<%@ include file="loadEvaluatorJS.jsp.h" %>

<% if (evalConstant == VECTORS_COMP) { %>
	<script src="<%= pathToRoot %>js/drawOnFig.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/offsets.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/position.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/wz_jsgraphics.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<% } // if evalConstant %>
<script type="text/javascript">
	// <!-- >

	<% if (evalConstant == VECTORS_COMP) { 
		int noAnySum = VectorsCompare.NO_VECTOR;
		int vectorQuant = VectorsCompare.WHOLE_VECTOR;
		int oper = VectorsCompare.EQUALS;
		int lengthTolerance = 10;
		int lengthToleranceUnit = VectorsCompare.PERCENT;
		int angleTolerance = 10;
		if (useInput) {
			final VectorsCompare impl = new VectorsCompare(inputSubeval.codedData);
			noAnySum = impl.getNoAnySum();
			vectorQuant = impl.getVectorQuant();
			oper = impl.getOper();
			lengthTolerance = impl.getLengthTolerance();
			lengthToleranceUnit = impl.getLengthToleranceUnit();
			angleTolerance = impl.getAngleTolerance();
		} // useInput 
		final String[] OPER_ENGLISH = VectorsCompare.OPER_ENGLISH[LESSER];
		final DrawVectors authVectors = new DrawVectors(inputSubeval.molStruct);
		final int[][] allCoords = authVectors.getAllCoords();
		Utils.alwaysPrint("loadEvaluatorPhysics.jsp: allCoords = ", allCoords,
				", inputSubeval.molStruct:\n", inputSubeval.molStruct);
	%>

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
			initDrawOnFigButtons(
					'<%= Utils.toValidJS(makeButton("Clear last", 
						"clearLast();")) %>',
					'<%= Utils.toValidJS(makeButton("Clear all", 
						"clearAllOfOne();")) %>',
					'<%= Utils.toValidJS(makeButton("Cancel", 
						"unselect();")) %>');
			setNumShapesLimit(1);
			setClickPurposeMenu();
			initDrawOnFigGraphics('<%= Utils.isEmpty(qData) 
					? "red" : qData[0].data %>');
			captureClicks();
			<% for (final int[] coords : allCoords) { 
				if (coords[DrawVectors.X1] != 0
						|| coords[DrawVectors.Y1] != 0
						|| coords[DrawVectors.X2] != 0
						|| coords[DrawVectors.Y2] != 0) { %>
					allShapes[ARROW].push(
							[canvasSetX(<%= coords[DrawVectors.X1] %>), 
							canvasSetY(<%= coords[DrawVectors.Y1] %>),
							canvasSetX(<%= coords[DrawVectors.X2] %>), 
							canvasSetY(<%= coords[DrawVectors.Y2] %>)]);
				<% } // if vector is not null %>
			<% } // for each vector to paint initially %>
			paintAll();
		<% } // if don't have image to write evaluator %>
	} // initDrawOnFigure()

	function changeVectorQuant() {
		var form = document.evaluatorForm;
		var newVectorQuant = parseInt(form.vectorQuant.value);
		var isWholeVector = 
				newVectorQuant === <%= VectorsCompare.WHOLE_VECTOR %>;
		if (newVectorQuant !== <%= VectorsCompare.DIRECTION %>) {
			var textBld = new String.builder();
			if (isWholeVector) textBld.append('with the magnitude ');
			textBld.append('within <input type="text" name="lengthTolerance" '
					+ 'value="<%= lengthTolerance %>" size="2">'
					+ '<select name="lengthToleranceUnit">'
					+ '<option value="<%= VectorsCompare.PERCENT %>"'
					+ '	<%= lengthToleranceUnit == VectorsCompare.PERCENT
								? SELECTED : "" %>>% of the longest vector</option>'
					+ '<option value="<%= VectorsCompare.PIXELS %>"'
					+ '	<%= lengthToleranceUnit == VectorsCompare.PIXELS
								? SELECTED : "" %>>pixels</option>'
					+ '</select>');
			setInnerHTML('lengthToleranceCell', textBld.toString());
		} else clearInnerHTML('lengthToleranceCell');
		if (newVectorQuant !== <%= VectorsCompare.MAGNITUDE %>) {
			var textBld = new String.builder();
			if (isWholeVector) textBld.append('and the direction ');
			textBld.append('within <input type="text" name="angleTolerance" '
					+ 'value="<%= angleTolerance %>" size="2">&deg;');
			setInnerHTML('angleToleranceCell', textBld.toString());
		} else clearInnerHTML('angleToleranceCell');
		var operBld = new String.builder();
		var oper = form.oper.value;
		if (isWholeVector) {
			<% for (int o = VectorsCompare.EQUALS; 
					o <= VectorsCompare.NOT_EQUALS; 
					o += (VectorsCompare.NOT_EQUALS - VectorsCompare.EQUALS)) { %>
				operBld.append('<option value="<%= o %>"');
				if (oper === <%= o %>) operBld.append(' <%= SELECTED %>');
				operBld.append('><%= OPER_ENGLISH[o] %><\/option>');
			<% } // for each operator %>
		} else {
			<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
				operBld.append('<option value="<%= o %>"');
				if (oper === <%= o %>) operBld.append(' <%= SELECTED %>');
				operBld.append('><%= OPER_ENGLISH[o] %><\/option>');
			<% } // for each operator %>
		} // if WHOLE_VECTOR
		setInnerHTML('oper', operBld.toString());
		setInnerHTML('is_are', isWholeVector ? 'are' : 'is');
		setInnerHTML('vectorQuantEcho', 
				newVectorQuant === <%= VectorsCompare.MAGNITUDE %> ? 'magnitude' 
				: newVectorQuant === <%= VectorsCompare.DIRECTION %> ? 'direction' 
				: 'magnitude and direction');
		captureTextFocus();
	} // changeVectorQuant()

	<% } // if evalConstant %>

	function initSelector() {
		var form = document.evaluatorForm;
		var initIsPositive = (form.initIsPositive 
				&& form.initIsPositive.value === 'true');
		<% if (Utils.among(evalConstant, EQN_SOLVED, EQNS_FOLLOW)) { %>
			writeIsPosSelector(initIsPositive, 'is', 'is not');
		<% } // if evalConstant %>
	} // initSelector()

	function submitIt(addNew) {
		var form = document.evaluatorForm;
		if (!gradeOK(form)) {
			alert('Enter a grade between 0 and 1.');
			return;
		}
		<% switch (evalConstant) { 
		case EQN_IS: %>
			form.molstruct.value = trim(form.molstruct.value);
			if (isEmpty(form.molstruct.value)) {
				alert('Please enter an expression or equation.');
				return;
			}
			var eqns = form.molstruct.value.split(',');
			for (var eqnNum = 0; eqnNum < eqns.length; eqnNum++) { // <!-- >
				var parts = eqns[eqnNum].split('=');
				if (parts.length > 2) {
					alert('Please enter a valid expression or equation.');
					return;
				} else if (parts.length > 1) {
					var leftSide = trim(parts[0]);
					if (!isAlphaNumeric(leftSide)
							|| !isAlphabetical(leftSide.charAt(0))) {
						alert('If you enter an equation, the left side must be '
								+ 'a single variable.');
						return;
					} // if left side is not a single variable
				} // if it's an equation
			} // for each equation
			var isPositiveAndHowManySolutions = 
					form.isPositiveAndHowManySolutions.value.split(':');
			form.isPositive.value = isPositiveAndHowManySolutions[0];
			form.howManySolutions.value = isPositiveAndHowManySolutions[1];
			form.molname.value = form.molstruct.value;
		<% break; 
		case EQN_SOLVED: %> 
			var variable = trim(form.variable.value);
			if (!isAlphaNumeric(variable)
					|| !isAlphabetical(variable.charAt(0))) {
				alert('Please enter a variable that begins with a letter '
						+ 'and contains only alphanumeric characters.');
				return;
			} // if variable is not a single variable
			form.variable.value = variable;
			form.mustBeReduced.value = form.mustBeReducedToNum.checked;
		<% break;
		case EQNS_CT: 
		case EQN_VARS: 
		case VECTORS_CT: %>
			if (!isNonnegativeInteger(form.number.value)) {
				alert('Please enter a valid nonnegative integer.');
				return;
			}
		<% break; 
		case VECTORS_COMP: %> 
			if (form.lengthTolerance
					&& !isWhiteSpace(form.lengthTolerance.value) 
					&& !canParseToInt(form.lengthTolerance.value)) {
				alert('Enter an integral tolerance for the magnitude, or leave it blank.');
				return;
			}
			if (form.angleTolerance
					&& !isWhiteSpace(form.angleTolerance.value) 
					&& !canParseToInt(form.angleTolerance.value)) {
				alert('Enter an integral tolerance for the angle, or leave it blank.');
				return;
			}
			var shapeSet = allShapes[ARROW];
			if (shapeSet.length > 1) {
				alert('Please submit a single vector.');
				return;
			}
			form.molstruct.value = getVectorsXML();
			form.molname.value = form.molstruct.value;
		<% break; 
		case VECTORS_AXES: %> 
			if (!isWhiteSpace(form.tolerance.value) 
					&& !canParseToInt(form.tolerance.value)) {
				alert('Enter an integral tolerance, or leave it blank.');
				return;
			}
		<% break; } // switch evalConstant %>
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
		onload="initGrade(); initSelector(); <%= 
				evalConstant == VECTORS_COMP ? "initDrawOnFigure(); changeVectorQuant();" 
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

		<% if (evalConstant == EQNS_CT) { 
			int oper = EqnsCt.EQUALS;
			int number = 4;
			if (useInput) {
				final EqnsCt impl = new EqnsCt(inputSubeval.codedData);
				oper = impl.getOper();
				number = impl.getAuthNum();
			} // useInput 
			final String[] OPER_ENGLISH = EqnsCt.OPER_ENGLISH[FEWER];
			%>
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
				If the number of equations in the response is
				<select name="oper">
					<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
						<option value="<%= o %>" <%= o == oper ? SELECTED : "" %> >
						<%= OPER_ENGLISH[o] %></option>
					<% } %>
				</select>
				<input name="number" type="text" size="2" value="<%= number %>" />
			</td></tr>
			</table>

		<% } else if (evalConstant == EQN_VARS) { 
			int oper = EqnVariables.EQUALS;
			int number = 4;
			int which = EqnVariables.LAST;
			if (useInput) {
				final EqnVariables impl = new EqnVariables(inputSubeval.codedData);
				oper = impl.getOper();
				number = impl.getAuthNum();
				which = impl.getWhich();
			} // useInput 
			final String[] OPER_ENGLISH = EqnVariables.OPER_ENGLISH[FEWER];
			%>
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
				If the number of units and variables in the
				<select name="which">
					<option value="<%= EqnVariables.FIRST %>" <%= 
							which == EqnVariables.FIRST ? SELECTED : "" %> >first
					</option>
					<option value="<%= EqnVariables.LAST %>" <%= 
							which == EqnVariables.LAST ? SELECTED : "" %> >last
					</option>
				</select>
				entry in the response is
				<select name="oper">
					<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
						<option value="<%= o %>" <%= o == oper ? SELECTED : "" %> >
						<%= OPER_ENGLISH[o] %></option>
					<% } %>
				</select>
				<input name="number" type="text" size="2" value="<%= number %>" />
			</td></tr>
			</table>

		<% } else if (evalConstant == EQN_IS) { 
			int which = EqnIs.LAST;
			boolean isPositive = false;
			String authEqn = "";
			int howManySolutions = EqnIs.ONE;
			if (useInput) {
				final EqnIs impl = new EqnIs(inputSubeval.codedData);
				which = impl.getWhich();
				isPositive = impl.getIsPositive();
				howManySolutions = impl.getHowManySolutions();
				authEqn = inputSubeval.molStruct;
			} // useInput 
			%>
			<input type="hidden" name="molname" value="" />
			<input type="hidden" name="isPositive" value="<%= isPositive %>" />
			<input type="hidden" name="howManySolutions" value="<%= howManySolutions %>" />
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
				If the expressions or equations in the
				<select name="which">
					<option value="<%= EqnIs.FIRST %>" <%= 
							which == EqnIs.FIRST ? SELECTED : "" %> >first
					</option>
					<option value="<%= EqnIs.LAST %>" <%= 
							which == EqnIs.LAST ? SELECTED : "" %> >last
					</option>
				</select>
				entry in the response
				<select name="isPositiveAndHowManySolutions">
					<option value="true:<%= EqnIs.ONE %>" <%= 
							isPositive && howManySolutions == EqnIs.ONE 
							? SELECTED : "" %> >all have solutions found among</option>
					<option value="false:<%= EqnIs.ONE %>" <%= 
							!isPositive && howManySolutions == EqnIs.ONE 
							? SELECTED : "" %> >do not all have solutions found among</option>
					<option value="true:<%= EqnIs.ALL %>" <%= 
							isPositive && howManySolutions == EqnIs.ALL
							? SELECTED : "" %> >correspond exactly to</option>
					<option value="false:<%= EqnIs.ALL %>" <%= 
							!isPositive && howManySolutions == EqnIs.ALL
							? SELECTED : "" %> >do not correspond exactly to</option>
				</select>
				these solutions:
			</td></tr>
			<tr><td class="regtext">
				<input name="molstruct" type="text" size="50"
						value="<%= Utils.toValidTextbox(authEqn) %>" />
			</td></tr>
			<tr><td class="regtext" style="padding-top:20px; color:green;">
				If you enter equations, the left side of each must be the same 
				single variable.
				Do not use implicit multiplication; that is, use 2 * <i>x</i>,
				not 2<i>x</i>.
			</td></tr>
			<tr><td class="regtext" style="color:green; padding-top:20px;">
				[ACE may generate automatic feedback for responses that
				satisfy this evaluator or for malformed responses.  ACE 
				will append your feedback.]
			</td></tr>
			</table>

		<% } else if (evalConstant == EQN_SOLVED) { 
			boolean isPositive = false;
			boolean mustBeReduced = false;
			String variable = "x";
			if (useInput) {
				final EqnSolved impl = new EqnSolved(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
				mustBeReduced = impl.getMustBeReduced();
				variable = impl.getVariable();
			} // useInput 
			%>
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" name="molname" value="" />
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<input type="hidden" name="mustBeReduced" value="<%= mustBeReduced %>" />
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
				If the last equation in the response
				<span id="isPosSelector"></span> 
				solved for the variable
				<input name="variable" type="text" size="5"
						value="<%= Utils.toValidTextbox(variable) %>" />
			</td></tr>
			<tr><td class="regtext" style="padding-top:10px;">
				<table><tr><td style="padding-right:5px; vertical-align:top; 
						text-align:left;">
					<input type="checkbox" name="mustBeReducedToNum"
						<%= mustBeReduced ? CHECKED : "" %> />
				</td><td style="text-align:left;">
					solution must be reduced to a single number
				</td></tr></table>
			</td></tr>
			</table>

		<% } else if (evalConstant == EQNS_FOLLOW) { 
			boolean isPositive = false;
			if (useInput) {
				final EqnsFollow impl = new EqnsFollow(inputSubeval.codedData);
				isPositive = impl.getIsPositive();
			} // useInput 
			%>
			<input type="hidden" name="initIsPositive" value="<%= isPositive %>" />
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
				If each expression or equation in the response
				<span id="isPosSelector"></span> 
				equivalent to the subsequent expression or equation
			</td></tr>
			<tr><td class="regtext" style="color:green; padding-top:20px;">
				[ACE may generate automatic feedback for responses that
				satisfy this evaluator or for malformed responses.  ACE 
				will append your feedback.]
			</td></tr>
			</table>

		<% } else if (evalConstant == VECTORS_CT) { 
			int oper = VectorsCt.EQUALS;
			int number = 4;
			if (useInput) {
				final VectorsCt impl = new VectorsCt(inputSubeval.codedData);
				oper = impl.getOper();
				number = impl.getAuthNum();
			} // useInput 
			final String[] OPER_ENGLISH = VectorsCt.OPER_ENGLISH[FEWER];
			%>
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
				If the number of arrows drawn in the response is
				<select name="oper">
					<% for (int o = 0; o < OPER_ENGLISH.length; o++) { %>
						<option value="<%= o %>" <%= o == oper ? SELECTED : "" %> >
						<%= OPER_ENGLISH[o] %></option>
					<% } %>
				</select>
				<input name="number" type="text" size="2" value="<%= number %>" />
			</td></tr>
			</table>

		<% } else if (evalConstant == VECTORS_COMP) { 
			int noAnySum = VectorsCompare.NO_VECTOR;
			int vectorQuant = VectorsCompare.WHOLE_VECTOR;
			int oper = VectorsCompare.EQUALS;
			int lengthTolerance = 10;
			int lengthToleranceUnit = VectorsCompare.PERCENT;
			int angleTolerance = 10;
			if (useInput) {
				final VectorsCompare impl = new VectorsCompare(inputSubeval.codedData);
				noAnySum = impl.getNoAnySum();
				vectorQuant = impl.getVectorQuant();
				oper = impl.getOper();
				lengthTolerance = impl.getLengthTolerance();
				lengthToleranceUnit = impl.getLengthToleranceUnit();
				angleTolerance = impl.getAngleTolerance();
			} // useInput 
			final String[] OPER_ENGLISH = VectorsCompare.OPER_ENGLISH[LESSER];
			%>
			<input type="hidden" name="molname" value="" />
			<input type="hidden" name="molstruct" value="" />
			<input type="hidden" id="scrollableDiv" value="evalContents" />
			<input type="hidden" id="shapeChooser" value="<%= DrawVectors.ARROW %>">
			<table style="margin-left:0; margin-right:auto; width:445px;
					text-align:left;" summary="">
				<% if (!Utils.isEmpty(figures)) { %>
					<tr><td class="regtext">If the 
						<select name="vectorQuant" onchange="changeVectorQuant();">
							<option value="<%= VectorsCompare.WHOLE_VECTOR %>"
								<%= vectorQuant == VectorsCompare.WHOLE_VECTOR
										? SELECTED : "" %>>magnitude and direction</option>
							<option value="<%= VectorsCompare.MAGNITUDE %>"
								<%= vectorQuant == VectorsCompare.MAGNITUDE
										? SELECTED : "" %>>magnitude</option>
							<option value="<%= VectorsCompare.DIRECTION %>"
								<%= vectorQuant == VectorsCompare.DIRECTION
										? SELECTED : "" %>>direction</option>
						</select>
						of 
						<select name="noAnySum">
							<option value="<%= VectorsCompare.NO_VECTOR %>"
								<%= noAnySum == VectorsCompare.NO_VECTOR
										? SELECTED : "" %>>none</option>
							<option value="<%= VectorsCompare.ANY_VECTOR %>"
								<%= noAnySum == VectorsCompare.ANY_VECTOR
										? SELECTED : "" %>>any</option>
							<option value="<%= VectorsCompare.VECTORS_SUM %>"
								<%= noAnySum == VectorsCompare.VECTORS_SUM
										? SELECTED : "" %>>the sum</option>
						</select>
						of the student's vectors
					</td></tr>
					<tr><td class="regtext">
						<span id="is_are"></span>
						<select id="oper" name="oper" >
						<option value="<%= oper %>"></option>
						</select>
						the <span id="vectorQuantEcho"></span> of the vector below 
					</td></tr>
					<tr><td class="regtext" id="lengthToleranceCell">
					</td></tr>
					<tr><td class="regtext" id="angleToleranceCell">
					</td></tr>
					<tr><td class="regtext" style="color:green; padding-top:10px;">
						For a vector of magnitude 0, do not draw a vector. If you draw a 
						second vector, ACE will erase the first one.
					</td></tr>
					<tr><td class="regtext">
						<div id="canvas" style="position:relative; left:0px; top:0px;">
						<img src="<%= pathToRoot + figures[0].bufferedImage %>" 
								id="clickableImage" alt="picture" class="unselectable"
								onselect="return false;" ondragstart="return false;"/>
						</div>
					</td></tr>
					<tr><td class="regtext">
						<table><tr>
							<td id="clickPurposeCell"></td>
							<td id="clickActions1"></td>
							<td id="clickActions2"></td>
							<!-- <td>acceptText = <span id="acceptText">false</span></td> -->
						</tr></table>
					</td></tr>
				<% } else { %>
					<tr><td class="boldtext" style="color:red;">
						Please upload an image before writing an evaluator.
					</td></tr>
				<% } // if there is a figure %>
			</table>

		<% } else if (evalConstant == VECTORS_AXES) { 
			int howMany = VectorsAxes.ANY;
			int direction = VectorsAxes.X_AND_Y; 
			int tolerance = 5;
			if (useInput) {
				final VectorsAxes impl = new VectorsAxes(inputSubeval.codedData);
				howMany = impl.getHowMany();
				direction = impl.getDirection();
				tolerance = impl.getAngleTolerance();
			} // useInput 
			%>
			<table style="margin-left:auto; margin-right:auto; 
					width:445px; text-align:left;" summary="">
			<tr><td class="regtext">
				If 
				<select name="howMany">
					<% for (int o = 1; o <= VectorsAxes.HOWMANY_ENGL.length; o++) { %>
						<option value="<%= o %>" <%= o == howMany 
								? SELECTED : "" %> >
						<%= VectorsAxes.HOWMANY_ENGL[o - 1] %></option>
					<% } %>
				</select>
				vector lays along the
				<select name="direction">
					<% for (int o = 0; o < VectorsAxes.DIRECTION_ENGL.length; o++) { %>
						<option value="<%= o %>" <%= o == direction 
								? SELECTED : "" %> >
						<%= VectorsAxes.DIRECTION_ENGL[o] %></option>
					<% } %>
				</select>axis within
				<input name="tolerance" type="text" size="2" value="<%= tolerance %>" />
				degrees
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
