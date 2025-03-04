<%@ page import="
	com.epoch.energyDiagrams.*,
	java.util.HashMap,
	java.util.List,
	java.util.Map" %>
<%
	final String DROP_HERE = OED.DROP_HERE;
	final String ADD = "Add";
	final String ORBS_OF_TYPE = "orbital(s) of type";
	final String CLICK_RCD = "Click on the table to add an energy state. "
			+ "Click <b>&bull;</b> to choose a state to delete, move, or "
			+ "connect to another energy state: click on an empty cell "
			+ "to move the state there, or click <b>&bull;</b> next to "
			+ "a second state to connect them with a line "
			+ "or to remove an existing line between them.";
	final String CLICK_OED = "Click <b>&bull;</b> to choose a group of orbitals "
			+ "to delete, move, or connect to another group: " 
			+ "click on an empty cell to move the group there, or click "
			+ "<b>&bull;</b> next to a second group to connect them with a "
			+ "line or to remove an existing line between them.";
	final String REFRESH_BUTTON = "Refresh";
	final String DELETE_BUTTON = "Delete";

	final String[] edPhrasesArr = new String[] {
			DROP_HERE,
			ADD,
			ORBS_OF_TYPE,
			CLICK_RCD,
			CLICK_OED,
			REFRESH_BUTTON,
			DELETE_BUTTON
			};
	Map<String, String> edPhrases = Utils.mapToSelf(edPhrasesArr);

%>
<!-- vim:filetype=jsp
-->
