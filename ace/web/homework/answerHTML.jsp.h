<table style="width:95%; margin-left:auto; margin-right:auto; text-align:left;" summary="">
<tr><td class="regtext">
	<table style="width:100%;" summary="">
	<tr><td class="regtext" style="vertical-align:top;">

<!-- Left-hand side of screen -->
		<table class="whiteTable" style="border-collapse:collapse;
				width:90%; background-color:#f6f7ed; text-align:left;" summary="">

<!-- General information on assignment -->
		<tr><td style="padding-top:10px; padding-left:10px;
				padding-right:10px; padding-bottom:10px;">
			<table style="width:100%;" summary=""><tr>
			<td class="boldtext big">
				<%= assgt.getName() %>
			</td>
			<% if (hwsession.isExam() && mode == SOLVE) { %>
				<td class="boldtext big" style="text-align:right;">
					<span id="clockRemaining"></span>
				</td>
				</tr>
				<tr><td colspan="2">
			<% } else { %>
				</tr>
				<tr><td>
			<% } // if is exam %>
			<% if (solveOrViewMode) { %>
				<%= isMasteryAssgt
					? user.translate("Maximum allowed tries to demonstrate mastery")
					: user.translate("Maximum allowed tries per question") %>: <b>
					<%= allowUnlimitedTries
							? user.translate("Unlimited") : maxTries
					%></b><br/>
				<% if (!Utils.isWhitespace(assgt.getRemarks())) { %>
					<i><%= chemFormatting ? Utils.toDisplay(assgt.getRemarks())
							: assgt.getRemarks() %></i>
				<% }
			} else if (practiceOrSimilarMode && !disallowSubmit) { %>
				<%= user.translate("You are in Practice mode.") %>
			<% } %>
			</td></tr>
			</table>
		</td></tr>

<!-- Question statement -->
		<tr><td class="regtext" style="padding-top:10px; padding-left:10px;
				padding-right:10px; padding-bottom:10px;">
			(<b><%= qNumDisplay %></b>)
	<% 		if (isInstructorOrTA) {
				if (qId != 0) {
					final String ques = (mode != PREVIEW
							? user.translateJS("Question")
							: "Question"); %>
					<span style="color:red;">(<%= ques %> #<%= 
							Utils.formatNegative(qId) %>)</span>
	<%			} else { %>
					<span style="color:red;">(New Question)</span>
	<%			} // if qId
			} // if user is instructor or TA
			if (pts != null) { %>
				<b>[<%= user.translate("1".equals(pts) ? "***1*** point"
						: "***2*** points", pts) %>]</b>&nbsp;&nbsp;
	<%		} // if there are points to display
		 	String qStatement = hwsession.getCurrentStatement();
			if (mode == PREVIEW) {
				// modified, unsaved statement passed through session 
				// attribute from mainheader.jsp to startPreview.jsp 
				// to jumpGo.jsp and then to here
				String qStatementMod;
				synchronized (session) {
					qStatementMod = (String) session.getAttribute("qStmt");
				} // synchronized
				if (qStatementMod != null) {
					qStatement = Utils.restoreLineBreaks(qStatementMod);
				} // if there's no attribute
				final QSetDescr qSetDescr = QSetRW.getQSetDescr(
							hwsession.getCurrentQSetId(),
							hwsession.getUserId());
				if (qSetDescr != null 
						&& !Utils.isEmptyOrWhitespace(qSetDescr.header)) {
					qStatement = Utils.toString(qSetDescr.header, ' ', qStatement); 
				} // if there's a common Q statement
			} // if PREVIEW
			if (isFillBlank) { 
				final Choice fillBlankResp = new Choice(lastResp);
				final int[] options = fillBlankResp.getAllOptions();
				final boolean[] choices = fillBlankResp.getAllChoices();
				/* Utils.alwaysPrint("answerHTML.jsp.h: fillblank last response = ",
						lastResp, "; options = ", options, ", choices = ", choices); /**/
			%>
				<%= question.getDisplayStatement(qStatement, fillBlankResp, 
						Choice.ADD_MENUS) %>
				</td></tr>
<!-- Submit button for fillBlank only -->
				<tr><td id="submitCell" style="text-align:center; padding-left:10px;
						padding-right:10px; padding-bottom:10px;
						padding-top:10px; visibility:visible">
			<% } else if (isNumeric && usesSubstns 
					&& (mode != GRADEBOOK_VIEW || !isInstructorOrTA)) {
				final String[] values = hwsession.getCurrentSubstns();
				final String qStmt = 
						question.getDisplayStatement(qStatement, values); %>
				<%= chemFormatting ? Utils.toDisplay(qStmt) : qStmt %>
			<% } else { // no substitutions in statement %>
				<%= chemFormatting ? Utils.toDisplay(qStatement) : qStatement %>
			<% } // if isFillBlank %>
			<% final StringBuilder refBld = new StringBuilder();
			final String book = question.getBook();
			if (book != null && "Literature".equals(book)
					&& (isInstructorOrTA || assgt.showRefsBeforeAnswered()
						|| (assgt.showRefsAfterAnswered() 
							&& evalResult != null
							&& evalResult.grade == 1.0))) { 
				final String chapter = question.getChapter();
				final String remarks = question.getRemarks();
				refBld.append("<p>");
				if (!Utils.isEmpty(remarks)) {
					refBld.append("<a href=\"");
					if (!remarks.startsWith("http://")) refBld.append("http://");
 					Utils.appendTo(refBld, remarks, "\" target=\"window2\">");
				}
				refBld.append(chapter);
				if (!Utils.isEmpty(remarks)) refBld.append("</a>");
				refBld.append("</p>");
			} // if should display reference %>
			<%= refBld.toString() %>
		</td></tr>

<!-- Figures -->
<% if (isFillBlank && !Utils.isEmpty(figures)) { // put Figures on right side of screen %>
		</table>
	</td><td rowspan="2" style="vertical-align:top;">
		<table class="whiteTable" style="margin-left:auto; margin-right:auto;
				background-color:#f6f7ed; width:445px; text-align:center;" summary="">
<% } // if isFillBlank %>
		<tr><td style="padding-bottom:10px; padding-left:10px;
				padding-right:10px;">
			<%
			final int startFigNum = (isClickableImage || isDrawVectors ? 2 : 1);
			for (int figNum = startFigNum; figNum <= figures.length; figNum++) {
				final Figure figure = figures[figNum - 1]; %>
				<%@ include file="dispFigure.jsp.h" %>
			<%
			} // for figNum %>
		</td></tr>
		</table>
<% if (!isFillBlank) { %>
	</td>

<!-- Right-hand side of screen -->
	<td style="vertical-align:top;<%= isRCD ? " width:67%;" : "" %>" rowspan="2">
		<table class="whiteTable" style="margin-left:auto; 
				margin-right:auto; background-color:#f6f7ed; width:<%= !isRCD 
						? "445px" : "95%" %>; text-align:left;" summary="">
					
<!-- Response collector -->
		<tr><td class="boldtext" style="text-align:right; padding-left:10px;
				padding-right:10px; padding-top:10px; font-style:italic;">
			<% if (isLewis) { %>
				LewisJS&trade; 
			<% } else if (isMarvin || isMechanism || isSynthesis || isOther) { %> 
				MarvinJS&trade;
			<% } // if qType %> 
		</td></tr>
		<tr><td style="text-align:center; padding-left:10px; padding-right:10px;">
			<table class="whiteTable" style="width:95%;" summary="">
			<tr><td id="responseCollectorCell">
				<% if (isNumeric) {
					final Numeric numResp = new Numeric(lastResp);
					final double num = numResp.getCoefficient();
					final String numStr = numResp.getCoefficientStr();
					final double exponent = numResp.getExponent();
					final String exponentStr = numResp.getExponentStr();
					final int unit = numResp.getUnitNum(); 
				%>
					<input type="text" size="10" id="numericResponse"
							value="<%= !useSciNotn && exponent != 0 ?
										num * Math.pow(10, exponent)
									: numStr == null ? "" : numStr %>" />
				<% 	if (useSciNotn) { %>
						&nbsp;&times;&nbsp;10<sup><input type="text"
								size="3" id="exponent"
								value="<%= exponentStr %>" /></sup>
				<% 	} // if useSciNotn
					if (!Utils.isEmpty(qData)) {
						final int numQData = qData.length;
					 	if (numQData == 1) { 
				%>
							&nbsp;&nbsp;<%= qData[0].toShortDisplay(chemFormatting) %>
							<input type="hidden" name="unit" id="unit"
									value="1" />
				<% 		} else { 
				%>
							&nbsp;&nbsp;<select name="unit" id="unit">
								<option value="0">&nbsp;</option>
				<% 				for (int qdNum = 1; qdNum <= numQData; qdNum++) { 
				%>
									<option value="<%= qdNum %>" <%= qdNum == unit 
											? "selected=\"selected\"" : "" %>>
									<%= Utils.toPopupMenuDisplay(qData[qdNum - 1].data) %>
									</option> 
				<% 				} // for each Qdatum qdNum 
				%>
							</select>
				<% 		} // if qData.length
					} // if qData isn't null or empty
				} else if (isTable) { 
				%>
					<input type="hidden" name="<%= TableQ.NUM_ROWS_TAG %>" 
							id="<%= TableQ.NUM_ROWS_TAG %>"
							value="<%= ((CaptionsQDatum) qData[TableQ.ROW_DATA])
								.getNumRowsOrCols() %>" />
					<input type="hidden" name="<%= TableQ.NUM_COLS_TAG %>" 
							id="<%= TableQ.NUM_COLS_TAG %>"
							value="<%= ((CaptionsQDatum) qData[TableQ.COL_DATA])
								.getNumRowsOrCols() %>" />
					<span id="tableDisp"><%= tableDisp %></span>
				<% } else if (isLogicalStmts) { %>
					<span id="stmtsTable" class="regtext"></span>
				<% } else if (isEquations) { %>
					<span id="eqnsTable" class="regtext"></span>
					<span id="eqnsAlerts" class="regtext"></span>
					<input type="hidden" name="<%= Equations.VARS_NOT_UNITS_TAG %>" 
							id="<%= Equations.VARS_NOT_UNITS_TAG %>" value="" />
				<% } else if (isText) { %>
					<form accept-charset="UTF-8">
					<textarea id="textResp" name="textResp" cols="65" rows="15"
							style="overflow:auto;"><%=
									Utils.toValidTextbox(lastResp) %></textarea>
					</form>
				<% } else if (isFormula) { %>
					<input type="text" id="textResp" name="textResp" size="20"
							value="<%= Utils.toValidTextbox(lastResp) %>" />
				<% } else if (isChoice || isChooseExplain) {
					if (qData != null) {
						final ChooseExplain chooseExplainResp = (isChooseExplain
								? new ChooseExplain(lastResp) : null);
						final Choice choiceResp = (isChoice
								? new Choice(lastResp)
								: chooseExplainResp.choice);
						// options contains 1-based qData in arbitrary order
						final int[] options = choiceResp.getAllOptions();
						final boolean[] choices = choiceResp.getAllChoices();
						final int numOpts = options.length;
						final int numQData = qData.length;
				%>	
						<table id="choiceRankTable" summary="">
				<%		final String CHECKED = "checked=\"checked\"";
						for (int optNum = 1; optNum <= numQData; optNum++) {
							final int qdNum = (optNum > numOpts
										|| options[optNum - 1] <= 0 // if corrupted
									? optNum : options[optNum - 1]);
							final boolean chosen = (optNum <= numOpts
									? choices[optNum - 1] : false);
				 			final QDatum thisOpt = qData[qdNum - 1];
							if (!thisOpt.isText()) { 
				%>
								<tr><td class="boldtext" colspan="2"
										id="launchMViewCell<%= optNum %>"
										style="text-align:right; padding-left:10px; 
											font-style:italic;">
									<input type="hidden" 
										id="qdNumOfCell<%= optNum %>"
										value="<%= qdNum %>" />
									<a onclick="launchMView(<%= optNum %>);"><u>Launch 
										MarvinJS&trade; viewer</u></a>
								</td></tr>
				<%			} // if not text
				%>
							<tr><td class="regtext" style="vertical-align:middle;">
								<br/>
				<%	
								if (disallowMultipleResponses) { // use radio buttons
				%>
									<input type="radio" name="options"
											value="<%= qdNum %>"
											<%= chosen ? CHECKED : "" %> />
				<%				} else { // use checkboxes
				%>
									<input type="checkbox" name="option<%= qdNum %>"
											id="option<%= qdNum %>" value="<%= qdNum %>"
											<%= chosen ? CHECKED : "" %> />
				<%				} // if disallowMultipleResponses
								final String qdFigId = Utils.toString("qdDisplay", optNum);
				%>	
							</td>
							<td class="regtext" id="<%= qdFigId %>"
									style="text-align:left; vertical-align:middle;">
				<% 				if (thisOpt.isText()) { %>
									<br/>
									<%= chemFormatting ? Utils.toDisplay(thisOpt.data) 
											: thisOpt.data %>
				<% 				} else { 
				%>
									<%= thisOpt.getImage(pathToRoot, user.prefersPNG(), 
										qdFigId) %>
				<% 				} // if text %>
							</td></tr>
				<%		} // for each option optNum
				%>
						</table>
				<% 		if (isChooseExplain) { %>
							<br /><br />
							<form accept-charset="UTF-8">
							<textarea id="textResp" name="textResp" cols="65" rows="15"
									style="overflow:auto;"><%= Utils.toValidTextbox(
										chooseExplainResp.text) %></textarea>
							</form>
				<%		} // if is chooseExplain
				 	} else { // qData is null %>
						No choices are provided.
				<% 	} // if qData
				} else if (isRank) {
					if (qData != null) {
						final Rank rankResp = new Rank(lastResp);
						// items contains 1-based qData in arbitrary order
						final int[] items = rankResp.getAllItems();
						final int[] ranks = rankResp.getAllRanks();
						final int numItems = items.length;
						final int numQData = qData.length;
				%>	
						<table id="choiceRankTable" summary="">
				<%		for (int itemNum = 1; itemNum <= numQData; itemNum++) {
							final int qdNum = (itemNum > numItems
										|| items[itemNum - 1] <= 0 // if data corrupt
									? itemNum : items[itemNum - 1]);
							final int rank = (itemNum <= numItems
									? ranks[itemNum - 1] : 0);
				 			final QDatum thisItem = qData[qdNum - 1];
				 			if (!thisItem.isText()) { 
				%>
								<tr>
								<td><input type="hidden" 
										id="qdNumOfCell<%= itemNum %>"
										value="<%= qdNum %>" />
								</td><td id="launchMViewCell<%= itemNum %>" 
										class="boldtext" colspan="2"
										style="text-align:right; padding-left:10px; 
											font-style:italic;">
									<a onclick="launchMView(<%= itemNum %>);"><u>Launch 
										MarvinJS&trade; viewer</u></a>
								</td></tr>
				<%			} // if the item is not text
							final String qdFigId = Utils.toString("qdDisplay", itemNum);
				%>
							<tr><td class="regtext" style="vertical-align:middle;">
								<br/>
								<!-- QD<%= items[itemNum - 1] %> -->
								<select name="qdValue<%= itemNum %>"
										id="qdValue<%= itemNum %>">
									<option value="0">&nbsp;</option>
									<% for (int rankNum = 1;
											rankNum <= numQData; rankNum++) { %>
										<option value="<%= rankNum %>"
											<%= rankNum == rank
													? "selected=\"selected\""
													: "" %>> <%= rankNum %>
										</option>
									<% } // for each rankNum %>
								</select>
							</td>
							<td class="regtext" id="<%= qdFigId %>"
									style="text-align:left; vertical-align:middle;">
				<% 				if (thisItem.isText()) { 
				%>
									<br/>
									<%= chemFormatting ? Utils.toDisplay(thisItem.data) 
											: thisItem.data %>
				<% 				} else { 
				%>
									<%= thisItem.getImage(pathToRoot, user.prefersPNG(), 
											qdFigId) %>
				<% 				} // if text %>
							</td></tr>
				<%		} // for each item itemNum
				%>
							<tr><td style="padding-top:10px;" colspan="2">
								<table style="margin-left:auto; margin-right:auto;"
										summary="">
								<tr><td>
									<%= makeButton(ansPhrases.get(SORT_BY_NUM), 
											"repaintRank();") %>
								</td></tr>
								</table>
							</td></tr>
						</table>
				<% 	} else { // qData is null or empty %>
						No choices are provided.
				<% 	} // if qData
				} else if (isClickableImage || isDrawVectors) { 
					if (!previewOrTextbookMode && mode != GRADEBOOK_VIEW) { 
				%>
						<input type="hidden" id="scrollableDiv" 
								value="contentsWithTabsWithoutFooter" />
				<% 	} // if this page has a scrollable DIV 
				%>
					<table summary="drawOnFigureTable">
					<tr><td style="text-align:left;">
						<div id="canvas" style="position:relative; left:0px; top:0px;">
						<img src="<%= pathToRoot %><%= figures[0].bufferedImage %>" 
								id="clickableImage" alt="picture" 
								class="unselectable"
								onselect="return false;" ondragstart="return false;"/>
						</div>
					</td></tr>
					<tr><td style="width:100%;">
						<input type="hidden" id="shapeChooser" 
								value="<%= isClickableImage 
									? ClickImage.MARK : DrawVectors.ARROW %>" />
						<table><tr>
							<td id="clickPurposeCell"></td>
							<td id="clickActions1" style="text-align:left;"></td>
							<td id="clickActions2"></td>
						</tr></table>
					</td></tr>
				<%	if (isDrawVectors && !Utils.isEmpty(preloadMol)) { %>
						<tr><td id="startOver">
						<%= makeButton(user.translate("Start over"), "startOver();") %>
						</td></tr>
				<%	} // if user can start over %>
					</table>
				<% } else if (isOED || isRCD) {
					final OED oed = (isRCD ? null : Utils.isEmpty(qData) 
							? new OED() : new OED(qData));
					final RCD rcd = (isOED ? null : Utils.isEmpty(qData) 
							? new RCD() : new RCD(qData));
					int numRows;
					int numCols;
					String[] captions = null;
					boolean haveYAxisScale;
					String tableParams;
					int canvasColSpan;
					String yAxisUnit;
					String[] yAxisLabels;
					if (isOED) {
						numRows = oed.getNumRows();
						numCols = oed.getNumCols();
						captions = oed.getCaptions();
						haveYAxisScale = oed.haveYAxisScale();
						canvasColSpan = numCols + (haveYAxisScale ? 1 : 0);
						tableParams = "style=\"width:600px; border-collapse:collapse; "
								+ "border-style:none;\" summary=\"diagram\"";
						yAxisUnit = oed.getYAxisUnit();
						yAxisLabels = oed.getYAxisLabels();
					} else {
						numRows = rcd.getNumRows();
						numCols = rcd.getNumCols();
						haveYAxisScale = rcd.haveYAxisScale();
						canvasColSpan = numCols + (haveYAxisScale ? 2 : 1);
						tableParams = "class=\"rcdTable\" style=\"width:95%; "
								+ "border-style:none;\" summary=\"diagram\"";
						yAxisUnit = rcd.getYAxisUnit();
						yAxisLabels = rcd.getYAxisLabels();
					} // if isOED
				%>
					<input type="hidden" id="numRows" value="<%= numRows %>" />
					<input type="hidden" id="numCols" value="<%= numCols %>" />
					<table <%= tableParams %>>
						<tr>
						<td colspan="<%= canvasColSpan %>" <%= isRCD 
								? "style=\"border-right-width:0px;\"" : "" %>>
							<div id="canvas0" style="position:relative; left:0px; 
									top:0px; width:600px; height:2px; 
									overflow:visible;">
							</div>
						</td>
						</tr>
						<tr>
				<% 		if (haveYAxisScale) { %>
							<th class="boldtext" style="text-align:center; 
									width:30px; border-right-style:solid; 
									border-right-width:1px; border-right-color:black;
									border-bottom-style:solid; border-bottom-width:1px; 
									border-bottom-color:black;">
								<%= Utils.toDisplay(user.translate("Energy")) %>
								(<%= Utils.toDisplay(yAxisUnit) %>)
							</th>
				<% 		} else if (isRCD) { %>
							<th style="border-bottom-style:none; width:1px;"></th>
				<% 		} // if there's a scale to display on the y-axis
						for (int cNum = 0; cNum < numCols; cNum++) { %>
							<th class="boldtext" style="text-align:center; 
									border-bottom-style:solid; border-bottom-width:1px; 
									border-bottom-color:black;">
								<%= !isOED || cNum >= captions.length ? "&nbsp;" 
									: Utils.toDisplay(captions[cNum]) %>
							</th>
				<% 		} // for each column %>
						</tr>
				<% 		for (int rNum = 0; rNum < numRows; rNum++) { %>
							<tr>
				<% 			if (haveYAxisScale) { %>
								<td class="boldtext" 
										style="text-align:right; white-space:nowrap;
											width:30px; border-right-style:solid; 
											border-right-width:1px; 
											border-right-color:black;
											padding-left:10px; padding-right:10px;">
									<%= yAxisLabels[rNum] %>
								</td>
				<% 			} else if (isRCD) { %>
								<td style="border-right-style:solid; 
										border-right-width:1px; 
										border-right-color:black;">
								</td>
				<% 			} // if there's a scale to display on the y-axis
							for (int cNum = 0; cNum < numCols; cNum++) { 
								final int row = numRows - rNum; 
								final int col = cNum + 1; %>
								<td class="regtext" id="r<%= row %>c<%= col %>" 
										style="text-align:center; white-space:nowrap;
											padding-left:10px; padding-right:10px;">
								</td>
				<% 			} // for each column %>
							</tr>
				<% 		} // for each row %>
						<tr>
				<% 		if (haveYAxisScale || isRCD) { %>
							<td style="border-right-style:none; width:1px;"></td>
				<% 		} // if haveYAxisScale %>
						<td colspan="<%= numCols %>" 
								style="width:100%; height:1px; border-top-style:solid; 
								border-top-width:1px; border-top-color:black; 
								border-left-style:none; border-right-style:none;">
						</td>
						</tr>
					</table>
					<table style="width:95%; margin-left:auto; margin-right:auto;"
							 summary="selectorsAndButtons">
						<tr>
						<td id="textAndButtons" colspan="<%= canvasColSpan %>">
						</td></tr>
						<tr>
					</table>
				<% } else if (isLewis) { %>
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
				<% } else { // mechanism, synthesis, structure 
					// needed for startMarvinJS()
					final int[] dims = new int[] {450, 400};
					if (isMechanism || isSynthesis) {
						dims[0] = 500;
						dims[1] = 530;
					} // if mechanism or synthesis
					final String toMarvin = Utils.isEmpty(lastResp) ? Utils.EMPTY_MRV 
							: Utils.unicodeToCERs(MolString.convertMol(lastResp, Utils.MRV));
					/* Utils.alwaysPrint("answerHTML.jsp.h: after converting to MRV:\n",
							toMarvin); /**/
				%>
					<div id="<%= APPLET_NAME %>" style="text-align:center; 
								width:<%= dims[0] %>px; height:<%= dims[1] %>px;">
					<script type="text/javascript">
						// <!-- >
						startMarvinJS('<%= Utils.toValidJS(toMarvin) %>', 
								<%= question.getQType() %>, 
								<%= qFlags %>, 
								'<%= APPLET_NAME %>', 
								'<%= pathToRoot %>');
						// -->
					</script>
					</div>
				<% } // if question type
				%>
			</td></tr>
			</table>
		<% if (isSynthesis) { %>
			<table class="whiteTable" style="margin-left:auto; margin-right:auto;
					width:100%; text-align:left;" summary="reagentTable">
			<tr><td id="reagentTable" class="regtext" >
			</td></tr>
			<% if (isInstructorOrTA) { %>
				<tr><td id="pasteSynCell">
				<p><%= ansPhrases.get(PASTE_SYN) %> </p>
				<p><textarea id="pasteSynthesis" name="pasteSynthesis" 
						rows="1" cols="10"
						onkeyup="loadPastedSynthMRV();"
						style="height:40px; width:95%;"></textarea>
				</td></tr>
			<% } // if instructorOrTA using MarvinJS %>
			</table>
		<% 	} // if isSynthesis %>
		</td></tr>

<!-- Submit button -->
		<tr>
		<td id="submitCell" style="text-align:center; padding-left:10px;
				padding-right:10px; padding-bottom:10px;
				visibility:visible">
		</td>
		</tr>
		</table>
<% } // if not isFillBlank %>

<% if (figures.length <= 2 || isFillBlank) {
		// if >= 3 figures, better to have feedback on right %>
	</td></tr>
		
<!-- Left-hand side of screen again -->
	<tr><td class="regtext" style="vertical-align:bottom;">
<% } // if there are two or fewer figures %>

		<table class="whiteTable" style="border-collapse:collapse;
				width:<%= figures.length <= 2 ? "90" : "100" %>%;
				background-color:#f6f7ed; text-align:left;" summary="">

<!-- Feedback -->
			<tr class="greenrow" style="height:2px;">
			<td class="boldtext" style="padding-left:10px; padding-right:10px;
					padding-top:5px;" id="statusText">
			</td></tr>
			<tr class="greenrow" style="height:2px;">
			<td class="regtext" style="padding-left:10px; padding-right:10px;
					padding-bottom:5px;" id="feedbackText">
			</td></tr>
			<tr class="greenrow" style="height:2px;">
			<td class="boldtext" style="padding-left:10px; padding-right:10px;
					padding-top:5px;" id="status2Text">
			</td></tr>
<% if (figures.length >= 3 && !isFillBlank) { // navigation bars appear on the left regardless %>
		</table>
	</td></tr>
<!-- Left-hand side of screen again -->
	<tr><td class="regtext" style="vertical-align:bottom;">
		<table class="whiteTable" style="border-collapse:collapse;
				width:90%; background-color:#f6f7ed; text-align:left;" summary="">
<% } // if there are 3 or more figures %>

<!-- Navigation -->
		<tr><td>
			<table style="background-color:#f6f7ed;
					margin-left:auto; margin-right:auto; white-space:nowrap;" summary="">
				<tr>
				<% if (!previewOrTextbookMode && mode != GRADEBOOK_VIEW) { %>
					<td id="previousCell" style="width:50%; visibility:visible;">
						<% if (currentQNum > 1) { %>
							<%= makeButtonIcon("back", pathToRoot, "movePrev();") %>
						<% } // if should display Previous arrow %>	
					</td>
				<% } // if should display Previous arrow %>	
				<td style="text-align:center;">
					<% if (mode == PREVIEW) { %>
						<%= makeButton("Close Preview", "self.close();") %>
					<% } else if (Utils.among(mode, GRADEBOOK_VIEW, TEXTBOOK)) { %>
						<%= makeButton(user.translate("Close"), "self.close();") %>
					<% } else { %>
						<%= makeButton(user.translate("Question List"),
								"viewList();") %>
					<% } // if mode %>	
				</td>
				<% if (!previewOrTextbookMode && mode != GRADEBOOK_VIEW) { %>
					<td style="text-align:center;">
						<%= makeButton(user.translate("Assignments List"),
								"closeHW();") %>
					</td>
					<td id="nextCell" style="width:50%; text-align:right;">
						<% if (currentQNum < hwsession.getCount()) { %>
							<%= makeButtonIcon("next", pathToRoot, "moveNext();") %>
						<% } // if not at last question %>	
					</td>
				<% } // if should display Next arrow %>	
				</tr>
			</table>
		</td></tr>
		</table>
	</td></tr>
	</table>
</td></tr>
<tr><td id="reportingCell"></td></tr>
</table>
<!-- vim:filetype=jsp
-->
