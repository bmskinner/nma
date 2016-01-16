package charting;

import java.util.List;

import analysis.AnalysisDataset;

public interface Cache {

	/**
	 * Remove all cached charts
	 */
	void purge();

	void refresh();

	/**
	 * Remove caches containing any of the given datasets.
	 * These will be recalculated at next call
	 * @param list
	 */
	void refresh(List<AnalysisDataset> list);

}