package org.cloudfoundry.multiapps.controller.web.api.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
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
import org.apache.commons.fileupload.util.LimitedInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.api.FilesApiService;
import org.cloudfoundry.multiapps.controller.api.model.FileMetadata;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableFileMetadata;
import org.cloudfoundry.multiapps.controller.client.util.CheckedSupplier;
import org.cloudfoundry.multiapps.controller.client.util.ResilientOperationExecutor;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingProvider;
import org.cloudfoundry.multiapps.controller.core.util.FileUtils;
import org.cloudfoundry.multiapps.controller.core.util.UriUtil;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.util.Configuration;
import org.cloudfoundry.multiapps.controller.web.Constants;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.util.ServletUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Named
public class FilesApiServiceImpl implements FilesApiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilesApiServiceImpl.class);

    @Inject
    @Named("fileService")
    private FileService fileService;

    private final ResilientOperationExecutor resilientOperationExecutor = new ResilientOperationExecutor();

    @Override
    public ResponseEntity<List<FileMetadata>> getFiles(String spaceGuid, String namespace) {
        try {
            List<FileEntry> entries = fileService.listFiles(spaceGuid, namespace);
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
    public ResponseEntity<FileMetadata> uploadFile(HttpServletRequest request, String spaceGuid, String namespace, String fileUrl) {
        try {
            FileEntry fileEntry;
            StopWatch stopWatch = StopWatch.createStarted();
            LOGGER.trace("Received upload request on URI: {}", ServletUtil.decodeUri(request));
            if (StringUtils.isEmpty(fileUrl)) {
                fileEntry = uploadFiles(request, spaceGuid, namespace).get(0);
            } else {
                fileEntry = uploadFileFromUrl(spaceGuid, namespace, fileUrl);
            }
            FileMetadata file = parseFileEntry(fileEntry);
            AuditLoggingProvider.getFacade()
                                .logConfigCreate(file);
            stopWatch.stop();
            LOGGER.trace("Uploaded file \"{}\" with name {}, size {} and digest {} (algorithm {}) for {} ms.", file.getId(), file.getName(),
                         file.getSize(), file.getDigest(), file.getDigestAlgorithm(), stopWatch.getTime());
            return ResponseEntity.status(HttpStatus.CREATED)
                                 .body(file);
        } catch (Exception e) {
            throw new SLException(e, Messages.COULD_NOT_UPLOAD_FILE_0, e.getMessage());
        }
    }

    private List<FileEntry> uploadFiles(HttpServletRequest request, String spaceGuid, String namespace)
        throws FileUploadException, IOException, FileStorageException {
        List<FileEntry> uploadedFiles = new ArrayList<>();
        FileItemIterator fileItemIterator = createFileIterator(request);

        while (fileItemIterator.hasNext()) {
            FileItemStream item = fileItemIterator.next();
            if (item.isFormField()) {
                continue; // ignore simple (non-file) form fields
            }

            try (InputStream in = item.openStream()) {
                FileEntry entry;
                long fileSize = getFileSizeHeaderValue(request);
                if (fileSize != -1) {
                    entry = fileService.addFile(spaceGuid, namespace, item.getName(), in, fileSize);
                } else {
                    entry = fileService.addFile(spaceGuid, namespace, item.getName(), in);
                }
                uploadedFiles.add(entry);
            }
        }
        return uploadedFiles;
    }

    private FileItemIterator createFileIterator(HttpServletRequest request) throws IOException, FileUploadException {
        long maxUploadSize = new Configuration().getMaxUploadSize();
        ServletFileUpload upload = getFileUploadServlet();
        upload.setSizeMax(maxUploadSize);
        try {
            return upload.getItemIterator(request);
        } catch (SizeLimitExceededException ex) {
            throw new SLException(MessageFormat.format(Messages.MAX_UPLOAD_SIZE_EXCEEDED, maxUploadSize));
        }
    }

    protected ServletFileUpload getFileUploadServlet() {
        return new ServletFileUpload();
    }

    private long getFileSizeHeaderValue(HttpServletRequest request) {
        String fileSizeHeader = request.getHeader("X-File-Size");
        if (StringUtils.isEmpty(fileSizeHeader)) {
            return -1L;
        }
        if (!StringUtils.isNumeric(fileSizeHeader)) {
            throw new SLException("Invalid \"X-File-Size\" header: " + fileSizeHeader);
        }
        return Long.parseLong(fileSizeHeader);
    }

    private FileMetadata parseFileEntry(FileEntry fileEntry) {
        return ImmutableFileMetadata.builder()
                                    .id(fileEntry.getId())
                                    .digest(fileEntry.getDigest())
                                    .digestAlgorithm(fileEntry.getDigestAlgorithm())
                                    .name(fileEntry.getName())
                                    .size(fileEntry.getSize())
                                    .space(fileEntry.getSpace())
                                    .namespace(fileEntry.getNamespace())
                                    .build();
    }

    private FileEntry uploadFileFromUrl(String spaceGuid, String namespace, String fileUrl) throws Exception {
        String decodedUrl = new String(Base64.getUrlDecoder()
                                             .decode(fileUrl));
        UriUtil.validateUrl(decodedUrl);
        HttpClient client = buildHttpClient(decodedUrl);

        HttpResponse<InputStream> response = callRemoteEndpointWithRetry(client, decodedUrl);

        long fileSize = response.headers()
                                .firstValueAsLong(Constants.CONTENT_LENGTH)
                                .orElseThrow(() -> new SLException(Messages.FILE_URL_RESPONSE_DID_NOT_RETURN_CONTENT_LENGTH));

        long maxUploadSize = new Configuration().getMaxUploadSize();
        if (fileSize > maxUploadSize) {
            throw new SLException(MessageFormat.format(Messages.MAX_UPLOAD_SIZE_EXCEEDED, maxUploadSize));
        }

        String fileName = extractFileName(decodedUrl);
        FileUtils.validateFileHasExtension(fileName);
        try (InputStream content = createLimitedInputStream(response.body(), maxUploadSize)) {
            return fileService.addFile(spaceGuid, namespace, fileName, content, fileSize);
        }
    }

    private HttpResponse<InputStream> callRemoteEndpointWithRetry(HttpClient client, String decodedUrl) throws Exception {
        return resilientOperationExecutor.execute((CheckedSupplier<HttpResponse<InputStream>>) () -> {
            var response = client.send(buildFetchFileRequest(decodedUrl), BodyHandlers.ofInputStream());
            if (response.statusCode() / 100 != 2) {
                try (InputStream is = response.body()) {
                    String error = IOUtils.toString(is, StandardCharsets.UTF_8);
                    throw new SLException(MessageFormat.format(Messages.ERROR_FROM_REMOTE_MTAR_ENDPOINT, error));
                }
            }
            return response;
        });
    }

    protected HttpClient buildHttpClient(String decodedUrl) {
        return HttpClient.newBuilder()
                         .version(HttpClient.Version.HTTP_1_1)
                         .connectTimeout(Duration.ofMinutes(10))
                         .followRedirects(Redirect.NORMAL)
                         .authenticator(buildPasswordAuthenticator(decodedUrl))
                         .build();
    }

    private Authenticator buildPasswordAuthenticator(String decodedUrl) {
        return new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                var uri = URI.create(decodedUrl);
                var userInfo = uri.getUserInfo();
                if (userInfo != null) {
                    var separatorIndex = userInfo.indexOf(':');
                    var username = userInfo.substring(0, separatorIndex);
                    var password = userInfo.substring(separatorIndex + 1);
                    return new PasswordAuthentication(username, password.toCharArray());
                }
                return super.getPasswordAuthentication();
            }
        };
    }

    private HttpRequest buildFetchFileRequest(String decodedUrl) {
        var builder = HttpRequest.newBuilder()
                                 .GET()
                                 .timeout(Duration.ofMinutes(5));
        var uri = URI.create(decodedUrl);
        var userInfo = uri.getUserInfo();
        if (userInfo != null) {
            builder.uri(URI.create(decodedUrl.replace(userInfo + "@", "")));
        } else {
            builder.uri(uri);
        }
        return builder.build();
    }

    private String extractFileName(String url) {
        String path = URI.create(url).getPath();
        if (path.indexOf('/') == -1) {
            return path;
        }
        String[] pathFragments = path.split("/");
        return pathFragments[pathFragments.length - 1];
    }

    private InputStream createLimitedInputStream(InputStream source, long maxUploadSize) {
        return new LimitedInputStream(source, maxUploadSize) {
            @Override
            protected void raiseError(long maxSize, long currentSize) {
                throw new SLException(MessageFormat.format(Messages.MAX_UPLOAD_SIZE_EXCEEDED, maxUploadSize));
            }
        };
    }

}
