package com.sap.cloud.lm.sl.cf.core.monitoring;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FssMonitor {
	private static double freeSpace;
	private static LocalTime lastUpdated;
	private static final int UPDATE_TIMEOUT_MINUTES = 30;
	private static final String OUTPUT_DELIMITER = "\\D";
	private static final Logger LOGGER = LoggerFactory.getLogger(FssMonitor.class);

	public static double calculateFreeSpace(String path) {
		if (!isCacheExpired()) {
			return freeSpace;
		}

		freeSpace = getFreeSpace(path);
		return freeSpace;
	}

	@SuppressWarnings("resource")
	private static double getFreeSpace(String path) {
		String command = "du " + path + " -sh";
		Scanner scanner = null;
		BufferedReader reader = null;
		try {
			Process process = Runtime.getRuntime().exec(command);
			process.waitFor();
			reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String output = reader.readLine();
			scanner = new Scanner(output).useDelimiter(OUTPUT_DELIMITER);
			return scanner.nextDouble();
		} catch (Exception e) {
			LOGGER.warn("Cannot get remaining space on file system service.");
			return freeSpace;
		} finally {
			try {
				reader.close();
				scanner.close();
			} catch (IOException e) {
				LOGGER.error("Cannot close output stream.");
			}
		}
	}

	private static boolean isCacheExpired() {
		if (lastUpdated == null) {
			lastUpdated = LocalTime.now();
			return true;
		}
		LocalTime currentTime = LocalTime.now();
		LocalTime lastUpdatedTimePlusTimeout = lastUpdated;
		
		if (lastUpdatedTimePlusTimeout.plusMinutes(UPDATE_TIMEOUT_MINUTES).isBefore(currentTime)) {
			lastUpdated = LocalTime.now();
			return true;
		}
		return false;
	}
}
