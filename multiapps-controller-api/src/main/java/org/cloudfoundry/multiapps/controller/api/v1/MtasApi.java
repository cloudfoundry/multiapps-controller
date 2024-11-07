package org.cloudfoundry.multiapps.controller.api.v1;

import java.util.List;

import jakarta.inject.Inject;

import org.cloudfoundry.multiapps.controller.api.MtasApiService;
import org.cloudfoundry.multiapps.controller.api.Constants.Endpoints;
import org.cloudfoundry.multiapps.controller.api.Constants.PathVariables;
import org.cloudfoundry.multiapps.controller.api.Constants.RequestVariables;
import org.cloudfoundry.multiapps.controller.api.Constants.Resources;
import org.cloudfoundry.multiapps.controller.api.model.Mta;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Api
@RestController
@RequestMapping(Resources.MTAS)
public class MtasApi {

    @Inject
    private MtasApiService delegate;

    @GetMapping(path = Endpoints.MTA, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "", notes = "Retrieves Multi-Target Application in a space ", response = Mta.class, authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Mta.class) })
    public ResponseEntity<Mta> getMta(@ApiParam(value = "GUID of space with mtas") @PathVariable(PathVariables.SPACE_GUID) String spaceGuid,
                                      @ApiParam(value = "mtaID of requested mta") @PathVariable(RequestVariables.MTA_ID) String mtaId) {
        return delegate.getMta(spaceGuid, mtaId);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "", notes = "Retrieves all Multi-Target Applications in a space ", response = Mta.class, responseContainer = "List", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Mta.class, responseContainer = "List") })
    public ResponseEntity<List<Mta>>
           getMtas(@ApiParam(value = "GUID of space with mtas") @PathVariable(PathVariables.SPACE_GUID) String spaceGuid) {
        return delegate.getMtas(spaceGuid);
    }

}
