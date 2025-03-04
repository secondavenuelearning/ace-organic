<%@ page language="java" %>
<%@ page import="
	chemaxon.formats.MolImporter,
	chemaxon.struc.MolAtom,
	chemaxon.struc.Molecule,
	com.epoch.utils.SortUtils,
	com.epoch.utils.Utils,
	java.util.ArrayList,
	java.util.Arrays,
	java.util.HashMap,
	java.util.List,
	java.util.Map"
%>
<%
	final List<Object> capitals = new ArrayList<Object>();
	capitals.add("Tokyo");
	capitals.add("Caracas");
	capitals.add("Washington");
	capitals.add("Ottawa");
	capitals.add("Madrid");
	capitals.add("Hanoi");
	capitals.add("New York City");
	capitals.add("Roma");
	capitals.add("Beijing");
	capitals.add("Paris");
	capitals.add("Bangkok");
	capitals.add("New Delhi");
	capitals.add("Lima");
	capitals.add("Brasilia");
	capitals.add("Berlin");
	capitals.add("Mexico City");
	capitals.add("Jerusalem");
	capitals.add("Jerusalem");
	final List<Comparable<?>[]> countries = new ArrayList<Comparable<?>[]>();
	countries.add(new String[] {"Asia", "Japan"});
	countries.add(new String[] {"South America", "Venezuela"});
	countries.add(new String[] {"North America", "United States"});
	countries.add(new String[] {"North America", "Canada"});
	countries.add(new String[] {"Europe", "Spain"});
	countries.add(new String[] {"Asia", "Vietnam"});
	countries.add(new String[] {"North America", "United States"});
	countries.add(new String[] {"Europe", "Italy"});
	countries.add(new String[] {"Asia", "China"});
	countries.add(new String[] {"Europe", "France"});
	countries.add(new String[] {"Asia", "Thailand"});
	countries.add(new String[] {"Asia", "India"});
	countries.add(new String[] {"South America", "Peru"});
	countries.add(new String[] {"South America", "Brazil"});
	countries.add(new String[] {"Europe", "Germany"});
	countries.add(new String[] {"North America", "Mexico"});
	countries.add(new String[] {"Asia", "Israel"});
	countries.add(new String[] {"Asia", "Palestine"});

%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<head>
<title>ACE Sort tester</title>
<link rel="stylesheet" href="../includes/epoch.css" type="text/css"/>
</head>

<body style="overflow:auto; margin-left:10px;">

<br/>

<!-- self submit form -->
<p class="boldtext big" style="text-align:center;">
ACE Sort tester
</P>

<p>
<h3>Unordered:</h3>
<table>
<% for (int capNum = 0; capNum < capitals.size(); capNum++) { %>
<tr><td>
<%= Arrays.toString(countries.get(capNum)) %>
</td><td>
<%= capitals.get(capNum) %>
</td></tr>
<% } %>
</table>

<% SortUtils.sortByArrays(capitals, countries); %>

<h3>Ordered by continent and country:</h3>

<table>
<% for (int capNum = 0; capNum < capitals.size(); capNum++) { %>
<tr><td>
<%= Arrays.toString(countries.get(capNum)) %>
</td><td>
<%= capitals.get(capNum) %>
</td></tr>
<% } %>
</table>

</body>
</html>
