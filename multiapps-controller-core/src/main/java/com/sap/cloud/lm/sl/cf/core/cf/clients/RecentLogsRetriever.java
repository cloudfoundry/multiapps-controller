package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.ApplicationLog;

import com.sap.cloud.lm.sl.cf.core.util.LogsOffset;

@Named
public class RecentLogsRetriever {

    public List<ApplicationLog> getRecentLogsSafely(CloudControllerClient client, String appName, LogsOffset offset) {
        try {
            return getRecentLogs(client, appName, offset);
        } catch (RuntimeException e) {
            return Collections.emptyList();
        }
    }

    public List<ApplicationLog> getRecentLogs(CloudControllerClient client, String appName, LogsOffset offset) {
        List<ApplicationLog> appLogs = client.getRecentLogs(appName);
        if (offset == null) {
            return appLogs;
        }
        return appLogs.stream()
                      .filter(appLog -> isLogNew(appLog, offset))
                      .collect(Collectors.toList());
    }

    private boolean isLogNew(ApplicationLog log, LogsOffset offset) {
        if (log.getTimestamp()
               .compareTo(offset.getTimestamp()) > 0) {
            return true;
        }
        return log.getTimestamp()
                  .equals(offset.getTimestamp())
            && !log.getMessage()
                   .equals(offset.getMessage());
    }
}
