<%!
// variable to store the path to the images directory
static final String pathToImagesFromRoot = "images/";

// Creates a button that when clicked takes the given action.  We use the css
// classes "button" and "button-normal" for formatting.
String makeButton(String label, Object... actionParts) {
	return Utils.toString("<table summary=\"button\"><tr><td "
				+ "class='button button-normal' " 
				+ "onmouseover=\"lighten(this)\" "
				+ "onmouseout=\"darken(this)\" " 
				+ "onmousedown=\"return depress(this);\" "
				+ "onmouseup=\"darken(this); ",
			Utils.join(actionParts, ""),
			"\">", label, "</td></tr></table>");
} // makeButton(String, Object...)

String makeButtonIcon(String type, String pathToRoot, Object... actionParts) {
	return Utils.toString("<a href=\"javascript:",
			Utils.join(actionParts, ""),
			"\"><img title=\"", type, "\" src=\"", 
			pathToRoot, pathToImagesFromRoot, type, 
			"Button.gif\" onmouseover=\"this.src='", pathToRoot, 
			pathToImagesFromRoot, type, 
			"ButtonOver.gif'\" onmouseout=\"this.src='", 
			pathToRoot, pathToImagesFromRoot, type, 
			"Button.gif'\" onmousedown=\"this.src='", 
			pathToRoot, pathToImagesFromRoot, type, 
			"ButtonClick.gif'\" onmouseup=\"this.src='", 
			pathToRoot, pathToImagesFromRoot, type, 
			"ButtonOver.gif'\" alt=\"", type, 
			"\" /></a>");
} // makeButtonIcon(String, String, Object...)

String makeTab(String label, String pathToRoot, Object... actionParts) {
	int labelCharCount = Utils.cersToUnicode(label).length();
	return Utils.toString("<div class=\"boldtext\" "
				+ "style=\"position:relative; cursor:pointer;\""
				+ " onmouseup=\"setTab('",
			toTabName(label), "'); ",
			Utils.join(actionParts, ""),
			"\"><img id=\"", toTabName(label), 
			"\" src=\"", pathToRoot, 
			pathToImagesFromRoot, 
			"tabOff.jpg\"  alt=\"\" ", 
			labelCharCount < 15 ? "" 
				: Utils.toString("height=\"24\" width=\"", 
					(labelCharCount - 15) * 6 + 120, "\" "),
			"/><div id=\"", toTabName(label), 
			"_text\" style=\"position:absolute; text-align:center; "
				+ "left:0px; top:5px; width:", 
			labelCharCount < 15 ? 120 : (labelCharCount - 15) * 6 + 120,
			"px;\">", label, "</div></div>");
} // makeTab(String, String, Object...)

String makeTab(String label, String action) {
	return makeTab(label, "", action);
} // makeTab(String, String)

// Takes a string and replaces spaces, ampersands, and single and double quotes with "_"
// Used in makeButton() and makeTab() so spaces can not be included in image ids
String toTabName(String it) {
	return Utils.toString("ID", it.replaceAll("[;#&\"'\\s]", "_"));
} // toTabName(String)

// Takes a string and replaces spaces with "_"
// Used in makeButton() and makeTab() so spaces can not be included in image ids
String convertSpaces(String it) {
	return it.replaceAll("[\"'\\s]", "_");
} // convertSpaces(String)

%>
