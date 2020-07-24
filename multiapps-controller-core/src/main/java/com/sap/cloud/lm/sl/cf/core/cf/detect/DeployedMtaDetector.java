package com.sap.cloud.lm.sl.cf.core.cf.detect;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudEntity;

import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadataLabels;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.criteria.MtaMetadataCriteria;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.criteria.MtaMetadataCriteriaBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.entity.processor.MtaMetadataEntityAggregator;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.entity.processor.MtaMetadataEntityCollector;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.util.MtaMetadataUtil;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;

@Named
public class DeployedMtaDetector {

    @Inject
    private List<MtaMetadataEntityCollector<?>> mtaMetadataEntityCollectors;

    @Inject
    private MtaMetadataEntityAggregator mtaMetadataEntityAggregator;

    @Inject
    private DeployedMtaEnvDetector deployedMtaEnvDetector;

    public List<DeployedMta> detectDeployedMtas(CloudControllerClient client) {
        MtaMetadataCriteria allMtasCriteria = MtaMetadataCriteriaBuilder.builder()
                                                                        .label(MtaMetadataLabels.MTA_ID)
                                                                        .exists()
                                                                        .build();
        List<DeployedMta> deployedMtas = getDeployedMtasByMetadataSelectionCriteria(allMtasCriteria, client);
        List<DeployedMta> deployedMtasByEnv = deployedMtaEnvDetector.detectDeployedMtas(client);

        return combineMetadataAndEnvMtas(deployedMtas, deployedMtasByEnv);
    }

    public List<DeployedMta> detectDeployedMtasWithoutNamespace(CloudControllerClient client) {
        MtaMetadataCriteria mtasWithoutNamespaceCriteria = MtaMetadataCriteriaBuilder.builder()
                                                                                     .label(MtaMetadataLabels.MTA_ID)
                                                                                     .exists()
                                                                                     .and()
                                                                                     .label(MtaMetadataLabels.MTA_NAMESPACE)
                                                                                     .doesNotExist()
                                                                                     .build();
        List<DeployedMta> deployedMtas = getDeployedMtasByMetadataSelectionCriteria(mtasWithoutNamespaceCriteria, client);
        List<DeployedMta> deployedMtasByEnv = deployedMtaEnvDetector.detectDeployedMtasWithoutNamespace(client);

        return combineMetadataAndEnvMtas(deployedMtas, deployedMtasByEnv);
    }

    public List<DeployedMta> detectDeployedMtasByName(String mtaName, CloudControllerClient client) {
        MtaMetadataCriteria selectionCriteria = MtaMetadataCriteriaBuilder.builder()
                                                                          .label(MtaMetadataLabels.MTA_ID)
                                                                          .hasValue(MtaMetadataUtil.getHashedLabel(mtaName))
                                                                          .build();
        List<DeployedMta> deployedMtas = getDeployedMtasByMetadataSelectionCriteria(selectionCriteria, client);
        List<DeployedMta> deployedMtasByEnv = deployedMtaEnvDetector.detectDeployedMtaWithoutNamespace(mtaName, client);

        return combineMetadataAndEnvMtas(deployedMtas, deployedMtasByEnv);
    }

    public List<DeployedMta> detectDeployedMtasByNamespace(String mtaNamespace, CloudControllerClient client) {
        MtaMetadataCriteria selectionCriteria = MtaMetadataCriteriaBuilder.builder()
                                                                          .label(MtaMetadataLabels.MTA_ID)
                                                                          .exists()
                                                                          .and()
                                                                          .label(MtaMetadataLabels.MTA_NAMESPACE)
                                                                          .hasValueOrIsntPresent(MtaMetadataUtil.getHashedLabel(mtaNamespace))
                                                                          .build();

        return getDeployedMtasByMetadataSelectionCriteria(selectionCriteria, client);
    }

    public Optional<DeployedMta> detectDeployedMtaByNameAndNamespace(String mtaName, String mtaNamespace, CloudControllerClient client,
                                                                     boolean envDetectionEnabled) {
        MtaMetadataCriteria selectionCriteria = MtaMetadataCriteriaBuilder.builder()
                                                                          .label(MtaMetadataLabels.MTA_ID)
                                                                          .hasValue(MtaMetadataUtil.getHashedLabel(mtaName))
                                                                          .and()
                                                                          .label(MtaMetadataLabels.MTA_NAMESPACE)
                                                                          .hasValueOrIsntPresent(MtaMetadataUtil.getHashedLabel(mtaNamespace))
                                                                          .build();

        List<DeployedMta> deployedMtas = getDeployedMtasByMetadataSelectionCriteria(selectionCriteria, client);

        if (deployedMtas.isEmpty() && StringUtils.isEmpty(mtaNamespace) && envDetectionEnabled) {
            // no need to check by env if namespace was provided - that guarantees mta has metadata
            deployedMtas = deployedMtaEnvDetector.detectDeployedMtaWithoutNamespace(mtaName, client);
        }

        return deployedMtas.stream()
                           .findFirst();
    }

    private List<DeployedMta> getDeployedMtasByMetadataSelectionCriteria(MtaMetadataCriteria criteria, CloudControllerClient client) {
        List<CloudEntity> mtaMetadataEntities = mtaMetadataEntityCollectors.stream()
                                                                           .map(collector -> collector.collect(client, criteria))
                                                                           .flatMap(List::stream)
                                                                           .collect(Collectors.toList());
        return mtaMetadataEntityAggregator.aggregate(mtaMetadataEntities);
    }

    /**
     * Extra step required for backwards compatibility (see {@link DeployedMtaEnvDetector})
     *
     * @param mtasByMetadata
     * @param mtasByEnv
     * @return
     */
    private List<DeployedMta> combineMetadataAndEnvMtas(List<DeployedMta> mtasByMetadata, List<DeployedMta> mtasByEnv) {

        List<DeployedMta> missedMtas = mtasByEnv.stream()
                                                .filter(mta -> !mtasByMetadata.contains(mta))
                                                .collect(Collectors.toList());

        return ListUtils.union(mtasByMetadata, missedMtas);
    }

}
