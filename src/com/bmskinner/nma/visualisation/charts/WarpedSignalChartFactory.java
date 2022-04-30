package com.bmskinner.nma.visualisation.charts;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.signals.IWarpedSignal;
import com.bmskinner.nma.visualisation.datasets.ChartDatasetCreationException;
import com.bmskinner.nma.visualisation.datasets.ComponentOutlineDataset;
import com.bmskinner.nma.visualisation.image.ImageAnnotator;
import com.bmskinner.nma.visualisation.options.ChartOptions;

import ij.process.ImageProcessor;

public class WarpedSignalChartFactory extends OutlineChartFactory {

	private static final String ERROR_CREATING_MESH_MSG = "Error creating mesh";

	private static final Logger LOGGER = Logger.getLogger(OutlineChartFactory.class.getName());

	protected static final String NO_CONSENSUS_ERROR_LBL = "No consensus nucleus in dataset";

	public WarpedSignalChartFactory(@NonNull ChartOptions o) {
		super(o);
	}

	public JFreeChart makeSignalWarpChart() {
		ImageProcessor image = ImageAnnotator.createMergedWarpedSignals(options.getWarpedSignals());
		return makeSignalWarpChart(image);
	}

	/**
	 * Draw the given images onto a consensus outline nucleus.
	 * 
	 * @param image the image processor to be drawn
	 * @return
	 */
	private JFreeChart makeSignalWarpChart(ImageProcessor image) {

		// Create the outline of the nucleus
		JFreeChart chart = createEmptyChart();

//		JFreeChart chart = new ConsensusNucleusChartFactory(options).makeNucleusBareOutlineChart();

		XYPlot plot = chart.getXYPlot();

		LOGGER.finer("Creating outline datasets");

		// Make outline of the components to draw
		List<XYDataset> outlineDatasets = new ArrayList<>();

		List<CellularComponent> components = new ArrayList<>();
		if (isCommonTargetSelected()) {
			components.add(getCommonSelectedTarget());
		} else {
			for (IWarpedSignal s : options.getWarpedSignals())
				components.add(s.target());
		}

		try {
			for (CellularComponent c : components) {
				outlineDatasets.add(new ComponentOutlineDataset(c, false, options.getScale()));
			}
			LOGGER.finer(String.format("Image bounds: %s x %s", image.getWidth(), image.getHeight()));
		} catch (ChartDatasetCreationException e) {
			LOGGER.log(Level.SEVERE, "Error creating outline", e);
			return createErrorChart();
		}

		// Calculate the offset at which to draw the image since
		// the plot area is larger than the image to be drawn
		double xChartMin = Double.MAX_VALUE;
		double yChartMin = Double.MAX_VALUE;
		for (XYDataset ds : outlineDatasets) {
			xChartMin = Math.min(xChartMin, DatasetUtils.findMinimumDomainValue(ds).doubleValue());
			yChartMin = Math.min(yChartMin, DatasetUtils.findMinimumRangeValue(ds).doubleValue());
		}

		// Get the max bounding box size for the consensus nuclei,
		// to find the offsets for the images created
		int xOffset = (int) Math.round(-xChartMin);
		int yOffset = (int) Math.round(-yChartMin);

		LOGGER.finer("Adding image as annotation with offset " + xOffset + " - " + yOffset);
		drawImageAsAnnotation(image, plot, 255, -xOffset, -yOffset, options.isShowBounds());

		// Set the colour of the nucleus outline
		plot.getRenderer().setDefaultPaint(Color.BLACK);
		plot.getRenderer().setDefaultSeriesVisible(true);
		for (int i = 0; i < outlineDatasets.size(); i++) {
			plot.setDataset(i, outlineDatasets.get(i));
			plot.getRenderer().setSeriesPaint(i, Color.black);
			plot.getRenderer().setSeriesVisible(i, true);
		}

		applyDefaultAxisOptions(chart);

		return chart;
	}

	/**
	 * Get the target shape in common to all selected keys
	 * 
	 * @return
	 */
	private synchronized CellularComponent getCommonSelectedTarget() {
		if (!isCommonTargetSelected())
			return null;
		return options.getWarpedSignals().stream().findFirst().get().target();
	}

	/**
	 * Test if all the selected visible keys have the same target
	 * 
	 * @return
	 */
	private synchronized boolean isCommonTargetSelected() {
		Nucleus t = options.getWarpedSignals().stream().findFirst().get().target();
		return options.getWarpedSignals().stream().allMatch(s -> s.target().getID().equals(t.getID()));
	}
}
