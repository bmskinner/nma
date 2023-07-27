package com.bmskinner.nma.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.io.ImageImporter.ImageImportException;

import ij.ImageStack;
import ij.process.ImageProcessor;

public class ImageImporterTest {

	@Test
	public void testND2opens() throws ImageImportException, InterruptedException {

		File testFolder = new File(TestResources.IMAGE_FOLDER_BASE, "ND2");

		File nd2File = new File(testFolder, "LS1.nd2");

		assertTrue(nd2File.exists());

		ImageStack ip = ImageImporter.importToStack(nd2File);

		assertEquals(1004, ip.getWidth());
		assertEquals(1002, ip.getHeight());
	}

	@Test
	public void testND2Is8Bit() throws ImageImportException, InterruptedException {

		File testFolder = new File(TestResources.IMAGE_FOLDER_BASE, "ND2");

		File nd2File = new File(testFolder, "LS1.nd2");

		assertTrue(nd2File.exists());

		ImageStack is = ImageImporter.importToStack(nd2File);

		ImageProcessor ip = is.getProcessor(1);

		assertEquals("Should be 8 bit", 8, ip.getBitDepth());
	}

}
