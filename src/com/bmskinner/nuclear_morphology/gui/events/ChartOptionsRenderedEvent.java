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


package com.bmskinner.nuclear_morphology.gui.events;

import java.util.EventObject;

import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;

/**
 * This event is fired when a chart has been rendered for a given set of options
 * 
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class ChartOptionsRenderedEvent extends EventObject {

    private ChartOptions options;

    /**
     * Create with a source component and the options that have been rendered
     * 
     * @param source
     * @param options
     */
    public ChartOptionsRenderedEvent(Object source, ChartOptions options) {
        super(source);
        this.options = options;

    }

    public ChartOptions getOptions() {
        return options;
    }
}
