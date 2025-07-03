package org.cloudfoundry.multiapps.controller.core.auditlogging.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.mta.model.AuditableConfiguration;
import org.cloudfoundry.multiapps.mta.model.ConfigurationIdentifier;

public class AuditLogConfiguration implements AuditableConfiguration {

    private static final String PERFORMED_ACTION_IDENTIFIER_KEY_NAME = "performed_action";
    private static final String TIME_IDENTIFIER_KEY_NAME = "time";
    private static final String SPACE_ID_IDENTIFIER_KEY_NAME = "spaceId";
    private final String userId;
    private final String spaceId;
    private final String performedAction;
    private final String configuration;
    private Map<String, String> parameters;

    public AuditLogConfiguration(String spaceId, String performedAction, String configuration) {
        this.spaceId = spaceId;
        this.userId = null;
        this.performedAction = performedAction;
        this.configuration = configuration;
        this.parameters = Collections.emptyMap();
    }

    public AuditLogConfiguration(String userId, String spaceId, String performedAction, String configuration) {
        this.spaceId = spaceId;
        this.userId = userId;
        this.performedAction = performedAction;
        this.configuration = configuration;
        this.parameters = Collections.emptyMap();
    }

    public AuditLogConfiguration(String userId, String spaceId, String performedAction, String configuration,
                                 Map<String, String> parameters) {
        this(userId, spaceId, performedAction, configuration);
        this.parameters = parameters;
    }

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

    public String getSpaceId() {
        return spaceId;
    }

    public String getUserId() {
        return userId;
    }

    public String getTimeOfPerformedAction() {
        return LocalDateTime.now()
                            .toString();
    }

    @Override
    public List<ConfigurationIdentifier> getConfigurationIdentifiers() {
        List<ConfigurationIdentifier> configurationIdentifiers = new ArrayList<>();
        configurationIdentifiers.add(new ConfigurationIdentifier(PERFORMED_ACTION_IDENTIFIER_KEY_NAME, getPerformedAction()));
        configurationIdentifiers.add(new ConfigurationIdentifier(TIME_IDENTIFIER_KEY_NAME, getTimeOfPerformedAction()));
        configurationIdentifiers.add(new ConfigurationIdentifier(SPACE_ID_IDENTIFIER_KEY_NAME, getSpaceId()));
        for (var parameter : parameters.entrySet()) {
            if (parameter.getValue() != null) {
                configurationIdentifiers.add(new ConfigurationIdentifier(parameter.getKey(), parameter.getValue()));
            }
        }
        return configurationIdentifiers;
    }
}
