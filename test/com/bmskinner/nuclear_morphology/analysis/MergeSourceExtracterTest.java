/*******************************************************************************
 *      Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.io.DatasetExportMethod;
import com.bmskinner.nuclear_morphology.io.DatasetExportMethod.ExportFormat;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;

public class MergeSourceExtracterTest extends SampleDatasetReader {
    
	private Logger logger;
    public static final String MERGED_DATASET_FILE = TestResources.DATASET_FOLDER + "Merge_of_merge.nmd";
    
//    /** A 1k cell dataset with 3 merge sources created in 1.13.8 on a different computer */
//    public static final String MERGED_1_13_8_DATASET_FILE = TestResources.DATASET_FOLDER + "LEWxPWK.nmd";
        
    @Before
    public void setUp() throws Exception {
    	logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		logger.setLevel(Level.FINE);

		boolean hasHandler = false;
		for(Handler h : logger.getHandlers()) {
			if(h instanceof ConsoleHandler)
				hasHandler = true;
		}
		if(!hasHandler)
			logger.addHandler(new ConsoleHandler(new LogPanelFormatter()));
    }
    
    @Test
    public void testExtractorReturnsEmptyResultOnEmptyInput() throws Exception {
    	 MergeSourceExtractionMethod mse = new MergeSourceExtractionMethod(new ArrayList<>());
    	 List<IAnalysisDataset> extracted = mse.call().getDatasets();
    	 assertTrue(extracted.isEmpty());
    }
    
    
//    @Test
//    public void testSourceExtractedFrom1_13_8DatasetGetsRPReassigned() throws Exception {
//    	IAnalysisDataset merged = SampleDatasetReader.openDataset(new File(MERGED_1_13_8_DATASET_FILE));
//
//    	List<IAnalysisDataset> sources = new ArrayList<>();
//    	sources.addAll(merged.getAllMergeSources());
//    	
//    	MergeSourceExtractionMethod mse = new MergeSourceExtractionMethod(sources);
//    	List<IAnalysisDataset> extracted = mse.call().getDatasets();
//
//    	DatasetValidator dv = new DatasetValidator();
//    	for(IAnalysisDataset m : extracted){
//    		if(!dv.validate(m))
//    			fail("Dataset "+m.getName()+" did not validate:\n"+dv.getErrors().stream().collect(Collectors.joining("\n")));
//    	}
//    	
//    }
    
    @Test
    public void testSourceExtractedFromMergedDatasetEqualsInput() throws Exception {
    	File f1 = new File(TestResources.MOUSE_CLUSTERS_DATASET);
    	File f2 = new File(TestResources.MOUSE_TEST_DATASET);
    	File f3 = new File(MERGED_DATASET_FILE);

    	IAnalysisDataset d1 = openDataset(f1);
    	IAnalysisDataset d2 = openDataset(f2);

    	List<IAnalysisDataset> datasets = new ArrayList<>();
    	datasets.add(d1);
    	datasets.add(d2);

    	DatasetMergeMethod dm = new DatasetMergeMethod(datasets, f3);
    	IAnalysisDataset merged = dm.call().getFirstDataset();
    	
    	new DatasetProfilingMethod(merged)
    	.then(new DatasetSegmentationMethod(merged, MorphologyAnalysisMode.NEW))
    	.then(new DatasetExportMethod(merged, f3, ExportFormat.JAVA))
    	.call();
    	DatasetValidator dv = new DatasetValidator();
    	if(!dv.validate(merged))
			fail("Dataset "+merged.getName()+" did not validate:\n"+dv.getErrors().stream().collect(Collectors.joining("\n")));
    	
    	List<IAnalysisDataset> sources = new ArrayList<>();
    	sources.addAll(merged.getAllMergeSources());
    	
    	MergeSourceExtractionMethod mse = new MergeSourceExtractionMethod(sources);
    	List<IAnalysisDataset> extracted = mse.call().getDatasets();

    	
    	for(IAnalysisDataset m : extracted){
    		if(!dv.validate(m))
    			fail("Dataset "+m.getName()+" did not validate:\n"+dv.getErrors().stream().collect(Collectors.joining("\n")));
    	}
    	
    	DatasetComparator dc = new DatasetComparator();
    	assertEquals(d1.getCollection().size(), extracted.get(0).getCollection().size());
    	assertEquals(d2.getCollection().size(), extracted.get(1).getCollection().size());
    	
    	assertEquals(d1.getAnalysisOptions(), extracted.get(0).getAnalysisOptions());
    	assertEquals(d2.getAnalysisOptions(), extracted.get(1).getAnalysisOptions());
    }

}
