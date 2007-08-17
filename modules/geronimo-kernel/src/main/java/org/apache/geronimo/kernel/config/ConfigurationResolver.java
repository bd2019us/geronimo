/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.kernel.config;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apache.geronimo.kernel.repository.Artifact;
import org.apache.geronimo.kernel.repository.ArtifactResolver;
import org.apache.geronimo.kernel.repository.Dependency;
import org.apache.geronimo.kernel.repository.ImportType;
import org.apache.geronimo.kernel.repository.MissingDependencyException;
import org.apache.geronimo.kernel.repository.Repository;

/**
 * @version $Rev$ $Date$
 */
public class ConfigurationResolver {
    private final Artifact configurationId;
    private final ArtifactResolver artifactResolver;
    private final Collection<? extends Repository> repositories;

    /**
     * file or configstore used to resolve classpath parts
     */
    private final File baseDir;
    private final ConfigurationStore configurationStore;

    /**
     * For nested configurations, the module name will be non-null.
     */
    private final String moduleName;
   

    public ConfigurationResolver(Artifact configurationId, File baseDir) {
        if (configurationId == null)  throw new NullPointerException("configurationId is null");

        this.configurationId = configurationId;
        this.baseDir = baseDir;
        artifactResolver = null;
        repositories = Collections.emptySet();
        configurationStore = null;
        moduleName = null;
    }

    public ConfigurationResolver(ConfigurationData configurationData, Collection<? extends Repository> repositories, ArtifactResolver artifactResolver) {
        if (configurationData == null)  throw new NullPointerException("configurationData is null");
        if (repositories == null) repositories = Collections.emptySet();

        configurationId = configurationData.getId();
        this.artifactResolver = artifactResolver;
        this.repositories = repositories;
        configurationStore = configurationData.getConfigurationStore();
        if (null != configurationData.getInPlaceConfigurationDir()) {
            baseDir = configurationData.getInPlaceConfigurationDir();
        } else {
            baseDir = configurationData.getConfigurationDir();
        }
        moduleName = null;
    }

    private ConfigurationResolver(Artifact configurationId, ArtifactResolver artifactResolver, Collection<? extends Repository> repositories, File baseDir, ConfigurationStore configurationStore, String moduleName) {
        this.configurationId = configurationId;
        this.artifactResolver = artifactResolver;
        this.repositories = repositories;
        this.baseDir = baseDir;
        this.configurationStore = configurationStore;
        this.moduleName = moduleName;
    }

    public ConfigurationResolver createChildResolver(String moduleName) {
        if (moduleName == null) throw new NullPointerException("moduleName is null");
        if (this.moduleName != null) {
            moduleName = this.moduleName + '/' + moduleName;
        }

        File childBaseDir = null;
        if (baseDir != null) {
            childBaseDir = new File(baseDir, moduleName);
        }
        return new ConfigurationResolver(configurationId, artifactResolver, repositories, childBaseDir, configurationStore, moduleName);
    }

    public File resolve(Artifact artifact) throws MissingDependencyException {
        for (Repository repository : repositories) {
            if (repository.contains(artifact)) {
                File file = repository.getLocation(artifact);
                return file;
            }
        }
        throw new MissingDependencyException("Unable to resolve dependency " + artifact);
    }

    public Set<URL> resolve(String pattern) throws MalformedURLException, NoSuchConfigException {
        if (configurationStore != null) {
            Set<URL> matches = configurationStore.resolve(configurationId, moduleName, pattern);
            return matches;
        } else if (baseDir != null) {
            Set<URL> matches = IOUtil.search(baseDir, pattern);
            return matches;
        } else {
            throw new IllegalStateException("No configurationStore or baseDir supplied so paths can not be resolved");
        }
    }

    public List<Dependency> resolveTransitiveDependencies(Collection<Configuration> parents, List<Dependency> dependencies) throws MissingDependencyException {
        Stack<Dependency> parentStack = new Stack<Dependency>();
        return internalResolveTransitiveDependencies(parents, dependencies, parentStack);
    }

    private List<Dependency> internalResolveTransitiveDependencies(Collection<Configuration> parents, List<Dependency> dependencies, Stack<Dependency> parentStack) throws MissingDependencyException {
        List<Dependency> resolvedDependencies = new ArrayList<Dependency>();
        for (Dependency dependency1 : dependencies) {
            Dependency dependency = resolveDependency(parents, dependency1, parentStack);

            if (!resolvedDependencies.contains(dependency)) {
                resolvedDependencies.add(dependency);

                List<Dependency> childDependencies = getChildDependencies(dependency);
                if (!childDependencies.isEmpty()) {
                    parentStack.push(dependency);
                    childDependencies = internalResolveTransitiveDependencies(parents, childDependencies, parentStack);
                    parentStack.pop();
                    resolvedDependencies.addAll(childDependencies);
                }
            }
        }
        return resolvedDependencies;
    }

    private Dependency resolveDependency(Collection<Configuration> parents, Dependency dependency, Stack<Dependency> parentStack) throws MissingDependencyException {
        Artifact artifact = dependency.getArtifact();

        // we might need an artifact resolver at this point
        if (artifactResolver == null) {
            // if it is already resolved we are done
            if (artifact.isResolved()) {
                return dependency;
            }
            throw new MissingDependencyException("Artifact is not resolved and there no artifact resolver available: " + artifact);
        }
        
        // resolve the artifact
        try {
            artifact = artifactResolver.resolveInClassLoader(artifact, parents);
        } catch (MissingDependencyException e) {
            // I'm throwing away the original error as the new message is lost on the stack as
            // most folks will drill down to the message on the bottom of the stack.
            StringBuffer sb = new StringBuffer();
            sb.append(e.getMessage().trim()).append("\n"+"  Parent stack:\n");
            boolean first = true;
            for (Dependency d  : parentStack) {
                sb.append("         ").append(d.getArtifact().toString().trim()).append((first?" (top)":"")).append("\n");
                first = false;
            }
            throw new MissingDependencyException(sb.toString());
        }

        // build a new dependency object to contain the resolved artifact
        Dependency resolvedDependency = new Dependency(artifact, dependency.getImportType());
        return resolvedDependency;
    }

    private ArrayList<Dependency> getChildDependencies(Dependency dependency) {
        ArrayList<Dependency> childDependencies = new ArrayList<Dependency>();
        for (Repository repository : repositories) {
            if (repository.contains(dependency.getArtifact())) {
                // get the child artifacts
                LinkedHashSet<Artifact> childArtifacts = repository.getDependencies(dependency.getArtifact());
                for (Artifact artifact : childArtifacts) {
                    // add each child as a classes-only dependency
                    childDependencies.add(new Dependency(artifact, ImportType.CLASSES));
                }
            }
        }
        return childDependencies;
    }
}
