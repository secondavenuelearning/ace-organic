<%-- Set the content type header with the JSP directive --%>
<%@ page contentType = "application/vnd.ms-excel" %>
<%-- Set the content disposition header --%>
<% 	response.setHeader("Content-Disposition", "attachment; filename=\"" 
			+ request.getParameter("filename") + ".xls\""); 
%>
<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>
<%@ page import="
	com.epoch.evals.EvalResult,
	com.epoch.session.GradeSet,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.text.NumberFormat,
	java.util.ArrayList,
	java.util.Arrays,
	java.util.List"
%>
<%@ include file="/includes/functions.inc.jsp.h" %>
<%@ include file="/navigation/menuHeaderJava.jsp.h" %>
<%@ include file="/navigation/courseSidebarJava.jsp.h" %>
<%
	response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
	request.setCharacterEncoding("UTF-8");

	final String pathToRoot = "../";
	final String studentNumLabel = user.getInstitutionStudentNumLabel();

	final boolean isStudentView = role == User.STUDENT; 
	final int oneStudentNum = 
			MathUtils.parseInt(request.getParameter("oneStudentNum"), -1);

	GradeSet hwGrades;
	String[] hwNames;
	String[] userIds; 
	String[] userNames; 
	String[] studentNums; 
	synchronized (session) {
		userIds = (String[]) session.getAttribute("userIds"); 
		userNames = (String[]) session.getAttribute("userNames"); 
		studentNums = (String[]) session.getAttribute("studentNums"); 
		hwNames = (String[]) session.getAttribute("hwNames"); 
		hwGrades = (GradeSet) session.getAttribute("hwGrades");
		if (role == User.STUDENT) {
			final StudentSession studSess = (StudentSession) userSess;
			if (studSess.isTA()) {
				role = (course.tasMayGrade() ? User.INSTRUCTOR : User.TA);
			}
		} // if a student
	} // synchronized

	/*
	Utils.alwaysPrint("allGrades.jsp: role = ", role, 
			", isStudentView = ", isStudentView,
			", userNames.length = ", userNames.length, 
			", userNames[0] = ", userNames[0);
	/**/

	final int numHWs = assgts.length;
	// final int[] allNumQs = new int[numHWs];
	final List<ArrayList<ArrayList<Integer>>> allGroupedQIds =
			new ArrayList<ArrayList<ArrayList<Integer>>>();
	final List<ArrayList<Integer>> allQIdsByPos =
			new ArrayList<ArrayList<Integer>>();
	final int[][] allQPicks = new int[numHWs][0];
	final int[] allNumGroups = new int[numHWs];
	final int[] allTotalQs = new int[numHWs];
	for (int hwNum = 1; hwNum <= numHWs; hwNum++) {
		// allNumQs[hwNum - 1] = hwGrades.getNumQuestionsAssigned(hwNum);
		List<ArrayList<Integer>> groupedQIds = null;
		int[] qPicks = null;
		int numGroups = 0;
		if (isStudentView) {
			groupedQIds = hwGrades.getAssignedQIds(hwNum, userIds[0]);
			numGroups = groupedQIds.size();
			if (numGroups != 0) {
				qPicks = new int[numGroups];
				Arrays.fill(qPicks, 1);
			} // if Qs have been assigned
		} // if should get only assigned Qs
		if (!isStudentView) {
			groupedQIds = hwGrades.getGroupedQIds(hwNum);
			qPicks = hwGrades.getQuestionPicks(hwNum);
			numGroups = qPicks.length;
		} // if should get all possible Qs
		allGroupedQIds.add(new ArrayList<ArrayList<Integer>>(groupedQIds));
		allQPicks[hwNum - 1] = qPicks;
		allNumGroups[hwNum - 1] = numGroups;
		final ArrayList<Integer> qIdsByPos = new ArrayList<Integer>();
		for (final ArrayList<Integer> qIdsGroup : groupedQIds) {
			for (final Integer qId : qIdsGroup) {
				qIdsByPos.add(qId);
			} // for each Q in group
		} // for each group of Qs
		allQIdsByPos.add(qIdsByPos);
		allTotalQs[hwNum - 1] = qIdsByPos.size();
	} // for each assignment

	final String detailFlag = request.getParameter("details");
	final boolean detailed = detailFlag == null || "yes".equals(detailFlag);

	final String UNATTEMPTED_STR = "";
	final String HUMAN_NEEDED_SYMB = "???";
	final String RIGHT_COLOR = "blue"; // "#000000";
	final String PARTIAL_COLOR = "purple"; // "#FE8502";
	final String WRONG_COLOR = "red"; // "#FF0000";
	final String HUMAN_NEEDED_COLOR = "gray"; // "#FF0000";
	final String HUMAN_NEEDED_STR = "??";

	final NumberFormat numberFormat = NumberFormat.getInstance();
	numberFormat.setMaximumFractionDigits(course.getNumDecimals());

	final char NONE = EvalResult.NO_STATUS;
	final char INITIALIZED = EvalResult.INITIALIZED;
	final char SAVED = EvalResult.SAVED;
	final char HUMAN_NEEDED = EvalResult.HUMAN_NEEDED;

	 %>
	<% for (int hwNum = 1; hwNum <= numHWs; hwNum++) { %>
		<table>
		<tr>
		<td><%= user.translate("Name") %></td>
		<% if (!isStudentView && !Utils.isEmpty(studentNumLabel)) { %>
			<td><%= studentNumLabel %></td>
		<% } // if !isStudentView
		final List<ArrayList<Integer>> groupedQIds = allGroupedQIds.get(hwNum - 1);
		final int[] qPicks = allQPicks[hwNum - 1];
		final int numGroups = allNumGroups[hwNum - 1]; %>
		<td><%= user.translate("Total for ***1***", hwNum) %></td>
		<% int qNum = 0;
		for (int grpNum = 0; grpNum < numGroups; grpNum++) {
			final List<Integer> qIdsGroup = groupedQIds.get(grpNum);
			final int numGroupQs = qIdsGroup.size(); %>
			<td colspan="<%= numGroupQs * (detailed && numGroupQs != 1 ? 2 : 1) %>">
				<% final StringBuilder qStrBld = new StringBuilder();
				for (int pick = 0; pick < qPicks[grpNum]; pick++) {
					qNum++;
					if (pick > 0) qStrBld.append(" &amp; ");
 					Utils.appendTo(qStrBld, 
							user.translate("Q"), hwNum, '.', qNum);
				} // for each pick 
				if (numGroupQs == 1) {
 					Utils.appendTo(qStrBld, " (#", qIdsGroup.get(0), ')');
				} // if there's just one Q %>
				<%= qStrBld.toString() %>
			</td>
			<% if (detailed && numGroupQs == 1) { %>
				<td><%= Utils.toString(user.translate("Q"), hwNum, '.', qNum, 
						user.translate("tries")) %></td>
			<% } // if detailed
		 } // for each grpNum %>
		<td><%= user.translate("Total") %></td>
		</tr>
		<% if (allTotalQs[hwNum - 1] > allNumGroups[hwNum - 1]) { %>
			<tr>
			<td></td>
			<% if (!isStudentView && !Utils.isEmpty(studentNumLabel)) { %>
				<td></td>
			<% } %>
			<td></td>
			<% for (int grpNum = 0; grpNum < numGroups; grpNum++) {
				final List<Integer> qIdsGroup = groupedQIds.get(grpNum);
				final int numGroupQs = qIdsGroup.size();
				for (int qNumInGrp = 0; qNumInGrp < numGroupQs; qNumInGrp++) { %>
					<td>
						<% if (numGroupQs > 1) { %>
							#<%= qNumInGrp + 1 %> 
							(#<%= qIdsGroup.get(qNumInGrp) %>) 
							<%= detailed ? user.translate("pts") : "" %>
						<% } // if there's more than one Q in this group %>
					</td>
					<% if (detailed) { %>
						<td>
						<% if (numGroupQs > 1) { %>
							#<%= qNumInGrp + 1 %> <%= user.translate("tries") %>
						<% } // if there's more than one Q in this group %>
						</td>
					<% } // if detailed %>
				<% } // for each Q in the group
			} // for each group %>
			</tr>
		<% } // if need a second row
		final List<Integer> qIdsByPosList = allQIdsByPos.get(hwNum - 1);
		final int[] qIdsByPos = Utils.listToIntArray(qIdsByPosList);
		for (int userNum = 1; userNum <= userIds.length; userNum++) {
			double totalGrades = 0;
			final double[] allSumGrades = new double[numHWs];
			final int[] allNumQsTried = new int[numHWs];
			final String[][] allGradeOps = new String[numHWs][];
			final String rowColor = (userNum % 2 != 0 ? "greenrow" : "whiterow");
	 		allGradeOps[hwNum - 1] = new String[allTotalQs[hwNum - 1]];
	 		Arrays.fill(allGradeOps[hwNum - 1], UNATTEMPTED_STR);
 			final EvalResult[] evalResults =
					hwGrades.getOrderedResults(hwNum, userIds[userNum - 1]);
			final int[] assignedQIds = (isStudentView ? qIdsByPos
					: hwGrades.getAssignedQIdsArr(hwNum, userIds[userNum - 1]));
			// decide what to display
			for (final EvalResult evalResult : evalResults) {
				if (evalResult != null && !Utils.among(evalResult.status, 
						NONE, INITIALIZED, SAVED)) {
					String gradeOp = "";
					allNumQsTried[hwNum - 1]++;
					final boolean humanReqd =
							evalResult.status == HUMAN_NEEDED;
					final String truncPts = (humanReqd ? HUMAN_NEEDED_SYMB
							: numberFormat.format(evalResult.modGrade));
					if (detailed) {
						gradeOp = Utils.toString(
								"<span style=\"font-weight:bold; color:", 
								humanReqd ? HUMAN_NEEDED_COLOR
									: evalResult.grade == 1.0 ? RIGHT_COLOR
									: evalResult.grade > 0.0 ? PARTIAL_COLOR
									: WRONG_COLOR, 
								";\">", truncPts, "</span></td><td>", 
								evalResult.tries);
					} else { // not detailed
						gradeOp = truncPts;
				 	} // if detailed
				 	allSumGrades[hwNum - 1] += evalResult.modGrade;
					final int qPos = Utils.indexOf(qIdsByPos, evalResult.qId);
					allGradeOps[hwNum - 1][qPos] = gradeOp;
				} // if there's an evaluated response
			} // for each result
			totalGrades += allSumGrades[hwNum - 1];
	 		if (!isStudentView) {
				final String studentNumDisp = (studentNums[userNum - 1] == null
							|| "-1".equals(studentNums[userNum - 1]) 
						? " " : studentNums[userNum - 1]); %>
				<td><%= userNames[userNum - 1] %></td>
				<% if (!Utils.isEmpty(studentNumLabel)) { %>
					<td><%= studentNumDisp %></td>
				<% } %>
			<% } else { %>
				<tr>
				<td>
					<%= Utils.isEmpty(userNames[userNum - 1]) 
							? user.translate("Your score") : userNames[userNum - 1] %>
				</td>
			<% } // if isStudentView %>
				<td>
					<%= allNumQsTried[hwNum - 1] > 0 
							? numberFormat.format(allSumGrades[hwNum - 1]) 
							: UNATTEMPTED_STR %>
				</td>
				<% for (int qPos = 0; qPos < allTotalQs[hwNum - 1]; qPos++) { %>
					<td>
						<%= detailed && Utils.isEmpty(allGradeOps[hwNum - 1][qPos])
								? "</td><td>" : allGradeOps[hwNum - 1][qPos] %>
					</td>
				<% } // for each Q in the assignment %>
			<td>
				<%= totalGrades %>
			</td>
			</tr>
		<% } // for each student userNum %>
	</table>
<% } // for each assignment %>

