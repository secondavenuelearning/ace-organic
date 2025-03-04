<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolImporter,
	chemaxon.struc.DPoint3,
	chemaxon.struc.MDocument,
	chemaxon.struc.MObject,
	chemaxon.struc.MPoint,
	chemaxon.struc.Molecule,
	chemaxon.struc.graphics.MPolyline,
	chemaxon.struc.graphics.MRectangle,
	com.epoch.AppConfig,
	com.epoch.chem.ChemUtils,
	com.epoch.chem.MolString,
	com.epoch.chem.VectorMath,
	com.epoch.qBank.Question,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";
	final String MRV_EXPORT = Utils.MRV.split(":")[0]; // until Marvin JS understands export parameters

	final String substrate = request.getParameter("substrate");
	final String SELF = "pointOnLine.jsp: ";

	Molecule substrateMol = null;
	if (substrate != null) {
		substrateMol = MolImporter.importMol(substrate);
		final MDocument mechDoc = substrateMol.getDocument();
		int arrowNum = 1;
		int rectangleNum = 1;
		final String[] rectPts = new String[] 
				{"NW", "NE", "SE", "SW", "CENTER", "N", "E", "S", "W"};
		MRectangle rect = null;
		MPolyline arrow = null;
		for (int objIndex = 0; objIndex < mechDoc.getObjectCount(); objIndex++) {
			final MObject mObject = mechDoc.getObject(objIndex);
			if (mObject instanceof MRectangle) {
				rect = (MRectangle) mObject;
				Utils.alwaysPrint(SELF + "rectangle ", rectangleNum++, ":");
				for (int ptNum = 0; ptNum < 9; ptNum++) {
					Utils.alwaysPrint("\t\tpoint ", ptNum + 1, " (",
							rectPts[ptNum], "): ",
							rect.getPointRef(ptNum, null).getLocation());
				} // for each of 9 points, center, and midpoints of rectangle
			} else if (mObject instanceof MPolyline) {
				arrow = (MPolyline) mObject;
				Utils.alwaysPrint(SELF + "arrow ", arrowNum++, ":");
				for (int ptNum = 0; ptNum < 2; ptNum++) {
					Utils.alwaysPrint("\t\tarrow point ", ptNum + 1, 
							": ", arrow.getPoint(ptNum).getLocation());
				} // for each of 3 points of arrow
			} // if is a rectangle or arrow
		} // for each object in the MDocument
		if (rect != null && arrow != null) {
			for (int arrowPtNum = 0; arrowPtNum < 2; arrowPtNum++) {
				final DPoint3 arrowPtLocn = 
						arrow.getPoint(arrowPtNum).getLocation();
				for (int rectPtNum = 0; rectPtNum <= rect.getPointRefCount(); rectPtNum++) {
					final DPoint3 rectPtLocn =
							rect.getPointRef(rectPtNum, null).getLocation();
					if (arrowPtLocn.equals(rectPtLocn)) {
						Utils.alwaysPrint("arrow point ", arrowPtNum + 1, 
								" coincides with rectangle point ",
								rectPts[rectPtNum]);
					} // if posns are equal
				} // for each of 9 points, center, and midpoints of rectangle
			} // for each of 2 points of arrow
		}
	}
	final String APPLET_NAME = "hybridMarvin";

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>Point coordinates calculator</title>
<link rel="stylesheet" href="<%= pathToRoot %>nosession/marvinJS/css/doc.css" type="text/css"/>
<link rel="stylesheet" href="<%= pathToRoot %>includes/epoch.css" type="text/css"/>
<script src="<%= pathToRoot %>js/jslib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/buttonImgs.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/marvinJSStart.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/openwindows.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>js/xmlLib.js" type="text/javascript"></script>
<script src="<%= pathToRoot %>nosession/marvinJS/js/promise.min.js"></script>
<script src="https://marvinjs.chemicalize.com/v1/<%= AppConfig.marvinJSLicense %>/client-settings.js"></script>
<script src="https://marvinjs.chemicalize.com/v1/client.js"></script>
<script type="text/javascript">
	// <!-- >

	<%@ include file="../js/marvinQuestionConstants.jsp.h" %>

	function loadSelections() { ; }

	function calculate() {
		marvinSketcherInstances['<%= APPLET_NAME %>'].
				exportStructure('<%= MRV_EXPORT %>').then(function(mol) {
			document.tester.substrate.value = mol;
			document.tester.submit();
		}, function(error) {
			alert('Molecule export failed:' + error);	
		});
	}

	// -->
</script>
</head>

<body style="overflow:auto;">

<br/>

<!-- self submit form -->
<form name="tester" action="pointOnLine.jsp" method="post">
<input type="hidden" name="substrate" value=""/>
<p class="boldtext big" style="text-align:center;">
ACE Point coordinates calculator
</P>
<p>
<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr><td class="regtext" >
	Draw arrows or rectangles, and see their coordinates in the log.
</td></tr>
</table>
<br/>

<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr>
<td style="text-align:center;" colspan="2">
	<div id="<%= APPLET_NAME %>" style="text-align:center;">
	<script type="text/javascript">
		// <!-- >
		startMarvinJS('<%= substrate != null ? Utils.toValidJS(substrate) : "" %>', 
				MECHANISM, 0, '<%= APPLET_NAME %>', '<%= pathToRoot %>'); 
		// -->
	</script>
	</div>
</td>
</tr>
<tr>
<td style="text-align:center;" colspan="2">
	<br/>
	<input type="button" value=" Calculate " onclick="calculate()"/>
	<br/>
	<br/>
</td>
</tr>
</table>
</form>

<table style="width:500px; margin-left:auto; margin-right:auto;">
<tr>
<td class="regtext" style="text-align:left; padding-top:10px;" >
<a href="welcome.html">Back</a> to public ACE pages.
</td>
</tr>
</table>

</body>
</html>
