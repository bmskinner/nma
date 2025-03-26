package com.bmskinner.nma.analysis.profiles;

import java.io.File;

import org.junit.Test;

import com.bmskinner.nma.TestImageDatasetCreator;
import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.analysis.detection.TextFileDetectionMethod;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.DefaultAnalysisOptions;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.OptionsBuilder;
import com.bmskinner.nma.components.rules.RuleSetCollection;

public class TextDatasetProfilingMethodTest {

	/**
	 * Tests outline and landmark files prepared manually from the otline and landmark export methods.
	 * This is because not all the fields exported are needed for a new analysis. 
	 * @throws Exception
	 */
	@Test
	public void testExternallyCreatedMouseDataset() throws Exception {

		// Detect nuclei outlines in the test folder 
		IAnalysisOptions op = new DefaultAnalysisOptions();
		
		// Note that currently we need to specify a ruleset for the constructor of a profile collection
		// Eventually have this inferred from the landmark file? TODO
		op.setRuleSetCollection(RuleSetCollection.mouseSpermRuleSetCollection());

		HashOptions nucleus = new OptionsBuilder()
				.withValue(HashOptions.MIN_CIRC, 0.15)
				.withValue(HashOptions.MAX_CIRC, 1)
				.withValue(HashOptions.MIN_SIZE_PIXELS, 500)
				.withValue(HashOptions.MAX_SIZE_PIXELS, 10000)
				.withValue(HashOptions.CHANNEL, 2)
				.withValue(HashOptions.LANDMARK_LOCATION_FILE_KEY, new File(TestResources.TEXT_OUTLINES_FOLDER, "Mouse_text_landmarks.tsv"))
				.withValue(HashOptions.COORDINATE_LOCATION_FILE_KEY, new File(TestResources.TEXT_OUTLINES_FOLDER, "Mouse_text_outlines.txt"))
				.withValue(HashOptions.LANDMARK_RP_NAME, "Tip of hook")
				.build();

		op.setDetectionOptions(CellularComponent.NUCLEUS, nucleus);

		IAnalysisDataset d = new TextFileDetectionMethod(TestResources.TEXT_OUTLINES_FOLDER, op).call().getFirstDataset();

		new TextDatasetProfilingMethod(d).call();

		TestImageDatasetCreator.saveTestDataset(d, new File(TestResources.datasetOutputFolder(), "Mouse_text_dataset.nmd"));
	}
}
