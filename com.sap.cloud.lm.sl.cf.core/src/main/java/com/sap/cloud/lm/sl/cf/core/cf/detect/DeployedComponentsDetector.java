package com.sap.cloud.lm.sl.cf.core.cf.detect;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.mta.model.Version;
import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.MetadataEntity;
import com.sap.cloud.lm.sl.cf.core.cf.detect.metadata.criteria.MtaMetadataCriteria;
import com.sap.cloud.lm.sl.cf.core.cf.detect.metadata.criteria.MtaMetadataCriteriaBuilder;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Component
public class DeployedComponentsDetector {

    /**
     * Detects all deployed components on this platform.
     * 
     */

    @Autowired
    private List<MtaMetadataCollector<? extends MetadataEntity>> collectors;

    @Autowired
    private MtaMetadataEntityAggregator mtaMetadataEntityAggregator;

    public Optional<DeployedMta> getDeployedMta(String mtaId, CloudControllerClient client) {
        MtaMetadataCriteria selectionCriteria = MtaMetadataCriteriaBuilder.builder().label(MtaMetadataCriteriaBuilder.LABEL_MTA_ID)
                                                                                .haveValue(mtaId)
                                                                                .build();
        Optional<List<DeployedMta>> optionalDeployedMtas = fetchDeployedMtas(selectionCriteria, client);
        final Optional<DeployedMta> deployedMta = getFirstElement(optionalDeployedMtas);
        return deployedMta.isPresent() ? deployedMta : getDeployedMtaByEnv(mtaId, client);
    }

    public Optional<List<DeployedMta>> getAllDeployedMta(CloudControllerClient client) {
        MtaMetadataCriteria selectionCriteria = MtaMetadataCriteriaBuilder.builder().label(MtaMetadataCriteriaBuilder.LABEL_MTA_ID)
                                                                                .exists()
                                                                                .build();
        return fetchDeployedMtas(selectionCriteria, client);
    }

    private Optional<List<DeployedMta>> fetchDeployedMtas(MtaMetadataCriteria criteria, CloudControllerClient client) {
        Map<String, Map<Version, List<MetadataEntity>>> mtaEntitiesByIdByVersion = collectors.stream()
                                                                                             .map(collector -> collector.collect(criteria,
                                                                                                                                 client))
                                                                                             .flatMap(List::stream)
                                                                                             .collect(Collectors.groupingBy(e -> e.getMtaMetadata()
                                                                                                                                  .getId(),
                                                                                                                            Collectors.groupingBy(e -> e.getMtaMetadata()
                                                                                                                                                        .getVersion())));

        List<DeployedMta> deployedMtas = mtaEntitiesByIdByVersion.values()
                                                                 .stream()
                                                                 .flatMap(versionsMaps -> versionsMaps.values()
                                                                                                      .stream())
                                                                 .map(listEntitiesSameIdDifferentVersion -> mtaMetadataEntityAggregator.aggregate(listEntitiesSameIdDifferentVersion))
                                                                 .collect(Collectors.toList());

        deployedMtas = processDeployedMtas(deployedMtas);
        return Optional.of(deployedMtas);
    }

    private List<DeployedMta> processDeployedMtas(List<DeployedMta> deployedMtas) {
        List<DeployedMta> mergedMtasById = mergeDifferentVersionsOfMtasWithSameId(deployedMtas);
        return removeEmptyMtas(mergedMtasById);
    }

    private List<DeployedMta> removeEmptyMtas(List<DeployedMta> mtas) {
        return mtas.stream()
                   .filter(mta -> CollectionUtils.isNotEmpty(mta.getModules()) || CollectionUtils.isNotEmpty(mta.getResources()))
                   .collect(Collectors.toList());
    }

    private List<DeployedMta> mergeDifferentVersionsOfMtasWithSameId(List<DeployedMta> mtas) {
        Map<String, Optional<DeployedMta>> deployedMtasById = mtas.stream()
                                                                  .collect(Collectors.groupingBy(e -> e.getMetadata()
                                                                                                       .getId(),
                                                                                                 Collectors.reducing(this::mergeMtas)));

        Collection<Optional<DeployedMta>> deployedMtas = deployedMtasById.values();

        return deployedMtas.stream()
                           .filter(e -> e.isPresent())
                           .map(e -> e.get())
                           .collect(Collectors.toList());
    }

    private DeployedMta mergeMtas(DeployedMta from, DeployedMta to) {
        to.getResources()
          .addAll(from.getResources());
        to.getModules()
          .addAll(from.getModules());
        if (!from.getMetadata()
                 .getVersion()
                 .equals(to.getMetadata()
                           .getVersion())) {
            to.getMetadata()
              .setVersion(null);
        }
        return to;
    }

    private <T> Optional<T> getFirstElement(Optional<List<T>> optionalList) {
        if (!optionalList.isPresent()) {
            return Optional.empty();
        }

        List<T> elements = optionalList.get();
        if (elements.size() == 0) {
            return Optional.empty();
        }

        return Optional.ofNullable(elements.get(0));
    }

    private Optional<DeployedMta> getDeployedMtaByEnv(String mtaId, CloudControllerClient client) {
        DeployedComponentsDetectorEnv envDeployedComponentsDetector = new DeployedComponentsDetectorEnv(client);
        final List<DeployedMta> deployedMtas = envDeployedComponentsDetector.detectAllDeployedComponents();
        if (deployedMtas == null) {
            return Optional.empty();
        }
        return deployedMtas.stream()
                .filter(mta -> mta.getMetadata()
                        .getId()
                        .equalsIgnoreCase(mtaId))
                .findFirst();
    }
}
