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
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.DigestHelper;
import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.cloudfoundry.multiapps.controller.persistence.DataSourceWithDialect;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.FileInfo;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileInfo;
import org.cloudfoundry.multiapps.controller.persistence.query.providers.ByteArraySqlFileQueryProvider;

@Named("processLogsPersistenceService")
public class ProcessLogsPersistenceService extends DatabaseFileService {

    public static final String TABLE_NAME = "process_log";

    public ProcessLogsPersistenceService(DataSourceWithDialect dataSourceWithDialect) {
        super(dataSourceWithDialect, new ByteArraySqlFileQueryProvider(TABLE_NAME, dataSourceWithDialect.getDataSourceDialect()));
    }

    public List<String> getLogNames(String space, String namespace) throws FileStorageException {
        List<FileEntry> logFiles = listFiles(space, namespace);
        return logFiles.stream()
                       .map(FileEntry::getName)
                       .distinct()
                       .collect(Collectors.toList());
    }

    public String getLogContent(String space, String namespace, String logName) throws FileStorageException {
        List<FileEntry> logFiles = listFiles(space, namespace, logName);
        if (logFiles.isEmpty()) {
            throw new NotFoundException(MessageFormat.format(Messages.ERROR_LOG_FILE_NOT_FOUND, logName, namespace, space));
        }

        StringBuilder builder = new StringBuilder();
        for (FileEntry file : logFiles) {
            String content = processFileContent(space, file.getId(), inputStream -> IOUtils.toString(inputStream, StandardCharsets.UTF_8));
            builder.append(content);
        }
        return builder.toString();
    }

    private List<FileEntry> listFiles(final String space, final String namespace, final String fileName) throws FileStorageException {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getListFilesQuery(space, namespace, fileName));
        } catch (SQLException e) {
            throw new FileStorageException(MessageFormat.format(Messages.ERROR_GETTING_FILES_WITH_SPACE_NAMESPACE_AND_NAME, space,
                                                                namespace, fileName),
                                           e);
        }
    }

    public void persistLog(String space, String namespace, File localLog, String remoteLogName) {
        try {
            storeLogFile(space, namespace, remoteLogName, localLog);
        } catch (FileStorageException e) {
            logger.warn(MessageFormat.format(Messages.COULD_NOT_PERSIST_LOGS_FILE, localLog.getName()));
        }
    }

    private void storeLogFile(final String space, final String namespace, final String remoteLogName, File localLog)
        throws FileStorageException {
        try (InputStream inputStream = new FileInputStream(localLog)) {
            FileEntry localLogFileEntry = createFileEntry(space, namespace, remoteLogName, localLog);
            getSqlQueryExecutor().execute(getSqlFileQueryProvider().getStoreFileQuery(localLogFileEntry, inputStream));
        } catch (SQLException | IOException | NoSuchAlgorithmException e) {
            throw new FileStorageException(MessageFormat.format(Messages.ERROR_STORING_LOG_FILE, localLog.getName()), e);
        }
    }

    private FileEntry createFileEntry(final String space, final String namespace, final String remoteLogName, File localLog)
        throws NoSuchAlgorithmException, IOException {

        FileInfo localLogFileInfo = ImmutableFileInfo.builder()
                                                     .file(localLog)
                                                     .size(BigInteger.valueOf(localLog.length()))
                                                     .digest(DigestHelper.computeFileChecksum(localLog.toPath(),
                                                                                              Constants.DIGEST_ALGORITHM))
                                                     .digestAlgorithm(Constants.DIGEST_ALGORITHM)
                                                     .build();
        return createFileEntry(space, namespace, remoteLogName, localLogFileInfo);
    }

    public int deleteByNamespace(final String namespace) {
        try {
            return getSqlQueryExecutor().execute(getSqlFileQueryProvider().getDeleteByNamespaceQuery(namespace));
        } catch (SQLException e) {
            throw new SLException(e, Messages.ERROR_DELETING_PROCESS_LOGS_WITH_NAMESPACE, namespace);
        }
    }
}
