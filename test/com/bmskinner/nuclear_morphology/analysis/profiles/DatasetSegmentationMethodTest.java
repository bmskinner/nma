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
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
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
	
	private void testDatasetMedianAndCellsAreSegmentedConsistently(IAnalysisDataset d) throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException, UnsegmentedProfileException {

		ISegmentedProfile median = d.getCollection()
				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);

		ICell cell = d.getCollection().getCells().stream().findFirst().get();

		ISegmentedProfile cellProfile = cell.getNucleus().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);

		assertEquals("Cell segment count should match median", median.getSegmentCount(), cellProfile.getSegmentCount());
		for(IBorderSegment s : median.getSegments()) {
			assertTrue(cellProfile.hasSegment(s.getID()));
		}
	}
		
	/**
	 * Test that increasing numbers of varying cells does not affect segment fitting.
	 * The number of segments may change in variable dataset, since the median may become distorted,
	 * but any segments found should be propogated to the cells.
	 * 
	 * This test ranges from variation of zero (identical cells) to 20 (highly variable dataset)
	 * across up to 50 cells. 
	 * @throws Exception
	 */
	@Test
	public void testSegmentationIsIndependentOfCellCountInVaryingDataset() throws Exception {
		
		int maxCells = 50;		
		for(int i=1; i<=maxCells; i++) {
			for(int var=0; var<=20; var++) {
				System.out.println(String.format("Testing variability %s on %s cells", var, i));
				IAnalysisDataset dataset = new TestDatasetBuilder().cellCount(i).withMaxSizeVariation(var)
						.baseHeight(40).baseWidth(40).offsetProfiles(true).profiled().build();
				new DatasetSegmentationMethod(dataset, MorphologyAnalysisMode.NEW).call();
				testDatasetMedianAndCellsAreSegmentedConsistently(dataset);
			}
		}
	}
}
