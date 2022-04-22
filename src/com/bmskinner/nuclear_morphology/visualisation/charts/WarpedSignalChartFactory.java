package com.bmskinner.nuclear_morphology.visualisation.charts;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.components.signals.IWarpedSignal;
import com.bmskinner.nuclear_morphology.visualisation.datasets.ChartDatasetCreationException;
import com.bmskinner.nuclear_morphology.visualisation.datasets.ComponentOutlineDataset;
import com.bmskinner.nuclear_morphology.visualisation.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;

import ij.process.ImageProcessor;

public class WarpedSignalChartFactory extends OutlineChartFactory {

	private static final String ERROR_CREATING_MESH_MSG = "Error creating mesh";

	private static final Logger LOGGER = Logger.getLogger(OutlineChartFactory.class.getName());

	protected static final String NO_CONSENSUS_ERROR_LBL = "No consensus nucleus in dataset";

	public WarpedSignalChartFactory(@NonNull ChartOptions o) {
		super(o);
	}

	public JFreeChart makeSignalWarpChart() {

		ImageProcessor image = createDisplayImage();
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
		JFreeChart chart = new ConsensusNucleusChartFactory(options).makeNucleusBareOutlineChart();

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

	/**
	 * Create an image for display. This applies thresholding and pseudocolouring
	 * options.
	 * 
	 * @return
	 */
	private synchronized ImageProcessor createDisplayImage() {

		if (options.getWarpedSignals().isEmpty())
			return ImageFilterer.createWhiteByteProcessor(100, 100);

		// Recolour each of the grey images according to the stored colours
		List<ImageProcessor> recoloured = isCommonTargetSelected() ? recolourImagesWithSameTarget()
				: recolourImagesWithDifferentTargets();

		if (recoloured.size() == 1)
			return recoloured.get(0);

		// If multiple images are in the list, make an blend of their RGB
		// values so territories can be compared
		try {
			ImageProcessor ip1 = recoloured.get(0);
			for (int i = 1; i < recoloured.size(); i++) {
				// Weighting by fractions reduces intensity across the image
				// Weighting by integer multiples washes the image out.
				// Since there is little benefit to 3 or more blended,
				// just keep equal weighting
//        		float weight1 = i/(i+1);
//        		float weight2 = 1-weight1; 

				ip1 = ImageFilterer.blendImages(ip1, 1, recoloured.get(i), 1);
			}
			return ip1;

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error averaging images", e);
			return ImageFilterer.createWhiteByteProcessor(100, 100);
		}
	}

	private synchronized List<ImageProcessor> recolourImagesWithSameTarget() {
		List<ImageProcessor> recoloured = new ArrayList<>();
		for (IWarpedSignal k : options.getWarpedSignals()) {
			// The image from the warper is greyscale. Change to use the signal colour
			ImageProcessor bp = k.toImage().convertToByteProcessor();
			bp.invert();

			ImageProcessor recol = bp;
			if (k.isPseudoColour())
				recol = ImageFilterer.recolorImage(bp, k.colour());
			else
				recol = bp.convertToColorProcessor();

			recol.setMinAndMax(0, k.displayThreshold());
			recoloured.add(recol);
		}
		return recoloured;
	}

	private synchronized List<ImageProcessor> recolourImagesWithDifferentTargets() {

		List<ImageProcessor> images = ImageFilterer
				.fitToCommonCanvas(options.getWarpedSignals().stream().map(IWarpedSignal::toImage).toList());

		Map<IWarpedSignal, ImageProcessor> map = new HashMap<>();
		for (int i = 0; i < images.size(); i++)
			map.put(options.getWarpedSignals().get(i), images.get(i));

		List<ImageProcessor> recoloured = new ArrayList<>();

		for (Entry<IWarpedSignal, ImageProcessor> e : map.entrySet()) {

			// The image from the warper is greyscale. Change to use the signal colour
			ImageProcessor bp = e.getValue().convertToByteProcessor();
			bp.invert();

			ImageProcessor recol = bp;
			if (e.getKey().isPseudoColour())
				recol = ImageFilterer.recolorImage(bp, e.getKey().colour());
			else
				recol = bp.convertToColorProcessor();

			recol.setMinAndMax(0, e.getKey().displayThreshold());
			recoloured.add(recol);

		}
		return recoloured;
	}
}
