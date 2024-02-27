package com.sap.cloud.lm.sl.cf.core.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.filters.OrderDirection;
import com.sap.cloud.lm.sl.cf.core.dto.persistence.AccessTokenDto;
import com.sap.cloud.lm.sl.cf.core.model.AccessToken;

@Component
public class AccessTokenDao {

    private final AccessTokenDtoDao accessTokenDtoDao;

    @Inject
    public AccessTokenDao(AccessTokenDtoDao accessTokenDtoDao) {
        this.accessTokenDtoDao = accessTokenDtoDao;
    }

    public List<AccessToken> findAll() {
        List<AccessTokenDto> accessTokenDtos = accessTokenDtoDao.findAll();
        return toAccessTokens(accessTokenDtos);
    }

    public void add(AccessToken accessToken) {
        AccessTokenDto accessTokenDto = new AccessTokenDto(accessToken);
        accessTokenDtoDao.add(accessTokenDto);
    }

    public void remove(AccessToken accessToken) {
        accessTokenDtoDao.remove(accessToken.getId());
    }

    public List<AccessToken> getTokensByUsernameSortedByExpirationDate(String username, OrderDirection orderDirection) {
        List<AccessTokenDto> accessTokenDtos = accessTokenDtoDao.getTokensByUsernameSortedByExpirationDate(username, orderDirection);
        return toAccessTokens(accessTokenDtos);
    }

    private List<AccessToken> toAccessTokens(List<AccessTokenDto> accessTokenDtos) {
        return accessTokenDtos.stream()
                              .map(AccessTokenDto::toAccessToken)
                              .collect(Collectors.toList());
    }

    public int deleteTokensWithExpirationBefore(LocalDateTime dateTime) {
        return accessTokenDtoDao.deleteTokensWithExpirationBefore(dateTime);
    }

    public int deleteAllTokens() {
        return accessTokenDtoDao.deleteAllTokens();
    }

}
