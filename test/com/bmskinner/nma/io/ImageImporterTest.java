package com.bmskinner.nma.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.io.ImageImporter.ImageImportException;
import com.bmskinner.nma.visualisation.image.ImageFilterer;

import ij.ImageStack;
import ij.process.ImageProcessor;

public class ImageImporterTest {

	@Test
	public void testND2opens() throws ImageImportException, InterruptedException {

		final File testFolder = new File(TestResources.IMAGE_FOLDER_BASE, "ND2");

		final File nd2File = new File(testFolder, "LS1.nd2");

		assertTrue("The test ND2 file does not exist: %s".formatted(nd2File.getAbsolutePath()), nd2File.exists());

		final ImageStack ip = ImageImporter.importToStack(nd2File);

		assertEquals("ND2 test image dimensions should match", 1004, ip.getWidth());
		assertEquals("ND2 test image dimensions should match", 1002, ip.getHeight());
	}

	@Test
	public void testND2Is8Bit() throws ImageImportException, InterruptedException {

		final File testFolder = new File(TestResources.IMAGE_FOLDER_BASE, "ND2");

		final File nd2File = new File(testFolder, "LS1.nd2");

		assertTrue("The test ND2 file does not exist: %s".formatted(nd2File.getAbsolutePath()), nd2File.exists());

		final ImageStack is = ImageImporter.importToStack(nd2File);

		final ImageProcessor ip = is.getProcessor(1);

		assertEquals("ND2 image should be 8 bit", 8, ip.getBitDepth());
	}

	/**
	 * Confirm the ND2 read can have Canny detection run
	 * 
	 * @throws ImageImportException
	 * @throws InterruptedException
	 */
	@Test
	public void testNDCanBeProcessedWithCanny() throws ImageImportException, InterruptedException {

		final File testFolder = new File(TestResources.IMAGE_FOLDER_BASE, "ND2");

		final File nd2File = new File(testFolder, "LS1.nd2");

		assertTrue("The test ND2 file does not exist: %s".formatted(nd2File.getAbsolutePath()), nd2File.exists());

		final ImageStack is = ImageImporter.importToStack(nd2File);

		final ImageProcessor ip = is.getProcessor(1);

		final ImageFilterer filt = new ImageFilterer(ip);
		final HashOptions op = OptionsFactory.makeCannyOptions().build();

		filt.cannyEdgeDetection(op);

	}

}
