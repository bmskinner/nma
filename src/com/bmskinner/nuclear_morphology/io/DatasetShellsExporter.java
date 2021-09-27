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
package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.signals.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.Aggregation;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.CountType;
import com.bmskinner.nuclear_morphology.components.signals.ISignalGroup;

/**
 * Export shell analysis results to a file
 * @author ben
 * @since 1.13.8
 *
 */
public class DatasetShellsExporter extends StatsExporter {

    /**
     * Create specifying the folder stats will be exported into
     * 
     * @param folder
     */
    public DatasetShellsExporter(@NonNull File file, @NonNull List<IAnalysisDataset> list) {
        super(file, list);
    }

    /**
     * Create specifying the folder stats will be exported into
     * 
     * @param folder
     */
    public DatasetShellsExporter(@NonNull File file, @NonNull IAnalysisDataset dataset) {
        super(file, dataset);
    }

    /**
     * Append a column header line to the StringBuilder.
     * @param outLine
     */
    @Override
    protected void appendHeader(StringBuilder outLine) {

        String[] headers = {
            "Dataset",
            "CellId",
            "ComponentId",
            "ComponentType",
            "ComponentImageFolder",
            "ComponentImage",
            "SignalType",
            "SignalGroupId",
            "SignalGroup",
            "SignalFolder",
            "SignalImage",
            "SignalChannel",
            "Aggregation"
        };

        outLine.append(Stream.of(headers).collect(Collectors.joining(TAB))+TAB);
        
        for(int i=0; i<getMaximumNumberOfShells(); i++){
            String label = "Signal_shell_"+i;
            outLine.append(label + TAB); 
        }
        
        for(int i=0; i<getMaximumNumberOfShells(); i++){
            String label = "Counterstain_shell_"+i;
            outLine.append(label + TAB); 
        }
        
        // remove the final tab character
        if (outLine.length() > 0)
            outLine.setLength(outLine.length() - 1);
        
        outLine.append(NEWLINE);
    }
    
    /**
     * Append the given dataset stats into the string builder
     * @param d the dataset to export
     * @param outLine the string builder to append to
     * @throws UnloadableImageException 
     * @throws UnavailableBorderTagException
     * @throws UnavailableProfileTypeException
     * @throws ProfileException
     */
    @Override
    protected void append(@NonNull IAnalysisDataset d, @NonNull StringBuilder outLine) {
        
        
        for(@NonNull UUID signalGroupId : d.getCollection().getSignalGroupIDs()){
        	
        	Optional<ISignalGroup> groupOptn =  d.getCollection().getSignalGroup(signalGroupId);
        	if(!groupOptn.isPresent())
        		continue;
            ISignalGroup signalGroup = groupOptn.get();
            String groupName   = signalGroup.getGroupName();
            String groupFolder = d.getAnalysisOptions().get().getNuclearSignalOptions(signalGroupId).getFolder().getAbsolutePath();            
            Optional<IShellResult> oShellResult = signalGroup.getShellResult();
            if(!oShellResult.isPresent())
                continue;
            
            IShellResult shellResult = oShellResult.get();

            for (ICell cell : d.getCollection().getCells()) {
                
                if (!cell.hasNucleus())
                    continue;

                for (Nucleus n : cell.getNuclei()) {

                    if(!n.getSignalCollection().hasSignal(signalGroupId))
                        continue;
                    
                    // Append random signal
                    outLine.append(d.getName() + TAB)
                    .append(cell.getId() + TAB)
                    .append(n.getID() + TAB)
                    .append(CellularComponent.NUCLEUS + TAB)
                    .append(n.getSourceFolder() + TAB)
                    .append(n.getSourceFileName() + TAB)
                    .append(CellularComponent.NUCLEUS + TAB)
                    .append(IShellResult.RANDOM_SIGNAL_ID.toString() + TAB)
                    .append("Random" + TAB)
                    .append("Null" + TAB)
                    .append("Null" + TAB)
                    .append("Null"+TAB)
                    .append(Aggregation.BY_NUCLEUS + TAB);

                    long[] randomByNucleus =  d.getCollection().getSignalGroup(IShellResult.RANDOM_SIGNAL_ID).get().getShellResult().get().getPixelValues(CountType.SIGNAL, cell, n, null);
                    long[] counterstain    = shellResult.getPixelValues(CountType.COUNTERSTAIN, cell, n, null);

                    for(int i=0; i<shellResult.getNumberOfShells(); i++){
                        outLine.append(randomByNucleus[i]+TAB);
                    }
                    for(int i=0; i<shellResult.getNumberOfShells(); i++){
                        outLine.append(counterstain[i]+TAB);
                    }

                    if (outLine.length() > 0)
                        outLine.setLength(outLine.length() - 1);
                    outLine.append(NEWLINE);
                    
                    
                    // Append real signals

                    outLine.append(d.getName() + TAB)
                    .append(cell.getId() + TAB)
                    .append(n.getID() + TAB)
                    .append(CellularComponent.NUCLEUS + TAB)
                    .append(n.getSourceFolder() + TAB)
                    .append(n.getSourceFileName() + TAB)
                    .append(CellularComponent.NUCLEUS + TAB)
                    .append(signalGroupId.toString() + TAB)
                    .append(groupName + TAB)
                    .append(groupFolder + TAB)
                    .append(n.getSignalCollection().getSourceFile(signalGroupId).getName() + TAB)
                    .append(n.getSignalCollection().getSourceChannel(signalGroupId)+TAB)
                    .append(Aggregation.BY_NUCLEUS + TAB);

                    long[] signalByNucleus = shellResult.getPixelValues(CountType.SIGNAL, cell, n, null);

                    for(int i=0; i<shellResult.getNumberOfShells(); i++){
                        outLine.append(signalByNucleus[i]+TAB);
                    }
                    for(int i=0; i<shellResult.getNumberOfShells(); i++){
                        outLine.append(counterstain[i]+TAB);
                    }

                    if (outLine.length() > 0)
                        outLine.setLength(outLine.length() - 1);
                    outLine.append(NEWLINE);

                    for(INuclearSignal s : n.getSignalCollection().getSignals(signalGroupId)){

                        outLine.append(d.getName() + TAB)
                        .append(cell.getId() + TAB)
                        .append(n.getID() + TAB)
                        .append(CellularComponent.NUCLEUS + TAB)
                        .append(n.getSourceFolder() + TAB)
                        .append(n.getSourceFileName() + TAB)
                        .append(CellularComponent.NUCLEAR_SIGNAL + TAB)
                        .append(signalGroupId.toString() + TAB)
                        .append(groupName + TAB)
                        .append(groupFolder + TAB)
                        .append(s.getSourceFile().getName() + TAB)
                        .append(s.getChannel() + TAB)
                        .append(Aggregation.BY_SIGNAL + TAB);

                        long[] signalBySignal = shellResult.getPixelValues(CountType.SIGNAL, cell, n, s);

                        for(int i=0; i<shellResult.getNumberOfShells(); i++){
                            outLine.append(signalBySignal[i]+TAB);
                        }
                        for(int i=0; i<shellResult.getNumberOfShells(); i++){
                            outLine.append(counterstain[i]+TAB);
                        }

                        if (outLine.length() > 0)
                            outLine.setLength(outLine.length() - 1);
                        outLine.append(NEWLINE);
                    }
                }



            }
        }
    }
    
    
    /**
     * Find the maximum number of shells within the datasets
     * @return
     */
    private int getMaximumNumberOfShells(){
        int shells = 0;
        for(IAnalysisDataset d : datasets){
            SignalManager sm = d.getCollection().getSignalManager();
            int sc = sm.getShellCount();
            shells = sc>shells?sc:shells;
        }
        return shells;
    }

}
