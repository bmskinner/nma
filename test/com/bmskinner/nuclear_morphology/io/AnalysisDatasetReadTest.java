package com.bmskinner.nuclear_morphology.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;


public class AnalysisDatasetReadTest extends SampleDatasetReader {
	
	public static final String TEST_PATH_1 = SAMPLE_DATASET_PATH + "Testing_1_13_7.nmd";
	public static final String TEST_PATH_2 = SAMPLE_DATASET_PATH + "Testing_multiple3.nmd";

	@Test
	public void testSample1DatasetCanBeRead() {
		File f = new File(TEST_PATH_1);
				
		try {
			IAnalysisDataset d = openDataset(f);
			assertEquals(d.getSavePath(), f);
			
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	@Test
    public void testSample2DatasetCanBeRead() {
        File f = new File(TEST_PATH_2);
                
        try {
            IAnalysisDataset d = openDataset(f);
            assertEquals(d.getSavePath(), f);
            
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
