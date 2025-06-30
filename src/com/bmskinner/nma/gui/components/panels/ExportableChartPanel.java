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
package com.bmskinner.nma.gui.components.panels;

import java.awt.Component;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.apache.batik.transcoder.TranscoderException;
import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.encoders.EncoderUtil;
import org.jfree.chart.encoders.ImageFormat;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.gui.DefaultInputSupplier;
import com.bmskinner.nma.gui.events.ChartSetEventListener;
import com.bmskinner.nma.io.ChartDataExtracter;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.utility.FileUtils;
import com.bmskinner.nma.visualisation.ChartImageConverter;
import com.bmskinner.nma.visualisation.datasets.FloatXYDataset;
import com.bmskinner.nma.visualisation.datasets.ShellResultDataset;

/**
 * This extension to a standard ChartPanel adds a popup menu item for copying
 * and exporting the underlying chart data. It also redraws the chart as the
 * panel is resized for better UX. The flag setFixedAspectRatio can be used to
 * give the chart a fixed aspect ratio. This replaces the dedicated
 * FixedAspectRatioChartPanel class
 * 
 * @author Ben Skinner
 *
 */
@SuppressWarnings("serial")
public class ExportableChartPanel extends ChartPanel implements ChartSetEventListener {

	private static final Logger LOGGER = Logger.getLogger(ExportableChartPanel.class.getName());

	/** The resolution that a chart expects to draw an image to */
	private static final double DEFAULT_SCREEN_DPI = 96;

	/** The resolution we want exported images to have */
	private static final int DEFAULT_EXPORT_DPI = 300;
	private static final double DPI_SCALE = DEFAULT_SCREEN_DPI / DEFAULT_EXPORT_DPI;

	private static final int SINGLE_COL_WIDTH_MM = 85;
	private static final int DOUBLE_COL_WIDTH_MM = 170;
	private static final int MAX_A4_HEIGHT_MM = 220;

	private static final String EXPORT_LBL = "Export data";

	private static final String EXPORT_PNG = "PNG...";
	private static final String EXPORT_SINGLE_PANEL_PNG = "PNG (single column)...";
	private static final String EXPORT_DOUBLE_PANEL_PNG = "PNG (double column)...";

	private static final String EXPORT_SVG = "SVG...";
	private static final String EXPORT_SINGLE_PANEL_SVG = "SVG (single column)...";
	private static final String EXPORT_DOUBLE_PANEL_SVG = "SVG (double column)...";
	private static final String COPY_LBL = "Copy data";

	protected final List<Object> listeners = new ArrayList<>();

	/** Control if the axis scales should be set to maintain aspect ratio */
	protected boolean isFixedAspectRatio = false;

	/** Control if the chart can be panned with the mouse */
	protected boolean isPannable = false;

	/** Used for subclasses with mouse listeners */
	protected volatile boolean mouseIsDown = false;

	/** Used for subclasses with mouse listeners */
	protected volatile boolean isRunning = false;

	/**
	 * The default bounds of the chart when empty: both axes run
	 * -{@link #DEFAULT_AUTO_RANGE} to +{@link #DEFAULT_AUTO_RANGE}
	 */
	protected static final double DEFAULT_AUTO_RANGE = 10;

	public ExportableChartPanel(@NonNull JFreeChart chart) {
		super(chart, true);

		final JPopupMenu popup = this.getPopupMenu();
		popup.addSeparator();

		final JMenuItem copyItem = new JMenuItem(COPY_LBL);
		copyItem.addActionListener(e -> copyData());
		copyItem.setEnabled(true);
		popup.add(copyItem);

		final JMenuItem exportItem = new JMenuItem(EXPORT_LBL);
		exportItem.addActionListener(e -> exportData());
		exportItem.setEnabled(true);
		popup.add(exportItem);

		// Export in the dimensions on screen
		final JMenuItem exportPNGItem = new JMenuItem(EXPORT_PNG);
		exportPNGItem
				.addActionListener(
						e -> {

							final int w = ChartImageConverter.pixelsToMM((int) (getWidth() / DPI_SCALE),
									300);
							final int h = Math.min(calcHeightFromWidth(w), MAX_A4_HEIGHT_MM);
							exportPNG(w, h);
						});
		exportPNGItem.setEnabled(true);

		// Export scaled to one panel
		final JMenuItem exportSinglePNGItem = new JMenuItem(EXPORT_SINGLE_PANEL_PNG);
		exportSinglePNGItem
				.addActionListener(e -> exportPNG(SINGLE_COL_WIDTH_MM, SINGLE_COL_WIDTH_MM));
		exportSinglePNGItem.setEnabled(true);

		// Export scaled to two panels
		final JMenuItem exportDoublePNGItem = new JMenuItem(EXPORT_DOUBLE_PANEL_PNG);
		exportDoublePNGItem
				.addActionListener(e -> exportPNG(DOUBLE_COL_WIDTH_MM, SINGLE_COL_WIDTH_MM));
		exportDoublePNGItem.setEnabled(true);

		final JMenuItem exportSvgItem = new JMenuItem(EXPORT_SVG);
		exportSvgItem.addActionListener(e -> {
			final int w = ChartImageConverter.pixelsToMM((int) (getWidth() / DPI_SCALE),
					300);
			final int h = Math.min(calcHeightFromWidth(w), MAX_A4_HEIGHT_MM);
			exportSVG(w, h);
		});
		exportSvgItem.setEnabled(true);

		final JMenuItem exportSingleSvgItem = new JMenuItem(EXPORT_SINGLE_PANEL_SVG);
		exportSingleSvgItem
				.addActionListener(e -> exportSVG(SINGLE_COL_WIDTH_MM, SINGLE_COL_WIDTH_MM));
		exportSingleSvgItem.setEnabled(true);

		final JMenuItem exportDoubleSvgItem = new JMenuItem(EXPORT_DOUBLE_PANEL_SVG);
		exportDoubleSvgItem
				.addActionListener(e -> exportSVG(DOUBLE_COL_WIDTH_MM, SINGLE_COL_WIDTH_MM));
		exportDoubleSvgItem.setEnabled(true);

		// Put the SVG export with the other save as items
		for (final Component c : popup.getComponents()) {
			if (c instanceof final JMenuItem t && t.getText().equals("Save as")) {
				t.removeAll(); // Remove the default PNG export item
				t.add(exportPNGItem);
				t.add(exportSinglePNGItem);
				t.add(exportDoublePNGItem);
				t.add(exportSvgItem);
				t.add(exportSingleSvgItem);
				t.add(exportDoubleSvgItem);
			}
		}

		// Ensure that the chart text and images are redrawn to
		// a proper aspect ratio when the panel is resized
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				restoreComponentRatio();
			}
		});

		// Add a scroll listener for zooming the chart
		// Unlike the default zoom listener, this constrains
		// the max zoom out to the data values
		this.addMouseWheelListener(new ScrollWheelZoomListener());

		this.setPannable(false);

	}

	private int calcHeightFromWidth(int w) {
		final double r = (double) getWidth() / (double) getHeight();
		return (int) (w / r);
	}

	public void restoreComponentRatio() {
		setMaximumDrawHeight(this.getHeight());
		setMaximumDrawWidth(this.getWidth());
		setMinimumDrawWidth(this.getWidth());
		setMinimumDrawHeight(this.getHeight());
	}

	/**
	 * Set if the panel should be considered to have a fixed aspect ratio. If true,
	 * the tick units of the x and y axes will have the same size on screen, and the
	 * axis ranges will update to fit the dimensions of the chart panel.
	 * 
	 * @param b
	 */
	public void setFixedAspectRatio(boolean b) {
		isFixedAspectRatio = b;

		if (b) {
			this.addComponentListener(new FixedAspectAdapter());

			restoreAutoBounds();
		} else {

			for (final ComponentListener l : this.getComponentListeners()) {
				if (l instanceof FixedAspectAdapter) {
					this.removeComponentListener(l);
				}
			}

		}
	}

	/**
	 * Test if the panel is set to use a fixed aspect ratio
	 * 
	 * @return
	 */
	public boolean isFixedAspectRatio() {
		return isFixedAspectRatio;
	}

	/**
	 * Replace the default JFreeChart listener with a panning listener
	 * 
	 * @param b
	 */
	public void setPannable(boolean b) {
		this.isPannable = b;

		if (b) {

			for (final MouseListener l : this.getMouseListeners()) {
				this.removeMouseListener(l);
			}
			final MousePanListener mpl = new MousePanListener();
			this.addMouseListener(mpl);
			this.addMouseMotionListener(mpl);

		} else {
			for (final MouseListener l : this.getMouseListeners()) {
				if (l instanceof MousePanListener) {
					this.removeMouseListener(l);
				}
			}
		}
	}

	/**
	 * Get the ratio of the width / height of the panel
	 * 
	 * @return
	 */
	public double getPanelAspectRatio() {
		return (double) this.getWidth() / (double) this.getHeight();
	}

	/**
	 * Get the ratio of the width / height of the plot (in chart units)
	 * 
	 * @return
	 */
	public double getPlotAspectRatio() {
		// Only apply to XYPlots
		if (!(this.getChart().getPlot() instanceof XYPlot))
			return 1;

		final XYPlot plot = (XYPlot) this.getChart().getPlot();

		final double w = plot.getDomainAxis().getRange().getLength();
		final double h = plot.getRangeAxis().getRange().getLength();
		return w / h;
	}

	@Override
	public void setChart(JFreeChart chart) {
		super.setChart(chart);
		try {
			fireChartSetEvent();
		} catch (final NullPointerException e) {
			// This occurs during init because setChart is called internally in
			// ChartPanel constructor
			// Catch and ignore
		}

		if (isFixedAspectRatio) {
			restoreAutoBounds();
		}
	}


	/**
	 * Get the domain axis range for all datasets in a plot
	 * 
	 * @param plot the xyplot to extract a domain range from
	 * @return
	 */
	public static Range getDataDomainRange(XYPlot plot) {
		if (plot.getDatasetCount() == 0)
			return new Range(-DEFAULT_AUTO_RANGE, DEFAULT_AUTO_RANGE);

		// start with impossible values, before finding the real chart
		// values
		double xMin = Double.MAX_VALUE;
		double xMax = -Double.MAX_VALUE;

		// get the max and min values on the chart by looking for
		// the min and max values within each dataset in the chart
		for (int i = 0; i < plot.getDatasetCount(); i++) {
			final XYDataset dataset = plot.getDataset(i);

			if (dataset == null) { // No dataset, skip
				continue;
			}

			// No values in the dataset, skip
			if (DatasetUtils.findMaximumDomainValue(dataset) == null) {
				continue;
			}

			xMax = DatasetUtils.findMaximumDomainValue(dataset).doubleValue() > xMax
					? DatasetUtils.findMaximumDomainValue(dataset).doubleValue()
					: xMax;

			xMin = DatasetUtils.findMinimumDomainValue(dataset).doubleValue() < xMin
					? DatasetUtils.findMinimumDomainValue(dataset).doubleValue()
					: xMin;
		}
		if (xMin >= xMax)
			return new Range(-DEFAULT_AUTO_RANGE, DEFAULT_AUTO_RANGE);

		return new Range(xMin, xMax);
	}

	/**
	 * Get the range axis range for all datasets in a plot
	 * 
	 * @param plot the xyplot to extract a range range from
	 * @return
	 */
	public static Range getDataRangeRange(XYPlot plot) {
		if (plot.getDatasetCount() == 0)
			return new Range(-DEFAULT_AUTO_RANGE, DEFAULT_AUTO_RANGE);

		// start with impossible values, before finding the real chart
		// values
		double yMin = Double.MAX_VALUE;
		double yMax = -Double.MAX_VALUE;

		// get the max and min values on the chart by looking for
		// the min and max values within each dataset in the chart
		for (int i = 0; i < plot.getDatasetCount(); i++) {
			final XYDataset dataset = plot.getDataset(i);

			if (dataset == null) { // No dataset, skip
				continue;
			}

			// No values in the dataset, skip
			if (DatasetUtils.findMaximumRangeValue(dataset) == null) {
				continue;
			}

			yMax = DatasetUtils.findMaximumRangeValue(dataset).doubleValue() > yMax
					? DatasetUtils.findMaximumRangeValue(dataset).doubleValue()
					: yMax;

			yMin = DatasetUtils.findMinimumRangeValue(dataset).doubleValue() < yMin
					? DatasetUtils.findMinimumRangeValue(dataset).doubleValue()
					: yMin;
		}

		if (yMin >= yMax)
			return new Range(-DEFAULT_AUTO_RANGE, DEFAULT_AUTO_RANGE);

		return new Range(yMin, yMax);
	}

	@Override
	public void restoreAutoBounds() {
		if (getChart() == null)
			return;

		// Only carry out if the flag is set
		if (!isFixedAspectRatio) {
			super.restoreAutoBounds();
			return;
		}

		try {

			// Only apply to XYPlots
			if (!(this.getChart().getPlot() instanceof XYPlot)) {
				super.restoreAutoBounds();
				return;
			}

			final XYPlot plot = (XYPlot) this.getChart().getPlot();

			// Only apply to plots with datasets
			if (plot.getDatasetCount() == 0)
				return;

			// Find the aspect ratio of the chart
			final double chartWidth = this.getWidth();
			final double chartHeight = this.getHeight();

			// If we can't get useful values for width and height, use defaults
			if (Double.valueOf(chartWidth) == null || Double.valueOf(chartHeight) == null) {
				plot.getRangeAxis().setRange(-DEFAULT_AUTO_RANGE, DEFAULT_AUTO_RANGE);
				plot.getDomainAxis().setRange(-DEFAULT_AUTO_RANGE, DEFAULT_AUTO_RANGE);
				return;
			}

			// Calculate the panel aspect ratio
			final double aspectRatio = chartWidth / chartHeight;

			final Range xDataRange = getDataDomainRange(plot);
			final Range yDataRange = getDataRangeRange(plot);

			// If no useful datasets were found (e.g. all datasets were
			// malformed) min and max default values have been set.

			// Find the real chart values
			double xMin = xDataRange.getLowerBound();
			double yMin = yDataRange.getLowerBound();
			double xMax = xDataRange.getUpperBound();
			double yMax = yDataRange.getUpperBound();

			// find the ranges the min and max values cover
			final double xRange = xDataRange.getLength();
			final double yRange = yDataRange.getLength();

			double newXRange = xDataRange.getLength();
			double newYRange = yDataRange.getLength();

			// test the aspect ratio
			if ((xRange / yRange) > aspectRatio) {
				// width is not enough
				newXRange = xRange * 1.1;
				newYRange = newXRange / aspectRatio;
			} else {
				// height is not enough
				newYRange = yRange * 1.1; // add some extra x space
				newXRange = newYRange * aspectRatio; // get the new Y range
			}

			// with the new ranges, find the best min and max values to use
			final double xDiff = (newXRange - xRange) / 2;
			final double yDiff = (newYRange - yRange) / 2;

			xMin -= xDiff;
			xMax += xDiff;
			yMin -= yDiff;
			yMax += yDiff;

			if (yMin >= yMax) {
				LOGGER.finest("Min and max are equal");
				xMin = -DEFAULT_AUTO_RANGE;
				yMin = -DEFAULT_AUTO_RANGE;
				xMax = DEFAULT_AUTO_RANGE;
				yMax = DEFAULT_AUTO_RANGE;
			}

			plot.getRangeAxis().setRange(yMin, yMax);
			plot.getDomainAxis().setRange(xMin, xMax);

		} catch (final Exception e) {
			LOGGER.log(Level.SEVERE,
					"Error restoring auto bounds, falling back to default: %s".formatted(e.getMessage()), e);
			if (getChart() != null) {
				super.restoreAutoBounds();
			}

		}

	}

	private String getChartData() {

		try {

			if (this.getChart().getPlot() instanceof CategoryPlot) {
				final CategoryPlot plot = this.getChart().getCategoryPlot();
				if (plot.getDataset() instanceof ShellResultDataset)
					return ChartDataExtracter.getShellData(this.getChart());
				if (plot.getDataset() instanceof BoxAndWhiskerCategoryDataset)
					return ChartDataExtracter.getBoxplotData(this.getChart());

			} else {
				final XYPlot plot = getChart().getXYPlot();
				if (plot.getDataset() instanceof XYZDataset)
					return ChartDataExtracter.getHeatMapData(this.getChart());
				if (plot.getDataset() instanceof FloatXYDataset
						|| plot.getDataset() instanceof DefaultXYDataset)
					return ChartDataExtracter.getXYData(this.getChart());
				if (plot.getDataset() instanceof HistogramDataset)
					return ChartDataExtracter.getHistogramData(this.getChart());

			}

		} catch (final ClassCastException e2) {

			final StringBuilder builder = new StringBuilder();
			builder.append("Class cast error: " + e2.getMessage() + Io.NEWLINE);

			for (final StackTraceElement el : e2.getStackTrace()) {
				builder.append(el.toString() + Io.NEWLINE);
			}
			return builder.toString();
		}
		return "";
	}

	private void copyData() {

		new Thread(() -> {
			final String string = getChartData();
			final StringSelection stringSelection = new StringSelection(string);
			final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stringSelection, null);
		}).start();
	}

	private void exportData() {
		try {
			final File saveFile = new DefaultInputSupplier().requestFileSave(
					FileUtils.commonPathOfDatasets(
							DatasetListManager.getInstance().getSelectedDatasets()),
					"Table export", Io.TAB_FILE_EXTENSION_NODOT);

			new Thread(() -> {
				final String string = getChartData();

				try (PrintWriter out = new PrintWriter(saveFile)) {
					out.println(string);
				} catch (final FileNotFoundException e) {
					LOGGER.warning("Cannot export to file");
					LOGGER.log(Level.SEVERE, "Error exporting", e);
				}
			}).start();
		} catch (final RequestCancelledException e) {
			// User cancelled, no action
		}
	}

	/**
	 * Export as a png with the given final pixel dimensions.
	 * 
	 * The image will be drawn at smaller dimensions, assuming a default 96 DPI,
	 * then scaled up to the final image size, so that text elements are readable in
	 * the final image. Note that this will result in blurrier images than ideal,
	 * and a better solution should be found.
	 * 
	 * @param w the width of the output in mm
	 */
	private void exportPNG(int w, int h) {

		try {
			final File file = new DefaultInputSupplier().requestFileSave(
					FileUtils.commonPathOfDatasets(
							DatasetListManager.getInstance().getSelectedDatasets()),
					"Chart export", Io.PNG_FILE_EXTENSION_NODOT);

			if (file.exists()
					&& !new DefaultInputSupplier().requestApproval("File exists. Overwrite?",
							"Overwrite existing file?"))
				return;

			try (OutputStream os = new FileOutputStream(file)) {
				final BufferedImage bi = ChartImageConverter.createPNG(getChart(), w, h,
						DEFAULT_EXPORT_DPI, this.isFixedAspectRatio);

				EncoderUtil.writeBufferedImage(bi, ImageFormat.PNG, os);
				LOGGER.info("Chart saved as '" + file.getName() + "'");

			} catch (final IOException e) {
				LOGGER.log(Level.SEVERE, "Unable to save chart as png", e);
			} catch (final TranscoderException e) {
				LOGGER.log(Level.SEVERE, "Unable to transcode chart to png", e);
			}

		} catch (final RequestCancelledException e) {
			// User cancelled, no action
		}

	}

	/**
	 * Export the chart in this panel as SVG of the given dimensions
	 * 
	 * @param w the width of the output in mm
	 * @param h the height of the output in mm
	 */
	private void exportSVG(int w, int h) {

		try {
			final File file = new DefaultInputSupplier().requestFileSave(
					FileUtils.commonPathOfDatasets(
							DatasetListManager.getInstance().getSelectedDatasets()),
					"Chart export", Io.SVG_FILE_EXTENSION_NODOT);

			if (file.exists()
					&& !new DefaultInputSupplier().requestApproval("File exists. Overwrite?",
							"Overwrite existing file?"))
				return;

			final String svg = ChartImageConverter.createSVG(getChart(), w, h, DEFAULT_EXPORT_DPI,
					this.isFixedAspectRatio);

			writeToSVG(file, svg);
			LOGGER.info("Chart saved as '" + file.getName() + "'");

		} catch (final RequestCancelledException e) {
			// User cancelled, no action
		} catch (final IOException e) {
			LOGGER.fine("Unable to export chart");
		}
	}

	/**
	 * Write the given svg to file. Modified from SVGUtils to avoid adding a second
	 * XML header
	 * 
	 * @param file
	 * @param svgElement
	 * @throws IOException
	 */
	private static void writeToSVG(File file, String svgElement)
			throws IOException {
		BufferedWriter writer = null;
		try (OutputStream os = new FileOutputStream(file);
				OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");) {
			writer = new BufferedWriter(osw);
			writer.write(svgElement + "\n");
			writer.flush();
		} finally {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (final IOException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	/**
	 * Signal listeners that the chart with the given options has been rendered
	 * 
	 * @param options
	 */
	public void fireChartSetEvent() {
		final ChartSetEvent e = new ChartSetEvent(this);
		final Iterator<Object> iterator = listeners.iterator();
		while (iterator.hasNext()) {

			final Object o = iterator.next();

			if (o instanceof ChartSetEventListener) {
				((ChartSetEventListener) o).chartSetEventReceived(e);
			}
		}
	}

	/**
	 * Add a listener for completed charts rendered into the chart cache of this
	 * panel.
	 * 
	 * @param l
	 */
	public synchronized void addChartSetEventListener(ChartSetEventListener l) {
		listeners.add(l);
	}

	public synchronized void removeChartSetEventListener(ChartSetEventListener l) {
		listeners.remove(l);
	}

	@Override
	public void chartSetEventReceived(ChartSetEvent e) {
		// TODO Auto-generated method stub

	}

	/**
	 * Get the position of the given point in chart value coordinates
	 * 
	 * @param panelPoint
	 * @return
	 */
	protected Point2D getChartValuePosition(Point panelPoint) {

		// Translate the panel location on screen to a Java2D point
		final Point2D p = translateScreenToJava2D(panelPoint);

		// Get the area covered by the panel
		final Rectangle2D plotArea = getChartRenderingInfo().getPlotInfo().getDataArea();

		final XYPlot plot = (XYPlot) getChart().getPlot();

		final double x = plot.getDomainAxis().java2DToValue(p.getX(), plotArea, plot.getDomainAxisEdge());
		final double y = plot.getRangeAxis().java2DToValue(p.getY(), plotArea, plot.getRangeAxisEdge());

		return new Point2D.Double(x, y);

	}

	public class FixedAspectAdapter extends ComponentAdapter {
		@Override
		public void componentResized(ComponentEvent e) {
			restoreAutoBounds();
		}
	}

	public class MousePanListener implements MouseListener, MouseMotionListener {

		Point2D startPoint = null;

		@Override
		public void mouseClicked(MouseEvent e) {
			ExportableChartPanel.this.mouseClicked(e);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			startPoint = getChartValuePosition(e.getPoint());
			if (e.isPopupTrigger()) {
				ExportableChartPanel.this.mousePressed(e);

			}

		}

		@Override
		public void mouseReleased(MouseEvent e) {
			startPoint = null;
			if (e.isPopupTrigger()) {
				ExportableChartPanel.this.mouseReleased(e);

			}

		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if (getChart() == null)
				return;

			if (!(getChart().getPlot() instanceof XYPlot))
				return;

			final XYPlot plot = getChart().getXYPlot();

			final XYDataset d = plot.getDataset();
			if (d == null)
				return;

			if (startPoint != null) {

				final Point2D p = getChartValuePosition(e.getPoint());
				final double dx = startPoint.getX() - p.getX();
				final double dy = startPoint.getY() - p.getY();

				final Range xoriginal = plot.getDomainAxis().getRange();
				final Range yoriginal = plot.getRangeAxis().getRange();

				plot.getDomainAxis().setRange(xoriginal.getLowerBound() + dx,
						xoriginal.getUpperBound() + dx);
				plot.getRangeAxis().setRange(yoriginal.getLowerBound() + dy,
						yoriginal.getUpperBound() + dy);
			}

		}

		@Override
		public void mouseMoved(MouseEvent e) {
			// TODO Auto-generated method stub

		}

	}

	/**
	 * Change the zoom of the chart based on scrolling
	 * 
	 * @author Ben Skinner
	 *
	 */
	public class ScrollWheelZoomListener implements MouseWheelListener {

		private static final double ZOOM_IN_FACTOR = 1.5d;
		private static final double ZOOM_OUT_FACTOR = 1.2d;

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			if (getChart() == null)
				return;

			if (!(getChart().getPlot() instanceof XYPlot))
				return;

			final XYPlot plot = getChart().getXYPlot();

			final XYDataset d = plot.getDataset();
			if (d == null)
				return;

			// Find the full data range of all values in the chart
			Range domainRange = DatasetUtils.findDomainBounds(d);
			Range rangeRange = DatasetUtils.findRangeBounds(d);

			if (plot.getDatasetCount() > 1) {
				for (int i = 0; i < plot.getDatasetCount(); i++) {

					if (plot.getDataset(i) == null) {
						continue;
					}

					domainRange = Range.combine(domainRange,
							DatasetUtils.findDomainBounds(plot.getDataset(i)));
					rangeRange = Range.combine(rangeRange,
							DatasetUtils.findRangeBounds(plot.getDataset(i)));
				}
			}

			// Find the anchor point for the zoom
			final Point2D p = getChartValuePosition(e.getPoint());

			final Range xoriginal = plot.getDomainAxis().getRange();
			final Range yoriginal = plot.getRangeAxis().getRange();

			// We want the point under the cursor to remain under the cursor
			// after zooming and not jump to the middle of the screen.
			// To do this, calculate the fractional position of the cursor
			// and preserve this in the new range.

			final double fx = (p.getX() - xoriginal.getLowerBound()) / xoriginal.getLength();
			final double fy = (p.getY() - yoriginal.getLowerBound()) / yoriginal.getLength();

			// Zoom the range
			if (e.getUnitsToScroll() < 0) { // Zoom in

				// The new range lengths to be covered
				final double xr = xoriginal.getLength() / ZOOM_IN_FACTOR;
				double yr = yoriginal.getLength() / ZOOM_IN_FACTOR;

				// Correct for aspect ratio
				if (isFixedAspectRatio) {
					yr = xr / getPanelAspectRatio();
				}

				// Set min and max of range from fraction position of anchor
				final double xMin = p.getX() - (fx * xr);
				final double xMax = p.getX() + (1 - fx) * xr;
				final double yMin = p.getY() - (fy * yr);
				final double yMax = p.getY() + (1 - fy) * yr;

				// Update the range
				plot.getDomainAxis().setRange(xMin, xMax);
				plot.getRangeAxis().setRange(yMin, yMax);

			} else { // Zoom out

				final double xr = xoriginal.getLength() * ZOOM_OUT_FACTOR;
				double yr = yoriginal.getLength() * ZOOM_OUT_FACTOR;

				// Correct for aspect ratio
				if (isFixedAspectRatio) {
					yr = xr / getPanelAspectRatio();
				}

				// Find the values range plus 10% to constrain zoom out
				final ChartRanges minZoomRanges = findMinimumZoomAxisRanges();

				// Ensure we only zoom out but only to the extent of the data
				final double xMin = minZoomRanges.xRange.constrain(p.getX() - (fx * xr));
				final double xMax = minZoomRanges.xRange.constrain(p.getX() + (1 - fx) * xr);
				final double yMin = minZoomRanges.yRange.constrain(p.getY() - (fy * yr));
				final double yMax = minZoomRanges.yRange.constrain(p.getY() + (1 - fy) * yr);

				// Update the range
				plot.getDomainAxis().setRange(xMin, xMax);
				plot.getRangeAxis().setRange(yMin, yMax);
			}
		}

		/**
		 * Find the ranges that should constrain the minimum zoom level for the loaded
		 * chart
		 * 
		 * @return
		 */
		private ChartRanges findMinimumZoomAxisRanges() {

			final XYPlot plot = getChart().getXYPlot();
			// Find the full data range of all values in the chart
			Range domainRange = DatasetUtils.findDomainBounds(plot.getDataset());
			Range rangeRange = DatasetUtils.findRangeBounds(plot.getDataset());

			if (plot.getDatasetCount() > 1) {

				for (int i = 0; i < plot.getDatasetCount(); i++) {

					if (plot.getDataset(i) == null) {
						continue;
					}

					domainRange = Range.combine(domainRange,
							DatasetUtils.findDomainBounds(plot.getDataset(i)));
					rangeRange = Range.combine(rangeRange,
							DatasetUtils.findRangeBounds(plot.getDataset(i)));
				}
			}

			// The maximum range depends on whether the chart is aspect ratio constrained.

			if (!isFixedAspectRatio)
				return new ChartRanges(Range.expand(domainRange, 0.10, 0.10),
						Range.expand(rangeRange, 0.10, 0.10));

			final double expandedRangeLength = Range.expand(rangeRange, 0.10, 0.10).getLength();

			if (domainRange.getLength() > expandedRangeLength) {

				domainRange = Range.expand(domainRange, 0.10, 0.10);
				final double rangeLength = domainRange.getLength() / getPanelAspectRatio();
				rangeRange = new Range(rangeRange.getCentralValue() - rangeLength / 2,
						rangeRange.getCentralValue() + rangeLength / 2);

			} else {
				rangeRange = Range.expand(rangeRange, 0.10, 0.10);
				final double domainLength = rangeRange.getLength() * getPanelAspectRatio();
				domainRange = new Range(domainRange.getCentralValue() - domainLength / 2,
						domainRange.getCentralValue() + domainLength / 2);

			}

			return new ChartRanges(domainRange, rangeRange);
		}

	}

	private record ChartRanges(Range xRange, Range yRange) {
	}

}
