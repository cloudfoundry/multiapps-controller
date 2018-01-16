package com.sap.cloud.lm.sl.cf.web.resources;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationSubscriptionDao;
import com.sap.cloud.lm.sl.cf.core.helpers.ClientHelper;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscriptions;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;

@Component
@Produces(MediaType.APPLICATION_XML)
@Path("/configuration-subscriptions")
public class ConfigurationSubscriptionsResource {

    @Inject
    private ConfigurationSubscriptionDao configurationSubscriptionsDao;

    @Inject
    private CloudFoundryClientProvider clientProvider;

    @Context
    private HttpServletRequest request;

    @GET
    public Response getConfigurationSubscriptions(@QueryParam("org") String org, @QueryParam("space") String space) {
        CloudFoundryOperations client = getCloudFoundryClient(org, space);

        List<ConfigurationSubscription> configurationSubscriptions = configurationSubscriptionsDao.findAll(null, null,
            computeSpaceId(client, org, space), null);

        return Response.ok().entity(wrap(configurationSubscriptions)).build();
    }

    private ConfigurationSubscriptions wrap(List<ConfigurationSubscription> configurationSubscriptions) {
        return new ConfigurationSubscriptions(configurationSubscriptions);
    }

    private CloudFoundryOperations getCloudFoundryClient(String org, String space) {
        UserInfo userInfo = SecurityContextUtil.getUserInfo();
        AuthorizationChecker.ensureUserIsAuthorized(request, clientProvider, userInfo, org, space,
            MessageFormat.format(Messages.RETRIEVE_CONFIGURATION_SUBSCRIPTIONS_IN_ORG_AND_SPACE, org, space));
        return clientProvider.getCloudFoundryClient(userInfo.getName(), org, space, null);
    }

    private String computeSpaceId(CloudFoundryOperations client, String orgName, String spaceName) {
        return new ClientHelper(client).computeSpaceId(orgName, spaceName);
    }
}
