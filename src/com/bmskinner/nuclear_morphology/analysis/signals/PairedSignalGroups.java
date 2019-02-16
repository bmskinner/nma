package com.bmskinner.nuclear_morphology.analysis.signals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Map dataset signal groups for merging datasets
 * @author ben
 * @since 1.15.1
 *
 */
public class PairedSignalGroups {
	private final Map<DatasetSignalId, Set<DatasetSignalId>> map = new HashMap<>();
	
	public void add(@NonNull final DatasetSignalId id1, @NonNull final DatasetSignalId id2) {
		if(map.containsKey(id1)) {
			map.get(id1).add(id2);
			return;
		}
		
		if(map.containsKey(id2)) {
			map.get(id2).add(id1);
			return;
		}
		
		map.put(id1, new HashSet<>());
		add(id1, id2);
	}
	
	public boolean isEmpty() {
		return map.isEmpty();
	}
	
	public Set<DatasetSignalId> keySet(){
		return map.keySet();
	}
	
	public Set<DatasetSignalId> get(DatasetSignalId key) {
		return map.get(key);
	}
	
	/**
	 * Combine a dataset id with a signal id. Ensures signal groups
	 * from different datasets with the same id can be distinguished.
	 * @author ben
	 * @since 1.15.0
	 *
	 */
	public class DatasetSignalId {
		public final UUID d, s; // dataset and signal ids
		
		public DatasetSignalId(UUID datasetId, UUID signalId) {
			d = datasetId;
			s = signalId;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((d == null) ? 0 : d.hashCode());
			result = prime * result + ((s == null) ? 0 : s.hashCode());
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
			DatasetSignalId other = (DatasetSignalId) obj;
			if (d == null) {
				if (other.d != null)
					return false;
			} else if (!d.equals(other.d))
				return false;
			if (s == null) {
				if (other.s != null)
					return false;
			} else if (!s.equals(other.s))
				return false;
			return true;
		}

	}
}
