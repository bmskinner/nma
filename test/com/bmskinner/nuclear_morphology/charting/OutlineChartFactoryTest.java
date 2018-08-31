package com.bmskinner.nuclear_morphology.charting;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.charting.charts.OutlineChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ProfileChartFactory;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;

/**
 * Test that individual components are drawn correctly in outline charts.
 * @author bms41
 * @since 1.14.0
 *
 */
public class OutlineChartFactoryTest extends ChartFactoryTest {
		
	private static final boolean IS_FIXED_ASPECT = true;
	
	/**
	 * Create and display outline and profile charts for all cells in the dataset
	 * @param dataset
	 * @param title
	 * @throws InterruptedException
	 */
	public static void generateOutlineChartsForAllCells(@NonNull IAnalysisDataset dataset, @Nullable String title) throws InterruptedException {
		if(title==null)
			title = new Exception().getStackTrace()[0].getMethodName();
		
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
		generateOutlineChartsForAllCells(d, null);
	}
	
	@Test
	public void testOutlineChartOfSingleCellSegmentedDataset() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(12345).cellCount(1)
				.baseHeight(40).baseWidth(40)
				.ofType(NucleusType.ROUND)
				.segmented().build();
		generateOutlineChartsForAllCells(d, null);
	}
	
	@Test
	public void testOutlineChartOfMultiCellNonSegmentedDataset() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(1234).cellCount(10)
				.baseHeight(40).baseWidth(40)
				.ofType(NucleusType.ROUND)
				.profiled().build();
		generateOutlineChartsForAllCells(d, null);
	}
	
	@Test
	public void testOutlineChartOfMultiCellSegmentedDataset() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(1234).cellCount(10)
				.baseHeight(40).baseWidth(40)
				.ofType(NucleusType.ROUND)
				.withMaxSizeVariation(10)
				.segmented().build();
		generateOutlineChartsForAllCells(d, null);
	}
	
	@Test
	public void testOutlineChartOfRotatedMultiCellSegmentedDataset() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(12345).cellCount(20)
				.baseHeight(40).baseWidth(40)
				.ofType(NucleusType.ROUND)
				.maxRotation(90)
				.segmented().build();
		generateOutlineChartsForAllCells(d, null);
	}

}
