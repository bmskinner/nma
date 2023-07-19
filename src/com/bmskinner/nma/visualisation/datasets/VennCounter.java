package com.bmskinner.nma.visualisation.datasets;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * Calculate the number of shared cells in datasets for Venn diagrams
 * 
 * @author ben
 *
 */
public class VennCounter {

	private static final int FOUR_DATASETS = 8;

	private Map<String, Integer> result = new HashMap<>();

	// Which datasets d1 - d4 correspond to venn circles
	private String a;
	private String b;
	private String c;
	private String d;

	private int nDatasets = -1;

	public enum VennCircle {
		ABCD, ABC, ABD, ACD, BCD, AB, AC, AD, BC, BD, CD, A, B, C, D
	}

	/**
	 * Create with a cluster of datasets with shared cells
	 * 
	 * @param cluster
	 */
	public VennCounter(List<IAnalysisDataset> cluster) {
		nDatasets = cluster.size();
		createCounts(cluster);
		assignDatasets();
	}

	/**
	 * Create the shared nucleus counts for a cluster
	 * 
	 * @param cluster the datasets to create counts for
	 */
	private void createCounts(List<IAnalysisDataset> cluster) {
		if (cluster.size() == 1) {
			result.put("d1", cluster.get(0).getCollection().size());
		}

		if (cluster.size() == 2) {
			int d1d2 = cluster.get(0).getCollection().countShared(cluster.get(1));
			int d1 = cluster.get(0).getCollection().size() - d1d2;
			int d2 = cluster.get(1).getCollection().size() - d1d2;

			result.put("d1d2", d1d2);
			result.put("d1", d1);
			result.put("d2", d2);
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

			result.put("d1d2d3", d1d2d3.size());
			result.put("d1d2", d1d2.size());
			result.put("d1d3", d1d3.size());
			result.put("d1", d1s.size());
			result.put("d2d3", d2d3.size());
			result.put("d2", d2s.size());
			result.put("d3", d3s.size());
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
			result.put("d1d2d3d4", d1d2d3d4.size());

			// 4 combos of 3 shared
			Set<UUID> d1d2d3 = new HashSet<>(d1);
			d1d2d3.retainAll(d2);
			d1d2d3.retainAll(d3);
			d1d2d3.removeAll(d1d2d3d4);
			result.put("d1d2d3", d1d2d3.size());

			Set<UUID> d1d2d4 = new HashSet<>(d1);
			d1d2d4.retainAll(d2);
			d1d2d4.retainAll(d4);
			d1d2d4.removeAll(d1d2d3d4);
			result.put("d1d2d4", d1d2d4.size());

			Set<UUID> d1d3d4 = new HashSet<>(d1);
			d1d3d4.retainAll(d3);
			d1d3d4.retainAll(d4);
			d1d3d4.removeAll(d1d2d3d4);
			result.put("d1d3d4", d1d3d4.size());

			Set<UUID> d2d3d4 = new HashSet<>(d2);
			d2d3d4.retainAll(d3);
			d2d3d4.retainAll(d4);
			d2d3d4.removeAll(d1d2d3d4);
			result.put("d2d3d4", d2d3d4.size());

			// 6 combos of 2 shared
			Set<UUID> d1d2 = new HashSet<>(d1);
			d1d2.retainAll(d2);
			d1d2.removeAll(d1d2d3);
			d1d2.removeAll(d1d2d4);
			d1d2.removeAll(d1d2d3d4);
			result.put("d1d2", d1d2.size());

			Set<UUID> d1d3 = new HashSet<>(d1);
			d1d3.retainAll(d3);
			d1d3.removeAll(d1d2d3);
			d1d3.removeAll(d1d3d4);
			d1d3.removeAll(d1d2d3d4);
			result.put("d1d3", d1d3.size());

			Set<UUID> d1d4 = new HashSet<>(d1);
			d1d4.retainAll(d4);
			d1d4.removeAll(d1d2d4);
			d1d4.removeAll(d1d3d4);
			d1d4.removeAll(d1d2d3d4);
			result.put("d1d4", d1d4.size());

			Set<UUID> d2d3 = new HashSet<>(d2);
			d2d3.retainAll(d3);
			d2d3.removeAll(d1d2d3);
			d2d3.removeAll(d2d3d4);
			d2d3.removeAll(d1d2d3d4);
			result.put("d2d3", d2d3.size());

			Set<UUID> d2d4 = new HashSet<>(d2);
			d2d4.retainAll(d4);
			d2d4.removeAll(d1d2d4);
			d2d4.removeAll(d2d3d4);
			d2d4.removeAll(d1d2d3d4);
			result.put("d2d4", d2d4.size());

			Set<UUID> d3d4 = new HashSet<>(d3);
			d3d4.retainAll(d4);
			d3d4.removeAll(d1d3d4);
			d3d4.removeAll(d2d3d4);
			d3d4.removeAll(d1d2d3d4);
			result.put("d3d4", d3d4.size());

			// 4 combos of single
			Set<UUID> d1s = new HashSet<>(d1);
			d1s.removeAll(d1d2);
			d1s.removeAll(d1d3);
			d1s.removeAll(d1d4);
			d1s.removeAll(d1d2d3);
			d1s.removeAll(d1d2d4);
			d1s.removeAll(d1d3d4);
			d1s.removeAll(d1d2d3d4);
			result.put("d1", d1s.size());

			Set<UUID> d2s = new HashSet<>(d2);
			d2s.removeAll(d1d2);
			d2s.removeAll(d2d3);
			d2s.removeAll(d2d4);
			d2s.removeAll(d1d2d3);
			d2s.removeAll(d1d2d4);
			d2s.removeAll(d2d3d4);
			d2s.removeAll(d1d2d3d4);
			result.put("d2", d2s.size());

			Set<UUID> d3s = new HashSet<>(d3);
			d3s.removeAll(d1d3);
			d3s.removeAll(d2d3);
			d3s.removeAll(d3d4);
			d3s.removeAll(d1d2d3);
			d3s.removeAll(d1d3d4);
			d3s.removeAll(d2d3d4);
			d3s.removeAll(d1d2d3d4);
			result.put("d3", d3s.size());

			Set<UUID> d4s = new HashSet<>(d4);
			d4s.removeAll(d1d4);
			d4s.removeAll(d2d4);
			d4s.removeAll(d3d4);
			d4s.removeAll(d1d2d4);
			d4s.removeAll(d1d3d4);
			d4s.removeAll(d2d3d4);
			d4s.removeAll(d1d2d3d4);
			result.put("d4", d4s.size());
		}

	}

	/**
	 * Determine which dataset belongs in which venn circle
	 */
	private void assignDatasets() {
		if (nDatasets == 2)
			assignTwoDatasets();

		if (nDatasets == 3)
			assignThreeDatasets();

		if (nDatasets == 4)
			assignFourDatasets();

	}

	/**
	 * Choose the mapping of datasets to venn circle positions
	 */
	private void assignTwoDatasets() {
		String type = getType();

		if ("0010".equals(type)) {
			a = "d1";
			b = "d2";
		}

		if ("0011".equals(type)) {
			a = result.get("d1") == 0 ? "d2" : "d1";
			b = result.get("d1") == 0 ? "d1" : "d2";
		}

		if ("0012".equals(type)) {
			a = "d1";
			b = "d2";
		}

	}

	private void assignThreeDatasets() {
		String type = getType();

		if ("0133".equals(type)) { // triangle
			a = "d1";
			b = "d2";
			c = "d3";
		}

		if ("0023".equals(type)) { // triple flat
			b = findDatasetsWithCount(3).get(0);

			List<String> sides = findDatasetsWithCount(2);
			a = sides.get(0);
			c = sides.get(1);
		}

		if ("0022".equals(type)) {
			b = findDatasetsWithCount(3).get(0);
			a = findDatasetsWithCount(2).get(0);
			c = findDatasetsWithCount(1).get(0);
		}

		if ("0021".equals(type)) {
			b = findDatasetsWithCount(3).get(0);
			a = findDatasetsWithCount(1).get(0);
			c = findDatasetsWithCount(1).get(1);
		}

		if ("0020".equals(type)) {
			b = findDatasetsWithCount(2).get(0);
			a = findDatasetsWithCount(1).get(0);
			c = findDatasetsWithCount(1).get(1);
		}
	}

	private void assignFourDatasets() {
		String type = getType();
	}

	/**
	 * Find the datasets present with a non-zero cell count in the results map a
	 * given number of times
	 * 
	 * @param k
	 * @return
	 */
	private List<String> findDatasetsWithCount(int k) {
		return result.entrySet().stream().filter(e -> e.getValue() > 0) // get combinations present
				.flatMap(e -> e.getKey().chars().mapToObj(i -> (char) i)) // turn into char stream so we can
				.filter(c -> c != "d".charAt(0)) // remove d's
				.collect(Collectors.groupingBy(Function.identity(), Collectors.counting())) // count occurances
				.entrySet().stream()
				.filter(e -> e.getValue() == k) // find the dataset with k entries in the map
				.map(e -> "d" + e.getKey()) // add d back
				.collect(Collectors.toList());
	}

	public Map<String, Integer> getCounts() {
		return result;
	}

	/**
	 * Get a value from the counts map, or -1 if missing
	 * 
	 * @param s
	 * @return
	 */
	private int getValue(String s) {
		if (result.containsKey(s))
			return result.get(s);
		return -1;
	}

	/**
	 * Get the value to be displayed atthe given intersection of venn circles
	 * 
	 * @param k
	 * @return
	 */
	public int getCount(@NonNull VennCircle k) {
		// identify which input datasset corresponds to the venn circle
		if (VennCircle.ABCD.equals(k))
			return getValue(arrange(a, b, c, d));

		if (VennCircle.ABC.equals(k))
			return getValue(arrange(a, b, c));

		if (VennCircle.ABD.equals(k))
			return getValue(arrange(a, b, d));

		if (VennCircle.ACD.equals(k))
			return getValue(arrange(a, c, d));

		if (VennCircle.BCD.equals(k))
			return getValue(arrange(b, c, d));

		if (VennCircle.AB.equals(k))
			return getValue(arrange(a, b));

		if (VennCircle.AC.equals(k))
			return getValue(arrange(a, c));

		if (VennCircle.AD.equals(k))
			return getValue(arrange(a, d));

		if (VennCircle.BC.equals(k))
			return getValue(arrange(b, c));

		if (VennCircle.BD.equals(k))
			return getValue(arrange(b, d));

		if (VennCircle.CD.equals(k))
			return getValue(arrange(c, d));

		if (VennCircle.A.equals(k))
			return getValue(a);

		if (VennCircle.B.equals(k))
			return getValue(b);

		if (VennCircle.C.equals(k))
			return getValue(c);

		if (VennCircle.D.equals(k))
			return getValue(d);

		return -1;

	}

	/**
	 * Given strings of datasets, order them by dataset number
	 * 
	 * @return
	 */
	private String arrange(String... names) {

		String out = Arrays.stream(names)
				.flatMap(s -> s.chars().mapToObj(i -> (char) i))
				.filter(c -> c != "d".charAt(0)).sorted()
				.map(c -> c.toString())
				.collect(Collectors.joining("d"));

		return "d" + out;
	}

	public String getType() {

		String r = "";
		for (int i = FOUR_DATASETS; i > 0; i -= 2) {
			int j = i;
			long c = result.entrySet().stream().filter(e -> e.getKey().length() == j && e.getValue() > 0)
					.collect(Collectors.counting());
			r += c;
		}
		return r;
	}

}
