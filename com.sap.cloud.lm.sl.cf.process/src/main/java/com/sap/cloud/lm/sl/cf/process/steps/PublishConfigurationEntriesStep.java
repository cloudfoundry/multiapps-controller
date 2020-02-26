package com.sap.cloud.lm.sl.cf.process.steps;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cp.security.credstore.client.CredentialStorage;
import com.sap.cp.security.credstore.client.CredentialStoreClientException;
import com.sap.cp.security.credstore.client.CredentialStoreFactory;
import com.sap.cp.security.credstore.client.CredentialStoreInstance;
import com.sap.cp.security.credstore.client.CredentialStoreNamespaceInstance;
import com.sap.cp.security.credstore.client.EnvCoordinates;
import com.sap.cp.security.credstore.client.PasswordCredential;

@Named("publishProvidedDependenciesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PublishConfigurationEntriesStep extends SyncFlowableStep {

    private final SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Inject
    ConfigurationEntryService configurationEntryService;

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

        List<ConfigurationEntry> publishedEntries = publish(entriesToPublish, execution.getContext());

        getStepLogger().debug(Messages.PUBLISHED_ENTRIES, secureSerializer.toJson(publishedEntries));
        StepsUtil.setPublishedEntries(execution.getContext(), publishedEntries);

        getStepLogger().debug(Messages.PUBLIC_PROVIDED_DEPENDENCIES_PUBLISHED);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(DelegateExecution context) {
        return Messages.ERROR_PUBLISHING_PUBLIC_PROVIDED_DEPENDENCIES;
    }

    private List<ConfigurationEntry> publish(List<ConfigurationEntry> entriesToPublish, DelegateExecution context) {
        List<ConfigurationEntry> publishedEntries = new ArrayList<>();
        for (ConfigurationEntry entry : entriesToPublish) {
            ConfigurationEntry publishedConfigurationEntry = publishConfigurationEntry(entry, context);
            publishedEntries.add(publishedConfigurationEntry);
        }
        return publishedEntries;
    }

    private ConfigurationEntry publishConfigurationEntry(ConfigurationEntry entry, DelegateExecution context) {
        infoConfigurationPublishment(entry);
        ConfigurationEntry currentEntry = getExistingEntry(entry);
        if (currentEntry == null) {
            ConfigurationEntriesUtil.addPasswordCredential(entry);
            return configurationEntryService.add(entry);
        } else {
            ConfigurationEntriesUtil.deletePasswordCredential(currentEntry);
            ConfigurationEntriesUtil.addPasswordCredential(entry);
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
