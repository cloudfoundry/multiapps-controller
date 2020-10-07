package org.cloudfoundry.multiapps.controller.process.util;

import java.text.MessageFormat;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.process.Messages;

public class ExceptionMessageTailMapper {

    public enum CloudComponents {
        CLOUD_CONTROLLER("cloud-controller"), DEPLOY_SERVICE("deploy-service"), SERVICE_BROKERS("service-brokers");

        private final String name;

        CloudComponents(String name) {
            this.name = name;
        }
    }

    private static String mapServiceBrokerComponent(Map<String, String> serviceBrokersComponents, String supportChannel, String offering) {
        String component = serviceBrokersComponents.get(offering);
        if (component != null) {
            return MessageFormat.format(Messages.CREATE_SUPPORT_TICKET_TO_SERVICE_BROKER_COMPONENT, supportChannel, component);
        }
        return Messages.CREATE_SUPPORT_TICKET_GENERIC_MESSAGE;
    }

    @SuppressWarnings("unchecked")
    public static String map(ApplicationConfiguration configuration, CloudComponents cloudComponent, String offering) {
        if (!configuration.isInternalEnvironment()) {
            return StringUtils.EMPTY;
        }

        Map<String, Object> cloudComponents = configuration.getCloudComponents();
        String supportChannel = configuration.getInternalSupportChannel();

        switch (cloudComponent) {
            case DEPLOY_SERVICE:
                return MessageFormat.format(Messages.CREATE_SUPPORT_TICKET_TO_DS_COMPONENT, supportChannel,
                                            cloudComponents.get(CloudComponents.DEPLOY_SERVICE.name));
            case CLOUD_CONTROLLER:
                return MessageFormat.format(Messages.CREATE_SUPPORT_TICKET_TO_CC_COMPONENT, supportChannel,
                                            cloudComponents.get(CloudComponents.CLOUD_CONTROLLER.name));
            case SERVICE_BROKERS:
                return mapServiceBrokerComponent((Map<String, String>) cloudComponents.get(CloudComponents.SERVICE_BROKERS.name),
                                                 supportChannel, offering);
            default:
                return StringUtils.EMPTY;
        }
    }

}
