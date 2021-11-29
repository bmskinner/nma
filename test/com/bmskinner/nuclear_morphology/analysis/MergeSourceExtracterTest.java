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
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.io.DatasetExportMethod;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

public class MergeSourceExtracterTest {
    
	private static final Logger LOGGER = Logger.getLogger(MergeSourceExtracterTest.class.getName());
    public static final String MERGED_DATASET_FILE = TestResources.DATASET_FOLDER + "Merge_of_merge.nmd";
    
//    /** A 1k cell dataset with 3 merge sources created in 1.13.8 on a different computer */
//    public static final String MERGED_1_13_8_DATASET_FILE = TestResources.DATASET_FOLDER + "LEWxPWK.nmd";
        
    @Before
    public void setUp() throws Exception {
    }
    
    @Test
    public void testExtractorReturnsEmptyResultOnEmptyInput() throws Exception {
    	 MergeSourceExtractionMethod mse = new MergeSourceExtractionMethod(new ArrayList<>());
    	 List<IAnalysisDataset> extracted = mse.call().getDatasets();
    	 assertTrue(extracted.isEmpty());
    }
    
    /**
     * This tests if a merge source can be recovered losslessly. We open two
     * datasets and merge them. We then extract the merge source
     * and compare it to the original saved dataset.
     * 
     * Note that segmentation is expected to change, and cannot be recovered 
     * perfectly
     * @throws Exception
     */
    @Test
    public void testSourceExtractedFromMergedDatasetEqualsInput() throws Exception {
    	File f1 = TestResources.MOUSE_CLUSTERS_DATASET;
    	File f2 = TestResources.MOUSE_TEST_DATASET;
    	File f3 = new File(MERGED_DATASET_FILE);

    	// Open the template datasets
    	IAnalysisDataset d1 = SampleDatasetReader.openDataset(f1);
    	IAnalysisDataset d2 = SampleDatasetReader.openDataset(f2);

    	List<IAnalysisDataset> datasets = new ArrayList<>();
    	datasets.add(d1);
    	datasets.add(d2);

    	// Merge the datasets
    	DatasetMergeMethod dm = new DatasetMergeMethod(datasets, f3);
    	IAnalysisDataset merged = dm.call().getFirstDataset();
    	
    	new DatasetProfilingMethod(merged)
    	.then(new DatasetSegmentationMethod(merged, MorphologyAnalysisMode.NEW))
    	.then(new DatasetExportMethod(merged, f3))
    	.call();
    	DatasetValidator dv = new DatasetValidator();
    	if(!dv.validate(merged))
			fail("Dataset "+merged.getName()+" did not validate:\n"+dv.getErrors().stream().collect(Collectors.joining("\n")));
    	
    	
    	// Extract the merge sources
    	List<IAnalysisDataset> sources = new ArrayList<>();
    	sources.addAll(merged.getAllMergeSources());
    	
    	MergeSourceExtractionMethod mse = new MergeSourceExtractionMethod(sources);
    	List<IAnalysisDataset> extracted = mse.call().getDatasets();

    	// Ensure the merge sources validate
    	for(IAnalysisDataset m : extracted){
    		if(!dv.validate(m))
    			fail("Dataset "+m.getName()+" did not validate:\n"+dv.getErrors().stream().collect(Collectors.joining("\n")));
    	}
    	
    	IAnalysisDataset r1 = extracted.stream().filter(d->d.getName().equals(d1.getName())).findFirst().orElseThrow(Exception::new); 
    	IAnalysisDataset r2 = extracted.stream().filter(d->d.getName().equals(d2.getName())).findFirst().orElseThrow(Exception::new); 
    	    	    	
    	for(ICell c : d1.getCollection()) {
    		assertTrue(r1.getCollection().contains(c));
    	}
    	
    	for(ICell c : d2.getCollection()) {
    		assertTrue(r2.getCollection().contains(c));
    	}
    	
    	assertEquals(d1.getCollection().size(), r1.getCollection().size());
    	assertEquals(d2.getCollection().size(), r2.getCollection().size());
    	
    	assertEquals(d1.getAnalysisOptions(), r1.getAnalysisOptions());
    	assertEquals(d2.getAnalysisOptions(), r2.getAnalysisOptions());
    }

}
