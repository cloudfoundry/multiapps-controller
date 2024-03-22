package org.cloudfoundry.multiapps.controller.core.auditlogging.model;

import org.cloudfoundry.multiapps.mta.model.AuditableConfiguration;
import org.cloudfoundry.multiapps.mta.model.ConfigurationIdentifier;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public abstract class CustomAuditLog implements AuditableConfiguration {

    private String userId;
    private String spaceId;

    public CustomAuditLog(String userId, String spaceId) {
        this.spaceId = spaceId;
        this.userId = userId;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public String getUserId() {
        return userId;
    }

    public String getTimeOfPerformedAction() {
        return new Date().toString();
    }

    public abstract String getPerformedAction();

    @Override
    public List<ConfigurationIdentifier> getConfigurationIdentifiers() {
        List<ConfigurationIdentifier> configurationIdentifiers = new ArrayList<>();
        configurationIdentifiers.add(new ConfigurationIdentifier("performed_action", getPerformedAction()));
        configurationIdentifiers.add(new ConfigurationIdentifier("time", getTimeOfPerformedAction()));
        configurationIdentifiers.add(new ConfigurationIdentifier("spaceId", getSpaceId()));
        return configurationIdentifiers;
    }
}
