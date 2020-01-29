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
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;

public class DeployedComponentsDetector {

    /**
     * Detects all deployed components on this platform.
     * 
     */
    public DeployedComponents detectAllDeployedComponents(Collection<CloudApplication> apps) {
        Map<DeployedMtaMetadata, Set<String>> servicesMap = new HashMap<>();
        Map<DeployedMtaMetadata, List<DeployedMtaApplication>> applicationsMap = new HashMap<>();
        List<String> standaloneApps = new ArrayList<>();

        for (CloudApplication app : apps) {
            String appName = app.getName();

            ApplicationMtaMetadata appMetadata = ApplicationMtaMetadataParser.parseAppMetadata(app);

            if (appMetadata != null) {
                // This application is an MTA module.
                String moduleName = (appMetadata.getModuleName() != null) ? appMetadata.getModuleName() : appName;
                List<String> providedDependencies = (appMetadata.getProvidedDependencyNames() != null)
                    ? appMetadata.getProvidedDependencyNames()
                    : new ArrayList<>();
                List<String> appServices = (appMetadata.getServices() != null) ? appMetadata.getServices() : new ArrayList<>();

                DeployedMtaMetadata mtaMetadata = appMetadata.getMtaMetadata();

                List<DeployedMtaApplication> deployedMtaApplications = applicationsMap.getOrDefault(mtaMetadata, new ArrayList<>());
                Date createdOn = app.getMetadata()
                                    .getCreatedAt();
                Date updatedOn = app.getMetadata()
                                    .getUpdatedAt();
                DeployedMtaApplication deployedMtaApplication = new DeployedMtaApplication(moduleName,
                                                                 appName,
                                                                 createdOn,
                                                                 updatedOn,
                                                                 appServices,
                                                                 providedDependencies,
                                                                 app.getUris());
                deployedMtaApplications.add(deployedMtaApplication);
                applicationsMap.put(mtaMetadata, deployedMtaApplications);

                Set<String> services = servicesMap.getOrDefault(mtaMetadata, new HashSet<>());
                services.addAll(appServices);
                servicesMap.put(mtaMetadata, services);
            } else {
                // This is a standalone application.
                standaloneApps.add(appName);
            }
        }

        return createComponents(applicationsMap, servicesMap, standaloneApps);
    }

    private DeployedComponents createComponents(Map<DeployedMtaMetadata, List<DeployedMtaApplication>> applicationsMap,
                                                Map<DeployedMtaMetadata, Set<String>> servicesMap, List<String> standaloneApps) {
        List<DeployedMta> mtas = applicationsMap.entrySet()
                                           .stream()
                                           .map(entry -> createDeployedMta(entry, servicesMap))
                                           .collect(Collectors.collectingAndThen(Collectors.toList(),
                                                                                 this::mergeDifferentVersionsOfMtasWithSameId));
        return new DeployedComponents(mtas, standaloneApps);
    }

    private DeployedMta createDeployedMta(Entry<DeployedMtaMetadata, List<DeployedMtaApplication>> entry,
                                          Map<DeployedMtaMetadata, Set<String>> servicesMap) {
        List<DeployedMtaApplication> deployedMtaApplications = entry.getValue();
        DeployedMtaMetadata mtaId = entry.getKey();
        return new DeployedMta(mtaId, deployedMtaApplications, servicesMap.get(mtaId));
    }

    private List<DeployedMta> mergeDifferentVersionsOfMtasWithSameId(List<DeployedMta> mtas) {
        Set<String> mtaIds = getMtaIds(mtas);
        List<DeployedMta> result = new ArrayList<>();
        for (String mtaId : mtaIds) {
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
                   .map(mta -> mta.getMetadata()
                                  .getId())
                   .collect(Collectors.toSet());
    }

    private List<DeployedMta> getMtasWithSameId(List<DeployedMta> mtas, String id) {
        return mtas.stream()
                   .filter(mta -> mta.getMetadata()
                                     .getId()
                                     .equals(id))
                   .collect(Collectors.toList());
    }

    private DeployedMta mergeMtas(String mtaId, List<DeployedMta> mtas) {
        List<DeployedMtaApplication> deployedMtaApplications = new ArrayList<>();
        Set<String> services = new HashSet<>();
        for (DeployedMta mta : mtas) {
            services.addAll(mta.getServices());
            deployedMtaApplications.addAll(mta.getApplications());
        }
        return new DeployedMta(new DeployedMtaMetadata(mtaId), deployedMtaApplications, services);
    }

}
