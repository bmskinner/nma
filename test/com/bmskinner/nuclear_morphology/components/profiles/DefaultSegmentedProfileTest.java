/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.components.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for the methods specific to the segmented float profile. Common methods are
 * tested by {@link ISegmentedProfileTest}
 * @author bms41
 *
 */
public class DefaultSegmentedProfileTest {
    
    // Ids for the test profile segments
    private static final String SEG_0 = "00000000-0000-0000-0000-000000000000";
    private static final String SEG_1 = "11111111-1111-1111-1111-111111111111";
    private static final String SEG_2 = "22222222-2222-2222-2222-222222222222";
    private static final String SEG_3 = "33333333-3333-3333-3333-333333333333";
        
    private ISegmentedProfile sp;
    
    @Before
    public void setUp() throws ProfileException{
        sp = makeTestProfile();
    }
		
	/**
	 * Create test segments in a linked list
	 * Segments run 10-20-45-89-10
	 * @return
	 * @throws ProfileException 
	 */
	private List<IProfileSegment> makeTestSegments() throws ProfileException{
		List<IProfileSegment> list = new ArrayList<>();
		list.add(makeSeg0());
		list.add(makeSeg1());
		list.add(makeSeg2());
		list.add(makeSeg3());

		IProfileSegment.linkSegments(list);

		return list;
	}
	
	private IProfileSegment makeSeg0(){
	    return new DefaultProfileSegment(10, 20, 100, UUID.fromString(SEG_0));
	}
	
	private IProfileSegment makeSeg1(){
        return new DefaultProfileSegment(20, 45, 100, UUID.fromString(SEG_1));
    }
	
	private IProfileSegment makeSeg2(){
        return new DefaultProfileSegment(45, 89, 100, UUID.fromString(SEG_2));
    }
	
	private IProfileSegment makeSeg3(){
        return new DefaultProfileSegment(89, 10, 100, UUID.fromString(SEG_3));
    }
	
	
	/**
	 * Creata a test profile with segments as in {@link #makeTestSegments()}
	 * @return
	 * @throws ProfileException 
	 */
	private ISegmentedProfile makeTestProfile() throws ProfileException{
		
		List<IProfileSegment> list = makeTestSegments();
		IProfile profile = new DefaultProfile(10, 100);
		return new DefaultSegmentedProfile(profile, list);
	}
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();

	/**
	 * Test that the sample profile has the correct number of segments
	 * @throws ProfileException
	 */
	@Test
	public void testSegmentedFloatProfileIProfileListOfIBorderSegment() throws ProfileException {
	    List<IProfileSegment> list = makeTestSegments();
	    assertEquals(list.size(), sp.getSegmentCount());
	}
		
	/**
	 * Test that a profile cannot be created when a source IProfile is paired with
	 * a list of segments from a different length profile.
	 * @throws ProfileException
	 */
	@Test
    public void testSegmentedFloatProfileIProfileListOfIBorderSegmentExceptsOnMismatchedProfileAndList() throws ProfileException {
        List<IProfileSegment> list = makeTestSegments();
        IProfile profile = new DefaultProfile(10, 110);
        exception.expect(IllegalArgumentException.class);
        new DefaultSegmentedProfile(profile, list);
    }

	/**
	 * Test that a profile created using another profile as a template is equal.
	 * @throws IndexOutOfBoundsException
	 * @throws ProfileException
	 */
	@Test
	public void testSegmentedFloatProfileISegmentedProfile() throws IndexOutOfBoundsException, ProfileException {
	    ISegmentedProfile result = new DefaultSegmentedProfile(sp);
	    assertEquals(sp, result);
	}
	
	/**
	 * Test that a segmented profile created from an IProfile has one default segment
	 * @throws ProfileException 
	 */
	@Test
	public void testSegmentedFloatProfileIProfile() throws ProfileException {
	    IProfile profile = new DefaultProfile(10, 100);
	    ISegmentedProfile p = new DefaultSegmentedProfile(profile);
	    assertEquals(1, p.getSegmentCount());
	    assertEquals("Single segment should have default id", IProfileCollection.DEFAULT_SEGMENT_ID, p.getSegments().get(0).getID());
	}
	
	/**
	 * Test that a profile created from a float array has the correct values
	 * @throws ProfileException 
	 */
	@Test
	public void testSegmentedFloatProfileFloatArray() throws ProfileException {
	    float[] array = new float[100];
        for (int i = 0; i < array.length; i++) {
            array[i] = 10;
        }
        ISegmentedProfile p = new DefaultSegmentedProfile(array);
        
        for (int i = 0; i < array.length; i++) {
            assertEquals(array[i], p.get(i), 0);
        }  
	}
	
	/**
	 * When a segmented profile is created from another profile and a list of 
	 * segments, check that the segments are equal in the new profile. 
	 * @throws Exception 
	 */
	@Test
	public void testConstructorCopiesSegmentState() throws Exception {
		
		// Lock the first segment in the input
		List<IProfileSegment> inputSegments = makeTestSegments();
		
		for(IProfileSegment s: inputSegments)
			s.setLocked(true);
		
		// Make the profile
		IProfile profile = new DefaultProfile(10, 100);
		ISegmentedProfile p = new DefaultSegmentedProfile(profile, inputSegments);
		
		List<IProfileSegment> outputSegments = p.getSegments();
		for(int i=0; i<inputSegments.size(); i++) {
			assertEquals("Segments should match", inputSegments.get(i), outputSegments.get(i));
			assertEquals("Segment lock state should match", 
					inputSegments.get(i).isLocked(), 
					outputSegments.get(i).isLocked());
		}
		
		assertEquals("Segments should match", inputSegments, outputSegments);
	}
	
	@Test
    public void testSegmentedFloatProfileFloatArrayExceptsOnNullArray() throws ProfileException {
	    exception.expect(IllegalArgumentException.class);
        new DefaultSegmentedProfile( (float[]) null);
    }
	
	@Test
    public void testGetSegmentIDsReturnsEmptyListWhenNoSegments() throws ProfileException {
		sp.clearSegments();
        List<UUID> result = sp.getSegmentIDs();
        assertEquals(1, result.size());
    }
	
	@Test
    public void testGetSegmentsReturnsSingleItemListAfterClearing() throws ProfileException {
		sp.clearSegments();
        List<IProfileSegment> result = sp.getSegments();
        assertEquals(1, result.size());
    }
	
	@Test
	public void testClearSegments() throws ProfileException {
	    assertTrue(sp.hasSegments());
	    sp.clearSegments();
	    assertEquals(1,sp.getSegmentCount());
	}

	@Test
	public void testHasSegments() throws ProfileException {
		assertTrue(sp.hasSegments());
		sp.clearSegments();
		assertEquals(1, sp.getSegmentCount());
	}

	@Test
    public void testToString() {
	    StringBuilder builder = new StringBuilder("Profile");
        for (IProfileSegment seg : sp.getOrderedSegments()) {
            builder.append(" | "+seg.toString());
        }

       assertEquals(builder.toString(), sp.toString());
    }
	
	
	@Test
	public void testProfileIsCreatedWhenOnlyOneSegment() throws ProfileException {
		DefaultSegmentedProfile p = new DefaultSegmentedProfile(new DefaultProfile(10, 100));
		assertEquals(1, p.getSegmentCount());
		assertEquals(0, p.getSegments().get(0).getStartIndex());
		assertEquals(0, p.getSegments().get(0).getEndIndex());
	}
}
