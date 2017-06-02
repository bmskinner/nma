/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.charting.charts;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.List;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.ui.RectangleEdge;

import com.bmskinner.nuclear_morphology.charting.datasets.ViolinCategoryDataset;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Max;

/**
 * The ViolinRenderer draws a boxplot with a probability density function
 * surrounding the box. It provides better visualisation of multimodal data than
 * the boxplot. It takes a MultiValueCategoryDataset, which contains the PDF at
 * appropriate intervals across the range
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class ViolinRenderer extends BoxAndWhiskerRenderer implements Loggable {

    // /** The color used to paint the median line and average marker. */
    // private transient Paint artifactPaint;
    //
    // /** A flag that controls whether or not the box is filled. */
    // private boolean fillBox;
    //
    // /** The margin between items (boxes) within a category. */
    // private double itemMargin;

    /**
     * Default constructor.
     */
    public ViolinRenderer() {
        super();
        this.setMaximumBarWidth(0.5);
        this.setMeanVisible(false);
        this.setWhiskerWidth(0.05);
        this.setUseOutlinePaintForWhiskers(true);

    }

    @Override
    public void drawItem(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea, CategoryPlot plot,
            CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset, int row, int column, int pass) {

        if (!(dataset instanceof ViolinCategoryDataset)) {
            throw new IllegalArgumentException(
                    "ViolinRenderer.drawItem() : the data should be " + "of type ViolinCategoryDataset only.");
        }

        PlotOrientation orientation = plot.getOrientation();

        // log("Drawing item "+row+" - "+column);
        if (orientation == PlotOrientation.HORIZONTAL) {
            // TODO
            // drawHorizontalItem(g2, state, dataArea, plot, domainAxis,
            // rangeAxis, dataset, row, column);
            log("Horizontal not supported");
        } else if (orientation == PlotOrientation.VERTICAL) {
            drawVerticalItem(g2, state, dataArea, plot, domainAxis, rangeAxis, dataset, row, column);
        }

    }

    private double calculateStepSize(ViolinCategoryDataset dataset, int row, int column) {

        double stepSize = Double.NaN;
        // log("Getting min and max y values");
        try {
            double ymax = dataset.getMax(row, column);
            // log("Got ymax: "+ymax);
            double ymin = dataset.getMin(row, column);

            // log("Got ymin: "+ymin);

            List<Number> values = dataset.getPdfValues(row, column);

            if (values != null) {
                stepSize = (ymax - ymin) / values.size();
            }

        } catch (Exception e) {
            error("Error in step size", e);
        }

        return stepSize;
    }

    private Shape makeViolinPath(ViolinCategoryDataset dataset, int row, int column, CategoryItemRendererState state,
            Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis) {

        double stepSize = calculateStepSize(dataset, row, column);

        double categoryEnd = domainAxis.getCategoryEnd(column, getColumnCount(), dataArea, plot.getDomainAxisEdge());
        double categoryStart = domainAxis.getCategoryStart(column, getColumnCount(), dataArea,
                plot.getDomainAxisEdge());
        double categoryWidth = categoryEnd - categoryStart;

        double xx = categoryStart; //
        int seriesCount = getRowCount();
        int categoryCount = getColumnCount();

        // log("Plot has "+seriesCount+" series and "+categoryCount+"
        // categories");
        //
        // log("Series is"+dataset.getRowKey(row)+" -
        // "+dataset.getColumnKey(column));

        if (seriesCount > 1) {

            double seriesGap = dataArea.getWidth() * getItemMargin() / (categoryCount * (seriesCount - 1));
            double usedWidth = (state.getBarWidth() * seriesCount) + (seriesGap * (seriesCount - 1));
            // offset the start of the boxes if the total width used is smaller
            // than the category width
            double offset = (categoryWidth - usedWidth) / 2;
            xx = xx + offset + (row * (state.getBarWidth() + seriesGap));

        } else {
            // offset the start of the box if the box width is smaller than the
            // category width
            double offset = (categoryWidth - state.getBarWidth()) / 2;
            xx = xx + offset;
        }

        double xxmid = xx + state.getBarWidth() / 2d; // the middle of the
                                                      // active bar
        double xxrange = state.getBarWidth();

        // Get the y-values that must be used for step calculations
        double yValMin = dataset.getMin(row, column); // the lowest y-value in
                                                      // the dataset
        double yValMax = dataset.getMax(row, column); // the highest y-value in
                                                      // the dataset

        RectangleEdge location = plot.getRangeAxisEdge();

        Path2D leftShape = new Path2D.Double();

        double yValPos = yValMin;

        leftShape.moveTo(xxmid, rangeAxis.valueToJava2D(yValPos, dataArea, location)); // start
                                                                                       // with
                                                                                       // the
                                                                                       // lowest
                                                                                       // value

        double maxProbability = new Max(dataset.getPdfValues(row, column)).doubleValue(); // Stats.max().doubleValue();

        List<Number> values = dataset.getPdfValues(row, column);
        for (Number v : values) {

            // Get the y position on the chart
            yValPos += stepSize;

            double yy = rangeAxis.valueToJava2D(yValPos, dataArea, location); // convert
                                                                              // y
                                                                              // values
                                                                              // to
                                                                              // to
                                                                              // java
                                                                              // coordinates

            // Get the x position on the chart
            double xValPos = (v.doubleValue() / maxProbability); // Express the
                                                                 // proability
                                                                 // as a
                                                                 // fraction of
                                                                 // the bar
                                                                 // width

            double xxPosL = xxmid + ((xxrange / 2) * xValPos); // multiply the
                                                               // fraction by
                                                               // the bar width
                                                               // to get x
                                                               // position

            // Add to the line
            leftShape.lineTo(xxPosL, yy);

        }

        // Move back to the x-midpoint at the highest value in the y-axis
        leftShape.lineTo(xxmid, rangeAxis.valueToJava2D(yValMax, dataArea, location)); // start
                                                                                       // with
                                                                                       // the
                                                                                       // lowest
                                                                                       // value

        // Now do the same on the other side of the midpoint
        Collections.reverse(values);
        leftShape.lineTo(xxmid, rangeAxis.valueToJava2D(yValMax, dataArea, location)); // start
                                                                                       // with
                                                                                       // the
                                                                                       // lowest
                                                                                       // value

        for (Number v : values) {

            double yy = rangeAxis.valueToJava2D(yValPos, dataArea, location); // convert
                                                                              // y
                                                                              // values
                                                                              // to
                                                                              // to
                                                                              // java
                                                                              // coordinates

            // Get the x position on the chart

            double xValPos = (v.doubleValue() / maxProbability); // Express the
                                                                 // proability
                                                                 // as a
                                                                 // fraction of
                                                                 // the bar
                                                                 // width

            double xxPosL = xxmid - ((xxrange / 2) * xValPos); // multiply the
                                                               // fraction by
                                                               // the bar width
                                                               // to get x
                                                               // position

            // Add to the line
            leftShape.lineTo(xxPosL, yy);

            // Get the y position on the chart
            yValPos -= stepSize;

            // log("Drawing to "+xxPosL+" - "+yy);

        }

        leftShape.closePath();
        Collections.reverse(values); // restore original order
        return leftShape;
    }

    /**
     * Draws the visual representation of a single data item when the plot has a
     * vertical orientation.
     *
     * @param g2
     *            the graphics device.
     * @param state
     *            the renderer state.
     * @param dataArea
     *            the area within which the plot is being drawn.
     * @param plot
     *            the plot (can be used to obtain standard color information
     *            etc).
     * @param domainAxis
     *            the domain axis.
     * @param rangeAxis
     *            the range axis.
     * @param dataset
     *            the dataset.
     * @param row
     *            the row index (zero-based).
     * @param column
     *            the column index (zero-based).
     */
    public void drawVerticalItem(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea,
            CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset, int row,
            int column) {

        ViolinCategoryDataset vioDataset = (ViolinCategoryDataset) dataset;

        if (vioDataset.hasProbabilities(row, column)) {

            // Draw the pdf behind of the boxplot

            Shape leftShape = makeViolinPath(vioDataset, row, column, state, dataArea, plot, domainAxis, rangeAxis);

            Paint p = getItemPaint(row, column);
            Color c = (Color) p;

            Color tr = new Color(c.getRed(), c.getGreen(), c.getBlue(), 128); // make
                                                                              // the
                                                                              // pdf
                                                                              // transparent
                                                                              // version
                                                                              // of
                                                                              // dataset
                                                                              // colour

            g2.setPaint(tr);

            Stroke s = getItemStroke(row, column);
            g2.setStroke(s);

            g2.fill(leftShape);
            g2.draw(leftShape);
        }

        drawVerticalBoxplotItem(g2, state, dataArea, plot, domainAxis, rangeAxis, dataset, row, column);

    }

    /**
     * Draws the visual representation of a single data item when the plot has a
     * vertical orientation.
     *
     * @param g2
     *            the graphics device.
     * @param state
     *            the renderer state.
     * @param dataArea
     *            the area within which the plot is being drawn.
     * @param plot
     *            the plot (can be used to obtain standard color information
     *            etc).
     * @param domainAxis
     *            the domain axis.
     * @param rangeAxis
     *            the range axis.
     * @param dataset
     *            the dataset (must be an instance of
     *            {@link BoxAndWhiskerCategoryDataset}).
     * @param row
     *            the row index (zero-based).
     * @param column
     *            the column index (zero-based).
     */
    public void drawVerticalBoxplotItem(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea,
            CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset, int row,
            int column) {

        BoxAndWhiskerCategoryDataset bawDataset = (BoxAndWhiskerCategoryDataset) dataset;

        double categoryEnd = domainAxis.getCategoryEnd(column, getColumnCount(), dataArea, plot.getDomainAxisEdge());
        double categoryStart = domainAxis.getCategoryStart(column, getColumnCount(), dataArea,
                plot.getDomainAxisEdge());
        double categoryWidth = categoryEnd - categoryStart;

        double xx = categoryStart;
        int seriesCount = getRowCount();
        int categoryCount = getColumnCount();

        if (seriesCount > 1) {
            double seriesGap = dataArea.getWidth() * getItemMargin() / (categoryCount * (seriesCount - 1));
            double usedWidth = (state.getBarWidth() * seriesCount) + (seriesGap * (seriesCount - 1));
            // offset the start of the boxes if the total width used is smaller
            // than the category width
            double offset = (categoryWidth - usedWidth) / 2;
            xx = xx + offset + (row * (state.getBarWidth() + seriesGap));
        } else {
            // offset the start of the box if the box width is smaller than the
            // category width
            double offset = (categoryWidth - state.getBarWidth()) / 2;
            xx = xx + offset;
        }

        double yyAverage;

        Paint itemPaint = getItemPaint(row, column);
        g2.setPaint(itemPaint);
        Stroke s = getItemStroke(row, column);
        g2.setStroke(s);

        double aRadius = 0; // average radius

        RectangleEdge location = plot.getRangeAxisEdge();

        Number yQ1 = bawDataset.getQ1Value(row, column);
        Number yQ3 = bawDataset.getQ3Value(row, column);
        Number yMax = bawDataset.getMaxRegularValue(row, column);
        Number yMin = bawDataset.getMinRegularValue(row, column);
        Shape box = null;

        // Get values for the box that are half the normal box width
        double barSixth = state.getBarWidth() / 6.0;
        double barThird = barSixth * 2d;
        double xxq1 = xx + barThird;
        double xxq3 = xxq1 + barThird;

        if (yQ1 != null && yQ3 != null && yMax != null && yMin != null) {

            double yyQ1 = rangeAxis.valueToJava2D(yQ1.doubleValue(), dataArea, location);
            double yyQ3 = rangeAxis.valueToJava2D(yQ3.doubleValue(), dataArea, location);
            double yyMax = rangeAxis.valueToJava2D(yMax.doubleValue(), dataArea, location);
            double yyMin = rangeAxis.valueToJava2D(yMin.doubleValue(), dataArea, location);
            double xxmid = xx + state.getBarWidth() / 2.0;

            double halfW = (state.getBarWidth() / 2.0) * this.getWhiskerWidth();

            // draw the body...
            // box = new Rectangle2D.Double(xx, Math.min(yyQ1, yyQ3),
            // state.getBarWidth(), Math.abs(yyQ1 - yyQ3));
            box = new Rectangle2D.Double(xxq1, Math.min(yyQ1, yyQ3), barThird, Math.abs(yyQ1 - yyQ3));
            if (this.getFillBox()) {
                g2.fill(box);
            }

            Paint outlinePaint = getItemOutlinePaint(row, column);
            if (this.getUseOutlinePaintForWhiskers()) {
                g2.setPaint(outlinePaint);
            }
            // draw the upper shadow...
            g2.draw(new Line2D.Double(xxmid, yyMax, xxmid, yyQ3));
            g2.draw(new Line2D.Double(xxmid - halfW, yyMax, xxmid + halfW, yyMax));

            // draw the lower shadow...
            g2.draw(new Line2D.Double(xxmid, yyMin, xxmid, yyQ1));
            g2.draw(new Line2D.Double(xxmid - halfW, yyMin, xxmid + halfW, yyMin));

            g2.setStroke(getItemOutlineStroke(row, column));
            g2.setPaint(outlinePaint);
            g2.draw(box);
        }

        g2.setPaint(this.getArtifactPaint());

        // draw mean - SPECIAL AIMS REQUIREMENT...
        if (this.isMeanVisible()) {
            Number yMean = bawDataset.getMeanValue(row, column);
            if (yMean != null) {
                yyAverage = rangeAxis.valueToJava2D(yMean.doubleValue(), dataArea, location);
                aRadius = state.getBarWidth() / 4;
                // here we check that the average marker will in fact be
                // visible before drawing it...
                if ((yyAverage > (dataArea.getMinY() - aRadius)) && (yyAverage < (dataArea.getMaxY() + aRadius))) {
                    Ellipse2D.Double avgEllipse = new Ellipse2D.Double(xx + aRadius, yyAverage - aRadius, aRadius * 2,
                            aRadius * 2);
                    g2.fill(avgEllipse);
                    g2.draw(avgEllipse);
                }
            }
        }

        // draw median...
        if (this.isMedianVisible()) {
            Number yMedian = bawDataset.getMedianValue(row, column);
            if (yMedian != null) {
                double yyMedian = rangeAxis.valueToJava2D(yMedian.doubleValue(), dataArea, location);
                g2.draw(new Line2D.Double(xxq1, yyMedian, xxq3, yyMedian));
            }
        }

        // collect entity and tool tip information...
        if (state.getInfo() != null && box != null) {
            EntityCollection entities = state.getEntityCollection();
            if (entities != null) {
                addItemEntity(entities, dataset, row, column, box);
            }
        }

    }

}
