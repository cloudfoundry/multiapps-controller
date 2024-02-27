package com.sap.cloud.lm.sl.cf.core.dto.serialization;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "configuration-entries")
@XmlAccessorType(value = XmlAccessType.FIELD)
public class ConfigurationEntriesDto {

    @XmlElement(name = "configuration-entry")
    private List<ConfigurationEntryDto> entries;

    public ConfigurationEntriesDto() {
        // Required by JAXB.
    }

    public ConfigurationEntriesDto(List<ConfigurationEntryDto> entries) {
        this.entries = entries;
    }

    public List<ConfigurationEntryDto> getConfigurationEntries() {
        return entries;
    }

}
