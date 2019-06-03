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
package com.bmskinner.nuclear_morphology.charting.charts.panels;

import java.awt.Cursor;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.logging.Logger;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.RectangleEdge;

import com.bmskinner.nuclear_morphology.charting.charts.overlays.RectangleOverlay;
import com.bmskinner.nuclear_morphology.charting.charts.overlays.RectangleOverlayObject;
import com.bmskinner.nuclear_morphology.gui.events.EventListener;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;

/**
 * This class takes a chart and adds a single draggable rectangle overlay. The
 * overlay moves with the mouse when dragged, and fires a SignalChangeEvent when
 * the overlay is released requesting that listeners update positions based on
 * the new rectangle location.
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class PositionSelectionChartPanel extends ExportableChartPanel {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    protected RectangleOverlayObject overlayRectangle = null;

    private double domainWidth;
    private double domainPct = DEFAULT_DOMAIN_PCT;

    private double rangeWidth;                   // the width of the range box
                                                 // in chart units
    private double rangePct = DEFAULT_RANGE_PCT; // the width of the range box
                                                 // as a percent of the total

    public static final int DEFAULT_DOMAIN_PCT = 10;  // 10% of x-range
    public static final int DEFAULT_RANGE_PCT  = 100; // 100% of y-range

    public PositionSelectionChartPanel(final JFreeChart chart) {
        super(chart);

        this.setRangeZoomable(false);
        this.setDomainZoomable(false);

        updateDomainWidth();
        updateRangeWidth();

        overlayRectangle = new RectangleOverlayObject(0, domainWidth, 0, rangeWidth);
        this.addOverlay(new RectangleOverlay(overlayRectangle));
    }

    public RectangleOverlayObject getOverlayRectangle() {
        return overlayRectangle;
    }

    @Override
    public synchronized void setChart(final JFreeChart chart) {
        if(chart==null) {
        	super.setChart(chart);
        	return;
        }
        double oldXPct = 0;
        double oldYPct = 0;

        if (overlayRectangle != null && chart.getXYPlot()!=null) {

            double maxX = chart.getXYPlot().getDomainAxis().getUpperBound();
            double minX = chart.getXYPlot().getDomainAxis().getLowerBound();
            double fullXRange = maxX - minX;

            double maxY = chart.getXYPlot().getRangeAxis().getUpperBound();
            double minY = chart.getXYPlot().getRangeAxis().getLowerBound();
            double fullYRange = maxY - minY;
            // LOGGER.finest( "Chart range "+fullXRange+": "+minX+" - "+maxX);
            LOGGER.finest( "Rectangle is x: " + overlayRectangle.getXMinValue() + " - " + overlayRectangle.getXMaxValue()
                    + "; y: " + overlayRectangle.getYMidValue() + " - " + overlayRectangle.getYMaxValue());
            oldXPct = (overlayRectangle.getXMidValue() - minX) / fullXRange;
            oldYPct = (overlayRectangle.getYMidValue() - minY) / fullYRange;

            LOGGER.finest( "Existing rectangle overlay midpoint (" + overlayRectangle.getXMidValue() + ") at fraction "
                    + oldXPct);
        }
        super.setChart(chart);
        updateDomainWidth();
        updateRangeWidth();

        if (overlayRectangle != null) {
            double maxX = getChart().getXYPlot().getDomainAxis().getUpperBound();
            double minX = getChart().getXYPlot().getDomainAxis().getLowerBound();
            double fullXRange = maxX - minX;
            LOGGER.finest( "New chart range " + fullXRange + ": " + minX + " - " + maxX);

            double maxY = getChart().getXYPlot().getRangeAxis().getUpperBound();
            double minY = getChart().getXYPlot().getRangeAxis().getLowerBound();
            double fullYRange = maxY - minY;

            double newXPosition = minX + (oldXPct * fullXRange);
            double newYPosition = minY + (oldYPct * fullYRange);

            double halfXRange = domainWidth / 2;
            overlayRectangle.setXMinValue(newXPosition - halfXRange);
            overlayRectangle.setXMaxValue(newXPosition + halfXRange);

            double halfYRange = rangeWidth / 2;
            overlayRectangle.setYMinValue(newYPosition - halfYRange);
            overlayRectangle.setYMaxValue(newYPosition + halfYRange);

            LOGGER.finest( "Restoring rectangle overlay midpoint to " + newXPosition + ": " + overlayRectangle.getXMinValue()
                    + " - " + overlayRectangle.getXMaxValue());
            fireSignalChangeEvent("UpdatePosition");
        }
    }

    /**
     * Update the domain width of the rectangle overlay based on the set percent
     */
    private synchronized void updateDomainWidth() {
    	if(getChart().getXYPlot()==null)
    		return;
        // Get the x bounds of the plot
        double max = getChart().getXYPlot().getDomainAxis().getUpperBound();
        double min = getChart().getXYPlot().getDomainAxis().getLowerBound();

        // Get the size of the total X range
        double fullRange = max - min;

        // Find 10% of that (in chart units)
        domainWidth = fullRange * (domainPct / 100);
    }

    /**
     * Update the range width of the rectangle overlay based on the set percent
     */
    private synchronized void updateRangeWidth() {
    	if(getChart().getXYPlot()==null)
    		return;
        // Get the bounds of the plot
        double max = getChart().getXYPlot().getRangeAxis().getUpperBound();
        double min = getChart().getXYPlot().getRangeAxis().getLowerBound();

        // Get the size of the total range
        double fullRange = max - min;

        // Find % of that (in chart units)
        rangeWidth = fullRange * (rangePct / 100);
    }

    /**
     * Set the width of the rectangle overlay if present, as a percent of the
     * total x-range
     * 
     * @param i
     *            the new percent (from 0-100)
     */
    public void setDomainPct(double i) {
        this.domainPct = i;
        updateDomainWidth();
    }

    /**
     * Set the width of the rectangle overlay if present, as a percent of the
     * total x-range
     * 
     * @param i
     *            the new percent (from 0-100)
     */
    public void setRangePct(double i) {
        this.rangePct = i;
        updateRangeWidth();
    }

    @Override
    public void mousePressed(MouseEvent e) {

        final int x = e.getX(); // These are pixel coordinates relative to the
                                // ChartPanel upper left
        final int y = e.getY();

        if (e.getButton() == MouseEvent.BUTTON1) {

            if (overlayRectangle != null) {

                mouseIsDown = true;

                if (rectangleOverlayEdgeContainsPoint(x, y, RectangleOverlayObject.X_MIN_EDGE)) {
                    // log("Left edge clicked");
                    initMinXThread();
                    return;
                }

                if (rectangleOverlayEdgeContainsPoint(x, y, RectangleOverlayObject.X_MAX_EDGE)) {
                    // log("Right edge clicked");
                    initMaxXThread();
                    return;
                }

                if (rectangleOverlayEdgeContainsPoint(x, y, RectangleOverlayObject.Y_MIN_EDGE)) {
                    // log("Bottom edge clicked");
                    initMinYThread();
                    return;
                }
                if (rectangleOverlayEdgeContainsPoint(x, y, RectangleOverlayObject.Y_MAX_EDGE)) {
                    // log("Top edge clicked");
                    initMaxYThread();
                    return;
                }

                if (rectangleOverlayContainsPoint(x, y)) {
                    initThread();
                    return;
                }

                // Move the rectangle directly over the mouse if the click was
                // not within an edge
                updateRectangleLocation(x, y);

            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {

        if (e.getButton() == MouseEvent.BUTTON1) {
            // log("Mouse released, updating listening charts");
            mouseIsDown = false;
            // overlayMoving = false;

            // Tell listening charts to update
            setDomainPct(getDomainCurrentPercent());
            setRangePct(getRangeCurrentPercent());

            fireSignalChangeEvent("UpdatePosition");
        }

    }

    @Override
    public void mouseMoved(MouseEvent e) {

        final int x = e.getX();
        final int y = e.getY();

        if (!mouseIsDown) { // Mouse is up

            if (rectangleOverlayContainsPoint(x, y)) {
                this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                return;
            }

            // Override rectangle cursor at edges
            if (rectangleOverlayEdgeContainsPoint(x, y, RectangleOverlayObject.X_MIN_EDGE)
                    || rectangleOverlayEdgeContainsPoint(x, y, RectangleOverlayObject.X_MAX_EDGE)) {
                this.setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
                return;
            }

            if (rectangleOverlayEdgeContainsPoint(x, y, RectangleOverlayObject.Y_MIN_EDGE)
                    || rectangleOverlayEdgeContainsPoint(x, y, RectangleOverlayObject.Y_MAX_EDGE)) {
                this.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
                return;
            }

            this.setCursor(Cursor.getDefaultCursor());
        }

    }

    private void updateRectangleLocation(int x, int y) {

        Rectangle2D dataArea = getScreenDataArea();
        JFreeChart chart = getChart();
        XYPlot plot = (XYPlot) chart.getPlot();
        ValueAxis xAxis = plot.getDomainAxis();
        ValueAxis yAxis = plot.getRangeAxis();

        double xUpper = xAxis.getUpperBound();
        double yUpper = yAxis.getUpperBound();

        double xLower = xAxis.getLowerBound();
        double yLower = yAxis.getLowerBound();

        double halfXRange = domainWidth / 2;
        double halfYRange = rangeWidth / 2;

        double xValue = xAxis.java2DToValue(x, dataArea, RectangleEdge.BOTTOM);

        double yValue = yAxis.java2DToValue(y, dataArea, RectangleEdge.LEFT);

        // Find the chart values to use as the rectangle x range
        double xMoveMin = xValue - halfXRange; // by default, half the box width
                                               // from the x-midpoint
        xMoveMin = xMoveMin < xLower ? xLower : xMoveMin; // correct for zero
                                                          // end

        xMoveMin = xMoveMin > xUpper - domainWidth ? xUpper - domainWidth : xMoveMin; // correct
                                                                                      // for
                                                                                      // upper
                                                                                      // end

        double xMoveMax = xValue + halfXRange;
        xMoveMax = xMoveMax < domainWidth ? domainWidth : xMoveMax; // correct
                                                                    // for zero
                                                                    // end
        xMoveMax = xMoveMax > xUpper ? xUpper : xMoveMax; // correct for upper
                                                          // end
        xMoveMax = xMoveMax > xMoveMin + domainWidth ? xMoveMin + domainWidth : xMoveMax; // correct
                                                                                          // for
                                                                                          // increasing
                                                                                          // box
                                                                                          // width
                                                                                          // when
                                                                                          // xMoveMin
                                                                                          // becomes
                                                                                          // less
                                                                                          // than
                                                                                          // zero

        // Set the values in chart units
        overlayRectangle.setXMinValue(xMoveMin);
        overlayRectangle.setXMaxValue(xMoveMax);

        // Find the chart values to use as the rectangle y range
        double yMoveMin = yValue - halfYRange;
        yMoveMin = yMoveMin < yLower ? yLower : yMoveMin; // correct for zero
                                                          // end

        yMoveMin = yMoveMin > yUpper - rangeWidth ? yUpper - rangeWidth : yMoveMin; // correct
                                                                                    // for
                                                                                    // upper
                                                                                    // end

        double yMoveMax = yValue + halfYRange;
        yMoveMax = yMoveMax < rangeWidth ? rangeWidth : yMoveMax; // correct for
                                                                  // zero end
        yMoveMax = yMoveMax > yUpper ? yUpper : yMoveMax; // correct for upper
                                                          // end
        yMoveMax = yMoveMax > yMoveMin + rangeWidth ? yMoveMin + rangeWidth : yMoveMax;

        // Set the values in chart units
        overlayRectangle.setYMinValue(yMoveMin);
        overlayRectangle.setYMaxValue(yMoveMax);

        LOGGER.finest( "Set rectangle x :" + xMoveMin + " - " + xMoveMax + ", y: " + yMoveMin + " - " + yMoveMax);
        fireSignalChangeEvent("UpdatePosition");
    }

    private void updateDomainRectangleSize(int x, boolean isLeft) {

        Rectangle2D dataArea = getScreenDataArea();
        JFreeChart chart = getChart();
        XYPlot plot = (XYPlot) chart.getPlot();
        ValueAxis xAxis = plot.getDomainAxis();
        double xUpper = xAxis.getUpperBound();
        double xLower = xAxis.getLowerBound();
        double xRange = xUpper - xLower;

        double xValue = xAxis.java2DToValue(x, dataArea, RectangleEdge.BOTTOM);

        if (isLeft) {
            xValue = xValue < xLower ? xLower : xValue; // correct for zero end

            xValue = xValue >= overlayRectangle.getXMaxValue() ? overlayRectangle.getXMaxValue() - (xRange / 100)
                    : xValue; // correct for max end

            // Set the values in chart units
            overlayRectangle.setXMinValue(xValue);
        } else {

            xValue = xValue >= xUpper ? xUpper : xValue; // correct for upper
                                                         // end

            xValue = xValue <= overlayRectangle.getXMinValue() ? overlayRectangle.getXMinValue() + (xRange / 100)
                    : xValue; // correct for min end

            // Set the values in chart units
            overlayRectangle.setXMaxValue(xValue);

        }

        fireSignalChangeEvent("UpdatePosition");
    }

    /**
     * Update the rectangle overlay to the new position
     * 
     * @param y
     *            the new endpoint
     * @param isBottom
     *            the bottom edge (true) or top edge (false)
     */
    private void updateRangeRectangleSize(int y, boolean isBottom) {

        Rectangle2D dataArea = getScreenDataArea();
        JFreeChart chart = getChart();
        XYPlot plot = (XYPlot) chart.getPlot();
        ValueAxis yAxis = plot.getRangeAxis();
        double yUpper = yAxis.getUpperBound();
        double yLower = yAxis.getLowerBound();
        double yRange = yUpper - yLower;

        double yValue = yAxis.java2DToValue(y, dataArea, RectangleEdge.LEFT);

        // This works correctly for normal y-axis orientation
        // Set the values in chart units
        if (isBottom) {
            if (yAxis.isInverted()) {
                // Y values increase from top to bottom
                // Bottom edge clicked, which is highest y values

                // Check that y does not go lower than minimum
                yValue = yValue <= overlayRectangle.getYMinValue() ? overlayRectangle.getYMinValue() + (yRange / 100)
                        : yValue;

                // Check that y does not go higher than chart maximum
                yValue = yValue >= yUpper ? yUpper : yValue;

                overlayRectangle.setYMaxValue(yValue);

            } else {

                yValue = yValue < yLower ? yLower : yValue; // correct for zero
                                                            // end

                yValue = yValue >= overlayRectangle.getYMaxValue() ? overlayRectangle.getYMaxValue() - (yRange / 100)
                        : yValue; // correct for max end

                overlayRectangle.setYMinValue(yValue);

            }

        } else {

            // Set the values in chart units
            if (yAxis.isInverted()) {
                // Y values increase from top to bottom
                // Top edge clicked, which is lowest y values

                // Check that y does not go higher than rectangle maximum
                yValue = yValue >= overlayRectangle.getYMaxValue() ? overlayRectangle.getYMaxValue() - (yRange / 100)
                        : yValue; // correct for max end

                // Check that y does not go lower than chart minimum
                yValue = yValue < yLower ? yLower : yValue;

                overlayRectangle.setYMinValue(yValue);

            } else {
                yValue = yValue >= yUpper ? yUpper : yValue; // correct for
                                                             // upper end

                yValue = yValue <= overlayRectangle.getYMinValue() ? overlayRectangle.getYMinValue() + (yRange / 100)
                        : yValue; // correct for min end

                overlayRectangle.setYMaxValue(yValue);
            }

        }

        fireSignalChangeEvent("UpdatePosition");
    }

    private double getDomainCurrentPercent() {
        double range = overlayRectangle.getXMaxValue() - overlayRectangle.getXMinValue();
        JFreeChart chart = getChart();
        XYPlot plot = (XYPlot) chart.getPlot();
        ValueAxis xAxis = plot.getDomainAxis();
        double xUpper = xAxis.getUpperBound();
        double xLower = xAxis.getLowerBound();
        double xRange = xUpper - xLower;
        double newPct = (range / xRange) * 100;

        return newPct;
    }

    private double getRangeCurrentPercent() {
        double range = overlayRectangle.getYMaxValue() - overlayRectangle.getYMinValue();
        JFreeChart chart = getChart();
        XYPlot plot = (XYPlot) chart.getPlot();
        ValueAxis yAxis = plot.getRangeAxis();
        double yUpper = yAxis.getUpperBound();
        double yLower = yAxis.getLowerBound();
        double yRange = yUpper - yLower;
        double newPct = (range / yRange) * 100;

        return newPct;
    }

    protected synchronized boolean checkAndMark() {
        if (isRunning)
            return false;
        isRunning = true;
        return true;
    }

    //
    protected void initThread() {
        if (checkAndMark()) {
            new Thread() {
                public void run() {
                    // IJ.log("Thread start : Running :"+checkRunning());
                    do {

                        /*
                         * Make the overlay under the mouse follow the mouse
                         */

                        int x = MouseInfo.getPointerInfo().getLocation().x - getLocationOnScreen().x;
                        int y = MouseInfo.getPointerInfo().getLocation().y - getLocationOnScreen().y;

                        // Move the box with the mouse
                        updateRectangleLocation(x, y);

                    } while (mouseIsDown);
                    isRunning = false;
                    // IJ.log("Thread end : Running :"+checkRunning());
                }
            }.start();
        } else {
            // IJ.log("Not starting thread: Running is "+checkRunning());
        }
    }

    protected void initMinXThread() {
        if (checkAndMark()) {
            new Thread() {
                public void run() {

                    do {

                        // Make the overlay under the mouse follow the mouse
                        int x = MouseInfo.getPointerInfo().getLocation().x - getLocationOnScreen().x;

                        // Move the box with the mouse
                        updateDomainRectangleSize(x, true);

                    } while (mouseIsDown);
                    isRunning = false;
                }
            }.start();
        }
    }

    protected void initMaxXThread() {
        if (checkAndMark()) {
            new Thread() {
                public void run() {
                    // IJ.log("Thread start : Running :"+checkRunning());
                    do {

                        /*
                         * Make the overlay under the mouse follow the mouse
                         */

                        int x = MouseInfo.getPointerInfo().getLocation().x - getLocationOnScreen().x;

                        // Move the box with the mouse
                        updateDomainRectangleSize(x, false);

                    } while (mouseIsDown);
                    isRunning = false;
                }
            }.start();
        }
    }

    protected void initMinYThread() {
        if (checkAndMark()) {
            new Thread() {
                public void run() {

                    do {

                        // Make the overlay under the mouse follow the mouse
                        int y = MouseInfo.getPointerInfo().getLocation().y - getLocationOnScreen().y;

                        // Move the box with the mouse
                        updateRangeRectangleSize(y, true);

                    } while (mouseIsDown);
                    isRunning = false;
                }
            }.start();
        }
    }

    protected void initMaxYThread() {
        if (checkAndMark()) {
            new Thread() {
                public void run() {

                    do {

                        // Make the overlay under the mouse follow the mouse
                        int y = MouseInfo.getPointerInfo().getLocation().y - getLocationOnScreen().y;

                        // Move the box with the mouse
                        updateRangeRectangleSize(y, false);

                    } while (mouseIsDown);
                    isRunning = false;
                }
            }.start();
        }
    }

    /**
     * Checks if the given cursor position is over the rectangle overlay
     * 
     * @param x
     *            the screen x coordinate
     * @param y
     *            the screen y coordinate
     * @return
     */
    private boolean rectangleOverlayContainsPoint(int x, int y) {
        Rectangle2D dataArea = this.getScreenDataArea();
        ValueAxis xAxis = this.getChart().getXYPlot().getDomainAxis();
        ValueAxis yAxis = this.getChart().getXYPlot().getRangeAxis();

        boolean isOverLine = false;

        // Turn the chart coordinates into screen coordinates
        double rectangleMinX = xAxis.valueToJava2D(overlayRectangle.getXMinValue(), dataArea, RectangleEdge.BOTTOM);

        double rectangleMaxX = xAxis.valueToJava2D(overlayRectangle.getXMaxValue(), dataArea, RectangleEdge.BOTTOM);

        double rectangleW = rectangleMaxX - rectangleMinX;

        double rectangleMinY = 0;
        double rectangleMaxY = 0;

        // Chart y-coordinates are inverse compared to screen coordinates
        if (yAxis.isInverted()) {
            rectangleMinY = yAxis.valueToJava2D(overlayRectangle.getYMinValue(), dataArea, RectangleEdge.LEFT);
            rectangleMaxY = yAxis.valueToJava2D(overlayRectangle.getYMaxValue(), dataArea, RectangleEdge.LEFT);
        } else {
            rectangleMinY = yAxis.valueToJava2D(overlayRectangle.getYMaxValue(), dataArea, RectangleEdge.LEFT);
            rectangleMaxY = yAxis.valueToJava2D(overlayRectangle.getYMinValue(), dataArea, RectangleEdge.LEFT);
        }

        double rectangleH = rectangleMaxY - rectangleMinY;

        final Rectangle bounds = new Rectangle((int) rectangleMinX, (int) rectangleMinY, (int) rectangleW,
                (int) rectangleH);

        if (bounds != null && bounds.contains(x, y)) {
            isOverLine = true;
        }
        //
        return isOverLine;
    }

    /**
     * Check if the given point overlaps the requested edge of the
     * {@link RectangleOverlayObject}
     * 
     * @param x
     *            the screen x position
     * @param y
     *            the screen y position
     * @param type
     *            the edge type
     * @return if the position overlaps the overlay edge
     * @see com.bmskinner.nuclear_morphology.charting.charts.overlays.RectangleOverlayObject
     */
    public boolean rectangleOverlayEdgeContainsPoint(int x, int y, int type) {

        Rectangle2D dataArea = this.getScreenDataArea();
        ValueAxis xAxis = this.getChart().getXYPlot().getDomainAxis();
        ValueAxis yAxis = this.getChart().getXYPlot().getRangeAxis();
        boolean isOverEdge = false;

        // Turn the chart coordinates into screen coordinates
        double rectangleMinX = 0;
        double rectangleMaxX = 0;
        double rectangleMinY = 0;
        double rectangleMaxY = 0;

        switch (type) {

        // Remember that the chart y-coordinates are inverter wrt the the screen
        // coordinates,
        // so min and max y positions are flipped here

        case RectangleOverlayObject.X_MIN_EDGE: {
            rectangleMinX = xAxis.valueToJava2D(overlayRectangle.getXMinValue(), dataArea, RectangleEdge.BOTTOM);
            rectangleMaxX = xAxis.valueToJava2D(overlayRectangle.getXMinValue(), dataArea, RectangleEdge.BOTTOM);

            // Chart y-coordinates are inverse compared to screen coordinates
            rectangleMinY = Math.min(yAxis.valueToJava2D(overlayRectangle.getYMaxValue(), dataArea, RectangleEdge.LEFT),
                    yAxis.valueToJava2D(overlayRectangle.getYMinValue(), dataArea, RectangleEdge.LEFT));

            // Chart y-coordinates are inverse compared to screen coordinates
            rectangleMaxY = Math.max(yAxis.valueToJava2D(overlayRectangle.getYMaxValue(), dataArea, RectangleEdge.LEFT),
                    yAxis.valueToJava2D(overlayRectangle.getYMinValue(), dataArea, RectangleEdge.LEFT));

            rectangleMinX -= 2;
            rectangleMaxX += 2;
            break;
        }

        case RectangleOverlayObject.Y_MIN_EDGE: {
            rectangleMinX = xAxis.valueToJava2D(overlayRectangle.getXMinValue(), dataArea, RectangleEdge.BOTTOM);
            rectangleMaxX = xAxis.valueToJava2D(overlayRectangle.getXMaxValue(), dataArea, RectangleEdge.BOTTOM);
            // Chart y-coordinates can be inverse compared to screen coordinates

            if (yAxis.isInverted()) {
                rectangleMinY = yAxis.valueToJava2D(overlayRectangle.getYMaxValue(), dataArea, RectangleEdge.LEFT);
                rectangleMaxY = yAxis.valueToJava2D(overlayRectangle.getYMaxValue(), dataArea, RectangleEdge.LEFT);
            } else {
                rectangleMinY = yAxis.valueToJava2D(overlayRectangle.getYMinValue(), dataArea, RectangleEdge.LEFT);
                rectangleMaxY = yAxis.valueToJava2D(overlayRectangle.getYMinValue(), dataArea, RectangleEdge.LEFT);
            }

            rectangleMinY -= 2;
            rectangleMaxY += 2;
            break;
        }

        case RectangleOverlayObject.X_MAX_EDGE: {
            rectangleMinX = xAxis.valueToJava2D(overlayRectangle.getXMaxValue(), dataArea, RectangleEdge.BOTTOM);
            rectangleMaxX = xAxis.valueToJava2D(overlayRectangle.getXMaxValue(), dataArea, RectangleEdge.BOTTOM);
            rectangleMinY = Math.min(yAxis.valueToJava2D(overlayRectangle.getYMaxValue(), dataArea, RectangleEdge.LEFT),
                    yAxis.valueToJava2D(overlayRectangle.getYMinValue(), dataArea, RectangleEdge.LEFT));

            // Chart y-coordinates may be inverse compared to screen coordinates
            rectangleMaxY = Math.max(yAxis.valueToJava2D(overlayRectangle.getYMaxValue(), dataArea, RectangleEdge.LEFT),
                    yAxis.valueToJava2D(overlayRectangle.getYMinValue(), dataArea, RectangleEdge.LEFT));
            rectangleMinX -= 2;
            rectangleMaxX += 2;
            break;
        }

        case RectangleOverlayObject.Y_MAX_EDGE: {
            rectangleMinX = xAxis.valueToJava2D(overlayRectangle.getXMinValue(), dataArea, RectangleEdge.BOTTOM);
            rectangleMaxX = xAxis.valueToJava2D(overlayRectangle.getXMaxValue(), dataArea, RectangleEdge.BOTTOM);
            if (yAxis.isInverted()) {
                rectangleMinY = yAxis.valueToJava2D(overlayRectangle.getYMinValue(), dataArea, RectangleEdge.LEFT);
                rectangleMaxY = yAxis.valueToJava2D(overlayRectangle.getYMinValue(), dataArea, RectangleEdge.LEFT);
            } else {
                rectangleMinY = yAxis.valueToJava2D(overlayRectangle.getYMaxValue(), dataArea, RectangleEdge.LEFT);
                rectangleMaxY = yAxis.valueToJava2D(overlayRectangle.getYMaxValue(), dataArea, RectangleEdge.LEFT);
            }
            rectangleMinY -= 2;
            rectangleMaxY += 2;
            break;
        }

        default:
            break;
        }

        double rectangleW = rectangleMaxX - rectangleMinX;
        double rectangleH = rectangleMaxY - rectangleMinY;

        final Rectangle bounds = new Rectangle((int) rectangleMinX, (int) rectangleMinY, (int) rectangleW,
                (int) rectangleH);

        if (bounds != null && bounds.contains(x, y)) {
            isOverEdge = true;
        }
        return isOverEdge;
    }

    protected synchronized void fireSignalChangeEvent(String message) {

        SignalChangeEvent event = new SignalChangeEvent(this, message, this.getClass().getSimpleName());
        Iterator<Object> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            ((EventListener) iterator.next()).eventReceived(event);
        }
    }

    public synchronized void addSignalChangeListener(EventListener l) {
        listeners.add(l);
    }

    public synchronized void removeSignalChangeListener(EventListener l) {
        listeners.remove(l);
    }

}
