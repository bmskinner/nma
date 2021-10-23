package com.bmskinner.nuclear_morphology.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.io.Io;

/**
 * Test the workflows can save a valid dataset
 * @author ben
 * @since 1.14.0
 *
 */
public class SavedOptionsAnalysisPipelineTest extends AnalysisPipelineTest {
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	protected void testPipelineCreatesReadableExportFile(File imageFolder, File xmlFile) throws Exception {
		assertTrue("Input image folder"+imageFolder.getAbsolutePath(), imageFolder.exists());
		assertTrue("XML options file"+xmlFile.getAbsolutePath(), xmlFile.exists());
		
		new SavedOptionsAnalysisPipeline(imageFolder, xmlFile, TestResources.DATASET_FOLDER).call();
		assertTrue("Analysis output folder "+TestResources.DATASET_FOLDER.getAbsolutePath(), TestResources.DATASET_FOLDER.exists());
		
		File expectedFile = new File(TestResources.DATASET_FOLDER, imageFolder.getName()+Io.SAVE_FILE_EXTENSION);
		assertTrue("Output file should exist: "+expectedFile.getAbsolutePath(), expectedFile.exists());
		
		assertTrue(validateDataset(expectedFile));
	}

	@Test
	public void testCreateRoundDataset() throws Exception {
		File xmlFile = new File(TestResources.ROUND_OUTPUT_FOLDER, "Round.options.xml");
		testPipelineCreatesReadableExportFile(TestResources.ROUND_INPUT_FOLDER, xmlFile);
	}
	
	@Test
	public void testCreateRoundDatasetWithSignals() throws Exception {
		File xmlFile = new File(TestResources.ROUND_SIGNALS_OUTPUT_FOLDER, "Round_with_signals.xml");
		testPipelineCreatesReadableExportFile(TestResources.ROUND_SIGNALS_INPUT_FOLDER, xmlFile);
	}
	
	@Test
	public void testCreateRoundDatasetWithClusters() throws Exception {
		File xmlFile = new File(TestResources.ROUND_CLUSTERS_OUTPUT_FOLDER, "Round_with_clusters.xml");
		testPipelineCreatesReadableExportFile(TestResources.ROUND_CLUSTERS_INPUT_FOLDER, xmlFile);
	}
	
	@Test
	public void testCreatePigDataset() throws Exception {
		File xmlFile = new File(TestResources.PIG_OUTPUT_FOLDER, "Pig.xml");
		testPipelineCreatesReadableExportFile(TestResources.PIG_INPUT_FOLDER, xmlFile);
	}
	
	@Test
	public void testCreatePigDatasetWithSignals() throws Exception {
		File xmlFile      = new File(TestResources.PIG_SIGNALS_OUTPUT_FOLDER, "Pig_with_signals.xml");
		testPipelineCreatesReadableExportFile(TestResources.PIG_SIGNALS_INPUT_FOLDER, xmlFile);
	}
	
	@Test
	public void testCreatePigDatasetWithClusters() throws Exception {
		File xmlFile      = new File(TestResources.PIG_CLUSTERS_OUTPUT_FOLDER, "Pig_with_clusters.xml");
		testPipelineCreatesReadableExportFile(TestResources.PIG_CLUSTERS_INPUT_FOLDER, xmlFile);
	}
	
	@Test
	public void testCreateMouseDataset() throws Exception {
		File xmlFile = new File(TestResources.MOUSE_OUTPUT_FOLDER, "Mouse.xml");
		testPipelineCreatesReadableExportFile(TestResources.MOUSE_INPUT_FOLDER, xmlFile);
	}
	
	@Test
	public void testCreateMouseDatasetWithSignals() throws Exception {
		File xmlFile      = new File(TestResources.MOUSE_SIGNALS_OUTPUT_FOLDER, "Mouse_with_signals.xml");
		testPipelineCreatesReadableExportFile(TestResources.MOUSE_SIGNALS_INPUT_FOLDER, xmlFile);
	}
	
	@Test
	public void testCreateMouseDatasetWithClusters() throws Exception {
		File xmlFile      = new File(TestResources.MOUSE_CLUSTERS_OUTPUT_FOLDER, "Mouse_with_clusters.xml");
		testPipelineCreatesReadableExportFile(TestResources.MOUSE_CLUSTERS_INPUT_FOLDER, xmlFile);
	}
	
	/**
	 * The pipeline is not designed to work with nested folders of images; while nucleus
	 * detection will work, signal detection will not. We need to fail if the folder passed
	 * to the pipeline does not contain analysable images
	 * @throws Exception
	 */
	@Test
	public void testMultipleImageFoldersAbortWhenNoImagesDetected() throws Exception {
		File testFolder   = TestResources.MULTIPLE_BASE_FOLDER;
		File xmlFile      = new File(TestResources.IMAGE_FOLDER, "Multiple_with_DAPI_signals.xml");
		
		// Check the input files exist
		assertTrue("Input image folder"+testFolder.getAbsolutePath(), testFolder.exists());
		assertTrue("XML options file"+xmlFile.getAbsolutePath(), xmlFile.exists());
		
		// Remove expected output files if they exist
		File expectedFile1 = new File(TestResources.DATASET_FOLDER, TestResources.MULTIPLE1 + Io.SAVE_FILE_EXTENSION);
		File expectedFile2 = new File(TestResources.DATASET_FOLDER, TestResources.MULTIPLE2 + Io.SAVE_FILE_EXTENSION);
		
		expectedFile1.delete();
		expectedFile2.delete();
		
		assertFalse(expectedFile1.exists());
		assertFalse(expectedFile2.exists());
		
		// Make the pipeline
		new SavedOptionsAnalysisPipeline(testFolder, xmlFile, TestResources.DATASET_FOLDER).call();

		// Files should now exist
		assertTrue("Analysis output file "+expectedFile1.getAbsolutePath(), expectedFile1.exists());
		assertTrue("Analysis output file "+expectedFile2.getAbsolutePath(), expectedFile2.exists());
		
		assertTrue(validateDataset(expectedFile1));
		assertTrue(validateDataset(expectedFile2));
	}

}
