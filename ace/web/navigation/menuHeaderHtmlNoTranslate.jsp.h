<div id="topNavBar">
	<table class="light" summary="" style="width:100%; height:43px; 
			border-bottom-style:solid; border-color:black; 
			border-width:1px; margin:0px; text-align:left;">
	<tr>
	<td style="width:100%; padding-left:10px;">
		<img src="<%= pathToAppRoot %>images/acelogo2.jpg" alt="ace logo"/>
	</td> 
	<td>
		<table summary="">
		<tr>
			<% if (realRole == User.ADMINISTRATOR) { %>
				<td style="padding-left:15px;">
					<%= makeButton("Admin Tool", "goToAdmin();") %>
				</td>
			<% } %>
			<% if (role != User.ADMINISTRATOR) { %>
				<td style="padding-left:15px;">
					<%= makeButton("My Courses", "goHome();") %>
				</td>
			<% } %>
			<td style="padding-left:15px;">
				<%= makeButton("My Profile", "editProfile();") %>
			</td>
			<% if (role != User.STUDENT && user.isEnabled()) { %>
				<td style="padding-left:15px;">
					<%= makeButton("Question Bank", "openQBE();") %>
				</td>
				<% if (role != User.ADMINISTRATOR) { %>
					<td style="padding-left:15px;">
						<%= makeButton("Textbooks", "openTexts();") %>
					</td>
				<% } %>
			<% } %>
			<% if (role != User.STUDENT 
					&& ((StudentSession) userSess).getNumCourses() > 1) { %>
				<td style="padding-left:15px;">
					<%= makeButton("Crosscourse reports", "studentReports();") %>
				</td>
			<% } %>
			<td style="padding-left:15px;">
				<%= makeButton("Logout", "logout();") %>
			</td>
		</tr>
		</table>
	</td>
	</tr>
	</table>
</div>
<!-- vim:filetype=jsp
-->
