package com.sap.cloud.lm.sl.cf.core.liquibase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;

public class TransformFilterColumnTest {

    private TransformFilterColumn transformFilterColumn = new TransformFilterColumn();

    @Test
    public void testSplitTargetSpaceValue() {

        CloudTarget targetSpace = ConfigurationEntriesUtil.splitTargetSpaceValue("org space");
        assertEquals("org", targetSpace.getOrganizationName());
        assertEquals("space", targetSpace.getSpaceName());

        targetSpace = ConfigurationEntriesUtil.splitTargetSpaceValue("orgspace");
        assertEquals("", targetSpace.getOrganizationName());
        assertEquals("orgspace", targetSpace.getSpaceName());

        targetSpace = ConfigurationEntriesUtil.splitTargetSpaceValue("org test space sap");
        assertEquals("org", targetSpace.getOrganizationName());
        assertEquals("test space sap", targetSpace.getSpaceName());

        targetSpace = ConfigurationEntriesUtil.splitTargetSpaceValue("");
        assertEquals("", targetSpace.getOrganizationName());
        assertEquals("", targetSpace.getSpaceName());
    }

    @Test
    public void testTransformData() {

        Map<Long, String> retrievedData = new HashMap<>();
        retrievedData.put(1l, "{\"requiredContent\":{\"type\":\"com.acme.plugin\"},\"targetSpace\":\"org space\"}");
        retrievedData.put(2l, "{\"requiredContent\":{\"type\":\"com.acme.plugin\"},\"targetSpace\":\"orgspace\"}");
        retrievedData.put(3l, "{\"requiredContent\":{\"type\":\"com.acme.plugin\"},\"targetSpace\":\"org test space sap\"}");

        Map<Long, String> transformedData = transformFilterColumn.transformData(retrievedData);
        assertEquals("{\"requiredContent\":{\"type\":\"com.acme.plugin\"},\"targetSpace\":{\"organizationName\":\"org\",\"spaceName\":\"space\"}}",
                     transformedData.get(1l));

        transformedData = transformFilterColumn.transformData(retrievedData);
        assertEquals("{\"requiredContent\":{\"type\":\"com.acme.plugin\"},\"targetSpace\":{\"organizationName\":\"\",\"spaceName\":\"orgspace\"}}",
                     transformedData.get(2l));

        transformedData = transformFilterColumn.transformData(retrievedData);
        assertEquals("{\"requiredContent\":{\"type\":\"com.acme.plugin\"},\"targetSpace\":{\"organizationName\":\"org\",\"spaceName\":\"test space sap\"}}",
                     transformedData.get(3l));
    }

    @Test
    public void testTransformDataEmptyContent() {

        Map<Long, String> retrievedData = new HashMap<>();
        Map<Long, String> transformedData = null;

        retrievedData.put(1l, "{\"requiredContent\":{\"type\":\"com.acme.plugin\"}}");
        transformedData = transformFilterColumn.transformData(retrievedData);
        assertTrue(transformedData.isEmpty());

        retrievedData.put(1l, "{}");
        transformedData = transformFilterColumn.transformData(retrievedData);
        assertTrue(transformedData.isEmpty());

        retrievedData.put(1l, "");
        transformedData = transformFilterColumn.transformData(retrievedData);
        assertTrue(transformedData.isEmpty());
    }
}
