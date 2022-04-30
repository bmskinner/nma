/**
 * 
 */
package com.bmskinner.nma.components.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nma.components.profiles.DefaultProfile;
import com.bmskinner.nma.components.profiles.DefaultProfileSegment;
import com.bmskinner.nma.components.profiles.DefaultSegmentedProfile;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileCollection;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;

/**
 * @author ben
 *
 */
public class DefaultBorderSegmentTest {

	private DefaultProfileSegment test = new DefaultProfileSegment(0, 20, 100);
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Before
	public void setUp(){
		test = new DefaultProfileSegment(0, 20, 100);
	}
	
	@Test
	public void testSegmentCannotBeCreatedWithDefaultIdSmallerThanProfile() {

		try {
			new DefaultProfileSegment(0, 50, 100, IProfileCollection.DEFAULT_SEGMENT_ID);
			fail("Should have thrown an illegal argument exception");
		} catch(IllegalArgumentException e) {

		}

	}
	
	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#DefaultBorderSegment(int, int, int, java.util.UUID)}.
	 */
	@Test
	public void testDefaultBorderSegmentIntIntIntUUID() {
		new DefaultProfileSegment(0, 20, 100, UUID.randomUUID());
	}
	
	@Test
    public void testDefaultBorderSegmentIntIntIntUUIDExceptsOnNegativeStart() {
        exception.expect(IllegalArgumentException.class);
        new DefaultProfileSegment(-1, 20, 100, UUID.randomUUID());
    }
	
	@Test
    public void testDefaultBorderSegmentIntIntIntUUIDExceptsOnLengthEqualsProfile() {
        exception.expect(IllegalArgumentException.class);
        new DefaultProfileSegment(0, 100, 100, UUID.randomUUID());
    }
	
	@Test
    public void testDefaultBorderSegmentIntIntIntUUIDExceptsOnLengthTooLong() {
        exception.expect(IllegalArgumentException.class);
        new DefaultProfileSegment(0, 99, 100, UUID.randomUUID());
    }
	
	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#DefaultBorderSegment(com.bmskinner.nma.components.profiles.IProfileSegment)}.
	 */
	@Test
	public void testDefaultBorderSegmentIBorderSegment() {
		DefaultProfileSegment s1 = new DefaultProfileSegment(test);
		assertEquals(test, s1);
	}

	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#getID()}.
	 */
	@Test
	public void testGetID() {
		UUID id = UUID.randomUUID();
		DefaultProfileSegment test = new DefaultProfileSegment(0, 20, 100, id);
		
		assertEquals(id, test.getID());
	}

	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#getMergeSources()}.
	 */
	@Test
	public void testGetMergeSources() {
						
		DefaultProfileSegment s1 = new DefaultProfileSegment(0, 11, 100);
		DefaultProfileSegment s2 = new DefaultProfileSegment(11, 20, 100);
		
		test.addMergeSource(s1);
		test.addMergeSource(s2);
		
		assertTrue(test.hasMergeSources());
		int[] mgeStart = { 0, 11 };
		int[] mgeEnd   = { 11, 20 };
		
		List<IProfileSegment> sources = test.getMergeSources();
		for(int i=0; i<sources.size(); i++){
			IProfileSegment s = sources.get(i);
			assertEquals(mgeStart[i], s.getStartIndex());
			assertEquals(  mgeEnd[i], s.getEndIndex());	
		}
	}


	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#hasMergeSources()}.
	 */
	@Test
	public void testHasMergeSources() {
		// with no sources
		assertFalse(test.hasMergeSources());
		
		// with sources
		
		DefaultProfileSegment s1 = new DefaultProfileSegment(0, 10, 100);
		DefaultProfileSegment s2 = new DefaultProfileSegment(11, 20, 100);
		
		test.addMergeSource(s1);
		test.addMergeSource(s2);
		
		assertTrue(test.hasMergeSources());
	}

	
	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#getName()}.
	 */
	@Test
	public void testGetName() {
		assertEquals("Seg_0", test.getName());
		
		test.setPosition(1);
		assertEquals("Seg_1", test.getName());
	}

	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#getShortestDistanceToStart(int)}.
	 */
	@Test
	public void testGetDistanceToStart() {
		assertEquals(5, test.getShortestDistanceToStart(5));
		
		/*
		 * Outside segment
		 */
		assertEquals(10, test.getShortestDistanceToStart(90));
		
	}

	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#getShortestDistanceToEnd(int)}.
	 */
	@Test
	public void testGetDistanceToEnd() {
		assertEquals("Within segment", 15, test.getShortestDistanceToEnd(5));
		assertEquals("Outside segment", 30, test.getShortestDistanceToEnd(90));
	}

	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#isLocked()}.
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
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#getProfileLength()}.
	 */
	@Test
	public void testGetTotalLength() {
		assertEquals(100, test.getProfileLength());
	}

	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#nextSegment()}.
	 */
	@Test
	public void testNextSegment() {
		DefaultProfileSegment s1 = new DefaultProfileSegment(0,  20, 100);
		DefaultProfileSegment s2 = new DefaultProfileSegment(20, 30, 100);
		
		s1.setNextSegment(s2);
		
		assertEquals(s2, s1.nextSegment());
	}

	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#prevSegment()}.
	 */
	@Test
	public void testPrevSegment() {
		DefaultProfileSegment s1 = new DefaultProfileSegment(0,  20, 100);
		DefaultProfileSegment s2 = new DefaultProfileSegment(20, 30, 100);
		
		s2.setPrevSegment(s1);
		
		assertEquals(s1, s2.prevSegment());
	}

	
	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#length()}.
	 */
	@Test
	public void testLength() {
		DefaultProfileSegment s1 = new DefaultProfileSegment(0,  20, 100);
		DefaultProfileSegment s2 = new DefaultProfileSegment(90, 30, 100);
		
		assertEquals(21, s1.length());
		assertEquals(41, s2.length());
	}

	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#testLength(int, int)}.
	 */
	@Test
	public void testTestLength() {
		DefaultProfileSegment s1 = new DefaultProfileSegment(0,  20, 100);
		assertEquals(21, s1.length());
		
		DefaultProfileSegment s2 = new DefaultProfileSegment(90,  20, 100);
		assertEquals(31, s2.length());
	}

	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#wraps(int, int)}.
	 */
	@Test
	public void testWrapsIntInt() {
		DefaultProfileSegment s1 = new DefaultProfileSegment(0,  20, 100);
		
		assertFalse(s1.wraps(0, 10));
		assertTrue(s1.wraps(90, 10));
		
		exception.expect(IllegalArgumentException.class);
		s1.wraps(-1, 10);
		s1.wraps(0, 100);
	}

	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#wraps()}.
	 */
	@Test
	public void testWraps() {
		
		DefaultProfileSegment s1 = new DefaultProfileSegment(0,  20, 100);
		assertFalse(s1.wraps());
		
		
		DefaultProfileSegment s2 = new DefaultProfileSegment(90, 30, 100);
		assertTrue(s2.wraps());
		
		DefaultProfileSegment s3 = new DefaultProfileSegment(99, 30, 100);
		assertTrue(s3.wraps());
		
	}

	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#contains(int)}.
	 */
	@Test
	public void testContains() {
		DefaultProfileSegment s1 = new DefaultProfileSegment(0,  20, 100);
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
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#testContains(int, int, int)}.
	 */
	@Test
	public void testTestContains() {
		DefaultProfileSegment s1 = new DefaultProfileSegment(0,  20, 100);

		// Non wrapping segment
		for(int i=0; i<=25; i++) {
			assertTrue(IProfileSegment.contains(0, 25, i, s1.getProfileLength()));
		}
		
		for(int i=26; i<s1.length(); i++) {
			assertFalse(IProfileSegment.contains(0, 25, i, s1.getProfileLength()));
		}
		
		// Wrapping segment
		for(int i=90; i<s1.getProfileLength(); i++) {
			assertTrue(IProfileSegment.contains(90, 25, i, s1.getProfileLength()));
		}
		for(int i=0; i<=25; i++) {
			assertTrue(IProfileSegment.contains(90, 25, i, s1.getProfileLength()));
		}
		for(int i=26; i<90; i++) {
			assertFalse(IProfileSegment.contains(90, 25, i, s1.getProfileLength()));
		}
	}
	
	
//	@Test
//	public void testUpdateLoneSegment() throws SegmentUpdateException {
//		DefaultBorderSegment s1 = new DefaultBorderSegment(0,  20, 100);
//		assertTrue(s1.update(0, 25));
//		assertEquals(0, s1.getStartIndex());
//		assertEquals(25, s1.getEndIndex());
//
//		assertTrue(s1.update(90, 20));
//		assertEquals(90, s1.getStartIndex());
//		assertEquals(20, s1.getEndIndex());
//
//		assertTrue(s1.update(0, 25));
//		assertEquals(0, s1.getStartIndex());
//		assertEquals(25, s1.getEndIndex());
//	}
//	
//	@Test
//	public void testUpdateLoneSegmentFailsWhenLocked() throws SegmentUpdateException {
//		test.setLocked(true);
//		assertFalse(test.update(10, 20));
//	}
//	
//	@Test
//	public void testUpdateLoneSegmentFailsWhenTooShort() throws SegmentUpdateException {
//		assertFalse(test.update(0, 2));
//		assertTrue(test.update(0,  3));
//	}
//	
//	
//	@Test
//	public void testUpdateLoneSegmentFailsOnStartOutOfBoundsLow() throws SegmentUpdateException {
//		exception.expect(IllegalArgumentException.class);
//		test.update(-1, 20);
//	}
//	
//	@Test
//	public void testUpdateLoneSegmentFailsOnStartOutOfBoundsHigh() throws SegmentUpdateException {
//		exception.expect(IllegalArgumentException.class);
//		test.update(101, 20);
//	}
//	
//	@Test
//	public void testUpdateLoneSegmentFailsOnEndOutOfBoundsLow() throws SegmentUpdateException {
//		exception.expect(IllegalArgumentException.class);
//		test.update(0, -1);
//	}
//	
//	@Test
//	public void testUpdateLoneSegmentFailsOnEndOutOfBoundsHigh() throws SegmentUpdateException {
//		exception.expect(IllegalArgumentException.class);
//		test.update(0, 101);
//	}
	
	private List<IProfileSegment> createLinkedList() throws ProfileException{
		DefaultProfileSegment s1 = new DefaultProfileSegment(0,  25, 100);
		DefaultProfileSegment s2 = new DefaultProfileSegment(25,  40, 100);
		DefaultProfileSegment s3 = new DefaultProfileSegment(40,  0, 100);
		List<IProfileSegment> list = new ArrayList<>();
		list.add(s1);
		list.add(s2);
		list.add(s3);
		IProfileSegment.linkSegments(list);
		return list;
	}
		
	@Test
	public void testUpdatingLinkedSegmentsAffectsTwoSegments() throws SegmentUpdateException, ProfileException{
		List<IProfileSegment> list = createLinkedList();
		IProfileSegment s1 = list.get(0);
		IProfileSegment s2 = list.get(1);
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
		List<IProfileSegment> list = createLinkedList();
		IProfileSegment s1 = list.get(0);
		IProfileSegment s2 = list.get(1);
		assertTrue(s1.hasNextSegment());


		try {
			s1.update(0, 42); // s2 is 25-40; this should fail
			fail("The segment should have thrown an exception for invalid update");
		} catch(SegmentUpdateException e) {
			assertEquals(0, s1.getStartIndex());
			assertEquals(25, s1.getEndIndex());
			assertEquals(25, s2.getStartIndex());
			assertEquals(40, s2.getEndIndex());
		}
		
		try {
			s2.update(90, 42); // Out of range of s2; should fail
			fail("The segment should have thrown an exception for invalid update");
		} catch(SegmentUpdateException e) {
			assertEquals(0, s1.getStartIndex());
			assertEquals(25, s1.getEndIndex());
			assertEquals(25, s2.getStartIndex());
			assertEquals(40, s2.getEndIndex());
		}		
	}

	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#update(int, int)}.
	 * @throws ProfileException 
	 * @throws SegmentUpdateException 
	 */
	@Test
	public void testUpdate() throws ProfileException, SegmentUpdateException {
		/*
		 * Complete profile of segments
		 */
		List<IProfileSegment> list = new ArrayList<IProfileSegment>();
		DefaultProfileSegment p1 = new DefaultProfileSegment(10, 20, 100);
		DefaultProfileSegment p2 = new DefaultProfileSegment(20, 45, 100);
		DefaultProfileSegment p3 = new DefaultProfileSegment(45, 89, 100);
		DefaultProfileSegment p4 = new DefaultProfileSegment(89, 10, 100);

		list.add(p1);
		list.add(p2);
		list.add(p3);
		list.add(p4);

		IProfileSegment.linkSegments(list);

		p1.update(5, 20);
		assertEquals(5, p1.getStartIndex());
		assertEquals(5, p4.getEndIndex());

		p4.update(89, 1);
		assertEquals(1, p1.getStartIndex());
		assertEquals(1, p4.getEndIndex());

		/*
		 * Can the profile be offset and still have segments adjusted?
		 */
		IProfile profile = new DefaultProfile(10, 100);
		ISegmentedProfile sp = new DefaultSegmentedProfile(profile, list);
	}
	
	/**
	 * Test that when a segment with merge sources is updated, the merge sources also
	 * update
	 * @throws Exception
	 */
	@Test
	public void testUpdateWithMergeSourcesPreservesSources() throws Exception {
		
		int midpoint = 30;
		IProfileSegment p1 = new DefaultProfileSegment(10, 50, 100);
		IProfileSegment m1 = new DefaultProfileSegment(10, midpoint, 100);
		IProfileSegment m2 = new DefaultProfileSegment(midpoint, 50, 100);

		p1.addMergeSource(m1);
		p1.addMergeSource(m2);

		int newStart = 14;
		int newEnd   = 40;
		p1.update(newStart, newEnd);
		
		assertEquals(newStart, p1.getMergeSource(m1.getID()).getStartIndex());
		assertEquals(midpoint, p1.getMergeSource(m1.getID()).getEndIndex());
		assertEquals(midpoint, p1.getMergeSource(m2.getID()).getStartIndex());
		assertEquals(newEnd, p1.getMergeSource(m2.getID()).getEndIndex());

		List<IProfileSegment> merges = p1.getMergeSources();
		assertEquals(2, merges.size());

	}
	
	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#setNextSegment(com.bmskinner.nma.components.profiles.IProfileSegment)}.
	 */
	@Test
	public void testSetNextSegment() {
		DefaultProfileSegment s1 = new DefaultProfileSegment(0, 11, 100);
		DefaultProfileSegment s2 = new DefaultProfileSegment(11, 20, 100);
		DefaultProfileSegment s3 = new DefaultProfileSegment(20, 30, 100);
		DefaultProfileSegment s4 = new DefaultProfileSegment(11, 20, 90);
		
		
		assertFalse(s1.hasNextSegment());
		s1.setNextSegment(s2);
		assertTrue(s1.hasNextSegment());
		
		// Add null and invalid segments
		exception.expect(IllegalArgumentException.class);
		s1.setNextSegment(s3);
		s1.setNextSegment(s4);
	}

	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#setPrevSegment(com.bmskinner.nma.components.profiles.IProfileSegment)}.
	 */
	@Test
	public void testSetPrevSegment() {
		DefaultProfileSegment s1 = new DefaultProfileSegment(0, 11, 100);
		DefaultProfileSegment s2 = new DefaultProfileSegment(11, 20, 100);
		DefaultProfileSegment s3 = new DefaultProfileSegment(0, 5, 100);
		DefaultProfileSegment s4 = new DefaultProfileSegment(0, 11, 90);
		
		
		assertFalse(s2.hasPrevSegment());
		s2.setPrevSegment(s1);
		assertTrue(s2.hasPrevSegment());
		
		// Add null and invalid segments
		exception.expect(IllegalArgumentException.class);
		s2.setPrevSegment(s3);
		s2.setPrevSegment(s4);
	}

	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#hasNextSegment()}.
	 */
	@Test
	public void testHasNextSegment() {
		DefaultProfileSegment s1 = new DefaultProfileSegment(0, 11, 100);
		DefaultProfileSegment s2 = new DefaultProfileSegment(11, 20, 100);
		
		assertFalse(s1.hasNextSegment());
		s1.setNextSegment(s2);
		assertTrue(s1.hasNextSegment());
		
	}

	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#hasPrevSegment()}.
	 */
	@Test
	public void testHasPrevSegment() {
		DefaultProfileSegment s1 = new DefaultProfileSegment(0, 11, 100);
		DefaultProfileSegment s2 = new DefaultProfileSegment(11, 20, 100);
		assertFalse(s2.hasPrevSegment());
		s2.setPrevSegment(s1);
		assertTrue(s2.hasPrevSegment());
	}

	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#setPosition(int)}.
	 */
	@Test
	public void testSetPosition() {
		DefaultProfileSegment s1 = new DefaultProfileSegment(0, 11, 100);
		assertEquals(0, s1.getPosition());
		s1.setPosition(3);
		assertEquals(3, s1.getPosition());
		
		// Out of range
		exception.expect(IllegalArgumentException.class);
		s1.setPosition(-1);
	}

	/**
	 * Test method for {@link com.bmskinner.nma.components.profiles.DefaultProfileSegment#iterator()}.
	 */
	@Test
	public void testIterator() {
		
		// Standard
		DefaultProfileSegment s1 = new DefaultProfileSegment(0, 11, 100);
		
		Iterator<Integer> it = s1.iterator();
		
		int i=0;
		
		int[] exp = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
		
		while(it.hasNext()){
			int index = it.next();
			assertEquals(exp[i], index);
			i++;
		}
		
		// Wrapping
		DefaultProfileSegment s2 = new DefaultProfileSegment(95, 5, 100);
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
	public void testCopy() throws ProfileException {
		
		int[] start = { 0,  10, 30, 88 };
		int[] end   = { 10, 30, 88, 0  };
		
		List<IProfileSegment> list = new ArrayList<>();
		
		for(int i=0; i<start.length; i++){
			list.add(new DefaultProfileSegment(start[i], end[i], 100));
		}

		IProfileSegment.linkSegments(list);

		List<IProfileSegment> result = IProfileSegment.copyAndLink(list);

		for(int i=0; i<start.length; i++){
		    IProfileSegment t = list.get(i);
		    IProfileSegment r = result.get(i);

		    assertEquals(t, r);
		}
	}

	@Test
    public void testOverlaps(){
	    
	    DefaultProfileSegment s1 = new DefaultProfileSegment(0,  20, 100);
	    DefaultProfileSegment s2 = new DefaultProfileSegment(20,  0, 100);
        DefaultProfileSegment s3 = new DefaultProfileSegment(10, 50, 100);
	    
        assertFalse(s1.overlapsBeyondEndpoints(s2));
        assertFalse(s2.overlapsBeyondEndpoints(s1));
        
        assertTrue(s1.overlapsBeyondEndpoints(s3));
        assertTrue(s3.overlapsBeyondEndpoints(s1));
        
        assertTrue(s2.overlapsBeyondEndpoints(s3));
        assertTrue(s3.overlapsBeyondEndpoints(s2));
	}
	
	@Test
	public void testXmlSerializes() throws Exception {
		
		Element e = test.toXmlElement();
		
		XMLOutputter xmlOutput = new XMLOutputter();
		xmlOutput.setFormat(Format.getPrettyFormat());

		DefaultProfileSegment recovered = new DefaultProfileSegment(e);
		
		assertEquals(test, recovered);
	}
	
}
