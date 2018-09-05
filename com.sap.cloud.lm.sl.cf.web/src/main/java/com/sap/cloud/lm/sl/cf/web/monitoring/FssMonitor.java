package com.sap.cloud.lm.sl.cf.web.monitoring;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.util.Hashtable;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;

@Component
public class FssMonitor {
    private static final String KILOBYTE_UNIT = "K";
    private static final String MEGABYTE_UNIT = "M";
    private static final String GIGABYTE_UNIT = "G";
    private static final String OUTPUT_DELIMITER = "[^0-9.,$s]";
    private static final long DETECTION_TIMEOUT_SECONDS = 30l;
    private static final Logger LOGGER = LoggerFactory.getLogger(FssMonitor.class);

    Map<String, Double> usedSpace = new Hashtable<>(1);
    Map<String, LocalTime> updateTimes = new Hashtable<>(1);

    private Integer updateTimeoutMinutes;

    @Inject
    public FssMonitor(ApplicationConfiguration appConfigurations) {
        this.updateTimeoutMinutes = appConfigurations.getFssCacheUpdateTimeoutMinutes();
    }

    public double calculateUsedSpace(String path) {
        if (!updateTimes.containsKey(path)) {
            return getUsedSpace(path);
        }
        if (isCacheValid(path)) {
            return usedSpace.get(path);
        }
        return getUsedSpace(path);
    }

    // using package-private modifier for testing
    boolean isCacheValid(String path) {
        LocalTime lastChecked = updateTimes.get(path);
        LocalTime invalidateDeadline = LocalTime.now()
            .minusMinutes(updateTimeoutMinutes);
        return invalidateDeadline.isBefore(lastChecked);
    }

    private double getUsedSpace(String path) {
        String command = MessageFormatter.format("du -sh -- {}", path)
            .getMessage();
        ProcessBuilder builder = new ProcessBuilder("sh", "-c", command);
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            boolean finishedOnTime = process.waitFor(DETECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finishedOnTime) {
                throw new IOException("Free space detection process failed to finish on time");// misuse?
            }
            if (process.exitValue() != 0) {
                throw new IOException(
                    String.format("Free space detection command {%s} failed with exit code: {%d}", command, process.exitValue()));
            }
            double parsedUsedSpace = parseProcessOutput(process.getInputStream());
            updateTimes.put(path, LocalTime.now());
            usedSpace.put(path, parsedUsedSpace);
            return parsedUsedSpace;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread()
                .interrupt();
            LOGGER.warn("Cannot detect remaining space on file system service.", e);
            return 0d;
        }
    }

    // using package-private modifier for testing
    double parseProcessOutput(InputStream inputStream) throws IOException {
        String resultLine = "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            resultLine = reader.readLine();
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
        } catch (IOException | InputMismatchException e) {
            LOGGER.debug("Log process output: {}", resultLine);
            throw new IOException("Failed to parse space detection process output:", e);
        }
    }
}
