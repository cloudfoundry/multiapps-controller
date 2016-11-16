package com.sap.cloud.lm.sl.cf.core.helpers.v1_0;

import static java.text.MessageFormat.format;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.model.ResolvedConfigurationReference;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.ElementContext;
import com.sap.cloud.lm.sl.mta.model.Visitor;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Resource;

public class ConfigurationReferencesResolver extends Visitor {

    protected ConfigurationReferenceResolver referenceResolver;
    protected ConfigurationEntryDao dao;
    protected Map<String, ResolvedConfigurationReference> resolvedReferences = new TreeMap<>();
    protected ConfigurationFilterParser filterParser;
    protected BiFunction<String, String, String> spaceIdSupplier;

    public ConfigurationReferencesResolver(ConfigurationEntryDao dao, ConfigurationFilterParser filterParser,
        BiFunction<String, String, String> spaceIdSupplier) {
        this.dao = dao;
        this.referenceResolver = createReferenceResolver(dao);
        this.filterParser = filterParser;
        this.spaceIdSupplier = spaceIdSupplier;
    }

    protected ConfigurationReferenceResolver createReferenceResolver(ConfigurationEntryDao dao) {
        return new ConfigurationReferenceResolver(dao);
    }

    public void resolve(DeploymentDescriptor descriptor) throws ContentException {
        descriptor.accept(this);
        insertResolvedResources(descriptor);
    }

    public List<String> getExpandedProperties() {
        return Collections.emptyList();
    }

    public Map<String, ResolvedConfigurationReference> getResolvedReferences() {
        return resolvedReferences;
    }

    protected void insertResolvedResources(DeploymentDescriptor descriptor) throws ContentException {
        descriptor.setResources1_0(getResolvedResources(descriptor));
        updateReferencesToResolvedResources(descriptor);
    }

    protected List<Resource> getResolvedResources(DeploymentDescriptor descriptor) {
        return descriptor.getResources1_0().stream().flatMap((resource) -> getResolvedResources(resource).stream()).collect(
            Collectors.toList());
    }

    protected List<Resource> getResolvedResources(Resource resource) {
        ResolvedConfigurationReference reference = resolvedReferences.get(resource.getName());
        if (reference != null) {
            return reference.getResolvedResources();
        }
        return Arrays.asList(resource);
    }

    protected void updateReferencesToResolvedResources(DeploymentDescriptor descriptor) throws ContentException {
        for (String resolvedResourceName : resolvedReferences.keySet()) {
            makeSureIsResolvedToSingleResource(resolvedResourceName, (resolvedReferences.get(resolvedResourceName)).getResolvedResources());
        }
    }

    protected void makeSureIsResolvedToSingleResource(String resolvedResourceName, List<Resource> resultingResourcess)
        throws ContentException {
        if (resultingResourcess.size() > 1) {
            throw new ContentException(format(Messages.MULTIPLE_CONFIGURATION_ENTRIES_WERE_FOUND, resolvedResourceName));
        } else if (resultingResourcess.isEmpty()) {
            throw new ContentException(format(Messages.NO_CONFIGURATION_ENTRIES_WERE_FOUND, resolvedResourceName));
        }
    }

    @Override
    public void visit(ElementContext context, Resource resource) throws ContentException {
        ConfigurationFilter configurationFilter = filterParser.parse(resource);
        if (configurationFilter != null) {
            ResolvedConfigurationReference resolvedReference = new ResolvedConfigurationReference(configurationFilter, resource,
                referenceResolver.resolve(resource, configurationFilter));
            resolvedReferences.put(resource.getName(), resolvedReference);
        }
    }

}
