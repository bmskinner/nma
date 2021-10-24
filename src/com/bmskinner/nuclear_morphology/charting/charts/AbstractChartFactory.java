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
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

import com.bmskinner.nuclear_morphology.charting.ChartComponents;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.profiles.DefaultLandmark;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;

/**
 * Base class for chart generation. Contains static methods to create loading and
 * error charts. All chart factories should extend this class.
 * @author bms41
 *
 */
public abstract class AbstractChartFactory {
	
	private static final Logger LOGGER = Logger.getLogger(AbstractChartFactory.class.getName());

	/** The X and Y axis positive & negative range magnitude for empty charts  */
    protected static final int DEFAULT_EMPTY_RANGE         = 10;
    
    /** The index profile charts begin at. Since the first index is 0, 
     * this prevents the value at zero being hidden by the chart border  */
    protected static final int DEFAULT_PROFILE_START_INDEX = -1;
    
    private static final String CHART_LOADING_LBL       = "Loading...";
    private static final String MULTI_DATASET_ERROR_LBL = "Cannot display multiple datasets";
    private static final String GENERAL_ERROR_LBL       = "Error creating chart";
    
    protected static final boolean DEFAULT_CREATE_TOOLTIPS = false;
    protected static final boolean DEFAULT_CREATE_LEGEND   = false;
    protected static final boolean DEFAULT_CREATE_URLS     = false;

    /** The options that will be used for chart generation */
    protected final ChartOptions options;

    /**
     * Create with options for the chart to be created
     * @param o the options
     */
    public AbstractChartFactory(@NonNull final ChartOptions o) {
        if (o == null)
            throw new IllegalArgumentException("Options cannot be null");
        options = o;
    }

    /**
     *  Creates an empty chart with the default range 
     * @return an empty chart
     */
    public static JFreeChart createEmptyChart() {

        JFreeChart chart = createBaseXYChart();
        XYPlot plot = chart.getXYPlot();

        plot.getDomainAxis().setRange(-DEFAULT_EMPTY_RANGE, DEFAULT_EMPTY_RANGE);
        plot.getRangeAxis().setRange(-DEFAULT_EMPTY_RANGE, DEFAULT_EMPTY_RANGE);

        plot.getDomainAxis().setVisible(false);
        plot.getRangeAxis().setVisible(false);

        return chart;

    }
    
    /**
     * Creates an empty chart with a message in the centre
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
     * @return
     */
    public static JFreeChart createLoadingChart() {
    	return createTextAnnotatedEmptyChart(CHART_LOADING_LBL);
    }
    

    /**
     * Creates an empty chart with a message that multiple datasets cannot be displayed in
     * this chart type.
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
     * Get a series or dataset index for colour selection when drawing charts.
     * The index is set in the DatasetCreator as part of the label. The format
     * is Name_index_other
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
     * @param label
     *            the charting dataset label
     * @return
     */
    public static UUID getSignalGroupFromLabel(String label) {

        if (label.startsWith(CellularComponent.NUCLEAR_SIGNAL)) {
            String[] names = label.split("_");
            return UUID.fromString(names[1]);
        }
        throw new IllegalArgumentException("Label does not start with CellularComponent.NUCLEAR_SIGNAL");

    }

    /**
     * Draw a domain marker - a vertical line - for the given border tag at the given position
     * 
     * @param plot the plot
     * @param tag the tag to use for colour selection
     * @param value the domain axis value to draw at
     */
    protected void addDomainMarkerToXYPlot(final XYPlot plot, final Landmark tag, final double value) {
        Color colour = chooseTagColour(tag);
        plot.addDomainMarker(new ValueMarker(value, colour, ChartComponents.MARKER_STROKE));
    }

    /**
     * Draw domain markers for the given border tag at the given position
     * 
     * @param plot the plot
     * @param tag the tag to use for colour selection
     * @param value the domain axis value to draw at
     */
    protected void addDomainMarkerToXYPlot(final XYPlot plot, final Landmark tag, final int value) {
        Color colour = chooseTagColour(tag);
        plot.addDomainMarker(new ValueMarker(value, colour, ChartComponents.MARKER_STROKE));
    }

    /**
     * Get the appropriate colour for rendering tag markers
     * 
     * @param tag the tag to be rendered
     * @return the colour for the tag, or black if the tag was null or unknown
     */
    private Color chooseTagColour(final Landmark tag) {
        Color colour = Color.BLACK;

        if (tag.equals(Landmark.ORIENTATION_POINT))
            colour = Color.BLUE;
        if (tag.equals(Landmark.REFERENCE_POINT))
            colour = Color.ORANGE;
        if (tag.getName().equals(Landmark.TOP_VERTICAL.toString()))
            colour = Color.GRAY;
        if (tag.getName().equals(Landmark.BOTTOM_VERTICAL.toString()))
            colour = Color.GRAY;
        if (tag.getName().equals(Landmark.LEFT_HORIZONTAL.toString()))
            colour = Color.GRAY;
        if (tag.getName().equals(Landmark.RIGHT_HORIZONTAL.toString()))
            colour = Color.GRAY;
        return colour;
    }

    /**
     * Create a new XY line Chart, with vertical orientation, and set the
     * background to white.
     * 
     * @param xLabel the x axis label
     * @param yLabel the y axis label
     * @param ds the charting dataset
     * @return a chart with default settings
     */
    protected static JFreeChart createBaseXYChart(final String xLabel, final String yLabel, final XYDataset ds) {
        JFreeChart chart = ChartFactory.createXYLineChart(null, xLabel, yLabel, ds, PlotOrientation.VERTICAL, false,
                false, false);

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);

        plot.getRenderer().setDefaultToolTipGenerator(null);
        plot.getRenderer().setURLGenerator(null);
        chart.setAntiAlias(GlobalOptions.getInstance().isAntiAlias()); // disabled for performance testing
        return chart;
    }

    /**
     * Create a new XY Line Chart, with vertical orientation, and set the
     * background to white. The charting dataset is null.
     * 
     * @param xLabel the x axis label
     * @param yLabel the y axis label
     * @return a chart with default settings
     */
    protected static JFreeChart createBaseXYChart(final String xLabel, final String yLabel) {
        return createBaseXYChart(xLabel, yLabel, null);
    }

    /**
     * Create a new XY Line Chart, with vertical orientation, and set the
     * background to white. The charting dataset is null.
     * 
     * @return
     */
    protected static JFreeChart createBaseXYChart() {
        return createBaseXYChart(null, null, null);
    }

    /**
     * Assuming there is a single XYDataset in the XYPlot of the chart, and a
     * single renderer, apply dataset colours based on position in the chart
     * options dataset list.
     * 
     * @param plot the plot to apply colours to 
     */
    protected void applySingleXYDatasetColours(final XYPlot plot) {
        int seriesCount = plot.getDataset().getSeriesCount();

        XYItemRenderer renderer = plot.getRenderer();
        for (int i = 0; i < seriesCount; i++) {
        	Paint colour = options.getDatasets().get(i)
            		.getDatasetColour().orElse(ColourSelecter.getColor(i));
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

}
