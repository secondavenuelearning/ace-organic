<%@ page language="java" %>
<%@ page import="
	com.epoch.chem.ChemUtils,
	com.epoch.qBank.Figure,
	com.epoch.qBank.Question,
	com.epoch.synthesis.Synthesis,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils"
%>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ include file="/errormsgs/verifyEntry.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

    /* 
		figNum  - 0 | seq num of figure
		data
	 */
	Question question;
	synchronized (session) {
		question = (Question) session.getAttribute("qBuffer");
	}

	if (question == null) {
	    %> <jsp:forward page="../errorParam.jsp"/> <%
    }

	final Figure figure = new Figure();
	final int type = MathUtils.parseInt(request.getParameter("figType"));
	figure.type = type;
	/* Utils.alwaysPrint("saveFigure.jsp: type = ", Figure.DBVALUES[type]); /**/
	if (figure.hasImage()) {
		figure.bufferedImage = request.getParameter("srcFile");
		/* Utils.alwaysPrint("saveFigure.jsp: imageFile = ", figure.bufferedImage); /**/
		if (figure.isImageAndVectors()) {
			figure.data = request.getParameter("figData");
			/* Utils.alwaysPrint("saveFigure.jsp: data = ", figure.data); /**/
		} // if vectors
	} else {
		final String data = request.getParameter("figData");
		// posting converts \n to \r\n; convert it back
		figure.data = Utils.inputToCERs(data.replaceAll("\r\n", "\n")); 
		/* Utils.alwaysPrint("saveFigure.jsp: type = ", type, ", data =\n", figure.data); /**/
		if (!figure.isLewis()) {
			if (question.getQId() == 0) {
				final long qFlags = MathUtils.parseLong(request.getParameter("qFlags"));
				question.setQFlags(qFlags); // needed for showMapping value in new Qs
			} // if question is new
			if (figure.isReaction() || figure.isJmol()) {
				String addlData1 = Utils.inputToCERs(request.getParameter("addlData1").trim());
				String addlData2 = Utils.inputToCERs(request.getParameter("addlData2").trim());
				if (addlData1 == null) addlData1 = "";
				else addlData1 = addlData1.replaceAll("\r\n", "\n");
				if (addlData2 == null) addlData2 = "";
				else addlData2 = addlData2.replaceAll("\r\n", "\n");
				if (!"".equals(addlData1) || !"".equals(addlData2)) {
					final StringBuilder bld = new StringBuilder().append(addlData1);
					if (figure.isReaction()) {
						bld.append(Figure.RXN_TEXT_SEP).append(addlData2);
					} else if (figure.isJmol()) {
						bld.append(Figure.JMOL_SEP).append(addlData2);
					} // if figure type
					figure.addlData = bld.toString();
				} else figure.addlData = "";
				/* Utils.alwaysPrint("saveFigure.jsp: "
						+ "after processing, addlData = ", figure.addlData); /**/
			} else if (figure.isSynthesis()) {
				final String rxnIdsStr = request.getParameter("rxnIds");
				if (!Utils.isEmpty(rxnIdsStr)) {
					figure.data = ChemUtils.setProperty(figure.data, 
							Synthesis.RXN_IDS, rxnIdsStr);
				} // if there are rxnIds to store
			} // if type
			if (!figure.isJmol() && !figure.isMRVText()) {
				figure.data = ChemUtils.setFromMarvinJS(figure.data);
			} // if don't add from marvinJS property
		} // if not Lewis
	} // if image

	final int figNum = MathUtils.parseInt(request.getParameter("figNum"));
	if (figNum == 0) question.addNewFigure(figure);
	else question.setFigure(figNum, figure);

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
			opener.location.href = '../question.jsp?qId=same&reloadFigs=true';
			self.close();
		}
		// -->
	</script>
</head>
<body onload="finish();">
	Saved!!!
</body>
</html>
