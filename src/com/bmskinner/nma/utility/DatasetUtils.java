package com.bmskinner.nma.utility;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.DatasetListManager;

/**
 * Utility class for common dataset methods
 * 
 * @author bs19022
 * @since 2.2.0
 *
 */
public class DatasetUtils {

	private DatasetUtils() {
		// static use only
	}

	/**
	 * Test if any of the given datasets are merge sources.
	 * 
	 * @return true if any of the datasets are a merge source, false otherwise
	 */
	public static boolean hasMergeSource(@NonNull List<IAnalysisDataset> datasets) {
		for (IAnalysisDataset d : datasets) {
			if (DatasetListManager.getInstance().isMergeSource(d))
				return true;
		}
		return false;
	}
}
