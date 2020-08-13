package org.cloudfoundry.multiapps.controller.api.v1;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.cloudfoundry.multiapps.controller.api.FilesApiService;
import org.cloudfoundry.multiapps.controller.api.Constants.PathVariables;
import org.cloudfoundry.multiapps.controller.api.Constants.RequestVariables;
import org.cloudfoundry.multiapps.controller.api.Constants.Resources;
import org.cloudfoundry.multiapps.controller.api.model.FileMetadata;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Api(description = "the files API")
@RestController
@RequestMapping(Resources.FILES)
public class FilesApi {

    @Inject
    private FilesApiService delegate;

    @GetMapping(produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
    @ApiOperation(value = "", nickname = "getMtaFiles", notes = "Retrieves all Multi-Target Application files ", response = FileMetadata.class, responseContainer = "List", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = FileMetadata.class, responseContainer = "List") })
    public ResponseEntity<List<FileMetadata>>
           getFiles(@ApiParam(value = "GUID of space with mtas") @PathVariable(PathVariables.SPACE_GUID) String spaceGuid,
                    @ApiParam(value = "Filter mtas by namespace") @RequestParam(name = RequestVariables.NAMESPACE, required = false) String namespace) {
        return delegate.getFiles(spaceGuid, namespace);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = { MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_JSON_UTF8_VALUE })
    @ApiOperation(value = "", nickname = "uploadMtaFile", notes = "Uploads an Multi Target Application archive or an Extension Descriptor ", response = FileMetadata.class, authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 201, message = "Created", response = FileMetadata.class) })
    public ResponseEntity<FileMetadata>
           uploadFile(HttpServletRequest request,
                      @ApiParam(value = "GUID of space you wish to deploy in") @PathVariable(PathVariables.SPACE_GUID) String spaceGuid,
                      @ApiParam(value = "file namespace") @RequestParam(name = RequestVariables.NAMESPACE, required = false) String namespace) {
        return delegate.uploadFile(request, spaceGuid, namespace);
    }

}
