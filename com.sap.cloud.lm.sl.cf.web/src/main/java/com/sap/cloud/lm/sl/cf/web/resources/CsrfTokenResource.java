package com.sap.cloud.lm.sl.cf.web.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/csrf-token")
public class CsrfTokenResource {

    @GET
    public Response getCsrfToken() {
        return Response.status(Response.Status.NO_CONTENT).build();
    }

}
