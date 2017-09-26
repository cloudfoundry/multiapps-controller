package com.sap.cloud.lm.sl.cf.core.liquibase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;

public class TransformFilterColumnTest {

    private TransformFilterColumn transformFilterColumn = new TransformFilterColumn();

    @Test
    public void testSplitTargetSpaceValue() {

        CloudTarget targetSpace = ConfigurationUtil.splitTargetSpaceValue("org space");
        assertEquals("org", targetSpace.getOrg());
        assertEquals("space", targetSpace.getSpace());

        targetSpace = ConfigurationUtil.splitTargetSpaceValue("orgspace");
        assertEquals("", targetSpace.getOrg());
        assertEquals("orgspace", targetSpace.getSpace());

        targetSpace = ConfigurationUtil.splitTargetSpaceValue("org test space sap");
        assertEquals("org", targetSpace.getOrg());
        assertEquals("test space sap", targetSpace.getSpace());

        targetSpace = ConfigurationUtil.splitTargetSpaceValue("");
        assertEquals("", targetSpace.getOrg());
        assertEquals("", targetSpace.getSpace());
    }

    @Test
    public void testTransformData() {

        Map<Long, String> retrievedData = new HashMap<Long, String>();
        retrievedData.put(1l, "{\"requiredContent\":{\"type\":\"com.acme.plugin\"},\"targetSpace\":\"org space\"}");
        retrievedData.put(2l, "{\"requiredContent\":{\"type\":\"com.acme.plugin\"},\"targetSpace\":\"orgspace\"}");
        retrievedData.put(3l, "{\"requiredContent\":{\"type\":\"com.acme.plugin\"},\"targetSpace\":\"org test space sap\"}");

        Map<Long, String> transformedData = transformFilterColumn.transformData(retrievedData);
        assertEquals("{\"requiredContent\":{\"type\":\"com.acme.plugin\"},\"targetSpace\":{\"space\":\"space\",\"org\":\"org\"}}",
            transformedData.get(1l));

        transformedData = transformFilterColumn.transformData(retrievedData);
        assertEquals("{\"requiredContent\":{\"type\":\"com.acme.plugin\"},\"targetSpace\":{\"space\":\"orgspace\",\"org\":\"\"}}",
            transformedData.get(2l));

        transformedData = transformFilterColumn.transformData(retrievedData);
        assertEquals("{\"requiredContent\":{\"type\":\"com.acme.plugin\"},\"targetSpace\":{\"space\":\"test space sap\",\"org\":\"org\"}}",
            transformedData.get(3l));
    }

    @Test
    public void testTransformDataEmptyContent() {

        Map<Long, String> retrievedData = new HashMap<Long, String>();
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
