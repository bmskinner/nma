package com.bmskinner.nuclear_morphology.io;

import java.io.File;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace.BioSample;
import com.bmskinner.nuclear_morphology.components.workspaces.WorkspaceFactory;

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
