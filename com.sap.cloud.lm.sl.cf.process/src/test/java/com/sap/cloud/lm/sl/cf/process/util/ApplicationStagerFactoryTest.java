package com.sap.cloud.lm.sl.cf.process.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;

public class ApplicationStagerFactoryTest {
    
    @Test
    public void testWithPlatformTypeCF() {
        assertTrue(ApplicationStagerFactory.createApplicationStager(PlatformType.CF) instanceof ApplicationStager);
    }
    
    @Test
    public void testWithPlatformTypeXS2() {
        assertTrue(ApplicationStagerFactory.createApplicationStager(PlatformType.XS2) instanceof XS2ApplicationStager);
    }
}
