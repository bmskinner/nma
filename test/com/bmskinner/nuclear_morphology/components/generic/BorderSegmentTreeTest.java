package com.bmskinner.nuclear_morphology.components.generic;

import static org.junit.Assert.*;

import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.SegmentedCellularComponent.DefaultSegmentedProfile.BorderSegmentTree;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BorderSegmentTreeTest extends DefaultSegmentedProfileTest {

	protected IBorderSegment segment; // segment covering entire profile
	
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		segment = singleSegment.getSegment(comp.getID());
	}

	@Test
	public void testBorderSegmentTreeUUIDIntInt() {
		fail("Not yet implemented");
	}

	@Test
	public void testBorderSegmentTreeUUIDIntIntBorderSegmentTree() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetID() {
		assertEquals(comp.getID(), segment.getID());
	}

	@Test
	public void testSplitAt() throws ProfileException {

		
		((BorderSegmentTree)segment).splitAt(segment.getMidpointIndex(), UUID.randomUUID(), UUID.randomUUID());
		List<IBorderSegment> list = segment.getMergeSources();
		IBorderSegment s0 = list.get(0);
		IBorderSegment s1 = list.get(1);
		
		assertEquals(0, s0.getStartIndex());
		assertEquals(segment.getMidpointIndex(), s0.getEndIndex());
		assertEquals(segment.getMidpointIndex(), s1.getStartIndex());
		assertEquals(0, s1.getEndIndex());
	}

	@Test
	public void testClearMergeSources() {
		assertFalse(segment.hasMergeSources());
		IBorderSegment mock1 = mock(IBorderSegment.class);
		when(mock1.getID()).thenReturn(UUID.randomUUID());
		when(mock1.getStartIndex()).thenReturn(segment.getMidpointIndex());
		when(mock1.length()).thenReturn(segment.getTotalLength()-segment.getMidpointIndex());
		segment.addMergeSource(mock1);
		assertTrue(segment.hasMergeSources());
		segment.clearMergeSources();
		assertFalse(segment.hasMergeSources());
	}

	@Test
	public void testHasMergeSources() {
		assertFalse(segment.hasMergeSources());
		
		IBorderSegment mock1 = mock(IBorderSegment.class);
		when(mock1.getID()).thenReturn(UUID.randomUUID());
		when(mock1.getStartIndex()).thenReturn(segment.getMidpointIndex());
		when(mock1.length()).thenReturn(segment.getTotalLength()-segment.getMidpointIndex());
		segment.addMergeSource(mock1);
		assertTrue(segment.hasMergeSources());
	}

	@Test
	public void testGetStartIndex() {
		assertEquals(0, segment.getStartIndex());
	}

	@Test
	public void testGetEndIndex() {
		assertEquals(0, segment.getEndIndex());
	}

	@Test
	public void testGetProportionalIndex() {
		assertEquals(0, segment.getProportionalIndex(0));
		assertEquals(segment.getMidpointIndex(), segment.getProportionalIndex(0.5));
		assertEquals(segment.getEndIndex(), segment.getProportionalIndex(1));
	}
	
	@Test
	public void testGetProportionalIndexExceptsBelowZero() {
		exception.expect(IllegalArgumentException.class);
		segment.getProportionalIndex(-0.1);
	}
	
	@Test
	public void testGetProportionalIndexExceptsAboveOne() {
		exception.expect(IllegalArgumentException.class);
		segment.getProportionalIndex(1.1);
	}

	@Test
	public void testGetIndexProportion() {
		assertEquals(0, segment.getIndexProportion(segment.getStartIndex()),0);
		assertEquals(0.5, segment.getIndexProportion(segment.getMidpointIndex()), 0);
		double penultimate =  (double)(singleSegment.size()-1)/ (double) singleSegment.size();
		assertEquals(penultimate, segment.getIndexProportion(segment.getEndIndex()-1),0);
	}

	@Test
	public void testGetName() {
		assertEquals("Seg_0", segment.getName());
	}

	@Test
	public void testGetMidpointIndex() {
		assertEquals(segment.getTotalLength()/2, segment.getMidpointIndex());
	}

	@Test
	public void testGetDistanceToStart() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetDistanceToEnd() {
		assertEquals(comp.getBorderLength(), segment.getDistanceToEnd(0));
		assertEquals(segment.length()/2, segment.getDistanceToEnd(segment.getMidpointIndex()));
		assertEquals(1, segment.getDistanceToEnd(comp.getBorderLength()));
	}

	@Test
	public void testIsLocked() {
		assertFalse(segment.isLocked());
	}

	@Test
	public void testSetLocked() {
		assertFalse(segment.isLocked());
		segment.setLocked(true);
		assertTrue(segment.isLocked());
	}

	@Test
	public void testGetTotalLength() {
		assertEquals(comp.getBorderLength(), segment.getTotalLength());
	}

	@Test
	public void testNextSegmentWithBaseSegment() {
		assertEquals(segment, segment.nextSegment());
	}
	
	@Test
	public void testNextSegmentWithChildSegment() {
		fail("Not yet implemented");
	}

	@Test
	public void testPrevSegmentWithBaseSegment() {
		assertEquals(segment, segment.prevSegment());
	}
	
	@Test
	public void testPrevSegmentWithChildSegment() {
		fail("Not yet implemented");
	}

	@Test
	public void testLength() {
		assertEquals(segment.getTotalLength(), segment.length() );
	}

	@Test
	public void testTestLength() {
		fail("Not yet implemented");
	}

	@Test
	public void testWrapsIntInt() {
		fail("Not yet implemented");
	}

	@Test
	public void testWraps() throws UnavailableComponentException {
		assertTrue(segment.wraps());
		
		assertEquals(2, doubleSegment.getSegments().size());
		assertFalse(doubleSegment.getSegment(DOUBLE_SEG_ID_0).wraps());
		assertTrue(doubleSegment.getSegment(DOUBLE_SEG_ID_1).wraps());
	}

	@Override
	@Test
	public void testContains() throws UnavailableComponentException {
		IBorderSegment s0 = doubleSegment.getSegment(DOUBLE_SEG_ID_0);
		IBorderSegment s1 = doubleSegment.getSegment(DOUBLE_SEG_ID_1);
		
		
		assertTrue(s0.contains(0));
		assertTrue(s1.contains(0));
		for(int i=1; i<50; i++) {
			assertTrue(s0.contains(i));
			assertFalse(s1.contains(i));
		}
		assertTrue(s0.contains(50));
		assertTrue(s1.contains(50));
		for(int i=51; i<doubleSegment.size(); i++) {
			assertFalse(s0.contains(i));
			assertTrue(s1.contains(i));
		}
	}

	@Override
	@Test
	public void testUpdate() {
		fail("Not yet implemented");
	}

	@Test
	public void testHasNextSegment() throws UnavailableComponentException {
		assertTrue(segment.hasNextSegment());
		assertTrue(doubleSegment.getSegment(DOUBLE_SEG_ID_0).hasNextSegment());
		assertTrue(doubleSegment.getSegment(DOUBLE_SEG_ID_1).hasNextSegment());
	}

	@Test
	public void testHasPrevSegment() throws UnavailableComponentException {
		assertTrue(segment.hasPrevSegment());
		assertTrue(doubleSegment.getSegment(DOUBLE_SEG_ID_0).hasPrevSegment());
		assertTrue(doubleSegment.getSegment(DOUBLE_SEG_ID_1).hasPrevSegment());
	}

	@Test
	public void testGetPosition() throws UnavailableComponentException {
		assertEquals(0, doubleSegment.getSegment(DOUBLE_SEG_ID_0).getPosition());
		assertEquals(1, doubleSegment.getSegment(DOUBLE_SEG_ID_1).getPosition());
	}

	@Test
	public void testIterator() {

		fail("Not yet implemented");
	}

	@Test
	public void testOverlaps() throws UnavailableComponentException, ProfileException {
		assertTrue(doubleSegment.getSegment(DOUBLE_SEG_ID_0).overlaps(doubleSegment.getSegment(DOUBLE_SEG_ID_1)));
		assertTrue(doubleSegment.getSegment(DOUBLE_SEG_ID_1).overlaps(doubleSegment.getSegment(DOUBLE_SEG_ID_0)));
		
		doubleSegment.splitSegment(doubleSegment.getSegmentContaining(1), 25, DOUBLE_SEG_ID_2, DOUBLE_SEG_ID_3);
		
		assertFalse(doubleSegment.getSegment(DOUBLE_SEG_ID_1).overlaps(doubleSegment.getSegment(DOUBLE_SEG_ID_2)));
		fail("Not yet implemented");
	}

}
