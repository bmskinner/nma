package com.bmskinner.nuclear_morphology.api;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.logging.Level;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.analysis.DatasetValidator;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.io.CellFileExporter;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;
import com.bmskinner.nuclear_morphology.io.Io.Importer;

/**
 * Test the workflows can save a valid dataset
 * @author ben
 * @since 1.14.0
 *
 */
public class SavedOptionsAnalysisPipelineTest extends AnalysisPipelineTest {
	
	protected void testPipelineCreatesReadableExportFile(File imageFolder, File xmlFile, File outputFolder) throws Exception {
		assertTrue("Input image folder"+imageFolder.getAbsolutePath(), imageFolder.exists());
		assertTrue("XML options file"+xmlFile.getAbsolutePath(), xmlFile.exists());
		
		new SavedOptionsAnalysisPipeline(imageFolder, xmlFile, outputFolder).call();
		assertTrue("Analysis output folder "+outputFolder.getAbsolutePath(), outputFolder.exists());
		
		File expectedFile = new File(outputFolder, imageFolder.getName()+Io.SAVE_FILE_EXTENSION);
		assertTrue("Analysis output file "+expectedFile.getAbsolutePath(), expectedFile.exists());
		
		IAnalysisDataset d = SampleDatasetReader.openDataset(expectedFile);
		DatasetValidator dv = new DatasetValidator();
        if (!dv.validate(d)) {
            for (String s : dv.getErrors()) {
                logger.log(Level.SEVERE, s);
            }
        	fail("Dataset failed validation");
        }
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

}
