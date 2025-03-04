// <!-- avoid parsing the following as HTML
	var current_tab = "none";

	function goCourseHome() {
		self.location.href = "<%= pathToAppRoot %>course/courseHome.jsp";
	}	

	function openAssignment() {
		self.location.href = "<%= pathToAppRoot %>hwcreator/hwSetList.jsp";
	}

	function openGrades() {
		self.location.href = "<%= pathToAppRoot %>gradebook/grade.jsp";
	}

	function openForum() {
		self.location.href = "<%= pathToAppRoot %>forum/topics.jsp?group50=1";
	}

	function openTextbook() {
		self.location.href = "<%= pathToAppRoot %>textbooks/readTextbook.jsp";
	}

	<% if (Utils.among(role, User.INSTRUCTOR, User.ADMINISTRATOR) || isTA) { %>
	function openEnrollment() {
		self.location.href = "<%= pathToAppRoot %>enroll/listEnrollment.jsp";
	}
	<% } %>

	function setTab(id) {
		if (current_tab !== id) {
			if (document.images[id])
				document.images[id].src = "<%= pathToRoot 
						+ pathToImagesFromRoot %>tabClick.jpg";
			removeEvents(id);
			if (current_tab !== 'none') {
				if (document.images[current_tab])
					document.images[current_tab].src = "<%= pathToRoot 
							+ pathToImagesFromRoot %>tabOff.jpg";
				setEvents(current_tab);
			}
			current_tab = id;
		}
	}

	function changeTab(name) {
		var tabName = name;
		if (parent.sidebar && parent.sidebar.loaded) {
			parent.sidebar.setTab(name);
		}
	}

	function removeEvents(id) {
		<% if (request.getHeader("USER-AGENT").contains("MSIE"))  { %>
			var elem = document.getElementById(id);
			if (elem) {
				elem.detachEvent('onmouseover',tabOver);
				elem.detachEvent('onmouseout',tabOut);
				elem.detachEvent('onmousedown',tabDown);
				elem.detachEvent('onmouseup',tabUp);
			}
			id = id.concat('_text');
			elem = document.getElementById(id);
			if (elem) {
				elem.detachEvent('onmouseover',tabOver);
				elem.detachEvent('onmouseout',tabOut);
				elem.detachEvent('onmousedown',tabDown);
				elem.detachEvent('onmouseup',tabUp);
			}
		<% } else { %>
			var elem = document.getElementById(id);
			if (elem) {
				elem.removeEventListener('mouseover',tabOver,false);
				elem.removeEventListener('mouseout',tabOut,false);
				elem.removeEventListener('mousedown',tabDown,false);
				elem.removeEventListener('mouseup',tabUp,false);
			}
			id = id.concat('_text');
			elem = document.getElementById(id);
			if (elem) {
				elem.removeEventListener('mouseover',tabOver,false);
				elem.removeEventListener('mouseout',tabOut,false);
				elem.removeEventListener('mousedown',tabDown,false);
				elem.removeEventListener('mouseup',tabUp,false);
			}
		<% } %>
	}

	function setEvents(id) {
		<% if (request.getHeader("USER-AGENT").contains("MSIE"))  { %>
			var elem = document.getElementById(id);
			if (elem) {
				elem.attachEvent('onmouseover',tabOver);
				elem.attachEvent('onmouseout',tabOut);
				elem.attachEvent('onmousedown',tabDown);
				elem.attachEvent('onmouseup',tabUp);
			}
			id = id.concat('_text');
			elem = document.getElementById(id);
			if (elem) {
				elem.attachEvent('onmouseover',tabOver);
				elem.attachEvent('onmouseout',tabOut);
				elem.attachEvent('onmousedown',tabDown);
				elem.attachEvent('onmouseup',tabUp);
			}
		<% } else { %>
			var elem = document.getElementById(id);
			if (elem) {
				elem.addEventListener('mouseover',tabOver,false);
				elem.addEventListener('mouseout',tabOut,false);
				elem.addEventListener('mousedown',tabDown,false);
				elem.addEventListener('mouseup',tabUp,false);
			}
			id = id.concat('_text');
			elem = document.getElementById(id);
			if (elem) {
				elem.addEventListener('mouseover',tabOver,false);
				elem.addEventListener('mouseout',tabOut,false);
				elem.addEventListener('mousedown',tabDown,false);
				elem.addEventListener('mouseup',tabUp,false);
			}
		<% } %>
	}

	function tabOver(evt) {
		<% if (request.getHeader("USER-AGENT").contains("MSIE"))  { %>
			var e = window.event;
			if (e.srcElement.id.indexOf('_text') > -1) {
				var temp = e.srcElement.id.substring(0, e.srcElement.id.indexOf('_text'));
				setSrc(temp, "<%= pathToRoot + pathToImagesFromRoot %>tabOver.jpg");
			}
			e.srcElement.src = "<%= pathToRoot + pathToImagesFromRoot %>tabOver.jpg";
		<% } else { %>	
			if (evt.target.id && evt.target.id.indexOf('_text') > -1) {
				var temp = evt.target.id.substring(0, evt.target.id.indexOf('_text'));
				setSrc(temp, "<%= pathToRoot + pathToImagesFromRoot %>tabOver.jpg");
			}
			evt.target.src = "<%= pathToRoot + pathToImagesFromRoot %>tabOver.jpg";
		<% } %>
	}

	function tabOut(evt) {
		<% if (request.getHeader("USER-AGENT").contains("MSIE"))  { %>
			var e = window.event;
			if (e.srcElement.id.indexOf('_text') > -1) {
				var temp = e.srcElement.id.substring(0, e.srcElement.id.indexOf('_text'));
				setSrc(temp, "<%= pathToRoot + pathToImagesFromRoot %>tabOff.jpg");
			}
			e.srcElement.src = "<%= pathToRoot + pathToImagesFromRoot %>tabOff.jpg";
		<% } else { %>	
			if (evt.target.id && evt.target.id.indexOf('_text') > -1) {
				var temp = evt.target.id.substring(0, evt.target.id.indexOf('_text'));
				setSrc(temp, "<%= pathToRoot + pathToImagesFromRoot %>tabOff.jpg");
			}
			evt.target.src = "<%= pathToRoot + pathToImagesFromRoot %>tabOff.jpg";
		<% } %>
	}

	function tabDown(evt) {
		<% if (request.getHeader("USER-AGENT").contains("MSIE"))  { %>
			var e = window.event;
			if (e.srcElement.id.indexOf('_text') > -1) {
				var temp = e.srcElement.id.substring(0, e.srcElement.id.indexOf('_text'));
				setSrc(temp, "<%= pathToRoot + pathToImagesFromRoot %>tabClick.jpg");
			}
			e.srcElement.src = "<%= pathToRoot + pathToImagesFromRoot %>tabClick.jpg";
		<% } else { %>	
			if (evt.target.id.indexOf('_text') > -1) {
				var temp = evt.target.id.substring(0, evt.target.id.indexOf('_text'));
				setSrc(temp, "<%= pathToRoot + pathToImagesFromRoot %>tabClick.jpg");
			}
			evt.target.src = "<%= pathToRoot + pathToImagesFromRoot %>tabClick.jpg";
		<% } %>
	}

	function tabUp(evt) {
		<% if (request.getHeader("USER-AGENT").contains("MSIE"))  { %>
			var e = window.event;
			if (e.srcElement.id.indexOf('_text') > -1) {
				var temp = e.srcElement.id.substring(0, e.srcElement.id.indexOf('_text'));
				setSrc(temp, "<%= pathToRoot + pathToImagesFromRoot %>tabOver.jpg");
			}
			e.srcElement.src = "<%= pathToRoot + pathToImagesFromRoot %>tabOver.jpg";
		<% } else { %>	
			if (evt.target.id.indexOf('_text') > -1) {
				var temp = evt.target.id.substring(0, evt.target.id.indexOf('_text'));
				setSrc(temp, "<%= pathToRoot + pathToImagesFromRoot %>tabOver.jpg");
			}
			evt.target.src = "<%= pathToRoot + pathToImagesFromRoot %>tabOver.jpg";
		<% } %>
	}
// --> end HTML comment
// vim:filetype=jsp:
