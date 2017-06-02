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


package com.bmskinner.nuclear_morphology.analysis.detection;

import ij.gui.Roi;
import ij.process.ImageProcessor;

import java.util.List;

/**
 * A generic use of the detector with no filtering on size or shape by default
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
public class GenericDetector extends Detector {

    /**
     * Create with no constraints on size or circularity.
     */
    public GenericDetector() {

        this.setMinCirc(0);
        this.setMaxCirc(1);
        this.setMinSize(1);
    }

    /**
     * Detect rois in the image with no size or circularity parameters set
     * 
     * @param ip
     * @return
     */
    public List<Roi> getRois(ImageProcessor ip) {
        this.setMaxSize(ip.getWidth() * ip.getHeight());
        // this.setThreshold(128); // should be already set
        return this.detectRois(ip);
    }

}
