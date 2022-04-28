package com.bmskinner.nuclear_morphology.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.DefaultNucleus;
import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.profiles.DefaultProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.DefaultSegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.rules.OrientationMark;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;

/**
 * Tests for implementations of the Taggable interface
 * 
 * @author ben
 * @since 1.14.0
 *
 */
@RunWith(Parameterized.class)
public class TaggableTest {

	private Taggable taggable;

	@Parameter(0)
	public Class<? extends Taggable> source;

	@Before
	public void setUp() throws Exception {
		taggable = createInstance(source);
	}

	/**
	 * Create an instance of the class under test
	 * 
	 * @param source the class to create
	 * @return
	 * @throws Exception
	 */
	public static Taggable createInstance(Class<? extends Taggable> source) throws Exception {

		if (source == DefaultNucleus.class) {
			IAnalysisDataset d = new TestDatasetBuilder(ComponentTester.RNG_SEED).cellCount(1)
					.ofType(RuleSetCollection.roundRuleSetCollection()).randomOffsetProfiles(true).segmented().build();
			return d.getCollection().getCells().stream().findFirst().get().getPrimaryNucleus();
		}

		throw new Exception("Unable to create instance of " + source);
	}

	@Parameters
	public static Iterable<Class<? extends Nucleus>> arguments() {
		return Arrays.asList(DefaultNucleus.class);
	}

	/**
	 * Ensure that getting a profile type is equivalent to getting the profile
	 * offset to the RP
	 * 
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 * @throws ProfileException
	 */
	@Test
	public void testGetProfileTypeLandmark()
			throws MissingLandmarkException, MissingProfileException, ProfileException {
		ISegmentedProfile rawProfile = taggable.getProfile(ProfileType.ANGLE);
		ISegmentedProfile tagProfile = taggable.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
		assertEquals(rawProfile, tagProfile);
	}

	@Test
	public void testUpdatingSegmentsInProfile() throws Exception {

		// Get the profile to update
		ISegmentedProfile oldProfile = taggable.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);

		IProfileSegment seg0 = oldProfile.getSegments().get(1);
		UUID segId = seg0.getID();

		int oldStart = seg0.getStartIndex();
		int newStart = oldStart + 10;

		int oldEnd = seg0.getEndIndex();
		int newEnd = oldEnd + 10;

		// Check the update was successful
		assertTrue(oldProfile.update(seg0, newStart, newEnd));

		// Put the updated profile back into the nucleus
		taggable.setSegments(oldProfile.getSegments());

		// Confirm everything was saved properly
		ISegmentedProfile newProfile = taggable.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
		assertEquals(oldProfile, newProfile);
	}

	/**
	 * Tests that a segment is not altered by being assigned to a profile; segmented
	 * profiles are assigned to a taggable object and retrieved, and their endpoints
	 * are checked. This test case uses a profile with multiple segments
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSettingMultiSegmentProfileIsReversible() throws Exception {
		// Fetch the profile zeroed on the RP
		ISegmentedProfile oldProfile = taggable.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);

		// Make a duplicate for manipulation - note this will have the same segment
		// pattern
		ISegmentedProfile templateProfile = new DefaultSegmentedProfile(oldProfile);

		// Set the profile of the object to the newly created profile
		taggable.setSegments(templateProfile.getSegments());

		// Fetch the profile back out from the object
		ISegmentedProfile testProfile = taggable.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
		assertEquals("Profiles should match", templateProfile, testProfile);
		assertEquals("Value at index 0", oldProfile.get(0), testProfile.get(0), 0);
		assertEquals("Segment count", oldProfile.getSegmentCount(), testProfile.getSegmentCount());

		// Test the multiple segments match
		IProfileSegment tempSeg = templateProfile.getSegments().get(0);
		IProfileSegment testSeg = testProfile.getSegments().get(0);
		assertEquals("Segments should match", tempSeg, testSeg);
	}

	/**
	 * Tests that a segment is not altered by being assigned to a profile; segmented
	 * profiles are assigned to a taggable object and retrieved, and their endpoints
	 * are checked. This test case uses a profile with a single segment
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSettingSingleSegmentProfileIsReversible() throws Exception {
		// Fetch the profile zeroed on the RP
		ISegmentedProfile oldProfile = taggable.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);

		// Make a duplicate for manipulation - note this will have only one segment
		ISegmentedProfile templateProfile = new DefaultSegmentedProfile(oldProfile.toFloatArray());

		// Set the profile of the object to the newly created profile
		taggable.setSegments(templateProfile.getSegments());

		// Fetch the profile back out from the object
		ISegmentedProfile testProfile = taggable.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);

		// Test the two profiles are identical
		assertEquals("Profiles should match", templateProfile, testProfile);
		assertEquals("Value at index 0 should be", templateProfile.get(0), testProfile.get(0), 0);
		assertEquals("Segment count should be", 1, testProfile.getSegmentCount());

		// Test the single segments match
		IProfileSegment tempSeg = templateProfile.getSegments().get(0);
		IProfileSegment testSeg = testProfile.getSegments().get(0);
		assertEquals("Segments should match", tempSeg, testSeg);
	}

	@Test
	public void testSettingBorderTags() throws Exception {

		int rpIndex = taggable.getBorderIndex(OrientationMark.REFERENCE);

		int newRpIndex = CellularComponent.wrapIndex(rpIndex + 10, taggable.getBorderLength());

		taggable.setOrientationMark(OrientationMark.REFERENCE, newRpIndex);
		assertEquals(newRpIndex, taggable.getBorderIndex(OrientationMark.REFERENCE));
	}

	@Test
	public void testSegmentsCanBeMerged() throws Exception {
		ISegmentedProfile profile = taggable.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
		UUID segId1 = profile.getSegments().get(1).getID();
		UUID segId2 = profile.getSegments().get(2).getID();

		UUID newId = UUID.randomUUID();
		profile.mergeSegments(segId1, segId2, newId);
		taggable.setSegments(profile.getSegments());

		ISegmentedProfile result = taggable.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);

		assertEquals(profile, result);

		assertTrue(result.getSegment(newId).hasMergeSources());

	}

	@Test
	public void testGetSegmentsReturnsLinkedSegments() throws Exception {
		ISegmentedProfile profile = taggable.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
		List<IProfileSegment> testSegs = profile.getSegments();

		for (int i = 0; i < profile.getSegmentCount(); i++) {
			assertTrue(testSegs.get(i).hasNextSegment());
			assertTrue(testSegs.get(i).hasPrevSegment());
		}
	}

	@Test
	public void testSetSegmentsUpdatesReplacesExistingSegments() throws Exception {
		ISegmentedProfile profile = taggable.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);

		List<IProfileSegment> newSegs = new ArrayList<>();
		newSegs.add(new DefaultProfileSegment(0, profile.size() / 2, profile.size()));
		newSegs.add(new DefaultProfileSegment(profile.size() / 2, 0, profile.size()));
		IProfileSegment.linkSegments(newSegs);

		ISegmentedProfile newProfile = new DefaultSegmentedProfile(profile, newSegs);

		taggable.setSegments(newProfile.getSegments());
		assertEquals(newProfile, taggable.getProfile(ProfileType.ANGLE));
	}

	@Test
	public void testReverseBorderReversesSegments() throws Exception {
		ISegmentedProfile profile = taggable.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);

		taggable.reverse();

		ISegmentedProfile revProfile = taggable.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);

		assertEquals("Reversed profile should have the same number of segments", profile.getSegmentCount(),
				revProfile.getSegmentCount());

		for (IProfileSegment s : profile.getSegments()) {
			assertTrue(revProfile.hasSegment(s.getID()));
		}

	}

}
