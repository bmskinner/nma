package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.util.List;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * Test the ability of the segmenter to properly segment profiles
 * TODO: switch from visual inspection to asserts
 * @author bms41
 * @since 1.14.0
 *
 */
public class ProfileSegmenterTest extends ComponentTester {
	
	private void segmentMedianProfile(IAnalysisDataset d) throws MissingLandmarkException, MissingProfileException, ProfileException, Exception {
		IProfile median = d.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN);
		
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
