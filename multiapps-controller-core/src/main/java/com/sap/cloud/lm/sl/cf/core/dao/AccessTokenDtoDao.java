package com.sap.cloud.lm.sl.cf.core.dao;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import com.sap.cloud.lm.sl.cf.core.dao.filters.OrderDirection;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.AccessTokenDto;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.NotFoundException;

public class AccessTokenDtoDao {

    private final EntityManagerFactory entityManagerFactory;

    public AccessTokenDtoDao(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    List<AccessTokenDto> findAll() {
        return new Executor<List<AccessTokenDto>>(createEntityManager()).execute(manager -> createSelectAllCriteriaQuery(manager).getResultList());

    }

    public void add(AccessTokenDto accessToken) {
        new TransactionalExecutor<Void>(createEntityManager()).execute(manager -> {
            if (existsInternal(manager, accessToken.getId())) {
                throw new ConflictException(Messages.ACCESS_TOKEN_ALREADY_EXISTS, accessToken.getId());
            }
            manager.persist(accessToken);
            return null;
        });
    }

    public void remove(long accessTokenId) {
        new TransactionalExecutor<Void>(createEntityManager()).execute(manager -> {
            AccessTokenDto dto = manager.find(AccessTokenDto.class, accessTokenId);
            if (dto == null) {
                throw new NotFoundException(Messages.ACCESS_TOKEN_NOT_FOUND, accessTokenId);
            }
            manager.remove(dto);
            return null;
        });
    }

    public List<AccessTokenDto> getTokensByUsernameSortedByExpirationDate(String username, OrderDirection orderDirection) {
        return new Executor<List<AccessTokenDto>>(createEntityManager()).execute(manager -> createFilteredByUsernameOrderedQuery(manager,
                                                                                                                                 username,
                                                                                                                                 orderDirection).getResultList());
    }

    private TypedQuery<AccessTokenDto> createFilteredByUsernameOrderedQuery(EntityManager manager, String username,
                                                                            OrderDirection orderDirection) {
        CriteriaBuilder criteriaBuilder = manager.getCriteriaBuilder();
        CriteriaQuery<AccessTokenDto> query = criteriaBuilder.createQuery(AccessTokenDto.class);
        Root<AccessTokenDto> root = query.from(AccessTokenDto.class);
        query.select(root)
             .where(criteriaBuilder.equal(root.get(AccessTokenDto.AttributeNames.USERNAME), username));
        if (orderDirection == OrderDirection.ASCENDING) {
            query.orderBy(criteriaBuilder.asc(root.get(AccessTokenDto.AttributeNames.EXPIRES_AT)));
        } else {
            query.orderBy(criteriaBuilder.desc(root.get(AccessTokenDto.AttributeNames.EXPIRES_AT)));
        }
        return manager.createQuery(query);
    }

    public int deleteTokensWithExpirationBefore(LocalDateTime dateTime) {
        return new TransactionalExecutor<Integer>(createEntityManager()).execute(manager -> createOlderThanExpirationDateDeleteQuery(manager,
                                                                                                                                     dateTime).executeUpdate());
    }

    private Query createOlderThanExpirationDateDeleteQuery(EntityManager manager, LocalDateTime dateTime) {
        CriteriaBuilder criteriaBuilder = manager.getCriteriaBuilder();
        CriteriaDelete<AccessTokenDto> query = criteriaBuilder.createCriteriaDelete(AccessTokenDto.class);
        Root<AccessTokenDto> root = query.from(AccessTokenDto.class);
        query.where(criteriaBuilder.lessThan(root.get(AccessTokenDto.AttributeNames.EXPIRES_AT), dateTime));
        return manager.createQuery(query);
    }

    protected void deleteAccessTokenByValue(byte[] value) {
        new TransactionalExecutor<Integer>(createEntityManager()).execute(manager -> createDeleteByValueQuery(manager,
                                                                                                              value).executeUpdate());
    }

    private Query createDeleteByValueQuery(EntityManager manager, byte[] value) {
        CriteriaBuilder criteriaBuilder = manager.getCriteriaBuilder();
        CriteriaDelete<AccessTokenDto> query = criteriaBuilder.createCriteriaDelete(AccessTokenDto.class);
        Root<AccessTokenDto> root = query.from(AccessTokenDto.class);
        query.where(criteriaBuilder.equal(root.get(AccessTokenDto.AttributeNames.VALUE), value));
        return manager.createQuery(query);
    }

    protected List<AccessTokenDto> getTokensWithExpirationBefore(LocalDateTime dateTime) {
        return new Executor<List<AccessTokenDto>>(createEntityManager()).execute(manager -> createOlderThanExpirationDateCriteriaQuery(manager,
                                                                                                                                       dateTime).getResultList());
    }

    private TypedQuery<AccessTokenDto> createOlderThanExpirationDateCriteriaQuery(EntityManager manager, LocalDateTime dateTime) {
        CriteriaBuilder criteriaBuilder = manager.getCriteriaBuilder();
        CriteriaQuery<AccessTokenDto> query = criteriaBuilder.createQuery(AccessTokenDto.class);
        Root<AccessTokenDto> root = query.from(AccessTokenDto.class);
        query.where(criteriaBuilder.lessThan(root.get(AccessTokenDto.AttributeNames.EXPIRES_AT), dateTime));
        return manager.createQuery(query);
    }

    private TypedQuery<AccessTokenDto> createSelectAllCriteriaQuery(EntityManager manager) {
        CriteriaBuilder criteriaBuilder = manager.getCriteriaBuilder();
        CriteriaQuery<AccessTokenDto> query = criteriaBuilder.createQuery(AccessTokenDto.class);
        return manager.createQuery(query);
    }

    public int deleteAllTokens() {
        return new TransactionalExecutor<Integer>(createEntityManager()).execute(manager -> createDeleteAllTokensQuery(manager).executeUpdate());
    }

    private Query createDeleteAllTokensQuery(EntityManager manager) {
        CriteriaBuilder criteriaBuilder = manager.getCriteriaBuilder();
        CriteriaDelete<AccessTokenDto> query = criteriaBuilder.createCriteriaDelete(AccessTokenDto.class);
        query.from(AccessTokenDto.class);
        return manager.createQuery(query);
    }

    protected List<AccessTokenDto> getTokensInBatches(int batch, int maxResults) {
        return new Executor<List<AccessTokenDto>>(createEntityManager()).execute(manager -> createBatchedTokensQuery(manager, batch,
                                                                                                                     maxResults).getResultList());
    }

    private TypedQuery<AccessTokenDto> createBatchedTokensQuery(EntityManager manager, int batch, int maxResults) {
        CriteriaBuilder criteriaBuilder = manager.getCriteriaBuilder();
        CriteriaQuery<AccessTokenDto> query = criteriaBuilder.createQuery(AccessTokenDto.class);
        Root<AccessTokenDto> root = query.from(AccessTokenDto.class);
        query.select(root);
        TypedQuery<AccessTokenDto> selectQuery = manager.createQuery(query);
        selectQuery.setFirstResult(batch * maxResults);
        selectQuery.setMaxResults(maxResults);
        return selectQuery;
    }

    private EntityManager createEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    private boolean existsInternal(EntityManager manager, long accessTokenId) {
        return manager.find(AccessTokenDto.class, accessTokenId) != null;
    }

    protected AccessTokenDto find(long accessTokenId) {
        return new TransactionalExecutor<AccessTokenDto>(createEntityManager()).execute(manager -> manager.find(AccessTokenDto.class,
                                                                                                                accessTokenId));
    }
}
