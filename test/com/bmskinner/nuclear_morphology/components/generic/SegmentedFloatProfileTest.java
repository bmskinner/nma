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

/**
 * Tests for the profile methods
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
	public void testSegmentedFloatProfileIProfileListOfIBorderSegment() throws ProfileException {
	    List<IBorderSegment> list = makeTestSegments();
	    assertEquals(list.size(), sp.getSegmentCount());
	}
	
	@Test
    public void testSegmentedFloatProfileIProfileListOfIBorderSegmentExceptsOnNullProfile() throws ProfileException {
	    List<IBorderSegment> list = makeTestSegments();
        exception.expect(IllegalArgumentException.class);
        new SegmentedFloatProfile(null, list);
    }
	
	@Test
	public void testSegmentedFloatProfileIProfileListOfIBorderSegmentExceptsOnNullSegmentList() throws ProfileException {
	    IProfile profile = new FloatProfile(10, 100);
	    exception.expect(IllegalArgumentException.class);
	    new SegmentedFloatProfile(profile, null);
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
    public void testSegmentedFloatProfileISegmentedProfileExceptsOnNullProfile() throws IndexOutOfBoundsException, ProfileException {
	    exception.expect(IllegalArgumentException.class);
        new SegmentedFloatProfile( (ISegmentedProfile)null);
    }

	@Test
	public void testSegmentedFloatProfileIProfile() {
	    IProfile profile = new FloatProfile(10, 100);
	    ISegmentedProfile p = new SegmentedFloatProfile(profile);
	    assertEquals(2, p.getSegmentCount());
	}
	
	@Test
	public void testSegmentedFloatProfileIProfileExceptsOnNullProfile() {
	    exception.expect(IllegalArgumentException.class);
	    new SegmentedFloatProfile((IProfile)null);
	}

	@Test
	public void testSegmentedFloatProfileFloatArray() {
	    float[] array = new float[100];
        for (int i = 0; i < array.length; i++) {
            array[i] = 10;
        }
        ISegmentedProfile p = new SegmentedFloatProfile(array);
        
        for (int i = 0; i < array.length; i++) {
            assertEquals(array[i], p.get(i), 0.000001);
        }  
	}
	
	@Test
    public void testSegmentedFloatProfileFloatArrayExceptsOnNullArray() {
	    exception.expect(IllegalArgumentException.class);
        new SegmentedFloatProfile( (float[]) null);
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
	public void testGetOrderedSegments() throws ProfileException {
	    List<IBorderSegment> test = makeTestSegments();
	    List<IBorderSegment> result = sp.getOrderedSegments();

	    for(int i=0; i<test.size(); i++){
            assertEquals(test.get(i), result.get(i));
        }
	}

	@Test
	public void testGetSegmentString() throws UnavailableComponentException {
	    IBorderSegment test = makeSeg1();
	    String name = "Seg_1";
	    IBorderSegment result = sp.getSegment(name);
	    assertEquals(test.getID(), result.getID());
        assertEquals(test.getStartIndex(), result.getStartIndex());
        assertEquals(test.getEndIndex(), result.getEndIndex());
	}
	
	@Test
    public void testGetSegmentStringExceptsWithNullInput() throws UnavailableComponentException{
        exception.expect(IllegalArgumentException.class);
        sp.getSegment( (String)null);
    }
	
	@Test
    public void testGetSegmentStringExceptsWithInvalidInput() throws UnavailableComponentException{
        exception.expect(UnavailableComponentException.class);
        sp.getSegment("Not present");
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
    public void testSetSegmentsExceptsOnListWithDifferentProfileLength() throws ProfileException {
	    List<IBorderSegment> list = new ArrayList<>();
        list.add(new DefaultBorderSegment(5,  10, 110, UUID.randomUUID()));
        list.add(new DefaultBorderSegment(10, 20, 110, UUID.randomUUID()));
        list.add(new DefaultBorderSegment(20, 60, 110, UUID.randomUUID()));
        list.add(new DefaultBorderSegment(60, 90, 110, UUID.randomUUID()));
        list.add(new DefaultBorderSegment(90, 5,  110, UUID.randomUUID()));
        IBorderSegment.linkSegments(list);
        exception.expect(IllegalArgumentException.class);
        sp.setSegments(list);
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
	    for(int i=0; i<sp.getSegmentCount(); i++){
	        test.add("Seg_"+i);
	    }
	    
	    List<String> result = sp.getSegmentNames();
	    
	    assertEquals(test.size(), result.size()); 
	    for(int i=0; i<test.size(); i++){
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
	public void testGetSegmentIDs() throws ProfileException {
	    List<IBorderSegment> test = makeTestSegments();
	    List<UUID> result = sp.getSegmentIDs();
	    assertEquals(test.size(), result.size());
	    for(int i=0; i<result.size(); i++){
            assertEquals(test.get(i).getID(), result.get(i));
        }
	}
	
	@Test
    public void testGetSegmentIDsReturnsEmptyListWhenNoSegments() throws ProfileException {
        sp.clearSegments();
        List<UUID> result = sp.getSegmentIDs();
        assertTrue(result.isEmpty());
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
	    IBorderSegment s1 = sp.getSegmentAt(1);
	    double d = sp.getDisplacement(s1);
	    assertEquals(0, d, 0.000001);
	}
	
	@Test
    public void testGetDisplacementExceptsOnNullSegment() {
        exception.expect(IllegalArgumentException.class);
        sp.getDisplacement(null);
    }
	
	@Test
    public void testGetDisplacementExceptsOnInvalidSegment() {
	    IBorderSegment s1 = new DefaultBorderSegment(5,  10, 110, UUID.randomUUID());
        exception.expect(IllegalArgumentException.class);
        sp.getDisplacement(s1);
    }

	@Test
	public void testContains() {
	    IBorderSegment s1 = sp.getSegmentAt(1);
		assertTrue(sp.contains(s1));
	}
	
	@Test
    public void testContainsReturnsFalseWhenSegmentNotPresent() {
	    IBorderSegment s1 = new DefaultBorderSegment(5,  10, 110, UUID.randomUUID());
        assertFalse(sp.contains(s1));
    }
	
	@Test
    public void testContainsReturnsFalseWhenSegmentNull() {
        assertFalse(sp.contains(null));
    }

	@Test
	public void testAdjustSegmentStartPositive() throws SegmentUpdateException {
	    IBorderSegment s1 = sp.getSegmentAt(1);
	    int expStart = s1.getStartIndex()+5;
	    assertTrue(sp.adjustSegmentStart(s1.getID(), 5));  
	    assertEquals(expStart, s1.getStartIndex());   
	}
	
	@Test
	public void testAdjustSegmentStartNegative() throws SegmentUpdateException {
	    IBorderSegment s1 = sp.getSegmentAt(1);

	    int expStart = s1.getStartIndex()-5;
	    assertTrue(sp.adjustSegmentStart(s1.getID(), -5));
	    assertEquals(expStart, s1.getStartIndex()); 
	}
	
	@Test
    public void testAdjustSegmentStartPositiveDoesNothingWhenSegmentWillBecomeTooSmall() throws SegmentUpdateException {
        IBorderSegment s1 = sp.getSegmentAt(1);
        int length = s1.length();
        int expStart = s1.getStartIndex();
        assertFalse(sp.adjustSegmentStart(s1.getID(), length-1));
        assertEquals(expStart, s1.getStartIndex()); 
    }
	
	@Test
    public void testAdjustSegmentStartNegativeDoesNothingWhenSegmentWillBecomeTooSmall() throws SegmentUpdateException {
        IBorderSegment s1 = sp.getSegmentAt(1);
        int length = s1.prevSegment().length();
        int expStart = s1.getStartIndex();
        assertFalse(sp.adjustSegmentStart(s1.getID(), -(length-1)));
        assertEquals(expStart, s1.getStartIndex()); 
    }
	
	@Test
    public void testAdjustSegmentStartExceptsWhenIdIsNull() throws SegmentUpdateException {
	    exception.expect(IllegalArgumentException.class);
        sp.adjustSegmentStart(null, 5);
    }
	
	@Test
    public void testAdjustSegmentStartExceptsWhenIdIsInvalid() throws SegmentUpdateException {
	    UUID id = UUID.fromString("00000001-1000-1000-1000-100000000000");
        exception.expect(IllegalArgumentException.class);
        sp.adjustSegmentStart(id, 5);
    }
	
	@Test
    public void testAdjustSegmentEndPositive() throws SegmentUpdateException {
        IBorderSegment s1 = sp.getSegmentAt(1);
        int exp = s1.getEndIndex()+5;
        assertTrue(sp.adjustSegmentEnd(s1.getID(), 5));  
        assertEquals(exp, s1.getEndIndex());   
    }
    
    @Test
    public void testAdjustSegmentEndNegative() throws SegmentUpdateException {
        IBorderSegment s1 = sp.getSegmentAt(1);

        int expStart = s1.getEndIndex()-5;
        assertTrue(sp.adjustSegmentEnd(s1.getID(), -5));
        assertEquals(expStart, s1.getEndIndex()); 
    }
    
    @Test
    public void testAdjustSegmentEndPositiveDoesNothingWhenSegmentWillBecomeTooSmall() throws SegmentUpdateException {
        IBorderSegment s1 = sp.getSegmentAt(1);
        int length = s1.nextSegment().length();
        int expStart = s1.getEndIndex();
        assertFalse(sp.adjustSegmentEnd(s1.getID(), length-1));
        assertEquals(expStart, s1.getEndIndex()); 
    }
    
    @Test
    public void testAdjustSegmentEndNegativeDoesNothingWhenSegmentWillBecomeTooSmall() throws SegmentUpdateException {
        IBorderSegment s1 = sp.getSegmentAt(1);
        int length = s1.length();
        int expStart = s1.getEndIndex();
        assertFalse(sp.adjustSegmentEnd(s1.getID(), -(length-1)));
        assertEquals(expStart, s1.getEndIndex()); 
    }
    
    @Test
    public void testAdjustSegmentEndExceptsWhenIdIsNull() throws SegmentUpdateException {
        exception.expect(IllegalArgumentException.class);
        sp.adjustSegmentEnd(null, 5);
    }
    
    @Test
    public void testAdjustSegmentEndExceptsWhenIdIsInvalid() throws SegmentUpdateException {
        UUID id = UUID.fromString("00000001-1000-1000-1000-100000000000");
        exception.expect(IllegalArgumentException.class);
        sp.adjustSegmentEnd(id, 5);
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
	public void testFrankenNormaliseToProfile() {
		fail("Not yet implemented");
	}

	@Test
	public void testMergeSegments() throws ProfileException, UnavailableComponentException {
	    
	    int expCount = sp.getSegmentCount()-1;
	    UUID mergedId = UUID.fromString("00000001-1000-1000-1000-100000000000");
	    IBorderSegment s1 = sp.getSegmentAt(1);
	    IBorderSegment s2 = sp.getSegmentAt(2);
	    
	    sp.mergeSegments(s1, s2, mergedId);
	    
	    assertEquals(expCount, sp.getSegmentCount());
	    
	    IBorderSegment m = sp.getSegment(mergedId);
	    assertEquals(s1.getStartIndex(), m.getStartIndex());
	    assertEquals(s2.getEndIndex(), m.getEndIndex());
	}
	
	@Test
    public void testMergeSegmentsExceptsOnNullSeg1() throws ProfileException, UnavailableComponentException {
        
        UUID mergedId = UUID.fromString("00000001-1000-1000-1000-100000000000");
        IBorderSegment s1 = sp.getSegmentAt(1);
        IBorderSegment s2 = sp.getSegmentAt(2);
        
        exception.expect(IllegalArgumentException.class);
        sp.mergeSegments(null, s2, mergedId);
    }
	
	@Test
	public void testMergeSegmentsExceptsOnNullSeg2() throws ProfileException, UnavailableComponentException {

	    UUID mergedId = UUID.fromString("00000001-1000-1000-1000-100000000000");
	    IBorderSegment s1 = sp.getSegmentAt(1);
	    IBorderSegment s2 = sp.getSegmentAt(2);

	    exception.expect(IllegalArgumentException.class);
	    sp.mergeSegments(s1, null, mergedId);
	}
	
	@Test
	public void testMergeSegmentsExceptsOnNullId() throws ProfileException, UnavailableComponentException {

	    UUID mergedId = UUID.fromString("00000001-1000-1000-1000-100000000000");
	    IBorderSegment s1 = sp.getSegmentAt(1);
	    IBorderSegment s2 = sp.getSegmentAt(2);

	    exception.expect(IllegalArgumentException.class);
	    sp.mergeSegments(s1, s2, null);
	}
	
	@Test
    public void testMergeSegmentsExceptsWhenSegmentsNotLinked() throws ProfileException, UnavailableComponentException {

        UUID mergedId = UUID.fromString("00000001-1000-1000-1000-100000000000");
        IBorderSegment s1 = makeSeg1();
        IBorderSegment s2 = makeSeg2();

        exception.expect(IllegalArgumentException.class);
        sp.mergeSegments(s1, s2, mergedId);
    }
	
	@Test
    public void testMergeSegmentsExceptsWhenSegment1NotInProfile() throws ProfileException, UnavailableComponentException {

        UUID mergedId = UUID.fromString("00000001-1000-1000-1000-100000000000");
        IBorderSegment s1 = new DefaultBorderSegment(5,  10, 110, UUID.randomUUID());
        IBorderSegment s2 = sp.getSegmentAt(2);

        exception.expect(IllegalArgumentException.class);
        sp.mergeSegments(s1, s2, mergedId);
    }
	
	@Test
    public void testMergeSegmentsExceptsWhenSegment2NotInProfile() throws ProfileException, UnavailableComponentException {

        UUID mergedId = UUID.fromString("00000001-1000-1000-1000-100000000000");
        IBorderSegment s1 = sp.getSegmentAt(1);
        IBorderSegment s2 = new DefaultBorderSegment(5,  10, 110, UUID.randomUUID());

        exception.expect(IllegalArgumentException.class);
        sp.mergeSegments(s1, s2, mergedId);
    }

	@Test
	public void testUnmergeSegment() throws ProfileException, UnavailableComponentException {
	    
	    UUID mergedId = UUID.fromString("00000001-1000-1000-1000-100000000000");
        IBorderSegment s1 = sp.getSegmentAt(1);
        IBorderSegment s2 = sp.getSegmentAt(2);
        
        int expCount = sp.getSegmentCount();
        
        sp.mergeSegments(s1, s2, mergedId);
        IBorderSegment m = sp.getSegment(mergedId);
		sp.unmergeSegment(m);
		
		assertEquals(expCount, sp.getSegmentCount());
		
		assertEquals(s1, sp.getSegmentAt(1));
		assertEquals(s2, sp.getSegmentAt(2));
	}

	@Test
	public void testUnmergeSegmentDoesNothingWhenNoMergedSegments() throws UnavailableComponentException, ProfileException {
        IBorderSegment s1 = sp.getSegmentAt(1);   
        sp.unmergeSegment(s1);
	}
	
	@Test
	public void testUnmergeSegmentExceptsOnNullSegment() throws ProfileException {
	    exception.expect(IllegalArgumentException.class);
	    sp.unmergeSegment(null);
	}
	
	@Test
    public void testIsSplittable() {
	    IBorderSegment s1 = sp.getSegmentAt(1);
        assertTrue(sp.isSplittable(s1.getID(), s1.getMidpointIndex()));
    }
	
	@Test
	public void testIsSplittableReturnsFalseWhenTooCloseToStart() {
	    IBorderSegment s1 = sp.getSegmentAt(1);
	    assertFalse(sp.isSplittable(s1.getID(), s1.getStartIndex()+1));
	}

	@Test
	public void testIsSplittableReturnsFalseWhenTooCloseToEnd() {
	    IBorderSegment s1 = sp.getSegmentAt(1);
	    assertFalse(sp.isSplittable(s1.getID(), s1.getEndIndex()-1));
	}

	@Test
	public void testIsSplittableReturnsFalseOnOutOfBoundsIndex() {
	    IBorderSegment s1 = sp.getSegmentAt(1);
	    exception.expect(IllegalArgumentException.class);
	    assertFalse(sp.isSplittable(s1.getID(), s1.getEndIndex()+1));
	}

	@Test
	public void testIsSplittableExceptsOnNullId() {
	    exception.expect(IllegalArgumentException.class);
	    sp.isSplittable(null, 30);
	}

	@Test
	public void testIsSplittableExceptsOnOutOfBoundsProfileIndex() {
	    IBorderSegment s1 = sp.getSegmentAt(1);
	    exception.expect(IllegalArgumentException.class);
	    sp.isSplittable(s1.getID(), sp.size()+1);
	}

	@Test
	public void testSplitSegment() throws ProfileException {
	    IBorderSegment s1 = sp.getSegmentAt(1);
	    
	    int start = s1.getStartIndex();
	    int mid = s1.getMidpointIndex();
	    int end = s1.getEndIndex();
	    
	    UUID id1 = UUID.fromString("00000001-1000-1000-1000-100000000000");
	    UUID id2 = UUID.fromString("00000002-2000-2000-2000-200000000000");
	    
	    sp.splitSegment(s1, mid, id1, id2);
	    
	    IBorderSegment r1 = sp.getSegmentAt(1);
	    IBorderSegment r2 = sp.getSegmentAt(2);
	    
	    assertEquals(id1, r1.getID());
	    assertEquals(id2, r2.getID());
	    
	    assertEquals("r1 start",start, r1.getStartIndex());
	    assertEquals("r1 end", mid, r1.getEndIndex());
	    assertEquals("r2 start", mid, r2.getStartIndex());
	    assertEquals("r2 end", end, r2.getEndIndex());
	}
	
	@Test
    public void testSplitSegmentExceptsOnNullSegment() throws ProfileException {
        IBorderSegment s1 = sp.getSegmentAt(1);
        
        int mid = s1.getMidpointIndex();
        
        UUID id1 = UUID.fromString("00000001-1000-1000-1000-100000000000");
        UUID id2 = UUID.fromString("00000002-2000-2000-2000-200000000000");
        
        exception.expect(IllegalArgumentException.class);
        sp.splitSegment(null, mid, id1, id2);
    }
	
	@Test
    public void testSplitSegmentExceptsOnNullId1() throws ProfileException {
        IBorderSegment s1 = sp.getSegmentAt(1);
        
        int mid = s1.getMidpointIndex();
        
        UUID id1 = UUID.fromString("00000001-1000-1000-1000-100000000000");
        UUID id2 = UUID.fromString("00000002-2000-2000-2000-200000000000");
        
        exception.expect(IllegalArgumentException.class);
        sp.splitSegment(s1, mid, null, id2);
    }
	
	@Test
    public void testSplitSegmentExceptsOnNullId2() throws ProfileException {
        IBorderSegment s1 = sp.getSegmentAt(1);
        
        int mid = s1.getMidpointIndex();
        
        UUID id1 = UUID.fromString("00000001-1000-1000-1000-100000000000");
        UUID id2 = UUID.fromString("00000002-2000-2000-2000-200000000000");
        
        exception.expect(IllegalArgumentException.class);
        sp.splitSegment(s1, mid, id1, null);
    }
	
	@Test
    public void testSplitSegmentExceptsOnInvalidIndex() throws ProfileException {
        IBorderSegment s1 = sp.getSegmentAt(1);
        
        int mid = s1.getStartIndex()+1;
        
        UUID id1 = UUID.fromString("00000001-1000-1000-1000-100000000000");
        UUID id2 = UUID.fromString("00000002-2000-2000-2000-200000000000");
        
        exception.expect(IllegalArgumentException.class);
        sp.splitSegment(s1, mid, id1, id2);
    }
	
	@Test
    public void testSplitSegmentExceptsOnIndexOutOfSegmentBounds() throws ProfileException {
        IBorderSegment s1 = sp.getSegmentAt(1);
        
        int mid = sp.size()+1;
        
        UUID id1 = UUID.fromString("00000001-1000-1000-1000-100000000000");
        UUID id2 = UUID.fromString("00000002-2000-2000-2000-200000000000");
        
        exception.expect(IllegalArgumentException.class);
        sp.splitSegment(s1, mid, id1, id2);
    }
	
	@Test
    public void testSplitSegmentExceptsOnInvalidSegment() throws ProfileException {

	    IBorderSegment s1 = new DefaultBorderSegment(5,  10, 110, UUID.randomUUID());
        int mid = s1.getStartIndex()+1;
        
        UUID id1 = UUID.fromString("00000001-1000-1000-1000-100000000000");
        UUID id2 = UUID.fromString("00000002-2000-2000-2000-200000000000");
        
        exception.expect(IllegalArgumentException.class);
        sp.splitSegment(s1, mid, id1, id2);
    }

	@Test
	public void testValueString() { 
	    StringBuilder builder = new StringBuilder();
        for (int i = 0; i < sp.size(); i++) {
            builder.append("Index " + i + "\t" + sp.get(i) + "\r\n");
        }
        assertEquals(builder.toString(), sp.valueString());
	}

	@Test
	public void testCopy() {
	    ISegmentedProfile copy = sp.copy();
		assertEquals(sp, copy);
	}

	@Test
	public void testInterpolate() throws ProfileException {
	    
	    ISegmentedProfile result = sp.interpolate(sp.size()*2);
	    
	    assertEquals(sp.getSegmentCount(), result.getSegmentCount());
	    for (int i=0; i < sp.getSegmentCount(); i++) {
	        assertEquals(sp.getSegmentAt(i).getStartIndex()*2, result.getSegmentAt(i).getStartIndex());
	    }  
	}
	
	@Test
    public void testInterpolateExceptsWhenLessThanZero() throws ProfileException {
	    exception.expect(IllegalArgumentException.class);
        sp.interpolate(-1);
    }
	
	@Test
    public void testToString() {
	    StringBuilder builder = new StringBuilder();
        for (IBorderSegment seg : sp.getSegments()) {
            builder.append(seg.toString() + System.getProperty("line.separator"));
        }

       assertEquals(builder.toString(), sp.toString());
    }
}
