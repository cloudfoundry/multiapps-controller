package org.cloudfoundry.multiapps.controller.persistence.services;

import java.util.List;
import java.util.Objects;

import javax.inject.Named;
import javax.persistence.EntityManagerFactory;

import org.apache.commons.lang3.ObjectUtils;
import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.dto.ConfigurationEntryDto;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata;
import org.cloudfoundry.multiapps.controller.persistence.query.ConfigurationEntryQuery;
import org.cloudfoundry.multiapps.controller.persistence.query.impl.ConfigurationEntryQueryImpl;
import org.cloudfoundry.multiapps.mta.model.Version;

import com.fasterxml.jackson.core.type.TypeReference;

public class ConfigurationEntryService extends PersistenceService<ConfigurationEntry, ConfigurationEntryDto, Long> {

    protected ConfigurationEntryMapper entryMapper;

    public ConfigurationEntryService(EntityManagerFactory entityManagerFactory, ConfigurationEntryMapper entryMapper) {
        super(entityManagerFactory);
        this.entryMapper = entryMapper;
    }

    public ConfigurationEntryQuery createQuery() {
        return new ConfigurationEntryQueryImpl(createEntityManager(), entryMapper);
    }

    @Override
    protected ConfigurationEntryDto merge(ConfigurationEntryDto existingEntry, ConfigurationEntryDto newEntry) {
        super.merge(existingEntry, newEntry);
        String providerNid = ObjectUtils.firstNonNull(removeDefault(newEntry.getProviderNid()), existingEntry.getProviderNid());
        String providerId = ObjectUtils.firstNonNull(newEntry.getProviderId(), existingEntry.getProviderId());
        String targetOrg = ObjectUtils.firstNonNull(newEntry.getTargetOrg(), existingEntry.getTargetOrg());
        String targetSpace = ObjectUtils.firstNonNull(newEntry.getTargetSpace(), existingEntry.getTargetSpace());
        String providerVersion = ObjectUtils.firstNonNull(removeDefault(newEntry.getProviderVersion()), existingEntry.getProviderVersion());
        String providerNamespace = ObjectUtils.firstNonNull(newEntry.getProviderNamespace(), existingEntry.getProviderNamespace());
        String content = ObjectUtils.firstNonNull(newEntry.getContent(), existingEntry.getContent());
        String visibility = ObjectUtils.firstNonNull(newEntry.getVisibility(), existingEntry.getVisibility());
        String spaceId = ObjectUtils.firstNonNull(newEntry.getSpaceId(), existingEntry.getSpaceId());
        String contentId = ObjectUtils.firstNonNull(newEntry.getContentId(), existingEntry.getContentId());
        return ConfigurationEntryDto.builder()
                                    .id(newEntry.getPrimaryKey())
                                    .providerNid(providerNid)
                                    .providerId(providerId)
                                    .providerVersion(providerVersion)
                                    .providerNamespace(providerNamespace)
                                    .targetOrg(targetOrg)
                                    .targetSpace(targetSpace)
                                    .content(content)
                                    .visibility(visibility)
                                    .spaceId(spaceId)
                                    .contentId(contentId)
                                    .build();
    }

    private String removeDefault(String value) {
        return value.equals(PersistenceMetadata.NOT_AVAILABLE) ? null : value;
    }

    @Override
    protected PersistenceObjectMapper<ConfigurationEntry, ConfigurationEntryDto> getPersistenceObjectMapper() {
        return entryMapper;
    }

    @Override
    protected void onEntityConflict(ConfigurationEntryDto entry, Throwable t) {
        throw new ConflictException(t,
                                    Messages.CONFIGURATION_ENTRY_ALREADY_EXISTS,
                                    entry.getProviderNid(),
                                    entry.getProviderId(),
                                    entry.getProviderVersion(),
                                    entry.getProviderNamespace(),
                                    entry.getTargetOrg(),
                                    entry.getTargetSpace());
    }

    @Override
    protected void onEntityNotFound(Long id) {
        throw new NotFoundException(Messages.CONFIGURATION_ENTRY_NOT_FOUND, id);
    }

    @Named("configurationEntryObjectMapper")
    public static class ConfigurationEntryMapper implements PersistenceObjectMapper<ConfigurationEntry, ConfigurationEntryDto> {

        @Override
        public ConfigurationEntry fromDto(ConfigurationEntryDto dto) {
            Long id = dto.getPrimaryKey();
            String providerNid = getOriginal(dto.getProviderNid());
            String providerId = dto.getProviderId();
            Version providerVersion = getParsedVersion(getOriginal(dto.getProviderVersion()));
            String providerNamespace = dto.getProviderNamespace();
            CloudTarget targetSpace = new CloudTarget(dto.getTargetOrg(), dto.getTargetSpace());
            String content = dto.getContent();
            List<CloudTarget> visibility = getParsedVisibility(dto.getVisibility());
            String spaceId = dto.getSpaceId();
            String contentId = dto.getContentId();
            return new ConfigurationEntry(id,
                                          providerNid,
                                          providerId,
                                          providerVersion,
                                          providerNamespace,
                                          targetSpace,
                                          content,
                                          visibility,
                                          spaceId,
                                          contentId);
        }

        protected String getOriginal(String source) {
            return Objects.equals(source, PersistenceMetadata.NOT_AVAILABLE) ? null : source;
        }

        protected Version getParsedVersion(String versionString) {
            return versionString == null ? null : Version.parseVersion(versionString);
        }

        protected List<CloudTarget> getParsedVisibility(String visibility) {
            return visibility == null ? null : JsonUtil.convertJsonToList(visibility, new TypeReference<List<CloudTarget>>() {
            });
        }

        @Override
        public ConfigurationEntryDto toDto(ConfigurationEntry entry) {
            long id = entry.getId();
            String providerNid = getNotNull(entry.getProviderNid());
            String providerId = entry.getProviderId();
            String providerVersion = getNotNull(entry.getProviderVersion());
            String providerNamespace = entry.getProviderNamespace();
            String targetOrg = entry.getTargetSpace() == null ? null
                : entry.getTargetSpace()
                       .getOrganizationName();
            String targetSpace = entry.getTargetSpace() == null ? null
                : entry.getTargetSpace()
                       .getSpaceName();
            String content = entry.getContent();
            String visibility = entry.getVisibility() == null ? null : JsonUtil.toJson(entry.getVisibility());
            String spaceId = entry.getSpaceId();
            String contentId = entry.getContentId();
            return ConfigurationEntryDto.builder()
                                        .id(id)
                                        .providerNid(providerNid)
                                        .providerId(providerId)
                                        .providerVersion(providerVersion)
                                        .providerNamespace(providerNamespace)
                                        .targetOrg(targetOrg)
                                        .targetSpace(targetSpace)
                                        .content(content)
                                        .visibility(visibility)
                                        .spaceId(spaceId)
                                        .contentId(contentId)
                                        .build();
        }

        private String getNotNull(Object source) {
            return source == null ? PersistenceMetadata.NOT_AVAILABLE : source.toString();
        }

    }

}