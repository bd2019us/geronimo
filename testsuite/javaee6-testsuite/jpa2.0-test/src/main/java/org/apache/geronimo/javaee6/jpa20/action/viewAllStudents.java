/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.geronimo.javaee6.jpa20.action;


import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.List;
import javax.ejb.EJB;

import org.apache.geronimo.javaee6.jpa20.entities.Course;
import org.apache.geronimo.javaee6.jpa20.entities.Student;
import org.apache.geronimo.javaee6.jpa20.bean.Facade;

@WebServlet(name="viewAllStudents", urlPatterns={"/viewAllStudents"})
public class viewAllStudents extends HttpServlet {
    @EJB
    private Facade facade = null;
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html;charset=8859_1");
        PrintWriter out = response.getWriter();
        try {
           
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet viewAllStudents</title>");  
            out.println("</head>");
            out.println("<body>");
            out.println("<table border=\"1px\"><tr><td>Student ID</td><td>Student Name</td><td>Delete</td><tr>");
            List<Student> students = facade.findAllStudent();
                   for (Student stu : students) {
                       out.println("<tr>");
                       out.println("<td>"+stu.getId()+"</td>");
                       out.println("<td>"+stu.getInfo().getName()+"</td>");
                       out.println("<td><a href=\"StudentDelete?sid="+stu.getId()+"\">Delete</a>");
                       out.println(" </td></tr>");
                   }
            out.println("</table>");
            out.println("</body>");
            out.println("</html>");
            
        } finally { 
            out.close();
        }
    } 

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    } 

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }


}
