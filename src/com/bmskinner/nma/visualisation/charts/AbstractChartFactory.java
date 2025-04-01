/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.visualisation.charts;

import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.Layer;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nma.components.ComponentOrienter;
import com.bmskinner.nma.components.Imageable;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.gui.RotationMode;
import com.bmskinner.nma.gui.components.ColourSelecter;
import com.bmskinner.nma.io.ImageImporter;
import com.bmskinner.nma.io.UnloadableImageException;
import com.bmskinner.nma.visualisation.ChartComponents;
import com.bmskinner.nma.visualisation.datasets.NucleusDatasetCreator;
import com.bmskinner.nma.visualisation.image.ImageConverter;
import com.bmskinner.nma.visualisation.options.ChartOptions;

import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/**
 * Base class for chart generation. Contains static methods to create loading
 * and error charts. All chart factories should extend this class.
 * 
 * @author Ben Skinner
 *
 */
public abstract class AbstractChartFactory {

	private static final Logger LOGGER = Logger.getLogger(AbstractChartFactory.class.getName());

	/** The X and Y axis positive & negative range magnitude for empty charts */
	protected static final int DEFAULT_EMPTY_RANGE = 10;

	/**
	 * The index profile charts begin at. Since the first index is 0, this prevents
	 * the value at zero being hidden by the chart border
	 */
	protected static final int DEFAULT_PROFILE_START_INDEX = -1;

	private static final String CHART_LOADING_LBL = "Loading...";
	private static final String MULTI_DATASET_ERROR_LBL = "Cannot display multiple datasets";
	private static final String GENERAL_ERROR_LBL = "Error creating chart";

	protected static final boolean DEFAULT_CREATE_TOOLTIPS = false;
	protected static final boolean DEFAULT_CREATE_LEGEND = false;
	protected static final boolean DEFAULT_CREATE_URLS = false;

	/** The options that will be used for chart generation */
	protected final @NonNull ChartOptions options;

	/**
	 * Create with options for the chart to be created
	 * 
	 * @param o the options
	 */
	protected AbstractChartFactory(@NonNull final ChartOptions o) {
		options = o;
	}

	/**
	 * Creates an empty chart with the default range
	 * 
	 * @return an empty chart
	 */
	@NonNull
	public static JFreeChart createEmptyChart() {
		JFreeChart c = createBaseXYChart();
		XYPlot plot = c.getXYPlot();

		plot.getDomainAxis().setRange(-DEFAULT_EMPTY_RANGE, DEFAULT_EMPTY_RANGE);
		plot.getRangeAxis().setRange(-DEFAULT_EMPTY_RANGE, DEFAULT_EMPTY_RANGE);

		plot.getDomainAxis().setVisible(false);
		plot.getRangeAxis().setVisible(false);
		return c;
	}

	/**
	 * Creates an empty chart with a message in the centre
	 * 
	 * @param labelText the text to display
	 * @return a chart with the given message
	 */
	protected static JFreeChart createTextAnnotatedEmptyChart(String labelText) {
		JFreeChart chart = createEmptyChart();
		XYTextAnnotation annotation = new XYTextAnnotation(labelText, 0, 0);
		annotation.setPaint(Color.BLACK);
		chart.getXYPlot().addAnnotation(annotation);
		return chart;
	}

	/**
	 * Creates an empty chart with a message that a further chart is loading
	 * 
	 * @return
	 */
	public static JFreeChart createLoadingChart() {
		return createTextAnnotatedEmptyChart(CHART_LOADING_LBL);
	}

	/**
	 * Creates an empty chart with a message that multiple datasets cannot be
	 * displayed in this chart type.
	 * 
	 * @return
	 */
	public static JFreeChart createMultipleDatasetEmptyChart() {
		return createTextAnnotatedEmptyChart(MULTI_DATASET_ERROR_LBL);
	}

	/**
	 * Create a chart displaying an error message
	 * 
	 * @return
	 */
	public static JFreeChart createErrorChart() {
		return createTextAnnotatedEmptyChart(GENERAL_ERROR_LBL);
	}

	/**
	 * Get a series or dataset index for colour selection when drawing charts. The
	 * index is set in the DatasetCreator as part of the label. The format is
	 * Name_index_other
	 * 
	 * @param label the label to extract the index from
	 * @return the index found
	 */
	public static int getIndexFromLabel(String label) {
		String[] names = label.split("_");
		return Integer.parseInt(names[1]);
	}

	/**
	 * Get the UUID of a signal group from a label. The expected format is
	 * CellularComponent.NUCLEAR_SIGNAL+"_"+UUID+"_signal_"+signalNumber
	 * 
	 * @param label the charting dataset label
	 * @return
	 */
	public static UUID getSignalGroupFromLabel(String label) {

		if (label.startsWith(CellularComponent.NUCLEAR_SIGNAL)) {
			String[] names = label.split("_");
			return UUID.fromString(names[1]);
		}
		throw new IllegalArgumentException(
				"Label does not start with CellularComponent.NUCLEAR_SIGNAL");

	}

	/**
	 * Draw a domain marker - a vertical line - for the given border tag at the
	 * given position
	 * 
	 * @param plot  the plot
	 * @param tag   the tag to use for colour selection
	 * @param value the domain axis value to draw at
	 */
	protected void addDomainMarkerToXYPlot(final XYPlot plot, final Landmark tag,
			final double value, double yval) {
		double range = plot.getRangeAxis().getRange().getLength();
		double minY = plot.getRangeAxis().getRange().getLowerBound();
		plot.addAnnotation(new XYTextAnnotation(tag.getName(), value, minY + (range * 0.1)), false);
		plot.addAnnotation(new XYLineAnnotation(value, minY + (range * 0.15), value, yval,
				ChartComponents.MARKER_STROKE, Color.GRAY));
	}

	/**
	 * Create a new XY line Chart, with vertical orientation, and set the background
	 * to white.
	 * 
	 * @param xLabel the x axis label
	 * @param yLabel the y axis label
	 * @param ds     the charting dataset
	 * @return a chart with default settings
	 */
	protected static JFreeChart createBaseXYChart(final String xLabel, final String yLabel,
			final XYDataset ds) {
		JFreeChart chart = ChartFactory.createXYLineChart(null, xLabel, yLabel, ds,
				PlotOrientation.VERTICAL, false,
				false, false);

		XYPlot plot = chart.getXYPlot();
		plot.setBackgroundPaint(Color.WHITE);

		plot.getRenderer().setDefaultToolTipGenerator(null);
		plot.getRenderer().setURLGenerator(null);
		plot.getRenderer().setDefaultCreateEntities(false);
		chart.setAntiAlias(GlobalOptions.getInstance().isAntiAlias()); // disabled for performance
																		// testing
		return chart;
	}

	/**
	 * Create a new XY Line Chart, with vertical orientation, and set the background
	 * to white. The charting dataset is null.
	 * 
	 * @param xLabel the x axis label
	 * @param yLabel the y axis label
	 * @return a chart with default settings
	 */
	protected static JFreeChart createBaseXYChart(final String xLabel, final String yLabel) {
		return createBaseXYChart(xLabel, yLabel, null);
	}

	/**
	 * Create a new XY Line Chart, with vertical orientation, and set the background
	 * to white. The charting dataset is null.
	 * 
	 * @return
	 */
	protected static JFreeChart createBaseXYChart() {
		return createBaseXYChart(null, null, null);
	}

	/**
	 * Assuming there is a single XYDataset in the XYPlot of the chart, and a single
	 * renderer, apply dataset colours based on position in the chart options
	 * dataset list.
	 * 
	 * @param plot the plot to apply colours to
	 */
	protected void applySingleXYDatasetColours(final XYPlot plot) {
		int seriesCount = plot.getDataset().getSeriesCount();

		XYItemRenderer renderer = plot.getRenderer();
		for (int i = 0; i < seriesCount; i++) {
			Paint colour = options.getDatasets().get(i).getDatasetColour()
					.orElse(ColourSelecter.getColor(i));
			renderer.setSeriesPaint(i, colour);

		}
	}

	/**
	 * Set basic parameters such as: axes inverted, axes visible
	 * 
	 * @param chart
	 * @param options
	 */
	protected void applyDefaultAxisOptions(final JFreeChart chart) {

		Plot plot = chart.getPlot();

		if (plot instanceof XYPlot) {

			XYPlot xy = chart.getXYPlot();
			xy.getDomainAxis().setVisible(options.isShowXAxis());
			xy.getRangeAxis().setVisible(options.isShowYAxis());
			xy.getDomainAxis().setInverted(options.isInvertXAxis());
			xy.getRangeAxis().setInverted(options.isInvertYAxis());

		}

		if (plot instanceof CategoryPlot) {
			CategoryPlot cat = chart.getCategoryPlot();
			cat.getDomainAxis().setVisible(options.isShowXAxis());
			cat.getRangeAxis().setVisible(options.isShowYAxis());
			cat.getRangeAxis().setInverted(options.isInvertYAxis());

		}
	}

	/**
	 * Remove the XYShapeAnnotations from this image This will leave all other
	 * annotation types.
	 */
	protected static void clearShapeAnnotations(XYPlot plot) {
		for (Object a : plot.getAnnotations()) {
			if (a.getClass() == XYShapeAnnotation.class) {
				plot.removeAnnotation((XYAnnotation) a);
			}
		}
	}

	/**
	 * Create a chart with an image drawn as an annotation in the background layer.
	 * 
	 * @param ip      the image
	 * @param plot    the plot to draw on
	 * @param alpha   the opacity (0-255)
	 * @param xOffset a position to move the image 0,0 to
	 * @param yOffset a position to move the image 0,0 to
	 * @return
	 */
	protected void drawImageAsAnnotation(ImageProcessor ip, XYPlot plot, int alpha, int xOffset,
			int yOffset,
			boolean showBounds) {
		plot.setBackgroundPaint(Color.WHITE);
		plot.getRangeAxis().setInverted(false);

		// Make a dataset to allow the autoscale to work even if no other datasets are
		// present
		// Hide the dataset from visibility
		XYDataset bounds = new NucleusDatasetCreator(options).createAnnotationRectangleDataset(
				ip.getWidth(),
				ip.getHeight());
		plot.setDataset(0, bounds);
		XYItemRenderer rend = plot.getRenderer(0);
		rend.setDefaultSeriesVisible(false);

		plot.getDomainAxis().setRange(0, ip.getWidth());
		plot.getRangeAxis().setRange(0, ip.getHeight());

		for (int x = 0; x < ip.getWidth(); x++) {
			for (int y = 0; y < ip.getHeight(); y++) {

				int pixel = ip.get(x, y);
				Color col = null;

				if (ip instanceof ColorProcessor) {
					if (pixel < 16777215) {
						col = new Color(pixel);
						col = ColourSelecter.getTransparentColour(col, true, alpha);
					}

				} else {
					if (pixel < 255) // Ignore anything that is not signal - the background is
										// already white
						col = new Color(pixel, pixel, pixel, alpha);
				}

				if (col == null && showBounds) // Draw red pixels at bounds
					col = new Color(255, 0, 0, alpha);

				if (col != null) {
					// Ensure the 'pixels' overlap to avoid lines of background
					// colour seeping through
					Rectangle2D r = new Rectangle2D.Double(x + xOffset - 0.1, y + yOffset - 0.1,
							1.2, 1.2);
					XYShapeAnnotation a = new XYShapeAnnotation(r, null, null, col);

					rend.addAnnotation(a, Layer.BACKGROUND);
				}
			}
		}
	}

	/**
	 * Create a chart with an image drawn as an annotation in the background layer.
	 * The image pixels are fully opaque
	 * 
	 * @param ip
	 * @param alpha
	 * @return
	 */
	protected void drawImageAsAnnotation(ImageProcessor ip, XYPlot plot, int alpha) {
		drawImageAsAnnotation(ip, plot, alpha, 0, 0, false);
	}

	/**
	 * Create a chart with an image drawn as an annotation in the background layer.
	 * The image pixels are fully opaque
	 * 
	 * @param ip
	 * @return
	 */
	protected JFreeChart drawImageAsAnnotation(ImageProcessor ip) {
		return drawImageAsAnnotation(ip, 255);
	}

	/**
	 * Create a chart with an image drawn as an annotation in the background layer.
	 * The image pixels have the given alpha transparency value
	 * 
	 * @param ip
	 * @param alpha
	 * @return
	 */
	protected JFreeChart drawImageAsAnnotation(ImageProcessor ip, int alpha) {

		JFreeChart chart = ChartFactory.createXYLineChart(null, null, null, null,
				PlotOrientation.VERTICAL, true, true,
				false);

		XYPlot plot = chart.getXYPlot();
		drawImageAsAnnotation(ip, plot, alpha);
		return chart;
	}

	private static ImageProcessor importAndCropImage(@NonNull ICell cell,
			@NonNull CellularComponent component)
			throws UnloadableImageException {
		ImageConverter ic = new ImageConverter(ImageImporter.importFullImageTo8bit(component))
				.invert();
		ImageProcessor openProcessor = ic.convertToRGBGreyscale().toProcessor();

		Nucleus n = cell.getPrimaryNucleus();

		int xBase = n.getXBase();
		int yBase = n.getYBase();

		int padding = Imageable.COMPONENT_BUFFER;
		int wideW = (int) n.getWidth() + (padding * 2);
		int wideH = (int) n.getHeight() + (padding * 2);
		int wideX = xBase - padding;
		int wideY = yBase - padding;

		wideX = wideX < 0 ? 0 : wideX;
		wideY = wideY < 0 ? 0 : wideY;

		openProcessor.setRoi(wideX, wideY, wideW, wideH);
		return openProcessor.crop();
	}

	/**
	 * Create a rotation transform for the given nucleus
	 * 
	 * @param n
	 * @return
	 * @throws MissingLandmarkException
	 * @throws ComponentCreationException
	 */
	private static AffineTransform createRotationTransform(Nucleus n)
			throws MissingLandmarkException, ComponentCreationException {
		AffineTransform at = new AffineTransform();

		// The point to rotate about
		IPoint com = n.getCentreOfMass();
		double rads = Math.toRadians(360 - ComponentOrienter.calcAngleToAlignVertically(n));

		at.concatenate(AffineTransform.getTranslateInstance(com.getX(), com.getY()));
		if (ComponentOrienter.isFlipNeeded(n)) {
			at.concatenate(AffineTransform.getScaleInstance(-1, 1));
		}
		at.concatenate(AffineTransform.getRotateInstance(rads));
		at.concatenate(AffineTransform.getTranslateInstance(-com.getX(), -com.getY()));
		return at;

	}

	private static ImageProcessor importAndCropRotatedImage(@NonNull ICell cell,
			@NonNull CellularComponent component)
			throws UnloadableImageException, MissingLandmarkException, ComponentCreationException {

		// Image with black background, white signal
		ImageConverter ic = new ImageConverter(ImageImporter.importFullImageTo8bit(component));

		ImageProcessor openProcessor = ic.convertToRGBGreyscale().toProcessor();

		// All rotation is relative to the nucleus
		Nucleus n = cell.getPrimaryNucleus();
		Nucleus rn = cell.getPrimaryNucleus().getOrientedNucleus();

		// Rotate the image about the nucleus CoM and flip if needed
		AffineTransform at = createRotationTransform(n);
		AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);

		// Add to new image, with black background filling in spaces
		BufferedImage mid = op.filter(openProcessor.getBufferedImage(), null);

		openProcessor = new ColorProcessor(mid);

		// Ensure background is white
		openProcessor.invert();

		// Crop the new image to the region containing the oriented nucleus
		// Issue - the image has more bulk on one side than the other,
		// so the cropping can clip the real bounds.
		// Caused by vertical nucleus CoM not being in the centre of the
		// new image
		int xBase = rn.getXBase();
		int yBase = rn.getYBase();
		int padding = Imageable.COMPONENT_BUFFER;
		int wideW = (int) rn.getWidth() + (padding * 2);
		int wideH = (int) rn.getHeight() + (padding * 2);
		int wideX = xBase - padding;
		int wideY = yBase - padding;

		wideX = wideX < 0 ? 0 : wideX;
		wideY = wideY < 0 ? 0 : wideY;

		openProcessor.setRoi(wideX, wideY, wideW, wideH);
		openProcessor = openProcessor.crop();
		return openProcessor;
	}

	/**
	 * Draw the greyscale image from the given channel on the plot
	 * 
	 * @param plot      the plot to annotate
	 * @param cell      the cell to annotate
	 * @param component the component in the cell to annotate
	 * @param isRGB     if the annotation should be RGB or greyscale
	 */
	protected static void drawImageAsAnnotation(@NonNull XYPlot plot, @NonNull ICell cell,
			@NonNull CellularComponent component, RotationMode mode) {
		try {

			XYItemRenderer rend = plot.getRenderer(0); // index zero should be the

			// Start with a buffer
			int xBase = -Imageable.COMPONENT_BUFFER;
			int yBase = -Imageable.COMPONENT_BUFFER;

			// nucleus outline dataset
			ImageProcessor openProcessor;
			if (RotationMode.VERTICAL.equals(mode)) {
				openProcessor = importAndCropRotatedImage(cell, component);

				xBase += cell.getPrimaryNucleus().getOrientedNucleus().getXBase();
				yBase += cell.getPrimaryNucleus().getOrientedNucleus().getYBase();
			} else {
				openProcessor = importAndCropImage(cell, component);
				xBase += cell.getPrimaryNucleus().getXBase();
				yBase += cell.getPrimaryNucleus().getYBase();
			}

			for (int x = 0; x < openProcessor.getWidth(); x++) {
				for (int y = 0; y < openProcessor.getHeight(); y++) {

					int pixel = openProcessor.get(x, y);
					Color col = new Color(pixel);
					// Ensure the 'pixels' overlap to avoid lines of background
					// colour seeping through
					Rectangle2D r = new Rectangle2D.Double(
							xBase + x - 0.6,
							yBase + y - 0.6,
							1.2, 1.2);
					XYShapeAnnotation a = new XYShapeAnnotation(r, null, null, col);

					rend.addAnnotation(a, Layer.BACKGROUND);
				}
			}
		} catch (UnloadableImageException | MissingLandmarkException
				| ComponentCreationException e) {
			// No action needed, no image drawn
		}
	}

}
