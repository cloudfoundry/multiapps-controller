package com.sap.cloud.lm.sl.cf.core.persistence.query.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.persistence.dto.ConfigurationSubscriptionDto;
import com.sap.cloud.lm.sl.cf.core.persistence.dto.ConfigurationSubscriptionDto.AttributeNames;
import com.sap.cloud.lm.sl.cf.core.persistence.query.ConfigurationSubscriptionQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.query.criteria.ImmutableQueryAttributeRestriction;
import com.sap.cloud.lm.sl.cf.core.persistence.query.criteria.QueryCriteria;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationSubscriptionService.ConfigurationSubscriptionMapper;

public class ConfigurationSubscriptionQueryImpl extends AbstractQueryImpl<ConfigurationSubscription, ConfigurationSubscriptionQuery>
    implements ConfigurationSubscriptionQuery {

    private QueryCriteria queryCriteria = new QueryCriteria();
    private ConfigurationSubscriptionMapper subscriptionMapper;
    private List<ConfigurationEntry> matchingEntries;

    public ConfigurationSubscriptionQueryImpl(EntityManager entityManager, ConfigurationSubscriptionMapper subscriptionMapper) {
        super(entityManager);
        this.subscriptionMapper = subscriptionMapper;
    }

    @Override
    public ConfigurationSubscriptionQuery id(Long id) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(id)
                                                                       .build());
        return this;
    }

    @Override
    public ConfigurationSubscriptionQuery mtaId(String mtaId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.MTA_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(mtaId)
                                                                       .build());
        return this;
    }

    @Override
    public ConfigurationSubscriptionQuery spaceId(String spaceId) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.SPACE_ID)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(spaceId)
                                                                       .build());
        return this;
    }

    @Override
    public ConfigurationSubscriptionQuery appName(String appName) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.APP_NAME)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(appName)
                                                                       .build());
        return this;
    }

    @Override
    public ConfigurationSubscriptionQuery resourceName(String resourceName) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.builder()
                                                                       .attribute(AttributeNames.RESOURCE_NAME)
                                                                       .condition(getCriteriaBuilder()::equal)
                                                                       .value(resourceName)
                                                                       .build());
        return this;
    }

    @Override
    public ConfigurationSubscriptionQuery onSelectMatching(List<ConfigurationEntry> entries) {
        this.matchingEntries = entries;
        return this;
    }

    @Override
    public ConfigurationSubscription singleResult() {
        ConfigurationSubscriptionDto dto = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                                       ConfigurationSubscriptionDto.class).getSingleResult());
        ConfigurationSubscription subscription = subscriptionMapper.fromDto(dto);
        if (!matchesEntries(subscription)) {
            throw new NoResultException(Messages.CONFIGURATION_SUBSCRIPTION_MATCHING_ENTRIES_NOT_FOUND_BY_QUERY);
        }
        return subscription;
    }

    private boolean matchesEntries(ConfigurationSubscription subscription) {
        if (matchingEntries != null && !matchingEntries.isEmpty()) {
            return subscription.matches(matchingEntries);
        }
        return true;
    }

    @Override
    public List<ConfigurationSubscription> list() {
        List<ConfigurationSubscriptionDto> dtos = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                                              ConfigurationSubscriptionDto.class).getResultList());
        return dtos.stream()
                   .map(subscriptionMapper::fromDto)
                   .filter(this::matchesEntries)
                   .collect(Collectors.toList());
    }

    @Override
    public int delete() {
        return executeInTransaction(manager -> createDeleteQuery(manager, queryCriteria,
                                                                 ConfigurationSubscriptionDto.class).executeUpdate());
    }

}