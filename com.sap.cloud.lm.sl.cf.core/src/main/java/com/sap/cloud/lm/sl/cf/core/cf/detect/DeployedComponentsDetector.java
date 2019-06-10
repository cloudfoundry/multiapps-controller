package com.sap.cloud.lm.sl.cf.core.cf.detect;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.MetadataEntity;
import com.sap.cloud.lm.sl.cf.core.cf.detect.metadata.criteria.MtaMetadataCriteria;
import com.sap.cloud.lm.sl.cf.core.cf.detect.metadata.criteria.MtaMetadataCriteriaBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.detect.process.AppMetadataCollector;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Component
public class DeployedComponentsDetector {

    /**
     * Detects all deployed components on this platform.
     * 
     */

    @Autowired
    private AppMetadataCollector appCollector;

    @Autowired
    private List<MtaMetadataCollector<? extends MetadataEntity>> collectors;

    @Autowired
    private MtaMetadataEntityAggregator mtaMetadataEntityAggregator;

    public Optional<DeployedMta> getDeployedMta(String mtaId, CloudControllerClient client) {
        MtaMetadataCriteria selectionCriteria = new MtaMetadataCriteriaBuilder().label(MtaMetadataCriteriaBuilder.LABEL_MTA_ID)
                                                                                .haveValue(mtaId)
                                                                                .build();
        Optional<List<DeployedMta>> optionalDeployedMtas = fetchDeployedMtas(selectionCriteria, client);
        return getFirstElement(optionalDeployedMtas);
    }

    public Optional<List<DeployedMta>> getAllDeployedMta(CloudControllerClient client) {
        MtaMetadataCriteria selectionCriteria = new MtaMetadataCriteriaBuilder().label(MtaMetadataCriteriaBuilder.LABEL_MTA_ID)
                                                                                .exists()
                                                                                .build();
        return fetchDeployedMtas(selectionCriteria, client);
    }

    private Optional<List<DeployedMta>> fetchDeployedMtas(MtaMetadataCriteria criteria, CloudControllerClient client) {
        Map<String, List<MetadataEntity>> mtaEntities = collectors.stream()
                                                                  .map(c -> c.collect(criteria, client))
                                                                  .flatMap(List::stream)
                                                                  .collect(Collectors.groupingBy(e -> e.getMtaMetadata()
                                                                                                          .getId()));

        List<DeployedMta> deployedMtas = mtaEntities.entrySet()
                                                    .stream()
                                                    .map(entry -> mtaMetadataEntityAggregator.aggregate(entry.getValue()))
                                                    .collect(Collectors.toList());

        deployedMtas = processDeployedMtas(deployedMtas);
        System.out.println("Detected deployed mtas: " + JsonUtil.toJson(deployedMtas, true));
        return Optional.of(deployedMtas);
    }

    private List<DeployedMta> processDeployedMtas(List<DeployedMta> deployedMtas) {
        System.out.println("Initial deployed mtas: " + JsonUtil.toJson(deployedMtas, true));
        List<DeployedMta> mergedMtasById = mergeDifferentVersionsOfMtasWithSameId(deployedMtas);
        System.out.println("mergedMtasById: " + JsonUtil.toJson(mergedMtasById, true));
        return removeEmptyMtas(mergedMtasById);
    }

    private List<DeployedMta> removeEmptyMtas(List<DeployedMta> mtas) {
        return mtas.stream()
                   .filter(mta -> CollectionUtils.isNotEmpty(mta.getModules()) || CollectionUtils.isNotEmpty(mta.getServices()))
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
        to.getServices()
          .addAll(from.getServices());
        to.getModules()
          .addAll(from.getModules());
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
}
