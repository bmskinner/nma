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
package com.bmskinner.nuclear_morphology.charting.charts.overlays;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;

public class EllipticalOverlayObject extends OverlayObject {

    private double xValue;
    private double xRadius;
    private double yValue;
    private double yRadius;

    public EllipticalOverlayObject(double xValue, double xRadius, double yValue, double yRadius) {
        this(xValue, xRadius, yValue, yRadius, new BasicStroke(1f), Color.BLACK);
    }

    public EllipticalOverlayObject(double xValue, double xRadius, double yValue, double yRadius, Stroke stroke,
            Paint outline) {
        this(xValue, xRadius, yValue, yRadius, stroke, outline, new Color(100, 100, 100, 128));
    }

    public EllipticalOverlayObject(double xValue, double xRadius, double yValue, double yRadius, Stroke stroke,
            Paint outline, Paint fill) {
        super(stroke, outline, fill);
        this.xValue = xValue;
        this.xRadius = xRadius;
        this.yValue = yValue;
        this.yRadius = yRadius;
    }

    public double getXValue() {
        return xValue;
    }

    public void setXValue(double value) {
        double oldValue = this.xValue;
        this.xValue = value;
        this.pcs.firePropertyChange("xValue", oldValue, value);
    }

    public double getXRadius() {
        return xRadius;
    }

    public void setXRadius(double value) {
        double oldValue = this.xRadius;
        this.xRadius = value;
        this.pcs.firePropertyChange("xRadius", oldValue, value);
    }

    public double getYValue() {
        return yValue;
    }

    public void setYValue(double value) {
        double oldValue = this.yValue;
        this.yValue = value;
        this.pcs.firePropertyChange("yValue", oldValue, value);
    }

    public double getYRadius() {
        return yRadius;
    }

    public void setYRadius(double value) {
        double oldValue = this.yRadius;
        this.yRadius = value;
        this.pcs.firePropertyChange("yRadius", oldValue, value);
    }

    @Override
    public boolean contains(int x, int y) {

        Ellipse2D e = new Ellipse2D.Double(xValue - xRadius, yValue - yRadius, xRadius * 2, yRadius * 2);
        return e.contains(x, y);
    }

    @Override
    public boolean contains(double x, double y) {
        Ellipse2D e = new Ellipse2D.Double(xValue - xRadius, yValue - yRadius, xRadius * 2, yRadius * 2);
        return e.contains(x, y);
    }

}
