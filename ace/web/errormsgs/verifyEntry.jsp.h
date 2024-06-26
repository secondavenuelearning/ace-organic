<!-- Epoch page access verifer -->
<%@ page import="com.epoch.access.EpochEntry" %>
<%
	EpochEntry entry;
	synchronized (session) {
		entry = (EpochEntry) session.getAttribute("entry");
	}
	if (entry == null || !entry.isValid()) {
		System.out.println("verifyEntry.jsp: entry is "
				+ (entry == null ? "null" : "not valid"));
		%><jsp:forward page="/errormsgs/noAccess.html"/><%
	}
%>
