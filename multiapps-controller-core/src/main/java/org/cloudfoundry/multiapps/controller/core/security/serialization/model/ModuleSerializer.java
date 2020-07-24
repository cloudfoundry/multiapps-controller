package org.cloudfoundry.multiapps.controller.core.security.serialization.model;

import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureJsonSerializer;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerializerConfiguration;
import org.cloudfoundry.multiapps.controller.core.security.serialization.masking.ModuleMasker;
import org.cloudfoundry.multiapps.mta.model.Module;

public class ModuleSerializer extends SecureJsonSerializer {

    final ModuleMasker masker = new ModuleMasker();

    public ModuleSerializer(SecureSerializerConfiguration configuration) {
        super(configuration);
    }

    @Override
    public String serialize(Object object) {
        Module clonedModule = Module.copyOf((Module) object);
        masker.mask(clonedModule);
        return super.serialize(clonedModule);
    }
}
