package com.sap.cloud.lm.sl.cf.web.resources;

import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.ORG;
import static com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters.SPACE;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
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
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.clients.SpaceGetter;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationSubscriptionDao;
import com.sap.cloud.lm.sl.cf.core.helpers.ClientHelper;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscriptions;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;

@Component
@Produces(MediaType.APPLICATION_XML)
@Path("/configuration-subscriptions")
public class ConfigurationSubscriptionsResource {

    @Inject
    private ConfigurationSubscriptionDao configurationSubscriptionsDao;

    @Inject
    private CloudControllerClientProvider clientProvider;

    @Inject
    private SpaceGetter spaceGetter;

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
            return spaceGetter.findSpaces(client, org);
        }
        return Arrays.asList(spaceGetter.findSpace(client, org, space));
    }

    private List<ConfigurationSubscription> getConfigurationEntries(List<CloudSpace> clientSpaces, CloudControllerClient client) {
        return clientSpaces.stream()
                           .map(clientSpace -> configurationSubscriptionsDao.findAll(null, null,
                                                                                     computeSpaceId(client, clientSpace.getOrganization()
                                                                                                                       .getName(),
                                                                                                    clientSpace.getName()),
                                                                                     null))
                           .flatMap(List::stream)
                           .collect(Collectors.toList());
    }

    private ConfigurationSubscriptions wrap(List<ConfigurationSubscription> configurationSubscriptions) {
        return new ConfigurationSubscriptions(configurationSubscriptions);
    }

    private CloudControllerClient getCloudFoundryClient() {
        UserInfo userInfo = SecurityContextUtil.getUserInfo();
        return clientProvider.getControllerClient(userInfo.getName());
    }

    private String computeSpaceId(CloudControllerClient client, String orgName, String spaceName) {
        return new ClientHelper(client, spaceGetter).computeSpaceId(orgName, spaceName);
    }

}
