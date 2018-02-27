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
import com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment;
import com.bmskinner.nuclear_morphology.components.generic.FloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.SegmentedFloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment.SegmentUpdateException;

public class SegmentedFloatProfileTest {
    
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
	public void testReverse() throws ProfileException {
	    ISegmentedProfile testProfile = makeTestProfile();
	    sp.reverse();
	    
	    for(int i=0, j=sp.getSegmentCount()-1; i<sp.getSegmentCount(); i++, j--){
	        IBorderSegment test   = testProfile.getSegmentAt(i);
	        IBorderSegment result = sp.getSegmentAt(j);
	        
	        assertEquals(test.getID(), result.getID());
	        assertEquals(test.length(), result.length()); 
	    } 
	}

	@Test
	public void testSegmentedFloatProfileIProfileListOfIBorderSegment() {
		fail("Not yet implemented");
	}

	@Test
	public void testSegmentedFloatProfileISegmentedProfile() {
		fail("Not yet implemented");
	}

	@Test
	public void testSegmentedFloatProfileIProfile() {
		fail("Not yet implemented");
	}

	@Test
	public void testSegmentedFloatProfileFloatArray() {
		fail("Not yet implemented");
	}

	@Test
	public void testHasSegments() {
		assertTrue(sp.hasSegments());
		sp.clearSegments();
		assertFalse(sp.hasSegments());
	}
	
	@Test
	public void testGetSegments() throws ProfileException {
	    List<IBorderSegment> test = makeTestSegments();
	    List<IBorderSegment> result = sp.getSegments();
	    
	    assertEquals(test.size(), result.size());
	    for(int i=0; i<test.size(); i++){
	        assertEquals(test.get(i), result.get(i));
	    }

	}
	
	@Test
    public void testGetSegmentsReturnsEmptyListAfterClearing() throws ProfileException {
        sp.clearSegments();
        List<IBorderSegment> result = sp.getSegments();
        assertTrue(result.isEmpty());
    }

	@Test
	public void testSegmentIterator() throws Exception {
        List<IBorderSegment> result = sp.getSegments(); 
        Iterator<IBorderSegment> it = sp.segmentIterator();
        int i=0;
        while(it.hasNext()){
            assertEquals(result.get(i++), it.next());
        }
	}

	@Test
	public void testGetSegmentUUID() throws ProfileException, UnavailableComponentException {
	    List<IBorderSegment> test = makeTestSegments();
	    
	    for(IBorderSegment s : test){
	        IBorderSegment seg = sp.getSegment(s.getID());
	        assertEquals(s, seg);
	    }
	}
	
	@Test
    public void testGetSegmentUUIDExceptsOnNullId() throws ProfileException, UnavailableComponentException {
	    exception.expect(IllegalArgumentException.class);
	    IBorderSegment seg = sp.getSegment( (UUID) null);
    }
	
	@Test
    public void testGetSegmentUUIDExceptsOnInvalidId() throws ProfileException, UnavailableComponentException {
	    UUID notPresent = UUID.fromString("00000001-1000-1000-1000-100000000000");
        exception.expect(UnavailableComponentException.class);
        IBorderSegment seg = sp.getSegment( notPresent);
    }

	@Test
	public void testHasSegment() throws ProfileException, UnavailableComponentException {
	    List<IBorderSegment> test = makeTestSegments();
        
        for(IBorderSegment s : test){
            IBorderSegment seg = sp.getSegment(s.getID());
            assertEquals(s, seg);
        }
	}

	@Test
	public void testGetSegmentsFrom() throws Exception {
	    
	    List<IBorderSegment> test = makeTestSegments();
	    List<IBorderSegment> result = sp.getSegmentsFrom(UUID.fromString(SEG_1));
	    assertEquals(test.get(1), result.get(0));
	}
	
	@Test
    public void testGetSegmentsFromExceptsOnNullId() throws Exception {
	    exception.expect(IllegalArgumentException.class);
        List<IBorderSegment> result = sp.getSegmentsFrom((UUID) null);
    }
	
	@Test
    public void testGetSegmentsFromExceptsOnInvalidId() throws Exception {
	    UUID notPresent = UUID.fromString("00000001-1000-1000-1000-100000000000");
        exception.expect(UnavailableComponentException.class);
        List<IBorderSegment> result = sp.getSegmentsFrom(notPresent);
    }

	@Test
	public void testGetOrderedSegments() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSegmentString() {
	    IBorderSegment test = makeSeg1();
	    String name = "Seg_1";
	    IBorderSegment result = sp.getSegment(name);
	    assertEquals(test.getID(), result.getID());
        assertEquals(test.getStartIndex(), result.getStartIndex());
        assertEquals(test.getEndIndex(), result.getEndIndex());
	}
	
	@Test
    public void testGetSegmentStringExceptsWithNullInput(){
        exception.expect(IllegalArgumentException.class);
        sp.getSegment( (String)null);
    }

	@Test
	public void testGetSegmentIBorderSegment() {
	    IBorderSegment test = makeSeg1();
	    IBorderSegment result = sp.getSegment(test);
	    assertEquals(test.getID(), result.getID());
	    assertEquals(test.getStartIndex(), result.getStartIndex());
	    assertEquals(test.getEndIndex(), result.getEndIndex());
	}
	
	@Test
    public void testGetSegmentIBorderSegmentExceptsWithNullInput(){
        exception.expect(IllegalArgumentException.class);
        sp.getSegment( (IBorderSegment)null);
    }

	@Test
	public void testGetSegmentAt() {
	    IBorderSegment s = sp.getSegmentAt(1);
	    assertEquals(UUID.fromString(SEG_1), s.getID());
	}
	
	@Test
	public void testGetSegmentAtExceptsWithInvalidInputLow(){
	    exception.expect(IllegalArgumentException.class);
	    sp.getSegmentAt(-1);
	}
	
	@Test
	public void testGetSegmentAtExceptsWithInvalidInputHigh(){
	    exception.expect(IllegalArgumentException.class);
	    sp.getSegmentAt(5);
	}

	@Test
	public void testGetSegmentContaining() throws ProfileException {
//		10-20-45-89-10
		IBorderSegment s = sp.getSegmentContaining(0);
		assertEquals(UUID.fromString(SEG_3), s.getID());

		sp = sp.offset(25);
		// Should now be 85-95-20-64-85
		s = sp.getSegmentContaining(0);
		assertEquals(UUID.fromString(SEG_1), s.getID());
	}
	
	@Test
	public void testGetSegmentContainingStartOutOfBounds(){
		exception.expect(IllegalArgumentException.class);
		sp.getSegmentContaining(-5);
	}
	
	@Test
	public void testGetSegmentContainingEndOutOfBounds(){
		exception.expect(IllegalArgumentException.class);
		sp.getSegmentContaining(100);
	}
	
	@Test
	public void testSetSegments() throws ProfileException {
	    
	    List<IBorderSegment> list = new ArrayList<>();
        list.add(new DefaultBorderSegment(5,  10, 100, UUID.randomUUID()));
        list.add(new DefaultBorderSegment(10, 20, 100, UUID.randomUUID()));
        list.add(new DefaultBorderSegment(20, 60, 100, UUID.randomUUID()));
        list.add(new DefaultBorderSegment(60, 90, 100, UUID.randomUUID()));
        list.add(new DefaultBorderSegment(90, 5,  100, UUID.randomUUID()));
        IBorderSegment.linkSegments(list);
        
        sp.setSegments(list);
        
        for(int i=0; i<list.size(); i++){
            assertEquals(list.get(i), sp.getSegmentAt(i));
        }
	}

	@Test
	public void testSetSegmentsExceptsOnEmptyList() {
	    exception.expect(IllegalArgumentException.class);
	    sp.setSegments(new ArrayList<IBorderSegment>(0));
	}
	
	@Test
	public void testSetSegmentsExceptsOnNullList() {
	    exception.expect(IllegalArgumentException.class);
	    sp.setSegments(null);
	}

	@Test
	public void testClearSegments() {
	    assertTrue(sp.hasSegments());
	    sp.clearSegments();
        assertFalse(sp.hasSegments());
	}

	@Test
	public void testGetSegmentNames() {
	    
	    List<String> test = new ArrayList<>();
	    for(int i=0; i<sp.size(); i++){
	        test.add("Seg_"+i);
	    }
	    
	    List<String> result = sp.getSegmentNames();
	    
	    assertEquals(test.size(), result.size());
	    
	    for(int i=0; i<sp.size(); i++){
	        assertEquals(test.get(i), result.get(i));
	    }
	}

	@Test
	public void testGetSegmentNamesReturnsEmptyListWhenNoSegments() {
	    sp.clearSegments();
	    List<String> result = sp.getSegmentNames();
	    assertTrue(result.isEmpty());
	}

	@Test
	public void testGetSegmentIDs() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSegmentCount() throws ProfileException {   
	    List<IBorderSegment> list = makeTestSegments();
	    assertEquals(list.size(), sp.getSegmentCount());  
	}
	
	@Test
    public void testGetSegmentCountZeroWhenEmpty() throws ProfileException {   
        sp.clearSegments();
        assertEquals(0, sp.getSegmentCount());  
    }

	@Test
	public void testGetDisplacement() {
		fail("Not yet implemented");
	}

	@Test
	public void testContains() {
		fail("Not yet implemented");
	}

	@Test
	public void testUpdate() {
		fail("Not yet implemented");
	}

	@Test
	public void testAdjustSegmentStart() {
		fail("Not yet implemented");
	}

	@Test
	public void testAdjustSegmentEnd() {
		fail("Not yet implemented");
	}

	@Test
	public void testNudgeSegments() {
		fail("Not yet implemented");
	}

	@Test
	public void testOffsetInt() throws SegmentUpdateException, ProfileException, UnavailableComponentException {
	    /*
	     * Complete profile of segments
	     */
	    List<IBorderSegment> list = makeTestSegments();
	    IBorderSegment p1 = list.get(0);
	    IBorderSegment p2 = list.get(1);
	    IBorderSegment p3 = list.get(2);
	    IBorderSegment p4 = list.get(3);


	    p1.update(5, 20);
	    assertEquals(5, p1.getStartIndex());
	    assertEquals(5, p4.getEndIndex());

	    p4.update(89, 1);
	    assertEquals(1, p1.getStartIndex());
	    assertEquals(1, p4.getEndIndex());

	    /*
	     * Can the profile be offset and still have segments adjusted?
	     */

	    sp = sp.offset(1);
	    // Should now be 9-19-44-88-9
	    assertEquals(9, sp.getSegment(p1.getID()).getStartIndex());
	    assertEquals(9, sp.getSegment(p4.getID()).getEndIndex());

	    sp = sp.offset(80);
	    // Should now be 29-39-64-8-29
	    assertEquals(29, sp.getSegment(p1.getID()).getStartIndex());
	    assertEquals(39, sp.getSegment(p2.getID()).getStartIndex());
	    assertEquals(64, sp.getSegment(p3.getID()).getStartIndex());
	    assertEquals(8, sp.getSegment(p4.getID()).getStartIndex());
	}

	@Test
	public void testInterpolateSegments() {
		fail("Not yet implemented");
	}

	@Test
	public void testFrankenNormaliseToProfile() {
		fail("Not yet implemented");
	}

	@Test
	public void testMergeSegments() {
		fail("Not yet implemented");
	}

	@Test
	public void testUnmergeSegment() {
		fail("Not yet implemented");
	}

	@Test
	public void testSplitSegment() {
		fail("Not yet implemented");
	}

	@Test
	public void testValueString() {
		fail("Not yet implemented");
	}

	@Test
	public void testFloatProfileFloatArray() {
		fail("Not yet implemented");
	}

	@Test
	public void testFloatProfileIProfile() {
		fail("Not yet implemented");
	}

	@Test
	public void testFloatProfileFloatInt() {
		fail("Not yet implemented");
	}

	@Test
	public void testCopy() {
		fail("Not yet implemented");
	}

	@Test
	public void testOffsetInt1() {
		fail("Not yet implemented");
	}

	@Test
	public void testInterpolate() {
		fail("Not yet implemented");
	}

}
