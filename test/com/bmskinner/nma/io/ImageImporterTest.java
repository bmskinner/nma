package com.bmskinner.nma.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.io.ImageImporter.ImageImportException;

import ij.ImagePlus;
import ij.ImageStack;

public class ImageImporterTest {

	@Test
	public void testND2opens() throws ImageImportException, InterruptedException {

		File testFolder = new File(TestResources.IMAGE_FOLDER_BASE, "ND2");

		File nd2File = new File(testFolder, "LS1.nd2");

		assertTrue(nd2File.exists());

		ImageStack ip = new ImageImporter(nd2File).importToStack();

		ImagePlus img = new ImagePlus("title", ip);
		img.show();

		assertEquals(16, ip.getBitDepth());
		assertEquals(1004, ip.getWidth());
		assertEquals(1002, ip.getHeight());
	}

}
