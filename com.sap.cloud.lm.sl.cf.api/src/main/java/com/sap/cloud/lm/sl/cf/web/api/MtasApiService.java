package com.sap.cloud.lm.sl.cf.web.api;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJAXRSCXFCDIServerCodegen", date = "2017-10-23T14:07:53.974+03:00")
public interface MtasApiService {
    public Response getMta(String mtaId, SecurityContext securityContext, String spaceGuid, HttpServletRequest request);

    public Response getMtas(SecurityContext securityContext, String spaceGuid, HttpServletRequest request);
}
