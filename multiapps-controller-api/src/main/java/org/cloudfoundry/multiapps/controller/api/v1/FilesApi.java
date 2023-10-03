package org.cloudfoundry.multiapps.controller.api.v1;

import java.util.List;

import javax.inject.Inject;

import org.cloudfoundry.multiapps.controller.api.FilesApiService;
import org.cloudfoundry.multiapps.controller.api.Constants.Endpoints;
import org.cloudfoundry.multiapps.controller.api.Constants.PathVariables;
import org.cloudfoundry.multiapps.controller.api.Constants.RequestVariables;
import org.cloudfoundry.multiapps.controller.api.Constants.Resources;
import org.cloudfoundry.multiapps.controller.api.model.FileMetadata;
import org.cloudfoundry.multiapps.controller.api.model.FileUrl;
import org.cloudfoundry.multiapps.controller.api.model.AsyncUploadResult;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;

@Api
@RestController
@RequestMapping(Resources.FILES)
public class FilesApi {

    @Inject
    private FilesApiService delegate;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "", nickname = "getMtaFiles", notes = "Retrieves all Multi-Target Application files ", response = FileMetadata.class, responseContainer = "List", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = FileMetadata.class, responseContainer = "List") })
    public ResponseEntity<List<FileMetadata>>
           getFiles(@ApiParam(value = "GUID of space with mtas") @PathVariable(PathVariables.SPACE_GUID) String spaceGuid,
                    @ApiParam(value = "Filter mtas by namespace") @RequestParam(name = RequestVariables.NAMESPACE, required = false) String namespace) {
        return delegate.getFiles(spaceGuid, namespace);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "", nickname = "uploadMtaFile", notes = "Uploads a Multi Target Application archive or an Extension Descriptor ", response = FileMetadata.class, authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 201, message = "Created", response = FileMetadata.class) })
    public ResponseEntity<FileMetadata>
           uploadFile(MultipartHttpServletRequest request,
                      @ApiParam(value = "GUID of space you wish to deploy in") @PathVariable(PathVariables.SPACE_GUID) String spaceGuid,
                      @ApiParam(value = "file namespace") @RequestParam(name = RequestVariables.NAMESPACE, required = false) String namespace) {
        return delegate.uploadFile(request, spaceGuid, namespace);
    }

    @PostMapping(path = Endpoints.ASYNC_UPLOAD, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "", nickname = "startUploadFromUrl", notes = "Uploads a Multi Target Application archive or an Extension Descriptor from a remote endpoint",  authorizations = {
            @Authorization(value = "oauth2", scopes = {

            }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 202, message = "Accepted") })
    public ResponseEntity<Void>
           startUploadFromUrl(@ApiParam(value = "GUID of space you wish to deploy in") @PathVariable(PathVariables.SPACE_GUID) String spaceGuid,
                              @ApiParam(value = "file namespace") @RequestParam(name = RequestVariables.NAMESPACE, required = false) String namespace,
                              @ApiParam(value = "URL reference to a remote file") @RequestBody FileUrl fileUrl) {
        return delegate.startUploadFromUrl(spaceGuid, namespace, fileUrl);
    }

    @GetMapping(path = Endpoints.ASYNC_UPLOAD_JOB, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "", nickname = "getAsyncUploadJob", notes = "Gets the status of an async upload job", response = AsyncUploadResult.class, authorizations = {
            @Authorization(value = "oauth2", scopes = {

            }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"), @ApiResponse(code = 201, message = "Created", response = AsyncUploadResult.class) })
    public ResponseEntity<AsyncUploadResult> getUploadFromUrlJob(@ApiParam(value = "GUID of space you wish to deploy in") @PathVariable(PathVariables.SPACE_GUID) String spaceGuid,
                                                                 @ApiParam(value = "file namespace") @RequestParam(name = RequestVariables.NAMESPACE, required = false) String namespace,
                                                                 @ApiParam(value = "ID of the upload job") @PathVariable(PathVariables.JOB_ID) String jobId) {
        return delegate.getUploadFromUrlJob(spaceGuid, namespace, jobId);
    }

}
