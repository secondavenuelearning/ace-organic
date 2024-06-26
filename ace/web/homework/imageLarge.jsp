<?xml version="1.0" encoding="UTF-8"?>
<%@ page language="java" %>
<%@ page import="
	chemaxon.struc.DPoint3,
	com.epoch.physics.DrawVectors,
	com.epoch.qBank.QDatum,
	com.epoch.utils.Utils"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<% 	final String pathToRoot = "../";
	request.setCharacterEncoding("UTF-8");

	final String srcFile = request.getParameter("imageurl"); 
	final String vectorXML = request.getParameter("vectorXML");
	final String color = request.getParameter("color");
	final boolean haveCoords = !Utils.isEmpty(vectorXML);
	// Utils.alwaysPrint("imageLarge.jsp: vectorXML = ", vectorXML);
	final DrawVectors drawVectors = new DrawVectors(vectorXML);
	final DPoint3[][] vectors = drawVectors.getVectorPoints();
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<title>Figure image</title>
<head>
<% if (haveCoords) { %>
	<script src="<%= pathToRoot %>js/drawOnFig.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/offsets.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/position.js" type="text/javascript"></script>
	<script src="<%= pathToRoot %>js/wz_jsgraphics.js" type="text/javascript"></script>
<% } // if haveCoords %>
<script type="text/javascript" src="../js/jslib.js"></script>
<script type="text/javascript">
	<% if (haveCoords) { %>
		// <!-- >
 		<%@ include file="/js/drawOnFig.jsp.h" %>
		function paintImage() {
			initDrawOnFigConstants();
			initDrawOnFigGraphics('<%= color != null ? color : "red" %>');
			disallowDrawing();
		<%  for (final DPoint3[] vector : vectors) { %>
				drawonfig.allShapes[<%= DrawVectors.ARROW %>].push(
						[canvasSetX(<%= vector[DrawVectors.ORIGIN].x %>), 
						canvasSetY(<%= vector[DrawVectors.ORIGIN].y %>),
						canvasSetX(<%= vector[DrawVectors.TARGET].x %>), 
						canvasSetY(<%= vector[DrawVectors.TARGET].y %>)]);
		<% 	} // for each vector %>
			paintAll();
		} // paintImage() 
		// -->
	<% } // if haveCoords %>
</script>
</head>
<body<%= haveCoords ? " onload=\"paintImage();\"" : "" %>>
	<div id="canvas" style="position:relative; left:0px; top:0px;">
	<% if (haveCoords) { %>
		<input type="hidden" id="shapeChooser" value="<%= DrawVectors.ARROW %>" />
	<% } // if haveCoords %>
	<img src="<%= srcFile %>" id="clickableImage" class="unselectable" alt="picture" 
			onselect="return false;" ondragstart="return false;"/>
	</div>
</body>
</html>
