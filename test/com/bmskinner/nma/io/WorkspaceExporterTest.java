package com.bmskinner.nma.io;

import java.io.File;

import org.junit.Test;

import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.workspaces.IWorkspace;
import com.bmskinner.nma.components.workspaces.WorkspaceFactory;

public class WorkspaceExporterTest {

	@Test
	public void testDatasetExported() throws Exception {
		IAnalysisDataset d = SampleDatasetReader.openTestRodentDataset();

		IWorkspace w = WorkspaceFactory.createWorkspace(d);

		File f = new File(TestResources.DATASET_FOLDER.getAbsolutePath(), "Example.wrk");
		w.setSaveFile(f);

		WorkspaceExporter.exportWorkspace(w);
	}

}
