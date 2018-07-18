package com.sap.cloud.lm.sl.cf.web.resources;

import javax.inject.Inject;
import javax.ws.rs.Path;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.web.api.FilesApi;
import com.sap.cloud.lm.sl.cf.web.api.MtasApi;
import com.sap.cloud.lm.sl.cf.web.api.OperationsApi;

@Path("/spaces/{space_guid}")
@Component
@Scope(value = "request")
public class BaseResource {

    @Inject
    private FilesApi filesApi;

    @Inject
    private MtasApi mtasApi;

    @Inject
    private OperationsApi operationsApi;

    @Path("/files")
    public FilesApi getMtaFiles() {
        return filesApi;
    }

    @Path("/mtas")
    public MtasApi getMtasApi() {
        return mtasApi;
    }

    @Path("/operations")
    public OperationsApi getOperationsApi() {
        return operationsApi;
    }
}
