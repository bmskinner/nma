package com.bmskinner.nuclear_morphology.analysis.profiles;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;

import com.bmskinner.nuclear_morphology.analysis.DatasetValidator;
import com.bmskinner.nuclear_morphology.analysis.FloatArrayTester;
import com.bmskinner.nuclear_morphology.charting.ChartFactoryTest;
import com.bmskinner.nuclear_morphology.charting.OutlineChartFactoryTest;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

public class AbstractProfileMethodTest extends FloatArrayTester {
	
	protected Logger logger;
	public static final long RNG_SEED = 1234;
	
	@Before
	public void setUp(){
		logger = Logger.getLogger(Loggable.PROGRAM_LOGGER);
		logger.setLevel(Level.FINE);
		logger.addHandler(new ConsoleHandler(new LogPanelFormatter()));
	}
	
	/**
	 * Test that profiles within a single cell dataset are consistent between 
	 * the median and the cell
	 * @param dataset
	 * @throws Exception
	 */
	protected void testProfilingIsConsistent(@NonNull IAnalysisDataset dataset) throws Exception {

		// Check the collection
		IProfile median = dataset.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
						
		for(ICell cell : dataset.getCollection().getCells()) {
			ISegmentedProfile cellProfile = cell.getNucleus().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
			if(!equals(median.toFloatArray(), cellProfile.toFloatArray(), 0.0001f))
				fail("Failed for dataset with "+dataset.getCollection().getNucleusCount()+" nuclei");			
		}
	}
	
	/**
	 * Test that the segmentation is consistent with a dataset
	 * @param d
	 * @throws UnavailableBorderTagException
	 * @throws UnavailableProfileTypeException
	 * @throws ProfileException
	 * @throws UnsegmentedProfileException
	 * @throws InterruptedException
	 */
	protected void testSegmentationIsConsistent(@NonNull IAnalysisDataset d) throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException, UnsegmentedProfileException, InterruptedException {

		DatasetValidator v = new DatasetValidator();
		boolean ok = v.validate(d);
		for(String s : v.getErrors()){
			System.out.println(s);
		}
		
		if(!ok) {
			ChartFactoryTest.showProfiles(v.getErrorCells(), d);
			OutlineChartFactoryTest.generateOutlineChartsForAllCells(d, "Known error");
		}
				
		assertTrue(ok);

		ISegmentedProfile median = d.getCollection()
				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		
		assertTrue(median.hasSegments() && median.getSegmentCount()>1);	
	}
	
	

}
