package com.bmskinner.nma.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nma.utility.FileUtils;

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

}
