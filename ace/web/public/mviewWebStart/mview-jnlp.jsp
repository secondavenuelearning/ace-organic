<% response.setContentType("application/x-java-jnlp-file"); 
%><%@ 
	page language="java" 
%><%@ 
	page import="com.epoch.utils.Utils"
%><%
	request.setCharacterEncoding("UTF-8");
	String applet = request.getParameter("applet"); 
	if (Utils.isEmpty(applet)) applet = "view";
	applet = applet.toLowerCase();
	final String appletFirstCapital = Utils.toString(
			applet.substring(0, 1).toUpperCase(), applet.substring(1));

	final boolean isChrome = "true".equals(request.getParameter("isChrome"));
	final String url = request.getRequestURL().toString();
	final String urlLead = (isChrome ? Utils.substringToWith(url, "/ace") : "");

%><?xml version="1.0" encoding="UTF-8"?>
<jnlp spec="1.0+" codebase="<%= urlLead %>/nosession/jchem/">
    <information>
        <title>Marvin<%= appletFirstCapital %></title>
        <vendor>ChemAxon</vendor>
        <homepage href="."/>
        <description>Application for <%= applet.equals("view") 
				? "display" : "draw" %>ing chemical structures.</description>
        <offline-allowed/>
        <icon href="m<%= applet %>64.gif"/>
    </information>
    <security>
        <all-permissions/> 
    </security>
<resources>
    <j2se version='1.8' />
    <jar href='signedLib/alchemist-commons__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/alignment__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/alignment-plugin__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/annotations__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/automapper__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/calculations__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/calculations-commons__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/calculations-elemanal__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/calculations-nmr__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/calculations-plugin__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/calculations-solubility__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/calculations-stereoanal__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/calculations-stereoanal-api__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/calculations-stereoisomers__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/chart__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/codeassist__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/common__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/concurrent__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/concurrent-utils__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/config-utils__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/convoy__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/core__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/core-calculations__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/enumeration__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/enumeration-plugin__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/icons__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/io__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/io-accord__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/io-all__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/io-cdx__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/io-csv__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/io-gaussian__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/io-inchi__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/io-mdl__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/io-mrv__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/io-pdb__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/io-peptide__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/io-skc__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/io-smiles__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/io-tripos__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/jchem-vis__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/jep__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/license__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/marvin-app__V17.14.0-7139-chemaxoncert.jar' main='true' />
    <jar href='signedLib/marvin-editor__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/marvin-gui__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/marvin-services__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/marvin-services-gui__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/marvin-space__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/marvin-utils__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/modelling__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/naming__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/search-api__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/search-base__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/search-mcs__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/search-sss__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/search-utils__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/sizeinfo__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/sizeinfo-agent__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/structurechecker__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/structurechecker-api__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/structurechecker-marvin__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/transformer__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/version__V17.14.0-7139-chemaxoncert.jar' />
    <jar href='signedLib/batik-dom__V1.8-chemaxoncert.jar' />
    <jar href='signedLib/batik-svggen__V1.8-chemaxoncert.jar' />
    <jar href='signedLib/batik-util__V1.8-chemaxoncert.jar' />
    <jar href='signedLib/xml-apis__V1.3.04-chemaxoncert.jar' />
    <jar href='signedLib/jgoodies-forms__V1.7.2-chemaxoncert.jar' />
    <jar href='signedLib/looks__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/itext__V2.1.7-chemaxoncert.jar' />
    <jar href='signedLib/itext-rtf__V2.1.7-chemaxoncert.jar' />
    <jar href='signedLib/msketch-javahelp__V20170710-chemaxoncert.jar' />
    <jar href='signedLib/mview-javahelp__V20170710-chemaxoncert.jar' />
    <jar href='signedLib/aloe__V1.0-chemaxoncert.jar' />
    <jar href='signedLib/cxnregistry-native-x86__V1.0-chemaxoncert.jar' />
    <jar href='signedLib/cxnregistry-native-x64__V1.0-chemaxoncert.jar' />
    <jar href='signedLib/jacob-native-x86__V1.17-chemaxoncert.jar' />
    <jar href='signedLib/jacob-native-x64__V1.17-chemaxoncert.jar' />
    <jar href='signedLib/jacob__V1.17-chemaxoncert.jar' />
    <jar href='signedLib/jai_core__V1.1.3-chemaxoncert.jar' />
    <jar href='signedLib/jai_codec__V1.0-chemaxoncert.jar' />
    <jar href='signedLib/jh__V1.0-chemaxoncert.jar' />
    <jar href='signedLib/freehep-graphicsio-emf__V2.2.1-chemaxoncert.jar' />
    <jar href='signedLib/freehep-graphicsio-ps__V2.2.1-chemaxoncert.jar' />
    <jar href='signedLib/freehep-graphicsio-pdf__V2.2.1-chemaxoncert.jar' />
    <jar href='signedLib/slf4j-api__V1.7.21-chemaxoncert.jar' />
    <jar href='signedLib/marvin-build__Vunspecified-chemaxoncert.jar' />
    <jar href='signedLib/guava__V20.0-chemaxoncert.jar' />
    <jar href='signedLib/jcampdx-lib__V0.9.1-chemaxoncert.jar' />
    <jar href='signedLib/chart__V1.0-chemaxoncert.jar' />
    <jar href='signedLib/regexp__V1.2-chemaxoncert.jar' />
    <jar href='signedLib/antlr__V2.7.2-chemaxoncert.jar' />
    <jar href='signedLib/commons-logging__V1.1.1-chemaxoncert.jar' />
    <jar href='signedLib/antlr4__V4.5-chemaxoncert.jar' />
    <jar href='signedLib/gson__V2.3.1-chemaxoncert.jar' />
    <jar href='signedLib/commons-io__V2.4-chemaxoncert.jar' />
    <jar href='signedLib/dom4j__V1.6.1-chemaxoncert.jar' />
    <jar href='signedLib/commons-lang__V2.5-chemaxoncert.jar' />
    <jar href='signedLib/commons-exec__V1.2-chemaxoncert.jar' />
    <jar href='signedLib/commons-csv__V1.4-chemaxoncert.jar' />
    <jar href='signedLib/jni-inchi__V0.7-chemaxoncert.jar' />
    <jar href='signedLib/inchi-native-linux__V1.3-chemaxoncert.jar' />
    <jar href='signedLib/inchi-native-linux64__V1.3-chemaxoncert.jar' />
    <jar href='signedLib/inchi-native-macosx__V1.3.1-chemaxoncert.jar' />
    <jar href='signedLib/inchi-native-windows__V1.3-chemaxoncert.jar' />
    <jar href='signedLib/inchi-native-windows64__V1.3-chemaxoncert.jar' />
    <jar href='signedLib/jackson-databind__V2.7.2-chemaxoncert.jar' />
    <jar href='signedLib/slf4j-jdk14__V1.7.21-chemaxoncert.jar' />
    <jar href='signedLib/AppleJavaExtensions__V1.4-chemaxoncert.jar' />
    <jar href='signedLib/xmlrpc-client__V3.1.3-chemaxoncert.jar' />
    <jar href='signedLib/json__V20140107-chemaxoncert.jar' />
    <jar href='signedLib/saaj-impl__V1.3.19-chemaxoncert.jar' />
    <jar href='signedLib/wsdl4j__V1.6.3-chemaxoncert.jar' />
    <jar href='signedLib/jextexp__V1.0-chemaxoncert.jar' />
    <jar href='signedLib/gluegen-rt-main__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/jogl-all-main__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/jcommander__V1.32-chemaxoncert.jar' />
    <jar href='signedLib/automaton__V1.11-8-chemaxoncert.jar' />
    <jar href='signedLib/jai-imageio-core__V1.3.0-chemaxoncert.jar' />
    <jar href='signedLib/levigo-jbig2-imageio__V1.6.1-chemaxoncert.jar' />
    <jar href='signedLib/pdfbox__V1.8.9-chemaxoncert.jar' />
    <jar href='signedLib/bcprov-jdk15__V1.46-chemaxoncert.jar' />
    <jar href='signedLib/tika-core__V1.2-chemaxoncert.jar' />
    <jar href='signedLib/tika-parsers__V1.2-chemaxoncert.jar' />
    <jar href='signedLib/tagsoup__V1.2.1-chemaxoncert.jar' />
    <jar href='signedLib/poi__V3.11-beta2-20140914-chemaxoncert.jar' />
    <jar href='signedLib/poi-scratchpad__V3.11-beta2-20140914-chemaxoncert.jar' />
    <jar href='signedLib/poi-ooxml__V3.11-beta2-20140914-56023-chemaxoncert.jar' />
    <jar href='signedLib/ooxml-schemas__V1.1-chemaxoncert.jar' />
    <jar href='signedLib/juniversalchardet__V1.0.3-chemaxoncert.jar' />
    <jar href='signedLib/commons-compress__V1.9-chemaxoncert.jar' />
    <jar href='signedLib/xmlbeans__V2.5.0-chemaxoncert.jar' />
    <jar href='signedLib/xz__V1.5-chemaxoncert.jar' />
    <jar href='signedLib/proxy-vole__V1.0.3-chemaxoncert.jar' />
    <jar href='signedLib/osra-linux-x64__V1.3.8.2-chemaxoncert.jar' />
    <jar href='signedLib/tesseract-data__V3.01_1-chemaxoncert.jar' />
    <jar href='signedLib/tesseract-linux-x32__V3.01_1-chemaxoncert.jar' />
    <jar href='signedLib/tesseract-linux-x64__V3.01_1-chemaxoncert.jar' />
    <jar href='signedLib/tesseract-macosx__V3.01_2-chemaxoncert.jar' />
    <jar href='signedLib/tesseract-windows__V3.01_1-chemaxoncert.jar' />
    <jar href='signedLib/batik-css__V1.8-chemaxoncert.jar' />
    <jar href='signedLib/batik-ext__V1.8-chemaxoncert.jar' />
    <jar href='signedLib/batik-xml__V1.8-chemaxoncert.jar' />
    <jar href='signedLib/xalan__V2.7.0-chemaxoncert.jar' />
    <jar href='signedLib/xml-apis-ext__V1.3.04-chemaxoncert.jar' />
    <jar href='signedLib/batik-awt-util__V1.8-chemaxoncert.jar' />
    <jar href='signedLib/jgoodies-common__V1.7.0-chemaxoncert.jar' />
    <jar href='signedLib/freehep-graphics2d__V2.2.1-chemaxoncert.jar' />
    <jar href='signedLib/freehep-graphicsio__V2.2.1-chemaxoncert.jar' />
    <jar href='signedLib/freehep-graphicsbase__V2.2.1-chemaxoncert.jar' />
    <jar href='signedLib/antlr4-runtime__V4.5-chemaxoncert.jar' />
    <jar href='signedLib/antlr-runtime__V3.5.2-chemaxoncert.jar' />
    <jar href='signedLib/ST4__V4.0.8-chemaxoncert.jar' />
    <jar href='signedLib/jackson-annotations__V2.7.0-chemaxoncert.jar' />
    <jar href='signedLib/jackson-core__V2.7.2-chemaxoncert.jar' />
    <jar href='signedLib/xmlrpc-common__V3.1.3-chemaxoncert.jar' />
    <jar href='signedLib/mimepull__V1.7-chemaxoncert.jar' />
    <jar href='signedLib/gluegen-rt__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/gluegen-rt-natives-android-armv6__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/gluegen-rt-natives-linux-amd64__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/gluegen-rt-natives-linux-armv6__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/gluegen-rt-natives-linux-armv6hf__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/gluegen-rt-natives-linux-i586__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/gluegen-rt-natives-macosx-universal__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/gluegen-rt-natives-solaris-amd64__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/gluegen-rt-natives-solaris-i586__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/gluegen-rt-natives-windows-amd64__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/gluegen-rt-natives-windows-i586__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/jogl-all__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/jogl-all-natives-android-armv6__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/jogl-all-natives-linux-amd64__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/jogl-all-natives-linux-armv6__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/jogl-all-natives-linux-armv6hf__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/jogl-all-natives-linux-i586__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/jogl-all-natives-macosx-universal__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/jogl-all-natives-solaris-amd64__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/jogl-all-natives-solaris-i586__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/jogl-all-natives-windows-amd64__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/jogl-all-natives-windows-i586__V2.1.4-chemaxoncert.jar' />
    <jar href='signedLib/fontbox__V1.8.9-chemaxoncert.jar' />
    <jar href='signedLib/jempbox__V1.8.9-chemaxoncert.jar' />
    <jar href='signedLib/ini4j__V0.5.4-chemaxoncert.jar' />
    <jar href='signedLib/jna__V4.2.2-chemaxoncert.jar' />
    <jar href='signedLib/jna-platform__V4.2.2-chemaxoncert.jar' />
    <jar href='signedLib/freehep-io__V2.2.2-chemaxoncert.jar' />
    <jar href='signedLib/org.abego.treelayout.core__V1.0.1-chemaxoncert.jar' />
    <jar href='signedLib/ws-commons-util__V1.0.2-chemaxoncert.jar' />
</resources>
    <application-desc main-class="chemaxon.marvin.<%= appletFirstCapital %>">
        <argument>--gridbag</argument>
        <argument><%= request.getParameter("argument") %></argument>
    </application-desc>
</jnlp>
