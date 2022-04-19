package com.bmskinner.nuclear_morphology.charting;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.visualisation.charts.OutlineChartFactory;
import com.bmskinner.nuclear_morphology.visualisation.charts.ProfileChartFactory;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptionsBuilder;

/**
 * Create the outline charts for test cases
 * @author bms41
 * @since 1.15.0
 *
 */
public class OutlineTestChartFactory extends ChartFactoryTest {
	
	private static final boolean IS_FIXED_ASPECT = true;
	
	/**
	 * Create a panel containing an outline chart for the given 
	 * @param dataset the dataset containing the cell
	 * @param cell the cell to draw the outline for
	 * @return a panel with the desired outline chart
	 * @throws InterruptedException
	 */
	public static JPanel generateOutlineChart(@NonNull IAnalysisDataset dataset, @NonNull ICell cell) throws InterruptedException {
		String title = new Exception().getStackTrace()[0].getMethodName();
		ChartOptions options = new ChartOptionsBuilder().setDatasets(dataset)
				.setCell(cell)
				.addCellularComponent(cell.getPrimaryNucleus())
				.build();
		return makeChartPanel(new OutlineChartFactory(options).makeCellOutlineChart(), options, title, IS_FIXED_ASPECT);
	}
	
	public static void showOutlineChart(@NonNull IAnalysisDataset dataset, @NonNull ICell cell) throws InterruptedException {
		List<JPanel> panels = new ArrayList<>();
		panels.add(generateOutlineChart(dataset, cell));
		showCharts(panels, "Outline");
	}
	
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
					.setLandmark(Landmark.REFERENCE_POINT)
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

}
