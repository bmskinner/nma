package com.bmskinner.nuclear_morphology.components.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.cells.SegmentedCellularComponent.DefaultSegmentedProfile;
import com.bmskinner.nuclear_morphology.components.cells.SegmentedCellularComponent.DefaultSegmentedProfile.BorderSegmentTree;

/**
 * Specific tests for the border segment tree. Basic methods are in 
 * IBorderSegmentTester
 * @author bms41
 * @see IBorderSegmentTester
 *
 */
public class BorderSegmentTreeTest extends DefaultSegmentedProfileTest {

	protected IProfileSegment singleSegment; // segment covering entire profile
	
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		singleSegment = singleSegmentProfile.getSegment(IProfileCollection.DEFAULT_SEGMENT_ID);
	}
	
	@Test
	public void testSegmentCannotBeCreatedWithDefaultIdSmallerThanProfile() {

		try {
			Constructor c = BorderSegmentTree.class.getDeclaredConstructor(DefaultSegmentedProfile.class, UUID.class, int.class, int.class, BorderSegmentTree.class);
			c.setAccessible(true);

			try {
				c.newInstance(IProfileCollection.DEFAULT_SEGMENT_ID,0, 50, null);
				fail("Should have thrown an illegal argument exception");
			} catch(IllegalArgumentException e) {
				
			}
			
		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | InvocationTargetException e) {

			e.printStackTrace();
			fail();
		}
		

	}


	@Test
	public void testSplitAt() throws ProfileException {

		
		((BorderSegmentTree)singleSegment).splitAt(singleSegment.getMidpointIndex(), UUID.randomUUID(), UUID.randomUUID());
		List<IProfileSegment> list = singleSegment.getMergeSources();
		IProfileSegment s0 = list.get(0);
		IProfileSegment s1 = list.get(1);
		
		assertEquals(0, s0.getStartIndex());
		assertEquals(singleSegment.getMidpointIndex(), s0.getEndIndex());
		assertEquals(singleSegment.getMidpointIndex(), s1.getStartIndex());
		assertEquals(0, s1.getEndIndex());
	}


	@Test
	public void testGetDistanceToStartWithDoubleSegment() throws UnavailableComponentException {
		IProfileSegment seg = doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_0);
		for(int i=seg.getStartIndex(); i<=seg.length()+seg.getStartIndex(); i++) {
			assertEquals(i, seg.getShortestDistanceToStart(i));
		}
	}


	@Test
	public void testNextSegmentWithBaseSegment() {
		assertEquals(singleSegment, singleSegment.nextSegment());
	}
	
	@Test
	public void testNextSegmentWithChildSegment() throws UnavailableComponentException {
		
		IProfileSegment seg_0 = doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_0);
		IProfileSegment seg_1 = doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_1);
		
		assertEquals(seg_1, seg_0.nextSegment());
		assertEquals(seg_0, seg_1.nextSegment());
	}

	@Test
	public void testPrevSegmentWithBaseSegment() {
		assertEquals(singleSegment, singleSegment.prevSegment());
	}
	
	@Test
	public void testPrevSegmentWithChildSegment() throws UnavailableComponentException {
		IProfileSegment seg_0 = doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_0);
		IProfileSegment seg_1 = doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_1);
		
		assertEquals(seg_1, seg_0.prevSegment());
		assertEquals(seg_0, seg_1.prevSegment());
	}



	@Test
	public void testTestLength() throws UnavailableComponentException {
		IProfileSegment seg_0 = doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_0);
		
		int start = seg_0.getStartIndex();
		int end  =  seg_0.getEndIndex();
		
		for(int i=start; i<end; i++) {
			int length = seg_0.testLength(i, end);
			assertEquals(end-i+1, length);			
		}

	}

	@Test
	public void testWraps() throws UnavailableComponentException {
		assertTrue(singleSegment.wraps());
		assertEquals(2, doubleSegmentProfile.getSegmentCount());
		assertFalse(doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_0).wraps());
		assertTrue(doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_1).wraps());
		
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
	public void testIteratorOnDoubleSegment() throws UnavailableComponentException {

		int[] segIndexes_0 = IntStream.range(0, 51).toArray();
		int[] segIndexes_1 = IntStream.range(50, doubleSegmentProfile.size()).toArray();
		
		Iterator<Integer> it_0 =  doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_0).iterator();
		for(int i=0; i<segIndexes_0.length; i++) {
			int index = it_0.next();
			assertEquals(segIndexes_0[i], index);
		}

		
		Iterator<Integer> it_1 =  doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_1).iterator();
		for(int i=0; i<segIndexes_1.length; i++) {
			int index = it_1.next();
			assertEquals(segIndexes_1[i], index);
		}
	}

	@Test
	public void testOverlapsBeyondEndpoints() throws UnavailableComponentException, ProfileException {
		assertFalse(doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_0).overlapsBeyondEndpoints(doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_1)));
		assertFalse(doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_1).overlapsBeyondEndpoints(doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_0)));
		
		doubleSegmentProfile.splitSegment(doubleSegmentProfile.getSegmentContaining(1), 25, DOUBLE_SEG_ID_2, DOUBLE_SEG_ID_3);
		
		assertFalse(doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_1).overlapsBeyondEndpoints(doubleSegmentProfile.getSegment(DOUBLE_SEG_ID_2)));

	}

}
