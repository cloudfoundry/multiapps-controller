package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.cf.core.cf.metadata.util.MtaMetadataUtil.hasEnvMtaMetadata;
import static com.sap.cloud.lm.sl.cf.core.cf.metadata.util.MtaMetadataUtil.hasMtaMetadata;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadata;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.EnvMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.MtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Named("deleteDiscontinuedConfigurationEntriesForAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteDiscontinuedConfigurationEntriesForAppStep extends SyncFlowableStep {

    @Inject
    private ConfigurationEntryService configurationEntryService;

    @Inject
    private MtaMetadataParser mtaMetadataParser;

    @Inject
    private EnvMtaMetadataParser envMtaMetadataParser;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        CloudApplication existingApp = context.getVariable(Variables.EXISTING_APP);
        if (existingApp == null) {
            return StepPhase.DONE;
        }
        getStepLogger().info(Messages.DELETING_DISCONTINUED_CONFIGURATION_ENTRIES_FOR_APP, existingApp.getName());
        String mtaId = context.getVariable(Variables.MTA_ID);
        MtaMetadata mtaMetadata = getMtaMetadata(existingApp);
        if (mtaMetadata == null) {
            return StepPhase.DONE;
        }
        List<String> providedDependencyNames = getDeployedMtaApplication(existingApp).getProvidedDependencyNames();
        String org = context.getVariable(Variables.ORG);
        String space = context.getVariable(Variables.SPACE);
        CloudTarget target = new CloudTarget(org, space);
        String oldMtaVersion = mtaMetadata.getVersion()
                                          .toString();
        List<ConfigurationEntry> publishedEntries = context.getVariable(Variables.PUBLISHED_ENTRIES);

        List<ConfigurationEntry> entriesToDelete = getEntriesToDelete(mtaId, oldMtaVersion, target, providedDependencyNames,
                                                                      publishedEntries);
        for (ConfigurationEntry entry : entriesToDelete) {
            int deletedEntries = configurationEntryService.createQuery()
                                                          .id(entry.getId())
                                                          .delete();
            if (deletedEntries == 0) {
                getStepLogger().warn(Messages.COULD_NOT_DELETE_PROVIDED_DEPENDENCY, entry.getProviderId());
            }
        }
        getStepLogger().debug(Messages.DELETED_ENTRIES, JsonUtil.toJson(entriesToDelete, true));
        context.setVariable(Variables.DELETED_ENTRIES, entriesToDelete);

        getStepLogger().debug(Messages.DISCONTINUED_CONFIGURATION_ENTRIES_FOR_APP_DELETED, existingApp.getName());
        return StepPhase.DONE;
    }

    private MtaMetadata getMtaMetadata(CloudApplication existingApp) {
        if (hasMtaMetadata(existingApp)) {
            return mtaMetadataParser.parseMtaMetadata(existingApp);
        } else if (hasEnvMtaMetadata(existingApp)) {
            return envMtaMetadataParser.parseMtaMetadata(existingApp);
        }
        return null;
    }

    private DeployedMtaApplication getDeployedMtaApplication(CloudApplication existingApp) {
        if (hasMtaMetadata(existingApp)) {
            return mtaMetadataParser.parseDeployedMtaApplication(existingApp);
        }
        return envMtaMetadataParser.parseDeployedMtaApplication(existingApp);
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_DELETING_DISCONTINUED_CONFIGURATION_ENTRIES_FOR_APP,
                                    context.getVariable(Variables.EXISTING_APP)
                                           .getName());
    }

    private List<ConfigurationEntry> getEntriesToDelete(String mtaId, String mtaVersion, CloudTarget target,
                                                        List<String> providedDependencyNames, List<ConfigurationEntry> publishedEntries) {
        List<ConfigurationEntry> entriesForCurrentMta = getEntries(mtaId, mtaVersion, target);
        List<ConfigurationEntry> entriesForCurrentModule = getConfigurationEntriesWithProviderIds(entriesForCurrentMta,
                                                                                                  getProviderIds(mtaId,
                                                                                                                 providedDependencyNames));
        return getEntriesNotUpdatedByThisProcess(entriesForCurrentModule, publishedEntries);
    }

    private List<ConfigurationEntry> getEntriesNotUpdatedByThisProcess(List<ConfigurationEntry> entriesForCurrentModule,
                                                                       List<ConfigurationEntry> publishedEntries) {
        return entriesForCurrentModule.stream()
                                      .filter(entry -> !hasId(entry, publishedEntries))
                                      .collect(Collectors.toList());
    }

    private boolean hasId(ConfigurationEntry entry, List<ConfigurationEntry> publishedEntries) {
        return publishedEntries.stream()
                               .anyMatch(publishedEntry -> publishedEntry.getId() == entry.getId());
    }

    private List<String> getProviderIds(String mtaId, List<String> providedDependencyNames) {
        return providedDependencyNames.stream()
                                      .map(providedDependencyName -> ConfigurationEntriesUtil.computeProviderId(mtaId,
                                                                                                                providedDependencyName))
                                      .collect(Collectors.toList());
    }

    private List<ConfigurationEntry> getConfigurationEntriesWithProviderIds(List<ConfigurationEntry> entries, List<String> providerIds) {
        return entries.stream()
                      .filter(entry -> hasProviderId(entry, providerIds))
                      .collect(Collectors.toList());
    }

    private boolean hasProviderId(ConfigurationEntry entry, List<String> providerIds) {
        return providerIds.stream()
                          .anyMatch(providerId -> entry.getProviderId()
                                                       .equals(providerId));
    }

    private List<ConfigurationEntry> getEntries(String mtaId, String mtaVersion, CloudTarget target) {
        return configurationEntryService.createQuery()
                                        .providerNid(ConfigurationEntriesUtil.PROVIDER_NID)
                                        .version(mtaVersion)
                                        .target(target)
                                        .mtaId(mtaId)
                                        .list();
    }

}
