package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudTask;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3GuidReference;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Task;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3TaskMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;

public class TasksV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3Task>> TASK_PAGE = new ParameterizedTypeReference<>() {
    };

    private static final ParameterizedTypeReference<V3ListResponse<V3GuidReference>> APP_PAGE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client client;

    private final CloudSpace targetSpace;

    public TasksV3Operations(CloudControllerV3Client client, CloudSpace targetSpace) {
        this.client = client;
        this.targetSpace = targetSpace;
    }

    public CloudTask getTask(UUID taskGuid) {
        V3Task task = client.get(CloudControllerV3Endpoints.TASKS + "/" + taskGuid, V3Task.class);

        return task == null ? null : V3TaskMapper.toCloudTask(task);
    }

    public List<CloudTask> getTasks(String applicationName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);

        String query = CloudControllerV3Endpoints.TASKS + CloudControllerV3Endpoints.QUERY_PER_PAGE
            + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE + CloudControllerV3Endpoints.AMPERSAND_APP_GUIDS + applicationGuid;

        List<V3Task> tasks = client.list(query, TASK_PAGE);

        return tasks.stream()
                    .map(V3TaskMapper::toCloudTask)
                    .toList();
    }

    public CloudTask runTask(String applicationName, CloudTask task) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        Map<String, Object> createTaskBody = OperationsUtil.buildCreateTaskBody(task);

        V3Task created = client.postForObject(CloudControllerV3Endpoints.APP_TASKS, createTaskBody, V3Task.class, applicationGuid);

        return created == null ? null : V3TaskMapper.toCloudTask(created);
    }

    public CloudTask cancelTask(UUID taskGuid) {
        V3Task cancelled = client.postForObject(CloudControllerV3Endpoints.TASK_CANCEL, null, V3Task.class, taskGuid);

        return cancelled == null ? null : V3TaskMapper.toCloudTask(cancelled);
    }

    private UUID getRequiredApplicationGuid(String applicationName) {
        String applicationsQuery = OperationsQueryUtil.buildApplicationsQuery(applicationName, targetSpace);
        List<V3GuidReference> apps = client.list(applicationsQuery, APP_PAGE);

        if (apps.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.APPLICATION_0_NOT_FOUND, applicationName));
        }

        return UUID.fromString(apps.getFirst()
                                   .guid());
    }

}
