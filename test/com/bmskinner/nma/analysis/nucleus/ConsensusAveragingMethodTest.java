package com.bmskinner.nma.analysis.nucleus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.bmskinner.nma.TestDatasetBuilder;
import com.bmskinner.nma.analysis.nucleus.ConsensusAveragingMethod;
import com.bmskinner.nma.components.MissingLandmarkException;
import com.bmskinner.nma.components.Statistical;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.profiles.ProfileManager;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.RuleSetCollection;

/**
 * Tests that the averaging method is working
 * 
 * @author ben
 * @since 2.0.0
 *
 */
public class ConsensusAveragingMethodTest {

	@Test
	public void testConsensusHasSameLandmarksAsCollection() throws Exception {

		IAnalysisDataset d = new TestDatasetBuilder(123)
				.cellCount(10)
				.ofType(RuleSetCollection.mouseSpermRuleSetCollection())
				.withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.addSignalsInChannel(0)
				.segmented()
				.build();

		assertFalse("Collection should not yet have consensus", d.getCollection().hasConsensus());

		// Add new landmarks
		ProfileManager m = d.getCollection().getProfileManager();
		m.updateLandmark(d.getCollection().getRuleSetCollection().getLandmark(OrientationMark.TOP)
				.orElseThrow(MissingLandmarkException::new), 0);
		m.updateLandmark(
				d.getCollection().getRuleSetCollection().getLandmark(OrientationMark.BOTTOM)
						.orElseThrow(MissingLandmarkException::new),
				10);

		// Expected landmarks
		OrientationMark[] lms = { OrientationMark.REFERENCE, OrientationMark.TOP,
				OrientationMark.BOTTOM };

		// Check landmarks are present in the profile collection
		for (OrientationMark l : lms) {
			assertTrue(l + " should be present",
					d.getCollection().getProfileCollection().hasLandmark(l));
		}

		// Make the consensus
		new ConsensusAveragingMethod(d).call();

		assertTrue("Consensus should be created", d.getCollection().hasConsensus());

		// Check that the landmarks are present in the consensus

		Nucleus n = d.getCollection().getConsensus();
		for (OrientationMark l : lms) {
			assertTrue(l + " should be present", n.hasLandmark(l));
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
				.segmented()
				.build();

		assertFalse("Collection should not yet have consensus", d.getCollection().hasConsensus());

		// Expected segments
		List<UUID> segs = d.getCollection().getProfileCollection().getSegmentIDs();

		// Make the consensus
		new ConsensusAveragingMethod(d).call();

		// Check that the segments are present in the consensus

		Nucleus n = d.getCollection().getConsensus();

		assertEquals("Segment count should match", segs.size(),
				n.getProfile(ProfileType.ANGLE).getSegmentCount());
		for (UUID id : segs) {
			assertTrue(id + " should be present", n.getProfile(ProfileType.ANGLE).hasSegment(id));
		}
	}

	@Test
	public void testConsensusHasSameScaleAsCollection() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(123).cellCount(10)
				.ofType(RuleSetCollection.roundRuleSetCollection()).withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.addSignalsInChannel(0).segmented().build();

		double scale = 20;
		d.setScale(scale);

		for (Nucleus n : d.getCollection().getNuclei())
			assertEquals("Scale should match", scale, n.getScale(), 0);

		// Make the consensus
		new ConsensusAveragingMethod(d).call();

		Nucleus n = d.getCollection().getConsensus();
		assertEquals("Scale should match", scale, n.getScale(), 0);
	}

	@Test
	public void testConsensusHasMeasurements() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(123).cellCount(10)
				.ofType(RuleSetCollection.roundRuleSetCollection()).withMaxSizeVariation(10)
				.randomOffsetProfiles(true)
				.addSignalsInChannel(0).segmented().build();

		double scale = 20;
		d.setScale(scale);

		// Make the consensus
		new ConsensusAveragingMethod(d).call();

		Nucleus n = d.getCollection().getConsensus();
		assertTrue(n.getMeasurement(Measurement.PERIMETER) != Statistical.ERROR_CALCULATING_STAT);
		assertTrue(n.getMeasurement(Measurement.AREA) != Statistical.ERROR_CALCULATING_STAT);
	}
}
