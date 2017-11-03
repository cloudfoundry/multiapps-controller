package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.cf.detect.ApplicationMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.ListUtil;

@Component("deleteDiscontinuedConfigurationEntriesForAppStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteDiscontinuedConfigurationEntriesForAppStep extends AbstractProcessStep {

    @Inject
    private ConfigurationEntryDao configurationEntryDao;

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) {
        getStepLogger().logActivitiTask();

        CloudApplication existingApp = StepsUtil.getExistingApp(context);
        if (existingApp == null) {
            return ExecutionStatus.SUCCESS;
        }
        getStepLogger().info(Messages.DELETING_DISCONTINUED_CONFIGURATION_ENTRIES_FOR_APP, existingApp.getName());
        String mtaId = (String) context.getVariable(Constants.PARAM_MTA_ID);
        ApplicationMtaMetadata mtaMetadata = ApplicationMtaMetadataParser.parseAppMetadata(existingApp);
        if (mtaMetadata == null) {
            return ExecutionStatus.SUCCESS;
        }
        List<String> providedDependencyNames = mtaMetadata.getProvidedDependencyNames();
        String org = StepsUtil.getOrg(context);
        String space = StepsUtil.getSpace(context);
        CloudTarget newTarget = new CloudTarget(org, space);
        CloudTarget oldTarget = new CloudTarget(null, StepsUtil.getSpaceId(context));
        String oldMtaVersion = mtaMetadata.getMtaMetadata().getVersion().toString();
        List<ConfigurationEntry> publishedEntries = StepsUtil.getPublishedEntries(context);

        List<ConfigurationEntry> entriesToDelete = getEntriesToDelete(mtaId, oldMtaVersion, newTarget, oldTarget, providedDependencyNames,
            publishedEntries);
        for (ConfigurationEntry entry : entriesToDelete) {
            try {
                configurationEntryDao.remove(entry.getId());
            } catch (NotFoundException e) {
                getStepLogger().warn(Messages.COULD_NOT_DELETE_PROVIDED_DEPENDENCY, entry.getProviderId());
            }
        }
        getStepLogger().debug(Messages.DELETED_ENTRIES, JsonUtil.toJson(entriesToDelete, true));
        StepsUtil.setDeletedEntries(context, entriesToDelete);

        getStepLogger().debug(Messages.DISCONTINUED_CONFIGURATION_ENTRIES_FOR_APP_DELETED, existingApp.getName());
        return ExecutionStatus.SUCCESS;
    }

    private List<ConfigurationEntry> getEntriesToDelete(String mtaId, String mtaVersion, CloudTarget newTarget, CloudTarget oldTarget,
        List<String> providedDependencyNames, List<ConfigurationEntry> publishedEntries) {
        List<ConfigurationEntry> entriesWithNewTargetFormat = getEntries(mtaId, mtaVersion, newTarget);
        /**
         * TODO: The following line of code should be removed when compatibility with versions lower than 1.18.2 is not required. In these
         * versions, the configuration entries were stored with the space ID of the provider as a value for their target element. This was
         * identified as an issue, since restricted users could not see configuration entries for spaces, in which they had no roles
         * assigned. The following line ensures that entries published by old deploy service versions, are detected properly as discontinued
         * during redeploy (update) of the MTA that provided them.
         */
        List<ConfigurationEntry> entriesWithOldTargetFormat = getEntries(mtaId, mtaVersion, oldTarget);
        List<ConfigurationEntry> allEntriesForCurrentMta = ListUtil.merge(entriesWithNewTargetFormat, entriesWithOldTargetFormat);
        List<ConfigurationEntry> entriesForCurrentModule = getConfigurationEntriesWithProviderIds(allEntriesForCurrentMta,
            getProviderIds(mtaId, providedDependencyNames));
        return getEntriesNotUpdatedByThisProcess(entriesForCurrentModule, publishedEntries);
    }

    private List<ConfigurationEntry> getEntriesNotUpdatedByThisProcess(List<ConfigurationEntry> entriesForCurrentModule,
        List<ConfigurationEntry> publishedEntries) {
        return entriesForCurrentModule.stream().filter(entry -> !hasId(entry, publishedEntries)).collect(Collectors.toList());
    }

    private boolean hasId(ConfigurationEntry entry, List<ConfigurationEntry> publishedEntries) {
        return publishedEntries.stream().anyMatch(publishedEntry -> publishedEntry.getId() == entry.getId());
    }

    private List<String> getProviderIds(String mtaId, List<String> providedDependencyNames) {
        return providedDependencyNames.stream().map(
            providedDependencyName -> ConfigurationEntriesUtil.computeProviderId(mtaId, providedDependencyName)).collect(
                Collectors.toList());
    }

    private List<ConfigurationEntry> getConfigurationEntriesWithProviderIds(List<ConfigurationEntry> entries, List<String> providerIds) {
        return entries.stream().filter(entry -> hasProviderId(entry, providerIds)).collect(Collectors.toList());
    }

    private boolean hasProviderId(ConfigurationEntry entry, List<String> providerIds) {
        return providerIds.stream().anyMatch(providerId -> entry.getProviderId().equals(providerId));
    }

    private List<ConfigurationEntry> getEntries(String mtaId, String mtaVersion, CloudTarget target) {
        return configurationEntryDao.find(ConfigurationEntriesUtil.PROVIDER_NID, null, mtaVersion, target, null, mtaId);
    }

}
