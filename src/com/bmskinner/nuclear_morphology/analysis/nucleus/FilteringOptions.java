package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.util.UUID;
import java.util.function.Predicate;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

/**
 * Specify options for filtering a collection.
 * @author ben
 * @since 1.14.0
 *
 */
public interface FilteringOptions {
	
	/** Should a cell be passed only if all statistics of a type pass filters,
	 * or if any statistics of a type pass filters? For example, if a nuclear signal
	 * filter is set, must all signals meet the criteria, or do we want the cell even if
	 * it has signals that don't match? */
	final static String ALL_MATCH_KEY = "All_match";
	
	/**
	 * Add a minimum value for the given statistic. The measurement scale is assumed to be pixels.
	 * @param stat the statistic
	 * @param component the component of the cell the statistic applies to
	 * @param value the minimum value
	 */
	void addMinimumThreshold(@NonNull final PlottableStatistic stat, @NonNull final String component, double value);
	
	/**
	 * Add a minimum value for the given statistic. The measurement scale is assumed to be pixels.
	 * @param stat the statistic
	 * @param component the component of the cell the statistic applies to
	 * @param id an id; the meaning of the id is inferred from the stat and component. Eg. nuclear signal groups, segments.
	 * @param value the minimum value
	 */
	void addMinimumThreshold(@NonNull final PlottableStatistic stat, @NonNull final String component, @Nullable UUID id, double value);
	
	/**
	 * Add a minimum value for the given statistic 
	 * @param stat the statistic
	 * @param component the component of the cell the statistic applies to
	 * @param scale the measurement scale
	 * @param value the minimum value
	 */
	void addMinimumThreshold(@NonNull final PlottableStatistic stat, @NonNull final String component, @NonNull final MeasurementScale scale, double value);
	
	/**
	 * Add a minimum value for the given statistic 
	 * @param stat the statistic
	 * @param component the component of the cell the statistic applies to
	 * @param scale the measurement scale
	 * @param id an id; the meaning of the id is inferred from the stat and component. Eg. nuclear signal groups, segments.
	 * @param value the minimum value
	 */
	void addMinimumThreshold(@NonNull final PlottableStatistic stat, @NonNull final String component, @NonNull final MeasurementScale scale, @Nullable UUID id, double value);
	
	
	/**
	 * Add a maximum value for the given statistic. The measurement scale is assumed to be pixels. 
	 * @param stat the statistic
	 * @param component the component of the cell the statistic applies to
	 * @param value the maximum value
	 */
	void addMaximumThreshold(@NonNull final PlottableStatistic stat, @NonNull final String component,  double value);
	
	/**
	 * Add a maximum value for the given statistic. The measurement scale is assumed to be pixels.
	 * @param stat the statistic
	 * @param component the component of the cell the statistic applies to
	 * @param id an id; the meaning of the id is inferred from the stat and component. Eg. nuclear signal groups, segments.
	 * @param value the maximum value
	 */
	void addMaximumThreshold(@NonNull final PlottableStatistic stat, @NonNull final String component, @Nullable UUID id, double value);
	
	/**
	 * Add a maximum value for the given statistic 
	 * @param stat the statistic
	 * @param component the component of the cell the statistic applies to
	 * @param scale the measurement scale
	 * @param value the maximum value
	 */
	void addMaximumThreshold(@NonNull final PlottableStatistic stat, @NonNull final String component, @NonNull final MeasurementScale scale, double value);
	
	/**
	 * Add a maximum value for the given statistic 
	 * @param stat the statistic
	 * @param component the component of the cell the statistic applies to
	 * @param scale the measurement scale
	 * @param id an id; the meaning of the id is inferred from the stat and component. Eg. nuclear signal groups, segments.
	 * @param value the maximum value
	 */
	void addMaximumThreshold(@NonNull final PlottableStatistic stat, @NonNull final String component, @NonNull final MeasurementScale scale, @Nullable UUID id, double value);
	
	/**
	 * Get the saved minimum value for the statistic
	 * @param stat the statistic
	 * @param component the component of the cell the statistic applies to
	 * @param scale the measurement scale
	 * @return
	 */
	double getMinimaThreshold(@NonNull final PlottableStatistic stat, @NonNull final String component, @NonNull final MeasurementScale scale);
	
	/**
	 * Get the saved minimum value for the statistic
	 * @param stat the statistic
	 * @param component the component of the cell the statistic applies to
	 * @param scale the measurement scale
	 * @param id the id
	 * @return
	 */
	double getMinimaThreshold(@NonNull final PlottableStatistic stat, @NonNull final String component, @NonNull final MeasurementScale scale, @Nullable UUID id);
	
	/**
	 * Get the saved maximum value for the statistic
	 * @param stat the statistic
	 * @param component the component of the cell the statistic applies to
	 * @param scale the measurement scale
	 * @return
	 */
	double getMaximaThreshold(@NonNull final PlottableStatistic stat, @NonNull final String component, @NonNull final MeasurementScale scale);
	
	/**
	 * Get the saved maximum value for the statistic
	 * @param stat the statistic
	 * @param component the component of the cell the statistic applies to
	 * @param scale the measurement scale
	 * @param id the id
	 * @return
	 */
	double getMaximaThreshold(@NonNull final PlottableStatistic stat, @NonNull final String component, @NonNull final MeasurementScale scale, @Nullable UUID id);
		
	/**
	 * Get the cell predicate based on this options for the given collection.
	 * Note that since some statistic values are calculated in reference to a collection
	 * (e.g. profile variability), a collection must be provided, and the predicate can't
	 * be pre-computed. 
	 * @param collection the cell collection to generate a predicate for
	 * @return the predicate for filtering the given collection
	 */
	Predicate<ICell> getPredicate(@NonNull final ICellCollection collection);
}
