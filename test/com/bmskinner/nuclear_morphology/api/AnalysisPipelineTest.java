package com.bmskinner.nuclear_morphology.api;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;

import com.bmskinner.nuclear_morphology.analysis.DatasetValidator;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

import ij.IJ;
import ij.Prefs;

public abstract class AnalysisPipelineTest {

    protected static Logger logger;

    @Before
	public void setUp(){
    	IJ.setBackgroundColor(0, 0, 0);
	}
    
    protected boolean validateDataset(File expectedFile) throws Exception {
    	IAnalysisDataset d = SampleDatasetReader.openDataset(expectedFile);
		DatasetValidator dv = new DatasetValidator();
        if (!dv.validate(d)) {
            for (String s : dv.getErrors()) {
                logger.log(Level.SEVERE, s);
            }
        	fail("Dataset failed validation");
        }
        return true;
    }

}
