package org.cloudfoundry.multiapps.controller.process.flowable;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceKey;
import org.cloudfoundry.multiapps.controller.core.cf.apps.ApplicationStateAction;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.ImmutableMtaMetadata;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaService;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaService;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationURI;
import org.cloudfoundry.multiapps.controller.process.steps.StepPhase;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionModule;
import org.cloudfoundry.multiapps.mta.model.ExtensionResource;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.ProvidedDependency;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.model.Version;

public class FlowableProcessTestDataUtils {

    // MTA and test identifiers
    private static final String DEFAULT_MTA_ID = "test-mta";
    private static final String DEFAULT_MTA_VERSION = "1.0.0";
    private static final String UPDATE_SCENARIO_MTA_ID = "test-mta-2";
    private static final String UPDATE_SCENARIO_VERSION_V2 = "2.0.0";
    private static final String TEST_USER_EMAIL = "test-user@example.com";

    // Module names and types
    private static final String MODULE_1_NAME = "module-1";
    private static final String MODULE_2_NAME = "module-2";
    private static final String MODULE_3_NAME = "module-3";
    private static final String MEMORY_1G = "1G";
    private static final String MODULE_TYPE_JAVASCRIPT = "javascript";
    private static final String MODULE_TYPE_JAVA = "java";
    private static final int MODULES_COUNT = 5;

    // Memory and disk quota values
    private static final String MEMORY_512M = "512M";
    private static final String DISK_QUOTA_256M = "256M";
    private static final String DISK_QUOTA_4096M = "4096M";
    private static final int INSTANCES_COUNT_2 = 2;

    // Route configurations
    private static final String ROUTE_1_EXAMPLE_COM = "route-1.example.com/foo-bar";
    private static final String ROUTE_2_EXAMPLE_COM = "route-2.example.com";
    private static final String ROUTE_3_EXAMPLE_COM = "route-3.example.com";
    private static final String ROUTE_1_IDLE_EXAMPLE_COM = "route-1-idle.example.com/bar-foo";
    private static final String ROUTE_2_IDLE_EXAMPLE_COM = "route-2-idle.example.com";
    private static final String MODULE_1_ROUTE_EXAMPLE_COM = "module-1-route.example.com";
    private static final String MODULE_2_ROUTE_EXAMPLE_COM = "module-2-route.example.com";
    private static final String MODULE_3_ROUTE_EXAMPLE_COM = "module-3-route.example.com";

    // Resource names
    private static final String RESOURCE_DB_NAME = "db";
    private static final String RESOURCE_CACHE_NAME = "cache";
    private static final String RESOURCE_AUTOSCALER_NAME = "autoscaler";
    private static final String RESOURCE_APPLICATION_LOGS_NAME = "application-logs";

    // Service configurations
    private static final String SERVICE_TYPE_MANAGED = "org.cloudfoundry.managed-service";
    private static final String SERVICE_TYPE_USER_PROVIDED = "org.cloudfoundry.user-provided-service";
    private static final String TEST_DB_SERVICE = "test-db-service";
    private static final String TEST_CACHE_SERVICE = "test-cache-service";
    private static final String APP_AUTOSCALER_SERVICE = "app-autoscaler";
    private static final String SERVICE_PLAN_FREE = "free";
    private static final String SERVICE_PLAN_DEFAULT = "default";

    // Task configurations
    private static final String TASK_1_NAME = "task-1";
    private static final String TASK_1_COMMAND = "migrate-db.sh";
    private static final String TASK_NAME_KEY = "name";
    private static final String TASK_COMMAND_KEY = "command";

    // Application and service names
    private static final String APP_1_NAME = "app-1";
    private static final String APP_2_NAME = "app-2";
    private static final String APP_3_NAME = "app-3";
    private static final String SERVICE_1_NAME = "service-1";
    private static final String SERVICE_2_NAME = "service-2";
    private static final String SERVICE_3_NAME = "service-3";

    // Service key names
    private static final String SERVICE_KEY_1_NAME = "service-key-1";
    private static final String SERVICE_KEY_2_NAME = "service-key-2";

    // Extension descriptor configurations
    private static final String TEST_EXTENSION_ID = "test-extension";
    private static final String TEST_PARAMETER_KEY = "test-parameter";

    // API configurations
    private static final String PROVIDED_DEPENDENCY_MY_API = "my-api";
    private static final String API_URL_KEY = "url";
    private static final String API_URL_VALUE = "https://api.example.com";

    // Parameter values
    private static final String NEW_PARAM_KEY = "new-param";
    private static final String NEW_PARAM_VALUE = "new-value";
    private static final String SYSLOG_DRAIN_URL_VALUE = "syslog://logs.example.com:514";

    private FlowableProcessTestDataUtils() {
    }

    public record UpdateScenario(FlowableProcessTestData initial, FlowableProcessTestData updated) {
    }

    public static FlowableProcessTestData predefinedScenario() {
        return baseBuilder(DEFAULT_MTA_ID, DEFAULT_MTA_VERSION).keepFiles(true)
                                                               .modulesCount(MODULES_COUNT)
                                                               .bigDescriptor(createBigDescriptor(DEFAULT_MTA_ID, DEFAULT_MTA_VERSION))
                                                               .routes(createRoutes(ROUTE_1_EXAMPLE_COM, ROUTE_2_EXAMPLE_COM,
                                                                                    ROUTE_3_EXAMPLE_COM))
                                                               .stepPhase(StepPhase.DONE)
                                                               .appActions(
                                                                   List.of(ApplicationStateAction.STAGE, ApplicationStateAction.START))
                                                               .build();
    }

    public static UpdateScenario updateScenario() {
        DeploymentDescriptor bigDescriptorV1 = createBigDescriptor(UPDATE_SCENARIO_MTA_ID, DEFAULT_MTA_VERSION);
        DeploymentDescriptor bigDescriptorV2 = DeploymentDescriptor.copyOf(
                                                                       createBigDescriptor(UPDATE_SCENARIO_MTA_ID, UPDATE_SCENARIO_VERSION_V2))
                                                                   .setParameters(Map.of(NEW_PARAM_KEY, NEW_PARAM_VALUE));
        FlowableProcessTestData initial = baseBuilder(UPDATE_SCENARIO_MTA_ID, DEFAULT_MTA_VERSION).deleteServices(true)
                                                                                                  .bigDescriptor(bigDescriptorV1)
                                                                                                  .deployedMta(
                                                                                                      createDeployed(UPDATE_SCENARIO_MTA_ID,
                                                                                                                     DEFAULT_MTA_VERSION))
                                                                                                  .routes(
                                                                                                      createRoutes(ROUTE_1_IDLE_EXAMPLE_COM,
                                                                                                                   ROUTE_2_IDLE_EXAMPLE_COM))
                                                                                                  .stepPhase(
                                                                                                      org.cloudfoundry.multiapps.controller.process.steps.StepPhase.POLL)
                                                                                                  .appActions(
                                                                                                      List.of(ApplicationStateAction.STAGE,
                                                                                                              ApplicationStateAction.START))
                                                                                                  .build();
        FlowableProcessTestData updated = baseBuilder(UPDATE_SCENARIO_MTA_ID, UPDATE_SCENARIO_VERSION_V2).deleteServices(true)
                                                                                                         .smallDescriptor(
                                                                                                             createSmallDescriptor(
                                                                                                                 UPDATE_SCENARIO_MTA_ID,
                                                                                                                 UPDATE_SCENARIO_VERSION_V2))
                                                                                                         .bigDescriptor(bigDescriptorV2)
                                                                                                         .deployedMta(createDeployed(
                                                                                                             UPDATE_SCENARIO_MTA_ID,
                                                                                                             UPDATE_SCENARIO_VERSION_V2))
                                                                                                         .routes(createRoutes(
                                                                                                             ROUTE_1_EXAMPLE_COM,
                                                                                                             ROUTE_2_EXAMPLE_COM,
                                                                                                             ROUTE_3_EXAMPLE_COM))
                                                                                                         .stepPhase(
                                                                                                             org.cloudfoundry.multiapps.controller.process.steps.StepPhase.DONE)
                                                                                                         .appActions(List.of(
                                                                                                             ApplicationStateAction.EXECUTE,
                                                                                                             ApplicationStateAction.STOP))
                                                                                                         .build();
        return new UpdateScenario(initial, updated);
    }

    public static FlowableProcessTestData insideStepScenario() {
        return baseBuilder(UPDATE_SCENARIO_MTA_ID, UPDATE_SCENARIO_VERSION_V2).deleteServices(true)
                                                                              .smallDescriptor(createSmallDescriptor(UPDATE_SCENARIO_MTA_ID,
                                                                                                                     UPDATE_SCENARIO_VERSION_V2))
                                                                              .bigDescriptor(createBigDescriptor(UPDATE_SCENARIO_MTA_ID,
                                                                                                                 UPDATE_SCENARIO_VERSION_V2))
                                                                              .deployedMta(createDeployed(UPDATE_SCENARIO_MTA_ID,
                                                                                                          UPDATE_SCENARIO_VERSION_V2))
                                                                              .routes(createRoutes(ROUTE_1_EXAMPLE_COM, ROUTE_2_EXAMPLE_COM,
                                                                                                   ROUTE_3_EXAMPLE_COM))
                                                                              .stepPhase(StepPhase.DONE)
                                                                              .appActions(List.of(ApplicationStateAction.STAGE,
                                                                                                  ApplicationStateAction.START))
                                                                              .build();
    }

    private static ImmutableFlowableProcessTestData.Builder baseBuilder(String mtaId, String mtaVersion) {
        return ImmutableFlowableProcessTestData.builder()
                                               .correlationId(UUID.randomUUID()
                                                                  .toString())
                                               .spaceGuid(UUID.randomUUID()
                                                              .toString())
                                               .orgGuid(UUID.randomUUID()
                                                            .toString())
                                               .username(TEST_USER_EMAIL)
                                               .userGuid(UUID.randomUUID()
                                                             .toString())
                                               .mtaId(mtaId)
                                               .mtaVersion(mtaVersion)
                                               .appsStageTimeout(Duration.ofMinutes(15))
                                               .smallDescriptor(createSmallDescriptor(mtaId, mtaVersion))
                                               .deployedMta(createDeployed(mtaId, mtaVersion))
                                               .stepPhase(StepPhase.DONE)
                                               .serviceKeys(createServiceKeys())
                                               .modulesForDeployment(List.of(MODULE_1_NAME, MODULE_2_NAME, MODULE_3_NAME))
                                               .extensionDescriptors(createExtensionDescriptorsWithNullValues(mtaId));
    }

    private static DeploymentDescriptor createSmallDescriptor(String id, String mtaVersion) {
        return DeploymentDescriptor.createV3()
                                   .setId(id)
                                   .setVersion(mtaVersion)
                                   .setModules(List.of(Module.createV3()
                                                             .setName(MODULE_1_NAME)
                                                             .setType(MODULE_TYPE_JAVASCRIPT)
                                                             .setParameters(Map.of(SupportedParameters.MEMORY, MEMORY_512M,
                                                                                   SupportedParameters.DISK_QUOTA, DISK_QUOTA_256M,
                                                                                   SupportedParameters.ROUTES,
                                                                                   List.of(SupportedParameters.ROUTE,
                                                                                           MODULE_1_ROUTE_EXAMPLE_COM)))
                                                             .setRequiredDependencies(List.of(RequiredDependency.createV3()
                                                                                                                .setName(
                                                                                                                    RESOURCE_DB_NAME)))))
                                   .setResources(List.of(Resource.createV3()
                                                                 .setName(RESOURCE_DB_NAME)
                                                                 .setType(SERVICE_TYPE_MANAGED)
                                                                 .setParameters(Map.of(SupportedParameters.SERVICE, TEST_DB_SERVICE,
                                                                                       SupportedParameters.SERVICE_PLAN,
                                                                                       SERVICE_PLAN_FREE))));
    }

    private static DeploymentDescriptor createBigDescriptor(String id, String mtaVersion) {
        return DeploymentDescriptor.createV3()
                                   .setId(id)
                                   .setVersion(mtaVersion)
                                   .setParameters(Map.of(SupportedParameters.ENABLE_PARALLEL_DEPLOYMENTS, true))
                                   .setModules(List.of(Module.createV3()
                                                             .setName(MODULE_1_NAME)
                                                             .setType(MODULE_TYPE_JAVASCRIPT)
                                                             .setParameters(Map.of(SupportedParameters.MEMORY, MEMORY_512M,
                                                                                   SupportedParameters.DISK_QUOTA, DISK_QUOTA_256M,
                                                                                   SupportedParameters.ROUTES,
                                                                                   List.of(SupportedParameters.ROUTE,
                                                                                           MODULE_1_ROUTE_EXAMPLE_COM),
                                                                                   SupportedParameters.TASKS, List.of(
                                                                     Map.of(TASK_NAME_KEY, TASK_1_NAME, TASK_COMMAND_KEY, TASK_1_COMMAND))))
                                                             .setRequiredDependencies(List.of(RequiredDependency.createV3()
                                                                                                                .setName(RESOURCE_DB_NAME),
                                                                                              RequiredDependency.createV3()
                                                                                                                .setName(
                                                                                                                    RESOURCE_APPLICATION_LOGS_NAME))),
                                                       Module.createV3()
                                                             .setName(MODULE_2_NAME)
                                                             .setType(MODULE_TYPE_JAVA)
                                                             .setParameters(Map.of(SupportedParameters.MEMORY, MEMORY_1G,
                                                                                   SupportedParameters.DISK_QUOTA, DISK_QUOTA_4096M,
                                                                                   SupportedParameters.INSTANCES, INSTANCES_COUNT_2,
                                                                                   SupportedParameters.ROUTES,
                                                                                   List.of(SupportedParameters.ROUTE,
                                                                                           MODULE_2_ROUTE_EXAMPLE_COM)))
                                                             .setRequiredDependencies(List.of(RequiredDependency.createV3()
                                                                                                                .setName(RESOURCE_DB_NAME),
                                                                                              RequiredDependency.createV3()
                                                                                                                .setName(
                                                                                                                    RESOURCE_CACHE_NAME),
                                                                                              RequiredDependency.createV3()
                                                                                                                .setName(
                                                                                                                    RESOURCE_AUTOSCALER_NAME),
                                                                                              RequiredDependency.createV3()
                                                                                                                .setName(
                                                                                                                    RESOURCE_APPLICATION_LOGS_NAME))),
                                                       Module.createV3()
                                                             .setName(MODULE_3_NAME)
                                                             .setType(MODULE_TYPE_JAVASCRIPT)
                                                             .setParameters(
                                                                 Map.of(SupportedParameters.MEMORY, MEMORY_512M,
                                                                        SupportedParameters.DISK_QUOTA,
                                                                        DISK_QUOTA_256M, SupportedParameters.ROUTES,
                                                                        List.of(SupportedParameters.ROUTE, MODULE_3_ROUTE_EXAMPLE_COM)))
                                                             .setRequiredDependencies(List.of(RequiredDependency.createV3()
                                                                                                                .setName(RESOURCE_DB_NAME),
                                                                                              RequiredDependency.createV3()
                                                                                                                .setName(
                                                                                                                    RESOURCE_CACHE_NAME),
                                                                                              RequiredDependency.createV3()
                                                                                                                .setName(
                                                                                                                    RESOURCE_AUTOSCALER_NAME),
                                                                                              RequiredDependency.createV3()
                                                                                                                .setName(
                                                                                                                    RESOURCE_APPLICATION_LOGS_NAME)))
                                                             .setProvidedDependencies(List.of(ProvidedDependency.createV3()
                                                                                                                .setName(
                                                                                                                    PROVIDED_DEPENDENCY_MY_API)
                                                                                                                .setProperties(
                                                                                                                    Map.of(API_URL_KEY,
                                                                                                                           API_URL_VALUE))))))
                                   .setResources(List.of(Resource.createV3()
                                                                 .setName(RESOURCE_DB_NAME)
                                                                 .setType(SERVICE_TYPE_MANAGED)
                                                                 .setParameters(Map.of(SupportedParameters.SERVICE, TEST_DB_SERVICE,
                                                                                       SupportedParameters.SERVICE_PLAN,
                                                                                       SERVICE_PLAN_FREE)),
                                                         Resource.createV3()
                                                                 .setName(RESOURCE_CACHE_NAME)
                                                                 .setType(SERVICE_TYPE_MANAGED)
                                                                 .setParameters(Map.of(SupportedParameters.SERVICE, TEST_CACHE_SERVICE,
                                                                                       SupportedParameters.SERVICE_PLAN,
                                                                                       SERVICE_PLAN_FREE)),
                                                         Resource.createV3()
                                                                 .setName(RESOURCE_AUTOSCALER_NAME)
                                                                 .setType(SERVICE_TYPE_MANAGED)
                                                                 .setParameters(Map.of(SupportedParameters.SERVICE, APP_AUTOSCALER_SERVICE,
                                                                                       SupportedParameters.SERVICE_PLAN,
                                                                                       SERVICE_PLAN_DEFAULT)),
                                                         Resource.createV3()
                                                                 .setName(RESOURCE_APPLICATION_LOGS_NAME)
                                                                 .setType(SERVICE_TYPE_USER_PROVIDED)
                                                                 .setParameters(Map.of(SupportedParameters.SYSLOG_DRAIN_URL,
                                                                                       SYSLOG_DRAIN_URL_VALUE))));
    }

    private static DeployedMta createDeployed(String id, String mtaVersion) {
        List<DeployedMtaApplication> apps = Stream.of(APP_1_NAME, APP_2_NAME, APP_3_NAME)
                                                  .map(app -> ImmutableDeployedMtaApplication.builder()
                                                                                             .name(app)
                                                                                             .moduleName(app)
                                                                                             .build())
                                                  .collect(Collectors.toList());
        List<DeployedMtaService> services = Stream.of(SERVICE_1_NAME, SERVICE_2_NAME, SERVICE_3_NAME)
                                                  .map(service -> ImmutableDeployedMtaService.builder()
                                                                                             .name(service)
                                                                                             .build())
                                                  .collect(Collectors.toList());
        return ImmutableDeployedMta.builder()
                                   .metadata(ImmutableMtaMetadata.builder()
                                                                 .id(id)
                                                                 .version(Version.parseVersion(mtaVersion))
                                                                 .build())
                                   .applications(apps)
                                   .services(services)
                                   .build();
    }

    private static List<CloudRoute> createRoutes(String... routes) {
        return Stream.of(routes)
                     .map(route -> new ApplicationURI(route, false, null).toCloudRoute())
                     .collect(Collectors.toList());
    }

    private static List<CloudServiceKey> createServiceKeys() {
        return List.of(ImmutableCloudServiceKey.builder()
                                               .name(SERVICE_KEY_1_NAME)
                                               .build(), ImmutableCloudServiceKey.builder()
                                                                                 .name(SERVICE_KEY_2_NAME)
                                                                                 .build());
    }

    private static List<ExtensionDescriptor> createExtensionDescriptorsWithNullValues(String mtaId) {
        Map<String, Object> moduleParams = new HashMap<>();
        moduleParams.put(SupportedParameters.MEMORY, null);
        moduleParams.put(SupportedParameters.DISK_QUOTA, DISK_QUOTA_256M);
        moduleParams.put(SupportedParameters.ROUTES, List.of(SupportedParameters.ROUTE, MODULE_1_ROUTE_EXAMPLE_COM));
        Map<String, Object> resourceParams = new HashMap<>();
        resourceParams.put(TEST_PARAMETER_KEY, null);
        return List.of(ExtensionDescriptor.createV3()
                                          .setId(TEST_EXTENSION_ID)
                                          .setParentId(mtaId)
                                          .setModules(List.of(ExtensionModule.createV3()
                                                                             .setName(MODULE_1_NAME)
                                                                             .setParameters(moduleParams)))
                                          .setResources(List.of(ExtensionResource.createV3()
                                                                                 .setName(RESOURCE_DB_NAME)
                                                                                 .setParameters(resourceParams))));
    }

}
