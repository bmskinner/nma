package com.bmskinner.nma.io;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.workspaces.IWorkspace;
import com.bmskinner.nma.components.workspaces.WorkspaceFactory;

public class WorkspaceExporterTest {

	/**
	 * Test that a workspace file can be created and read
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMouseDatasetWorkspaceCreated() throws Exception {

		final IAnalysisDataset dataset = SampleDatasetReader.openDataset(TestResources.MOUSE_TEST_DATASET);

		// Use mouse dataset to create a workspace
		final IWorkspace w = WorkspaceFactory.createWorkspace(dataset);

		final File f = new File(dataset.getSavePath().getParent(), TestResources.WORKSPACE_NAME);
		w.setSaveFile(f);

		WorkspaceExporter.exportWorkspace(w);

		final IWorkspace result = XMLReader.readWorkspace(f);

		assertEquals("The test workspace dataset should be the mouse dataset", TestResources.MOUSE, result.getName());
		assertEquals("Test workspace should have 1 file", 1, result.getFiles().size());
	}
}
