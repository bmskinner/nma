package com.bmskinner.nuclear_morphology.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder.TestComponentShape;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;

public class DatasetMergeMethodTest {

	@Before
	public void setUp() throws Exception {
	}

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
			Map<Landmark, Integer> tags = n.getBorderTags();
			for(Landmark tag : tags.keySet()) {
				if(Landmark.REFERENCE_POINT.equals(tag))
					continue;
				n.setBorderTag(tag, n.getBorderIndex(tag)+10);
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
			Map<Landmark, Integer> tags = n.getBorderTags();
			for(Landmark tag : tags.keySet()) {
				if(Landmark.REFERENCE_POINT.equals(tag))
					continue;
				assertEquals(tag.toString(), 
						tags.get(tag).intValue(), 
						test.getBorderIndex(tag));
			}
		}
	}

}
