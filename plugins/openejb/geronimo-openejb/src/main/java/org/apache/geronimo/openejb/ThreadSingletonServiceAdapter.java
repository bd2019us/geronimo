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


package org.apache.geronimo.openejb;

import org.apache.geronimo.openwebbeans.GeronimoSingletonService;
import org.apache.geronimo.openwebbeans.OpenWebBeansWebInitializer;
import org.apache.geronimo.openwebbeans.OsgiMetaDataScannerService;
import org.apache.openejb.cdi.CdiAppContextsService;
import org.apache.openejb.cdi.CdiResourceInjectionService;
import org.apache.openejb.cdi.OWBContext;
import org.apache.openejb.cdi.OpenEJBLifecycle;
import org.apache.openejb.cdi.StartupObject;
import org.apache.openejb.cdi.ThreadSingletonService;
import org.apache.webbeans.config.OpenWebBeansConfiguration;
import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.corespi.ServiceLoader;
import org.apache.webbeans.spi.ContainerLifecycle;
import org.apache.webbeans.spi.ResourceInjectionService;

/**
 * @version $Rev$ $Date$
 */
public class ThreadSingletonServiceAdapter extends GeronimoSingletonService implements ThreadSingletonService {
    public ThreadSingletonServiceAdapter() {
        super();
    }

    @Override
    public void initialize(StartupObject startupObject) {
        //share owb singletons
        OWBContext owbContext = new OWBContext();
        Object old = contextEntered(owbContext);
        try {
            if (old == null) {
                //not embedded. Are we the first ejb module to try this?
                if (startupObject.getAppContext().get(OWBContext.class) == null) {
                    startupObject.getAppContext().set(OWBContext.class, owbContext);
                    setConfiguration(OpenWebBeansConfiguration.getInstance());
                    try {
                        ServiceLoader.getService(ContainerLifecycle.class).startApplication(startupObject);
                    } catch (Exception e) {
                        throw new RuntimeException("couldn't start owb context", e);
                    }
                }
                // an existing OWBConfiguration will have already been initialized
            } else {
                startupObject.getAppContext().set(OWBContext.class, new OWBContext((WebBeansContext) old));
            }
        } finally {
            contextExited(old);
        }
    }

    private void setConfiguration(OpenWebBeansConfiguration configuration) {
        configuration.setProperty(OpenWebBeansConfiguration.USE_EJB_DISCOVERY, "true");
        //from CDI builder
        configuration.setProperty(OpenWebBeansConfiguration.INTERCEPTOR_FORCE_NO_CHECKED_EXCEPTIONS, "false");

        configuration.setProperty(OpenWebBeansConfiguration.CONTAINER_LIFECYCLE, OpenEJBLifecycle.class.getName());
        configuration.setProperty(OpenWebBeansConfiguration.JNDI_SERVICE, OpenWebBeansWebInitializer.NoopJndiService.class.getName());
        configuration.setProperty(OpenWebBeansConfiguration.SCANNER_SERVICE, OsgiMetaDataScannerService.class.getName());
        configuration.setProperty(OpenWebBeansConfiguration.CONTEXTS_SERVICE, CdiAppContextsService.class.getName());
        configuration.setProperty(ResourceInjectionService.class.getName(), CdiResourceInjectionService.class.getName());
//        configuration.setProperty(ELAdaptor.class.getName(), EL22Adaptor.class.getName());
    }

    @Override
    public Object contextEntered(org.apache.openejb.cdi.OWBContext owbContext) {
        return GeronimoSingletonService.contextEntered(owbContext.getSingletons());
    }

    @Override
    public void contextExited(Object oldContext) {
        if (oldContext != null && !(oldContext instanceof WebBeansContext)) throw new IllegalArgumentException("Expecting a WebBeansContext not " + oldContext.getClass().getName());
        GeronimoSingletonService.contextExited((WebBeansContext) oldContext);
    }
}
