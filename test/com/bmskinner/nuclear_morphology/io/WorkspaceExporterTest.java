package com.bmskinner.nuclear_morphology.io;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.DefaultBioSample;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace.BioSample;
import com.bmskinner.nuclear_morphology.components.workspaces.WorkspaceFactory;

public class WorkspaceExporterTest {
	
	public static final String SAMPLE_DATASET_PATH = "test/com/bmskinner/nuclear_morphology/samples/datasets/";
	public static final String FILE_NAME_1_14_0 = "1.14.0/Example.wrk";
	public static final String FILE_NAME_1_13_8 = "1.13.8/Example.wrk";

	@Test
	public void testDatasetExported() throws Exception {
		IAnalysisDataset d = SampleDatasetReader.openTestRodentDataset();
		
		IWorkspace w = WorkspaceFactory.createWorkspace(d);
		
		w.addBioSample("wild type");
		w.getBioSample("wild type").addDataset(d.getSavePath());
		
		w.setSaveFile(new File(SAMPLE_DATASET_PATH+"1.14.0", "Example.wrk"));
		
		WorkspaceExporter exp = WorkspaceExporter.createExporter();
		exp.exportWorkspace(w);
//		fail("Not yet implemented");
	}

}
