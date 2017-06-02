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

import java.io.File;

/**
 * A default implementation of the IMutableLobeDetectionOptions interface.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class DefaultLobeDetectionOptions extends DefaultNucleusDetectionOptions
        implements IMutableLobeDetectionOptions {

    private static final long serialVersionUID = 1L;

    private double lobeFraction = 0.5;

    public DefaultLobeDetectionOptions(File folder) {
        super(folder);
    }

    public DefaultLobeDetectionOptions(ILobeDetectionOptions template) {
        super(template);
        lobeFraction = template.getLobeDiameter();
    }

    @Override
    public IMutableDetectionOptions duplicate() {
        return new DefaultLobeDetectionOptions(this);
    }

    @Override
    public void setLobeDiameter(double d) {
        lobeFraction = d;

    }

    @Override
    public double getLobeDiameter() {
        return lobeFraction;
    }

}
