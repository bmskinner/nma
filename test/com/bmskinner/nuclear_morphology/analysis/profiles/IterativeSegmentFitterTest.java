package com.bmskinner.nuclear_morphology.analysis.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.ComponentTester;
import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.charting.ChartFactoryTest;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusType;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.SegmentedFloatProfile;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.stats.Stats;

/** 
 * Tests for the iterative segment fitting
 * @author bms41
 * @since 1.14.0
 *
 */
public class IterativeSegmentFitterTest extends ComponentTester {

	@Rule
	public final ExpectedException expectedException = ExpectedException.none();
	
	private IterativeSegmentFitter fitter;
		
	@Test
	public void testFitterTakesNoActionOnTemplateWithNoSegments() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(1).ofType(RuleSetCollection.roundRuleSetCollection())
				.randomOffsetProfiles(false)
				.baseHeight(40).baseWidth(40).profiled().build();
		
		ISegmentedProfile template = d.getCollection()
				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN);
		
		ISegmentedProfile target = template.copy();
		
		fitter = new IterativeSegmentFitter(template.copy());
		ISegmentedProfile fitted = fitter.fit(target);
		assertEquals(target, fitted);
	}
	
	@Test
	public void testFittingIdenticalProfileMakesNoChange() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(1).ofType(RuleSetCollection.roundRuleSetCollection())
				.randomOffsetProfiles(false)
				.baseHeight(40).baseWidth(40).segmented().build();
		
		ISegmentedProfile template = d.getCollection()
				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN);
		
		
		ISegmentedProfile target = template.copy();
				
		fitter = new IterativeSegmentFitter(template.copy());
		
		ISegmentedProfile result = fitter.fit(target);
		
		List<IProfile> profiles = new ArrayList<>();
		profiles.add(template);
		profiles.add(target);
		profiles.add(result);
		
		List<String> names = new ArrayList<>();
		names.add("Template");
		names.add("Target");
		names.add("Result");
		
		if(!template.equals(result))
			ChartFactoryTest.showProfiles(profiles, names, "Identical profiles in fitter");
		
		assertEquals("Identical profiles", template, result);
	}
	
	@Test
	public void testFittingSquareTemplateToRectangularTarget() throws Exception {
		IAnalysisDataset d1 = new TestDatasetBuilder(RNG_SEED).cellCount(1).ofType(RuleSetCollection.roundRuleSetCollection())
				.randomOffsetProfiles(false)
				.baseHeight(40).baseWidth(40).segmented().build();
		
		ISegmentedProfile template = d1.getCollection()
				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN);
		
		IAnalysisDataset d2 = new TestDatasetBuilder(RNG_SEED).cellCount(1).ofType(RuleSetCollection.roundRuleSetCollection())
				.randomOffsetProfiles(false)
				.baseHeight(50).baseWidth(30).segmented().build();
		
		ISegmentedProfile target = d2.getCollection()
				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN);

				
		fitter = new IterativeSegmentFitter(template.copy());
		
		ISegmentedProfile result = fitter.fit(target.copy());
		
		for(int i=0; i<target.getSegmentCount(); i++) {
			IProfileSegment targetSeg = target.getOrderedSegments().get(i);
			IProfileSegment resultSeg = result.getOrderedSegments().get(i);	
			assertEquals("Segments should match", targetSeg.toString(), resultSeg.toString());
		}

//		ChartFactoryTest.showProfiles(profiles, names, "Square versus rectangle in fitter");
	}
	
	
	@Test
	public void testFittingOfSingleSegmentTemplate() throws Exception {
		
		IAnalysisDataset d = new TestDatasetBuilder(RNG_SEED).cellCount(10).ofType(RuleSetCollection.roundRuleSetCollection())
				.baseHeight(40).baseWidth(40).profiled().build();
		
		ISegmentedProfile singleSegmentProfile = new SegmentedFloatProfile(d.getCollection().getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN));
		
		assertEquals(1, singleSegmentProfile.getSegmentCount());
		
		fitter = new IterativeSegmentFitter(singleSegmentProfile);
		
		for(Nucleus n : d.getCollection().getNuclei()) {
			IProfile nucleusProfile  = n.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
			ISegmentedProfile segProfile = fitter.fit(nucleusProfile);
			n.setProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, segProfile);
			if(segProfile.getSegmentCount()!=singleSegmentProfile.getSegmentCount())
				fail("Segments could not be fitted to nucleus");
			
			assertEquals("Single segment before adding to nucleus", 0, segProfile.getSegment(IProfileCollection.DEFAULT_SEGMENT_ID).getStartIndex());	
						
			ISegmentedProfile test  = n.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
			IProfileSegment seg = test.getSegment(IProfileCollection.DEFAULT_SEGMENT_ID);
			assertEquals("Single segment start fetched from nucleus", 0, seg.getStartIndex());			
		}
		
	}
	

}
