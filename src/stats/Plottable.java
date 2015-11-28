package stats;

import components.generic.MeasurementScale;

/**
 * This interface is implemented by the enums describing statistical measures
 * that can be plotted in charts.
 * @author ben
 *
 */
public interface Plottable {

	/**
	 * Get the string representation (name) of the statistic. 
	 * @return
	 */
	public String toString();

	public boolean isDimensionless();

	/**
	 * Get the dimension of the statistic (area, length, none)
	 * @return
	 */
	public StatisticDimension getDimension();

	/**
	 * Get the label (name and units) for the stat
	 * @return
	 */
	public String label(MeasurementScale scale);


	/**
	 * Convert the input value (assumed to be pixels) using the given
	 * factor ( Nucleus.getScale() ) into the appropriate scale
	 * @param value the pixel measure
	 * @param factor the conversion factor to microns
	 * @param scale the desired scale
	 * @return
	 */
	public double convert(double value, double factor, MeasurementScale scale);

	/**
	 * Get the appropriate units label for the statistic, based on its dimension.
	 * Eg. square units, units or nothing
	 * @param scale
	 * @return
	 */
	public String units(MeasurementScale scale);
	
	/**
	 * Calls the values() method of the underlying enum, allowing
	 * iteration access via the interface
	 * @return
	 */
	public Plottable[] getValues();
}

