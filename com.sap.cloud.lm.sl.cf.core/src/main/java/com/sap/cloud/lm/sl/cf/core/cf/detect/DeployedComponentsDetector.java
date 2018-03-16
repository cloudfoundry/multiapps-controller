package com.sap.cloud.lm.sl.cf.core.cf.detect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.core.model.ApplicationMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedComponents;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.common.ParsingException;

public class DeployedComponentsDetector {

    /**
     * Detects all deployed components on this platform.
     * 
     */
    public DeployedComponents detectAllDeployedComponents(Collection<CloudApplication> apps) throws ParsingException {
        Map<DeployedMtaMetadata, Set<String>> servicesMap = new HashMap<>();
        Map<DeployedMtaMetadata, List<DeployedMtaModule>> modulesMap = new HashMap<>();
        List<String> standaloneApps = new ArrayList<>();

        for (CloudApplication app : apps) {
            String appName = app.getName();

            ApplicationMtaMetadata appMetadata = ApplicationMtaMetadataParser.parseAppMetadata(app);

            if (appMetadata != null) {
                // This application is an MTA module;
                String moduleName = (appMetadata.getModuleName() != null) ? appMetadata.getModuleName() : appName;
                List<String> providedDependencies = (appMetadata.getProvidedDependencyNames() != null)
                    ? appMetadata.getProvidedDependencyNames()
                    : new ArrayList<>();
                List<String> appServices = (appMetadata.getServices() != null) ? appMetadata.getServices() : new ArrayList<>();
                Map<String, Object> deployAttributes = (appMetadata.getDeployAttributes() != null) ? appMetadata.getDeployAttributes()
                    : new HashMap<>();

                DeployedMtaMetadata mtaMetadata = appMetadata.getMtaMetadata();

                List<DeployedMtaModule> modules = modulesMap.getOrDefault(mtaMetadata, new ArrayList<>());
                Date createdOn = app.getMeta()
                    .getCreated();
                Date updatedOn = app.getMeta()
                    .getUpdated();
                DeployedMtaModule module = new DeployedMtaModule(moduleName, appName, createdOn, updatedOn, appServices,
                    providedDependencies, deployAttributes, app.getUris());
                modules.add(module);
                modulesMap.put(mtaMetadata, modules);

                Set<String> services = servicesMap.getOrDefault(mtaMetadata, new HashSet<>());
                services.addAll(appServices);
                servicesMap.put(mtaMetadata, services);
            } else {
                // This is a standalone application;
                standaloneApps.add(appName);
            }
        }

        return createComponents(modulesMap, servicesMap, standaloneApps);
    }

    private DeployedComponents createComponents(Map<DeployedMtaMetadata, List<DeployedMtaModule>> modulesMap,
        Map<DeployedMtaMetadata, Set<String>> servicesMap, List<String> standaloneApps) {
        List<DeployedMta> mtas = new ArrayList<>();
        for (Entry<DeployedMtaMetadata, List<DeployedMtaModule>> entry : modulesMap.entrySet()) {
            List<DeployedMtaModule> modules = entry.getValue();
            DeployedMtaMetadata mtaId = entry.getKey();
            mtas.add(new DeployedMta(mtaId, modules, servicesMap.get(mtaId)));
        }
        mtas = mergeDifferentVersionsOfMtasWithSameId(mtas);
        return new DeployedComponents(mtas, standaloneApps);
    }

    private List<DeployedMta> mergeDifferentVersionsOfMtasWithSameId(List<DeployedMta> mtas) {
        List<DeployedMta> result = new ArrayList<>();
        for (String mtaId : getMtaIds(mtas)) {
            List<DeployedMta> mtasWithSameId = getMtasWithSameId(mtas, mtaId);
            if (mtasWithSameId.size() > 1) {
                result.add(mergeMtas(mtaId, mtasWithSameId));
            } else {
                result.add(mtasWithSameId.get(0));
            }
        }
        return result;
    }

    private Set<String> getMtaIds(List<DeployedMta> mtas) {
        return mtas.stream()
            .map((mta) -> mta.getMetadata()
                .getId())
            .collect(Collectors.toSet());
    }

    private List<DeployedMta> getMtasWithSameId(List<DeployedMta> mtas, String id) {
        return mtas.stream()
            .filter((mta) -> mta.getMetadata()
                .getId()
                .equals(id))
            .collect(Collectors.toList());
    }

    private DeployedMta mergeMtas(String mtaId, List<DeployedMta> mtas) {
        List<DeployedMtaModule> modules = new ArrayList<>();
        Set<String> services = new HashSet<>();
        for (DeployedMta mta : mtas) {
            services.addAll(mta.getServices());
            modules.addAll(mta.getModules());
        }
        return new DeployedMta(new DeployedMtaMetadata(mtaId), modules, services);
    }

}
