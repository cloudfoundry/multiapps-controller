package org.cloudfoundry.multiapps.controller.persistence.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Named;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.common.util.DigestHelper;
import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.cloudfoundry.multiapps.controller.persistence.DataSourceWithDialect;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.FileInfo;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileInfo;
import org.cloudfoundry.multiapps.controller.persistence.query.providers.ByteArraySqlFileQueryProvider;

@Named("processLogsPersistenceService")
public class ProcessLogsPersistenceService extends DatabaseFileService {

    public static final String TABLE_NAME = "process_log";

    public ProcessLogsPersistenceService(DataSourceWithDialect dataSourceWithDialect) {
        super(dataSourceWithDialect, new ByteArraySqlFileQueryProvider(TABLE_NAME, dataSourceWithDialect.getDataSourceDialect()));
    }

    public List<String> getLogNames(String space, String operationId) throws FileStorageException {
        List<FileEntry> logFiles = listFilesBySpaceAndOperationId(space, operationId);
        return logFiles.stream()
                       .map(FileEntry::getName)
                       .distinct()
                       .collect(Collectors.toList());
    }

    public String getLogContent(String space, String operationId, String logName) throws FileStorageException {
        List<FileEntry> logFiles = listFiles(space, operationId, logName);
        if (logFiles.isEmpty()) {
            throw new NotFoundException(MessageFormat.format(Messages.ERROR_LOG_FILE_NOT_FOUND, logName, operationId, space));
        }

        StringBuilder builder = new StringBuilder();
        for (FileEntry file : logFiles) {
            String content = processFileContent(space, file.getId(), inputStream -> IOUtils.toString(inputStream, StandardCharsets.UTF_8));
            builder.append(content);
        }
        return builder.toString();
    }

    private List<FileEntry> listFiles(final String space, final String operationId, final String fileName) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getListFilesQueryBySpaceOperationIdAndFileName(space,
                                                                                                                          operationId,
                                                                                                                          fileName));
        } catch (SQLException e) {
            throw new FileStorageException(MessageFormat.format(Messages.ERROR_GETTING_FILES_WITH_SPACE_OPERATION_ID_AND_NAME, space,
                                                                operationId, fileName),
                                           e);
        }
    }

    public void persistLog(String space, String operationId, File localLog, String remoteLogName) {
        try {
            storeLogFile(space, operationId, remoteLogName, localLog);
        } catch (FileStorageException e) {
            logger.warn(MessageFormat.format(Messages.COULD_NOT_PERSIST_LOGS_FILE, localLog.getName()), e);
        }
    }

    private void storeLogFile(final String space, final String operationId, final String remoteLogName, File localLog)
        throws FileStorageException {
        try (InputStream inputStream = new FileInputStream(localLog)) {
            FileEntry localLogFileEntry = createFileEntry(space, operationId, remoteLogName, localLog);
            getSqlQueryExecutor().execute(getSqlFileQueryProvider().getStoreFileQuery(localLogFileEntry, inputStream));
        } catch (SQLException | IOException | NoSuchAlgorithmException e) {
            throw new FileStorageException(MessageFormat.format(Messages.ERROR_STORING_LOG_FILE, localLog.getName()), e);
        }
    }

    private FileEntry createFileEntry(final String space, final String operationId, final String remoteLogName, File localLog)
        throws NoSuchAlgorithmException, IOException {
        FileInfo localLogFileInfo = ImmutableFileInfo.builder()
                                                     .file(localLog)
                                                     .size(BigInteger.valueOf(localLog.length()))
                                                     .digest(DigestHelper.computeFileChecksum(localLog.toPath(),
                                                                                              Constants.DIGEST_ALGORITHM))
                                                     .digestAlgorithm(Constants.DIGEST_ALGORITHM)
                                                     .build();
        return ImmutableFileEntry.builder()
                                 .id(generateRandomId())
                                 .space(space)
                                 .name(remoteLogName)
                                 .size(localLogFileInfo.getSize())
                                 .digest(localLogFileInfo.getDigest())
                                 .digestAlgorithm(localLogFileInfo.getDigestAlgorithm())
                                 .modified(LocalDateTime.now())
                                 .operationId(operationId)
                                 .build();
    }

}
