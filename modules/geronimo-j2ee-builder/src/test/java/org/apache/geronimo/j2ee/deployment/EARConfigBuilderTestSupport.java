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

package org.apache.geronimo.j2ee.deployment;

import java.io.File;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URI;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import javax.xml.namespace.QName;

import org.apache.geronimo.common.DeploymentException;
import org.apache.geronimo.deployment.DeploymentContext;
import org.apache.geronimo.deployment.ModuleIDBuilder;
import org.apache.geronimo.deployment.NamespaceDrivenBuilder;
import org.apache.geronimo.deployment.util.DeploymentUtil;
import org.apache.geronimo.gbean.AbstractName;
import org.apache.geronimo.gbean.AbstractNameQuery;
import org.apache.geronimo.j2ee.j2eeobjectnames.NameFactory;
import org.apache.geronimo.j2ee.management.impl.J2EEServerImpl;
import org.apache.geronimo.kernel.Jsr77Naming;
import org.apache.geronimo.kernel.Naming;
import org.apache.geronimo.kernel.config.ConfigurationData;
import org.apache.geronimo.kernel.config.IOUtil;
import org.apache.geronimo.kernel.config.InvalidConfigException;
import org.apache.geronimo.kernel.config.NoSuchConfigException;
import org.apache.geronimo.kernel.config.NullConfigurationStore;
import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.geronimo.kernel.repository.Environment;
import org.apache.geronimo.kernel.repository.ImportType;
import org.apache.geronimo.kernel.repository.ArtifactManager;
import org.apache.geronimo.kernel.repository.DefaultArtifactManager;
import org.apache.geronimo.kernel.repository.ArtifactResolver;
import org.apache.geronimo.kernel.repository.DefaultArtifactResolver;

import org.apache.geronimo.testsupport.TestSupport;

/**
 * Provides support for EAR config builder tests.
 *
 * @version $Rev:386276 $ $Date$
 */
public abstract class EARConfigBuilderTestSupport
    extends TestSupport
{
    protected static String WEB_NAMESPACE = "foo";
    
    protected static JarFile earFile;
    
    protected static MockConfigStore configStore = new MockConfigStore();
    
    protected static ArtifactManager artifactManager = new DefaultArtifactManager();
    
    protected static ArtifactResolver artifactResolver = new DefaultArtifactResolver(artifactManager, Collections.EMPTY_SET, null);
    
    protected static MockEJBConfigBuilder ejbConfigBuilder = new MockEJBConfigBuilder();
    
    protected static MockWARConfigBuilder webConfigBuilder = new MockWARConfigBuilder();
    
    protected static MockConnectorConfigBuilder connectorConfigBuilder = new MockConnectorConfigBuilder();
    
    protected static ResourceReferenceBuilder resourceReferenceBuilder = connectorConfigBuilder;
    
    protected static ModuleBuilder appClientConfigBuilder = null;
    
    protected final static ModuleIDBuilder idBuilder = new ModuleIDBuilder();
    
    protected static ServiceReferenceBuilder serviceReferenceBuilder = new ServiceReferenceBuilder() {
        //it could return a Service or a Reference, we don't care
        public Object createService(Class serviceInterface, URI wsdlURI, URI jaxrpcMappingURI, QName serviceQName, Map portComponentRefMap, List handlerInfos, Object serviceRefType, DeploymentContext deploymentContext, Module module, ClassLoader classLoader) {
            return null;
        }
    };

    protected static final NamespaceDrivenBuilder securityBuilder = null;
    
    protected static final NamespaceDrivenBuilder serviceBuilder = null;

    protected static final Naming naming = new Jsr77Naming();

    protected static final AbstractName rootConfig = naming.createRootName(new Artifact("test", "stuff", "", "car"), "test", "test") ;
    
    protected static final AbstractName transactionManagerObjectName = naming.createChildName(rootConfig, "TransactionManager", "TransactionManager");
    
    protected static final AbstractName connectionTrackerObjectName = naming.createChildName(rootConfig, "ConnectionTracker", "ConnectionTracker");
    
    protected static final AbstractName transactionalTimerObjectName = naming.createChildName(rootConfig, "TransactionalThreaPooledTimer", "ThreadPooledTimer");
    
    protected static final AbstractName nonTransactionalTimerObjectName = naming.createChildName(rootConfig, "NonTransactionalThreaPooledTimer", "ThreadPooledTimer");
    
    protected static final AbstractName serverName = naming.createChildName(rootConfig, "J2EEServer", "Server");
    
    protected static final AbstractName earName = naming.createRootName(new Artifact("org.apache.geronimo.test.test-ear", "ear", "", "ear"), "test", NameFactory.J2EE_APPLICATION);
    
    protected static final AbstractName ejbModuleName = naming.createChildName(earName, "ejb-jar", NameFactory.EJB_MODULE);
    
    protected static final AbstractName webModuleName = naming.createChildName(earName, "war", NameFactory.WEB_MODULE);
    
    protected static final AbstractName raModuleName = naming.createChildName(earName, "rar", NameFactory.RESOURCE_ADAPTER_MODULE);

    protected Environment defaultParentId;
    
    protected static String contextRoot = "test";
    
    protected static final Map portMap = null;
    
    protected final AbstractNameQuery transactionManagerAbstractNameQuery = new AbstractNameQuery(transactionManagerObjectName, null);
    
    protected final AbstractNameQuery connectionTrackerAbstractNameQuery = new AbstractNameQuery(connectionTrackerObjectName, null);
    
    protected final AbstractNameQuery transactionalTimerAbstractNameQuery = new AbstractNameQuery(transactionalTimerObjectName, null);
    
    protected final AbstractNameQuery nonTransactionalTimerAbstractNameQuery = new AbstractNameQuery(nonTransactionalTimerObjectName, null);
    
    protected final AbstractNameQuery corbaGBeanAbstractNameQuery = new AbstractNameQuery(serverName, null);

    /*
    
    TODO: Hook up these tests...
    
    TestSetup setupUnpackedAltDD = new TestSetup(inner) {
        protected void setUp() throws Exception {
            earFile = DeploymentUtil.createJarFile(new File(basedir, "target/test-unpacked-ear/alt-dd/"));
            ejbConfigBuilder.ejbModule = new EJBModule(false, ejbModuleName, null, null, "test-ejb-jar.jar/", null, null, null);
            webConfigBuilder.contextRoot = contextRoot;
            webConfigBuilder.webModule = new WebModule(false, webModuleName, null, null, "test-war.war/", null, null, null, contextRoot, portMap, WEB_NAMESPACE);
            connectorConfigBuilder.connectorModule = new ConnectorModule(false, raModuleName, null, null, "test-rar.rar", null, null, null);
        }

        protected void tearDown() {
            DeploymentUtil.close(earFile);
            close(ejbConfigBuilder.ejbModule);
            close(webConfigBuilder.webModule);
            close(connectorConfigBuilder.connectorModule);
        }
    };
    
    TestSetup setupPackedAltDD = new TestSetup(inner) {
        protected void setUp() throws Exception {
            earFile = DeploymentUtil.createJarFile(new File(basedir, "target/test-unpacked-ear/alt-dd.ear"));
            ejbConfigBuilder.ejbModule = new EJBModule(false, ejbModuleName, null, null, "test-ejb-jar.jar/", null, null, null);
            webConfigBuilder.contextRoot = contextRoot;
            webConfigBuilder.webModule = new WebModule(false, webModuleName, null, null, "test-war.war/", null, null, null, contextRoot, portMap, WEB_NAMESPACE);
            connectorConfigBuilder.connectorModule = new ConnectorModule(false, raModuleName, null, null, "test-rar.rar", null, null, null);
        }

        protected void tearDown() {
            DeploymentUtil.close(earFile);
            close(ejbConfigBuilder.ejbModule);
            close(webConfigBuilder.webModule);
            close(connectorConfigBuilder.connectorModule);
        }
    };
    */
    
    protected void setUp() throws Exception {
        super.setUp();
        
        defaultParentId = new Environment();
        defaultParentId.addDependency(new Artifact("org.apache.geronimo.tests", "test", "1", "car"), ImportType.ALL);
    }

    public void testBuildConfiguration() throws Exception {
        ConfigurationData configurationData = null;
        DeploymentContext context = null;
        try {
            EARConfigBuilder configBuilder = new EARConfigBuilder(defaultParentId,
                    transactionManagerAbstractNameQuery,
                    connectionTrackerAbstractNameQuery,
                    transactionalTimerAbstractNameQuery,
                    nonTransactionalTimerAbstractNameQuery,
                    corbaGBeanAbstractNameQuery,
                    null,
                    null,
                    ejbConfigBuilder,
                    ejbConfigBuilder,
                    webConfigBuilder,
                    connectorConfigBuilder,
                    resourceReferenceBuilder,
                    appClientConfigBuilder,
                    serviceReferenceBuilder,
                    securityBuilder,
                    serviceBuilder,
                    naming);

            Object plan = configBuilder.getDeploymentPlan(null, earFile, idBuilder);
            context = configBuilder.buildConfiguration(false, configBuilder.getConfigurationID(plan, earFile, idBuilder), plan, earFile, Collections.singleton(configStore), artifactResolver, configStore);
            configurationData = getConfigurationData(context);
        } finally {
            if (context != null) {
                context.close();
            }
            if (configurationData != null) {
                DeploymentUtil.recursiveDelete(configurationData.getConfigurationDir());
            }
        }
    }

    public void testBadEJBJARConfiguration() throws Exception {
        EARConfigBuilder configBuilder = new EARConfigBuilder(defaultParentId,
                transactionManagerAbstractNameQuery,
                connectionTrackerAbstractNameQuery,
                transactionalTimerAbstractNameQuery,
                nonTransactionalTimerAbstractNameQuery,
                corbaGBeanAbstractNameQuery,
                null,
                null,
                ejbConfigBuilder,
                ejbConfigBuilder,
                webConfigBuilder,
                connectorConfigBuilder,
                resourceReferenceBuilder,
                appClientConfigBuilder,
                serviceReferenceBuilder,
                securityBuilder,
                serviceBuilder,
                naming);

        ConfigurationData configurationData = null;
        DeploymentContext context = null;
        try {
            Object plan = configBuilder.getDeploymentPlan(resolveFile("src/test/resources/plans/test-bad-ejb-jar.xml"), earFile, idBuilder);
            context = configBuilder.buildConfiguration(false, configBuilder.getConfigurationID(plan, earFile, idBuilder), plan, earFile, Collections.singleton(configStore), artifactResolver, configStore);
            configurationData = getConfigurationData(context);
            fail("Should have thrown a DeploymentException");
        } catch (DeploymentException e) {
            if (e.getCause() instanceof IOException) {
                fail("Should not be complaining about bad vendor DD for invalid module entry");
            }
        } finally {
            if (context != null) {
                context.close();
            }
            if (configurationData != null) {
                DeploymentUtil.recursiveDelete(configurationData.getConfigurationDir());
            }
        }
    }

    public void testBadWARConfiguration() throws Exception {
        EARConfigBuilder configBuilder = new EARConfigBuilder(defaultParentId,
                transactionManagerAbstractNameQuery,
                connectionTrackerAbstractNameQuery,
                transactionalTimerAbstractNameQuery,
                nonTransactionalTimerAbstractNameQuery,
                corbaGBeanAbstractNameQuery,
                null,
                null,
                ejbConfigBuilder,
                ejbConfigBuilder,
                webConfigBuilder,
                connectorConfigBuilder,
                resourceReferenceBuilder,
                appClientConfigBuilder,
                serviceReferenceBuilder,
                securityBuilder,
                serviceBuilder,
                naming);

        ConfigurationData configurationData = null;
        DeploymentContext context = null;
        try {
            Object plan = configBuilder.getDeploymentPlan(resolveFile("src/test/resources/plans/test-bad-war.xml"), earFile, idBuilder);
            context = configBuilder.buildConfiguration(false, configBuilder.getConfigurationID(plan, earFile, idBuilder), plan, earFile, Collections.singleton(configStore), artifactResolver, configStore);
            configurationData = getConfigurationData(context);
            fail("Should have thrown a DeploymentException");
        } catch (DeploymentException e) {
            if (e.getCause() instanceof IOException) {
                fail("Should not be complaining about bad vendor DD for invalid module entry");
            }
        } finally {
            if (context != null) {
                context.close();
            }
            if (configurationData != null) {
                DeploymentUtil.recursiveDelete(configurationData.getConfigurationDir());
            }
        }
    }

    public void testBadRARConfiguration() throws Exception {
        EARConfigBuilder configBuilder = new EARConfigBuilder(defaultParentId,
                transactionManagerAbstractNameQuery,
                connectionTrackerAbstractNameQuery,
                transactionalTimerAbstractNameQuery,
                nonTransactionalTimerAbstractNameQuery,
                corbaGBeanAbstractNameQuery,
                null,
                null,
                ejbConfigBuilder,
                ejbConfigBuilder,
                webConfigBuilder,
                connectorConfigBuilder,
                resourceReferenceBuilder,
                appClientConfigBuilder,
                serviceReferenceBuilder,
                securityBuilder,
                serviceBuilder,
                naming);

        ConfigurationData configurationData = null;
        DeploymentContext context = null;
        try {
            Object plan = configBuilder.getDeploymentPlan(resolveFile("src/test/resources/plans/test-bad-rar.xml"), earFile, idBuilder);
            context = configBuilder.buildConfiguration(false, configBuilder.getConfigurationID(plan, earFile, idBuilder), plan, earFile, Collections.singleton(configStore), artifactResolver, configStore);
            configurationData = getConfigurationData(context);
            fail("Should have thrown a DeploymentException");
        } catch (DeploymentException e) {
            if (e.getCause() instanceof IOException) {
                fail("Should not be complaining about bad vendor DD for invalid module entry");
            }
        } finally {
            if (context != null) {
                context.close();
            }
            if (configurationData != null) {
                DeploymentUtil.recursiveDelete(configurationData.getConfigurationDir());
            }
        }
    }

    public void testBadCARConfiguration() throws Exception {
        EARConfigBuilder configBuilder = new EARConfigBuilder(defaultParentId,
                transactionManagerAbstractNameQuery,
                connectionTrackerAbstractNameQuery,
                transactionalTimerAbstractNameQuery,
                nonTransactionalTimerAbstractNameQuery,
                corbaGBeanAbstractNameQuery,
                null,
                null,
                ejbConfigBuilder,
                ejbConfigBuilder,
                webConfigBuilder,
                connectorConfigBuilder,
                resourceReferenceBuilder,
                appClientConfigBuilder,
                serviceReferenceBuilder,
                securityBuilder,
                serviceBuilder,
                naming);

        ConfigurationData configurationData = null;
        DeploymentContext context = null;
        try {
            Object plan = configBuilder.getDeploymentPlan(resolveFile("src/test/resources/plans/test-bad-car.xml"), earFile, idBuilder);
            context = configBuilder.buildConfiguration(false, configBuilder.getConfigurationID(plan, earFile, idBuilder), plan, earFile, Collections.singleton(configStore), artifactResolver, configStore);
            configurationData = getConfigurationData(context);
            fail("Should have thrown a DeploymentException");
        } catch (DeploymentException e) {
            if (e.getCause() instanceof IOException) {
                fail("Should not be complaining about bad vendor DD for invalid module entry");
            }
        } finally {
            if (context != null) {
                context.close();
            }
            if (configurationData != null) {
                DeploymentUtil.recursiveDelete(configurationData.getConfigurationDir());
            }
        }
    }

    public void testNoEJBDeployer() throws Exception {
        EARConfigBuilder configBuilder = new EARConfigBuilder(defaultParentId,
                transactionManagerAbstractNameQuery,
                connectionTrackerAbstractNameQuery,
                transactionalTimerAbstractNameQuery,
                nonTransactionalTimerAbstractNameQuery,
                corbaGBeanAbstractNameQuery,
                null,
                null,
                null,
                null,
                webConfigBuilder,
                connectorConfigBuilder,
                resourceReferenceBuilder,
                appClientConfigBuilder,
                serviceReferenceBuilder,
                securityBuilder,
                serviceBuilder,
                naming);


        ConfigurationData configurationData = null;
        DeploymentContext context = null;
        try {
            Object plan = configBuilder.getDeploymentPlan(null, earFile, idBuilder);
            context = configBuilder.buildConfiguration(false, configBuilder.getConfigurationID(plan, earFile, idBuilder), plan, earFile, Collections.singleton(configStore), artifactResolver, configStore);
            configurationData = getConfigurationData(context);
            fail("Should have thrown a DeploymentException");
        } catch (DeploymentException e) {
            // expected
        } finally {
            if (context != null) {
                context.close();
            }
            if (configurationData != null) {
                DeploymentUtil.recursiveDelete(configurationData.getConfigurationDir());
            }
        }
    }

    public void testNoWARDeployer() throws Exception {
        EARConfigBuilder configBuilder = new EARConfigBuilder(defaultParentId,
                transactionManagerAbstractNameQuery,
                connectionTrackerAbstractNameQuery,
                transactionalTimerAbstractNameQuery,
                nonTransactionalTimerAbstractNameQuery,
                corbaGBeanAbstractNameQuery,
                null,
                null,
                ejbConfigBuilder,
                ejbConfigBuilder,
                null,
                connectorConfigBuilder,
                resourceReferenceBuilder,
                appClientConfigBuilder,
                serviceReferenceBuilder,
                securityBuilder,
                serviceBuilder,
                naming);

        ConfigurationData configurationData = null;
        DeploymentContext context = null;
        try {
            Object plan = configBuilder.getDeploymentPlan(null, earFile, idBuilder);
            context = configBuilder.buildConfiguration(false, configBuilder.getConfigurationID(plan, earFile, idBuilder), plan, earFile, Collections.singleton(configStore), artifactResolver, configStore);
            configurationData = getConfigurationData(context);
            fail("Should have thrown a DeploymentException");
        } catch (DeploymentException e) {
            // expected
        } finally {
            if (context != null) {
                context.close();
            }
            if (configurationData != null) {
                DeploymentUtil.recursiveDelete(configurationData.getConfigurationDir());
            }
        }
    }

    public void testNoConnectorDeployer() throws Exception {
        EARConfigBuilder configBuilder = new EARConfigBuilder(defaultParentId,
                transactionManagerAbstractNameQuery,
                connectionTrackerAbstractNameQuery,
                transactionalTimerAbstractNameQuery,
                nonTransactionalTimerAbstractNameQuery,
                corbaGBeanAbstractNameQuery,
                null,
                null,
                ejbConfigBuilder,
                null,
                webConfigBuilder,
                null,
                resourceReferenceBuilder,
                appClientConfigBuilder,
                serviceReferenceBuilder,
                securityBuilder,
                serviceBuilder,
                naming);

        ConfigurationData configurationData = null;
        DeploymentContext context = null;
        try {
            Object plan = configBuilder.getDeploymentPlan(null, earFile, idBuilder);
            context = configBuilder.buildConfiguration(false, configBuilder.getConfigurationID(plan, earFile, idBuilder), plan, earFile, Collections.singleton(configStore), artifactResolver, configStore);
            configurationData = getConfigurationData(context);
            fail("Should have thrown a DeploymentException");
        } catch (DeploymentException e) {
            // expected
        } finally {
            if (context != null) {
                context.close();
            }
            if (configurationData != null) {
                DeploymentUtil.recursiveDelete(configurationData.getConfigurationDir());
            }
        }
    }

    protected ConfigurationData getConfigurationData(DeploymentContext context) throws Exception {
        // add the a j2ee server so the application context reference can be resolved
        context.addGBean("geronimo", J2EEServerImpl.GBEAN_INFO);

        return context.getConfigurationData();
    }

    protected static void close(Module module) {
        if (module != null) {
            module.close();
        }
    }
}
