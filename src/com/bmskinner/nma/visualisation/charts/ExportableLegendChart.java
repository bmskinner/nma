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

	public ExportableLegendChart(JFreeChart chart, String exportFileName) {
		super(null, null, chart.getPlot(), true);
		this.defaultLegend = this.getLegend();
		this.exportFileName = exportFileName;
		this.removeLegend();
	}

	public ExportableLegendChart(Plot plot) {
		this(plot, DEFAULT_EXPORT_FILE_NAME);
	}

	public ExportableLegendChart(Plot plot, String exportFileName) {
		super(plot);
		defaultLegend = this.getLegend();
		this.exportFileName = exportFileName;
		this.removeLegend();
	}

	public ExportableLegendChart(String title, Font titleFont, Plot plot, boolean createLegend) {
		this(title, titleFont, plot, createLegend, DEFAULT_EXPORT_FILE_NAME);

	}

	public ExportableLegendChart(String title, Font titleFont, Plot plot, boolean createLegend, String exportFileName) {
		super(title, titleFont, plot, createLegend);
		defaultLegend = this.getLegend();
		this.exportFileName = exportFileName;
		this.removeLegend();
	}

	public ExportableLegendChart(String title, Plot plot) {
		this(title, plot, DEFAULT_EXPORT_FILE_NAME);
	}

	public ExportableLegendChart(String title, Plot plot, String exportFileName) {
		super(title, plot);
		defaultLegend = this.getLegend();
		this.exportFileName = exportFileName;
		this.removeLegend();
	}

	public void setLegendVisible(boolean b) {
		this.isLegendVisible = b;

		if (b) {
			this.addLegend(defaultLegend);
		} else {
			this.removeLegend();
		}
	}

	public void setExportFileName(String name) {
		this.exportFileName = name;
	}

	public String getExportFileName() {
		return exportFileName;
	}

}
