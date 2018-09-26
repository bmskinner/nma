package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.charting.ChartFactoryTest;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;
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
	private Logger logger;
	@Before
	public void setUp(){
		logger = Logger.getLogger(Loggable.PROGRAM_LOGGER);
		logger.setLevel(Level.FINE);
		logger.addHandler(new ConsoleHandler(new LogPanelFormatter()));
	}
	
	private void segmentMedianProfile(IAnalysisDataset d) throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException, Exception {
		IProfile median = d.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		
		ProfileSegmenter segmenter = new ProfileSegmenter(median);
				
		List<IBorderSegment> segments = segmenter.segment();
		
		d.getCollection().getProfileCollection().addSegments(segments);
	}


	@Test
	public void testSingleCellSquareDatasetSegmentation() throws Exception {

		IAnalysisDataset d = new TestDatasetBuilder(1234).cellCount(1)
				.ofType(NucleusType.ROUND)
				.baseHeight(40).baseWidth(40)
				.profiled().build();

		segmentMedianProfile(d);
//		ChartFactoryTest.showMedianProfile(d, "Single cell dataset segmented");
	}
	
	@Test
	public void testMultiCellSquareDatasetSegmentation() throws Exception {

		IAnalysisDataset d = new TestDatasetBuilder(1234).cellCount(50)
				.ofType(NucleusType.ROUND)
				.baseHeight(40).baseWidth(40)
				.profiled().build();

		segmentMedianProfile(d);
//		ChartFactoryTest.showMedianProfile(d, "Multiple identical cells");
	}
	
	@Test
	public void testMultiCellVariableSquareDatasetSegmentation() throws Exception {

		IAnalysisDataset d = new TestDatasetBuilder(1234).cellCount(50).ofType(NucleusType.ROUND)
				.withMaxSizeVariation(20)
				.randomOffsetProfiles(false)
				.baseHeight(40).baseWidth(40).profiled().build();

		segmentMedianProfile(d);
		ChartFactoryTest.showMedianProfile(d, "Multiple variable cells");
	}
	
	@Test
	public void testMultiCellVariableOffsetSquareDatasetSegmentation() throws Exception {

		IAnalysisDataset d = new TestDatasetBuilder(1234).cellCount(50).ofType(NucleusType.ROUND)
				.withMaxSizeVariation(20)
				.randomOffsetProfiles(true)
				.baseHeight(40).baseWidth(40).profiled().build();

		segmentMedianProfile(d);
//		ChartFactoryTest.showMedianProfile(d, "Multiple variable cells with offsets");
	}
	
	@Test
	public void testMultiCellVariableOffsetSquareDatasetSegmentationAndRotation() throws Exception {

		IAnalysisDataset d = new TestDatasetBuilder(1234).cellCount(50).ofType(NucleusType.ROUND)
				.withMaxSizeVariation(20)
				.maxRotation(270)
				.randomOffsetProfiles(true)
				.baseHeight(40).baseWidth(40).profiled().build();

		segmentMedianProfile(d);
//		ChartFactoryTest.showMedianProfile(d, "Multiple variable cells with offsets and rotation");
	}
	
	@Test
	public void testSegmentationOfRodentDataset() throws Exception{
		File f = new File(SampleDatasetReader.SAMPLE_DATASET_PATH, "Unsegmented_mouse.nmd");
		IAnalysisDataset dataset = SampleDatasetReader.openDataset(f);
		ISegmentedProfile template = dataset.getCollection()
				.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);

		RepresentativeMedianFinder finder = new RepresentativeMedianFinder(dataset.getCollection());				
		IProfile result = finder.findMedian();
		ProfileSegmenter segmenter = new ProfileSegmenter(result);
		
		List<IBorderSegment> segments = segmenter.segment();
		for(IBorderSegment s : segments) {
			System.out.println(s.getDetail());
		}
	}

}
