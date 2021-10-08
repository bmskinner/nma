package com.bmskinner.nuclear_morphology.io;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.components.Version;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;

public class WorkspaceImporterTest {

	public static final String FILE_NAME = "Example.wrk";

	@Test
	public void testWorkspaceImported() {
		File f = new File(TestResources.DATASET_FOLDER.getAbsolutePath()+Version.currentVersion(), FILE_NAME);
		IWorkspace w = WorkspaceImporter.createImporter(f).importWorkspace();

		assertEquals(TestResources.MOUSE, w.getName());
		assertEquals(1, w.getBioSamples().size());
	}	
}
