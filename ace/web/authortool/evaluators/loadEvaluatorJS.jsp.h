	<title>ACE Evaluator Editor</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<style type="text/css">
	body {
		margin:0;
		border:0;
		padding:0;
		height:100%; 
		max-height:100%; 
		font-family:arial, verdana, sans-serif; 
		font-size:76%;
		overflow:hidden; 
	}

	#evalHeader {
		position:absolute; 
		top:0; 
		left:0; 
		width:100%; 
		height:<%= headerHeight %>px; 
		overflow:auto; 
	}

	#evalFooter {
		position:absolute; 
		bottom:0; 
		left:0;
		width:100%; 
		height:<%= footerHeight %>px; 
		overflow:auto; 
		text-align:right; 
		vertical-align:bottom;
		padding-top:20px;
	}

	#evalContents {
		position:fixed; 
		top:<%= headerHeight %>px;
		left:0;
		bottom:<%= footerHeight %>px; 
		right:0; 
		overflow:auto; 
	}

	* html #evalHeader {
		height:100%; 
	}

	* html #evalContents {
		height:100%; 
	}

	* html body {
		padding:<%= headerHeight %>px 0 <%= footerHeight %>px 0; 
	}
	</style>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- avoid parsing the following as HTML <!-->

	<%@ include file="/js/marvinQuestionConstants.jsp.h" %>

	function getFeedback() {
		var suggestion = '<%= Utils.toValidJS(
				Utils.toValidTextbox(inputParentEval.feedback)) %>';
		if (isEmpty(suggestion) && <%= editingSubeval %>) suggestion =
		<% switch (evalConstant) {
			case EvalManager.TOTAL_CHARGE: %>
				'<%= Utils.toValidJS(Utils.toValidTextbox(
				dummyUser.translate("Please draw an uncharged product."))) %>';
			<% break; case EvalManager.NUM_MOLECULES: %>
				'<%= Utils.toValidJS(Utils.toValidTextbox(
				dummyUser.translate("Please draw a single, organic product."))) %>';
			<% break; case EvalManager.LEWIS_VALENCE_ELECS: %>
				'<%= Utils.toValidJS(Utils.toValidTextbox(
				dummyUser.translate("The total number of valence electrons"
				+ " shown in your structure (twice the number of bonds plus the"
				+ " number of unshared electrons) does not equal the"
				+ " number calculated from the formula and charge."))) %>';
			<% break; case EvalManager.LEWIS_OUTER_SHELL_COUNT: %>
				'<%= Utils.toValidJS(Utils.toValidTextbox(
				dummyUser.translate("Each highlighted atom has "
				+ "too many electrons in its outer shell."))) %>';
			<% break; case EvalManager.LEWIS_FORMAL_CHGS: %>
				'<%= Utils.toValidJS(Utils.toValidTextbox(
				dummyUser.translate("The calculated formal charge of "
				+ "each highlighted atom (valence electrons of the element minus "
				+ "the number of bonds minus the number of unshared electrons) "
				+ "does not match the formal charge you have indicated for"
				+ " that atom."))) %>';
			<% break; case EvalManager.LEWIS_ELECTRON_DEFICIENT: %>
				'<%= Utils.toValidJS(Utils.toValidTextbox(
				dummyUser.translate("Each highlighted atom is "
				+ "electron-deficient. In the best answer, none of the "
				+ "highlighted atoms is electron-deficient."))) %>';
			<% break; case EvalManager.MECH_TOPOLOGY: %>
				'<%= Utils.toValidJS(Utils.toValidTextbox(
				dummyUser.translate("You have drawn a mechanism with "
				+ "a linear topology. This mechanism involves radicals, so it "
				+ "is much more likely to proceed by a chain mechanism."
				+ " In a chain mechanism, the boxes must be"
				+ " connected by arrows in a circle, although there may be"
				+ " a short linear sequence of boxes that leads to one box"
				+ " in the circle."))) %>';
			<% break; case EvalManager.MECH_PIECES_COUNT: %>
				'<%= Utils.toValidJS(Utils.toValidTextbox(
				dummyUser.translate("Your mechanism has an insufficient "
				+ "number of steps. Don't try to execute too many mechanistic "
				+ "steps at once."))) %>';
			<% break; case EvalManager.SYNTH_STEPS: %>
				'<%= Utils.toValidJS(Utils.toValidTextbox(
				dummyUser.translate("Your synthesis has too many steps. "
				+ "There is a more efficient route."))) %>';
			<% break; case EvalManager.SYNTH_TARGET: %>
				'<%= Utils.toValidJS(Utils.toValidTextbox(
				dummyUser.translate("The target compound of your synthesis "
				+ "does not match the requested target compound."))) %>';
			<% break; case EvalManager.SYNTH_SM_MADE: %>
				'<%= Utils.toValidJS(Utils.toValidTextbox(
				dummyUser.translate("The highlighted stage contains a "
				+ "compound that you have gone to the trouble of making, but "
				+ "is a permissible starting material."))) %>';
			<% break; case EvalManager.CHOICE_NUM_CHECKED: %>
				'<%= Utils.toValidJS(Utils.toValidTextbox(
				dummyUser.translate("Please choose just one option."))) %>';
			<% break; default: %>
				'';
		<% } // switch evalConstant %>
		return suggestion;
	} // getFeedback()

	function changeEvaluator() {
		self.location.href = getChangeEvaluatorString();
	} // changeEvaluator()

	function getChangeEvaluatorString() {
		var form = document.evaluatorForm;
		var selectedEval = form.evalList.value;
		var gradeType = (form.correct_type ? form.correct_type.value : 'wrong');
		var go = new String.builder().
				append('<%= SELF %>?evalNum=<%= cloneEdit ? 0 : evalNum %>'
					+ '&subevalNum=<%= cloneEdit ? 0 : subevalNum %>'
					+ '&virgin=<%= isFromQuestionJSP %>&evalConstant=').
				append(selectedEval).append('&grade=').
				append(gradeType === 'wrong' 
					? '0.0' : gradeType === 'full' 
					? '1.0' : form.grade ? form.grade.value : '0.0');
		<% if (!Utils.isEmpty(responseIndex)) { %>
			go.append('&responseIndex=<%= responseIndex %>'
						+ '&responseCorrectness=<%= respCorrectnessStr %>');
		<% } %>
		return go.toString();
	} // getChangeEvaluatorString()

	function initGrade() {
		if (<%= inputSubeval.codedData == null || !inputSubeval.calculatesGrade() %>) {
			toggleGradeSelector();
			if (<%= editingParent %>) changeGrade();
		}
	} // initGrade()

	function changeGrade() {
		var form = document.evaluatorForm;
		if (form.correct_type) {
			var gradeType = form.correct_type.value;
			if (gradeType === 'full') {
				form.grade.value = 1.0;
				hideLayer('gradebox');
			} else if (gradeType === 'partial') {
				if (['1', '0'].contains(form.grade.value)) form.grade.value = '0.5';
				showLayer('gradebox');
			} else if (gradeType === 'wrong') {
				form.grade.value = 0.0;
				hideLayer('gradebox');
			} // if gradeType
			onChangeGradeOrPositivity();
		} // if form.correct_type
	} // changeGrade()

	function onChangeGradeOrPositivity() {
		<% if (Utils.among(evalConstant, 
				EvalManager.SYNTH_SCHEME, 
				EvalManager.SYNTH_SELEC)) { %>
			var form = document.evaluatorForm;
			if (form.correct_type.value !== 'full'
					&& form.isPositive.value === 'false') {
				populatePartCreditCell();
			} else {
				clearInnerHTML('partCreditCell');
				clearInnerHTML('synthSelectiveGradingOpts');
			}
		<% } // if SYNTH_SCHEME or SYNTH_SELEC %>
	} // onChangeGradeOrPositivity()

	function gradeOK(form) {
		var valid = true;
		if (form.correct_type && form.correct_type.value === 'partial') {
			valid = canParseToFloat(form.grade.value);
			var grade = parseFloat(form.grade.value);
			valid = valid && grade < 1.0 && grade > 0.0;
		}
		return valid;
	} // gradeOK()

	// used by many evaluators
	function writeIsPosSelector(isPos, posTxt, negTxt) {
		var SELECTED = '<%= SELECTED %>';
		var bld = new String.builder()
				.append('<select name="isPositive" '
					+ 'onchange="onChangeGradeOrPositivity();"'
					+ '><option value="true"');
		if (isPos) bld.append(SELECTED);
		bld.append('>').append(posTxt)
				.append('<\/option><option value="false"');
		if (!isPos) bld.append(SELECTED);
		bld.append('>').append(negTxt).append('<\/option><\/select>');
		var selector = document.getElementById('isPosSelector');
		if (selector) selector.innerHTML = bld.toString();
	} // writeIsPosSelector()

	// --> end HTML comment
</script>
<!-- vim:filetype=jsp
-->
