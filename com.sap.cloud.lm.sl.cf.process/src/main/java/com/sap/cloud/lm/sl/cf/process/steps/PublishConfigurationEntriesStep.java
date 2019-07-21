package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.util.CommonUtil;

@Component("publishProvidedDependenciesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PublishConfigurationEntriesStep extends SyncFlowableStep {

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Inject
    ConfigurationEntryDao configurationEntryDao;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        CloudApplicationExtended app = StepsUtil.getApp(execution.getContext());

        getStepLogger().debug(MessageFormat.format(Messages.PUBLISHING_PUBLIC_PROVIDED_DEPENDENCIES, app.getName()));

        List<ConfigurationEntry> entriesToPublish = StepsUtil.getConfigurationEntriesToPublish(execution.getContext());

        if (CollectionUtils.isEmpty(entriesToPublish)) {
            StepsUtil.setPublishedEntries(execution.getContext(), Collections.emptyList());
            getStepLogger().debug(Messages.NO_PUBLIC_PROVIDED_DEPENDENCIES_FOR_PUBLISHING);
            return StepPhase.DONE;
        }

        List<ConfigurationEntry> publishedEntries = publish(entriesToPublish);

        getStepLogger().debug(Messages.PUBLISHED_ENTRIES, secureSerializer.toJson(publishedEntries));
        StepsUtil.setPublishedEntries(execution.getContext(), publishedEntries);

        getStepLogger().debug(Messages.PUBLIC_PROVIDED_DEPENDENCIES_PUBLISHED);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_PUBLISHING_PUBLIC_PROVIDED_DEPENDENCIES;
    }

    private List<ConfigurationEntry> publish(List<ConfigurationEntry> entriesToPublish) {
        List<ConfigurationEntry> publishedEntries = new ArrayList<>();
        for (ConfigurationEntry entry : entriesToPublish) {
            ConfigurationEntry publishedConfigurationEntry = publishConfigurationEntry(entry);
            publishedEntries.add(publishedConfigurationEntry);
        }
        return publishedEntries;
    }

    private ConfigurationEntry publishConfigurationEntry(ConfigurationEntry entry) {
        infoConfigurationPublishment(entry);
        ConfigurationEntry currentEntry = getExistingEntry(entry);
        if (currentEntry == null) {
            configurationEntryDao.add(entry);
            return entry;
        } else {
            return configurationEntryDao.update(currentEntry.getId(), entry);
        }
    }

    private void infoConfigurationPublishment(ConfigurationEntry entry) {
        if (!CommonUtil.isNullOrEmpty(entry.getContent())) {
            getStepLogger().info(MessageFormat.format(Messages.PUBLISHING_PUBLIC_PROVIDED_DEPENDENCY, entry.getProviderId()));
        }
    }

    private ConfigurationEntry getExistingEntry(ConfigurationEntry targetEntry) {
        List<ConfigurationEntry> existingEntries = configurationEntryDao.find(targetEntry.getProviderNid(), targetEntry.getProviderId(),
            targetEntry.getProviderVersion()
                .toString(),
            targetEntry.getTargetSpace(), Collections.emptyMap(), null);
        return existingEntries.isEmpty() ? null : existingEntries.get(0);
    }

}
