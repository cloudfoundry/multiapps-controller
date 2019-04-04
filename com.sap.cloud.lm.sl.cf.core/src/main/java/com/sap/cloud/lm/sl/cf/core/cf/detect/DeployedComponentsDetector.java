package com.sap.cloud.lm.sl.cf.core.cf.detect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.springframework.beans.factory.annotation.Autowired;

import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.MtaMetadataEntity;
import com.sap.cloud.lm.sl.cf.core.cf.detect.metadata.criteria.MtaMetadataCriteria;
import com.sap.cloud.lm.sl.cf.core.cf.detect.metadata.criteria.MtaMetadataCriteriaBuilder;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;

public class DeployedComponentsDetector {

    /**
     * Detects all deployed components on this platform.
     * 
     */

    @Autowired
    private List<MtaMetadataCollector<MtaMetadataEntity>> collectors;
    @Autowired
    private MtaMetadataExtractorFactory<MtaMetadataEntity> metadataExtractorFactory;

    public Optional<DeployedMta> getDeployedMta(String mtaId, CloudControllerClient client) {
        MtaMetadataCriteria selectionCriteria = new MtaMetadataCriteriaBuilder().label(MtaMetadataCriteriaBuilder.LABEL_MTA_ID)
                                                                                .haveValue(mtaId)
                                                                                .build();
        Optional<List<DeployedMta>> optionalDeployedMtas = fetchDeployedMtas(selectionCriteria, client);
        return getFirstElement(optionalDeployedMtas);
    }

    public Optional<DeployedMta> getDeployedMtaByCriteria(MtaMetadataCriteria criteria, CloudControllerClient client) {
        Optional<List<DeployedMta>> optionalDeployedMtas = fetchDeployedMtas(criteria, client);
        return getFirstElement(optionalDeployedMtas);
    }

    public Optional<List<DeployedMta>> getAllDeployedMta(CloudControllerClient client) {
        return fetchDeployedMtas(new MtaMetadataCriteria(MtaMetadataCriteria.NONE), client);
    }

    private Optional<List<DeployedMta>> fetchDeployedMtas(MtaMetadataCriteria criteria, CloudControllerClient client) {
        Map<String, DeployedMta> deployedMtasById = Collections.synchronizedMap(MapUtils.lazyMap(new HashMap<>(), () -> new DeployedMta()));

        collectors.stream()
                  .map(c -> c.collect(criteria, client))
                  .flatMap(List::stream)
                  .forEach(e -> {
                      MtaMetadataExtractor<MtaMetadataEntity> mtaMetadataExtractor = metadataExtractorFactory.get(e);
                      DeployedMta deployedMta = deployedMtasById.get(e.getMtaMetadata()
                                                                      .getId());
                      mtaMetadataExtractor.extract(e, deployedMta);
                  });

        if (deployedMtasById.values()
                            .isEmpty()) {
            return Optional.empty();
        }

        List<DeployedMta> deployedMtas = new ArrayList<>(deployedMtasById.values());
        deployedMtas = processDeployedMtas(deployedMtas);
        return Optional.of(deployedMtas);
    }

    private List<DeployedMta> processDeployedMtas(List<DeployedMta> deployedMtas) {
        return mergeDifferentVersionsOfMtasWithSameId(deployedMtas);
    }

    private List<DeployedMta> mergeDifferentVersionsOfMtasWithSameId(List<DeployedMta> mtas) {
        Map<String, Optional<DeployedMta>> deployedMtasById = mtas.stream()
                                                                  .collect(Collectors.groupingBy(e -> e.getMetadata()
                                                                                                       .getId(),
                                                                                                 Collectors.reducing(this::mergeMtas)));

        Collection<Optional<DeployedMta>> deployedMtas = deployedMtasById.values();

        List<DeployedMta> mappedDeployedMtas = deployedMtas.stream()
                                                           .filter(e -> e.isPresent())
                                                           .map(e -> e.get())
                                                           .collect(Collectors.toList());
        return mappedDeployedMtas;
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
