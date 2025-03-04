<%@ page language="java" %>
<%@ page import="
	com.epoch.chem.ChemUtils,
	com.epoch.energyDiagrams.EDiagram,
	com.epoch.energyDiagrams.YAxisScale,
	com.epoch.exceptions.EquationFormatException,
	com.epoch.genericQTypes.TableQ,
	com.epoch.physics.EquationFunctions,
	com.epoch.physics.Equations,
	com.epoch.qBank.CaptionsQDatum,
	com.epoch.qBank.EDiagramQDatum,
	com.epoch.qBank.QDatum,
	com.epoch.qBank.Question,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.ArrayList,
	java.util.List"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

	Question question;
	int qType;
	synchronized (session) {
		question = (Question) session.getAttribute("qBuffer");
		qType = MathUtils.parseInt((String) session.getAttribute("qType"));
	}
    if (question == null) {
	    %> <jsp:forward page="../errorParam.jsp"/> <%
    }
	final int qdNum = MathUtils.parseInt(request.getParameter("qdNum"));
	final int afterSave = MathUtils.parseInt(request.getParameter("afterSave"));
	final boolean isTableQRowOrCol = request.getParameter("isTableQRowOrCol") != null;
	final boolean isED = Question.isOED(qType) || Question.isRCD(qType);
	QDatum qDatum = (isED ? new EDiagramQDatum() 
			: isTableQRowOrCol ? new CaptionsQDatum() : new QDatum());
	qDatum.dataType = MathUtils.parseInt(request.getParameter("qdType"));
	String formatProblem = null;
	if (qDatum.isMarvin()) {
		final String data = request.getParameter("qdData");
		qDatum.data = ChemUtils.setFromMarvinJS(
				Utils.inputToCERs(data.replaceAll("\r\n", "\n"))); 
		/* Utils.alwaysPrint("saveQData.jsp: after converting to CERs, qdData = ", 
				qDatum.data); */
    	qDatum.name = Utils.inputToCERs(request.getParameter("molname"));
		final String displayOpts = request.getParameter("displayOpts");
		qDatum.setDisplayOptions(displayOpts);
	} else if (request.getParameter("preloadTable") != null) {
		qDatum.data = TableQ.responseToXML(request);
	} else if (isTableQRowOrCol) {
		qDatum.data = request.getParameter("numRowsOrCols");
		int captionNum = 0;
		final List<String> captions = new ArrayList<String>();
		while (true) {
			String caption = request.getParameter(
					Utils.toString("caption", captionNum++));
			if (caption == null) break;
			captions.add(Utils.inputToCERs(caption.trim()));
		} // while true
		((CaptionsQDatum) qDatum).captions = 
				captions.toArray(new String[captions.size()]);
	} else if (isED) {
		final EDiagramQDatum eqDatum = (EDiagramQDatum) qDatum;
		// data is one dimension for OED and two tab-separated dimensions for RCD
		final StringBuilder dimsBld = new StringBuilder().append(
				request.getParameter("numRows"));
		if (Question.isOED(qType)) { // captions for OED only
			final String[] captions = new String[3];
			for (int capNum = 1; capNum <= 3; capNum++) {
				final String caption = request.getParameter(Utils.toString(
						"col", capNum, "Caption"));
				captions[capNum - 1] = (caption == null ? "" 
						: Utils.inputToCERs(caption.trim()));
			} // for each caption
			eqDatum.captions = captions;
		} else { // 2nd dimension for RCD only
			dimsBld.append(EDiagram.QDATA_SEP).append(
					request.getParameter("numCols"));
		} // if qType
		eqDatum.data = dimsBld.toString();
		// labels for OED or RCD
		int lblNum = 1;
		final List<String> labels = new ArrayList<String>();
		while (true) {
			final String label = 
					request.getParameter(Utils.toString("label", lblNum++));
			if (label == null) break;
			labels.add(Utils.inputToCERs(label.trim()));
		} // while there are more labels
		final String[] labelsArr = labels.toArray(new String[labels.size()]);
		if (!Utils.membersAreEmpty(labelsArr)) {
			eqDatum.labels = labelsArr;
		} // if there are labels
		final String useYAxisScale = request.getParameter("useYAxisScale");
		// Utils.alwaysPrint("saveQData.jsp: useYAxisScale = ", useYAxisScale);
		if ("on".equals(useYAxisScale)) {
			final String[] data = new String[YAxisScale.NUM_SCALE_DATA];
			data[YAxisScale.ROW_INIT] = request.getParameter("rowInit");
			data[YAxisScale.QUANT_INIT] = request.getParameter("quantInit");
			data[YAxisScale.ROW_INCREMENT] = request.getParameter("rowIncrement");
			data[YAxisScale.QUANT_INCREMENT] = request.getParameter("quantIncrement");
			data[YAxisScale.UNIT] = request.getParameter("unit");
			eqDatum.yAxisScale = new YAxisScale(data);
			// Utils.alwaysPrint("saveQData.jsp: ", eqDatum.yAxisScale.toEnglish());
		} // if have y-axis scale
		/* Utils.alwaysPrint("saveQData.jsp: captions = ", eqDatum.captions,
				", labels = ", eqDatum.labels); /**/
		qDatum = eqDatum;
	} else if (qDatum.isText()) {
		qDatum.data = Utils.inputToCERs(request.getParameter("qdData"));
		if (Question.isEquations(qType)) {
			final Equations eqnsObj = new Equations(qDatum.data);
			if (eqnsObj.getEntries().length == 0 
					&& Utils.isEmpty(eqnsObj.getConstants())) {
				if (Utils.isEmpty(eqnsObj.getVariablesNotUnitsStr())) {
					formatProblem = "";
				}
			} else try {
				eqnsObj.checkForValidity();
			} catch (EquationFormatException e) {
				formatProblem = e.getEquation();
			} // try
		} // if qType
	} else {
		qDatum.data = request.getParameter("qdData");
	} // if data needs to be processed

	/* Utils.alwaysPrint("saveQData.jsp: qdNum = ", qdNum,
			", qdType = ", qDatum.dataType, 
			" (MARVIN = ", QDatum.MARVIN, ", ", "TEXT = ", QDatum.TEXT, ", ",
			"RGROUP = ", QDatum.RGROUP, ", ", "VAR_VALUES = ", QDatum.VAR_VALUES, ", ", 
			"SYNTH_OK_SM = ", QDatum.SYNTH_OK_SM, ", ", "SM_EXPR = ", QDatum.SM_EXPR, 
			"), data = ", qDatum.data); /**/
	if (!"".equals(formatProblem)) {
		if (qdNum == 0) question.addQDatum(qDatum);
		else question.setQDatum(qdNum, qDatum);
	} // if format is OK

	final int ADD_NEW = 1;
	final int ADD_CLONE = 2;
	final int tableNum = MathUtils.parseInt(request.getParameter("tableNum"));
%>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<meta http-equiv="Content-Script-Type" content="type"/>
	<meta http-equiv="Content-Style-Type" content="text/css"/>
	<script type="text/javascript">
	// <!-- >
	function finish() {
		<% 
		final int newQDNum = MathUtils.parseInt(request.getParameter("newQDNum"));
		if (formatProblem != null) { 
			if (Utils.isEmpty(formatProblem)) { %>
				alert('You cannot save no equations.  Either delete '
						+ 'existing equations, or, if you have none, '
						+ 'use the Cancel button.');
				closeMe();
			<% } else { %>
				alert('The following equation is misformatted:\n\n'
						+ '<%= Utils.toValidJS(formatProblem) %>\n\n'
						+ 'Returning you to the editing page to fix it.');
        			var go = 'loadQData.jsp?qDatumNum=<%= qdNum == 0
						? newQDNum : qdNum %>&tableNum=<%= tableNum %>';
				self.location.href = go;
			<% } // if there is a format problem %>
		<% } else if (afterSave == ADD_NEW) { %>
			self.location.href = 'loadQData.jsp?qDatumNum=0&tableNum=<%= tableNum %>';
		<% } else if (afterSave == ADD_CLONE) { %>
        		var go = 'loadQData.jsp?qDatumNum=<%= qdNum == 0 ? newQDNum 
					: qdNum %>&cloneEdit=true&tableNum=<%= tableNum %>';
			self.location.href = go;
		<% } else { %>
			closeMe();
		<% } %>
	} // finish()

	function closeMe() {
		opener.location.href = '../question.jsp?qId=same';
		self.close();
	} // closeMe()
	// -->
	</script>
</head>
<body onload="finish();"></body>
</html>
