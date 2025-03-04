// <!-- avoid parsing the following as HTML
	function logout() {
		self.location.href = '<%= pathToAppRoot %>logout.jsp';
	}
	
	function goHome() {
		self.location.href = '<%= pathToAppRoot %>userHome.jsp';
	}

	<% if (realRole == User.ADMINISTRATOR) { %>
	function goToAdmin() {
		self.location.href = '<%= pathToAppRoot %>admin/listProfiles.jsp';
	}
	<% } %>

	function getHere() {
		var here = self.location.href;
		var lastSlashPosn = here.lastIndexOf('/');
		var end = here.substring(lastSlashPosn + 1);
		if (end === 'Login') {
			here = '../<%= pathToRoot %>userHome.jsp';
		}
		return encodeURIComponent(here);
	} // getHere()

	function editProfile() {
		self.location.href = '<%= pathToRoot %>profile/editProfile.jsp?goBack='
				+ getHere();
	}
	
	function openQBE() {
		self.location.href = '<%= pathToAppRoot %>authortool/enterAuthortool.jsp';
	}

	function openTexts() {
		self.location.href = '<%= pathToAppRoot %>textbooks/chooseTextbook.jsp';
	}

	function studentReports() {
		self.location.href = '<%= pathToAppRoot %>reports/chooseStudents.jsp?goBack='
				+ getHere();
	}

	function openFeedback() {
		var content = self.location.href;
		var role = (<%= role == User.ADMINISTRATOR %> ? "Administrator"
				: <%= role == User.INSTRUCTOR %> ? "Instructor"
				: "Student");
		var w = window.open("<%= pathToAppRoot %>sendFeedback.jsp?content=" + content 
					+ "&username=<%= user.getUserId() %>&role=" + role, "Feedback",
				"width=500, height=500, top=200, left=100, scrollbars=yes, " 
					+ "resizable=yes");
		w.focus();
	} // openFeedback()

// --> end HTML comment
// vim:filetype=jsp:
