/* Modified from http://www.elated.com/articles/creating-a-javascript-clock/
Include this file in the Javascript section of a page that needs a time
or countdown clock.  The including page must contain an empty HTML
element with id="clockRemaining", and possibly one with id="clockSince".
In the body onload attribute, include:
		setUpClock();
In the Javascript block, include:
		<%@ include file="/js/setUpClock.jsp.h" %>
*/

/*jsl:option explicit*/

// <!-- avoid parsing the following as HTML
var msecNow; // number of milliseconds since the Epoch until now
var msecDue; // number of milliseconds since the Epoch until the assignment is due
var msecLeft; // number of milliseconds remaining until the assignment is due
var INDEFINITE = 'indefinite';
var showTimeRemaining = false;
var indefiniteWritten = false;
var ONE_SEC = 1000;

/* Called by setUpClock() to set starting values and translated text. */
function setClockConstants(now, due, translatedINDEFINITE) {
	"use strict";
	msecNow = now;
	msecDue = due;
	msecLeft = msecDue - msecNow;
	INDEFINITE = translatedINDEFINITE;
} // setClockConstants()

/* Updates the time left until the due time. */
function updateTimeLeft() {
	"use strict";
	var clock = document.getElementById('clockRemaining'),
		timeLeftBld,
		ONE_MIN,
		ONE_HR,
		hrLeft,
		hrRemainderLeft,
		minLeft,
		secLeft,
		fracMinLeft,
		START_R,
		END_R,
		START_G,
		END_G,
		fracHalfMinLeft,
		rInt,
		gInt,
		rStr,
		gStr;
	if (clock && showTimeRemaining && !indefiniteWritten) {
		timeLeftBld = new String.builder().
				append('<a href="javascript:hideClockRemaining();">');
		if (msecDue === 0) {
			indefiniteWritten = true;
			timeLeftBld.append('<span style="color:green;">').
					append(INDEFINITE).append('<\/span>');
		} else if (msecLeft > 0) {
			ONE_MIN = ONE_SEC * 60;
			ONE_HR = ONE_MIN * 60;
			hrLeft = Math.floor(msecLeft / ONE_HR);
			hrRemainderLeft = msecLeft % ONE_HR;
			minLeft = Math.floor(hrRemainderLeft / ONE_MIN);
			secLeft = Math.floor((hrRemainderLeft % ONE_MIN) / ONE_SEC);
			timeLeftBld.append('<span style="color:');
			if (msecLeft < ONE_MIN) {
				fracMinLeft = msecLeft / ONE_MIN;
				START_R = (fracMinLeft >= 0.5 ? 0x00 : 0xCC);
				END_R = (fracMinLeft >= 0.5 ? 0xCC : 0xFF);
				START_G = (fracMinLeft >= 0.5 ? 0x80 : 0xCC);
				END_G = (fracMinLeft >= 0.5 ? 0xCC : 0x00);
				fracHalfMinLeft = (fracMinLeft * 2) % 1;
				rInt = Math.round(END_R - (END_R - START_R) * fracHalfMinLeft);
				gInt = Math.round(END_G - (END_G - START_G) * fracHalfMinLeft);
				rStr = rInt.toString(16);
				gStr = gInt.toString(16);
				timeLeftBld.append('#');
				if (rStr.length < 2) {
					timeLeftBld.append('0');
				}
				timeLeftBld.append(rStr);
				if (gStr.length < 2) {
					timeLeftBld.append('0');
				}
				timeLeftBld.append(gStr).append('00');
			} else { // lots of time left
				timeLeftBld.append('green');
			}
			timeLeftBld.append(';">').append(hrLeft).
					append(minLeft < 10 ? ':0' : ':').append(minLeft).
					append(secLeft < 10 ? ':0' : ':').append(secLeft).
					append('<\/span>');
		} else {
			timeLeftBld.append('<span style="color:red;">0:00:00<\/span>');
		} // if time is expired
		timeLeftBld.append('<\/a>');
		clock.innerHTML = timeLeftBld.toString();
	} // if time-remaining clock exists and should be shown, definite due date
	msecLeft -= ONE_SEC;
} // updateTimeLeft()

/* Turns on the flag to show the time-remaining clock. */
function showClockRemaining() {
	"use strict";
	showTimeRemaining = true;
} // showClockRemaining()

/* Hides the time-remaining clock. */
function hideClockRemaining() {
	"use strict";
	var clock = document.getElementById('clockRemaining');
	if (clock) {
		clock.innerHTML =
				'<span class="regtext" style="color:green;">' +
				'<a href="javascript:showClockRemaining();" style="font-size:32px;">' +
				'&#9200;<\/a><\/span>';
	} // if clock exists
	showTimeRemaining = false;
} // hideClockRemaining()

// --> end HTML comment
