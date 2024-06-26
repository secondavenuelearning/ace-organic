<%@ page language="java" %>
<%@ page import="
	com.epoch.synthesis.RxnCondition,
	com.epoch.synthesis.Synthesis,
	com.epoch.utils.Utils,
	java.util.ArrayList,
	java.util.Arrays,
	java.util.List,
	java.util.Map"
%>
<%
	// user and pathToRoot are defined by pages that include this one

	response.setHeader("Cache-Control","no-cache, must-revalidate"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

	final String rxnNum = request.getParameter("rxnNum");
	final boolean UNIQUE = true;
	// get the unique classifications, alphabetize them
	final String[] uniqueClassificns = RxnCondition.getAllClassifns(UNIQUE);
	Arrays.sort(uniqueClassificns);
	// make Lists of ids for each classification
	final int numUniqueClasses = uniqueClassificns.length;
	final List<Integer>[] ids = new ArrayList[numUniqueClasses]; 
	for (int classNum = 0; classNum < numUniqueClasses; classNum++) {
		ids[classNum] = new ArrayList<Integer>();
	}
	// get parallel arrays of ids and classes
	final int[] allIds = RxnCondition.getAllReactionIds();
	final String[] allClasses = RxnCondition.getAllClassifns(!UNIQUE);
	// get permissible reactions
	final String allowedRxnsStr = request.getParameter("allowedRxnConds");
	final int[] allowedRxns = (allowedRxnsStr == null
			? RxnCondition.getAllReactionIds()
			: Utils.stringToIntArray(allowedRxnsStr.split(Synthesis.RXN_ID_SEP)));
	// Utils.alwaysPrint("chooseRxnConds.jsp.h: allowedRxns = ", allowedRxns);
	// sort ids into class Lists
	final List<String> classes = Arrays.asList(uniqueClassificns);
	for (int idNum = 0; idNum < allIds.length; idNum++) {
		final int rxnId = allIds[idNum];
		if (allowedRxns == null // all reactions allowed
				|| Utils.contains(allowedRxns, rxnId)) { // this reaction is allowed
			final String[] rxnClass = allClasses[idNum].split(",");
			for (int classIdx = 0; classIdx < rxnClass.length; classIdx++) {
				final int classNum = classes.indexOf(rxnClass[classIdx]);
				if (classNum >= 0 && classNum < numUniqueClasses) {
					ids[classNum].add(Integer.valueOf(rxnId));
				} else {
					Utils.alwaysPrint("chooseRxnConds.jsp: "
							+ "couldn't find classification ", 
							rxnClass[classIdx], " of reaction ", allIds[idNum], 
							" in list of unique classifications.");
				} // if class is found
			} // for each reaction class
		} // if reaction is allowed
	} // for each reaction 
	final Integer noRgts = Integer.valueOf(RxnCondition.NO_REAGENTS);
	final Map<Integer, String> reactionNamesByIds =
			RxnCondition.getRxnNamesKeyedByIds();
	String defaultRgt = reactionNamesByIds.get(noRgts);
	if (user != null) defaultRgt = user.translate(defaultRgt);
	reactionNamesByIds.put(noRgts, defaultRgt);
	// alphabetize each class of reactions
	// convert each class of reactions from List to 
	// colon-separated string of id numbers of allowed reactions
	final String[] reactions = new String[numUniqueClasses];
	for (int classNum = 0; classNum < numUniqueClasses; classNum++) {
		if (ids[classNum].size() > 0) { 
			reactions[classNum] = Utils.join(ids[classNum], ":");
			reactions[classNum] = RxnCondition.alphabetize(
					reactions[classNum], reactionNamesByIds, 
					!RxnCondition.DEFAULT_NO_RGTS);
		} 
	} // for each class
	final boolean fromSynthTest = request.getParameter("fromSynthTest") != null;

%>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<title>ACE Reaction Condition Chooser</title>
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css" />
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
	// <!-- > avoid parsing the following as HTML

	var currentClass = -1;
	var names = new Array();
	<% for (int idNum = 0; idNum < allIds.length; idNum++) { %>
		names[<%= allIds[idNum] %>] = '<%= Utils.toValidJS(
				Utils.toDisplay(reactionNamesByIds.get(
						Integer.valueOf(allIds[idNum])))) %>';
	<% } // for each rxn id %>

	function paint(chosenClass) {
		if (currentClass !== -1) {
			clearInnerHTML('class' + currentClass);
		}
		if (chosenClass === currentClass) {
			currentClass = -1;
			return;
		}
		var out = new String.builder().append('<table class="regtext">');
		<% for (int classNum = 0; classNum < numUniqueClasses; classNum++) { 
			if (ids[classNum].size() > 0) {
  				final String[] classIds = reactions[classNum].split(":"); %>
				if (chosenClass === <%= classNum %>) {
					<% for (final String classId : classIds) { 
						final int id = Integer.parseInt(classId); %>
						out.append('<tr>'
									+ '<td style="padding-left:20px; '
									+ 'height:27px; width:100%;">'
									+ '<a onclick="javascript:chooseRxn(<%= 
										id %>);">').
								append(names[<%= id %>]).
								append('</a></td></tr>');
					<% } // for each reaction in this class %>
				} // if this is the class	
		<% 	} // if there are allowed reactions in this class
		} // for each class %>
		out.append('</table>');
		setInnerHTML('class' + chosenClass, out.toString());
		currentClass = chosenClass;
	}

	function chooseRxn(id) {
		// alert('Reaction ' + id); 
		opener.setInnerHTML('reaction<%= rxnNum %>Name', names[id]);
		opener.setValue('reaction<%= rxnNum %>Id', id);
		<% if (fromSynthTest) { %>
			opener.setReactionDef(id); 
		<% } %>
		self.close();
	}
	// --> end HTML comment
</script>	
</head>
<body style="overflow:auto;">
	<table class="regtext" style="width:400px;">
		<tr>
			<td>Click on a reaction classification to see 
			a list of reaction conditions.  Click on the reaction
			condition to put it in your list.  
			<br/><br/></td>
		</tr>
	<% for (int classNum = 0; classNum < numUniqueClasses; classNum++) { 
		final int modClassNum = (classNum == 0 ? numUniqueClasses - 1 : classNum - 1); 
		if (ids[modClassNum].size() > 0) { %>
			<tr><td><b><a onclick="javascript:paint(<%= modClassNum %>);"><%= 
					user != null
					? user.translate(uniqueClassificns[modClassNum]) 
					: uniqueClassificns[modClassNum] 
					%></a></b></td></tr>
			<tr><td id="class<%= modClassNum %>"></td></tr>
		<% } // if there are allowed reactions in this class	
	} // for each class %>
	</table>
</body>
</html>

<!-- vim:filetype=jsp
-->
