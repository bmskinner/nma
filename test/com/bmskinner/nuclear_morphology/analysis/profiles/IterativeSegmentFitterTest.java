package com.bmskinner.nuclear_morphology.analysis.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.charting.ChartFactoryTest;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.generic.BooleanProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/** 
 * Tests for the iterative segment fitting
 * @author bms41
 * @since 1.14.0
 *
 */
public class IterativeSegmentFitterTest {
	
	@Rule
	public final ExpectedException expectedException = ExpectedException.none();
	
	private IterativeSegmentFitter fitter;
	
	@Before
	public void setUp(){
		Logger logger = Logger.getLogger(Loggable.PROGRAM_LOGGER);
		logger.setLevel(Level.FINE);
		logger.addHandler(new ConsoleHandler(new LogPanelFormatter()));

	}
	
	@Test
	public void testFitterExceptsOnTemplateWithNoSegments() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(1234).cellCount(1).ofType(NucleusType.ROUND)
				.randomOffsetProfiles(false)
				.baseHeight(40).baseWidth(40).profiled().build();
		
		ISegmentedProfile template = d.getCollection()
				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		
		ISegmentedProfile target = template.copy();
		
		expectedException.expect(IllegalArgumentException.class);
		fitter = new IterativeSegmentFitter(template.copy());
	}
	
	@Test
	public void testFittingIdenticalProfileMakesNoChange() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(1234).cellCount(1).ofType(NucleusType.ROUND)
				.randomOffsetProfiles(false)
				.baseHeight(40).baseWidth(40).segmented().build();
		
		ISegmentedProfile template = d.getCollection()
				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		
		
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
		IAnalysisDataset d1 = new TestDatasetBuilder(1234).cellCount(1).ofType(NucleusType.ROUND)
				.randomOffsetProfiles(false)
				.baseHeight(40).baseWidth(40).segmented().build();
		
		ISegmentedProfile template = d1.getCollection()
				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		
		IAnalysisDataset d2 = new TestDatasetBuilder(1234).cellCount(1).ofType(NucleusType.ROUND)
				.randomOffsetProfiles(false)
				.baseHeight(50).baseWidth(30).segmented().build();
		
		ISegmentedProfile target = d2.getCollection()
				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);

				
		fitter = new IterativeSegmentFitter(template.copy());
		
		ISegmentedProfile result = fitter.fit(target.copy());
		
		List<IProfile> profiles = new ArrayList<>();
		profiles.add(template);
		profiles.add(target);
		profiles.add(result);
		
		List<String> names = new ArrayList<>();
		names.add("Template");
		names.add("Target");
		names.add("Result");

		ChartFactoryTest.showProfiles(profiles, names, "Square versus rectangle in fitter");
	}
	
	

}
