package org.cloudfoundry.multiapps.controller.core.auditlogging;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;

public class ConfigurationEntryServiceAuditLog {

    private static final String PROVIDER_ID_PROPERTY_NAME = "providerId";
    private static final String PROVIDER_NID_PROPERTY_NAME = "providerNid";
    private static final String PROVIDER_VERSION_PROPERTY_NAME = "providerVersion";
    private static final String PROVIDER_NAMESPACE_PROPERTY_NAME = "providerNamespace";
    private static final String PROVIDER_TARGET_PROPERTY_NAME = "providerTarget";
    private static final String PROVIDER_CONTENT_PROPERTY_NAME = "providerContent";
    private static final String PROVIDER_CONTENT_ID_PROPERTY_NAME = "providerContentId";

    private static final String PROVIDER_TARGET_TEMPLATE = "{0}/{1}";

    private final AuditLoggingFacade auditLoggingFacade;

    public ConfigurationEntryServiceAuditLog(AuditLoggingFacade auditLoggingFacade) {
        this.auditLoggingFacade = auditLoggingFacade;
    }

    public void logAddConfigurationEntry(String username, String spaceGuid, ConfigurationEntry entry) {
        String performedAction = MessageFormat.format(Messages.ENTRY_CREATE, spaceGuid);
        auditLoggingFacade.logConfigurationChangeAuditLog(new AuditLogConfiguration(username,
                                                                                    spaceGuid,
                                                                                    performedAction,
                                                                                    Messages.ENTRY_CREATE_AUDIT_LOG_CONFIG,
                                                                                    buildAddConfigEntryParameters(entry)),
                                                          ConfigurationChangeActions.CONFIGURATION_CREATE);
    }

    public void logUpdateConfigurationEntry(String username, String spaceGuid, ConfigurationEntry oldEntry, ConfigurationEntry newEntry) {
        String performedAction = MessageFormat.format(Messages.ENTRY_UPDATE, spaceGuid);

        auditLoggingFacade.logConfigurationChangeAuditLog(new AuditLogConfiguration(username,
                                                                                    spaceGuid,
                                                                                    performedAction,
                                                                                    Messages.ENTRY_UPDATE_AUDIT_LOG_CONFIG),
                                                          ConfigurationChangeActions.CONFIGURATION_UPDATE, oldEntry, newEntry);

    }

    private Map<String, String> buildAddConfigEntryParameters(ConfigurationEntry entry) {
        Map<String, String> identifiers = new HashMap<>();
        String providerTarget = MessageFormat.format(PROVIDER_TARGET_TEMPLATE,
                                                     entry.getTargetSpace()
                                                          .getOrganizationName(),
                                                     entry.getTargetSpace()
                                                          .getSpaceName());

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
