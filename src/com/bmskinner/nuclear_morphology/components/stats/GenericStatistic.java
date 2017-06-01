package com.bmskinner.nuclear_morphology.components.stats;

import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;

/**
 * Allows for arbitrary statistics, moving away from the enums previously used.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class GenericStatistic implements PlottableStatistic {

    private static final long        serialVersionUID = 1L;
    private final String             name;
    private final StatisticDimension dim;

    public GenericStatistic(String s, StatisticDimension d) {
        name = s;
        dim = d;
    }

    @Override
    public boolean isDimensionless() {
        return StatisticDimension.DIMENSIONLESS.equals(dim);
    }

    @Override
    public boolean isAngle() {
        return StatisticDimension.ANGLE.equals(dim);
    }

    @Override
    public StatisticDimension getDimension() {
        return dim;
    }

    @Override
    public String label(MeasurementScale scale) {

        StringBuilder b = new StringBuilder(name);

        switch (dim) {
        case DIMENSIONLESS:
            break;
        default:
            b.append(" (").append(units(scale)).append(")");
            break;
        }

        return b.toString();
    }

    @Override
    public double convert(double value, double factor, MeasurementScale scale) {
        return PlottableStatistic.convert(value, factor, scale, dim);
    }

    @Override
    public String units(MeasurementScale scale) {
        return PlottableStatistic.units(scale, dim);
    }

    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dim == null) ? 0 : dim.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GenericStatistic other = (GenericStatistic) obj;
        if (dim != other.dim)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

}
