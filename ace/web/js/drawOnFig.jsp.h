// <!-- avoid parsing the following as HTML
/* Include this page in any page that calls /js/drawOnFig.js.
 * Use: &lt;%@ include file="/js/drawOnFig.jsp.h" %&gt;
 * Calling page must include com.epoch.physics.DrawVectors.
 */

function initDrawOnFigConstants() {
	initDrawOnFigConstants2(
			<%= DrawVectors.X %>,
			<%= DrawVectors.Y %>,
			<%= DrawVectors.WIDTH %>,
			<%= DrawVectors.HEIGHT %>,
			<%= DrawVectors.RECT %>,
			<%= DrawVectors.ELLIP %>,
			<%= DrawVectors.CIRC %>,
			<%= DrawVectors.MARK %>,
			<%= DrawVectors.ARROW %>,
			'<%= DrawVectors.XML_TAG %>',
			'<%= DrawVectors.MARK_TAG %>',
			'<%= DrawVectors.X_TAG %>',
			'<%= DrawVectors.Y_TAG %>',
			'<%= DrawVectors.WIDTH_TAG %>',
			'<%= DrawVectors.HEIGHT_TAG %>',
			'<%= DrawVectors.TEXT_TAG %>',
			'<%= DrawVectors.VECTOR_TAG %>',
			'<%= DrawVectors.ORIGIN_TAG %>',
			'<%= DrawVectors.TARGET_TAG %>');
} // initDrawOnFigConstants()

// --> end HTML comment
// vim:filetype=jsp:
