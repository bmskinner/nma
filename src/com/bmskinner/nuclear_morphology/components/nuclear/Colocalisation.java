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
package com.bmskinner.nuclear_morphology.components.nuclear;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.components.stats.StatisticDimension;

public class Colocalisation<E extends CellularComponent> {

    private final E comp1;
    private final E comp2;

    public Colocalisation(final E c1, final E c2) {

        if (c1 == null || c2 == null) {
            throw new IllegalArgumentException("An input component is null");
        }
        comp1 = c1;
        comp2 = c2;
    }

    public boolean contains(E c) {
        return comp1 == c || comp2 == c;
    }

    public double getDistance(MeasurementScale scale) {

        double value = comp1.getCentreOfMass().getLengthTo(comp2.getCentreOfMass());
        return PlottableStatistic.convert(value, comp1.getScale(), scale, StatisticDimension.LENGTH);
    }

}
