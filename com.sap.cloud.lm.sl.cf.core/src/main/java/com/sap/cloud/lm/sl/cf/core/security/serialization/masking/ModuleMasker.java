package com.sap.cloud.lm.sl.cf.core.security.serialization.masking;

import com.sap.cloud.lm.sl.mta.model.v3_1.Module;
import com.sap.cloud.lm.sl.mta.model.v3_1.ProvidedDependency;
import com.sap.cloud.lm.sl.mta.model.v3_1.RequiredDependency;

public class ModuleMasker extends AbstractMasker<Module> {

    private ProvidedDependencyMasker providedMasker = new ProvidedDependencyMasker();
    private RequiredDependencyMasker requiredMasker = new RequiredDependencyMasker();

    @Override
    public void mask(Module module) {
        maskProperties(module);
        maskParameters(module);
        for (ProvidedDependency providedDependency : module.getProvidedDependencies3_1()) {
            providedMasker.mask(providedDependency);
        }
        for (RequiredDependency requiredDependency : module.getRequiredDependencies3_1()) {
            requiredMasker.mask(requiredDependency);
        }
    }
}
