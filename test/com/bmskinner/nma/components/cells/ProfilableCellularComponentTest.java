package com.bmskinner.nma.components.cells;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.components.TestComponentFactory;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.logging.ConsoleFormatter;
import com.bmskinner.nma.logging.ConsoleHandler;
import com.bmskinner.nma.logging.Loggable;

/**
 * Tests for profilable objects
 * 
 * @author Ben Skinner
 * @since 2.0.0
 *
 */
public class ProfilableCellularComponentTest {

	private static final Logger LOGGER = Logger.getLogger(Loggable.PROJECT_LOGGER);

	static {
		for (Handler h : LOGGER.getHandlers())
			LOGGER.removeHandler(h);
		Handler h = new ConsoleHandler(new ConsoleFormatter());
		LOGGER.setLevel(Level.FINE);
		h.setLevel(Level.FINE);
		LOGGER.addHandler(h);
	}

	/**
	 * When the RP is set, segments should be offset so that a segment boundary is
	 * still at the RP. Test updating a single nucleus repeatedly to ensure there
	 * are no issues caused by saved state.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testProfilesOffsetCorrectlyOnceWhenFetchingFromLandmark() throws Exception {
		// Make a cell
		ICell c = TestComponentFactory.roundCell(50, 50, 50, 50, 0, 10,
				RuleSetCollection.roundRuleSetCollection());
		Nucleus n = c.getPrimaryNucleus();

		// Test setting RP to every position in the profile on an existing cell
		for (int rpIndex = 0; rpIndex < n.getBorderLength(); rpIndex++) {
			n.setOrientationMark(OrientationMark.REFERENCE, rpIndex);

			// A profile starting from RP will have RP at index zero.
			// One segment should start at index 0
			ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
			boolean rpIsOk = isRPOnSegmentBoundary(profile, rpIndex);
			assertTrue("One segment should start at zero in a profile offset to start at RP",
					rpIsOk);
		}
	}

	/**
	 * When the RP is set, segments should be offset so that a segment boundary is
	 * still at the RP. Test setting every possible index as RP on a new nucleus
	 * each time to ensure there are no issues setting any profile indexes.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testProfilesOffsetCorrectlyRepeatedlyWhenFetchingFromLandmark() throws Exception {
		ICell c = TestComponentFactory.roundCell(50, 50, 50, 50, 0, 10,
				RuleSetCollection.roundRuleSetCollection());
		Nucleus n = c.getPrimaryNucleus();

		// Test setting RP to every position in the profile on a fresh cell
		for (int rpIndex = 0; rpIndex < n.getBorderLength(); rpIndex++) {
			c = TestComponentFactory.roundCell(50, 50, 50, 50, 0, 10,
					RuleSetCollection.roundRuleSetCollection());
			n = c.getPrimaryNucleus();
			n.setOrientationMark(OrientationMark.REFERENCE, rpIndex);

			// A profile starting from RP will have RP at index zero.
			// One segment should start at index 0
			ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
			boolean rpIsOk = isRPOnSegmentBoundary(profile, rpIndex);
			assertTrue("One segment should start at zero in a profile offset to start at RP",
					rpIsOk);
		}
	}

	/**
	 * When we offset a profile, then reverse the offset, segments should follow
	 * such that the RP is still on a segment boundary. Ensure that offsetting is
	 * reversible.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testProfilesOffsetCorrectlyReversiblyWhenFetchingFromLandmark() throws Exception {
		ICell c = TestComponentFactory.roundCell(50, 50, 50, 50, 0, 10,
				RuleSetCollection.roundRuleSetCollection());
		Nucleus n = c.getPrimaryNucleus();

		// Test setting RP to every position in the profile on a fresh cell
		for (int rpIndex = 1; rpIndex < n.getBorderLength(); rpIndex++) {
			c = TestComponentFactory.roundCell(50, 50, 50, 50, 0, 10,
					RuleSetCollection.roundRuleSetCollection());
			n = c.getPrimaryNucleus();
			int oldRpIndex = n.getBorderIndex(OrientationMark.REFERENCE);

			n.setOrientationMark(OrientationMark.REFERENCE, rpIndex);

			// A profile starting from RP will have RP at index zero.
			// One segment should start at index 0
			ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
			boolean rpIsOk = isRPOnSegmentBoundary(profile, rpIndex);
			assertTrue("One segment should start at zero in a profile offset to start at RP",
					rpIsOk);

			// Set back to zero landmark
			n.setOrientationMark(OrientationMark.REFERENCE, oldRpIndex);
			// A profile starting from RP will have RP at index zero.
			// One segment should start at index 0
			profile = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
			rpIsOk = isRPOnSegmentBoundary(profile, oldRpIndex);
			assertTrue("One segment should start at zero in a profile offset to start at RP",
					rpIsOk);

		}
	}

	private boolean isRPOnSegmentBoundary(ISegmentedProfile profile, int rpIndex)
			throws SegmentUpdateException {
		for (IProfileSegment s : profile.getSegments()) {
			if (s.getStartIndex() == 0)
				return (true);
		}
		return false;
	}

	/**
	 * Test setting the segment start to locked and unlocked
	 */
	@Test
	public void testSetSegmentStartLock() throws Exception {
		ICell c = TestComponentFactory.roundCell(50, 50, 50, 50, 0, 10,
				RuleSetCollection.roundRuleSetCollection());
		Nucleus n = c.getPrimaryNucleus();
		ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);

		// Get the first segment in the profile
		UUID segId = profile.getSegmentIDs().stream().findFirst().get();

		// Check that the lock succeeded
		n.setSegmentStartLock(true, segId);
		ISegmentedProfile p2 = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
		assertTrue(p2.getSegment(segId).isLocked());

		// Check unlocking succeeded
		n.setSegmentStartLock(false, segId);
		ISegmentedProfile p3 = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
		assertFalse(p3.getSegment(segId).isLocked());
	}

	/**
	 * Test that when a profile is retrived from an object, the segments are linked
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetProfileReturnsLinkedSegments() throws Exception {
		ICell c = TestComponentFactory.roundCell(50, 50, 50, 50, 0, 10,
				RuleSetCollection.roundRuleSetCollection());
		Nucleus n = c.getPrimaryNucleus();
		ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);

		Field f = profile.getClass().getDeclaredField("segments");
		f.setAccessible(true);

		IProfileSegment[] value = (IProfileSegment[]) f.get(profile);
		for (IProfileSegment s : value) {
			assertTrue(s.hasNextSegment());
			assertTrue(s.hasPrevSegment());
		}
	}

	@Test
	public void testDuplicateWithSingleDefaultSegment() throws Exception {
		ICell c = TestComponentFactory.roundCell(50, 50, 50, 50, 0, 10,
				RuleSetCollection.roundRuleSetCollection());
		Nucleus n = c.getPrimaryNucleus();

		Nucleus dup = n.duplicate();
		ComponentTester.testDuplicatesByField(n.getNameAndNumber(), n, dup);
		assertEquals(n, dup);
	}
}
