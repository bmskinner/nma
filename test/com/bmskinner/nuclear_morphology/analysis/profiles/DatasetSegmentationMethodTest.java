package com.bmskinner.nuclear_morphology.analysis.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.FloatArrayTester;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.TestDatasetFactory;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

public class DatasetSegmentationMethodTest extends FloatArrayTester {
	
	@Before
	public void setUp(){
		Logger logger = Logger.getLogger(Loggable.PROGRAM_LOGGER);
		logger.setLevel(Level.FINE);
		logger.addHandler(new ConsoleHandler(new LogPanelFormatter()));
	}

	@Test
	public void testSingleCellSquareDatasetHasFourSegments() throws Exception {

		IAnalysisDataset dataset = TestDatasetFactory.squareDataset(1);
		
		IAnalysisMethod m = new DatasetProfilingMethod(dataset);
		m.call();
		
		m = new DatasetSegmentationMethod(dataset, MorphologyAnalysisMode.NEW);
		m.call();
		
		// Check the collection
		ISegmentedProfile median = dataset.getCollection()
				.getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		
		ICell cell = dataset.getCollection().getCells().stream().findFirst().get();
		
		ISegmentedProfile cellProfile = cell.getNucleus().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);

//		A single cell dataset has the same profile for median
		assertTrue(equals(median.toFloatArray(), cellProfile.toFloatArray(), 0.0001f));
		
		for(IBorderSegment s : median.getSegments()) {
			System.out.println(s.toString());
		}
		assertEquals(4, median.getSegmentCount());
	}
	
	@Test
	public void testMultiCellSquareDatasetHasFourSegments() throws Exception {

		IAnalysisDataset dataset = TestDatasetFactory.squareDataset(50);
		
		IAnalysisMethod m = new DatasetProfilingMethod(dataset);
		m.call();
		
		m = new DatasetSegmentationMethod(dataset, MorphologyAnalysisMode.NEW);
		m.call();
		
		// Check the collection
		ISegmentedProfile median = dataset.getCollection()
				.getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		
		ICell cell = dataset.getCollection().getCells().stream().findFirst().get();
		
		ISegmentedProfile cellProfile = cell.getNucleus().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);

//		A single cell dataset has the same profile for median
		assertTrue(equals(median.toFloatArray(), cellProfile.toFloatArray(), 0.0001f));
		
		for(IBorderSegment s : median.getSegments()) {
			System.out.println(s.toString());
		}
		assertEquals(4, median.getSegmentCount());
	}

}
