package com.sap.cloud.lm.sl.cf.web.api;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJAXRSCXFCDIServerCodegen", date = "2017-10-23T14:07:53.974+03:00")
public interface FilesApiService {
    public Response getMtaFiles(SecurityContext securityContext, String spaceGuid);

    public Response uploadMtaFile(HttpServletRequest request, SecurityContext securityContext, String spaceGuid);
}
