<!DOCTYPE html>
<html lang='en'>
<head>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" >
<title>ACE Gradebook</title>
<link rel="stylesheet" href="styles.css" type="text/css" />
<style>
img {border: 2px solid black; width: 90%;}
</style>
<script src="../../js/jslib.js" ></script>
<script >
// <!-- >

function change(name) {
	var value = getValue(name + 'Sel');
	setSrc(name, 'img/' + name + value + '.png');
}

// -->
</script>
</head>
<body>
<h3>The gradebook</h3>

<p>
When you press the <b>Gradebook</b> tab, you enter the overall gradebook for the
course.  The overall gradebook provides statistics on the performance 
of students on each assignment.  You can see the name of each assignment
by mousing over the assignment number, and you can choose to display the
grading parameters associated with an assignment.
Note also the button that allows you to export the
gradebook into a Microsoft Excel&reg; spreadsheet.
</p>

<select id="overallGradeSel" onchange="change('overallGrade');">
<option value="1">Overall gradebook: displaying an assignment's name</option>
<option value="2">Overall gradebook: displaying an assignment's grading parameters</option>
</select>
<p style="text-align:center;">
<img id="overallGrade" src="img/overallGrade1.png" alt="image"/>
</p>

<p>
If you click on the number of an assignment, you arrive at the detailed gradebook, where
you see every student's grade and number of attempts on each question in the
assignment plus statistics on the class's performance on each question.  You can click 
on the <b>attempt(s)</b> link to see a student's last response to a question.  You can click
on a student's name to see just that student's results; in the window that opens,
you can choose to alter the student's grade manually or comment on the response.  You can click
on a question number to launch a problem-solving window, into which you can paste the 
student's response and see how ACE responds to it.  ACE provides <b>Regrade</b> links at the
bottom of each question's column in case you discover and fix an error in how ACE
has been evaluating responses to the question.
</p>

<select id="detailedGradeSel" onchange="change('detailedGrade');">
<option value="1">Detailed gradebook</option>
<option value="2">Detailed gradebook: displaying a student's last response</option>
<option value="3">Detailed gradebook: choosing to display one student's results</option>
<option value="4">Detailed gradebook: displaying the student's last response</option>
<option value="5">Detailed gradebook: altering a student's grade or commenting</option>
<option value="6">Detailed gradebook: launching a problem-solving window</option>
<option value="7">Detailed gradebook: seeing how ACE evaluates a student's last response</option>
</select>
<p style="text-align:center;">
<img id="detailedGrade" src="img/detailedGrade1.png" alt="image"/>
</p>

<p>
The screen shots on this page show the gradebook from the point of view of an instructor
or a TA.  Ordinary students will see only their own grades.
</p>

<ul>
<li><a href="index.html">Index</a></li>
<li><a href="courseSetup.html">Setting up a course</a></li>
<li><a href="assgtSetup.html">Creating an assignment</a></li>
<li><a href="doAssgt.html">Working an assignment</a></li>
<li><a href="qTypes.jsp">Types of questions ACE can ask</a></li>
<li><a href="forum.html">Discussion forum and chat tool</a></li>
</ul>

</body>
</html>
