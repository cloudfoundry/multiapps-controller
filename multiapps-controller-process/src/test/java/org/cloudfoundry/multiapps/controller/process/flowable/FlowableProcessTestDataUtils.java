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
    private FlowableProcessTestDataUtils() {
    }

    public record UpdateScenario(FlowableProcessTestData initial, FlowableProcessTestData updated) {
    }

    public static FlowableProcessTestData predefinedScenario() {
        String mtaId = "test-mta";
        String mtaVersion = "1.0.0";
        return baseBuilder(mtaId, mtaVersion).keepFiles(true)
                                             .modulesCount(5)
                                             .bigDescriptor(createBigDescriptor(mtaId, mtaVersion))
                                             .routes(
                                                 createRoutes("route-1.example.com/foo-bar", "route-2.example.com", "route-3.example.com"))
                                             .stepPhase(StepPhase.DONE)
                                             .appActions(List.of(ApplicationStateAction.STAGE, ApplicationStateAction.START))
                                             .build();
    }

    public static UpdateScenario updateScenario() {
        String id = "test-mta-2";
        DeploymentDescriptor bigDescriptorV1 = createBigDescriptor(id, "1.0.0");
        DeploymentDescriptor bigDescriptorV2 = DeploymentDescriptor.copyOf(createBigDescriptor(id, "2.0.0"))
                                                                   .setParameters(Map.of("new-param", "new-value"));
        FlowableProcessTestData initial = baseBuilder(id, "1.0.0").deleteServices(true)
                                                                  .bigDescriptor(bigDescriptorV1)
                                                                  .deployedMta(createDeployed(id, "1.0.0"))
                                                                  .routes(
                                                                      createRoutes("route-1-idle.example.com/bar-foo",
                                                                                   "route-2-idle.example.com"))
                                                                  .stepPhase(
                                                                      org.cloudfoundry.multiapps.controller.process.steps.StepPhase.POLL)
                                                                  .appActions(
                                                                      List.of(ApplicationStateAction.STAGE, ApplicationStateAction.START))
                                                                  .build();
        FlowableProcessTestData updated = baseBuilder(id, "2.0.0").deleteServices(true)
                                                                  .smallDescriptor(createSmallDescriptor(id, "2.0.0"))
                                                                  .bigDescriptor(bigDescriptorV2)
                                                                  .deployedMta(createDeployed(id, "2.0.0"))
                                                                  .routes(createRoutes("route-1.example.com/foo-bar", "route-2.example.com",
                                                                                       "route-3.example.com"))
                                                                  .stepPhase(
                                                                      org.cloudfoundry.multiapps.controller.process.steps.StepPhase.DONE)
                                                                  .appActions(
                                                                      List.of(ApplicationStateAction.EXECUTE, ApplicationStateAction.STOP))
                                                                  .build();
        return new UpdateScenario(initial, updated);
    }

    public static FlowableProcessTestData insideStepScenario() {
        String id = "test-mta-2";
        String mtaVersion = "2.0.0";
        return baseBuilder(id, mtaVersion).deleteServices(true)
                                          .smallDescriptor(createSmallDescriptor(id, mtaVersion))
                                          .bigDescriptor(createBigDescriptor(id, mtaVersion))
                                          .deployedMta(createDeployed(id, mtaVersion))
                                          .routes(createRoutes("route-1.example.com/foo-bar", "route-2.example.com", "route-3.example.com"))
                                          .stepPhase(org.cloudfoundry.multiapps.controller.process.steps.StepPhase.DONE)
                                          .appActions(List.of(ApplicationStateAction.STAGE, ApplicationStateAction.START))
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
                                               .username("test-user@example.com")
                                               .userGuid(UUID.randomUUID()
                                                             .toString())
                                               .mtaId(mtaId)
                                               .mtaVersion(mtaVersion)
                                               .appsStageTimeout(Duration.ofMinutes(15))
                                               .smallDescriptor(createSmallDescriptor(mtaId, mtaVersion))
                                               .deployedMta(createDeployed(mtaId, mtaVersion))
                                               .stepPhase(StepPhase.DONE)
                                               .serviceKeys(createServiceKeys())
                                               .modulesForDeployment(List.of("module-1", "module-2", "module-3"))
                                               .extensionDescriptors(createExtensionDescriptorsWithNullValues(mtaId));
    }

    private static DeploymentDescriptor createSmallDescriptor(String id, String mtaVersion) {
        return DeploymentDescriptor.createV3()
                                   .setId(id)
                                   .setVersion(mtaVersion)
                                   .setModules(List.of(Module.createV3()
                                                             .setName("module-1")
                                                             .setType("javascript")
                                                             .setParameters(
                                                                 Map.of(SupportedParameters.MEMORY, "512M", SupportedParameters.DISK_QUOTA,
                                                                        "256M", SupportedParameters.ROUTES,
                                                                        List.of(SupportedParameters.ROUTE, "module-1-route.example.com")))
                                                             .setRequiredDependencies(List.of(RequiredDependency.createV3()
                                                                                                                .setName("db")))))
                                   .setResources(List.of(Resource.createV3()
                                                                 .setName("db")
                                                                 .setType("org.cloudfoundry.managed-service")
                                                                 .setParameters(Map.of(SupportedParameters.SERVICE, "test-db-service",
                                                                                       SupportedParameters.SERVICE_PLAN, "free"))));
    }

    private static DeploymentDescriptor createBigDescriptor(String id, String mtaVersion) {
        return DeploymentDescriptor.createV3()
                                   .setId(id)
                                   .setVersion(mtaVersion)
                                   .setParameters(Map.of(SupportedParameters.ENABLE_PARALLEL_DEPLOYMENTS, true))
                                   .setModules(List.of(Module.createV3()
                                                             .setName("module-1")
                                                             .setType("javascript")
                                                             .setParameters(
                                                                 Map.of(SupportedParameters.MEMORY, "512M", SupportedParameters.DISK_QUOTA,
                                                                        "256M", SupportedParameters.ROUTES,
                                                                        List.of(SupportedParameters.ROUTE, "module-1-route.example.com"),
                                                                        SupportedParameters.TASKS,
                                                                        List.of(Map.of("name", "task-1", "command", "migrate-db.sh"))))
                                                             .setRequiredDependencies(List.of(RequiredDependency.createV3()
                                                                                                                .setName("db"),
                                                                                              RequiredDependency.createV3()
                                                                                                                .setName(
                                                                                                                    "application-logs"))),
                                                       Module.createV3()
                                                             .setName("module-2")
                                                             .setType("java")
                                                             .setParameters(
                                                                 Map.of(SupportedParameters.MEMORY, "1G", SupportedParameters.DISK_QUOTA,
                                                                        "4096M", SupportedParameters.INSTANCES, 2,
                                                                        SupportedParameters.ROUTES,
                                                                        List.of(SupportedParameters.ROUTE, "module-2-route.example.com")))
                                                             .setRequiredDependencies(List.of(RequiredDependency.createV3()
                                                                                                                .setName("db"),
                                                                                              RequiredDependency.createV3()
                                                                                                                .setName("cache"),
                                                                                              RequiredDependency.createV3()
                                                                                                                .setName("autoscaler"),
                                                                                              RequiredDependency.createV3()
                                                                                                                .setName(
                                                                                                                    "application-logs"))),
                                                       Module.createV3()
                                                             .setName("module-3")
                                                             .setType("javascript")
                                                             .setParameters(
                                                                 Map.of(SupportedParameters.MEMORY, "512M", SupportedParameters.DISK_QUOTA,
                                                                        "256M", SupportedParameters.ROUTES,
                                                                        List.of(SupportedParameters.ROUTE, "module-3-route.example.com")))
                                                             .setRequiredDependencies(List.of(RequiredDependency.createV3()
                                                                                                                .setName("db"),
                                                                                              RequiredDependency.createV3()
                                                                                                                .setName("cache"),
                                                                                              RequiredDependency.createV3()
                                                                                                                .setName("autoscaler"),
                                                                                              RequiredDependency.createV3()
                                                                                                                .setName(
                                                                                                                    "application-logs")))
                                                             .setProvidedDependencies(List.of(ProvidedDependency.createV3()
                                                                                                                .setName("my-api")
                                                                                                                .setProperties(Map.of("url",
                                                                                                                                      "https://api.example.com"))))))
                                   .setResources(List.of(Resource.createV3()
                                                                 .setName("db")
                                                                 .setType("org.cloudfoundry.managed-service")
                                                                 .setParameters(Map.of(SupportedParameters.SERVICE, "test-db-service",
                                                                                       SupportedParameters.SERVICE_PLAN, "free")),
                                                         Resource.createV3()
                                                                 .setName("cache")
                                                                 .setType("org.cloudfoundry.managed-service")
                                                                 .setParameters(Map.of(SupportedParameters.SERVICE, "test-cache-service",
                                                                                       SupportedParameters.SERVICE_PLAN, "free")),
                                                         Resource.createV3()
                                                                 .setName("autoscaler")
                                                                 .setType("org.cloudfoundry.managed-service")
                                                                 .setParameters(Map.of(SupportedParameters.SERVICE, "app-autoscaler",
                                                                                       SupportedParameters.SERVICE_PLAN, "default")),
                                                         Resource.createV3()
                                                                 .setName("application-logs")
                                                                 .setType("org.cloudfoundry.user-provided-service")
                                                                 .setParameters(Map.of(SupportedParameters.SYSLOG_DRAIN_URL,
                                                                                       "syslog://logs.example.com:514"))));
    }

    private static DeployedMta createDeployed(String id, String mtaVersion) {
        List<DeployedMtaApplication> apps = Stream.of("app-1", "app-2", "app-3")
                                                  .map(app -> ImmutableDeployedMtaApplication.builder()
                                                                                             .name(app)
                                                                                             .moduleName(app)
                                                                                             .build())
                                                  .collect(Collectors.toList());
        List<DeployedMtaService> services = Stream.of("service-1", "service-2", "service-3")
                                                  .map(service -> ImmutableDeployedMtaService.builder()
                                                                                             .name(service)
                                                                                             .build())
                                                  .collect(Collectors.toList());
        return ImmutableDeployedMta.builder()
                                   .metadata(ImmutableMtaMetadata.builder()
                                                                 .id(id)
                                                                 .version(
                                                                     Version.parseVersion(
                                                                         mtaVersion))
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
                                               .name("service-key-1")
                                               .build(), ImmutableCloudServiceKey.builder()
                                                                                 .name("service-key-2")
                                                                                 .build());
    }

    private static List<ExtensionDescriptor> createExtensionDescriptorsWithNullValues(String mtaId) {
        Map<String, Object> moduleParams = new HashMap<>();
        moduleParams.put(SupportedParameters.MEMORY, null);
        moduleParams.put(SupportedParameters.DISK_QUOTA, "256M");
        moduleParams.put(SupportedParameters.ROUTES, List.of(SupportedParameters.ROUTE, "module-1-route.example.com"));
        Map<String, Object> resourceParams = new HashMap<>();
        resourceParams.put("test-parameter", null);
        return List.of(ExtensionDescriptor.createV3()
                                          .setId("test-extension")
                                          .setParentId(mtaId)
                                          .setModules(List.of(ExtensionModule.createV3()
                                                                             .setName("module-1")
                                                                             .setParameters(moduleParams)))
                                          .setResources(List.of(ExtensionResource.createV3()
                                                                                 .setName("db")
                                                                                 .setParameters(resourceParams))));
    }

}
