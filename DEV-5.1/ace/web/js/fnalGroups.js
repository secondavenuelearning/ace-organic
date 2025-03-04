// <!-- avoid parsing the following as HTML
/* Included wherever we need to choose functional groups from a pulldown
menu.
Calling page should have JS global variable showCategoryWarning, HTML element
groupsPopup.
It should also have this code called by the body onload property:
	<% for (int grpNum = 1; grpNum <= sortedGroups.length; grpNum++) {
		final FnalGroupDef group = sortedGroups[grpNum - 1]; %>
		setArrayValues(<%= grpNum %>,
				'<%= Utils.toValidJS(group.getPulldownName()) %>',
				'<%= Utils.toValidJS(group.category) %>',
				<%= group.groupId %>);
	<% } // for each group %>
	initFnalGroupConstants(parseInt(form.initGroupId.value));
	setCatSelector();
	initializeGroupSelector(form);
*/

/*jsl:option explicit*/
/*jsl:import jslib.js*/

// all Javascript arrays are 1-based
var fnalGroups = { // file-local, global variables
	groupDisplayNames: [],
	categoryNames: [],
	groupIds: [],
	numCats: 0,
	namesCats: [],
	numGroupsInCat: [],
	prevgrpCat: '',
	initialGroupId: 0,
	initialGroupSortNum: 0
};

function initFnalGroupConstants(initialGrpId) {
	"use strict";
	// alert('initialGrpId = ' + initialGrpId);
	var grpNum;
	fnalGroups.initialGroupId = initialGrpId;
	for (grpNum = 1; grpNum < fnalGroups.groupIds.length; grpNum += 1) {
		if (fnalGroups.groupIds[grpNum] === fnalGroups.initialGroupId) {
			fnalGroups.initialGroupSortNum = grpNum;
		}
		if (fnalGroups.categoryNames[grpNum] !== fnalGroups.prevgrpCat) {
			fnalGroups.numCats += 1;
			fnalGroups.numGroupsInCat[fnalGroups.numCats] = 1;
			fnalGroups.namesCats[fnalGroups.numCats] =
				fnalGroups.categoryNames[grpNum];
		} else {
			fnalGroups.numGroupsInCat[fnalGroups.numCats] += 1;
		} // if grpCat
		fnalGroups.prevgrpCat = fnalGroups.categoryNames[grpNum];
	} // for each group
} // initFnalGroupConstants()

function setArrayValues(grpNum, grpName, catName, grpId) {
	"use strict";
	fnalGroups.groupDisplayNames[grpNum] = grpName;
	fnalGroups.categoryNames[grpNum] = catName;
	fnalGroups.groupIds[grpNum] = grpId;
} // setArrayValues()

function setCatSelector() {
	"use strict";
	var bld = new String.builder().
			append('<option value="0">No category selected</option>'),
		cat;
	for (cat = 1; cat <= fnalGroups.numCats; cat += 1) {
		bld.append('<option value=').append(cat).
				append('>').append(fnalGroups.namesCats[cat]).append('</option>');
	}
	setInnerHTML('categories', bld.toString());
} // setCatSelector()

function setCategoryWarning(category) {
	"use strict";
	setInnerHTML('categoryWarning',
			'&nbsp;' + (category === 'Aromatics' ?
				'<br \/>Warning: ACE can correctly determine the presence ' +
					'or absence of groups in this category, but it cannot ' +
					'count them correctly.  Use with caution.<br\/>&nbsp;' :
				''));
} // setCategoryWarning()

function setGroupWarning(groupSelector) {
	"use strict";
	if (document.getElementById('categoryWarning')) {
		// var groupText = groupSelector.options[groupSelector.selectedIndex].text;
		//    groupText is not used?  Raphael 7/2015
		setInnerHTML('categoryWarning', '&nbsp;');
	}
} // setGroupWarning()

// called by loadGroupsInChangedCat() and initializeGroupSelector()
function loadGroupsInCat(form, catNum, isThereInitialGroup) {
	"use strict";
	// find where this category starts among the groups
	var start = 1,
		out,
		cat,
		grpNum;
	for (cat = 1; cat < catNum; cat += 1) {
		if (cat === catNum) {
			break;
		}
		start += fnalGroups.numGroupsInCat[cat];
	}
	// set the options in the popup
	out = new String.builder();
	out.append('<select id="fnalGroupId" name="fnalGroupId" ' +
				'size="1" style="width:300px;"').
			append('><option value="0">No group selected</option>');
	form.fnalGroupId.length = fnalGroups.numGroupsInCat[catNum] + 1;
	for (grpNum = start;
			grpNum < (start + fnalGroups.numGroupsInCat[catNum]);
			grpNum += 1) {
		out.append('<option value="').append(fnalGroups.groupIds[grpNum]).
			append('"');
		if (isThereInitialGroup &&
				(fnalGroups.groupIds[grpNum] === fnalGroups.initialGroupId)) {
			out.append(' selected="selected"');
		}
		out.append('>').append(fnalGroups.groupDisplayNames[grpNum]);
	} // for grpNum
	out.append('</select>');
	setInnerHTML('groupsPopup', out.toString());
	// if (fnalGroups.namesCats[catNum] === 'CH only') setGroupWarning(form.fnalGroupId);
} // loadGroupsInCat()

function initializeGroupSelector(form) {
	"use strict";
	var initCatNum = 0,
		cat;
	if (fnalGroups.initialGroupId >= 1) {
		// find and set initCategory index
		for (cat = 1; cat <= fnalGroups.numCats; cat += 1) {
			if (fnalGroups.namesCats[cat] ===
					fnalGroups.categoryNames[fnalGroups.initialGroupSortNum]) {
				// alert('category ' + cat + ', ' + fnalGroups.namesCats[cat]);
				form.categories.selectedIndex = cat;
				initCatNum = cat;
				break;
			} // if fnalGroups.namesCats[cat]
		} // for cat
		// alert('initCatNum = ' + initCatNum);
		setCategoryWarning(
			fnalGroups.categoryNames[fnalGroups.initialGroupSortNum]
		);
		loadGroupsInCat(form, initCatNum, true);
	} // if fnalGroups.initialGroupId >= 1
} // initializeGroupSelector(form)

function loadGroupsInChangedCat(form) {
	"use strict";
	var selCatNum = form.categories.selectedIndex;
	// load category number selCatNum
	form.fnalGroupId.length = 1;
	form.fnalGroupId.options[0].value = 0;
	form.fnalGroupId.options[0].text = 'No group selected';
	if (showCategoryWarning) {
		setCategoryWarning(form.categories.options[selCatNum].text);
	}
	if (selCatNum !== 0) {
		loadGroupsInCat(form, selCatNum, false);
	}
} // loadGroupsInChangedCat(form)

// --> end HTML comment
