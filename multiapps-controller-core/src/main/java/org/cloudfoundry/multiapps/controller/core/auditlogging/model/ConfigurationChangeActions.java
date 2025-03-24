package org.cloudfoundry.multiapps.controller.core.auditlogging.model;

public enum ConfigurationChangeActions {

    CONFIGURATION_CREATE("configuration-create"), CONFIGURATION_UPDATE("configuration-update"), CONFIGURATION_DELETE(
        "configuration-delete");

    private final String configurationAction;

    ConfigurationChangeActions(String configurationAction) {
        this.configurationAction = configurationAction;
    }

    public String getConfigurationChangeAction() {
        return this.configurationAction;
    }
}