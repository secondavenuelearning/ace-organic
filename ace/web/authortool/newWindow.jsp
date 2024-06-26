<%@ page language="java" %>
<%@ page errorPage="/errormsgs/errorHandler.jsp" %>

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
	"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" >
<head>
	<script type="text/javascript">
	// <!-- >
	function getValues() {
		document.saveEditablesForm.qType.value = opener.getMajorQType();
		document.saveEditablesForm.qFlags.value = opener.calculateQFlags();
		document.saveEditablesForm.book.value = opener.getBook();
		document.saveEditablesForm.chapter.value = opener.getChapter();
		document.saveEditablesForm.bookQNumber.value = opener.getBookQNumber();
		document.saveEditablesForm.qStmt.value = opener.getStatement();
		document.saveEditablesForm.keywords.value = opener.getKeywords();
		document.saveEditablesForm.destination.value =
				opener.document.saveEditablesForm.destination.value;
		/* alert('qType = ' + document.saveEditablesForm.qType.value
				+ ', qFlags = ' + document.saveEditablesForm.qFlags.value
				+ ', qStmt = ' + document.saveEditablesForm.qStmt.value
				+ ', destination = ' + document.saveEditablesForm.destination.value); /**/
		document.saveEditablesForm.submit();
	}
	// -->
	</script>
</head>
<body onload="getValues();">
<form name="saveEditablesForm" action="saveEditables.jsp" 
		method="post" accept-charset="UTF-8">
	<input type="hidden" name="qType" value = "" />
	<input type="hidden" name="qFlags" value = "" />
	<input type="hidden" name="book" value = "" />
	<input type="hidden" name="chapter" value = "" />
	<input type="hidden" name="bookQNumber" value = "" />
	<input type="hidden" name="qStmt" value = "" />
	<input type="hidden" name="keywords" value = "" />
	<input type="hidden" name="destination" value = "" />
</form>
</body>
</html>

