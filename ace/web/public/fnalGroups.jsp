<%@ page language="java" %>
<%@ page import="
	com.epoch.AppConfig,
	com.epoch.chem.FnalGroupDef,
	com.epoch.evals.impl.chemEvals.FnalGroup,
	com.epoch.qBank.Question,
	com.epoch.responses.Response,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	final FnalGroupDef[] sortedGroups = FnalGroupDef.getAllGroups();
	final int grpCnt = sortedGroups.length;

	final boolean[] match = new boolean[grpCnt];
	
	final String molstruct = request.getParameter("molstruct");	
	// if (molstruct != null) Utils.alwaysPrint("fnalGroups.jsp: creating response.");
	final Response resp = (molstruct == null ? null
			: new Response(Question.MARVIN, molstruct));
	for (int grp = 0; grp < grpCnt; grp++) {	
		final int grpId = sortedGroups[grp].groupId;
		if (molstruct != null) {
			final String grpStr = Utils.toString(grpId, "/N=/0");
			final FnalGroup grpEval = new FnalGroup(grpStr);
			match[grp] = grpEval.isResponseMatching(resp, null).isSatisfied;
		}
	} 
	final String outStyle = "padding-left:10px; padding-right:10px;"; 
	final String APPLET_NAME = "responseApplet";
	
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>Functional-group finder</title>
<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" type="text/css"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>

<!-- place values so updatePage() can find them
	follows ACE conventions: four-@-delimited strings.
	<% for (int grp = 0; grp < grpCnt; grp++) { %>
		match<%= grp %>Value = @@@@<%= match[grp] %>@@@@
	<% } %>	
-->

<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>nosession/marvinJS/js/promise.min.js"
		type="text/javascript"></script>
<script src="https://marvinjs.chemicalize.com/v1/<%= AppConfig.marvinJSLicense %>/client-settings.js"></script>
<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
<script type="text/javascript">
	// <!-- >

	<%@ include file="../js/marvinQuestionConstants.jsp.h" %>

	var groupDef = [];
	<% for (int grpNum = 0; grpNum < grpCnt; grpNum++) { %>
		groupDef.push('<%= Utils.toValidJS(sortedGroups[grpNum].definition) %>');
	<% } // for each fnal group %>

	function loadSelections() { ; }

	function callServer() {
		marvinSketcherInstances['<%= APPLET_NAME %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(mol) {
			var atomString = '';
			clearInnerHTML('resultTable');
			setValue('submit', ' Processing ... ');
			disableCell('submit');
			var url = 'fnalGroups.jsp';
			var toSend = 'molstruct=' + encodeURIComponent(mol);
			callAJAX(url, toSend);
		}, function(error) {
			alert('Molecule export failed:' + error);	
		});
	} // callServer

	function updatePage() {
		if (xmlHttp.readyState === 4) { // ready to continue
			var response = xmlHttp.responseText;
			// the response is the entire web page
			var out = new String.builder().append('<table border><tr>');
			var th = '<th class="regtext" style="<%= outStyle %> text-align:center;">';
			var tdcent = th.replace(/th/g, 'td');
			var tdleft = tdcent.replace(/center/, 'left');
			out.append(th).append('No.<\/th>').append(th).
					append('Category<\/th>').append(th).
					append('Functional group<\/th>').append(th).
					append('ID number<\/th>').append(th).
					append('Present?<\/th><\/tr>');
			<% for (int grp = 0; grp < grpCnt; grp++) { %>
				var match<%= grp %>Value = extractField(response, "match<%= grp %>Value");
				out.append('<tr bgcolor="').
						append(match<%= grp %>Value === 'true' 
							? '9dff98' : '#c0c0c0').
						append('">').append(tdcent).
						append('<%= grp + 1 %><\/td>').append(tdleft).
						append('<%= sortedGroups[grp].category %><\/td>').
						append(tdleft).
						append('<a href="javascript:alert(groupDef[<%= grp %>]);">').
						append('<%= Utils.toValidJS(
							sortedGroups[grp].getDisplayName()) %><\/a><\/td>').
						append(tdcent).
						append('<%= sortedGroups[grp].groupId %><\/td>').
						append(tdcent).
						append(match<%= grp %>Value === 'true' 
							? '<b>YES<\/b>' : 'no').
						append('<\/td><\/tr>');
			<% } // for grp %>
			out.append('<\/table>');
			setInnerHTML('resultTable', out.toString());
			enableCell('submit');
			setValue('submit', ' Check for functional groups ');
		} // ready to continue
	} // updatePage

	// -->
</script>
</head>

<body style="overflow:auto;">
<form name="molform" action="fnalGroups.jsp" method="post">
<input type="hidden" name="molstruct"/>

<table style="width:600px; margin-left:auto; margin-right:auto;">
<tr><td class="boldtext big" style="padding-bottom:12px; text-align:center;">
ACE functional group finder
</td></tr>
<tr><td class="regtext" style="padding-bottom:12px;">
	<p>Draw a compound, and press the button to find which functional
	groups it contains.
</td></tr>

<tr><td style="text-align:center;">
	<div id="<%= APPLET_NAME %>" style="text-align:center;">
	<script type="text/javascript">
		// <!-- >
		startMarvinJS('<%= molstruct != null ? Utils.toValidJS(molstruct) : "" %>', 
				MARVIN, 0, '<%= APPLET_NAME %>', '<%= pathToRoot %>'); 
		// -->
	</script>
	</div>
</td></tr>
<tr><td style="text-align:center;">
	&nbsp;
</td></tr>
<tr><td style="text-align:center;">
	<input type="button" name="submit" id="submit" 
			value=" Check for functional groups " onclick="callServer();" />
</td></tr>

<tr><td style="text-align:center;">
	&nbsp;
</td></tr>

</table>

<table style="margin-left:auto; margin-right:auto;">
<tr><td id="resultTable" style="text-align:center;">
</td></tr>
</table>

<table style="width:600px; margin-left:auto; margin-right:auto;">
<tr>
<td class="regtext" >&nbsp;
<p><a href="welcome.html">Back to public ACE pages</a>.
</td>
</tr>
</table>
</form>

</body>
</html>
