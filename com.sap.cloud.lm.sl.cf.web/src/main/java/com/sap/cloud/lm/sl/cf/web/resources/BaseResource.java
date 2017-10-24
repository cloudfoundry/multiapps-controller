package com.sap.cloud.lm.sl.cf.web.resources;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.web.api.FilesApi;
import com.sap.cloud.lm.sl.cf.web.api.MtasApi;
import com.sap.cloud.lm.sl.cf.web.api.OperationsApi;

@Path("/spaces/{space_guid}")
@Component
public class BaseResource {

    @PathParam("space_guid")
    protected String spaceGuid;

    @Inject
    FilesApi filesApi;

    @Inject
    MtasApi mtasApi;

    @Inject
    OperationsApi operationsApi;

    @Path("/files")
    public FilesApi getMtaFiles() {
        filesApi.setSpaceGuid(spaceGuid);
        return filesApi;
    }

    @Path("/mtas")
    public MtasApi getMtasApi() {
        mtasApi.setSpaceGuid(spaceGuid);
        return mtasApi;
    }

    @Path("/operations")
    public OperationsApi getOperationsApi() {
        operationsApi.setSpaceGuid(spaceGuid);
        return operationsApi;
    }
}
