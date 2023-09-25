package com.bmskinner.nma.analysis.nucleus;

import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.options.DefaultFilteringOptions;
import com.bmskinner.nma.components.options.FilteringOptions;
import com.bmskinner.nma.components.options.FilteringOptions.FilterMatchType;

/**
 * Simplify creation of filtering options
 * @author Ben Skinner
 * @since 1.17.2
 *
 */
public class CellCollectionFilterBuilder {
	
	private FilteringOptions options;
	
	/**
	 * Default constructor
	 */
	public CellCollectionFilterBuilder() {
		options = new DefaultFilteringOptions();
	}
	

	
	/**
	 * Build the options
	 * @return
	 */
	public FilteringOptions build() {
		return options;
	}
	
	public CellCollectionFilterBuilder setMatchType(FilterMatchType type) {
		options.setMatchState(type);
		return this;
	}
	
	/**
	 * Add a thresholded value
	 * @param stat the stat to filter on
	 * @param component the cell component
	 * @param minValue the minimum value
	 * @param maxValue the maximum value
	 * @return this builder
	 */
	public CellCollectionFilterBuilder add(@NonNull Measurement stat, @NonNull String component, double minValue, double maxValue) {
		options.addMinimumThreshold(stat, component, minValue);
		options.addMaximumThreshold(stat, component, maxValue);
		return this;
	}
	
	/**
	 * Add a thresholded value
	 * @param stat the stat to filter on
	 * @param component the cell component
	 * @param scale the measurement scale
	 * @param minValue the minimum value
	 * @param maxValue the maximum value
	 * @return this builder
	 */
	public CellCollectionFilterBuilder add(@NonNull Measurement stat, @NonNull String component, MeasurementScale scale, double minValue, double maxValue) {
		options.addMinimumThreshold(stat, component, scale, minValue);
		options.addMaximumThreshold(stat, component, scale, maxValue);
		return this;
	}
	
	/**
	 * Add a thresholded value
	 * @param stat the stat to filter on
	 * @param component the cell component
	 * @param id the component id
	 * @param minValue the minimum value
	 * @param maxValue the maximum value
	 * @return this builder
	 */
	public CellCollectionFilterBuilder add(@NonNull Measurement stat, @NonNull String component, @Nullable UUID id, double minVvalue, double maxValue) {
		options.addMinimumThreshold(stat, component, MeasurementScale.PIXELS, id, minVvalue);
		options.addMaximumThreshold(stat, component, MeasurementScale.PIXELS, id, maxValue);
		return this;
	}
	
	
}
