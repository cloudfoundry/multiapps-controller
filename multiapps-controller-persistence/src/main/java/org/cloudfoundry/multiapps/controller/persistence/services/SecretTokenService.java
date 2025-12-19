package org.cloudfoundry.multiapps.controller.persistence.services;

import java.sql.SQLException;
import java.time.LocalDateTime;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManagerFactory;
import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.controller.persistence.DataSourceWithDialect;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.dto.SecretTokenDto;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableSecretToken;
import org.cloudfoundry.multiapps.controller.persistence.model.SecretToken;
import org.cloudfoundry.multiapps.controller.persistence.query.SecretTokenQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.impl.SecretTokenQueryImpl;
import org.cloudfoundry.multiapps.controller.persistence.query.providers.SqlSecretTokenQueryProvider;
import org.cloudfoundry.multiapps.controller.persistence.util.SqlQueryExecutor;

@Named
public class SecretTokenService extends PersistenceService<SecretToken, SecretTokenDto, Long> {

    private static final String TABLE_NAME = "secret_token";

    @Inject
    private SecretTokenMapper secretTokenMapper;

    private SqlQueryExecutor sqlQueryExecutor;

    private SqlSecretTokenQueryProvider sqlSecretTokenQueryProvider;

    public SecretTokenService(EntityManagerFactory entityManagerFactory, DataSourceWithDialect dataSourceWithDialect) {
        super(entityManagerFactory);
        this.sqlQueryExecutor = new SqlQueryExecutor(dataSourceWithDialect.getDataSource());
        this.sqlSecretTokenQueryProvider = new SqlSecretTokenQueryProvider(TABLE_NAME);
    }

    public long putSecretToken(String processInstanceId, String variableName, byte[] content, String keyId) throws SQLException {
        return sqlQueryExecutor.execute(sqlSecretTokenQueryProvider.insertSecretToken(processInstanceId, variableName, content, keyId));
    }

    public byte[] getSecretToken(String processInstanceId, long id) throws SQLException {
        return sqlQueryExecutor.execute(sqlSecretTokenQueryProvider.getSecretToken(processInstanceId, id));
    }

    public int deleteForProcess(String processInstanceId) throws SQLException {
        return sqlQueryExecutor.execute(sqlSecretTokenQueryProvider.deleteForProcessInstance(processInstanceId));
    }

    public int deleteOlderThan(LocalDateTime expirationTime) throws SQLException {
        return sqlQueryExecutor.execute(sqlSecretTokenQueryProvider.deleteOlderThan(expirationTime));
    }

    public SecretTokenQuery createQuery() {
        return new SecretTokenQueryImpl(createEntityManager(), secretTokenMapper);
    }

    @Override
    protected PersistenceObjectMapper<SecretToken, SecretTokenDto> getPersistenceObjectMapper() {
        return secretTokenMapper;
    }

    @Override
    protected void onEntityConflict(SecretTokenDto secretTokenDto, Throwable t) {
        throw new ConflictException(t, Messages.SECRET_TOKEN_FOR_VARIABLE_NAME_0_AND_ID_1_ALREADY_EXIST, secretTokenDto.getVariableName(),
                                    secretTokenDto.getPrimaryKey());
    }

    @Override
    protected void onEntityNotFound(Long id) {
        throw new NotFoundException(Messages.SECRET_TOKEN_WITH_ID_NOT_EXIST, id);
    }

    @Named
    public static class SecretTokenMapper implements PersistenceObjectMapper<SecretToken, SecretTokenDto> {

        @Override
        public SecretToken fromDto(SecretTokenDto dto) {
            return ImmutableSecretToken.builder()
                                       .id(dto.getPrimaryKey())
                                       .processInstanceId(dto.getProcessInstanceId())
                                       .variableName(dto.getVariableName())
                                       .content(
                                           dto.getContent())
                                       .timestamp(dto.getTimestamp())
                                       .keyId(dto.getKeyId())
                                       .build();
        }

        @Override
        public SecretTokenDto toDto(SecretToken secretToken) {
            long id = secretToken.getId();
            String processInstanceId = secretToken.getProcessInstanceId();
            String variableName = secretToken.getVariableName();
            byte[] content = secretToken.getContent();
            LocalDateTime timestamp = secretToken.getTimestamp();
            String keyId = secretToken.getKeyId();
            return new SecretTokenDto(id, processInstanceId, variableName, content, timestamp, keyId);
        }
    }

    public SqlQueryExecutor getSqlQueryExecutor() {
        return sqlQueryExecutor;
    }

    public SqlSecretTokenQueryProvider getSqlSecretTokenQueryProvider() {
        return sqlSecretTokenQueryProvider;
    }

}
