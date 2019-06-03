package com.bmskinner.nuclear_morphology.io;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class UpdateCheckerTest {

	@Test
	public void testUpdateSiteFound() {
		assertFalse(UpdateChecker.isUpdateAvailable());
	}

}
