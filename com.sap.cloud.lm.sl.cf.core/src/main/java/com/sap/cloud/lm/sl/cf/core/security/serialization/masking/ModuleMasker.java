package com.sap.cloud.lm.sl.cf.core.security.serialization.masking;

import com.sap.cloud.lm.sl.mta.model.Module;
import com.sap.cloud.lm.sl.mta.model.ProvidedDependency;
import com.sap.cloud.lm.sl.mta.model.RequiredDependency;

public class ModuleMasker extends AbstractMasker<Module> {

    private final ProvidedDependencyMasker providedMasker = new ProvidedDependencyMasker();
    private final RequiredDependencyMasker requiredMasker = new RequiredDependencyMasker();

    @Override
    public void mask(Module module) {
        maskProperties(module);
        maskParameters(module);
        for (ProvidedDependency providedDependency : module.getProvidedDependencies()) {
            providedMasker.mask(providedDependency);
        }
        for (RequiredDependency requiredDependency : module.getRequiredDependencies()) {
            requiredMasker.mask(requiredDependency);
        }
    }
}
