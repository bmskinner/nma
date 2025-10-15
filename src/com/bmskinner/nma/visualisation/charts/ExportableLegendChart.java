package com.bmskinner.nma.visualisation.charts;

import java.awt.Font;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.title.LegendTitle;

/**
 * An extension to JFreeChart that allows the legend visibility to be toggled
 * for exported images.
 * 
 */
public class ExportableLegendChart extends JFreeChart {

	private static final String DEFAULT_EXPORT_FILE_NAME = "Chart export";

	private boolean isLegendVisible = false;
	private final LegendTitle defaultLegend;

	private String exportFileName;

	public ExportableLegendChart(JFreeChart chart) {
		this(chart, DEFAULT_EXPORT_FILE_NAME);
	}

	/**
	 * Create with an underlying chart. Any existing legend is removed.
	 * 
	 * @param chart          the chart to create from.
	 * @param exportFileName the default name to suggest for exported image files
	 */
	public ExportableLegendChart(JFreeChart chart, String exportFileName) {
		super(null, null, chart.getPlot(), true);
		this.defaultLegend = this.getLegend();
		this.exportFileName = exportFileName;
		this.removeLegend();
	}

	/**
	 * Create with an underlying plot.
	 * 
	 * @param plot the plot to create from
	 */
	public ExportableLegendChart(Plot plot) {
		this(plot, DEFAULT_EXPORT_FILE_NAME);
	}

	/**
	 * Create with an underlying plot.
	 * 
	 * @param plot           the plot to create from
	 * @param exportFileName the default name to suggest for exported image files
	 */
	public ExportableLegendChart(Plot plot, String exportFileName) {
		super(plot);
		defaultLegend = this.getLegend();
		this.exportFileName = exportFileName;
		this.removeLegend();
	}

	/**
	 * Create with a plot, specifying title
	 * 
	 * @param title the chart title
	 * @param plot  the plot to create from
	 */
	public ExportableLegendChart(String title, Plot plot) {
		this(title, plot, DEFAULT_EXPORT_FILE_NAME);
	}

	/**
	 * Create with a plot, specifying title
	 * 
	 * @param title          the chart title
	 * @param plot           the plot to create from
	 * @param exportFileName the default name to suggest for exported image files
	 */
	public ExportableLegendChart(String title, Plot plot, String exportFileName) {
		super(title, plot);
		defaultLegend = this.getLegend();
		this.exportFileName = exportFileName;
		this.removeLegend();
	}

	/**
	 * Create with a plot, specifying title and fond
	 * 
	 * @param title        the chart title
	 * @param titleFont    the chart font
	 * @param plot         the plot to create from
	 * @param createLegend should the legend be created
	 */
	public ExportableLegendChart(String title, Font titleFont, Plot plot, boolean createLegend) {
		this(title, titleFont, plot, createLegend, DEFAULT_EXPORT_FILE_NAME);

	}

	/**
	 * Create with a plot, specifying title and fond
	 * 
	 * @param title          the chart title
	 * @param titleFont      the chart font
	 * @param plot           the plot to create from
	 * @param createLegend   should the legend be created
	 * @param exportFileName the default name to suggest for exported image files
	 */
	public ExportableLegendChart(String title, Font titleFont, Plot plot, boolean createLegend, String exportFileName) {
		super(title, titleFont, plot, createLegend);
		defaultLegend = this.getLegend();
		this.exportFileName = exportFileName;
		this.removeLegend();
	}

	/**
	 * Toggle whether the legend is visible. Removes or adds the legend from the
	 * plot.
	 * 
	 * @param b the legend visibility. True if the legend is visible, false
	 *          otherwise.
	 */
	public void setLegendVisible(boolean b) {
		this.isLegendVisible = b;

		if (b) {
			// Don't add another legend if there is already one present
			if (this.getLegend() == null) {
				this.addLegend(defaultLegend);
			}
		} else {
			this.removeLegend();
		}
	}

	/**
	 * Set the default name for exported images
	 * 
	 * @param name
	 */
	public void setExportFileName(String name) {
		this.exportFileName = name;
	}

	/**
	 * Get the default name for exported images
	 * 
	 * @return
	 */
	public String getExportFileName() {
		return exportFileName;
	}

}
