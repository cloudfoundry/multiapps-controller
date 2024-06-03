package org.cloudfoundry.multiapps.controller.process.util;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FilePartsMerger implements Closeable {

    private final List<InputStream> fileContentInputStreams;

    public FilePartsMerger(String fileName) {
        fileContentInputStreams = new ArrayList<>();
    }

    public void merge(InputStream filePartInputStream) throws IOException {
        fileContentInputStreams.add(new ByteArrayInputStream(filePartInputStream.readAllBytes()));
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

    public class Test {

        private SequenceInputStream sequenceInputStream;
        private int length;
        public Test(SequenceInputStream sequenceInputStream, int length) {
            this.length = length;
            this.sequenceInputStream = sequenceInputStream;
        }
    }
}
