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

import java.util.EventObject;

public class OptionsChangeEvent extends EventObject {

    public static final int KUWAHARA     = 0;
    public static final int EDGE         = 1;
    public static final int CHROMOCENTRE = 2;
    public static final int GAP_CLOSING  = 3;

    private static final long serialVersionUID = 1L;
    // private ImageSet set; // the image set to work with
    // private int type; // the image that is affected by the change

    /**
     * Create an event from a source, with the given message
     * 
     * @param source
     *            the source of the event
     * @param set
     *            the image set
     * @param type
     *            the image type affected
     */
    public OptionsChangeEvent(Object source) {
        // , ImageSet set, int type
        super(source);
        // this.set = set;
        // this.type = type;
    }

    // public ImageSet getImageSet() {
    // return set;
    // }
    //
    // public int getType() {
    // return type;
    // }

}
