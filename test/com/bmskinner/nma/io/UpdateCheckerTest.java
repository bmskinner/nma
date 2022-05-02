package com.bmskinner.nma.io;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.bmskinner.nma.components.Version;

public class UpdateCheckerTest {

	@Test
	public void testUpdateSiteFound() {
		assertFalse(UpdateChecker.isUpdateAvailable(Version.currentVersion()));
	}

	@Test
	public void testUpdateFoundForOlderVersion() {
		assertTrue(UpdateChecker.isUpdateAvailable(new Version(1, 13, 0)));
	}

}
