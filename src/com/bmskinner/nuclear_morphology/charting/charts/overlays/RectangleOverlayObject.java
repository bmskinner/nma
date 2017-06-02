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


package com.bmskinner.nuclear_morphology.charting.charts.overlays;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;

public class RectangleOverlayObject extends OverlayObject {

    private double xMinValue;
    private double xMaxValue;
    private double yMinValue;
    private double yMaxValue;

    public static final int X_MIN_EDGE = 0;
    public static final int X_MAX_EDGE = 1;
    public static final int Y_MIN_EDGE = 2;
    public static final int Y_MAX_EDGE = 3;

    public RectangleOverlayObject(double xMin, double xMax, double yMin, double yMax) {
        this(xMin, xMax, yMin, yMax, new BasicStroke(1f), Color.BLACK);
    }

    public RectangleOverlayObject(double xMin, double xMax, double yMin, double yMax, Stroke stroke, Paint outline) {
        this(xMin, xMax, yMin, yMax, stroke, outline, new Color(100, 100, 100, 128));
    }

    public RectangleOverlayObject(double xMin, double xMax, double yMin, double yMax, Stroke stroke, Paint outline,
            Paint fill) {
        super(stroke, outline, fill);
        this.xMinValue = xMin;
        this.xMaxValue = xMax;
        this.yMinValue = yMin;
        this.yMaxValue = yMax;
    }

    public double getXMinValue() {
        return xMinValue;
    }

    public void setXMinValue(double minValue) {
        double oldValue = this.xMinValue;
        this.xMinValue = minValue;
        this.pcs.firePropertyChange("minXValue", oldValue, minValue);
    }

    public double getXMaxValue() {
        return xMaxValue;
    }

    public void setXMaxValue(double maxValue) {
        double oldValue = this.xMaxValue;
        this.xMaxValue = maxValue;
        this.pcs.firePropertyChange("maxXValue", oldValue, maxValue);
    }

    public double getXMidValue() {
        return ((xMaxValue - xMinValue) / 2) + xMinValue;
    }

    public double getYMinValue() {
        return yMinValue;
    }

    public void setYMinValue(double minValue) {
        double oldValue = this.yMinValue;
        this.yMinValue = minValue;
        this.pcs.firePropertyChange("minYValue", oldValue, minValue);
    }

    public double getYMaxValue() {
        return yMaxValue;
    }

    public void setYMaxValue(double maxValue) {
        double oldValue = this.yMaxValue;
        this.yMaxValue = maxValue;
        this.pcs.firePropertyChange("maxYValue", oldValue, maxValue);
    }

    public double getYMidValue() {
        return ((yMaxValue - yMinValue) / 2) + yMinValue;
    }

    @Override
    public boolean contains(int x, int y) {
        Rectangle2D e = new Rectangle2D.Double(xMinValue, yMinValue, xMaxValue - xMinValue, yMaxValue - yMinValue);
        return e.contains(x, y);
    }

    @Override
    public boolean contains(double x, double y) {
        Rectangle2D e = new Rectangle2D.Double(xMinValue, yMinValue, xMaxValue - xMinValue, yMaxValue - yMinValue);
        return e.contains(x, y);
    }

}
