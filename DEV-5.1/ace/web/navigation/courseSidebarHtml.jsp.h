<div id="tabsBar">
	<table style="background-color:#f6f7ed; width:100%; height:44px; 
			border-style:none; border-collapse:collapse; text-align:left;"
			summary="">
	<tr>
		<td class="boldtext big" style="width:100%; left:0px; top:14px; 
				white-space:nowrap; padding-left:10px;">
			<%= course.getName() %>
			<% if (!course.isEnabled()) { %>
				<a onclick="alert('<%= user.translateJS(
						"Students may not enter a disabled course.") %>');"><span 
					class="enlarged" style="color:green;">(<u><%= 
							user.translate("disabled") %></u>)</span></a>
			<% } // if course is disabled %>
		</td>
		<td style="vertical-align:bottom; position:relative; bottom:-1px;">
			<%= makeTab(user.translate("Course Home"), pathToAppRoot, "goCourseHome();") %>
			<script type="text/javascript">
				// <!-- 
				setEvents('<%= toTabName(user.translateJS("Course Home")) %>');
				// -->
			</script>
		</td>
		<td style="vertical-align:bottom; position:relative; bottom:-1px;">
			<%= makeTab(user.translate("Assignments"), pathToAppRoot, "openAssignment();") %>
			<script type="text/javascript">
				// <!-- 
				setEvents('<%= toTabName(user.translateJS("Assignments")) %>');
				// -->
			</script>
		</td>
		<% if (!(isInstructor || isTA) 
				|| course.getId() != AppConfig.tutorialId) { %>
		<td style="vertical-align:bottom; position:relative; bottom:-1px;">
			<%= makeTab(user.translate("Grade Book"), pathToAppRoot, "openGrades();") %>
			<script type="text/javascript">
				// <!-- 
				setEvents('<%= toTabName(user.translateJS("Grade Book")) %>');
				// -->
			</script>
		</td>
		<% } // if should display gradebook %>
		<% if (course.hasACEBook()) { %>
		<td style="vertical-align:bottom; position:relative; bottom:-1px;">
			<%= makeTab(user.translate("Textbook"), pathToAppRoot, "openTextbook();") %>
			<script type="text/javascript">
				// <!-- 
				setEvents('<%= toTabName(user.translateJS("Textbook")) %>');
				// -->
			</script>
		</td>
		<% } // if should display textbook %>
		<% if (course.forumEnabled() && (role != User.STUDENT 
				|| !user.isBlockedFromForum(course.getId()))) { %>
		<td style="vertical-align:bottom; position:relative; bottom:-1px;">
			<%= makeTab(user.translate("Forum"), pathToAppRoot, "openForum();") %>
			<script type="text/javascript">
				// <!--
				setEvents('<%= toTabName(user.translateJS("Forum")) %>');
				// -->
			</script>
		</td>
		<% } // if should display forum %>
		<% if ((isInstructor || isTA) 
				&& course.getId() != AppConfig.tutorialId) { %>
		<td style="vertical-align:bottom; position:relative; bottom:-1px;">
			<%= makeTab(user.translate("Enrollment"), pathToAppRoot, "openEnrollment();") %>
			<script type="text/javascript">
				// <!--
				setEvents('<%= toTabName(user.translateJS("Enrollment")) %>');
				// -->
			</script>
		</td>
		<% } // if should display enrollment %>
	</tr>
	</table>
</div>
<!-- vim:filetype=jsp 
-->
