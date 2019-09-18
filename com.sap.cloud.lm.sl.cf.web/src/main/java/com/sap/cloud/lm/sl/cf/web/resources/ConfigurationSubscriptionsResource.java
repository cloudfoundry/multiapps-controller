package com.sap.cloud.lm.sl.cf.web.resources;

import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.ORG;
import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.SPACE;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudSpace;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.helpers.ClientHelper;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscriptions;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationSubscriptionService;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;

@Named
@Produces(MediaType.APPLICATION_XML)
@Path("/configuration-subscriptions")
public class ConfigurationSubscriptionsResource {

    @Inject
    private ConfigurationSubscriptionService configurationSubscriptionsService;

    @Inject
    private CloudControllerClientProvider clientProvider;

    @Context
    private HttpServletRequest request;

    @GET
    public Response getConfigurationSubscriptions(@QueryParam(ORG) String org, @QueryParam(SPACE) String space) {
        CloudControllerClient client = getCloudFoundryClient();
        List<CloudSpace> clientSpaces = getClientSpaces(org, space, client);

        List<ConfigurationSubscription> configurationSubscriptions = getConfigurationEntries(clientSpaces, client);

        return Response.ok()
                       .entity(wrap(configurationSubscriptions))
                       .build();
    }

    private List<CloudSpace> getClientSpaces(String org, String space, CloudControllerClient client) {
        if (space == null) {
            return client.getSpaces(org);
        }
        return Arrays.asList(client.getSpace(org, space));
    }

    private List<ConfigurationSubscription> getConfigurationEntries(List<CloudSpace> clientSpaces, CloudControllerClient client) {
        return clientSpaces.stream()
                           .map(clientSpace -> getConfigurationEntries(client, clientSpace))
                           .flatMap(List::stream)
                           .collect(Collectors.toList());
    }

    private List<ConfigurationSubscription> getConfigurationEntries(CloudControllerClient client, CloudSpace clientSpace) {
        return configurationSubscriptionsService.createQuery()
                                                .spaceId(computeSpaceId(client, clientSpace.getOrganization()
                                                                                           .getName(),
                                                                        clientSpace.getName()))
                                                .list();
    }

    private ConfigurationSubscriptions wrap(List<ConfigurationSubscription> configurationSubscriptions) {
        return new ConfigurationSubscriptions(configurationSubscriptions);
    }

    private CloudControllerClient getCloudFoundryClient() {
        UserInfo userInfo = SecurityContextUtil.getUserInfo();
        return clientProvider.getControllerClient(userInfo.getName());
    }

    private String computeSpaceId(CloudControllerClient client, String orgName, String spaceName) {
        return new ClientHelper(client).computeSpaceId(orgName, spaceName);
    }

}
