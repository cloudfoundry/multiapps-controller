package com.sap.cloud.lm.sl.cf.process.util;

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
}
