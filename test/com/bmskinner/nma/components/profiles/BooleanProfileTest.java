package com.bmskinner.nma.components.profiles;

import static org.junit.Assert.assertEquals;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nma.logging.ConsoleFormatter;
import com.bmskinner.nma.logging.ConsoleHandler;

public class BooleanProfileTest {
	private static final Logger LOGGER = Logger.getLogger("com.bmskinner.nma");

	@Before
	public void setUp() {
		LogManager.getLogManager().reset();
		LOGGER.setLevel(Level.FINE);
		final Handler h = new ConsoleHandler(new ConsoleFormatter());
		h.setLevel(Level.FINE);
		LOGGER.addHandler(h);
	}

	@Test
	public void testDilate() {
		
		BooleanProfile b = new BooleanProfile(new boolean[] { false, true, true, false, false, false });
		BooleanProfile expected = new BooleanProfile(new boolean[] { true, true, true, true, false, false });
		assertEquals(expected, b.dilate(3));

		// Check gap closing worked
		b = new BooleanProfile(new boolean[] { false, true, false, true, false, false });
		expected = new BooleanProfile(new boolean[] { true, true, true, true, true, false });
		assertEquals(expected, b.dilate(3));
	}

	@Test
	public void testErode() {

		BooleanProfile b = new BooleanProfile(new boolean[] { true, true, true, true, false, false });
		BooleanProfile expected = new BooleanProfile(new boolean[] { false, true, true, false, false, false });
		assertEquals(expected, b.erode(3));

		// Check gap closing worked
		b = new BooleanProfile(new boolean[] { false, true, false, true, false, false });
		expected = new BooleanProfile(new boolean[] { false, true, true, true, false, false });
		assertEquals(expected, b.dilate(3).erode(3));

	}

}
