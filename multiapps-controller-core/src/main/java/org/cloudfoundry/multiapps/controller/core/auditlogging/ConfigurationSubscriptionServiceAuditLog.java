package org.cloudfoundry.multiapps.controller.core.auditlogging;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.AuditLogConfiguration;
import org.cloudfoundry.multiapps.controller.core.auditlogging.model.ConfigurationChangeActions;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;

public class ConfigurationSubscriptionServiceAuditLog {

    private static final String SUBSCRIPTION_ID_PROPERTY_NAME = "subscriptionId";
    private static final String APPLICATION_ID_PROPERTY_NAME = "applicationId";
    private static final String MTA_ID_PROPERTY_NAME = "mtaId";

    private final AuditLoggingFacade auditLoggingFacade;

    public ConfigurationSubscriptionServiceAuditLog(AuditLoggingFacade auditLoggingFacade) {
        this.auditLoggingFacade = auditLoggingFacade;
    }

    public void logAddConfigurationSubscription(String username, String spaceGuid, ConfigurationSubscription subscription) {
        String performedAction = MessageFormat.format(Messages.SUBSCRIPTION_CREATE, spaceGuid);
        auditLoggingFacade.logConfigurationChangeAuditLog(new AuditLogConfiguration(username,
                                                                                    spaceGuid,
                                                                                    performedAction,
                                                                                    Messages.SUBSCRIPTION_CREATE_AUDIT_LOG_CONFIG,
                                                                                    buildAddConfigSubscriptionParameters(
                                                                                        subscription)),
                                                          ConfigurationChangeActions.CONFIGURATION_CREATE);
    }

    public void logUpdateConfigurationSubscription(String username, String spaceGuid, ConfigurationSubscription oldSubscription,
                                                   ConfigurationSubscription updatedSubscription) {

        String performedAction = MessageFormat.format(Messages.SUBSCRIPTION_UPDATE, spaceGuid);

        auditLoggingFacade.logConfigurationChangeAuditLog(new AuditLogConfiguration(username,
                                                                                    spaceGuid,
                                                                                    performedAction,
                                                                                    Messages.SUBSCRIPTION_UPDATE_AUDIT_LOG_CONFIG),
                                                          ConfigurationChangeActions.CONFIGURATION_UPDATE, oldSubscription,
                                                          updatedSubscription);
    }

    private Map<String, String> buildAddConfigSubscriptionParameters(ConfigurationSubscription subscription) {
        Map<String, String> identifiers = new HashMap<>();

        identifiers.put(APPLICATION_ID_PROPERTY_NAME, subscription.getAppName());
        identifiers.put(MTA_ID_PROPERTY_NAME, subscription.getMtaId());
        identifiers.put(SUBSCRIPTION_ID_PROPERTY_NAME, String.valueOf(subscription.getId()));

        return identifiers;
    }

}
