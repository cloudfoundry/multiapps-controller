package com.sap.cloud.lm.sl.cf.process.util;

import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.Staging;
import org.hamcrest.Description;
import org.mockito.ArgumentMatcher;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.OngoingOperation;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class ArgumentMatcherProvider {
    private static class StagingMatcher extends ArgumentMatcher<Staging> {

        private final Staging staging;

        public StagingMatcher(Staging staging) {
            this.staging = staging;
        }

        @Override
        public boolean matches(Object argument) {
            return JsonUtil.toJson(staging).equals(JsonUtil.toJson(argument));
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(JsonUtil.toJson(staging, true));
        }

    }

    private static class ServiceMatcher extends ArgumentMatcher<CloudServiceExtended> {

        private final CloudServiceExtended service;

        public ServiceMatcher(CloudServiceExtended service) {
            this.service = service;
        }

        @Override
        public boolean matches(Object argument) {
            return service.getName().equals(((CloudServiceExtended) argument).getName());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(service.getName());
        }

    }

    private static class OngoingOperationMatcher extends ArgumentMatcher<OngoingOperation> {

        private final OngoingOperation ongoingOperation;

        public OngoingOperationMatcher(OngoingOperation oo) {
            this.ongoingOperation = oo;
        }

        @Override
        public boolean matches(Object argument) {
            OngoingOperation operation = (OngoingOperation) argument;
            return ongoingOperation.getProcessId().equals(operation.getProcessId())
                && ongoingOperation.getProcessType() == operation.getProcessType()
                && ongoingOperation.getSpaceId().equals(operation.getSpaceId()) && ongoingOperation.getUser().equals(operation.getUser());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(ongoingOperation.toString());
        }

    }

    private static class ConfigurationEntryMatcher extends ArgumentMatcher<ConfigurationEntry> {
        private String providerId;
        private String version;
        private String spaceId;

        public ConfigurationEntryMatcher(String providerId, String version, String spaceId) {
            this.providerId = providerId;
            this.spaceId = spaceId;
            this.version = version;
        }

        @Override
        public boolean matches(Object argument) {
            ConfigurationEntry otherEntry = (ConfigurationEntry) argument;
            return otherEntry.getProviderId().equals(providerId) && otherEntry.getProviderVersion().toString().equals(version)
                && otherEntry.getTargetSpace().equals(spaceId);
        }

    }

    private static class CloudServiceBrokerMatcher extends ArgumentMatcher<CloudServiceBroker> {

        private String brokerName;
        private String brokerUserName;
        private String brokerPassword;
        private String brokerUrl;

        public CloudServiceBrokerMatcher(String brokerName, String brokerUserName, String brokerPassword, String brokerUrl) {
            this.brokerName = brokerName;
            this.brokerUserName = brokerUserName;
            this.brokerPassword = brokerPassword;
            this.brokerUrl = brokerUrl;
        }

        @Override
        public boolean matches(Object argument) {
            CloudServiceBroker broker = (CloudServiceBroker) argument;
            return broker.getName().equals(brokerName) && broker.getUsername().equals(brokerUserName)
                && broker.getPassword().equals(brokerPassword) && broker.getUrl().equals(brokerUrl);
        }

    }

    public static StagingMatcher getStagingMatcher(Staging staging) {
        return new StagingMatcher(staging);
    }

    public static ServiceMatcher getServiceMatcher(CloudServiceExtended service) {
        return new ServiceMatcher(service);
    }

    public static OngoingOperationMatcher getOngoingOpMatcher(OngoingOperation oo) {
        return new OngoingOperationMatcher(oo);
    }

    public static ConfigurationEntryMatcher getConfigurationEntryMatcher(String providerId, String version, String spaceId) {
        return new ConfigurationEntryMatcher(providerId, version, spaceId);
    }

    public static CloudServiceBrokerMatcher getCloudServiceBrokerMatcher(String brokerName, String brokerUserName, String brokerPassword,
        String brokerUrl) {
        return new CloudServiceBrokerMatcher(brokerName, brokerUserName, brokerPassword, brokerUrl);
    }
}
