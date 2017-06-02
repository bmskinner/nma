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


package com.bmskinner.nuclear_morphology.components.generic;

import com.bmskinner.nuclear_morphology.components.stats.StatisticDimension;

/**
 * Describes the types of profiles that can be generated during the morphology
 * analysis.
 * 
 * @author bms41
 *
 */
public enum ProfileType {
    ANGLE("Angle profile", "Angle", StatisticDimension.ANGLE), FRANKEN("Franken profile", "Angle",
            StatisticDimension.ANGLE), DIAMETER("Diameter profile", "Distance across CoM",
                    StatisticDimension.LENGTH), RADIUS("Radius profile", "Distance from CoM",
                            StatisticDimension.LENGTH),
    // P2P ( "P2P length profile" , "Distance from previous" ,
    // StatisticDimension.LENGTH),
    ZAHN_ROSKIES("Zahn-Roskies profile", "Angle delta", StatisticDimension.DIMENSIONLESS);

    private String             name;
    private String             label;
    private StatisticDimension dimension;

    /**
     * Constructor
     * 
     * @param name
     *            the name of the profile for display
     * @param label
     *            the label to use on chart axes with this profile
     * @param dimension
     *            the statistical dimension the profile covers
     */
    ProfileType(String name, String label, StatisticDimension dimension) {
        this.name = name;
        this.label = label;
        this.dimension = dimension;
    }

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

    public StatisticDimension getDimension() {
        return this.dimension;
    }

    /**
     * The profile types that should be exported to file
     * 
     * @return
     */
    public static ProfileType[] exportValues() {
        ProfileType[] array = { ANGLE, DIAMETER, RADIUS };
        return array;
    }

    /**
     * The profile types that should be displayed in the GUI
     * 
     * @return
     */
    public static ProfileType[] displayValues() {
        ProfileType[] array = { ANGLE, DIAMETER, RADIUS, ZAHN_ROSKIES };
        return array;
    }
}
