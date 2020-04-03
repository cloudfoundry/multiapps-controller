package com.sap.cloud.lm.sl.cf.process.variables;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.UploadToken;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaArchiveElements;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.ServiceOperation;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Hook;
import com.sap.cloud.lm.sl.mta.model.Module;

public interface Variables {

    Variable<String> CORRELATION_ID = ImmutableSimpleVariable.<String> builder()
                                                             .name(Constants.VAR_CORRELATION_ID)
                                                             .build();
    Variable<String> ORG = ImmutableSimpleVariable.<String> builder()
                                                  .name(Constants.VAR_ORG)
                                                  .build();
    Variable<String> SPACE = ImmutableSimpleVariable.<String> builder()
                                                    .name(Constants.VAR_SPACE)
                                                    .build();
    Variable<String> ORG_ID = ImmutableSimpleVariable.<String> builder()
                                                     .name(Constants.VAR_ORG_ID)
                                                     .build();
    Variable<String> SPACE_ID = ImmutableSimpleVariable.<String> builder()
                                                       .name(com.sap.cloud.lm.sl.cf.persistence.Constants.VARIABLE_NAME_SPACE_ID)
                                                       .build();
    Variable<String> NEW_MTA_VERSION = ImmutableSimpleVariable.<String> builder()
                                                              .name(Constants.VAR_NEW_MTA_VERSION)
                                                              .build();
    Variable<String> PARENT_PROCESS_ID = ImmutableSimpleVariable.<String> builder()
                                                                .name(Constants.VAR_PARENT_PROCESS_ID)
                                                                .build();
    Variable<String> SUBPROCESS_ID = ImmutableSimpleVariable.<String> builder()
                                                            .name(Constants.VAR_SUBPROCESS_ID)
                                                            .build();
    Variable<String> SERVICE_ID = ImmutableSimpleVariable.<String> builder()
                                                         .name(com.sap.cloud.lm.sl.cf.persistence.Constants.VARIABLE_NAME_SERVICE_ID)
                                                         .build();
    Variable<String> TASK_ID = ImmutableSimpleVariable.<String> builder()
                                                      .name(Constants.TASK_ID)
                                                      .build();
    Variable<String> SERVICE_TO_PROCESS_NAME = ImmutableSimpleVariable.<String> builder()
                                                                      .name(Constants.VAR_SERVICE_TO_PROCESS_NAME)
                                                                      .build();
    Variable<String> APP_ARCHIVE_ID = ImmutableSimpleVariable.<String> builder()
                                                             .name(Constants.PARAM_APP_ARCHIVE_ID)
                                                             .build();
    Variable<String> EXT_DESCRIPTOR_FILE_ID = ImmutableSimpleVariable.<String> builder()
                                                                     .name(Constants.PARAM_EXT_DESCRIPTOR_FILE_ID)
                                                                     .build();
    Variable<String> MTA_ID = ImmutableSimpleVariable.<String> builder()
                                                     .name(Constants.PARAM_MTA_ID)
                                                     .build();
    Variable<String> GIT_URI = ImmutableSimpleVariable.<String> builder()
                                                      .name(Constants.PARAM_GIT_URI)
                                                      .build();
    Variable<String> GIT_REF = ImmutableSimpleVariable.<String> builder()
                                                      .name(Constants.PARAM_GIT_REF)
                                                      .build();
    Variable<String> GIT_REPO_PATH = ImmutableSimpleVariable.<String> builder()
                                                            .name(Constants.PARAM_GIT_REPO_PATH)
                                                            .build();
    Variable<String> CTS_PROCESS_ID = ImmutableSimpleVariable.<String> builder()
                                                             .name(Constants.PARAM_CTS_PROCESS_ID)
                                                             .build();
    Variable<String> DEPLOY_URI = ImmutableSimpleVariable.<String> builder()
                                                         .name(Constants.PARAM_DEPLOY_URI)
                                                         .build();
    Variable<String> USERNAME = ImmutableSimpleVariable.<String> builder()
                                                       .name(Constants.PARAM_USERNAME)
                                                       .build();
    Variable<String> PASSWORD = ImmutableSimpleVariable.<String> builder()
                                                       .name(Constants.PARAM_PASSWORD)
                                                       .build();
    Variable<String> TRANSFER_TYPE = ImmutableSimpleVariable.<String> builder()
                                                            .name(Constants.PARAM_TRANSFER_TYPE)
                                                            .build();
    Variable<String> APPLICATION_TYPE = ImmutableSimpleVariable.<String> builder()
                                                               .name(Constants.PARAM_APPLICATION_TYPE)
                                                               .build();
    Variable<String> USER = ImmutableSimpleVariable.<String> builder()
                                                   .name(Constants.VAR_USER)
                                                   .build();
    Variable<String> SERVICE_OFFERING = ImmutableSimpleVariable.<String> builder()
                                                               .name(Constants.VAR_SERVICE_OFFERING)
                                                               .build();
    Variable<String> INDEX_VARIABLE_NAME = ImmutableSimpleVariable.<String> builder()
                                                                  .name(Constants.VAR_INDEX_VARIABLE_NAME)
                                                                  .build();
    Variable<String> STEP_EXECUTION = ImmutableSimpleVariable.<String> builder()
                                                             .name(Constants.VAR_STEP_EXECUTION)
                                                             .build();
    // TODO: Saving booleans as strings... Really???
    Variable<String> APP_CONTENT_CHANGED = ImmutableSimpleVariable.<String> builder()
                                                                  .name(Constants.VAR_APP_CONTENT_CHANGED)
                                                                  .defaultValue(Boolean.toString(false))
                                                                  .build();
    Variable<Integer> MTA_MAJOR_SCHEMA_VERSION = ImmutableSimpleVariable.<Integer> builder()
                                                                        .name(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION)
                                                                        .build();
    Variable<Integer> START_TIMEOUT = ImmutableSimpleVariable.<Integer> builder()
                                                             .name(Constants.PARAM_START_TIMEOUT)
                                                             .defaultValue(Constants.DEFAULT_START_TIMEOUT)
                                                             .build();
    Variable<Integer> UPDATED_SERVICE_BROKER_SUBSCRIBERS_COUNT = ImmutableSimpleVariable.<Integer> builder()
                                                                                        .name(Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS_COUNT)
                                                                                        .build();
    Variable<Integer> UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX = ImmutableSimpleVariable.<Integer> builder()
                                                                                        .name(Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS_INDEX)
                                                                                        .build();
    Variable<Integer> MODULES_COUNT = ImmutableSimpleVariable.<Integer> builder()
                                                             .name(Constants.VAR_MODULES_COUNT)
                                                             .build();
    Variable<Integer> MODULES_INDEX = ImmutableSimpleVariable.<Integer> builder()
                                                             .name(Constants.VAR_MODULES_INDEX)
                                                             .build();
    Variable<Integer> MTARS_COUNT = ImmutableSimpleVariable.<Integer> builder()
                                                           .name(Constants.VAR_MTARS_COUNT)
                                                           .build();
    Variable<Integer> MTARS_INDEX = ImmutableSimpleVariable.<Integer> builder()
                                                           .name(Constants.VAR_MTARS_INDEX)
                                                           .build();
    Variable<Integer> TASKS_COUNT = ImmutableSimpleVariable.<Integer> builder()
                                                           .name(Constants.VAR_TASKS_COUNT)
                                                           .build();
    Variable<Integer> TASKS_INDEX = ImmutableSimpleVariable.<Integer> builder()
                                                           .name(Constants.VAR_TASKS_INDEX)
                                                           .build();
    Variable<Integer> SERVICES_TO_CREATE_COUNT = ImmutableSimpleVariable.<Integer> builder()
                                                                        .name(Constants.VAR_SERVICES_TO_CREATE_COUNT)
                                                                        .build();
    Variable<Integer> ASYNC_STEP_EXECUTION_INDEX = ImmutableSimpleVariable.<Integer> builder()
                                                                          .name(Constants.ASYNC_STEP_EXECUTION_INDEX)
                                                                          .build();
    Variable<Long> START_TIME = ImmutableSimpleVariable.<Long> builder()
                                                       .name(Constants.VAR_START_TIME)
                                                       .build();
    Variable<Boolean> SKIP_MANAGE_SERVICE_BROKER = ImmutableSimpleVariable.<Boolean> builder()
                                                                          .name(Constants.VAR_SKIP_MANAGE_SERVICE_BROKER)
                                                                          .build();
    Variable<Boolean> VERIFY_ARCHIVE_SIGNATURE = ImmutableSimpleVariable.<Boolean> builder()
                                                                        .name(Constants.PARAM_VERIFY_ARCHIVE_SIGNATURE)
                                                                        .build();
    Variable<Boolean> DELETE_IDLE_URIS = ImmutableSimpleVariable.<Boolean> builder()
                                                                .name(Constants.VAR_DELETE_IDLE_URIS)
                                                                .defaultValue(false)
                                                                .build();
    Variable<Boolean> USE_IDLE_URIS = ImmutableSimpleVariable.<Boolean> builder()
                                                             .name(Constants.VAR_USE_IDLE_URIS)
                                                             .defaultValue(false)
                                                             .build();
    Variable<Boolean> USE_NAMESPACES = ImmutableSimpleVariable.<Boolean> builder()
                                                              .name(Constants.PARAM_USE_NAMESPACES)
                                                              .defaultValue(false)
                                                              .build();
    Variable<Boolean> USE_NAMESPACES_FOR_SERVICES = ImmutableSimpleVariable.<Boolean> builder()
                                                                           .name(Constants.PARAM_USE_NAMESPACES_FOR_SERVICES)
                                                                           .defaultValue(false)
                                                                           .build();
    Variable<Boolean> IS_SERVICE_UPDATED = ImmutableSimpleVariable.<Boolean> builder()
                                                                  .name(Constants.VAR_IS_SERVICE_UPDATED)
                                                                  .defaultValue(false)
                                                                  .build();
    Variable<Boolean> SKIP_UPDATE_CONFIGURATION_ENTRIES = ImmutableSimpleVariable.<Boolean> builder()
                                                                                 .name(Constants.VAR_SKIP_UPDATE_CONFIGURATION_ENTRIES)
                                                                                 .defaultValue(false)
                                                                                 .build();
    Variable<Boolean> FAIL_ON_CRASHED = ImmutableSimpleVariable.<Boolean> builder()
                                                               .name(Constants.PARAM_FAIL_ON_CRASHED)
                                                               .defaultValue(true)
                                                               .build();
    Variable<Boolean> USER_PROPERTIES_CHANGED = ImmutableSimpleVariable.<Boolean> builder()
                                                                       .name(Constants.VAR_USER_PROPERTIES_CHANGED)
                                                                       .defaultValue(false)
                                                                       .build();
    Variable<Boolean> VCAP_APP_PROPERTIES_CHANGED = ImmutableSimpleVariable.<Boolean> builder()
                                                                           .name(Constants.VAR_VCAP_APP_PROPERTIES_CHANGED)
                                                                           .defaultValue(false)
                                                                           .build();
    Variable<Boolean> VCAP_SERVICES_PROPERTIES_CHANGED = ImmutableSimpleVariable.<Boolean> builder()
                                                                                .name(Constants.VAR_VCAP_SERVICES_PROPERTIES_CHANGED)
                                                                                .defaultValue(false)
                                                                                .build();
    Variable<Boolean> SHOULD_SKIP_SERVICE_REBINDING = ImmutableSimpleVariable.<Boolean> builder()
                                                                             .name(Constants.VAR_SHOULD_SKIP_SERVICE_REBINDING)
                                                                             .defaultValue(false)
                                                                             .build();
    Variable<Boolean> DELETE_SERVICES = ImmutableSimpleVariable.<Boolean> builder()
                                                               .name(Constants.PARAM_DELETE_SERVICES)
                                                               .defaultValue(false)
                                                               .build();
    Variable<Boolean> DELETE_SERVICE_KEYS = ImmutableSimpleVariable.<Boolean> builder()
                                                                   .name(Constants.PARAM_DELETE_SERVICE_KEYS)
                                                                   .defaultValue(false)
                                                                   .build();
    Variable<Boolean> DELETE_SERVICE_BROKERS = ImmutableSimpleVariable.<Boolean> builder()
                                                                      .name(Constants.PARAM_DELETE_SERVICE_BROKERS)
                                                                      .defaultValue(false)
                                                                      .build();
    Variable<Boolean> NO_START = ImmutableSimpleVariable.<Boolean> builder()
                                                        .name(Constants.PARAM_NO_START)
                                                        .defaultValue(false)
                                                        .build();
    Variable<Boolean> KEEP_FILES = ImmutableSimpleVariable.<Boolean> builder()
                                                          .name(Constants.PARAM_KEEP_FILES)
                                                          .defaultValue(false)
                                                          .build();
    Variable<Boolean> NO_CONFIRM = ImmutableSimpleVariable.<Boolean> builder()
                                                          .name(Constants.PARAM_NO_CONFIRM)
                                                          .defaultValue(false)
                                                          .build();
    Variable<Boolean> NO_RESTART_SUBSCRIBED_APPS = ImmutableSimpleVariable.<Boolean> builder()
                                                                          .name(Constants.PARAM_NO_RESTART_SUBSCRIBED_APPS)
                                                                          .defaultValue(false)
                                                                          .build();
    Variable<Boolean> GIT_SKIP_SSL = ImmutableSimpleVariable.<Boolean> builder()
                                                            .name(Constants.PARAM_GIT_SKIP_SSL)
                                                            .build();
    Variable<Boolean> NO_FAIL_ON_MISSING_PERMISSIONS = ImmutableSimpleVariable.<Boolean> builder()
                                                                              .name(Constants.PARAM_NO_FAIL_ON_MISSING_PERMISSIONS)
                                                                              .build();
    Variable<Boolean> ABORT_ON_ERROR = ImmutableSimpleVariable.<Boolean> builder()
                                                              .name(Constants.PARAM_ABORT_ON_ERROR)
                                                              .defaultValue(false)
                                                              .build();
    Variable<Boolean> KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY = ImmutableSimpleVariable.<Boolean> builder()
                                                                                    .name(Constants.PARAM_KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY)
                                                                                    .build();
    Variable<Boolean> EXECUTE_ONE_OFF_TASKS = ImmutableSimpleVariable.<Boolean> builder()
                                                                     .name(Constants.EXECUTE_ONE_OFF_TASKS)
                                                                     .build();
    Variable<Boolean> SHOULD_UPLOAD_APPLICATION_CONTENT = ImmutableSimpleVariable.<Boolean> builder()
                                                                                 .name(Constants.SHOULD_UPLOAD_APPLICATION_CONTENT)
                                                                                 .build();
    Variable<Boolean> REBUILD_APP_ENV = ImmutableSimpleVariable.<Boolean> builder()
                                                               .name(Constants.REBUILD_APP_ENV)
                                                               .build();
    Variable<UUID> BUILD_GUID = ImmutableSimpleVariable.<UUID> builder()
                                                       .name(Constants.VAR_BUILD_GUID)
                                                       .build();
    Variable<DeploymentDescriptor> DEPLOYMENT_DESCRIPTOR = ImmutableJsonStringVariable.<DeploymentDescriptor> builder()
                                                                                      .name(Constants.VAR_MTA_DEPLOYMENT_DESCRIPTOR)
                                                                                      .type(Variable.typeReference(DeploymentDescriptor.class))
                                                                                      .build();
    Variable<DeploymentDescriptor> DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS = ImmutableJsonStringVariable.<DeploymentDescriptor> builder()
                                                                                                             .name(Constants.VAR_MTA_DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS)
                                                                                                             .type(Variable.typeReference(DeploymentDescriptor.class))
                                                                                                             .build();
    Variable<DeploymentDescriptor> COMPLETE_DEPLOYMENT_DESCRIPTOR = ImmutableJsonStringVariable.<DeploymentDescriptor> builder()
                                                                                               .name(Constants.VAR_COMPLETE_MTA_DEPLOYMENT_DESCRIPTOR)
                                                                                               .type(Variable.typeReference(DeploymentDescriptor.class))
                                                                                               .build();
    Variable<CloudApplicationExtended> APP_TO_PROCESS = ImmutableJsonStringVariable.<CloudApplicationExtended> builder()
                                                                                   .name(Constants.VAR_APP_TO_PROCESS)
                                                                                   .type(Variable.typeReference(CloudApplicationExtended.class))
                                                                                   .build();
    Variable<MtaArchiveElements> MTA_ARCHIVE_ELEMENTS = ImmutableJsonStringVariable.<MtaArchiveElements> builder()
                                                                                   .name(Constants.VAR_MTA_ARCHIVE_ELEMENTS)
                                                                                   .type(Variable.typeReference(MtaArchiveElements.class))
                                                                                   .defaultValue(new MtaArchiveElements())
                                                                                   .build();
    Variable<CloudServiceExtended> SERVICE_TO_PROCESS = ImmutableJsonStringVariable.<CloudServiceExtended> builder()
                                                                                   .name(Constants.VAR_SERVICE_TO_PROCESS)
                                                                                   .type(Variable.typeReference(CloudServiceExtended.class))
                                                                                   .build();
    Variable<UploadToken> UPLOAD_TOKEN = ImmutableJsonStringVariable.<UploadToken> builder()
                                                                    .name(Constants.VAR_UPLOAD_TOKEN)
                                                                    .type(Variable.typeReference(UploadToken.class))
                                                                    .build();
    Variable<Hook> HOOK_FOR_EXECUTION = ImmutableJsonStringVariable.<Hook> builder()
                                                                   .name(Constants.VAR_HOOK_FOR_EXECUTION)
                                                                   .type(Variable.typeReference(Hook.class))
                                                                   .build();
    Variable<List<String>> APPS_TO_DEPLOY = ImmutableJsonBinaryVariable.<List<String>> builder()
                                                                       .name(Constants.VAR_APPS_TO_DEPLOY)
                                                                       .type(new TypeReference<List<String>>() {
                                                                       })
                                                                       .build();
    Variable<List<String>> APPS_TO_RENAME = ImmutableJsonBinaryVariable.<List<String>> builder()
                                                                       .name(Constants.VAR_APPS_TO_RENAME)
                                                                       .type(new TypeReference<List<String>>() {
                                                                       })
                                                                       .build();
    Variable<List<ConfigurationEntry>> CONFIGURATION_ENTRIES_TO_PUBLISH = ImmutableJsonBinaryVariable.<List<ConfigurationEntry>> builder()
                                                                                                     .name(Constants.VAR_CONFIGURATION_ENTRIES_TO_PUBLISH)
                                                                                                     .type(new TypeReference<List<ConfigurationEntry>>() {
                                                                                                     })
                                                                                                     .build();
    Variable<List<ConfigurationEntry>> DELETED_ENTRIES = ImmutableJsonBinaryVariable.<List<ConfigurationEntry>> builder()
                                                                                    .name(Constants.VAR_DELETED_ENTRIES)
                                                                                    .type(new TypeReference<List<ConfigurationEntry>>() {
                                                                                    })
                                                                                    .defaultValue(Collections.emptyList())
                                                                                    .build();
    Variable<List<ConfigurationEntry>> PUBLISHED_ENTRIES = ImmutableJsonBinaryVariable.<List<ConfigurationEntry>> builder()
                                                                                      .name(Constants.VAR_PUBLISHED_ENTRIES)
                                                                                      .type(new TypeReference<List<ConfigurationEntry>>() {
                                                                                      })
                                                                                      .build();
    Variable<CloudServiceBroker> CREATED_OR_UPDATED_SERVICE_BROKER = ImmutableJsonBinaryVariable.<CloudServiceBroker> builder()
                                                                                                .name(Constants.VAR_CREATED_OR_UPDATED_SERVICE_BROKER)
                                                                                                .type(Variable.typeReference(CloudServiceBroker.class))
                                                                                                .build();
    Variable<List<String>> CUSTOM_DOMAINS = ImmutableJsonBinaryVariable.<List<String>> builder()
                                                                       .name(Constants.VAR_CUSTOM_DOMAINS)
                                                                       .type(new TypeReference<List<String>>() {
                                                                       })
                                                                       .build();
    Variable<DeployedMta> DEPLOYED_MTA = ImmutableJsonBinaryVariable.<DeployedMta> builder()
                                                                    .name(Constants.VAR_DEPLOYED_MTA)
                                                                    .type(Variable.typeReference(DeployedMta.class))
                                                                    .build();
    Variable<CloudApplication> EXISTING_APP = ImmutableJsonBinaryVariable.<CloudApplication> builder()
                                                                         .name(Constants.VAR_EXISTING_APP)
                                                                         .type(Variable.typeReference(CloudApplication.class))
                                                                         .build();
    Variable<Module> MODULE_TO_DEPLOY = ImmutableJsonBinaryVariable.<Module> builder()
                                                                   .name(Constants.VAR_MODULE_TO_DEPLOY)
                                                                   .type(Variable.typeReference(Module.class))
                                                                   .build();
    Variable<Set<String>> MTA_ARCHIVE_MODULES = ImmutableJsonBinaryVariable.<Set<String>> builder()
                                                                           .name(Constants.VAR_MTA_ARCHIVE_MODULES)
                                                                           .type(new TypeReference<Set<String>>() {
                                                                           })
                                                                           .build();
    Variable<Set<String>> MTA_MODULES = ImmutableJsonBinaryVariable.<Set<String>> builder()
                                                                   .name(Constants.VAR_MTA_MODULES)
                                                                   .type(new TypeReference<Set<String>>() {
                                                                   })
                                                                   .build();
    Variable<Map<String, Map<String, String>>> SERVICE_KEYS_CREDENTIALS_TO_INJECT = ImmutableJsonBinaryVariable.<Map<String, Map<String, String>>> builder()
                                                                                                               .name(Constants.VAR_SERVICE_KEYS_CREDENTIALS_TO_INJECT)
                                                                                                               .type(new TypeReference<Map<String, Map<String, String>>>() {
                                                                                                               })
                                                                                                               .build();
    Variable<Map<String, List<CloudServiceKey>>> SERVICE_KEYS_TO_CREATE = ImmutableJsonBinaryVariable.<Map<String, List<CloudServiceKey>>> builder()
                                                                                                     .name(Constants.VAR_SERVICE_KEYS_TO_CREATE)
                                                                                                     .type(new TypeReference<Map<String, List<CloudServiceKey>>>() {
                                                                                                     })
                                                                                                     .build();
    Variable<String> SERVICE_TO_DELETE = ImmutableSimpleVariable.<String> builder()
                                                                .name(Constants.VAR_SERVICE_TO_DELETE)
                                                                .build();
    Variable<List<String>> SERVICES_TO_DELETE = ImmutableSimpleVariable.<List<String>> builder()
                                                                       .name(Constants.VAR_SERVICES_TO_DELETE)
                                                                       .build();
    Variable<List<CloudServiceExtended>> SERVICES_TO_POLL = ImmutableJsonBinaryVariable.<List<CloudServiceExtended>> builder()
                                                                                       .name(Constants.VAR_SERVICES_TO_POLL)
                                                                                       .type(new TypeReference<List<CloudServiceExtended>>() {
                                                                                       })
                                                                                       .build();
    Variable<CloudTask> STARTED_TASK = ImmutableJsonBinaryVariable.<CloudTask> builder()
                                                                  .name(Constants.VAR_STARTED_TASK)
                                                                  .type(Variable.typeReference(CloudTask.class))
                                                                  .build();
    Variable<StartingInfo> STARTING_INFO = ImmutableJsonBinaryVariable.<StartingInfo> builder()
                                                                      .name(Constants.VAR_STARTING_INFO)
                                                                      .type(Variable.typeReference(StartingInfo.class))
                                                                      .build();
    Variable<List<ConfigurationSubscription>> SUBSCRIPTIONS_TO_CREATE = ImmutableJsonBinaryVariable.<List<ConfigurationSubscription>> builder()
                                                                                                   .name(Constants.VAR_SUBSCRIPTIONS_TO_CREATE)
                                                                                                   .type(new TypeReference<List<ConfigurationSubscription>>() {
                                                                                                   })
                                                                                                   .build();
    Variable<List<ConfigurationSubscription>> SUBSCRIPTIONS_TO_DELETE = ImmutableJsonBinaryVariable.<List<ConfigurationSubscription>> builder()
                                                                                                   .name(Constants.VAR_SUBSCRIPTIONS_TO_DELETE)
                                                                                                   .type(new TypeReference<List<ConfigurationSubscription>>() {
                                                                                                   })
                                                                                                   .build();
    Variable<List<CloudTask>> TASKS_TO_EXECUTE = ImmutableJsonBinaryVariable.<List<CloudTask>> builder()
                                                                            .name(Constants.VAR_TASKS_TO_EXECUTE)
                                                                            .type(new TypeReference<List<CloudTask>>() {
                                                                            })
                                                                            .build();
    Variable<Map<String, ServiceOperation.Type>> TRIGGERED_SERVICE_OPERATIONS = ImmutableJsonBinaryVariable.<Map<String, ServiceOperation.Type>> builder()
                                                                                                           .name(Constants.VAR_TRIGGERED_SERVICE_OPERATIONS)
                                                                                                           .type(new TypeReference<Map<String, ServiceOperation.Type>>() {
                                                                                                           })
                                                                                                           .build();
    Variable<List<CloudApplication>> UPDATED_SERVICE_BROKER_SUBSCRIBERS = ImmutableJsonBinaryVariable.<List<CloudApplication>> builder()
                                                                                                     .name(Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS)
                                                                                                     .type(new TypeReference<List<CloudApplication>>() {
                                                                                                     })
                                                                                                     .build();
    Variable<List<CloudApplication>> UPDATED_SUBSCRIBERS = ImmutableJsonBinaryVariable.<List<CloudApplication>> builder()
                                                                                      .name(Constants.VAR_UPDATED_SUBSCRIBERS)
                                                                                      .type(new TypeReference<List<CloudApplication>>() {
                                                                                      })
                                                                                      .build();
    Variable<Map<String, String>> GIT_REPOSITORY_CONFIG_MAP = ImmutableSimpleVariable.<Map<String, String>> builder()
                                                                                     .name(Constants.VAR_GIT_REPOSITORY_CONFIG_MAP)
                                                                                     .build();
    Variable<List<Map<String, Map<String, String>>>> FILE_LIST = ImmutableSimpleVariable.<List<Map<String, Map<String, String>>>> builder()
                                                                                        .name(Constants.PARAM_FILE_LIST)
                                                                                        .defaultValue(Collections.emptyList())
                                                                                        .build();
    Variable<List<Map<String, Map<String, Object>>>> GIT_REPOSITORY_LIST = ImmutableSimpleVariable.<List<Map<String, Map<String, Object>>>> builder()
                                                                                                  .name(Constants.PARAM_GIT_REPOSITORY_LIST)
                                                                                                  .defaultValue(Collections.emptyList())
                                                                                                  .build();

}
