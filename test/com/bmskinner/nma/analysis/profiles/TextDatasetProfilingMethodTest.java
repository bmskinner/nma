package com.bmskinner.nma.analysis.profiles;

import java.io.File;

import org.junit.Test;

import com.bmskinner.nma.TestImageDatasetCreator;
import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.analysis.detection.TextFileDetectionMethod;
import com.bmskinner.nma.analysis.detection.TextFileNucleusFinder;
import com.bmskinner.nma.components.Version;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.DefaultAnalysisOptions;
import com.bmskinner.nma.components.options.DefaultOptions;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.io.DatasetLandmarkExportMethod;
import com.bmskinner.nma.io.DatasetOutlinesExportMethod;
import com.bmskinner.nma.io.SampleDatasetReader;

public class TextDatasetProfilingMethodTest {

	@Test
	public void testRoundObjects() throws Exception {

		// Detect nuclei outlines in the test folder 
		IAnalysisOptions op = new DefaultAnalysisOptions();
		op.setDetectionFolder(CellularComponent.NUCLEUS, TestResources.TEXT_OUTLINES_FOLDER);
		
		// Note that currently we need to specify a ruleset for the constructor of a profile collection
		// Eventually have this inferred from the landmark file? TODO
		op.setRuleSetCollection(RuleSetCollection.roundRuleSetCollection());


		HashOptions nucleus = new DefaultOptions(); 
		nucleus.setDouble(HashOptions.MIN_CIRC, 0.15);
		nucleus.setDouble(HashOptions.MAX_CIRC, 1);

		nucleus.setInt(HashOptions.MIN_SIZE_PIXELS, 500);
		nucleus.setInt(HashOptions.MAX_SIZE_PIXELS, 10000);
		
		nucleus.setInt(HashOptions.CHANNEL, 2);

		nucleus.set(HashOptions.LANDMARK_LOCATION_FILE_KEY, new File(TestResources.TEXT_OUTLINES_FOLDER, "Text_keypoints.tsv"));
		nucleus.set(HashOptions.LANDMARK_RP_NAME, "Longest axis");

		op.setDetectionOptions(CellularComponent.NUCLEUS, nucleus);

		IAnalysisDataset d = new TextFileDetectionMethod(TestResources.TEXT_OUTLINES_FOLDER, op).call().getFirstDataset();

		new TextDatasetProfilingMethod(d).call();

		TestImageDatasetCreator.saveTestDataset(d, new File(TestResources.datasetOutputFolder(), "text_full.nmd"));
	}
	
	@Test
	public void testObjectsFromExportedNMDOutline() throws Exception {
		
		// Export the outlines and landmarks from a known dataset
		IAnalysisDataset source = SampleDatasetReader.openTestMouseDataset();
		new DatasetOutlinesExportMethod( new File(TestResources.datasetOutputFolder(), "Mouse_outlines.txt"),source, new DefaultOptions()).call();
		new DatasetLandmarkExportMethod(new File(TestResources.datasetOutputFolder(), "Mouse_landmarks.tsv"),source, new DefaultOptions()).call();
		
		// Create a new dataset from the saved files

		IAnalysisOptions op = new DefaultAnalysisOptions();
		op.setDetectionFolder(CellularComponent.NUCLEUS, TestResources.datasetOutputFolder());

		// Note that currently we need to specify a ruleset for the constructor of a profile collection
		// Eventually have this inferred from the landmark file? TODO
		op.setRuleSetCollection(RuleSetCollection.mouseSpermRuleSetCollection());


		HashOptions nucleus = new DefaultOptions(); 
		nucleus.setInt(HashOptions.CHANNEL, 2);
		
		nucleus.set(HashOptions.LANDMARK_LOCATION_FILE_KEY, new File(TestResources.datasetOutputFolder(), "Mouse_landmarks.tsv"));
		nucleus.set(HashOptions.LANDMARK_RP_NAME, "Tip of hook");
		
		// Set the column indexes for detection
		nucleus.setInt(TextFileNucleusFinder.IMAGE_FILE_COL, 4);
		nucleus.setInt(TextFileNucleusFinder.NUCLEUS_ID_COL, 2);
		nucleus.setInt(TextFileNucleusFinder.X_COORDINATE_COL, 5);
		nucleus.setInt(TextFileNucleusFinder.Y_COORDINATE_COL, 6);

		op.setDetectionOptions(CellularComponent.NUCLEUS, nucleus);

		IAnalysisDataset d = new TextFileDetectionMethod(TestResources.datasetOutputFolder(), op).call().getFirstDataset();

		new TextDatasetProfilingMethod(d).call();

		TestImageDatasetCreator.saveTestDataset(d, new File(TestResources.datasetOutputFolder(), "Mouse_by_text.nmd"));
	}

}
