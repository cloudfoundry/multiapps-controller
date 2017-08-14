package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.StepMetadata;

@Component("publishProvidedDependenciesStep")
public class PublishConfigurationEntriesStep extends AbstractXS2ProcessStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublishConfigurationEntriesStep.class);

    private SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Inject
    ConfigurationEntryDao configurationEntryDao;

    public static StepMetadata getMetadata() {
        return StepMetadata.builder().id("publishProvidedDependenciesTask").displayName("Publish Provided Dependencies").description(
            "Publish Provided Dependencies").build();
    }

    @Override
    protected ExecutionStatus executeStepInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);

        CloudApplicationExtended app = StepsUtil.getApp(context);

        try {
            info(context, MessageFormat.format(Messages.PUBLISHING_PUBLIC_PROVIDED_DEPENDENCIES, app.getName()), LOGGER);

            Map<String, List<ConfigurationEntry>> entriesToPublish = StepsUtil.getConfigurationEntriesToPublish(context);

            List<ConfigurationEntry> entriesToPublishPerApp = entriesToPublish.get(app.getName());
            List<ConfigurationEntry> publishedEntries = publish(entriesToPublishPerApp);

            debug(context, format(Messages.PUBLISHED_ENTRIES, secureSerializer.toJson(publishedEntries)), LOGGER);
            StepsUtil.setPublishedEntries(context, publishedEntries);

            debug(context, Messages.PUBLIC_PROVIDED_DEPENDENCIES_PUBLISHED, LOGGER);
            return ExecutionStatus.SUCCESS;
        } catch (SLException e) {
            error(context, Messages.ERROR_PUBLISHING_PUBLIC_PROVIDED_DEPENDENCIES, e, LOGGER);
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
