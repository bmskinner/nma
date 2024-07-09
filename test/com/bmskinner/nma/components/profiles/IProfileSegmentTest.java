package com.bmskinner.nma.components.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.samples.dummy.DummySegmentedCellularComponent;

/**
 * Test the common methods for segment classes implementing the IBorderSegment
 * interface.
 * 
 * @author bms41
 * @since 1.14.0
 *
 */
@RunWith(Parameterized.class)
public class IProfileSegmentTest {

	public final static int START_INDEX = 0;
	public final static int END_INDEX = 49;
	public final static int SEG_LENGTH = 50;
	public final static int PROFILE_LENGTH = 330;

	protected final static UUID SEG_ID_0 = UUID.fromString("00000000-0000-0000-0000-000000000001");

	private static final UUID middleSegmentId = UUID
			.fromString("00000000-0000-0000-0000-000000000004");
	private static final UUID finalSegmentId = UUID
			.fromString("00000000-0000-0000-0000-000000000005");

	private IProfileSegment segment;

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Parameter(0)
	public Class<? extends IProfileSegment> source;

	@Before
	public void setUp() throws Exception {
		this.segment = createInstance(source);
	}

	/**
	 * Create an instance of the class under test, using the default index
	 * parameters. The segment is part of a 3-segment profile
	 * 
	 * @param source the class to create
	 * @return
	 * @throws ProfileException
	 * @throws ComponentCreationException
	 */
	public static IProfileSegment createInstance(Class<?> source)
			throws SegmentUpdateException, ComponentCreationException {

		// The component from which profiles will be generated
		DummySegmentedCellularComponent comp = new DummySegmentedCellularComponent();
		float[] data = new float[comp.getBorderLength()];
		Arrays.fill(data, 1);

		// Older classes use the same approach to linking segments
		List<IProfileSegment> list = createInstanceSegmentList(source);

		IProfileSegment.linkSegments(list);

		return list.get(0);
	}

	private static List<IProfileSegment> createInstanceSegmentList(Class<?> source) {

		int middleSegmentStart = END_INDEX;
		int middleSegmentEnd = END_INDEX + 30;

		// Make a 3 segment profile, so that updating can be properly tested with
		// segment locking
		List<IProfileSegment> list = new ArrayList<>();
		if (source == DefaultProfileSegment.class) {
			IProfileSegment s0 = new DefaultProfileSegment(START_INDEX, END_INDEX, PROFILE_LENGTH,
					SEG_ID_0);
			IProfileSegment s1 = new DefaultProfileSegment(middleSegmentStart, middleSegmentEnd,
					PROFILE_LENGTH,
					middleSegmentId);
			IProfileSegment s2 = new DefaultProfileSegment(middleSegmentEnd, START_INDEX,
					PROFILE_LENGTH,
					finalSegmentId);
			list.add(s0);
			list.add(s1);
			list.add(s2);
		}

//		if(source==OpenBorderSegment.class) {
//			IProfileSegment s0 = new OpenBorderSegment(START_INDEX, END_INDEX, PROFILE_LENGTH, SEG_ID_0);
//			IProfileSegment s1 = new OpenBorderSegment(middleSegmentStart, middleSegmentEnd, PROFILE_LENGTH, middleSegmentId);
//			IProfileSegment s2 = new OpenBorderSegment(middleSegmentEnd, START_INDEX, PROFILE_LENGTH, finalSegmentId);
//			list.add(s0); list.add(s1); list.add(s2); 
//		}
		return list;
	}

	@Parameters
	public static Iterable<Class<?>> arguments() {

		// Since the objects created here persist throughout all tests,
		// we're making class references. The actual objects under test
		// are created fresh from the appropriate class.
		return Arrays.asList(DefaultProfileSegment.class);
	}

	@Test
	public void testGetID() {
		assertEquals(SEG_ID_0, segment.getID());
	}

	@Test
	public void testClearMergeSources() {
		assertFalse(segment.hasMergeSources());
		IProfileSegment mock1 = mock(IProfileSegment.class);
		when(mock1.getID()).thenReturn(UUID.randomUUID());
		when(mock1.getStartIndex()).thenReturn(segment.getMidpointIndex());
		when(mock1.length()).thenReturn(segment.length() / 2);
		when(mock1.getProfileLength()).thenReturn(segment.getProfileLength());
		when(mock1.toString()).thenReturn(segment.toString());
		segment.addMergeSource(mock1);
		assertTrue(segment.hasMergeSources());
		segment.clearMergeSources();
		assertFalse(segment.hasMergeSources());
	}

	@Test
	public void testHasMergeSources() {
		assertFalse(segment.hasMergeSources());

		IProfileSegment mock1 = mock(IProfileSegment.class);
		when(mock1.getID()).thenReturn(UUID.randomUUID());
		when(mock1.getStartIndex()).thenReturn(segment.getMidpointIndex());
		when(mock1.length()).thenReturn(segment.length() / 2);
		when(mock1.getProfileLength()).thenReturn(segment.getProfileLength());
		when(mock1.toString()).thenReturn(segment.toString());
		segment.addMergeSource(mock1);
		assertTrue(segment.hasMergeSources());
		segment.clearMergeSources();
	}

	@Test
	public void testAddMergeSource() {
		// proper merge source
		DefaultProfileSegment s1 = new DefaultProfileSegment(START_INDEX, START_INDEX + 20,
				PROFILE_LENGTH);
		segment.addMergeSource(s1);

		IProfileSegment obs = segment.getMergeSources().get(0);
		assertEquals("Start", s1.getStartIndex(), obs.getStartIndex());
		assertEquals("End", s1.getEndIndex(), obs.getEndIndex());
		assertEquals("Id", s1.getID(), obs.getID());
	}

	@Test
	public void testAddMergeSourceExceptsOnOutOfRangeArg0() {
		// invalid merge source - out of range
		DefaultProfileSegment s2 = new DefaultProfileSegment(END_INDEX, END_INDEX + 20,
				PROFILE_LENGTH);
		exception.expect(IllegalArgumentException.class);
		segment.addMergeSource(s2);
	}

	@Test
	public void testAddMergeSourceExceptsOnOutOfRangeArg1() {
		// invalid merge source - out of range
		exception.expect(IllegalArgumentException.class);
		new DefaultProfileSegment(-1, START_INDEX, PROFILE_LENGTH);
	}

	@Test
	public void testAddMergeSourceExceptsOnWrongProfileLength() {
		// invalid merge source - wrong length
		DefaultProfileSegment s = new DefaultProfileSegment(START_INDEX + 10, END_INDEX - 10,
				PROFILE_LENGTH - 50);
		exception.expect(IllegalArgumentException.class);
		segment.addMergeSource(s);
	}

	@Test
	public void testAddMergeSourceExceptsWhenSegmentIdAlreadyExists() {
		IProfileSegment s = new DefaultProfileSegment(5, 10, PROFILE_LENGTH, SEG_ID_0);
		exception.expect(IllegalArgumentException.class);
		segment.addMergeSource(s);
	}

	@Test
	public void testMergeSourcesPreservedWhenSegmentIsDuplicated() {
		DefaultProfileSegment s1 = new DefaultProfileSegment(START_INDEX, START_INDEX + 20,
				PROFILE_LENGTH);
		DefaultProfileSegment s2 = new DefaultProfileSegment(START_INDEX + 20, END_INDEX,
				PROFILE_LENGTH);

		segment.addMergeSource(s1);
		segment.addMergeSource(s2);

		IProfileSegment duplicated = segment.duplicate();
		assertTrue(duplicated.hasMergeSources());
		for (IProfileSegment mge : segment.getMergeSources()) {
			assertTrue(duplicated.hasMergeSource(mge.getID()));
		}

		assertEquals(segment, duplicated);
	}

	@Test
	public void testGetStartIndex() throws SegmentUpdateException {
		assertEquals(START_INDEX, segment.getStartIndex());
	}

	@Test
	public void testGetEndIndex() throws SegmentUpdateException {
		assertEquals(END_INDEX, segment.getEndIndex());
	}

	@Test
	public void testGetEndIndexOnWrappingSegment() throws SegmentUpdateException {
		segment.update(PROFILE_LENGTH - 10, 10);
		assertEquals(PROFILE_LENGTH - 10, segment.getStartIndex());
		assertEquals(10, segment.getEndIndex());
	}

	@Test
	public void testGetEndIndexOnSingleSegmentProfile() throws SegmentUpdateException {
		segment.update(0, 0);
		assertEquals(0, segment.getStartIndex());
		assertEquals(0, segment.getEndIndex());
	}

	@Test
	public void testGetEndIndexOnSingleSegmentProfileWrapping() throws SegmentUpdateException {
		segment.update(1, 1);
		assertEquals(1, segment.getStartIndex());
		assertEquals(1, segment.getEndIndex());
	}

	@Test
	public void testGetProportionalIndex() {

		for (int i = 0; i <= 100; i++) {
			double d = i / 200d;
			double dist = SEG_LENGTH * d;
			double exp = Math.round(
					CellularComponent.wrapIndex(START_INDEX + dist, segment.getProfileLength()));
			assertEquals("Testing " + d + ": " + dist, (int) exp, segment.getProportionalIndex(d));
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
	public void testOffset() throws SegmentUpdateException, ComponentCreationException {

		for (int i = -PROFILE_LENGTH; i < PROFILE_LENGTH; i++) {
			segment = createInstance(source);
			IProfileSegment s2 = segment.offset(i);
			assertEquals("Offsetting by " + i, CellularComponent.wrapIndex(i, PROFILE_LENGTH),
					s2.getStartIndex());
		}
	}

	@Test
	public void testOffsetPreservesSegmentLock()
			throws SegmentUpdateException, ComponentCreationException {
		IProfileSegment seg = createInstance(source);
		seg.setLocked(true);
		assertTrue(seg.isLocked());

		IProfileSegment s2 = seg.offset(10);
		assertTrue(s2.isLocked());
	}

	@Test
	public void testOffsetByZeroHasNoEffect() {
		int exp = segment.getStartIndex();
		IProfileSegment s2 = segment.offset(0);
		assertEquals(exp, s2.getStartIndex());
	}

	@Test
	public void testGetIndexProportion() {

		for (int i = 0; i < segment.length(); i++) {
			double prop = (double) i / (double) segment.length();
			assertEquals("Testing " + i, prop, segment.getIndexProportion(i), 0);
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
		for (int i = 0; i < segment.getProfileLength(); i++) {

			int d1 = Math.abs(i - startIndex);
			int d2 = profileLength - d1;

			int dist = Math.min(d1, d2);
			assertEquals("Testing " + i, dist, segment.getShortestDistanceToStart(i));
		}
	}

	@Test
	public void testGetShortestDistanceToEnd() {
		int endIndex = segment.getEndIndex();
		int profileLength = segment.getProfileLength();

		// Profile is 330. Shortest distance is always <165
		// If end is 0, shortest distances are 0, 1, 2 ... 164, 165, 164 ... 1

		// Shortest distance between two indexes in a wrapped profile

		for (int i = 0; i < segment.getProfileLength(); i++) {

			int d1 = Math.abs(i - endIndex);
			int d2 = profileLength - d1;

			int dist = Math.min(d1, d2);
			assertEquals("Testing " + i, dist, segment.getShortestDistanceToEnd(i));
		}
	}

	/**
	 * Test segment can be locked
	 */
	@Test
	public void testSetLockedTrue() {
		assertFalse(segment.isLocked());
		segment.setLocked(true);
		assertTrue(segment.isLocked());
	}

	/**
	 * Test segment can be unlocked
	 */
	@Test
	public void testSetLockedFalse() {
		assertFalse(segment.isLocked());
		segment.setLocked(true);
		assertTrue(segment.isLocked());
		segment.setLocked(false);
		assertFalse(segment.isLocked());
	}

	/**
	 * Test that when a segment is duplicated, the lock state is copied
	 */
	@Test
	public void testLockStateIsCopied() {
		// Default state
		IProfileSegment s2 = segment.duplicate();
		assertEquals("Segment lock state should be copied", segment.isLocked(), s2.isLocked());

		// Explicitly lock
		segment.setLocked(true);
		s2 = segment.duplicate();
		assertEquals("Segment lock state should be copied", segment.isLocked(), s2.isLocked());

		// Explicitly unlock
		segment.setLocked(false);
		s2 = segment.duplicate();
		assertEquals("Segment lock state should be copied", segment.isLocked(), s2.isLocked());
	}

	@Test
	public void testSingleSegmentListCanBeLinked() throws SegmentUpdateException {
		List<IProfileSegment> segs = new ArrayList<>();
		segs.add(new DefaultProfileSegment(START_INDEX, START_INDEX, PROFILE_LENGTH, SEG_ID_0));
		assertFalse(segs.get(0).hasNextSegment());
		assertFalse(segs.get(0).hasPrevSegment());

		IProfileSegment.linkSegments(segs);
		assertTrue(segs.get(0).hasNextSegment());
		assertTrue(segs.get(0).hasPrevSegment());
	}

	@Test
	public void testMultiSegmentListCanBeLinked() throws SegmentUpdateException {
		List<IProfileSegment> segs = createInstanceSegmentList(source);

		// After first creation, segments should not be linked
		IProfileSegment firstSeg = segs.stream().filter(s -> s.getID().equals(SEG_ID_0)).findFirst()
				.get();
		IProfileSegment finalSeg = segs.stream().filter(s -> s.getID().equals(finalSegmentId))
				.findFirst().get();
		assertFalse(firstSeg.hasNextSegment());
		assertFalse(firstSeg.hasPrevSegment());
		assertFalse(finalSeg.hasNextSegment());
		assertFalse(finalSeg.hasPrevSegment());

		// Link and test again
		IProfileSegment.linkSegments(segs);
		firstSeg = segs.stream().filter(s -> s.getID().equals(SEG_ID_0)).findFirst().get();
		finalSeg = segs.stream().filter(s -> s.getID().equals(finalSegmentId)).findFirst().get();
		assertTrue(firstSeg.hasNextSegment());
		assertTrue(firstSeg.hasPrevSegment());
		assertTrue(finalSeg.hasNextSegment());
		assertTrue(finalSeg.hasPrevSegment());
	}

	/**
	 * Test that linking a single segment to itself does not affect lock state
	 * 
	 * @throws ProfileException
	 */
	@Test
	public void testSingleSegmentListLockStatePersistsThroughLinking()
			throws SegmentUpdateException {
		List<IProfileSegment> segs = new ArrayList<>();
		segs.add(new DefaultProfileSegment(START_INDEX, START_INDEX, PROFILE_LENGTH, SEG_ID_0));
		assertFalse(segs.get(0).isLocked());
		segs.get(0).setLocked(true);
		assertTrue(segs.get(0).isLocked());

		IProfileSegment.linkSegments(segs);
		assertTrue(segs.get(0).isLocked());
	}

	/**
	 * Test that linking segments does not affect lock state
	 * 
	 * @throws ProfileException
	 */
	@Test
	public void testMultiSegmentListLockStatePersistsThroughLinking()
			throws SegmentUpdateException {
		List<IProfileSegment> segs = createInstanceSegmentList(source);
		for (IProfileSegment s : segs) {
			s.setLocked(true);
			assertTrue(s.isLocked());
		}

		IProfileSegment.linkSegments(segs);

		for (IProfileSegment s : segs) {
			assertTrue(s.isLocked());
		}
	}

	@Test
	public void testLinkingPreservesMergeSources() throws SegmentUpdateException {
		List<IProfileSegment> segs = createInstanceSegmentList(source);

		IProfileSegment merged = new DefaultProfileSegment(segs.get(0).getStartIndex(),
				segs.get(1).getEndIndex(),
				segs.get(0).getProfileLength(), UUID.randomUUID());

		merged.addMergeSource(segs.get(0));
		merged.addMergeSource(segs.get(1));

		List<IProfileSegment> newSegs = new ArrayList<>();
		newSegs.add(merged);
		newSegs.add(segs.get(2).duplicate());

		assertTrue(segment.getClass().getSimpleName(),
				newSegs.get(0).hasMergeSource(segs.get(0).getID()));
		assertTrue(segment.getClass().getSimpleName(),
				newSegs.get(0).hasMergeSource(segs.get(1).getID()));

		newSegs = IProfileSegment.linkSegments(newSegs);

		assertTrue(segment.getClass().getSimpleName(),
				newSegs.get(0).hasMergeSource(segs.get(0).getID()));
		assertTrue(segment.getClass().getSimpleName(),
				newSegs.get(0).hasMergeSource(segs.get(1).getID()));
	}

	@Test
	public void testGetProfileLength() {
		assertEquals(PROFILE_LENGTH, segment.getProfileLength());
	}

	@Test
	public void testLength() {
		assertEquals(SEG_LENGTH, segment.length());
	}

	@Test
	public void testWraps() {
		assertFalse(segment.wraps());
	}

	@Test
	public void testContains() {

		for (int i = START_INDEX; i <= END_INDEX; i++) {
			assertTrue(segment.contains(i));
		}

		for (int i = END_INDEX + 1; i < segment.getProfileLength(); i++) {
			assertFalse(segment.contains(i));
		}
	}

	@Test
	public void testIteratorOnNonWrappingSegment() throws SegmentUpdateException {

		segment.update(10, 20);
		int[] exp = { 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 };

		Iterator<Integer> it = segment.iterator();

		for (int i = 0; i < exp.length; i++) {
			int index = it.next();
			assertEquals("Testing " + i + ": index " + index, exp[i], index);
		}
		assertEquals("Segment length equals iterator size", segment.length(), exp.length);
		assertFalse("Iterator has more entries", it.hasNext());
	}

	@Test
	public void testIteratorOnWrappingSegment() throws SegmentUpdateException {

		segment.update(PROFILE_LENGTH - 10, 10);

		int[] exp = { 320, 321, 322, 323, 324, 325, 326, 327, 328, 329, 0, 1, 2, 3, 4, 5, 6, 7, 8,
				9, 10 };

		Iterator<Integer> it = segment.iterator();
		for (int i = 0; i < exp.length; i++) {
			int index = it.next();
			assertEquals("Testing " + i + ": index " + index, exp[i], index);
		}
		assertEquals("Segment length equals iterator size", segment.length(), exp.length);
		assertFalse("Iterator has more entries", it.hasNext());
	}

	@Test
	public void testIteratorOnSegmentStartingAtZero() throws SegmentUpdateException {

		segment.update(0, 10);

		int[] exp = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

		Iterator<Integer> it = segment.iterator();
		for (int i = 0; i < exp.length; i++) {
			int index = it.next();
			assertEquals("Testing " + i + ": index " + index, exp[i], index);
		}
		assertEquals("Segment length equals iterator size", segment.length(), exp.length);
		assertFalse("Iterator has more entries", it.hasNext());
	}

	@Test
	public void testOverlapsOfNonWrappingSegments() {
		IProfileSegment overlappingEnd = new DefaultProfileSegment(END_INDEX, END_INDEX + 10,
				PROFILE_LENGTH);
		assertTrue("Testing overlap of " + segment.toString() + " and " + overlappingEnd.toString(),
				segment.overlaps(overlappingEnd));

		IProfileSegment overlappingStart = new DefaultProfileSegment(END_INDEX + 10, START_INDEX,
				PROFILE_LENGTH);
		assertTrue(
				"Testing overlap of " + segment.toString() + " and " + overlappingStart.toString(),
				segment.overlaps(overlappingStart));

		IProfileSegment overlappingBoth = new DefaultProfileSegment(END_INDEX, START_INDEX,
				PROFILE_LENGTH);
		assertTrue(
				"Testing overlap of " + segment.toString() + " and " + overlappingBoth.toString(),
				segment.overlaps(overlappingBoth));

		IProfileSegment nonoverlapping = new DefaultProfileSegment(END_INDEX + 10,
				PROFILE_LENGTH - 10, PROFILE_LENGTH);
		assertFalse(
				"Testing overlap of " + segment.toString() + " and " + nonoverlapping.toString(),
				segment.overlaps(nonoverlapping));

		IProfileSegment endMinusOne = new DefaultProfileSegment(END_INDEX - 1, END_INDEX + 10,
				PROFILE_LENGTH);
		assertTrue("Testing overlap of " + segment.toString() + " and " + endMinusOne.toString(),
				segment.overlaps(endMinusOne));

		IProfileSegment startPlusOne = new DefaultProfileSegment(END_INDEX + 10, START_INDEX + 1,
				PROFILE_LENGTH);
		assertTrue("Testing overlap of " + segment.toString() + " and " + startPlusOne.toString(),
				segment.overlaps(startPlusOne));
	}

	@Test
	public void testOverlapsOfWrappingSegments() throws SegmentUpdateException {

		segment.update(PROFILE_LENGTH - 10, 10);

		IProfileSegment endOnly = new DefaultProfileSegment(segment.getEndIndex(), 30,
				PROFILE_LENGTH);
		assertTrue(
				String.format("Testing overlap of %s and %s", segment.toString(),
						endOnly.toString()),
				segment.overlaps(endOnly));

		IProfileSegment startOnly = new DefaultProfileSegment(PROFILE_LENGTH - 20,
				segment.getStartIndex(),
				PROFILE_LENGTH);
		assertTrue(
				String.format("Testing overlap of %s and %s", segment.toString(),
						startOnly.toString()),
				segment.overlaps(startOnly));

		IProfileSegment bothRev = new DefaultProfileSegment(segment.getEndIndex(),
				segment.getStartIndex(),
				PROFILE_LENGTH);
		assertTrue(
				String.format("Testing overlap of %s and %s", segment.toString(),
						bothRev.toString()),
				segment.overlaps(bothRev));

		IProfileSegment bothFwd = new DefaultProfileSegment(segment.getStartIndex(),
				segment.getEndIndex(),
				PROFILE_LENGTH);
		assertTrue(
				String.format("Testing overlap of %s and %s", segment.toString(),
						bothFwd.toString()),
				segment.overlaps(bothFwd));

		IProfileSegment neither = new DefaultProfileSegment(segment.getEndIndex() + 10,
				segment.getStartIndex() - 10,
				PROFILE_LENGTH);
		assertFalse(
				String.format("Testing overlap of %s and %s", segment.toString(),
						neither.toString()),
				segment.overlaps(neither));

		IProfileSegment endMinusOne = new DefaultProfileSegment(segment.getEndIndex() - 1,
				segment.getEndIndex() + 10,
				PROFILE_LENGTH);
		assertTrue(
				String.format("Testing overlap of %s and %s", segment.toString(),
						endMinusOne.toString()),
				segment.overlaps(endMinusOne));

		IProfileSegment startPlusOne = new DefaultProfileSegment(segment.getEndIndex() + 10,
				segment.getStartIndex() + 1, PROFILE_LENGTH);
		assertTrue(
				String.format("Testing overlap of %s and %s", segment.toString(),
						startPlusOne.toString()),
				segment.overlaps(startPlusOne));
	}

	@Test
	public void overlapsBeyondEndpointsOfNonWrappingSegments() {
		IProfileSegment endOnly = new DefaultProfileSegment(END_INDEX, END_INDEX + 10,
				PROFILE_LENGTH);
		assertFalse("Expected no overlap of " + segment.toString() + " and " + endOnly.toString(),
				segment.overlapsBeyondEndpoints(endOnly));

		IProfileSegment startOnly = new DefaultProfileSegment(END_INDEX + 10, START_INDEX,
				PROFILE_LENGTH);
		assertFalse("Expected no overlap of " + segment.toString() + " and " + startOnly.toString(),
				segment.overlapsBeyondEndpoints(startOnly));

		IProfileSegment both = new DefaultProfileSegment(END_INDEX, START_INDEX, PROFILE_LENGTH);
		assertFalse("Expected no overlap of  " + segment.toString() + " and " + both.toString(),
				segment.overlapsBeyondEndpoints(both));

		IProfileSegment neither = new DefaultProfileSegment(END_INDEX + 10, PROFILE_LENGTH - 10,
				PROFILE_LENGTH);
		assertFalse("Expected no overlap of " + segment.toString() + " and " + neither.toString(),
				segment.overlapsBeyondEndpoints(neither));

		IProfileSegment endMinusOne = new DefaultProfileSegment(END_INDEX - 1, END_INDEX + 10,
				PROFILE_LENGTH);
		assertTrue("Expected overlap of  " + segment.toString() + " and " + endMinusOne.toString(),
				segment.overlapsBeyondEndpoints(endMinusOne));

		IProfileSegment startPlusOne = new DefaultProfileSegment(END_INDEX + 10, START_INDEX + 1,
				PROFILE_LENGTH);
		assertTrue("Expected overlap of  " + segment.toString() + " and " + startPlusOne.toString(),
				segment.overlapsBeyondEndpoints(startPlusOne));
	}

	@Test
	public void overlapsBeyondEndpointsOfWrappingSegments() throws SegmentUpdateException {

		segment.update(PROFILE_LENGTH - 10, 10);

		IProfileSegment endOnly = new DefaultProfileSegment(segment.getEndIndex(), 30,
				PROFILE_LENGTH);
		assertFalse(
				String.format("Testing overlap of %s and %s", segment.toString(),
						endOnly.toString()),
				segment.overlapsBeyondEndpoints(endOnly));

		IProfileSegment startOnly = new DefaultProfileSegment(PROFILE_LENGTH - 20,
				segment.getStartIndex(),
				PROFILE_LENGTH);
		assertFalse(
				String.format("Testing overlap of %s and %s", segment.toString(),
						startOnly.toString()),
				segment.overlapsBeyondEndpoints(startOnly));

		IProfileSegment bothRev = new DefaultProfileSegment(segment.getEndIndex(),
				segment.getStartIndex(),
				PROFILE_LENGTH);
		assertFalse(
				String.format("Testing overlap of %s and %s", segment.toString(),
						bothRev.toString()),
				segment.overlapsBeyondEndpoints(bothRev));

		IProfileSegment bothFwd = new DefaultProfileSegment(segment.getStartIndex(),
				segment.getEndIndex(),
				PROFILE_LENGTH);
		assertTrue(
				String.format("Testing overlap of %s and %s", segment.toString(),
						bothFwd.toString()),
				segment.overlapsBeyondEndpoints(bothFwd));

		IProfileSegment neither = new DefaultProfileSegment(segment.getEndIndex() + 10,
				segment.getStartIndex() - 10,
				PROFILE_LENGTH);
		assertFalse(
				String.format("Testing overlap of %s and %s", segment.toString(),
						neither.toString()),
				segment.overlapsBeyondEndpoints(neither));

		IProfileSegment endMinusOne = new DefaultProfileSegment(segment.getEndIndex() - 1,
				segment.getEndIndex() + 10,
				PROFILE_LENGTH);
		assertTrue(
				String.format("Testing overlap of %s and %s", segment.toString(),
						endMinusOne.toString()),
				segment.overlapsBeyondEndpoints(endMinusOne));

		IProfileSegment startPlusOne = new DefaultProfileSegment(segment.getEndIndex() + 10,
				segment.getStartIndex() + 1, PROFILE_LENGTH);
		assertTrue(
				String.format("Testing overlap of %s and %s", segment.toString(),
						startPlusOne.toString()),
				segment.overlapsBeyondEndpoints(startPlusOne));
	}

	@Test
	public void testHasNextSegment() throws MissingDataException {
		IProfileSegment overlappingEnd = new DefaultProfileSegment(END_INDEX, PROFILE_LENGTH,
				PROFILE_LENGTH);
		segment.setNextSegment(overlappingEnd);
		assertTrue(segment.hasNextSegment());
	}

	@Test
	public void testHasPrevSegment() throws MissingDataException {
		IProfileSegment overlappingStart = new DefaultProfileSegment(END_INDEX + 1, START_INDEX,
				PROFILE_LENGTH);
		segment.setPrevSegment(overlappingStart);
		assertTrue(segment.hasPrevSegment());
	}

	@Test
	public void testUpdateSegmentEndIndex() throws SegmentUpdateException {
		assertTrue(segment.update(START_INDEX, END_INDEX + 5));
		assertEquals(START_INDEX, segment.getStartIndex());
		assertEquals(END_INDEX + 5, segment.getEndIndex());
	}

	@Test
	public void testUpdateSegmentStartIndex() throws SegmentUpdateException {
		int newStart = START_INDEX + 5;
		assertTrue(segment.update(newStart, END_INDEX));
		assertEquals(newStart, segment.getStartIndex());
		assertEquals(END_INDEX, segment.getEndIndex());
	}

	@Test
	public void testUpdateSegmentFailsWhenLocked() throws SegmentUpdateException {
		segment.setLocked(true);
		exception.expect(SegmentUpdateException.class);
		segment.update(START_INDEX, END_INDEX);
	}

	@Test
	public void testUpdateToStartOfSegmentFailsWhenPreviousSegmentIsLocked()
			throws SegmentUpdateException {
		segment.prevSegment().setLocked(true);
		exception.expect(SegmentUpdateException.class);
		segment.update(START_INDEX + 1, END_INDEX);
	}

	@Test
	public void testUpdateToEndOfSegmentSucceedsWhenPreviousSegmentIsLocked()
			throws SegmentUpdateException {
		segment.prevSegment().setLocked(true);
		segment.update(START_INDEX, END_INDEX - 1);
	}

	@Test
	public void testUpdateToEndOfSegmentFailsWhenNextSegmentIsLocked()
			throws SegmentUpdateException {
		segment.nextSegment().setLocked(true);
		exception.expect(SegmentUpdateException.class);
		segment.update(START_INDEX, END_INDEX - 1);
	}

	@Test
	public void testUpdateToStartfSegmentSucceedsWhenNextSegmentIsLocked()
			throws SegmentUpdateException {
		segment.nextSegment().setLocked(true);
		segment.update(START_INDEX + 1, END_INDEX);
	}

	@Test
	public void testUpdateSegmentFailsWhenTooShort() throws SegmentUpdateException {
		exception.expect(SegmentUpdateException.class);
		// We need to subtract 2 rather than 1 because we are specifying the end index,
		// not the length
		segment.update(START_INDEX, START_INDEX + IProfileSegment.MINIMUM_SEGMENT_LENGTH - 2);
	}

	@Test
	public void testUpdateSegmentFailsIfStartIndexNotWithinPrevOrCurrentSegment()
			throws SegmentUpdateException {
		exception.expect(SegmentUpdateException.class);
		int testStart = segment.prevSegment().getStartIndex() - 1;
		segment.update(testStart, segment.getEndIndex());
	}

	@Test
	public void testUpdateSegmentFailsIfEndIndexNotWithinNextOrCurrentSegment()
			throws SegmentUpdateException {
		exception.expect(SegmentUpdateException.class);
		int testEnd = segment.nextSegment().getEndIndex() + 1;
		segment.update(segment.getStartIndex(), testEnd);
	}

	@Test
	public void testUpdateSegmentFailsIfPrevSegmentWillBecomeTooShort()
			throws SegmentUpdateException {
		exception.expect(SegmentUpdateException.class);
		int testStart = segment.prevSegment().getStartIndex() + 1;
		segment.update(testStart, segment.getEndIndex());
	}

	@Test
	public void testUpdateSegmentFailsIfNextSegmentWillBecomeTooShort()
			throws SegmentUpdateException {
		exception.expect(SegmentUpdateException.class);
		int testEnd = segment.nextSegment().getEndIndex() - 1;
		segment.update(segment.getStartIndex(), testEnd);
	}

	@Test
	public void testUpdateLoneSegmentFailsOnStartOutOfBoundsLow() throws SegmentUpdateException {
		exception.expect(SegmentUpdateException.class);
		segment.update(-1, END_INDEX);
	}

	@Test
	public void testUpdateLoneSegmentFailsOnStartOutOfBoundsHigh() throws SegmentUpdateException {
		exception.expect(SegmentUpdateException.class);
		segment.update(PROFILE_LENGTH + 1, END_INDEX);
	}

	@Test
	public void testUpdateLoneSegmentFailsOnEndOutOfBoundsLow() throws SegmentUpdateException {
		exception.expect(SegmentUpdateException.class);
		segment.update(START_INDEX, -1);
	}

	@Test
	public void testUpdateLoneSegmentFailsOnEndOutOfBoundsHigh() throws SegmentUpdateException {
		exception.expect(SegmentUpdateException.class);
		segment.update(START_INDEX, PROFILE_LENGTH + 1);
	}

	@Test
	public void testCopy() {
		IProfileSegment s2 = segment.duplicate();
		assertEquals(segment, s2);
	}
}
