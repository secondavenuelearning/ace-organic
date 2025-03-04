<%
	// Include this to display a single figure 
	// Globally accessed object - figure
	// Utils.getRandName() forces browser to get fresh copies of images after editing

	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	
	final boolean isLewisFig = figure.isLewis();
	final String[] figData = figure.getDisplayData();
	final boolean isMolecule = figure.isReaction()
			|| figure.isMarvinOnly()
			|| figure.isMRVText()
			|| figure.isSynthesis()
			|| isLewisFig;
	synchronized (session) {
		session.setAttribute("sourceCode", figData[Figure.STRUCT]);
	} // synchronized
	final boolean isFromMarvinJS = isMolecule && !isLewisFig
			&& ChemUtils.getWhetherFromMarvinJS(figData[Figure.STRUCT]);
%>

<script type="text/javascript">
	// <!-- avoid parsing the following as HTML
	function getDisplayMol() {
		<% if (isLewisFig) { %>
			return getLewisImageMRV(getOrigMol());
		<% } else { %>
			return getOrigMol();
		<% } // if is Lewis figure %>
	} // getDisplayMol()

	function getOrigMol() {
		return '<%= Utils.toValidJS(figData[Figure.STRUCT]) %>';
	}

	function figIsLewis() {
		return <%= isLewisFig %>;
	} 

	function getBestDimensions() {
		<% final int[] dims = (isMolecule 
				? MolString.getBestAppletSize(figData[Figure.STRUCT])
				: new int[] {MolString.OPT_WIDTH, MolString.OPT_WIDTH}); %>
		return [<%= dims[0] %>, <%= dims[1] %>];
	} // getBestDimensions()

	function showSource() {
		openSourceCodeWindow('<%= pathToRoot %>includes/showSourceCode.jsp');
	} // showSource()

	function shownFigureIsMolecule() {
		return <%= isMolecule %>;
	}

	function shownFigureIsFromMarvinJS() {
		return <%= isFromMarvinJS %>;
	}
	// --> end HTML comment
</script>

	<% if (isLewisFig) { %>
		<table style="margin-left:auto; margin-right:auto;" summary="">
		<tr><td class="boldtext" style="text-align:right; white-space:nowrap; font-style:italic;">
			Click image to copy source
		</td></tr>
		<tr><td id="fig" class="whiteTable" style="text-align:center;">
		</td>
		</tr>
		</table>
	<% } else if (isMolecule) { %>
		<table style="margin-left:auto; margin-right:auto;" summary="">
		<tr><td class="boldtext" style="text-align:right; white-space:nowrap; font-style:italic;">
			Click image to copy source
		</td></tr>
		<% if (figure.isSynthesis() || figure.isReaction()) { %>
			<tr><td style="text-align:center;">
				<table class="whiteTable" summary="">
				<tr><td id="fig">
		<% } else { %>
			<tr><td id="fig" class="whiteTable" style="text-align:center;">
		<% } // if synthesis %>
			<a onclick="showSource();"><%= 
					figure.getImage(pathToRoot, user.prefersPNG(), qFlags, "") %></a>
		<% if (figure.isSynthesis()) { %>
			</td>
			</tr>
			<%= figData[Synthesis.RXNID] %>
			</table>
		<% } else if (figure.isReaction()) { 
			final String above = Utils.toDisplay(figData[Figure.RXN_ABOVE]);
			final String below = Utils.toDisplay(figData[Figure.RXN_BELOW]);
			final int arrowSize = 36;
			/* Utils.alwaysPrint("dispFigure.jsp.h: above = ", above,
					", below = ", below); /**/ %>
			</td>
			<td>
				<%@ include file="/includes/reactionArrow.jsp.h" %>
			</td>
			</tr>
			</table>
		<% } // if synthesis or reaction %>
		</td>
		</tr>
		</table>
	<% } else if (figure.isJmol()) { 
		final String jmolJSCmds = figData[Figure.JMOL_JS_CMDS]; %>
		<table style="margin-left:auto; margin-right:auto;" summary="">
		<tr><td class="whiteTable" style="text-align:center;">
			<script type="text/javascript">
				// <!-- avoid parsing the following as HTML
				setJmol(0, getDisplayMol(),
						'white', 250, 250,
						'<%= Utils.toValidJS(figData[Figure.JMOL_SCRIPTS]) %>');
				<%= jmolJSCmds + (Utils.isEmpty(jmolJSCmds) 
						|| jmolJSCmds.endsWith(";") ? "" : ";") %>
				// --> end HTML comment
			</script>
		</td>
		</tr>
		</table>
	<% } else { // image +/- vectors 
		final String srcFile = pathToRoot + figure.bufferedImage; 
		final String color = (!figure.isImageAndVectors() 
				? "" : Utils.isEmpty(qData[GENERAL]) || !isClickableImage || !isDrawVectors 
				? "red" : qData[GENERAL][0].data); %>
		<table style="margin-left:auto; margin-right:auto;" summary="">
		<tr>
		<td class="boldtext" style="text-align:right; white-space:nowrap;">
			<% if (figure.isImageAndVectors()) { %>
				ACE will display vectors on this image.
				Click the image to see them.
			<% } else { %>
				<span id="enlargeCell<%= figureIndex %>">Click image to enlarge.</span>
			<% } // if vector %>
		</td>
		</tr>
		<tr><td style="text-align:center;">
			<a href="javascript:enlargeImage('<%= srcFile %>', '<%= 
					figure.isImageAndVectors() 
						? Utils.toValidHTMLAttributeValue(figData[Figure.COORDS]) 
						: "" %>', '<%= color %>')">
			<img class="whiteTable" src="<%= srcFile
						+ (reloadFigs ? "?reloadFigs=" + Utils.getRandName() : "") %>"
					alt="picture" style="visibility:hidden;"
					onload="prepareImage(this, 'enlargeCell<%= figureIndex %>');"
					onmouseover="this.style.cursor='pointer'" /></a>
		</td>
		</tr>
		</table>
	<% } // if figure.type %>
	<br />
<!-- vim:filetype=jsp:
-->
