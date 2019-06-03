/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.MultipleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Perform filtering of collections on arbitrary predicates
 * @author ben
 * @since 1.14.0
 *
 */
public class CellCollectionFilteringMethod extends MultipleDatasetAnalysisMethod {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		
	private final FilteringOptions options;
	private final String newCollectionName;
	
	public CellCollectionFilteringMethod(@NonNull IAnalysisDataset dataset, @NonNull FilteringOptions op, @NonNull String name) {
		super(dataset);
		options = op;
		newCollectionName = name;
	}
	
	public CellCollectionFilteringMethod(@NonNull List<IAnalysisDataset> datasets, @NonNull FilteringOptions op, @NonNull String name) {
		super(datasets);
		options = op;
		newCollectionName = name;
	}


	@Override
	public IAnalysisResult call() throws Exception {

		for(IAnalysisDataset d : datasets) {
			ICellCollection filtered = d.getCollection().filter(options.getPredicate(d.getCollection()));
			if(filtered==null)
				continue;
			if (!filtered.hasCells())
				LOGGER.info("No cells passed filter for "+d.getName());
			if(filtered.size()==d.getCollection().size()) {
				LOGGER.info("Skipping; all cells passed filter for "+d.getName());
				continue;
			}
			ICellCollection v = new VirtualCellCollection(d, filtered);
			v.setName(newCollectionName);
			try {
				d.getCollection().getProfileManager().copyCollectionOffsets(v);
				d.getCollection().getSignalManager().copySignalGroups(v);

			} catch (ProfileException e) {
				LOGGER.warning("Error copying collection offsets");
				LOGGER.log(Loggable.STACK, "Error in offsetting", e);
			}
			d.addChildCollection(v);
		}
		
		return new DefaultAnalysisResult(datasets);
	}
	

}
