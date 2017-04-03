/**
 * 
 */
package components.generic;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;

/**
 * @author ben
 *
 */
public class DefaultBorderSegmentTest {

	DefaultBorderSegment test = new DefaultBorderSegment(0, 20, 100);
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#DefaultBorderSegment(int, int, int, java.util.UUID)}.
	 */
	@Test
	public void testDefaultBorderSegmentIntIntIntUUID() {
		UUID id = UUID.randomUUID();
		DefaultBorderSegment test = new DefaultBorderSegment(0, 20, 100, id);
		
		exception.expect(IllegalArgumentException.class);
		test = new DefaultBorderSegment(0, 20, 100, null);
		test = new DefaultBorderSegment(-1, 20, 100, id);
		test = new DefaultBorderSegment(0, 100, 100, id);
		test = new DefaultBorderSegment(0, 99, 100, id);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#DefaultBorderSegment(com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment)}.
	 */
	@Test
	public void testDefaultBorderSegmentIBorderSegment() {
		DefaultBorderSegment s1 = new DefaultBorderSegment(test);
		assertEquals(test, s1);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getID()}.
	 */
	@Test
	public void testGetID() {
		UUID id = UUID.randomUUID();
		DefaultBorderSegment test = new DefaultBorderSegment(0, 20, 100, id);
		
		assertEquals(id, test.getID());
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getMergeSources()}.
	 */
	@Test
	public void testGetMergeSources() {
		DefaultBorderSegment test = new DefaultBorderSegment(0, 20, 100);
						
		DefaultBorderSegment s1 = new DefaultBorderSegment(0, 11, 100);
		DefaultBorderSegment s2 = new DefaultBorderSegment(11, 20, 100);
		
		test.addMergeSource(s1);
		test.addMergeSource(s2);
		
		assertTrue(test.hasMergeSources());
		int[] mgeStart = { 0, 11 };
		int[] mgeEnd   = { 11, 20 };
		
		List<IBorderSegment> sources = test.getMergeSources();
		for(int i=0; i<sources.size(); i++){
			IBorderSegment s = sources.get(i);
			assertEquals(mgeStart[i], s.getStartIndex());
			assertEquals(  mgeEnd[i], s.getEndIndex());	
		}
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#addMergeSource(com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment)}.
	 */
	@Test
	public void testAddMergeSource() {
		DefaultBorderSegment test = new DefaultBorderSegment(0, 20, 100);
		
		// proper merge source
		DefaultBorderSegment s1 = new DefaultBorderSegment(0, 10, 100);
		test.addMergeSource(s1);
		
		// invalid merge source - out of range
		DefaultBorderSegment s2 = new DefaultBorderSegment(10, 30, 100);
		
		exception.expect(IllegalArgumentException.class);
		test.addMergeSource(s2);

		// invalid merge source - wrong length
		DefaultBorderSegment s3 = new DefaultBorderSegment(10, 20, 200);
		exception.expect(IllegalArgumentException.class);
		test.addMergeSource(s3);
		
		// invalid merge source - out of range
		DefaultBorderSegment s4 = new DefaultBorderSegment(-1, 20, 100);
		exception.expect(IllegalArgumentException.class);
		test.addMergeSource(s4);
		
		// invalid merge source - null
		exception.expect(IllegalArgumentException.class);
		test.addMergeSource(null);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#hasMergeSources()}.
	 */
	@Test
	public void testHasMergeSources() {
		DefaultBorderSegment test = new DefaultBorderSegment(0, 20, 100);
		
		// with no sources
		assertFalse(test.hasMergeSources());
		
		// with sources
		
		DefaultBorderSegment s1 = new DefaultBorderSegment(0, 10, 100);
		DefaultBorderSegment s2 = new DefaultBorderSegment(11, 20, 100);
		
		test.addMergeSource(s1);
		test.addMergeSource(s2);
		
		assertTrue(test.hasMergeSources());
	}


	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getStartIndex()}.
	 */
	@Test
	public void testGetStartIndex() {

		DefaultBorderSegment test = new DefaultBorderSegment(0, 20, 100);
		
		assertEquals(0, test.getStartIndex());
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getEndIndex()}.
	 */
	@Test
	public void testGetEndIndex() {
		DefaultBorderSegment test = new DefaultBorderSegment(0, 20, 100);
		
		assertEquals(20, test.getEndIndex());
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getProportionalIndex(double)}.
	 */
	@Test
	public void testGetProportionalIndex() {
		DefaultBorderSegment test = new DefaultBorderSegment(0, 20, 100);
		assertEquals(10, test.getProportionalIndex(0.5));
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getIndexProportion(int)}.
	 */
	@Test
	public void testGetIndexProportion() {
		DefaultBorderSegment test = new DefaultBorderSegment(0, 20, 100);
		assertEquals(0.5, test.getIndexProportion(10), 0);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getName()}.
	 */
	@Test
	public void testGetName() {
		DefaultBorderSegment test = new DefaultBorderSegment(0, 20, 100);
		assertEquals("Seg_0", test.getName());
		
		test.setPosition(1);
		assertEquals("Seg_1", test.getName());
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getMidpointIndex()}.
	 */
	@Test
	public void testGetMidpointIndex() {
		DefaultBorderSegment test = new DefaultBorderSegment(0, 20, 100);
		assertEquals(10, test.getMidpointIndex());
		
		/*
		 * Wrapping segment
		 */
		test = new DefaultBorderSegment(90, 10, 100);
		assertEquals(0, test.getMidpointIndex());
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getDistanceToStart(int)}.
	 */
	@Test
	public void testGetDistanceToStart() {
		/*
		 * Within segment
		 */
		DefaultBorderSegment test = new DefaultBorderSegment(0, 20, 100);
		assertEquals(5, test.getDistanceToStart(5));
		
		/*
		 * Outside segment
		 */
		assertEquals(10, test.getDistanceToStart(90));
		
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getDistanceToEnd(int)}.
	 */
	@Test
	public void testGetDistanceToEnd() {
		/*
		 * Within segment
		 */
		DefaultBorderSegment test = new DefaultBorderSegment(0, 20, 100);
		assertEquals(15, test.getDistanceToEnd(5));
		
		/*
		 * Outside segment
		 */
		assertEquals(30, test.getDistanceToEnd(90));
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#isLocked()}.
	 */
	@Test
	public void testIsLocked() {
		// Created unlocked
		DefaultBorderSegment test = new DefaultBorderSegment(0, 20, 100);
		assertFalse(test.isLocked());
		
		test.setLocked(true);
		assertTrue(test.isLocked());
		test.setLocked(false);
		assertFalse(test.isLocked());
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getTotalLength()}.
	 */
	@Test
	public void testGetTotalLength() {
		DefaultBorderSegment test = new DefaultBorderSegment(0, 20, 100);
		assertEquals(100, test.getTotalLength());
		
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#nextSegment()}.
	 */
	@Test
	public void testNextSegment() {
		DefaultBorderSegment s1 = new DefaultBorderSegment(0,  20, 100);
		DefaultBorderSegment s2 = new DefaultBorderSegment(20, 30, 100);
		
		s1.setNextSegment(s2);
		
		assertEquals(s2, s1.nextSegment());
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#prevSegment()}.
	 */
	@Test
	public void testPrevSegment() {
		DefaultBorderSegment s1 = new DefaultBorderSegment(0,  20, 100);
		DefaultBorderSegment s2 = new DefaultBorderSegment(20, 30, 100);
		
		s2.setPrevSegment(s1);
		
		assertEquals(s1, s2.prevSegment());
	}

	
	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#length()}.
	 */
	@Test
	public void testLength() {
		DefaultBorderSegment s1 = new DefaultBorderSegment(0,  20, 100);
		DefaultBorderSegment s2 = new DefaultBorderSegment(90, 30, 100);
		
		assertEquals(20, s1.length());
		assertEquals(40, s2.length());
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#testLength(int, int)}.
	 */
	@Test
	public void testTestLength() {
		DefaultBorderSegment s1 = new DefaultBorderSegment(0,  20, 100);
		assertEquals(20, s1.length());
		
		DefaultBorderSegment s2 = new DefaultBorderSegment(90,  20, 100);
		assertEquals(30, s2.length());
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#wraps(int, int)}.
	 */
	@Test
	public void testWrapsIntInt() {
		DefaultBorderSegment s1 = new DefaultBorderSegment(0,  20, 100);
		
		assertFalse(s1.wraps(0, 10));
		assertTrue(s1.wraps(90, 10));
		
		exception.expect(IllegalArgumentException.class);
		s1.wraps(-1, 10);
		s1.wraps(0, 100);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#wraps()}.
	 */
	@Test
	public void testWraps() {
		
		DefaultBorderSegment s1 = new DefaultBorderSegment(0,  20, 100);
		assertFalse(s1.wraps());
		
		
		DefaultBorderSegment s2 = new DefaultBorderSegment(90, 30, 100);
		assertTrue(s2.wraps());
		
		DefaultBorderSegment s3 = new DefaultBorderSegment(99, 30, 100);
		assertTrue(s3.wraps());
		
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#contains(int)}.
	 */
	@Test
	public void testContains() {
		DefaultBorderSegment s1 = new DefaultBorderSegment(0,  20, 100);
		assertTrue(s1.contains(0));
		assertTrue(s1.contains(10));
		assertTrue(s1.contains(20));
		assertFalse(s1.contains(99));
		assertFalse(s1.contains(21));
		assertFalse(s1.contains(60));
		
		// Invalid inputs
		assertFalse(s1.contains(-1));
		assertFalse(s1.contains(101));
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#testContains(int, int, int)}.
	 */
	@Test
	public void testTestContains() {
		DefaultBorderSegment s1 = new DefaultBorderSegment(0,  20, 100);
		assertTrue(s1.testContains(0, 25, 0));
		assertTrue(s1.testContains(0, 25, 10));
		assertTrue(s1.testContains(0, 25, 20));
		assertFalse(s1.testContains(0, 25, 99));
		assertTrue(s1.testContains(0, 25, 21));
		assertFalse(s1.testContains(0, 25, 60));
		
		assertFalse(s1.testContains(90, 25, 60));
		assertTrue(s1.testContains(90, 25, 90));
		assertFalse(s1.testContains(90, 25, 89));
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#update(int, int)}.
	 */
	@Test
	public void testUpdate() {
		/*
		 * Lone segment
		 */
		DefaultBorderSegment s1 = new DefaultBorderSegment(0,  20, 100);
		
		assertTrue(s1.update(0, 25));
		assertTrue(s1.update(90, 20));
		assertTrue(s1.update(0, 25));
		
		/*
		 * Linked segment
		 */
		DefaultBorderSegment s2 = new DefaultBorderSegment(25,  40, 100);
		s1.setNextSegment(s2);
		s2.setPrevSegment(s1);
		
		// No effect on end of s2, but start is updated
		assertTrue(s1.update(0, 26));
		assertEquals(26, s2.getStartIndex());
		assertEquals(40, s2.getEndIndex());
		
		// Out of range of s2; should fail
		assertFalse(s1.update(0, 42));
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#setNextSegment(com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment)}.
	 */
	@Test
	public void testSetNextSegment() {
		DefaultBorderSegment s1 = new DefaultBorderSegment(0, 11, 100);
		DefaultBorderSegment s2 = new DefaultBorderSegment(11, 20, 100);
		DefaultBorderSegment s3 = new DefaultBorderSegment(20, 30, 100);
		DefaultBorderSegment s4 = new DefaultBorderSegment(11, 20, 90);
		
		
		assertFalse(s1.hasNextSegment());
		s1.setNextSegment(s2);
		assertTrue(s1.hasNextSegment());
		
		// Add null and invalid segments
		exception.expect(IllegalArgumentException.class);
		s1.setNextSegment(null);
		s1.setNextSegment(s3);
		s1.setNextSegment(s4);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#setPrevSegment(com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment)}.
	 */
	@Test
	public void testSetPrevSegment() {
		DefaultBorderSegment s1 = new DefaultBorderSegment(0, 11, 100);
		DefaultBorderSegment s2 = new DefaultBorderSegment(11, 20, 100);
		DefaultBorderSegment s3 = new DefaultBorderSegment(0, 5, 100);
		DefaultBorderSegment s4 = new DefaultBorderSegment(0, 11, 90);
		
		
		assertFalse(s2.hasPrevSegment());
		s2.setPrevSegment(s1);
		assertTrue(s2.hasPrevSegment());
		
		// Add null and invalid segments
		exception.expect(IllegalArgumentException.class);
		s2.setPrevSegment(null);
		s2.setPrevSegment(s3);
		s2.setPrevSegment(s4);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#hasNextSegment()}.
	 */
	@Test
	public void testHasNextSegment() {
		DefaultBorderSegment s1 = new DefaultBorderSegment(0, 11, 100);
		DefaultBorderSegment s2 = new DefaultBorderSegment(11, 20, 100);
		
		assertFalse(s1.hasNextSegment());
		s1.setNextSegment(s2);
		assertTrue(s1.hasNextSegment());
		
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#hasPrevSegment()}.
	 */
	@Test
	public void testHasPrevSegment() {
		DefaultBorderSegment s1 = new DefaultBorderSegment(0, 11, 100);
		DefaultBorderSegment s2 = new DefaultBorderSegment(11, 20, 100);
		assertFalse(s2.hasPrevSegment());
		s2.setPrevSegment(s1);
		assertTrue(s2.hasPrevSegment());
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#setPosition(int)}.
	 */
	@Test
	public void testSetPosition() {
		DefaultBorderSegment s1 = new DefaultBorderSegment(0, 11, 100);
		assertEquals(0, s1.getPosition());
		s1.setPosition(3);
		assertEquals(3, s1.getPosition());
		
		// Out of range
		exception.expect(IllegalArgumentException.class);
		s1.setPosition(-1);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#iterator()}.
	 */
	@Test
	public void testIterator() {
		
		// Standard
		DefaultBorderSegment s1 = new DefaultBorderSegment(0, 11, 100);
		
		Iterator<Integer> it = s1.iterator();
		
		int i=0;
		
		int[] exp = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
		
		while(it.hasNext()){
			int index = it.next();
			assertEquals(exp[i], index);
			i++;
		}
		
		// Wrapping
		DefaultBorderSegment s2 = new DefaultBorderSegment(95, 5, 100);
		i=0;
		
		int[] exp2 = { 95, 96, 97, 98, 99, 0, 1, 2, 3, 4, 5 };
		
		 it = s2.iterator();
		while(it.hasNext()){
			int index = it.next();
			assertEquals(exp2[i], index);
			i++;
		}
	}
	
	@Test
	public void testNudgeUnlinked(){
		
		testNudgeUnlinkedWithoutMergeSources();
		testNudgeUnlinkedWithMergeSources();
	}
	
	@Test
	public void testNudgeUnlinkedWithoutMergeSources(){
		int[] start = { 0,  10, 30, 88 };
		int[] end   = { 10, 30, 88, 0  };
		
		List<IBorderSegment> list = new ArrayList<IBorderSegment>();
		
		for(int i=0; i<start.length; i++){
			list.add(new DefaultBorderSegment(start[i], end[i], 100));
		}
		
		/*
		 * Offset of 1
		 */
		
		int[] expStart = { 1,  11, 31, 89 };
		int[] expEnd   = { 11, 31, 89, 1 };
		
		List<IBorderSegment> result = IBorderSegment.nudgeUnlinked(list, 1);
		
		for(int i=0; i<list.size(); i++){
			IBorderSegment s = result.get(i);
			assertEquals(expStart[i], s.getStartIndex());
			assertEquals(  expEnd[i], s.getEndIndex());			
		}
		
		/*
		 * Offset of -2
		 */
		int[] expStart_2 = { 98, 8,  28, 86 };
		int[] expEnd_2   = { 8, 28,  86, 98 };
		
		result = IBorderSegment.nudgeUnlinked(list, -2);
		
		for(int i=0; i<list.size(); i++){
			IBorderSegment s = result.get(i);
			assertEquals(expStart_2[i], s.getStartIndex());
			assertEquals(  expEnd_2[i], s.getEndIndex());			
		}
		
		/*
		 * Invalid input
		 */
		exception.expect(IllegalArgumentException.class);
		IBorderSegment.nudgeUnlinked(null, -2);
	}
	
	
	@Test
	public void testNudgeUnlinkedWithMergeSources(){
		
		int[] start = { 0,  10, 30, 88 };
		int[] end   = { 10, 30, 88, 0  };
		
		List<IBorderSegment> list = new ArrayList<IBorderSegment>();
		
		for(int i=0; i<start.length; i++){
			list.add(new DefaultBorderSegment(start[i], end[i], 100));
		}
		// Add merge sources to seg 1
		
		list.get(0).addMergeSource( new DefaultBorderSegment(0, 4,  100) );
		list.get(0).addMergeSource( new DefaultBorderSegment(4, 10, 100) );
		
		
		/*
		 * Offset of 1
		 */
		
		int[] expStart = { 1,  11, 31, 89 };
		int[] expEnd   = { 11, 31, 89, 1 };
		
		List<IBorderSegment> result = IBorderSegment.nudgeUnlinked(list, 1);
		
		for(int i=0; i<list.size(); i++){
			IBorderSegment s = result.get(i);
			assertEquals(expStart[i], s.getStartIndex());
			assertEquals(  expEnd[i], s.getEndIndex());			
		}
		
		
		int[] mgeStart = { 1, 5 };
		int[] mgeEnd   = { 5, 11 };
		List<IBorderSegment> sources = list.get(0).getMergeSources();
		for(int i=0; i<sources.size(); i++){
			IBorderSegment s = sources.get(i);
			assertEquals(mgeStart[i], s.getStartIndex());
			assertEquals(  mgeEnd[i], s.getEndIndex());	
		}
		
		
	}
	
	@Test
	public void testCopy(){
		
		int[] start = { 0,  10, 30, 88 };
		int[] end   = { 10, 30, 88, 0  };
		
		List<IBorderSegment> list = new ArrayList<IBorderSegment>();
		
		for(int i=0; i<start.length; i++){
			list.add(new DefaultBorderSegment(start[i], end[i], 100));
		}
		
		try {
			IBorderSegment.linkSegments(list);
		} catch (ProfileException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			fail("Error linking segments");
			
		}
		
		try {
			List<IBorderSegment> result = IBorderSegment.copy(list);
			
			for(int i=0; i<start.length; i++){
				IBorderSegment t = list.get(i);
				IBorderSegment r = result.get(i);
				
				assertEquals(t, r);
				
			}
			
		} catch (ProfileException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			fail("Error copying segments");
			
		}
	}

}
