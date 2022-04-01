package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.flowable.FlowableFacade;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Component("deleteDiscontinuedConfigurationEntriesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DeleteDiscontinuedConfigurationEntriesStep extends SyncFlowableStep {

    @Inject
    private ConfigurationEntryDao configurationEntryDao;

    @Inject
    private FlowableFacade flowableFacade;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        getStepLogger().debug(Messages.DELETING_PUBLISHED_DEPENDENCIES);
        String mtaId = (String) execution.getContext()
                                         .getVariable(Constants.PARAM_MTA_ID);
        String org = StepsUtil.getOrg(execution.getContext());
        String space = StepsUtil.getSpace(execution.getContext());
        CloudTarget newTarget = new CloudTarget(org, space);
        CloudTarget oldTarget = new CloudTarget(null, StepsUtil.getSpaceId(execution.getContext()));

        List<ConfigurationEntry> publishedConfigurationEntries = StepsUtil.getPublishedEntriesFromSubProcesses(execution.getContext(),
                                                                                                               flowableFacade);

        List<ConfigurationEntry> entriesToDelete = getEntriesToDelete(mtaId, newTarget, oldTarget, publishedConfigurationEntries);
        for (ConfigurationEntry entry : entriesToDelete) {
            try {
                getStepLogger().info(MessageFormat.format(Messages.DELETING_DISCONTINUED_DEPENDENCY_0, entry.getProviderId()));
                configurationEntryDao.remove(entry.getId());
            } catch (NotFoundException e) {
                getStepLogger().warn(Messages.COULD_NOT_DELETE_PROVIDED_DEPENDENCY, entry.getProviderId());
            }
        }
        getStepLogger().debug(Messages.DELETED_ENTRIES, JsonUtil.toJson(entriesToDelete, true));
        StepsUtil.setDeletedEntries(execution.getContext(), entriesToDelete);

        getStepLogger().debug(Messages.PUBLISHED_DEPENDENCIES_DELETED);
        return StepPhase.DONE;
    }

    private List<ConfigurationEntry> getEntriesToDelete(String mtaId, CloudTarget newTarget, CloudTarget oldTarget,
                                                        List<ConfigurationEntry> publishedEntries) {
        List<ConfigurationEntry> entriesWithNewTargetFormat = getEntriesToDelete(mtaId, newTarget, publishedEntries);
        /**
         * TODO: The following line of code should be removed when compatibility with versions lower than 1.18.2 is not required. In these
         * versions, the configuration entries were stored with the space ID of the provider as a value for their target element. This was
         * identified as an issue, since restricted users could not see configuration entries for spaces, in which they had no roles
         * assigned. The following line ensures that entries published by old deploy service versions, are detected properly as discontinued
         * during redeploy (update) of the MTA that provided them.
         */
        List<ConfigurationEntry> entriesWithOldTargetFormat = getEntriesToDelete(mtaId, oldTarget, publishedEntries);
        return ListUtils.union(entriesWithNewTargetFormat, entriesWithOldTargetFormat);
    }

    private List<ConfigurationEntry> getEntriesToDelete(String mtaId, CloudTarget target, List<ConfigurationEntry> publishedEntries) {
        List<ConfigurationEntry> allEntriesForCurrentMta = getEntries(mtaId, target);
        List<Long> publishedEntryIds = getEntryIds(publishedEntries);
        return allEntriesForCurrentMta.stream()
                                      .filter(entry -> !publishedEntryIds.contains(entry.getId()))
                                      .collect(Collectors.toList());
    }

    private List<ConfigurationEntry> getEntries(String mtaId, CloudTarget target) {
        return configurationEntryDao.find(ConfigurationEntriesUtil.PROVIDER_NID, null, null, target, null, mtaId);
    }

    private List<Long> getEntryIds(List<ConfigurationEntry> configurationEntries) {
        return configurationEntries.stream()
                                   .map(ConfigurationEntry::getId)
                                   .collect(Collectors.toList());
    }

}
