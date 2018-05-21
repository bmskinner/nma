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

	protected IBorderSegment singleSegment; // segment covering entire profile
	
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		singleSegment = singleSegmentProfile.getSegment(comp.getID());
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
		assertEquals(comp.getID(), singleSegment.getID());
	}

	@Test
	public void testSplitAt() throws ProfileException {

		
		((BorderSegmentTree)singleSegment).splitAt(singleSegment.getMidpointIndex(), UUID.randomUUID(), UUID.randomUUID());
		List<IBorderSegment> list = singleSegment.getMergeSources();
		IBorderSegment s0 = list.get(0);
		IBorderSegment s1 = list.get(1);
		
		assertEquals(0, s0.getStartIndex());
		assertEquals(singleSegment.getMidpointIndex(), s0.getEndIndex());
		assertEquals(singleSegment.getMidpointIndex(), s1.getStartIndex());
		assertEquals(0, s1.getEndIndex());
	}

	@Test
	public void testClearMergeSources() {
		assertFalse(singleSegment.hasMergeSources());
		IBorderSegment mock1 = mock(IBorderSegment.class);
		when(mock1.getID()).thenReturn(UUID.randomUUID());
		when(mock1.getStartIndex()).thenReturn(singleSegment.getMidpointIndex());
		when(mock1.length()).thenReturn(singleSegment.getTotalLength()-singleSegment.getMidpointIndex());
		singleSegment.addMergeSource(mock1);
		assertTrue(singleSegment.hasMergeSources());
		singleSegment.clearMergeSources();
		assertFalse(singleSegment.hasMergeSources());
	}

	@Test
	public void testHasMergeSources() {
		assertFalse(singleSegment.hasMergeSources());
		
		IBorderSegment mock1 = mock(IBorderSegment.class);
		when(mock1.getID()).thenReturn(UUID.randomUUID());
		when(mock1.getStartIndex()).thenReturn(singleSegment.getMidpointIndex());
		when(mock1.length()).thenReturn(singleSegment.getTotalLength()-singleSegment.getMidpointIndex());
		singleSegment.addMergeSource(mock1);
		assertTrue(singleSegment.hasMergeSources());
	}

	@Test
	public void testGetStartIndex() {
		assertEquals(0, singleSegment.getStartIndex());
	}

	@Test
	public void testGetEndIndex() {
		assertEquals(0, singleSegment.getEndIndex());
	}

	@Test
	public void testGetProportionalIndex() {
		assertEquals(0, singleSegment.getProportionalIndex(0));
		assertEquals(singleSegment.getMidpointIndex(), singleSegment.getProportionalIndex(0.5));
		assertEquals(singleSegment.getEndIndex(), singleSegment.getProportionalIndex(1));
	}
	
	@Test
	public void testGetProportionalIndexExceptsBelowZero() {
		exception.expect(IllegalArgumentException.class);
		singleSegment.getProportionalIndex(-0.1);
	}
	
	@Test
	public void testGetProportionalIndexExceptsAboveOne() {
		exception.expect(IllegalArgumentException.class);
		singleSegment.getProportionalIndex(1.1);
	}

	@Test
	public void testGetIndexProportion() {
		assertEquals(0, singleSegment.getIndexProportion(singleSegment.getStartIndex()),0);
		assertEquals(0.5, singleSegment.getIndexProportion(singleSegment.getMidpointIndex()), 0);
		double penultimate =  (double)(singleSegmentProfile.size()-1)/ (double) singleSegmentProfile.size();
		assertEquals(penultimate, singleSegment.getIndexProportion(singleSegment.getEndIndex()-1),0);
	}

	@Test
	public void testGetName() {
		assertEquals("Seg_0", singleSegment.getName());
	}

	@Test
	public void testGetMidpointIndex() {
		assertEquals(singleSegment.getTotalLength()/2, singleSegment.getMidpointIndex());
	}

	@Test
	public void testGetDistanceToStartWithSingleSegment() {
		
		for(int i=0; i<singleSegment.getMidpointIndex(); i++) {
			assertEquals(i, singleSegment.getDistanceToStart(i));
		}
		for(int i=singleSegment.getMidpointIndex(); i<=singleSegment.getTotalLength(); i++) {
			assertEquals(singleSegment.getTotalLength()-i, singleSegment.getDistanceToStart(i));
		}
	}
	
	@Test
	public void testGetDistanceToStartWithDoubleSegment() throws UnavailableComponentException {
		IBorderSegment seg = doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_0);
		for(int i=seg.getStartIndex(); i<=seg.length()+seg.getStartIndex(); i++) {
			assertEquals(i, seg.getDistanceToStart(i));
		}
	}

	@Test
	public void testGetDistanceToEnd() {
		assertEquals(comp.getBorderLength(), singleSegment.getDistanceToEnd(0));
		assertEquals(singleSegment.length()/2, singleSegment.getDistanceToEnd(singleSegment.getMidpointIndex()));
		assertEquals(1, singleSegment.getDistanceToEnd(comp.getBorderLength()));
	}

	@Test
	public void testIsLocked() {
		assertFalse(singleSegment.isLocked());
	}

	@Test
	public void testSetLocked() {
		assertFalse(singleSegment.isLocked());
		singleSegment.setLocked(true);
		assertTrue(singleSegment.isLocked());
	}

	@Test
	public void testGetTotalLength() {
		assertEquals(comp.getBorderLength(), singleSegment.getTotalLength());
	}

	@Test
	public void testNextSegmentWithBaseSegment() {
		assertEquals(singleSegment, singleSegment.nextSegment());
	}
	
	@Test
	public void testNextSegmentWithChildSegment() {
		fail("Not yet implemented");
	}

	@Test
	public void testPrevSegmentWithBaseSegment() {
		assertEquals(singleSegment, singleSegment.prevSegment());
	}
	
	@Test
	public void testPrevSegmentWithChildSegment() {
		fail("Not yet implemented");
	}

	@Test
	public void testLength() {
		assertEquals(singleSegment.getTotalLength(), singleSegment.length() );
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
		assertTrue(singleSegment.wraps());
		assertEquals(2, doubleSegmentProfile.getSegmentCount());
		assertFalse(doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_0).wraps());
		assertTrue(doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_1).wraps());
		
	}

	@Override
	@Test
	public void testContains() throws UnavailableComponentException {
		IBorderSegment s0 = doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_0);
		IBorderSegment s1 = doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_1);
		
		
		assertTrue(s0.contains(0));
		assertTrue(s1.contains(0));
		for(int i=1; i<50; i++) {
			assertTrue(s0.contains(i));
			assertFalse(s1.contains(i));
		}
		assertTrue(s0.contains(50));
		assertTrue(s1.contains(50));
		for(int i=51; i<doubleSegmentProfile.size(); i++) {
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
		assertTrue(singleSegment.hasNextSegment());
		assertTrue(doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_0).hasNextSegment());
		assertTrue(doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_1).hasNextSegment());
	}

	@Test
	public void testHasPrevSegment() throws UnavailableComponentException {
		assertTrue(singleSegment.hasPrevSegment());
		assertTrue(doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_0).hasPrevSegment());
		assertTrue(doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_1).hasPrevSegment());
	}

	@Test
	public void testGetPosition() throws UnavailableComponentException {
		assertEquals(0, doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_0).getPosition());
		assertEquals(1, doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_1).getPosition());
	}

	@Test
	public void testIterator() {

		fail("Not yet implemented");
	}

	@Test
	public void testOverlaps() throws UnavailableComponentException, ProfileException {
		assertTrue(doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_0).overlaps(doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_1)));
		assertTrue(doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_1).overlaps(doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_0)));
		
		doubleSegmentProfile.splitSegment(doubleSegmentProfile.getSegmentContaining(1), 25, DOUBLE_SEG_ID_2, DOUBLE_SEG_ID_3);
		
		assertFalse(doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_1).overlaps(doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_2)));
		fail("Not yet implemented");
	}

}
