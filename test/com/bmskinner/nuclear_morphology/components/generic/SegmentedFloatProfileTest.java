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

package com.bmskinner.nuclear_morphology.components.generic;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment;
import com.bmskinner.nuclear_morphology.components.generic.FloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.SegmentedFloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment.SegmentUpdateException;

/**
 * Tests for the methods specific to the segmented float profile. Common methods are
 * tested by {@link ISegmentedProfileTester}
 * @author bms41
 *
 */
public class SegmentedFloatProfileTest {
    
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
	private List<IBorderSegment> makeTestSegments() throws ProfileException{
		List<IBorderSegment> list = new ArrayList<>();
		list.add(makeSeg0());
		list.add(makeSeg1());
		list.add(makeSeg2());
		list.add(makeSeg3());

		IBorderSegment.linkSegments(list);

		return list;
	}
	
	private IBorderSegment makeSeg0(){
	    return new DefaultBorderSegment(10, 20, 100, UUID.fromString(SEG_0));
	}
	
	private IBorderSegment makeSeg1(){
        return new DefaultBorderSegment(20, 45, 100, UUID.fromString(SEG_1));
    }
	
	private IBorderSegment makeSeg2(){
        return new DefaultBorderSegment(45, 89, 100, UUID.fromString(SEG_2));
    }
	
	private IBorderSegment makeSeg3(){
        return new DefaultBorderSegment(89, 10, 100, UUID.fromString(SEG_3));
    }
	
	
	/**
	 * Creata a test profile with segments as in {@link #makeTestSegments()}
	 * @return
	 * @throws ProfileException 
	 */
	private ISegmentedProfile makeTestProfile() throws ProfileException{
		
		List<IBorderSegment> list = makeTestSegments();
		IProfile profile = new FloatProfile(10, 100);
		return new SegmentedFloatProfile(profile, list);
	}
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Test
	public void testSegmentedFloatProfileIProfileListOfIBorderSegment() throws ProfileException {
	    List<IBorderSegment> list = makeTestSegments();
	    assertEquals(list.size(), sp.getSegmentCount());
	}
		
	@Test
    public void testSegmentedFloatProfileIProfileListOfIBorderSegmentExceptsOnMismatchedProfileAndList() throws ProfileException {
        List<IBorderSegment> list = makeTestSegments();
        IProfile profile = new FloatProfile(10, 110);
        exception.expect(IllegalArgumentException.class);
        new SegmentedFloatProfile(profile, list);
    }

	@Test
	public void testSegmentedFloatProfileISegmentedProfile() throws IndexOutOfBoundsException, ProfileException {
	    ISegmentedProfile result = new SegmentedFloatProfile(sp);
	    assertEquals(sp, result);
	}
	
	@Test
	public void testSegmentedFloatProfileIProfile() {
	    IProfile profile = new FloatProfile(10, 100);
	    ISegmentedProfile p = new SegmentedFloatProfile(profile);
	    assertEquals(1, p.getSegmentCount());
	}
	
	@Test
	public void testSegmentedFloatProfileFloatArray() {
	    float[] array = new float[100];
        for (int i = 0; i < array.length; i++) {
            array[i] = 10;
        }
        ISegmentedProfile p = new SegmentedFloatProfile(array);
        
        for (int i = 0; i < array.length; i++) {
            assertEquals(array[i], p.get(i), 0);
        }  
	}
	
	@Test
    public void testSegmentedFloatProfileFloatArrayExceptsOnNullArray() {
	    exception.expect(IllegalArgumentException.class);
        new SegmentedFloatProfile( (float[]) null);
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
        List<IBorderSegment> result = sp.getSegments();
        assertEquals(1, result.size());
    }
	
	@Test
	public void testClearSegments() {
	    assertTrue(sp.hasSegments());
	    sp.clearSegments();
	    assertEquals(1,sp.getSegmentCount());
	}

	@Test
	public void testHasSegments() {
		assertTrue(sp.hasSegments());
		sp.clearSegments();
		assertEquals(1, sp.getSegmentCount());
	}

	@Test
    public void testToString() {
	    StringBuilder builder = new StringBuilder("Profile");
        for (IBorderSegment seg : sp.getSegments()) {
            builder.append(" | "+seg.toString());
        }

       assertEquals(builder.toString(), sp.toString());
    }
}
