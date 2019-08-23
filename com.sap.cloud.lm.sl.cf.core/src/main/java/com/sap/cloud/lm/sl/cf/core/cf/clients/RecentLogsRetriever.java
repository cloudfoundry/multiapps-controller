package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import com.sap.cloud.lm.sl.cf.core.util.LogsOffset;
import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.ApplicationLog;

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
        return offset == null ? appLogs : ListUtils.select(appLogs, appLog -> isLogNew(appLog, offset));
    }

    private boolean isLogNew(ApplicationLog log, LogsOffset offset) {
        if (log.getTimestamp()
               .compareTo(offset.getTimestamp()) > 0) {
            return true;
        }
        return log.getTimestamp()
                  .equals(offset.getTimestamp()) && !log.getMessage()
                                                        .equals(offset.getMessage());
    }
}
