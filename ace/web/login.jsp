<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	com.epoch.AppConfig, 
	com.epoch.courseware.User,
	com.epoch.servlet.LoginServlet,
	com.epoch.session.AnonSession,
	com.epoch.translations.PhraseTransln,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%	final String pathToRoot = "";
	request.setCharacterEncoding("UTF-8");

	final String url = request.getRequestURL().toString();
	if (url != null && url.matches("http://epoch\\.uky\\.edu:91[\\d]+/.*")) { 
		response.sendRedirect(url.replaceFirst("http:", "https:").replaceFirst("91", "93"));
	} else if (url != null && url.matches("http://epoch\\.uky\\.edu/.*")) {
		response.sendRedirect("https://epoch.uky.edu/ace");
	} // if url should be redirected
	final boolean aceIsDown = false;

	String flag = request.getParameter("flag");
	if (flag == null) flag = ""; 
	final String[] ERR_MSGS = LoginServlet.ERR_MSGS;
	final int errmsgNum = MathUtils.parseInt(request.getParameter("errmsg"), -1);
	final String errmsg = (errmsgNum >= 0 && errmsgNum < ERR_MSGS.length
			? ERR_MSGS[errmsgNum] : null);
	final User tempUser = new User();
	String chosenLang = request.getParameter("language");
	if (chosenLang == null && AppConfig.notEnglish) {
		chosenLang = AppConfig.defaultLanguage;
	}
	tempUser.setLanguage(chosenLang);
	final String[] allLanguages = AnonSession.getAllLanguages();

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Pragma" content="no-cache"/>
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
	<link rel="stylesheet" href="includes/epoch.css" type="text/css"/>
	<link rel="icon" href="images/favicon.ico" type="image/x-icon"/>
	<link rel="shortcut icon" href="images/favicon.ico" type="image/x-icon"/> 
	<title>ACE Organic</title>
	<script src="js/md5.js" type="text/javascript"></script>
	<script src="js/jslib.js" type="text/javascript"></script>
	<script type="text/javascript">
		// <!-- >

		function openAbout() {
			var w = window.open('nosession/about.html',
				'About', 'width=550, height=400, scrollbars=yes');
			w.focus();
		}

		function reloadMe() {
			self.location.href = 'login.jsp?language=' +
				encodeURIComponent(document.languageForm.language.value);
		} // reloadMe()

		function checkBrowser() {
			self.location.href = 'browsercheckup/browserCheck.jsp?language=' +
				encodeURIComponent(document.languageForm.language.value);
		} // checkBrowser()

		function register() {
			self.location.href =
				'nosession/register.jsp?userType=<%= User.STUDENT %>&language=' +
				encodeURIComponent(document.languageForm.language.value);
		} // register()

		function resetPwd() {
			self.location.href = 'nosession/getUsername.jsp?language=' +
				encodeURIComponent(document.languageForm.language.value);
		} // resetPwd()

		function resetUsername() {
			toAlert('<%= tempUser.translateJS("Ask your instructor to retrieve "
					+ "your username for you. If you are an instructor, ask "
					+ "the ACE administrator.") %>');
		} // resetUsername()

		var arImageSrc = new Array (
			'ace_anim.gif',
			'acelogo2.jpg',
			'acelogo.jpg',
			'ace_nonanim.gif',
			'addButtonClick.gif',
			'addButton.gif',
			'addButtonOver.gif',
			'backButtonClick.gif',
			'backButton.gif',
			'backButtonOver.gif',
			'border.jpg',
			'clear.gif',
			'deleteButtonClick.gif',
			'deleteButton.gif',
			'deleteButtonOver.gif',
			'duplicateButtonClick.gif',
			'duplicateButton.gif',
			'duplicateButtonOver.gif',
			'editButtonClick.gif',
			'editButton.gif',
			'editButtonOver.gif',
			'nextButtonClick.gif',
			'nextButton.gif',
			'nextButtonOver.gif',
			'revertButtonClick.gif',
			'revertButton.gif',
			'revertButtonOver.gif',
			'tabClick.jpg',
			'tabOff.jpg',
			'tabOver.jpg',
			'viewButtonClick.gif',
			'viewButton.gif',
			'viewButtonOver.gif'
		)
	
		var arImageList = new Array();
	
		for (var counter = 0; counter < arImageSrc.length; counter++) { // <!-- >
			arImageList[counter] = new Image();
			arImageList[counter].src = 'images/' + arImageSrc[counter];
		}

		function setWarning() {
			var idparam = location.search.substring(1);
			var warning = document.getElementById('warning');
			if (idparam.indexOf('deny=yes') !== -1) {
				warning.innerHTML = '<%= tempUser.translateJS(
						"Invalid username or password!") %>';
			}
			<% if ("Success".equals(flag)) { %>
				warning.innerHTML = '<%= tempUser.translateJS(
						"You have successfully registered.") %>';
			<% } else if ("email".equals(flag)) { %>
				warning.innerHTML = '<%= tempUser.translateJS(
						"Your request has been sent.") %>';
			<% } %>
		} // setWarning()

		function checkSubmit(e) {
			if (e && ([10, 13].contains(e.keyCode))) submitMe();
		} // checkSubmit()

		function submitMe() {
			var userId = document.loginForm.userid.value;
			var passPhrase = document.loginForm.pphrase_entry.value;
			if (isWhiteSpace(passPhrase)) {
				var warning = document.getElementById('warning');
				warning.innerHTML = '<%= tempUser.translateJS(
						"Please enter a password.") %>';
			} else if (<%= !aceIsDown %> 
					|| ['bob', 'raphael', 'admin'].contains(userId)) {
				document.loginForm.submit();
			}
		} // submitMe()

		function checkForNonAsciiUserId() {
		}

		// -->
	</script>
</head>
<body class="light" style="overflow:auto;" onload="checkForNonAsciiUserId();">
	<table style="margin-left:auto; margin-right:auto; width:626px;" summary="">
		<tr>
		<td style="text-align:left;">
			<img src="<%= pathToRoot + pathToImagesFromRoot %>acelogo.jpg" alt="logo"/>
			<% if (aceIsDown) { %>
				<p class="boldtext big" style="color:red;">ACE is down for the moment.  
				We're working as quickly as possible to bring it back up.
				Please check back later.  </p>
			<% } // if ACE is down %>
			<p><b><span class="big">
			<%= AppConfig.defaultInstitution %> development version (ACE DEV-5.1)
			</span></b></p>
			<p><span class="boldtext" style="color:red;">
			NOTE: This version of ACE is undergoing active 
			development.  Find a more stable version  
			<a href="https://epoch.uky.edu/ace">here</a>. 
			</span></p>
			<br/>&nbsp;
		</td>
		</tr>
		<% if (errmsg != null) { 
			Utils.alwaysPrint("login.jsp: errmsgNum = ", 
					errmsgNum, ", errmsg = ", errmsg); %>
			<tr><td style="text-align:center;">
				<table class="whiteTable" summary=""
						style="width:626px; border-color:#FF0000;">
					<tr>
					<td style="text-align:center;"><%= 
						tempUser.translate(errmsg) %></td>
					</tr>
					<tr>
					<td class="boldtext" style="color:red;">
				 		<%= tempUser.translate(
								"If your username contains any "
									+ "non-ASCII characters such as letters "
									+ "with accents or characters from "
									+ "non-Roman writing systems such as "
									+ "Arabic or Chinese, please visit "
									+ "***this page***.",
								new String[] { Utils.toString(
									"<a href=\"nosession/findNonAsciiUserId.jsp;\">",
									tempUser.translate("this page"), 
									"</a>") }) %>
					</td>
					</tr>
				</table>
			</td></tr>
		<% } %>
		<tr>
		<td style="vertical-align:top; text-align:center;">
			<table class="whiteTable" style="width:626px;" summary="">
				<tr>
				<td class="regtext" style="padding-top:10px; padding-left:20px; 
						vertical-align:middle">
					<form name="languageForm" id="languageForm" method="post" action="dummy">
					<%= tempUser.translate("Choose a language") %>:
					<select name="language" id="language" onchange="reloadMe();">
						<option value="English">English</option>
					<% for (final String lang : allLanguages) { %>
						<option value="<%= Utils.toValidHTMLAttributeValue(lang) %>"
								<%= lang.equals(chosenLang) ? "selected=\"selected\"" : "" %>>
							<%= Utils.capitalize(lang) %> </option>
					<% } %>
					</select>
					</form>
				</td>
				</tr>
				<tr>
				<td style="vertical-align:top; text-align:left; width:50%;
						padding-left:20px;">
					<div class="boldtext" style="line-height:30px;">
					<%= tempUser.translate("Registered users login here") %>:
					</div>
					<div id="warning" style="color:red; font-weight:bold;"></div>
					<script type="text/javascript">
						// <!-- >
						setWarning();
						// -->
					</script>
					<form name="loginForm" id="loginForm" method="post"
							action="loginNow.jsp" accept-charset="UTF-8">
						<input type="hidden" name="action" value="login"/> 
						<input type="hidden" name="language" value="<%= 
								Utils.toValidHTMLAttributeValue(chosenLang) %>"/> 
						<table style="margin:0px;" summary="">
							<tr>
							<td class="regtext">
								<%= tempUser.translate("Username") %>:
							</td>
							<td>
								<input type="text" name="userid" size="16"
								value=""/>
							</td>
							</tr>
							<tr>
							<td class="regtext">
								<%= tempUser.translate("Password") %>:
							</td>
							<td>
								<input type="password" name="pphrase_entry"
									onkeypress="checkSubmit(event);"
									value="" size="16" />
							</td>
							</tr>
							<tr>
							<td></td>
							<td>
								<%= makeButton(tempUser.translate("Login"), "submitMe();") %>
							</td>
							</tr>
						</table>
					</form>
				</td>
				<td rowspan="2" style="vertical-align:top; text-align:left; padding-left:20px;
						width:50%; padding-right:40px; padding-bottom:5px;">
					<div class="boldtext" style="line-height:30px;"><%= 
							tempUser.translate("Not registered?") %></div>
					<%= makeButton(tempUser.translate("Register"), "register();") %>
				</td>
				</tr>
				<tr>
				<td style="padding-bottom:5px; vertical-align:bottom;
						text-align:center;">
					<%= tempUser.translate("Forgot ***username*** or ***password***?",
						new String[] {
							Utils.toString("<a href=\"javascript:resetUsername();\">",
								tempUser.translate("username"), "</a>"),
							Utils.toString("<a href=\"javascript:resetPwd();\">",
								tempUser.translate("password"), "</a>")
							}) %>
				</td>
				</tr>
				<tr>
				<td style="padding-bottom:5px; vertical-align:bottom;
						text-align:center;">
					<a href="javascript:checkBrowser();"><%= 
							tempUser.translate("Browser checkup") %></a> |
					<a href="nosession/troubleshooting.html" target="window2"><%= 
							tempUser.translate("Troubleshooting") %></a> <br/> 
					<a href="javascript:openAbout();"><%= 
							tempUser.translate("About") %></a>
				</td>
				</tr>
			</table>
		</td>
		</tr>
		<tr>
		<td style="vertical-align:top; text-align:center;">
			<table class="boldtext" style="width:626px; margin-left:auto;
					margin-right:auto; border-collapse:collapse; padding:0px;" summary="">
				<tr>
				<td style="text-align:left; vertical-align:top;">
					<a href="http://www.nsf.gov" target="window2"><img 
						class="whiteTable" 
						src="https://new.nsf.gov/themes/custom/nsf_theme/components/images/logo/logo-desktop.svg" 
						style="height:60px;"
						alt="NSF"/></a>
					<a href="http://www.pearson.com" target="window2"><img 
						class="whiteTable" 
						src="https://www.pearson.com/content/dam/corporate/global/pearson-dot-com/Pearson_logo.png"
						alt="Pearson"
						style="height:60px;"/></a>
					<a href="http://secondavenuelearning.com" target="window2"><img 
						class="whiteTable" 
						src="https://secondavenuelearning.com/wp-content/uploads/2019/08/ezgif-1-d2c2f8bb7ac8.png" 
						width="265" alt="SAL"/></a>
				</td>
				<td style="text-align:right; vertical-align:top; width:55%;">
					<%= tempUser.translate("Created by ***Robert B. Grossman*** and "
							+ "***Raphael Finkel***", new String[] {
								"<a href=\"https://chem.as.uky.edu/users/rbgros1\">"
									+ "Robert B. Grossman</a>", 
								"<a href=\"http://www.cs.uky.edu/~raphael/\">"
									+ "Raphael Finkel</a>"}) %><br />
					<%= tempUser.translate("Departments of Chemistry and Computer Science") %><br />
					<a href="http://www.uky.edu"><%= tempUser.translate(
							"University of Kentucky") %></a><br />
					<% if (chosenLang != null) { %>
						<%= tempUser.translate("United States of America") %><br />
					<% } // if chosen language %>
					&copy; 2005&ndash;2024 <%= tempUser.translate("University of Kentucky") %> 
					&amp; Pearson Education<br />
					<% if ("portugu&#234;s".equalsIgnoreCase(chosenLang)) { %>
						<%= tempUser.translate("Translated by") %> 
						<a href="mailto:rodrigocormanich@gmail.com">Rodrigo Cormanich</a><br/>
					<% } // if chosen language %>
				</td>
				</tr>
			</table>
		</td>
		</tr>
		<tr>
		<td class="boldtext" style="vertical-align:top;">
			<a href="http://www.chemaxon.com" target="window2"><img 
			class="whiteTable" src="images/ChemAxon_free_acpack_400.png" alt="ChemAxon"/></a>
		</td>
		</tr>
		<tr>
		<td class="boldtext" style="padding-top:20px;">
			<%= tempUser.translate("The development of this program has been funded "
						+ "partly by the ***National Science Foundation***.",
					"<a href=\"http://www.nsf.gov\" target=\"window2\">"
						+ tempUser.translate("National Science Foundation")
						+ "</a>") %>
		</td>
		</tr>
		<tr>
		<td class="boldtext" style="padding-top:20px;">
			<a href="../ace/public/features/index.html"><%= 
					tempUser.translate("Learn more about ACE.") %></a>
		</td>
		</tr>
		<tr>
		<td class="boldtext" style="padding-top:20px;">
			<%= tempUser.translate("Check out some ***utility programs*** "
						+ "based on methods developed for ACE.",
					"<a href=\"../ace/public/welcome.html\">"
						+ tempUser.translate("utility programs") + "</a>") %>
		</td>
		</tr>
	</table>
</body>
</html>

