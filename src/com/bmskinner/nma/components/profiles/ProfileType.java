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
package com.bmskinner.nma.components.profiles;

import com.bmskinner.nma.components.measure.MeasurementDimension;

/**
 * Describes the types of profiles that can be generated during the morphology
 * analysis.
 * 
 * @author bms41
 *
 */
public enum ProfileType {
    ANGLE("Angle profile", "Angle", MeasurementDimension.ANGLE), 
//    FRANKEN("Franken profile", "Angle", MeasurementDimension.ANGLE), 
    DIAMETER("Diameter profile", "Distance across CoM", MeasurementDimension.LENGTH), 
    RADIUS("Radius profile", "Distance from CoM", MeasurementDimension.LENGTH),
    ZAHN_ROSKIES("Zahn-Roskies profile", "Angle delta", MeasurementDimension.NONE);

    private String             name;
    private String             label;
    private MeasurementDimension dimension;

    /**
     * Constructor
     * 
     * @param name the name of the profile for display
     * @param label the label to use on chart axes with this profile
     * @param dimension the statistical dimension the profile covers
     */
    ProfileType(String name, String label, MeasurementDimension dimension) {
        this.name = name;
        this.label = label;
        this.dimension = dimension;
    }

    @Override
	public String toString() {
        return this.name;
    }

    public String getLabel() {
        return this.label;
    }

    public static ProfileType fromString(String s) {
        for (ProfileType p : ProfileType.values()) {
            if (s.equals(p.name)) {
                return p;
            }
        }
        return null;
    }

    public MeasurementDimension getDimension() {
        return this.dimension;
    }

    /**
     * The profile types that should be exported to file
     * 
     * @return
     */
    public static ProfileType[] exportValues() {
    	return new ProfileType[] { ANGLE, DIAMETER, RADIUS };
    }

    /**
     * The profile types that should be displayed in the GUI
     * 
     * @return
     */
    public static ProfileType[] displayValues() {
    	return new ProfileType[] { ANGLE, DIAMETER, RADIUS };
    }
}
