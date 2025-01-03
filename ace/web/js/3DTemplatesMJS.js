// <!-- avoid parsing the following as HTML
/*jsl:option explicit*/
/*jsl:import jslib.js*/

// returns an array of JSONs describing buttons that place 3D structures on the
// canvas
function get3DButtons()
{ return [
{'structure' : '<cml>' +
		'<MDocument>' +
		'<MChemicalStruct>' +
		'<molecule molID="m1">' +
		'<atomArray atomID="a1 a2 a3 a4 a5 a6" ' +
			'elementType="C C C C C C" ' +
			'mrvMap="1 2 3 4 5 6" ' +
			'x3="1.048566666428248 ' +
				'-0.384633333571752 ' +
				'-1.346333333571752 ' +
				'-0.9605333335717523 ' +
				'0.47266666642824806 ' +
				'1.4342666664282482" ' +
			'y3="-0.3266666673819224 ' +
				'-0.08946666738192244 ' +
				'-0.26396666738192237 ' +
				'0.5906333326180775 ' +
				'0.3535333326180776 ' +
				'0.5279333326180775" ' +
			'z3="0.96 1.3814 0.22699999999999998 -0.96 -1.3814 -0.22700000000000004"/>' +
		'<bondArray>' +
		'<bond id="b1" atomRefs2="a1 a2" order="1"/>' +
		'<bond id="b2" atomRefs2="a1 a6" order="1"/>' +
		'<bond id="b3" atomRefs2="a2 a3" order="1"/>' +
		'<bond id="b4" atomRefs2="a3 a4" order="1"/>' +
		'<bond id="b5" atomRefs2="a4 a5" order="1"/>' +
		'<bond id="b6" atomRefs2="a5 a6" order="1"/>' +
		'</bondArray>' +
		'</molecule>' +
		'</MChemicalStruct>' +
		'</MDocument>' +
		'</cml>',
'name' : 'chair left',
'icon' : 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB4AAAAUCAYAAACaq43EAA' +
		'AACXBIWXMAAAP0AAAD9AEnGM3WAAAB+npUWHRtb2xTb3VyY2UAAHicfVTbbqMwE' +
		'P2VkZ8X33FMFFJVW1Wq1Lxsu9K+RQ44KVLAEZDbfv0OpNtAq8Y8MDo+Z2Y8M' +
		'/bs7lRu4eDrpghVSgTlBHyVhbyoNin5/foYWXI3n2VIQmLVpOStbXdTx' +
		'o7HI83efOlOoaJZKMllf3pqihHnqGioN0xyLtifxfNLr4mKqmldlXlUNcW06cHnkLm2' +
		'z+KbEKx09aGo2IXOyvpw8bYUemmXmp6anFyP8hO37lEJ62LrYR3q0rVwEJpaqn/Axl' +
		'e+dq3PYXXu0QRxTuazxUPI9qWvWjQ7F0Xmti9tvc8QKMPWZ3v0hsbTQ0pKgQLXhvK' +
		'+rt0ZOquDnQAnwSlwGlwMzmBJt77z+XreecwMPj4CeIqF22HlQYICDTEg/aRSEnFqV' +
		'CwktxOhLVfcQCQpN1aNIEXxzz+xjNZyAGmIBJVCDGkCMIDUiR5gBM4YmFMEjI6tV' +
		'JPExAmXIKjg1g4gAZwmSqsBpJA1sTa5QgjEsbCfPE2kkVeIwN8+ZGJwS1mh0' +
		'bGUJhku26WKhOid0SUuJ3y4NDaCfXRiPluFKh+YUOQpWQnSt+iXXzfyvUsEQp' +
		'37GsvfOeioQ4H8IjC3BWos6IbgtkCPBd3I3BbEY0E3YLcFZiyIvzkDG1SM/Z9zN' +
		'L/cATa4IAyfhfk/CYU39o+wzjAAAAFCSURBVHja7ZShr4JQFMb9H2xmRiKw' +
		'aSQQDAYCgWAwOQI0gsHAZmAkApXN4sZMBjeNBAKBDSKBQDAYDASDgUD43s4NLz18' +
		'4+1tFk65uzvb+X33+87uCB+q0QAewB8DTyYTKIoCwzCw3+9RFAXatv118P1+x' +
		'+FwwPF47A8+n89Yr9fI8xzX6xW73Q6r1QqiKILjOCyXS9i2Dd/3mSjqa5oGSZLY' +
		'OZ1OEQRBfzANqKqqs3+73XA6nWCaJlzXRRRFeDwerEenLMud7nSC0zRlqv9anuchDMP' +
		'+GZONSZJ83+u6xuVywXa7RVmWeD6fnUOpJwjC2134EZxlGXieh+M4TABlqqoqLMtit' +
		'uq6zmyczWZYLBZMDGVMLjVNw15LuffeasqNILRQlPE75a/XC3EcM/Bms8F8Psd4PH' +
		'7ryPCBDOAB/K/1BXf+63D8ZndiAAAAAElFTkSuQmCC'},
{'structure' : '<cml>' +
		'<MDocument>' +
		'<MChemicalStruct>' +
		'<molecule molID="m1">' +
		'<atomArray atomID="a1 a2 a3 a4 a5 a6" ' +
			'elementType="C C C C C C" ' +
			'mrvMap="1 2 3 4 5 6" ' +
			'x3="-1.04144999666214 ' +
				'0.39515000333786 ' +
				'0.7683500033378601 ' +
				'-0.19054999666214 ' +
				'-1.6271499966621397 ' +
				'-2.0003499966621403" ' +
			'y3="0.44381666523615526 ' +
				'0.6741166652361552 ' +
				'-0.1443833347638448 ' +
				'0.08421666523615523 ' +
				'-0.14618333476384476 ' +
				'0.6724166652361552" ' +
			'z3="-1.3884 -0.975 0.2411 1.3884 0.975 -0.2411"/>' +
		'<bondArray>' +
		'<bond id="b1" atomRefs2="a1 a2" order="1"/>' +
		'<bond id="b2" atomRefs2="a1 a6" order="1"/>' +
		'<bond id="b3" atomRefs2="a2 a3" order="1"/>' +
		'<bond id="b4" atomRefs2="a3 a4" order="1"/>' +
		'<bond id="b5" atomRefs2="a4 a5" order="1"/>' +
		'<bond id="b6" atomRefs2="a5 a6" order="1"/>' +
		'</bondArray>' +
		'</molecule>' +
		'</MChemicalStruct>' +
		'</MDocument>' +
		'</cml>',
'name' : 'chair right',
'icon' : 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB4AAAAUCAYAAACaq43EAA' +
		'AACXBIWXMAAAP0AAAD9AEnGM3WAAAB9XpUWHRtb2xTb3VyY2UAAHicfZTbjpswEIZfZe' +
		'TrxviEMVHIatVVpUqbm+5W6l3kgJNFCjgCcurTdwzbLTTaiJvRz/+NPQdYPFyqPZxc05' +
		'a+zginjICrc1+U9S4jP1+/zQx5WC5yNKGxbjPy1nWHeRSdz2eav7nKXnxNc1+R4f380pYT' +
		'z1lS3+wiwRiPfq2eX3pmVtZtZ+vcIdWW87YXn31uu/4WnxwRVbY5lXU02K' +
		'OqOQ3Z1lytzVrRS1uQf6V8xVePSMK23DvY+qayHZy4ooaqL7BztWts5wrYXHs1RZ2R5WL1' +
		'5PNj5eoOw5CizO3+pWuOOQqV37v8iNkw+P6UkYojYDtfPTaNvUKIgmw5WAFWglVgY7AaW7p' +
		'3Iefr9eDwZvDxEMAqVvaAnQcBEhTEgPaLzMiMU61FqtI0FpJrYZSEGaNCaDnR' +
		'GOVKm5gxlWgjEx4nvc9wwaasoEIZPdKCpAU3IwnPvsqwBjivJFacpUppwRID' +
		'nCrJ2UhK8GTNRfyfZBTnN65JriTkEqkcSwR+DyVLE+7FaJrESApMBu/ioM0GEfsefT' +
		'R+udj4uhiFUBYZ2XDST+SH27bifSgEfFO4BgsMCYJ1DIgbQN8H5BQIM78PqCkQNuQ' +
		'+EE+BsE/3AT0F4k9qiEYdi/6uNYY3Kx+NvocI/wLLPy8UNGkiJ5s9AAABM0lEQVR42u2UIa' +
		'uDUBTH92EcGAwiRoOCYcEghjFFhsHgB1gatgWDQVgVNBj2AWRpoMEPMMRgNC4sGIwL' +
		'/8c98MrKY8J7r3jShf/h/s75n3PvCv8UqwW8gP8UfL1ecTwe0ff9R5ex/LIsYVkWXq' +
		'/X5+C2bcFxHFzXhSzL2O12SJIE9/sd4zgS4HK54HQ6kaaqKhRFge/7iKII2+2W9FlW' +
		'M2jTNHQehgFFUcBxHKzXawLEcYzb7UbaezweDypkFph1p+v67Bl6nke2z1ouwzCogPd4Pp' +
		'/oug55niMMQ0iShP1+T66wbr8LN01zHriua5pfmqY4HA7kAM/zEAQBm82GoEyr' +
		'qgpZliEIAoiiSDobFctld8x6TpqmEYB1w5ZqmqYfbWYLyGy2bRvn83n5QBbwAv7d' +
		'+AJiyekPFOurkgAAAABJRU5ErkJggg=='},
{'structure' : '<cml>' +
		'<MDocument>' +
		'<MChemicalStruct>' +
		'<molecule molID="m1">' +
		'<atomArray atomID="a1 a2 a3 a4 a5 a6 a7 a8" ' +
			'elementType="C C H H H H H H" ' +
			'mrvMap="1 2 0 0 0 0 0 0" ' +
			'x3="-1.2514749952316284 ' +
				'-0.8522749952316284 ' +
				'-1.2450749952316282 ' +
				'-2.2529749952316287 ' +
				'-0.5469749952316285 ' +
				'-1.5567749952316283 ' +
				'0.14922500476837164 ' +
				'-0.8586749952316284" ' +
			'y3="0.5755750166893006 ' +
				'1.3425750166893007 ' +
				'-0.4982249833106994 ' +
				'0.8723750166893006 ' +
				'0.7940750166893006 ' +
				'1.1239750166893008 ' +
				'1.0457750166893005 ' +
				'2.4163750166893" ' +
			'z3="0.6359 -0.6359 0.4413 0.9517 1.44 -1.44 -0.9517 -0.4413"/>' +
		'<bondArray>' +
		'<bond id="b1" atomRefs2="a1 a2" order="1"/>' +
		'<bond id="b2" atomRefs2="a1 a3" order="1"/>' +
		'<bond id="b3" atomRefs2="a1 a4" order="1"/>' +
		'<bond id="b4" atomRefs2="a1 a5" order="1"/>' +
		'<bond id="b5" atomRefs2="a2 a6" order="1"/>' +
		'<bond id="b6" atomRefs2="a2 a7" order="1"/>' +
		'<bond id="b7" atomRefs2="a2 a8" order="1"/>' +
		'</bondArray>' +
		'</molecule>' +
		'</MChemicalStruct>' +
		'</MDocument>' +
		'</cml>',
'name' : 'perspective staggered',
'icon' : 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABwAAAAhCAYAAADK6cvn' +
		'AAAACXBIWXMAAAP0AAAD9AEnGM3WAAACJ3pUWHRtb2xTb3VyY2UAAHicfZTbbtpAEI' +
		'ZfZbTX9XrPBwREUaKqlcJNk0q9Q4tZiCVsI9uc+vQdm7SY0iJbZvR7vn9md7yMH4' +
		'7FBvaxbvKqnBBOGYFYZtUyL9cT8v3tc+LIw3ScYRImls2EvLftdpSmh8OBZu' +
		'+xCMeqpFlVkPP70bHJr3IOklb1OhWM8fTH7OW1Z5K8bNpQZhGpJh81vfhSZaHtu' +
		'/hPibQI9T4v03N6WtT7s9ucq7mbK3psluSylCd89YgkrPJNhFVVF6GFPVfUUfUJ1' +
		'rGMdWjjEhanXvWoMzIdz56rbFfEssWws8izsHlt612GQlFtYrZDNwy' +
		'+Pk9IwREIbVU81nU4QRd1cuAQBAQJQUHQEAwEC8Hhxm5i5/x22kbsD57gy' +
		'+UigOuZhS3OAASwy4VbJCck4VRorqzyXgvJjXAKEkadFuJawzyl2UATkAhkhR' +
		'9otmO1MkNNd6zWxg40CYxy5YXQjClrnLTcfNR1ZliXwAmbRE+r8WbcGOclYwY4lUoM' +
		'pb608k4IfEjJmfFeYRlnhbwiGbVesb/MuJB+IDmUmNJ2IGkQVHFz8SLws2/NSO272' +
		'v0vtqB4tzqvuUUT1e+c6tfWS8k5A+eb/hnwdLyoyuUghHw5IQtO+sl/i6tGfAyfQFU' +
		'vY42z7Ay61CEgbgB5H5A3gLoPqBtA3wf0NYBfr7kPmBvA3gfsDeD+BaSDLU5/nzcMb' +
		'85iOjioKf49TX8Bz0JX9R4m4nwAAAFKSURBVHja7ZaxyoJQGIYPSPfRJQRuTt6A' +
		'NLdFSbOESCBCF9AVBOGQ4OYiCA4SOAs6uDgJioNTsyC9P57hp3740YrOkt/yceDAA' +
		'8fveT8JGBcZgd8JDILgoX8caNs2LpcL7UyAvu8/dCbfsGkaRFHEDlhVFTabDVvgarViB0zTFI' +
		'IgsAPmeQ5RFD8DvPeubVucTidIkgSO47Db7eA4Dh2iIY6SZ7yzLAvr9RqqqmI+n9N' +
		'zHMc4Ho/QNA2GYfQ6SoakyF/vyrLEbDb7PXd3b7cbsizrdZT0pcj1eqXPeF+yLON8Pr' +
		'+UOKQvRfb7PRaLBX1K13WRJAl4nsfhcKBP+2zikKGpYpomFEXBZDLBdDqFruuo6/rzWiyX' +
		'S6oEs/XUTWcnPTPgdrtFURTsgN0QdTnKBNj5FoYhPM9jA3xn078EfGfTj7+JI3AE/ls' +
		'/Q6r6cT+bZU0AAAAASUVORK5CYII='},
{'structure' : '<cml>' +
		'<MDocument>' +
		'<MChemicalStruct>' +
		'<molecule molID="m1">' +
		'<atomArray atomID="a1 a2 a3 a4 a5 a6 a7 a8" ' +
			'elementType="C C H H H H H H" ' +
			'mrvMap="1 2 0 0 0 0 0 0" ' +
			'x3="-0.9388625476837159 ' +
				'-1.226762547683716 ' +
				'-0.7603625476837159 ' +
				'-0.05596254768371596 ' +
				'-1.790562547683716 ' +
				'-0.48366254768371597 ' +
				'-1.1880625476837159 ' +
				'-2.218262547683716" ' +
			'y3="1.2631500095367432 ' +
				'0.7168500095367432 ' +
				'0.43765000953674305 ' +
				'1.904150009536743 ' +
				'1.8451500095367432 ' +
				'1.092850009536743 ' +
				'-0.37364999046325686 ' +
				'1.033850009536743" ' +
			'z3="-0.7041875 ' +
				'0.7042125000000001 ' +
				'-1.3951875 ' +
				'-0.6824874999999999 ' +
				'-1.0599875 ' +
				'1.4093125 ' +
				'0.6965125000000001 ' +
				'1.0318125"/>' +
		'<bondArray>' +
		'<bond id="b1" atomRefs2="a1 a2" order="1"/>' +
		'<bond id="b2" atomRefs2="a1 a3" order="1"/>' +
		'<bond id="b3" atomRefs2="a1 a4" order="1"/>' +
		'<bond id="b4" atomRefs2="a1 a5" order="1"/>' +
		'<bond id="b5" atomRefs2="a2 a6" order="1"/>' +
		'<bond id="b6" atomRefs2="a2 a7" order="1"/>' +
		'<bond id="b7" atomRefs2="a2 a8" order="1"/>' +
		'</bondArray>' +
		'</molecule>' +
		'</MChemicalStruct>' +
		'</MDocument>' +
		'</cml>',
'name' : 'perspective eclipsed',
'icon' : 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABoAAAAdCAYAAAC0T3x2AAAA' +
		'CXBIWXMAAAP0AAAD9AEnGM3WAAACNnpUWHRtb2xTb3VyY2UAAHicfZRbb9pAEIX' +
		'/ymify3rvFwREUaKqlZKXJpX6hjZmQyxhG9kOl/76zpq22KHFfhmdOd/ZC2NmN4dyA7vY' +
		'tEVdzQmnjECs8npVVOs5+f78eeLIzWKWowmNVTsnb123nWbZfr+n+Vssw6GuaF6X5NSf' +
		'Htpi5NlLWjfrTDDGsx+PD089MymqtgtVHpFqi2nbiw91Hrp+F/9ZIitDsyuq7GTPymZ3' +
		'SltytXRLRQ/tipyPcoetWyThtdhEeK2bMnSw44o6qj7BOlaxCV1cwcuxVz3qjCxmj/d' +
		'1/l7GqsMyRRR52Dx1zXuOQllvYv6OaVh8vZ+TkiMQurq8bZpwhFQlOXAIAoKEoCBo' +
		'CAaCheDwYjcxJT8ftxH3B3fw5fwSwPM8hi3+BiCAnV+8IjknE0a9dM4Iraxx0nLtYcK' +
		'pEMaeNQNos4bJsY1RprUfaiax1jP9gVVOmqHPJh93jo0DBRXciQFL4CjT8AgjuWaM' +
		'eS2NVRLPQbHrPkhKWjOQmAZOPVNDEhWn9DgLZ9OLYVbasbTSKO8RN1Jo40yySTm0Ef' +
		'h5ukCLaziroa8EF8nTPzwdU3rdd9FonFDOptjTk9pMe5/anCrmJcIYY7zRo5i0Nnc' +
		'o4VxkfwdjMXupq9WghGI1Jy+c9BPzLb624vfQEKibVWzwKlNAsg4BcQHI64C8ANR1QF0A' +
		'+jqgxwBOvbkOmAvAXgfsBeD+BWSDK87+fKdYXnzD2eADz/BvbfELDVhjLipyWPEAAAE' +
		'qSURBVHja7ZY9ioNQFEYFe3E5ilbiFsQVaOEGRNDGIlmApbZiq6VFsLASrKzsxSwgEiQ' +
		'ofIMWw4Rh/HmTBIbJbS7C1QPvO+8ihRcV9Qb9bdDpdLrrv5ldBEVRhCzL5r5Wa7OLoDR' +
		'NP/v1ev1x7nK5II7ju3eIMwqCAJZlzR/9WlVVQdd1nM/nx8kwHY0gCMjzfH4+HA5IkgR9' +
		'3++XYS3UpmkgSRIYhoHruptloUgEOB6PYFkWYRhuloVaEmAcx28idF0HURTRti0cx1kUY' +
		'HNGt9sNnudBUZT5uHzfh2EYkGUZqqqiruvHb4aiKGDbNmiahmmaGIZhUzbEK4jjuF0XmRj' +
		'E8/xdjk8DaZr2mu09bYGng6bwy7LcJQERiEQCIhCJBO9/hjfoH4A+AJhlYjYnUOUiAAAA' +
		'AElFTkSuQmCC'},
{'structure' : '<cml>' +
		'<MDocument>' +
		'<propertyList>' +
		'<property dictRef="viewEulerAngles" title="viewEulerAngles">' +
		'<array size="3" dataType="xsd:double">' +
		'-0.029945974150597656 0.0014140410115725634 -0.6165071641399984</array>' +
		'</property>' +
		'</propertyList>' +
		'<MChemicalStruct>' +
		'<molecule molID="m1">' +
		'<atomArray atomID="a1 a2 a3 a4 a5 a6 a7 a8" ' +
			'elementType="C C H H H H H H" ' +
			'mrvMap="1 2 0 0 0 0 0 0" ' +
			'x3="-1.516123916087437 ' +
				'-1.5273906843608587 ' +
				'-0.6292288066685323 ' +
				'-1.5332937363505077 ' +
				'-2.381292245368258 ' +
				'-2.3911078972237942 ' +
				'-1.4780220739751422 ' +
				'-0.6710402203487711" ' +
			'y3="1.0095249841102971 ' +
				'1.0122691414806768 ' +
				'0.5468676126827638 ' +
				'2.001143787461338 ' +
				'0.46383717051596185 ' +
				'1.5324845319845712 ' +
				'0.03754736581894069 ' +
				'1.5638249481817785" ' +
			'z3="0.8052893934561745 ' +
				'-0.811288358574212 ' +
				'-1.2172522220556312 ' +
				'-1.2697554659154366 ' +
				'-1.2089999341322855 ' +
				'1.1913157449425797 ' +
				'1.297130586493431 ' +
				'1.2135602557853802"/>' +
		'<bondArray>' +
		'<bond id="b1" atomRefs2="a1 a2" order="1"/>' +
		'<bond id="b2" atomRefs2="a1 a6" order="1"/>' +
		'<bond id="b3" atomRefs2="a1 a7" order="1"/>' +
		'<bond id="b4" atomRefs2="a1 a8" order="1"/>' +
		'<bond id="b5" atomRefs2="a2 a3" order="1"/>' +
		'<bond id="b6" atomRefs2="a2 a4" order="1"/>' +
		'<bond id="b7" atomRefs2="a2 a5" order="1"/>' +
		'</bondArray>' +
		'</molecule>' +
		'</MChemicalStruct>' +
		'<MEllipse background="#ffffff" lineColor="#000000" id="o2">' +
		'<MPoint x="-1.7445626928954667" y="1.5800169921913347" z="-0.1385181257264432"/>' +
		'<MPoint x="-0.9510378673962028" y="1.2684474934670886" z="-0.1385181257264432"/>' +
		'<MPoint x="-1.2726577393236165" y="0.44932574440854656" z="-0.1385181257264432"/>' +
		'<MPoint x="-2.0661825648228813" y="0.7608952431327924" z="-0.1385181257264432"/>' +
		'</MEllipse>' +
		'</MDocument>' +
		'</cml>',
'name' : 'Newman staggered',
'icon' : 'data:image/png;base64,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'},
{'structure' : '<cml>' +
		'<MDocument>' +
		'<MChemicalStruct>' +
		'<molecule molID="m1">' +
		'<atomArray atomID="a1 a2 a3 a4 a5 a6 a7 a8" ' +
			'elementType="C C H H H H H H" ' +
			'mrvMap="1 2 0 0 0 0 0 0" ' +
			'x3="-0.35252928558250696 ' +
				'-0.3637960538559286 ' +
				'1.3043658047629112 ' +
				'-0.36969910584557775 ' +
				'-1.7676976118830958 ' +
				'-0.33712502253994014 ' +
				'-1.1752356738765686 ' +
				'0.5867170441881385" ' +
			'y3="0.5987638215817546 ' +
				'0.6015079789521341 ' +
				'-0.3038935474615929 ' +
				'2.140382696458369 ' +
				'-0.3502573579297865 ' +
				'1.6180019510874215 ' +
				'0.07977784923584519 ' +
				'0.07071653178191017" ' +
			'z3="0.8052893934561745 ' +
				'-0.811288358574212 ' +
				'-1.2172522220556312 ' +
				'-1.2697554659154366 ' +
				'-1.2089999341322855 ' +
				'1.1913157449425797 ' +
				'1.297130586493431 ' +
				'1.2135602557853802"/>' +
		'<bondArray>' +
		'<bond id="b1" atomRefs2="a1 a2" order="1"/>' +
		'<bond id="b2" atomRefs2="a2 a3" order="1"/>' +
		'<bond id="b3" atomRefs2="a2 a4" order="1"/>' +
		'<bond id="b4" atomRefs2="a2 a5" order="1"/>' +
		'<bond id="b5" atomRefs2="a1 a6" order="1"/>' +
		'<bond id="b6" atomRefs2="a1 a7" order="1"/>' +
		'<bond id="b7" atomRefs2="a1 a8" order="1"/>' +
		'</bondArray>' +
		'</molecule>' +
		'</MChemicalStruct>' +
		'<MEllipse background="#ffffff" lineColor="#000000" id="o2">' +
		'<MPoint x="-0.5809680623905368" y="1.1692558296627922" z="-0.1385181257264432"/>' +
		'<MPoint x="0.21255676310872718" y="0.857686330938546" z="-0.1385181257264432"/>' +
		'<MPoint x="-0.10906310881868653" y="0.038564581880004" z="-0.1385181257264432"/>' +
		'<MPoint x="-0.9025879343179516" y="0.3501340806042499" z="-0.1385181257264432"/>' +
		'</MEllipse>' +
		'</MDocument>' +
		'</cml>', 
'name' : 'Newman eclipsed',
'icon' : 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAeCAYAAA' +
		'BNChwpAAAACXBIWXMAAAP0AAAD9AEnGM3WAAADm2lUWHRtb2xTb3VyY2UAAQBFTi1VUwB' +
		'tb2xTb3VyY2UAeJyVVVFv2zYQ/iuEnmuJd+QdScN2kTXrViAOiroD9hYospIKsKRMUhK7' +
		'T8N+yn5K98d2lOMlgTdjtfxAHe+7u+/48TR7u6036qHs+qpt5gmkOlFlU7TrqrmdJ798f' +
		'j/xydvFrBAncWz6efJlGO6mWfb4+JgWX8o637ZNWrR1st+fbvvqlc+jSdvuNkOtIft1eb' +
		'EaMZOq6Ye8KUpB9dW0H40XbZEPYxX/kSKr8+6harK9e1Z3D/toV2Cv/JVNt/06eabyTrb' +
		'OBKluqk2pbtquzgf1ADb1qX2jbsum7PKhXKvr3WgNYtfJYrY8b4v7umwGlQ9tvSqHTz/9' +
		'ME/09PwNTC+Tg/F92wzSrekqb/pV2VU3k48XZx8uJ8CJum6b9TPuMsaMtVRFvlkN3X0xL' +
		'GZ1uymLeylLFh/O50kN4hVDn3VdvhuTRHMOKkeVG5VblZPKWeVO5V5OaFPGEj/v7kohqt' +
		'6pn5+fREljlvmdlKdQ6edn3DjbVLkcolZ//fnt929/HO1L4avyt+gAr/e2Zp5MdGq8scx' +
		'eezSIDoJT0RisM4YckyYOho2CFJ2x6OUBMGAZo5/Vmg0zehtIdh2rCaQueDYExpJBz9qP' +
		'ASUDI1kCH5wmGB1RMzgDgQnZGMNKp0TkXCAEkrqcoUTtpEydMgos5rUYpEZpQ8oGrSXrw' +
		'IUglY5p0GEg1mQZLJA2ClNwYFBLWqPJQ0ws1UCQvxd2wtqaIOzYikew8mKsZyu9SgG0g0' +
		'hLeyHifEwKGpisRuEfDKGc3NexPq/lJUQwCSdLMYtkQ+8NeXIWAUfG4JBQfpqIzZONgyO' +
		'pmAKQlWbuO+OD/IyV2tETSYEQhL5EssEiOTkm8QrCTVixFU8D0QJG6KM00UsDNYoIs39U' +
		'uJhFIb9Yqmo9T65hfwc+lTc9Pik0UW23LjsRXAwQXV8C8DUg6vk0wBwB7GmAPQLQaQAdc' +
		'eDTAD4CuNMAdwTw/wbIXrQ4OwwFWR4NjOWPm01115dj9DYe1PJjW8mQ2o63kgGCaMSTtt' +
		'qIeuUexFkeh24wogkCLcqN8hvdwXi5WSDCQBYNj+e+j/cyrvh5YAhotSUfGONw243y9Z7' +
		'RyGUQ3QRZf1/c6CiTIM4CGQxeLjnZp8CaA2nUzjoU6TKb744sdCkODg7sHPChYuPH2ykz' +
		'SYZJQPo/cbND0+Py8FWQtXwLF38D1FjvOVltZj0AAAFiSURBVHja7Za/yoJQGMaFbsM7c' +
		'Ema3L2C1oSiVi/AtRJEl4Y2paHFIoRAaAiKhtpFFxEH9+5A/PN8eOCLWqL6OMkHnuU9HA' +
		'+8Px4en/cwqHkxDcC/BDgejw/16wCr1Qqn04nUWgD2+/1DbTzQeKAWD5RlWW8QXS4X6Lr' +
		'+PQDf9zGfz8l+uVxCkiRwHIdutwvDMLDb7egCxHEM27ZhmiYGgwGCIIAgCKTxdDqlD1AZ' +
		'brPZoN1uw3VdJEkCURRJ49lsRr5RBaiarNdrdDodAtDr9dBqtTCZTGBZFhzHoW/CoigwH' +
		'A6JCqPRCKqqgud5sCxLfEAF4D71DocD+v0+FEVBlmXk/Hw+E3NWvhiPx28nJPNO6v029T' +
		'wPsixD0zRst9vb3cVicbub5/nfAdI0Jb/cq6l3n5DX65UoU/nlmSrMM8mjKEIYhh+HTKVO' +
		'BfFsbjA0B80rc4OhOWiaV3ED0AC8sn4AySJ85RIuL7cAAAAASUVORK5CYII='},
{'structure' : '<cml>' +
		'<MDocument>' +
		'<MChemicalStruct>' +
		'<molecule molID="m1">' +
		'<atomArray atomID="a1 a2 a3 a4 a5 a6 a7 a8" ' +
			'elementType="C C H H H H H H" ' +
			'mrvMap="1 2 0 0 0 0 0 0" ' +
			'x3="-0.5668516982722889 ' +
				'-0.5701085724142687 ' +
				'-0.524077180467213 ' +
				'0.3199970978671116 ' +
				'-1.4720153720209812 ' +
				'-0.5514474352297221 ' +
				'-1.4262247629684421 ' +
				'0.3357279997997493" ' +
			'y3="1.0027885974448214 ' +
				'1.0139179836733467 ' +
				'0.00338621648210391 ' +
				'1.440134176644712 ' +
				'1.4792024423545849 ' +
				'2.022026726950488 ' +
				'0.48380262509891214 ' +
				'0.47474130764497635" ' +
			'z3="0.8052893934561745 ' +
				'-0.811288358574212 ' +
				'-1.2172522220556312 ' +
				'-1.2697554659154366 ' +
				'-1.2089999341322855 ' +
				'1.1913157449425797 ' +
				'1.297130586493431 ' +
				'1.2135602557853802"/>' +
		'<bondArray>' +
		'<bond id="b1" atomRefs2="a1 a2" order="1"/>' +
		'<bond id="b2" atomRefs2="a2 a3" order="1"/>' +
		'<bond id="b3" atomRefs2="a2 a4" order="1"/>' +
		'<bond id="b4" atomRefs2="a2 a5" order="1"/>' +
		'<bond id="b5" atomRefs2="a1 a6" order="1"/>' +
		'<bond id="b6" atomRefs2="a1 a7" order="1"/>' +
		'<bond id="b7" atomRefs2="a1 a8" order="1"/>' +
		'</bondArray>' +
		'</molecule>' +
		'</MChemicalStruct>' +
		'<MEllipse background="#ffffff" lineColor="#000000" id="o2">' +
		'<MPoint x="-0.18701051502212795" y="1.4858683854105375" z="-0.1385181257264432"/>' +
		'<MPoint x="-0.06007520321207571" y="0.6428709786324378" z="-0.1385181257264432"/>' +
		'<MPoint x="-0.9302653826528228" y="0.511841083554205" z="-0.1385181257264432"/>' +
		'<MPoint x="-1.0572006944628751" y="1.354838490332305" z="-0.1385181257264432"/>' +
		'</MEllipse>' +
		'</MDocument>' +
		'</cml>', 
'name' : 'Newman staggered 2',
'icon' : 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAbCAYAAABm409' +
		'WAAAACXBIWXMAAAP0AAAD9AEnGM3WAAADjmlUWHRtb2xTb3VyY2UAAQBFTi1V' +
		'UwBtb2xTb3VyY2UAeJyVVe1u20YQfJUDf0fk7d3elyApcOOmDWAZQZQC/WecJdoh' +
		'IJIuSdtyfxXom/RR0hfrHGXXdtUKDSgIh7mdudm9XXL2dldvxV3Z9VXbzDPKZSbKZt' +
		'1uquZ6nv30+f3EZ28XszWCENj08+zLMNxMi+L+/j5ffynruGubfN3W2X5/uuurVzH3O' +
		'm+760JJScXPy7PVyJlUTT/EZl2C1VfTfgTP2nUcRhf/cURRx+6uaop9eFF3d3u1C' +
		'+ILf8H5rt9kz6m8w9YJmOKq2pbiqu3qOIg74tzn/EZcl03ZxaHciMuHEQ3AZbaYLU' +
		'/b9W1dNoOIQ1uvyuHTD9/NMzk9fUPT8+wJfN82A6o1XcWmX5VddTX5eHby4X' +
		'xCNhOXbbN55p0nzeSlWsftauhu18NiVrfbcn0LW1h8OJ1nNSEqSZ90XXwYD0lwJBGVi' +
		'FpEFtGIaEV0Inrc0LZMFj8/3JRIVLwTPz4/mUBhlvEG9oQS8vkZN062VcQlSvHnH19/+' +
		'/r7wT6Mr8pfUgC93tvpeTaRubHWG7LBK6eU90EkzEmS3jjFxMp6N2KKpXPkJVunSAuZa' +
		'wohOBmct46IrJhQzk5JMhr/SgZPamQaYnasjVIBZ9AYp6xS7KwK1jMDg5zGgS5J4sdBZ' +
		'+JBj/0rlfPeAGP2ilgAIh3IBa+t0xp+wJZSa28V2RQjsY0wZgQyOWtxPrwkewHOcKA2b' +
		'DwHoXKpgCAlG4xk7yHFXntAyiCDQOlEYA4PaekgFZzVJhO/wp3MvTTKBx00G0uOTUrY' +
		'E6GS2qCCSE2lfBU5hfxxljFWP2I2OGPYmkCGtR3rp6RHBaBGGrdhDDxTIE1Q4sDKu' +
		'OAAoY7wYrxFmVinVHEjxkpljPMmuUf3FX+332KWOvjFUlSbeXZJ++b/VF716rE1M9F' +
		'2m7JD2ZNACn1JUK8JqZGPE/QBgY8T+IBgjhPMQQ72OMEeENxxgjsg+H8jFC9KXDy9' +
		'DbA8eFMsv99uq5u+HNXbdFHLj22Ft9NuHEfyafYMGfQlYRyQ4EOaAva4bjSmYexql' +
		'/pvHw+IMGiYHbSmHi9+L/gPYWmldEZJDVksHI3CMres/H6ItWLt/DcLB41hQdPhT3k07' +
		'aOuIfLwijEwjK7/RlnMOCKktIHZwqChxzpgcFEFDhh3pf+fbPFU8rR8+hhgjU/g4i+JC' +
		'+1s3dszVQAAATJJREFUeNrtlT/qgzAcxb2CJ3Ds6CC4VG/h5hEEQQcRKegNKu' +
		'iiDoKDLW0XBweH0guUbsUOXR3cXfX9MFs7COk/foMB+SYY8onvPRMGX27MDPgY4Hg8' +
		'PtSPAzabDU6nE6lfAVRV9VD/rwdTWg/DQO0NQ6P1+XxGFEVU3jBTWl8uFwRBQ' +
		'MaGYUCWZfA8D13XUZYlecY5U95MenC9XpHnOSzLgu/7CMMQiqKQr0iShAC6rnvd5' +
		'HFXu90OoiiSxWzbxnK5JP3VakXqWylar9fYbrcQBAGapoHjOLAsC1VVYZrma4DnV' +
		'NR1DUmSiP6HwwFxHJPxYrGA4zjo+54O8JwKz/Nwv9+Rpilc1yXSjPq3bUveFUVBF9Op' +
		'P/Z2u2G/378X06nWNA2yLKM6QubTdL4yZ8APAH9bX6QaEiUUNQAAAABJRU5ErkJggg=='},
{'structure' : '<cml>' +
		'<MDocument>' +
		'<MChemicalStruct>' +
		'<molecule molID="m1">' +
		'<atomArray atomID="a1 a2 a3 a4 a5 a6 a7 a8" ' +
			'elementType="C C H H H H H H" ' +
			'mrvMap="1 2 0 0 0 0 0 0" ' +
			'x3="1.3450121335949943 ' +
				'1.3417552594530142 ' +
				'1.391735466262939 ' +
				'2.6715083320601902 ' +
				'-0.18444847974176704 ' +
				'2.235400377773918 ' +
				'0.48420390318863743 ' +
				'1.357332931114128" ' +
			'y3="0.9864421573215538 ' +
				'0.9975715435500793 ' +
				'-0.8997997668449659 ' +
				'1.772121095286093 ' +
				'1.737503288773199 ' +
				'1.4827207389529469 ' +
				'1.439433803267001 ' +
				'-0.09099266957104335" ' +
			'z3="0.8052893934561745 ' +
				'-0.811288358574212 ' +
				'-1.2172522220556312 ' +
				'-1.2697554659154366 ' +
				'-1.2089999341322855 ' +
				'1.1913157449425797 ' +
				'1.297130586493431 ' +
				'1.2135602557853802"/>' +
		'<bondArray>' +
		'<bond id="b1" atomRefs2="a1 a2" order="1"/>' +
		'<bond id="b2" atomRefs2="a2 a3" order="1"/>' +
		'<bond id="b3" atomRefs2="a2 a4" order="1"/>' +
		'<bond id="b4" atomRefs2="a2 a5" order="1"/>' +
		'<bond id="b5" atomRefs2="a1 a6" order="1"/>' +
		'<bond id="b6" atomRefs2="a1 a7" order="1"/>' +
		'<bond id="b7" atomRefs2="a1 a8" order="1"/>' +
		'</bondArray>' +
		'</molecule>' +
		'</MChemicalStruct>' +
		'<MEllipse background="#ffffff" lineColor="#000000" id="o2">' +
		'<MPoint x="1.7248533168451547" y="1.46952194528727" z="-0.1385181257264432"/>' +
		'<MPoint x="1.851788628655206" y="0.6265245385091707" z="-0.1385181257264432"/>' +
		'<MPoint x="0.9815984492144603" y="0.4954946434309373" z="-0.1385181257264432"/>' +
		'<MPoint x="0.8546631374044078" y="1.3384920502090374" z="-0.1385181257264432"/>' +
		'</MEllipse>' +
		'</MDocument>' +
		'</cml>', 
'name' : 'Newman eclipsed 2',
'icon' : 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB8AAAAgCAYAAADqgqNBAAA' +
		'ACXBIWXMAAAP0AAAD9AEnGM3WAAADa3pUWHRtb2xTb3VyY2UAAHiclVXbbttGEP2VBZ8jc' +
		'mZ3Zy+CpMCNmzaAZQRRCuTNoCXaISCRLkXbUr6+Zym7tqFGaCTDWJyZc+ayM6vJ' +
		'+91mrR6qblu3zTTjnDJVNct2VTe30+yvrx9HIXs/myzhBMdmO82+9/3duCgeHx/z5fdqU' +
		'+7aJl+2m+xgH++29RufR5O33W2hibj4Nr9YDJxR3Wz7sllWYG3r8XYAL9pl2Q9Z' +
		'/CREsSm7h7opDu7Fpns4qF2xvQpXNt9tV9lLKR9gOgNT3dTrSt203abs1QPbPOT2nbq' +
		'tmqor+2qlrvcDGoFTNpvMz9vl/aZqelX27WZR9V/++G2a0fj8HY8vs2fwY9v' +
		'06NZ4UTbbRdXVN6PPF2efLkfsMnXdNqsX3mXSTLnUy3K96Lv7ZT+bbNp1tbxHWjh8Op9m' +
		'G4ZXkj7runI/BElwyarUqjSqtKoUVTpVelUG3NC6Sil+3d9VKFR9UH++fDOFxszLO' +
		'6SntKKX72A4W9clLpFUfmRCzovq72Tjt7adSYNhrBBrNkaijdEalSD2IhqAGGKrExTZG7HO' +
		'aaejiUrnzrNQMEaTI46k1YhyDtbaYH30UHCeLPw0aETG4wORoCiHhyYTyXAIzvhDSPGQi' +
		'oaZLWu0Yo/kKI/BWasZRvwTk9gxekFoa0SIfDQpbgCIP+cQPzqJEPReoyqKooOjmEJ444WMD' +
		'gGJcEw+NmivyZsAr2jdABm0wAT4IX3iJE6RYtTORYQlGCVTP4bkAkEczUAHHXsrQyaM7IORI' +
		'B55oymca/ZaND4k4swT5lAF2ikxVeLcgBHKiFBjo3UQQTYc2aB4FGW1oKuAdPRsSNAXeBpOCB' +
		'txpEV8QIdIY+SKf2duNklj++qo6tU0u+bDxH+pbrb6aR4z1XarqsNIJIHk+pqg3xLS9J4mmCO' +
		'CPU2wRwQ5TZCjGtxpgjsi+NMEf0QI/0UoXrW4eH4CcDx6Hua/r9f13bYa1Nt0UfPPbY0naZ' +
		'cW0WuLCzSMIRZMBYLvE4y5FM1YRR28BvhjmqVNM0E4MKZCY0PMcOkHsbei8PLYMiwBFprco' +
		'Ek5tlg0ljsIYa/pF1XTWrLEtGuarXVknmRtFEyqsxhMrJw3vyob0gNjGE8CWUs+PHUA+4h' +
		'IJKSxijD+H9niudfp+Pz044wfvNk/FgfnpXdrZPEAAAE9SURBVHja7Zavq4NQFMf3f9gEQdY' +
		'tsm6yG2R/wMxqnGA0DMyCbfsHhrDB1Goy6ooGWV2wDcP34Q3yfIO9OecM85Qjcjmfe37fGUaU' +
		'2QSf4KfTqaX7yH+27uC73Q5BEBDdV47H40Nbd/DD4dDScRx3isbvs+fzuWWrc84Nw4DjOE' +
		'9Hw3Xd5uztdutfcJ7nNR6kaUq+bduGKIqQJAm6riOKIiiKAk3THnrbq9r3+z0WiwV4nm' +
		'88qyND0zQ4jsP1eh221VRVJXCWZcEwDARBgO/7ME1z+D5fr9cIwxDL5RIURSHLMvLfsqzh4X' +
		'VRJUmCPM8hyzKqqsJms0FRFJ+ZcPUFVqsV5vM5iURXcO/xerlcSCuOMtvLsiS5/zj8HTvg' +
		'Zfg7dsDL8L874PseE9vtdryCGyXnU8FNT+cJPsGfkR/Ltg6P0EgG4AAAAABJRU5ErkJggg=='},
{'structure' : '<cml>' +
		'<MDocument>' +
		'<MChemicalStruct>' +
		'<molecule molID="m1">' +
		'<atomArray>' +
		'<atom id="a1" elementType="C" ' +
				'x3="-1.8952959263165792" ' +
				'y3="-0.22963958690961184" ' +
				'z3="0.96" ' +
				'mrvMap="1"/>' +
		'<atom id="a2" elementType="C" ' +
				'x3="-3.328495926316579" ' +
				'y3="0.0075604130903881295" ' +
				'z3="1.3814" ' +
				'mrvMap="2"/>' +
		'<atom id="a3" elementType="C" ' +
				'x3="-4.29019592631658" ' +
				'y3="-0.16693958690961175" ' +
				'z3="0.22699999999999998" ' +
				'mrvMap="3"/>' +
		'<atom id="a4" elementType="C" ' +
				'x3="-3.9043959263165795" ' +
				'y3="0.6876604130903881" ' +
				'z3="-0.96" ' +
				'mrvMap="4"/>' +
		'<atom id="a5" elementType="O" ' +
				'x3="-2.4711959263165792" ' +
				'y3="0.4505604130903882" ' +
				'z3="-1.3814" ' +
				'mrvMap="5" lonePair="2"/>' +
		'<atom id="a6" elementType="C" ' +
				'x3="-1.509595926316579" ' +
				'y3="0.6249604130903881" ' +
				'z3="-0.22700000000000004" ' +
				'mrvMap="6"/>' +
		'</atomArray>' +
		'<bondArray>' +
		'<bond atomRefs2="a1 a2" order="1" id="b1"/>' +
		'<bond atomRefs2="a1 a6" order="1" id="b2"/>' +
		'<bond atomRefs2="a2 a3" order="1" id="b3"/>' +
		'<bond atomRefs2="a3 a4" order="1" id="b4"/>' +
		'<bond atomRefs2="a4 a5" order="1" id="b5"/>' +
		'<bond atomRefs2="a5 a6" order="1" id="b6"/>' +
		'</bondArray>' +
		'</molecule>' +
		'</MChemicalStruct>' +
		'</MDocument>' +
		'</cml>', 
'name' : 'oxane chair',
'icon' : 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB4AAAAUCAYAAACaq43EAA' +
		'AACXBIWXMAAAP0AAAD9AEnGM3WAAACA3pUWHRtb2xTb3VyY2UAAHicfZRta9swEMe/yqH' +
		'Xi54tyyFJKS2DQcNg7WDvgmIrqSG2g+087dPv5LSbvNI4by5//X930p3s2d252sHRt13Z' +
		'1HMiKCfg67wpyno7Jz9fvk4suVvMcjShse7m5LXv91PGTqcTzV995c5NTfOmItf16bkr' +
		'R56Tok27ZZJzwX4tn54HZlLWXe/q3CPVldNuEJ+a3PXDLj4pwSrXHsuaXe2sao/XbCuhV' +
		'3al6bkryL+jPODSPZKwKXceNk1buR6OQlNL9RfY+tq3rvcFrC+DmqHOyWK2fGzyQ+XrHs' +
		'OQoszd7rlvDzkKVbPz+QGzYfDtcU4qgYDrm+q+bd0FQhRkJ8BJcAqcBpeAM9jSnQ85Xy5' +
		'7jzuD6+87PBDAUyzdHjsPEhRo4ID2swqjyJQ0mTAZT7hNpE6AU51leiRNUDNScqt4WNFp' +
		'Mmg8NTrSrABBVWLSiNUgqRJWRVJK4IKVORUSMaSM0ZorabGyMkZFUmpQE5mwsc1iFa7xT' +
		'+RDm+WZ+i9bZlUauwj8HgpnJmzUCuwDlXj6+LHhYGiYvDkmwZLy+NE4D/Z3IIvZuqmLKI' +
		'SymJO1IMOkfvhNJ9+GRaBpC99i00OCYI0B+QEwtwE1BsJduA3oMRBuzm0gGQPhnt0GzBh' +
		'IPjkDizrG3q87hh9eBRa9Jwy/Dos/OiA5n5ns6fEAAAFOSURBVHja7ZQhq8JQFMf9DmvT' +
		'vGSYYDQYDEYFw4JJLLYFg+E144KYHAwM+qaCQdwwGwyCX8BgMBoWFgyGhd+7d4KPB09hj' +
		'weWnTbu2fmd/zn/ezO8KTIpOAW/DayqKpVKhX6/jzceE263cLt9J2w2sFrBbPbjv8vlwn' +
		'w+x3Xd5OCVKNhqtTifz/i+z2e3S9BogK5DLgfNJkwm9+ThENptKJX4yGYx6nUKhQK2bSc' +
		'Hl0SR0+n0+2EUQRDAYgHrNYxGsN9DGD4Ul8tlkRYlA+9FkYZU98ewLIvpdJp8x4ZhsNvt' +
		'Ht+BUOd5Hr1ej+PxKISFT4vKs3w+/1TtU/DhcEDTtNhQsgFd7LRWq2GaJp1OR6yyHY+xW' +
		'CxSrVbjZhzHiad0E8aTageDQXJXL5fLGCINJXf8qvPr9cpWOF2Cu8J88gYoivJyIukDko' +
		'JT8L/GF8gA8vTK0EsFAAAAAElFTkSuQmCC'},
{'structure' : '<cml>' +
		'<MDocument>' +
		'<MChemicalStruct>' +
		'<molecule molID="m1">' +
		'<atomArray atomID="a1 a2 a3 a4 a5 a6 a7" ' +
			'elementType="C C C C N C H" ' +
			'mrvMap="1 2 3 4 5 6 0" ' +
			'x3="6.592836186541576 ' +
				'5.1055911521930835 ' +
				'4.0931681938303495 ' +
				'4.5337718167047925 ' +
				'5.957552144725356 ' +
				'6.937452585264776 ' +
				'6.194001318567216" ' +
			'y3="-3.14593412357069 ' +
				'-2.9277870768674994 ' +
				'-3.080471013128954 ' +
				'-2.180530523710906 ' +
				'-2.4349299366218258 ' +
				'-2.243484319448746 ' +
				'-1.7449880734085816" ' +
			'z3="1.2728961376242363 ' +
				'1.6818129744367836 ' +
				'0.5077665714122135 ' +
				'-0.6830434432901489 ' +
				'-1.059339272138872 ' +
				'0.052705300975256525 ' +
				'-1.772798269019469"/>' +
		'<bondArray>' +
		'<bond id="b1" atomRefs2="a1 a2" order="1"/>' +
		'<bond id="b2" atomRefs2="a1 a6" order="1"/>' +
		'<bond id="b3" atomRefs2="a2 a3" order="1"/>' +
		'<bond id="b4" atomRefs2="a3 a4" order="1"/>' +
		'<bond id="b5" atomRefs2="a4 a5" order="1"/>' +
		'<bond id="b6" atomRefs2="a5 a6" order="1"/>' +
		'<bond id="b7" atomRefs2="a5 a7" order="1"/>' +
		'</bondArray>' +
		'</molecule>' +
		'</MChemicalStruct>' +
		'</MDocument>' +
		'</cml>', 
'name' : 'piperidine chair',
'icon' : 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB8AAAAXCAYAAADz/ZRUAA' +
		'AACXBIWXMAAAP0AAAD9AEnGM3WAAAChnpUWHRtb2xTb3VyY2UAAHichZRta9swEMe/yuH' +
		'XiyzpTjqpJCllZWzQ7sXawd4V1XFbQ2wXx03Sffqdkj0kdMsIhsvf97snnTw937ZLWNfD' +
		'qum7WWGULqDuqn7RdI+z4uvth0kozufTSpzEsVvNiqdxfD4ry81mo6qnuk3bvlNV3xb79' +
		'2fbVXPks0HVD4+l1dqU366vbnbMpOlWY+qqWqhVc7baiVd9lcZdFf9IUbZpWDdduXcv22' +
		'G9j3Zn6C7ckdquFsWfVt7Lqwsh4aFZ1vDQD20aYW1IBUXv4LHu6iGN9QLuX3dqFF0X8+n' +
		'1ZV+9tHU3iplDNFVa3ozDSyVC2y/r6kWiifHpcla0RoA09u3FMKRXyFaWk4FkISEkguQg' +
		'eUgsU13WOezt63MtxcH+91mejwVIK9fpWcYPFhAIHHiQg9jirPDKRRvQm+AdGccenDLau' +
		'WiMsyaiDuiAlI5ofJD/ATVSzJJDZDbBeNbE0ToBo2MnFBFbh86DVxGZnHXBWU/MWTGR5K' +
		'zQBOfZGl/Aq1QxQWXIRSRj0bH2ESZWRcscWLMPnilGAvHSQZKZzNsQHWU3E7RD7SyKHrX' +
		'PEkmJNkb03pog2bNmRQyEkp4Ck7gZxUQxBM1IOriQa/mOeUctS3BvkL0lix7BKOk9GBuF' +
		'QM8yLtDKaWnIOzZStDUypYkWN9SSh9BGbSjEnEVLXyi9iE8IbIWUYjnXrCPLbLw8u2rYc' +
		'gzWCxnJRzn68vfZz6f3fbc4MKFZzIp7U+yW4kv9sLI/96KAfljUg7SRA2TXQ8C+AfxpAI' +
		'+BvHanAToG8pKeBtwxkFf6NOCPAfffHvgNwH8DyoMRl7+uophvrml5cIdL+XLNfwDQCl4' +
		'S/n9asQAAAZRJREFUeNrtlbFqAkEQhn0BX0EsFCwsLSxEsNNKCdda2VhY+ABio2jASqzE' +
		'wuICVxkNchALEdKIELEM6BFFT8QiSLSQixf+7E4REKJyQS4Ebpo5dnf2m/3nh7PhD8Nmw' +
		'f81vNvtHmVT4ZIkodfrUTYd3ul0jrLpM1cU5boz3+/3mM1mFy+az9col6vXhWezWTgcDr' +
		'hcLgiCgETiFrI8xnK5pf1q9ZmB35nRXlmjH7S23WpoNB5Rq9UQCoXQ7/eNw/mrvV4vNps' +
		'NffNLCgURlcqANfGASOQO+fwTWq0XNmsFg4FK6/H4PQKBG5RKJbjdbux2O+Nw3nkmk7lg' +
		'NAXj8RspoGk6dP3ze6/dbiOdThuX/XA4wOfzYTKZ/NqAsVjsbP1JeL1eRyqVOlrj8smyj' +
		'OFwSKM4F6vVikkfMG44Pl+n04lcLkcNhMNhMpzH44Hf70cwGCQv8DMcwKUtFosk82g0ItV' +
		'4LX+AYbiqqohGoxBFkS6bTqfU0E+xWCzQbDaZCStIJpMkNW/UbrefrLH+ahbcgpsaX+3f' +
		'pFVZttwxAAAAAElFTkSuQmCC'}
]} // get3DButtons()

// --> end HTML comment
