package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudTask;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Application;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Task;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3TaskMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;

public class TasksV3Operations {

    private final CloudControllerV3Client cc;
    private final CloudSpace target;

    public TasksV3Operations(CloudControllerV3Client cc, CloudSpace target) {
        this.cc = cc;
        this.target = target;
    }

    public CloudTask getTask(UUID taskGuid) {
        V3Task task = cc.get(CloudControllerV3Endpoints.TASKS + "/" + taskGuid, V3Task.class);

        return task == null ? null : V3TaskMapper.toCloudTask(task);
    }

    public List<CloudTask> getTasks(String applicationName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);

        String query = CloudControllerV3Endpoints.TASKS + CloudControllerV3Endpoints.QUERY_PER_PAGE
            + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE + CloudControllerV3Endpoints.AMPERSAND_APP_GUIDS + applicationGuid;

        List<V3Task> tasks = cc.list(query, new ParameterizedTypeReference<V3ListResponse<V3Task>>() {
        });

        return tasks.stream()
                    .map(V3TaskMapper::toCloudTask)
                    .toList();
    }

    public CloudTask runTask(String applicationName, CloudTask task) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);

        V3Task created = cc.getRestClient()
                           .post()
                           .uri(CloudControllerV3Endpoints.APP_TASKS, applicationGuid)
                           .body(buildCreateTaskBody(task))
                           .retrieve()
                           .body(V3Task.class);

        return created == null ? null : V3TaskMapper.toCloudTask(created);
    }

    public CloudTask cancelTask(UUID taskGuid) {
        V3Task cancelled = cc.getRestClient()
                             .post()
                             .uri(CloudControllerV3Endpoints.TASK_CANCEL, taskGuid)
                             .retrieve()
                             .body(V3Task.class);

        return cancelled == null ? null : V3TaskMapper.toCloudTask(cancelled);
    }

    private Map<String, Object> buildCreateTaskBody(CloudTask task) {
        Map<String, Object> body = new HashMap<>();
        body.put("command", task.getCommand());
        body.put("name", task.getName());

        CloudTask.Limits limits = task.getLimits();
        if (limits != null) {
            if (limits.getMemory() != null) {
                body.put("memory_in_mb", limits.getMemory());
            }

            if (limits.getDisk() != null) {
                body.put("disk_in_mb", limits.getDisk());
            }
        }

        return body;
    }

    private UUID getRequiredApplicationGuid(String applicationName) {
        StringBuilder query = new StringBuilder(CloudControllerV3Endpoints.APPS + CloudControllerV3Endpoints.QUERY_PER_PAGE
                                                    + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE);

        if (target != null && target.getGuid() != null) {
            query.append(CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS)
                 .append(target.getGuid());
        }

        query.append(CloudControllerV3Endpoints.AMPERSAND_NAMES)
             .append(applicationName);
        List<V3Application> apps = cc.list(query.toString(), new ParameterizedTypeReference<V3ListResponse<V3Application>>() {
        });

        if (apps.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.APPLICATION_0_NOT_FOUND, applicationName));
        }

        return UUID.fromString(apps.getFirst()
                                   .guid());
    }

}
