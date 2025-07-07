package com.bmskinner.nma.components.generic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nma.components.Version;

public class VersionTest {

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Test
	public void testVersion() {
		new Version(1, 13, 8);
	}

	@Test
	public void testParseString() {
		final String s = "1.13.8";
		final Version v = Version.parseString(s);
		assertEquals(new Version(1, 13, 8), v);

		final Version v1 = Version.parseString("1.2.3.4");
		assertFalse(new Version(1, 2, 3).equals(v1));

		exception.expect(IllegalArgumentException.class);
		Version.parseString("1.2");


	}

	@Test
	public void testParseStringWithTooManyParametersExcepts() {
		exception.expect(IllegalArgumentException.class);
		Version.parseString("1.2.3.4.5");
	}

	@Test
	public void testParseStringWithTooFewParametersExcepts() {
		exception.expect(IllegalArgumentException.class);
		Version.parseString("1.2");
	}

	@Test
	public void testParseStringTooManyDigits() {
		exception.expect(IllegalArgumentException.class);
		Version.parseString("1.2");
	}

	@Test
	public void testParseStringTooFewDigits() {
		exception.expect(IllegalArgumentException.class);
		Version.parseString("1.2");
	}

	@Test
	public void testParseStringNotDigits() {
		exception.expect(IllegalArgumentException.class);
		Version.parseString("moose");
	}

	@Test
	public void testIsOlderThan() {
		final Version v1 = Version.parseString("1.13.4");
		final Version v2 = Version.parseString("1.13.5");

		assertTrue(v1.isOlderThan(v2));
		assertFalse(v2.isOlderThan(v1));

		final Version v3 = new Version(1, 5, 1);
		assertTrue(v3.isOlderThan(v2));
		assertFalse(v2.isOlderThan(v3));

		final Version v4 = new Version(2, 3, 2);
		assertTrue(v2.isOlderThan(v4));
		assertFalse(v4.isOlderThan(v2));

		// Check suffix handling
		final Version v5 = new Version(2, 3, 2, "alpha-1");
		final Version v6 = new Version(2, 3, 2, "alpha-2");
		final Version v7 = new Version(2, 3, 2, "beta-1");
		final Version v8 = new Version(2, 3, 2, "beta-2");

		assertTrue(v5.isOlderThan(v4));
		assertTrue(v5.isOlderThan(v6));
		assertTrue(v6.isOlderThan(v7));
		assertTrue(v7.isOlderThan(v8));
		assertTrue(v8.isOlderThan(v4));
		assertTrue(v1.isOlderThan(v8));

	}

	@Test
	public void testIsNewerThan() {
		final Version v1 = Version.parseString("1.13.4");
		final Version v2 = Version.parseString("1.13.5");

		assertTrue(v2.isNewerThan(v1));
		assertFalse(v1.isNewerThan(v2));

		final Version v3 = new Version(1, 5, 1);
		assertTrue(v2.isNewerThan(v3));
		assertFalse(v3.isNewerThan(v2));

		final Version v4 = new Version(2, 3, 2);
		assertTrue(v4.isNewerThan(v2));
		assertFalse(v2.isNewerThan(v4));
	}

	@Test
	public void testEqualsObject() {
		assertFalse(Version.parseString("1.13.4").equals(null));
		assertFalse(Version.parseString("1.13.4").equals(Version.parseString("1.13.3")));
		assertTrue(Version.parseString("1.13.4").equals(new Version(1, 13, 4)));

		final Version v1 = new Version(2, 13, 4);
		assertFalse(Version.parseString("1.13.4").equals(v1));

		final Version v2 = new Version(1, 14, 4);
		assertFalse(Version.parseString("1.13.4").equals(v2));

		assertFalse(Version.parseString("1.13.4").equals(new Object()));
	}

	@Test
	public void testGetMajor() {
		assertEquals(1, Version.parseString("1.13.4").getMajor());
	}

	@Test
	public void testGetMinor() {
		assertEquals(13, Version.parseString("1.13.4").getMinor());
	}

	@Test
	public void testGetRevision() {
		assertEquals(4, Version.parseString("1.13.4").getRevision());
	}

	@Test
	public void testToString() {
		final String s = "1.13.4";
		assertEquals(s, Version.parseString("1.13.4").toString());
	}

	@Test
	public void testVersionIsSupported() {
		assertTrue(Version.versionIsSupported(Version.currentVersion()));
		assertFalse(Version.versionIsSupported(Version.parseString("1.20.0")));
	}

}
