package org.cloudfoundry.multiapps.controller.process.util;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.cloudfoundry.multiapps.common.SLException;

public class FilePartsMerger implements Closeable {

    private List<InputStream> fileContentInputStreams;

    public FilePartsMerger(String fileName) {
        fileContentInputStreams = new ArrayList<>();
    }

    public void merge(InputStream filePartInputStream) throws IOException {
        fileContentInputStreams.add(filePartInputStream);
    }

    public SequenceInputStream getMergedInputStream() {
        return new SequenceInputStream(Collections.enumeration(fileContentInputStreams));
    }

    public void cleanUp() {
        fileContentInputStreams.forEach(fileContentInputStream -> {
            try {
                fileContentInputStream.close();
            } catch (IOException e) {
                //ignore
            }
        });
    }

    @Override
    public void close() {
        //cleanUp();
    }
}
