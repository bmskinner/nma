package com.bmskinner.nuclear_morphology.analysis.profiles;

import static org.junit.Assert.*;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.TestDatasetFactory;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * Test the ability of the segmenter to properly segment profiles
 * @author bms41
 * @since 1.14.0
 *
 */
public class ProfileSegmenterTest {
	
	@Before
	public void setUp(){
		Logger logger = Logger.getLogger(Loggable.PROGRAM_LOGGER);
		logger.setLevel(Level.FINE);
		logger.addHandler(new ConsoleHandler(new LogPanelFormatter()));
	}

	@Test
	public void testSingleCellSquareDatasetHasFourSegmentsWithNoForcing() throws Exception {

		IAnalysisDataset d = new TestDatasetBuilder().cellCount(1).ofType(NucleusType.ROUND)
				.baseHeight(40).baseWidth(40).build();
		new DatasetProfilingMethod(d).call();

		IProfile median = d.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		
		ProfileSegmenter segmenter = new ProfileSegmenter(median);
		
		System.out.println(median.toString());
		
		List<IBorderSegment> segments = segmenter.segment();
		for(IBorderSegment s : segments) {
			System.out.println(s.toString());
		}
		assertEquals(4, segments.size());
	}

}
