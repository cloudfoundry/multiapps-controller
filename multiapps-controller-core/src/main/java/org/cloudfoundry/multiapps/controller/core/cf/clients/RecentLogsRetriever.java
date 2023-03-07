package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.util.LogsOffset;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.ApplicationLog;

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
