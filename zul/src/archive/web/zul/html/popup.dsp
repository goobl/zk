<%--
popup.dsp

{{IS_NOTE
	Purpose:
		
	Description:
		
	History:
		Mon Jun  5 11:03:53     2006, Created by tomyeh@potix.com
}}IS_NOTE

Copyright (C) 2006 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
}}IS_RIGHT
--%><%@ taglib uri="/WEB-INF/tld/web/core.dsp.tld" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/zul/core.dsp.tld" prefix="u" %>
<c:set var="self" value="${requestScope.arg.self}"/>
<div id="${self.uuid}" zk_type="zul.html.widget.Pop"${self.outerAttrs}${self.innerAttrs}>
	<c:forEach var="child" items="${self.children}">
  ${u:redraw(child, null)}
	</c:forEach>
</div>