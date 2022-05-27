package com.bmskinner.nma.visualisation.tables;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.analysis.classification.ClusteringMethod;
import com.bmskinner.nma.analysis.classification.PrincipalComponentAnalysis;
import com.bmskinner.nma.analysis.classification.TsneMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.io.Io;

public class ClusterGroupTableModel extends DatasetTableModel {

	private static final long serialVersionUID = -1955010937568900425L;

	private static final Logger LOGGER = Logger.getLogger(ClusterGroupTableModel.class.getName());

	private static final List<String> ROW_NAMES = List.of(Labels.Clusters.CLUSTER_GROUP,
			Labels.Clusters.CLUSTER_FOUND,
			Labels.Clusters.CLUSTER_PARAMS, Labels.Clusters.CLUSTER_DIM_RED,
			Labels.Clusters.CLUSTER_DIM_PLOT,
			Labels.Clusters.CLUSTER_METHOD, Labels.Clusters.TREE);

	private IAnalysisDataset[] colNames;
	private Object[][] rowData;

	public ClusterGroupTableModel(@Nullable List<IAnalysisDataset> datasets) {

		if (datasets == null) {
			makeEmptyTable();
			return;
		}

		colNames = makeColumns(datasets);
		int colCount = colNames.length;

		rowData = new Object[ROW_NAMES.size()][colCount];
		for (int r = 0; r < ROW_NAMES.size(); r++) {
			rowData[r][0] = ROW_NAMES.get(r);
		}

		int c = 1;
		// format the numbers and make into a tablemodel
		for (IAnalysisDataset d : datasets) {
			for (IClusterGroup g : d.getClusterGroups()) {
				rowData[0][c] = g;
				rowData[1][c] = String.valueOf(g.size());
				rowData[2][c] = createClusterParameterString(g);
				rowData[3][c] = createDimensionalReductionString(g);
				rowData[4][c] = createDimensionalPlotString(g);
				rowData[5][c] = createClusterMethodString(g);
				rowData[6][c] = g.hasTree() ? Labels.Clusters.CLUSTER_SHOW_TREE : Labels.NA;
				c++;
			}
		}

	}

	private IAnalysisDataset[] makeColumns(@NonNull List<IAnalysisDataset> datasets) {
		List<IAnalysisDataset> names = new ArrayList<>();
		names.add(null);

		for (IAnalysisDataset d : datasets) {
			for (IClusterGroup g : d.getClusterGroups()) {
				names.add(d);
			}
		}
		return names.toArray(new IAnalysisDataset[0]);
	}

	@Override
	protected void makeEmptyTable() {
		colNames = new IAnalysisDataset[0];
		rowData = new String[1][colNames.length];
		for (int c = 0; c < colNames.length; c++) {
			rowData[0][c] = EMPTY_STRING;
		}
	}

	private String createDimensionalPlotString(IClusterGroup group) {
		StringBuilder builder = new StringBuilder();
		Optional<HashOptions> opn = group.getOptions();

		if (!opn.isPresent()) {
			builder.append(Labels.NA);
			return builder.toString();
		}

		HashOptions op = opn.get();
		if (op.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY)
				|| op.getBoolean(HashOptions.CLUSTER_USE_PCA_KEY)) {
			builder.append(Labels.Clusters.VIEW_PLOT);
		}

		String s = builder.toString();
		if (s.equals(EMPTY_STRING))
			return Labels.NA;
		return s;
	}

	private String createClusterParameterString(IClusterGroup group) {
		StringBuilder builder = new StringBuilder();
		Optional<HashOptions> opn = group.getOptions();

		if (!opn.isPresent()) {
			builder.append(Labels.NA);
			return builder.toString();
		}

		HashOptions op = opn.get();

		for (ProfileType t : ProfileType.displayValues())
			if (op.getBoolean(t.toString()))
				builder.append(t + Io.NEWLINE);

		for (Measurement stat : Measurement.getNucleusStats())
			if (op.getBoolean(stat.toString()))
				builder.append(stat.toString() + Io.NEWLINE);

		for (String s : op.getStringKeys()) {
			try {
				UUID id = UUID.fromString(s);
				if (op.getBoolean(id.toString()))
					builder.append("Segment_" + id.toString() + Io.NEWLINE);
			} catch (IllegalArgumentException e) {
				// not a UUID, skip
			}
		}

		String s = builder.toString();
		if (s.equals(EMPTY_STRING))
			return Labels.NA;
		return s;
	}

	private String createDimensionalReductionString(IClusterGroup group) {
		StringBuilder builder = new StringBuilder();
		Optional<HashOptions> opn = group.getOptions();

		if (!opn.isPresent()) {
			builder.append(Labels.NA);
			return builder.toString();
		}

		HashOptions op = opn.get();
		if (op.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY)) {
			builder.append(Labels.Clusters.TSNE + Io.NEWLINE);
			builder.append(
					Labels.Clusters.TSNE_PERPLEXITY + ": " + op.getDouble(TsneMethod.PERPLEXITY_KEY)
							+ Io.NEWLINE);
			builder.append(Labels.Clusters.TSNE_MAX_ITER + ": "
					+ op.getInt(TsneMethod.MAX_ITERATIONS_KEY));
		}

		if (op.getBoolean(HashOptions.CLUSTER_USE_PCA_KEY)) {
			builder.append(Labels.Clusters.PCA + Io.NEWLINE);
			builder.append(Labels.Clusters.PCA_VARIANCE + ": "
					+ op.getDouble(PrincipalComponentAnalysis.PROPORTION_VARIANCE_KEY)
					+ Io.NEWLINE);
			builder.append(
					Labels.Clusters.PCA_NUM_PCS + ": " + op.getInt(HashOptions.CLUSTER_NUM_PCS_KEY)
							+ Io.NEWLINE);
		}

		String s = builder.toString();
		if (s.equals(EMPTY_STRING))
			return Labels.NA;
		return s;
	}

	private String createClusterMethodString(IClusterGroup group) {
		StringBuilder builder = new StringBuilder();
		Optional<HashOptions> opn = group.getOptions();

		if (!opn.isPresent()) {
			builder.append(Labels.NA);
			return builder.toString();
		}

		HashOptions op = opn.get();

		ClusteringMethod method = ClusteringMethod.from(op);
		builder.append(method + Io.NEWLINE);
		if (ClusteringMethod.EM.equals(method)) {
			builder.append(
					op.getInt(HashOptions.CLUSTER_EM_ITERATIONS_KEY) + " iterations" + Io.NEWLINE);
		}

		if (ClusteringMethod.HIERARCHICAL.equals(method)) {
			builder.append("Distance: " + op.getString(HashOptions.CLUSTER_HIERARCHICAL_METHOD_KEY)
					+ Io.NEWLINE);
		}
		return builder.toString();
	}

	@Override
	public int getRowCount() {
		return rowData.length;
	}

	@Override
	public int getColumnCount() {
		return colNames.length;
	}

	@Override
	public String getColumnName(int column) {
		if (column == 0)
			return EMPTY_STRING;
		return colNames[column].getName();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return rowData[rowIndex][columnIndex];
	}

	public IAnalysisDataset getDataset(int columnIndex) {
		return colNames[columnIndex];
	}

	public IClusterGroup getClusterGroup(int columnIndex) {
		return (IClusterGroup) rowData[0][columnIndex];
	}
}
