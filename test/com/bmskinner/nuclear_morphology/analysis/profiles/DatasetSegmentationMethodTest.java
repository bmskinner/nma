package com.bmskinner.nuclear_morphology.analysis.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nuclear_morphology.analysis.FloatArrayTester;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.TestDatasetFactory;
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

public class DatasetSegmentationMethodTest extends FloatArrayTester {
	
	@Rule
	public final ExpectedException expectedException = ExpectedException.none();
	
	@Before
	public void setUp(){
		Logger logger = Logger.getLogger(Loggable.PROGRAM_LOGGER);
		logger.setLevel(Level.FINE);
		logger.addHandler(new ConsoleHandler(new LogPanelFormatter()));
	}
	
	@Test
	public void testSegmentationMethodExceptsOnUnprofiledDataset() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder().cellCount(1).ofType(NucleusType.ROUND)
				.baseHeight(40).baseWidth(40).build();
		expectedException.expect(UnavailableProfileTypeException.class);
		new DatasetSegmentationMethod(d, MorphologyAnalysisMode.NEW).call();
	}
	
	/**
	 * The square dataset is a round nucleus type, so there should not be any segmentation by default
	 * @throws Exception
	 */
	@Test
	public void testSingleCellSquareDatasetProducesSingleSegmentMedianWhenRound() throws Exception {
		IAnalysisDataset dataset = new TestDatasetBuilder().cellCount(1).baseHeight(40).baseWidth(40).profiled().build();

		new DatasetSegmentationMethod(dataset, MorphologyAnalysisMode.NEW).call();

		ISegmentedProfile median = dataset.getCollection()
				.getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);

		assertEquals("Median segment count", 1, median.getSegmentCount());
		
		ICell cell = dataset.getCollection().getCells().stream().findFirst().get();
		
		ISegmentedProfile cellProfile = cell.getNucleus().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
		
		assertEquals("Cell segment count", median.getSegmentCount(), cellProfile.getSegmentCount());
	}
	
	/**
	 * Force the nucleus type to segment
	 * @throws Exception
	 */
	@Test
	public void testSingleCellSquareDatasetProducesFourSegmentMedianWhenSetToAysmmetric() throws Exception {
		IAnalysisDataset dataset = new TestDatasetBuilder().cellCount(1).ofType(NucleusType.OTHER_ASYMMETRIC)
				.baseHeight(40).baseWidth(40).profiled().build();

		new DatasetSegmentationMethod(dataset, MorphologyAnalysisMode.NEW).call();

		ISegmentedProfile median = dataset.getCollection()
				.getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);

		assertEquals("Median segment count", 4, median.getSegmentCount());
		ICell cell = dataset.getCollection().getCells().stream().findFirst().get();
		
		ISegmentedProfile cellProfile = cell.getNucleus().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
		
		assertEquals("Cell segment count", median.getSegmentCount(), cellProfile.getSegmentCount());
	}

	@Test
	public void testSingleCellSquareDatasetHasFourSegments() throws Exception {
		
		IAnalysisDataset dataset = new TestDatasetBuilder().cellCount(1)
				.baseHeight(40).baseWidth(40).profiled().build();

		
		new DatasetSegmentationMethod(dataset, MorphologyAnalysisMode.NEW).call();
		
		// Check the collection
		ISegmentedProfile median = dataset.getCollection()
				.getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		
		ICell cell = dataset.getCollection().getCells().stream().findFirst().get();
		
		ISegmentedProfile cellProfile = cell.getNucleus().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
		
		for(IBorderSegment s : median.getSegments()) {
			System.out.println(s.toString());
		}
		assertEquals(4, median.getSegmentCount());
	}
	
	@Test
	public void testMultiCellSquareDatasetHasFourSegments() throws Exception {

		IAnalysisDataset dataset = new TestDatasetBuilder().cellCount(50)
				.baseHeight(40).baseWidth(40).profiled().build();
						
		new DatasetSegmentationMethod(dataset, MorphologyAnalysisMode.NEW).call();
		
		// Check the collection
		ISegmentedProfile median = dataset.getCollection()
				.getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		
		ICell cell = dataset.getCollection().getCells().stream().findFirst().get();
		
		ISegmentedProfile cellProfile = cell.getNucleus().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);		
		for(IBorderSegment s : median.getSegments()) {
			System.out.println(s.toString());
		}
		assertEquals(4, median.getSegmentCount());
	}

}
