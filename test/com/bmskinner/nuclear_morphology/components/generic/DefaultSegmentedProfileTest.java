package com.bmskinner.nuclear_morphology.components.generic;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;

public class DefaultSegmentedProfileTest extends DefaultProfileTest {
	
	protected ISegmentedProfile singleSegmentProfile;
	protected ISegmentedProfile doubleSegmentProfile;
	protected final static UUID DOUBLE_SEG_ID_0 = UUID.fromString("00000000-0000-0000-0000-000000000001");
	protected final static UUID DOUBLE_SEG_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000002");
	protected final static UUID DOUBLE_SEG_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000003");
	protected final static UUID DOUBLE_SEG_ID_3 = UUID.fromString("00000000-0000-0000-0000-000000000004");
	
	protected final static int SEG_0_1_SPLIT_INDEX = 50;
	
	
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		singleSegmentProfile = comp.new DefaultSegmentedProfile(data);
		doubleSegmentProfile = comp.new DefaultSegmentedProfile(data);
		doubleSegmentProfile.splitSegment(doubleSegmentProfile.getSegmentContaining(1), SEG_0_1_SPLIT_INDEX, DOUBLE_SEG_ID_0, DOUBLE_SEG_ID_1);
	}
	
	@Test
    public void testGetSegmentIDsReturnsEmptyListWhenNoSegments() throws ProfileException {
		singleSegmentProfile.clearSegments();
        List<UUID> result = singleSegmentProfile.getSegmentIDs();
        assertTrue(result.size()==1);
    }
	
	@Test
	public void testClearSegments() {
	    assertTrue(singleSegmentProfile.hasSegments());
	    singleSegmentProfile.clearSegments();
	    assertTrue(singleSegmentProfile.hasSegments());
	}

	@Test
	public void testHasSegments() {
		assertTrue(singleSegmentProfile.hasSegments());
		singleSegmentProfile.clearSegments();
		assertTrue(singleSegmentProfile.hasSegments());
	}
}
