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
package com.bmskinner.nma.components.measure;

/**
 * Describes the scales at which data may be presented. The pixel scale is what
 * is captured by the camera. If a conversion factor is provided, this can be
 * shown in real units.
 * 
 * @author bms41
 *
 */
public enum MeasurementScale {

    PIXELS("Pixels"), MICRONS("Microns");

    final private String name;

    MeasurementScale(final String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }
}
