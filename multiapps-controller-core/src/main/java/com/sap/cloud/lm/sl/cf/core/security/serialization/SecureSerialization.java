package com.sap.cloud.lm.sl.cf.core.security.serialization;

import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.ProvidedDependency;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.model.VersionedEntity;

import com.sap.cloud.lm.sl.cf.core.security.serialization.model.DeploymentDescriptorSerializer;
import com.sap.cloud.lm.sl.cf.core.security.serialization.model.ModuleSerializer;
import com.sap.cloud.lm.sl.cf.core.security.serialization.model.ProvidedDependencySerializer;
import com.sap.cloud.lm.sl.cf.core.security.serialization.model.RequiredDependencySerializer;
import com.sap.cloud.lm.sl.cf.core.security.serialization.model.ResourceSerializer;

public final class SecureSerialization {

    private static final SecureSerializerConfiguration CONFIGURATION = new SecureSerializerConfiguration();

    private SecureSerialization() {
    }

    public static String toJson(Object object) {
        return createJsonSerializer(object).serialize(object);
    }

    private static SecureJsonSerializer createJsonSerializer(Object object) {
        SecureJsonSerializer secureJsonSerializer = createJsonSerializerForVersionedEntity(object);
        if (secureJsonSerializer == null) {
            return new SecureJsonSerializer(CONFIGURATION);
        }
        return secureJsonSerializer;
    }

    private static SecureJsonSerializer createJsonSerializerForVersionedEntity(Object object) {
        if (object instanceof VersionedEntity) {
            return createJsonSerializerForVersionedEntity((VersionedEntity) object);
        }
        return null;
    }

    private static SecureJsonSerializer createJsonSerializerForVersionedEntity(VersionedEntity versionedEntity) {
        if (versionedEntity.getMajorSchemaVersion() < 3) {
            return null;
        }
        if (versionedEntity instanceof DeploymentDescriptor) {
            return new DeploymentDescriptorSerializer(CONFIGURATION);
        }
        if (versionedEntity instanceof Module) {
            return new ModuleSerializer(CONFIGURATION);
        }
        if (versionedEntity instanceof ProvidedDependency) {
            return new ProvidedDependencySerializer(CONFIGURATION);
        }
        if (versionedEntity instanceof RequiredDependency) {
            return new RequiredDependencySerializer(CONFIGURATION);
        }
        if (versionedEntity instanceof Resource) {
            return new ResourceSerializer(CONFIGURATION);
        }
        return null;
    }

}
