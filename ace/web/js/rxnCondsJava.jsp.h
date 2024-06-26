<%@ page import="
	com.epoch.energyDiagrams.*,
	java.util.HashMap,
	java.util.List,
	java.util.Map" %>
<%
	final String CLICK_RXN = "Click on a reaction condition's name to change it."; 
	final String INSERT_HERE = "Insert here";
	final String ADD_1ST = "Add first reaction condition";
	final String RXN_CONDN = "Reaction condition";
	final String REMOVE = "Remove";
	final String AFTER_HERE = "Insert after here";

	final String[] rcPhrasesArr = new String[] {
			CLICK_RXN,
			INSERT_HERE,
			ADD_1ST,
			RXN_CONDN,
			REMOVE,
			AFTER_HERE
			};
	Map<String, String> rcPhrases = Utils.mapToSelf(rcPhrasesArr);

// vim:filetype=jsp
%>
