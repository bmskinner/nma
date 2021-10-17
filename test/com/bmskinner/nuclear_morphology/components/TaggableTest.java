package com.bmskinner.nuclear_morphology.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.DefaultNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.SegmentedFloatProfile;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;

/**
 * Tests for implementations of the Taggable interface
 * @author ben
 * @since 1.14.0
 *
 */
@RunWith(Parameterized.class)
public class TaggableTest extends ComponentTester {
	
	private Taggable taggable;

	@Parameter(0)
	public Class<? extends Taggable> source;

	@Override
	@Before
	public void setUp() throws Exception{
		super.setUp();
		taggable = createInstance(source);
	}

	/**
	 * Create an instance of the class under test
	 * @param source the class to create
	 * @return
	 * @throws Exception 
	 */
	public static Taggable createInstance(Class<? extends Taggable> source) throws Exception {

		if(source==DefaultNucleus.class){
			IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(1)
					.ofType(RuleSetCollection.roundRuleSetCollection())
					.randomOffsetProfiles(true)
					.segmented().build();
			return d.getCollection().getCells().stream().findFirst().get().getPrimaryNucleus();
		}

		throw new Exception("Unable to create instance of "+source);
	}

	@Parameters
	public static Iterable<Class<? extends Nucleus>> arguments() {
		return Arrays.asList(DefaultNucleus.class);
	}

	@Test
	public void testGetProfileTypeTag() throws MissingLandmarkException, MissingProfileException, ProfileException {
		ISegmentedProfile rawProfile = taggable.getProfile(ProfileType.ANGLE);
		ISegmentedProfile tagProfile = taggable.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
		assertEquals(rawProfile.offset(taggable.getBorderIndex(Landmark.REFERENCE_POINT)).toString(), tagProfile.toString());
	}

	
	@Test
	public void testUpdatingSegmentsInProfile() throws Exception {
		ISegmentedProfile oldProfile = taggable.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
		
		IProfileSegment seg0 = oldProfile.getSegmentAt(0);
		UUID segId = seg0.getID();
		
		int oldStart = seg0.getStartIndex();
		int newStart = oldStart+10;
		
		int oldEnd = seg0.getEndIndex();
		int newEnd = oldEnd+10;

		assertTrue(oldProfile.update(seg0, newStart, newEnd));
		
		taggable.setProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, oldProfile);
		
		// Confirm everything was saved properly
		ISegmentedProfile newProfile = taggable.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
		assertEquals(oldProfile.toString(), newProfile.toString());
	}
	
	/**
	 * Tests that a segment is not altered by being assigned to 
	 * a profile; segmented profiles are assigned to a taggable object
	 * and retrieved, and their endpoints are checked. This test case
	 * uses a profile with multiple segments
	 * @throws Exception
	 */
	@Test
	public void testSettingMultiSegmentProfileIsReversible() throws Exception {
		// Fetch the profile zeroed on the RP
		ISegmentedProfile oldProfile = taggable.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
		
		// Make a duplicate for manipulation - note this will have the same segment pattern
		ISegmentedProfile templateProfile = new SegmentedFloatProfile(oldProfile);
		
		// Set the profile of the object to the newly created profile
		taggable.setProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, templateProfile);
		
		// Fetch the profile back out from the object
		ISegmentedProfile testProfile  = taggable.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
		assertEquals("Profiles should match", templateProfile, testProfile);
		assertEquals("Value at index 0", oldProfile.get(0), testProfile.get(0), 0);
		assertEquals("Segment count", oldProfile.getSegmentCount(), testProfile.getSegmentCount());
		
		// Test the multiple segments match
		IProfileSegment tempSeg  = templateProfile.getSegmentAt(0);	
		IProfileSegment testSeg = testProfile.getSegmentAt(0);	
		assertEquals("Segments should match", tempSeg, testSeg);
	}
	
	/**
	 * Tests that a segment is not altered by being assigned to 
	 * a profile; segmented profiles are assigned to a taggable object
	 * and retrieved, and their endpoints are checked. This test case
	 * uses a profile with a single segment
	 * @throws Exception
	 */
	@Test
	public void testSettingSingleSegmentProfileIsReversible() throws Exception {
		// Fetch the profile zeroed on the RP
		ISegmentedProfile oldProfile = taggable.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
		
		// Make a duplicate for manipulation - note this will have only one segment
		ISegmentedProfile templateProfile = new SegmentedFloatProfile(oldProfile.toFloatArray());
		
		// Set the profile of the object to the newly created profile
		taggable.setProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, templateProfile);
		
		// Fetch the profile back out from the object
		ISegmentedProfile testProfile  = taggable.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
		
		// Test the two profiles are identical
		assertEquals("Profiles should match", templateProfile, testProfile);
		assertEquals("Value at index 0 should be", templateProfile.get(0), testProfile.get(0), 0);
		assertEquals("Segment count should be", 1, testProfile.getSegmentCount());
				
		// Test the single segments match
		IProfileSegment tempSeg  = templateProfile.getSegmentAt(0);	
		IProfileSegment testSeg = testProfile.getSegmentAt(0);	
		assertEquals("Segments should match", tempSeg, testSeg);
	}
	
	@Test
	public void testSettingBorderTags() throws Exception {
		
		int rpIndex = taggable.getBorderIndex(Landmark.REFERENCE_POINT);
		
		int newRpIndex = CellularComponent.wrapIndex(rpIndex+10, taggable.getBorderLength());
		
		taggable.setBorderTag(Landmark.REFERENCE_POINT, newRpIndex);
		assertEquals(newRpIndex, taggable.getBorderIndex(Landmark.REFERENCE_POINT));
	}

}
