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
package com.bmskinner.nuclear_morphology.charting.charts;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.util.SortOrder;

import com.bmskinner.ViolinPlots.ViolinCategoryDataset;
import com.bmskinner.ViolinPlots.ViolinRenderer;
import com.bmskinner.nuclear_morphology.charting.ChartComponents;
import com.bmskinner.nuclear_morphology.charting.datasets.ChartDatasetCreationException;
import com.bmskinner.nuclear_morphology.charting.datasets.NuclearSignalDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.ShellResultDataset;
import com.bmskinner.nuclear_morphology.charting.datasets.ViolinDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.Aggregation;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.Normalisation;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;

/**
 * Create the charts for nuclear signals
 * 
 * @author ben
 *
 */
public class ShellChartFactory extends AbstractChartFactory {
	
	private static final String SHELL_CHART_X_LABEL = "Outer <--- Shell ---> Interior";

	public ShellChartFactory(ChartOptions o) {
		super(o);
	}


	/**
	 * Create a shell chart with no data
	 * 
	 * @return
	 */
	public JFreeChart createEmptyShellChart() {
		JFreeChart shellsChart = ChartFactory.createBarChart(null, SHELL_CHART_X_LABEL, "Percent", null);
		shellsChart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
		shellsChart.getCategoryPlot().getRangeAxis().setRange(0, 100);
		return shellsChart;
	}

	/**
	 * Create an overlapping bar chart showing the signal distribution in each
	 * shell
	 * 
	 * @return a chart
	 */
	public JFreeChart createShellChart() {

		if (!options.hasDatasets())
			return createEmptyShellChart();

		if(options.isMultipleDatasets())
			return createMultipleDatasetShellBarChart();

		return createSingleDatasetShellBarChart();
	}

	/**
	 * Create a stacked bar chart of shell values from a single dataset
	 * @return
	 */
	private JFreeChart createSingleDatasetShellBarChart() {
		try {
			List<CategoryDataset> list = new NuclearSignalDatasetCreator(options).createShellBarChartDataset();

			JFreeChart chart = ChartFactory.createBarChart(null, SHELL_CHART_X_LABEL, "Percent of signal",
					list.get(0));
			chart.getCategoryPlot().setBackgroundPaint(Color.WHITE);

			chart.getCategoryPlot().addRangeMarker(ChartComponents.ZERO_MARKER);

			Range range = new Range(0, 1);

			int datasetCount = 0;
			for (CategoryDataset ds : list) {

				ShellResultDataset shellDataset = (ShellResultDataset) ds;

				chart.getCategoryPlot().setDataset(datasetCount, ds);

				IAnalysisDataset d = options.getDatasets().get(datasetCount);

				ShellResultBarRenderer rend = new ShellResultBarRenderer();
				rend.setBarPainter(new StandardBarPainter());
				rend.setShadowVisible(false);

				chart.getCategoryPlot().setRenderer(datasetCount, rend);

				for (int i = 0; i < ds.getColumnCount(); i++) {
					Comparable<String> colKey = ds.getColumnKey(i).toString();

					for (int j = 0; j < ds.getRowCount(); j++) {

						Comparable<String> rowKey = ds.getRowKey(j).toString();

						// Get the visible range of the chart
						range = Range.combine(range, shellDataset.getVisibleRange());

						UUID signalGroup = shellDataset.getSignalGroup(rowKey, colKey);

						rend.setSeriesVisibleInLegend(j, false);
						rend.setSeriesStroke(j, ChartComponents.MARKER_STROKE);

						Optional<ISignalGroup> g = d.getCollection().getSignalGroup(signalGroup);
						if(g.isPresent()){
							Paint colour = g.get().getGroupColour().orElse(ColourSelecter.getColor(j));
							rend.setSeriesPaint(j, colour);
							rend.setSeriesBarWidth(j, 1);
						}
					}
				}
				chart.getCategoryPlot().setRowRenderingOrder(SortOrder.DESCENDING); // ensure the narrower bars are on top
				datasetCount++;
			}

			chart.getCategoryPlot().getRangeAxis().setRange(range);

			String percentLabel = options.getNormalisation().equals(Normalisation.DAPI) ? "Normalised percent" : "Percent";
			String locationLabel = options.getAggregation().equals(Aggregation.BY_NUCLEUS) ? Labels.NUCLEI : "signal borders";

			chart.getCategoryPlot().getRangeAxis().setLabel(percentLabel + " pixel intensity within " + locationLabel);

			return chart;
		} catch (ChartDatasetCreationException e) {
			return createErrorChart();
		}
	}

	/**
	 * Create a stacked bar chart of shell values from a single dataset
	 * @return
	 */
	private JFreeChart createMultipleDatasetShellBarChart() {
		try {
			XYZDataset xyz = new NuclearSignalDatasetCreator(options).createMultipleDatasetShellHeatMapDataset();

			// create a paint-scale and a legend showing it
			LinearPaintScale paintScale = new LinearPaintScale(0,1);

	        PaintScaleLegend psl = new PaintScaleLegend(paintScale, new NumberAxis());
	        psl.setPosition(RectangleEdge.RIGHT);
	        psl.setAxisLocation(AxisLocation.TOP_OR_RIGHT);
	        psl.setMargin(50.0, 20.0, 80.0, 0.0);
	        
	        NumberAxis xAxis = new NumberAxis(SHELL_CHART_X_LABEL);
	        xAxis.setLowerBound(-0.5);
	        xAxis.setUpperBound(4.5);
	        xAxis.setVisible(true);
	        xAxis.setTickUnit(new NumberTickUnit(1.0));
	        
	        String labels[] = new String[xyz.getSeriesCount()];
	        for (int i = 0; i<xyz.getSeriesCount(); i++)
	            labels[i] = xyz.getSeriesKey(i).toString().replaceAll("_Series_\\d+$", ""); // Series added in case datasets have same name
	        SymbolAxis yAxis = new SymbolAxis(null, labels);

	        // finally a renderer and a plot
	        XYBlockRenderer renderer = new XYBlockRenderer();
	        renderer.setPaintScale(paintScale);
	        
	        XYPlot plot = new XYPlot(xyz, xAxis, yAxis, renderer);

	        JFreeChart chart = new JFreeChart(null, null, plot, false);
	        chart.addSubtitle(psl);
	        return chart;
		} catch (ChartDatasetCreationException e) {
			return createErrorChart();
		}
	}
	
	/**
	 * A linear interpolating paint scale
	 * @author ben
	 *
	 */
	private static class LinearPaintScale implements PaintScale {

        private final double lowerBound;
        private final double upperBound;

        public LinearPaintScale(double lowerBound, double upperBound) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }

        @Override
        public double getLowerBound() {
            return lowerBound;
        }

        @Override
        public double getUpperBound() {
            return upperBound;
        }

        @Override
        public Paint getPaint(double value) {
            int greyVal = (int) ( (1-value)*255);
            return new Color(greyVal, greyVal, greyVal);
        }
    }


	

}
