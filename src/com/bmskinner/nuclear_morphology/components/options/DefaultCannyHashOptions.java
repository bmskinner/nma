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


package com.bmskinner.nuclear_morphology.components.options;

/**
 * An implementation of the canny options using the hash options
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class DefaultCannyHashOptions extends AbstractHashOptions implements ICannyOptions {

    private static final long serialVersionUID = 1L;

    public DefaultCannyHashOptions() {
        setBoolean(IS_USE_CANNY, DEFAULT_USE_CANNY);

        setCannyAutoThreshold(DEFAULT_AUTO_THRESHOLD);
        this.setFlattenImage(DEFAULT_FLATTEN_CHROMOCENTRES);
        this.setFlattenThreshold(DEFAULT_FLATTEN_THRESHOLD);
        this.setUseKuwahara(DEFAULT_USE_KUWAHARA);
        this.setKuwaharaKernel(DEFAULT_KUWAHARA_KERNEL_RADIUS);

        this.setLowThreshold(DEFAULT_CANNY_LOW_THRESHOLD);
        this.setHighThreshold(DEFAULT_CANNY_HIGH_THRESHOLD);
        this.setKernelRadius(DEFAULT_CANNY_KERNEL_RADIUS);
        this.setKernelWidth(DEFAULT_CANNY_KERNEL_WIDTH);
        this.setClosingObjectRadius(DEFAULT_CLOSING_OBJECT_RADIUS);
        this.setAddBorder(DEFAULT_ADD_BORDER);

    }

    /**
     * Construct using a canny options as a template
     * 
     * @param o
     */
    public DefaultCannyHashOptions(ICannyOptions o) {
        setUseCanny(o.isUseCanny());
        setCannyAutoThreshold(o.isCannyAutoThreshold());
        this.setFlattenImage(o.isUseFlattenImage());
        this.setFlattenThreshold(o.getFlattenThreshold());
        this.setUseKuwahara(o.isUseKuwahara());
        this.setKuwaharaKernel(o.getKuwaharaKernel());

        this.setLowThreshold(o.getLowThreshold());
        this.setHighThreshold(o.getHighThreshold());
        this.setKernelRadius(o.getKernelRadius());
        this.setKernelWidth(o.getKernelWidth());
        this.setClosingObjectRadius(o.getClosingObjectRadius());
        this.setAddBorder(o.isAddBorder());
    }

    @Override
    public boolean isUseCanny() {
        return getBoolean(IS_USE_CANNY);
    }

    @Override
    public boolean isUseFlattenImage() {
        return getBoolean(IS_FLATTEN_CHROMOCENTRE);
    }

    @Override
    public int getFlattenThreshold() {
        return getInt(FLATTEN_THRESHOLD);
    }

    @Override
    public boolean isUseKuwahara() {
        return getBoolean(IS_USE_KUWAHARA);
    }

    @Override
    public int getKuwaharaKernel() {
        return getInt(KUWAHARA_RADIUS);
    }

    @Override
    public int getClosingObjectRadius() {
        return getInt(CLOSING_RADIUS);
    }

    @Override
    public boolean isCannyAutoThreshold() {
        return getBoolean(IS_AUTO_THRESHOLD);
    }

    @Override
    public float getLowThreshold() {
        return getFloat(CANNY_LOW_THRESHOLD);
    }

    @Override
    public float getHighThreshold() {
        return getFloat(CANNY_HIGH_THRESHOLD);
    }

    @Override
    public float getKernelRadius() {
        return getFloat(CANNY_KERNEL_RADIUS);
    }

    @Override
    public int getKernelWidth() {
        return getInt(CANNY_KERNEL_WIDTH);
    }

    @Override
    public boolean isAddBorder() {
        return getBoolean(IS_ADD_BORDER);
    }

    @Override
    public ICannyOptions duplicate() {
        return new DefaultCannyHashOptions(this);
    }

    @Override
    public void setUseCanny(boolean useCanny) {
        setBoolean(IS_USE_CANNY, useCanny);
    }

    @Override
    public void setFlattenImage(boolean flattenImage) {
        setBoolean(IS_FLATTEN_CHROMOCENTRE, flattenImage);
    }

    @Override
    public void setFlattenThreshold(int flattenThreshold) {
        setInt(FLATTEN_THRESHOLD, flattenThreshold);

    }

    @Override
    public void setUseKuwahara(boolean b) {
        setBoolean(IS_USE_KUWAHARA, b);

    }

    @Override
    public void setKuwaharaKernel(int radius) {
        setInt(KUWAHARA_RADIUS, radius);

    }

    @Override
    public void setClosingObjectRadius(int closingObjectRadius) {
        setInt(CLOSING_RADIUS, closingObjectRadius);

    }

    @Override
    public void setCannyAutoThreshold(boolean cannyAutoThreshold) {
        setBoolean(IS_AUTO_THRESHOLD, cannyAutoThreshold);

    }

    @Override
    public void setLowThreshold(float lowThreshold) {
        setFloat(CANNY_LOW_THRESHOLD, lowThreshold);

    }

    @Override
    public void setHighThreshold(float highThreshold) {
        setFloat(CANNY_HIGH_THRESHOLD, highThreshold);

    }

    @Override
    public void setKernelRadius(float kernelRadius) {
        setFloat(CANNY_KERNEL_RADIUS, kernelRadius);

    }

    @Override
    public void setKernelWidth(int kernelWidth) {
        setInt(CANNY_KERNEL_WIDTH, kernelWidth);
    }

    @Override
    public void setAddBorder(boolean b) {
        setBoolean(IS_ADD_BORDER, b);

    }

    @Override
    public void set(ICannyOptions options) {
        warn("Setting Canny options not yet implemented");
    }
}
