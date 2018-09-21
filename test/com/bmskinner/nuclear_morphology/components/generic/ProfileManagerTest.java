package com.bmskinner.nuclear_morphology.components.generic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * Tests for population level profile manipulation
 * consistency
 * @author ben
 * @since 1.14.0
 *
 */
public class ProfileManagerTest {
	
	private static final long RNG_SEED = 1234;
	private ProfileManager manager;
	private ICellCollection collection;

	@Before
	public void setUp() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(10)
				.ofType(NucleusType.ROUND)
				.randomOffsetProfiles(true)
				.segmented().build();
		collection = d.getCollection();
		manager = collection.getProfileManager();
	}

	@Test
	public void testGetProfileLength() {
		int perimeter = TestDatasetBuilder.DEFAULT_BASE_HEIGHT*2 + TestDatasetBuilder.DEFAULT_BASE_WIDTH*2 - 2;
		assertEquals(perimeter, manager.getProfileLength());
	}

	@Test
	public void testUpdateTagToMedianBestFit() {
		fail("Not yet implemented");
	}

	@Test
	public void testUpdateProfileCollectionOffsets() {
		fail("Not yet implemented");
	}

	@Test
	public void testCalculateTopAndBottomVerticals() {
		fail("Not yet implemented");
	}

	@Test
	public void testUpdateBorderTag() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSegmentCount() {
		fail("Not yet implemented");
	}

	@Test
	public void testRecalculateProfileAggregates() {
		fail("Not yet implemented");
	}

	@Test
	public void testCopyCollectionOffsets() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetLockOnAllNucleusSegmentsExcept() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetLockOnAllNucleusSegments() {
		fail("Not yet implemented");
	}

	@Test
	public void testUpdateCellSegmentStartIndex() {
		fail("Not yet implemented");
	}

	@Test
	public void testUpdateMedianProfileSegmentIndex() {
		fail("Not yet implemented");
	}

	@Test
	public void testTestSegmentsMergeable() throws Exception {
		
		ISegmentedProfile profile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		
		List<UUID> segIds = profile.getSegmentIDs();
		for(int i=0; i<segIds.size(); i++) {
			UUID segId1 = segIds.get(i);
			IBorderSegment seg1 = profile.getSegment(segId1);
			for(int j=0; j<segIds.size(); j++) {
				UUID segId2 = segIds.get(j);
				IBorderSegment seg2 = profile.getSegment(segId2);
				if(i==j)
					assertFalse("Merging "+seg1.toString()+" and "+seg2.toString()+" should fail", manager.testSegmentsMergeable(seg1, seg2));
				else {
					if(i==j-1)
						assertTrue("Merging "+seg1.toString()+" and "+seg2.toString()+" should succeed", manager.testSegmentsMergeable(seg1, seg2));
					else
						assertFalse("Merging "+seg1.toString()+" and "+seg2.toString()+" should fail", manager.testSegmentsMergeable(seg1, seg2));
				}
			}
		}
	}

	@Test
	public void testCountNucleiNotMatchingMedianSegmentation() throws Exception {
		assertEquals(0, manager.countNucleiNotMatchingMedianSegmentation());
		
		// Merge on one nucleus will take it out of sync
		Nucleus n = collection.streamCells().findFirst().get().getNucleus();
		ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
		UUID segId1 = profile.getSegmentAt(1).getID();
		UUID segId2 = profile.getSegmentAt(2).getID();
		profile.mergeSegments(segId1, segId2, UUID.randomUUID());
		n.setProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, profile);
		
		assertEquals(1, manager.countNucleiNotMatchingMedianSegmentation());
	}

	@Test
	public void testMergeSegments() throws Exception {

		ISegmentedProfile profile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		List<UUID> segIds = profile.getSegmentIDs();
		UUID newId = UUID.randomUUID();
		UUID segId1 = profile.getSegmentAt(1).getID();
		UUID segId2 = profile.getSegmentAt(2).getID();
		IBorderSegment seg1 = profile.getSegmentAt(1);
		IBorderSegment seg2 = profile.getSegmentAt(2);
				
		manager.mergeSegments(seg1, seg2, newId);
		
		List<UUID> newIds = collection.getProfileCollection().getSegmentIDs();
		assertEquals(segIds.size()-1, newIds.size());
		assertTrue(newIds.contains(newId));
		assertFalse(newIds.contains(segId1));
		assertFalse(newIds.contains(segId2));
	}

	@Test
	public void testSplitSegmentIBorderSegment() {
		fail("Not yet implemented");
	}

	@Test
	public void testSplitSegmentIBorderSegmentUUIDUUID() {
		fail("Not yet implemented");
	}

	@Test
	public void testUnmergeSegments() {
		fail("Not yet implemented");
	}

}
