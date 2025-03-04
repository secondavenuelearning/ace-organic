	<% if (containsJmol) { %>
		jmolInitialize('../nosession/jmol');
	<% } else if (containsLewis) { %>

		function initLewis() {
			initLewisConstants(
					[<%= LewisMolecule.CANVAS_WIDTH %>, 
						<%= LewisMolecule.CANVAS_HEIGHT %>],
					<%= LewisMolecule.MARVIN_WIDTH %>, 
					['<%= LewisMolecule.PAIRED_ELECS %>',
						'<%= LewisMolecule.UNPAIRED_ELECS %>',
						'<%= LewisMolecule.UNSHARED_ELECS %>' ],
					'<%= LewisMolecule.LEWIS_PROPERTY %>',
					'<%= LewisMolecule.HIGHLIGHT %>');
			var figuresData = [];
			<% int cNum = 0;
			for (final TextContent content : contents) { 
				cNum++; %>
				if (cellExists('figData<%= cNum %>')) {
					figuresData.push([
							getValue('figData<%= cNum %>'),
							0,
							'<%= cNum %>']);
				} // if content is Lewis
			<% } // for each content %>
			if (!isEmpty(figuresData)) {
				loadLewisInlineImages('<%= pathToRoot %>', figuresData);
			}
		} // initLewis()

	<% } // if there's a Jmol figure %>

	function goBack(readOnly) {
		self.location.href = (readOnly ? 'readTextbook.jsp'
				: 'writeChapter.jsp?chapNum=<%= chapNum %>');
	} // goBack()

	function startQ(qId) {
		var go = 'startQuestion.jsp?isInstructorOrTA=<%= 
				isInstructorOrTA %>&qId=' + qId;
		// alert(go);
		openPreviewWindow(go);
	} // startQ()

// vim:filetype=jsp
