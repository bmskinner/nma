package com.bmskinner.nma.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;

import org.junit.Before;
import org.junit.Test;

public class FileUtilsTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testExtantComponent() {
		File f = new File(System.getProperty("user.home"));
		assertTrue(f.exists());
		File g = FileUtils.extantComponent(f);
		assertNotNull(g);
		assertEquals(f, g);
	}

	@Test
	public void testExtantComponentHandlesNonExistentFiles() {
		File f = new File("Q:/non/existant/folder");
		assertFalse(f.exists());
		File g = FileUtils.extantComponent(f);
		assertNotNull(g);
		assertEquals(new File(System.getProperty("user.home")), g);
	}

	@Test
	public void testJarFileCanBeRead() throws MalformedURLException, IOException {
		File destination = new File("test", "file-dest");

		org.apache.commons.io.FileUtils.deleteQuietly(destination);
		assertFalse(Files.exists(destination.toPath()));

		File jarFile = new File(
				"C:\\Users\\ben\\workspace\\Nuclear_morphology\\target\\Nuclear_Morphology_Analysis_2.0.0_beta_2.jar");

		assertTrue(Files.exists(jarFile.toPath()));

		URL fileSysUrl = new URL(
				"jar:file:/" + jarFile.getAbsolutePath() + "!/help-book");
		// Create a jar URL connection object
		JarURLConnection jarURLConn = (JarURLConnection) fileSysUrl.openConnection();

		FileUtils.copyJarResourcesRecursively(destination, jarURLConn);

		assertTrue(Files.exists(destination.toPath()));
		org.apache.commons.io.FileUtils.deleteQuietly(destination);
	}

}
