package org.cloudfoundry.multiapps.controller.process.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.process.Messages;

public class InflatorUtil {

    private static final int BUFFER_SIZE = 60 * 1024 * 1024; // 4KB

    public static void inflate(InputStream compressedStream, OutputStream outputStream, ApplicationArchiveContext applicationArchiveContext)
        throws DataFormatException, IOException {
        Inflater inflater = new Inflater(true);
        byte[] compressedBytes = new byte[BUFFER_SIZE];
        byte[] buffer = new byte[BUFFER_SIZE];
        int numberOfReadBytes = 0;
        long maxSizeInBytes = applicationArchiveContext.getMaxSizeInBytes();
        try (compressedStream) {
            while ((numberOfReadBytes = compressedStream.read(compressedBytes)) != -1) {
                inflater.setInput(compressedBytes, 0, numberOfReadBytes);
                while (!inflater.finished()) {
                    int count = inflater.inflate(buffer);
                    long currentSizeInBytes = applicationArchiveContext.getCurrentSizeInBytes();
                    if (currentSizeInBytes + count > maxSizeInBytes) {
                        throw new ContentException(Messages.SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT, maxSizeInBytes);
                    }
                    outputStream.write(buffer, 0, count);
                    applicationArchiveContext.calculateCurrentSizeInBytes(count);
                    if (count == 0) {
                        break;
                    }
                }
            }
        } catch (DataFormatException e) {
            throw new DataFormatException("Data format error while inflating: " + e.getMessage());
        } finally {
            inflater.end();
        }
    }

    public static byte[] inflate(byte[] compressedBytes, long maxSizeInBytes) throws DataFormatException, IOException {
        Inflater inflater = new Inflater(true);
        inflater.setInput(compressedBytes);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(compressedBytes.length);
        try (outputStream) {
            byte[] buffer = new byte[BUFFER_SIZE];
            long currentSize = 0;
            while (!inflater.finished()) {
                int inflatedBytes = inflater.inflate(buffer);
                if (inflatedBytes + currentSize > maxSizeInBytes) {
                    throw new ContentException(Messages.SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT, maxSizeInBytes);
                }
                outputStream.write(buffer, 0, inflatedBytes);
                currentSize += inflatedBytes;
                if (inflatedBytes == 0) {
                    break;
                }
            }
        } catch (DataFormatException e) {
            throw new DataFormatException("Data format error while inflating: " + e.getMessage());
        } finally {
            inflater.end();
        }
        return outputStream.toByteArray();
    }
}
