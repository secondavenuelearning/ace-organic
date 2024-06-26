<table class="regtext" style="margin-left:5px; margin-right:auto; 
		border-collapse:collapse;" summary="">
	<tr>
	<td class="boldtext big" style="text-align:left; padding-bottom:10px; padding-top:10px;">
		<i><%= Utils.toDisplay(book.getName()) %></i>, 
		<% final int numAuthors = allAuthorNames.length;
		if (numAuthors == 2) { %>
			<%= allAuthorNames[0] %> and <%= allAuthorNames[1] %>
		<% } else { 
			for (int authNum = 1; authNum <= numAuthors; authNum++) { %>
				<%= allAuthorNames[authNum - 1] 
						+ (authNum == numAuthors ? ""
							: authNum == numAuthors - 1 ? ", and" : ",") %>
			<% } // for each author
		} // if numAuthors %>
	</td></tr><tr>
	<td class="boldtext big" style="text-align:left; padding-bottom:10px;">
		<%= user.translate("Chapter ***1***", chapNum) %>:
		<%= Utils.toDisplay(chapter.getName()) %>
	</td>
	</tr>
<% if (numContents > 0) { 
	int numACEQuestions = 0; 
	contentNum = 0;
	int figNum = 0;
	for (final ArrayList<TextContent> groupContents : arrangedContents) { 
		final boolean newRow = groupContents.get(0).isText()
				|| groupContents.get(0).isACEQuestion();
		if (newRow) { 
			if (contentNum > 0) { %>
				</tr>
			<% } // if ending a row %>
			<tr class="whiterow" style="padding-top:5px; border-collapse:collapse;">
		<% } // if starting new row %>
		<td style="vertical-align:top;<%= newRow ? "" : " padding-right:20px;" %>">
			<table <%= newRow ? "" : "align=\"center\"" %> 
					style="width:100%;" summary="contentRow">
			<% for (final TextContent content : groupContents) {
				final String data = content.getFormattedContent(status,
						pathToRoot, 400, 300);
				final String caption = content.getCaption(); 
				contentNum++; %>
				<tr><td style="width:100%;">
				<a name="content<%= contentNum %>"></a>
				<% if (content.isText()) { %>
					<%= data %>
				<% } else if (content.isMarvin()) { 
					final int viewOpts = MathUtils.parseInt(
								content.getExtraData());
					final int[] dims = content.getAppletSize();
					final String contentIdStr = Utils.toString(
							"content", contentNum);
				%>
					<table style="width:100%;" summary="marvinView">
					<tr><td>
					<table style="margin-left:auto; margin-right:auto;">
					<% if (!"print".equals(status)) { %>
						<tr><td id="launchMViewCell<%= contentNum %>" 
								class="boldtext" style="text-align:right; 
									padding-left:10px; font-style:italic;">
							<a onclick="launchMView_<%= contentNum %>();"><u>Launch 
								MarvinJS&trade; viewer</u></a>
						</td><td>
							<script type="text/javascript">
								// <!-- >
								function getMolForMView_<%= contentNum %>() {
									return '<%= Utils.toValidJS(data) %>';
								} // getMolForMView_<%= contentNum %>()

								function launchMView_<%= contentNum %>() {
									var url = new String.builder().
											append('<%= pathToRoot 
												%>includes\/marvinJSViewer.jsp' +
												'?viewOpts=<%= viewOpts %>&getMolMethodName=').
											append(encodeURIComponent(
												'getMolForMView_<%= contentNum %>()')).
											toString();
									openSketcherWindow(url);
									/* startMViewWebStart(
											'<%= Utils.toValidJS(data) %>', 
											'<%= pathToRoot %>', 
											'<%= user.getUserId() %>'); /**/
								} // launchMView_<%= contentNum %>()
								// -->
							</script>
						</td></tr>
					<% } // if not printable %>
					<tr><td class="whiteTable" id="<%= contentIdStr %>" 
							style="text-align:center;">
						<%= content.getImage(pathToRoot, user.prefersPNG(),
								contentIdStr) %>
					</td></tr>
					</table>
					</td></tr>
					</table>
				<% } else if (content.isLewis()) { %>
					<table style="width:100%;" summary="marvinView">
					<tr><td>
						<table style="margin-left:auto; margin-right:auto;">
						<tr><td class="whiteTable" id="content<%= contentNum %>" 
								style="text-align:center;">
							<span id="fig<%= contentNum %>">
							<input type="hidden" id="figData<%= contentNum %>"
									value="<%= Utils.toValidHTMLAttributeValue(data) %>" />
							</span>
						</td></tr>
						</table>
					</td></tr>
					</table>
				<% } else if (content.isJmol()) {
					jmolNum++;
					final String[] jmolCmds = content.getJmolScripts(); 
					final String jmolScripts = jmolCmds[TextContent.JMOL_SCRIPTS];
					final String jmolJSCmds = jmolCmds[TextContent.JMOL_JS_CMDS]; %>
					<script type="text/javascript">
						// <!-- 
						setJmol(<%= jmolNum %>, 
								'<%= Utils.toValidJS(data) %>',
								'#ffffff', 250, 250,
								'<%= Utils.toValidJS(jmolScripts) %>');
						<%= jmolJSCmds %><%= Utils.isEmpty(jmolJSCmds) 
								|| jmolJSCmds.endsWith(";") ? "" : ";" %>
						// -->
					</script>
				<% } else if (content.isImage() || content.isImageURL()) { 
					/* Utils.alwaysPrint("writeChapter.jsp: content number ",
							contentNum, " is image; data = ", data); /**/ %>
					<table style="width:100%;" summary="image">
					<tr><td class="boldtext" id="enlargeCell<%= contentNum %>"
							style="width:100%; text-align:right; padding-bottom:5px;">
						<%= user.translate("Click image to enlarge") %>
					</td></tr>
					<tr><td style="text-align:center; width:100%; 
							margin-left:auto; margin-right:auto;">
						<a href="javascript:enlargeImage('<%= data %>')">
						<img class="whiteTable" src="<%= data %>" alt="picture"
								style="visibility:hidden;"
								onload="prepareImage(this, 'enlargeCell<%= 
										contentNum %>');" 
								onmouseover="this.style.cursor='pointer'" /></a>
					</td></tr>
					</table>
				<% } else if (content.isMovie()) { 
					/* Utils.alwaysPrint("readChapterHTML.jsp.h: content number ",
							contentNum, " is movie; data = ", data); /**/ %>
					<table class="whiteTable" 
							style="margin-left:auto; margin-right:auto;" summary="movie">
					<tr><td>
						<%= data %>
					</td></tr>
					</table>
				<% } else if (content.isACEQuestion()) { %>
					<table summary="question">
					<tr><td>
					<% if ("print".equals(status)) {
						final int qId = MathUtils.parseInt(data);
						final String ownerId = book.getOwnerId();
						final HWSession hwsession = 
								new HWSession(ownerId, qId, user);
						final Question oneQ = hwsession.getCurrentQuestion();
						final boolean isChoice = oneQ.isChoice();
						final boolean isChooseExplain = oneQ.isChooseExplain();
						final boolean isFillBlank = oneQ.isFillBlank();
						final boolean isRank = oneQ.isRank();
						final boolean usesSubstns = oneQ.usesSubstns();
						final QDatum[] qData = (isRank || isChoice || isFillBlank 
								|| isChooseExplain ? oneQ.getQData(Question.GENERAL) : null);
						final Figure[] figures = hwsession.getCurrentFigures();
					%>
						<table class="whiteTable" style="margin-left:10px; 
								border-collapse:collapse; text-align:left;">
						<tr><td class="boldtext" style="padding-left:10px; padding-top:10px; 
								padding-right:10px; padding-bottom:10px;">
							<%= user.translate("Practice question ***1***.***1***", 
									new int[] {chapNum, ++numACEQuestions}) %>
						</td><td>
						</td></tr>
						<tr><td class="regtext" style="padding-left:10px; padding-top:10px; 
								padding-right:10px; padding-bottom:10px; vertical-align:top;">
							<span class="regtext">
							<%= oneQ.getDisplayStatement() %>
							</span>
							<% final int numFigs = oneQ.getNumFigures();
							boolean haveImage = false;
							for (int qFigNum = 1; qFigNum <= numFigs; qFigNum++) {
								if (figures[qFigNum - 1].isImage()) {
									haveImage = true;
									break;
								} // if have an image
							} // for each figure 
							if (haveImage) { // start new row 
					%>
								<td>&nbsp;</td></tr>
								<tr>
					<% 		} // if the figure is an image %>
						<td style="vertical-align:top; padding-right:10px;
								padding-top:10px; padding-bottom:10px;">
					<% 		if (numFigs > 0) {
					%>
									<table style="width:100%;">
									<tr><td class="regtext">
										<table class="regtext" summary="outer">
					<% 			for (int qFigNum = 1; qFigNum <= numFigs; qFigNum++) { 
									final Figure figure = figures[qFigNum - 1]; 
									final boolean subRGroups = usesSubstns 
											&& !oneQ.isNumeric() && qFigNum == 1
											&& !figure.isImage();
									final String[] figData = (subRGroups
											? figure.getDisplayData(hwsession.getCurrentRGroupMols())
											: figure.isSynthesis() ? figure.getDisplayData(
													Synthesis.getRxnsDisplayPhrases(user))
											: figure.getDisplayData());
									/* Utils.alwaysPrint("printChapter.jsp: Q", qId, 
									 		", figure ", qFigNum, 
											", subRGroups = ", subRGroups, 
											", figData[Figure.STRUCT] =\n", 
											figData[Figure.STRUCT]); /**/
									// begin display
									final boolean showNumFigs = numFigs > 1;
									final boolean useMView = 
											!figure.isJmol() && !figure.isImage();
									if (showNumFigs || useMView) {
					%> 
										<tr><td style="width:100%;">
											<table class="boldtext" style="width:100%;" 
													summary="inner">
											<tr><td style="width:100%;">
					<%						if (showNumFigs) { 
												if (figure.isImage()) {
					%>
													</td><td>&nbsp;
					<%							} // if is image 
					%>
												</td></tr><tr><td class="boldtext" 
														style="padding-left:10px;">
												<%= user.translate(
														"Fig. ***1*** of ***2***", 
														new int[] {qFigNum, numFigs}) %>
												</td><td>&nbsp;</td>
					<%						} // if showNumFigs
					%>
											</td>
					<%						if (useMView) {
					%>
											<td style="text-align:right; font-style:italic;">
											</td>
					<%						} // if useMView
					%>
											</tr></table>
										</td></tr>
					<%				} // if there's text above the figure
						 			if (figure.isJmol()) { 
										jmolNum++;
					%>
										<tr><td class="whiteTable">
										<script type="text/javascript">
											// <!-- 
											setJmol(<%= jmolNum %>, 
													'<%= Utils.toValidJS(
														figData[Figure.STRUCT]) %>',
													'white', 250, 250,
													'<%= Utils.toValidJS(
														figData[Figure.JMOL_SCRIPTS]) %>');
											// -->
										</script>
										</td></tr>
					<%				} else if (!figure.isImage()) { 
										final String figIdStr = Utils.toString(
												"fig", qId, '_', qFigNum);
										if (figure.isReaction()) {
					%>
											<tr><td>
											<table class="whiteTable"
													style="border-collapse:collapse; 
														margin-left:0px; margin-right:0px;">
												<tr><td id="<%= figIdStr %>" 
														style="background-color:#FFFFFF;">
					<%					} else {
					%>
											<tr><td id="<%= figIdStr %>" class="whiteTable">
					<%					} // if figure type
					%>
										<%= figure.getImage(pathToRoot, user.prefersPNG(), 
												oneQ.getQFlags(), figIdStr) %>
					<%					if (figure.isReaction()) {
											final String above = Utils.toDisplay(
													figData[Figure.RXN_ABOVE]);
											final String below = Utils.toDisplay(
													figData[Figure.RXN_BELOW]);
											final int arrowSize = 36; %>
											</td><td style="width:85px; vertical-align:middle;
													background-color:#FFFFFF; 
													text-align:center;">
												<%@ include file="/includes/reactionArrow.jsp.h" %>
											</td>
											</tr>
										 	</table> <!-- table A ends -->
					<%					} else if (figure.isSynthesis()) {
					%>
											<%= figData[Synthesis.RXNID] %>
					<%					} // if synthesis figure
					%>
										</td></tr> <!-- tr B ends -->
					<%				} else { // an image
					%>
										<tr><td> &nbsp;</td><td>&nbsp;</td></tr>
										<tr><td style="vertical-align:top; padding-right:10px;
												padding-bottom:20px; text-align:center; 
												border-collapse:collapse;" colspan="2">
											<img id="image<%= qId %>.<%= qFigNum %>"
													class="whiteTable"
													src="<%= pathToRoot %><%= figure.bufferedImage %>"
													onload="fixTheImageSize(this, 800);"
													alt="picture" />
										</td></tr>
										<tr><td style="vertical-align:top; 
												border-collapse:collapse; padding-right:10px;">
										</td></tr>
					<%				} // if figure type 
						 		} // for each figure qFigNum  
					%>
									</table>
								</td></tr></table>
					<%		} // if there's at least one figure  

							// display QData
							if ((isRank || isChoice) 
									&& oneQ.getNumQData(Question.GENERAL) > 0) {
								final EvalResult evalResult = 
										hwsession.initializeStudentView();
								final String lastResp = evalResult.lastResponse;
								final String delimiter = (isChoice 
										? Choice.SEPARATOR : Rank.MAJOR_SEP);
								int[] optIndices = null;
								if (isChoice) {
									final Choice choiceResp = new Choice(lastResp);
									optIndices = choiceResp.getAllOptions(); // 1-based
								} else {
									final Rank rankResp = new Rank(lastResp);
									optIndices = rankResp.getAllItems(); // 1-based
								} // choice or rank
								final boolean chemFormatting = oneQ.chemFormatting();
					%>
								<table style="padding-left:10px;">
					<%				for (int qdNum = 0; qdNum < qData.length; qdNum++) { 
										final int optIndex = (qdNum < optIndices.length
												? optIndices[qdNum] - 1 : qdNum);
										if (optIndex < qData.length) {
											final QDatum qDatum = qData[optIndex];
											final String outStr = Utils.group(
													Utils.toString("<b>", 
														user.translate(isChoice 
															? "Option" : "Item"), ' ', 
														qdNum + 1, ".</b>"));
											if (qDatum.isText()) { 
					%>
												<tr><td class="regtext" style="vertical-align:top;">
													<%= outStr %>
													<%= qDatum.toShortDisplay(
															chemFormatting, user) %>
												</td></tr>
					<%						} else { 
												final String qdIdStr = Utils.toString(
														"qd", qId, '_', qdNum);
					%>
												<tr><td class="regtext" style="vertical-align:top;">
													<%= outStr %>
												</td></tr>
												<tr><td id="<%= qdIdStr %>" 
														class="whiteTable"
														style="padding-left:20px;">
													<%= qDatum.getImage(pathToRoot,
															user.prefersPNG(), qdIdStr) %>
													<br/>
												</td></tr>
					<%		 				} // if Marvin
										} // optIndex < qData.length 
						 			} // for each Qdatum optIndex 
					%>
								</table>
					<%		} // if Qtype is choice or rank %>
						</td>
					<%		if (haveImage) { %>
								<td>&nbsp;</td>
					<%		} // if haveImage %>
						</tr>
						</table>
					<% } else { %>
						<%= makeButton(user.translate("Practice question ***1***.***1***", 
								new int[] {chapNum, ++numACEQuestions}), 
								"startQ(", data, ");") %>
					<% } // if printable %>
					</td></tr>
					</table>
				<% } // if content type %>
				</td></tr>
				<% if (!content.isText() && 
						(!content.isACEQuestion() || !Utils.isEmpty(caption))) { %>
					<tr><td class="boldtext">
						<% if (!content.isACEQuestion()) { %>
							Figure <%= chapNum %>.<%= ++figNum %>. 
						<% } // if not an ACE question %>
						<%= Utils.toDisplay(caption) %>
					</td></tr>
				<% } // if content type 
			} // for each content in the group %>
			</table>
		</td>
	<% } // for each content %>
	</tr>
	</table>
<% } // if there are contents %>
<!-- 
vim:filetype=jsp 
-->
