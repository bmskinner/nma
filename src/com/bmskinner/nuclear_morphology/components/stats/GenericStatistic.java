package com.bmskinner.nuclear_morphology.components.stats;

import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;

/**
 * Allows for arbitrary statistics, moving away from the enums prviously
 * used
 * @author ben
 * @since 1.13.4
 *
 */
public class GenericStatistic implements PlottableStatistic {
	private final String name;
	private final StatisticDimension dim;
	
	public GenericStatistic(String s, StatisticDimension d){
		name = s;
		dim  = d;
	}
	
	@Override
	public boolean isDimensionless() {
		return dim.equals(StatisticDimension.DIMENSIONLESS);
	}

	@Override
	public StatisticDimension getDimension() {
		return dim;
	}

	@Override
	public String label(MeasurementScale scale) {
		
		StringBuilder b = new StringBuilder(name);

		 switch(dim){
			  case DIMENSIONLESS:
				  break;
			  default:
				  b.append(" (")
				  .append(units(scale))
				  .append(")");
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

	@Override
	public PlottableStatistic[] getValues() {
		return null;
	}
	
	public String toString(){
		return name;
	}
	
	

}
