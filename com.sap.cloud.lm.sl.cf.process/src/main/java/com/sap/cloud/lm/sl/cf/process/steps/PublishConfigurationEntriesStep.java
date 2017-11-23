package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("publishProvidedDependenciesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PublishConfigurationEntriesStep extends SyncActivitiStep {

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Inject
    ConfigurationEntryDao configurationEntryDao;

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws SLException {
        getStepLogger().logActivitiTask();

        CloudApplicationExtended app = StepsUtil.getApp(execution.getContext());

        try {
            getStepLogger().info(MessageFormat.format(Messages.PUBLISHING_PUBLIC_PROVIDED_DEPENDENCIES, app.getName()));

            Map<String, List<ConfigurationEntry>> entriesToPublish = StepsUtil.getConfigurationEntriesToPublish(execution.getContext());

            List<ConfigurationEntry> entriesToPublishPerApp = entriesToPublish.get(app.getName());
            List<ConfigurationEntry> publishedEntries = publish(entriesToPublishPerApp);

            getStepLogger().debug(Messages.PUBLISHED_ENTRIES, secureSerializer.toJson(publishedEntries));
            StepsUtil.setPublishedEntries(execution.getContext(), publishedEntries);

            getStepLogger().debug(Messages.PUBLIC_PROVIDED_DEPENDENCIES_PUBLISHED);
            return StepPhase.DONE;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_PUBLISHING_PUBLIC_PROVIDED_DEPENDENCIES);
            throw e;
        }
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
        ConfigurationEntry currentEntry = getExistingEntry(entry);
        if (currentEntry == null) {
            return configurationEntryDao.add(entry);
        } else {
            return configurationEntryDao.update(currentEntry.getId(), entry);
        }
    }

    private ConfigurationEntry getExistingEntry(ConfigurationEntry targetEntry) {
        List<ConfigurationEntry> existingEntries = configurationEntryDao.find(targetEntry.getProviderNid(), targetEntry.getProviderId(),
            targetEntry.getProviderVersion().toString(), targetEntry.getTargetSpace(), Collections.emptyMap(), null);
        return existingEntries.isEmpty() ? null : existingEntries.get(0);
    }

}
