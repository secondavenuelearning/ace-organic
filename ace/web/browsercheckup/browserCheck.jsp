<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.AppConfig, 
	com.epoch.chem.MolString,
	com.epoch.courseware.User,
	com.epoch.utils.Utils"
%>
<%	
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final User tempUser = new User();
	String chosenLang = request.getParameter("language");
	if (chosenLang == null && AppConfig.notEnglish) {
		chosenLang = AppConfig.defaultLanguage;
	}
	tempUser.setLanguage(chosenLang);
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
<meta http-equiv="Pragma" content="no-cache"/>
<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
<title>ACE Organic Browser Checkup</title>
<script src="<%= pathToRoot %>js/ajax.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script type="text/javascript">
	// <!-- >
	function openFeedback(site, width, height) {
		var w = window.open(site, "opop", "width=" + width + ", height=" + height 
				+ ", top=200, left=100, scrollbars=yes, resizable=yes");
		w.focus();
	}

	function openPopup() {
		openFeedback('popuptest.jsp?language='
				+ encodeURIComponent('<%= Utils.toValidJS(chosenLang) %>'), 
				400, 200);
	}

	function openCookie() {
		openFeedback('cookietest.jsp?language='
				+ encodeURIComponent('<%= Utils.toValidJS(chosenLang) %>'), 
				400, 200);
	}

	function launchMView() {
		startMViewWebStart('CCCO', '<%= pathToRoot %>');
	} // launchMView()

	// -->
</script>
</head>

<body class="light" style="overflow:auto;">
<table style="width:100%;">
	<tr>
		<td style="text-align:center;">
			<img src="<%= pathToRoot %>images/acelogo.jpg" alt="logo"/>
		</td>
	</tr>
	<tr>
		<td style="vertical-align:top; text-align:center;">
			<table class="whiteTable" style="width:626px;">
			<tr>
			<td style="vertical-align:top; text-align:left; width:50%; padding-left:40px; 
					padding-top:10px; padding-right:40px; padding-bottom:10px;">
			<span class="boldtext"><%= tempUser.translate("Browser checkup") %><br /></span>
			<ol>
			<li><p>
				<%= tempUser.translate("Pop-up Windows") %><br />
				<br />
				<%= tempUser.translate("Parts of the ACE Organic system "
						+ "(including tests on "
						+ "this check-up page) are opened in new browser windows. "
						+ "If you have a pop-up blocker installed, problems may "
						+ "occur when navigating within the system.") %>
				<br /></p>
				<%= makeButton(tempUser.translate("Test pop-ups"),
						"openPopup();") %>
				<ul>
				<li>
				<p><%= tempUser.translate("If the pop-up test failed, "
						+ "please make sure that you either "
						+ "disable your pop-up blocker or set it to allow pop-ups coming "
						+ "from this domain name.  All pop-ups used in ACE Organic are "
						+ "related directly to the use of the system.") %></p>
				</li>
				</ul>
			</li>
			<li><p>
				<%= tempUser.translate("Cookies") %><br />
				<br />
				<%= tempUser.translate("In order to log in and use "
						+ "ACE Organic, your browser must accept cookies.") %>
				<br /></p>
				<%= makeButton(tempUser.translate("Test cookies"),
						"openCookie();") %>
				<br />
				<ul>
				<li>
				<%= tempUser.translate("If the cookie test failed, please "
						+ "make sure that your browser is set to at least accept "
						+ "first-party cookies.") %>
				<br />
				<br />
					<ul>
					<li>
					<b><%= tempUser.translate("Internet Explorer") %></b>:
						<ol>
						<li><%= tempUser.translate(
							"Go to <i>Tools</i> &rarr; <i>Internet Options</i>.") %></li>
						<li><%= tempUser.translate(
							"Pick the <i>Privacy</i> tab.") %></li>
						<li><%= tempUser.translate(
							"Press the <i>Advanced...</i> button.") %></li>
						<li><%= tempUser.translate(
							"Set it to accept first-party cookies.") %></li>
						</ol>
					</li>
					<li>
					<b><%= tempUser.translate("Firefox") %></b>:
						<ol>
						<li><%= tempUser.translate(
							"Go to <i>Tools</i> &rarr; <i>Options</i> " 
							+ "(<i>Firefox</i> &rarr; <i>Preferences</i> on a Mac).") %></li>
						<li><%= tempUser.translate(
							"Select the <i>Privacy</i> section.") %></li>
						<li><%= tempUser.translate
							("Expand the <i>Cookies</i> section.") %></li>
						<li><%= tempUser.translate(
							"Verify that <i>Allow sites to set cookies</i> is checked.") %></li>
						</ol>
					</li>
					<li>
					<b><%= tempUser.translate("Safari") %></b>:
						<ol>
						<li><%= tempUser.translate(
							"Go to <i>Safari</i> &rarr; <i>Preferences</i>.") %></li>
						<li><%= tempUser.translate(
							"Select the <i>Security</i> section.") %></li>
						<li><%= tempUser.translate(
							"Verify that <i>Accept Cookies</i> is set to either "
							+ "<i>'Always'</i> or <i>'Only from sites you navigate "
							+ "to'</i>.") %></li>
						</ol>
					</li>
					</ul>
				</li>
				</ul>
			</li>
			<p>
			<%= tempUser.translate("If your browser passes all of the tests on this page "
					+ "and you are using one of the supported "
					+ "browsers, you should not experience any problems while using ACE Organic. "
					+ "Use your browser's <b>Back</b> button to return to the login page.") %>
			</p><p>
			<%= tempUser.translate("If, however, your browser does not pass some of the tests, "
					+ "and you have tried all of the steps "
					+ "provided, make sure that any firewall software you are running isn't causing "
					+ "the problem.  Try adjusting the security level or shutting security off "
					+ "completely.") %>
			</p>
			</td>
			</tr>
			</table>
		</td>
	</tr>
	<tr>
		<td style="vertical-align:top; text-align:center;">
			<table class="boldtext" style="width:626px; margin-left:auto; 
			margin-right:auto; border-collapse:collapse; padding:0px;">
			<tr>
			<td style="text-align:left; vertical-align:top;">
				<a href="http://www.nsf.gov" target="window2">
				<img class="whiteTable"
				src="http://www.nsf.gov/images/common/nsf.gif" alt="NSF"/>
				</a>
				<a href="http://www.pearson.com" target="window2">
				<img class="whiteTable" src="<%= pathToRoot %>images/pearson.jpg" alt="Pearson"/>
				</a>
				<a href="http://www.secondavenuesoftware.com" target="window2">
				<img class="whiteTable" src="<%= pathToRoot %>images/sas.jpg" alt="SAS"/>
				</a>
			</td>
			<td style="text-align:right; vertical-align:top;">
				<%= tempUser.translate("Created by ***Robert B. Grossman*** and "
						+ "***Raphael Finkel***", new String[] {
							"<a href=\"http://www.as.uky.edu/academics/"
								+ "departments_programs/Chemistry/Chemistry/"
								+ "FacultyResearch/Faculty/RobertGrossman/Pages/"
								+ "default.aspx\">Robert B. Grossman</a>", 
							"<a href=\"http://www.cs.uky.edu/~raphael/\">"
								+ "Raphael A. Finkel</a>"
							})
				%>
				<br /><%= tempUser.translate("Departments of Chemistry and Computer Science") %>
				<br /><%= tempUser.translate("University of Kentucky") %>
			</td>
			</tr>
			</table>
		</td>
	</tr>
</table>
</body>
</html>
