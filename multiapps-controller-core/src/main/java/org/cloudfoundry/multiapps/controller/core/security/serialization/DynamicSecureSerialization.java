package org.cloudfoundry.multiapps.controller.core.security.serialization;

import org.cloudfoundry.multiapps.controller.core.security.serialization.model.DeploymentDescriptorSerializer;
import org.cloudfoundry.multiapps.controller.core.security.serialization.model.ModuleSerializer;
import org.cloudfoundry.multiapps.controller.core.security.serialization.model.ProvidedDependencySerializer;
import org.cloudfoundry.multiapps.controller.core.security.serialization.model.RequiredDependencySerializer;
import org.cloudfoundry.multiapps.controller.core.security.serialization.model.ResourceSerializer;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.ProvidedDependency;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.model.VersionedEntity;

public final class DynamicSecureSerialization {

    private final SecureSerializerConfiguration secureSerializerConfiguration;

    public DynamicSecureSerialization(SecureSerializerConfiguration secureSerializerConfiguration) {
        this.secureSerializerConfiguration = secureSerializerConfiguration;
    }

    public String toJson(Object object) {
        SecureJsonSerializer secureJsonSerializer = createDynamicJsonSerializer(object);
        return secureJsonSerializer.serialize(object);
    }

    private SecureJsonSerializer createDynamicJsonSerializer(Object object) {
        SecureJsonSerializer secureJsonSerializer = createDynamicJsonSerializerForVersionedEntity(object);
        if (secureJsonSerializer == null) {
            return new SecureJsonSerializer(secureSerializerConfiguration);
        }

        return secureJsonSerializer;
    }

    private SecureJsonSerializer createDynamicJsonSerializerForVersionedEntity(Object object) {
        if (object instanceof VersionedEntity) {
            return createDynamicJsonSerializerForVersionedEntity((VersionedEntity) object);
        }

        return null;
    }

    private SecureJsonSerializer createDynamicJsonSerializerForVersionedEntity(VersionedEntity versionedEntity) {
        if (versionedEntity.getMajorSchemaVersion() < 3) {
            return null;
        }

        if (versionedEntity instanceof DeploymentDescriptor) {
            return new DeploymentDescriptorSerializer(secureSerializerConfiguration);
        }

        if (versionedEntity instanceof Module) {
            return new ModuleSerializer(secureSerializerConfiguration);
        }

        if (versionedEntity instanceof ProvidedDependency) {
            return new ProvidedDependencySerializer(secureSerializerConfiguration);
        }

        if (versionedEntity instanceof RequiredDependency) {
            return new RequiredDependencySerializer(secureSerializerConfiguration);
        }

        if (versionedEntity instanceof Resource) {
            return new ResourceSerializer(secureSerializerConfiguration);
        }

        return null;
    }

}
