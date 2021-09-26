package com.bmskinner.nuclear_morphology.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.logging.Level;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.analysis.DatasetValidator;
import com.bmskinner.nuclear_morphology.api.AnalysisPipeline.AnalysisPipelineException;
import com.bmskinner.nuclear_morphology.components.Version;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;

/**
 * Test the workflows can save a valid dataset
 * @author ben
 * @since 1.14.0
 *
 */
public class SavedOptionsAnalysisPipelineTest extends AnalysisPipelineTest {
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	protected void testPipelineCreatesReadableExportFile(File imageFolder, File xmlFile, File outputFolder) throws Exception {
		assertTrue("Input image folder"+imageFolder.getAbsolutePath(), imageFolder.exists());
		assertTrue("XML options file"+xmlFile.getAbsolutePath(), xmlFile.exists());
		
		new SavedOptionsAnalysisPipeline(imageFolder, xmlFile, outputFolder).call();
		assertTrue("Analysis output folder "+outputFolder.getAbsolutePath(), outputFolder.exists());
		
		File expectedFile = new File(outputFolder, imageFolder.getName()+Io.SAVE_FILE_EXTENSION);
		assertTrue("Analysis output file "+expectedFile.getAbsolutePath(), expectedFile.exists());
		
		assertTrue(validateDataset(expectedFile));
	}

	@Test
	public void testCreateRoundDataset() throws Exception {
		File testFolder   = new File(TestResources.TESTING_ROUND_FOLDER);
		File xmlFile      = new File(TestResources.IMAGE_FOLDER, "Round.xml");
		File outputFolder = new File(TestResources.DATASET_FOLDER+Version.currentVersion());
		testPipelineCreatesReadableExportFile(testFolder, xmlFile, outputFolder);
	}
	
	@Test
	public void testCreateRoundDatasetWithSignals() throws Exception {
		File testFolder   = new File(TestResources.TESTING_ROUND_SIGNALS_FOLDER);
		File xmlFile      = new File(TestResources.IMAGE_FOLDER, "Round_with_signals.xml");
		File outputFolder = new File(TestResources.DATASET_FOLDER+Version.currentVersion());
		testPipelineCreatesReadableExportFile(testFolder, xmlFile, outputFolder);
	}
	
	@Test
	public void testCreateRoundDatasetWithClusters() throws Exception {
		File testFolder   = new File(TestResources.TESTING_ROUND_CLUSTERS_FOLDER);
		File xmlFile      = new File(TestResources.IMAGE_FOLDER, "Round_with_clusters.xml");
		File outputFolder = new File(TestResources.DATASET_FOLDER+Version.currentVersion());
		testPipelineCreatesReadableExportFile(testFolder, xmlFile, outputFolder);
	}
	
	@Test
	public void testCreatePigDataset() throws Exception {
		File testFolder = new File(TestResources.TESTING_PIG_FOLDER);
		File xmlFile = new File(TestResources.IMAGE_FOLDER, "Pig.xml");
		File outputFolder = new File(TestResources.DATASET_FOLDER+Version.currentVersion());
		testPipelineCreatesReadableExportFile(testFolder, xmlFile, outputFolder);
	}
	
	@Test
	public void testCreatePigDatasetWithSignals() throws Exception {
		File testFolder   = new File(TestResources.TESTING_PIG_SIGNALS_FOLDER);
		File xmlFile      = new File(TestResources.IMAGE_FOLDER, "Pig_with_signals.xml");
		File outputFolder = new File(TestResources.DATASET_FOLDER+Version.currentVersion());
		testPipelineCreatesReadableExportFile(testFolder, xmlFile, outputFolder);
	}
	
	@Test
	public void testCreatePigDatasetWithClusters() throws Exception {
		File testFolder   = new File(TestResources.TESTING_PIG_CLUSTERS_FOLDER);
		File xmlFile      = new File(TestResources.IMAGE_FOLDER, "Pig_with_clusters.xml");
		File outputFolder = new File(TestResources.DATASET_FOLDER+Version.currentVersion());
		testPipelineCreatesReadableExportFile(testFolder, xmlFile, outputFolder);
	}
	
	@Test
	public void testCreateMouseDataset() throws Exception {
		File testFolder = new File(TestResources.TESTING_MOUSE_FOLDER);
		File xmlFile = new File(TestResources.IMAGE_FOLDER, "Mouse.xml");
		File outputFolder = new File(TestResources.DATASET_FOLDER+Version.currentVersion());
		testPipelineCreatesReadableExportFile(testFolder, xmlFile, outputFolder);
	}
	
	@Test
	public void testCreateMouseDatasetWithSignals() throws Exception {
		File testFolder   = new File(TestResources.TESTING_MOUSE_SIGNALS_FOLDER);
		File xmlFile      = new File(TestResources.IMAGE_FOLDER, "Mouse_with_signals.xml");
		File outputFolder = new File(TestResources.DATASET_FOLDER+Version.currentVersion());
		testPipelineCreatesReadableExportFile(testFolder, xmlFile, outputFolder);
	}
	
	@Test
	public void testCreateMouseDatasetWithClusters() throws Exception {
		File testFolder   = new File(TestResources.TESTING_MOUSE_CLUSTERS_FOLDER);
		File xmlFile      = new File(TestResources.IMAGE_FOLDER, "Mouse_with_clusters.xml");
		File outputFolder = new File(TestResources.DATASET_FOLDER+Version.currentVersion());
		testPipelineCreatesReadableExportFile(testFolder, xmlFile, outputFolder);
	}
	
	/**
	 * The pipeline is not designed to work with nested folders of images; while nucleus
	 * detection will work, signal detection will not. We need to fail if the folder passed
	 * to the pipeline does not contain analysable images
	 * @throws Exception
	 */
	@Test
	public void testMultipleImageFoldersAbortWhenNoImagesDetected() throws Exception {
		File testFolder   = new File(TestResources.TESTING_MULTIPLE_BASE_FOLDER);
		File xmlFile      = new File(TestResources.IMAGE_FOLDER, "Multiple_with_DAPI_signals.xml");
		File outputFolder = new File(TestResources.DATASET_FOLDER+Version.currentVersion());
		
		// Check the input files exist
		assertTrue("Input image folder"+testFolder.getAbsolutePath(), testFolder.exists());
		assertTrue("XML options file"+xmlFile.getAbsolutePath(), xmlFile.exists());
		
		// Remove expected output files if they exist
		File expectedFile1 = new File(outputFolder, TestResources.MULTIPLE1 + Io.SAVE_FILE_EXTENSION);
		File expectedFile2 = new File(outputFolder, TestResources.MULTIPLE2 + Io.SAVE_FILE_EXTENSION);
		
		expectedFile1.delete();
		expectedFile2.delete();
		
		assertFalse(expectedFile1.exists());
		assertFalse(expectedFile2.exists());
		
		// Make the pipeline
		new SavedOptionsAnalysisPipeline(testFolder, xmlFile, outputFolder).call();

		// Files should now exist
		assertTrue("Analysis output file "+expectedFile1.getAbsolutePath(), expectedFile1.exists());
		assertTrue("Analysis output file "+expectedFile2.getAbsolutePath(), expectedFile2.exists());
		
		assertTrue(validateDataset(expectedFile1));
		assertTrue(validateDataset(expectedFile2));
	}

}
