package com.bmskinner.nma.analysis.profiles;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.bmskinner.nma.TestDatasetBuilder;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.stats.Stats;

/**
 * Tests for the median finder 
 * @author bms41
 * @since 1.14.0
 *
 */
public class RepresentativeMedianFinderTest extends AbstractProfileMethodTest {
		
	@Test
	public void testSingleNucleusDatasetReturnsIdenticalProfile() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(1).ofType(RuleSetCollection.roundRuleSetCollection())
				.randomOffsetProfiles(false)
				.baseHeight(40).baseWidth(40).segmented().build();
		
		ISegmentedProfile template = d.getCollection()
				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN);

				
		RepresentativeMedianFinder finder = new RepresentativeMedianFinder(d.getCollection());
		
		IProfile result = finder.findMedian();
		
		List<IProfile> profiles = new ArrayList<>();
		profiles.add(template);
		profiles.add(result);
		
		List<String> names = new ArrayList<>();
		names.add("Overall median");
		names.add("Representative median");
		
//		if(!template.equals(result))
//			ChartFactoryTest.showProfiles(profiles, names, "Identical profiles in fitter");
		
		equals(template.toFloatArray(), result.toFloatArray(), 0);
	}
	
	@Test
	public void testTwoIdenticalNucleusDataset() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(2)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.randomOffsetProfiles(false)
				.baseHeight(40).baseWidth(40)
				.segmented().build();
		
		ISegmentedProfile template = d.getCollection()
				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN);

				
		RepresentativeMedianFinder finder = new RepresentativeMedianFinder(d.getCollection());
		
		IProfile result = finder.findMedian();
			
		equals(template.toFloatArray(), result.toFloatArray(), 0);
	}
		
	@Test
	public void testMultipleIdenticalCells() throws Exception {

			IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(50)
					.withMaxSizeVariation(0)
					.baseHeight(40).baseWidth(40)
					.randomOffsetProfiles(false)
					.profiled().build();
			ISegmentedProfile template = dataset.getCollection()
					.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN);

			RepresentativeMedianFinder finder = new RepresentativeMedianFinder(dataset.getCollection());
			
			IProfile result = finder.findMedian();
			
			assertTrue(result!=null);		
	}
	
	@Test
	public void testMedianFindingIsRobustToIncreasingVariation() throws Exception {
		int maxCells = 50;		
		for(int var=0; var<=20; var++) {
			IAnalysisDataset dataset = new TestDatasetBuilder(RNG_SEED).cellCount(maxCells)
					.withMaxSizeVariation(var)
					.baseHeight(40).baseWidth(40)
					.randomOffsetProfiles(false)
					.profiled().build();
			ISegmentedProfile template = dataset.getCollection()
					.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN);

					
			RepresentativeMedianFinder finder = new RepresentativeMedianFinder(dataset.getCollection());
			
			IProfile result = finder.findMedian();
			
			assertTrue(result!=null);
		}
		
	}

}
