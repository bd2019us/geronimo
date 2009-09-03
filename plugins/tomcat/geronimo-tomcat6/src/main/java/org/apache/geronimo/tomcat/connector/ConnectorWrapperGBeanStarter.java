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
package org.apache.geronimo.tomcat.connector;

import java.util.Set;

import org.apache.catalina.connector.Connector;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.gbean.GBeanData;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoBuilder;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.gbean.annotation.AnnotationGBeanInfoFactory;
import org.apache.geronimo.gbean.annotation.GBean;
import org.apache.geronimo.gbean.annotation.ParamReference;
import org.apache.geronimo.gbean.annotation.ParamSpecial;
import org.apache.geronimo.gbean.annotation.SpecialAttributeType;
import org.apache.geronimo.kernel.Kernel;
import org.apache.geronimo.system.serverinfo.ServerInfo;
import org.apache.geronimo.tomcat.TomcatContainer;
import org.apache.geronimo.tomcat.TomcatServerGBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to create corresponding connector GBeans based on
 * connectors defined in server.xml
 * 
 * @version $Rev$ $Date$
 */
@GBean
public class ConnectorWrapperGBeanStarter implements GBeanLifecycle {

    private static final Logger log = LoggerFactory.getLogger(ConnectorWrapperGBeanStarter.class);
    private final TomcatServerGBean server;
    private final TomcatContainer container;
    private final ClassLoader classLoader;
    private final Kernel kernel;

    public ConnectorWrapperGBeanStarter(
            @ParamReference(name = "Server") TomcatServerGBean server,
            @ParamReference(name = "TomcatContainer") TomcatContainer container,
            @ParamSpecial(type = SpecialAttributeType.classLoader) ClassLoader classLoader,
            @ParamSpecial(type = SpecialAttributeType.kernel) Kernel kernel) throws Exception {

        this.server = server;
        this.container = container;
        this.classLoader = classLoader;
        this.kernel = kernel;

    }

    private void buildConnectorGBean(ClassLoader cl, Kernel kernel, Connector conn, TomcatContainer container) {

        GBeanInfo gbeanInfo = this.getConnectorGBeanInfo(conn);

        String uniqueName = TomcatServerGBean.ConnectorName.get(conn);

        if (uniqueName == null) {
            uniqueName = conn.getProtocol() + "_" + conn.getAttribute("host") + "_" + conn.getPort();
        }

        AbstractName containerAbstractName = kernel.getAbstractNameFor(container);

        AbstractName name = kernel.getNaming().createSiblingName(containerAbstractName, uniqueName,
                GBeanInfoBuilder.DEFAULT_J2EE_TYPE);
        server.getService(null);
        GBeanData gbeanData = new GBeanData(name, gbeanInfo);
        gbeanData.setAttribute("name", uniqueName);
        gbeanData.setAttribute("connector", conn);
        gbeanData.setAttribute("host", conn.getAttribute("address"));
        gbeanData.setAttribute("port", conn.getPort());

        gbeanData.setReferencePattern(ConnectorGBean.CONNECTOR_CONTAINER_REFERENCE, containerAbstractName);

        AbstractNameQuery query = new AbstractNameQuery(ServerInfo.class.getName());
        Set<AbstractName> set = kernel.listGBeans(query);
        gbeanData.setReferencePattern("ServerInfo", set.iterator().next());

        try {
            kernel.loadGBean(gbeanData, cl);
            kernel.startGBean(name);
        } catch (Exception e) {
            log.error("Error when building connectorGbean for connector: " + conn.getAttribute("address") + ":"
                    + conn.getPort(), e);
        }

    }

    private GBeanInfo getConnectorGBeanInfo(Connector conn) {

        String className = conn.getProtocolHandlerClassName();
        AnnotationGBeanInfoFactory annotationGbeanInfoFactory=new AnnotationGBeanInfoFactory();
        
        
            // BIO
            if (className.equalsIgnoreCase("org.apache.coyote.http11.Http11Protocol")) {
                if (conn.getScheme().equalsIgnoreCase("https"))

                    return annotationGbeanInfoFactory.getGBeanInfo(Https11ConnectorGBean.class);

                else
                    return annotationGbeanInfoFactory.getGBeanInfo(Http11ConnectorGBean.class);
            }

            // NIO
            if (className.equalsIgnoreCase("org.apache.coyote.http11.Http11NioProtocol")) {
                if (conn.getScheme().equalsIgnoreCase("https"))
                    return annotationGbeanInfoFactory.getGBeanInfo(Https11NIOConnectorGBean.class);
                else
                    return annotationGbeanInfoFactory.getGBeanInfo(Http11NIOConnectorGBean.class);
            }

            // AJP
            if (className.equalsIgnoreCase("org.apache.coyote.ajp.AjpAprProtocol")) {

                return annotationGbeanInfoFactory.getGBeanInfo(AJP13ConnectorGBean.class);
            }

            if (className.equalsIgnoreCase("org.apache.jk.server.JkCoyoteHandler")) {

                return annotationGbeanInfoFactory.getGBeanInfo(AJP13ConnectorGBean.class);
            }

            // Apr
            if (className.equalsIgnoreCase("org.apache.coyote.http11.Http11AprProtocol")) {
                if (conn.getScheme().equalsIgnoreCase("https"))
                    return annotationGbeanInfoFactory.getGBeanInfo(Https11APRConnectorGBean.class);
            else
                return annotationGbeanInfoFactory.getGBeanInfo(Http11APRConnectorGBean.class);
            }
    

        return null;
    }

    public void doFail() {
        // TODO Auto-generated method stub

    }

    public void doStart() throws Exception {

        Connector[] connectors = server.getService(null).findConnectors();

        for (Connector conn : connectors) {
            this.buildConnectorGBean(classLoader, kernel, conn, container);
        }
    }

    public void doStop() throws Exception {
        // TODO Auto-generated method stub

    }
}
