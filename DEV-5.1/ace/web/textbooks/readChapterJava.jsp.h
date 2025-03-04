<%@ page import="
	chemaxon.struc.Molecule,
	com.epoch.chem.ChemUtils,
	com.epoch.chem.MolString,
	com.epoch.evals.EvalResult,
	com.epoch.genericQTypes.Choice,
	com.epoch.genericQTypes.Rank,
	com.epoch.lewis.LewisMolecule,
	com.epoch.qBank.Figure,
	com.epoch.qBank.QDatum,
	com.epoch.qBank.Question,
	com.epoch.session.HWSession,
	com.epoch.synthesis.Synthesis,
	com.epoch.textbooks.Textbook,
	com.epoch.textbooks.TextChapter,
	com.epoch.textbooks.TextContent,
	com.epoch.utils.MathUtils,
	com.epoch.utils.Utils,
	java.util.ArrayList,
	java.util.List"
%><%
	request.setCharacterEncoding("UTF-8");
	final String pathToRoot = "../";

	Textbook book;
	synchronized (session) {
		book = (Textbook) session.getAttribute("textbook");
	} // synchronized
	final int chapNum = MathUtils.parseInt(request.getParameter("chapNum"));
	final TextChapter chapter = book.getChapter(chapNum);
	final List<TextContent> contents = chapter.getContents();
	final int numContents = contents.size();
	final List<ArrayList<TextContent>> arrangedContents = 
			new ArrayList<ArrayList<TextContent>>();
	int contentNum = 0;
	while (contentNum < numContents) {
		final TextContent currentContent = contents.get(contentNum);
		final ArrayList<TextContent> groupContents = 
				new ArrayList<TextContent>();
		groupContents.add(currentContent);
		arrangedContents.add(groupContents);
		if (!currentContent.isText() && !currentContent.isACEQuestion()) {
			while (contentNum + 1 < numContents) {
				final TextContent nextContent = contents.get(contentNum + 1);
				if (!nextContent.isText() && !nextContent.isACEQuestion()) {
					groupContents.add(nextContent);
					contentNum++;
				} else break;
			} // while more subsequent nontext contents
		} // if current content is not text
		contentNum++;
	} // while true
	final boolean preview = "true".equals(request.getParameter("preview"));
	final String[] allAuthorNames = (String[]) session.getAttribute("allAuthorNames");

	final boolean containsJmol = chapter.containsJmol();
	final boolean containsLewis = chapter.containsLewis();
	int jmolNum = 0;
	int mviewNum = 0;
// vim:filetype=jsp
%>
