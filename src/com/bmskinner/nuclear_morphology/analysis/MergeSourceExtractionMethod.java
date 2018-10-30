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
package com.bmskinner.nuclear_morphology.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.ChildAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.DefaultAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.MergeSourceAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.io.DatasetConverter;
import com.bmskinner.nuclear_morphology.io.DatasetConverter.DatasetConversionException;

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
    	fine("Extracting merge sources");
    	List<IAnalysisDataset> result = new ArrayList<>();     
    	
    	DatasetValidator dv = new DatasetValidator();
    	
        for (IAnalysisDataset virtualMergeSource : datasets) {
            
        	IAnalysisDataset extracted = extractMergeSource(virtualMergeSource);
            fine("Checking new datasets from merge source "+extracted.getName());
         	if(!dv.validate(extracted)) {
         		warn("New dataset failed to validate; resegmentation is recommended");
         		fine(dv.getErrors().stream().collect(Collectors.joining("\n")));
         	}

            result.add(extracted);

        }
        fine("Finished extracting merge sources");
        return result;
    }
    
    private IAnalysisDataset extractMergeSource(IAnalysisDataset template) {

    	ICellCollection templateCollection = template.getCollection();
    	// Make a new real cell collection from the virtual collection
    	ICellCollection newCollection = new DefaultCellCollection(templateCollection.getFolder(), null,
    			templateCollection.getName(), templateCollection.getNucleusType());

    	templateCollection.getCells().forEach(c->newCollection.addCell(c.duplicate()));


    	IAnalysisDataset newDataset = new DefaultAnalysisDataset(newCollection);
    	newDataset.setRoot(true);
    	
    	try {
    		// Copy over the profile collections
    		newDataset.getCollection().createProfileCollection();

    		IAnalysisDataset parent = getRootParent(template);

    		// Copy the merged dataset segmentation into the new dataset.
    		// This wil match cell segmentations by default, since the cells
    		// have been copied from the merged dataset.
    		parent.getCollection().getProfileManager()
    		.copyCollectionOffsets(newDataset.getCollection());

    		// Copy over the signal collections where appropriate
    		copySignalGroups(templateCollection, newDataset);

    		// Child datasets are not present in merge sources
//    		copyChildDatasets(template, newDataset);

    	} catch (ProfileException e) {
    		error("Cannot copy profile offsets to recovered merge source", e);
    	}

         Optional<IAnalysisOptions> op = template.getAnalysisOptions();
         if(op.isPresent())
             newDataset.setAnalysisOptions(op.get().duplicate());
         
         return newDataset;
    }
    
    private IAnalysisDataset getRootParent(IAnalysisDataset dataset) {
    	if(dataset.isRoot())
    		return dataset;
    	if (dataset instanceof MergeSourceAnalysisDataset) {

     		MergeSourceAnalysisDataset d = (MergeSourceAnalysisDataset) dataset;
     		IAnalysisDataset parent =  d.getParent();
     		if(parent.isRoot())
     			return parent;
     		return getRootParent(parent);
     	}
    	return null;
    }
    
    
//    private void copyChildDatasets(IAnalysisDataset template, IAnalysisDataset newDataset) throws ProfileException{
//    	 fine("Adding children of "+template.getName());
//    	for(IAnalysisDataset childTemplate : template.getChildDatasets()) {
//    		fine("Adding child "+childTemplate.getName());
//    		ICellCollection templateCollection = childTemplate.getCollection();
//    		
//            // Make a new real cell collection from the virtual collection
//            ICellCollection newCollection = new VirtualCellCollection(newDataset, templateCollection.getName());
//            
//            templateCollection.getCells().forEach(c->newCollection.addCell(c));
//
//            IAnalysisDataset newChildDataset = new ChildAnalysisDataset(newDataset, newCollection);
//            newDataset.addChildDataset(newChildDataset);
//            newChildDataset.getCollection().createProfileCollection();
//            
//            // Recursive copy
//            copyChildDatasets(childTemplate, newChildDataset);
//    		
//    	}
//    	
//    	
//    }
    
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
