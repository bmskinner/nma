package com.bmskinner.nma.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.TestDatasetBuilder;
import com.bmskinner.nma.TestDatasetBuilder.TestComponentShape;
import com.bmskinner.nma.TestImageDatasetCreator;
import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nma.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nma.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nma.analysis.signals.PairedSignalGroups;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.io.DatasetExportMethod;
import com.bmskinner.nma.io.SampleDatasetReader;

public class DatasetMergeMethodTest {

	public static final String MERGED_DATASET_FILE = "Merge_of_datasets.nmd";

	public static final String MERGED_SIGNAL_DATASET_FILE = "Merge_of_signals.nmd";

	@Test
	public void testMergedDatasetCanBeProfiled() throws Exception {
		IAnalysisDataset d1 = new TestDatasetBuilder(123)
				.withNucleusShape(TestComponentShape.SQUARE).cellCount(10)
				.segmented().build();

		IAnalysisDataset d2 = new TestDatasetBuilder(456)
				.withNucleusShape(TestComponentShape.SQUARE).cellCount(10)
				.segmented().build();

		// Move all OrientationMarks in d1 except RP by a fixed distance
		for (Nucleus n : d1.getCollection().getNuclei()) {
			Map<OrientationMark, Integer> tags = n.getOrientationMarkMap();
			for (OrientationMark tag : tags.keySet()) {
				if (OrientationMark.REFERENCE.equals(tag))
					continue;
				n.setOrientationMark(tag, n.wrapIndex(n.getBorderIndex(tag) + 10));
			}
		}

		List<IAnalysisDataset> list = List.of(d1, d2);

		// Merge and resegment the datasets
		IAnalysisDataset result = new DatasetMergeMethod(list, new File("Empty path")).call()
				.getFirstDataset();
		assertNotNull("Merged dataset should not be null", result);
		assertEquals("Merged dataset should have correct cell count", 20,
				result.getCollection().size());

		// Run new profiling on the merged dataset
		new DatasetProfilingMethod(result)
				.then(new DatasetSegmentationMethod(result,
						MorphologyAnalysisMode.SEGMENT_FROM_SCRATCH))
				.call();
	}

	@Test
	public void testLandmarksArePresentInMergedDatasetAfterResegmentation() throws Exception {

		// Make two different datasets
		IAnalysisDataset d1 = new TestDatasetBuilder(123)
				.withNucleusShape(TestComponentShape.SQUARE)
				.cellCount(10)
				.segmented().build();

		IAnalysisDataset d2 = new TestDatasetBuilder(456)
				.withNucleusShape(TestComponentShape.SQUARE)
				.cellCount(10)
				.segmented().build();

		// Move all OrientationMarks in d1 except RP by a fixed distance
		for (Nucleus n : d1.getCollection().getNuclei()) {
			Map<OrientationMark, Integer> tags = n.getOrientationMarkMap();
			for (OrientationMark tag : tags.keySet()) {
				Landmark lm = n.getLandmark(tag);
				if (n.getLandmark(OrientationMark.REFERENCE).equals(lm))
					continue;
				n.setOrientationMark(tag, n.getBorderIndex(tag) + 10);
			}
		}

		List<IAnalysisDataset> list = List.of(d1, d2);

		// Merge and resegment the datasets
		DatasetMergeMethod dm = new DatasetMergeMethod(list, new File("Empty path"));
		IAnalysisDataset result = dm.call().getFirstDataset();
		assertNotNull("Merged dataset should not be null", result);
		assertEquals("Merged dataset should have correct cell count", 20,
				result.getCollection().size());

		// Run new profiling on the merged dataset
		new DatasetProfilingMethod(result).call();
		new DatasetSegmentationMethod(result, MorphologyAnalysisMode.SEGMENT_FROM_SCRATCH).call();

		// Are OrientationMark positions properly restored?
		for (Nucleus n : d1.getCollection().getNuclei()) {
			Nucleus test = result.getCollection().getNucleus(n.getID()).get();
			for (OrientationMark tag : n.getOrientationMarks())
				assertTrue(test.hasLandmark(tag));
		}
	}

	/**
	 * Test that if a consensus nucleus was present in the merge source, it is not
	 * carried over to the merge source. This is because segmentation patterns will
	 * change in the merging, so we should clear the consensus
	 * 
	 * @throws Exception
	 */
	@Test
	public void testConsensusIsRemovedInMergeSource() throws Exception {
		File f1 = TestResources.MOUSE_CLUSTERS_DATASET;
		File f2 = TestResources.MOUSE_TEST_DATASET;
		File f3 = new File(TestResources.DATASET_FOLDER, MERGED_DATASET_FILE);

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

		for (IAnalysisDataset d : merged.getMergeSources()) {
			assertFalse(d.getCollection().hasConsensus());
		}
	}

	/**
	 * Test that when datasets are merged the merge sources contain the correct
	 * cells
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMergedDatasetsContainAllCells() throws Exception {
		File f1 = TestResources.MOUSE_CLUSTERS_DATASET;
		File f2 = TestResources.MOUSE_TEST_DATASET;
		File f3 = new File(TestResources.DATASET_FOLDER, MERGED_DATASET_FILE);

		// Open the template datasets
		IAnalysisDataset d1 = SampleDatasetReader.openDataset(f1);
		IAnalysisDataset d2 = SampleDatasetReader.openDataset(f2);

		// Merge the datasets
		IAnalysisDataset merged = new DatasetMergeMethod(List.of(d1, d2), f3).call()
				.getFirstDataset();

		// Profile, segment, save and reopen
		merged = new DatasetProfilingMethod(merged)
				.then(new DatasetSegmentationMethod(merged,
						MorphologyAnalysisMode.SEGMENT_FROM_SCRATCH))
				.call().getFirstDataset();

		for (ICell c : d1.getCollection())
			assertTrue(merged.getCollection().contains(c));

		for (ICell c : d2.getCollection())
			assertTrue(merged.getCollection().contains(c));

		assertTrue("Merged dataset should have options", merged.hasAnalysisOptions());

		assertEquals("Merged nucleus options should match input dataset 1",
				d1.getAnalysisOptions().get().getNucleusDetectionOptions(),
				merged.getAnalysisOptions().get().getNucleusDetectionOptions());
		assertEquals("Merged nucleus options should match input dataset 2",
				d2.getAnalysisOptions().get().getNucleusDetectionOptions(),
				merged.getAnalysisOptions().get().getNucleusDetectionOptions());

	}

	/**
	 * Test that when datasets are merged and saved, the merge sources contain the
	 * correct data
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMergedDatasetsAreUnmarshalled() throws Exception {
		File f1 = TestResources.MOUSE_CLUSTERS_DATASET;
		File f2 = TestResources.MOUSE_TEST_DATASET;
		File f3 = new File(TestResources.DATASET_FOLDER, MERGED_DATASET_FILE);

		// Open the template datasets
		IAnalysisDataset d1 = SampleDatasetReader.openDataset(f1);
		IAnalysisDataset d2 = SampleDatasetReader.openDataset(f2);

		// Merge the datasets
		IAnalysisDataset merged = new DatasetMergeMethod(List.of(d1, d2), f3).call()
				.getFirstDataset();

		// Profile, segment, save and reopen
		new DatasetProfilingMethod(merged)
				.then(new DatasetSegmentationMethod(merged,
						MorphologyAnalysisMode.SEGMENT_FROM_SCRATCH))
				.then(new DatasetExportMethod(merged, f3))
				.call();

		IAnalysisDataset d3 = SampleDatasetReader.openDataset(f3);

		ComponentTester.testDuplicatesByField("Merged dataset should be equal after unmarshalling",
				merged, d3);
	}

	@Test
	public void testMergedDatasetsMergeSignals() throws Exception {
		File f1 = TestResources.MOUSE_SIGNALS_DATASET;
		File f2 = TestResources.MOUSE_SIGNALS_DATASET;
		File f3 = new File(TestResources.DATASET_FOLDER, MERGED_SIGNAL_DATASET_FILE);

		// Open the template datasets
		IAnalysisDataset d1 = SampleDatasetReader.openDataset(f1);
		IAnalysisDataset d2 = SampleDatasetReader.openDataset(f2);

		PairedSignalGroups ps = new PairedSignalGroups();
		ps.add(d1, d1.getCollection().getSignalGroup(TestImageDatasetCreator.RED_SIGNAL_ID).get(),
				d2,
				d2.getCollection().getSignalGroup(TestImageDatasetCreator.RED_SIGNAL_ID).get());

		// Merge resegment the dataset
		IAnalysisDataset merged = new DatasetMergeMethod(List.of(d1, d2), f3, ps).call()
				.getFirstDataset();

		merged = new DatasetProfilingMethod(merged)
				.then(new DatasetSegmentationMethod(merged,
						MorphologyAnalysisMode.SEGMENT_FROM_SCRATCH))
				.call().getFirstDataset();

		assertTrue("Merged dataset should have signals",
				merged.getCollection().getSignalManager().hasSignals());
		assertTrue("Merged dataset should have single signal group",
				merged.getCollection().getSignalGroupIDs().size() == 1);

		// the merged signal id is randomly chosen by the merger, find what was picked
		UUID mergedSignalId = merged.getCollection().getSignalGroupIDs().stream()
				.findAny().get();
		assertTrue(
				"Merged dataset should have options for the merged signal group " + mergedSignalId,
				merged.getAnalysisOptions().get().hasNuclearSignalDetectionOptions(mergedSignalId));
	}

	@Test
	public void testMergedDatasetsWithSignalsAreUnmarshalled() throws Exception {
		File f1 = TestResources.MOUSE_SIGNALS_DATASET;
		File f2 = TestResources.MOUSE_SIGNALS_DATASET;
		File f3 = new File(TestResources.DATASET_FOLDER, MERGED_SIGNAL_DATASET_FILE);

		// Open the template datasets
		IAnalysisDataset d1 = SampleDatasetReader.openDataset(f1);
		IAnalysisDataset d2 = SampleDatasetReader.openDataset(f2);

		PairedSignalGroups ps = new PairedSignalGroups();
		ps.add(d1, d1.getCollection().getSignalGroup(TestImageDatasetCreator.RED_SIGNAL_ID).get(),
				d2,
				d2.getCollection().getSignalGroup(TestImageDatasetCreator.RED_SIGNAL_ID).get());

		// Merge resegment and save the dataset
		IAnalysisDataset merged = new DatasetMergeMethod(List.of(d1, d2), f3, ps).call()
				.getFirstDataset();

		// Profile, segment, save and reopen
		new DatasetProfilingMethod(merged)
				.then(new DatasetSegmentationMethod(merged,
						MorphologyAnalysisMode.SEGMENT_FROM_SCRATCH))
				.then(new DatasetExportMethod(merged, f3))
				.call();

		IAnalysisDataset d3 = SampleDatasetReader.openDataset(f3);

		ComponentTester.testDuplicatesByField("Merged dataset should be equal after unmarshalling",
				merged, d3);

		assertTrue("Merged dataset should have signals",
				merged.getCollection().getSignalManager().hasSignals());
		assertTrue("Merged dataset should have single signal group",
				merged.getCollection().getSignalGroupIDs().size() == 1);
	}

}
