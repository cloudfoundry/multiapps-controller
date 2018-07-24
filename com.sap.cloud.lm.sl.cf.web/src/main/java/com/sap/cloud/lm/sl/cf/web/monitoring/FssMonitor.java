package com.sap.cloud.lm.sl.cf.web.monitoring;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.util.Hashtable;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

public class FssMonitor {
    private static final String KILOBYTE_UNIT = "K";
    private static final String MEGABYTE_UNIT = "M";
    private static final String GIGABYTE_UNIT = "G";
    private static Map<String, Double> usedSpace = new Hashtable<>(1);
    private static Map<String, LocalTime> updateTimes = new Hashtable<>(1);
    private static final int UPDATE_TIMEOUT_MINUTES = 30;
    private static final long DETECTION_TIMEOUT_SECONDS = 30l;
    private static final String OUTPUT_DELIMITER = "\\D";
    private static final Logger LOGGER = LoggerFactory.getLogger(FssMonitor.class);

    private FssMonitor() {

    }

    public static final FssMonitor instance = new FssMonitor();

    public double calculateUsedSpace(String path) {
        if (!updateTimes.containsKey(path)) {
            return getUsedSpace(path);
        }
        if (isCacheValid(path)) {
            return usedSpace.get(path);
        }
        return getUsedSpace(path);
    }

    private boolean isCacheValid(String path) {
        LocalTime lastChecked = updateTimes.get(path);
        LocalTime invalidateDeadline = LocalTime.now()
            .minusMinutes(UPDATE_TIMEOUT_MINUTES);
        return invalidateDeadline.isAfter(lastChecked);
    }

    private double getUsedSpace(String path) {
        String command = MessageFormatter.format("du -sh -- {}", path)
            .getMessage();
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            boolean finishedOnTime = process.waitFor(DETECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finishedOnTime) {
                throw new IOException("Free space detection process failed to finish on time");// misuse?
            }
            if (process.exitValue() != 0) {
                throw new IOException("Free space detection command {1} failed with exit code: {2}");// misuse?
            }
            return parseProcessOutput(process.getInputStream());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread()
                .interrupt();
            LOGGER.warn("Cannot detect remaining space on file system service.", e);
            return Double.MAX_VALUE;// TODO what would a runtime exception result in here?
        }
    }

    // TODO - unit test this
    //@formatter:off
    //parses e.g. '625M    /vcap/data/some/path/here/'
    //parses e.g. '2.5G    /vcap/data/some/path/here/'
    //@formatter:on
    private double parseProcessOutput(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String resultLine = reader.readLine();
            @SuppressWarnings("resource") // no need to close the scanner of a string
            Scanner scanner = new Scanner(resultLine);
            scanner.useDelimiter(OUTPUT_DELIMITER); // non-digit to parse the leading digits
            double value = scanner.nextDouble();
            scanner.reset();// to parse the units
            String unit = scanner.next();
            switch (unit) {
                case GIGABYTE_UNIT:
                    return value * 1024;
                case MEGABYTE_UNIT:
                    return value;
                case KILOBYTE_UNIT:
                    return value / 1024;
                default:
                    return value;
            }
        } catch (IOException e) {
            throw new IOException("Failed to parse space detection process output:", e);
        }
    }
}
