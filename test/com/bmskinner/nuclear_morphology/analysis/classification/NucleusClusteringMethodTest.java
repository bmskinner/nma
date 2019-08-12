package com.bmskinner.nuclear_morphology.analysis.classification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.analysis.DatasetMergeMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;

/**
 * Tests for the nucleus clustering
 * @author ben
 *
 */
public class NucleusClusteringMethodTest extends ComponentTester {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	private static final int CELLS_PER_CLUSTER = 50;
	private static final int TWO_CLUSTERS = 2;

	private IAnalysisDataset merged;
	
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		
		// Create two datasets with differently sized nuclei and merge them
		IAnalysisDataset dataset1 = new TestDatasetBuilder(RNG_SEED).cellCount(CELLS_PER_CLUSTER)
				.ofType(NucleusType.ROUND)
				.baseHeight(60)
				.baseWidth(50)
				.withMaxSizeVariation(0)
				.randomOffsetProfiles(false)
				.segmented().build();
		
		IAnalysisDataset dataset2 = new TestDatasetBuilder(RNG_SEED).cellCount(CELLS_PER_CLUSTER)
				.ofType(NucleusType.ROUND)
				.baseHeight(20)
				.baseWidth(30)
				.withMaxSizeVariation(0)
				.randomOffsetProfiles(false)
				.segmented().build();
		
		List<IAnalysisDataset> toMerge = new ArrayList<>();
		toMerge.add(dataset1);
		toMerge.add(dataset2);
		merged = new DatasetMergeMethod(toMerge, new File(TestResources.DATASET_FOLDER)).call().getFirstDataset();
		
		new DatasetProfilingMethod(merged)
		.then(new DatasetSegmentationMethod(merged, MorphologyAnalysisMode.NEW))
		.call();
	}

	@Test
	public void testCorrectNumberOfClustersReturned() throws Exception {
		IClusteringOptions o = OptionsFactory.makeClusteringOptions();
		o.setClusterNumber(TWO_CLUSTERS);
		new NucleusClusteringMethod(merged, o).call();
		assertNotNull(merged.getCollection());
		assertTrue(merged.hasClusters());
		assertTrue(merged.getClusterGroups().size()==1);
		
		IClusterGroup group = merged.getClusterGroups().stream().findFirst().get();		
		assertEquals(TWO_CLUSTERS, group.getUUIDs().size());
		
		for(UUID childId : group.getUUIDs()) {
			assertEquals(CELLS_PER_CLUSTER, merged.getChildDataset(childId).getCollection().size());
		}
	}

}
