package org.cloudfoundry.multiapps.controller.api.v1;

import jakarta.inject.Inject;

import org.cloudfoundry.multiapps.controller.api.CsrfTokenApiService;
import org.cloudfoundry.multiapps.controller.api.Constants.Resources;
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
@RequestMapping(Resources.CSRF)
public class CsrfTokenApi {

    @Inject
    private CsrfTokenApiService delegate;

    @GetMapping
    @ApiOperation(value = "", notes = "Retrieves a csrf-token header ", authorizations = { @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 204, message = "No Content") })
    public ResponseEntity<Void> getCsrfToken() {
        return delegate.getCsrfToken();
    }

}
