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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.datasets.DefaultAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.datasets.VirtualDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.signals.DefaultSignalGroup;
import com.bmskinner.nuclear_morphology.components.signals.ISignalGroup;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Extract virtual merge source datasets into real root datasets.
 * @author bms41
 * @since 1.13.8
 *
 */
public class MergeSourceExtractionMethod extends MultipleDatasetAnalysisMethod {
	
	private static final Logger LOGGER = Logger.getLogger(MergeSourceExtractionMethod.class.getName());
    
    public MergeSourceExtractionMethod(@NonNull List<IAnalysisDataset> toExtract) {
        super(toExtract);
    }
    
    @Override
    public IAnalysisResult call() throws Exception {
        List<IAnalysisDataset> extracted = extractSourceDatasets();
        return new DefaultAnalysisResult(extracted);
    }
    
    private List<IAnalysisDataset> extractSourceDatasets(){
    	LOGGER.fine("Extracting merge sources");
    	List<IAnalysisDataset> result = new ArrayList<>();     
    	
    	DatasetValidator dv = new DatasetValidator();
    	
        for (IAnalysisDataset virtualMergeSource : datasets) {
            
        	try {
        	IAnalysisDataset extracted = extractMergeSource(virtualMergeSource);
        	
            LOGGER.fine("Checking new datasets from merge source "+extracted.getName());
         	if(!dv.validate(extracted)) {
         		LOGGER.warning("New dataset failed to validate; resegmentation is recommended");
         		LOGGER.fine(dv.getErrors().stream().collect(Collectors.joining("\n")));
         	}

            result.add(extracted);
        	} catch(MissingOptionException | MissingLandmarkException e) {
        		LOGGER.warning("Missing analysis options or landmark; skipping "+virtualMergeSource.getName());  
        		LOGGER.log(Loggable.STACK, "Missing analysis options in dataset "+virtualMergeSource.getName(),e);
        	}

        }
        LOGGER.fine("Finished extracting merge sources");
        return result;
    }
    
    /**
     * Extract the merge source for the given dataset into a real collection
     * @param template
     * @return
     * @throws MissingOptionException 
     * @throws MissingLandmarkException 
     * @throws NoSuchElementException if the template analysis options are not present
     */
    private IAnalysisDataset extractMergeSource(@NonNull IAnalysisDataset template) throws MissingOptionException, MissingLandmarkException {

    	ICellCollection templateCollection = template.getCollection();
    	
    	// Make a new real cell collection from the virtual collection
    	File imageFolder = template.getAnalysisOptions()
    			.orElseThrow(MissingOptionException::new)
    			.getNucleusDetectionOptions()
    			.orElseThrow(MissingOptionException::new)
    			.getFile(HashOptions.DETECTION_FOLDER);
    	
    	ICellCollection newCollection = new DefaultCellCollection(
    			templateCollection.getRuleSetCollection(), templateCollection.getName(), templateCollection.getId());

    	templateCollection.getCells().forEach(c->newCollection.addCell(c.duplicate()));


    	IAnalysisDataset newDataset = new DefaultAnalysisDataset(newCollection, template.getSavePath());
    	
    	try {
    		// Copy over the profile collections
    		newDataset.getCollection().createProfileCollection();

    		IAnalysisDataset parent = getRootParent(template);

    		// Copy the merged dataset segmentation into the new dataset.
    		// This wil match cell segmentations by default, since the cells
    		// have been copied from the merged dataset.
    		parent.getCollection().getProfileManager()
    		.copySegmentsAndLandmarksTo(newDataset.getCollection());

    		// Copy over the signal collections where appropriate
    		copySignalGroups(templateCollection, newDataset);

    		// Child datasets are not present in merge sources

    	} catch (ProfileException | MissingProfileException e) {
    		LOGGER.log(Loggable.STACK, "Cannot copy profile offsets to recovered merge source", e);
    	}

         Optional<IAnalysisOptions> op = template.getAnalysisOptions();
         if(op.isPresent())
             newDataset.setAnalysisOptions(op.get().duplicate());
         
         return newDataset;
    }
    
    /**
     * Get the root parent of the dataset. IF the dataset is root, returns unchanged.
     * @param dataset the dataset to get the root parent of
     * @return the root parent of the dataset
     */
    private @NonNull IAnalysisDataset getRootParent(@NonNull IAnalysisDataset dataset) {
    	if(dataset.isRoot())
    		return dataset;
    	
    	if (dataset instanceof VirtualDataset) {

    		VirtualDataset d = (VirtualDataset) dataset;
     		IAnalysisDataset parent =  d.getParent().get();
     		if(parent.isRoot())
     			return parent;
     		return getRootParent(parent);
     	}
    	return dataset;
    }
    
    /**
     * Copy any signal groups in the template collection into the new dataset.
     * @param templateCollection the collection to copy signal groups from
     * @param newDataset the dataset to copy the signal groups to
     * @throws MissingOptionException 
     * @throws NoSuchElementException if a template signal group is not present
     */
    private void copySignalGroups(ICellCollection templateCollection, IAnalysisDataset newDataset) throws MissingOptionException{
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
				ISignalGroup oldGroup = templateCollection.getSignalGroup(signalGroupId).orElseThrow(MissingOptionException::new);
				ISignalGroup newGroup = new DefaultSignalGroup(oldGroup);
			    newDataset.getCollection().addSignalGroup(newGroup);
			}
        }
    }
}
