package com.sap.cloud.lm.sl.cf.web.resources;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.CloudFoundryOperationsExtended;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.web.util.ClientUtil;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;
import com.sap.cloud.lm.sl.common.SLException;

@Component
@Path("/{org}/{space}/roles")
@Produces(MediaType.TEXT_PLAIN)
public class RolesResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(RolesResource.class);

    private static final String SPACE_MANAGER = "SpaceManager";
    private static final String SPACE_DEVELOPER = "SpaceDeveloper";
    private static final String SPACE_AUDITOR = "SpaceAuditor";

    @PathParam("space")
    private String space;

    @Inject
    private CloudFoundryClientProvider clientProvider;

    @PathParam("org")
    private String org;

    @GET
    public Response getRoles() throws SLException {
        try {
            return Response.ok().entity(getRoles(ClientUtil.getCloudFoundryClient(clientProvider, org, space))).build();
        } catch (IllegalArgumentException e) {
            /**
             * Thrown if the user has no roles in the specified organization and space. In that case the request should not fail - it should
             * instead return an empty roles list.
             */
            return Response.ok().entity("").build();
        }
    }

    private String getRoles(CloudFoundryOperations client) throws SLException {
        String userId = SecurityContextUtil.getUserInfo().getId();
        LOGGER.debug(MessageFormat.format("User guid: {0}", userId));
        CloudFoundryOperationsExtended clientx = (CloudFoundryOperationsExtended) client;
        List<String> roles = new ArrayList<>();
        if (clientx.getSpaceAuditors2(org, space).contains(userId)) {
            roles.add(SPACE_AUDITOR);
        }
        if (clientx.getSpaceDevelopers2(org, space).contains(userId)) {
            roles.add(SPACE_DEVELOPER);
        }
        if (clientx.getSpaceManagers2(org, space).contains(userId)) {
            roles.add(SPACE_MANAGER);
        }
        return String.join(",", roles);
    }

}
