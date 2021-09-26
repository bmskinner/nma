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
package com.bmskinner.nuclear_morphology.components.signals;

import java.util.UUID;

/**
 * Holds the shortest distance between signals in two signal groups within a
 * nucleus.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class PairwiseSignalDistanceValue {
    private UUID group1;
    private UUID group2;

    private double value;

    public PairwiseSignalDistanceValue(UUID id1, UUID id2, double value) {
        group1 = id1;
        group2 = id2;
        this.value = value;
    }

    public UUID getGroup1() {
        return group1;
    }

    public UUID getGroup2() {
        return group2;
    }

    public double getValue() {
        return value;
    }

}
