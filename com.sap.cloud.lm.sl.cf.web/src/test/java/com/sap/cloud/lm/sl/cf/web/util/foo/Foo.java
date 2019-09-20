package com.sap.cloud.lm.sl.cf.web.util.foo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "model")
public class Foo {

    @XmlElement(name = "property-1")
    private String property1;
    @XmlElement(name = "property-2")
    private String property2;
    @XmlElement(name = "property-3")
    private Integer property3;
    @XmlElement(name = "property-4")
    private Boolean property4;

    public Foo() {
        // Required by JAXB.
    }

    public Foo(String property1, String property2, Integer property3, Boolean property4) {
        this.property1 = property1;
        this.property2 = property2;
        this.property3 = property3;
        this.property4 = property4;
    }

    public String getProperty1() {
        return property1;
    }

    public String getProperty2() {
        return property2;
    }

    public Integer getProperty3() {
        return property3;
    }

    public Boolean getProperty4() {
        return property4;
    }

}
