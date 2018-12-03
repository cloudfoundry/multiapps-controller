package com.sap.cloud.lm.sl.cf.core.helpers.v1;

import static java.text.MessageFormat.format;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ResolvedConfigurationReference;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.ElementContext;
import com.sap.cloud.lm.sl.mta.model.Visitor;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;

public class ConfigurationReferencesResolver extends Visitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationReferencesResolver.class);

    protected ConfigurationReferenceResolver configurationResolver;
    protected ConfigurationEntryDao dao;
    protected Map<String, ResolvedConfigurationReference> resolvedReferences = new TreeMap<>();
    protected ConfigurationFilterParser filterParser;
    protected BiFunction<String, String, String> spaceIdSupplier;
    protected CloudTarget cloudTarget;
    protected ApplicationConfiguration configuration;

    public ConfigurationReferencesResolver(ConfigurationEntryDao dao, ConfigurationFilterParser filterParser,
        BiFunction<String, String, String> spaceIdSupplier, CloudTarget cloudTarget, ApplicationConfiguration configuration) {
        this.dao = dao;
        this.filterParser = filterParser;
        this.spaceIdSupplier = spaceIdSupplier;
        this.cloudTarget = cloudTarget;
        this.configuration = configuration;
        this.configurationResolver = createReferenceResolver(dao);
    }

    protected ConfigurationReferenceResolver createReferenceResolver(ConfigurationEntryDao dao) {
        return new ConfigurationReferenceResolver(dao, configuration);
    }

    public void resolve(DeploymentDescriptor descriptor) {
        descriptor.accept(this);
        insertResolvedResources(descriptor);
    }

    public List<String> getExpandedProperties() {
        return Collections.emptyList();
    }

    public Map<String, ResolvedConfigurationReference> getResolvedReferences() {
        return resolvedReferences;
    }

    protected void insertResolvedResources(DeploymentDescriptor descriptor) {
        descriptor.setResources1(getResolvedResources(descriptor));
        updateReferencesToResolvedResources(descriptor);
    }

    protected List<Resource> getResolvedResources(DeploymentDescriptor descriptor) {
        return descriptor.getResources1()
            .stream()
            .flatMap(resource -> getResolvedResources(resource).stream())
            .collect(Collectors.toList());
    }

    protected List<Resource> getResolvedResources(Resource resource) {
        ResolvedConfigurationReference reference = resolvedReferences.get(resource.getName());
        if (reference != null) {
            return reference.getResolvedResources();
        }
        return Arrays.asList(resource);
    }

    protected void updateReferencesToResolvedResources(DeploymentDescriptor descriptor) {
        for (String resolvedResourceName : resolvedReferences.keySet()) {
            makeSureIsResolvedToSingleResource(resolvedResourceName, (resolvedReferences.get(resolvedResourceName)).getResolvedResources());
        }
    }

    protected void makeSureIsResolvedToSingleResource(String resolvedResourceName, List<Resource> resultingResources) {
        if (resultingResources.size() > 1) {
            LOGGER.debug(Messages.MULTIPLE_CONFIGURATION_ENTRIES, resolvedResourceName, resultingResources);
            throw new ContentException(format(Messages.MULTIPLE_CONFIGURATION_ENTRIES_WERE_FOUND, resolvedResourceName));
        } else if (resultingResources.isEmpty()) {
            throw new ContentException(format(Messages.NO_CONFIGURATION_ENTRIES_WERE_FOUND, resolvedResourceName));
        }
    }

    @Override
    public void visit(ElementContext context, Resource sourceResource) {
        ConfigurationFilter configurationFilter = filterParser.parse(sourceResource);
        if (configurationFilter == null) {
            // resource is not a config reference.
            return;
        }
        List<Resource> resolvedResources = configurationResolver.resolve(sourceResource, configurationFilter, cloudTarget);
        ResolvedConfigurationReference resolvedReference = new ResolvedConfigurationReference(configurationFilter, sourceResource,
            resolvedResources);
        resolvedReferences.put(sourceResource.getName(), resolvedReference);
    }

}
