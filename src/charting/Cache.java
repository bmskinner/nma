package charting;

import java.util.List;

import analysis.AnalysisDataset;

public interface Cache {

	/**
	 * Remove all cached charts
	 */
	void purge();

	/*
	 * Removes all stored entries from the cache
	 */
	void clear();

	/**
	 * Remove caches containing any of the given datasets.
	 * These will be recalculated at next call
	 * @param list
	 */
	void clear(List<AnalysisDataset> list);

}