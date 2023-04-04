package com.bmskinner.nma.io;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

import com.bmskinner.nma.visualisation.datasets.ExportableBoxAndWhiskerCategoryDataset;
import com.bmskinner.nma.visualisation.datasets.ShellResultDataset;

public class ChartDataExtracter {

	private ChartDataExtracter() {
		// static use only
	}

	public static String getHeatMapData(JFreeChart chart) throws ClassCastException {
		XYPlot plot = chart.getXYPlot();
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

	public static String getShellData(JFreeChart chart) throws ClassCastException {
		CategoryPlot plot = chart.getCategoryPlot();
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
	public static String getXYData(JFreeChart chart) throws ClassCastException {
		XYPlot plot = chart.getXYPlot();
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

	public static String getBoxplotData(JFreeChart chart) throws ClassCastException {

		CategoryPlot plot = chart.getCategoryPlot();
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

	public static String getHistogramData(JFreeChart chart) throws ClassCastException {

		XYPlot plot = chart.getXYPlot();
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

}
