package org.cloudfoundry.multiapps.controller.core.auditlogging.model;

import org.cloudfoundry.multiapps.mta.model.ConfigurationIdentifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtentensionAuditLog extends CustomAuditLog {

    private String performedAction;
    private String configuration;
    private Map<String, String> parameters;
    public ExtentensionAuditLog(String userId, String spaceId, String performedAction, String configuration) {
        super(userId, spaceId);
        this.performedAction = performedAction;
        this.configuration = configuration;
        this.parameters = new HashMap<>();
    }

    public ExtentensionAuditLog(String userId, String spaceId, String performedAction, String configuration, Map<String, String> parameters) {
        this(userId, spaceId, performedAction, configuration);
        this.parameters = parameters;
    }

    @Override
    public String getPerformedAction() {
        return performedAction;
    }

    @Override
    public String getConfigurationType() {
        return configuration;
    }

    @Override
    public String getConfigurationName() {
        return configuration;
    }

    @Override
    public List<ConfigurationIdentifier> getConfigurationIdentifiers() {
        List<ConfigurationIdentifier> configurationIdentifier = super.getConfigurationIdentifiers();
        for (var parameter : parameters.entrySet()) {
            if (parameter.getValue() != null) {
                configurationIdentifier.add(new ConfigurationIdentifier(parameter.getKey(), parameter.getValue()));
            }
        }
        return configurationIdentifier;
    }
}
