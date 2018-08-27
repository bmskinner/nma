package com.bmskinner.nuclear_morphology.components.generic;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.jfree.ui.LengthAdjustmentType;
import org.junit.Before;
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
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusBorderSegment;
import com.bmskinner.nuclear_morphology.samples.dummy.DummySegmentedCellularComponent;

/**
 * Test the common methods for segment classes implementing the IBorderSegment interface.
 * @author bms41
 * @since 1.14.0
 *
 */
@RunWith(Parameterized.class)
public class IBorderSegmentTester {
	
	public final static int startIndex = 0;
	public final static int endIndex = 49;
	public final static int segmentLength = 50;
	public final static int profileLength = 330;
	
	protected final static UUID SEG_ID_0 = UUID.fromString("00000000-0000-0000-0000-000000000001");
	
	private IBorderSegment segment;

	
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	@Parameter(0)
	public Class<? extends IBorderSegment> source;

	@Before
    public void setUp() throws Exception {
        this.segment = createInstance(source);
    }

	/**
	 * Create an instance of the class under test, using the default index parameters.
	 * The segment is part of a 3-segment profile
	 * @param source the class to create
	 * @return
	 */
	public static IBorderSegment createInstance(Class source) {
		
		int middleSegmentStart = endIndex;
		int middleSegmentEnd = endIndex+30;
		
		// Make a 3 segment profile, so that updating can be properly tested with segment locking
		UUID tempId          = UUID.fromString("00000000-0000-0000-0000-000000000002");
		UUID middleSegmentId = UUID.fromString("00000000-0000-0000-0000-000000000004");
		UUID finalSegmentId  = UUID.fromString("00000000-0000-0000-0000-000000000005");
		
		// The component from which profiles will be generated
		DummySegmentedCellularComponent comp = new DummySegmentedCellularComponent();
		float[] data = new float[comp.getBorderLength()];
		Arrays.fill(data, 1);
		
		if(source==BorderSegmentTree.class){

			DefaultSegmentedProfile doubleSegmentProfile = comp.new DefaultSegmentedProfile(data);
			
			IBorderSegment borderSegmentTree = null;
			try {
				doubleSegmentProfile.splitSegment(doubleSegmentProfile.getSegment(IProfileCollection.DEFAULT_SEGMENT_ID), middleSegmentEnd, tempId, finalSegmentId);
				doubleSegmentProfile.splitSegment(doubleSegmentProfile.getSegment(tempId), endIndex, SEG_ID_0, middleSegmentId);
				
				borderSegmentTree = doubleSegmentProfile.getSegment(SEG_ID_0);
			} catch (UnavailableComponentException | ProfileException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return borderSegmentTree;
		}
		
		// Older classes use the same approach to linking segments
		List<IBorderSegment> list = new ArrayList<>();
		if(source==DefaultBorderSegment.class) {
			IBorderSegment s0 = new DefaultBorderSegment(startIndex, endIndex, profileLength, SEG_ID_0);
			IBorderSegment s1 = new DefaultBorderSegment(middleSegmentStart, middleSegmentEnd, profileLength, middleSegmentId);
			IBorderSegment s2 = new DefaultBorderSegment(middleSegmentEnd, startIndex, profileLength, finalSegmentId);
			list.add(s0); list.add(s1); list.add(s2); 
		}
		
		if(source==OpenBorderSegment.class) {
			IBorderSegment s0 = new OpenBorderSegment(startIndex, endIndex, profileLength, SEG_ID_0);
			IBorderSegment s1 = new OpenBorderSegment(middleSegmentStart, middleSegmentEnd, profileLength, middleSegmentId);
			IBorderSegment s2 = new OpenBorderSegment(middleSegmentEnd, startIndex, profileLength, finalSegmentId);
			list.add(s0); list.add(s1); list.add(s2); 
		}

		if(source==NucleusBorderSegment.class) {
			IBorderSegment s0 = new NucleusBorderSegment(startIndex, endIndex, profileLength, SEG_ID_0);
			IBorderSegment s1 = new NucleusBorderSegment(middleSegmentStart, middleSegmentEnd, profileLength, middleSegmentId);
			IBorderSegment s2 = new NucleusBorderSegment(middleSegmentEnd, startIndex, profileLength, finalSegmentId);
			list.add(s0); list.add(s1); list.add(s2); 
		}
		
		try {
			IBorderSegment.linkSegments(list);
		} catch (ProfileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list.get(0);
	}
	
	@SuppressWarnings("unchecked")
    @Parameters
    public static Iterable<Class> arguments() {

		// Since the objects created here persist throughout all tests,
		// we're making class references. The actual objects under test
		// are created fresh from the appropriate class.
		return Arrays.asList(
				BorderSegmentTree.class,
				DefaultBorderSegment.class,
				OpenBorderSegment.class);
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
	public void testGetStartIndex() throws SegmentUpdateException {
		assertEquals(startIndex, segment.getStartIndex());
	}

	@Test
	public void testGetEndIndex() throws SegmentUpdateException {
		assertEquals(endIndex, segment.getEndIndex());
	}

	@Test
	public void testGetProportionalIndex() {
		
		for(int i=0; i<=100; i++) {
			double d = i/200d;
			double dist = segmentLength*d;
			double exp  = Math.round(CellularComponent.wrapIndex(startIndex+dist, segment.getProfileLength()));
			assertEquals("Testing "+d+": "+dist,  (int)exp, segment.getProportionalIndex(d));
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

		// Segment starts at 0, ends at 49. Length 50.
		// When length is even, there are two midpoints.
		// Method should return the lower, i.e. 24
		
		int exp = 24;
		
		assertEquals("Midpoint index", exp, segment.getMidpointIndex());
	}

	@Test
	public void testGetShortestDistanceToStart() {
				
		int startIndex = segment.getStartIndex();
		int profileLength = segment.getProfileLength();	
		for(int i=0; i<segment.getProfileLength(); i++) {
			
			int d1 = Math.abs(i-startIndex);
			int d2 = profileLength - d1;
			
			int dist = Math.min(d1, d2);
			assertEquals("Testing "+i, dist, segment.getShortestDistanceToStart(i));
		}
	}
	

	@Test
	public void testGetShortestDistanceToEnd() {
		int endIndex = segment.getEndIndex();
		int profileLength = segment.getProfileLength();	
		
		
		// Profile is 330. Shortest distance is always <165
		// If end is 0, shortest distances are 0, 1, 2 ... 164, 165, 164 ... 1
		
		// Shortest distance between two indexes in a wrapped profile
				
		for(int i=0; i<segment.getProfileLength(); i++) {
			
			int d1 = Math.abs(i-endIndex);
			int d2 = profileLength - d1;
			
			int dist = Math.min(d1, d2);
			assertEquals("Testing "+i, dist, segment.getShortestDistanceToEnd(i));
		}
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
	public void testIteratorOnNonWrappingSegment() {

		int[] segIndexes = IntStream.range(startIndex, endIndex+1).toArray();
		
		Iterator<Integer> it =  segment.iterator();
		
		for(int i=0; i<segIndexes.length; i++) {
			int index = it.next();
			assertEquals("Testing "+i, segIndexes[i], index);
		}
	}
	
	@Test
	public void testIteratorOnWrappingSegment() throws SegmentUpdateException {

		segment.update(profileLength-10, 10);
		
		int[] exp = { 320, 321, 322, 323, 324, 325, 326, 327, 328, 329, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
		
		Iterator<Integer> it =  segment.iterator();
		
		for(int i=0; i<exp.length; i++) {
			System.out.println("Testing "+i);
			int index = it.next();
			
			assertEquals("Testing "+i, exp[i], index);
		}
	}
	
	@Test
	public void testOverlaps() {
		IBorderSegment overlappingEnd = new DefaultBorderSegment(endIndex, endIndex+10, profileLength);
		assertTrue("Testing overlap of "+segment.toString()+" and "+overlappingEnd.toString(), segment.overlaps(overlappingEnd));
		
		IBorderSegment overlappingStart = new DefaultBorderSegment(endIndex+10, startIndex, profileLength);
		assertTrue("Testing overlap of "+segment.toString()+" and "+overlappingStart.toString(), segment.overlaps(overlappingStart));
		
		IBorderSegment overlappingBoth = new DefaultBorderSegment(endIndex, startIndex, profileLength);
		assertTrue("Testing overlap of "+segment.toString()+" and "+overlappingBoth.toString(), segment.overlaps(overlappingBoth));
		
		
		IBorderSegment nonoverlapping = new DefaultBorderSegment(endIndex+10, profileLength-10, profileLength);
		assertFalse("Testing overlap of "+segment.toString()+" and "+nonoverlapping.toString(), segment.overlaps(nonoverlapping));
	}
	
	@Test
	public void overlapsBeyondEndpoints() {
		IBorderSegment endOnly = new DefaultBorderSegment(endIndex, endIndex+10, profileLength);
		assertFalse("Testing overlap of "+segment.toString()+" and "+endOnly.toString(), segment.overlapsBeyondEndpoints(endOnly));
		
		IBorderSegment startOnly = new DefaultBorderSegment(endIndex+10, startIndex, profileLength);
		assertFalse("Testing overlap of "+segment.toString()+" and "+startOnly.toString(), segment.overlapsBeyondEndpoints(startOnly));
		
		IBorderSegment both = new DefaultBorderSegment(endIndex, startIndex, profileLength);
		assertFalse("Testing overlap of "+segment.toString()+" and "+both.toString(), segment.overlapsBeyondEndpoints(both));

		IBorderSegment neither = new DefaultBorderSegment(endIndex+10, profileLength-10, profileLength);
		assertFalse("Testing overlap of "+segment.toString()+" and "+neither.toString(), segment.overlapsBeyondEndpoints(neither));
		
		IBorderSegment endMinusOne = new DefaultBorderSegment(endIndex-1, endIndex+10, profileLength);
		assertTrue("Testing overlap of "+segment.toString()+" and "+endMinusOne.toString(), segment.overlapsBeyondEndpoints(endMinusOne));
		
		IBorderSegment startPlusOne = new DefaultBorderSegment(endIndex+10, startIndex+1, profileLength);
		assertTrue("Testing overlap of "+segment.toString()+" and "+startPlusOne.toString(), segment.overlapsBeyondEndpoints(startPlusOne));
	}
	
	@Test
	public void testHasNextSegment() throws UnavailableComponentException {
		IBorderSegment overlappingEnd = new DefaultBorderSegment(endIndex, profileLength, profileLength);
		segment.setNextSegment(overlappingEnd);
		assertTrue(segment.hasNextSegment());
	}

	@Test
	public void testHasPrevSegment() throws UnavailableComponentException {
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
	public void testUpdateSegmentStartIndex() throws SegmentUpdateException {
		int newStart = startIndex+5;
		assertTrue(segment.update(newStart, endIndex));
		assertEquals(newStart, segment.getStartIndex());
		assertEquals(endIndex, segment.getEndIndex());
	}
	
	@Test
	public void testUpdateSegmentFailsWhenLocked() throws SegmentUpdateException {
		segment.setLocked(true);
		exception.expect(SegmentUpdateException.class);
		segment.update(startIndex, endIndex);
	}
	
	@Test
	public void testUpdateToStartOfSegmentFailsWhenPreviousSegmentIsLocked() throws SegmentUpdateException {
		segment.prevSegment().setLocked(true);
		exception.expect(SegmentUpdateException.class);
		segment.update(startIndex+1, endIndex);
	}
	
	@Test
	public void testUpdateToEndOfSegmentSucceedsWhenPreviousSegmentIsLocked() throws SegmentUpdateException {
		segment.prevSegment().setLocked(true);
		segment.update(startIndex, endIndex-1);
	}
	
	@Test
	public void testUpdateToEndOfSegmentFailsWhenNextSegmentIsLocked() throws SegmentUpdateException {
		segment.nextSegment().setLocked(true);
		exception.expect(SegmentUpdateException.class);
		segment.update(startIndex, endIndex-1);
	}
	
	@Test
	public void testUpdateToStartfSegmentSucceedsWhenNextSegmentIsLocked() throws SegmentUpdateException {
		segment.nextSegment().setLocked(true);
		segment.update(startIndex+1, endIndex);
	}
	
	@Test
	public void testUpdateSegmentFailsWhenTooShort() throws SegmentUpdateException {
		exception.expect(SegmentUpdateException.class);
		// We need to subtract 2 rather than 1 because we are specifying the end index, not the length
		segment.update(startIndex, startIndex+IBorderSegment.MINIMUM_SEGMENT_LENGTH-2);
	}

	@Test
	public void testUpdateLoneSegmentFailsOnStartOutOfBoundsLow() throws SegmentUpdateException {
		exception.expect(SegmentUpdateException.class);
		segment.update(-1, endIndex);
	}
	
	@Test
	public void testUpdateLoneSegmentFailsOnStartOutOfBoundsHigh() throws SegmentUpdateException {
		exception.expect(SegmentUpdateException.class);
		segment.update(profileLength+1, endIndex);
	}
	
	@Test
	public void testUpdateLoneSegmentFailsOnEndOutOfBoundsLow() throws SegmentUpdateException {
		exception.expect(SegmentUpdateException.class);
		segment.update(startIndex, -1);
	}
	
	@Test
	public void testUpdateLoneSegmentFailsOnEndOutOfBoundsHigh() throws SegmentUpdateException {
		exception.expect(SegmentUpdateException.class);
		segment.update(startIndex, profileLength+1);
	}
	
}
