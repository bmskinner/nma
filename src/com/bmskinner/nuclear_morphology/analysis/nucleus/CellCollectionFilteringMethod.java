package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.util.List;
import java.util.function.Predicate;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.MultipleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;

/**
 * Perform filtering of collections on arbitrary predicates
 * @author ben
 * @since 1.14.0
 *
 */
public class CellCollectionFilteringMethod extends MultipleDatasetAnalysisMethod {
		
	private final Predicate<ICell> predicate;
	private final String newCollectionName;
	
	public CellCollectionFilteringMethod(@NonNull IAnalysisDataset dataset, @NonNull Predicate<ICell> pred, @NonNull String name) {
		super(dataset);
		predicate = pred;
		newCollectionName = name;
	}
	
	public CellCollectionFilteringMethod(@NonNull List<IAnalysisDataset> datasets, @NonNull Predicate<ICell> pred, @NonNull String name) {
		super(datasets);
		predicate = pred;
		newCollectionName = name;
	}


	@Override
	public IAnalysisResult call() throws Exception {

		for(IAnalysisDataset d : datasets) {
			ICellCollection filtered = d.getCollection().filter(predicate);
			if(filtered==null)
				continue;
			if (!filtered.hasCells())
				log("No cells passed filter for "+d.getName());
			if(filtered.size()==d.getCollection().size()) {
				log("Skipping; all cells passed filter for "+d.getName());
				continue;
			}
			ICellCollection v = new VirtualCellCollection(d, filtered);
			v.setName(newCollectionName);
			try {
				d.getCollection().getProfileManager().copyCollectionOffsets(v);
				d.getCollection().getSignalManager().copySignalGroups(v);

			} catch (ProfileException e) {
				warn("Error copying collection offsets");
				stack("Error in offsetting", e);
			}
			d.addChildCollection(v);
		}
		
		return new DefaultAnalysisResult(datasets);
	}
	

}
