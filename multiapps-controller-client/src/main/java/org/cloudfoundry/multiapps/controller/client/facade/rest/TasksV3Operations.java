package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudTask;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3QueryParameters;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Task;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3TaskMapper;
import org.springframework.core.ParameterizedTypeReference;

public class TasksV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3Task>> TASK_PAGE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client client;

    private final CloudSpace targetSpace;

    public TasksV3Operations(CloudControllerV3Client client, CloudSpace targetSpace) {
        this.client = client;
        this.targetSpace = targetSpace;
    }

    public CloudTask getTask(UUID taskGuid) {
        V3Task task = client.get(V3Uri.path(CloudControllerV3Endpoints.TASK_BY_GUID, taskGuid)
                                      .build(), V3Task.class);

        return task == null ? null : V3TaskMapper.toCloudTask(task);
    }

    public List<CloudTask> getTasks(String applicationName) {
        UUID applicationGuid = OperationsUtil.getRequiredApplicationGuid(client, targetSpace, applicationName);

        String query = V3Uri.path(CloudControllerV3Endpoints.TASKS)
                            .perPage(CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE)
                            .query(V3QueryParameters.APP_GUIDS, applicationGuid)
                            .build();

        List<V3Task> tasks = client.list(query, TASK_PAGE);

        return tasks.stream()
                    .map(V3TaskMapper::toCloudTask)
                    .toList();
    }

    public CloudTask runTask(String applicationName, CloudTask task) {
        UUID applicationGuid = OperationsUtil.getRequiredApplicationGuid(client, targetSpace, applicationName);
        Map<String, Object> createTaskBody = OperationsUtil.buildCreateTaskBody(task);

        V3Task created = client.postForObject(CloudControllerV3Endpoints.APP_TASKS, createTaskBody, V3Task.class, applicationGuid);

        return created == null ? null : V3TaskMapper.toCloudTask(created);
    }

    public CloudTask cancelTask(UUID taskGuid) {
        V3Task cancelled = client.postForObject(CloudControllerV3Endpoints.TASK_CANCEL, null, V3Task.class, taskGuid);

        return cancelled == null ? null : V3TaskMapper.toCloudTask(cancelled);
    }

}
