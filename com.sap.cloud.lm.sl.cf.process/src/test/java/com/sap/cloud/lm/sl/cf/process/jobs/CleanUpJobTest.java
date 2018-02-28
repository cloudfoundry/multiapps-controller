package com.sap.cloud.lm.sl.cf.process.jobs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class CleanUpJobTest {
    private CleanUpJob cleanUpJob = new CleanUpJob();
    
    @Test
    public void testSplitAllFilesInChunks() {
        Map<String, List<String>> spaceToFileIds = new HashMap<>();
        List<String> fileIds = new ArrayList<String>();
        fileIds.add(null);
        fileIds.add("9f87be64-6519-4576-b426-42548840f2ec");
        fileIds.add("9f87be64-6519-4516-b426-42543845f2az,9f87ne64-6519-1234-b426-42548840f2gh,9f87be64-1239-4567-b426-34548840f2oq");
        spaceToFileIds.put("space", fileIds);
        Map<String, List<String>> splitAllFilesInChunks = cleanUpJob.splitAllFilesInChunks(spaceToFileIds);
        assertEquals("All file chunks must be five.", 5, splitAllFilesInChunks.get("space").size());
        
        List<String> expectedFileIds = new ArrayList<String>();
        expectedFileIds.add("9f87be64-6519-4516-b426-42543845f2az");
        expectedFileIds.add("9f87ne64-6519-1234-b426-42548840f2gh");
        expectedFileIds.add("9f87be64-1239-4567-b426-34548840f2oq");
        expectedFileIds.add("9f87be64-6519-4576-b426-42548840f2ec");
        
        assertTrue("Splited file Ids must match with given ones.", splitAllFilesInChunks.get("space").containsAll(expectedFileIds));
    }
}
