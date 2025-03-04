<div id="topNavBar">
	<table summary="" class="light" style="width:100%; height:43px; 
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
					<%= makeButton(user.translate("Admin Tool"), "goToAdmin();") %>
				</td>
			<% } %>
			<% if (role != User.ADMINISTRATOR) { %>
				<td style="padding-left:15px;">
					<%= makeButton(user.translate("My Courses"), "goHome();") %>
				</td>
			<% } %>
			<td style="padding-left:15px;">
				<%= makeButton(user.translate("My Profile"), "editProfile();") %>
			</td>
			<% if (role != User.STUDENT && user.isEnabled()) { %>
				<td style="padding-left:15px;">
					<%= makeButton(user.translate("Question Bank"), "openQBE();") %>
				</td>
				<% if (role != User.ADMINISTRATOR) { %>
					<td style="padding-left:15px;">
						<%= makeButton(user.translate("Textbooks"), "openTexts();") %>
					</td>
				<% } %>
			<% } %>
			<% if (role != User.STUDENT 
					&& ((StudentSession) userSess).getNumCourses() > 1) { %>
				<td style="padding-left:15px;">
					<%= makeButton(user.translate("Crosscourse reports"), 
							"studentReports();") %>
				</td>
			<% } %>
			<td style="padding-left:15px;">
				<%= makeButton(user.translate("Logout"), "logout();") %>
			</td>
		</tr>
		</table>
	</td>
	</tr>
	</table>
</div>
<!-- vim:filetype=jsp
-->
