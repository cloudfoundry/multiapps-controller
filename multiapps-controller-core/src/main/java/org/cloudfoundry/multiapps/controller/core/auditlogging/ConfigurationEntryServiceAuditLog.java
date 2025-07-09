package org.cloudfoundry.multiapps.controller.core.auditlogging;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConfigurationEntryServiceAuditLog {

    private static final String PROVIDER_ID_PROPERTY_NAME = "providerId";
    private static final String PROVIDER_NID_PROPERTY_NAME = "providerNid";
    private static final String PROVIDER_VERSION_PROPERTY_NAME = "providerVersion";
    private static final String PROVIDER_NAMESPACE_PROPERTY_NAME = "providerNamespace";
    private static final String PROVIDER_TARGET_PROPERTY_NAME = "providerTarget";
    private static final String PROVIDER_CONTENT_PROPERTY_NAME = "providerContent";
    private static final String PROVIDER_CONTENT_ID_PROPERTY_NAME = "providerContentId";

    private final AuditLoggingFacade auditLoggingFacade;

    public ConfigurationEntryServiceAuditLog(AuditLoggingFacade auditLoggingFacade) {
        this.auditLoggingFacade = auditLoggingFacade;
    }

    public void logAddConfigurationEntry(String username, String spaceGuid, ConfigurationEntry entry) {
        String performedAction = MessageFormat.format(Messages.ENTRY_CREATE, spaceGuid);
        auditLoggingFacade.logConfigurationChangeAuditLog(new AuditLogConfiguration(username,
                                                                                    spaceGuid,
                                                                                    performedAction,
                                                                                    Messages.ENTRY_CONFIG,
                                                                                    createAddEntryConfigurationIdentifier(entry)),
                                                          ConfigurationChangeActions.CONFIGURATION_CREATE);
    }

    public void logUpdateConfigurationEntry(String username, String spaceGuid, ConfigurationEntry oldEntry, ConfigurationEntry newEntry) {
        String performedAction = MessageFormat.format(Messages.ENTRY_UPDATE, spaceGuid);

        List<String> attributes = List.of(oldEntry.getConfigurationType(), JsonUtil.toJson(oldEntry),
                                          JsonUtil.toJson(newEntry));

        auditLoggingFacade.logConfigurationChangeAuditLog(new AuditLogConfiguration(username,
                                                                                    spaceGuid,
                                                                                    performedAction,
                                                                                    Messages.ENTRY_CONFIG),
                                                          ConfigurationChangeActions.CONFIGURATION_UPDATE, attributes);
    }

    private Map<String, String> createAddEntryConfigurationIdentifier(ConfigurationEntry entry) {
        Map<String, String> identifiers = new HashMap<>();
        String providerTarget = entry.getTargetSpace()
                                     .getOrganizationName()
            + "/" + entry.getTargetSpace()
                         .getSpaceName();

        identifiers.put(PROVIDER_ID_PROPERTY_NAME, entry.getProviderId());
        identifiers.put(PROVIDER_NID_PROPERTY_NAME, entry.getProviderNid());
        identifiers.put(PROVIDER_VERSION_PROPERTY_NAME, Objects.toString(entry.getProviderVersion()));
        identifiers.put(PROVIDER_NAMESPACE_PROPERTY_NAME, entry.getProviderNamespace());
        identifiers.put(PROVIDER_TARGET_PROPERTY_NAME, providerTarget);
        identifiers.put(PROVIDER_CONTENT_PROPERTY_NAME, entry.getContent());
        identifiers.put(PROVIDER_CONTENT_ID_PROPERTY_NAME, entry.getContentId());

        return identifiers;
    }

}
