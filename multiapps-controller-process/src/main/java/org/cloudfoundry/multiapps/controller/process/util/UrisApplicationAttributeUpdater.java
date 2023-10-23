package org.cloudfoundry.multiapps.controller.process.util;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.SetUtils;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.util.UriUtil;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ElementUpdater.UpdateStrategy;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;

public class UrisApplicationAttributeUpdater extends ApplicationAttributeUpdater {

    private final List<CloudRoute> existingRoutes;

    public UrisApplicationAttributeUpdater(Context context, UpdateStrategy updateStrategy, List<CloudRoute> existingRoutes) {
        super(context, updateStrategy);
        this.existingRoutes = existingRoutes;
    }

    @Override
    protected boolean shouldUpdateAttribute(CloudApplication existingApplication, CloudApplicationExtended application) {
        if (!SetUtils.isEqualSet(application.getRoutes(), existingRoutes)) {
            return true;
        }
        return application.getRoutes()
                          .stream()
                          .filter(updatedRoute -> updatedRoute.getRequestedProtocol() != null)
                          .anyMatch(updatedRoute -> doesProtocolOfTheExistingRouteDiffer(updatedRoute, existingApplication.getGuid()));
    }

    private boolean doesProtocolOfTheExistingRouteDiffer(CloudRoute updatedRoute, UUID applicationGuid) {
        Optional<CloudRoute> existingRoute = findCloudRoute(updatedRoute.getUrl(), existingRoutes);
        if (existingRoute.isEmpty()) {
            return true;
        }
        return existingRoute.get()
                            .getDestinations()
                            .stream()
                            .noneMatch(destination -> Objects.equals(destination.getApplicationGuid(), applicationGuid)
                                && Objects.equals(destination.getProtocol(), updatedRoute.getRequestedProtocol()));
    }

    private Optional<CloudRoute> findCloudRoute(String url, List<CloudRoute> cloudRoutes) {
        return cloudRoutes.stream()
                          .filter(cloudRoute -> url.equals(cloudRoute.getUrl()))
                          .findFirst();
    }

    @Override
    protected void updateAttribute(CloudApplication existingApplication, CloudApplicationExtended application) {
        Set<CloudRoute> routes = applyUpdateStrategy(existingRoutes, application.getRoutes());
        getControllerClient().updateApplicationRoutes(application.getName(), routes);
    }

    private Set<CloudRoute> applyUpdateStrategy(List<CloudRoute> existingRoutes, Set<CloudRoute> routes) {
        getLogger().debug(Messages.EXISTING_URIS_0, UriUtil.prettyPrintRoutes(existingRoutes));
        getLogger().debug(Messages.APPLYING_UPDATE_STRATEGY_0_WITH_URIS_1, updateStrategy, UriUtil.prettyPrintRoutes(routes));
        Set<CloudRoute> result = getElementUpdater().updateSet(new HashSet<>(existingRoutes), routes);
        getLogger().debug(Messages.RESULT_0, UriUtil.prettyPrintRoutes(result));
        return result;
    }

}
