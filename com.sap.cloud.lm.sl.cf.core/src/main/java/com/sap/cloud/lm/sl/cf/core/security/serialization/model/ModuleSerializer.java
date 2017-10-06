package com.sap.cloud.lm.sl.cf.core.security.serialization.model;

import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureJsonSerializer;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializerConfiguration;
import com.sap.cloud.lm.sl.cf.core.security.serialization.masking.ModuleMasker;
import com.sap.cloud.lm.sl.mta.model.v3_1.Module;

public class ModuleSerializer extends SecureJsonSerializer {

    ModuleMasker masker = new ModuleMasker();

    public ModuleSerializer(SecureSerializerConfiguration configuration) {
        super(configuration);
    }

    @Override
    public String serialize(Object object) {
        Module clonedModule = ((Module) object).copyOf();
        masker.mask(clonedModule);
        return super.serialize(clonedModule);
    }
}
