package org.cloudfoundry.multiapps.controller.process.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.SetUtils;
import org.cloudfoundry.multiapps.controller.core.util.UriUtil;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ElementUpdater.UpdateStrategy;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudRouteSummary;

public class UrisApplicationAttributeUpdater extends ApplicationAttributeUpdater {

    public UrisApplicationAttributeUpdater(Context context, UpdateStrategy updateStrategy) {
        super(context, updateStrategy);
    }

    @Override
    protected boolean shouldUpdateAttribute(CloudApplication existingApplication, CloudApplication application) {
        return !SetUtils.isEqualSet(application.getRoutes(), existingApplication.getRoutes());
    }

    @Override
    protected void updateAttribute(CloudApplication existingApplication, CloudApplication application) {
        Set<CloudRouteSummary> routes = applyUpdateStrategy(existingApplication.getRoutes(), application.getRoutes());
        getControllerClient().updateApplicationRoutes(application.getName(), routes);
    }

    private Set<CloudRouteSummary> applyUpdateStrategy(Set<CloudRouteSummary> existingRoutes, Set<CloudRouteSummary> routes) {
        getLogger().debug(Messages.EXISTING_URIS_0, UriUtil.prettyPrintRoutes(existingRoutes));
        getLogger().debug(Messages.APPLYING_UPDATE_STRATEGY_0_WITH_URIS_1, updateStrategy, UriUtil.prettyPrintRoutes(routes));
        Set<CloudRouteSummary> result = getElementUpdater().updateSet(existingRoutes, routes);
        getLogger().debug(Messages.RESULT_0, UriUtil.prettyPrintRoutes(result));
        return result;
    }

}
