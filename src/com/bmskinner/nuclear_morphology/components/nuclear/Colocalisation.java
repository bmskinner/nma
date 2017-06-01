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
