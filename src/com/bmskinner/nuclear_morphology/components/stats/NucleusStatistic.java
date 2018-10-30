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
package com.bmskinner.nuclear_morphology.components.stats;

import java.util.HashSet;
import java.util.Set;

import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * The measures that can be calculated for nuclei
 * 
 * @author ben
 *
 */
@Deprecated
public enum NucleusStatistic implements PlottableStatistic, Loggable {
    AREA("Area", StatisticDimension.AREA, new NucleusType[] { NucleusType.ROUND }), 
    PERIMETER("Perimeter",
            StatisticDimension.LENGTH, new NucleusType[] { NucleusType.ROUND }), 
    MAX_FERET("Max feret",
            StatisticDimension.LENGTH, new NucleusType[] { NucleusType.ROUND }), 
    MIN_DIAMETER("Min diameter",
            StatisticDimension.LENGTH, new NucleusType[] { NucleusType.ROUND }), 
    ASPECT("Aspect",
            StatisticDimension.DIMENSIONLESS,
            new NucleusType[] { NucleusType.ROUND }), 
    CIRCULARITY("Circularity",
            StatisticDimension.DIMENSIONLESS,
            new NucleusType[] { NucleusType.ROUND }), 
    VARIABILITY("Variability",
            StatisticDimension.DIMENSIONLESS,
            new NucleusType[] { NucleusType.ROUND }), 
    BOUNDING_HEIGHT(
            "Bounding height", StatisticDimension.LENGTH,
            new NucleusType[] { NucleusType.ROUND }), 
    BOUNDING_WIDTH(
            "Bounding width", StatisticDimension.LENGTH,
            new NucleusType[] {
                    NucleusType.ROUND }), 
    OP_RP_ANGLE(
            "Angle between reference points",
            StatisticDimension.ANGLE,
            new NucleusType[] {
                    NucleusType.ROUND }), 
    HOOK_LENGTH(
            "Length of hook",
            StatisticDimension.LENGTH,
            new NucleusType[] {
                    NucleusType.RODENT_SPERM }), 
    BODY_WIDTH(
            "Width of body",
            StatisticDimension.LENGTH,
            new NucleusType[] {
                    NucleusType.RODENT_SPERM });

    private String             name;
    private StatisticDimension dimension;
    private NucleusType[]      applicableSuperType;

    NucleusStatistic(String name, StatisticDimension dimension, NucleusType[] type) {
        this.name = name;
        this.dimension = dimension;
        this.applicableSuperType = type;
    }

    public String toString() {
        return this.name;
    }

    /**
     * Get the types of nucleus for which the statistic applies
     * 
     * @return an array of types
     */
    public NucleusType[] getApplicableTypes() {
        return this.applicableSuperType;
    }

    public boolean isDimensionless() {
        return dimension.equals(StatisticDimension.DIMENSIONLESS);
    }

    @Override
    public boolean isAngle() {
        return StatisticDimension.ANGLE.equals(dimension);
    }

    /**
     * Get the dimension of the statistic (area, length, none)
     * 
     * @return
     */
    public StatisticDimension getDimension() {
        return this.dimension;
    }

    /**
     * Get the label (name and units) for the statistic
     * 
     * @return
     */
    public String label(MeasurementScale scale) {
        String result = "";

        switch (this.dimension) {
        case DIMENSIONLESS:
            result = this.toString();
            break;
        default:
            result = this.toString() + " (" + this.units(scale) + ")";
            break;
        }

        return result;
    }

    /**
     * Convert the input value (assumed to be pixels) using the given factor (
     * Nucleus.getScale() ) into the appropriate scale
     * 
     * @param value
     *            the pixel measure
     * @param factor
     *            the conversion factor pixels per micron
     * @param scale
     *            the desired scale
     * @return
     */
    public double convert(double value, double factor, MeasurementScale scale) {

        return PlottableStatistic.convert(value, factor, scale, dimension);
    }

    public String units(MeasurementScale scale) {
        return PlottableStatistic.units(scale, dimension);
    }

    public PlottableStatistic[] getValues() {
        return NucleusStatistic.values();
    }

    /**
     * Get all the statistics that apply to the given nucleus type
     * 
     * @param type
     * @return
     */
    public NucleusStatistic[] values(NucleusType type) {

        Set<NucleusStatistic> result = new HashSet<NucleusStatistic>();
        for (NucleusStatistic stat : NucleusStatistic.values()) {

            for (NucleusType t : stat.getApplicableTypes()) {
                if (t.equals(NucleusType.ROUND)) {
                    result.add(stat);
                }

                if (t.equals(type)) {
                    result.add(stat);
                }
            }

        }

        return result.toArray(new NucleusStatistic[0]);
    }
}
