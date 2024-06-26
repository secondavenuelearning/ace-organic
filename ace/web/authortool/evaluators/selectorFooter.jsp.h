<div id="evalHeader">
<table style="width:90%;" summary="">
	<tr><td class="boldtext enlarged" style="padding-left:20px;">
		<%= evalNum == 0 ? "New" : (cloneEdit ? "Clone of " : "")
					+ "Evaluator " + evalNum %>:
		<% if (editingParent) { // display a selectbox %>
			<span id="gradeSelector"></span>
		<% } // if editing a parent evaluator %>
		<% if (!editingSubeval) { %>
			<p>If the response satisfies this evaluator:
			<input type="hidden" name="evalList" value="-1" />
		<% } else { %>
			<select name="evalList" onchange="changeEvaluator();">
			<% for (final int allowedEvalConstant : allowedEvalConstants) { %>
				<option value="<%= allowedEvalConstant %>" <%= 
						evalConstant == allowedEvalConstant ? SELECTED : "" %>>
					<%= EvalManager.getDescription(allowedEvalConstant, qFlags) %> 
				</option>
			<% } // for evaluator %>
			</select>
		<% } // if is header %>
	</td></tr>
	<tr><td id="warning" style="padding-top:10px; text-align:left;">
		<% if (foreignEval) { %>
			<span class="boldtext" style="color:red;">
			Warning:</span>
			<span class="regtext"> 
			You have loaded a previously existing evaluator of a type 
			that is not normally displayed for the current question 
			type.  The evaluator selected in the popup menu 
			is a different type from the one displayed below.  
			If you wish to write an evaluator of the 
			type selected in the popup menu, you need to change the 
			popup menu above to a different evaluator, then change it 
			back to the desired one.  The information below may be 
			lost if you do so.
		<% } // if eval is not allowed %>
	</td></tr>
</table>
</div>

<div id="evalFooter" style="margin-bottom:0px;">
<script type="text/javascript">
	// <!-- >
	
	function cancelMe() {
		<% if (!isFromQuestionJSP) { %>
			opener.location.href = '../question.jsp?qId=same';
		<% } %>
		self.close();
	} // cancelMe()

	function toggleGradeSelector(show) {
		var showCell = (show == null ? getInnerHTML('gradeSelector') === '' : show);
		setInnerHTML('gradeSelector', showCell
				? '<select name="correct_type" onchange="changeGrade();">'
					+ '<option value="full" <%= inputParentEval.grade >= 1.0 
							? SELECTED : "" %>> Fully correct <\/option>'
					+ '<option value="partial" <%= inputParentEval.grade > 0.0
							&& inputParentEval.grade < 1.0 ? SELECTED : "" %>> ' 
							+ 'Partially correct <\/option>'
					+ '<option value="wrong" <%= inputParentEval.grade == 0.0 
						? SELECTED : "" %>> Wrong <\/option>'
					+ '<\/select>&nbsp;&nbsp;&nbsp;<span id="gradebox">'
					+ 'Grade (0&ndash;1) <input type="text" name="grade" '
					+ 'value="<%= inputParentEval.grade %>" size="4" /><\/span>'
					+ '<\/td><\/tr><tr><td class="boldtext enlarged" '
					+ 'style="padding-top:10px; padding-left:20px;">'
				: 'Grade autocalculated.<br\/>');
		if (showCell) changeGrade();
	} // toggleGradeSelector()

	// --> end HTML comment
</script>
<table style="margin-left:auto; margin-right:auto; width:90%;" summary="">
	<% if (editingParent) { %>
		<tr><td style="text-align:left;">
			<span class="boldtext enlarged">Feedback</span>
			<span class="regtext">
					(Leave blank if this evaluator is or will be 
					part of a compound evaluator)</span> 
			<br/><span id="feedbackBox"></span>
			<script type="text/javascript">
				// <!-- >
				var textBoxHtml = new String.builder().
						append('<textarea id="feedback" name="feedback" '
							+ 'style="overflow:auto; width:100%;" rows="4">').
						append(getFeedback()).
						append('<\/textarea>');
				setInnerHTML('feedbackBox', textBoxHtml.toString());
				// --> end HTML comment
			</script>
		</td></tr>
		</table>
		<table style="margin-left:auto; margin-right:auto; width:90%;" summary="">
	<% } %> 
	<tr><td>
		<table style="margin-left:auto; margin-right:auto;" summary="">
			<tr>
			<td><%= makeButton("Save, close", "submitIt(", RETURN, ");") %></td>
			<% if (editingSubeval && editingParent) { %>
				<td><%= makeButton("Save, add new", "submitIt(", ADD_NEW, ");") %></td>
				<td><%= makeButton("Save, add clone", "submitIt(", ADD_CLONE, ");") %></td>
			<% } // if evaluator is simple %>
			<td><%= makeButton("Cancel", "cancelMe();") %></td>
			</tr>
		</table>
	</td></tr>
</table>
</div>

<!-- vim:filetype=jsp
-->
