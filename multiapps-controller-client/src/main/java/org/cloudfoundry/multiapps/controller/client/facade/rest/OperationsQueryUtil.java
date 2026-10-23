package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3QueryParameters;

public class OperationsQueryUtil {

    private OperationsQueryUtil() {
    }

    public static String buildApplicationsQuery(String name, CloudSpace targetSpace) {
        return buildApplicationsQuery(name, targetSpace, null);
    }

    public static String buildApplicationsQuery(String name, CloudSpace targetSpace, String labelSelector) {
        return V3Uri.path(CloudControllerV3Endpoints.APPS)
                    .perPage(CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE)
                    .query(V3QueryParameters.SPACE_GUIDS, spaceGuidOrNull(targetSpace))
                    .queryEncoded(V3QueryParameters.NAMES, name)
                    .query(V3QueryParameters.LABEL_SELECTOR, labelSelector)
                    .build();
    }

    public static String buildRoutesQuery(UUID domainGuid, String host, String path, UUID targetSpaceGuid) {
        return V3Uri.path(CloudControllerV3Endpoints.ROUTES)
                    .perPage(CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE)
                    .query(V3QueryParameters.DOMAIN_GUIDS, domainGuid)
                    .query(V3QueryParameters.SPACE_GUIDS, targetSpaceGuid)
                    .query(V3QueryParameters.HOSTS, host)
                    .query(V3QueryParameters.PATHS, path)
                    .build();
    }

    public static String buildServiceInstancesQuery(String name, CloudSpace targetSpace) {
        return V3Uri.path(CloudControllerV3Endpoints.SERVICE_INSTANCES)
                    .perPage(CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE)
                    .query(V3QueryParameters.SPACE_GUIDS, spaceGuidOrNull(targetSpace))
                    .queryEncoded(V3QueryParameters.NAMES, name)
                    .build();
    }

    private static UUID spaceGuidOrNull(CloudSpace targetSpace) {
        return targetSpace == null ? null : targetSpace.getGuid();
    }

}
