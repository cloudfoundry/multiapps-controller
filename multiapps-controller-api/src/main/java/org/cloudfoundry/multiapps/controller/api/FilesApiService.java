package org.cloudfoundry.multiapps.controller.api;

import java.util.List;

import org.cloudfoundry.multiapps.controller.api.model.AsyncUploadResult;
import org.cloudfoundry.multiapps.controller.api.model.FileMetadata;
import org.cloudfoundry.multiapps.controller.api.model.FileUrl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartHttpServletRequest;

public interface FilesApiService {

    ResponseEntity<List<FileMetadata>> getFiles(String spaceGuid, String namespace);

    ResponseEntity<FileMetadata> uploadFile(MultipartHttpServletRequest request, String spaceGuid, String namespace);

    ResponseEntity<Void> startUploadFromUrl(String spaceGuid, String namespace, FileUrl fileUrl);

    ResponseEntity<AsyncUploadResult> getUploadFromUrlJob(String spaceGuid, String namespace, String jobId);
}
