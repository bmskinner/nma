package com.bmskinner.nma.analysis.signals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.signals.ISignalGroup;

/**
 * Map dataset signal groups for merging datasets
 * 
 * @author Ben Skinner
 * @since 1.15.1
 *
 */
public class PairedSignalGroups {

	/** The new signal group ID with the signals that belong to it */
	private final Map<UUID, Set<DatasetSignalId>> map = new HashMap<>();

	/**
	 * Add a signal group pair
	 * 
	 * @param d1 the id of the first dataset
	 * @param s1 the id of the first signal group
	 * @param d2 the id of the second dataset
	 * @param s2 the id of the second signal group
	 */
	public void add(@NonNull final IAnalysisDataset d1, @NonNull final ISignalGroup s1,
			@NonNull final IAnalysisDataset d2,
			@NonNull final ISignalGroup s2) {
		DatasetSignalId i1 = new DatasetSignalId(d1, s1);
		DatasetSignalId i2 = new DatasetSignalId(d2, s2);

		// If one is in the set, add the other to the same set
		for (Entry<UUID, Set<DatasetSignalId>> entry : map.entrySet()) {
			if (entry.getValue().contains(i1) || entry.getValue().contains(i2)) {
				entry.getValue().add(i1);
				entry.getValue().add(i2);
				return;
			}
		}

		// Neither is in the set, make a new signal group
		UUID id = add(UUID.randomUUID(), d1, s1);
		add(id, d2, s2);
	}

	private UUID add(@NonNull UUID newSignalGroup, @NonNull final IAnalysisDataset dataset,
			@NonNull final ISignalGroup signal) {
		map.computeIfAbsent(newSignalGroup, k -> new HashSet<>());
		map.get(newSignalGroup).add(new DatasetSignalId(dataset, signal));
		return newSignalGroup;
	}

	public List<DatasetSignalId> get(@NonNull UUID newSignalGroup) {
		return map.get(newSignalGroup).stream().toList();
	}

	public Set<UUID> getMergedSignalGroups() {
		return map.keySet();
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	/**
	 * Combine a dataset id with a signal id. Ensures signal groups from different
	 * datasets with the same id can be distinguished.
	 * 
	 * @author Ben Skinner
	 * @since 1.15.0
	 *
	 */
	public record DatasetSignalId(IAnalysisDataset datasetId, ISignalGroup signalId) {
		@Override
		public String toString() {
			return datasetId.getName() + " - " + signalId.getGroupName();
		}
	}
}
