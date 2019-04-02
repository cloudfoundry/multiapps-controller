package com.sap.cloud.lm.sl.cf.core.security.serialization;

import java.util.Collection;

import com.sap.cloud.lm.sl.cf.core.security.serialization.model.DeploymentDescriptorSerializer;
import com.sap.cloud.lm.sl.cf.core.security.serialization.model.ModuleSerializer;
import com.sap.cloud.lm.sl.cf.core.security.serialization.model.ProvidedDependencySerializer;
import com.sap.cloud.lm.sl.cf.core.security.serialization.model.RequiredDependencySerializer;
import com.sap.cloud.lm.sl.cf.core.security.serialization.model.ResourceSerializer;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;
import com.sap.cloud.lm.sl.mta.model.ProvidedDependency;
import com.sap.cloud.lm.sl.mta.model.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.Resource;
import com.sap.cloud.lm.sl.mta.model.VersionedEntity;

public class SecureSerializationFacade {

    private SecureSerializerConfiguration configuration = new SecureSerializerConfiguration();

    public SecureSerializationFacade setSensitiveElementNames(Collection<String> sensitiveElementNames) {
        this.configuration.setSensitiveElementNames(sensitiveElementNames);
        return this;
    }

    public SecureSerializationFacade setSensitiveElementPaths(Collection<String> sensitiveElementPaths) {
        this.configuration.setSensitiveElementPaths(sensitiveElementPaths);
        return this;
    }

    public SecureSerializationFacade setFormattedOutput(boolean formattedOutput) {
        this.configuration.setFormattedOutput(formattedOutput);
        return this;
    }

    public String toYaml(Object object) {
        throw new UnsupportedOperationException();
    }

    public String toJson(Object object) {
        return getSerializer(object).serialize(object);
    }

    private SecureJsonSerializer getSerializer(Object object) {
        if (object instanceof VersionedEntity) {
            VersionedEntity versionedEntity = (VersionedEntity) object;
            if (versionedEntity.getMajorSchemaVersion() >= 3) {
                return getVersionedEntitySerializer(versionedEntity);
            }
        }
        return new SecureJsonSerializer(configuration);
    }

    private SecureJsonSerializer getVersionedEntitySerializer(VersionedEntity versionedEntity) {
        if (versionedEntity instanceof DeploymentDescriptor) {
            return new DeploymentDescriptorSerializer(configuration);
        }
        if (versionedEntity instanceof Module) {
            return new ModuleSerializer(configuration);
        }
        if (versionedEntity instanceof Resource) {
            return new ResourceSerializer(configuration);
        }
        if (versionedEntity instanceof ProvidedDependency) {
            return new ProvidedDependencySerializer(configuration);
        }
        if (versionedEntity instanceof RequiredDependency) {
            return new RequiredDependencySerializer(configuration);
        }
        return new SecureJsonSerializer(configuration);
    }

    public String toXml(Object object) {
        throw new UnsupportedOperationException();
    }

}
