package com.bmskinner.nuclear_morphology.api;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;

import com.bmskinner.nuclear_morphology.analysis.DatasetValidator;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.IJ;

public abstract class AnalysisPipelineTest {
	

    private static final Logger LOGGER = Logger.getLogger(Loggable.PROJECT_LOGGER);
    
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
