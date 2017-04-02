/**
 * 
 */
package components.generic;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment;

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
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#DefaultBorderSegment(int, int, int)}.
	 */
	@Test
	public void testDefaultBorderSegmentIntIntInt() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#DefaultBorderSegment(com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment)}.
	 */
	@Test
	public void testDefaultBorderSegmentIBorderSegment() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getID()}.
	 */
	@Test
	public void testGetID() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getMergeSources()}.
	 */
	@Test
	public void testGetMergeSources() {
		fail("Not yet implemented");
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
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getEndIndex()}.
	 */
	@Test
	public void testGetEndIndex() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getProportionalIndex(double)}.
	 */
	@Test
	public void testGetProportionalIndex() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getIndexProportion(int)}.
	 */
	@Test
	public void testGetIndexProportion() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getName()}.
	 */
	@Test
	public void testGetName() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getMidpointIndex()}.
	 */
	@Test
	public void testGetMidpointIndex() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getDistanceToStart(int)}.
	 */
	@Test
	public void testGetDistanceToStart() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getDistanceToEnd(int)}.
	 */
	@Test
	public void testGetDistanceToEnd() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#isLocked()}.
	 */
	@Test
	public void testIsLocked() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#setLocked(boolean)}.
	 */
	@Test
	public void testSetLocked() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getTotalLength()}.
	 */
	@Test
	public void testGetTotalLength() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#nextSegment()}.
	 */
	@Test
	public void testNextSegment() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#prevSegment()}.
	 */
	@Test
	public void testPrevSegment() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#shortenStart(int)}.
	 */
	@Test
	public void testShortenStart() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#shortenEnd(int)}.
	 */
	@Test
	public void testShortenEnd() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#lengthenStart(int)}.
	 */
	@Test
	public void testLengthenStart() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#lengthenEnd(int)}.
	 */
	@Test
	public void testLengthenEnd() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#length()}.
	 */
	@Test
	public void testLength() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#equals(java.lang.Object)}.
	 */
	@Test
	public void testEqualsObject() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#testLength(int, int)}.
	 */
	@Test
	public void testTestLength() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#wraps(int, int)}.
	 */
	@Test
	public void testWrapsIntInt() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#wraps()}.
	 */
	@Test
	public void testWraps() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#contains(int)}.
	 */
	@Test
	public void testContains() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#testContains(int, int, int)}.
	 */
	@Test
	public void testTestContains() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#update(int, int)}.
	 */
	@Test
	public void testUpdate() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#setNextSegment(com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment)}.
	 */
	@Test
	public void testSetNextSegment() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#setPrevSegment(com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment)}.
	 */
	@Test
	public void testSetPrevSegment() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#hasNextSegment()}.
	 */
	@Test
	public void testHasNextSegment() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#hasPrevSegment()}.
	 */
	@Test
	public void testHasPrevSegment() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#setPosition(int)}.
	 */
	@Test
	public void testSetPosition() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getPosition()}.
	 */
	@Test
	public void testGetPosition() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#toString()}.
	 */
	@Test
	public void testToString() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#getDetail()}.
	 */
	@Test
	public void testGetDetail() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment#iterator()}.
	 */
	@Test
	public void testIterator() {
		fail("Not yet implemented");
	}

}
