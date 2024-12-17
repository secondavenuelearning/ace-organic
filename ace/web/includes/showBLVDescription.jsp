<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolImporter,
	chemaxon.struc.Molecule,
	com.epoch.chem.MolString,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String mrvStr = request.getParameter("mrvStr");
%>
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>Description of window contents for BLV people</title>
<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" type="text/css"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
</head>
<body class="regtext" style="overflow:auto;">
<p><%= MolString.getBLVDescription(mrvStr) %>
</body>
</html>
