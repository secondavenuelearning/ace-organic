<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.AppConfig,
	com.epoch.db.ResponseWrite,
	com.epoch.utils.DisplayRules,
	com.epoch.utils.Utils,
	com.epoch.xmlparser.XMLUtils,
	org.apache.commons.lang.StringEscapeUtils,
	java.util.List"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String enteredString = Utils.inputToCERs(request.getParameter("enteredString"));
	final boolean entered = enteredString != null;
	// Utils.alwaysPrint("toDisplay.jsp: enteredString = ", enteredString);
	final List<String[]> displayRules = DisplayRules.getAllDisplayRules();
	final String limMult = "/2";
	final String[] limMults = limMult.split("/");
	final String toDisp = Utils.toDisplay(enteredString);

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<title>ACE toDisplay() tester</title>
<link rel="stylesheet" href="../includes/epoch.css" type="text/css"/>
<script src="../js/jslib.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- >
	function toDisplay() {
		toAlert('Displayed in an alert:\n' 
				+ document.tester.enteredString.value);
		document.tester.submit();
	}
	// -->
</script>
</head>

<body style="overflow:auto;">

<br/>

<!-- self submit form -->
<form name="tester" action="toDisplay.jsp" method="post" accept-charset="UTF-8">
<P class="boldtext big" align=center>
ACE toDisplay() tester
</P>
<table>
<tr><td class="regtext"  colspan=2>
	<% if (entered) { %>
		You entered: 
		<blockquote>
			<%= Utils.toValidTextbox(enteredString) %>
		</blockquote>
		<!--
		It will be displayed in ACE as:
		<blockquote><%= toDisp %></blockquote>
		-->
		<P><table class="whiteTable" style="width:60%;">
			<tr><th class="regtext" style="width:40%;">Method</th>
				<th class="regtext">Returns</th></tr>
			<tr><td class="regtext">[none]</td>
				<td class="regtext">
					<%= enteredString %></td></tr>
			<tr><td class="regtext">toDisplay()</td>
				<td class="regtext">
					<%= toDisp %></td></tr>
			<tr><td class="regtext">toDisplay() with R groups</td>
				<td class="regtext">
					<%= Utils.toDisplay(enteredString, 
							Utils.SUPERSCRIPT_RGROUP_NUMS) %></td></tr>
			<tr><td class="regtext">chopDisplayStr(toDisplay(), 25)</td>
				<td class="regtext">
					<%= Utils.chopDisplayStr(toDisp, 25) %></td></tr>
			<tr><td class="regtext">toPopupMenuDisplay()</td>
				<td class="regtext">
					<select>
					<option><%= Utils.toPopupMenuDisplay(enteredString) %>
					</option>
					</select></td></tr>
			<tr><td class="regtext">toValidJS()</td>
				<td class="regtext">
					<%= Utils.toValidHTML(Utils.toValidJS(enteredString)) %></td></tr>
			<tr><td class="regtext">toValidFileName()</td>
				<td class="regtext">
					<%= Utils.toValidHTML(Utils.toValidFileName(enteredString)) %></td></tr>
			<tr><td class="regtext">toValidXML()</td>
				<td class="regtext">
					<%= Utils.toValidHTML(XMLUtils.toValidXML(enteredString)) %></td></tr>
			<tr><td class="regtext">toValidTextbox()</td>
				<td class="regtext">
					<%= Utils.toValidHTML(Utils.toValidTextbox(enteredString)) %></td></tr>
			<tr><td class="regtext">toValidHTML()</td>
				<td class="regtext">
					<%= Utils.toValidHTML(Utils.toValidHTML(enteredString)) %></td></tr>
			<tr><td class="regtext">toValidHTMLAttributeValue()</td>
				<td class="regtext">
					<%= Utils.toValidHTMLAttributeValue(Utils.toValidHTMLAttributeValue(enteredString)) %></td></tr>
			<tr><td class="regtext">makeSortName()</td>
				<td class="regtext">
					<%= Utils.toValidHTML(Utils.makeSortName(enteredString)) %></td></tr>
			<tr><td class="regtext">hashCode()</td>
				<td class="regtext">
					<%= enteredString.hashCode() %></td></tr>
		</table>
		<P>Try again, if you like.
	<% } else { %>
		Enter a chemical formula or technical term and see how ACE converts it for display.  
	<% } %>
</td></tr>
<tr><td>
	&nbsp;
</td></tr>

<tr><td>
	<input type="text" size="100" name="enteredString"/>
</td></tr>
<tr><td>
	<br/>
	<input type="button" value=" Submit " onclick="toDisplay()"/>
	<br/>
</td></tr>

<tr><td>
	&nbsp;
</td></tr>

<tr><td class="regtext">
<table class="regtext" align=center style="vertical-align:middle;">
<tr><th colspan=3><b>The Rules</b></th></tr>
<tr style="padding-top:5px; height:20px; vertical-align:middle;">
<td>(CH3CO2)4Rh2</td>
<td>becomes</td>
<td><%= Utils.toDisplay("(CH3CO2)4Rh2") %></td>
</tr>
<tr style="padding-top:5px; height:20px; vertical-align:middle;">
<td>(R1)2NCOR2</td>
<td>becomes</td>
<td><%= Utils.toDisplay("(R1)2NCOR2", true) %></td>
</tr>
<tr style="padding-top:5px; height:20px; vertical-align:middle;">
<td colspan="3">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;when a question uses 
R groups; otherwise, it becomes
<%= Utils.toDisplay("(R1)2NCOR2") %></td>
</tr>
<tr style="padding-top:5px; height:20px; vertical-align:middle;">
<td>NH4^+ ^-OH</td>
<td>becomes</td>
<td><%= Utils.toDisplay("NH4^+ ^-OH") %></td>
</tr>
<tr style="padding-top:5px; height:20px; vertical-align:middle;">
<td>(20 cm^^-1)</td>
<td>becomes</td>
<td><%= Utils.toDisplay("(20 cm^^-1)") %></td>
</tr>
<tr style="padding-top:5px; height:20px; vertical-align:middle;">
<td>Bu3Sn^.</td>
<td>becomes</td>
<td><%= Utils.toDisplay("Bu3Sn^.") %></td>
</tr>
<tr style="padding-top:5px; height:20px; vertical-align:middle;">
<td>SN1, SN2, and SRN1</td>
<td style="padding-right:20px;">become</td>
<td><%= Utils.toDisplay("SN1, SN2, and SRN1") %></td>
</tr>
<tr style="padding-top:5px; height:20px; vertical-align:middle;">
<td>but E1 and E2</td>
<td style="padding-right:20px;">remain</td>
<td><%= Utils.toDisplay("E1 and E2") %></td>
</tr>
<tr style="padding-top:5px; height:20px; vertical-align:middle;">
<td>pKa</td>
<td>becomes</td>
<td><%= Utils.toDisplay("pKa") %></td>
</tr>
<tr style="padding-top:5px; height:20px; vertical-align:middle;">
<td>13C</td>
<td>becomes</td>
<td><%= Utils.toDisplay("13C") %></td>
</tr>
<tr style="padding-top:5px; height:20px; vertical-align:middle;">
<td>1H NMR</td>
<td>becomes</td>
<td><%= Utils.toDisplay("1H NMR") %></td>
</tr>
<tr style="padding-top:5px; height:20px; vertical-align:middle;">
<td>but 1H not followed by NMR</td>
<td>remains</td>
<td><%= Utils.toDisplay("1H not followed by NMR") %></td>
</tr>
<tr style="padding-top:5px; height:20px; vertical-align:middle;">
<td>although ^1H, ^2H, and ^56Fe</td>
<td>become</td>
<td><%= Utils.toDisplay("^1H, ^2H, and ^56Fe") %></td>
</tr>
<tr style="padding-top:5px; height:20px; vertical-align:middle;">
<td>Diels-Alder and Friedel-Crafts</td>
<td style="padding-right:20px;">become</td>
<td><%= Utils.toDisplay("Diels-Alder and Friedel-Crafts") %></td>
</tr>
<tr style="padding-top:5px; height:20px; vertical-align:middle;">
<td>C-O and Si-H bonds</td>
<td style="padding-right:20px;">become</td>
<td><%= Utils.toDisplay("C-O and Si-H bonds") %></td>
</tr>
<tr style="padding-top:5px; height:20px; vertical-align:middle;">
<td>C#tripleC</td>
<td>becomes</td>
<td><%= Utils.toDisplay("C#tripleC") %></td>
</tr>
<tr style="padding-top:5px; height:20px; vertical-align:middle;">
<td>#a, #b, #gamma, #delta, <br/>#Delta, #pi, and #sigma</td>
<td>become</td>
<td><%= Utils.toDisplay("#a, #b, #gamma, #delta, <br/>#Delta, #pi, and #sigma") %></td>
</tr>
<tr style="padding-top:5px; height:20px; vertical-align:middle;">
<td style="padding-left:20px;" colspan=3>[Use &amp;name; or &amp;Name; for other Greek letters]</td>
</tr>
<tr style="padding-top:5px; height:20px; vertical-align:middle;">
<td>#light and #heat</td>
<td>become</td>
<td><%= Utils.toDisplay("#light and #heat") %></td>
</tr>
<tr style="padding-top:5px; height:20px; vertical-align:middle;">
<td>&amp;deg;, &amp;rarr;, and &amp;harr;</td>
<td>become</td>
<td>&deg;, &rarr;, and &harr;</td>
</tr>
<tr style="padding-top:20px; height:20px; vertical-align:middle;">
<td style="padding-left:20px;" colspan=3>Visit 
	<a href="http://www.alanwood.net/demos/ent4_frame.html">this 
	page</a> to see other characters that can be displayed with this method.</td>
</tr>
<tr style="height:20px; vertical-align:middle;">
<td style="padding-top:30px;" colspan=3>Standard HTML tags such as 
	&lt;sup&gt;, &lt;sub&gt;, &lt;p&gt;, and &lt;i&gt; may also be used.</td>
</tr>
</table>

</td>
</tr>

<tr>
<td align="left" class="regtext"  colspan=2>
<br/><br/>For those who want all the gory details, toDisplay() runs replaceAll(x, y)
on the submitted string with these values for x and y in this order:
</td>
</tr>
<tr><td class="regtext">
<br/>
<table class="regtext" align=center style="vertical-align:middle; width:400px;">
<tr>
<th style="text-align:left;">Regular expression</th>
<th style="text-align:left; padding-left:10px;">Replacement string</th>
</tr>
<% for (int i = 0; i < displayRules.size(); i++) { 
	final String[] rule = displayRules.get(i); 
	%>
	<tr>
	<td><%= Utils.toValidHTML(rule[0]) 
			+ (rule[0].equals("R(\\d+)") ?
				"<span style=\"color:green; padding-left:20px;\">"
				+ " [applied to R-group questions only]</span>" : "") %></td>	
	<td style="padding-left:10px;"><%= Utils.toValidHTML(rule[1]) %></td>	
	</tr>
<% } %>
</table>
</td>
</tr>
<tr>
<td align="left" class="regtext">
<br/><br/><a href="welcome.html">Back</a> to public ACE pages.
</td>
</tr>

</table>
</form>

</body>
</html>
