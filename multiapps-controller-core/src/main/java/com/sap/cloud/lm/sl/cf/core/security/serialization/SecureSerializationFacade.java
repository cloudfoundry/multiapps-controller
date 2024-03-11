package com.sap.cloud.lm.sl.cf.core.security.serialization;

import java.util.Collection;

import com.sap.cloud.lm.sl.cf.core.security.serialization.model.DeploymentDescriptorSerializer;
import com.sap.cloud.lm.sl.cf.core.security.serialization.model.ModuleSerializer;
import com.sap.cloud.lm.sl.cf.core.security.serialization.model.ProvidedDependencySerializer;
import com.sap.cloud.lm.sl.cf.core.security.serialization.model.RequiredDependencySerializer;
import com.sap.cloud.lm.sl.cf.core.security.serialization.model.ResourceSerializer;
import com.sap.cloud.lm.sl.mta.model.v3.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3.Module;
import com.sap.cloud.lm.sl.mta.model.v3.ProvidedDependency;
import com.sap.cloud.lm.sl.mta.model.v3.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.v3.Resource;

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
        if (object instanceof DeploymentDescriptor) {
            return new DeploymentDescriptorSerializer(configuration);
        }
        if (object instanceof Module) {
            return new ModuleSerializer(configuration);
        }
        if (object instanceof Resource) {
            return new ResourceSerializer(configuration);
        }
        if (object instanceof ProvidedDependency) {
            return new ProvidedDependencySerializer(configuration);
        }
        if (object instanceof RequiredDependency) {
            return new RequiredDependencySerializer(configuration);
        }
        return new SecureJsonSerializer(configuration);
    }

    public String toXml(Object object) {
        throw new UnsupportedOperationException();
    }

}
