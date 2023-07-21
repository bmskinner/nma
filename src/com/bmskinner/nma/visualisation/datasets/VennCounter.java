package com.bmskinner.nma.visualisation.datasets;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * Calculate the number of shared cells in datasets for Venn diagrams
 * 
 * @author ben
 *
 */
public class VennCounter {

	private static final Logger LOGGER = Logger.getLogger(VennCounter.class.getName());

	private static final int FOUR_DATASETS = 8;

	public static final String D1 = "d1";
	public static final String D2 = "d2";
	public static final String D3 = "d3";
	public static final String D4 = "d4";

	// uses "d1" syntax - track counts
	private static final Map<String, Integer> counts = new HashMap<>();

	// uses "d1" syntax - track which dataset has which position
	private static final Map<VennDatasetPosition, String> positions = new EnumMap<>(
			VennDatasetPosition.class);

	// uses "d1" syntax - track which position has which dataset
	private Map<String, IAnalysisDataset> datasets = new HashMap<>();

	private int nDatasets = -1;

	/**
	 * Link analysis datasets to a venn circle for positioning
	 * 
	 * @author ben
	 *
	 */
	public enum VennDatasetPosition {
		A, B, C, D
	}

	/**
	 * Links counts to intersections
	 * 
	 * @author ben
	 *
	 */
	public enum VennIntersection {
		ABCD(VennDatasetPosition.A, VennDatasetPosition.B, VennDatasetPosition.C,
				VennDatasetPosition.D),

		ABC(VennDatasetPosition.A, VennDatasetPosition.B, VennDatasetPosition.C),
		ABD(VennDatasetPosition.A, VennDatasetPosition.B, VennDatasetPosition.D),
		ACD(VennDatasetPosition.A, VennDatasetPosition.C, VennDatasetPosition.D),
		BCD(VennDatasetPosition.B, VennDatasetPosition.C, VennDatasetPosition.D),

		AB(VennDatasetPosition.A, VennDatasetPosition.B),
		AC(VennDatasetPosition.A, VennDatasetPosition.C),
		AD(VennDatasetPosition.A, VennDatasetPosition.D),
		BC(VennDatasetPosition.B, VennDatasetPosition.C),
		BD(VennDatasetPosition.B, VennDatasetPosition.D),
		CD(VennDatasetPosition.C, VennDatasetPosition.D),

		A(VennDatasetPosition.A),
		B(VennDatasetPosition.B),
		C(VennDatasetPosition.C),
		D(VennDatasetPosition.D);

		private VennDatasetPosition[] vdp;

		/**
		 * Given strings of datasets (e.g. d2d1d3), order them by dataset number (e.g.
		 * d1d2d3)
		 * 
		 * @return the numerically sorted dataset string
		 */
		private VennIntersection(VennDatasetPosition... vdp) {
			this.vdp = vdp;
		}

		public int getValue() {
			String out = "d" + Stream.of(vdp).map(p -> positions.get(p))
					.flatMap(s -> s.chars().mapToObj(i -> (char) i))
					.filter(c -> c != "d".charAt(0)).sorted()
					.map(c -> c.toString())
					.collect(Collectors.joining("d"));

			if (!counts.containsKey(out)) {
				throw new IllegalArgumentException(
						"No key '" + out + "' in count data for position " + Arrays.toString(vdp));
			}
			return counts.get(out);
		}
	}

	/**
	 * Create with a cluster of datasets with shared cells
	 * 
	 * @param cluster
	 */
	public VennCounter(List<IAnalysisDataset> cluster) {
		positions.clear();
		counts.clear();
		nDatasets = cluster.size();

		// Store the datasets with the "d" key
		for (int i = 0; i < cluster.size(); i++) {
			datasets.put("d" + (i + 1), cluster.get(i));
		}

		createCounts(cluster);
		assignDatasets();
	}

	public int size() {
		return nDatasets;
	}

	/**
	 * Create the shared nucleus counts for a cluster
	 * 
	 * @param cluster the datasets to create counts for
	 */
	private void createCounts(List<IAnalysisDataset> cluster) {
		if (cluster.size() == 1) {
			counts.put("d1", cluster.get(0).getCollection().size());
		}

		if (cluster.size() == 2) {
			int d1d2 = cluster.get(0).getCollection().countShared(cluster.get(1));
			int d1 = cluster.get(0).getCollection().size() - d1d2;
			int d2 = cluster.get(1).getCollection().size() - d1d2;

			counts.put("d1d2", d1d2);
			counts.put("d1", d1);
			counts.put("d2", d2);
		}

		if (cluster.size() == 3) {
			Set<UUID> d1 = cluster.get(0).getCollection().getCellIDs();
			Set<UUID> d2 = cluster.get(1).getCollection().getCellIDs();
			Set<UUID> d3 = cluster.get(2).getCollection().getCellIDs();

			Set<UUID> d1d2d3 = new HashSet<>(d1);
			d1d2d3.retainAll(d2);
			d1d2d3.retainAll(d3);

			Set<UUID> d1d2 = new HashSet<>(d1);
			d1d2.retainAll(d2);
			d1d2.removeAll(d1d2d3);

			Set<UUID> d1d3 = new HashSet<>(d1);
			d1d3.retainAll(d3);
			d1d3.removeAll(d1d2d3);

			Set<UUID> d1s = new HashSet<>(d1);
			d1s.removeAll(d1d2d3);
			d1s.removeAll(d1d2);
			d1s.removeAll(d1d3);

			Set<UUID> d2d3 = new HashSet<>(d2);
			d2d3.retainAll(d3);
			d2d3.removeAll(d1d2d3);

			Set<UUID> d2s = new HashSet<>(d2);
			d2s.removeAll(d1d2);
			d2s.removeAll(d2d3);
			d2s.removeAll(d1d2d3);

			Set<UUID> d3s = new HashSet<>(d3);
			d3s.removeAll(d1d3);
			d3s.removeAll(d2d3);
			d3s.removeAll(d1d2d3);

			counts.put("d1d2d3", d1d2d3.size());
			counts.put("d1d2", d1d2.size());
			counts.put("d1d3", d1d3.size());
			counts.put("d1", d1s.size());
			counts.put("d2d3", d2d3.size());
			counts.put("d2", d2s.size());
			counts.put("d3", d3s.size());
		}

		if (cluster.size() == 4) {
			Set<UUID> d1 = cluster.get(0).getCollection().getCellIDs();
			Set<UUID> d2 = cluster.get(1).getCollection().getCellIDs();
			Set<UUID> d3 = cluster.get(2).getCollection().getCellIDs();
			Set<UUID> d4 = cluster.get(3).getCollection().getCellIDs();

			// 1 combo of 4 shared
			Set<UUID> d1d2d3d4 = new HashSet<>(d1);
			d1d2d3d4.retainAll(d2);
			d1d2d3d4.retainAll(d3);
			d1d2d3d4.retainAll(d4);
			counts.put("d1d2d3d4", d1d2d3d4.size());

			// 4 combos of 3 shared
			Set<UUID> d1d2d3 = new HashSet<>(d1);
			d1d2d3.retainAll(d2);
			d1d2d3.retainAll(d3);
			d1d2d3.removeAll(d1d2d3d4);
			counts.put("d1d2d3", d1d2d3.size());

			Set<UUID> d1d2d4 = new HashSet<>(d1);
			d1d2d4.retainAll(d2);
			d1d2d4.retainAll(d4);
			d1d2d4.removeAll(d1d2d3d4);
			counts.put("d1d2d4", d1d2d4.size());

			Set<UUID> d1d3d4 = new HashSet<>(d1);
			d1d3d4.retainAll(d3);
			d1d3d4.retainAll(d4);
			d1d3d4.removeAll(d1d2d3d4);
			counts.put("d1d3d4", d1d3d4.size());

			Set<UUID> d2d3d4 = new HashSet<>(d2);
			d2d3d4.retainAll(d3);
			d2d3d4.retainAll(d4);
			d2d3d4.removeAll(d1d2d3d4);
			counts.put("d2d3d4", d2d3d4.size());

			// 6 combos of 2 shared
			Set<UUID> d1d2 = new HashSet<>(d1);
			d1d2.retainAll(d2);
			d1d2.removeAll(d1d2d3);
			d1d2.removeAll(d1d2d4);
			d1d2.removeAll(d1d2d3d4);
			counts.put("d1d2", d1d2.size());

			Set<UUID> d1d3 = new HashSet<>(d1);
			d1d3.retainAll(d3);
			d1d3.removeAll(d1d2d3);
			d1d3.removeAll(d1d3d4);
			d1d3.removeAll(d1d2d3d4);
			counts.put("d1d3", d1d3.size());

			Set<UUID> d1d4 = new HashSet<>(d1);
			d1d4.retainAll(d4);
			d1d4.removeAll(d1d2d4);
			d1d4.removeAll(d1d3d4);
			d1d4.removeAll(d1d2d3d4);
			counts.put("d1d4", d1d4.size());

			Set<UUID> d2d3 = new HashSet<>(d2);
			d2d3.retainAll(d3);
			d2d3.removeAll(d1d2d3);
			d2d3.removeAll(d2d3d4);
			d2d3.removeAll(d1d2d3d4);
			counts.put("d2d3", d2d3.size());

			Set<UUID> d2d4 = new HashSet<>(d2);
			d2d4.retainAll(d4);
			d2d4.removeAll(d1d2d4);
			d2d4.removeAll(d2d3d4);
			d2d4.removeAll(d1d2d3d4);
			counts.put("d2d4", d2d4.size());

			Set<UUID> d3d4 = new HashSet<>(d3);
			d3d4.retainAll(d4);
			d3d4.removeAll(d1d3d4);
			d3d4.removeAll(d2d3d4);
			d3d4.removeAll(d1d2d3d4);
			counts.put("d3d4", d3d4.size());

			// 4 combos of single
			Set<UUID> d1s = new HashSet<>(d1);
			d1s.removeAll(d1d2);
			d1s.removeAll(d1d3);
			d1s.removeAll(d1d4);
			d1s.removeAll(d1d2d3);
			d1s.removeAll(d1d2d4);
			d1s.removeAll(d1d3d4);
			d1s.removeAll(d1d2d3d4);
			counts.put("d1", d1s.size());

			Set<UUID> d2s = new HashSet<>(d2);
			d2s.removeAll(d1d2);
			d2s.removeAll(d2d3);
			d2s.removeAll(d2d4);
			d2s.removeAll(d1d2d3);
			d2s.removeAll(d1d2d4);
			d2s.removeAll(d2d3d4);
			d2s.removeAll(d1d2d3d4);
			counts.put("d2", d2s.size());

			Set<UUID> d3s = new HashSet<>(d3);
			d3s.removeAll(d1d3);
			d3s.removeAll(d2d3);
			d3s.removeAll(d3d4);
			d3s.removeAll(d1d2d3);
			d3s.removeAll(d1d3d4);
			d3s.removeAll(d2d3d4);
			d3s.removeAll(d1d2d3d4);
			counts.put("d3", d3s.size());

			Set<UUID> d4s = new HashSet<>(d4);
			d4s.removeAll(d1d4);
			d4s.removeAll(d2d4);
			d4s.removeAll(d3d4);
			d4s.removeAll(d1d2d4);
			d4s.removeAll(d1d3d4);
			d4s.removeAll(d2d3d4);
			d4s.removeAll(d1d2d3d4);
			counts.put("d4", d4s.size());
		}

	}

	/**
	 * Determine which dataset belongs in which venn circle
	 */
	private void assignDatasets() {

		LOGGER.fine(getType());
		LOGGER.fine(getCountInfo());

		if (nDatasets == 1)
			positions.put(VennDatasetPosition.A, D1);

		if (nDatasets == 2)
			assignTwoDatasets();

		if (nDatasets == 3)
			assignThreeDatasets();

		if (nDatasets == 4)
			assignFourDatasets();

	}

	/**
	 * Choose the mapping of datasets to venn circles
	 */
	private void assignTwoDatasets() {
		String type = getType();

		if ("0010".equals(type)) {
			positions.put(VennDatasetPosition.A, D1);
			positions.put(VennDatasetPosition.B, D2);
		}

		if ("0011".equals(type)) {
			String a = counts.get(D1) == 0 ? D2 : D1;
			String b = counts.get(D1) == 0 ? D1 : D2;
			positions.put(VennDatasetPosition.A, a);
			positions.put(VennDatasetPosition.B, b);
		}

		if ("0012".equals(type)) {
			positions.put(VennDatasetPosition.A, D1);
			positions.put(VennDatasetPosition.B, D2);
		}
	}

	private void assignThreeDatasets() {
		String type = getType();

		// Default to triangle if the type is not found
		String a = D1;
		String b = D2;
		String c = D3;

		if ("0023".equals(type)) { // triple flat
			b = findDatasetsWithCount(3).get(0);
			List<String> sides = findDatasetsWithCount(2);
			a = sides.get(0);
			c = sides.get(1);
		}

		if ("0022".equals(type)) { // triple flat
			List<String> all = findDatasetsWithCount(2);
			b = all.stream().filter(s -> counts.get(s) == 0).findFirst().orElse(D1);
			all.remove(b);
			a = all.get(0);
			c = all.get(1);
		}

		if ("0021".equals(type)) { // two unshared within third
			b = findDatasetsWithCount(3).get(0);
			a = findDatasetsWithCount(1).get(0);
			c = findDatasetsWithCount(1).get(1);
		}

		if ("0020".equals(type)) { // two unshared within third, none only in third
			b = findDatasetsWithCount(2).get(0);
			a = findDatasetsWithCount(1).get(0);
			c = findDatasetsWithCount(1).get(1);
		}

		positions.put(VennDatasetPosition.A, a);
		positions.put(VennDatasetPosition.B, b);
		positions.put(VennDatasetPosition.C, c);
	}

	private void assignFourDatasets() {
		String type = getType();

		String a = D1;
		String b = D2;
		String c = D3;
		String d = D4;

		if ("0030".equals(type)) { // three unshared within fourth, none only in fourth
			b = findDatasetsWithCount(3).get(0);
			a = findDatasetsWithCount(1).get(0);
			c = findDatasetsWithCount(1).get(1);
			d = findDatasetsWithCount(1).get(2);
		}

		if ("0031".equals(type)) { // three unshared within fourth
			b = findDatasetsWithCount(3).get(0);
			a = findDatasetsWithCount(2).get(0);
			c = findDatasetsWithCount(2).get(1);
			d = findDatasetsWithCount(2).get(2);
		}

		if ("0033".equals(type)) { // triple flat, with subset in middle

		}

		if ("0131".equals(type)) { // all within fourth, one and two shared, third unshared

		}

		if ("1331".equals(type)) { // all within fourth, other three are triangle

		}

		if ("0231".equals(type)) { // all within fourth, other three are triple flat
			a = findDatasetsWithCount(6).get(0);
			b = findDatasetsWithCount(3).get(0);
			c = findDatasetsWithCount(2).get(0);
			d = findDatasetsWithCount(2).get(1);
		}

		positions.put(VennDatasetPosition.A, a);
		positions.put(VennDatasetPosition.B, b);
		positions.put(VennDatasetPosition.C, c);
		positions.put(VennDatasetPosition.D, d);
	}

	/**
	 * Find the datasets present with a non-zero cell count in the results map a
	 * given number of times
	 * 
	 * @param k
	 * @return
	 */
	private List<String> findDatasetsWithCount(int k) {
		return counts.entrySet().stream().filter(e -> e.getValue() > 0) // get combinations present
				.flatMap(e -> e.getKey().chars().mapToObj(i -> (char) i)) // turn into char stream
																			// so we can
				.filter(c -> c != "d".charAt(0)) // remove d's
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting())) // count
																							// occurances
				.entrySet().stream()
				.filter(e -> e.getValue() == k) // find the dataset with k entries in the map
				.map(e -> "d" + e.getKey()) // add d back
				.collect(Collectors.toList());
	}

	private String getCountInfo() {
		return counts.entrySet().stream().filter(e -> e.getValue() > 0) // get combinations present
				.flatMap(e -> e.getKey().chars().mapToObj(i -> (char) i)) // turn into char stream
																			// so we can
				.filter(c -> c != "d".charAt(0)) // remove d's
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting())) // count
																							// occurances
				.entrySet().stream()
				.map(e -> "d" + e.getKey() + ": " + e.getValue()) // add d back
				.collect(Collectors.joining(", "));
	}

	public Map<String, Integer> getCounts() {
		return counts;
	}

	/**
	 * Get the value at the given intersection of venn circles
	 * 
	 * @param k
	 * @return
	 */
	public int getCount(@NonNull VennIntersection k) {
		return k.getValue();
	}

	public IAnalysisDataset getDataset(@NonNull VennDatasetPosition k) {
		String d = positions.get(k);
		return datasets.get(d);
	}

	/**
	 * Find the type of the cluster (a string definition of the number of
	 * intersecting circles). Digit 0 = number of 4 dataset comparisons Digit 1 =
	 * number of 3 dataset comparisons Digit 2 = number of 2 dataset comparisons
	 * Digit 3 = number of 1 dataset comparisons
	 * 
	 * @return
	 */
	public String getType() {

		StringBuilder r = new StringBuilder();
		for (int i = FOUR_DATASETS; i > 0; i -= 2) {
			int j = i;
			long l = counts.entrySet().stream()
					.filter(e -> e.getKey().length() == j && e.getValue() > 0)
					.collect(Collectors.counting());
			r.append(l);
		}
		return r.toString();
	}

}
