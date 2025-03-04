	function setUpClock() {
		setClockConstants(<%= System.currentTimeMillis() %>,
				<%= hwsession.getDueDateInMillis() %>,
				'<%= user.translateJS("indefinite") %>');
		hideClockRemaining();
		setInterval('updateTimeLeft()', ONE_SEC);
	} // setUpClock()

// vim:filetype=jsp 
