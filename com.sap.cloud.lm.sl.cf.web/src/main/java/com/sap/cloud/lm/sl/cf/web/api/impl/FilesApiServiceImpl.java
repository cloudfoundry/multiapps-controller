package com.sap.cloud.lm.sl.cf.web.api.impl;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadBase.SizeLimitExceededException;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.persistence.util.Configuration;
import com.sap.cloud.lm.sl.cf.persistence.util.DefaultConfiguration;
import com.sap.cloud.lm.sl.cf.web.api.FilesApiService;
import com.sap.cloud.lm.sl.cf.web.api.model.FileMetadata;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Named
public class FilesApiServiceImpl implements FilesApiService {

    @Inject
    @Named("fileService")
    private FileService fileService;

    @Override
    public ResponseEntity<List<FileMetadata>> getFiles(String spaceGuid) {
        try {
            List<FileEntry> entries = fileService.listFiles(spaceGuid, null);
            List<FileMetadata> files = entries.stream()
                                              .map(this::parseFileEntry)
                                              .collect(Collectors.toList());
            return ResponseEntity.ok()
                                 .body(files);
        } catch (FileStorageException e) {
            throw new SLException(e, Messages.COULD_NOT_GET_FILES_0, e.getMessage());
        }
    }

    @Override
    public ResponseEntity<FileMetadata> uploadFile(HttpServletRequest request, String spaceGuid) {
        try {
            FileEntry fileEntry = uploadFiles(request, spaceGuid).get(0);
            FileMetadata file = parseFileEntry(fileEntry);
            AuditLoggingProvider.getFacade()
                                .logConfigCreate(file);
            return ResponseEntity.status(HttpStatus.CREATED)
                                 .body(file);
        } catch (FileUploadException | IOException | FileStorageException e) {
            throw new SLException(e, Messages.COULD_NOT_UPLOAD_FILE_0, e.getMessage());
        }
    }

    private List<FileEntry> uploadFiles(HttpServletRequest request, String spaceGuid)
        throws FileUploadException, IOException, FileStorageException {
        ServletFileUpload upload = getFileUploadServlet();
        long maxUploadSize = getConfiguration().getMaxUploadSize();
        upload.setSizeMax(maxUploadSize);

        List<FileEntry> uploadedFiles = new ArrayList<>();
        FileItemIterator fileItemIterator = null;
        try {
            fileItemIterator = upload.getItemIterator(request);
        } catch (SizeLimitExceededException ex) {
            throw new SLException(MessageFormat.format(Messages.MAX_UPLOAD_SIZE_EXCEEDED, maxUploadSize));
        }
        while (fileItemIterator.hasNext()) {
            FileItemStream item = fileItemIterator.next();
            if (item.isFormField()) {
                continue; // ignore simple (non-file) form fields
            }

            try (InputStream in = item.openStream()) {
                FileEntry entry = fileService.addFile(spaceGuid, item.getName(), getConfiguration().getFileUploadProcessor(), in);
                uploadedFiles.add(entry);
            }
        }
        return uploadedFiles;
    }

    protected ServletFileUpload getFileUploadServlet() {
        return new ServletFileUpload();
    }

    protected Configuration getConfiguration() {
        return new DefaultConfiguration();
    }

    private FileMetadata parseFileEntry(FileEntry fileEntry) {
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setId(fileEntry.getId());
        fileMetadata.setDigest(fileEntry.getDigest());
        fileMetadata.setDigestAlgorithm(fileEntry.getDigestAlgorithm());
        fileMetadata.setName(fileEntry.getName());
        fileMetadata.setSize(fileEntry.getSize());
        fileMetadata.setSpace(fileEntry.getSpace());
        return fileMetadata;
    }
}
