package com.sap.cloud.lm.sl.cf.core.persistence.query.impl;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.apache.commons.lang3.StringUtils;

import com.sap.cloud.lm.sl.cf.core.filters.ContentFilter;
import com.sap.cloud.lm.sl.cf.core.filters.TargetWildcardFilter;
import com.sap.cloud.lm.sl.cf.core.filters.VersionFilter;
import com.sap.cloud.lm.sl.cf.core.filters.VisibilityFilter;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.persistence.dto.ConfigurationEntryDto;
import com.sap.cloud.lm.sl.cf.core.persistence.dto.ConfigurationEntryDto.AttributeNames;
import com.sap.cloud.lm.sl.cf.core.persistence.query.ConfigurationEntryQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.query.criteria.ImmutableQueryAttributeRestriction;
import com.sap.cloud.lm.sl.cf.core.persistence.query.criteria.QueryCriteria;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService.ConfigurationEntryMapper;

public class ConfigurationEntryQueryImpl extends AbstractQueryImpl<ConfigurationEntry, ConfigurationEntryQuery>
    implements ConfigurationEntryQuery {

    private static final BiPredicate<ConfigurationEntry, String> VERSION_FILTER = new VersionFilter();
    private static final BiPredicate<ConfigurationEntry, List<CloudTarget>> VISIBILITY_FILTER = new VisibilityFilter();
    private static final BiPredicate<CloudTarget, CloudTarget> TARGET_WILDCARD_FILTER = new TargetWildcardFilter();
    private static final BiPredicate<String, Map<String, Object>> CONTENT_FILTER = new ContentFilter();

    private QueryCriteria queryCriteria = new QueryCriteria();
    private ConfigurationEntryMapper entryMapper;

    private Map<String, Object> requiredProperties;
    private CloudTarget target;
    private List<CloudTarget> visibilityTargets;
    private String version;

    public ConfigurationEntryQueryImpl(EntityManager entityManager, ConfigurationEntryMapper entryMapper) {
        super(entityManager);
        this.entryMapper = entryMapper;
    }

    @Override
    public ConfigurationEntryQuery id(Long id) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(id)
                                                                       .build());
        return this;
    }

    @Override
    public ConfigurationEntryQuery providerNid(String providerNid) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.PROVIDER_NID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(providerNid)
                                                                       .build());
        return this;
    }

    @Override
    public ConfigurationEntryQuery providerId(String providerId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.PROVIDER_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(providerId)
                                                                       .build());
        return this;
    }

    @Override
    public ConfigurationEntryQuery target(CloudTarget target) {
        this.target = target;
        if (target != null && !StringUtils.isEmpty(target.getSpace())) {
            queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                           .attribute(AttributeNames.TARGET_SPACE)
                                                                           .condition(getCriteriaBuilder()::equal)
                                                                           .value(target.getSpace())
                                                                           .build());
        }
        if (target != null && !StringUtils.isEmpty(target.getOrg())) {
            queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                           .attribute(AttributeNames.TARGET_ORG)
                                                                           .condition(getCriteriaBuilder()::equal)
                                                                           .value(target.getOrg())
                                                                           .build());
        }
        return this;
    }

    @Override
    public ConfigurationEntryQuery spaceId(String spaceId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.SPACE_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(spaceId)
                                                                       .build());
        return this;
    }

    @Override
    public ConfigurationEntryQuery mtaId(String mtaId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.<String> builder()
                                                                       .attribute(AttributeNames.PROVIDER_ID)
                                                                       .condition(getCriteriaBuilder()::like)
                                                                       .value(mtaId + ":%")
                                                                       .build());
        return this;
    }

    @Override
    public ConfigurationEntryQuery version(String version) {
        this.version = version;
        return this;
    }

    @Override
    public ConfigurationEntryQuery visibilityTargets(List<CloudTarget> visibilityTargets) {
        this.visibilityTargets = visibilityTargets;
        return this;
    }

    @Override
    public ConfigurationEntryQuery requiredProperties(Map<String, Object> requiredProperties) {
        this.requiredProperties = requiredProperties;
        return this;
    }

    @Override
    public ConfigurationEntry singleResult() {
        ConfigurationEntryDto dto = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                                ConfigurationEntryDto.class).getSingleResult());
        ConfigurationEntry entry = entryMapper.fromDto(dto);
        if (satisfiesVersion(entry) && satisfiesVisibilityTargets(entry)) {
            return entryMapper.fromDto(dto);
        }
        throw new NoResultException(MessageFormat.format(Messages.CONFIGURATION_ENTRY_SATISFYING_VERSION_AND_VIS_NOT_FOUND, version,
                                                         getVisibilityTargets()));
    }

    public String getVisibilityTargets() {
        StringBuilder visibilityTargetsStringBuilder = new StringBuilder();
        for (CloudTarget cloudTarget : visibilityTargets) {
            visibilityTargetsStringBuilder.append(MessageFormat.format("('\"{0}\"', '\"{1}\"')", cloudTarget.getOrg(),
                                                                       cloudTarget.getSpace()));
        }
        return visibilityTargetsStringBuilder.toString();
    }

    @Override
    public List<ConfigurationEntry> list() {
        List<ConfigurationEntryDto> dtos = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                                       ConfigurationEntryDto.class).getResultList());
        return dtos.stream()
                   .filter(this::satisfiesTargetWildcard)
                   .filter(this::satisfiesContent)
                   .map(entryMapper::fromDto)
                   .filter(this::satisfiesVersion)
                   .filter(this::satisfiesVisibilityTargets)
                   .collect(Collectors.toList());
    }

    private boolean satisfiesVersion(ConfigurationEntry entry) {
        return VERSION_FILTER.test(entry, version);
    }

    private boolean satisfiesVisibilityTargets(ConfigurationEntry entry) {
        return VISIBILITY_FILTER.test(entry, visibilityTargets);
    }

    private boolean satisfiesTargetWildcard(ConfigurationEntryDto entryDto) {
        return TARGET_WILDCARD_FILTER.test(new CloudTarget(entryDto.getTargetOrg(), entryDto.getTargetSpace()), target);
    }

    private boolean satisfiesContent(ConfigurationEntryDto entryDto) {
        return CONTENT_FILTER.test(entryDto.getContent(), requiredProperties);
    }

    @Override
    public int delete() {
        return executeInTransaction(manager -> createDeleteQuery(manager, queryCriteria, ConfigurationEntryDto.class).executeUpdate());
    }

}