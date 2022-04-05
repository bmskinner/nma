package com.bmskinner.nuclear_morphology.analysis.nucleus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.Statistical;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileManager;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;

/**
 * Tests that the averaging method is working
 * @author ben
 * @since 2.0.0
 *
 */
public class ConsensusAveragingMethodTest {
	
	@Test
	public void testConsensusHasSameLandmarksAsCollection() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(123)
				.cellCount(10)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.addSignalsInChannel(0)
				.segmented().build();
		
		assertFalse("Collection should not yet have consensus", d.getCollection().hasConsensus());
		
		// Add new landmarks
		ProfileManager m = d.getCollection().getProfileManager();
		m.updateLandmark(Landmark.TOP_VERTICAL, 0);
		m.updateLandmark(Landmark.BOTTOM_VERTICAL, 10);
		
		//Expected landmarks
		Landmark[] lms = { Landmark.REFERENCE_POINT, Landmark.TOP_VERTICAL, Landmark.BOTTOM_VERTICAL};
		
		// Check landmarks are present in the profile collection
		for(Landmark l : lms) {
			assertTrue(l+" should be present", d.getCollection().getProfileCollection().hasLandmark(l));
		}
		
		// Make the consensus
		new ConsensusAveragingMethod(d).call();	
		
		// Check that the landmarks are present in the consensus
		
		Nucleus n = d.getCollection().getConsensus();
		for(Landmark l : lms) {
			assertTrue(l+" should be present", n.hasLandmark(l));
		}
	}
	
	@Test
	public void testConsensusHasSameSegmentsAsCollection() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(123)
				.cellCount(10)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.addSignalsInChannel(0)
				.segmented().build();
		
		assertFalse("Collection should not yet have consensus", d.getCollection().hasConsensus());
			
		//Expected segments
		List<UUID> segs = d.getCollection().getProfileCollection().getSegmentIDs();
				
		// Make the consensus
		new ConsensusAveragingMethod(d).call();	
		
		// Check that the segments are present in the consensus

		Nucleus n = d.getCollection().getConsensus();
		
		assertEquals("Segment count should match", segs.size(), n.getProfile(ProfileType.ANGLE).getSegmentCount());
		for(UUID id : segs) {
			assertTrue(id+" should be present", n.getProfile(ProfileType.ANGLE).hasSegment(id));
		}
	}
	
	@Test
	public void testConsensusHasSameScaleAsCollection() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(123)
				.cellCount(10)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.addSignalsInChannel(0)
				.segmented().build();
		
		double scale = 20;
		d.setScale(scale);
		
		for(Nucleus n : d.getCollection().getNuclei())
			assertEquals("Scale should match", scale, n.getScale(), 0);
		
		// Make the consensus
		new ConsensusAveragingMethod(d).call();	
		
		Nucleus n = d.getCollection().getConsensus();
		assertEquals("Scale should match", scale, n.getScale(), 0);
	}
	
	@Test
	public void testConsensusHasMeasurements() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(123)
				.cellCount(10)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.addSignalsInChannel(0)
				.segmented().build();
		
		double scale = 20;
		d.setScale(scale);
		
		// Make the consensus
		new ConsensusAveragingMethod(d).call();	
		
		Nucleus n = d.getCollection().getConsensus();
		assertTrue(n.getMeasurement(Measurement.PERIMETER)!=Statistical.ERROR_CALCULATING_STAT);
		assertTrue(n.getMeasurement(Measurement.AREA)!=Statistical.ERROR_CALCULATING_STAT);
	}
}
