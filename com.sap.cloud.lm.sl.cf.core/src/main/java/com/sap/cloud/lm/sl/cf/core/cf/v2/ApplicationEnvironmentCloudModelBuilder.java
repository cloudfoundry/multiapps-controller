package com.sap.cloud.lm.sl.cf.core.cf.v2;

import static com.sap.cloud.lm.sl.common.util.ListUtil.asList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.v1.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.PropertiesAccessor;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorHandler;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Module;
import com.sap.cloud.lm.sl.mta.model.v2.RequiredDependency;

public class ApplicationEnvironmentCloudModelBuilder {

    private static final int MTA_MAJOR_VERSION = 2;
    
    protected CloudModelConfiguration configuration;
    protected DeploymentDescriptor deploymentDescriptor;
    protected XsPlaceholderResolver xsPlaceholderResolver;
    protected DescriptorHandler handler;
    protected PropertiesAccessor propertiesAccessor;
    protected String deployId;

    public ApplicationEnvironmentCloudModelBuilder(CloudModelConfiguration configuration, DeploymentDescriptor deploymentDescriptor,
        XsPlaceholderResolver xsPlaceholderResolver, DescriptorHandler handler, PropertiesAccessor propertiesAccessor, String deployId) {
        this.configuration = configuration;
        this.deploymentDescriptor = deploymentDescriptor;
        this.xsPlaceholderResolver = xsPlaceholderResolver;
        this.handler = handler;
        this.propertiesAccessor = propertiesAccessor;
        this.deployId = deployId;
    }

    protected void addToGroup(Map<String, List<Object>> groups, String group, String name, Map<String, Object> properties) {
        groups.computeIfAbsent(group, key -> new ArrayList<>())
            .add(properties);
    }

    protected void addDependencies(Map<String, Object> env, com.sap.cloud.lm.sl.mta.model.v1.Module module) {
        addDependencies(env, (Module) module);
    }

    protected void addDependencies(Map<String, Object> env, Module module) {
        Map<String, List<Object>> groupsMap = new TreeMap<>();
        for (RequiredDependency requiredDependency : module.getRequiredDependencies2()) {
            addDependency(requiredDependency, env, groupsMap);
        }
        env.putAll(groupsMap);
    }

    protected void addDependency(RequiredDependency dependency, Map<String, Object> env, Map<String, List<Object>> groups) {
        if (dependency.getList() != null) {
            dependency.setGroup(dependency.getList());
        }
        addToGroupsOrEnvironment(env, groups, asList(dependency.getGroup()), dependency.getName(), dependency.getProperties());
    }
    
    protected void addToGroupsOrEnvironment(Map<String, Object> env, Map<String, List<Object>> groups, List<String> destinationGroups,
        String subgroupName, Map<String, Object> properties) {
        if (!destinationGroups.isEmpty()) {
            addToGroups(groups, destinationGroups, subgroupName, properties);
        } else {
            properties.forEach(env::put);
        }
    }

    protected void addToGroups(Map<String, List<Object>> groups, List<String> destinationGroups, String subgroupName,
        Map<String, Object> properties) {
        for (String group : destinationGroups) {
            addToGroup(groups, group, subgroupName, properties);
        }
    }
    
    protected HandlerFactory getHandlerFactory() {
        return new HandlerFactory(MTA_MAJOR_VERSION);
    }
}
