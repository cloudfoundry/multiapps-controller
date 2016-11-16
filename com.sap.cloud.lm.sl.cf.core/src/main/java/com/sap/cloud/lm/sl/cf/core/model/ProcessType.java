package com.sap.cloud.lm.sl.cf.core.model;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "process-type")
@XmlEnum
public enum ProcessType {

    @XmlEnumValue("deploy") DEPLOY, @XmlEnumValue("undeploy") UNDEPLOY, @XmlEnumValue("blue-green-deploy") BLUE_GREEN_DEPLOY, @XmlEnumValue("cts-deploy") CTS_DEPLOY;

}
