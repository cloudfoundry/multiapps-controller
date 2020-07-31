package org.cloudfoundry.multiapps.controller.core.model;

import org.cloudfoundry.multiapps.common.Nullable;
import org.cloudfoundry.multiapps.controller.core.cf.CloudHandlerFactory;
import org.cloudfoundry.multiapps.controller.core.persistence.service.ConfigurationEntryService;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableMtaDescriptorPropertiesResolverContext.class)
@JsonDeserialize(as = ImmutableMtaDescriptorPropertiesResolverContext.class)
public interface MtaDescriptorPropertiesResolverContext {

    CloudHandlerFactory getHandlerFactory();

    ConfigurationEntryService getConfigurationEntryService();

    CloudTarget getCloudTarget();

    String getCurrentSpaceId();

    ApplicationConfiguration getApplicationConfiguration();

    @Nullable
    String getNamespace();

    boolean applyNamespace();

    boolean shouldReserveTemporaryRoute();
}
