/**
 *
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.connector.deployment;

import java.beans.PropertyEditor;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import javax.management.AttributeNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.geronimo.common.propertyeditor.PropertyEditors;
import org.apache.geronimo.common.xml.XmlBeansUtil;
import org.apache.geronimo.connector.ActivationSpecInfo;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.LocalTransactions;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.NoPool;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.NoTransactions;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.PartitionedPool;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.PoolingSupport;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.SinglePool;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.TransactionLog;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.TransactionSupport;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.XATransactions;
import org.apache.geronimo.connector.outbound.security.PasswordCredentialRealm;
import org.apache.geronimo.deployment.DeploymentException;
import org.apache.geronimo.deployment.service.GBeanHelper;
import org.apache.geronimo.deployment.util.JarUtil;
import org.apache.geronimo.deployment.util.IOUtil;
import org.apache.geronimo.gbean.DynamicGAttributeInfo;
import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoFactory;
import org.apache.geronimo.gbean.InvalidConfigurationException;
import org.apache.geronimo.gbean.jmx.GBeanMBean;
import org.apache.geronimo.j2ee.deployment.ConnectorModule;
import org.apache.geronimo.j2ee.deployment.EARContext;
import org.apache.geronimo.j2ee.deployment.Module;
import org.apache.geronimo.j2ee.deployment.ModuleBuilder;
import org.apache.geronimo.schema.SchemaConversionUtils;
import org.apache.geronimo.xbeans.geronimo.GerAdminobjectInstanceType;
import org.apache.geronimo.xbeans.geronimo.GerAdminobjectType;
import org.apache.geronimo.xbeans.geronimo.GerConfigPropertySettingType;
import org.apache.geronimo.xbeans.geronimo.GerConnectionDefinitionType;
import org.apache.geronimo.xbeans.geronimo.GerConnectiondefinitionInstanceType;
import org.apache.geronimo.xbeans.geronimo.GerConnectionmanagerType;
import org.apache.geronimo.xbeans.geronimo.GerConnectorDocument;
import org.apache.geronimo.xbeans.geronimo.GerConnectorType;
import org.apache.geronimo.xbeans.geronimo.GerDependencyType;
import org.apache.geronimo.xbeans.geronimo.GerGbeanType;
import org.apache.geronimo.xbeans.geronimo.GerResourceadapterType;
import org.apache.geronimo.xbeans.j2ee.ActivationspecType;
import org.apache.geronimo.xbeans.j2ee.AdminobjectType;
import org.apache.geronimo.xbeans.j2ee.ConfigPropertyType;
import org.apache.geronimo.xbeans.j2ee.ConnectionDefinitionType;
import org.apache.geronimo.xbeans.j2ee.ConnectorDocument;
import org.apache.geronimo.xbeans.j2ee.ConnectorType;
import org.apache.geronimo.xbeans.j2ee.MessagelistenerType;
import org.apache.geronimo.xbeans.j2ee.RequiredConfigPropertyType;
import org.apache.geronimo.xbeans.j2ee.ResourceadapterType;
import org.apache.geronimo.xbeans.j2ee.connector_1_0.ConfigPropertyType10;
import org.apache.geronimo.xbeans.j2ee.connector_1_0.ConnectorDocument10;
import org.apache.geronimo.xbeans.j2ee.connector_1_0.ConnectorType10;
import org.apache.geronimo.xbeans.j2ee.connector_1_0.ResourceadapterType10;
import org.apache.xmlbeans.SchemaTypeLoader;
import org.apache.xmlbeans.XmlBeans;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;

/**
 * @version $Rev$ $Date$
 */
public class ConnectorModuleBuilder implements ModuleBuilder {
    private static final SchemaTypeLoader SCHEMA_TYPE_LOADER = XmlBeans.typeLoaderUnion(new SchemaTypeLoader[]{
        XmlBeans.typeLoaderForClassLoader(org.apache.geronimo.xbeans.j2ee.String.class.getClassLoader()),
        XmlBeans.typeLoaderForClassLoader(GerConnectorDocument.class.getClassLoader())
    });

    private static final String BASE_REALM_BRIDGE_NAME = "geronimo.security:service=RealmBridge,name=";
    private static final String BASE_PASSWORD_CREDENTIAL_LOGIN_MODULE_NAME = "geronimo.security:service=Realm,type=PasswordCredential,name=";
    private static final String BASE_WORK_MANAGER_NAME = "geronimo.server:type=WorkManager,name=";

    public XmlObject parseSpecDD(URL path) throws DeploymentException {
        String specDD;
        try {
            specDD = IOUtil.readAll(path);
        } catch (IOException e) {
            throw new DeploymentException("Unable to read specDD: " + path.toExternalForm());
        }
        return parseSpecDD(specDD);
    }

    public XmlObject parseSpecDD(String specDD) throws DeploymentException {
        try {
            // try 1.0
            ConnectorDocument10 connectorDoc = ConnectorDocument10.Factory.parse(specDD);
            SchemaConversionUtils.validateDD(connectorDoc);
            return connectorDoc.getConnector();
        } catch (Exception ignore) {
            // ignore
        }

        // that didn't work try 1.5
        try {
            ConnectorDocument connectorDoc = ConnectorDocument.Factory.parse(specDD);
            SchemaConversionUtils.validateDD(connectorDoc);
            return connectorDoc.getConnector();
        } catch (Exception e) {
            // ignore
        }

        throw new DeploymentException("Unable to parse specDD");
    }

    public XmlObject parseVendorDD(URL path) throws DeploymentException {
        try {
             XmlObject dd = SchemaConversionUtils.parse(path.openStream());
             return dd.changeType(GerConnectorDocument.type);
         } catch (Exception e) {
             throw new DeploymentException(e);
         }
    }

    public XmlObject getDeploymentPlan(URL module) throws DeploymentException {
        try {
            URL moduleBase;
            if (module.toString().endsWith("/")) {
                moduleBase = module;
            } else {
                moduleBase = new URL("jar:" + module.toString() + "!/");
            }
            return (GerConnectorDocument) parseVendorDD(new URL(moduleBase, "META-INF/geronimo-ra.xml"));
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public boolean canHandlePlan(XmlObject plan) {
        return plan instanceof GerConnectorDocument;
    }

    public URI getParentId(XmlObject plan) throws DeploymentException {
        GerConnectorType geronimoConnector = ((GerConnectorDocument) plan).getConnector();
        if (geronimoConnector.isSetParentId()) {
            try {
                return new URI(geronimoConnector.getParentId());
            } catch (URISyntaxException e) {
                throw new DeploymentException("Invalid parentId " + geronimoConnector.getParentId(), e);
            }
        } else {
            return null;
        }
    }

    public URI getConfigId(XmlObject plan) throws DeploymentException {
        GerConnectorType geronimoConnector = ((GerConnectorDocument) plan).getConnector();
        try {
            return new URI(geronimoConnector.getConfigId());
        } catch (URISyntaxException e) {
            throw new DeploymentException("Invalid configId " + geronimoConnector.getConfigId(), e);
        }
    }

    public Module createModule(String name, JarFile moduleFile, XmlObject vendorDD) throws DeploymentException {
        return createModule(name, moduleFile, vendorDD, "connector", null);
    }

    public Module createModule(String name, JarFile moduleFile, XmlObject vendorDD, String targetPath, URL specDDUrl) throws DeploymentException {
        URI moduleURI;
        if (targetPath != null) {
            moduleURI = URI.create(targetPath);
            if (targetPath.endsWith("/")) {
                throw new DeploymentException("targetPath must not end with a '/'");
            }
            targetPath += "/";
        } else {
            targetPath = "connector/";
            moduleURI = URI.create("");
        }


        if (specDDUrl == null) {
            specDDUrl = JarUtil.createJarURL(moduleFile, "META-INF/ra.xml");
        }
        String specDD;
        try {
            specDD = IOUtil.readAll(specDDUrl);
        } catch (IOException e) {
            throw new DeploymentException("Unable to read specDD: " + specDDUrl.toExternalForm());
        }
        XmlObject connector = parseSpecDD(specDD);

        if (vendorDD == null) {
            try {
                JarEntry entry = moduleFile.getJarEntry("META-INF/geronimo-ra.xml");
                if (entry != null) {
                    InputStream in = moduleFile.getInputStream(entry);
                    if (in != null) {
                        vendorDD = XmlBeansUtil.parse(in, GerConnectorDocument.type);
                    }
                }
            } catch (Exception e) {
                throw new DeploymentException("Unable to parse META-INF/geronimo-ra.xml", e);
            }
        }
        if (vendorDD == null) {
            throw new DeploymentException("No geronimo-ra.xml.");
        }

        GerConnectorDocument geronimoConnectorDoc = (GerConnectorDocument) vendorDD;
        GerConnectorType geronimoConnector = geronimoConnectorDoc.getConnector();

        return new ConnectorModule(name, moduleURI, moduleFile, targetPath, connector, geronimoConnector, specDD);
    }

    public void installModule(JarFile earFile, EARContext earContext, Module module) throws DeploymentException {
        try {
            URI targetURI = URI.create(module.getTargetPath());

            JarFile moduleFile = module.getModuleFile();

            Enumeration entries = moduleFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                URI target = targetURI.resolve(entry.getName());
                InputStream in = moduleFile.getInputStream(entry);
                try {
                    if (entry.getName().endsWith(".jar")) {
                        earContext.addStreamInclude(target, in);
                    } else {
                        earContext.addFile(target, in);
                    }
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                    }
                }
            }

            GerConnectorType vendorConnector = (GerConnectorType) module.getVendorDD();
            GerDependencyType[] dependencies = vendorConnector.getDependencyArray();
            for (int i = 0; i < dependencies.length; i++) {
                earContext.addDependency(getDependencyURI(dependencies[i]));
            }
        } catch (IOException e) {
            throw new DeploymentException("Problem deploying connector", e);
        }
    }

    public void initContext(EARContext earContext, Module module, ClassLoader cl) throws DeploymentException {
        // connectors do not add anything to the shared context
        // TODO should the 1.5 ActivationSpecInfos be processed here? YES
    }

    public void addGBeans(EARContext earContext, Module module, ClassLoader cl) throws DeploymentException {
        addResourceAdapterModuleGBean(earContext, module, cl);

        GerConnectorType geronimoConnector = (GerConnectorType) module.getVendorDD();
        XmlObject specDD = module.getSpecDD();
        if (specDD instanceof ConnectorType10) {
            addConnectorGBeans(earContext, (ConnectorType10) specDD, geronimoConnector, cl);
        } else {
            addConnectorGBeans(earContext, (ConnectorModule) module, (ConnectorType) specDD, geronimoConnector, cl);
        }

        GerGbeanType[] gbeans = geronimoConnector.getGbeanArray();
        for (int i = 0; i < gbeans.length; i++) {
            GBeanHelper.addGbean(new RARGBeanAdapter(gbeans[i]), cl, earContext);
        }
    }

    public SchemaTypeLoader getSchemaTypeLoader() {
        return SCHEMA_TYPE_LOADER;
    }

    private void addResourceAdapterModuleGBean(EARContext earContext, Module module, ClassLoader cl) throws DeploymentException {
        // build the objectName
        Properties nameProps = new Properties();
        nameProps.put("j2eeType", "ResourceAdapterModule");
        nameProps.put("name", module.getName());
        nameProps.put("J2EEServer", earContext.getJ2EEServerName());
        nameProps.put("J2EEApplication", earContext.getJ2EEApplicationName());

        ObjectName resourceAdapterModuleObjectName = null;
        try {
            resourceAdapterModuleObjectName = new ObjectName(earContext.getJ2EEDomainName(), nameProps);
        } catch (MalformedObjectNameException e) {
            throw new DeploymentException("Unable to construct ResourceAdapterModule ObjectName", e);
        }

        // initalize the GBean
        GBeanMBean resourceAdapterModule = new GBeanMBean(ResouceAdapterModuleImpl.GBEAN_INFO, cl);
        try {
            resourceAdapterModule.setReferencePatterns("J2EEServer", Collections.singleton(earContext.getServerObjectName()));
            if (!earContext.getJ2EEApplicationName().equals("null")) {
                resourceAdapterModule.setReferencePatterns("J2EEApplication", Collections.singleton(earContext.getApplicationObjectName()));
            }

            resourceAdapterModule.setAttribute("deploymentDescriptor", module.getOriginalSpecDD());
        } catch (Exception e) {
            throw new DeploymentException("Unable to initialize EJBModule GBean", e);
        }

        // add it
        earContext.addGBean(resourceAdapterModuleObjectName, resourceAdapterModule);
    }

    private void addConnectorGBeans(EARContext context, ConnectorType10 connector, GerConnectorType geronimoConnector, ClassLoader cl) throws DeploymentException {
        ResourceadapterType10 resourceAdapter = connector.getResourceadapter();
        GerResourceadapterType geronimoResourceAdapter = geronimoConnector.getResourceadapter();
        for (int i = 0; i < geronimoResourceAdapter.getOutboundResourceadapter().getConnectionDefinitionArray().length; i++) {
            GerConnectionDefinitionType geronimoConnectionDefinition = geronimoResourceAdapter.getOutboundResourceadapter().getConnectionDefinitionArray(i);
            assert geronimoConnectionDefinition != null: "Null GeronimoConnectionDefinition";

            for (int j = 0; j < geronimoConnectionDefinition.getConnectiondefinitionInstanceArray().length; j++) {
                GerConnectiondefinitionInstanceType connectionfactoryInstance = geronimoConnectionDefinition.getConnectiondefinitionInstanceArray()[j];
                String managedConnectionFactoryClass = resourceAdapter.getManagedconnectionfactoryClass().getStringValue();
                String connectionFactoryInterface = resourceAdapter.getConnectionfactoryInterface().getStringValue();
                String connectionFactoryImplClass = resourceAdapter.getConnectionfactoryImplClass().getStringValue();
                String connectionInterface = resourceAdapter.getConnectionInterface().getStringValue();
                String connectionImplClass = resourceAdapter.getConnectionImplClass().getStringValue();
                ConfigProperty[] configProperties = getConfigProperties(resourceAdapter.getConfigPropertyArray(), connectionfactoryInstance.getConfigPropertySettingArray());

                addOutboundGBeans(context, null, connectionfactoryInstance, configProperties, managedConnectionFactoryClass, connectionFactoryInterface, connectionFactoryImplClass, connectionInterface, connectionImplClass, cl);
            }
        }
    }

    private void addConnectorGBeans(EARContext earContext, ConnectorModule module, ConnectorType connector, GerConnectorType geronimoConnector, ClassLoader cl) throws DeploymentException {
        ResourceadapterType resourceadapter = connector.getResourceadapter();
        GerResourceadapterType geronimoResourceAdapter = geronimoConnector.getResourceadapter();

        //
        // Resource Adapter
        //

        ObjectName resourceAdapterObjectName = null;
        if (resourceadapter.isSetResourceadapterClass()) {
            String resourceAdapterName = geronimoResourceAdapter.getResourceadapterInstance().getResourceadapterName();

            // Create the resource adapter gbean
            GBeanInfoFactory resourceAdapterInfoFactory = new GBeanInfoFactory("org.apache.geronimo.connector.ResourceAdapterWrapper", cl);
            ConfigProperty[] configProperties = getConfigProperties(resourceadapter.getConfigPropertyArray(), geronimoResourceAdapter.getResourceadapterInstance().getConfigPropertySettingArray());
            GBeanMBean resourceAdapterGBean = setUpDynamicGBean(resourceAdapterInfoFactory, configProperties, cl);

            // set the resource adapter class and activationSpec info map
            try {
                resourceAdapterGBean.setAttribute("resourceAdapterClass", cl.loadClass(resourceadapter.getResourceadapterClass().getStringValue()));
                if (resourceadapter.isSetInboundResourceadapter() && resourceadapter.getInboundResourceadapter().isSetMessageadapter()) {
                    //get the ActivationSpec metadata as GBeanInfos
                    Map activationSpecInfoMap = getActivationSpecInfoMap(resourceadapter.getInboundResourceadapter().getMessageadapter().getMessagelistenerArray(), cl);
                    resourceAdapterGBean.setAttribute("activationSpecInfoMap", activationSpecInfoMap);
                    earContext.addResourceAdapter(resourceAdapterName, module.getName(), activationSpecInfoMap);
                }
            } catch (Exception e) {
                throw new DeploymentException("Could not set ResourceAdapterClass", e);
            }

            // set the work manager name
            try {
                resourceAdapterGBean.setReferencePattern("WorkManager",
                        ObjectName.getInstance(BASE_WORK_MANAGER_NAME + geronimoResourceAdapter.getResourceadapterInstance().getWorkmanagerName().getStringValue()));
            } catch (MalformedObjectNameException e) {
                throw new DeploymentException("Could not create object name for work manager", e);
            }

            // add it
            try {
                Properties nameProps = new Properties();
                nameProps.put("j2eeType", "ResourceAdapter");
                nameProps.put("name", resourceAdapterName);
                nameProps.put("J2EEServer", earContext.getJ2EEServerName());
                nameProps.put("J2EEApplication", earContext.getJ2EEApplicationName());
                nameProps.put("ResourceAdapterModule", module.getName());

                resourceAdapterObjectName = new ObjectName(earContext.getJ2EEDomainName(), nameProps);
            } catch (MalformedObjectNameException e) {
                throw new DeploymentException("Could not construct resource adapter object name", e);
            }
            earContext.addGBean(resourceAdapterObjectName, resourceAdapterGBean);
        }

        //
        // Outbound Managed Connectopn Factories (think JDBC data source or JMS connection factory)
        //

        // first we need a map of the published outbound adaptors by connection factory interface type
        Map connectionDefinitions = new HashMap();
        if (resourceadapter.isSetOutboundResourceadapter()) {
            for (int j = 0; j < resourceadapter.getOutboundResourceadapter().getConnectionDefinitionArray().length; j++) {
                ConnectionDefinitionType connectionDefinition = resourceadapter.getOutboundResourceadapter().getConnectionDefinitionArray(j);
                connectionDefinitions.put(connectionDefinition.getConnectionfactoryInterface().getStringValue(), connectionDefinition);
            }

            // ManagedConnectionFactory setup
            if (geronimoResourceAdapter.isSetOutboundResourceadapter()) {
                for (int i = 0; i < geronimoResourceAdapter.getOutboundResourceadapter().getConnectionDefinitionArray().length; i++) {
                    GerConnectionDefinitionType geronimoConnectionDefinition = geronimoResourceAdapter.getOutboundResourceadapter().getConnectionDefinitionArray(i);
                    assert geronimoConnectionDefinition != null: "Null GeronimoConnectionDefinition";

                    String connectionFactoryInterfaceName = geronimoConnectionDefinition.getConnectionfactoryInterface().getStringValue();
                    ConnectionDefinitionType connectionDefinition = (ConnectionDefinitionType) connectionDefinitions.get(connectionFactoryInterfaceName);
                    if (connectionDefinition == null) {
                        throw new DeploymentException("No connection definition for ConnectionFactory class: " + connectionFactoryInterfaceName);
                    }

                    for (int j = 0; j < geronimoConnectionDefinition.getConnectiondefinitionInstanceArray().length; j++) {
                        GerConnectiondefinitionInstanceType connectionfactoryInstance = geronimoConnectionDefinition.getConnectiondefinitionInstanceArray()[j];
                        String managedConnectionFactoryClass = connectionDefinition.getManagedconnectionfactoryClass().getStringValue();
                        String connectionFactoryInterface = connectionDefinition.getConnectionfactoryInterface().getStringValue();
                        String connectionFactoryImplClass = connectionDefinition.getConnectionfactoryImplClass().getStringValue();
                        String connectionInterface = connectionDefinition.getConnectionInterface().getStringValue();
                        String connectionImplClass = connectionDefinition.getConnectionImplClass().getStringValue();
                        ConfigProperty[] configProperties = getConfigProperties(connectionDefinition.getConfigPropertyArray(), connectionfactoryInstance.getConfigPropertySettingArray());

                        addOutboundGBeans(earContext, resourceAdapterObjectName, connectionfactoryInstance, configProperties, managedConnectionFactoryClass, connectionFactoryInterface, connectionFactoryImplClass, connectionInterface, connectionImplClass, cl);
                    }
                }
            }
        }

        //
        // admin objects (think message queuse and topics)
        //

        // first we need a map of the published admin objects by interface type
        Map adminObjectInterfaceMap = new HashMap();
        for (int i = 0; i < resourceadapter.getAdminobjectArray().length; i++) {
            AdminobjectType adminobject = resourceadapter.getAdminobjectArray()[i];
            adminObjectInterfaceMap.put(adminobject.getAdminobjectInterface().getStringValue(), adminobject);
        }

        // add configured admin objects
        for (int i = 0; i < geronimoResourceAdapter.getAdminobjectArray().length; i++) {
            GerAdminobjectType gerAdminObject = geronimoResourceAdapter.getAdminobjectArray()[i];

            String adminObjectInterface = gerAdminObject.getAdminobjectInterface().getStringValue();
            AdminobjectType adminObject = (AdminobjectType) adminObjectInterfaceMap.get(adminObjectInterface);
            if (adminObject == null) {
                throw new DeploymentException("No admin object declared for interface: " + adminObjectInterface);
            }

            for (int j = 0; j < gerAdminObject.getAdminobjectInstanceArray().length; j++) {
                GerAdminobjectInstanceType gerAdminObjectInstance = gerAdminObject.getAdminobjectInstanceArray()[j];

                // create the adminObjectGBean
                GBeanInfoFactory adminObjectInfoFactory = new GBeanInfoFactory("org.apache.geronimo.connector.AdminObjectWrapper", cl);
                ConfigProperty[] configProperties = getConfigProperties(adminObject.getConfigPropertyArray(), gerAdminObjectInstance.getConfigPropertySettingArray());
                GBeanMBean adminObjectGBean = setUpDynamicGBean(adminObjectInfoFactory, configProperties, cl);

                // set the standard properties
                try {
                    adminObjectGBean.setAttribute("adminObjectInterface", cl.loadClass(adminObjectInterface));
                    adminObjectGBean.setAttribute("adminObjectClass", cl.loadClass(adminObject.getAdminobjectClass().getStringValue()));
                } catch (Exception e) {
                    throw new DeploymentException("Could not initialize AdminObject", e);
                }

                // add it
                try {
                    Properties nameProps = new Properties();
                    nameProps.put("j2eeType", "JCAAdminObject");
                    nameProps.put("name", gerAdminObjectInstance.getMessageDestinationName());
                    nameProps.put("J2EEServer", earContext.getJ2EEServerName());

                    ObjectName adminObjectObjectName = new ObjectName(earContext.getJ2EEDomainName(), nameProps);
                    earContext.addGBean(adminObjectObjectName, adminObjectGBean);
                } catch (MalformedObjectNameException e) {
                    throw new DeploymentException("Could not create object name for admin object", e);
                }
            }
        }
    }

    private Map getActivationSpecInfoMap(MessagelistenerType[] messagelistenerArray, ClassLoader cl) throws DeploymentException {
        Map activationSpecInfos = new HashMap();
        for (int i = 0; i < messagelistenerArray.length; i++) {
            MessagelistenerType messagelistenerType = messagelistenerArray[i];
            ActivationspecType activationspec = messagelistenerType.getActivationspec();
            String activationSpecClassName = activationspec.getActivationspecClass().getStringValue();
            GBeanInfoFactory infoFactory = new GBeanInfoFactory("org.apache.geronimo.connector.ActivationSpecWrapper", cl);
            for (int j = 0; j < activationspec.getRequiredConfigPropertyArray().length; j++) {
                RequiredConfigPropertyType requiredConfigPropertyType = activationspec.getRequiredConfigPropertyArray()[j];
                String propertyName = requiredConfigPropertyType.getConfigPropertyName().getStringValue();
                infoFactory.addAttribute(new DynamicGAttributeInfo(propertyName, true));
            }
            GBeanInfo gbeanInfo = infoFactory.getBeanInfo();
            Class activationSpecClass = null;
            try {
                activationSpecClass = cl.loadClass(activationSpecClassName);
            } catch (ClassNotFoundException e) {
                throw new DeploymentException("Could not load ActivationSpec class", e);
            }
            ActivationSpecInfo activationSpecInfo = new ActivationSpecInfo(activationSpecClass, gbeanInfo);
            activationSpecInfos.put(activationSpecClassName, activationSpecInfo);
        }
        return activationSpecInfos;
    }

    private GBeanMBean setUpDynamicGBean(GBeanInfoFactory infoFactory, ConfigProperty[] configProperties, ClassLoader cl) throws DeploymentException {
        for (int i = 0; i < configProperties.length; i++) {
            infoFactory.addAttribute(new DynamicGAttributeInfo(configProperties[i].getName(), true));
        }

        GBeanInfo gbeanInfo = infoFactory.getBeanInfo();
        GBeanMBean gbean;
        try {
            gbean = new GBeanMBean(gbeanInfo, cl);
        } catch (InvalidConfigurationException e) {
            throw new DeploymentException("Unable to create GMBean", e);
        }

        try {
            for (int i = 0; i < configProperties.length; i++) {
                ConfigProperty configProperty = configProperties[i];

                setAttributeValue(gbean,
                        configProperty.getName(),
                        configProperty.getType(),
                        configProperty.getExplicitValue(),
                        configProperty.getDefaultValue());
            }
        } catch (DeploymentException e) {
            throw e;
        } catch (Exception e) {
            throw new DeploymentException(e);
        }
        return gbean;
    }

    private ConfigProperty[] getConfigProperties(ConfigPropertyType10[] configPropertyTypes, GerConfigPropertySettingType[] configPropertySettings) {
        Map explicitValues = getExplicitValuesMap(configPropertySettings);

        ConfigProperty[] configProperties = new ConfigProperty[configPropertyTypes.length];
        for (int i = 0; i < configPropertyTypes.length; i++) {
            ConfigPropertyType10 configPropertyType = configPropertyTypes[i];
            String name = configPropertyType.getConfigPropertyName().getStringValue();
            String type = configPropertyType.getConfigPropertyType().getStringValue();
            String explicitValue = (String) explicitValues.get(name);
            String defaultValue;
            if (configPropertyType.getConfigPropertyValue() != null) {
                defaultValue = configPropertyType.getConfigPropertyValue().getStringValue();
            } else {
                defaultValue = null;
            }
            configProperties[i] = new ConfigProperty(name, type, explicitValue, defaultValue);
        }
        return configProperties;
    }

    private ConfigProperty[] getConfigProperties(ConfigPropertyType[] configPropertyTypes, GerConfigPropertySettingType[] configPropertySettings) {
        Map explicitValues = getExplicitValuesMap(configPropertySettings);

        ConfigProperty[] configProperties = new ConfigProperty[configPropertyTypes.length];
        for (int i = 0; i < configPropertyTypes.length; i++) {
            ConfigPropertyType configPropertyType = configPropertyTypes[i];
            String name = configPropertyType.getConfigPropertyName().getStringValue();
            String type = configPropertyType.getConfigPropertyType().getStringValue();
            String explicitValue = (String) explicitValues.get(name);
            String defaultValue = null;
            if (configPropertyType.isSetConfigPropertyValue()) {
                defaultValue = configPropertyType.getConfigPropertyValue().getStringValue();
            }
            configProperties[i] = new ConfigProperty(name, type, explicitValue, defaultValue);
        }
        return configProperties;
    }

    private Map getExplicitValuesMap(GerConfigPropertySettingType[] configPropertySettings) {
        Map explicitValues = new HashMap();
        for (int j = 0; j < configPropertySettings.length; j++) {
            GerConfigPropertySettingType configPropertySetting = configPropertySettings[j];
            String name = configPropertySetting.getName();
            String value = configPropertySetting.getStringValue();
            explicitValues.put(name, value);
        }
        return explicitValues;
    }

    private static void setAttributeValue(GBeanMBean gbean, String name, String type, String explicitValue, String defaultValue) throws DeploymentException, ReflectionException, AttributeNotFoundException {
        if (explicitValue == null && defaultValue == null) {
            return;
        }

        Class clazz;
        try {
            clazz = gbean.getClassLoader().loadClass(type);
        } catch (ClassNotFoundException e) {
            throw new DeploymentException("Could not load attribute class: attribute: " + name + ", type: " + type, e);
        }

        PropertyEditor editor = PropertyEditors.getEditor(clazz);
        if (explicitValue != null) {
            editor.setAsText(explicitValue);
        } else {
            editor.setAsText(defaultValue);
        }
        Object value = editor.getValue();

        gbean.setAttribute(name, value);
    }

    private ObjectName configureConnectionManager(GerConnectiondefinitionInstanceType connectionfactoryInstance, EARContext earContext, ClassLoader cl) throws DeploymentException {
        if (connectionfactoryInstance.getConnectionmanagerRef() != null) {
            //we don't configure anything, just use the supplied gbean
            try {
                return ObjectName.getInstance(connectionfactoryInstance.getConnectionmanagerRef());
            } catch (MalformedObjectNameException e) {
                throw new DeploymentException("Invalid ObjectName string supplied for ConnectionManager reference", e);
            }
        }

        //we configure our connection manager
        GerConnectionmanagerType connectionManager = connectionfactoryInstance.getConnectionmanager();
        GBeanMBean connectionManagerGBean;
        try {
            connectionManagerGBean = new GBeanMBean(GBeanInfo.getGBeanInfo("org.apache.geronimo.connector.outbound.GenericConnectionManager", cl), cl);
        } catch (InvalidConfigurationException e) {
            throw new DeploymentException("Unable to create GMBean", e);
        }
        TransactionSupport transactionSupport = null;
        if (connectionManager.getNoTransaction() != null) {
            transactionSupport = NoTransactions.INSTANCE;
        } else if (connectionManager.getLocalTransaction() != null) {
            transactionSupport = LocalTransactions.INSTANCE;
        } else if (connectionManager.getTransactionLog() != null) {
            transactionSupport = TransactionLog.INSTANCE;
        } else if (connectionManager.getXaTransaction() != null) {
            transactionSupport = new XATransactions(connectionManager.getXaTransaction().getTransactionCaching() != null,
                    connectionManager.getXaTransaction().getThreadCaching() != null);
        } else {
            throw new DeploymentException("Unexpected transaction support element");
        }
        PoolingSupport pooling = null;
        if (connectionManager.getSinglePool() != null) {
            pooling = new SinglePool(connectionManager.getSinglePool().getMaxSize(),
                    connectionManager.getSinglePool().getBlockingTimeoutMilliseconds(),
                    connectionManager.getSinglePool().getMatchOne() != null,
                    connectionManager.getSinglePool().getMatchAll() != null,
                    connectionManager.getSinglePool().getSelectOneAssumeMatch() != null);
        } else if (connectionManager.getPartitionedPool() != null) {
            pooling = new PartitionedPool(connectionManager.getPartitionedPool().getPartitionByConnectionrequestinfo() != null,
                    connectionManager.getPartitionedPool().getPartitionBySubject() != null,
                    connectionManager.getPartitionedPool().getMaxSize(),
                    connectionManager.getPartitionedPool().getBlockingTimeoutMilliseconds(),
                    connectionManager.getPartitionedPool().getMatchOne() != null,
                    connectionManager.getPartitionedPool().getMatchAll() != null,
                    connectionManager.getPartitionedPool().getSelectOneAssumeMatch() != null);
        } else if (connectionManager.getNoPool() != null) {
            pooling = new NoPool();
        } else {
            throw new DeploymentException("Unexpected pooling support element");
        }
        try {
            connectionManagerGBean.setAttribute("name", connectionfactoryInstance.getName());
            connectionManagerGBean.setAttribute("transactionSupport", transactionSupport);
            connectionManagerGBean.setAttribute("pooling", pooling);
            connectionManagerGBean.setReferencePattern("ConnectionTracker", earContext.getConnectionTrackerObjectName());
            if (connectionManager.getRealmBridge() != null) {
                connectionManagerGBean.setReferencePattern("RealmBridge", ObjectName.getInstance(BASE_REALM_BRIDGE_NAME + connectionManager.getRealmBridge()));
            }
        } catch (Exception e) {
            throw new DeploymentException("Problem setting up ConnectionManager", e);
        }

        // add it
        try {
            Properties nameProps = new Properties();
            nameProps.put("j2eeType", "ConnectionManager");
            nameProps.put("name", connectionfactoryInstance.getName());
            nameProps.put("J2EEServer", earContext.getJ2EEServerName());

            ObjectName connectionManagerFactoryObjectName = new ObjectName(earContext.getJ2EEDomainName(), nameProps);
            earContext.addGBean(connectionManagerFactoryObjectName, connectionManagerGBean);
            return connectionManagerFactoryObjectName;
        } catch (MalformedObjectNameException e) {
            throw new DeploymentException("Could not name ConnectionManager", e);
        }
    }

    private void addOutboundGBeans(EARContext earContext, ObjectName resourceAdapterObjectName, GerConnectiondefinitionInstanceType connectionfactoryInstance, ConfigProperty[] configProperties, String managedConnectionFactoryClass, String connectionFactoryInterface, String connectionFactoryImplClass, String connectionInterface, String connectionImplClass, ClassLoader cl) throws DeploymentException {
        // ConnectionManager
        ObjectName connectionManagerObjectName = configureConnectionManager(connectionfactoryInstance, earContext, cl);

        // ManagedConnectionFactory
        GBeanInfoFactory managedConnectionFactoryInfoFactory = new GBeanInfoFactory("org.apache.geronimo.connector.outbound.ManagedConnectionFactoryWrapper", cl);
        GBeanMBean managedConnectionFactoryGBean = setUpDynamicGBean(managedConnectionFactoryInfoFactory, configProperties, cl);
        try {
            managedConnectionFactoryGBean.setAttribute("managedConnectionFactoryClass", cl.loadClass(managedConnectionFactoryClass));
            managedConnectionFactoryGBean.setAttribute("connectionFactoryInterface", cl.loadClass(connectionFactoryInterface));
            managedConnectionFactoryGBean.setAttribute("connectionFactoryImplClass", cl.loadClass(connectionFactoryImplClass));
            managedConnectionFactoryGBean.setAttribute("connectionInterface", cl.loadClass(connectionInterface));
            managedConnectionFactoryGBean.setAttribute("connectionImplClass", cl.loadClass(connectionImplClass));
            managedConnectionFactoryGBean.setAttribute("globalJNDIName", connectionfactoryInstance.getGlobalJndiName());
            if (resourceAdapterObjectName != null) {
                managedConnectionFactoryGBean.setReferencePattern("ResourceAdapterWrapper", resourceAdapterObjectName);
            }
            managedConnectionFactoryGBean.setReferencePattern("ConnectionManagerFactory", connectionManagerObjectName);
            if (connectionfactoryInstance.getCredentialInterface() != null && "javax.resource.spi.security.PasswordCredential".equals(connectionfactoryInstance.getCredentialInterface().getStringValue())) {
                GBeanMBean realmGBean = new GBeanMBean(PasswordCredentialRealm.getGBeanInfo(), cl);
                realmGBean.setAttribute("realmName", BASE_PASSWORD_CREDENTIAL_LOGIN_MODULE_NAME + connectionfactoryInstance.getName());
                ObjectName realmObjectNam = ObjectName.getInstance(BASE_PASSWORD_CREDENTIAL_LOGIN_MODULE_NAME + connectionfactoryInstance.getName());
                earContext.addGBean(realmObjectNam, realmGBean);
                managedConnectionFactoryGBean.setReferencePattern("ManagedConnectionFactoryListener", realmObjectNam);
            }
        } catch (Exception e) {
            throw new DeploymentException(e);
        }

        ObjectName managedConnectionFactoryObjectName;
        try {
            Properties nameProps = new Properties();
            nameProps.put("j2eeType", "JCAManagedConnectionFactory");
            nameProps.put("name", connectionfactoryInstance.getName());
            nameProps.put("J2EEServer", earContext.getJ2EEServerName());

            managedConnectionFactoryObjectName = new ObjectName(earContext.getJ2EEDomainName(), nameProps);
            earContext.addGBean(managedConnectionFactoryObjectName, managedConnectionFactoryGBean);
        } catch (MalformedObjectNameException e) {
            throw new DeploymentException("Could not construct ManagedConnectionFactory object name", e);
        }

        // ConnectionFactory
        GBeanMBean connectionFactory = new GBeanMBean(JCAConnectionFactoryImpl.GBEAN_INFO, cl);
        connectionFactory.setReferencePatterns("J2EEServer", Collections.singleton(earContext.getServerObjectName()));
        try {
            connectionFactory.setAttribute("managedConnectionFactory", managedConnectionFactoryObjectName.getCanonicalName());
        } catch (Exception e) {
            throw new DeploymentException("Could not initialize JCAConnectionFactory", e);
        }

        try {
            Properties nameProps = new Properties();
            nameProps.put("j2eeType", "JCAConnectionFactory");
            nameProps.put("name", connectionfactoryInstance.getName());
            if (resourceAdapterObjectName == null) {
                nameProps.put("JCAResource", "null");
            } else {
                nameProps.put("JCAResource", resourceAdapterObjectName.getKeyProperty("name"));
            }
            nameProps.put("J2EEServer", earContext.getJ2EEServerName());

            ObjectName connectionFactoryObjectName = new ObjectName(earContext.getJ2EEDomainName(), nameProps);
            earContext.addGBean(connectionFactoryObjectName, connectionFactory);
        } catch (MalformedObjectNameException e) {
            throw new DeploymentException("Could not construct ConnectionFactory object name", e);
        }
    }

    private static URI getDependencyURI(GerDependencyType dependency) throws DeploymentException {
        if (dependency.isSetUri()) {
            try {
                return new URI(dependency.getUri());
            } catch (URISyntaxException e) {
                throw new DeploymentException("Invalid dependency URI " + dependency.getUri(), e);
            }
        } else {
            String id = dependency.getGroupId() + "/jars/" + dependency.getArtifactId() + '-' + dependency.getVersion() + ".jar";
            try {
                return new URI(id);
            } catch (URISyntaxException e) {
                throw new DeploymentException("Unable to construct URI for groupId=" + dependency.getGroupId() + ", artifactId=" + dependency.getArtifactId() + ", version=" + dependency.getVersion(), e);
            }
        }
    }

    private final static class ConfigProperty {
        private final String name;
        private final String type;
        private final String explicitValue;
        private final String defaultValue;

        public ConfigProperty(String name, String type, String explicitValue, String defaultValue) {
            this.name = name;
            this.type = type;
            this.explicitValue = explicitValue;
            this.defaultValue = defaultValue;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getExplicitValue() {
            return explicitValue;
        }

        public String getDefaultValue() {
            return defaultValue;
        }
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoFactory infoFactory = new GBeanInfoFactory(ConnectorModuleBuilder.class);
        infoFactory.addInterface(ModuleBuilder.class);
        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}
