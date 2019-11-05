/*******************************************************************************
 * Copyright (C) 2019 Ben Skinner
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
package com.bmskinner.nuclear_morphology.analysis.signals;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.signals.PairedSignalGroups.DatasetSignalId;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;

/**
 * Method to merge signals groups within a single dataaset
 * @author bs19022
 * @since 1.16.1
 *
 */
public class SignalGroupMergeMethod extends SingleDatasetAnalysisMethod {
	
	private static final Logger LOGGER = Logger.getGlobal();
	
	private static final int MAX_PROGRESS = 100;
    private static final int MILLISECONDS_TO_SLEEP = 10;
	
	private PairedSignalGroups pairedSignalGroups;

	/**
	 * Create the method
	 * @param dataset the dataset with signals to be merged
	 * @param pairedSignalGroups the signal groups to be merged
	 */
	public SignalGroupMergeMethod(@NonNull IAnalysisDataset dataset, @NonNull PairedSignalGroups pairedSignalGroups) {
		super(dataset);
		
		if(dataset.getCollection().getSignalManager().getSignalGroupCount()<2)
        	throw new IllegalArgumentException("Must have two or more signal groups to merge");
		this.pairedSignalGroups = pairedSignalGroups;
		
	}

	@Override
	public IAnalysisResult call() throws Exception {
		mergeSignalGroups();
		spinWheels(MAX_PROGRESS, MILLISECONDS_TO_SLEEP);
		return new DefaultAnalysisResult(dataset);
	}
	
	private void mergeSignalGroups() {
        if (pairedSignalGroups == null || pairedSignalGroups.isEmpty()) {
            LOGGER.finer( "No signal groups to merge");
            return;
        }
		LOGGER.fine("Pairs to merge: "+pairedSignalGroups.toString());
        
        // For each set of mergeable signals, make a new signal group
        for(Entry<DatasetSignalId, Set<DatasetSignalId>> entry : pairedSignalGroups.entrySet()) {
        	
        	// Create merged group name
        	DatasetSignalId initialId = entry.getKey();
        	StringBuilder sb = new StringBuilder();
        	
        	Set<DatasetSignalId> idSet = entry.getValue();
        	idSet.add(initialId);
        	        	
        	for(DatasetSignalId id : idSet) {
        		ISignalGroup signalGroup = dataset.getCollection().getSignalGroup(id.s).get();
        		sb.append(signalGroup.getGroupName()+"_");
        	}
        	sb.append("merged");
        	SignalGroup newGroup = new SignalGroup(sb.toString());
        	
        	// Duplicate the signals into the new signal group
        	UUID newId = UUID.randomUUID();
        	dataset.getCollection().addSignalGroup(newId, newGroup);
        	for(Nucleus n : dataset.getCollection().getNuclei()) {
        		for(DatasetSignalId id : idSet) {
        			List<INuclearSignal> signals = n.getSignalCollection().getSignals(id.s);
        			for(INuclearSignal s : signals) {
        				n.getSignalCollection().addSignal(s.duplicate(), newId);
        			}
        		}
        	}

        }    
        
       
    }

}
