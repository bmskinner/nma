package com.bmskinner.nuclear_morphology.io;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.generic.Version;

public class UpdateCheckerTest {

	@Test
	public void testUpdateSiteFound() {
		assertFalse(UpdateChecker.isUpdateAvailable(Version.currentVersion()));
	}
	
	@Test
	public void testUpdateFoundForOlderVersion() {
		assertTrue(UpdateChecker.isUpdateAvailable(Version.v_1_13_0));
	}
	
}
