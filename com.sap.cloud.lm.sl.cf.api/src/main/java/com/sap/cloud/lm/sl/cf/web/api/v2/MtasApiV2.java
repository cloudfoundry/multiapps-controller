package com.sap.cloud.lm.sl.cf.web.api.v2;

import java.util.List;

import javax.inject.Inject;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sap.cloud.lm.sl.cf.web.api.Constants.Resources;
import com.sap.cloud.lm.sl.cf.web.api.Constants.PathVariables;
import com.sap.cloud.lm.sl.cf.web.api.Constants.RequestVariables;
import com.sap.cloud.lm.sl.cf.web.api.MtasApiService;
import com.sap.cloud.lm.sl.cf.web.api.model.Mta;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Api(description = "mtas API V2")
@RestController
@RequestMapping(Resources.MTAS_V2)
public class MtasApiV2 {

    @Inject
    private MtasApiService delegate;

    @GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
    @ApiOperation(value = "", notes = "Retrieves all Multi-Target Applications in a space ", response = Mta.class, responseContainer = "List", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Mta.class, responseContainer = "List") })
    public ResponseEntity<List<Mta>>
           getMtas(@ApiParam(value = "GUID of space with mtas") @PathVariable(PathVariables.SPACE_GUID) String spaceGuid,
                   @ApiParam(value = "Filter mtas by namespace") @RequestParam(name = RequestVariables.NAMESPACE, required = false) String namespace,
                   @ApiParam(value = "Filter mtas by name") @RequestParam(name = RequestVariables.MTA_NAME, required = false) String name) {
        return delegate.getMtas(spaceGuid, namespace, name);
    }

}
