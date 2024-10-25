package org.cloudfoundry.multiapps.controller.api.v1;

import jakarta.inject.Inject;

import org.cloudfoundry.multiapps.controller.api.InfoApiService;
import org.cloudfoundry.multiapps.controller.api.Constants.Resources;
import org.cloudfoundry.multiapps.controller.api.model.Info;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Api
@RestController
@RequestMapping(Resources.INFO)
public class InfoApi {

    @Inject
    private InfoApiService delegate;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "", notes = "Retrieve information about the Deploy Service application ", response = Info.class, authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = Info.class) })
    public ResponseEntity<Info> getInfo() {
        return delegate.getInfo();
    }

}
