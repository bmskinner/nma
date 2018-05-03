/**
 * 
 */
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
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment.SegmentUpdateException;

/**
 * @author ben
 *
 */
public class DefaultBorderSegmentTest {

	private DefaultBorderSegment test = new DefaultBorderSegment(0, 20, 100);
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Before
	public void setUp(){
		test = new DefaultBorderSegment(0, 20, 100);
	}
	
	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#DefaultBorderSegment(int, int, int, java.util.UUID)}.
	 */
	@Test
	public void testDefaultBorderSegmentIntIntIntUUID() {
		new DefaultBorderSegment(0, 20, 100, UUID.randomUUID());
	}

	@Test
    public void testDefaultBorderSegmentIntIntIntUUIDExceptsOnNullId() {
        exception.expect(IllegalArgumentException.class);
        new DefaultBorderSegment(0, 20, 100, null);
    }
	
	@Test
    public void testDefaultBorderSegmentIntIntIntUUIDExceptsOnNegativeStart() {
        exception.expect(IllegalArgumentException.class);
        new DefaultBorderSegment(-1, 20, 100, UUID.randomUUID());
    }
	
	@Test
    public void testDefaultBorderSegmentIntIntIntUUIDExceptsOnLengthEqualsProfile() {
        exception.expect(IllegalArgumentException.class);
        new DefaultBorderSegment(0, 100, 100, UUID.randomUUID());
    }
	
	@Test
    public void testDefaultBorderSegmentIntIntIntUUIDExceptsOnLengthTooLong() {
        exception.expect(IllegalArgumentException.class);
        new DefaultBorderSegment(0, 99, 100, UUID.randomUUID());
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
		// proper merge source
		DefaultBorderSegment s1 = new DefaultBorderSegment(0, 10, 100);
		test.addMergeSource(s1);
		
		assertEquals(s1, test.getMergeSources().get(0));
	}
	
	@Test
	public void testAddMergeSourceOutOfRangeArg0() {
		// invalid merge source - out of range
		DefaultBorderSegment s2 = new DefaultBorderSegment(10, 30, 100);

		exception.expect(IllegalArgumentException.class);
		test.addMergeSource(s2);
	}
	
	@Test
	public void testAddMergeSourceExceptsOnOutOfRangeArg1() {	
		// invalid merge source - out of range
		exception.expect(IllegalArgumentException.class);
		DefaultBorderSegment s4 = new DefaultBorderSegment(-1, 20, 100);
	}
	
	@Test
	public void testAddMergeSourceWrongLength() {
		// invalid merge source - wrong length
		DefaultBorderSegment s = new DefaultBorderSegment(10, 20, 200);
		exception.expect(IllegalArgumentException.class);
		test.addMergeSource(s);
	}
	
	@Test
	public void testAddMergeSourceNull() {
		// invalid merge source - null
		exception.expect(IllegalArgumentException.class);
		test.addMergeSource(null);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#hasMergeSources()}.
	 */
	@Test
	public void testHasMergeSources() {
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
		assertEquals(0, test.getStartIndex());
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getEndIndex()}.
	 */
	@Test
	public void testGetEndIndex() {
		assertEquals(20, test.getEndIndex());
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getProportionalIndex(double)}.
	 */
	@Test
	public void testGetProportionalIndex() {
		assertEquals(10, test.getProportionalIndex(0.5));
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getIndexProportion(int)}.
	 */
	@Test
	public void testGetIndexProportion() {
		assertEquals(0.5, test.getIndexProportion(10), 0);
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getName()}.
	 */
	@Test
	public void testGetName() {
		assertEquals("Seg_0", test.getName());
		
		test.setPosition(1);
		assertEquals("Seg_1", test.getName());
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getMidpointIndex()}.
	 */
	@Test
	public void testGetMidpointIndex() {
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
		assertEquals("Within segment", 15, test.getDistanceToEnd(5));
		assertEquals("Outside segment", 30, test.getDistanceToEnd(90));
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#isLocked()}.
	 */
	@Test
	public void testIsLocked() {

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

		// Non wrapping segment
		for(int i=0; i<=25; i++) {
			assertTrue(IBorderSegment.contains(0, 25, i, s1.getTotalLength()));
		}
		
		for(int i=26; i<s1.length(); i++) {
			assertFalse(IBorderSegment.contains(0, 25, i, s1.getTotalLength()));
		}
		
		// Wrapping segment
		for(int i=90; i<s1.getTotalLength(); i++) {
			assertTrue(IBorderSegment.contains(90, 25, i, s1.getTotalLength()));
		}
		for(int i=0; i<=25; i++) {
			assertTrue(IBorderSegment.contains(90, 25, i, s1.getTotalLength()));
		}
		for(int i=26; i<90; i++) {
			assertFalse(IBorderSegment.contains(90, 25, i, s1.getTotalLength()));
		}
	}
	
	
	@Test
	public void testUpdateLoneSegment() throws SegmentUpdateException {
		DefaultBorderSegment s1 = new DefaultBorderSegment(0,  20, 100);
		assertTrue(s1.update(0, 25));
		assertEquals(0, s1.getStartIndex());
		assertEquals(25, s1.getEndIndex());

		assertTrue(s1.update(90, 20));
		assertEquals(90, s1.getStartIndex());
		assertEquals(20, s1.getEndIndex());

		assertTrue(s1.update(0, 25));
		assertEquals(0, s1.getStartIndex());
		assertEquals(25, s1.getEndIndex());
	}
	
	@Test
	public void testUpdateLoneSegmentFailsWhenLocked() throws SegmentUpdateException {
		test.setLocked(true);
		assertFalse(test.update(10, 20));
	}
	
	@Test
	public void testUpdateLoneSegmentFailsWhenTooShort() throws SegmentUpdateException {
		assertFalse(test.update(0, 2));
		assertTrue(test.update(0,  3));
	}
	
	
	@Test
	public void testUpdateLoneSegmentFailsOnStartOutOfBoundsLow() throws SegmentUpdateException {
		exception.expect(IllegalArgumentException.class);
		test.update(-1, 20);
	}
	
	@Test
	public void testUpdateLoneSegmentFailsOnStartOutOfBoundsHigh() throws SegmentUpdateException {
		exception.expect(IllegalArgumentException.class);
		test.update(101, 20);
	}
	
	@Test
	public void testUpdateLoneSegmentFailsOnEndOutOfBoundsLow() throws SegmentUpdateException {
		exception.expect(IllegalArgumentException.class);
		test.update(0, -1);
	}
	
	@Test
	public void testUpdateLoneSegmentFailsOnEndOutOfBoundsHigh() throws SegmentUpdateException {
		exception.expect(IllegalArgumentException.class);
		test.update(0, 101);
	}
	
	private List<IBorderSegment> createLinkedList() throws ProfileException{
		DefaultBorderSegment s1 = new DefaultBorderSegment(0,  25, 100);
		DefaultBorderSegment s2 = new DefaultBorderSegment(25,  40, 100);
		DefaultBorderSegment s3 = new DefaultBorderSegment(40,  0, 100);
		List<IBorderSegment> list = new ArrayList<>();
		list.add(s1);
		list.add(s2);
		list.add(s3);
		IBorderSegment.linkSegments(list);
		return list;
	}
		
	@Test
	public void testUpdatingLinkedSegmentsAffectsTwoSegments() throws SegmentUpdateException, ProfileException{
		List<IBorderSegment> list = createLinkedList();
		IBorderSegment s1 = list.get(0);
		IBorderSegment s2 = list.get(1);
		assertTrue(s1.hasNextSegment());

		// No effect on end of s2, but start is updated
		assertTrue(s1.update(0, 26));
		assertEquals(0, s1.getStartIndex());
		assertEquals(26, s1.getEndIndex());
		assertEquals(26, s2.getStartIndex());
		assertEquals(40, s2.getEndIndex());
	}
	
	
	/**
	 * @throws SegmentUpdateException
	 * @throws ProfileException 
	 */
	@Test
	public void testUpdateLinkedSegmentFailsWhenIndexOutOfBounds() throws SegmentUpdateException, ProfileException{
		List<IBorderSegment> list = createLinkedList();
		IBorderSegment s1 = list.get(0);
		IBorderSegment s2 = list.get(1);
		assertTrue(s1.hasNextSegment());


		// Out of range of s2; should fail
		assertFalse(s1.update(0, 42)); // s2 is 25-40; this should fail
		assertEquals(0, s1.getStartIndex());
		assertEquals(26, s1.getEndIndex());
		assertEquals(26, s2.getStartIndex());
		assertEquals(40, s2.getEndIndex());

		// Out of range of s2; should fail
		assertFalse(s2.update(90, 42));
		assertEquals(0, s1.getStartIndex());
		assertEquals(26, s1.getEndIndex());
		assertEquals(26, s2.getStartIndex());
		assertEquals(40, s2.getEndIndex());
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#update(int, int)}.
	 * @throws ProfileException 
	 * @throws SegmentUpdateException 
	 */
	@Test
	public void testUpdate() throws ProfileException, SegmentUpdateException {
		/*
		 * Complete profile of segments
		 */
		List<IBorderSegment> list = new ArrayList<IBorderSegment>();
		DefaultBorderSegment p1 = new DefaultBorderSegment(10, 20, 100);
		DefaultBorderSegment p2 = new DefaultBorderSegment(20, 45, 100);
		DefaultBorderSegment p3 = new DefaultBorderSegment(45, 89, 100);
		DefaultBorderSegment p4 = new DefaultBorderSegment(89, 10, 100);

		list.add(p1);
		list.add(p2);
		list.add(p3);
		list.add(p4);

		IBorderSegment.linkSegments(list);

		p1.update(5, 20);
		assertEquals(5, p1.getStartIndex());
		assertEquals(5, p4.getEndIndex());

		p4.update(89, 1);
		assertEquals(1, p1.getStartIndex());
		assertEquals(1, p4.getEndIndex());

		/*
		 * Can the profile be offset and still have segments adjusted?
		 */
		IProfile profile = new FloatProfile(10, 100);
		ISegmentedProfile sp = new SegmentedFloatProfile(profile, list);
	}
	
	@Test
	public void testUpdateWithMergeSources() throws SegmentUpdateException{
		IBorderSegment p1 = new DefaultBorderSegment(10, 30, 100);
		IBorderSegment m1 = new DefaultBorderSegment(10, 20, 100);
		IBorderSegment m2 = new DefaultBorderSegment(20, 30, 100);

		p1.addMergeSource(m1);
		p1.addMergeSource(m2);

		p1.update(14, 30);

		List<IBorderSegment> merges = p1.getMergeSources();
		assertEquals(0, merges.size());

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
	public void testCopy() throws ProfileException{
		
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

		List<IBorderSegment> result = IBorderSegment.copy(list);

		for(int i=0; i<start.length; i++){
		    IBorderSegment t = list.get(i);
		    IBorderSegment r = result.get(i);

		    assertEquals(t, r);

		}

	}

	@Test
    public void testOverlaps(){
	    
	    DefaultBorderSegment s1 = new DefaultBorderSegment(0,  20, 100);
	    DefaultBorderSegment s2 = new DefaultBorderSegment(20,  0, 100);
        DefaultBorderSegment s3 = new DefaultBorderSegment(10, 50, 100);
	    
        assertFalse(s1.overlaps(s2));
        assertFalse(s2.overlaps(s1));
        
        assertTrue(s1.overlaps(s3));
        assertTrue(s3.overlaps(s1));
        
        assertTrue(s2.overlaps(s3));
        assertTrue(s3.overlaps(s2));
	}
	
}
