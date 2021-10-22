package com.bmskinner.nuclear_morphology.io;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.jdom2.JDOMException;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;

public class WorkspaceImporterTest {

	@Test
	public void testWorkspaceImported() throws JDOMException, IOException {
		File f = new File(TestResources.DATASET_FOLDER.getAbsolutePath(), "Example.wrk");
		IWorkspace w = WorkspaceImporter.importWorkspace(f);

		assertEquals(TestResources.MOUSE, w.getName());
		assertEquals(1, w.getBioSamples().size());
	}	
}
