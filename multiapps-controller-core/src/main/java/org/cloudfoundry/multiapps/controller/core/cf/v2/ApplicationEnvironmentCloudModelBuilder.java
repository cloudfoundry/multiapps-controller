package org.cloudfoundry.multiapps.controller.core.cf.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.cloudfoundry.multiapps.common.util.ListUtil;
import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.core.cf.CloudHandlerFactory;
import org.cloudfoundry.multiapps.controller.core.helpers.MapToEnvironmentConverter;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;

public class ApplicationEnvironmentCloudModelBuilder {

    private static final int MTA_MAJOR_VERSION = 2;

    protected final DeploymentDescriptor deploymentDescriptor;
    protected final String deployId;
    protected final String namespace;
    protected final boolean prettyPrinting;

    public ApplicationEnvironmentCloudModelBuilder(DeploymentDescriptor deploymentDescriptor, String deployId, String namespace,
                                                   boolean prettyPrinting) {
        this.deploymentDescriptor = deploymentDescriptor;
        this.deployId = deployId;
        this.namespace = namespace;
        this.prettyPrinting = prettyPrinting;
    }

    public Map<String, String> build(Module module) {
        Map<String, Object> properties = module.getProperties();
        Map<String, Object> parameters = module.getParameters();
        Map<String, Object> env = new TreeMap<>();
        addAttributes(env, parameters);
        addProperties(env, properties);
        addDependencies(env, module);
        return new MapToEnvironmentConverter(prettyPrinting).asEnv(env);
    }

    protected void addAttributes(Map<String, Object> env, Map<String, Object> properties) {
        Map<String, Object> attributes = new TreeMap<>(properties);
        attributes.keySet()
                  .retainAll(SupportedParameters.APP_ATTRIBUTES);
        if (!attributes.isEmpty()) {
            env.put(Constants.ENV_DEPLOY_ATTRIBUTES, attributes);
        }
        Boolean checkDeployId = (Boolean) attributes.get(SupportedParameters.CHECK_DEPLOY_ID);
        if (checkDeployId != null && checkDeployId) {
            env.put(Constants.ENV_DEPLOY_ID, deployId);
        }
    }

    protected void addProperties(Map<String, Object> env, Map<String, Object> properties) {
        env.putAll(properties);
    }

    protected void addToGroup(Map<String, List<Object>> groups, String group, String name, Map<String, Object> properties) {
        groups.computeIfAbsent(group, key -> new ArrayList<>())
              .add(createExtendedProperties(name, properties));
    }

    protected static Map<String, Object> createExtendedProperties(String name, Map<String, Object> properties) {
        Map<String, Object> extendedProperties = new TreeMap<>();
        extendedProperties.put(Constants.ATTR_NAME, name);
        extendedProperties.putAll(properties);
        return extendedProperties;
    }

    protected void addDependencies(Map<String, Object> env, Module module) {
        Map<String, List<Object>> groupsMap = new TreeMap<>();
        for (RequiredDependency requiredDependency : module.getRequiredDependencies()) {
            addDependency(requiredDependency, env, groupsMap);
        }
        env.putAll(groupsMap);
    }

    protected void addDependency(RequiredDependency dependency, Map<String, Object> env, Map<String, List<Object>> groups) {
        if (dependency.getList() != null) {
            dependency.setGroup(dependency.getList());
        }
        addToGroupsOrEnvironment(env, groups, ListUtil.asList(dependency.getGroup()), dependency.getName(), dependency.getProperties());
    }

    protected void addToGroupsOrEnvironment(Map<String, Object> env, Map<String, List<Object>> groups, List<String> destinationGroups,
                                            String subgroupName, Map<String, Object> properties) {
        if (!destinationGroups.isEmpty()) {
            addToGroups(groups, destinationGroups, subgroupName, properties);
        } else {
            env.putAll(properties);
        }
    }

    protected void addToGroups(Map<String, List<Object>> groups, List<String> destinationGroups, String subgroupName,
                               Map<String, Object> properties) {
        for (String group : destinationGroups) {
            addToGroup(groups, group, subgroupName, properties);
        }
    }

    protected CloudHandlerFactory createCloudHandlerFactory() {
        return CloudHandlerFactory.forSchemaVersion(MTA_MAJOR_VERSION);
    }

}
