package com.bmskinner.nma.visualisation.tables;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.stats.Stats.WilcoxonRankSumResult;

/**
 * Tuple to store results of Wilcoxon rank sum tests between datasets
 * 
 * @author Ben Skinner
 *
 */
public final record WilcoxDatasetResult(long id, WilcoxonRankSumResult r) {

	public static final long toId(IAnalysisDataset d1, IAnalysisDataset d2) {
		return d1.getId().getMostSignificantBits() * d2.getId().getMostSignificantBits();
	}
}
