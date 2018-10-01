/*******************************************************************************
 *      Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.DefaultAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.MergeSourceAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;

/**
 * Extract virtual merge source datasets into real root datasets.
 * @author bms41
 * @since 1.13.8
 *
 */
public class MergeSourceExtractionMethod extends MultipleDatasetAnalysisMethod {
    
    public MergeSourceExtractionMethod(@NonNull List<IAnalysisDataset> toExtract) {
        super(toExtract);
    }
    
    @Override
    public IAnalysisResult call() throws Exception {
        List<IAnalysisDataset> extracted = extractSourceDatasets();
        IAnalysisResult r = new DefaultAnalysisResult(extracted);
        return r;
    }
    
    private List<IAnalysisDataset> extractSourceDatasets(){
        List<IAnalysisDataset> result = new ArrayList<>();     
        
        for (IAnalysisDataset virtualMergeSource : datasets) {
            
            ICellCollection templateCollection = virtualMergeSource.getCollection();
            // Make a new real cell collection from the virtual collection
            ICellCollection newCollection = new DefaultCellCollection(templateCollection.getFolder(), null,
                    templateCollection.getName(), templateCollection.getNucleusType());

            templateCollection.getCells().forEach(c->newCollection.addCell(new DefaultCell(c)));


            IAnalysisDataset newDataset = new DefaultAnalysisDataset(newCollection);
            newDataset.setRoot(true);
            try {
            	// Copy over the profile collections
            	newDataset.getCollection().createProfileCollection();

            	// Copy the merged dataset segmentation into the new dataset.
            	// This wil match cell segmentations by default, since the cells
            	// have been copied from the merged dataset.
            	if (virtualMergeSource instanceof MergeSourceAnalysisDataset) {

            		MergeSourceAnalysisDataset d = (MergeSourceAnalysisDataset) virtualMergeSource;


            		IAnalysisDataset parent =  d.getParent();
            		// When the parent is also a virtual cell collection, recurse up to the root dataset
            		while (parent instanceof MergeSourceAnalysisDataset) {
            			parent = ((MergeSourceAnalysisDataset) parent).getParent();
            		}

            		parent.getCollection().getProfileManager()
            		.copyCollectionOffsets(newDataset.getCollection());


            	}

            } catch (ProfileException e) {
            	error("Cannot copy profile offsets to recovered merge source", e);
            }

            // Copy over the signal collections where appropriate
            copySignalGroups(templateCollection, newDataset);

            Optional<IAnalysisOptions> op = virtualMergeSource.getAnalysisOptions();
            if(op.isPresent())
                newDataset.setAnalysisOptions(op.get());
            
            DatasetValidator dv = new DatasetValidator();
        	if(!dv.validate(newDataset))
        		warn("New dataset failed to validate");

            result.add(newDataset);

        }
        return result;
    }
    
    
    private void copySignalGroups(ICellCollection templateCollection, IAnalysisDataset newDataset){
        ICellCollection newCollection = newDataset.getCollection();
        for (UUID signalGroupId : templateCollection.getSignalGroupIDs()) {

            // We only want to make a signal group if a cell with
			// the signal
			// is present in the merge source.
			boolean addSignalGroup = false;
			for (Nucleus n : newCollection.getNuclei()) {
			    addSignalGroup |= n.getSignalCollection().hasSignal(signalGroupId);
			}

			if (addSignalGroup) {
				ISignalGroup newGroup = new SignalGroup(templateCollection.getSignalGroup(signalGroupId).get());
			    newDataset.getCollection().addSignalGroup(signalGroupId, newGroup);
			}
        }
    }
}
