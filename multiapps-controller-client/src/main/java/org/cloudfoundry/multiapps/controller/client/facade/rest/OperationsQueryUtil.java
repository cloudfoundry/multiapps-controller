package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;

public class OperationsQueryUtil {

    private OperationsQueryUtil() {
    }

    public static String buildApplicationsQuery(String name, CloudSpace targetSpace) {
        StringBuilder query = new StringBuilder(CloudControllerV3Endpoints.APPS + CloudControllerV3Endpoints.QUERY_PER_PAGE
                                                    + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE);

        if (targetSpace != null && targetSpace.getGuid() != null) {
            query.append(CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS)
                 .append(targetSpace.getGuid());
        }

        if (name != null) {
            query.append(CloudControllerV3Endpoints.AMPERSAND_NAMES)
                 .append(name);
        }

        return query.toString();
    }

    public static String buildRoutesQuery(UUID domainGuid, String host, String path, UUID targetSpaceGuid) {
        StringBuilder query = new StringBuilder(
            CloudControllerV3Endpoints.ROUTES + CloudControllerV3Endpoints.QUERY_PER_PAGE + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE);

        query.append(CloudControllerV3Endpoints.AMPERSAND_DOMAIN_GUIDS)
             .append(domainGuid);

        query.append(CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS)
             .append(targetSpaceGuid);

        if (host != null) {
            query.append(CloudControllerV3Endpoints.AMPERSAND_HOSTS)
                 .append(host);
        }

        if (path != null) {
            query.append(CloudControllerV3Endpoints.AMPERSAND_PATHS)
                 .append(path);
        }

        return query.toString();
    }

    public static String buildServiceInstancesQuery(String name, CloudSpace targetSpace) {
        StringBuilder query = new StringBuilder(CloudControllerV3Endpoints.SERVICE_INSTANCES + CloudControllerV3Endpoints.QUERY_PER_PAGE
                                                    + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE);

        if (targetSpace != null && targetSpace.getGuid() != null) {
            query.append(CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS)
                 .append(targetSpace.getGuid());
        }

        if (name != null) {
            query.append(CloudControllerV3Endpoints.AMPERSAND_NAMES)
                 .append(name);
        }

        return query.toString();
    }

}
