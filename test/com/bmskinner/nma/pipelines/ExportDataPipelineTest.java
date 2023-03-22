package com.bmskinner.nma.pipelines;

import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;

import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.io.Io;

/**
 * Test the export pipeline is working
 * 
 * @author bs19022
 *
 */
public class ExportDataPipelineTest {

	@Test
	public void testPipelineExportsData() throws Exception {
		File outputFile = new File(
				TestResources.MOUSE_TEST_DATASET.getAbsolutePath() + Io.TAB_FILE_EXTENSION);
		if (outputFile.exists())
			outputFile.delete();
//		new ExportDataPipeline(TestResources.MOUSE_TEST_DATASET).run();
//		assertTrue(outputFile.exists());
		fail("In progress");
	}

}
