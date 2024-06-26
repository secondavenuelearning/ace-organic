<table summary="HTML to display reaction arrow with text above and below the arrow"> 
	<tr><td class="boldtext" style="text-align:center; vertical-align:top;">
		<%= above %>
	</td></tr>
	<tr><td style="text-align:center; vertical-align:middle; 
			font-size:<%= arrowSize %>px; line-height:<%= arrowSize / 4 %>px;">
		&rarr;
	</td></tr>
	<tr><td class="boldtext" style="padding-top:<%= arrowSize / 4 %>px; 
			text-align:center; vertical-align:bottom;">
		<%= below %>
	</td></tr>
</table>
