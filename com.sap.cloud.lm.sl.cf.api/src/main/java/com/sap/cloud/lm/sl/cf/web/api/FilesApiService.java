package com.sap.cloud.lm.sl.cf.web.api;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;

import com.sap.cloud.lm.sl.cf.web.api.model.FileMetadata;

public interface FilesApiService {

    ResponseEntity<List<FileMetadata>> getFiles(String spaceGuid, String namespace);

    ResponseEntity<FileMetadata> uploadFile(HttpServletRequest request, String spaceGuid, String namespace);

}
