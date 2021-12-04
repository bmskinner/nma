package com.bmskinner.nuclear_morphology.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder.TestComponentShape;
import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

public class DatasetMergeMethodTest {
	
	public static final String MERGED_DATASET_FILE = TestResources.DATASET_FOLDER + "Merge_of_merge.nmd";

	@Test
	public void testTagsAreCopiedAfterResegmentation() throws Exception {
		
		IAnalysisDataset d1 = new TestDatasetBuilder(123)
				.withNucleusShape(TestComponentShape.SQUARE)
				.cellCount(10)
				.segmented()
				.build();
		
		IAnalysisDataset d2 = new TestDatasetBuilder(456)
				.withNucleusShape(TestComponentShape.SQUARE)
				.cellCount(10)
				.segmented()
				.build();
		
		// Move all tags except RP by a fixed distance
		for(Nucleus n : d1.getCollection().getNuclei()) {
			Map<Landmark, Integer> tags = n.getLandmarks();
			for(Landmark tag : tags.keySet()) {
				if(Landmark.REFERENCE_POINT.equals(tag))
					continue;
				n.setLandmark(tag, n.getBorderIndex(tag)+10);
			}
			
		}
		
		List<IAnalysisDataset> list = new ArrayList<>();
		list.add(d1);
		list.add(d2);
		
		// Merge and resegment the datasets
		DatasetMergeMethod dm = new DatasetMergeMethod(list, new File("Empty path"));
		IAnalysisDataset result = dm.call().getFirstDataset();
		assertNotNull("Merged dataset should not be null", result);
		
		
		new DatasetProfilingMethod(result).call();
		new DatasetSegmentationMethod(result, MorphologyAnalysisMode.NEW).call();
				
		// Are tag positions properly restored?
		for(Nucleus n : d1.getCollection().getNuclei()) {
			Nucleus test = result.getCollection().getNucleus(n.getID()).get();
			Map<Landmark, Integer> tags = n.getLandmarks();
			for(Landmark tag : tags.keySet()) {
				if(Landmark.REFERENCE_POINT.equals(tag))
					continue;
				assertEquals(tag.toString(), 
						tags.get(tag).intValue(), 
						test.getBorderIndex(tag));
			}
		}
	}
	
	/**
	 * Test that if a consensus nucleus was present in the merge source, it
	 * is not carried over to the merge source. This is because segmentation 
	 * patterns will change in the merging, so we should clear the consensus
	 * @throws Exception
	 */
	@Test
	public void testConsensusIsRemovedInMergeSource() throws Exception {
		File f1 = TestResources.MOUSE_CLUSTERS_DATASET;
    	File f2 = TestResources.MOUSE_TEST_DATASET;
    	File f3 = new File(MERGED_DATASET_FILE);

    	// Open the template datasets
    	IAnalysisDataset d1 = SampleDatasetReader.openDataset(f1);
    	IAnalysisDataset d2 = SampleDatasetReader.openDataset(f2);
    	
    	assertTrue(d1.getCollection().hasConsensus());
    	assertTrue(d2.getCollection().hasConsensus());
    	

    	List<IAnalysisDataset> datasets = new ArrayList<>();
    	datasets.add(d1);
    	datasets.add(d2);

    	// Merge the datasets
    	DatasetMergeMethod dm = new DatasetMergeMethod(datasets, f3);
    	IAnalysisDataset merged = dm.call().getFirstDataset();
    	
    	for(IAnalysisDataset d : merged.getMergeSources()) {
    		assertFalse(d.getCollection().hasConsensus());
    	}
	}

}
