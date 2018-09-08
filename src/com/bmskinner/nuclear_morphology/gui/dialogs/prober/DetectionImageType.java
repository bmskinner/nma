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


package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

/**
 * Hold stages of the detection pipeline to display
 * 
 * @since 1.13.4
 */
public enum DetectionImageType implements ImageType {
    ORIGINAL("Input image"),
    KUWAHARA("Kuwahara filtering"), 
    FLATTENED("Chromocentre flattening"), 
    EDGE_DETECTION( "Edge detection"), 
    MORPHOLOGY_CLOSED("Gap closing"), 
    FISH_IMAGE("FISH image"), 
    DETECTED_OBJECTS( "Detected objects"), 
    ANNOTAED_OBJECTS("Annotated objects"), 
    CYTOPLASM("Cytoplasm"), NUCLEUS( "Nucleus"), 
    CYTO_FLATTENED("Cytoplasm flattened"), 
    NUCLEUS_FLATTENED("Nucleus flattened");

    private String name;

    DetectionImageType(String name) {
        this.name = name;
    }

    @Override
	public String toString() {
        return this.name;
    }

    @Override
	public ImageType[] getValues() {
        return DetectionImageType.values();
    }

    @Override
    public int getPosition() {
        // TODO Auto-generated method stub
        return 0;
    }
}
