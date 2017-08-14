package com.sap.cloud.lm.sl.cf.core.helpers.v1_0;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.model.ZdmActionEnum;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Module;

public class ZdmHelper {

    public List<Module> getZdmHdiDeployerModulesNotInInstallAction(DeploymentDescriptor descriptor, int majorVersion, int minorVersion) {
        List<Module> zdmHdiDeployerModules = getHdiDeployerModulesInZdmMode(descriptor, majorVersion, minorVersion);
        List<Module> zdmHdiDeployerModulesInActionInstall = new ArrayList<>();

        for (Module module : zdmHdiDeployerModules) {
            if (!isInZdmActionInstall(module)) {
                zdmHdiDeployerModulesInActionInstall.add(module);
            }
        }

        return zdmHdiDeployerModulesInActionInstall;
    }

    private static boolean isInZdmActionInstall(Module module) {
        Map<String, Object> properties = module.getProperties();
        for (Entry<String, Object> entry : properties.entrySet()) {
            if (!entry.getKey().equals(ZdmActionEnum.ZDM_ACTION.toString())) {
                continue;
            }
            if (entry.getValue().equals(ZdmActionEnum.INSTALL.toString())) {
                return true;
            }
        }
        return false;
    }

    public List<Module> getHdiDeployerModulesInZdmMode(DeploymentDescriptor descriptor, int majorVersion, int minorVersion) {
        List<Module> modules = descriptor.getModules1_0();
        List<Module> zdmHdiDeployerModules = new ArrayList<>();

        for (Module module : modules) {
            if (module.getType().equals(com.sap.cloud.lm.sl.cf.core.model.ModuleTypeEnum.HDI_ZDM.toString())) {
                zdmHdiDeployerModules.add(module);
                continue;
            }
            if (!module.getType().equals(com.sap.cloud.lm.sl.cf.core.model.ModuleTypeEnum.HDI.toString())) {
                continue;
            }
            if (containsZdmModeParameter(module, majorVersion, minorVersion)) {
                zdmHdiDeployerModules.add(module);
                continue;
            }
        }

        return zdmHdiDeployerModules;
    }

    private boolean containsZdmModeParameter(Module module, int majorVersion, int minorVersion) {
        HandlerFactory handlerFactory = new HandlerFactory(majorVersion, minorVersion);
        PropertiesAccessor pA = handlerFactory.getPropertiesAccessor();
        Map<String, Object> parameters = pA.getParameters(module);

        for (Entry<String, Object> entry : parameters.entrySet()) {
            if (!entry.getKey().equals(SupportedParameters.ZDM_MODE.toString())) {
                continue;
            }
            Boolean isZdmMode = (Boolean) entry.getValue();
            if (isZdmMode) {
                return true;
            }
        }
        return false;
    }

    public Boolean existsZdmMarker(DeploymentDescriptor descriptor, int majorVersion, int minorVersion) {
        return !getHdiDeployerModulesInZdmMode(descriptor, majorVersion, minorVersion).isEmpty();
    }
}
