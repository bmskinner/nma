package com.bmskinner.nuclear_morphology.components.generic;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.SegmentedCellularComponent.DefaultProfile;
import com.bmskinner.nuclear_morphology.components.SegmentedCellularComponent.DefaultSegmentedProfile;
import com.bmskinner.nuclear_morphology.components.SegmentedCellularComponent.DefaultSegmentedProfile.BorderSegmentTree;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment.SegmentUpdateException;
import com.bmskinner.nuclear_morphology.samples.dummy.DummySegmentedCellularComponent;

/**
 * Test the common methods for segment classes
 * @author bms41
 * @since 1.14.0
 *
 */
@RunWith(Parameterized.class)
public class IBorderSegmentTester {
	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Parameter(0)
	public IBorderSegment segment;
	
	public static int startIndex = 0;
	public static int endIndex = 49;
	public static int segmentLength = 50;
	public static int profileLength = 330;
	
	protected final static UUID SEG_ID_0 = UUID.fromString("00000000-0000-0000-0000-000000000001");
	
	@SuppressWarnings("unchecked")
    @Parameters
    public static Iterable<IBorderSegment> arguments() {
		
		// Create the border tree segment
		DummySegmentedCellularComponent comp = new DummySegmentedCellularComponent();
		float[] data = new float[comp.getBorderLength()];
		for(int i=0; i<data.length; i++) {
			data[i] = (float) ((Math.sin(Math.toRadians(i))+1)*180);
		}

		DefaultSegmentedProfile doubleSegmentProfile = comp.new DefaultSegmentedProfile(data);
		IBorderSegment borderSegmentTree = doubleSegmentProfile.new BorderSegmentTree(SEG_ID_0, startIndex, segmentLength); 

		// Create the default border segment with the same length
		IBorderSegment defaultBorderSegment = new DefaultBorderSegment(startIndex, endIndex, profileLength, SEG_ID_0);
		
		IBorderSegment openBorderSegment = new OpenBorderSegment(startIndex, endIndex, profileLength, SEG_ID_0);

		return Arrays.asList(
				borderSegmentTree,
				defaultBorderSegment,
				openBorderSegment);
	}
	
	@Test
	public void testGetID() {
		assertEquals(SEG_ID_0, segment.getID());
	}


	@Test
	public void testClearMergeSources() {
		assertFalse(segment.hasMergeSources());
		IBorderSegment mock1 = mock(IBorderSegment.class);
		when(mock1.getID()).thenReturn(UUID.randomUUID());
		when(mock1.getStartIndex()).thenReturn(segment.getMidpointIndex());
		when(mock1.length()).thenReturn(segment.length()-segment.getMidpointIndex());
		when(mock1.getProfileLength()).thenReturn(segment.getProfileLength());
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
		when(mock1.length()).thenReturn(segment.getProfileLength()-segment.getMidpointIndex());
		when(mock1.getProfileLength()).thenReturn(segment.getProfileLength());
		segment.addMergeSource(mock1);
		assertTrue(segment.hasMergeSources());
		segment.clearMergeSources();
	}
	
	@Test
	public void testAddMergeSource() {
		// proper merge source
		DefaultBorderSegment s1 = new DefaultBorderSegment(startIndex, startIndex+20, profileLength);
		segment.addMergeSource(s1);
		
		IBorderSegment obs = segment.getMergeSources().get(0);
		assertEquals("Start", s1.getStartIndex(), obs.getStartIndex());
		assertEquals("End", s1.getEndIndex(), obs.getEndIndex());
		assertEquals("Id", s1.getID(), obs.getID());
	}
	
	@Test
	public void testAddMergeSourceExceptsOnOutOfRangeArg0() {
		// invalid merge source - out of range
		DefaultBorderSegment s2 = new DefaultBorderSegment(endIndex, endIndex+20, profileLength);

		exception.expect(IllegalArgumentException.class);
		segment.addMergeSource(s2);
	}
	
	@Test
	public void testAddMergeSourceExceptsOnOutOfRangeArg1() {	
		// invalid merge source - out of range
		exception.expect(IllegalArgumentException.class);
		new DefaultBorderSegment(-1, startIndex, profileLength);
	}
	
	@Test
	public void testAddMergeSourceExceptsOnWrongProfileLength() {
		// invalid merge source - wrong length
		DefaultBorderSegment s = new DefaultBorderSegment(startIndex+10, endIndex-10, profileLength-50);
		exception.expect(IllegalArgumentException.class);
		segment.addMergeSource(s);
	}
	
	@Test
	public void testAddMergeSourceExceptsOnNull() {
		exception.expect(IllegalArgumentException.class);
		segment.addMergeSource(null);
	}

	@Test
	public void testGetStartIndex() {
		assertEquals(startIndex, segment.getStartIndex());
	}

	@Test
	public void testGetEndIndex() {
		assertEquals(endIndex, segment.getEndIndex());
	}

	@Test
	public void testGetProportionalIndex() {
		for(double d=0; d<=1; d+=0.01) {
			int exp =  (int)Math.round(((double)segmentLength*d)+startIndex);
			assertEquals("Testing "+d,exp, segment.getProportionalIndex(d));
		}
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
		
		for(int i=0; i<segment.length(); i++) {
			double prop = (double)i/ (double)segment.length();
			assertEquals("Testing "+i, prop, segment.getIndexProportion(i),0);
		}
	}

	@Test
	public void testGetName() {
		assertEquals("Seg_0", segment.getName());
	}

	@Test
	public void testGetMidpointIndex() {
		assertEquals("Midpoint index",endIndex/2, segment.getMidpointIndex());
	}

	@Test
	public void testGetDistanceToStart() {
		
		for(int i=0; i<segment.getMidpointIndex(); i++) {
			assertEquals(i, segment.getShortestDistanceToStart(i));
		}
		for(int i=segment.getMidpointIndex(); i<=segment.getProfileLength(); i++) {
			assertEquals(segment.getProfileLength()-i, segment.getShortestDistanceToStart(i));
		}
	}
	

	@Test
	public void testGetShortestDistanceToEnd() {
		for(int i=0; i<segment.getProfileLength(); i++) {
			int dist = Math.abs(i-segment.getEndIndex());
			assertEquals("Testing "+i, dist, segment.getShortestDistanceToEnd(i));
		}
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
	public void testGetProfileLength() {
		assertEquals(profileLength, segment.getProfileLength());
	}
	
	@Test
	public void testLength() {
		assertEquals(segmentLength, segment.length() );
	}
	
	
	@Test
	public void testWraps() {
		assertFalse(segment.wraps());
	}

	@Test
	public void testContains() {

		for(int i=startIndex; i<=endIndex; i++) {
			assertTrue(segment.contains(i));
		}
		
		for(int i=endIndex+1; i<segment.getProfileLength(); i++) {
			assertFalse(segment.contains(i));
		}
	}
	
	@Test
	public void testIterator() {

		int[] segIndexes = IntStream.range(startIndex, endIndex+1).toArray();
		
		Iterator<Integer> it =  segment.iterator();
		
		for(int i=0; i<segIndexes.length; i++) {
			int index = it.next();
			assertEquals("Testing "+i, segIndexes[i], index);
		}

	}
	
	@Test
	public void testOverlaps() {
		IBorderSegment overlappingEnd = new DefaultBorderSegment(endIndex, endIndex+10, profileLength);
		assertTrue(overlappingEnd.overlaps(segment));
		
		IBorderSegment overlappingStart = new DefaultBorderSegment(endIndex+10, startIndex, profileLength);
		assertTrue(overlappingEnd.overlaps(segment));
		
		IBorderSegment overlappingBoth = new DefaultBorderSegment(endIndex, startIndex, profileLength);
		assertTrue(overlappingEnd.overlaps(segment));
		
		
		IBorderSegment nonoverlapping = new DefaultBorderSegment(endIndex+10, profileLength-10, profileLength);
		assertFalse(nonoverlapping.overlaps(segment));
	}
	
	@Test
	public void testHasNextSegment() throws UnavailableComponentException {
		assertFalse(segment.hasNextSegment());
		IBorderSegment overlappingEnd = new DefaultBorderSegment(endIndex, profileLength, profileLength);
		segment.setNextSegment(overlappingEnd);
		assertTrue(segment.hasNextSegment());
	}

	@Test
	public void testHasPrevSegment() throws UnavailableComponentException {
		assertFalse(segment.hasPrevSegment());
		IBorderSegment overlappingStart = new DefaultBorderSegment(endIndex+1, startIndex, profileLength);
		segment.setPrevSegment(overlappingStart);
		assertTrue(segment.hasPrevSegment());
	}
	
	@Test
	public void testUpdateSegmentEndIndex() throws SegmentUpdateException {

		assertTrue(segment.update(startIndex, endIndex+5));
		assertEquals(startIndex, segment.getStartIndex());
		assertEquals(endIndex+5, segment.getEndIndex());
	}
	
	@Test
	public void testUpdateSegmentStartndex() throws SegmentUpdateException {

		assertTrue(segment.update(startIndex+5, endIndex));
		assertEquals(startIndex+5, segment.getStartIndex());
		assertEquals(endIndex, segment.getEndIndex());
	}
	
	@Test
	public void testUpdateLoneSegmentFailsWhenLocked() throws SegmentUpdateException {
		segment.setLocked(true);
		assertFalse(segment.update(10, 20));
	}
	
	@Test
	public void testUpdateLoneSegmentFailsWhenTooShort() throws SegmentUpdateException {
		assertFalse(segment.update(startIndex, startIndex+2));
	}

	@Test
	public void testUpdateLoneSegmentFailsOnStartOutOfBoundsLow() throws SegmentUpdateException {
		exception.expect(IllegalArgumentException.class);
		segment.update(-1, 20);
	}
	
	@Test
	public void testUpdateLoneSegmentFailsOnStartOutOfBoundsHigh() throws SegmentUpdateException {
		exception.expect(IllegalArgumentException.class);
		segment.update(101, 20);
	}
	
	@Test
	public void testUpdateLoneSegmentFailsOnEndOutOfBoundsLow() throws SegmentUpdateException {
		exception.expect(IllegalArgumentException.class);
		segment.update(0, -1);
	}
	
	@Test
	public void testUpdateLoneSegmentFailsOnEndOutOfBoundsHigh() throws SegmentUpdateException {
		exception.expect(IllegalArgumentException.class);
		segment.update(0, profileLength+1);
	}
	
}
