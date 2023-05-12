package org.cloudfoundry.multiapps.controller.process.variables;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.cf.DeploymentMode;
import org.cloudfoundry.multiapps.controller.core.cf.apps.ApplicationStateAction;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveElements;
import org.cloudfoundry.multiapps.controller.core.model.ApplicationColor;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaServiceKey;
import org.cloudfoundry.multiapps.controller.core.model.ErrorType;
import org.cloudfoundry.multiapps.controller.core.model.Phase;
import org.cloudfoundry.multiapps.controller.core.model.SubprocessPhase;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.process.DeployStrategy;
import org.cloudfoundry.multiapps.controller.process.steps.StepPhase;
import org.cloudfoundry.multiapps.controller.process.util.ServiceAction;
import org.cloudfoundry.multiapps.controller.process.util.ServiceDeletionActions;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionDescriptor;
import org.cloudfoundry.multiapps.mta.model.Hook;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.model.VersionRule;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudPackage;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBroker;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.CloudTask;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;

public interface Variables {

    Variable<String> CORRELATION_ID = ImmutableSimpleVariable.<String> builder()
                                                             .name("correlationId")
                                                             .build();
    Variable<String> ORGANIZATION_NAME = ImmutableSimpleVariable.<String> builder()
                                                                .name("org")
                                                                .build();
    Variable<String> ORGANIZATION_GUID = ImmutableSimpleVariable.<String> builder()
                                                                .name("orgId")
                                                                .build();
    Variable<String> SPACE_NAME = ImmutableSimpleVariable.<String> builder()
                                                         .name("space")
                                                         .build();
    Variable<String> SPACE_GUID = ImmutableSimpleVariable.<String> builder()
                                                         .name(org.cloudfoundry.multiapps.controller.persistence.Constants.VARIABLE_NAME_SPACE_ID)
                                                         .build();
    Variable<String> SUBPROCESS_ID = ImmutableSimpleVariable.<String> builder()
                                                            .name("subProcessId")
                                                            .build();
    Variable<String> SERVICE_ID = ImmutableSimpleVariable.<String> builder()
                                                         .name(org.cloudfoundry.multiapps.controller.persistence.Constants.VARIABLE_NAME_SERVICE_ID)
                                                         .build();
    Variable<String> TASK_ID = ImmutableSimpleVariable.<String> builder()
                                                      .name("__TASK_ID")
                                                      .build();
    Variable<String> TIMESTAMP = ImmutableSimpleVariable.<String> builder()
                                                        .name("timestamp")
                                                        .build();
    Variable<String> SERVICE_TO_PROCESS_NAME = ImmutableSimpleVariable.<String> builder()
                                                                      .name("serviceToProcessName")
                                                                      .build();
    Variable<String> APP_ARCHIVE_ID = ImmutableSimpleVariable.<String> builder()
                                                             .name("appArchiveId")
                                                             .build();
    Variable<String> EXT_DESCRIPTOR_FILE_ID = ImmutableSimpleVariable.<String> builder()
                                                                     .name("mtaExtDescriptorId")
                                                                     .build();
    Variable<String> MTA_ID = ImmutableSimpleVariable.<String> builder()
                                                     .name("mtaId")
                                                     .build();
    Variable<String> MTA_NAMESPACE = ImmutableSimpleVariable.<String> builder()
                                                            .name("namespace")
                                                            .build();
    Variable<Boolean> APPLY_NAMESPACE = ImmutableSimpleVariable.<Boolean> builder()
                                                               .name("applyNamespace")
                                                               .defaultValue(false)
                                                               .build();
    Variable<String> CTS_PROCESS_ID = ImmutableSimpleVariable.<String> builder()
                                                             .name("ctsProcessId")
                                                             .build();
    Variable<String> DEPLOY_URI = ImmutableSimpleVariable.<String> builder()
                                                         .name("deployUri")
                                                         .build();
    Variable<String> CTS_USERNAME = ImmutableSimpleVariable.<String> builder()
                                                           .name("userId")
                                                           .build();
    Variable<String> CTS_PASSWORD = ImmutableSimpleVariable.<String> builder()
                                                           .name("password")
                                                           .build();
    Variable<String> TRANSFER_TYPE = ImmutableSimpleVariable.<String> builder()
                                                            .name("transferType")
                                                            .build();
    Variable<String> APPLICATION_TYPE = ImmutableSimpleVariable.<String> builder()
                                                               .name("applType")
                                                               .build();
    Variable<String> USER = ImmutableSimpleVariable.<String> builder()
                                                   .name("user")
                                                   .build();
    Variable<String> SERVICE_OFFERING = ImmutableSimpleVariable.<String> builder()
                                                               .name("serviceOffering")
                                                               .build();
    Variable<String> INDEX_VARIABLE_NAME = ImmutableSimpleVariable.<String> builder()
                                                                  .name("indexVariableName")
                                                                  .build();
    Variable<String> STEP_EXECUTION = ImmutableSimpleVariable.<String> builder()
                                                             .name("StepExecution")
                                                             .build();
    Variable<Boolean> APP_CONTENT_CHANGED = ImmutableSimpleVariable.<Boolean> builder()
                                                                   .name("appContentChanged")
                                                                   .defaultValue(false)
                                                                   .build();
    Variable<Boolean> START_APPS = ImmutableSimpleVariable.<Boolean> builder()
                                                          .name("startApps")
                                                          .defaultValue(true)
                                                          .build();
    Variable<Integer> MTA_MAJOR_SCHEMA_VERSION = ImmutableSimpleVariable.<Integer> builder()
                                                                        .name("mtaMajorSchemaVersion")
                                                                        .build();
    Variable<Duration> START_TIMEOUT = ImmutableSimpleVariable.<Duration> builder()
                                                              .name("startTimeout")
                                                              .defaultValue(Duration.ofHours(1))
                                                              .build();
    Variable<Integer> UPDATED_SERVICE_BROKER_SUBSCRIBERS_COUNT = ImmutableSimpleVariable.<Integer> builder()
                                                                                        .name("updatedServiceBrokerSubscribersCount")
                                                                                        .build();
    Variable<Integer> UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX = ImmutableSimpleVariable.<Integer> builder()
                                                                                        .name("updatedServiceBrokerSubscribersIndex")
                                                                                        .build();
    Variable<Integer> MODULES_COUNT = ImmutableSimpleVariable.<Integer> builder()
                                                             .name("modulesCount")
                                                             .build();
    Variable<Integer> MODULES_INDEX = ImmutableSimpleVariable.<Integer> builder()
                                                             .name("modulesIndex")
                                                             .build();
    Variable<Integer> MTARS_COUNT = ImmutableSimpleVariable.<Integer> builder()
                                                           .name("mtarsCount")
                                                           .build();
    Variable<Integer> MTARS_INDEX = ImmutableSimpleVariable.<Integer> builder()
                                                           .name("mtarsIndex")
                                                           .build();
    Variable<Integer> TASKS_COUNT = ImmutableSimpleVariable.<Integer> builder()
                                                           .name("tasksCount")
                                                           .build();
    Variable<Integer> TASKS_INDEX = ImmutableSimpleVariable.<Integer> builder()
                                                           .name("tasksIndex")
                                                           .build();
    Variable<Integer> SERVICES_TO_CREATE_COUNT = ImmutableSimpleVariable.<Integer> builder()
                                                                        .name("servicesToCreateCount")
                                                                        .build();
    Variable<Integer> ASYNC_STEP_EXECUTION_INDEX = ImmutableSimpleVariable.<Integer> builder()
                                                                          .name("asyncStepExecutionIndex")
                                                                          .build();
    Variable<Long> START_TIME = ImmutableSimpleVariable.<Long> builder()
                                                       .name("startTime")
                                                       .build();
    Variable<Boolean> SKIP_MANAGE_SERVICE_BROKER = ImmutableSimpleVariable.<Boolean> builder()
                                                                          .name("skipManageServiceBroker")
                                                                          .build();
    Variable<Boolean> VERIFY_ARCHIVE_SIGNATURE = ImmutableSimpleVariable.<Boolean> builder()
                                                                        .name("verifyArchiveSignature")
                                                                        .defaultValue(false)
                                                                        .build();
    Variable<Boolean> DELETE_IDLE_URIS = ImmutableSimpleVariable.<Boolean> builder()
                                                                .name("deleteIdleUris")
                                                                .defaultValue(false)
                                                                .build();
    Variable<Boolean> USE_IDLE_URIS = ImmutableSimpleVariable.<Boolean> builder()
                                                             .name("useIdleUris")
                                                             .defaultValue(false)
                                                             .build();
    Variable<Boolean> IS_SERVICE_UPDATED = ImmutableSimpleVariable.<Boolean> builder()
                                                                  .name("isServiceUpdated")
                                                                  .defaultValue(false)
                                                                  .build();
    Variable<Boolean> SKIP_UPDATE_CONFIGURATION_ENTRIES = ImmutableSimpleVariable.<Boolean> builder()
                                                                                 .name("skipUpdateConfigurationEntries")
                                                                                 .defaultValue(false)
                                                                                 .build();
    Variable<Boolean> FAIL_ON_CRASHED = ImmutableSimpleVariable.<Boolean> builder()
                                                               .name("failOnCrashed")
                                                               .defaultValue(true)
                                                               .build();
    Variable<Boolean> USER_PROPERTIES_CHANGED = ImmutableSimpleVariable.<Boolean> builder()
                                                                       .name("vcapUserPropertiesChanged")
                                                                       .defaultValue(false)
                                                                       .build();
    Variable<Boolean> APP_NEEDS_RESTAGE = ImmutableSimpleVariable.<Boolean> builder()
                                                                 .name("appNeedsRestage")
                                                                 .defaultValue(false)
                                                                 .build();
    Variable<Boolean> VCAP_APP_PROPERTIES_CHANGED = ImmutableSimpleVariable.<Boolean> builder()
                                                                           .name("vcapAppPropertiesChanged")
                                                                           .defaultValue(false)
                                                                           .build();
    Variable<Boolean> VCAP_SERVICES_PROPERTIES_CHANGED = ImmutableSimpleVariable.<Boolean> builder()
                                                                                .name("vcapServicesPropertiesChanged")
                                                                                .defaultValue(false)
                                                                                .build();
    Variable<Boolean> SHOULD_SKIP_SERVICE_REBINDING = ImmutableSimpleVariable.<Boolean> builder()
                                                                             .name("shouldSkipServiceRebinding")
                                                                             .defaultValue(false)
                                                                             .build();
    Variable<Boolean> DELETE_SERVICES = ImmutableSimpleVariable.<Boolean> builder()
                                                               .name("deleteServices")
                                                               .defaultValue(false)
                                                               .build();
    Variable<Boolean> DELETE_SERVICE_KEYS = ImmutableSimpleVariable.<Boolean> builder()
                                                                   .name("deleteServiceKeys")
                                                                   .defaultValue(false)
                                                                   .build();
    Variable<Boolean> DELETE_SERVICE_BROKERS = ImmutableSimpleVariable.<Boolean> builder()
                                                                      .name("deleteServiceBrokers")
                                                                      .defaultValue(false)
                                                                      .build();
    Variable<Boolean> NO_START = ImmutableSimpleVariable.<Boolean> builder()
                                                        .name("noStart")
                                                        .defaultValue(false)
                                                        .build();
    Variable<Boolean> KEEP_FILES = ImmutableSimpleVariable.<Boolean> builder()
                                                          .name("keepFiles")
                                                          .defaultValue(false)
                                                          .build();
    Variable<Boolean> NO_CONFIRM = ImmutableSimpleVariable.<Boolean> builder()
                                                          .name("noConfirm")
                                                          .defaultValue(false)
                                                          .build();
    Variable<Boolean> NO_RESTART_SUBSCRIBED_APPS = ImmutableSimpleVariable.<Boolean> builder()
                                                                          .name("noRestartSubscribedApps")
                                                                          .defaultValue(false)
                                                                          .build();
    Variable<Boolean> NO_FAIL_ON_MISSING_PERMISSIONS = ImmutableSimpleVariable.<Boolean> builder()
                                                                              .name("noFailOnMissingPermissions")
                                                                              .defaultValue(false)
                                                                              .build();
    Variable<Boolean> ABORT_ON_ERROR = ImmutableSimpleVariable.<Boolean> builder()
                                                              .name("abortOnError")
                                                              .defaultValue(false)
                                                              .build();
    Variable<Boolean> KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY = ImmutableSimpleVariable.<Boolean> builder()
                                                                                    .name("keepOriginalAppNamesAfterDeploy")
                                                                                    .defaultValue(false)
                                                                                    .build();
    Variable<Boolean> SKIP_IDLE_START = ImmutableSimpleVariable.<Boolean> builder()
                                                               .name("skipIdleStart")
                                                               .defaultValue(false)
                                                               .build();
    Variable<Boolean> EXECUTE_ONE_OFF_TASKS = ImmutableSimpleVariable.<Boolean> builder()
                                                                     .name("executeOneOffTasks")
                                                                     .build();
    Variable<Boolean> SHOULD_UPLOAD_APPLICATION_CONTENT = ImmutableSimpleVariable.<Boolean> builder()
                                                                                 .name("shouldUploadApplicationContent")
                                                                                 .build();
    Variable<Boolean> REBUILD_APP_ENV = ImmutableSimpleVariable.<Boolean> builder()
                                                               .name("rebuildAppEnv")
                                                               .build();
    Variable<UUID> BUILD_GUID = ImmutableSimpleVariable.<UUID> builder()
                                                       .name("buildGuid")
                                                       .build();
    Variable<DeploymentDescriptor> DEPLOYMENT_DESCRIPTOR = ImmutableJsonStringVariable.<DeploymentDescriptor> builder()
                                                                                      .name("mtaDeploymentDescriptor")
                                                                                      .type(Variable.typeReference(DeploymentDescriptor.class))
                                                                                      .build();
    Variable<DeploymentDescriptor> DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS = ImmutableJsonStringVariable.<DeploymentDescriptor> builder()
                                                                                                             .name("mtaDeploymentDescriptorWithSystemParameters")
                                                                                                             .type(Variable.typeReference(DeploymentDescriptor.class))
                                                                                                             .build();
    Variable<DeploymentDescriptor> COMPLETE_DEPLOYMENT_DESCRIPTOR = ImmutableJsonStringVariable.<DeploymentDescriptor> builder()
                                                                                               .name("completeMtaDeploymentDescriptor")
                                                                                               .type(Variable.typeReference(DeploymentDescriptor.class))
                                                                                               .build();
    Variable<CloudApplicationExtended> APP_TO_PROCESS = ImmutableJsonStringVariable.<CloudApplicationExtended> builder()
                                                                                   .name("appToProcess")
                                                                                   .type(Variable.typeReference(CloudApplicationExtended.class))
                                                                                   .build();
    Variable<MtaArchiveElements> MTA_ARCHIVE_ELEMENTS = ImmutableJsonStringVariable.<MtaArchiveElements> builder()
                                                                                   .name("mtaArchiveElements")
                                                                                   .type(Variable.typeReference(MtaArchiveElements.class))
                                                                                   .defaultValue(new MtaArchiveElements())
                                                                                   .build();
    Variable<CloudServiceInstanceExtended> SERVICE_TO_PROCESS = ImmutableJsonStringVariable.<CloudServiceInstanceExtended> builder()
                                                                                           .name("serviceToProcess")
                                                                                           .type(Variable.typeReference(CloudServiceInstanceExtended.class))
                                                                                           .build();
    Variable<CloudPackage> CLOUD_PACKAGE = ImmutableJsonStringVariable.<CloudPackage> builder()
                                                                      .name("uploadedCloudPackage")
                                                                      .type(Variable.typeReference(CloudPackage.class))
                                                                      .build();
    Variable<Hook> HOOK_FOR_EXECUTION = ImmutableJsonStringVariable.<Hook> builder()
                                                                   .name("hookForExecution")
                                                                   .type(Variable.typeReference(Hook.class))
                                                                   .build();
    Variable<List<String>> APPS_TO_DEPLOY = ImmutableJsonBinaryVariable.<List<String>> builder()
                                                                       .name("appsToDeploy")
                                                                       .type(new TypeReference<>() {
                                                                       })
                                                                       .build();
    Variable<List<String>> APPS_TO_RENAME = ImmutableJsonBinaryVariable.<List<String>> builder()
                                                                       .name("appsToRename")
                                                                       .type(new TypeReference<>() {
                                                                       })
                                                                       .build();
    Variable<List<ConfigurationEntry>> CONFIGURATION_ENTRIES_TO_PUBLISH = ImmutableJsonBinaryVariable.<List<ConfigurationEntry>> builder()
                                                                                                     .name("configurationEntriesToPublish")
                                                                                                     .type(new TypeReference<>() {
                                                                                                     })
                                                                                                     .build();
    Variable<List<ConfigurationEntry>> DELETED_ENTRIES = ImmutableJsonBinaryVariable.<List<ConfigurationEntry>> builder()
                                                                                    .name("deletedEntries")
                                                                                    .type(new TypeReference<>() {
                                                                                    })
                                                                                    .defaultValue(Collections.emptyList())
                                                                                    .build();
    Variable<List<ConfigurationEntry>> PUBLISHED_ENTRIES = ImmutableJsonBinaryVariable.<List<ConfigurationEntry>> builder()
                                                                                      .name("publishedEntries")
                                                                                      .type(new TypeReference<>() {
                                                                                      })
                                                                                      .build();
    Variable<CloudServiceBroker> CREATED_OR_UPDATED_SERVICE_BROKER = ImmutableJsonBinaryVariable.<CloudServiceBroker> builder()
                                                                                                .name("createdOrUpdatedServiceBroker")
                                                                                                .type(Variable.typeReference(CloudServiceBroker.class))
                                                                                                .build();
    Variable<List<String>> CUSTOM_DOMAINS = ImmutableJsonBinaryVariable.<List<String>> builder()
                                                                       .name("customDomains")
                                                                       .type(new TypeReference<>() {
                                                                       })
                                                                       .build();
    Variable<DeployedMta> DEPLOYED_MTA = ImmutableJsonBinaryVariable.<DeployedMta> builder()
                                                                    .name("deployedMta")
                                                                    .type(Variable.typeReference(DeployedMta.class))
                                                                    .build();
    Variable<List<DeployedMtaServiceKey>> DEPLOYED_MTA_SERVICE_KEYS = ImmutableJsonBinaryVariable.<List<DeployedMtaServiceKey>> builder()
                                                                                                 .name("deployedMtaServiceKeys")
                                                                                                 .type(new TypeReference<>() {
                                                                                                 })
                                                                                                 .defaultValue(Collections.emptyList())
                                                                                                 .build();
    Variable<CloudApplication> EXISTING_APP = ImmutableJsonBinaryVariable.<CloudApplication> builder()
                                                                         .name("existingApp")
                                                                         .type(Variable.typeReference(CloudApplication.class))
                                                                         .build();
    Variable<Module> MODULE_TO_DEPLOY = ImmutableJsonBinaryVariable.<Module> builder()
                                                                   .name("moduleToDeploy")
                                                                   .type(Variable.typeReference(Module.class))
                                                                   .build();
    Variable<Set<String>> MTA_ARCHIVE_MODULES = ImmutableJsonBinaryVariable.<Set<String>> builder()
                                                                           .name("mtaArchiveModules")
                                                                           .type(new TypeReference<>() {
                                                                           })
                                                                           .build();
    Variable<Set<String>> MTA_MODULES = ImmutableJsonBinaryVariable.<Set<String>> builder()
                                                                   .name("mtaModules")
                                                                   .type(new TypeReference<>() {
                                                                   })
                                                                   .build();
    Variable<Map<String, Map<String, String>>> SERVICE_KEYS_CREDENTIALS_TO_INJECT = ImmutableJsonBinaryVariable.<Map<String, Map<String, String>>> builder()
                                                                                                               .name("serviceKeysCredentialsToInject")
                                                                                                               .type(new TypeReference<>() {
                                                                                                               })
                                                                                                               .build();
    Variable<Map<String, List<CloudServiceKey>>> SERVICE_KEYS_TO_CREATE = ImmutableJsonBinaryVariable.<Map<String, List<CloudServiceKey>>> builder()
                                                                                                     .name("serviceKeysToCreate")
                                                                                                     .type(new TypeReference<>() {
                                                                                                     })
                                                                                                     .build();
    //TODO: keep custom serializer only for one release, delete after
//    Variable<List<DeployedMtaServiceKey>> SERVICE_KEYS_TO_DELETE = ImmutableJsonStringListVariable.<DeployedMtaServiceKey> builder()
//                                                                                                  .name("serviceKeysToDelete")
//                                                                                                  .type(new TypeReference<>() {
//                                                                                                  })
//                                                                                                  .defaultValue(Collections.emptyList())
//                                                                                                  .build();
    Variable<List<DeployedMtaServiceKey>> SERVICE_KEYS_TO_DELETE = new JsonStringListVariable<>() {

        @Override
        public String getName() {
            return "serviceKeysToDelete";
        }

        @Override
        public List<DeployedMtaServiceKey> getDefaultValue() {
            return Collections.emptyList();
        }

        @Override
        public TypeReference<DeployedMtaServiceKey> getType() {
            return ServiceKeysToDeleteSerializationAdapter.SERVICE_KEY_ELEMENT_TYPE_REFERENCE;
        }
        
        @Override
        public Serializer<List<DeployedMtaServiceKey>> getSerializer() {
            return new ServiceKeysToDeleteSerializationAdapter();
        }
        
    };
    Variable<Map<String, List<CloudServiceKey>>> SERVICE_KEYS_FOR_CONTENT_DEPLOY = ImmutableJsonBinaryVariable.<Map<String, List<CloudServiceKey>>> builder()
                                                                                                              .name("serviceKeysForContentDeploy")
                                                                                                              .type(new TypeReference<>() {
                                                                                                              })
                                                                                                              .build();
    Variable<String> SERVICE_TO_DELETE = ImmutableSimpleVariable.<String> builder()
                                                                .name("serviceToDelete")
                                                                .build();
    Variable<List<String>> SERVICES_TO_DELETE = ImmutableSimpleVariable.<List<String>> builder()
                                                                       .name("servicesToDelete")
                                                                       .build();
    Variable<List<CloudServiceInstanceExtended>> SERVICES_TO_POLL = ImmutableJsonBinaryVariable.<List<CloudServiceInstanceExtended>> builder()
                                                                                               .name("servicesToPoll")
                                                                                               .type(new TypeReference<>() {
                                                                                               })
                                                                                               .build();
    Variable<CloudTask> STARTED_TASK = ImmutableJsonBinaryVariable.<CloudTask> builder()
                                                                  .name("startedTask")
                                                                  .type(Variable.typeReference(CloudTask.class))
                                                                  .build();
    Variable<List<ConfigurationSubscription>> SUBSCRIPTIONS_TO_CREATE = ImmutableJsonBinaryVariable.<List<ConfigurationSubscription>> builder()
                                                                                                   .name("subscriptionsToCreate")
                                                                                                   .type(new TypeReference<>() {
                                                                                                   })
                                                                                                   .build();
    Variable<List<ConfigurationSubscription>> SUBSCRIPTIONS_TO_DELETE = ImmutableJsonBinaryVariable.<List<ConfigurationSubscription>> builder()
                                                                                                   .name("subscriptionsToDelete")
                                                                                                   .type(new TypeReference<>() {
                                                                                                   })
                                                                                                   .build();
    Variable<List<CloudTask>> TASKS_TO_EXECUTE = ImmutableJsonBinaryVariable.<List<CloudTask>> builder()
                                                                            .name("tasksToExecute")
                                                                            .type(new TypeReference<>() {
                                                                            })
                                                                            .build();
    Variable<Map<String, ServiceOperation.Type>> TRIGGERED_SERVICE_OPERATIONS = ImmutableJsonBinaryVariable.<Map<String, ServiceOperation.Type>> builder()
                                                                                                           .name("triggeredServiceOperations")
                                                                                                           .type(new TypeReference<>() {
                                                                                                           })
                                                                                                           .build();
    Variable<List<CloudApplication>> UPDATED_SERVICE_BROKER_SUBSCRIBERS = ImmutableJsonBinaryVariable.<List<CloudApplication>> builder()
                                                                                                     .name("updatedServiceBrokerSubscribers")
                                                                                                     .type(new TypeReference<>() {
                                                                                                     })
                                                                                                     .build();
    Variable<List<CloudApplication>> UPDATED_SUBSCRIBERS = ImmutableJsonBinaryVariable.<List<CloudApplication>> builder()
                                                                                      .name("updatedSubscribers")
                                                                                      .type(new TypeReference<>() {
                                                                                      })
                                                                                      .build();
    Variable<List<CloudRoute>> CURRENT_ROUTES = ImmutableJsonBinaryListVariable.<CloudRoute> builder()
                                                                               .name("currentRoutes")
                                                                               .type(Variable.typeReference(CloudRoute.class))
                                                                               .defaultValue(Collections.emptyList())
                                                                               .build();
    Variable<List<CloudServiceInstanceExtended>> SERVICES_DATA = ImmutableJsonBinaryVariable.<List<CloudServiceInstanceExtended>> builder()
                                                                                            .name("servicesData")
                                                                                            .type(new TypeReference<>() {
                                                                                            })
                                                                                            .defaultValue(Collections.emptyList())
                                                                                            .build();
    Variable<List<Map<String, Map<String, String>>>> FILE_LIST = ImmutableSimpleVariable.<List<Map<String, Map<String, String>>>> builder()
                                                                                        .name("fileList")
                                                                                        .defaultValue(Collections.emptyList())
                                                                                        .build();
    Variable<ErrorType> ERROR_TYPE = ImmutableEnumVariable.<ErrorType> builder()
                                                          .name("errorType")
                                                          .type(ErrorType.class)
                                                          .build();
    Variable<ApplicationColor> IDLE_MTA_COLOR = ImmutableEnumVariable.<ApplicationColor> builder()
                                                                     .name("idleMtaColor")
                                                                     .type(ApplicationColor.class)
                                                                     .build();
    Variable<ApplicationColor> LIVE_MTA_COLOR = ImmutableEnumVariable.<ApplicationColor> builder()
                                                                     .name("liveMtaColor")
                                                                     .type(ApplicationColor.class)
                                                                     .build();
    Variable<StepPhase> STEP_PHASE = ImmutableEnumVariable.<StepPhase> builder()
                                                          .name("stepPhase")
                                                          .type(StepPhase.class)
                                                          .defaultValue(StepPhase.EXECUTE)
                                                          .build();
    Variable<Phase> PHASE = ImmutableEnumVariable.<Phase> builder()
                                                 .name("phase")
                                                 .type(Phase.class)
                                                 .build();
    Variable<SubprocessPhase> SUBPROCESS_PHASE = ImmutableEnumVariable.<SubprocessPhase> builder()
                                                                      .name("subprocessPhase")
                                                                      .type(SubprocessPhase.class)
                                                                      .build();
    Variable<VersionRule> VERSION_RULE = ImmutableEnumVariable.<VersionRule> builder()
                                                              .name("versionRule")
                                                              .type(VersionRule.class)
                                                              .defaultValue(VersionRule.SAME_HIGHER)
                                                              .build();
    Variable<DeploymentMode> DEPLOYMENT_MODE = ImmutableEnumVariable.<DeploymentMode> builder()
                                                                    .name("deploymentMode")
                                                                    .type(DeploymentMode.class)
                                                                    .build();
    Variable<List<ApplicationStateAction>> APP_STATE_ACTIONS_TO_EXECUTE = ImmutableEnumListVariable.<ApplicationStateAction> builder()
                                                                                                   .name("appStateActionsToExecute")
                                                                                                   .type(ApplicationStateAction.class)
                                                                                                   .build();
    Variable<List<ServiceAction>> SERVICE_ACTIONS_TO_EXCECUTE = ImmutableEnumListVariable.<ServiceAction> builder()
                                                                                         .name("serviceActionsToExecute")
                                                                                         .type(ServiceAction.class)
                                                                                         .defaultValue(Collections.emptyList())
                                                                                         .build();
    Variable<List<CloudApplication>> APPS_TO_UNDEPLOY = ImmutableJsonStringListVariable.<CloudApplication> builder()
                                                                                       .name("appsToUndeploy")
                                                                                       .type(Variable.typeReference(CloudApplication.class))
                                                                                       .defaultValue(Collections.emptyList())
                                                                                       .build();
    Variable<List<CloudServiceInstanceExtended>> SERVICES_TO_BIND = ImmutableJsonStringListVariable.<CloudServiceInstanceExtended> builder()
                                                                                                   .name("servicesToBind")
                                                                                                   .type(Variable.typeReference(CloudServiceInstanceExtended.class))
                                                                                                   .defaultValue(Collections.emptyList())
                                                                                                   .build();
    Variable<List<CloudServiceInstanceExtended>> SERVICES_TO_CREATE = ImmutableJsonStringListVariable.<CloudServiceInstanceExtended> builder()
                                                                                                     .name("servicesToCreate")
                                                                                                     .type(Variable.typeReference(CloudServiceInstanceExtended.class))
                                                                                                     .defaultValue(Collections.emptyList())
                                                                                                     .build();
    Variable<List<Hook>> HOOKS_FOR_EXECUTION = ImmutableJsonStringListVariable.<Hook> builder()
                                                                              .name("hooksForExecution")
                                                                              .type(Variable.typeReference(Hook.class))
                                                                              .defaultValue(Collections.emptyList())
                                                                              .build();
    Variable<List<Module>> MODULES_TO_DEPLOY = ImmutableJsonBinaryListVariable.<Module> builder()
                                                                              .name("modulesToDeploy")
                                                                              .type(Variable.typeReference(Module.class))
                                                                              .defaultValue(Collections.emptyList())
                                                                              .build();
    Variable<List<Module>> ALL_MODULES_TO_DEPLOY = ImmutableJsonBinaryListVariable.<Module> builder()
                                                                                  .name("allModulesToDeploy")
                                                                                  .type(Variable.typeReference(Module.class))
                                                                                  .defaultValue(Collections.emptyList())
                                                                                  .build();
    Variable<List<Module>> ITERATED_MODULES_IN_PARALLEL = ImmutableJsonBinaryListVariable.<Module> builder()
                                                                                         .name("iteratedModulesInParallel")
                                                                                         .type(Variable.typeReference(Module.class))
                                                                                         .defaultValue(Collections.emptyList())
                                                                                         .build();
    Variable<List<Module>> MODULES_TO_ITERATE_IN_PARALLEL = ImmutableJsonBinaryListVariable.<Module> builder()
                                                                                           .name("modulesToIterateInParallel")
                                                                                           .type(Variable.typeReference(Module.class))
                                                                                           .defaultValue(Collections.emptyList())
                                                                                           .build();
    Variable<List<FileEntry>> FILE_ENTRIES = ImmutableJsonBinaryListVariable.<FileEntry> builder()
                                                                            .name("fileEntries")
                                                                            .type(Variable.typeReference(FileEntry.class))
                                                                            .defaultValue(Collections.emptyList())
                                                                            .build();
    Variable<List<ExtensionDescriptor>> MTA_EXTENSION_DESCRIPTOR_CHAIN = ImmutableJsonBinaryListVariableAllowingNulls.<ExtensionDescriptor> builder()
                                                                                                                     .name("mtaExtensionDescriptorChain")
                                                                                                                     .type(Variable.typeReference(ExtensionDescriptor.class))
                                                                                                                     .build();
    Variable<List<String>> MODULES_FOR_DEPLOYMENT = ImmutableCommaSeparatedValuesVariable.builder()
                                                                                         .name("modulesForDeployment")
                                                                                         .build();
    Variable<List<String>> RESOURCES_FOR_DEPLOYMENT = ImmutableCommaSeparatedValuesVariable.builder()
                                                                                           .name("resourcesForDeployment")
                                                                                           .build();
    Variable<DeployStrategy> DEPLOY_STRATEGY = ImmutableSimpleVariable.<DeployStrategy> builder()
                                                                      .name("strategy")
                                                                      .defaultValue(DeployStrategy.DEFAULT)
                                                                      .build();
    Variable<Boolean> MISSING_DEFAULT_DOMAIN = ImmutableSimpleVariable.<Boolean> builder()
                                                                      .name("missingDefaultDomain")
                                                                      .defaultValue(false)
                                                                      .build();
    Variable<List<String>> SERVICES_TO_UNBIND_BIND = ImmutableSimpleVariable.<List<String>> builder()
                                                                            .name("servicesToUnbindBind")
                                                                            .defaultValue(Collections.emptyList())
                                                                            .build();
    Variable<String> SERVICE_TO_UNBIND_BIND = ImmutableSimpleVariable.<String> builder()
                                                                     .name("serviceToUnbindBind")
                                                                     .build();
    Variable<Boolean> SHOULD_UNBIND_SERVICE_FROM_APP = ImmutableSimpleVariable.<Boolean> builder()
                                                                              .name("shouldUnbindServiceFromApp")
                                                                              .defaultValue(false)
                                                                              .build();
    Variable<Boolean> SHOULD_BIND_SERVICE_TO_APP = ImmutableSimpleVariable.<Boolean> builder()
                                                                          .name("shouldBindServiceToApp")
                                                                          .defaultValue(false)
                                                                          .build();
    Variable<Map<String, Object>> SERVICE_BINDING_PARAMETERS = ImmutableJsonBinaryVariable.<Map<String, Object>> builder()
                                                                                          .name("serviceBindingParameters")
                                                                                          .type(new TypeReference<Map<String, Object>>() {
                                                                                          })
                                                                                          .build();
    Variable<Boolean> SHOULD_UNBIND_BIND_SERVICES_IN_PARALLEL = ImmutableSimpleVariable.<Boolean> builder()
                                                                                       .name("shouldUnbindBindServicesInParallel")
                                                                                       .defaultValue(true)
                                                                                       .build();

    Variable<String> SERVICE_BROKER_ASYNC_JOB_ID = ImmutableSimpleVariable.<String> builder()
                                                                          .name("serviceBrokerAsyncJobId")
                                                                          .build();

    Variable<Map<String, String>> SERVICE_BROKER_NAMES_JOB_IDS = ImmutableSimpleVariable.<Map<String, String>> builder()
                                                                                        .name("serviceBrokerNamesJobsIds")
                                                                                        .build();

    Variable<List<List<Resource>>> BATCHES_TO_PROCESS = ImmutableJsonStringListVariable.<List<Resource>> builder()
                                                                                       .name("batchesToProcess")
                                                                                       .type(new TypeReference<>() {
                                                                                       })
                                                                                       .defaultValue(Collections.emptyList())
                                                                                       .build();
    Variable<List<Resource>> BATCH_TO_PROCESS = ImmutableJsonStringVariable.<List<Resource>> builder()
                                                                           .name("batchToProcess")
                                                                           .type(new TypeReference<>() {
                                                                           })
                                                                           .defaultValue(Collections.emptyList())
                                                                           .build();
    Variable<Boolean> SHOULD_RECREATE_SERVICE_BINDING = ImmutableSimpleVariable.<Boolean> builder()
                                                                               .name("shouldRecreateServiceBinding")
                                                                               .defaultValue(false)
                                                                               .build();
    Variable<String> SERVICE_BINDING_JOB_ID = ImmutableSimpleVariable.<String> builder()
                                                                     .name("serviceBindingJobId")
                                                                     .build();
    Variable<String> SERVICE_UNBINDING_JOB_ID = ImmutableSimpleVariable.<String> builder()
                                                                       .name("serviceUnbindingJobId")
                                                                       .build();
    Variable<String> SERVICE_KEY_CREATION_JOB_ID = ImmutableSimpleVariable.<String> builder()
                                                                          .name("serviceKeyCreationJobId")
                                                                          .build();
    Variable<String> SERVICE_KEY_DELETION_JOB_ID = ImmutableSimpleVariable.<String> builder()
                                                                          .name("serviceKeyDeletionJobId")
                                                                          .build();
    Variable<Boolean> USE_LAST_OPERATION_FOR_SERVICE_BINDING_CREATION = ImmutableSimpleVariable.<Boolean> builder()
                                                                                               .name("useLastOperationForServiceBindingCreation")
                                                                                               .defaultValue(false)
                                                                                               .build();
    Variable<Boolean> USE_LAST_OPERATION_FOR_SERVICE_BINDING_DELETION = ImmutableSimpleVariable.<Boolean> builder()
                                                                                               .name("useLastOperationForServiceBindingDeletion")
                                                                                               .defaultValue(false)
                                                                                               .build();
    Variable<Boolean> USE_LAST_OPERATION_FOR_SERVICE_KEY_CREATION = ImmutableSimpleVariable.<Boolean> builder()
                                                                                           .name("useLastOperationForServiceKeyCreation")
                                                                                           .defaultValue(false)
                                                                                           .build();
    Variable<Boolean> USE_LAST_OPERATION_FOR_SERVICE_KEY_DELETION = ImmutableSimpleVariable.<Boolean> builder()
                                                                                           .name("useLastOperationForServiceKeyDeletion")
                                                                                           .defaultValue(false)
                                                                                           .build();
    Variable<CloudServiceBinding> SERVICE_BINDING_TO_DELETE = ImmutableJsonStringVariable.<CloudServiceBinding> builder()
                                                                                         .name("serviceBindingToDelete")
                                                                                         .type(Variable.typeReference(CloudServiceBinding.class))
                                                                                         .build();
    Variable<List<CloudServiceBinding>> CLOUD_SERVICE_BINDINGS_TO_DELETE = ImmutableJsonStringListVariable.<CloudServiceBinding> builder()
                                                                                                          .name("cloudServiceBindingsToDelete")
                                                                                                          .type(new TypeReference<>() {
                                                                                                          })
                                                                                                          .defaultValue(Collections.emptyList())
                                                                                                          .build();
    Variable<List<CloudServiceKey>> CLOUD_SERVICE_KEYS_TO_CREATE = ImmutableJsonStringListVariable.<CloudServiceKey> builder()
                                                                                                  .name("cloudServiceKeysToCreate")
                                                                                                  .type(new TypeReference<>() {
                                                                                                  })
                                                                                                  .defaultValue(Collections.emptyList())
                                                                                                  .build();
    Variable<List<CloudServiceKey>> CLOUD_SERVICE_KEYS_TO_DELETE = ImmutableJsonStringListVariable.<CloudServiceKey> builder()
                                                                                                  .name("cloudServiceKeysToDelete")
                                                                                                  .type(new TypeReference<>() {
                                                                                                  })
                                                                                                  .defaultValue(Collections.emptyList())
                                                                                                  .build();
    Variable<List<CloudServiceKey>> CLOUD_SERVICE_KEYS_TO_UPDATE_METADATA = ImmutableJsonStringListVariable.<CloudServiceKey> builder()
                                                                                                  .name("cloudServiceKeysToUpdate")
                                                                                                  .type(new TypeReference<>() {
                                                                                                  })
                                                                                                  .defaultValue(Collections.emptyList())
                                                                                                  .build();
    Variable<List<ServiceDeletionActions>> SERVICE_DELETION_ACTIONS = ImmutableEnumListVariable.<ServiceDeletionActions> builder()
                                                                                               .name("serviceDeletionActions")
                                                                                               .type(ServiceDeletionActions.class)
                                                                                               .defaultValue(Collections.emptyList())
                                                                                               .build();
    Variable<CloudServiceKey> SERVICE_KEY_TO_PROCESS = ImmutableJsonStringVariable.<CloudServiceKey> builder()
                                                                                  .name("serviceKeyToProcess")
                                                                                  .type(Variable.typeReference(CloudServiceKey.class))
                                                                                  .build();
    Variable<Boolean> SERVICE_KEY_DOES_NOT_EXIST = ImmutableSimpleVariable.<Boolean> builder()
                                                                          .name("serviceKeyDoesNotExist")
                                                                          .defaultValue(false)
                                                                          .build();
    Variable<List<CloudServiceKey>> CLOUD_SERVICE_KEYS_FOR_WAITING = ImmutableJsonStringListVariable.<CloudServiceKey> builder()
                                                                                                    .name("cloudServiceKeysForWaiting")
                                                                                                    .type(new TypeReference<>() {
                                                                                                    })
                                                                                                    .defaultValue(Collections.emptyList())
                                                                                                    .build();
    // we need to use a Json string serialization because the nanosecond precision is being lost when using SimpleVariable
    Variable<LocalDateTime> LOGS_OFFSET_FOR_APP_EXECUTION = ImmutableJsonStringVariable.<LocalDateTime> builder()
                                                                                       .name("logsOffsetForAppExecution")
                                                                                       .type(Variable.typeReference(LocalDateTime.class))
                                                                                       .defaultValue(LocalDateTime.ofInstant(Instant.EPOCH,
                                                                                                                             ZoneId.of("UTC")))
                                                                                       .build();
    // we need to use a Json string serialization because the nanosecond precision is being lost when using SimpleVariable
    Variable<LocalDateTime> LOGS_OFFSET = ImmutableJsonStringVariable.<LocalDateTime> builder()
                                                                     .name("logsOffset")
                                                                     .type(Variable.typeReference(LocalDateTime.class))
                                                                     .defaultValue(LocalDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC")))
                                                                     .build();

    Variable<CloudApplication> EXISTING_APP_TO_POLL = ImmutableJsonBinaryVariable.<CloudApplication> builder()
                                                                                 .name("existingAppToPoll")
                                                                                 .type(Variable.typeReference(CloudApplication.class))
                                                                                 .build();

    Variable<Duration> DELAY_AFTER_APP_STOP = ImmutableSimpleVariable.<Duration> builder()
                                                                     .name("delayAfterAppStop")
                                                                     .defaultValue(Duration.ZERO)
                                                                     .build();

}
