package com.bmskinner.nma.io;

import java.io.File;

import org.junit.Test;

import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.workspaces.IWorkspace;
import com.bmskinner.nma.components.workspaces.WorkspaceFactory;
import com.bmskinner.nma.components.workspaces.IWorkspace.BioSample;
import com.bmskinner.nma.io.WorkspaceExporter;

public class WorkspaceExporterTest {
	
	@Test
	public void testDatasetExported() throws Exception {
		IAnalysisDataset d = SampleDatasetReader.openTestRodentDataset();
		
		IWorkspace w = WorkspaceFactory.createWorkspace(d);
		
		w.addBioSample("wild type");
		BioSample b = w.getBioSample("wild type");
		if(b!=null)
			b.addDataset(d.getSavePath());
		
		File f = new File(TestResources.DATASET_FOLDER.getAbsolutePath(), "Example.wrk");
		w.setSaveFile(f);
		
		WorkspaceExporter.exportWorkspace(w);
	}

}
