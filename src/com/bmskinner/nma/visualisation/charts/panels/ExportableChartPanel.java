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
package com.bmskinner.nma.visualisation.charts.panels;

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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import org.jfree.svg.SVGUtils;

import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.gui.DefaultInputSupplier;
import com.bmskinner.nma.gui.events.ChartSetEventListener;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.utility.FileUtils;
import com.bmskinner.nma.visualisation.ChartImageConverter;
import com.bmskinner.nma.visualisation.datasets.ExportableBoxAndWhiskerCategoryDataset;
import com.bmskinner.nma.visualisation.datasets.FloatXYDataset;
import com.bmskinner.nma.visualisation.datasets.ShellResultDataset;

/**
 * This extension to a standard ChartPanel adds a popup menu item for copying
 * and exporting the underlying chart data. It also redraws the chart as the
 * panel is resized for better UX. The flag setFixedAspectRatio can be used to
 * give the chart a fixed aspect ratio. This replaces the dedicated
 * FixedAspectRatioChartPanel class
 * 
 * @author ben
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
		super(chart, false);

		JPopupMenu popup = this.getPopupMenu();
		popup.addSeparator();

		JMenuItem copyItem = new JMenuItem(COPY_LBL);
		copyItem.addActionListener(e -> copyData());
		copyItem.setEnabled(true);
		popup.add(copyItem);

		JMenuItem exportItem = new JMenuItem(EXPORT_LBL);
		exportItem.addActionListener(e -> exportData());
		exportItem.setEnabled(true);
		popup.add(exportItem);

		JMenuItem exportPNGItem = new JMenuItem(EXPORT_PNG);
		exportPNGItem
				.addActionListener(
						e -> exportPNG(
								ChartImageConverter.pixelsToMM((int) (getWidth() / DPI_SCALE),
										300)));
		exportPNGItem.setEnabled(true);

		JMenuItem exportSinglePNGItem = new JMenuItem(EXPORT_SINGLE_PANEL_PNG);
		exportSinglePNGItem.addActionListener(e -> exportPNG(SINGLE_COL_WIDTH_MM));
		exportSinglePNGItem.setEnabled(true);

		JMenuItem exportDoublePNGItem = new JMenuItem(EXPORT_DOUBLE_PANEL_PNG);
		exportDoublePNGItem.addActionListener(e -> exportPNG(DOUBLE_COL_WIDTH_MM));
		exportDoublePNGItem.setEnabled(true);

		JMenuItem exportSvgItem = new JMenuItem(EXPORT_SVG);
		exportSvgItem.addActionListener(e -> exportSVG(
				ChartImageConverter.pixelsToMM((int) (getWidth() / DPI_SCALE), 300)));
		exportSvgItem.setEnabled(true);

		JMenuItem exportSingleSvgItem = new JMenuItem(EXPORT_SINGLE_PANEL_SVG);
		exportSingleSvgItem.addActionListener(e -> exportSVG(SINGLE_COL_WIDTH_MM));
		exportSingleSvgItem.setEnabled(true);

		JMenuItem exportDoubleSvgItem = new JMenuItem(EXPORT_DOUBLE_PANEL_SVG);
		exportDoubleSvgItem.addActionListener(e -> exportSVG(DOUBLE_COL_WIDTH_MM));
		exportDoubleSvgItem.setEnabled(true);

		// Put the SVG export with the other save as items
		for (Component c : popup.getComponents()) {
			if (c instanceof JMenuItem t && t.getText().equals("Save as")) {
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
		double r = (double) getWidth() / (double) getHeight();
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

			for (ComponentListener l : this.getComponentListeners()) {
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

			for (MouseListener l : this.getMouseListeners()) {
				this.removeMouseListener(l);
			}
			MousePanListener mpl = new MousePanListener();
			this.addMouseListener(mpl);
			this.addMouseMotionListener(mpl);

		} else {
			for (MouseListener l : this.getMouseListeners()) {
				if (l instanceof MousePanListener)
					this.removeMouseListener(l);
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
		if (!(this.getChart().getPlot() instanceof XYPlot)) {
			return 1;
		}

		XYPlot plot = (XYPlot) this.getChart().getPlot();

		double w = plot.getDomainAxis().getRange().getLength();
		double h = plot.getRangeAxis().getRange().getLength();
		return w / h;
	}

	@Override
	public void setChart(JFreeChart chart) {
		super.setChart(chart);
		try {
			fireChartSetEvent();
		} catch (NullPointerException e) {
			// This occurs during init because setChart is called internally in
			// ChartPanel constructor
			// Catch and ignore
		}

		if (isFixedAspectRatio) {
			restoreAutoBounds();
		}
	}

	@Override
	public void restoreAutoBounds() {

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

			XYPlot plot = (XYPlot) this.getChart().getPlot();

			// Only apply to plots with datasets
			if (plot.getDatasetCount() == 0) {
				return;
			}

			// Find the aspect ratio of the chart
			double chartWidth = this.getWidth();
			double chartHeight = this.getHeight();

			// If we can't get useful values for width and height, use defaults
			if (Double.valueOf(chartWidth) == null || Double.valueOf(chartHeight) == null) {
				plot.getRangeAxis().setRange(-DEFAULT_AUTO_RANGE, DEFAULT_AUTO_RANGE);
				plot.getDomainAxis().setRange(-DEFAULT_AUTO_RANGE, DEFAULT_AUTO_RANGE);
				return;
			}

			// Calculate the panel aspect ratio
			double aspectRatio = chartWidth / chartHeight;

			// start with impossible values, before finding the real chart
			// values
			double xMin = Double.MAX_VALUE;
			double yMin = Double.MAX_VALUE;
			//
			double xMax = -Double.MAX_VALUE;
			double yMax = -Double.MAX_VALUE;

			// get the max and min values on the chart by looking for
			// the min and max values within each dataset in the chart
			for (int i = 0; i < plot.getDatasetCount(); i++) {
				XYDataset dataset = plot.getDataset(i);

				if (dataset == null) { // No dataset, skip
					LOGGER.finest("Null dataset " + i);
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

				yMax = DatasetUtils.findMaximumRangeValue(dataset).doubleValue() > yMax
						? DatasetUtils.findMaximumRangeValue(dataset).doubleValue()
						: yMax;

				yMin = DatasetUtils.findMinimumRangeValue(dataset).doubleValue() < yMin
						? DatasetUtils.findMinimumRangeValue(dataset).doubleValue()
						: yMin;
			}

			// If no useful datasets were found (e.g. all datasets were
			// malformed)
			// min and max 'impossible' values have not changed. In this case,
			// set defaults
			if (xMin == Double.MAX_VALUE || yMin == Double.MAX_VALUE) {
				xMin = -DEFAULT_AUTO_RANGE;
				yMin = -DEFAULT_AUTO_RANGE;
				xMax = DEFAULT_AUTO_RANGE;
				yMax = DEFAULT_AUTO_RANGE;
			}

			// find the ranges the min and max values cover
			double xRange = xMax - xMin;
			double yRange = yMax - yMin;

			double newXRange = xRange;
			double newYRange = yRange;

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
			double xDiff = (newXRange - xRange) / 2;
			double yDiff = (newYRange - yRange) / 2;

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

		} catch (Exception e) {
			LOGGER.log(Loggable.STACK, "Error restoring auto bounds, falling back to default", e);
			super.restoreAutoBounds();
		}

	}

	private String getChartData() {

		try {

			if (this.getChart().getPlot() instanceof CategoryPlot) {
				CategoryPlot plot = this.getChart().getCategoryPlot();
				if (plot.getDataset() instanceof ShellResultDataset)
					return getShellData();
				if (plot.getDataset() instanceof BoxAndWhiskerCategoryDataset)
					return getBoxplotData();

			} else {
				XYPlot plot = getChart().getXYPlot();
				if (plot.getDataset() instanceof XYZDataset)
					return getHeatMapData();
				if (plot.getDataset() instanceof FloatXYDataset
						|| plot.getDataset() instanceof DefaultXYDataset)
					return getXYData();
				if (plot.getDataset() instanceof HistogramDataset)
					return getHistogramData();

			}

		} catch (ClassCastException e2) {

			StringBuilder builder = new StringBuilder();
			builder.append("Class cast error: " + e2.getMessage() + Io.NEWLINE);

			for (StackTraceElement el : e2.getStackTrace()) {
				builder.append(el.toString() + Io.NEWLINE);
			}
			return builder.toString();
		}
		return "";
	}

	private void copyData() {

		new Thread(() -> {
			String string = getChartData();
			StringSelection stringSelection = new StringSelection(string);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stringSelection, null);
		}).start();
	}

	private void exportData() {
		try {
			File saveFile = new DefaultInputSupplier().requestFileSave(
					FileUtils.commonPathOfDatasets(
							DatasetListManager.getInstance().getSelectedDatasets()),
					"Table export", Io.TAB_FILE_EXTENSION_NODOT);

			new Thread(() -> {
				String string = getChartData();

				try (PrintWriter out = new PrintWriter(saveFile)) {
					out.println(string);
				} catch (FileNotFoundException e) {
					LOGGER.warning("Cannot export to file");
					LOGGER.log(Loggable.STACK, "Error exporting", e);
				}
			}).start();
		} catch (RequestCancelledException e) {
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
	private void exportPNG(int w) {

		int h = calcHeightFromWidth(w);

		try {
			File file = new DefaultInputSupplier().requestFileSave(
					FileUtils.commonPathOfDatasets(
							DatasetListManager.getInstance().getSelectedDatasets()),
					"Chart export", Io.PNG_FILE_EXTENSION_NODOT);

			if (file.exists()
					&& !new DefaultInputSupplier().requestApproval("File exists. Overwrite?",
							"Overwrite existing file?"))
				return;

			try (OutputStream os = new FileOutputStream(file)) {

				BufferedImage bi = ChartImageConverter.createPNG(getChart(), w, h,
						DEFAULT_EXPORT_DPI);

				EncoderUtil.writeBufferedImage(bi, ImageFormat.PNG, os);
				LOGGER.info("Chart saved as '" + file.getName() + "'");

			} catch (IOException e) {
				LOGGER.log(Loggable.STACK, "Unable to save chart as png", e);
			} catch (TranscoderException e) {
				LOGGER.log(Loggable.STACK, "Unable to transcode chart to png", e);
			}

		} catch (RequestCancelledException e) {
			// User cancelled, no action
		}

	}

	/**
	 * Export the chart in this panel as SVG of the given dimensions
	 * 
	 * @param w the width of the output in mm
	 */
	private void exportSVG(int w) {

		int h = calcHeightFromWidth(w);

		try {
			File file = new DefaultInputSupplier().requestFileSave(
					FileUtils.commonPathOfDatasets(
							DatasetListManager.getInstance().getSelectedDatasets()),
					"Chart export", Io.SVG_FILE_EXTENSION_NODOT);

			if (file.exists()
					&& !new DefaultInputSupplier().requestApproval("File exists. Overwrite?",
							"Overwrite existing file?"))
				return;

			String svg = ChartImageConverter.createSVG(getChart(), w, h, DEFAULT_EXPORT_DPI);

			SVGUtils.writeToSVG(file, svg);
			LOGGER.info("Chart saved as '" + file.getName() + "'");

		} catch (RequestCancelledException e) {
			// User cancelled, no action
		} catch (IOException e) {
			LOGGER.fine("Unable to export chart");
		}
	}

	private String getHeatMapData() throws ClassCastException {
		XYPlot plot = this.getChart().getXYPlot();
		StringBuilder builder = new StringBuilder();
		DecimalFormat df = new DecimalFormat("#0.0000");

		int datasetCount = plot.getDatasetCount();
		// header
		builder.append("Dataset");
		int shells = plot.getDataset(0).getItemCount(0);

		for (int i = 0; i < shells; i++)
			builder.append(Io.TAB + "Shell_" + i);
		builder.append(Io.NEWLINE);

		for (int dataset = 0; dataset < datasetCount; dataset++) {
			XYZDataset ds = (XYZDataset) plot.getDataset(dataset);

			for (int series = 0; series < ds.getSeriesCount(); series++) {
				String columnName = ds.getSeriesKey(series).toString();
				builder.append(columnName);

				for (int item = 0; item < ds.getItemCount(series); item++) {
					double value = ds.getZValue(series, item);
					builder.append(Io.TAB + df.format(value));
				}
				builder.append(Io.NEWLINE);
			}
		}
		return builder.toString();
	}

	private String getShellData() throws ClassCastException {
		CategoryPlot plot = this.getChart().getCategoryPlot();
		StringBuilder builder = new StringBuilder();
		DecimalFormat df = new DecimalFormat("#0.0000");

		int datasetCount = plot.getDatasetCount();
		// header
		builder.append("Dataset");
		int shells = plot.getDataset(0).getColumnCount();
		for (int i = 0; i < shells; i++)
			builder.append(Io.TAB + "Shell_" + i);
		builder.append(Io.NEWLINE);

		for (int dataset = 0; dataset < datasetCount; dataset++) {
			ShellResultDataset ds = (ShellResultDataset) plot.getDataset(dataset);
			for (int row = 0; row < ds.getRowCount(); row++) {

				String datasetKey = ds.getRowKey(row).toString();
				builder.append(datasetKey);
				for (int column = 0; column < shells; column++) {
					double value = ds.getValue(row, column).doubleValue();
					builder.append(Io.TAB + df.format(value));
				}
				builder.append(Io.NEWLINE);
			}
		}
		return builder.toString();
	}

	// Invoke when dealing with an XY chart
	private String getXYData() throws ClassCastException {
		XYPlot plot = this.getChart().getXYPlot();
		String xAxisName = plot.getDomainAxis().getLabel();
		String yAxisName = plot.getRangeAxis().getLabel();

		StringBuilder builder = new StringBuilder(
				"Series" + Io.TAB + xAxisName + Io.TAB + yAxisName + Io.NEWLINE);
		DecimalFormat df = new DecimalFormat("#0.00");

		for (int dataset = 0; dataset < plot.getDatasetCount(); dataset++) {

			XYDataset ds = plot.getDataset(dataset);

			for (int series = 0; series < ds.getSeriesCount(); series++) {

				String seriesName = ds.getSeriesKey(series).toString();

				for (int i = 0; i < ds.getItemCount(series); i++) {
					double x = ds.getXValue(series, i);
					double y = ds.getYValue(series, i);

					builder.append(seriesName + Io.TAB + df.format(x) + Io.TAB + df.format(y)
							+ Io.NEWLINE);
				}
			}
		}

		return builder.toString();
	}

	private String getBoxplotData() throws ClassCastException {

		CategoryPlot plot = this.getChart().getCategoryPlot();
		StringBuilder builder = new StringBuilder();
		DecimalFormat df = new DecimalFormat("#0.000");

		builder.append("Row_name" + Io.TAB + "Column_name" + Io.TAB + "ValueType" + Io.TAB + "Value"
				+ Io.NEWLINE);

		for (int dataset = 0; dataset < plot.getDatasetCount(); dataset++) {

			DefaultBoxAndWhiskerCategoryDataset ds = (DefaultBoxAndWhiskerCategoryDataset) plot
					.getDataset(dataset);

			if (ds instanceof ExportableBoxAndWhiskerCategoryDataset) {

				for (int column = 0; column < ds.getColumnCount(); column++) {

					String columnName = ds.getColumnKey(column).toString();
					for (int row = 0; row < ds.getRowCount(); row++) {
						String rowName = ds.getRowKey(row).toString();
						Number number = ds.getValue(row, column);
						double value = Double.NaN;
						if (number != null)
							value = number.doubleValue();

						List rawData = ((ExportableBoxAndWhiskerCategoryDataset) ds)
								.getRawData(rowName, columnName);
						if (rawData == null)
							continue;

						Collections.sort(rawData);

						builder.append(rowName + Io.TAB + columnName + Io.TAB + "Min_value" + Io.TAB
								+ df.format(rawData.get(0)) + Io.NEWLINE);
						builder.append(rowName + Io.TAB + columnName + Io.TAB + "Lower_whisker"
								+ Io.TAB
								+ df.format(ds.getMinRegularValue(row, column)) + Io.NEWLINE);
						builder.append(
								rowName + Io.TAB + columnName + Io.TAB + "Lower_quartile" + Io.TAB
										+ df.format(ds.getQ1Value(row, column)) + Io.NEWLINE);
						builder.append(rowName + Io.TAB + columnName + Io.TAB + "Median" + Io.TAB
								+ df.format(value)
								+ Io.NEWLINE);
						builder.append(
								rowName + Io.TAB + columnName + Io.TAB + "Upper_quartile" + Io.TAB
										+ df.format(ds.getQ3Value(row, column)) + Io.NEWLINE);
						builder.append(rowName + Io.TAB + columnName + Io.TAB + "Upper_whisker"
								+ Io.TAB
								+ df.format(ds.getMaxRegularValue(row, column)) + Io.NEWLINE);
						builder.append(rowName + Io.TAB + columnName + Io.TAB + "Max_value" + Io.TAB
								+ df.format(rawData.get(rawData.size() - 1)) + Io.NEWLINE);

						for (Object o : rawData)
							builder.append(rowName + Io.TAB + columnName + Io.TAB + "Raw_value"
									+ Io.TAB + o.toString()
									+ Io.NEWLINE);

					}
				}
			}
		}
		return builder.toString();
	}

	private String getHistogramData() throws ClassCastException {

		XYPlot plot = this.getChart().getXYPlot();
		DecimalFormat df = new DecimalFormat("#0.00");
		StringBuilder builder = new StringBuilder();

		for (int dataset = 0; dataset < plot.getDatasetCount(); dataset++) {

			HistogramDataset ds = (HistogramDataset) plot.getDataset(dataset);

			for (int series = 0; series < ds.getSeriesCount(); series++) {

				String seriesName = ds.getSeriesKey(series).toString();
				builder.append(seriesName + ":" + Io.NEWLINE);

				for (int i = 0; i < ds.getItemCount(series); i++) {

					double x = ds.getXValue(series, i);
					double y = ds.getYValue(series, i);
					builder.append(Io.TAB + df.format(x) + Io.TAB + df.format(y) + Io.NEWLINE);
				}
				builder.append(Io.NEWLINE);
			}
		}
		return builder.toString();
	}

	/**
	 * Signal listeners that the chart with the given options has been rendered
	 * 
	 * @param options
	 */
	public void fireChartSetEvent() {
		ChartSetEvent e = new ChartSetEvent(this);
		Iterator<Object> iterator = listeners.iterator();
		while (iterator.hasNext()) {

			Object o = iterator.next();

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
		Point2D p = translateScreenToJava2D(panelPoint);

		// Get the area covered by the panel
		Rectangle2D plotArea = getChartRenderingInfo().getPlotInfo().getDataArea();

		XYPlot plot = (XYPlot) getChart().getPlot();

		double x = plot.getDomainAxis().java2DToValue(p.getX(), plotArea, plot.getDomainAxisEdge());
		double y = plot.getRangeAxis().java2DToValue(p.getY(), plotArea, plot.getRangeAxisEdge());

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

			if (!(getChart().getPlot() instanceof XYPlot)) {
				return;
			}

			XYPlot plot = getChart().getXYPlot();

			XYDataset d = plot.getDataset();
			if (d == null)
				return;

			if (startPoint != null) {

				Point2D p = getChartValuePosition(e.getPoint());
				double dx = startPoint.getX() - p.getX();
				double dy = startPoint.getY() - p.getY();

				Range xoriginal = plot.getDomainAxis().getRange();
				Range yoriginal = plot.getRangeAxis().getRange();

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
	 * @author bs19022
	 *
	 */
	public class ScrollWheelZoomListener implements MouseWheelListener {

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			if (getChart() == null)
				return;

			if (!(getChart().getPlot() instanceof XYPlot)) {
				return;
			}

			XYPlot plot = getChart().getXYPlot();

			XYDataset d = plot.getDataset();
			if (d == null)
				return;

			// Find the full data range
			Range domainRange = DatasetUtils.findDomainBounds(d);
			Range rangeRange = DatasetUtils.findRangeBounds(d);

			if (plot.getDatasetCount() > 1) {
				for (int i = 0; i < plot.getDatasetCount(); i++) {
					domainRange = Range.combine(domainRange,
							DatasetUtils.findDomainBounds(plot.getDataset(i)));
					rangeRange = Range.combine(rangeRange,
							DatasetUtils.findRangeBounds(plot.getDataset(i)));
				}
			}

			Point2D p = getChartValuePosition(e.getPoint());
			if (e.getUnitsToScroll() < 0) { // Zoom in

				Range xoriginal = plot.getDomainAxis().getRange();
				Range yoriginal = plot.getRangeAxis().getRange();

				// The new range lengths to be covered
				double xr = xoriginal.getLength() / 1.5;
				double yr = yoriginal.getLength() / 1.5;

				// We want the point under the cursor to remain under the cursor
				// after zooming and not jump to the middle of the screen.
				// To do this, calculate the fractional position of the cursor
				// and preserve this in the new range.

				double fx = (p.getX() - xoriginal.getLowerBound()) / xoriginal.getLength();
				double fy = (p.getY() - yoriginal.getLowerBound()) / yoriginal.getLength();

				plot.getDomainAxis().setRange(p.getX() - (fx * xr), p.getX() + (1 - fx) * xr);
				plot.getRangeAxis().setRange(p.getY() - (fy * yr), p.getY() + (1 - fy) * yr);

			} else { // Zoom out

				Range xoriginal = plot.getDomainAxis().getRange();
				Range yoriginal = plot.getRangeAxis().getRange();

				double xr = plot.getDomainAxis().getRange().getLength() * 1.2;
				double yr = plot.getRangeAxis().getRange().getLength() * 1.2;

				// Find the values range plus 10% to constrain zoom out
				domainRange = Range.expand(domainRange, 0.05, 0.05);
				rangeRange = Range.expand(rangeRange, 0.05, 0.05);

				// Zoom out from anchor point
				double fx = (p.getX() - xoriginal.getLowerBound()) / xoriginal.getLength();
				double fy = (p.getY() - yoriginal.getLowerBound()) / yoriginal.getLength();

				// Ensure we only zoom out to the extent of the data
				plot.getDomainAxis().setRange(domainRange.constrain(p.getX() - (fx * xr)),
						domainRange.constrain(p.getX() + (1 - fx) * xr));

				plot.getRangeAxis().setRange(rangeRange.constrain(p.getY() - (fy * yr)),
						rangeRange.constrain(p.getY() + (1 - fy) * yr));
			}
		}

	}

}
