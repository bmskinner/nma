package com.bmskinner.nuclear_morphology.charting;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.junit.Ignore;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter.ColourSwatch;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import com.bmskinner.nuclear_morphology.visualisation.charts.ProfileChartFactory;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptionsBuilder;

/**
 * Test the profile charting functions
 * @author bms41
 * @since 1.14.0
 *
 */
@Ignore
public class ProfileChartFactoryTest extends ChartFactoryTest {
	
	private static final boolean IS_FIXED_ASPECT = false;
	
	
	/**
	 * Create charts with a default range of options variables for the given dataset
	 * @param d
	 * @throws InterruptedException
	 */
	private void generateChartsforOptions(IAnalysisDataset d, String title) throws InterruptedException {
		List<IAnalysisDataset> list = new ArrayList<>();
		list.add(d);
		generateChartsforOptions(list, title);
	}
	
	/**
	 * Create charts with a default range of options variables for the given datasets
	 * @param datasets
	 * @throws InterruptedException
	 */
	private void generateChartsforOptions(List<IAnalysisDataset> datasets, String title) throws InterruptedException {
		List<JPanel> panels = new ArrayList<>();
		for(Landmark tag : Landmark.defaultValues()) {
			ChartOptions options = new ChartOptionsBuilder().setDatasets(datasets)
					.setLandmark(tag)
					.setShowMarkers(true)
					.setNormalised(true)
					.build();
			panels.add(makeChartPanel(new ProfileChartFactory(options).createProfileChart(), options, "Tag: "+tag, IS_FIXED_ASPECT));
		}
		
		for(ProfileAlignment p : ProfileAlignment.values()) {
			ChartOptions options = new ChartOptionsBuilder().setDatasets(datasets)
					.setAlignment(p)
					.setNormalised(false)
					.build();
			panels.add(makeChartPanel(new ProfileChartFactory(options).createProfileChart(), options, "Alignment: "+p, IS_FIXED_ASPECT));
		}
		
		for(ColourSwatch p : ColourSwatch.values()) {
			ChartOptions options = new ChartOptionsBuilder().setDatasets(datasets)
					.setSwatch(p)
					.build();
			panels.add(makeChartPanel(new ProfileChartFactory(options).createProfileChart(), options, "Swatch: "+p, IS_FIXED_ASPECT));
		}
		ChartOptions trueOptions = new ChartOptionsBuilder().setDatasets(datasets)
				.setNormalised(true)
				.setAlignment(ProfileAlignment.LEFT)
				.setLandmark(Landmark.REFERENCE_POINT)
				.setShowMarkers(true)
				.setProfileType(ProfileType.ANGLE)
				.setSwatch(ColourSwatch.REGULAR_SWATCH)
				.setShowAnnotations(true)
				.setShowPoints(true)
				.setShowXAxis(true)
				.setShowYAxis(true)
				.build();

		panels.add(makeChartPanel(new ProfileChartFactory(trueOptions).createProfileChart(), trueOptions, "All true", IS_FIXED_ASPECT));
		
		ChartOptions falseOptions = new ChartOptionsBuilder().setDatasets(datasets)
				.setNormalised(false)
				.setAlignment(ProfileAlignment.LEFT)
				.setLandmark(Landmark.REFERENCE_POINT)
				.setShowMarkers(false)
				.setProfileType(ProfileType.ANGLE)
				.setSwatch(ColourSwatch.REGULAR_SWATCH)
				.setShowIQR(false)
				.setShowAnnotations(false)
				.setShowPoints(false)
				.setShowXAxis(false)
				.setShowYAxis(false)
				.build();

		panels.add(makeChartPanel(new ProfileChartFactory(falseOptions).createProfileChart(), falseOptions, "All false", IS_FIXED_ASPECT));
		showCharts(panels, title);
	}
	
	@Test
	public void testSingleNucleusProfile() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder()
				.cellCount(1)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.baseHeight(40)
				.baseWidth(40)
				.build();
		ICell c = d.getCollection().getCells().stream().findFirst().get();
		d.setDatasetColour(Color.BLUE);

		List<JPanel> panels = new ArrayList<>();
		
		ChartOptions trueOptions = new ChartOptionsBuilder().setDatasets(d)
				.setCell(c)
				.setNormalised(true)
				.setAlignment(ProfileAlignment.LEFT)
				.setLandmark(Landmark.REFERENCE_POINT)
				.setShowMarkers(true)
				.setProfileType(ProfileType.ANGLE)
				.setSwatch(ColourSwatch.REGULAR_SWATCH)
				.setShowAnnotations(true)
				.setShowPoints(true)
				.setShowXAxis(true)
				.setShowYAxis(true)
				.build();

		panels.add(makeChartPanel(new ProfileChartFactory(trueOptions).createProfileChart(), trueOptions, "All true", IS_FIXED_ASPECT));
		
		ChartOptions falseOptions = new ChartOptionsBuilder().setDatasets(d)
				.setCell(c)
				.setNormalised(false)
				.setAlignment(ProfileAlignment.LEFT)
				.setLandmark(Landmark.REFERENCE_POINT)
				.setShowMarkers(false)
				.setProfileType(ProfileType.ANGLE)
				.setSwatch(ColourSwatch.REGULAR_SWATCH)
				.setShowAnnotations(false)
				.setShowPoints(false)
				.setShowXAxis(false)
				.setShowYAxis(false)
				.build();

		panels.add(makeChartPanel(new ProfileChartFactory(falseOptions).createProfileChart(), falseOptions, "All false", IS_FIXED_ASPECT));
		showCharts(panels, "Single nucleus, no dataset");
	}
	
	@Test
	public void testSingleNucleusDatasetProfileWithSingleSegment() throws Exception {
		
		IAnalysisDataset d = new TestDatasetBuilder().cellCount(1).ofType(RuleSetCollection.roundRuleSetCollection())
				.profiled().build();
		generateChartsforOptions(d, "Single nucleus dataset, no segments");
	}
	
	@Test
	public void testSingleNucleusDatasetProfileWithMultipleSegments() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder().cellCount(1).ofType(RuleSetCollection.roundRuleSetCollection())
				.segmented().build();
		generateChartsforOptions(d, "Single nucleus, segmented");
	}

	@Test
	public void testMultipleNucleusDatasetProfileWithSingleSegment() throws Exception {
		
		IAnalysisDataset d = new TestDatasetBuilder().cellCount(100).withMaxSizeVariation(4).profiled().build();
		generateChartsforOptions(d, "Single dataset, multiple nuclei, single segment");
	}
	
	@Test
	public void testMultipleNucleusDatasetProfileWithMultipleSegments() throws Exception {
		
		IAnalysisDataset d = new TestDatasetBuilder().cellCount(100).withMaxSizeVariation(4).segmented().build();
		generateChartsforOptions(d, "Single dataset, multiple nuclei, multiple segments");
	}
	
	@Test
	public void testMultipleDatasetsProfileWithSingleSegment() throws Exception {
		IAnalysisDataset d1 = new TestDatasetBuilder().cellCount(100).withMaxSizeVariation(4).profiled().build();
		IAnalysisDataset d2 = new TestDatasetBuilder().cellCount(100).withMaxSizeVariation(10).profiled().build();
		
		List<IAnalysisDataset> list = new ArrayList<>();
		list.add(d1);
		list.add(d2);
		
		generateChartsforOptions(list, "Multiple datasets, multiple nuclei, single segment");
	}

}
