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
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import org.jfree.svg.SVGGraphics2D;
import org.jfree.svg.SVGUtils;

import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.gui.DefaultInputSupplier;
import com.bmskinner.nma.gui.components.FileSelector;
import com.bmskinner.nma.gui.events.ChartSetEventListener;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.utility.FileUtils;
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

	private static final String EXPORT_LBL = "Export data";
	private static final String EXPORT_SVG = "SVG...";
	private static final String COPY_LBL = "Copy data";

	protected final List<Object> listeners = new ArrayList<>();

	/** Control if the axis scales should be set to maintain aspect ratio */
	protected boolean isFixedAspectRatio = false;

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
		copyItem.addActionListener(e -> copy());
		copyItem.setEnabled(true);
		popup.add(copyItem);

		JMenuItem exportItem = new JMenuItem(EXPORT_LBL);
		exportItem.addActionListener(e -> export());
		exportItem.setEnabled(true);
		popup.add(exportItem);

		JMenuItem exportSvgItem = new JMenuItem(EXPORT_SVG);
		exportSvgItem.addActionListener(e -> exportSVG());
		exportSvgItem.setEnabled(true);

		// Put the SVG export with the other save as items
		for (Component c : popup.getComponents()) {
			if (c instanceof JMenuItem t && t.getText().equals("Save as"))
				t.add(exportSvgItem);
		}

		// Ensure that the chart text and images are redrawn to
		// a proper aspect ratio when the panel is resized
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				restoreComponentRatio();
			}
		});

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

			LOGGER.finest(
					"Plot w: " + chartWidth + "; h: " + chartHeight + "; asp: " + aspectRatio);

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

	private void copy() {

		new Thread(() -> {
			String string = getChartData();
			StringSelection stringSelection = new StringSelection(string);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stringSelection, null);
		}).start();
	}

	private void export() {
		File saveFile = FileSelector.chooseTableExportFile();

		new Thread(() -> {
			String string = getChartData();

			try (PrintWriter out = new PrintWriter(saveFile)) {
				out.println(string);
			} catch (FileNotFoundException e) {
				LOGGER.warning("Cannot export to file");
				LOGGER.log(Loggable.STACK, "Error exporting", e);
			}
		}).start();
	}

	/**
	 * Export the chart in this panel as SVG
	 */
	private void exportSVG() {
		JFreeChart chart = this.getChart();
		SVGGraphics2D g2 = new SVGGraphics2D(this.getWidth(), this.getHeight());
		Rectangle r = new Rectangle(0, 0, this.getWidth(), this.getHeight());
		chart.draw(g2, r);
		try {
			File file = new DefaultInputSupplier().requestFileSave(
					FileUtils.commonPathOfDatasets(
							DatasetListManager.getInstance().getSelectedDatasets()),
					"Chart export", Io.SVG_FILE_EXTENSION_NODOT);

			if (file.exists()
					&& !new DefaultInputSupplier().requestApproval("File exists. Overwrite?",
							"Overwrite existing file?"))
				return;
			SVGUtils.writeToSVG(file, g2.getSVGElement());
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

	public class FixedAspectAdapter extends ComponentAdapter {
		@Override
		public void componentResized(ComponentEvent e) {
			restoreAutoBounds();
		}
	}

	@Override
	public void chartSetEventReceived(ChartSetEvent e) {
		// TODO Auto-generated method stub

	}

}
