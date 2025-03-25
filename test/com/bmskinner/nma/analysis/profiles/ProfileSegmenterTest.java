package com.bmskinner.nma.analysis.profiles;

import java.util.List;

import org.junit.Test;

import com.bmskinner.nma.ComponentTester;
import com.bmskinner.nma.TestDatasetBuilder;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.stats.Stats;

/**
 * Test the ability of the segmenter to properly segment profiles
 * TODO: switch from visual inspection to asserts
 * @author Ben Skinner
 * @since 1.14.0
 *
 */
public class ProfileSegmenterTest extends ComponentTester {
	
	private void segmentMedianProfile(IAnalysisDataset d) throws MissingLandmarkException, MissingProfileException, ProfileException, Exception {
		IProfile median = d.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN);
		
		ProfileSegmenter segmenter = new ProfileSegmenter(median);
				
		List<IProfileSegment> segments = segmenter.segment();
		
		d.getCollection().getProfileCollection().setSegments(segments);
	}


	@Test
	public void testSingleCellSquareDatasetSegmentation() throws Exception {

		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(1)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.baseHeight(40).baseWidth(40)
				.profiled().build();

		segmentMedianProfile(d);
//		ChartFactoryTest.showMedianProfile(d, "Single cell dataset segmented");
	}
	
	@Test
	public void testMultiCellSquareDatasetSegmentation() throws Exception {

		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(50)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.baseHeight(40).baseWidth(40)
				.profiled().build();

		segmentMedianProfile(d);
//		ChartFactoryTest.showMedianProfile(d, "Multiple identical cells");
	}
	
	@Test
	public void testMultiCellVariableSquareDatasetSegmentation() throws Exception {

		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(50).ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(20)
				.randomOffsetProfiles(false)
				.baseHeight(40).baseWidth(40).profiled().build();

		segmentMedianProfile(d);
//		ChartFactoryTest.showMedianProfile(d, "Multiple variable cells");
	}
	
	@Test
	public void testMultiCellVariableOffsetSquareDatasetSegmentation() throws Exception {

		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(50).ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(20)
				.randomOffsetProfiles(true)
				.baseHeight(40).baseWidth(40).profiled().build();

		segmentMedianProfile(d);
//		ChartFactoryTest.showMedianProfile(d, "Multiple variable cells with offsets");
	}
	
	@Test
	public void testMultiCellVariableOffsetSquareDatasetSegmentationAndRotation() throws Exception {

		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(50).ofType(RuleSetCollection.roundRuleSetCollection())
				.withMaxSizeVariation(20)
				.maxRotation(270)
				.randomOffsetProfiles(true)
				.baseHeight(40).baseWidth(40).profiled().build();

		segmentMedianProfile(d);
//		ChartFactoryTest.showMedianProfile(d, "Multiple variable cells with offsets and rotation");
	}
	
}
