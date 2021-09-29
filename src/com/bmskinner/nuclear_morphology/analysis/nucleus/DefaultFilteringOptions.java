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
package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.AbstractHashOptions;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.signals.INuclearSignal;

/**
 * Default implementation of the filtering options
 * @author ben
 * @since 1.14.0
 *
 */
public class DefaultFilteringOptions extends AbstractHashOptions implements FilteringOptions {
	
	private static final long serialVersionUID = 1L;
	private final Map<Key, Double> minima = new HashMap<>();
	private final Map<Key, Double> maxima = new HashMap<>();
	
	private static final String ALL_MATCH_KEY = "All_match";
	
	/**
	 * Construct with default multiple matching rules:
	 * all sub-components must pass their filter for a cell
	 * to be included.
	 */
	public DefaultFilteringOptions() {
		this(FilterMatchType.ALL_MATCH);
	}
	
	/**
	 * Construct an options specifying the matching rules for 
	 * multiple objects of the same type within a cell. For example,
	 * nuclear signals: should all signals pass the filter for the cell
	 * to pass, or should any signals pass the filter for the cell to pass? 
	 * @param allMatch should all cell components pass the filter?
	 */
	public DefaultFilteringOptions(FilterMatchType matchType) {
		setMatchState(matchType);
	}
	

	@Override
	public void setMatchState(FilterMatchType type) {
		setBoolean(ALL_MATCH_KEY, type.equals(FilterMatchType.ALL_MATCH));
	}
	

	@Override
	public void addMinimumThreshold(@NonNull Measurement stat, @NonNull String component, double value) {
		addMinimumThreshold(stat, component, MeasurementScale.PIXELS, null, value);
	}
	
	@Override
	public void addMinimumThreshold(@NonNull Measurement stat, @NonNull String component, @Nullable UUID id, double value) {
		addMinimumThreshold(stat, component, MeasurementScale.PIXELS, id, value);
	}
	
	@Override
	public void addMinimumThreshold(@NonNull Measurement stat, @NonNull String component, @NonNull MeasurementScale scale, double value) {
		addMinimumThreshold(stat, component, scale, null, value);
	}
	
	@Override
	public void addMinimumThreshold(@NonNull Measurement stat, @NonNull String component,
			@NonNull MeasurementScale scale, @Nullable UUID id, double value) {
		Key k = new Key(stat, component, scale, id);
		minima.put(k, value);
	}
	
	@Override
	public void addMaximumThreshold(@NonNull Measurement stat, @NonNull String component, double value) {
		addMaximumThreshold(stat, component, MeasurementScale.PIXELS, value);
	}
	
	@Override
	public void addMaximumThreshold(@NonNull Measurement stat, @NonNull String component, @Nullable UUID id, double value) {
		addMaximumThreshold(stat, component, MeasurementScale.PIXELS, id, value);
	}

	@Override
	public void addMaximumThreshold(@NonNull Measurement stat, @NonNull String component, @NonNull MeasurementScale scale, double value) {
		addMaximumThreshold(stat, component, scale, null, value);
	}	
	
	@Override
	public void addMaximumThreshold(@NonNull Measurement stat, @NonNull String component,
			@NonNull MeasurementScale scale, @Nullable UUID id, double value) {
		Key k = new Key(stat, component, scale, id);
		maxima.put(k, value);
	}
	
	@Override
	public double getMinimaThreshold(@NonNull Measurement stat, @NonNull String component, @NonNull MeasurementScale scale) {
		return getMinimaThreshold(stat, component, scale, null);
	}
	
	@Override
	public double getMinimaThreshold(@NonNull Measurement stat, @NonNull String component,
			@NonNull MeasurementScale scale, @Nullable UUID id) {
		Key k = new Key(stat, component, scale, id);
		return minima.get(k);
	}

	@Override
	public double getMaximaThreshold(@NonNull Measurement stat, @NonNull String component, @NonNull MeasurementScale scale) {
		return getMaximaThreshold(stat, component, scale, null);
	}
	
	@Override
	public double getMaximaThreshold(@NonNull Measurement stat, @NonNull String component,
			@NonNull MeasurementScale scale, @Nullable UUID id) {
		Key k = new Key(stat, component, scale, id);
		return maxima.get(k);
	}
	
	@Override
	public Predicate<ICell> getPredicate(@NonNull ICellCollection collection) {
		return  (c) -> {
			boolean passes = true;
			for(Key k : minima.keySet()) {
				
				switch(k.component){
					case CellularComponent.NUCLEUS:        passes &= createNucleusFilter(k, c, collection, true); break;
					case CellularComponent.NUCLEAR_SIGNAL: passes &= createNuclearSignalFilter(k, c, collection, true); break;
				}
			}
			
			for(Key k : maxima.keySet()) {
				
				switch(k.component){
					case CellularComponent.NUCLEUS:        passes &= createNucleusFilter(k, c, collection, false); break;
					case CellularComponent.NUCLEAR_SIGNAL: passes &= createNuclearSignalFilter(k, c, collection, false); break;
				}
			}
			return passes;
		};
	}
	
	
	private boolean createNucleusFilter(Key k, ICell c, ICellCollection collection, boolean isMin){
//		log(String.format("Making nucleus filter for key %s", k));
		if(getBoolean(ALL_MATCH_KEY))
			return c.getNuclei().stream().allMatch(n->nucleusMatches(k, n, collection, isMin));
		return c.getNuclei().stream().anyMatch(n->nucleusMatches(k, n, collection, isMin));
	}
	
	private boolean nucleusMatches(Key k, Nucleus n, ICellCollection collection, boolean isMin){
		try {
			if(k.stat.equals(Measurement.VARIABILITY)) {
				double v = collection.getNormalisedDifferenceToMedian(Landmark.REFERENCE_POINT, n);
				return isMin ? v>=minima.get(k) : v<=maxima.get(k);
			}
				
			if(k.stat.equals(Measurement.NUCLEUS_SIGNAL_COUNT)) {
				if(k.id==null)
					return false;
				double v = n.getSignalCollection().numberOfSignals(k.id);
				return isMin ? v>=minima.get(k) : v<=maxima.get(k);
			}
			double v = n.getStatistic(k.stat, k.scale);
			return isMin ? v>=minima.get(k) : v<=maxima.get(k);
		} catch (UnavailableBorderTagException e) {
			return false;
		}
	}
	
	private boolean createNuclearSignalFilter(Key k, ICell c, ICellCollection collection, boolean isMin){
		
		if(k.id==null) { // filter all signal groups
			if(getBoolean(ALL_MATCH_KEY))
				return c.getNuclei().stream().flatMap(n->n.getSignalCollection().getAllSignals().stream()).allMatch(s->signalMatches(k, s, isMin));
			return c.getNuclei().stream().flatMap(n->n.getSignalCollection().getAllSignals().stream()).anyMatch(s->signalMatches(k, s, isMin));
		}
		// filter specific signal group
		if(getBoolean(ALL_MATCH_KEY))
			return c.getNuclei().stream().flatMap(n->n.getSignalCollection().getSignals(k.id).stream()).allMatch(s->signalMatches(k, s, isMin));
		return c.getNuclei().stream().flatMap(n->n.getSignalCollection().getSignals(k.id).stream()).anyMatch(s->signalMatches(k, s, isMin));
	}
	
	private boolean signalMatches(Key k, INuclearSignal s, boolean isMin){
		double v = s.getStatistic(k.stat, k.scale);
		return isMin ? v>=minima.get(k) : v<=maxima.get(k);
	}

	
	private class Key{
		
		private Measurement stat;
		private String component;
		private MeasurementScale scale;
		private UUID id;
		
		public Key(@NonNull Measurement stat, @NonNull String component, @NonNull MeasurementScale scale, @Nullable UUID id) {
			this.stat = stat;
			this.component = component;
			this.scale = scale;
			this.id = id;
		}
		
		@Override
		public String toString() {
			String s = stat.toString()+"_"+component+"_"+scale;
			return id==null ? s : s+"_"+id.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((component == null) ? 0 : component.hashCode());
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			result = prime * result + ((scale == null) ? 0 : scale.hashCode());
			result = prime * result + ((stat == null) ? 0 : stat.hashCode());
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
			Key other = (Key) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (component == null) {
				if (other.component != null)
					return false;
			} else if (!component.equals(other.component))
				return false;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			if (scale != other.scale)
				return false;
			if (stat == null) {
				if (other.stat != null)
					return false;
			} else if (!stat.equals(other.stat))
				return false;
			return true;
		}

		private DefaultFilteringOptions getOuterType() {
			return DefaultFilteringOptions.this;
		}

	}
}
