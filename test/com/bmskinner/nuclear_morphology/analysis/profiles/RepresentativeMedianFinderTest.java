package com.bmskinner.nuclear_morphology.analysis.profiles;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.FloatArrayTester;
import com.bmskinner.nuclear_morphology.charting.ChartFactoryTest;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * Tests for the median finder 
 * @author bms41
 * @since 1.14.0
 *
 */
public class RepresentativeMedianFinderTest extends FloatArrayTester {
	
	@Before
	public void setUp(){
		Logger logger = Logger.getLogger(Loggable.PROGRAM_LOGGER);
		logger.setLevel(Level.FINE);
		logger.addHandler(new ConsoleHandler(new LogPanelFormatter()));

	}
	
	@Test
	public void testSingleNucleusDatasetReturnsIdenticalProfile() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(1234).cellCount(1).ofType(NucleusType.ROUND)
				.randomOffsetProfiles(false)
				.baseHeight(40).baseWidth(40).segmented().build();
		
		ISegmentedProfile template = d.getCollection()
				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);

				
		RepresentativeMedianFinder finder = new RepresentativeMedianFinder(d.getCollection());
		
		IProfile result = finder.findMedian();
		
		List<IProfile> profiles = new ArrayList<>();
		profiles.add(template);
		profiles.add(result);
		
		List<String> names = new ArrayList<>();
		names.add("Overall median");
		names.add("Representative median");
		
		if(!template.equals(result))
			ChartFactoryTest.showProfiles(profiles, names, "Identical profiles in fitter");
		
		equals(template.toFloatArray(), result.toFloatArray(), 0);
	}
	
	@Test
	public void testTwoIdenticalNucleusDataset() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(1234).cellCount(2).ofType(NucleusType.ROUND)
				.randomOffsetProfiles(false)
				.baseHeight(40).baseWidth(40).segmented().build();
		
		ISegmentedProfile template = d.getCollection()
				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);

				
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
		
//		equals(template.toFloatArray(), result.toFloatArray(), 0);
	}
	
	@Test
	public void testThreeVariableNucleiDataset() throws Exception {
		IAnalysisDataset dataset = new TestDatasetBuilder(1234).cellCount(3)
				.withMaxSizeVariation(16)
				.baseHeight(40).baseWidth(40)
				.randomOffsetProfiles(false)
				.profiled().build();
		
		ISegmentedProfile template = dataset.getCollection()
				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);

				
		RepresentativeMedianFinder finder = new RepresentativeMedianFinder(dataset.getCollection());
		
		IProfile result = finder.findMedian();
		
		List<IProfile> profiles = new ArrayList<>();
		profiles.add(template);
		profiles.add(result);
		
		List<String> names = new ArrayList<>();
		names.add("Overall median");
		names.add("Representative median");
		
//		if(!template.equals(result))
			ChartFactoryTest.showProfiles(profiles, names, "Identical profiles in fitter");
		
//		equals(template.toFloatArray(), result.toFloatArray(), 0);
	}
	
	@Test
	public void testMedianFindingIsRobustToIncreasingVariation() throws Exception {
		long seed = 1234;
		int maxCells = 50;		
		for(int var=0; var<=20; var++) {
			System.out.println(String.format("Testing variability %s on %s cells", var, maxCells));
			IAnalysisDataset dataset = new TestDatasetBuilder(seed).cellCount(maxCells)
					.withMaxSizeVariation(var)
					.baseHeight(40).baseWidth(40)
					.randomOffsetProfiles(false)
					.profiled().build();
			ISegmentedProfile template = dataset.getCollection()
					.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);

					
			RepresentativeMedianFinder finder = new RepresentativeMedianFinder(dataset.getCollection());
			
			IProfile result = finder.findMedian();
			
			List<IProfile> profiles = new ArrayList<>();
			profiles.add(template);
			profiles.add(result);
			
			List<String> names = new ArrayList<>();
			names.add("Overall median");
			names.add("Representative median");
			
//			if(!template.equals(result))
				ChartFactoryTest.showProfiles(profiles, names, "50 cells with variability "+var);
		}
		
	}

}
