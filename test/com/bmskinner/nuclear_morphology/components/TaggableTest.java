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
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusType;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.SegmentedFloatProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Tag;
import com.bmskinner.nuclear_morphology.components.profiles.UnavailableProfileTypeException;

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
					.ofType(NucleusType.ROUND)
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
	public void testGetProfileTypeTag() throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException {
		ISegmentedProfile rawProfile = taggable.getProfile(ProfileType.ANGLE);
		ISegmentedProfile tagProfile = taggable.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
		assertEquals(rawProfile.offset(taggable.getBorderIndex(Tag.REFERENCE_POINT)).toString(), tagProfile.toString());
	}

	
	@Test
	public void testUpdatingSegmentsInProfile() throws Exception {
		ISegmentedProfile oldProfile = taggable.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
		
		IProfileSegment seg0 = oldProfile.getSegmentAt(0);
		UUID segId = seg0.getID();
		
		int oldStart = seg0.getStartIndex();
		int newStart = oldStart+10;
		
		int oldEnd = seg0.getEndIndex();
		int newEnd = oldEnd+10;

		assertTrue(oldProfile.update(seg0, newStart, newEnd));
		
		taggable.setProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, oldProfile);
		
		// Confirm everything was saved properly
		ISegmentedProfile newProfile = taggable.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
		assertEquals(oldProfile.toString(), newProfile.toString());
	}
	
	@Test
	public void testSettingMultiSegmentProfileIsReversible() throws Exception {
		// Fetch the profile zeroed on the RP
		ISegmentedProfile oldProfile = taggable.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
		ISegmentedProfile templateProfile = new SegmentedFloatProfile(oldProfile);
		
		taggable.setProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, templateProfile);
		ISegmentedProfile testProfile  = taggable.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
		
		assertEquals("Value at index 0", oldProfile.get(0), testProfile.get(0), 0);
		assertEquals("Segment count", 1, oldProfile.getSegmentCount(), testProfile.getSegmentCount());
		
		// Test multi segments
		IProfileSegment oldSeg = oldProfile.getSegmentAt(0);	
		IProfileSegment testSeg = testProfile.getSegmentAt(0);	
		assertEquals("Segment start", oldSeg.getStartIndex(), testSeg.getStartIndex());
		
		// Test default segments
		IProfileSegment oldDefaultSeg = oldProfile.getSegment(IProfileCollection.DEFAULT_SEGMENT_ID);
		IProfileSegment testDefaultSeg = testProfile.getSegment(IProfileCollection.DEFAULT_SEGMENT_ID);
		assertEquals("Default segment start", oldDefaultSeg.getStartIndex(), testDefaultSeg.getStartIndex());
		
		
	}
	
	@Test
	public void testSettingSingleSegmentProfileIsReversible() throws Exception {
		// Fetch the profile zeroed on the RP
		ISegmentedProfile oldProfile = taggable.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
		ISegmentedProfile templateProfile = new SegmentedFloatProfile(oldProfile.toFloatArray());
		
		taggable.setProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, templateProfile);
		ISegmentedProfile testProfile  = taggable.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
		
		assertEquals("Value at index 0", oldProfile.get(0), testProfile.get(0), 0);
		assertEquals("Segment count", 1, templateProfile.getSegmentCount(), testProfile.getSegmentCount());
		
		// Test multi segments
		IProfileSegment oldSeg = oldProfile.getSegmentAt(0);	
		IProfileSegment testSeg = testProfile.getSegmentAt(0);	
		assertEquals("Segment start", oldSeg.getStartIndex(), testSeg.getStartIndex());
		
		// Test default segments
		IProfileSegment oldDefaultSeg = oldProfile.getSegment(IProfileCollection.DEFAULT_SEGMENT_ID);
		IProfileSegment templateDefaultSeg = templateProfile.getSegment(IProfileCollection.DEFAULT_SEGMENT_ID);
		IProfileSegment testDefaultSeg = testProfile.getSegment(IProfileCollection.DEFAULT_SEGMENT_ID);
		assertEquals("Default segment start", templateDefaultSeg.getStartIndex(), testDefaultSeg.getStartIndex());
		assertEquals("Default segment start", oldDefaultSeg.getStartIndex(), testDefaultSeg.getStartIndex());
	}
	
	@Test
	public void testSettingBorderTags() throws Exception {
		
		int rpIndex = taggable.getBorderIndex(Tag.REFERENCE_POINT);
		
		int newRpIndex = CellularComponent.wrapIndex(rpIndex+10, taggable.getBorderLength());
		
		taggable.setBorderTag(Tag.REFERENCE_POINT, newRpIndex);
		assertEquals(newRpIndex, taggable.getBorderIndex(Tag.REFERENCE_POINT));
	}

}
