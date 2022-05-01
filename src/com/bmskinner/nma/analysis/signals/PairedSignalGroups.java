package com.bmskinner.nma.analysis.signals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.io.Io;

/**
 * Map dataset signal groups for merging datasets
 * 
 * @author ben
 * @since 1.15.1
 *
 */
public class PairedSignalGroups {
	private final Map<DatasetSignalId, Set<DatasetSignalId>> map = new HashMap<>();

	/**
	 * Add a signal group pair
	 * 
	 * @param d1 the id of the first dataset
	 * @param s1 the id of the first signal group
	 * @param d2 the id of the second dataset
	 * @param s2 the id of the second signal group
	 */
	public void add(@NonNull final UUID d1, @NonNull final UUID s1, @NonNull final UUID d2,
			@NonNull final UUID s2) {
		add(new DatasetSignalId(d1, s1), new DatasetSignalId(d2, s2));
	}

	/**
	 * Add a signal group pair
	 * 
	 * @param id1 the first dataset/signal group combined id
	 * @param id2 the second dataset/signal group combined id
	 */
	public void add(@NonNull final DatasetSignalId id1, @NonNull final DatasetSignalId id2) {
		if (map.containsKey(id1)) {
			map.get(id1).add(id2);
			return;
		}

		if (map.containsKey(id2)) {
			map.get(id2).add(id1);
			return;
		}

		map.put(id1, new HashSet<>());
		add(id1, id2);
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public Set<DatasetSignalId> keySet() {
		return map.keySet();
	}

	public Set<Entry<DatasetSignalId, Set<DatasetSignalId>>> entrySet() {
		return map.entrySet();
	}

	public Set<DatasetSignalId> get(DatasetSignalId key) {
		return map.get(key);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Entry<DatasetSignalId, Set<DatasetSignalId>> entry : map.entrySet()) {
			for (DatasetSignalId id : entry.getValue()) {
				sb.append(entry.getKey().signalId + " : " + id.signalId + Io.NEWLINE);
			}
		}
		return sb.toString();
	}

	/**
	 * Combine a dataset id with a signal id. Ensures signal groups from different
	 * datasets with the same id can be distinguished.
	 * 
	 * @author ben
	 * @since 1.15.0
	 *
	 */
	public record DatasetSignalId(UUID datasetId, UUID signalId) {
	}
}
