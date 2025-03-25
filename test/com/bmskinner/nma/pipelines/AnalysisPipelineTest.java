package com.bmskinner.nma.pipelines;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.junit.Before;

import com.bmskinner.nma.components.datasets.DatasetValidator;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.io.SampleDatasetReader;


import ij.IJ;

public abstract class AnalysisPipelineTest {
	

    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    @Before
	public void setUp(){
    	IJ.setBackgroundColor(0, 0, 0);
	}
    
    protected boolean validateDataset(File expectedFile) throws Exception {
    	IAnalysisDataset d = SampleDatasetReader.openDataset(expectedFile);
		DatasetValidator dv = new DatasetValidator();
        if (!dv.validate(d)) {
            for (String s : dv.getErrors()) {
            	LOGGER.log(Level.SEVERE, s);
            }
        	fail("Dataset failed validation");
        }
        return true;
    }

}
