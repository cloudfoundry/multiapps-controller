package org.cloudfoundry.multiapps.controller.api;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.cloudfoundry.multiapps.controller.api.model.FileMetadata;
import org.springframework.http.ResponseEntity;

public interface FilesApiService {

    ResponseEntity<List<FileMetadata>> getFiles(String spaceGuid, String namespace);

    ResponseEntity<FileMetadata> uploadFile(HttpServletRequest request, String spaceGuid, String namespace);

}
