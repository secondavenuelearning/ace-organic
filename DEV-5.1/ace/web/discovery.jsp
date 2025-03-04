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
	if (url != null && url.matches("http://ace\\.chem\\.illinois\\.edu:91[\\d]+/.*")) { 
		response.sendRedirect(url.replaceFirst("http:", "https:").replaceFirst("91", "93"));
	} else if (url != null && url.matches("http://ace\\.chem\\.illinois\\.edu/.*")) {
		response.sendRedirect("https://ace.chem.illinois.edu/ace");
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

<!doctype html >
<html>
<head>
	<meta http-equiv="Pragma" content="no-cache"/>
	<meta http-equiv="Expires" content="Mon, 01 Jan 2001 12:00:00 GMT"/>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
	<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
	<link rel="shortcut icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/> 
	<title>ACE Organic Log In</title>
	<style type="text/css">
		  body {
			 margin: 0;
			 padding: 0;
		  }
		  a {
			 color: #2370a6;
			 text-decoration: none;
		  }
		  a:hover,
		  a:focus {
			 text-decoration: underline;
		  }
		  div#content {
			  margin: 0 auto;
			  width: 60%;
		  }
		  div.page-wrapper {
			 max-width: 960px;
			 margin: 0 auto;
			 border-left: 1px solid #267bb6;
			 border-right: 1px solid #267bb6;
		  }
		  header {
			 padding: 10px;
			 background-color: #267BB6;
			 border-bottom: 1px solid #0D5E94;
			 box-shadow: 0 -3px 0 -2px #0D5E94 inset, 0 1px 1px #CCCCCC;
			 filter: none;
			 overflow: hidden;
		  }
		  header img.wordmark {
			 float: left;
			 width: 316px;
			 margin-top: 5px;
			 display: block;
		  }
		  header h1 {
			 float: right;
			 margin: 0;
			 width: 77px;
			 height: 46px;
		  }
		  p.instVersion{
			  font-weight: bold;
			  text-align: left;
			  margin: -0.5rem 0 2rem;
		  }
		  ul.buttons {
			 list-style-type: none;
			 margin: 0;
			 padding: 0;
		  }
		  ul.buttons li {
			 margin: 0;
			 border: 1px solid #ccd;
			 border-radius: 5px;
			 background-color: #eef;
		  }
		  ul.buttons a:hover,
		  ul.buttons a:focus {
			 background-color: #eef;
		  }
		  ul.buttons li a {
			 text-align: center;
			 display: inline-block;
			 font-size: 1.6em;
			 line-height: 1.6em;
			 padding: 1rem;
			 text-decoration: none;
		  }
		  ul.buttons li a.login span {
			 display: block;
			 margin-left: auto;
			 margin-right: auto;
			 background: url('../images/login.png') no-repeat 0 10px;
		  }
		  ul.buttons li a.signup span {
			 margin-left: auto;
			 margin-right: auto;
			 display: block;
			 padding-left: 30px;
			 background: url('../images/signup.png') no-repeat 0 10px;
			 width: 4em;
		  }
		  div.desc-wrapper {
			 padding: 20px;
		  }
		  div.desc-wrapper h2 {
			 margin-top: 0px;
			 margin-bottom: 0px;
		  }
		  div.desc-wrapper p.signup {
			 margin-top: 5px;
			 margin-bottom: 20px;
		  }
		  div.loggedout {
			 #margin: 5px 10px;
		 margin-right: 5px;
			 margin-left: 5px;
		 margin-top: 4px;
		 margin-bottom: 15px;
			 padding: 10px;
			 float: right;
			 width: 30%;
			 background-color: #eef;
			 border: 1px solid #ccd;
			 border-radius: 5px;
		  }
		  div.not-uofi {
			 margin: 25px auto 0 auto;
			 padding: 10px 10px;
			 background-color: #eef;
			 text-align: center;
			 border: 1px solid #ccd;
			 border-radius: 5px;
		  }
		  div.not-uofi h2 {
			  line-height: 1.5em;
		  }
		  div.not-uofi ul {
			 list-style-type: none;
			 margin: 0;
			 padding: 0;
		  }
		  div.not-uofi a {
			 line-height: 1.6rem;
		  }
		  footer {
			 padding: 10px;
			 background-color: #267BB6;
			 border-top: 1px solid #0D5E94;
			 overflow: hidden;
		  }
		  footer ul {
			 list-style-type: none;
			 margin: 0 auto;
			 padding: 0;
			 width: 60%;
			 max-width: 600px;
		  }
		  footer ul li {
			 margin: 5px 0;
			 padding: 0;
			 float: left;
			 width: 32%;
			 text-align: center;
			 font-size: 0.8em;
		  }
		  footer ul li#uc {
			 width: 36%;
		  }
		  footer ul li a {
			 color: #fff;
		  }
		  @media all and (min-width: 0px) and (max-width: 720px) {
			 body {
				line-height: 1.2em;
			 }
			 header h1 {
				left: 80%;
			 }
			 ul.buttons li {
				border-bottom: 2px solid #ccc;
			 }
			 ul.buttons li a {
				font-size: 1.5em;
				line-height: 1.7em;
			 }
			 ul.buttons li a.login span {
				background-image: url('../images/login-mobile.png');
				background-position: 0 0px;
			 }
			 ul.buttons li a.signup span {
				background-image: url('../images/signup-mobile.png');
				background-position: 0 0px;
			 }
			 footer ul {
				width: 100%;
			 }
		  }
		  @media all and (min-width: 0px) and (max-width: 460px) {
			 header img.wordmark,
			 header h1 {
				margin-left: auto;
				margin-right: auto;
				float: none;
			 }
			 footer ul li {
				font-size: 1em;
				width: 100% !important;
			 }
			 div.loggedout {
				float: none;
				width: auto;
			 }
		  }
		  .visually-hidden {
			 position: absolute;
			 top: -30em;
			 left: -300em;
		  }
	   </style>
</head>
<body class="light" >
	<div id="content" role="main">
		<div>
			<img src="images/acelogo.jpg" alt="logo"/>
			<% if (aceIsDown) { %>
				<p class="boldtext big" style="color:red;">ACE is down for the moment.  
				We're working as quickly as possible to bring it back up.
				Please check back later.  </p>
			<% } // if ACE is down %>
			<p class="instVersion">
			<%= AppConfig.defaultInstitution %> (ACE 4.6)
			</p>
		</div>

		<% if (errmsg != null) { %>
			<p><%= tempUser.translate(errmsg) %></td>
		<% } %>

		<ul class="buttons">
		  <li class="bn-left">
			 <a class="login" href="extAuth/extAuth.jsp" 
			 		title="Log in using your University of Illinois credentials">
				<span>Log in using your <%= AppConfig.defaultInstitution %> credentials</span>
			 </a>
		  </li>
		</ul>

		<div class="not-uofi">
			<h2>Not part of the <%= AppConfig.defaultInstitution %>?</h2>
			<ul>
				<li><a id="login_form_switch_to_credentials" href="login.jsp">Log in 
					using ACE Organic credentials</a></li>
				<li><a href="nosession/register.jsp?userType=S&language=English">Sign 
					up for a non-<%= AppConfig.defaultInstitution %> account</a></li>
			</ul>
		</div>
	</div>
</body>
</html>

