<!DOCTYPE html>
<html lang='en'>
<%@ page language="java" %>
<%@ page import="
	com.epoch.utils.Utils"
%>

<%@ include file="/includes/functions.inc.jsp.h" %>
<%
	final String pathToRoot = "../../";
%>
<head>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" >
<head>
<title>ACE Question types</title>
<meta http-equiv="Content-Script-Type" content="type"/>
<meta http-equiv="Content-Style-Type" content="text/css"/>
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<link rel="stylesheet" href="styles.css" type="text/css" />
<link rel="icon" href="<%= pathToRoot %>images/favicon.ico" type="image/x-icon"/>
<style type="text/css">
img {border: 2px solid black; width: 90%;}
</style>
<script src="../../js/jslib.js" ></script>
<script >
// <!-- >

function movePrev(qType) {
	move(qType, -1);
}

function moveNext(qType) {
	move(qType, 1);
}

function move(qType, increment) {
	var value = parseToInt(getValue(qType + 'Value'));
	var newValue = value + increment;
	var maxValue = parseToInt(getValue(qType + 'MaxValue'));
	if (newValue >= 0 && newValue < maxValue) {
		var newSrc = 'img/' + qType + newValue + '.png';
		setSrc(qType, newSrc);
		setValue(qType + 'Value', newValue);
		var qTypeNum = qTypeArr.indexOf(qType);
		var field = qType + 'DisplayValue';
		setInnerHTML(field, displayArrs[qTypeNum][newValue]);
	}
}

function init() {
	for (var qTypeNum = 0; qTypeNum < qTypeArr.length; qTypeNum++) {
		var qType = qTypeArr[qTypeNum];
		var field = qType + 'DisplayValue';
		setInnerHTML(field, displayArrs[qTypeNum][0]);
	}
}

var qTypeArr = [
		'lewis',
		'skelA',
		'skelB',
		'skelRGrp',
		'mech',
		'reson',
		'syn',
		'conform',
		'chair2D',
		'map',
		'OED',
		'RCD',
		'order',
		'choose',
		'fb',
		'click',
		'table',
		'num',
		'text'
		];

var displayArrs = [
		[
			'Lewis structure question',
			'Lewis structure response 1',
			'Lewis structure response 2',
			'Lewis structure response 3',
			'Lewis structure response 4',
			'Lewis structure response 5',
			'Lewis structure response 6'],
		[
			'Skeletal structure (spectroscopy) question',
			'Spectroscopy response 1',
			'Spectroscopy response 2',
			'Spectroscopy response 3',
			'Spectroscopy response 4',
			'Spectroscopy response 5'],
		[
			'Skeletal structure (reaction product) question',
			'Reaction product response 1',
			'Reaction product response 2',
			'Reaction product response 3',
			'Reaction product response 4',
			'Reaction product response 5'],
		[
			'R-group question in question list page',
			'R-group question ready to be solved',
			'R-group question correct response',
			'Answered R-group question in question list page',
			'Practice similar R-group question'],
		[
			'Mechanism question',
			'Mechanism response 1',
			'Mechanism response 2',
			'Mechanism response 3',
			'Mechanism response 4',
			'Mechanism response 5',
			'Mechanism response 6',
			'Calculated products of incorrect electron-flow arrows',
			'Mechanism response 7',
			'Mechanism response 8 (correct)',
			'Mechanism practice response (also correct)',
			'Mechanism practice response (also correct)',
			'Mechanism practice response (incorrect)'],
		[
			'Resonance question',
			'Resonance response 1',
			'Resonance response 2',
			'Resonance response 3',
			'Resonance response 4',
			'Resonance response 5'],
		[
			'Multistep synthesis question',
			'Selecting a reaction condition 1',
			'Selecting a reaction condition 2',
			'Selecting a reaction condition 3',
			'Selecting a reaction condition 4',
			'Multistep synthesis response 1',
			'Multistep synthesis response 2',
			'Multistep synthesis response 3',
			'Displaying calculated products',
			'Multistep synthesis response 4',
			'Multistep synthesis response 5',
			'Multistep synthesis response 6 (correct)',
			'Multistep synthesis practice response (also correct)'],
		[
			'3D Conformation question with chair template',
			'Adding axial and equatorial H atoms',
			'3D Chair template with axial and equatorial H atoms',
			'3D Conformation response 1',
			'3D Conformation response 2',
			'3D Conformation response 3',
			'3D Conformation response 4',
			'Rotation of correct structure in 3D'],
		[
			'2D Chair conformation question',
			'2D Chair conformation response 1',
			'2D Chair conformation response 2',
			'2D Chair conformation response 3'],
		[
			'Atom mapping question',
			'Atom mapping response 1',
			'Atom mapping response 2',
			'Atom mapping response 3',
			'Atom mapping response 4'],
		[
			'Orbital energy diagram question',
			'Orbital energy diagram response 1',
			'Orbital energy diagram response 2',
			'Orbital energy diagram response 3',
			'Orbital energy diagram response 4'],
		[
			'Reaction coordinate diagram question',
			'Reaction coordinate diagram response 1',
			'Reaction coordinate diagram response 2',
			'Reaction coordinate diagram response 3',
			'Reaction coordinate diagram response 4',
			'Reaction coordinate diagram response 5'],
		[
			'Ranking/ordering question',
			'Rearranging the options in order',
			'Ranking/ordering response 1',
			'Ranking/ordering response 2'],
		[
			'Multiple-choice question',
			'Multiple-choice response 1',
			'Multiple-choice response 2',
			'Multiple-choice response 3'],
		[
			'Fill-in-the-blank question',
			'Fill-in-the-blank response 1',
			'Fill-in-the-blank response 2',
			'Fill-in-the-blank response 3'],
		[
			'Clickable image question',
			'Clickable image response 1',
			'Clickable image response 2',
			'Clickable image response 3',
			'Clickable image response 4'],
		[
			'Table question',
			'Table response 1',
			'Table response 2'],
		[
			'Numeric question',
			'Numeric response 1',
			'Numeric response 2',
			'Numeric response 3',
			'Numeric response 4 (correct)',
			'Numeric practice response (correct)'],
		[
			'Text question',
			'Text response 1',
			'Text response 2',
			'Text response 3',
			'Text response 4',
			'Text response 5']
		];

// -->
</script>
</head>
<body onload="init();">
<h3>Types of questions ACE can ask</h3>

<p>
Over the years, we have added more and more capabilities to ACE,
so ACE can now ask almost any type of question that an instructor 
of organic chemistry would like to ask.  The list now includes:
</p>

<ul><li><a href="#lewisStruc">Lewis structure</a>,
</li><li><a href="#skelQs">draw-structure</a> 
(including <a href="#Rgroup">R-group</a>),
</li><li><a href="#mechan">mechanism</a> 
(including <a href="#resonance">resonance structure</a>),
</li><li><a href="#synth">multistep synthesis</a>, 
</li><li><a href="#conformn">conformation</a>,
</li><li><a href="#mapping">atom mapping/labeling</a>,
</li><li><a href="#OEDiag">orbital energy diagram</a>, 
</li><li><a href="#RCDiag">reaction coordinate diagram</a>, 
</li><li><a href="#rank">ordering/ranking</a>,
</li><li><a href="#mc">multiple-choice</a>,
</li><li><a href="#fillBlank">fill-in-the-blank</a>,
</li><li><a href="#clickable">clickable image</a>,
</li><li><a href="#tabular">complete-the-table</a>,
</li><li><a href="#numeric">numeric</a>, and
</li><li><a href="#textual">text</a>.
</li></ul>

<p>Use the arrow buttons to scroll through typical responses and feedback 
for each question type.

<a id="lewisStruc"></a>
<hr/>
<h3>Lewis structure questions</h3>

<p>
ACE can ask students to draw the best Lewis structure of a compound.  Students 
use a structure-drawing program called Lewis JS that we have devised 
for the purpose of drawing Lewis structures.  Students
need to include the correct number of unshared electrons on each atom and the 
correct formal charges for ACE to mark the structure as correct.  Look through
the responses that you can access through the pulldown menu to see
the different kinds of mistakes that students may make and
how ACE provides appropriate feedback that addresses those mistakes without giving
away the answer.
</p>

<table>
<tr><td>
<%= makeButtonIcon("back", pathToRoot, "movePrev('lewis');") %>
</td><td>
<%= makeButtonIcon("next", pathToRoot, "moveNext('lewis');") %>
</td><td id="lewisDisplayValue">
</td></tr></table>
<p class="centered">
<img style="height:80%; width:80%;" id="lewis" src="img/lewis0.png" alt="lewis"/>
<input type="hidden" name="lewisValue" id="lewisValue" value="0">
<input type="hidden" name="lewisMaxValue" id="lewisMaxValue" value="7">
</p>

<a id="skelQs"></a>
<hr/>
<h3>Draw-structure questions</h3>

<p>
Draw-structure questions, the most common kind of question in ACE, ask students
to draw a structure or a small group of structures.  Students use the 
structure-drawing programs 
<a href="https://www.chemaxon.com/products/marvin/marvin-js/" 
target="window2">MarvinJS</a>, 
which was developed by <a href="http://www.chemaxon.com">ChemAxon</a>, a Hungarian
cheminformatics company; you may be familiar with it from its 
incorporation into numerous other chemistry tools, such as 
<a href="http://www.reaxys.com">Reaxys</a>.
Typical skeletal questions include, "Draw the product of the following reaction,"
or, "Draw the structure that gives rise to the following spectra," or, "Draw all
stereoisomers of the given structure."  
</p>

<table>
<tr><td>
<%= makeButtonIcon("back", pathToRoot, "movePrev('skelA');") %>
</td><td>
<%= makeButtonIcon("next", pathToRoot, "moveNext('skelA');") %>
</td><td id="skelADisplayValue">
</td></tr>
</table>
<p class="centered">
<img style="height:80%; width:80%;"id="skelA" src="img/skelA0.png" alt="skelA"/>
<input type="hidden" name="skelAValue" id="skelAValue" value="0">
<input type="hidden" name="skelAMaxValue" id="skelAMaxValue" value="6">
</p>

<table>
<tr><td>
<%= makeButtonIcon("back", pathToRoot, "movePrev('skelB');") %>
</td><td>
<%= makeButtonIcon("next", pathToRoot, "moveNext('skelB');") %>
</td><td id="skelBDisplayValue">
</td></tr>
</table>
<p class="centered">
<img style="height:80%; width:80%;"id="skelB" src="img/skelB0.png" alt="skelB"/>
<input type="hidden" name="skelBValue" id="skelBValue" value="0">
<input type="hidden" name="skelBMaxValue" id="skelBMaxValue" value="6">
</p>

<a id="Rgroup"></a>
<p>
R-group questions are a special class of draw-structure questions.  In
an R-group question, the question author has drawn a figure that contains a
generic R group.  When ACE displays this figure to a student, it randomly
chooses a specific R group to substitute for the author's generic R group.
After the student answers the question correctly, the student can choose
to practice a similar question, in which ACE substitutes yet another
specific R group.
</p>

<table>
<tr><td>
<%= makeButtonIcon("back", pathToRoot, "movePrev('skelRGrp');") %>
</td><td>
<%= makeButtonIcon("next", pathToRoot, "moveNext('skelRGrp');") %>
</td><td id="skelRGrpDisplayValue">
</td></tr>
</table>
<p class="centered">
<img style="height:80%; width:80%;"id="skelRGrp" src="img/skelRGrp0.png" alt="skelRGrp"/>
<input type="hidden" name="skelRGrpValue" id="skelRGrpValue" value="0">
<input type="hidden" name="skelRGrpMaxValue" id="skelRGrpMaxValue" value="5">
</p>

<a id="mechan"></a>
<hr/>
<h3>Multistep mechanism questions</h3>

<p>
We are very proud of ACE's ability to ask students to draw multistep mechanisms.
Again, the format of the response is just like what a student would draw on
paper (with the exception of the boxes enclosing each step), and again,
ACE is able to analyze the entire mechanism at once and provide appropriate
feedback.  Note especially how ACE understands that there are often many correct
ways to draw a mechanism for a particular reaction: one, another, or several resonance
structures of intermediates may be drawn, uninteresting coproducts may or may
not be omitted, etc.  (Compare the correct responses.)  At the same time, 
ACE understands that some mechanisms that may superficially appear to be
correct are in fact not reasonable.  (See the last response.)
</p>

<table>
<tr><td>
<%= makeButtonIcon("back", pathToRoot, "movePrev('mech');") %>
</td><td>
<%= makeButtonIcon("next", pathToRoot, "moveNext('mech');") %>
</td><td id="mechDisplayValue">
</td></tr>
</table>
<p class="centered">
<img style="height:80%; width:80%;"id="mech" src="img/mech0.png" alt="mech"/>
<input type="hidden" name="mechValue" id="mechValue" value="0">
<input type="hidden" name="mechMaxValue" id="mechMaxValue" value="13">
</p>

<a id="resonance"></a>
<p>
Resonance structure questions are a subset of mechanism questions.  Again,
ACE asks students to draw structures and electron-flow arrows, and it
provides appropriate feedback for different kinds of mistakes.
</p>

<table>
<tr><td>
<%= makeButtonIcon("back", pathToRoot, "movePrev('reson');") %>
</td><td>
<%= makeButtonIcon("next", pathToRoot, "moveNext('reson');") %>
</td><td id="resonDisplayValue">
</td></tr>
</table>
<p class="centered">
<img style="height:80%; width:80%;"id="reson" src="img/reson0.png" alt="reson"/>
<input type="hidden" name="resonValue" id="resonValue" value="0">
<input type="hidden" name="resonMaxValue" id="resonMaxValue" value="6">
</p>

<a id="synth"></a>
<hr/>
<h3>Multistep synthesis questions</h3>

<p>
Multistep synthesis questions are similar to mechanism questions in
that students need to connect a series of structures in a logical way.
They are different in that the logic that connects the structures
in multistep syntheses is synthetic transformations, not
electron-flow arrows.  For each synthetic step, ACE has 
students choose from a set of over 100 reaction conditions.  A 
multistep synthesis is correct if the synthesis produces the requested
target compound, if the starting materials all obey the rules established
by the question author (number of C atoms, functional groups, formula,
etc.), and if the reactions chosen by the student do indeed convert each
compound in each step to at least one compound shown in the subsequent
step.  Note that ACE understands that a synthesis is incorrect if it 
gives the incorrect diastereomer.  Note also that ACE understands that
two completely different approaches to the same target can both be
perfectly acceptable.
</p>

<table>
<tr><td>
<%= makeButtonIcon("back", pathToRoot, "movePrev('syn');") %>
</td><td>
<%= makeButtonIcon("next", pathToRoot, "moveNext('syn');") %>
</td><td id="synDisplayValue">
</td></tr>
</table>
<p class="centered">
<img style="height:80%; width:80%;"id="syn" src="img/syn0.png" alt="syn"/>
<input type="hidden" name="synValue" id="synValue" value="0">
<input type="hidden" name="synMaxValue" id="synMaxValue" value="13">
</p>

<a id="conformn"></a>
<hr/>
<h3>Conformation questions</h3>

<p>
In a 3D conformation question, a student draws a chair cyclohexane or a sawhorse 
in a requested conformation.  Students begin with a template that already 
contains 3D coordinate information and have to place 
substituents in their correct 3D positions.  One
very nice feature of MarvinJS is its ability to rotate 
these structures in three dimensions.  
</p>

<table>
<tr><td>
<%= makeButtonIcon("back", pathToRoot, "movePrev('conform');") %>
</td><td>
<%= makeButtonIcon("next", pathToRoot, "moveNext('conform');") %>
</td><td id="conformDisplayValue">
</td></tr>
</table>
<p class="centered">
<img style="height:80%; width:80%;"id="conform" src="img/conform0.png" alt="conform"/>
<input type="hidden" name="conformValue" id="conformValue" value="0">
<input type="hidden" name="conformMaxValue" id="conformMaxValue" value="8">
</p>

<p>
ACE also provides students the opportunity to practice drawing 2D 
chair projections.  ACE analyzes 2D chairs
by calculating the angles between certain lines in the drawing.
</p>

<table>
<tr><td>
<%= makeButtonIcon("back", pathToRoot, "movePrev('chair2D');") %>
</td><td>
<%= makeButtonIcon("next", pathToRoot, "moveNext('chair2D');") %>
</td><td id="chair2DDisplayValue">
</td></tr>
</table>
<p class="centered">
<img style="height:80%; width:80%;"id="chair2D" src="img/chair2D0.png" alt="chair2D"/>
<input type="hidden" name="chair2DValue" id="chair2DValue" value="0">
<input type="hidden" name="chair2DMaxValue" id="chair2DMaxValue" value="4">
</p>

<a id="mapping"></a>
<hr/>
<h3>Atom labeling/mapping questions</h3>

<p>In an atom mapping question, ACE asks students to label one or more
atoms according to a particular property (e.g., hybridization, NMR topicity 
or multiplicity, or acidity).
</p>

<table>
<tr><td>
<%= makeButtonIcon("back", pathToRoot, "movePrev('map');") %>
</td><td>
<%= makeButtonIcon("next", pathToRoot, "moveNext('map');") %>
</td><td id="mapDisplayValue">
</td></tr>
</table>
<p class="centered">
<img style="height:80%; width:80%;"id="map" src="img/map0.png" alt="map"/>
<input type="hidden" name="mapValue" id="mapValue" value="0">
<input type="hidden" name="mapMaxValue" id="mapMaxValue" value="5">
</p>

<a id="OEDiag"></a>
<hr/>
<h3>Orbital energy diagram questions</h3>

<p> In orbital energy diagrams, students place orbitals in three
columns.  The students choose the number, type, relative
energies, and occupancies of the orbitals, and they correlate atomic orbitals 
to molecular ones.  The question author determines the size of the canvas.
</p>

<table>
<tr><td>
<%= makeButtonIcon("back", pathToRoot, "movePrev('OED');") %>
</td><td>
<%= makeButtonIcon("next", pathToRoot, "moveNext('OED');") %>
</td><td id="OEDDisplayValue">
</td></tr>
</table>
<p class="centered">
<img style="height:80%; width:80%;"id="OED" src="img/OED0.png" alt="OED"/>
<input type="hidden" name="OEDValue" id="OEDValue" value="0">
<input type="hidden" name="OEDMaxValue" id="OEDMaxValue" value="7">
</p>

<a id="RCDiag"></a>
<hr/>
<h3>Reaction coordinate diagram questions</h3>

<p> In reaction coordinate diagrams, students place maxima
or minima (states) in columns.  The students choose the number of 
states, their relative energies, and their labels, and they correlate
each state to states in the adjacent columns.  The question author 
determines the number of rows and columns in the canvas.
</p>

<table>
<tr><td>
<%= makeButtonIcon("back", pathToRoot, "movePrev('RCD');") %>
</td><td>
<%= makeButtonIcon("next", pathToRoot, "moveNext('RCD');") %>
</td><td id="RCDDisplayValue">
</td></tr>
</table>
<p class="centered">
<img style="height:80%; width:80%;"id="RCD" src="img/RCD0.png" alt="RCD"/>
<input type="hidden" name="RCDValue" id="RCDValue" value="0">
<input type="hidden" name="RCDMaxValue" id="RCDMaxValue" value="6">
</p>

<a id="rank"></a>
<hr/>
<h3>Ordering/ranking questions</h3>

<p>Ordering/ranking questions are useful for querying students about
rates, energy, and acidity.
</p>

<table>
<tr><td>
<%= makeButtonIcon("back", pathToRoot, "movePrev('order');") %>
</td><td>
<%= makeButtonIcon("next", pathToRoot, "moveNext('order');") %>
</td><td id="orderDisplayValue">
</td></tr>
</table>
<p class="centered">
<img style="height:80%; width:80%;"id="order" src="img/order0.png" alt="order"/>
<input type="hidden" name="orderValue" id="orderValue" value="0">
<input type="hidden" name="orderMaxValue" id="orderMaxValue" value="4">
</p>

<a id="mc"></a>
<hr/>
<h3>Multiple-choice questions</h3>

<p>Multiple-choice questions can allow the student to select only a
single option, or, more interestingly, can allow them to select any number 
of options.
</p>

<table>
<tr><td>
<%= makeButtonIcon("back", pathToRoot, "movePrev('choose');") %>
</td><td>
<%= makeButtonIcon("next", pathToRoot, "moveNext('choose');") %>
</td><td id="chooseDisplayValue">
</td></tr>
</table>
<p class="centered">
<img style="height:80%; width:80%;"id="choose" src="img/choose0.png" alt="choose"/>
<input type="hidden" name="chooseValue" id="chooseValue" value="0">
<input type="hidden" name="chooseMaxValue" id="chooseMaxValue" value="4">
</p>

<a id="fillBlank"></a>
<hr/>
<h3>Fill-in-the-blank questions</h3>

<p>
In a fill-in-the-blank question, students use pulldown menus to complete
a sentence.  It's essentially a multiple-choice question, but with
<i>n</i><sup>2</sup> possible answers instead of <i>n</i> or <i>n!</i>.
</p>

<table>
<tr><td>
<%= makeButtonIcon("back", pathToRoot, "movePrev('fb');") %>
</td><td>
<%= makeButtonIcon("next", pathToRoot, "moveNext('fb');") %>
</td><td id="fbDisplayValue">
</td></tr>
</table>
<p class="centered">
<img style="height:80%; width:80%;"id="fb" src="img/fb0.png" alt="fb"/>
<input type="hidden" name="fbValue" id="fbValue" value="0">
<input type="hidden" name="fbMaxValue" id="fbMaxValue" value="4">
</p>

<a id="clickable"></a>
<hr/>
<h3>Clickable image questions</h3>

<p>
A clickable image question asks the student to click somewhere on an image.
ACE marks where the student clicks with a red X.
</p>

<table>
<tr><td>
<%= makeButtonIcon("back", pathToRoot, "movePrev('click');") %>
</td><td>
<%= makeButtonIcon("next", pathToRoot, "moveNext('click');") %>
</td><td id="clickDisplayValue">
</td></tr>
</table>
<p class="centered">
<img style="height:80%; width:80%;"id="click" src="img/click0.png" alt="click"/>
<input type="hidden" name="clickValue" id="clickValue" value="0">
<input type="hidden" name="clickMaxValue" id="clickMaxValue" value="5">
</p>

<a id="tabular"></a>
<hr/>
<h3>Complete-the-table questions</h3>

<p>
ACE can evaluate tables containing text and numerical values.  The
question author sets the tolerances within which ACE will accept numerical
values.  ACE can assign a grade based on the percentage of cells into which
the student has entered an incorrect value.
</p>

<table>
<tr><td>
<%= makeButtonIcon("back", pathToRoot, "movePrev('table');") %>
</td><td>
<%= makeButtonIcon("next", pathToRoot, "moveNext('table');") %>
</td><td id="tableDisplayValue">
</td></tr>
</table>
<p class="centered">
<img style="height:80%; width:80%;"id="table" src="img/table0.png" alt="table"/>
<input type="hidden" name="tableValue" id="tableValue" value="0">
<input type="hidden" name="tableMaxValue" id="tableMaxValue" value="3">
</p>

<a id="numeric"></a>
<hr/>
<h3>Numeric questions</h3>

<p>
ACE understands the concept of significant figures.
If the number that the student is calculating has dimensions, it is up to the
question author to determine how many choices of dimension to offer the student.
</p>

<table>
<tr><td>
<%= makeButtonIcon("back", pathToRoot, "movePrev('num');") %>
</td><td>
<%= makeButtonIcon("next", pathToRoot, "moveNext('num');") %>
</td><td id="numDisplayValue">
</td></tr>
</table>
<p class="centered">
<img style="height:80%; width:80%;"id="num" src="img/num0.png" alt="num"/>
<input type="hidden" name="numValue" id="numValue" value="0">
<input type="hidden" name="numMaxValue" id="numMaxValue" value="6">
</p>

<a id="textual"></a>
<hr/>
<h3>Text questions</h3>

<p>
Not surprisingly, ACE can also collect text answers, which it can
analyze by simple string searches. 
</p>

<table>
<tr><td>
<%= makeButtonIcon("back", pathToRoot, "movePrev('text');") %>
</td><td>
<%= makeButtonIcon("next", pathToRoot, "moveNext('text');") %>
</td><td id="textDisplayValue">
</td></tr>
</table>
<p class="centered">
<img style="height:80%; width:80%;"id="text" src="img/text0.png" alt="text"/>
<input type="hidden" name="textValue" id="textValue" value="0">
<input type="hidden" name="textMaxValue" id="textMaxValue" value="6">
</p>

<ul>
<li><a href="index.html">Index</a></li>
<li><a href="courseSetup.html">Setting up a course</a></li>
<li><a href="assgtSetup.html">Creating an assignment</a></li>
<li><a href="doAssgt.html">Working an assignment</a></li>
<li><a href="grades.jsp">The gradebook</a></li>
<li><a href="forum.html">Discussion forum and chat tool</a></li>
</ul>

</body>
</html>
