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
package com.bmskinner.nma.components.measure;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.Statistical;
import com.bmskinner.nma.stats.Stats;

/**
 * Store summary measurements for the cell collection. Saves calculating median,
 * min and max each time.
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
public class MeasurementCache {

	// Need to be able to store the same measurement for different components of the
	// cell. This requires keys on component and measurement.
	private record Key(@NonNull Measurement stat,
			@NonNull String component,
			@NonNull MeasurementScale scale,
			UUID id) {

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof Key))
				return false;
			Key key = (Key) o;

			if (!stat.equals(key.stat))
				return false;

			if (!component.equals(key.component))
				return false;

			if (!scale.equals(key.scale))
				return false;

			if (id != null && !id.equals(key.id))
				return false;

			if (id == null && key.id != null)
				return false;

			return true;
		}

		@Override
		public int hashCode() {

			final int prime = 31;
			int result = 1;
			result = prime * result + stat.hashCode();

			result = prime * result + component.hashCode();

			result = prime * result + scale.hashCode();

			if (id != null) {
				result = prime * result + id.hashCode();
			}

			return result;
		}

	}

	private Map<Key, Double> median = new HashMap<>(); // median values
	private Map<Key, Double> min = new HashMap<>(); // median values
	private Map<Key, Double> max = new HashMap<>(); // median values

	public MeasurementCache() {
		// no constructor
	}

	/**
	 * Add values to the cache. Min, max and median will be automatically calculated
	 * 
	 * @param measurement the measurement being stored
	 * @param component   the cellular component the measurement applies to
	 * @param scale       the measurement scale
	 * @param id          the component id (e.g. a signal group)
	 * @param values      the values to be stored
	 */
	public void set(Measurement measurement, String component, MeasurementScale scale, UUID id,
			double[] values) {
		Key key = new Key(measurement, component, scale, id);
		min.put(key, Arrays.stream(values).min().orElse(Statistical.ERROR_CALCULATING_STAT));
		max.put(key, Arrays.stream(values).max().orElse(Statistical.ERROR_CALCULATING_STAT));
		median.put(key, Stats.quartile(values, Stats.MEDIAN));
	}

	public double getMedian(Measurement stat, String component, MeasurementScale scale, UUID id) {

		if (this.has(stat, component, scale, id)) {
			Key key = new Key(stat, component, scale, id);
			return median.get(key);
		}
		return Statistical.VALUE_NOT_PRESENT;
	}

	public double getMin(Measurement stat, String component, MeasurementScale scale, UUID id) {

		if (this.has(stat, component, scale, id)) {
			Key key = new Key(stat, component, scale, id);
			return min.get(key);
		}
		return Statistical.VALUE_NOT_PRESENT;
	}

	public double getMax(Measurement stat, String component, MeasurementScale scale, UUID id) {

		if (this.has(stat, component, scale, id)) {
			Key key = new Key(stat, component, scale, id);
			return max.get(key);
		}
		return Statistical.VALUE_NOT_PRESENT;
	}

	/**
	 * Clear all stored measurements
	 */
	public void clear() {
		min.clear();
		max.clear();
		median.clear();
	}

	/**
	 * Clear all values for the given measurement
	 * 
	 * @param measurement the measurement being stored
	 * @param component   the cellular component the measurement applies to
	 * @param scale       the measurement scale
	 * @param id          the component id (e.g. a signal group)
	 */
	public void clear(Measurement stat, String component, MeasurementScale scale, UUID id) {
		Key key = new Key(stat, component, scale, id);
		min.remove(key);
		max.remove(key);
		median.remove(key);
	}

	/**
	 * Clear all values for the given measurement
	 * 
	 * @param stat
	 * @param component
	 * @param scale
	 */
	public void clear(Measurement stat, String component, UUID id) {

		for (MeasurementScale s : MeasurementScale.values()) {
			Key key = new Key(stat, component, s, id);
			min.remove(key);
			max.remove(key);
			median.remove(key);
		}
	}

	/**
	 * Clear all values for the given scale
	 * 
	 * @param scale
	 * @param scale
	 */
	public void clear(MeasurementScale scale) {

		Iterator<Key> it = median.keySet().iterator();
		while (it.hasNext()) {
			Key key = it.next();

			if (key.scale.equals(scale)) {
				it.remove();
				min.remove(key);
				max.remove(key);
			}
		}
	}

	/**
	 * Test if the given key is present in the cache
	 * 
	 * @param stat
	 * @param component
	 * @param scale
	 * @param id
	 * @return
	 */
	public boolean has(Measurement stat, String component, MeasurementScale scale, UUID id) {

		Key key = new Key(stat, component, scale, id);
		return median.containsKey(key);

	}
}
