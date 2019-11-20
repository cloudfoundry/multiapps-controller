package com.sap.cloud.lm.sl.cf.web.api;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sap.cloud.lm.sl.cf.web.api.Constants.PathVariables;
import com.sap.cloud.lm.sl.cf.web.api.Constants.Resources;
import com.sap.cloud.lm.sl.cf.web.api.model.FileMetadata;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
    @ApiOperation(value = "", notes = "Retrieves all Multi-Target Application files ", response = FileMetadata.class, responseContainer = "List", authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = FileMetadata.class, responseContainer = "List") })
    public ResponseEntity<List<FileMetadata>> getFiles(@PathVariable(PathVariables.SPACE_GUID) String spaceGuid) {
        return delegate.getFiles(spaceGuid);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = { MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_JSON_UTF8_VALUE })
    @ApiOperation(value = "", notes = "Uploads an Multi Target Application file ", response = FileMetadata.class, authorizations = {
        @Authorization(value = "oauth2", scopes = {

        }) }, tags = {})
    @ApiResponses(value = { @ApiResponse(code = 201, message = "Created", response = FileMetadata.class) })
    public ResponseEntity<FileMetadata> uploadFile(HttpServletRequest request, @PathVariable(PathVariables.SPACE_GUID) String spaceGuid) {
        return delegate.uploadFile(request, spaceGuid);
    }

}
