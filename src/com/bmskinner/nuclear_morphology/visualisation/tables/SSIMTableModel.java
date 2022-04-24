package com.bmskinner.nuclear_morphology.visualisation.tables;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.analysis.image.MultiScaleStructuralSimilarityIndex;
import com.bmskinner.nuclear_morphology.analysis.image.MultiScaleStructuralSimilarityIndex.MSSIMScore;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.signals.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.signals.IWarpedSignal;

public class SSIMTableModel extends DatasetTableModel {
	private static final String[] COL_NAMES = { "Source 1", "Signal 1", "Source 2", "Signal 2", "Target", "Luminance",
			"Contrast", "Structure", "MS-SSIM*" };

	/** The MS-SSIM calculator */
	private MultiScaleStructuralSimilarityIndex msi = new MultiScaleStructuralSimilarityIndex();

	private static final DecimalFormat SSIM_FORMAT = new DecimalFormat("#0.0000");

	private String[][] rowData;

	private record SignalTuple(IAnalysisDataset d, ISignalGroup g, IWarpedSignal s) {
	}

	private record SignalTuplePair(SignalTuple t1, SignalTuple t2) {
	}

	public SSIMTableModel(@Nullable List<IAnalysisDataset> datasets) {

		if (datasets == null) {
			makeEmptyTable();
			return;
		}

		int colCount = COL_NAMES.length;

		// Get all the warped signals in all given datasets
		List<SignalTuple> allSignals = new ArrayList<>();
		for (IAnalysisDataset d : datasets) {
			for (ISignalGroup s : d.getCollection().getSignalGroups()) {
				for (IWarpedSignal w : s.getWarpedSignals()) {
					if (!w.sourceDatasetId().equals(d.getId()))
						continue;
					allSignals.add(new SignalTuple(d, s, w));
				}
			}
		}

		// Decide which signals can be compared and create a list of pairs
		List<SignalTuplePair> allPairs = new ArrayList<>();

		for (SignalTuple t1 : allSignals) {
			for (SignalTuple t2 : allSignals) {
				if (t1 == t2)
					continue;
				if (t1.s.target().equals(t2.s.target())) {
					SignalTuplePair p1 = new SignalTuplePair(t2, t1);
					if (!allPairs.contains(p1))
						allPairs.add(new SignalTuplePair(t1, t2));
				}
			}
		}
		int rowCount = allPairs.size();
		rowData = new String[rowCount][COL_NAMES.length];

		for (int r = 0; r < rowCount; r++) {
			SignalTuplePair p = allPairs.get(r);
			rowData[r][0] = p.t1.d.getName();
			rowData[r][1] = p.t1.g().getGroupName();
			rowData[r][2] = p.t2.d.getName();
			rowData[r][3] = p.t2.g().getGroupName();
			rowData[r][4] = p.t1.s.targetName();

			MSSIMScore score = msi.calculateMSSIM(p.t1().s().toImage(), p.t2().s().toImage());
			rowData[r][5] = SSIM_FORMAT.format(score.luminance);
			rowData[r][6] = SSIM_FORMAT.format(score.contrast);
			rowData[r][7] = SSIM_FORMAT.format(score.structure);
			rowData[r][8] = SSIM_FORMAT.format(score.msSsimIndex);
		}

	}

	@Override
	protected void makeEmptyTable() {
		rowData = new String[1][COL_NAMES.length];
		for (int c = 0; c < COL_NAMES.length; c++) {
			rowData[0][c] = EMPTY_STRING;
		}
	}

	@Override
	public int getRowCount() {
		return rowData.length;
	}

	@Override
	public int getColumnCount() {
		return COL_NAMES.length;
	}

	@Override
	public String getColumnName(int column) {
		return COL_NAMES[column];
	}

	@Override
	public String getValueAt(int rowIndex, int columnIndex) {
		return rowData[rowIndex][columnIndex];
	}
}
