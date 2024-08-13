package org.cloudfoundry.multiapps.controller.process.stream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LazyArchiveInputStream extends InputStream {

    private static final Logger LOGGER = LoggerFactory.getLogger(LazyArchiveInputStream.class);
    private static final int BUFFERED_SIZE = 16 * 1024;

    private final FileService fileService;
    private final List<FileEntry> archiveFileEntries;
    private final StepLogger stepLogger;
    private final long archiveSize;
    private final AtomicInteger totalBytesRead;
    private final AtomicInteger partIndex;

    private InputStream currentInputStream;

    public LazyArchiveInputStream(FileService fileService, List<FileEntry> archiveFileEntries, StepLogger stepLogger, long archiveSize) {
        this.fileService = fileService;
        this.archiveFileEntries = archiveFileEntries;
        this.stepLogger = stepLogger;
        this.archiveSize = archiveSize;
        this.totalBytesRead = new AtomicInteger(0);
        this.partIndex = new AtomicInteger(0);
    }

    @Override
    public synchronized int read() throws IOException {
        if (currentInputStream == null) {
            currentInputStream = openBufferedInputStream(archiveFileEntries.get(partIndex.get()));
        }
        int c = currentInputStream.read();
        if (c == -1 && partIndex.get() < archiveFileEntries.size() - 1) {
            IOUtils.closeQuietly(currentInputStream, e -> LOGGER.warn(e.getMessage(), e));
            LOGGER.info(MessageFormat.format(Messages.CLOSING_STREAM_FOR_PART_0, partIndex));
            currentInputStream = openBufferedInputStream(archiveFileEntries.get(partIndex.incrementAndGet()));
            c = currentInputStream.read();
        } else if (c == -1) {
            LOGGER.info(MessageFormat.format(Messages.CLOSING_STREAM_FOR_PART_STREAM_FINISHED_0, partIndex));
            IOUtils.closeQuietly(currentInputStream, e -> LOGGER.warn(e.getMessage(), e));
        }
        if (c >= 0) {
            totalBytesRead.incrementAndGet();
        }
        return c;
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        if (currentInputStream == null) {
            currentInputStream = openBufferedInputStream(archiveFileEntries.get(partIndex.get()));
        }
        int bytesRead = currentInputStream.read(b, off, len);
        if (bytesRead == -1 && partIndex.get() < archiveFileEntries.size() - 1) {
            IOUtils.closeQuietly(currentInputStream, e -> LOGGER.warn(e.getMessage(), e));
            LOGGER.info(MessageFormat.format(Messages.CLOSING_STREAM_FOR_PART_0, partIndex));
            currentInputStream = openBufferedInputStream(archiveFileEntries.get(partIndex.incrementAndGet()));
            bytesRead = currentInputStream.read(b, off, len);
        } else if (bytesRead == -1) {
            LOGGER.info(MessageFormat.format(Messages.CLOSING_STREAM_FOR_PART_STREAM_FINISHED_0, partIndex));
            IOUtils.closeQuietly(currentInputStream, e -> LOGGER.warn(e.getMessage(), e));
        }
        if (bytesRead > 0) {
            totalBytesRead.addAndGet(bytesRead);
        }
        return bytesRead;
    }

    @Override
    public synchronized int available() throws IOException {
        // The return value of this method must be anything except 0
        // because this way jclouds will use it to skip these bytes
        // but the skip method actually does not skip anything intentionally
        // (jclouds creates a new stream and overrides skip and close and makes them do nothing)...
        // If this method returns 0 jclouds will try to skip the stream by reading it
        // thus making it invalid as skip is not required
        long remainingBytes = archiveSize - totalBytesRead.get();
        if (remainingBytes > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) remainingBytes;
    }

    @Override
    public void close() throws IOException {
        LOGGER.info(MessageFormat.format(Messages.CLOSING_LAST_STREAM_FOR_PART_0, partIndex));
        IOUtils.closeQuietly(currentInputStream, e -> LOGGER.warn(e.getMessage(), e));
    }

    private BufferedInputStream openBufferedInputStream(FileEntry archiveFileEntry) {
        try {
            stepLogger.debug(Messages.OPENING_A_NEW_INPUT_STREAM_FOR_FILE_WITH_ID_0_AND_NAME_1, archiveFileEntry.getId(),
                             archiveFileEntry.getName());
            InputStream inputStream = fileService.openInputStream(archiveFileEntry.getSpace(), archiveFileEntry.getId());
            return new BufferedInputStream(inputStream, BUFFERED_SIZE);
        } catch (FileStorageException e) {
            throw new SLException(e, e.getMessage());
        }
    }
}
