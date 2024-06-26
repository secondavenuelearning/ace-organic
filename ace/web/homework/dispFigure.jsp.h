<%
	// Include this to display a single figure 
	// Globally accessed objects: figure, figNum, figure.bufferedImage, usesSubstns

	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	
	final boolean isLewisFig = figure.isLewis();
	final String[] synPhrases = (!figure.isSynthesis() ? null
			: mode == PREVIEW ? Synthesis.getRxnsDisplayPhrases()
			: Synthesis.getRxnsDisplayPhrases(user));
	final String[] figData = (usesSubstns && !isNumeric
			? figure.getDisplayData(hwsession.getCurrentRGroupMols())
			: figure.isSynthesis() ? figure.getDisplayData(synPhrases)
			: figure.getDisplayData());
	final String[] substnStrs = (usesSubstns && !isNumeric
			? hwsession.getCurrentSubstns() : null);
	final boolean mayUseMView = figure.isReaction() 
			|| figure.isSynthesis() || figure.isMarvinOnly();
	synchronized (session) {
		session.setAttribute("sourceCode" + figNum, figData[Figure.STRUCT]);
	} // synchronized
	if (mayUseMView) {
		final int[] dims = (mayUseMView
					&& !figure.isJmol() && !figure.hasImage()
				? MolString.getBestAppletSize(figData[Figure.STRUCT], 
					mappingIsVisible)
				: new int[] {MolString.OPT_WIDTH, MolString.OPT_WIDTH});
		%>
		<script type="text/javascript">
		// <!-- > avoid parsing the following as HTML
		function getMolForMView_<%= figNum %>() {
			return '<%= Utils.toValidJS(figData[Figure.STRUCT]) %>';
		} // getMolForMView_<%= figNum %>()

		function launchMView_<%= figNum %>() {
			var url = new String.builder().
					append('<%= pathToRoot %>includes\/marvinJSViewer.jsp' +
						'?viewOpts=<%= qFlags %>&getMolMethodName=').
					append(encodeURIComponent('getMolForMView_<%= figNum %>()')).
					toString();
			openSketcherWindow(url);
		} // launchMView_<%= figNum %>()

		// --> end HTML comment
		</script>
	<% } // if may use MView %>
	<% if (figure.isJmol()) { %>
		<script type="text/javascript">
		// <!-- > avoid parsing the following as HTML
		function openJmolWindow_<%= figNum %>() {
			// target page name must be exactly the same as in openwindows.js
			var targetPage = '\/ace\/includes\/jmolWindow.jsp';
			var newForm = prepareForm(targetPage, 'Jmol');
			newForm.appendChild(prepareField('data', 
					'<%= Utils.toValidJS(figData[Figure.STRUCT]) %>'));
			newForm.appendChild(prepareField('scripts', 
					'<%= Utils.toValidJS(figData[Figure.JMOL_SCRIPTS]) %>'));
			newForm.appendChild(prepareField('commands', 
					'<%= Utils.toValidJS(figData[Figure.JMOL_JS_CMDS]) %>'));
			document.body.appendChild(newForm); // Firefox
			openJmolWindow(targetPage);
			newForm.submit();
		} // openJmolWindow_<%= figNum %>()
		</script>
	<% } // if Jmol %>

<table class="regtext" style="border-collapse:collapse; border-style:none; border-color:black;
border-width:1px; margin-left:0px; margin-right:0px; background-color:#FFFFFF;
color:#000000;">

<% if (figure.isReaction()) { 
	final int numCols = (figures.length == 1 ? 3 : 2);
	final String above = Utils.toDisplay(figData[Figure.RXN_ABOVE]);
	final String below = Utils.toDisplay(figData[Figure.RXN_BELOW]);
	final int arrowSize = 36;
%>
	<tr style="line-height:0px; border-style:none; background-color:#f6f7ed;">
		<td>&nbsp;</td>
		<td>&nbsp;</td>
		<td>&nbsp;</td>
	</tr>
	<tr style="background-color:#f6f7ed;">
	<% if (figures.length > 1) { %>
		<td class="boldtext" style="text-align:left;">
			<%= ansPhrases.get(FIG) %> <%= figNum %>
		</td>
	<% } %>
	<td id="launchMViewCell<%= figNum %>" class="boldtext" colspan="<%= numCols %>"
			style="text-align:right; padding-left:10px; font-style:italic;">
		<a onclick="launchMView_<%= figNum %>();"><u>Launch MarvinJS&trade; viewer</u></a>
		or click image to copy source
	</td>
	</tr>
	<tr style="background-color:white; border-style:solid; border-width:1px;">
	<td id="fig<%= figNum %>" style="text-align:center;" colspan="2">
		<a onclick="showSource(<%= figNum %>);"><%= 
				figure.getImage(pathToRoot, user.prefersPNG(), qFlags, 
					substnStrs, figNum) %></a>
	</td>
	<td>
		<%@ include file="/includes/reactionArrow.jsp.h" %>
	</td>
	</tr>

<% } else if (figure.isSynthesis() || figure.isMarvinOnly() 
		|| figure.isMRVText() || isLewisFig) { 
	final int numCols = (figures.length == 1 ? 2 : 1);
%>
	<tr style="background-color:#f6f7ed; border-style:none;">
	<% if (figures.length > 1) { %>
		<td class="boldtext" style="text-align:left;">
			<%= ansPhrases.get(FIG) %> <%= figNum %>
		</td>
	<% } %>
	<% if (mayUseMView) { %>
		<td id="launchMViewCell<%= figNum %>" class="boldtext" colspan="<%= numCols %>"
				style="text-align:right; padding-left:10px; font-style:italic;">
			<a onclick="launchMView_<%= figNum %>();"><u>Launch MarvinJS&trade; viewer</u></a>
			or click image to copy source
		</td>
	<% } else { %>
		<td class="boldtext" colspan="<%= numCols %>" 
				style="text-align:right; padding-left:10px; font-style:italic;">
			Click image to copy source
		</td>
	<% } // if may open in Marvin viewer %>
	</tr>
	<tr style="background-color:white; border-style:solid; border-width:1px;">
	<td id="fig<%= figNum %>" colspan="<%= 3 - numCols %>"
			style="text-align:center; border-style:solid; border-width:1px;">
		<% if (!isLewisFig) { %>
			<a onclick="showSource(<%= figNum %>);"><%= 
					figure.getImage(pathToRoot, user.prefersPNG(), qFlags, 
						substnStrs, figNum) %></a>
		<% } // if is not Lewis figure %>
	</td>
	</tr>
	<% if (figure.isSynthesis()) { %>
		<tr><td <%= figures.length > 1 ? "colspan=\"2\"" : "" %>><table summary="">
			<%= figData[Synthesis.RXNID] %>
		</table></td></tr>
	<% } // if is synthesis %>

<% } else if (figure.isJmol()) { 
	jmolNum++;
	final String jmolJSCmds = figData[Figure.JMOL_JS_CMDS]; %>
	<% if (figures.length > 1) { %>
		<tr style="background-color:#f6f7ed; border-style:none;">
		<td class="boldtext" style="text-align:left;">
			<%= ansPhrases.get(FIG) %> <%= figNum %>
		</td>
		</tr>
	<% } %>
	<tr style="background-color:white; border-style:solid; border-width:1px;">
	<td style="text-align:center;">
		<script type="text/javascript">
			// <!-- avoid parsing the following as HTML
			setJmol(<%= jmolNum %>, 
					'<%= Utils.toValidJS(figData[Figure.STRUCT]) %>',
					'white', 250, 250,
					'<%= Utils.toValidJS(figData[Figure.JMOL_SCRIPTS]) %>');
			<%= Utils.toString(jmolJSCmds, Utils.isEmpty(jmolJSCmds) 
					|| jmolJSCmds.endsWith(";") ? "" : ";") %>
			// --> end HTML comment
		</script>
	</td>
	</tr>
	<tr><td>
		<%= makeButton(ansPhrases.get(OPEN_NEW_WINDOW), Utils.toString(
				"openJmolWindow_", figNum, "();")) %>
	</td>
	</tr>

<% } else if (figure.hasImage()) {
	final String srcFile = pathToRoot + figure.bufferedImage; 
	final String color = (!figure.isImageAndVectors() 
			? "" : Utils.isEmpty(qData) || !isClickableImage || !isDrawVectors 
			? "red" : qData[0].data); %>
	<tr style="background-color:#f6f7ed; border-style:none;">
	<% if (figures.length > 1) { %>
		<td class="boldtext" style="text-align:left;">
			<%= ansPhrases.get(FIG) %> <%= figNum %>
		</td>
	<% } %>
	<td class="boldtext" style="text-align:right; padding-left:10px;"
			<%= figures.length == 1 ? "colspan=\"2\"" : "" %> >
		<% if (figure.isImage()) { %>
			<span id="enlargeCell<%= figNum %>"><%= ansPhrases.get(CLICK_ENLARGE) %></span>
		<% } else { %>
			<%= ansPhrases.get(CLICK_ENLARGE_VECS) %>
		<% } // if image %>
	</td>
	</tr>
	<tr style="background-color:white; border-style:solid; border-width:1px;">
	<td style="text-align:center;" <%= figures.length > 1 ? "colspan=\"2\"" : "" %> >
		<a href="javascript:enlargeImage('<%= srcFile %>', '<%= 
				figure.isImage() ? "" 
					: Utils.toValidHTMLAttributeValue(figData[Figure.COORDS])
				%>', '<%= color %>')">
		<img src="<%= srcFile %>" 
				alt="picture" style="visibility:hidden;"
				onload="prepareImage(this, 'enlargeCell<%= figNum %>');" 
				onmouseover="this.style.cursor='pointer'" /></a>
	</td>
	</tr>
<% } // if figure.type %>

</table>
<br/>
<!-- vim:filetype=jsp
-->
