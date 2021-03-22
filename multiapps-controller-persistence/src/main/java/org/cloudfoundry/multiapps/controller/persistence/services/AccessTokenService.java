package org.cloudfoundry.multiapps.controller.persistence.services;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;

import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.dto.AccessTokenDto;
import org.cloudfoundry.multiapps.controller.persistence.model.AccessToken;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableAccessToken;
import org.cloudfoundry.multiapps.controller.persistence.query.AccessTokenQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.impl.AccessTokenQueryImpl;

@Named
public class AccessTokenService extends PersistenceService<AccessToken, AccessTokenDto, Long> {

    private final AccessTokenMapper accessTokenMapper;

    @Inject
    public AccessTokenService(EntityManagerFactory entityManagerFactory, AccessTokenMapper accessTokenMapper) {
        super(entityManagerFactory);
        this.accessTokenMapper = accessTokenMapper;
    }

    public AccessTokenQuery createQuery() {
        return new AccessTokenQueryImpl(createEntityManager(), accessTokenMapper);
    }

    @Override
    protected PersistenceObjectMapper<AccessToken, AccessTokenDto> getPersistenceObjectMapper() {
        return accessTokenMapper;
    }

    @Override
    protected void onEntityConflict(AccessTokenDto dto, Throwable t) {
        throw new ConflictException(t, Messages.ACCESS_TOKEN_ALREADY_EXISTS, dto.getPrimaryKey());
    }

    @Override
    protected void onEntityNotFound(Long primaryKey) {
        throw new NotFoundException(Messages.ACCESS_TOKEN_NOT_FOUND, primaryKey);
    }

    @Named
    public static class AccessTokenMapper implements PersistenceObjectMapper<AccessToken, AccessTokenDto> {

        @Override
        public AccessToken fromDto(AccessTokenDto accessTokenDto) {
            return ImmutableAccessToken.builder()
                                       .id(accessTokenDto.getPrimaryKey())
                                       .value(accessTokenDto.getValue())
                                       .username(accessTokenDto.getUsername())
                                       .expiresAt(accessTokenDto.getExpiresAt())
                                       .build();
        }

        @Override
        public AccessTokenDto toDto(AccessToken accessToken) {
            return new AccessTokenDto(accessToken.getId(), accessToken.getValue(), accessToken.getUsername(), accessToken.getExpiresAt());
        }
    }
}
