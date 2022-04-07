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
package com.bmskinner.nuclear_morphology.visualisation.charts.overlays;

import java.awt.Paint;
import java.awt.Stroke;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public abstract class OverlayObject {

    protected Stroke                stroke;
    protected Paint                 fill;
    protected Paint                 outline;
    protected boolean               isVisible;
    protected PropertyChangeSupport pcs;

    public OverlayObject(Stroke stroke, Paint outline, Paint fill) {
        this.stroke = stroke;
        this.outline = outline;
        this.fill = fill;
        this.isVisible = true;
        this.pcs = new PropertyChangeSupport(this);
    }

    public Stroke getStroke() {
        return stroke;
    }

    public void setStroke(Stroke stroke) {
        this.stroke = stroke;
    }

    public Paint getFill() {
        return fill;
    }

    public void setFill(Paint fill) {
        this.fill = fill;
    }

    public Paint getOutline() {
        return outline;
    }

    public void setOutline(Paint outline) {
        this.outline = outline;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    /**
     * Adds a property change listener.
     *
     * @param l
     *            the listener.
     */
    public void addPropertyChangeListener(PropertyChangeListener l) {
        this.pcs.addPropertyChangeListener(l);
    }

    /**
     * Removes a property change listener.
     *
     * @param l
     *            the listener.
     */
    public void removePropertyChangeListener(PropertyChangeListener l) {
        this.pcs.removePropertyChangeListener(l);
    }

    /**
     * Test if the given point (in chart coordinates) lies within this overlay
     * 
     * @param x
     * @param y
     * @return
     */
    public abstract boolean contains(int x, int y);

    /**
     * Test if the given point (in chart coordinates) lies within this overlay
     * 
     * @param x
     * @param y
     * @return
     */
    public abstract boolean contains(double x, double y);

}
