package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

@Named("publishProvidedDependenciesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PublishConfigurationEntriesStep extends SyncFlowableStep {

    private final SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Inject
    private ConfigurationEntryService configurationEntryService;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);

        getStepLogger().debug(MessageFormat.format(Messages.PUBLISHING_PUBLIC_PROVIDED_DEPENDENCIES, app.getName()));

        List<ConfigurationEntry> entriesToPublish = context.getVariable(Variables.CONFIGURATION_ENTRIES_TO_PUBLISH);

        if (CollectionUtils.isEmpty(entriesToPublish)) {
            context.setVariable(Variables.PUBLISHED_ENTRIES, Collections.emptyList());
            getStepLogger().debug(Messages.NO_PUBLIC_PROVIDED_DEPENDENCIES_FOR_PUBLISHING);
            return StepPhase.DONE;
        }

        List<ConfigurationEntry> publishedEntries = publish(entriesToPublish);

        getStepLogger().debug(Messages.PUBLISHED_ENTRIES, secureSerializer.toJson(publishedEntries));
        context.setVariable(Variables.PUBLISHED_ENTRIES, publishedEntries);

        getStepLogger().debug(Messages.PUBLIC_PROVIDED_DEPENDENCIES_PUBLISHED);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_PUBLISHING_PUBLIC_PROVIDED_DEPENDENCIES;
    }

    private List<ConfigurationEntry> publish(List<ConfigurationEntry> entriesToPublish) {
        return entriesToPublish.stream()
                               .map(this::publishConfigurationEntry)
                               .collect(Collectors.toList());
    }

    private ConfigurationEntry publishConfigurationEntry(ConfigurationEntry entry) {
        infoConfigurationPublishment(entry);
        ConfigurationEntry currentEntry = getExistingEntry(entry);
        if (currentEntry == null) {
            return configurationEntryService.add(entry);
        } else {
            return configurationEntryService.update(currentEntry.getId(), entry);
        }
    }

    private void infoConfigurationPublishment(ConfigurationEntry entry) {
        if (!ObjectUtils.isEmpty(entry.getContent())) {
            getStepLogger().info(MessageFormat.format(Messages.PUBLISHING_PUBLIC_PROVIDED_DEPENDENCY, entry.getProviderId()));
        }
    }

    private ConfigurationEntry getExistingEntry(ConfigurationEntry targetEntry) {
        List<ConfigurationEntry> existingEntries = configurationEntryService.createQuery()
                                                                            .providerNid(targetEntry.getProviderNid())
                                                                            .providerId(targetEntry.getProviderId())
                                                                            .version(targetEntry.getProviderVersion()
                                                                                                .toString())
                                                                            .target(targetEntry.getTargetSpace())
                                                                            .list();
        return existingEntries.isEmpty() ? null : existingEntries.get(0);
    }

}
