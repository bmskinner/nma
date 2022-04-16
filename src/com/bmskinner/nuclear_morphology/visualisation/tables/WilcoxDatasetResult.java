package com.bmskinner.nuclear_morphology.visualisation.tables;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.stats.Stats.WilcoxonRankSumResult;

/**
 * Tuple to store results of Wilcoxon rank sum tests between datasets
 * 
 * @author ben
 *
 */
public final record WilcoxDatasetResult(long id, WilcoxonRankSumResult r) {

	public static final long toId(IAnalysisDataset d1, IAnalysisDataset d2) {
		return d1.getId().getMostSignificantBits() * d2.getId().getMostSignificantBits();
	}
}
