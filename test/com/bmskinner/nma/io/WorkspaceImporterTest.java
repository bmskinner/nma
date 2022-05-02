package com.bmskinner.nma.io;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.jdom2.JDOMException;
import org.junit.Test;

import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.components.workspaces.IWorkspace;
import com.bmskinner.nma.io.XMLReader.XMLReadingException;

public class WorkspaceImporterTest {

	@Test
	public void testWorkspaceImported() throws JDOMException, IOException, XMLReadingException {
		File f = new File(TestResources.DATASET_FOLDER.getAbsolutePath(), "Example.wrk");
		IWorkspace w = XMLReader.readWorkspace(f);

		assertEquals(TestResources.MOUSE, w.getName());
		assertEquals("Test workspace should have 1 file", 1, w.getFiles().size());
	}
}
