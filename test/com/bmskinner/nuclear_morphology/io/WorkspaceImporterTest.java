package com.bmskinner.nuclear_morphology.io;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;

public class WorkspaceImporterTest {
	
//	public static final String SAMPLE_DATASET_PATH = "test/com/bmskinner/nuclear_morphology/samples/datasets/";
	public static final String FILE_NAME_1_14_0 = "1.14.0/Example.wrk";
	public static final String FILE_NAME_1_13_8 = "1.13.8/Example.wrk";

	@Test
	public void test_1_14_0_WorkspaceImported() {
		File f = new File(TestResources.DATASET_FOLDER+FILE_NAME_1_14_0);
		IWorkspace w = WorkspaceImporter.createImporter(f).importWorkspace();

		assertEquals(TestResources.MOUSE, w.getName());
		assertEquals(1, w.getBioSamples().size());
	}
	
	@Test
	public void test_1_13_8_WorkspaceImported() {
		File f = new File(TestResources.DATASET_FOLDER+FILE_NAME_1_13_8);
		IWorkspace w = WorkspaceImporter.createImporter(f).importWorkspace();
	}
	
}
