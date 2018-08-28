package com.bmskinner.nuclear_morphology.charting;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.charting.charts.OutlineChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ProfileChartFactory;
import com.bmskinner.nuclear_morphology.charting.datasets.ChartDatasetCreationException;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.generic.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter.ColourSwatch;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;

/**
 * Test that individual components are drawn correctly in outline charts.
 * @author bms41
 * @since 1.14.0
 *
 */
public class OutlineChartFactoryTest extends ChartFactoryTest {
		
	private static final boolean IS_FIXED_ASPECT = true;
	
	/**
	 * Create charts with a default range of options variables for the given datasets
	 * @param datasets
	 * @throws InterruptedException
	 */
	private void generateChartsForAllCells(IAnalysisDataset dataset, String title) throws InterruptedException {
		List<JPanel> panels = new ArrayList<>();
		
		
		for(ICell cell : dataset.getCollection().getCells()) {
			ChartOptions options = new ChartOptionsBuilder().setDatasets(dataset)
					.setCell(cell)
					.build();
			JPanel outline = makeChartPanel(new OutlineChartFactory(options).makeCellOutlineChart(), options, title, IS_FIXED_ASPECT);
			
			// show the profile corresponding to the chart
			ChartOptions profileOptions = new ChartOptionsBuilder().setDatasets(dataset)
					.setCell(cell)
					.setTag(Tag.REFERENCE_POINT)
					.setShowMarkers(true)
					.setShowAnnotations(true)
					.setProfileType(ProfileType.ANGLE)
					.build();
			JPanel profile = makeChartPanel(new ProfileChartFactory(profileOptions).createProfileChart(), profileOptions, "Profile", false);
			
			JPanel bothPanel = new JPanel();
			bothPanel.add(outline);
			bothPanel.add(profile);

			panels.add(bothPanel);
		}
		
		showCharts(panels, title);
	}
	
	@Test
	public void testOutlineChartOfSingleCellNonSegmentedDataset() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(12345).cellCount(1)
				.baseHeight(40).baseWidth(40)
				.ofType(NucleusType.ROUND)
				.profiled().build();
		generateChartsForAllCells(d, "Single nucleus, square, not segmented");
	}
	
	@Test
	public void testOutlineChartOfSingleCellSegmentedDataset() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(12345).cellCount(1)
				.baseHeight(40).baseWidth(40)
				.ofType(NucleusType.ROUND)
				.segmented().build();
		generateChartsForAllCells(d, "Single nucleus, square, segmented");
	}
	
	@Test
	public void testOutlineChartOfMultiCellNonSegmentedDataset() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(1234).cellCount(10)
				.baseHeight(40).baseWidth(40)
				.ofType(NucleusType.ROUND)
				.profiled().build();
		generateChartsForAllCells(d, "Multiple nuclei, square, not segmented");
	}
	
	@Test
	public void testOutlineChartOfMultiCellSegmentedDataset() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(1234).cellCount(10)
				.baseHeight(40).baseWidth(40)
				.ofType(NucleusType.ROUND)
				.withMaxSizeVariation(10)
				.segmented().build();
		generateChartsForAllCells(d, "Multiple nuclei, square, segmented");
	}
	
	@Test
	public void testOutlineChartOfRotatedMultiCellSegmentedDataset() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(12345).cellCount(20)
				.baseHeight(40).baseWidth(40)
				.ofType(NucleusType.ROUND)
				.maxRotation(90)
				.segmented().build();
		generateChartsForAllCells(d, "Multiple nuclei, square, rotated, segmented");
	}

}
