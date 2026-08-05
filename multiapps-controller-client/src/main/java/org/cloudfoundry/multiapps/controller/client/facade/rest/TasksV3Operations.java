package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudTask;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Application;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Task;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3TaskMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;

/**
 * CF v3 <em>task</em> operations for the cf-java-client replacement. Reproduces the HTTP shape and domain-mapping of the OSS
 * {@code CloudControllerRestClientImpl} task methods:
 * <ul>
 * <li>{@code getTask} &rarr; {@code GET /v3/tasks/{guid}}</li>
 * <li>{@code getTasks} &rarr; {@code GET /v3/tasks?app_guids={appGuid}} (paginated)</li>
 * <li>{@code runTask} &rarr; {@code POST /v3/apps/{appGuid}/tasks}</li>
 * <li>{@code cancelTask} &rarr; {@code POST /v3/tasks/{guid}/actions/cancel}</li>
 * </ul>
 * Application-name resolution mirrors the OSS impl's {@code getRequiredApplicationGuid}: it queries the target space and throws
 * {@link CloudOperationException} with {@link HttpStatus#NOT_FOUND} when the application does not exist.
 */
public class TasksV3Operations {

    // CF v3 caps per_page at 5000; use a large page to minimise round-trips (the pagination walker still handles multiple pages).
    private static final int DEFAULT_PAGE_SIZE = 5000;

    private final CloudControllerV3Client cc;
    private final CloudSpace target;

    public TasksV3Operations(CloudControllerV3Client cc, CloudSpace target) {
        this.cc = cc;
        this.target = target;
    }

    public CloudTask getTask(UUID taskGuid) {
        V3Task task = cc.get("/v3/tasks/" + taskGuid, V3Task.class);
        return task == null ? null : V3TaskMapper.toCloudTask(task);
    }

    public List<CloudTask> getTasks(String applicationName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        String query = "/v3/tasks?per_page=" + DEFAULT_PAGE_SIZE + "&app_guids=" + applicationGuid;
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
                           .uri("/v3/apps/{guid}/tasks", applicationGuid)
                           .body(buildCreateTaskBody(task))
                           .retrieve()
                           .body(V3Task.class);
        return created == null ? null : V3TaskMapper.toCloudTask(created);
    }

    public CloudTask cancelTask(UUID taskGuid) {
        V3Task cancelled = cc.getRestClient()
                             .post()
                             .uri("/v3/tasks/{guid}/actions/cancel", taskGuid)
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

    // Mirrors the OSS impl's getRequiredApplicationGuid: look up the app in the target space and 404 if it is absent.
    private UUID getRequiredApplicationGuid(String applicationName) {
        StringBuilder query = new StringBuilder("/v3/apps?per_page=" + DEFAULT_PAGE_SIZE);
        if (target != null && target.getGuid() != null) {
            query.append("&space_guids=")
                 .append(target.getGuid());
        }
        query.append("&names=")
             .append(applicationName);
        List<V3Application> apps = cc.list(query.toString(), new ParameterizedTypeReference<V3ListResponse<V3Application>>() {
        });
        if (apps.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Application " + applicationName + " not found.");
        }
        return UUID.fromString(apps.get(0)
                                   .guid());
    }

}
