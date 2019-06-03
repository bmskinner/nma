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
package com.bmskinner.nuclear_morphology.components.options;

import java.io.Serializable;

import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions;

/**
 * The parameters for edge detection using the Canny algorithm, and the image
 * pre-processing options to apply.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface ICannyOptions extends IDetectionSubOptions, Serializable {

    // The default values below work for our mouse sperm images
    // pretty well.
    static final String CANNY_LOW_THRESHOLD     = "Canny low threshold";
    static final String CANNY_HIGH_THRESHOLD    = "Canny high threshold";
    static final String CANNY_KERNEL_RADIUS     = "Canny kernel radius";
    static final String CANNY_KERNEL_WIDTH      = "Canny kernel width";
    static final String CLOSING_RADIUS          = "Closing radius";
    static final String KUWAHARA_RADIUS         = "Kuwahara radius";
    static final String IS_USE_KUWAHARA         = "Use Kuwahara";
    static final String IS_FLATTEN_CHROMOCENTRE = "Use chromocentre flattening";
    static final String FLATTEN_THRESHOLD       = "Flattening threshold";
    static final String IS_AUTO_THRESHOLD       = "Use auto threshold";
    static final String IS_ADD_BORDER           = "Add border";
    static final String IS_USE_CANNY            = "Use Canny";

    static final float   DEFAULT_CANNY_LOW_THRESHOLD        = 0.5f;
    static final float   DEFAULT_CANNY_HIGH_THRESHOLD       = 1.5f;
    static final float   DEFAULT_CANNY_TAIL_LOW_THRESHOLD   = 0.1f;
    static final float   DEFAULT_CANNY_TAIL_HIGH_THRESHOLD  = 0.5f;
    static final float   DEFAULT_CANNY_KERNEL_RADIUS        = 3;
    static final int     DEFAULT_CANNY_KERNEL_WIDTH         = 16;
    static final int     DEFAULT_CLOSING_OBJECT_RADIUS      = 5;
    static final int     DEFAULT_TAIL_CLOSING_OBJECT_RADIUS = 3;
    static final int     DEFAULT_KUWAHARA_KERNEL_RADIUS     = 3;
    static final boolean DEFAULT_USE_KUWAHARA               = true;
    static final boolean DEFAULT_FLATTEN_CHROMOCENTRES      = true;
    static final boolean DEFAULT_USE_CANNY                  = true;
    static final boolean DEFAULT_AUTO_THRESHOLD             = false;
    static final int     DEFAULT_FLATTEN_THRESHOLD          = 100;
    static final boolean DEFAULT_ADD_BORDER                 = false;

    /**
     * Make the options mutable
     * 
     * @return
     */
//    IMutableCannyOptions unlock();

    /**
     * Create a copy of this options
     * 
     * @return
     */
    @Override
	ICannyOptions duplicate();

    /**
     * Should edge detection be run?
     * 
     * @return
     */
    boolean isUseCanny();

    /**
     * Should chromocentres be flattened?
     * 
     * @return
     */
    boolean isUseFlattenImage();

    int getFlattenThreshold();

    boolean isUseKuwahara();

    int getKuwaharaKernel();

    int getClosingObjectRadius();

    boolean isCannyAutoThreshold();

    float getLowThreshold();

    float getHighThreshold();

    float getKernelRadius();

    int getKernelWidth();

    boolean isAddBorder();
    
    /**
     * @param useCanny
     */
    void setUseCanny(boolean useCanny);

    void setFlattenImage(boolean flattenImage);

    void setFlattenThreshold(int flattenThreshold);

    void setUseKuwahara(boolean b);

    void setKuwaharaKernel(int radius);

    void setClosingObjectRadius(int closingObjectRadius);

    void setCannyAutoThreshold(boolean cannyAutoThreshold);

    void setLowThreshold(float lowThreshold);

    void setHighThreshold(float highThreshold);

    void setKernelRadius(float kernelRadius);

    void setKernelWidth(int kernelWidth);

    void setAddBorder(boolean b);

    /**
     * Set the options to match the given values
     * 
     * @param options
     */
    void set(ICannyOptions options);

}
