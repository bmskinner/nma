package com.bmskinner.nuclear_morphology.api;

import java.io.File;

import org.junit.Test;

public class SavedOptionsAnalysisPipelineTest extends AnalysisPipelineTest {



	@Test
	public void testRoundDataset() throws Exception {
		
		File testFolder = new File(TESTING_ROUND_FOLDER);
		File xmlFile = new File(IMAGE_FOLDER, "Red_green_shell_cluster.xml");
		
		new SavedOptionsAnalysisPipeline(testFolder, xmlFile).call();
	}

}
