package com.bmskinner.nuclear_morphology.components.generic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * Tests for population level profile manipulation
 * consistency
 * @author ben
 * @since 1.14.0
 *
 */
public class ProfileManagerTest {
	
	private Logger logger;
	private static final long RNG_SEED = 1234;
	private ProfileManager manager;
	private ICellCollection collection;

	@Before
	public void setUp() throws Exception {
		
		logger = Logger.getLogger(Loggable.ROOT_LOGGER);
		logger.setLevel(Level.FINE);
		logger.addHandler(new ConsoleHandler(new LogPanelFormatter()));
		
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
	public void testSetLockOnAllNucleusSegmentsExcept() throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException, UnsegmentedProfileException {
		ISegmentedProfile profile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		UUID segId1 = profile.getSegmentAt(1).getID();
		
		manager.setLockOnAllNucleusSegments(false);
		for(ICell c : collection) {
			for(Nucleus n : c.getNuclei()) {
				ISegmentedProfile p = n.getProfile(ProfileType.ANGLE);
				for(IBorderSegment s : p.getSegments()) {
					assertFalse(s.isLocked());
				}
			}
		}
		
		manager.setLockOnAllNucleusSegmentsExcept(segId1, true);
		for(ICell c : collection) {
			for(Nucleus n : c.getNuclei()) {
				ISegmentedProfile p = n.getProfile(ProfileType.ANGLE);
				for(IBorderSegment s : p.getSegments()) {
					if(s.getID().equals(segId1))
						assertFalse(s.isLocked());
					else
						assertTrue(s.isLocked());
				}
			}
		}
		
	}

	@Test
	public void testSetLockOnAllNucleusSegments() throws Exception {
		manager.setLockOnAllNucleusSegments(false);
		for(ICell c : collection) {
			for(Nucleus n : c.getNuclei()) {
				ISegmentedProfile p = n.getProfile(ProfileType.ANGLE);
				for(IBorderSegment s : p.getSegments()) {
					assertFalse(s.isLocked());
				}
			}
		}
		
		manager.setLockOnAllNucleusSegments(true);
		
		for(ICell c : collection) {
			for(Nucleus n : c.getNuclei()) {
				ISegmentedProfile p = n.getProfile(ProfileType.ANGLE);
				for(IBorderSegment s : p.getSegments()) {
					assertTrue(s.isLocked());
				}
			}
		}
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
		assertEquals(0, manager.countNucleiNotMatchingMedianSegmentation());
		List<UUID> newIds = collection.getProfileCollection().getSegmentIDs();
		assertEquals(segIds.size()-1, newIds.size());
		assertTrue(newIds.contains(newId));
		assertFalse(newIds.contains(segId1));
		assertFalse(newIds.contains(segId2));
		IBorderSegment mergedSegment = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN).getSegment(newId);
		assertTrue(mergedSegment.hasMergeSources());
		assertTrue(mergedSegment.hasMergeSource(segId1));
		assertTrue(mergedSegment.hasMergeSource(segId2));
		

		for(Nucleus n : collection.getNuclei()) {
			ISegmentedProfile nucleusProfile =  n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
			List<UUID> nucleusIds = nucleusProfile.getSegmentIDs();
			assertEquals(newIds.size(), nucleusIds.size());
			assertTrue(newIds.contains(newId));
			assertFalse(newIds.contains(segId1));
			assertFalse(newIds.contains(segId2));
			IBorderSegment mergedSeg = nucleusProfile.getSegment(newId);
			assertTrue(mergedSeg.hasMergeSources());
			assertTrue(mergedSeg.hasMergeSource(segId1));
			assertTrue(mergedSeg.hasMergeSource(segId2));
		}
	}


	@Test
	public void testUnmergeSegments() throws Exception {
		ISegmentedProfile profile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		List<UUID> segIds = profile.getSegmentIDs();
		UUID newId = UUID.randomUUID();
		UUID segId1 = profile.getSegmentAt(1).getID();
		UUID segId2 = profile.getSegmentAt(2).getID();
		IBorderSegment seg1 = profile.getSegmentAt(1);
		IBorderSegment seg2 = profile.getSegmentAt(2);

		manager.mergeSegments(seg1, seg2, newId);
		assertEquals(0, manager.countNucleiNotMatchingMedianSegmentation());
		
		manager.unmergeSegments(newId);
		
		List<UUID> newIds = collection.getProfileCollection().getSegmentIDs();
		assertEquals(segIds.size(), newIds.size());
		assertFalse(newIds.contains(newId));
		assertTrue(newIds.contains(segId1));
		assertTrue(newIds.contains(segId2));
		
		assertEquals(0, manager.countNucleiNotMatchingMedianSegmentation());
		
		for(Nucleus n : collection.getNuclei()) {
			ISegmentedProfile nucleusProfile =  n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
			List<UUID> nucleusIds = nucleusProfile.getSegmentIDs();
			assertEquals(segIds.size(), nucleusIds.size());
			assertFalse(newIds.contains(newId));
			assertTrue(newIds.contains(segId1));
			assertTrue(newIds.contains(segId2));
		}
		
	}

}
