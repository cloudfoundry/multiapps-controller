package com.sap.cloud.lm.sl.cf.core.cf.detect.mapping;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.v3.Metadata;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaResource;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
 
@Component
public class AppMetadataMapper extends MetadataMapper {

    public static final String RESOURCES = "resources";
    public static final String PROVIDED_DEPENDENCY_NAMES = "provided_dependency_names";
    public static final String MODULE_NAME = "module_name";
    public static final String URIS = "uris";

    public String getModuleName(Metadata metadata) {
        return metadata.getAnnotations().get(MODULE_NAME);
    }

    public List<String> getProvidedDependencyNames(Metadata metadata) {
        String providedDependencyNamesJson = metadata.getAnnotations().get(PROVIDED_DEPENDENCY_NAMES);
        return providedDependencyNamesJson == null ? null : JsonUtil.convertJsonToList(providedDependencyNamesJson, new TypeReference<List<String>>() {});
    }

    public List<DeployedMtaResource> getResource(Metadata metadata) {
        String resourcesJson = metadata.getAnnotations().get(RESOURCES);
        return resourcesJson == null ? null : JsonUtil.fromJson(resourcesJson, new TypeReference<List<DeployedMtaResource>>() {});
    }

    private List<String> getUris(Metadata metadata) {
        String urisJson = metadata.getAnnotations().get(URIS);
        return urisJson == null ? null : JsonUtil.fromJson(urisJson, new TypeReference<List<String>>() {});
    }
    
    public ApplicationMtaMetadata mapMetadata(CloudApplication app) {
        if (app.getMetadata() == null) {
            return null;
        }

        DeployedMtaMetadata mtaMetadata = new DeployedMtaMetadata();
        mtaMetadata.setId(getMtaId(app.getV3Metadata()));
        mtaMetadata.setVersion(getMtaVersion(app.getV3Metadata()));
        String moduleName = getModuleName(app.getV3Metadata());
        List<String> uris = getUris(app.getV3Metadata());
        List<String> providedDependencyNames = getProvidedDependencyNames(app.getV3Metadata());
        List<DeployedMtaResource> deployedMtaResources = getResource(app.getV3Metadata());
        
        DeployedMtaModule module = DeployedMtaModule.builder()
                         .withModuleName(moduleName)
                         .withProvidedDependencyNames(providedDependencyNames)
                         .withUris(uris)
                         .withServices(deployedMtaResources)
                         .build();
        
        return ApplicationMtaMetadata.builder().withModule(module).withMtaMetadata(mtaMetadata).build();
    }
}
