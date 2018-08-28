package com.bmskinner.nuclear_morphology.analysis.profiles;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.charting.ChartFactoryTest;
import com.bmskinner.nuclear_morphology.charting.charts.ProfileChartFactory;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.SegmentedFloatProfile;
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
public class ProfileSegmenterTest extends ChartFactoryTest {
	
	@Before
	public void setUp(){
		Logger logger = Logger.getLogger(Loggable.PROGRAM_LOGGER);
		logger.setLevel(Level.FINE);
		logger.addHandler(new ConsoleHandler(new LogPanelFormatter()));
	}

	@Test
	public void testSingleCellSquareDatasetSegmentation() throws Exception {

		IAnalysisDataset d = new TestDatasetBuilder(12345).cellCount(1).ofType(NucleusType.ROUND)
				.baseHeight(40).baseWidth(40).build();
		new DatasetProfilingMethod(d).call();

		IProfile median = d.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		
		ProfileSegmenter segmenter = new ProfileSegmenter(median);
				
		List<IBorderSegment> segments = segmenter.segment();

		ISegmentedProfile p = new SegmentedFloatProfile(median, segments);
		
		d.getCollection().getProfileCollection().addSegments(segments);
		
		ChartOptions options = new ChartOptionsBuilder().setDatasets(d)
				.setShowAnnotations(true)
				.build();
		showSingleChart(new ProfileChartFactory(options).createProfileChart(), options, "Single cell", false);
	}
	
	@Test
	public void testMultiCellSquareDatasetSegmentation() throws Exception {

		IAnalysisDataset d = new TestDatasetBuilder(12345).cellCount(50).ofType(NucleusType.ROUND)
				.baseHeight(40).baseWidth(40).profiled().build();

		IProfile median = d.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		
		ProfileSegmenter segmenter = new ProfileSegmenter(median);
				
		List<IBorderSegment> segments = segmenter.segment();

		ISegmentedProfile p = new SegmentedFloatProfile(median, segments);
		
		d.getCollection().getProfileCollection().addSegments(segments);
		
		ChartOptions options = new ChartOptionsBuilder().setDatasets(d)
				.setShowAnnotations(true)
				.build();
		showSingleChart(new ProfileChartFactory(options).createProfileChart(), options, "Multiple identical cells", false);
	}
	
	@Test
	public void testMultiCellVariableSquareDatasetSegmentation() throws Exception {

		IAnalysisDataset d = new TestDatasetBuilder(12345).cellCount(50).ofType(NucleusType.ROUND)
				.withMaxSizeVariation(20)
				.baseHeight(40).baseWidth(40).build();
		new DatasetProfilingMethod(d).call();

		IProfile median = d.getCollection()
				.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		
		ProfileSegmenter segmenter = new ProfileSegmenter(median);
				
		List<IBorderSegment> segments = segmenter.segment();

		ISegmentedProfile p = new SegmentedFloatProfile(median, segments);
		
		d.getCollection().getProfileCollection().addSegments(segments);
		
		ChartOptions options = new ChartOptionsBuilder().setDatasets(d)
				.setShowAnnotations(true)
				.build();
		showSingleChart(new ProfileChartFactory(options).createProfileChart(), options, "Multiple variable cells", false);
	}

}
