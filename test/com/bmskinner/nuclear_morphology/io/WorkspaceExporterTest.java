package com.bmskinner.nuclear_morphology.io;

import java.io.File;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.Version;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace.BioSample;
import com.bmskinner.nuclear_morphology.components.workspaces.WorkspaceFactory;

public class WorkspaceExporterTest {
	
	public static final String SAMPLE_DATASET_PATH = "test/samples/datasets/";

	@Test
	public void testDatasetExported() throws Exception {
		IAnalysisDataset d = SampleDatasetReader.openTestRodentDataset();
		
		IWorkspace w = WorkspaceFactory.createWorkspace(d);
		
		w.addBioSample("wild type");
		BioSample b = w.getBioSample("wild type");
		if(b!=null)
			b.addDataset(d.getSavePath());
		
		w.setSaveFile(new File(SAMPLE_DATASET_PATH+Version.currentVersion(), "Example.wrk"));
		
		WorkspaceExporter.exportWorkspace(w);
	}

}
