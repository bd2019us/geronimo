/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.geronimo.testsuite.console;

/**
 * ???
 *
 * @version $Rev$ $Date$
 */
public class SimpleLoginTest
    extends ConsoleTestSupport
{
    /**
     * @testng.test
     */
    public void testLogin() throws Exception {
        login();
        
        selenium.removeCookie("JSESSIONID", "/");
    }
    
    /**
     * @testng.test
     */
    public void testLoginAndLogout() throws Exception {
        login();
        logout();
    }
    
    /**
     * @testng.test
     */
    public void testClickSomeLinks() throws Exception {
        login();
        
        selenium.click("link=Information");
        selenium.waitForPageToLoad("30000");
        assertEquals("Geronimo Console", selenium.getTitle());
        
        selenium.click("link=JVM");
        selenium.waitForPageToLoad("30000");
        assertEquals("Geronimo Console", selenium.getTitle());
        
        selenium.click("link=DB Info");
        selenium.waitForPageToLoad("30000");
        assertEquals("Geronimo Console", selenium.getTitle());
        
        logout();
    }
}

