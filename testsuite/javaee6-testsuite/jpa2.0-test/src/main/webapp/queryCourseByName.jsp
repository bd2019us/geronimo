<!--

	Licensed to the Apache Software Foundation (ASF) under one or more
	contributor license agreements. See the NOTICE file distributed with
	this work for additional information regarding copyright ownership.
	The ASF licenses this file to You under the Apache License, Version
	2.0 (the "License"); you may not use this file except in compliance
	with the License. You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0 Unless required by
	applicable law or agreed to in writing, software distributed under the
	License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
	CONDITIONS OF ANY KIND, either express or implied. See the License for
	the specific language governing permissions and limitations under the
	License.
-->

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
    "http://www.w3.org/TR/html4/loose.dtd">
<%@page import="org.apache.geronimo.javaee6.jpa20.entities.Student" %>
<%@page import="org.apache.geronimo.javaee6.jpa20.entities.Course" %>
<%@page import="org.apache.geronimo.javaee6.jpa20.bean.Facade" %>
<%@page import="java.util.List" %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
    </head>
    <body>

        <%
                    Facade facade = new Facade();
                    String cname = request.getParameter("cname");
                    Course c = facade.findCourseByName(cname);
        %>
             Query Information is:
            <ol>
                <li>
                    Course ID:<%=c.getCid()%>
                    <br/>
                    Course NAME:<%=c.getCname()%>
                </li>
            </ol>
    </body>
</html>
