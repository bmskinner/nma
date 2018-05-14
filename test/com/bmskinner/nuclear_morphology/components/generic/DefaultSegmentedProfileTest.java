package com.bmskinner.nuclear_morphology.components.generic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;

public class DefaultSegmentedProfileTest extends DefaultProfileTest {
	
	protected ISegmentedProfile singleSegment;
	protected ISegmentedProfile doubleSegment;
	protected final static UUID DOUBLE_SEG_ID_0 = UUID.fromString("00000000-0000-0000-0000-000000000001");
	protected final static UUID DOUBLE_SEG_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000002");
	protected final static UUID DOUBLE_SEG_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000003");
	protected final static UUID DOUBLE_SEG_ID_3 = UUID.fromString("00000000-0000-0000-0000-000000000004");
	
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		singleSegment = comp.new DefaultSegmentedProfile(data);
		doubleSegment = comp.new DefaultSegmentedProfile(data);
		int splitIndex = 50;
		doubleSegment.splitSegment(doubleSegment.getSegmentContaining(1), splitIndex, DOUBLE_SEG_ID_0, DOUBLE_SEG_ID_1);
	}

	@Override
	@Test
	public void testEqualsObject() {
		fail("Not yet implemented");
	}

	@Test
	public void testReverseWithSingleSegment() {
		super.testReverse();
		int expStart = 0;
		int expEnd = 0;
		singleSegment.reverse();
		List<IBorderSegment> segs = singleSegment.getSegments();
		assertEquals(expStart, segs.get(0).getStartIndex());
		assertEquals(expEnd, segs.get(0).getEndIndex());
	}
	
	@Test
	public void testReverseWithDoubleSegment() {
		super.testReverse();
		List<IBorderSegment> segs = doubleSegment.getSegments();
		int[] old = new int[segs.size()];
		for(int i=0; i<old.length; i++){
			old[i] = segs.get(i).getStartIndex();
		}
		
		int[] exp = new int[old.length];
		for(int i=0; i<old.length; i++){
			exp[i] = doubleSegment.size()-1-old[old.length-i-1];
		}

		doubleSegment.reverse();
		segs = doubleSegment.getSegments();
		for(int i=0; i<old.length; i++){
			assertEquals(exp[i], segs.get(i).getStartIndex());
		}
	}

	@Test
	public void testDefaultSegmentedProfileIProfileListOfIBorderSegment() {
		fail("Not yet implemented");
	}

	@Test
	public void testDefaultSegmentedProfileISegmentedProfile() {
		fail("Not yet implemented");
	}

	@Test
	public void testDefaultSegmentedProfileIProfile() {
		fail("Not yet implemented");
	}

	@Test
	public void testDefaultSegmentedProfileFloatArray() {
		fail("Not yet implemented");
	}

	@Test
	public void testHasSegments() {
		assertTrue(singleSegment.hasSegments());
	}

	@Test
	public void testGetSegments() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSegmentUUID() {
		fail("Not yet implemented");
	}

	@Test
	public void testHasSegment() {
		assertTrue(singleSegment.hasSegment(comp.getID()));
	}

	@Test
	public void testGetSegmentsFrom() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetOrderedSegments() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSegmentString() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSegmentIBorderSegment() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSegmentAt() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSegmentContaining() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetSegments() {
		fail("Not yet implemented");
	}

	@Test
	public void testClearSegments() {
		assertTrue(singleSegment.hasSegments());
		singleSegment.clearSegments();
		assertFalse(singleSegment.hasSegments());
	}

	@Test
	public void testGetSegmentNames() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSegmentIDs() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSegmentCount() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetDisplacement() {
		fail("Not yet implemented");
	}

	@Test
	public void testContains() throws UnavailableComponentException {
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
	public void testOffsetInt() {
		fail("Not yet implemented");
	}

	@Test
	public void testInterpolateIntLengthensSingleSegmentProfile() throws ProfileException, UnavailableComponentException {
		
		int newLength = singleSegment.size() * 2;
		ISegmentedProfile interpolated = singleSegment.interpolate(newLength);
		assertEquals(newLength, interpolated.size());
		assertEquals(0, interpolated.getSegment(comp.getID()).getStartIndex());
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
	public void testIsSplittable() {
		assertTrue(singleSegment.isSplittable(singleSegment.getSegmentContaining(1).getID(), 50));
	}
	
	@Test
	public void testIsSplittableReturnsFalseWhenTooSmall() {
		assertFalse(singleSegment.isSplittable(singleSegment.getSegmentContaining(1).getID(), 2));
	}

	@Test
	public void testSplitSegmentForSingleSegmentProfile() throws ProfileException {
		assertEquals(1, singleSegment.getSegmentCount());
		singleSegment.splitSegment(singleSegment.getSegmentContaining(1), 50, DOUBLE_SEG_ID_0, DOUBLE_SEG_ID_1);
		assertEquals(2, singleSegment.getSegmentCount());
		
		List<IBorderSegment> list = singleSegment.getSegments();
		assertEquals(DOUBLE_SEG_ID_0, list.get(0).getID());
		assertEquals(DOUBLE_SEG_ID_1, list.get(1).getID());
		
		assertEquals(0, list.get(0).getStartIndex());
		assertEquals(50, list.get(0).getEndIndex());
		assertEquals(50, list.get(1).getStartIndex());
		assertEquals(0, list.get(1).getEndIndex());
	}
	
	@Test
	public void testSplitSegmentForDoubleSegmentProfile() throws ProfileException {
		assertEquals(2, doubleSegment.getSegmentCount());
		doubleSegment.splitSegment(doubleSegment.getSegmentContaining(1), 25, DOUBLE_SEG_ID_2, DOUBLE_SEG_ID_3);
		assertEquals(3, doubleSegment.getSegmentCount());
		
		List<IBorderSegment> list = doubleSegment.getSegments();
		assertEquals(DOUBLE_SEG_ID_2, list.get(0).getID());
		assertEquals(DOUBLE_SEG_ID_3, list.get(1).getID());
		assertEquals(DOUBLE_SEG_ID_1, list.get(2).getID());
		
		assertEquals(0, list.get(0).getStartIndex());
		assertEquals(25, list.get(0).getEndIndex());
		assertEquals(25, list.get(1).getStartIndex());
		assertEquals(50, list.get(1).getEndIndex());
		assertEquals(50, list.get(2).getStartIndex());
		assertEquals(0, list.get(2).getEndIndex());
	}
	
	@Test
	public void testSplitSegmentFailsWhenSegmentTooShort() throws ProfileException {
		exception.expect(IllegalArgumentException.class);
		singleSegment.splitSegment(singleSegment.getSegmentContaining(1), 2, UUID.randomUUID(), UUID.randomUUID());
	}

	@Override
	@Test
	public void testCopy() {
		ISegmentedProfile p = singleSegment.copy();
		assertEquals(singleSegment, p);
	}
}
