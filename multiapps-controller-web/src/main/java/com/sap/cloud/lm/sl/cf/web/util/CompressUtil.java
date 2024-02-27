package com.sap.cloud.lm.sl.cf.web.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

import org.apache.commons.io.IOUtils;

import com.sap.cloud.lm.sl.cf.web.message.Messages;

public class CompressUtil {

    public static byte[] compress(byte[] bytes) {
        ByteArrayOutputStream bytesOut = null;
        OutputStream deflaterOutputStream = null;
        try {
            bytesOut = new ByteArrayOutputStream();
            deflaterOutputStream = new DeflaterOutputStream(bytesOut, new Deflater(Deflater.BEST_COMPRESSION));
            deflaterOutputStream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(Messages.ERROR_COMPRESSING_OAUTH_TOKEN, e);
        } finally {
            IOUtils.closeQuietly(bytesOut);
            IOUtils.closeQuietly(deflaterOutputStream);
        }
        return bytesOut.toByteArray();
    }

    public static byte[] decompress(byte[] bytes) {
        ByteArrayOutputStream bytesOut = null;
        OutputStream inflaterOutputStream = null;
        try {
            bytesOut = new ByteArrayOutputStream();
            inflaterOutputStream = new InflaterOutputStream(bytesOut);
            inflaterOutputStream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(Messages.ERROR_DECOMPRESSING_OAUTH_TOKEN, e);
        } finally {
            IOUtils.closeQuietly(bytesOut);
            IOUtils.closeQuietly(inflaterOutputStream);
        }
        return bytesOut.toByteArray();
    }
}
